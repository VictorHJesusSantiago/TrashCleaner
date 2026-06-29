import java.io.*;

/**
 * Otimizacao do sistema Windows: efeitos visuais, plano de energia,
 * memoria, registro, disco, pilha de rede e reparos diversos.
 */
public final class Optimizer {

    private Optimizer() {}

    // ---------------------------------------------------------------
    // Sistema
    // ---------------------------------------------------------------

    public static void optimizeSystem(SystemInfo si, Logger log) {
        log.section("OTIMIZACAO DE SISTEMA");

        log.info("Efeitos visuais para desempenho...");
        log.progress("Efeitos visuais", 10);
        Utils.reg("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\VisualEffects",
            "VisualFXSetting", "REG_DWORD", "3");
        Utils.reg("HKCU\\Control Panel\\Desktop\\WindowMetrics", "MinAnimate", "REG_SZ", "0");
        Utils.reg("HKCU\\Control Panel\\Desktop", "MenuShowDelay", "REG_SZ", "0");
        Utils.reg("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Advanced",
            "TaskbarAnimations", "REG_DWORD", "0");
        Utils.reg("HKCU\\Software\\Microsoft\\Windows\\DWM", "AnimationsShiftKey", "REG_DWORD", "0");
        log.ok("Efeitos visuais otimizados.");

        log.info("Plano de energia Alto Desempenho...");
        log.progress("Plano de energia", 18);
        int r = Utils.exec("powercfg", "/setactive", "8c5e7fda-e8bf-4a96-9a85-a6e23a8c635c");
        if (r != 0) Utils.exec("powercfg", "/setactive", "SCHEME_MIN");
        log.ok("Plano Alto Desempenho ativado.");

        log.info("Gerenciamento de memoria (kernel na RAM)...");
        log.progress("Memoria kernel", 26);
        Utils.reg("HKLM\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Memory Management",
            "DisablePagingExecutive", "REG_DWORD", "1");
        log.ok("Memoria otimizada.");

        log.info("Prioridade de CPU para processos em primeiro plano...");
        log.progress("Prioridade CPU", 34);
        Utils.reg("HKLM\\SYSTEM\\CurrentControlSet\\Control\\PriorityControl",
            "Win32PrioritySeparation", "REG_DWORD", "26");
        log.ok("Prioridade CPU otimizada.");

        log.info("Historicos e MRU do Explorer...");
        log.progress("MRU / historicos", 42);
        Utils.regDelete("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\RunMRU");
        Utils.regDelete("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\TypedPaths");
        Utils.regDelete("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\RecentDocs");
        Utils.regDelete(
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\ComDlg32\\LastVisitedPidlMRU");
        Utils.regDelete(
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\ComDlg32\\OpenSavePidlMRU");
        Utils.wipeDir(new File(si.appData + "\\Microsoft\\Windows\\Recent"));
        log.ok("Historicos MRU limpos.");

        log.info("Desabilitando efeitos de som do Windows...");
        log.progress("Sons do sistema", 50);
        disableSystemSounds(log);

        log.info("Logs do Visualizador de Eventos...");
        log.progress("Logs de eventos", 58);
        clearEventLogs(si, log);

        log.info("Disco: Defrag (HDD) ou TRIM (SSD)...");
        log.progress("Otimizacao de disco", 68);
        optimizeDisk(si, log);

        log.info("Reconstruindo cache de icones...");
        log.progress("Cache de icones", 82);
        rebuildIconCache(si, log);

        log.progressDone();
        log.ok("Otimizacao de sistema concluida.");
    }

    // ---------------------------------------------------------------
    // Rede
    // ---------------------------------------------------------------

    public static void optimizeNetwork(SystemInfo si, Logger log) {
        log.section("OTIMIZACAO DE REDE");

        log.info("Cache DNS...");
        log.progress("DNS", 10);
        Utils.exec("ipconfig", "/flushdns");
        log.ok("DNS limpo.");

        log.info("Cache ARP...");
        log.progress("ARP", 20);
        Utils.exec("arp", "-d", "*");
        log.ok("ARP limpo.");

        log.info("Cache NetBIOS...");
        log.progress("NetBIOS", 30);
        Utils.exec("nbtstat", "-R");
        Utils.exec("nbtstat", "-RR");
        log.ok("NetBIOS limpo.");

        log.info("Winsock (reset para padrao)...");
        log.progress("Winsock", 42);
        Utils.exec("netsh", "winsock", "reset");
        log.ok("Winsock resetado.");

        log.info("Pilha TCP/IP IPv4...");
        log.progress("TCP/IP IPv4", 54);
        Utils.exec("netsh", "int", "ip", "reset");
        log.ok("TCP/IP IPv4 resetado.");

        log.info("Pilha TCP/IP IPv6...");
        log.progress("TCP/IP IPv6", 63);
        Utils.exec("netsh", "int", "ipv6", "reset");
        log.ok("TCP/IP IPv6 resetado.");

        if (si.isVistaPlus()) {
            log.info("Otimizacoes avancadas TCP (Vista+)...");
            log.progress("TCP avancado", 72);
            Utils.exec("netsh", "int", "tcp", "set", "global", "autotuninglevel=normal");
            Utils.exec("netsh", "int", "tcp", "set", "global", "ecncapability=enabled");
            Utils.exec("netsh", "int", "tcp", "set", "global", "timestamps=disabled");
            Utils.exec("netsh", "int", "tcp", "set", "global", "rss=enabled");
            log.ok("TCP avancado otimizado.");
        }

        log.info("Cliente DNS local...");
        log.progress("DNS client", 82);
        Utils.reg("HKLM\\SYSTEM\\CurrentControlSet\\Services\\Dnscache\\Parameters",
            "MaxCacheEntryTtlLimit", "REG_DWORD", "86400");
        Utils.reg("HKLM\\SYSTEM\\CurrentControlSet\\Services\\Dnscache\\Parameters",
            "CacheHashTableSize", "REG_DWORD", "256");
        log.ok("DNS client otimizado.");

        log.info("Redefinindo configuracao de proxy...");
        log.progress("Proxy reset", 88);
        Utils.exec("netsh", "winhttp", "reset", "proxy");
        Utils.reg("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings",
            "ProxyEnable", "REG_DWORD", "0");
        log.ok("Proxy redefinido.");

        log.info("Renovando configuracao DHCP...");
        log.progress("DHCP", 95);
        Utils.exec("ipconfig", "/release");
        Utils.exec("ipconfig", "/renew");
        log.ok("DHCP renovado.");

        log.progressDone();
        log.ok("Otimizacao de rede concluida.");
    }

    // ---------------------------------------------------------------
    // Configurar memoria virtual (pagefile)
    // ---------------------------------------------------------------

    public static void configurePagefile(SystemInfo si, Logger log, String mode, long minMb, long maxMb) {
        log.section("MEMORIA VIRTUAL (PAGEFILE)");

        log.info("Configuracao atual do pagefile...");
        Utils.execPrint("wmic", "pagefile", "list", "full");
        log.println("");

        if (mode.equals("auto")) {
            log.info("Configurando gerenciamento automatico do pagefile...");
            // Habilitar gerenciamento automatico (o mais recomendado)
            File vbs = new File(si.temp + "\\_tc_pagefile.vbs");
            try (PrintWriter pw = new PrintWriter(new FileWriter(vbs))) {
                pw.println("Set svc = GetObject(\"winmgmts:root\\cimv2:Win32_ComputerSystem.Name='\" & CreateObject(\"WScript.Network\").ComputerName & \"'\")" );
                pw.println("svc.AutomaticManagedPagefile = True");
                pw.println("svc.Put_()");
            } catch (IOException ignored) {}
            Utils.exec("cscript", "//nologo", vbs.getAbsolutePath());
            vbs.delete();
            log.ok("Pagefile configurado para gerenciamento automatico pelo Windows.");
        } else if (mode.equals("custom")) {
            log.info("Configurando pagefile personalizado: " + minMb + "MB - " + maxMb + "MB...");
            // PowerShell: desabilitar auto e configurar manualmente
            String script =
                "$cs = Get-WmiObject Win32_ComputerSystem -EnableAllPrivileges; " +
                "$cs.AutomaticManagedPagefile = $false; " +
                "$cs.Put(); " +
                "$pf = Get-WmiObject Win32_PageFileSetting; " +
                "if ($pf -eq $null) { " +
                "  $pf = ([WMIClass]'Win32_PageFileSetting').CreateInstance(); " +
                "  $pf.Name = '" + si.systemDrive + "\\pagefile.sys'; " +
                "}; " +
                "$pf.InitialSize = " + minMb + "; " +
                "$pf.MaximumSize = " + maxMb + "; " +
                "$pf.Put();";
            Utils.exec("powershell", "-noprofile", "-Command", script);
            log.ok("Pagefile configurado: " + minMb + "MB - " + maxMb + "MB em " + si.systemDrive);
        } else if (mode.equals("off")) {
            log.warn("Desabilitar o pagefile pode causar instabilidade em sistemas com pouca RAM!");
            log.info("Desabilitando pagefile...");
            String script =
                "$cs = Get-WmiObject Win32_ComputerSystem -EnableAllPrivileges; " +
                "$cs.AutomaticManagedPagefile = $false; " +
                "$cs.Put(); " +
                "$pf = Get-WmiObject Win32_PageFileSetting; " +
                "if ($pf) { $pf.Delete(); }";
            Utils.exec("powershell", "-noprofile", "-Command", script);
            log.ok("Pagefile desabilitado. Reinicie para aplicar.");
        }

        log.println("  ATENCAO: Reinicie o computador para aplicar as mudancas do pagefile.");
    }

    // ---------------------------------------------------------------
    // Reconstruir indice de pesquisa do Windows
    // ---------------------------------------------------------------

    public static void rebuildSearchIndex(SystemInfo si, Logger log) {
        log.section("RECONSTRUIR INDICE DE PESQUISA");

        log.info("Parando servico Windows Search...");
        Utils.exec("net", "stop", "WSearch");
        Utils.sleep(2000);

        log.info("Removendo banco de dados do indice...");
        File searchData = new File(si.programData + "\\Microsoft\\Search\\Data\\Applications\\Windows");
        if (searchData.exists()) {
            Utils.wipeDir(searchData);
            log.ok("Banco de dados do indice removido.");
        } else {
            log.println("  (Nenhum banco de dados encontrado — pode ja estar limpo)");
        }

        log.info("Reiniciando servico Windows Search (recriara o indice)...");
        Utils.exec("net", "start", "WSearch");

        log.ok("Indice de pesquisa sera reconstruido em segundo plano.");
        log.println("  Isso pode levar varios minutos dependendo do numero de arquivos.");
    }

    // ---------------------------------------------------------------
    // Desabilitar efeitos de som do Windows
    // ---------------------------------------------------------------

    public static void disableSystemSounds(Logger log) {
        log.info("Desabilitando todos os sons do sistema Windows...");
        // Definir esquema de som como 'Sem sons'
        Utils.exec("reg", "add",
            "HKCU\\AppEvents\\Schemes",
            "/ve", "/t", "REG_SZ", "/d", ".None", "/f");
        // Desativar sons de notificacao individuais comuns
        String[] events = {
            "SystemAsterisk", "SystemExclamation", "SystemExit",
            "SystemHand", "SystemNotification", "SystemQuestion",
            "SystemStart", "SystemWelcome", "WindowsLogoff",
            "WindowsLogon", "WindowsUAC", "WindowsUnlock"
        };
        for (String evt : events) {
            Utils.exec("reg", "add",
                "HKCU\\AppEvents\\Schemes\\Apps\\.Default\\" + evt + "\\.Current",
                "/ve", "/t", "REG_SZ", "/d", "", "/f");
        }
        log.ok("Sons do sistema desabilitados.");
    }

    // ---------------------------------------------------------------
    // Otimizacao de write-caching do disco
    // ---------------------------------------------------------------

    public static void optimizeWriteCache(SystemInfo si, Logger log) {
        log.section("WRITE-CACHING DE DISCO");

        if (si.isWin8Plus()) {
            log.info("Habilitando write-caching via PowerShell (Win8+)...");
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "Get-Disk | ForEach-Object { " +
                "  $disk = $_; " +
                "  try { " +
                "    Set-Disk -Number $disk.Number -IsReadOnly $false; " +
                "    Write-Host ('Disco ' + $disk.Number + ': ' + $disk.FriendlyName + ' - OK'); " +
                "  } catch { Write-Host ('Disco ' + $disk.Number + ': nao modificado') } " +
                "}");
        }

        // Habilitar via politica de registro (se aplicavel)
        log.info("Configurando politica de write-cache do sistema...");
        Utils.reg("HKLM\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Memory Management",
            "LargeSystemCache", "REG_DWORD", "0");
        log.ok("Write-caching verificado/otimizado. Reinicie para aplicar.");
    }

    // ---------------------------------------------------------------
    // Reparar Windows Update (sequencia completa)
    // ---------------------------------------------------------------

    public static void repairWindowsUpdate(SystemInfo si, Logger log) {
        log.section("REPARAR WINDOWS UPDATE");

        log.info("Parando servicos do Windows Update...");
        for (String svc : new String[]{"wuauserv", "cryptSvc", "bits", "msiserver"}) {
            Utils.exec("net", "stop", svc);
        }
        Utils.sleep(2000);

        log.info("Renomeando pastas SoftwareDistribution e catroot2...");
        File sd  = new File(si.systemRoot + "\\SoftwareDistribution");
        File cr2 = new File(si.systemRoot + "\\System32\\catroot2");
        File sdBak  = new File(si.systemRoot + "\\SoftwareDistribution.bak");
        File cr2Bak = new File(si.systemRoot + "\\System32\\catroot2.bak");

        // Remover backups anteriores se existirem
        if (sdBak.exists())  { Utils.wipeDir(sdBak);  sdBak.delete(); }
        if (cr2Bak.exists()) { Utils.wipeDir(cr2Bak); cr2Bak.delete(); }

        if (!Config.dryRun) {
            if (sd.exists())  sd.renameTo(sdBak);
            if (cr2.exists()) cr2.renameTo(cr2Bak);
        } else {
            System.out.println("  [SIMULACAO] rename SoftwareDistribution -> SoftwareDistribution.bak");
        }
        log.ok("Pastas renomeadas.");

        log.info("Resetando Winsock e proxies...");
        Utils.exec("netsh", "winsock", "reset");
        Utils.exec("netsh", "winhttp", "reset", "proxy");

        log.info("Re-registrando DLLs do Windows Update...");
        String[] dlls = {
            "atl.dll", "urlmon.dll", "mshtml.dll", "shdocvw.dll", "browseui.dll",
            "jscript.dll", "vbscript.dll", "scrrun.dll", "msxml.dll", "msxml3.dll",
            "msxml6.dll", "actxprxy.dll", "softpub.dll", "wintrust.dll", "dssenh.dll",
            "rsaenh.dll", "cryptdlg.dll", "oleaut32.dll", "ole32.dll", "shell32.dll",
            "wuapi.dll", "wuaueng.dll", "wuaueng1.dll", "wucltui.dll", "wups.dll",
            "wups2.dll", "wuweb.dll", "qmgr.dll", "qmgrprxy.dll", "wucltux.dll",
            "muweb.dll", "wuwebv.dll"
        };
        for (String dll : dlls) {
            Utils.exec("regsvr32", "/s", dll);
        }
        log.ok("DLLs re-registradas.");

        log.info("Reiniciando servicos do Windows Update...");
        for (String svc : new String[]{"bits", "cryptSvc", "msiserver", "wuauserv"}) {
            Utils.exec("net", "start", svc);
        }

        log.info("Forcando deteccao de atualizacoes...");
        Utils.exec("wuauclt.exe", "/resetauthorization", "/detectnow");
        if (si.isWin10Plus()) {
            Utils.exec("usoclient.exe", "StartScan");
        }

        log.ok("Reparacao do Windows Update concluida.");
        log.println("  Abra Windows Update e verifique se as atualizacoes estao disponiveis.");
    }

    // ---------------------------------------------------------------
    // Reparar / habilitar .NET Framework
    // ---------------------------------------------------------------

    public static void repairDotNet(SystemInfo si, Logger log) {
        log.section("VERIFICAR / REPARAR .NET FRAMEWORK");

        log.info("Verificando versoes do .NET Framework instaladas...");
        Utils.execPrint("reg", "query",
            "HKLM\\SOFTWARE\\Microsoft\\NET Framework Setup\\NDP", "/s", "/v", "Version");

        if (si.isWin8Plus()) {
            log.info("Habilitando .NET Framework 3.5 (inclui 2.0 e 3.0) via DISM...");
            int r = Utils.exec("dism", "/online",
                "/enable-feature", "/featurename:NetFx3",
                "/all", "/norestart");
            if (r == 0) {
                log.ok(".NET Framework 3.5 habilitado.");
            } else {
                log.warn(".NET 3.5 nao foi habilitado (pode ja estar ativo ou sem acesso a Internet).");
            }

            log.info("Verificando .NET Framework 4.x...");
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "[System.Runtime.InteropServices.RuntimeEnvironment]::GetRuntimeDirectory()");
        } else {
            log.info("Para instalar/reparar .NET no XP/Vista/7:");
            log.println("  Acesse: https://dotnet.microsoft.com/download/dotnet-framework");
        }

        log.ok("Verificacao do .NET Framework concluida.");
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

    static boolean detectSsd(SystemInfo si) {
        if (si.isWin8Plus()) {
            try {
                Process p = new ProcessBuilder(
                    "powershell", "-noprofile", "-Command",
                    "try{(Get-PhysicalDisk|Where{$_.MediaType-eq'SSD'-or$_.BusType-eq'NVMe'}|Measure).Count}catch{0}")
                    .redirectErrorStream(true).start();
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = br.readLine();
                p.waitFor();
                if (line != null && !line.trim().equals("0")) return true;
            } catch (Exception ignored) {}
        }
        try {
            Process p = new ProcessBuilder(
                "fsutil", "behavior", "query", "DisableDeleteNotify")
                .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            p.waitFor();
            if (line != null && line.contains("= 0")) return true;
        } catch (Exception ignored) {}
        return false;
    }

    static void clearEventLogs(SystemInfo si, Logger log) {
        if (si.supportsWevtutil()) {
            try {
                Process p = new ProcessBuilder("wevtutil", "el")
                    .redirectErrorStream(true).start();
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String name;
                while ((name = br.readLine()) != null) {
                    name = name.trim();
                    if (!name.isEmpty()) Utils.exec("wevtutil", "cl", name);
                }
                p.waitFor();
                log.log("Logs de eventos limpos (wevtutil).");
            } catch (Exception ignored) {}
        } else {
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
        Utils.sleep(1200);

        Utils.wipeDir(new File(si.appData + "\\Microsoft\\Windows\\Recent\\AutomaticDestinations"));
        Utils.wipeDir(new File(si.appData + "\\Microsoft\\Windows\\Recent\\CustomDestinations"));
        new File(si.localAppData + "\\IconCache.db").delete();
        Utils.deleteGlob(new File(si.localAppData + "\\Microsoft\\Windows\\Explorer"), "iconcache_*.db");

        Utils.exec("explorer.exe");
        log.ok("Cache de icones reconstruido.");
    }
}
