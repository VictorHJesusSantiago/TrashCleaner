import java.io.*;
import java.util.*;

/**
 * Ferramentas de seguranca: Firewall, Windows Defender, Credential Manager,
 * usuarios, atualizacoes, historico de BSOD, processos, Secure Boot e drivers.
 */
public final class SecurityTools {

    private SecurityTools() {}

    // ---------------------------------------------------------------
    // Gerenciar Firewall do Windows
    // ---------------------------------------------------------------

    public static void manageFirewall(SystemInfo si, Logger log, Scanner sc) {
        log.section("FIREWALL DO WINDOWS");

        // Status atual
        log.println("  Status atual dos perfis do Firewall:");
        log.println("");
        Utils.execPrint("netsh", "advfirewall", "show", "allprofiles", "state");
        log.println("");

        if (!si.isVistaPlus()) {
            // XP: netsh firewall
            log.println("  Perfil XP:");
            Utils.execPrint("netsh", "firewall", "show", "state");
        }

        log.println("  Opcoes:");
        log.println("  [1] Habilitar Firewall (todos os perfis)");
        log.println("  [2] Desabilitar Firewall (todos os perfis) [PERIGOSO]");
        log.println("  [3] Listar regras de entrada (ALLOW)");
        log.println("  [4] Listar regras de saida (ALLOW)");
        log.println("  [5] Bloquear programa pelo caminho");
        log.println("  [0] Voltar");

        if (Config.silent || sc == null) {
            log.println("  (Modo silencioso - sem alteracoes)");
            return;
        }

        System.out.print("  Escolha: ");
        String choice = sc.nextLine().trim();

        switch (choice) {
            case "1":
                Utils.exec("netsh", "advfirewall", "set", "allprofiles", "state", "on");
                log.ok("Firewall habilitado em todos os perfis.");
                break;
            case "2":
                log.warn("Desabilitar o Firewall expoe o sistema a riscos!");
                System.out.print("  Confirma? [S para Sim]: ");
                if ("S".equalsIgnoreCase(sc.nextLine().trim())) {
                    Utils.exec("netsh", "advfirewall", "set", "allprofiles", "state", "off");
                    log.warn("Firewall DESABILITADO. Habilite novamente apos o uso!");
                }
                break;
            case "3":
                log.println("  Regras de entrada ALLOW:");
                Utils.execPrint("netsh", "advfirewall", "firewall", "show", "rule",
                    "name=all", "dir=in", "action=allow");
                break;
            case "4":
                log.println("  Regras de saida ALLOW:");
                Utils.execPrint("netsh", "advfirewall", "firewall", "show", "rule",
                    "name=all", "dir=out", "action=allow");
                break;
            case "5":
                System.out.print("  Caminho do programa (ex: C:\\app\\app.exe): ");
                String prog = sc.nextLine().trim();
                if (!prog.isEmpty()) {
                    String ruleName = "TC_Block_" + new File(prog).getName();
                    Utils.exec("netsh", "advfirewall", "firewall", "add", "rule",
                        "name=" + ruleName, "dir=in", "action=block", "program=" + prog);
                    Utils.exec("netsh", "advfirewall", "firewall", "add", "rule",
                        "name=" + ruleName, "dir=out", "action=block", "program=" + prog);
                    log.ok("Regras de bloqueio criadas para: " + prog);
                }
                break;
        }
    }

    // ---------------------------------------------------------------
    // Windows Defender - scan rapido
    // ---------------------------------------------------------------

    public static void defenderScan(SystemInfo si, Logger log) {
        log.section("WINDOWS DEFENDER - SCAN RAPIDO");

        if (!si.isWin8Plus()) {
            log.warn("Windows Defender integrado disponivel somente no Windows 8+.");
            log.println("  No Windows 7/Vista, use o Microsoft Security Essentials.");
            return;
        }

        log.info("Atualizando definicoes do Defender antes do scan...");
        Utils.exec("powershell", "-noprofile", "-Command",
            "try { Update-MpSignature -ErrorAction SilentlyContinue } catch {}");
        log.ok("Definicoes atualizadas.");

        log.info("Iniciando scan rapido (Quick Scan)...");
        log.println("  Aguarde — pode demorar varios minutos...");
        log.println("");
        Utils.execPrint("powershell", "-noprofile", "-Command",
            "Start-MpScan -ScanType QuickScan");

        log.info("Verificando ameacas detectadas...");
        Utils.execPrint("powershell", "-noprofile", "-Command",
            "Get-MpThreatDetection | Select-Object ThreatName,ActionSuccess,InitialDetectionTime | Format-Table -AutoSize");

        log.ok("Scan rapido do Defender concluido.");
    }

