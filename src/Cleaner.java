import java.io.*;
import java.util.Scanner;

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
        long before = Utils.getDiskFree(si.systemDrive);

        log.info("Pasta Temp do usuario...");
        log.progress("Temp usuario", 10);
        Utils.wipeDir(new File(si.temp));
        String tmp = System.getenv("TMP");
        if (tmp != null && !tmp.equals(si.temp)) Utils.wipeDir(new File(tmp));
        Utils.wipeDir(new File(si.localAppData + "\\Temp"));
        Utils.wipeDir(new File(si.userProfile  + "\\Local Settings\\Temp"));
        log.ok("Temp do usuario limpo.");

        log.info("Pasta Temp do sistema...");
        log.progress("Temp sistema", 25);
        Utils.wipeDir(new File(si.systemRoot + "\\Temp"));
        log.ok("Temp do sistema limpo.");

        log.info("Esvaziando Lixeira...");
        log.progress("Lixeira", 40);
        emptyRecycleBin(si, log);

        log.info("Cache de Internet Explorer / Edge Legacy...");
        log.progress("Cache IE/Edge", 55);
        Utils.wipeDir(new File(si.localAppData + "\\Microsoft\\Windows\\INetCache"));
        Utils.wipeDir(new File(si.localAppData + "\\Microsoft\\Windows\\History\\History.IE5"));
        Utils.wipeDir(new File(si.userProfile  + "\\Local Settings\\Temporary Internet Files"));
        Utils.wipeDir(new File(si.localAppData + "\\Microsoft\\Windows\\IECompatCache"));
        log.ok("Cache IE/Edge limpo.");

        log.info("Cache de miniaturas e icones...");
        log.progress("Miniaturas", 70);
        Utils.deleteGlob(new File(si.localAppData + "\\Microsoft\\Windows\\Explorer"), "thumbcache_*.db");
        Utils.deleteGlob(new File(si.localAppData + "\\Microsoft\\Windows\\Explorer"), "iconcache_*.db");
        log.ok("Cache de miniaturas limpo.");

        log.info("Historico de busca do Windows (WordWheelQuery)...");
        log.progress("Busca Windows", 82);
        Utils.regDelete("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\WordWheelQuery");
        log.ok("Historico de busca limpo.");

        log.info("Cache DNS...");
        log.progress("DNS", 92);
        Utils.exec("ipconfig", "/flushdns");
        log.ok("DNS limpo.");

        log.progressDone();
        log.showFreed(before, Utils.getDiskFree(si.systemDrive));
        log.ok("Limpeza Rapida concluida.");
    }

    // ---------------------------------------------------------------
    // Limpeza Profunda (inclui a rapida internamente)
    // ---------------------------------------------------------------

    public static void deepClean(SystemInfo si, Logger log) {
        log.section("LIMPEZA PROFUNDA");
        long before = Utils.getDiskFree(si.systemDrive);

        log.info("Limpeza basica (temp, lixeira, IE/Edge, DNS)...");
        log.progress("Base", 2);
        Utils.wipeDir(new File(si.temp));
        Utils.wipeDir(new File(si.localAppData + "\\Temp"));
        Utils.wipeDir(new File(si.systemRoot   + "\\Temp"));
        emptyRecycleBin(si, log);
        Utils.wipeDir(new File(si.localAppData + "\\Microsoft\\Windows\\INetCache"));
        Utils.wipeDir(new File(si.userProfile  + "\\Local Settings\\Temporary Internet Files"));
        Utils.deleteGlob(new File(si.localAppData + "\\Microsoft\\Windows\\Explorer"), "thumbcache_*.db");
        Utils.exec("ipconfig", "/flushdns");
        Utils.regDelete("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\WordWheelQuery");
        log.ok("Limpeza basica concluida.");

        log.info("Prefetch do Windows...");
        log.progress("Prefetch", 6);
        Utils.deleteGlob(new File(si.systemRoot + "\\Prefetch"), "*.pf");
        Utils.deleteGlob(new File(si.systemRoot + "\\Prefetch"), "*.db");
        log.ok("Prefetch limpo.");

        log.info("Cache do Windows Update (pausando servico)...");
        log.progress("Windows Update cache", 10);
        Utils.exec("net", "stop", "wuauserv");
        Utils.exec("net", "stop", "bits");
        Utils.exec("net", "stop", "cryptsvc");
        Utils.wipeDir(new File(si.systemRoot + "\\SoftwareDistribution\\Download"));
        Utils.deleteGlob(new File(si.systemRoot + "\\SoftwareDistribution\\DataStore\\Logs"), "*");
        Utils.exec("net", "start", "cryptsvc");
        Utils.exec("net", "start", "bits");
        Utils.exec("net", "start", "wuauserv");
        log.ok("Cache Windows Update limpo.");

        log.info("Logs do sistema Windows...");
        log.progress("Logs sistema", 16);
        Utils.deleteGlob(new File(si.systemRoot + "\\Logs"),         "*.log");
        Utils.deleteGlob(new File(si.systemRoot + "\\Logs"),         "*.etl");
        Utils.deleteGlob(new File(si.systemRoot + "\\Logs\\CBS"),    "*.log");
        Utils.deleteGlob(new File(si.systemRoot + "\\Logs\\CBS"),    "*.cab");
        Utils.deleteGlob(new File(si.systemRoot + "\\inf"),          "*.log");
        Utils.deleteGlob(new File(si.systemRoot + "\\Debug"),        "*.log");
        log.ok("Logs do sistema limpos.");

        log.info("Relatorios de Erro do Windows (WER)...");
        log.progress("WER", 20);
        Utils.wipeDir(new File(si.localAppData + "\\Microsoft\\Windows\\WER\\ReportQueue"));
        Utils.wipeDir(new File(si.localAppData + "\\Microsoft\\Windows\\WER\\ReportArchive"));
        Utils.wipeDir(new File(si.programData  + "\\Microsoft\\Windows\\WER\\ReportQueue"));
        Utils.wipeDir(new File(si.programData  + "\\Microsoft\\Windows\\WER\\ReportArchive"));
        log.ok("WER limpo.");

        log.info("Dumps de memoria (crash dumps)...");
        log.progress("Crash dumps", 24);
        new File(si.systemRoot + "\\memory.dmp").delete();
        Utils.deleteGlob(new File(si.systemRoot + "\\Minidump"), "*.dmp");
        Utils.wipeDir(new File(si.localAppData + "\\CrashDumps"));
        log.ok("Crash dumps limpos.");

        log.info("Cache do Google Chrome...");
        log.progress("Chrome", 28);
        Utils.wipeBrowserProfiles(
            new File(si.localAppData + "\\Google\\Chrome\\User Data"),
            "Cache", "Cache2", "Code Cache", "GPUCache",
            "Media Cache", "ShaderCache",
            "Service Worker\\CacheStorage", "Service Worker\\ScriptCache");
        log.ok("Chrome limpo.");

        log.info("Cache do Mozilla Firefox...");
        log.progress("Firefox", 32);
        Utils.wipeBrowserProfiles(
            new File(si.localAppData + "\\Mozilla\\Firefox\\Profiles"),
            "cache2", "thumbnails", "startupCache", "shader-cache");
        Utils.wipeBrowserProfiles(
            new File(si.appData + "\\Mozilla\\Firefox\\Profiles"),
            "cache2", "thumbnails", "startupCache");
        log.ok("Firefox limpo.");

        log.info("Cache do Microsoft Edge (Chromium)...");
        log.progress("Edge", 36);
        Utils.wipeBrowserProfiles(
            new File(si.localAppData + "\\Microsoft\\Edge\\User Data"),
            "Cache", "Code Cache", "GPUCache", "ShaderCache",
            "Service Worker\\CacheStorage");
        log.ok("Edge limpo.");

        log.info("Cache do Opera e Opera GX...");
        log.progress("Opera", 40);
        Utils.wipeDir(new File(si.appData      + "\\Opera Software\\Opera Stable\\Cache"));
        Utils.wipeDir(new File(si.appData      + "\\Opera Software\\Opera GX Stable\\Cache"));
        Utils.wipeDir(new File(si.localAppData + "\\Opera Software\\Opera Stable\\Cache"));
        log.ok("Opera limpo.");

        log.info("Cache do Brave Browser...");
        log.progress("Brave", 43);
        Utils.wipeBrowserProfiles(
            new File(si.localAppData + "\\BraveSoftware\\Brave-Browser\\User Data"),
            "Cache", "Code Cache", "GPUCache", "ShaderCache");
        log.ok("Brave limpo.");

        log.info("Cache do Vivaldi...");
        log.progress("Vivaldi", 46);
        Utils.wipeBrowserProfiles(
            new File(si.localAppData + "\\Vivaldi\\User Data"),
            "Cache", "Code Cache", "GPUCache");
        log.ok("Vivaldi limpo.");

        log.info("Font Cache do Windows (pausando servico)...");
        log.progress("Font Cache", 50);
        Utils.exec("net", "stop", "FontCache");
        Utils.exec("net", "stop", "Windows Font Cache Service");
        Utils.deleteGlob(
            new File(si.systemRoot + "\\ServiceProfiles\\LocalService\\AppData\\Local\\FontCache"), "*");
        new File(si.systemRoot + "\\System32\\FNTCACHE.DAT").delete();
        Utils.exec("net", "start", "FontCache");
        log.ok("Font Cache limpo.");

        log.info("DirectX Shader Cache...");
        log.progress("DirectX Cache", 54);
        Utils.wipeDir(new File(si.localAppData + "\\Microsoft\\DirectX Shader Cache"));
        Utils.wipeDir(new File(si.localAppData + "\\D3DSCache"));
        log.ok("Shader Cache limpo.");

        log.info("Historico do Windows Defender...");
        log.progress("Defender history", 58);
        Utils.wipeDir(new File(si.programData +
            "\\Microsoft\\Windows Defender\\Scans\\History\\Service\\DetectionHistory"));
        Utils.wipeDir(new File(si.programData +
            "\\Microsoft\\Windows Defender\\Scans\\History\\Results\\Quick"));
        Utils.wipeDir(new File(si.programData +
            "\\Microsoft\\Windows Defender\\Scans\\History\\Results\\Full"));
        log.ok("Defender history limpo.");

        log.info("Jump Lists e Quick Access do Explorer...");
        log.progress("Jump Lists", 61);
        Utils.wipeDir(new File(si.appData + "\\Microsoft\\Windows\\Recent\\AutomaticDestinations"));
        Utils.wipeDir(new File(si.appData + "\\Microsoft\\Windows\\Recent\\CustomDestinations"));
        log.ok("Jump Lists limpos.");

        log.info("Cache do Windows Search Index...");
        log.progress("Search Index cache", 64);
        Utils.exec("net", "stop", "WSearch");
        Utils.wipeDir(new File(si.programData + "\\Microsoft\\Search\\Data\\Applications\\Windows\\GatherLogs"));
        Utils.exec("net", "start", "WSearch");
        log.ok("Cache do Search Index limpo.");

        log.info("Cache do Windows Store...");
        log.progress("Windows Store", 67);
        Utils.exec("wsreset.exe");
        Utils.wipeDir(new File(si.localAppData + "\\Packages\\Microsoft.WindowsStore_8wekyb3d8bbwe\\LocalCache"));
        log.ok("Windows Store limpo.");

        log.info("Cache do Microsoft Teams...");
        log.progress("Teams", 71);
        File teamsDir = new File(si.appData + "\\Microsoft\\Teams");
        if (teamsDir.exists()) {
            for (String sub : new String[]{
                "Cache", "blob_storage", "databases", "GPUCache",
                "IndexedDB", "Local Storage", "tmp", "logs"}) {
                Utils.wipeDir(new File(teamsDir, sub));
            }
        }
        // Teams (novo app MSIX)
        Utils.wipeDir(new File(si.localAppData + "\\Packages\\MSTeams_8wekyb3d8bbwe\\LocalCache"));
        log.ok("Teams limpo.");

        log.info("Cache do OneDrive...");
        log.progress("OneDrive", 74);
        Utils.wipeDir(new File(si.localAppData + "\\Microsoft\\OneDrive\\logs"));
        Utils.wipeDir(new File(si.localAppData + "\\Microsoft\\OneDrive\\Setup\\Logs"));
        Utils.deleteGlob(new File(si.userProfile + "\\OneDrive"), "*.tmp");
        log.ok("OneDrive limpo.");

        log.info("Cache do Discord...");
        log.progress("Discord", 77);
        Utils.wipeDir(new File(si.appData + "\\discord\\Cache"));
        Utils.wipeDir(new File(si.appData + "\\discord\\Code Cache"));
        Utils.wipeDir(new File(si.appData + "\\discord\\GPUCache"));
        Utils.wipeDir(new File(si.appData + "\\discord\\CachedData"));
        log.ok("Discord limpo.");

        log.info("Cache do Spotify...");
        log.progress("Spotify", 80);
        Utils.wipeDir(new File(si.localAppData + "\\Spotify\\Storage"));
        Utils.wipeDir(new File(si.localAppData + "\\Spotify\\Data"));
        Utils.wipeDir(new File(si.appData + "\\Spotify\\Cache"));
        log.ok("Spotify limpo.");

        log.info("Cache do Slack...");
        log.progress("Slack", 83);
        Utils.wipeDir(new File(si.appData + "\\Slack\\Cache"));
        Utils.wipeDir(new File(si.appData + "\\Slack\\Code Cache"));
        Utils.wipeDir(new File(si.appData + "\\Slack\\GPUCache"));
        Utils.wipeDir(new File(si.appData + "\\Slack\\CachedData"));
        log.ok("Slack limpo.");

        log.info("Cache do Visual Studio Code...");
        log.progress("VS Code", 86);
        Utils.wipeDir(new File(si.appData + "\\Code\\Cache"));
        Utils.wipeDir(new File(si.appData + "\\Code\\CachedData"));
        Utils.wipeDir(new File(si.appData + "\\Code\\Code Cache"));
        Utils.wipeDir(new File(si.appData + "\\Code\\GPUCache"));
        Utils.deleteGlob(new File(si.localAppData + "\\Temp"), "*.vscode*");
        log.ok("VS Code limpo.");

        log.info("Cache do Windows Installer (MSI patches)...");
        log.progress("MSI PatchCache", 88);
        Utils.wipeDir(new File(si.systemRoot + "\\Installer\\$PatchCache$"));
        log.ok("MSI PatchCache limpo.");

        log.info("Arquivos temporarios do sistema (.bak, .old, .chk)...");
        log.progress("Arquivos antigos", 90);
        Utils.deleteGlob(new File(si.systemRoot), "*.bak");
        Utils.deleteGlob(new File(si.systemRoot), "*.old");
        Utils.deleteGlob(new File(si.systemRoot), "*.chk");
        Utils.deleteGlob(new File(si.systemRoot + "\\System32"), "*.bak");
        log.ok("Arquivos .bak/.old/.chk limpos.");

        log.info("Instaladores temporarios antigos...");
        log.progress("Instaladores antigos", 92);
        Utils.deleteGlob(new File(si.systemRoot), "$NtUninstall*");
        Utils.wipeDir(new File(si.systemRoot + "\\$hf_mig$"));
        log.ok("Instaladores antigos limpos.");

        log.info("Limpeza de Disco automatica (cleanmgr)...");
        log.progress("Cleanmgr", 95);
        runCleanmgr(si);
        log.ok("Cleanmgr concluido.");

        log.progressDone();
        log.showFreed(before, Utils.getDiskFree(si.systemDrive));
        log.ok("Limpeza Profunda concluida.");
    }

    // ---------------------------------------------------------------
    // Limpeza de cache de aplicativos (submenu dedicado)
    // ---------------------------------------------------------------

    /**
     * Limpa apenas caches de aplicativos de terceiros.
     * Separado da limpeza profunda para uso standalone.
     */
    public static void cleanAppCache(SystemInfo si, Logger log) {
        log.section("CACHE DE APLICATIVOS");
        long before = Utils.getDiskFree(si.systemDrive);
        int step = 0, total = 12;

        log.info("Cache do Microsoft Teams...");
        log.progress("Teams", pct(++step, total));
        File teamsDir = new File(si.appData + "\\Microsoft\\Teams");
        if (teamsDir.exists()) {
            for (String sub : new String[]{
                "Cache", "blob_storage", "databases", "GPUCache",
                "IndexedDB", "Local Storage", "tmp", "logs"}) {
                Utils.wipeDir(new File(teamsDir, sub));
            }
        }
        Utils.wipeDir(new File(si.localAppData + "\\Packages\\MSTeams_8wekyb3d8bbwe\\LocalCache"));
        log.ok("Teams limpo.");

        log.info("Cache do OneDrive...");
        log.progress("OneDrive", pct(++step, total));
        Utils.wipeDir(new File(si.localAppData + "\\Microsoft\\OneDrive\\logs"));
        Utils.wipeDir(new File(si.localAppData + "\\Microsoft\\OneDrive\\Setup\\Logs"));
        log.ok("OneDrive limpo.");

        log.info("Cache do Discord...");
        log.progress("Discord", pct(++step, total));
        Utils.wipeDir(new File(si.appData + "\\discord\\Cache"));
        Utils.wipeDir(new File(si.appData + "\\discord\\Code Cache"));
        Utils.wipeDir(new File(si.appData + "\\discord\\GPUCache"));
        Utils.wipeDir(new File(si.appData + "\\discord\\CachedData"));
        log.ok("Discord limpo.");

        log.info("Cache do Spotify...");
        log.progress("Spotify", pct(++step, total));
        Utils.wipeDir(new File(si.localAppData + "\\Spotify\\Storage"));
        Utils.wipeDir(new File(si.localAppData + "\\Spotify\\Data"));
        Utils.wipeDir(new File(si.appData + "\\Spotify\\Cache"));
        log.ok("Spotify limpo.");

        log.info("Cache do Slack...");
        log.progress("Slack", pct(++step, total));
        Utils.wipeDir(new File(si.appData + "\\Slack\\Cache"));
        Utils.wipeDir(new File(si.appData + "\\Slack\\Code Cache"));
        Utils.wipeDir(new File(si.appData + "\\Slack\\GPUCache"));
        log.ok("Slack limpo.");

        log.info("Cache do Visual Studio Code...");
        log.progress("VS Code", pct(++step, total));
        Utils.wipeDir(new File(si.appData + "\\Code\\Cache"));
        Utils.wipeDir(new File(si.appData + "\\Code\\CachedData"));
        Utils.wipeDir(new File(si.appData + "\\Code\\Code Cache"));
        Utils.wipeDir(new File(si.appData + "\\Code\\GPUCache"));
        log.ok("VS Code limpo.");

        log.info("Cache do Windows Store (wsreset)...");
        log.progress("Windows Store", pct(++step, total));
        Utils.exec("wsreset.exe");
        log.ok("Windows Store limpo.");

        log.info("Cache do Google Chrome...");
        log.progress("Chrome", pct(++step, total));
        Utils.wipeBrowserProfiles(
            new File(si.localAppData + "\\Google\\Chrome\\User Data"),
            "Cache", "Cache2", "Code Cache", "GPUCache", "ShaderCache");
        log.ok("Chrome limpo.");

        log.info("Cache do Mozilla Firefox...");
        log.progress("Firefox", pct(++step, total));
        Utils.wipeBrowserProfiles(
            new File(si.localAppData + "\\Mozilla\\Firefox\\Profiles"),
            "cache2", "thumbnails", "startupCache");
        log.ok("Firefox limpo.");

        log.info("Cache do Brave, Opera, Vivaldi...");
        log.progress("Outros browsers", pct(++step, total));
        Utils.wipeBrowserProfiles(
            new File(si.localAppData + "\\BraveSoftware\\Brave-Browser\\User Data"),
            "Cache", "Code Cache", "GPUCache");
        Utils.wipeDir(new File(si.appData + "\\Opera Software\\Opera Stable\\Cache"));
        Utils.wipeDir(new File(si.appData + "\\Opera Software\\Opera GX Stable\\Cache"));
        Utils.wipeBrowserProfiles(
            new File(si.localAppData + "\\Vivaldi\\User Data"), "Cache", "Code Cache");
        log.ok("Brave/Opera/Vivaldi limpos.");

        log.info("Cache do Steam (arquivos HTML/shader)...");
        log.progress("Steam", pct(++step, total));
        String steamPath = si.localAppData + "\\Steam";
        Utils.wipeDir(new File(steamPath + "\\htmlcache"));
        Utils.deleteGlob(new File(steamPath), "*.log");
        // Tambem procurar em C:\Program Files (x86)\Steam
        Utils.wipeDir(new File("C:\\Program Files (x86)\\Steam\\logs"));
        Utils.wipeDir(new File("C:\\Program Files\\Steam\\logs"));
        log.ok("Steam limpo.");

        log.info("Cache do Zoom...");
        log.progress("Zoom", pct(++step, total));
        Utils.wipeDir(new File(si.appData + "\\Zoom\\logs"));
        Utils.wipeDir(new File(si.appData + "\\Zoom\\data"));
        log.ok("Zoom limpo.");

        log.progressDone();
        log.showFreed(before, Utils.getDiskFree(si.systemDrive));
        log.ok("Limpeza de Cache de Aplicativos concluida.");
    }

    // ---------------------------------------------------------------
    // Limpeza de shadow copies antigas (VSS)
    // ---------------------------------------------------------------

    public static void cleanShadowCopies(SystemInfo si, Logger log) {
        log.section("SHADOW COPIES ANTIGAS (VSS)");

        if (!si.isVistaPlus()) {
            log.warn("VSS shadow copies gerenciaveis somente no Vista+.");
            return;
        }

        log.info("Listando shadow copies existentes...");
        Utils.execPrint("vssadmin", "list", "shadows");
        log.println("");

        log.info("Removendo shadow copies mais antigas (mantendo a mais recente)...");
        // Executar por cada drive
        for (char d = 'A'; d <= 'Z'; d++) {
            File root = new File(d + ":\\");
            if (!root.exists()) continue;
            int r = Utils.exec("vssadmin", "delete", "shadows",
                "/for=" + d + ":", "/oldest", "/quiet");
            if (r == 0) {
                log.ok("Shadow copies antigas de " + d + ": removidas.");
            }
        }
        log.ok("Limpeza de Shadow Copies concluida.");
    }

    // ---------------------------------------------------------------
    // Limpeza do WinSxS (DISM)
    // ---------------------------------------------------------------

    public static void cleanWinSxS(SystemInfo si, Logger log) {
        log.section("LIMPEZA DO WinSxS (DISM)");

        if (!si.supportsDism()) {
            log.warn("DISM nao disponivel nesta versao do Windows.");
            return;
        }

        log.info("Analisando tamanho do WinSxS antes da limpeza...");
        Utils.execPrint("dism", "/online", "/Cleanup-Image", "/AnalyzeComponentStore");

        log.info("Executando StartComponentCleanup...");
        log.println("  (Pode demorar varios minutos. Aguarde...)");
        Utils.execPrint("dism", "/online", "/Cleanup-Image", "/StartComponentCleanup");

        log.info("Removendo backups de service packs (ResetBase)...");
        log.warn("ATENCAO: Apos ResetBase nao sera possivel desinstalar Windows Updates anteriores.");
        Utils.execPrint("dism", "/online", "/Cleanup-Image", "/StartComponentCleanup", "/ResetBase");

        log.ok("Limpeza do WinSxS concluida.");
    }

    // ---------------------------------------------------------------
    // Limpeza de perfis de usuarios inativos
    // ---------------------------------------------------------------

    public static void cleanUserProfiles(SystemInfo si, Logger log, Scanner sc) {
        log.section("PERFIS DE USUARIOS");

        log.info("Listando perfis de usuarios no sistema...");
        log.println("");
        Utils.execPrint("net", "user");
        log.println("");

        // Listar via registro
        String profilesKey = "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileList";
        try {
            Process p = new ProcessBuilder("reg", "query", profilesKey)
                .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("HKEY") && line.contains("S-1-5-21-")) {
                    // Ler ProfileImagePath
                    String path = Utils.regQuery(line, "ProfileImagePath");
                    if (path != null && !path.isEmpty()) {
                        File profileDir = new File(path);
                        long size = Utils.calcDirSize(profileDir);
                        log.println("  " + (++count) + ". " + path +
                            "  [" + Logger.fmt(size) + "]");
                    }
                }
            }
            p.waitFor();
        } catch (Exception ignored) {}

        log.println("");
        log.println("  ATENCAO: A remocao de perfis e IRREVERSIVEL.");
        log.println("  Use 'Painel de Controle > Sistema > Configuracoes Avancadas'");
        log.println("  para remover perfis com seguranca.");
        log.ok("Listagem de perfis concluida. Remocao manual recomendada.");
    }

    // ---------------------------------------------------------------
    // Limpeza de pacotes de idioma
    // ---------------------------------------------------------------

    public static void cleanLanguagePacks(SystemInfo si, Logger log) {
        log.section("PACOTES DE IDIOMA NAO USADOS");

        if (si.isWin8Plus()) {
            log.info("Listando pacotes de idioma instalados...");
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "Get-WindowsPackage -Online | Where-Object {$_.PackageName -like '*Language*'} | " +
                "Select-Object PackageName,PackageState | Format-List");
            log.println("");
            log.println("  Para remover um pacote: Settings > Time & Language > Language");
            log.println("  Ou: dism /Online /Remove-Package /PackageName:<nome>");
        } else if (si.isVistaPlus()) {
            log.info("Listando pacotes de idioma (Vista/7)...");
            Utils.execPrint("dism", "/online", "/get-packages",
                "/format:table");
        } else {
            // XP: lpksetup.exe
            log.info("Para gerenciar pacotes de idioma no XP, use lpksetup.exe.");
            Utils.exec("lpksetup.exe");
        }

        log.ok("Verificacao de pacotes de idioma concluida.");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    static void emptyRecycleBin(SystemInfo si, Logger log) {
        if (si.isVistaPlus()) {
            Utils.exec("powershell", "-noprofile", "-Command",
                "Clear-RecycleBin -Force -ErrorAction SilentlyContinue");
        }
        for (char d = 'A'; d <= 'Z'; d++) {
            deleteRecycleDir(new File(d + ":\\$Recycle.Bin"));
            deleteRecycleDir(new File(d + ":\\RECYCLER"));
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

    private static int pct(int step, int total) {
        return Math.min(99, (step * 100) / total);
    }
}
