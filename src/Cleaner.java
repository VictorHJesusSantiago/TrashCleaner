import java.io.*;

/**
 * Modulo de limpeza: remove arquivos temporarios, caches de browsers,
 * logs, dumps e outros arquivos desnecessarios.
 */
public final class Cleaner {

    private Cleaner() {}

    // ---------------------------------------------------------------
    // Limpeza Rapida
    // ---------------------------------------------------------------

    public static void quickClean(SystemInfo si, Logger log) {
        log.section("LIMPEZA RAPIDA");

        log.info("Pasta Temp do usuario...");
        Utils.wipeDir(new File(si.temp));
        String tmp = System.getenv("TMP");
        if (tmp != null && !tmp.equals(si.temp)) Utils.wipeDir(new File(tmp));
        Utils.wipeDir(new File(si.localAppData + "\\Temp"));
        Utils.wipeDir(new File(si.userProfile  + "\\Local Settings\\Temp"));
        log.ok("Temp do usuario limpo.");

        log.info("Pasta Temp do sistema...");
        Utils.wipeDir(new File(si.systemRoot + "\\Temp"));
        log.ok("Temp do sistema limpo.");

        log.info("Esvaziando Lixeira...");
        emptyRecycleBin(si, log);

        log.info("Cache de Internet Explorer / Edge Legacy...");
        Utils.wipeDir(new File(si.localAppData + "\\Microsoft\\Windows\\INetCache"));
        Utils.wipeDir(new File(si.localAppData + "\\Microsoft\\Windows\\History\\History.IE5"));
        Utils.wipeDir(new File(si.userProfile  + "\\Local Settings\\Temporary Internet Files"));
        Utils.wipeDir(new File(si.localAppData + "\\Microsoft\\Windows\\IECompatCache"));
        log.ok("Cache IE/Edge limpo.");

        log.info("Cache de miniaturas e icones...");
        Utils.deleteGlob(new File(si.localAppData + "\\Microsoft\\Windows\\Explorer"), "thumbcache_*.db");
        Utils.deleteGlob(new File(si.localAppData + "\\Microsoft\\Windows\\Explorer"), "iconcache_*.db");
        log.ok("Cache de miniaturas limpo.");

        log.info("Cache DNS...");
        Utils.exec("ipconfig", "/flushdns");
        log.ok("DNS limpo.");

        log.showFreed(Utils.getDiskFree(si.systemDrive),
                      Utils.getDiskFree(si.systemDrive));
        log.ok("Limpeza Rapida concluida.");
    }

    // ---------------------------------------------------------------
    // Limpeza Profunda  (inclui a rapida internamente)
    // ---------------------------------------------------------------

