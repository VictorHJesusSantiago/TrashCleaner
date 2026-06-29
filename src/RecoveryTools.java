import java.io.*;
import java.util.*;

/**
 * Ferramentas de recuperacao e reparo: pontos de restauracao (listar/aplicar),
 * MBR/boot, permissoes de arquivo, servicos criticos e associacoes de arquivo.
 */
public final class RecoveryTools {

    private RecoveryTools() {}

    // ---------------------------------------------------------------
    // Listar pontos de restauracao
    // ---------------------------------------------------------------

    public static void listRestorePoints(SystemInfo si, Logger log) {
        log.section("PONTOS DE RESTAURACAO DO SISTEMA");

        if (si.isVistaPlus()) {
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "try { " +
                "  Get-ComputerRestorePoint " +
                "  | Select-Object SequenceNumber,Description,CreationTime,RestorePointType " +
                "  | Format-Table -AutoSize " +
                "} catch { Write-Host 'Pontos de restauracao nao disponiveis ou protecao desabilitada.' }");
        } else {
            // XP: VBScript WMI
            File vbs = new File(si.temp + "\\_tc_rp.vbs");
            try (PrintWriter pw = new PrintWriter(new FileWriter(vbs))) {
                pw.println("Set rp = GetObject(\"winmgmts:\\\\.\\root\\default\")");
                pw.println("For Each p in rp.ExecQuery(\"SELECT * FROM SystemRestore\")");
                pw.println("  WScript.Echo p.SequenceNumber & \": \" & p.Description & \" (\" & p.CreationTime & \")\"");
                pw.println("Next");
            } catch (IOException ignored) {}
            Utils.execPrint("cscript", "//nologo", vbs.getAbsolutePath());
            vbs.delete();
        }

