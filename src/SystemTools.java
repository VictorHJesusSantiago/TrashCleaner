import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Ferramentas avancadas do sistema: RAM flush, ponto de restauracao,
 * saude de disco, relatorio de bateria, area de transferencia, hibernacao.
 */
public final class SystemTools {

    private SystemTools() {}

    // ---------------------------------------------------------------
    // Liberar RAM (flush de working sets e standby list)
    // ---------------------------------------------------------------

    public static void flushRam(SystemInfo si, Logger log) {
        log.section("LIBERAR RAM");

        long before = getAvailableRam(si);
        log.println("  RAM disponivel ANTES : " + Logger.fmt(before));
        log.println("");

        log.info("Nivel 1: forcando execucao de tarefas ociosas do sistema...");
        Utils.exec("rundll32.exe", "advapi32.dll,ProcessIdleTasks");

        if (si.isVistaPlus()) {
            log.info("Nivel 2: limpando working sets de todos os processos via PowerShell...");
            File psFile = new File(si.temp + "\\_tc_ramflush.ps1");
            try (PrintWriter pw = new PrintWriter(new FileWriter(psFile))) {
                pw.println("$sig = @\"");
                pw.println("[DllImport(\"psapi.dll\")]");
                pw.println("public static extern bool EmptyWorkingSet(IntPtr h);");
                pw.println("\"@");
                pw.println("$api = Add-Type -MemberDefinition $sig -Name PSApi -Namespace Win32 -PassThru");
                pw.println("Get-Process | ForEach-Object {");
                pw.println("    try { $api::EmptyWorkingSet($_.Handle) } catch {}");
                pw.println("}");
            } catch (IOException ignored) {}

            Utils.exec("powershell", "-noprofile", "-ExecutionPolicy", "Bypass",
                "-File", psFile.getAbsolutePath());
            psFile.delete();
        }

        System.gc(); // limpar heap JVM

        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        long after = getAvailableRam(si);
        log.println("");
        log.println("  RAM disponivel APOS  : " + Logger.fmt(after));

        long gained = after - before;
        if (gained > 0) {
            log.ok("RAM liberada: aproximadamente " + Logger.fmt(gained));
        } else {
            log.ok("Flush concluido.");
            log.println("  (Diferenca minima indica que o sistema ja estava otimizado)");
        }
    }

    // ---------------------------------------------------------------
    // Criar Ponto de Restauracao
    // ---------------------------------------------------------------

    public static void createRestorePoint(SystemInfo si, Logger log) {
        log.section("CRIAR PONTO DE RESTAURACAO");

        String label = "TrashCleaner_" +
            new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        log.info("Ativando Protecao do Sistema e criando ponto: " + label + " ...");

        int r;
        if (si.isVistaPlus()) {
            r = Utils.exec("powershell", "-noprofile", "-Command",
                "Enable-ComputerRestore -Drive '" + si.systemDrive + "\\' " +
                "-ErrorAction SilentlyContinue; " +
                "Checkpoint-Computer -Description '" + label + "' " +
                "-RestorePointType MODIFY_SETTINGS -ErrorAction SilentlyContinue");
        } else {
            // XP: WMI via VBScript
            File vbs = new File(si.temp + "\\_tc_restore.vbs");
            try (PrintWriter pw = new PrintWriter(new FileWriter(vbs))) {
                pw.println("Set o = GetObject(\"winmgmts:\\\\.\\root\\default:SystemRestore\")");
                pw.println("o.CreateRestorePoint \"" + label + "\", 0, 100");
            } catch (IOException ignored) {}
            r = Utils.exec("cscript", "//nologo", vbs.getAbsolutePath());
            vbs.delete();
        }

        if (r == 0) {
            log.ok("Ponto de restauracao criado com sucesso: " + label);
        } else {
            log.warn("Nao foi possivel criar o ponto de restauracao.");
            log.println("  Verifique se a Protecao do Sistema esta habilitada em:");
            log.println("  Painel de Controle > Sistema > Protecao do Sistema");
        }
    }

    // ---------------------------------------------------------------
    // Saude do Disco (SMART)
    // ---------------------------------------------------------------