    public static void deepClean(SystemInfo si, Logger log) {
        log.section("LIMPEZA PROFUNDA");
        long before = Utils.getDiskFree(si.systemDrive);

        // --- nucleo rapido (sem header separado) ---
        log.info("Limpeza basica (temp, lixeira, IE/Edge, DNS)...");
        Utils.wipeDir(new File(si.temp));
        Utils.wipeDir(new File(si.localAppData + "\\Temp"));
        Utils.wipeDir(new File(si.systemRoot   + "\\Temp"));
        emptyRecycleBin(si, log);
        Utils.wipeDir(new File(si.localAppData + "\\Microsoft\\Windows\\INetCache"));
        Utils.wipeDir(new File(si.userProfile  + "\\Local Settings\\Temporary Internet Files"));
        Utils.deleteGlob(new File(si.localAppData + "\\Microsoft\\Windows\\Explorer"), "thumbcache_*.db");
        Utils.exec("ipconfig", "/flushdns");
        log.ok("Limpeza basica concluida.");

        // --- Prefetch ---
        log.info("Prefetch do Windows...");
        Utils.deleteGlob(new File(si.systemRoot + "\\Prefetch"), "*.pf");
        Utils.deleteGlob(new File(si.systemRoot + "\\Prefetch"), "*.db");
        log.ok("Prefetch limpo.");

        // --- Windows Update ---
        log.info("Cache do Windows Update (pausando servico)...");
        Utils.exec("net", "stop", "wuauserv");
        Utils.exec("net", "stop", "bits");
        Utils.exec("net", "stop", "cryptsvc");
        Utils.wipeDir(new File(si.systemRoot + "\\SoftwareDistribution\\Download"));
        Utils.deleteGlob(new File(si.systemRoot + "\\SoftwareDistribution\\DataStore\\Logs"), "*");
        Utils.exec("net", "start", "cryptsvc");
        Utils.exec("net", "start", "bits");
        Utils.exec("net", "start", "wuauserv");
        log.ok("Cache Windows Update limpo.");

        // --- Logs do sistema ---
        log.info("Logs do sistema Windows...");
        Utils.deleteGlob(new File(si.systemRoot + "\\Logs"),         "*.log");
        Utils.deleteGlob(new File(si.systemRoot + "\\Logs"),         "*.etl");
        Utils.deleteGlob(new File(si.systemRoot + "\\Logs\\CBS"),    "*.log");
        Utils.deleteGlob(new File(si.systemRoot + "\\Logs\\CBS"),    "*.cab");
        Utils.deleteGlob(new File(si.systemRoot + "\\inf"),          "*.log");
        Utils.deleteGlob(new File(si.systemRoot + "\\Debug"),        "*.log");
        log.ok("Logs do sistema limpos.");

        // --- WER ---
        log.info("Relatorios de Erro do Windows (WER)...");
        Utils.wipeDir(new File(si.localAppData + "\\Microsoft\\Windows\\WER\\ReportQueue"));
        Utils.wipeDir(new File(si.localAppData + "\\Microsoft\\Windows\\WER\\ReportArchive"));
        Utils.wipeDir(new File(si.programData  + "\\Microsoft\\Windows\\WER\\ReportQueue"));
        Utils.wipeDir(new File(si.programData  + "\\Microsoft\\Windows\\WER\\ReportArchive"));
        log.ok("WER limpo.");

        // --- Crash dumps ---
        log.info("Dumps de memoria (crash dumps)...");
        new File(si.systemRoot + "\\memory.dmp").delete();
        Utils.deleteGlob(new File(si.systemRoot + "\\Minidump"), "*.dmp");
        Utils.wipeDir(new File(si.localAppData + "\\CrashDumps"));
        log.ok("Crash dumps limpos.");

        // --- Browsers ---
        log.info("Cache do Google Chrome...");
        Utils.wipeBrowserProfiles(
            new File(si.localAppData + "\\Google\\Chrome\\User Data"),
            "Cache", "Cache2", "Code Cache", "GPUCache",
            "Media Cache", "ShaderCache",
            "Service Worker\\CacheStorage",
            "Service Worker\\ScriptCache");
        log.ok("Chrome limpo.");

        log.info("Cache do Mozilla Firefox...");
        Utils.wipeBrowserProfiles(
            new File(si.localAppData + "\\Mozilla\\Firefox\\Profiles"),
            "cache2", "thumbnails", "startupCache", "shader-cache");
        Utils.wipeBrowserProfiles(
            new File(si.appData + "\\Mozilla\\Firefox\\Profiles"),
            "cache2", "thumbnails", "startupCache");
        log.ok("Firefox limpo.");

        log.info("Cache do Microsoft Edge (Chromium)...");
        Utils.wipeBrowserProfiles(
            new File(si.localAppData + "\\Microsoft\\Edge\\User Data"),
            "Cache", "Code Cache", "GPUCache", "ShaderCache",
            "Service Worker\\CacheStorage");
        log.ok("Edge limpo.");

        log.info("Cache do Opera e Opera GX...");
        Utils.wipeDir(new File(si.appData      + "\\Opera Software\\Opera Stable\\Cache"));
        Utils.wipeDir(new File(si.appData      + "\\Opera Software\\Opera GX Stable\\Cache"));
        Utils.wipeDir(new File(si.localAppData + "\\Opera Software\\Opera Stable\\Cache"));
        log.ok("Opera limpo.");

        log.info("Cache do Brave Browser...");
        Utils.wipeBrowserProfiles(
            new File(si.localAppData + "\\BraveSoftware\\Brave-Browser\\User Data"),
            "Cache", "Code Cache", "GPUCache", "ShaderCache");
        log.ok("Brave limpo.");

        log.info("Cache do Vivaldi...");
        Utils.wipeBrowserProfiles(
            new File(si.localAppData + "\\Vivaldi\\User Data"),
            "Cache", "Code Cache", "GPUCache");
        log.ok("Vivaldi limpo.");

        // --- Font Cache ---
        log.info("Font Cache do Windows (pausando servico)...");
        Utils.exec("net", "stop", "FontCache");
        Utils.exec("net", "stop", "Windows Font Cache Service");
        Utils.deleteGlob(
            new File(si.systemRoot + "\\ServiceProfiles\\LocalService\\AppData\\Local\\FontCache"), "*");
        new File(si.systemRoot + "\\System32\\FNTCACHE.DAT").delete();
        Utils.exec("net", "start", "FontCache");
        log.ok("Font Cache limpo.");

        // --- DirectX Shader Cache ---
        log.info("DirectX Shader Cache...");
        Utils.wipeDir(new File(si.localAppData + "\\Microsoft\\DirectX Shader Cache"));
        Utils.wipeDir(new File(si.localAppData + "\\D3DSCache"));
        log.ok("Shader Cache limpo.");

        // --- Windows Defender history ---
        log.info("Historico do Windows Defender...");
        Utils.wipeDir(new File(si.programData +
            "\\Microsoft\\Windows Defender\\Scans\\History\\Service\\DetectionHistory"));
        Utils.wipeDir(new File(si.programData +
            "\\Microsoft\\Windows Defender\\Scans\\History\\Results\\Quick"));
        Utils.wipeDir(new File(si.programData +
            "\\Microsoft\\Windows Defender\\Scans\\History\\Results\\Full"));
        log.ok("Defender history limpo.");

        // --- Instaladores antigos ---
        log.info("Instaladores temporarios antigos...");
        Utils.deleteGlob(new File(si.systemRoot), "$NtUninstall*");
        Utils.wipeDir(new File(si.systemRoot + "\\$hf_mig$"));
        log.ok("Instaladores antigos limpos.");

        // --- Limpeza de Disco nativa ---
        log.info("Limpeza de Disco automatica (cleanmgr)...");
        runCleanmgr(si);
        log.ok("Cleanmgr concluido.");

        log.showFreed(before, Utils.getDiskFree(si.systemDrive));
        log.ok("Limpeza Profunda concluida.");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    static void emptyRecycleBin(SystemInfo si, Logger log) {
        if (si.isVistaPlus()) {
            Utils.exec("powershell", "-noprofile", "-Command",
                "Clear-RecycleBin -Force -ErrorAction SilentlyContinue");
        }
        // Fallback direto para todos os drives A-Z
        for (char d = 'A'; d <= 'Z'; d++) {
            deleteRecycleDir(new File(d + ":\\$Recycle.Bin"));  // Vista+
            deleteRecycleDir(new File(d + ":\\RECYCLER"));       // XP
        }
        log.log("Lixeira esvaziada.");
    }

    private static void deleteRecycleDir(File root) {
        if (!root.exists()) return;
        File[] sids = root.listFiles(f -> f.isDirectory());
        if (sids == null) return;
        for (File sid : sids) {
            Utils.wipeDir(sid);
            sid.delete();
        }
    }

    private static void runCleanmgr(SystemInfo si) {
        File exe = new File(si.systemRoot + "\\System32\\cleanmgr.exe");
        if (!exe.exists()) return;

        String base = "HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Explorer\\VolumeCaches";
        String[] cats = {
            "Active Setup Temp Folders", "Content Indexer Cleaner",
            "D3D Shader Cache", "Delivery Optimization Files",
            "Downloaded Program Files", "Internet Cache Files",
            "Memory Dump Files", "Old ChkDsk Files", "Recycle Bin",
            "Setup Log Files", "System error memory dump files",
            "System error minidump files", "Temporary Files",
            "Temporary Setup Files", "Thumbnail Cache", "Update Cleanup",
            "Windows Error Reporting Archive Files",
            "Windows Error Reporting Files",
            "Windows Error Reporting Queue Files",
            "Windows Upgrade Log Files"
        };
        for (String cat : cats) {
            Utils.exec("reg", "add", base + "\\" + cat,
                "/v", "StateFlags0099", "/t", "REG_DWORD", "/d", "2", "/f");
        }
        Utils.exec("cleanmgr", "/sagerun:99");
    }
}
