/**
 * Tweaks de privacidade e performance para Windows 10/11.
 * A maioria das opcoes e compativel com Win7/8 tambem.
 */
public final class PrivacyOptimizer {

    private PrivacyOptimizer() {}

    // ---------------------------------------------------------------
    // Privacidade e telemetria
    // ---------------------------------------------------------------

    public static void applyPrivacy(SystemInfo si, Logger log) {
        log.section("PRIVACIDADE E TELEMETRIA");

        log.info("Desabilitando telemetria do Windows...");
        RegistryUtils.reg("HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows\\DataCollection",
            "AllowTelemetry", "REG_DWORD", "0");
        RegistryUtils.reg("HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Policies\\DataCollection",
            "AllowTelemetry", "REG_DWORD", "0");
        // Parar e desabilitar servico de telemetria
        Utils.exec("sc", "config", "DiagTrack",         "start=", "disabled");
        Utils.exec("sc", "stop",   "DiagTrack");
        Utils.exec("sc", "config", "dmwappushservice",  "start=", "disabled");
        Utils.exec("sc", "stop",   "dmwappushservice");
        // Bloquear host de telemetria
        Utils.exec("reg", "add",
            "HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows\\DataCollection",
            "/v", "DisableEnterpriseAuthProxy", "/t", "REG_DWORD", "/d", "1", "/f");
        log.ok("Telemetria desabilitada.");

        log.info("Desabilitando feedback automatico do Windows...");
        RegistryUtils.reg("HKCU\\Software\\Microsoft\\Siuf\\Rules",
            "NumberOfSIUFInPeriod", "REG_DWORD", "0");
        RegistryUtils.reg("HKCU\\Software\\Microsoft\\Siuf\\Rules",
            "PeriodInNanoSeconds", "REG_DWORD", "0");
        RegistryUtils.reg("HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows\\DataCollection",
            "DoNotShowFeedbackNotifications", "REG_DWORD", "1");
        log.ok("Feedback automatico desabilitado.");

        if (si.isWin10Plus()) {
            log.info("Desabilitando ID de Publicidade (AdvertisingInfo)...");
            RegistryUtils.reg("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\AdvertisingInfo",
                "Enabled", "REG_DWORD", "0");
            RegistryUtils.reg("HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows\\AdvertisingInfo",
                "DisabledByGroupPolicy", "REG_DWORD", "1");
            log.ok("ID de Publicidade desabilitado.");

            log.info("Desabilitando localizacao do dispositivo...");
            Utils.exec("reg", "add",
                "HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\CapabilityAccessManager\\ConsentStore\\location",
                "/v", "Value", "/t", "REG_SZ", "/d", "Deny", "/f");
            RegistryUtils.reg("HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows\\LocationAndSensors",
                "DisableLocation", "REG_DWORD", "1");
            log.ok("Localizacao desabilitada.");

            log.info("Desabilitando Cortana e pesquisa Bing...");
            RegistryUtils.reg("HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows\\Windows Search",
                "AllowCortana", "REG_DWORD", "0");
            RegistryUtils.reg("HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows\\Windows Search",
                "DisableWebSearch", "REG_DWORD", "1");
            RegistryUtils.reg("HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows\\Windows Search",
                "ConnectedSearchUseWeb", "REG_DWORD", "0");
            RegistryUtils.reg("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Search",
                "BingSearchEnabled", "REG_DWORD", "0");
            RegistryUtils.reg("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Search",
                "CortanaConsent", "REG_DWORD", "0");
            log.ok("Cortana e pesquisa Bing desabilitados.");

            log.info("Desabilitando sugestoes e apps promovidos no menu Iniciar...");
            String cdm = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\ContentDeliveryManager";
            RegistryUtils.reg(cdm, "SystemPaneSuggestionsEnabled",       "REG_DWORD", "0");
            RegistryUtils.reg(cdm, "SubscribedContent-338388Enabled",    "REG_DWORD", "0");
            RegistryUtils.reg(cdm, "SubscribedContent-338389Enabled",    "REG_DWORD", "0");
            RegistryUtils.reg(cdm, "SubscribedContent-353694Enabled",    "REG_DWORD", "0");
            RegistryUtils.reg(cdm, "SubscribedContent-353696Enabled",    "REG_DWORD", "0");
            RegistryUtils.reg(cdm, "RotatingLockScreenEnabled",          "REG_DWORD", "0");
            RegistryUtils.reg(cdm, "SilentInstalledAppsEnabled",         "REG_DWORD", "0");
            RegistryUtils.reg(cdm, "SoftLandingEnabled",                 "REG_DWORD", "0");
            log.ok("Sugestoes e apps promovidos desabilitados.");

            log.info("Desabilitando Otimizacao de Entrega (atualizacoes P2P)...");
            RegistryUtils.reg("HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows\\DeliveryOptimization",
                "DODownloadMode", "REG_DWORD", "0");
            log.ok("Delivery Optimization (P2P) desabilitado.");

            log.info("Desabilitando coleta de dados de diagnostico de apps...");
            RegistryUtils.reg("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Privacy",
                "TailoredExperiencesWithDiagnosticDataEnabled", "REG_DWORD", "0");
            RegistryUtils.reg("HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows\\AppCompat",
                "DisableInventory", "REG_DWORD", "1");
            log.ok("Coleta de diagnostico de apps desabilitada.");

            log.info("Desabilitando acesso de apps a camera, microfone e conta...");
            for (String cap : new String[]{"microphone", "webcam", "userAccountInformation"}) {
                Utils.exec("reg", "add",
                    "HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\CapabilityAccessManager\\ConsentStore\\" + cap,
                    "/v", "Value", "/t", "REG_SZ", "/d", "Deny", "/f");
            }
            log.ok("Acesso de apps a camera/microfone/conta restrito.");
        }

        log.ok("Tweaks de privacidade aplicados.");
        log.println("  ATENCAO: Reinicie para consolidar as mudancas.");
    }