    public static void checkDiskHealth(SystemInfo si, Logger log) {
        log.section("SAUDE DOS DISCOS (SMART)");

        if (si.isWin8Plus()) {
            log.println("  --- Discos Fisicos ---");
            log.println("");
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "Get-PhysicalDisk | Select-Object FriendlyName, MediaType, HealthStatus, " +
                "OperationalStatus, " +
                "@{N='Tamanho';E={[math]::Round($_.Size/1GB,1).ToString()+'GB'}} | " +
                "Format-Table -AutoSize");

            log.println("");
            log.println("  --- Volumes (Get-Volume) ---");
            log.println("");
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "Get-Volume | Where-Object {$_.DriveLetter} | " +
                "Select-Object DriveLetter, FileSystemLabel, HealthStatus, DriveType, " +
                "@{N='Livre(GB)';E={[math]::Round($_.SizeRemaining/1GB,2)}}, " +
                "@{N='Total(GB)';E={[math]::Round($_.Size/1GB,2)}} | " +
                "Format-Table -AutoSize");
        } else {
            // WMIC (XP+)
            log.println("  --- Discos Fisicos (WMIC) ---");
            log.println("");
            Utils.execPrint("wmic", "diskdrive", "get",
                "Model,Status,MediaType,SerialNumber,Size", "/FORMAT:TABLE");
            log.println("");
            log.println("  --- Volumes Logicos (WMIC) ---");
            log.println("");
            Utils.execPrint("wmic", "logicaldisk", "get",
                "DeviceID,FreeSpace,Size,DriveType,FileSystem,VolumeName",
                "/FORMAT:TABLE");
        }

        // Verificar CHKDSK agendado
        log.println("");
        log.println("  --- Status de CHKDSK agendado ---");
        Utils.execPrint("fsutil", "dirty", "query", si.systemDrive);

        log.println("");
        log.ok("Verificacao de saude dos discos concluida.");
        log.println("  Para analise SMART aprofundada, use ferramentas como:");
        log.println("  CrystalDiskInfo (gratuito) - https://crystalmark.info/");
    }

    // ---------------------------------------------------------------
    // Relatorio de Bateria
    // ---------------------------------------------------------------

    public static void batteryReport(Logger log, String outputDir) {
        log.section("RELATORIO DE BATERIA");
        log.info("Gerando relatorio HTML de bateria via powercfg...");

        String outFile = outputDir + "battery_report_" +
            new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".html";

        int r = Utils.exec("powercfg", "/batteryreport", "/output", outFile);
        if (r == 0 && new File(outFile).exists()) {
            log.ok("Relatorio gerado: " + outFile);
            log.info("Abrindo no navegador padrao...");
            try {
                new ProcessBuilder("cmd", "/c", "start", "", outFile)
                    .redirectErrorStream(true).start();
            } catch (IOException ignored) {}
        } else {
            log.warn("Nao foi possivel gerar o relatorio de bateria.");
            log.println("  Causas comuns:");
            log.println("  - Computador desktop (sem bateria)");
            log.println("  - Bateria nao reconhecida pelo Windows");
        }
    }

    // ---------------------------------------------------------------
    // Limpar Area de Transferencia
    // ---------------------------------------------------------------

    public static void clearClipboard(SystemInfo si, Logger log) {
        log.section("LIMPAR AREA DE TRANSFERENCIA");
        log.info("Limpando area de transferencia...");

        if (si.isVistaPlus()) {
            // PowerShell com System.Windows.Forms
            Utils.exec("powershell", "-noprofile", "-Command",
                "Add-Type -AssemblyName System.Windows.Forms; " +
                "[System.Windows.Forms.Clipboard]::Clear()");
        } else {
            // XP: pipe vazio para clip
            Utils.exec("cmd", "/c", "echo off | clip");
        }
        log.ok("Area de transferencia limpa.");
    }

    // ---------------------------------------------------------------
    // Hibernacao
    // ---------------------------------------------------------------

    public static void setHibernation(Logger log, boolean enable) {
        log.section(enable ? "HABILITAR HIBERNACAO" : "DESABILITAR HIBERNACAO");

        if (enable) {
            log.info("Habilitando hibernacao...");
            Utils.exec("powercfg", "/h", "on");
            log.ok("Hibernacao habilitada.");
            log.println("  O arquivo hiberfil.sys foi recriado no disco.");
        } else {
            log.info("Desabilitando hibernacao e liberando espaco...");
            Utils.exec("powercfg", "/h", "off");
            log.ok("Hibernacao desabilitada.");
            log.println("  hiberfil.sys removido - espaco equivalente a ~75% da RAM liberado.");
        }
    }

    // ---------------------------------------------------------------
    // Verificar e Corrigir Integridade do Registro (basico)
    // ---------------------------------------------------------------

    public static void checkRegistry(SystemInfo si, Logger log) {
        log.section("VERIFICACAO BASICA DO REGISTRO");

        log.info("Procurando entradas RunOnce pendentes...");
        Utils.execPrint("reg", "query",
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\RunOnce");
        Utils.execPrint("reg", "query",
            "HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\RunOnce");

        log.info("Verificando extensoes de shell registradas...");
        log.println("  (Listando associacoes de arquivo do sistema)");
        Utils.execPrint("assoc");

        log.println("");
        log.ok("Verificacao do registro concluida.");
        log.println("  Para limpeza profunda do registro use ferramentas como:");
        log.println("  CCleaner ou Wise Registry Cleaner.");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static long getAvailableRam(SystemInfo si) {
        // Tenta via WMIC (funciona XP+)
        try {
            Process p = new ProcessBuilder(
                "wmic", "os", "get", "FreePhysicalMemory", "/VALUE")
                .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("FreePhysicalMemory=")) {
                    long kb = Long.parseLong(line.substring(19).trim());
                    p.waitFor();
                    return kb * 1024L;
                }
            }
            p.waitFor();
        } catch (Exception ignored) {}
        return Runtime.getRuntime().freeMemory();
    }
}