        log.ok("Lista de pontos de restauracao exibida.");
    }

    // ---------------------------------------------------------------
    // Restaurar para um ponto especifico
    // ---------------------------------------------------------------

    public static void applyRestorePoint(SystemInfo si, Logger log, int sequenceNumber) {
        log.section("RESTAURAR SISTEMA - PONTO " + sequenceNumber);
        log.warn("ATENCAO: A restauracao reiniciara o computador!");
        log.warn("Salve todos os arquivos abertos antes de continuar.");
        log.println("");

        if (si.isVistaPlus()) {
            log.info("Iniciando restauracao para o ponto " + sequenceNumber + "...");
            int r = Utils.exec("powershell", "-noprofile", "-Command",
                "try { Restore-Computer -RestorePoint " + sequenceNumber + " } " +
                "catch { Write-Host $_.Exception.Message }");
            if (r == 0) {
                log.ok("Restauracao iniciada. O computador sera reiniciado.");
            } else {
                log.warn("Nao foi possivel iniciar a restauracao.");
                log.println("  Acesse: Painel de Controle > Sistema > Protecao do Sistema > Restaurar");
            }
        } else {
            // XP: rstrui.exe
            log.info("Abrindo assistente de restauracao do sistema (XP)...");
            Utils.exec("rstrui.exe");
        }
    }

    // ---------------------------------------------------------------
    // Reparar MBR / Boot (bootrec)
    // ---------------------------------------------------------------

    public static void repairBoot(SystemInfo si, Logger log, String mode) {
        log.section("REPARAR MBR / BOOT");

        if (!si.isVistaPlus()) {
            log.warn("bootrec.exe disponivel somente no Vista+.");
            log.println("  Para reparar o MBR no XP, use:");
            log.println("    fixmbr  (no Recovery Console)");
            log.println("    fixboot (no Recovery Console)");
            return;
        }

        log.warn("ATENCAO: Operacoes no MBR/Boot podem deixar o sistema inacessivel se mal executadas!");
        log.warn("Certifique-se de ter uma midia de recuperacao disponivel.");
        log.println("");

        switch (mode.toLowerCase()) {
            case "fixmbr":
                log.info("Reparando Master Boot Record (MBR)...");
                Utils.execPrint("bootrec", "/fixmbr");
                log.ok("MBR reparado.");
                break;
            case "fixboot":
                log.info("Reparando setor de boot do volume ativo...");
                Utils.execPrint("bootrec", "/fixboot");
                log.ok("Setor de boot reparado.");
                break;
            case "rebuildbcd":
                log.info("Reconstruindo BCD (Boot Configuration Data)...");
                Utils.execPrint("bootrec", "/scanos");
                Utils.execPrint("bootrec", "/rebuildbcd");
                log.ok("BCD reconstruido.");
                break;
            case "all":
            default:
                log.info("Executando reparacao completa do boot...");
                Utils.execPrint("bootrec", "/fixmbr");
                Utils.execPrint("bootrec", "/fixboot");
                Utils.execPrint("bootrec", "/scanos");
                Utils.execPrint("bootrec", "/rebuildbcd");
                log.ok("Reparacao completa do boot executada.");
                log.println("  Reinicie o computador para verificar se o boot foi restaurado.");
                break;
        }
    }

    // ---------------------------------------------------------------
    // Reparar permissoes de arquivo do sistema
    // ---------------------------------------------------------------

    public static void repairPermissions(SystemInfo si, Logger log, String target) {
        log.section("REPARAR PERMISSOES DE ARQUIVO");

        if (target == null || target.isEmpty()) {
            target = si.systemRoot + "\\System32";
        }

        log.info("Redefinindo permissoes em: " + target);
        log.println("  (Pode demorar varios minutos em pastas grandes)");
        log.println("");

        // icacls /reset redefine para as permissoes herdadas do pai
        int r = Utils.exec("icacls", target, "/reset", "/T", "/C", "/L", "/Q");
        if (r == 0) {
            log.ok("Permissoes redefinidas em: " + target);
        } else {
            log.warn("Algumas permissoes nao puderam ser redefinidas (arquivos em uso ou acesso negado).");
        }

        // Para o diretorio do usuario
        if (target.equalsIgnoreCase(si.systemRoot + "\\System32")) {
            log.info("Redefinindo permissoes do perfil do usuario...");
            Utils.exec("icacls", si.userProfile, "/reset", "/T", "/C", "/L", "/Q");
            log.ok("Permissoes do perfil do usuario redefinidas.");
        }
    }

    // ---------------------------------------------------------------
    // Reparar associacoes de arquivo
    // ---------------------------------------------------------------

    public static void repairFileAssociations(SystemInfo si, Logger log) {
        log.section("REPARAR ASSOCIACOES DE ARQUIVO");

        log.info("Verificando associacoes de arquivo do sistema...");
        log.println("");

        // Listar associacoes comuns e seus programas
        String[] exts = {".txt", ".html", ".pdf", ".jpg", ".png", ".mp3", ".mp4",
            ".doc", ".docx", ".xls", ".xlsx", ".zip", ".rar"};

        log.println(String.format("  %-12s  %s", "Extensao", "Programa associado"));
        log.println("  " + rep('-', 60));

        for (String ext : exts) {
            String assoc = Utils.execCapture("assoc", ext);
            if (assoc == null) assoc = "";
            assoc = assoc.trim();
            if (assoc.isEmpty() || assoc.contains("not found") || assoc.contains("nao encontrada")) {
                log.println(String.format("  %-12s  [SEM ASSOCIACAO]", ext));
            } else {
                String ftype = assoc.contains("=") ?
                    assoc.substring(assoc.indexOf('=') + 1).trim() : assoc;
                // Resolver ftype para programa
                String prog = Utils.execCapture("ftype", ftype.trim());
                if (prog == null) prog = "";
                prog = prog.trim();
                if (prog.contains("=")) prog = prog.substring(prog.indexOf('=')+1).trim();
                log.println(String.format("  %-12s  %s", ext,
                    prog.isEmpty() ? ftype : prog));
            }
        }

        log.println("");
        log.println("  Restaurando associacoes criticas do sistema...");

        // Restaurar .exe via ftype e assoc
        Utils.exec("assoc", ".exe=exefile");
        Utils.exec("ftype", "exefile=%1 %*");
        Utils.exec("assoc", ".bat=batfile");
        Utils.exec("ftype", "batfile=cmd.exe /c \"%1\" %*");
        Utils.exec("assoc", ".com=comfile");
        Utils.exec("ftype", "comfile=%1 %*");
        Utils.exec("assoc", ".cmd=cmdfile");
        Utils.exec("ftype", "cmdfile=cmd.exe /c \"%1\" %*");
        Utils.exec("assoc", ".msi=Msi.Package");
        Utils.exec("assoc", ".reg=regfile");
        Utils.exec("ftype", "regfile=regedit.exe \"%1\"");

        log.ok("Associacoes criticas restauradas (.exe, .bat, .com, .cmd, .msi, .reg).");
        log.println("  Para associacoes de documentos (.doc, .pdf etc.), use:");
        log.println("  Configuracoes > Aplicativos > Aplicativos Padrao");
    }

    // ---------------------------------------------------------------
    // Verificar e reparar .NET Framework
    // ---------------------------------------------------------------

    public static void checkDotNet(SystemInfo si, Logger log) {
        log.section("VERIFICAR .NET FRAMEWORK");

        log.println("  --- Versoes do .NET Framework instaladas ---");
        log.println("");

        // Listar via registro
        try {
            Process p = new ProcessBuilder("reg", "query",
                "HKLM\\SOFTWARE\\Microsoft\\NET Framework Setup\\NDP",
                "/s", "/v", "Version")
                .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("Version")) {
                    log.println("  " + line.trim());
                }
            }
            p.waitFor();
        } catch (Exception ignored) {}

        if (si.isVistaPlus()) {
            log.println("");
            log.println("  --- .NET via PowerShell ---");
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "(Get-ChildItem 'HKLM:\\SOFTWARE\\Microsoft\\NET Framework Setup\\NDP' " +
                "-Recurse | Get-ItemProperty -Name Version,Release -ErrorAction SilentlyContinue " +
                "| Where-Object {$_.Version}) | " +
                "Sort-Object Version | Format-Table PSPath,Version -AutoSize");
        }

        if (si.isWin8Plus()) {
            log.println("");
            log.println("  --- .NET Core / .NET 5+ (se instalado) ---");
            Utils.execPrint("dotnet", "--list-runtimes");
        }

        log.ok("Verificacao do .NET concluida.");
    }

    // ---------------------------------------------------------------
    // Reparar WinSxS via DISM (alias para Cleaner.cleanWinSxS)
    // ---------------------------------------------------------------

    public static void repairWinSxS(SystemInfo si, Logger log) {
        Cleaner.cleanWinSxS(si, log);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static String rep(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }
}