    // ---------------------------------------------------------------
    // Performance
    // ---------------------------------------------------------------

    public static void applyPerformance(SystemInfo si, Logger log) {
        log.section("TWEAKS DE PERFORMANCE");

        log.info("Desabilitando Xbox Game Bar e Game DVR...");
        RegistryUtils.reg("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\GameDVR",
            "AppCaptureEnabled", "REG_DWORD", "0");
        RegistryUtils.reg("HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows\\GameDVR",
            "AllowGameDVR", "REG_DWORD", "0");
        RegistryUtils.reg("HKCU\\System\\GameConfigStore",
            "GameDVR_Enabled", "REG_DWORD", "0");
        RegistryUtils.reg("HKCU\\System\\GameConfigStore",
            "GameDVR_FSEBehaviorMode", "REG_DWORD", "2");
        log.ok("Game Bar e DVR desabilitados.");

        log.info("Desabilitando transparencia de janelas...");
        RegistryUtils.reg("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
            "EnableTransparency", "REG_DWORD", "0");
        log.ok("Transparencia desabilitada.");

        log.info("Desabilitando Autoplay...");
        RegistryUtils.reg("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\AutoplayHandlers",
            "DisableAutoplay", "REG_DWORD", "1");
        RegistryUtils.reg("HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Policies\\Explorer",
            "NoDriveTypeAutoRun", "REG_DWORD", "255");
        log.ok("Autoplay desabilitado.");

        log.info("Otimizando carregamento de DLLs em RAM...");
        RegistryUtils.reg("HKLM\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Memory Management",
            "LargeSystemCache", "REG_DWORD", "0");
        // Aumentar cache de I/O nao paginavel
        RegistryUtils.reg("HKLM\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Memory Management",
            "IoPageLockLimit", "REG_DWORD", "983040");
        log.ok("Carregamento de DLLs otimizado.");

        log.info("Desabilitando indexacao de conteudo em discos...");
        // Nao desabilitar servico (quebraria pesquisa), apenas reduzir impacto
        RegistryUtils.reg("HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows\\Windows Search",
            "PreventIndexingLowDiskSpaceMB", "REG_DWORD", "2000");
        log.ok("Indexacao otimizada.");

        if (si.isWin10Plus()) {
            log.info("Tentando ativar plano Ultimate Performance (Win10 1803+)...");
            // Adicionar o plano se nao existir, depois ativar
            Utils.exec("powercfg", "/duplicatescheme",
                "e9a42b02-d5df-448d-aa00-03f14749eb61");
            int r = Utils.exec("powercfg", "/setactive",
                "e9a42b02-d5df-448d-aa00-03f14749eb61");
            if (r != 0) {
                // Fallback para Alto Desempenho
                Utils.exec("powercfg", "/setactive", "8c5e7fda-e8bf-4a96-9a85-a6e23a8c635c");
                log.ok("Plano Alto Desempenho ativado (Ultimate nao disponivel nesta versao).");
            } else {
                log.ok("Plano Ultimate Performance ativado.");
            }

            log.info("Desabilitando apps em background...");
            RegistryUtils.reg(
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\BackgroundAccessApplications",
                "GlobalUserDisabled", "REG_DWORD", "1");
            log.ok("Apps em background desabilitados.");

            log.info("Desabilitando animacoes de tela de bloqueio e Spotlight...");
            String cdm = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\ContentDeliveryManager";
            RegistryUtils.reg(cdm, "RotatingLockScreenEnabled",    "REG_DWORD", "0");
            RegistryUtils.reg(cdm, "RotatingLockScreenOverlayEnabled", "REG_DWORD", "0");
            log.ok("Animacoes de tela de bloqueio desabilitadas.");

            log.info("Desabilitando notificacoes de sugestoes do Windows...");
            RegistryUtils.reg("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Notifications\\Settings\\Windows.SystemToast.Suggested",
                "Enabled", "REG_DWORD", "0");
            log.ok("Sugestoes de notificacao desabilitadas.");
        }

        log.ok("Tweaks de performance aplicados.");
        log.println("  ATENCAO: Reinicie para aplicar todas as mudancas.");
    }

    // ---------------------------------------------------------------
    // Tudo junto
    // ---------------------------------------------------------------

    public static void applyAll(SystemInfo si, Logger log) {
        applyPrivacy(si, log);
        applyPerformance(si, log);
        log.ok("Todos os tweaks de privacidade e performance aplicados.");
    }
}
