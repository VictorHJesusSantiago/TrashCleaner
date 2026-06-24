import java.io.*;

/**
 * Otimizacao do sistema Windows: efeitos visuais, plano de energia,
 * memoria, registro, disco e pilha de rede.
 */
public final class Optimizer {

    private Optimizer() {}

    // ---------------------------------------------------------------
    // Sistema
    // ---------------------------------------------------------------

    public static void optimizeSystem(SystemInfo si, Logger log) {
        log.section("OTIMIZACAO DE SISTEMA");

        log.info("Efeitos visuais para desempenho...");
        // Modo 3 = personalizado; desliga animacoes sem alterar aparencia basica
        Utils.reg("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\VisualEffects",
            "VisualFXSetting", "REG_DWORD", "3");
        Utils.reg("HKCU\\Control Panel\\Desktop\\WindowMetrics",
            "MinAnimate", "REG_SZ", "0");
        Utils.reg("HKCU\\Control Panel\\Desktop",
            "MenuShowDelay", "REG_SZ", "0");
        Utils.reg("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Advanced",
            "TaskbarAnimations", "REG_DWORD", "0");
        Utils.reg("HKCU\\Software\\Microsoft\\Windows\\DWM",
            "AnimationsShiftKey", "REG_DWORD", "0");
        log.ok("Efeitos visuais otimizados.");

        log.info("Plano de energia Alto Desempenho...");
        int r = Utils.exec("powercfg", "/setactive",
            "8c5e7fda-e8bf-4a96-9a85-a6e23a8c635c");
        if (r != 0) Utils.exec("powercfg", "/setactive", "SCHEME_MIN");
        log.ok("Plano Alto Desempenho ativado.");

        log.info("Gerenciamento de memoria (kernel na RAM)...");
        Utils.reg(
            "HKLM\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Memory Management",
            "DisablePagingExecutive", "REG_DWORD", "1");
        log.ok("Memoria otimizada.");

        log.info("Prioridade de CPU para processos em primeiro plano...");
        // 26 = 2-quantum boost, prioridade maxima para foreground
        Utils.reg("HKLM\\SYSTEM\\CurrentControlSet\\Control\\PriorityControl",
            "Win32PrioritySeparation", "REG_DWORD", "26");
        log.ok("Prioridade CPU otimizada.");

        log.info("Historicos e MRU do Explorer...");
        Utils.regDelete("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\RunMRU");
        Utils.regDelete("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\TypedPaths");
        Utils.regDelete("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\RecentDocs");
        Utils.regDelete(
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\ComDlg32\\LastVisitedPidlMRU");
        Utils.regDelete(
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\ComDlg32\\OpenSavePidlMRU");
        Utils.wipeDir(new File(si.appData + "\\Microsoft\\Windows\\Recent"));
        log.ok("Historicos MRU limpos.");

        log.info("Logs do Visualizador de Eventos...");
        clearEventLogs(si, log);

        log.info("Disco: Defrag (HDD) ou TRIM (SSD)...");
        optimizeDisk(si, log);

        log.info("Reconstruindo cache de icones...");
        rebuildIconCache(si, log);

        log.ok("Otimizacao de sistema concluida.");
    }

    // ---------------------------------------------------------------
    // Rede
    // ---------------------------------------------------------------

    public static void optimizeNetwork(SystemInfo si, Logger log) {
        log.section("OTIMIZACAO DE REDE");

        log.info("Cache DNS...");
        Utils.exec("ipconfig", "/flushdns");
        log.ok("DNS limpo.");

        log.info("Cache ARP...");
        Utils.exec("arp", "-d", "*");
        log.ok("ARP limpo.");

        log.info("Cache NetBIOS...");
        Utils.exec("nbtstat", "-R");
        Utils.exec("nbtstat", "-RR");
        log.ok("NetBIOS limpo.");

        log.info("Winsock (reset para padrao)...");
        Utils.exec("netsh", "winsock", "reset");
        log.ok("Winsock resetado.");

        log.info("Pilha TCP/IP IPv4...");
        Utils.exec("netsh", "int", "ip", "reset");
        log.ok("TCP/IP IPv4 resetado.");

        log.info("Pilha TCP/IP IPv6...");
        Utils.exec("netsh", "int", "ipv6", "reset");
        log.ok("TCP/IP IPv6 resetado.");

        if (si.isVistaPlus()) {
            log.info("Otimizacoes avancadas TCP (Vista+)...");
            Utils.exec("netsh", "int", "tcp", "set", "global", "autotuninglevel=normal");
            Utils.exec("netsh", "int", "tcp", "set", "global", "ecncapability=enabled");
            Utils.exec("netsh", "int", "tcp", "set", "global", "timestamps=disabled");
            Utils.exec("netsh", "int", "tcp", "set", "global", "rss=enabled");
            log.ok("TCP avancado otimizado.");
        }

        log.info("Cliente DNS local...");
        Utils.reg("HKLM\\SYSTEM\\CurrentControlSet\\Services\\Dnscache\\Parameters",
            "MaxCacheEntryTtlLimit", "REG_DWORD", "86400");
        Utils.reg("HKLM\\SYSTEM\\CurrentControlSet\\Services\\Dnscache\\Parameters",
            "CacheHashTableSize", "REG_DWORD", "256");
        log.ok("DNS client otimizado.");

        log.info("Renovando configuracao DHCP...");
        Utils.exec("ipconfig", "/release");
        Utils.exec("ipconfig", "/renew");
        log.ok("DHCP renovado.");

        log.ok("Otimizacao de rede concluida.");
    }

    // ---------------------------------------------------------------
    // Helpers privados
    // ---------------------------------------------------------------

    private static void optimizeDisk(SystemInfo si, Logger log) {
        boolean ssd = detectSsd(si);
        if (ssd) {
            log.println("    [DISCO] SSD detectado - executando TRIM/Optimize...");
            if (si.isWin10Plus()) {
                Utils.exec("defrag", si.systemDrive, "/O", "/U");
            } else {
                Utils.exec("fsutil", "behavior", "set", "DisableDeleteNotify", "0");
            }
            log.log("SSD otimizado via TRIM.");
        } else {
            log.println("    [DISCO] HDD detectado - desfragmentando...");
            log.println("    (Aguarde - pode levar varios minutos...)");
            Utils.exec("defrag", si.systemDrive, "/U");
            log.log("HDD desfragmentado.");
        }
        log.ok("Disco otimizado.");
    }

    private static boolean detectSsd(SystemInfo si) {
        // Win8+: PowerShell Get-PhysicalDisk e mais confiavel
        if (si.isWin8Plus()) {
            try {
                Process p = new ProcessBuilder(
                    "powershell", "-noprofile", "-Command",
                    "try{(Get-PhysicalDisk|Where{$_.MediaType-eq'SSD'-or$_.BusType-eq'NVMe'}|Measure).Count}catch{0}")
                    .redirectErrorStream(true).start();
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
                String line = br.readLine();
                p.waitFor();
                if (line != null && !line.trim().equals("0")) return true;
            } catch (Exception ignored) {}
        }
        // Fallback: TRIM habilitado geralmente indica SSD
        try {
            Process p = new ProcessBuilder(
                "fsutil", "behavior", "query", "DisableDeleteNotify")
                .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            p.waitFor();
            if (line != null && line.contains("= 0")) return true;
        } catch (Exception ignored) {}
        return false;
    }

    private static void clearEventLogs(SystemInfo si, Logger log) {
        if (si.supportsWevtutil()) {
            try {
                Process p = new ProcessBuilder("wevtutil", "el")
                    .redirectErrorStream(true).start();
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
                String name;
                while ((name = br.readLine()) != null) {
                    name = name.trim();
                    if (!name.isEmpty()) Utils.exec("wevtutil", "cl", name);
                }
                p.waitFor();
                log.log("Logs de eventos limpos (wevtutil).");
            } catch (Exception ignored) {}
        } else {
            // XP: WMI via VBScript
            File vbs = new File(si.temp + "\\_tc_evtlog.vbs");
            try (PrintWriter pw = new PrintWriter(new FileWriter(vbs))) {
                pw.println("Set o = GetObject(\"winmgmts:root\\cimv2\")");
                pw.println("For Each L in o.ExecQuery(\"SELECT * FROM Win32_NTEventLogFile\")");
                pw.println("  L.ClearEventLog()");
                pw.println("Next");
            } catch (IOException ignored) {}
            Utils.exec("cscript", "//nologo", vbs.getAbsolutePath());
            vbs.delete();
            log.log("Logs de eventos limpos (WMI/XP).");
        }
        log.ok("Logs de eventos limpos.");
    }

    private static void rebuildIconCache(SystemInfo si, Logger log) {
        Utils.exec("taskkill", "/f", "/im", "explorer.exe");
        try { Thread.sleep(1200); } catch (InterruptedException ignored) {}

        Utils.wipeDir(new File(si.appData + "\\Microsoft\\Windows\\Recent\\AutomaticDestinations"));
        Utils.wipeDir(new File(si.appData + "\\Microsoft\\Windows\\Recent\\CustomDestinations"));
        new File(si.localAppData + "\\IconCache.db").delete();
        Utils.deleteGlob(new File(si.localAppData + "\\Microsoft\\Windows\\Explorer"), "iconcache_*.db");

        Utils.exec("explorer.exe");
        log.ok("Cache de icones reconstruido.");
    }
}