    // ---------------------------------------------------------------
    // Gerenciar Credential Manager
    // ---------------------------------------------------------------

    public static void manageCredentials(Logger log, Scanner sc) {
        log.section("GERENCIADOR DE CREDENCIAIS");

        log.println("  Credenciais genericas salvas:");
        log.println("");
        Utils.execPrint("cmdkey", "/list");
        log.println("");

        log.println("  Opcoes:");
        log.println("  [1] Remover credencial pelo nome/destino");
        log.println("  [2] Remover TODAS as credenciais");
        log.println("  [0] Voltar");

        if (Config.silent || sc == null) {
            log.println("  (Modo silencioso - sem alteracoes)");
            return;
        }

        System.out.print("  Escolha: ");
        String choice = sc.nextLine().trim();

        if (choice.equals("1")) {
            System.out.print("  Nome/destino da credencial: ");
            String target = sc.nextLine().trim();
            if (!target.isEmpty()) {
                int r = Utils.exec("cmdkey", "/delete:" + target);
                if (r == 0) log.ok("Credencial removida: " + target);
                else log.warn("Credencial nao encontrada: " + target);
            }
        } else if (choice.equals("2")) {
            log.warn("Remover todas as credenciais pode deslogar de servicos!");
            System.out.print("  Confirma? [S para Sim]: ");
            if ("S".equalsIgnoreCase(sc.nextLine().trim())) {
                // Listar e remover cada uma
                try {
                    Process p = new ProcessBuilder("cmdkey", "/list")
                        .redirectErrorStream(true).start();
                    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String line;
                    List<String> targets = new ArrayList<>();
                    while ((line = br.readLine()) != null) {
                        String t = line.trim();
                        if (t.startsWith("Target:") || t.startsWith("Destino:")) {
                            String tname = t.substring(t.indexOf(':') + 1).trim();
                            targets.add(tname);
                        }
                    }
                    p.waitFor();
                    for (String t : targets) {
                        Utils.exec("cmdkey", "/delete:" + t);
                    }
                    log.ok("Todas as credenciais removidas (" + targets.size() + ").");
                } catch (Exception e) {
                    log.warn("Erro: " + e.getMessage());
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Listar usuarios do sistema
    // ---------------------------------------------------------------

    public static void listUsers(Logger log) {
        log.section("USUARIOS DO SISTEMA");

        log.println("  --- Contas de usuario locais ---");
        log.println("");
        Utils.execPrint("net", "user");
        log.println("");

        log.println("  --- Detalhes do usuario atual ---");
        Utils.execPrint("net", "user", System.getProperty("user.name"));
        log.println("");

        // Verificar conta Guest
        log.info("Verificando status da conta Guest...");
        String guestInfo = Utils.execCapture("net", "user", "Guest");
        if (guestInfo != null && guestInfo.contains("Account active") ||
            (guestInfo != null && guestInfo.contains("Conta ativa"))) {
            if (guestInfo.contains("Yes") || guestInfo.contains("Sim")) {
                log.warn("Conta Guest esta ATIVA! Recomendado desabilitar por seguranca.");
                log.println("  Para desabilitar: net user Guest /active:no");
            } else {
                log.ok("Conta Guest esta desabilitada (correto).");
            }
        }

        log.println("");
        log.println("  --- Grupos de administradores ---");
        Utils.execPrint("net", "localgroup", "Administrators");
    }

    // ---------------------------------------------------------------
    // Verificar atualizacoes pendentes
    // ---------------------------------------------------------------

    public static void checkUpdates(SystemInfo si, Logger log) {
        log.section("VERIFICAR ATUALIZACOES DO WINDOWS");

        if (si.isWin10Plus()) {
            log.info("Disparando verificacao de atualizacoes (UsoClient)...");
            Utils.exec("usoclient.exe", "StartScan");
            Utils.sleep(3000);
            log.info("Verificando historico via PowerShell...");
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "Get-HotFix | Sort-Object InstalledOn -Descending | " +
                "Select-Object -First 10 HotFixID,InstalledOn,Description | Format-Table -AutoSize");
        } else if (si.isVistaPlus()) {
            log.info("Disparando verificacao de atualizacoes (Windows Update)...");
            Utils.exec("wuauclt.exe", "/resetauthorization", "/detectnow");
            Utils.sleep(5000);
        } else {
            log.info("Verificando via WMIC (XP)...");
            Utils.execPrint("wmic", "qfe", "list", "brief", "/FORMAT:TABLE");
        }

        log.ok("Verificacao de atualizacoes iniciada.");
        log.println("  Abra Windows Update (Configuracoes > Atualizar e Seguranca) para ver resultados.");
    }

    // ---------------------------------------------------------------
    // Historico de erros criticos (BSOD / crash)
    // ---------------------------------------------------------------

    public static void showBsodHistory(SystemInfo si, Logger log) {
        log.section("HISTORICO DE ERROS CRITICOS (BSOD/CRASH)");

        if (si.supportsWevtutil()) {
            log.println("  --- Eventos criticos do sistema (ultimos 20) ---");
            log.println("");
            // EventID 41 = kernel-power (crash inesperado), 1001 = BugCheck, 6008 = desligamento inesperado
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "Get-EventLog -LogName System -EntryType Error " +
                "-Newest 20 -ErrorAction SilentlyContinue " +
                "| Where-Object {$_.EventID -in @(41,1001,6008,55,129)} " +
                "| Select-Object TimeGenerated,EventID,Source,Message " +
                "| Format-List");

            log.println("");
            log.println("  --- Resumo de eventos criticos (ultimos 30 dias) ---");
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "$start = (Get-Date).AddDays(-30); " +
                "Get-EventLog -LogName System -EntryType Error " +
                "-After $start -ErrorAction SilentlyContinue " +
                "| Where-Object {$_.EventID -in @(41,1001,6008,55,129)} " +
                "| Group-Object EventID | Select-Object Name,Count | Format-Table -AutoSize");
        } else {
            // XP: usar wevtutil se disponivel ou listar via WMI
            Utils.execPrint("wmic", "nteventlog", "where",
                "logfilename='System'", "get",
                "TotalRecords,MaxFileSize,NumberOfRecords");
        }

        // Verificar dumps de memoria recentes
        log.println("");
        log.println("  --- Dumps de memoria recentes ---");
        File minidump = new File(si.systemRoot + "\\Minidump");
        if (minidump.exists()) {
            File[] dumps = minidump.listFiles((d, n) -> n.endsWith(".dmp"));
            if (dumps != null && dumps.length > 0) {
                log.warn("Encontrados " + dumps.length + " dump(s) de memoria em " + minidump.getPath());
                // Mostrar os 5 mais recentes
                Arrays.sort(dumps, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                for (int i = 0; i < Math.min(5, dumps.length); i++) {
                    log.println("    " + dumps[i].getName() +
                        "  (" + Logger.fmt(dumps[i].length()) + ")");
                }
            } else {
                log.ok("Nenhum dump de memoria encontrado (sistema esta estavel).");
            }
        }

        log.ok("Analise de historico de erros concluida.");
    }

    // ---------------------------------------------------------------
    // Listar processos ativos com uso CPU/RAM
    // ---------------------------------------------------------------

    public static void listProcesses(SystemInfo si, Logger log) {
        log.section("PROCESSOS ATIVOS");

        if (si.isVistaPlus()) {
            log.println("  --- Top 20 processos por uso de CPU ---");
            log.println("");
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "Get-Process | Sort-Object CPU -Descending " +
                "| Select-Object -First 20 " +
                "  Name,Id," +
                "  @{N='CPU(s)';E={[math]::Round($_.CPU,1)}}," +
                "  @{N='RAM(MB)';E={[math]::Round($_.WorkingSet64/1MB,1)}}," +
                "  @{N='Threads';E={$_.Threads.Count}} " +
                "| Format-Table -AutoSize");

            log.println("");
            log.println("  --- Top 20 processos por uso de RAM ---");
            log.println("");
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "Get-Process | Sort-Object WorkingSet64 -Descending " +
                "| Select-Object -First 20 " +
                "  Name,Id," +
                "  @{N='RAM(MB)';E={[math]::Round($_.WorkingSet64/1MB,1)}}," +
                "  @{N='CPU(s)';E={[math]::Round($_.CPU,1)}} " +
                "| Format-Table -AutoSize");

            // Detectar processos sem assinatura digital
            log.println("");
            log.println("  --- Processos sem publisher verificado (pode indicar malware) ---");
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "Get-Process | Where-Object {$_.Company -eq $null -or $_.Company -eq ''} " +
                "| Sort-Object Name " +
                "| Select-Object -First 30 Name,Id,Path " +
                "| Format-Table -AutoSize");
        } else {
            Utils.execPrint("tasklist", "/FO", "TABLE");
        }

        log.ok("Listagem de processos concluida.");
    }

    // ---------------------------------------------------------------
    // Verificar integridade de drivers
    // ---------------------------------------------------------------

    public static void checkDrivers(SystemInfo si, Logger log) {
        log.section("INTEGRIDADE DE DRIVERS");

        log.println("  --- Todos os drivers carregados ---");
        Utils.execPrint("driverquery", "/FO", "TABLE", "/SI");
        log.println("");

        if (si.isWin8Plus()) {
            log.println("  --- Drivers nao assinados (potencialmente problematicos) ---");
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "try { " +
                "  Get-WindowsDriver -Online -All " +
                "  | Where-Object {$_.Inbox -eq $false} " +
                "  | Sort-Object Date -Descending " +
                "  | Select-Object Driver,ProviderName,Date,Version " +
                "  | Format-Table -AutoSize " +
                "} catch { Write-Host 'Informacao nao disponivel.' }");
        }

        // Verificar drivers com problemas via Device Manager
        log.println("");
        log.println("  --- Dispositivos com problemas (Device Manager) ---");
        Utils.execPrint("powershell", "-noprofile", "-Command",
            "try { Get-WmiObject Win32_PnPEntity " +
            "| Where-Object {$_.ConfigManagerErrorCode -ne 0} " +
            "| Select-Object Name,DeviceID,ConfigManagerErrorCode " +
            "| Format-Table -AutoSize } catch { Write-Host 'N/A' }");

        log.ok("Verificacao de drivers concluida.");
    }

    // ---------------------------------------------------------------
    // Verificar Secure Boot e TPM
    // ---------------------------------------------------------------

    public static void checkSecureBoot(SystemInfo si, Logger log) {
        log.section("SECURE BOOT E TPM");

        if (si.isWin8Plus()) {
            log.println("  --- Secure Boot ---");
            String sb = Utils.execCapture("powershell", "-noprofile", "-Command",
                "try { Confirm-SecureBootUEFI } catch { 'Nao disponivel (BIOS legado ou sem suporte)' }");
            if (sb != null) {
                String s = sb.trim();
                String status = "True".equalsIgnoreCase(s) ? "HABILITADO (seguro)" :
                    "False".equalsIgnoreCase(s) ? "DESABILITADO" : s;
                log.println("  Status: " + status);
            }
            log.println("");

            log.println("  --- TPM (Trusted Platform Module) ---");
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "try { " +
                "  $t = Get-Tpm; " +
                "  Write-Host ('Presente  : ' + $t.TpmPresent); " +
                "  Write-Host ('Habilitado: ' + $t.TpmEnabled); " +
                "  Write-Host ('Ativado   : ' + $t.TpmActivated); " +
                "  Write-Host ('Versao    : '); " +
                "  Get-WmiObject -Namespace Root\\CIMv2\\Security\\MicrosoftTpm -Class Win32_Tpm " +
                "  | Select-Object SpecVersion | Format-List " +
                "} catch { Write-Host 'TPM nao disponivel ou sem permissao.' }");

            log.println("");
            log.println("  --- Status de BitLocker ---");
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "try { Get-BitLockerVolume | Select-Object MountPoint,EncryptionMethod,VolumeStatus," +
                "ProtectionStatus | Format-Table -AutoSize } catch { Write-Host 'BitLocker N/A' }");
        } else {
            log.warn("Secure Boot e TPM verificaveis somente no Windows 8+.");
        }

        log.ok("Verificacao de Secure Boot/TPM concluida.");
    }
}
