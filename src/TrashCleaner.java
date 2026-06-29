import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/**
 * TrashCleaner v2.0 - Limpador e Otimizador de Windows (XP ao 11)
 *
 * MODO INTERATIVO: java -jar TrashCleaner.jar
 *
 * MODO CLI:
 *   --dry-run           Simular operacoes sem executar nada
 *   --silent            Sem prompts interativos (para scripts/agendamentos)
 *
 * LIMPEZA:
 *   --quick   -q        Limpeza rapida (temp, lixeira, IE, DNS, busca)
 *   --deep    -d        Limpeza profunda (browsers, WinUpdate, logs, dumps, apps)
 *   --apps              Cache de aplicativos (Teams, Discord, Spotify, Slack, etc.)
 *   --shadows-old       Remover shadow copies antigas (manter a mais recente)
 *   --shadows-all       Remover TODAS as shadow copies
 *   --winsxs            Limpeza WinSxS via DISM
 *   --lang-packs        Verificar pacotes de idioma nao usados
 *   --all     -a        deep + sistema + rede + privacidade + performance
 *
 * OTIMIZACAO:
 *   --optimize  -s      Otimizar sistema (efeitos, energia, disco, icones)
 *   --network   -n      Otimizar rede (DNS, Winsock, TCP/IP, DHCP)
 *   --privacy           Privacidade e telemetria
 *   --performance       Tweaks de performance
 *   --tweaks            Privacidade + Performance juntos
 *   --sounds-off        Desabilitar efeitos de som
 *   --pagefile-auto     Configurar pagefile como automatico
 *   --pagefile-custom M,X  Pagefile min M MB, max X MB
 *   --pagefile-off      Desabilitar pagefile (CUIDADO)
 *   --search-rebuild    Reconstruir indice de pesquisa
 *   --write-cache       Otimizar write-caching de disco
 *   --winupdate-repair  Reparar Windows Update (seq. completa)
 *   --dotnet-repair     Verificar/reparar .NET Framework
 *
 * SISTEMA:
 *   --check   -c        SFC + DISM
 *   --info    -i        Informacoes do sistema
 *   --programs          Listar programas instalados
 *   --drivers           Listar drivers instalados
 *   --updates           Listar atualizacoes instaladas
 *   --startup           Listar programas de inicializacao
 *   --services          Listar servicos opcionais
 *   --services-gaming   Aplicar perfil Gaming (desabilitar Xbox, etc.)
 *   --services-min      Aplicar perfil Minimo
 *   --services-restart  Reiniciar servicos criticos
 *   --tasks-list        Listar tarefas agendadas de telemetria
 *   --tasks-disable     Desabilitar todas as tarefas de telemetria
 *   --tasks-enable      Habilitar todas as tarefas de telemetria
 *   --boot-diag         Diagnostico de tempo de boot
 *
 * DISCO:
 *   --disk-health       Saude dos discos (SMART + volumes)
 *   --disk-top [N]      Top N pastas maiores (padrao: 20)
 *   --disk-large [MB]   Arquivos grandes (padrao: 100 MB)
 *   --disk-dupes [path] Localizador de duplicados (padrao: %USERPROFILE%)
 *   --disk-frag [drive] Verificar fragmentacao (padrao: C:)
 *   --chkdsk [drive]    Agendar CHKDSK (padrao: C:)
 *   --shadows-list      Listar shadow copies
 *   --ntfs-compress [path]  Comprimir pasta com NTFS
 *   --backup [src] [dst]    Copiar arquivos de src para dst
 *
 * REDE AVANCADA:
 *   --net-test          Teste de conectividade (ping multi-destino)
 *   --net-connections   Listar conexoes ativas (netstat)
 *   --net-ports         Listar portas abertas
 *   --net-dns-set [provider]  DNS: google, cloudflare, opendns, quad9, auto, ou "1.2.3.4,5.6.7.8"
 *   --net-wifi          Gerenciar perfis Wi-Fi (interativo)
 *   --net-adapters      Gerenciar adaptadores de rede
 *   --net-proxy-reset   Redefinir configuracoes de proxy
 *   --net-trace [host]  Traceroute (padrao: google.com)
 *   --net-info          Informacoes completas de rede
 *
 * SEGURANCA:
 *   --firewall          Gerenciar Firewall do Windows
 *   --defender-scan     Windows Defender scan rapido
 *   --credentials       Gerenciar Credential Manager
 *   --users             Listar usuarios do sistema
 *   --check-updates     Verificar atualizacoes pendentes
 *   --bsod-history      Historico de erros criticos/BSOD
 *   --processes         Listar processos por CPU/RAM
 *   --secure-boot       Verificar Secure Boot e TPM
 *   --driver-check      Verificar integridade de drivers
 *
 * RECUPERACAO:
 *   --restore-list      Listar pontos de restauracao
 *   --restore-apply N   Restaurar para o ponto N
 *   --restore-point     Criar ponto de restauracao
 *   --mbr-repair [mode] Reparar MBR/Boot (all/fixmbr/fixboot/rebuildbcd)
 *   --perms [path]      Reparar permissoes de arquivo
 *   --file-assoc        Reparar associacoes de arquivo
 *   --dotnet-check      Verificar .NET Framework
 *
 * FERRAMENTAS:
 *   --ram               Liberar RAM (flush working sets)
 *   --battery           Relatorio de bateria (HTML)
 *   --clipboard         Limpar area de transferencia
 *   --hibernate-on      Habilitar hibernacao
 *   --hibernate-off     Desabilitar hibernacao
 *   --reg-check         Verificar registro (RunOnce, assoc)
 *
 * RELATORIOS / AUTOMACAO:
 *   --report-html       Gerar relatorio HTML da sessao
 *   --history           Ver historico de sessoes
 *   --schedule-daily [HH:MM] [op]   Agendar limpeza diaria
 *   --schedule-weekly [DIA] [HH:MM] [op]  Agendar limpeza semanal
 *   --schedule-list     Listar agendamentos
 *   --schedule-remove [daily|weekly|all]  Remover agendamentos
 *
 * AJUDA:
 *   --help  -h          Esta mensagem
 */
public final class TrashCleaner {

    static final String VERSION = "2.0";

    public static void main(String[] args) throws Exception {
        if (!System.getProperty("os.name", "").toLowerCase().contains("windows")) {
            System.out.println("[ERRO] TrashCleaner e exclusivo para Windows.");
            System.exit(1);
        }

        // Processar flags globais antes de qualquer outra coisa
        boolean hasDryRun = false, hasSilent = false;
        for (String a : args) {
            if (a.equals("--dry-run"))   { hasDryRun = true;  Config.dryRun  = true; }
            if (a.equals("--silent"))    { hasSilent = true;  Config.silent  = true; }
            if (a.equals("--no-progress")) Config.showProgress = false;
        }

        SystemInfo si  = new SystemInfo();
        Logger     log = buildLogger(si);
        writeHeader(si, log);

        // Registrar espaco e RAM iniciais na sessao
        long initFree = Utils.getDiskFree(si.systemDrive);
        long initRam  = getAvailableRam(si);
        Config.startSession(initFree, initRam);

        if (Config.dryRun) {
            log.warn("MODO SIMULACAO ATIVO — nenhuma operacao sera executada de fato.");
            log.println("");
        }

        // --help nao precisa de admin e nao deve bloquear
        for (String a : args) {
            if (a.equals("--help") || a.equals("-h")) { printHelp(); log.close(); return; }
        }

        if (!si.admin && !Config.dryRun) {
            log.warn("Execute como Administrador para acesso completo!");
            log.println("");
            log.println("  Clique com o botao direito em run.bat");
            log.println("  e selecione 'Executar como administrador'.");
            log.println("");
            if (!Config.silent) {
                log.println("  Pressione ENTER para continuar mesmo assim...");
                System.in.read();
            }
        }

        if (args.length > 0 && !allGlobal(args)) {
            runCLI(args, si, log);
        } else if (!hasNonGlobal(args)) {
            runInteractive(si, log);
        }

        Config.endSession(Utils.getDiskFree(si.systemDrive), getAvailableRam(si));
        log.close();
    }

    // ---------------------------------------------------------------
    // Modo CLI
    // ---------------------------------------------------------------

    private static void runCLI(String[] args, SystemInfo si, Logger log) {
        long before = Utils.getDiskFree(si.systemDrive);
        boolean didWork = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i].toLowerCase();

            // flags globais ja processadas
            if (arg.equals("--dry-run") || arg.equals("--silent") ||
                arg.equals("--no-progress")) continue;

            didWork = true;

            switch (arg) {
                // LIMPEZA
                case "--quick": case "-q":   Cleaner.quickClean(si, log); break;
                case "--deep":  case "-d":   Cleaner.deepClean(si, log);  break;
                case "--apps":               Cleaner.cleanAppCache(si, log); break;
                case "--shadows-old":        Cleaner.cleanShadowCopies(si, log); break;
                case "--shadows-all":        DiskAnalyzer.deleteShadowCopies(si, log, false); break;
                case "--winsxs":             Cleaner.cleanWinSxS(si, log); break;
                case "--lang-packs":         Cleaner.cleanLanguagePacks(si, log); break;
                case "--all": case "-a":
                    Cleaner.deepClean(si, log);
                    Cleaner.cleanAppCache(si, log);
                    Optimizer.optimizeSystem(si, log);
                    Optimizer.optimizeNetwork(si, log);
                    PrivacyOptimizer.applyAll(si, log);
                    break;

                // OTIMIZACAO
                case "--optimize": case "-s":  Optimizer.optimizeSystem(si, log);  break;
                case "--network":  case "-n":  Optimizer.optimizeNetwork(si, log); break;
                case "--privacy":              PrivacyOptimizer.applyPrivacy(si, log); break;
                case "--performance":          PrivacyOptimizer.applyPerformance(si, log); break;
                case "--tweaks":               PrivacyOptimizer.applyAll(si, log); break;
                case "--sounds-off":           Optimizer.disableSystemSounds(log); break;
                case "--search-rebuild":       Optimizer.rebuildSearchIndex(si, log); break;
                case "--write-cache":          Optimizer.optimizeWriteCache(si, log); break;
                case "--winupdate-repair":     Optimizer.repairWindowsUpdate(si, log); break;
                case "--dotnet-repair":        Optimizer.repairDotNet(si, log); break;
                case "--pagefile-auto":
                    Optimizer.configurePagefile(si, log, "auto", 0, 0); break;
                case "--pagefile-off":
                    Optimizer.configurePagefile(si, log, "off", 0, 0); break;
                case "--pagefile-custom":
                    long minMb = 1024, maxMb = 4096;
                    if (i+1 < args.length && args[i+1].contains(",")) {
                        String[] mm = args[++i].split(",");
                        try { minMb = Long.parseLong(mm[0].trim()); } catch (Exception ignored) {}
                        try { maxMb = Long.parseLong(mm[1].trim()); } catch (Exception ignored) {}
                    }
                    Optimizer.configurePagefile(si, log, "custom", minMb, maxMb);
                    break;

                // SISTEMA
                case "--check": case "-c":     SystemChecker.check(si, log); break;
                case "--info":  case "-i":     SystemInfoDisplay.show(si, log); break;
                case "--programs":             SystemInfoDisplay.showPrograms(si, log); break;
                case "--drivers":              SystemInfoDisplay.showDrivers(si, log); break;
                case "--updates":              SystemInfoDisplay.showUpdates(si, log); break;
                case "--startup":              StartupManager.listStartup(si, log); break;
                case "--services":             ServiceManager.listServices(log, ""); break;
                case "--services-gaming":      ServiceManager.applyGamingProfile(log); break;
                case "--services-min":         ServiceManager.applyMinimalProfile(log); break;
                case "--services-restart":     ServiceManager.restartCritical(log); break;
                case "--tasks-list":           ScheduledTaskManager.listTasks(log, "telemetry"); break;
                case "--tasks-disable":        ScheduledTaskManager.disableTelemetryTasks(log); break;
                case "--tasks-enable":         ScheduledTaskManager.enableTelemetryTasks(log); break;
                case "--boot-diag":            ScheduledTaskManager.bootDiagnostic(si, log); break;

                // DISCO
                case "--disk-health":          SystemTools.checkDiskHealth(si, log); break;
                case "--disk-top": {
                    int n = 20;
                    if (i+1 < args.length && args[i+1].matches("\\d+")) n = Integer.parseInt(args[++i]);
                    DiskAnalyzer.topFolders(log, si.systemDrive, n);
                    break;
                }
                case "--disk-large": {
                    long mb = 100;
                    if (i+1 < args.length && args[i+1].matches("\\d+")) mb = Long.parseLong(args[++i]);
                    DiskAnalyzer.findLargeFiles(log, si.systemDrive, mb, 50);
                    break;
                }
                case "--disk-dupes": {
                    String path = si.userProfile;
                    if (i+1 < args.length && !args[i+1].startsWith("-")) path = args[++i];
                    DiskAnalyzer.findDuplicates(log, path);
                    break;
                }
                case "--disk-frag": {
                    String drive = si.systemDrive;
                    if (i+1 < args.length && args[i+1].matches("[A-Za-z]:")) drive = args[++i];
                    DiskAnalyzer.checkFragmentation(si, log, drive);
                    break;
                }
                case "--chkdsk": {
                    String drive = si.systemDrive;
                    if (i+1 < args.length && args[i+1].matches("[A-Za-z]:")) drive = args[++i];
                    DiskAnalyzer.scheduleChkdsk(log, drive);
                    break;
                }
                case "--shadows-list":         DiskAnalyzer.listShadowCopies(si, log); break;
                case "--ntfs-compress": {
                    String path = i+1 < args.length && !args[i+1].startsWith("-") ?
                        args[++i] : si.userProfile;
                    DiskAnalyzer.compressFolder(log, path);
                    break;
                }
                case "--backup": {
                    String src = i+1 < args.length ? args[++i] : si.userProfile;
                    String dst = i+1 < args.length ? args[++i] : getJarDir() + "backup\\";
                    DiskAnalyzer.backupFiles(log, new String[]{src}, dst);
                    break;
                }

                // REDE AVANCADA
                case "--net-test":             NetworkTools.testConnectivity(log); break;
                case "--net-connections":      NetworkTools.listConnections(log); break;
                case "--net-ports":            NetworkTools.listOpenPorts(log); break;
                case "--net-dns-set": {
                    String provider = i+1 < args.length && !args[i+1].startsWith("-") ?
                        args[++i] : "cloudflare";
                    NetworkTools.configureDns(si, log, provider);
                    break;
                }
                case "--net-wifi":             NetworkTools.manageWifiProfiles(log, null); break;
                case "--net-adapters":         NetworkTools.manageAdapters(log, null); break;
                case "--net-proxy-reset":      NetworkTools.resetProxy(log); break;
                case "--net-trace": {
                    String host = i+1 < args.length && !args[i+1].startsWith("-") ?
                        args[++i] : "google.com";
                    NetworkTools.traceroute(log, host);
                    break;
                }
                case "--net-info":             NetworkTools.showNetInfo(log); break;

                // SEGURANCA
                case "--firewall":             SecurityTools.manageFirewall(si, log, null); break;
                case "--defender-scan":        SecurityTools.defenderScan(si, log); break;
                case "--credentials":          SecurityTools.manageCredentials(log, null); break;
                case "--users":                SecurityTools.listUsers(log); break;
                case "--check-updates":        SecurityTools.checkUpdates(si, log); break;
                case "--bsod-history":         SecurityTools.showBsodHistory(si, log); break;
                case "--processes":            SecurityTools.listProcesses(si, log); break;
                case "--secure-boot":          SecurityTools.checkSecureBoot(si, log); break;
                case "--driver-check":         SecurityTools.checkDrivers(si, log); break;

                // RECUPERACAO
                case "--restore-list":         RecoveryTools.listRestorePoints(si, log); break;
                case "--restore-point":        SystemTools.createRestorePoint(si, log); break;
                case "--restore-apply": {
                    int seqNo = 1;
                    if (i+1 < args.length && args[i+1].matches("\\d+"))
                        seqNo = Integer.parseInt(args[++i]);
                    RecoveryTools.applyRestorePoint(si, log, seqNo);
                    break;
                }
                case "--mbr-repair": {
                    String mode = i+1 < args.length && !args[i+1].startsWith("-") ?
                        args[++i] : "all";
                    RecoveryTools.repairBoot(si, log, mode);
                    break;
                }
                case "--perms": {
                    String path = i+1 < args.length && !args[i+1].startsWith("-") ?
                        args[++i] : null;
                    RecoveryTools.repairPermissions(si, log, path);
                    break;
                }
                case "--file-assoc":           RecoveryTools.repairFileAssociations(si, log); break;
                case "--dotnet-check":         RecoveryTools.checkDotNet(si, log); break;

                // FERRAMENTAS
                case "--ram":                  SystemTools.flushRam(si, log); break;
                case "--battery":              SystemTools.batteryReport(log, getJarDir()); break;
                case "--clipboard":            SystemTools.clearClipboard(si, log); break;
                case "--hibernate-on":         SystemTools.setHibernation(log, true); break;
                case "--hibernate-off":        SystemTools.setHibernation(log, false); break;
                case "--reg-check":            SystemTools.checkRegistry(si, log); break;

                // RELATORIOS / AUTOMACAO
                case "--report-html": {
                    Config.endSession(Utils.getDiskFree(si.systemDrive), getAvailableRam(si));
                    String htmlPath = ReportGenerator.generate(si, log, getJarDir());
                    if (htmlPath != null) ReportGenerator.open(htmlPath);
                    break;
                }
                case "--history":
                    SessionHistory.listSessions(log, getJarDir(), null);
                    break;
                case "--schedule-daily": {
                    String time = i+1 < args.length && !args[i+1].startsWith("-") ?
                        args[++i] : "03:00";
                    String ops  = i+1 < args.length && !args[i+1].startsWith("-") ?
                        args[++i] : "--quick";
                    AutoScheduler.scheduleDaily(log, getJarPath(), time, ops);
                    break;
                }
                case "--schedule-weekly": {
                    String day  = i+1 < args.length ? args[++i] : "SUN";
                    String time = i+1 < args.length && !args[i+1].startsWith("-") ?
                        args[++i] : "03:00";
                    String ops  = i+1 < args.length && !args[i+1].startsWith("-") ?
                        args[++i] : "--quick";
                    AutoScheduler.scheduleWeekly(log, getJarPath(), day, time, ops);
                    break;
                }
                case "--schedule-list":        AutoScheduler.listSchedules(log); break;
                case "--schedule-remove": {
                    String type = i+1 < args.length && !args[i+1].startsWith("-") ?
                        args[++i] : "all";
                    AutoScheduler.removeSchedule(log, type);
                    break;
                }

                default:
                    log.warn("Opcao desconhecida: " + arg);
                    printHelp();
                    return;
            }
        }

        if (didWork) {
            long after = Utils.getDiskFree(si.systemDrive);
            log.showFreed(before, after);
            log.println("  Log salvo em: " + log.getLogPath());
        }
    }

    // ---------------------------------------------------------------
    // Modo interativo - menu principal
    // ---------------------------------------------------------------

    private static void runInteractive(SystemInfo si, Logger log) {
        Scanner sc = new Scanner(System.in);
        int sessions = SessionHistory.countSessions(getJarDir());

        while (true) {
            clearScreen();
            printMainMenu(si, sessions);

            System.out.print("  >>> Escolha: ");
            String opt = sc.nextLine().trim().toLowerCase();
            System.out.println();

            long before = Utils.getDiskFree(si.systemDrive);

            switch (opt) {
                // LIMPEZA
                case "1": Cleaner.quickClean(si, log); pressEnter(sc); break;
                case "2": Cleaner.deepClean(si, log);  pressEnter(sc); break;
                case "3": Cleaner.cleanAppCache(si, log); pressEnter(sc); break;

                // OTIMIZACAO
                case "4": Optimizer.optimizeSystem(si, log);   pressEnter(sc); break;
                case "5": Optimizer.optimizeNetwork(si, log);  pressEnter(sc); break;
                case "6": PrivacyOptimizer.applyPrivacy(si, log); pressEnter(sc); break;
                case "7": PrivacyOptimizer.applyPerformance(si, log); pressEnter(sc); break;

                // SISTEMA
                case "8": SystemChecker.check(si, log); pressEnter(sc); break;
                case "9": runTotalClean(si, log, before, sc); continue;

                // FERRAMENTAS E INFORMACOES
                case "i": SystemInfoDisplay.show(si, log); pressEnter(sc); break;
                case "d": runDiskMenu(si, log, sc); continue;
                case "n": runNetworkMenu(si, log, sc); continue;
                case "s": runSecurityMenu(si, log, sc); continue;
                case "g": ServiceManager.manage(log, sc); continue;
                case "t": ScheduledTaskManager.manage(log, sc); continue;
                case "f": runToolsMenu(si, log, sc); continue;
                case "r": runRecoveryMenu(si, log, sc); continue;
                case "o": runOptimizerMenu(si, log, sc); continue;

                // RELATORIOS / MISC
                case "h": {
                    sessions = SessionHistory.countSessions(getJarDir());
                    SessionHistory.listSessions(log, getJarDir(), sc);
                    pressEnter(sc);
                    break;
                }
                case "v": openReport(log); continue;
                case "a": runSchedulerMenu(si, log, sc); continue;
                case "0": printBye(log, sessions); return;

                case "?":
                    showContextHelp();
                    pressEnter(sc);
                    break;

                default:
                    log.warn("Opcao invalida: " + opt + "  (Digite ? para ajuda)");
                    Utils.sleep(700);
                    continue;
            }
        }
    }

    // ---------------------------------------------------------------
    // Submenus
    // ---------------------------------------------------------------

    private static void runDiskMenu(SystemInfo si, Logger log, Scanner sc) {
        while (true) {
            clearScreen();
            System.out.println("  " + rep('=', 65));
            System.out.println("   ANALISADOR DE DISCO");
            System.out.println("  " + rep('=', 65));
            System.out.println("   [1] Top 20 pastas maiores");
            System.out.println("   [2] Arquivos grandes (>= 100 MB)");
            System.out.println("   [3] Arquivos duplicados (por nome+tamanho)");
            System.out.println("   [4] Verificar fragmentacao");
            System.out.println("   [5] Agendar CHKDSK no proximo boot");
            System.out.println("   [6] Listar Shadow Copies (VSS)");
            System.out.println("   [7] Remover Shadow Copies antigas");
            System.out.println("   [8] Compressao NTFS de pasta");
            System.out.println("   [9] Backup de arquivos");
            System.out.println("   [0] Voltar");
            System.out.println("  " + rep('=', 65));
            System.out.print("  >>> ");
            String opt = sc.nextLine().trim();
            System.out.println();
            switch (opt) {
                case "1": DiskAnalyzer.topFolders(log, si.systemDrive, 20); break;
                case "2": DiskAnalyzer.findLargeFiles(log, si.systemDrive, 100, 50); break;
                case "3":
                    System.out.print("  Pasta para buscar duplicados [" + si.userProfile + "]: ");
                    String dp = sc.nextLine().trim();
                    DiskAnalyzer.findDuplicates(log, dp.isEmpty() ? si.userProfile : dp);
                    break;
                case "4":
                    System.out.print("  Drive [" + si.systemDrive + "]: ");
                    String df = sc.nextLine().trim();
                    DiskAnalyzer.checkFragmentation(si, log, df.isEmpty() ? si.systemDrive : df);
                    break;
                case "5":
                    System.out.print("  Drive [" + si.systemDrive + "]: ");
                    String dc = sc.nextLine().trim();
                    DiskAnalyzer.scheduleChkdsk(log, dc.isEmpty() ? si.systemDrive : dc);
                    break;
                case "6": DiskAnalyzer.listShadowCopies(si, log); break;
                case "7": Cleaner.cleanShadowCopies(si, log); break;
                case "8":
                    System.out.print("  Caminho da pasta: ");
                    String cp = sc.nextLine().trim();
                    if (!cp.isEmpty()) DiskAnalyzer.compressFolder(log, cp);
                    break;
                case "9":
                    System.out.print("  Origem (arquivo ou pasta): ");
                    String bsrc = sc.nextLine().trim();
                    System.out.print("  Destino: ");
                    String bdst = sc.nextLine().trim();
                    if (!bsrc.isEmpty() && !bdst.isEmpty())
                        DiskAnalyzer.backupFiles(log, new String[]{bsrc}, bdst);
                    break;
                case "0": return;
                default: log.warn("Opcao invalida."); Utils.sleep(600); continue;
            }
            if (!opt.equals("0")) pressEnter(sc);
        }
    }

    private static void runNetworkMenu(SystemInfo si, Logger log, Scanner sc) {
        while (true) {
            clearScreen();
            System.out.println("  " + rep('=', 65));
            System.out.println("   FERRAMENTAS DE REDE");
            System.out.println("  " + rep('=', 65));
            System.out.println("   [1] Teste de conectividade (ping multi-destino)");
            System.out.println("   [2] Listar conexoes ativas (netstat)");
            System.out.println("   [3] Listar portas abertas");
            System.out.println("   [4] Configurar DNS personalizado");
            System.out.println("   [5] Gerenciar perfis Wi-Fi");
            System.out.println("   [6] Gerenciar adaptadores de rede");
            System.out.println("   [7] Redefinir proxy");
            System.out.println("   [8] Traceroute");
            System.out.println("   [9] Informacoes completas de rede");
            System.out.println("   [0] Voltar");
            System.out.println("  " + rep('=', 65));
            System.out.print("  >>> ");
            String opt = sc.nextLine().trim();
            System.out.println();
            switch (opt) {
                case "1": NetworkTools.testConnectivity(log); break;
                case "2": NetworkTools.listConnections(log); break;
                case "3": NetworkTools.listOpenPorts(log); break;
                case "4":
                    System.out.println("  [1] Google (8.8.8.8)  [2] Cloudflare (1.1.1.1)");
                    System.out.println("  [3] OpenDNS            [4] Quad9 (9.9.9.9)");
                    System.out.println("  [5] Automatico (DHCP)  [6] Personalizado");
                    System.out.print("  Escolha: ");
                    String dc = sc.nextLine().trim();
                    String prov;
                    switch(dc) {
                        case "1": prov="google"; break;
                        case "2": prov="cloudflare"; break;
                        case "3": prov="opendns"; break;
                        case "4": prov="quad9"; break;
                        case "5": prov="auto"; break;
                        default:
                            System.out.print("  DNS primario,secundario: ");
                            prov = sc.nextLine().trim();
                    }
                    NetworkTools.configureDns(si, log, prov.isEmpty() ? "cloudflare" : prov);
                    break;
                case "5": NetworkTools.manageWifiProfiles(log, sc); break;
                case "6": NetworkTools.manageAdapters(log, sc); break;
                case "7": NetworkTools.resetProxy(log); break;
                case "8":
                    System.out.print("  Destino [google.com]: ");
                    String host = sc.nextLine().trim();
                    NetworkTools.traceroute(log, host.isEmpty() ? "google.com" : host);
                    break;
                case "9": NetworkTools.showNetInfo(log); break;
                case "0": return;
                default: log.warn("Opcao invalida."); Utils.sleep(600); continue;
            }
            if (!opt.equals("0")) pressEnter(sc);
        }
    }

    private static void runSecurityMenu(SystemInfo si, Logger log, Scanner sc) {
        while (true) {
            clearScreen();
            System.out.println("  " + rep('=', 65));
            System.out.println("   SEGURANCA");
            System.out.println("  " + rep('=', 65));
            System.out.println("   [1] Gerenciar Firewall do Windows");
            System.out.println("   [2] Windows Defender - Scan Rapido");
            System.out.println("   [3] Gerenciar Credential Manager");
            System.out.println("   [4] Listar usuarios do sistema");
            System.out.println("   [5] Verificar atualizacoes pendentes");
            System.out.println("   [6] Historico de BSOD / erros criticos");
            System.out.println("   [7] Listar processos (CPU / RAM)");
            System.out.println("   [8] Verificar Secure Boot e TPM");
            System.out.println("   [9] Verificar integridade de drivers");
            System.out.println("   [0] Voltar");
            System.out.println("  " + rep('=', 65));
            System.out.print("  >>> ");
            String opt = sc.nextLine().trim();
            System.out.println();
            switch (opt) {
                case "1": SecurityTools.manageFirewall(si, log, sc); break;
                case "2": SecurityTools.defenderScan(si, log); break;
                case "3": SecurityTools.manageCredentials(log, sc); break;
                case "4": SecurityTools.listUsers(log); break;
                case "5": SecurityTools.checkUpdates(si, log); break;
                case "6": SecurityTools.showBsodHistory(si, log); break;
                case "7": SecurityTools.listProcesses(si, log); break;
                case "8": SecurityTools.checkSecureBoot(si, log); break;
                case "9": SecurityTools.checkDrivers(si, log); break;
                case "0": return;
                default: log.warn("Opcao invalida."); Utils.sleep(600); continue;
            }
            if (!opt.equals("0")) pressEnter(sc);
        }
    }

    private static void runRecoveryMenu(SystemInfo si, Logger log, Scanner sc) {
        while (true) {
            clearScreen();
            System.out.println("  " + rep('=', 65));
            System.out.println("   RECUPERACAO E REPARO");
            System.out.println("  " + rep('=', 65));
            System.out.println("   [1] Criar ponto de restauracao");
            System.out.println("   [2] Listar pontos de restauracao");
            System.out.println("   [3] Restaurar para ponto especifico");
            System.out.println("   [4] Reparar MBR / Boot (bootrec)");
            System.out.println("   [5] Reparar permissoes de arquivo");
            System.out.println("   [6] Reparar associacoes de arquivo");
            System.out.println("   [7] Reiniciar servicos criticos");
            System.out.println("   [8] Verificar .NET Framework");
            System.out.println("   [9] Limpeza WinSxS (DISM)");
            System.out.println("   [0] Voltar");
            System.out.println("  " + rep('=', 65));
            System.out.print("  >>> ");
            String opt = sc.nextLine().trim();
            System.out.println();
            switch (opt) {
                case "1": SystemTools.createRestorePoint(si, log); break;
                case "2": RecoveryTools.listRestorePoints(si, log); break;
                case "3":
                    System.out.print("  Numero do ponto de restauracao: ");
                    String rpn = sc.nextLine().trim();
                    try { RecoveryTools.applyRestorePoint(si, log, Integer.parseInt(rpn)); }
                    catch (NumberFormatException e) { log.warn("Numero invalido."); }
                    break;
                case "4":
                    System.out.println("  [1] Reparacao completa  [2] Apenas MBR  [3] Apenas Boot  [4] Apenas BCD");
                    System.out.print("  Escolha: ");
                    String bm = sc.nextLine().trim();
                    String mode;
                    switch (bm) {
                        case "2": mode="fixmbr"; break;
                        case "3": mode="fixboot"; break;
                        case "4": mode="rebuildbcd"; break;
                        default:  mode="all";
                    }
                    RecoveryTools.repairBoot(si, log, mode);
                    break;
                case "5":
                    System.out.print("  Caminho [" + si.systemRoot + "\\System32]: ");
                    String pp = sc.nextLine().trim();
                    RecoveryTools.repairPermissions(si, log, pp.isEmpty() ? null : pp);
                    break;
                case "6": RecoveryTools.repairFileAssociations(si, log); break;
                case "7": ServiceManager.restartCritical(log); break;
                case "8": RecoveryTools.checkDotNet(si, log); break;
                case "9": Cleaner.cleanWinSxS(si, log); break;
                case "0": return;
                default: log.warn("Opcao invalida."); Utils.sleep(600); continue;
            }
            if (!opt.equals("0")) pressEnter(sc);
        }
    }

    private static void runOptimizerMenu(SystemInfo si, Logger log, Scanner sc) {
        while (true) {
            clearScreen();
            System.out.println("  " + rep('=', 65));
            System.out.println("   OTIMIZACAO AVANCADA");
            System.out.println("  " + rep('=', 65));
            System.out.println("   [1] Desabilitar efeitos de som");
            System.out.println("   [2] Configurar Memoria Virtual (pagefile)");
            System.out.println("   [3] Reconstruir indice de pesquisa");
            System.out.println("   [4] Otimizar write-caching de disco");
            System.out.println("   [5] Reparar Windows Update");
            System.out.println("   [6] Verificar/reparar .NET Framework");
            System.out.println("   [7] Desabilitar tarefas de telemetria");
            System.out.println("   [8] Diagnostico de tempo de boot");
            System.out.println("   [0] Voltar");
            System.out.println("  " + rep('=', 65));
            System.out.print("  >>> ");
            String opt = sc.nextLine().trim();
            System.out.println();
            switch (opt) {
                case "1": Optimizer.disableSystemSounds(log); break;
                case "2":
                    System.out.println("  [1] Automatico (recomendado)");
                    System.out.println("  [2] Personalizado");
                    System.out.println("  [3] Desabilitar (CUIDADO)");
                    System.out.print("  Escolha: ");
                    String pf = sc.nextLine().trim();
                    if (pf.equals("1")) {
                        Optimizer.configurePagefile(si, log, "auto", 0, 0);
                    } else if (pf.equals("2")) {
                        System.out.print("  Minimo (MB): "); long mn = parseLong(sc.nextLine());
                        System.out.print("  Maximo (MB): "); long mx = parseLong(sc.nextLine());
                        Optimizer.configurePagefile(si, log, "custom", mn > 0 ? mn : 1024, mx > 0 ? mx : 4096);
                    } else if (pf.equals("3")) {
                        Optimizer.configurePagefile(si, log, "off", 0, 0);
                    }
                    break;
                case "3": Optimizer.rebuildSearchIndex(si, log); break;
                case "4": Optimizer.optimizeWriteCache(si, log); break;
                case "5": Optimizer.repairWindowsUpdate(si, log); break;
                case "6": Optimizer.repairDotNet(si, log); break;
                case "7": ScheduledTaskManager.disableTelemetryTasks(log); break;
                case "8": ScheduledTaskManager.bootDiagnostic(si, log); break;
                case "0": return;
                default: log.warn("Opcao invalida."); Utils.sleep(600); continue;
            }
            if (!opt.equals("0")) pressEnter(sc);
        }
    }

    private static void runToolsMenu(SystemInfo si, Logger log, Scanner sc) {
        while (true) {
            clearScreen();
            System.out.println("  " + rep('=', 65));
            System.out.println("   FERRAMENTAS AVANCADAS");
            System.out.println("  " + rep('=', 65));
            System.out.println("   [1] Gerenciar Inicializacao (startup)");
            System.out.println("   [2] Saude do Disco (SMART)");
            System.out.println("   [3] Liberar RAM");
            System.out.println("   [4] Criar Ponto de Restauracao");
            System.out.println("   [5] Relatorio de Bateria (HTML)");
            System.out.println("   [6] Limpar Area de Transferencia");
            System.out.println("   [7] Verificar Registro");
            System.out.println("   [8] Hibernacao (habilitar/desabilitar)");
            System.out.println("   [9] Informacoes do Sistema (rapido)");
            System.out.println("   [0] Voltar");
            System.out.println("  " + rep('=', 65));
            System.out.print("  >>> ");
            String opt = sc.nextLine().trim();
            System.out.println();
            switch (opt) {
                case "1": StartupManager.manage(si, log, sc); continue;
                case "2": SystemTools.checkDiskHealth(si, log); break;
                case "3": SystemTools.flushRam(si, log); break;
                case "4": SystemTools.createRestorePoint(si, log); break;
                case "5": SystemTools.batteryReport(log, getJarDir()); break;
                case "6": SystemTools.clearClipboard(si, log); break;
                case "7": SystemTools.checkRegistry(si, log); break;
                case "8":
                    System.out.print("  [1] Habilitar  [2] Desabilitar: ");
                    String hib = sc.nextLine().trim();
                    if (hib.equals("1"))      SystemTools.setHibernation(log, true);
                    else if (hib.equals("2")) SystemTools.setHibernation(log, false);
                    break;
                case "9": SystemInfoDisplay.showQuick(si, log); break;
                case "0": return;
                default: log.warn("Opcao invalida."); Utils.sleep(600); continue;
            }
            if (!opt.equals("0")) pressEnter(sc);
        }
    }

    private static void runSchedulerMenu(SystemInfo si, Logger log, Scanner sc) {
        clearScreen();
        System.out.println("  " + rep('=', 65));
        System.out.println("   AGENDAR LIMPEZA AUTOMATICA");
        System.out.println("  " + rep('=', 65));
        System.out.println("   [1] Criar agendamento (assistente)");
        System.out.println("   [2] Listar agendamentos do TrashCleaner");
        System.out.println("   [3] Remover agendamentos");
        System.out.println("   [0] Voltar");
        System.out.println("  " + rep('=', 65));
        System.out.print("  >>> ");
        String opt = sc.nextLine().trim();
        System.out.println();
        switch (opt) {
            case "1": AutoScheduler.scheduleInteractive(log, getJarPath(), sc); break;
            case "2": AutoScheduler.listSchedules(log); break;
            case "3":
                System.out.print("  [1] Diario  [2] Semanal  [3] Todos: ");
                String rm = sc.nextLine().trim();
                String type = rm.equals("1") ? "daily" : rm.equals("2") ? "weekly" : "all";
                AutoScheduler.removeSchedule(log, type);
                break;
        }
        if (!opt.equals("0")) pressEnter(sc);
    }

    // ---------------------------------------------------------------
    // Limpeza Total
    // ---------------------------------------------------------------

    private static void runTotalClean(SystemInfo si, Logger log, long before, Scanner sc) {
        clearScreen();
        log.section("LIMPEZA TOTAL");
        log.println("  Aguarde - esta operacao pode demorar varios minutos...");
        log.println("");

        Cleaner.deepClean(si, log);
        Cleaner.cleanAppCache(si, log);
        Optimizer.optimizeSystem(si, log);
        Optimizer.optimizeNetwork(si, log);
        PrivacyOptimizer.applyAll(si, log);
        ScheduledTaskManager.disableTelemetryTasks(log);

        long after = Utils.getDiskFree(si.systemDrive);
        long ram   = getAvailableRam(si);
        Config.endSession(after, ram);
        log.showComparison(before, after, Config.sessionStartRam, ram);

        // Gerar relatorio HTML automaticamente
        String htmlPath = ReportGenerator.generate(si, log, getJarDir());

        log.println("");
        log.println("  " + rep('=', 60));
        log.println("   LIMPEZA TOTAL CONCLUIDA!");
        log.println("   Log  : " + log.getLogPath());
        if (htmlPath != null) log.println("   HTML : " + htmlPath);
        log.println("  " + rep('=', 60));
        log.println("");

        if (!Config.silent) {
            System.out.print("  Abrir relatorio HTML? [S]: ");
            if ("S".equalsIgnoreCase(sc.nextLine().trim()) && htmlPath != null) {
                ReportGenerator.open(htmlPath);
            }

            System.out.print("  Deseja reiniciar agora? [S para Sim]: ");
            if ("S".equalsIgnoreCase(sc.nextLine().trim())) {
                Utils.exec("shutdown", "/r", "/t", "60",
                    "/c", "TrashCleaner: Reinicio apos otimizacao.");
                log.println("  Reiniciando em 60 segundos. (shutdown /a para cancelar)");
            }
        }
        pressEnter(sc);
    }

    // ---------------------------------------------------------------
    // UI helpers
    // ---------------------------------------------------------------

    private static void printMainMenu(SystemInfo si, int sessions) {
        long free  = Utils.getDiskFree(si.systemDrive);
        long total = Utils.getDiskTotal(si.systemDrive);
        int  pct   = total > 0 ? (int)(((total-free)*100L)/total) : 0;

        System.out.println("  " + rep('=', 67));
        System.out.println("   TRASHCLEANER v" + VERSION +
            " - Limpador e Otimizador de Windows");
        System.out.println("   " + si.winName + "  |  " + si.computerName + " / " + si.userName);
        System.out.println("   Disco " + si.systemDrive + ": " + Logger.fmt(free) +
            " livre de " + Logger.fmt(total) + "  (" + pct + "% usado)");
        System.out.println("  " + rep('=', 67));
        System.out.println();
        System.out.println("  --- LIMPEZA ---");
        System.out.println("   [1] Limpeza Rapida         Temp, Lixeira, IE/Edge, DNS");
        System.out.println("   [2] Limpeza Profunda       +Browsers, WinUpdate, Logs, Dumps");
        System.out.println("   [3] Cache de Aplicativos   Teams, Discord, Spotify, Slack, VS Code");
        System.out.println();
        System.out.println("  --- OTIMIZACAO ---");
        System.out.println("   [4] Otimizar Sistema       Efeitos, Energia, Defrag/TRIM");
        System.out.println("   [5] Otimizar Rede          DNS, ARP, Winsock, TCP/IP");
        System.out.println("   [6] Privacidade            Telemetria, Cortana, Ads");
        System.out.println("   [7] Performance            Game Bar, Transparencia, BG Apps");
        System.out.println("   [O] Otimizacao Avancada    Pagefile, Som, WU Repair, .NET");
        System.out.println();
        System.out.println("  --- SISTEMA ---");
        System.out.println("   [8] Verificar Integridade  SFC + DISM");
        System.out.println("   [9] LIMPEZA TOTAL          Tudo acima de uma vez");
        System.out.println("   [I] Informacoes do Sistema CPU, GPU, RAM, Disco, BIOS");
        System.out.println();
        System.out.println("  --- FERRAMENTAS ---");
        System.out.println("   [D] Analisador de Disco    Top pastas, Grandes, Duplicados");
        System.out.println("   [N] Rede Avancada          Ping, Netstat, DNS, Wi-Fi, Portas");
        System.out.println("   [S] Seguranca              Firewall, Defender, BSOD, Usuarios");
        System.out.println("   [G] Servicos do Windows    Gerenciar servicos por perfil");
        System.out.println("   [T] Tarefas Agendadas      Desabilitar telemetria, boot diag");
        System.out.println("   [F] Ferramentas Avancadas  RAM, Bateria, Hibernacao, Startup");
        System.out.println("   [R] Recuperacao e Reparo   MBR, Permissoes, Pontos rest.");
        System.out.println();
        System.out.println("  --- RELATORIOS ---");
        System.out.println("   [H] Historico de Sessoes   " + (sessions > 0 ? sessions + " sessao(es) anterior(es)" : "Nenhuma sessao anterior"));
        System.out.println("   [V] Ver Relatorio Atual");
        System.out.println("   [A] Agendar Limpeza Automatica");
        System.out.println();
        System.out.println("   [?] Ajuda contextual   [0] Sair");
        System.out.println("  " + rep('=', 67));
        System.out.println();
    }

    private static void showContextHelp() {
        System.out.println();
        System.out.println("  AJUDA RAPIDA - TrashCleaner v" + VERSION);
        System.out.println();
        System.out.println("  Este programa limpa e otimiza o Windows (XP ao 11).");
        System.out.println("  Todas as operacoes sao reversiveis (exceto Limpeza Total + reiniciar).");
        System.out.println();
        System.out.println("  ORDEM RECOMENDADA:");
        System.out.println("  1. Criar Ponto de Restauracao  [F > 4]");
        System.out.println("  2. Limpeza Profunda            [2]");
        System.out.println("  3. Otimizar Sistema            [4]");
        System.out.println("  4. Privacidade                 [6]");
        System.out.println("  5. Reiniciar o computador");
        System.out.println();
        System.out.println("  Para linha de comando: run.bat --help");
    }

    private static void printHelp() {
        System.out.println();
        System.out.println("  TrashCleaner v" + VERSION + " - Ajuda");
        System.out.println("  Uso: run.bat [--dry-run] [--silent] <opcao> [opcao...]");
        System.out.println();
        System.out.println("  Flags globais:");
        System.out.println("    --dry-run      Simular sem executar");
        System.out.println("    --silent       Sem prompts interativos");
        System.out.println();
        System.out.println("  Limpeza: --quick  --deep  --apps  --shadows-old  --winsxs  --all");
        System.out.println("  Otimizacao: --optimize  --network  --privacy  --performance  --tweaks");
        System.out.println("              --sounds-off  --pagefile-auto  --search-rebuild  --write-cache");
        System.out.println("              --winupdate-repair  --dotnet-repair");
        System.out.println("  Sistema: --check  --info  --programs  --drivers  --updates  --startup");
        System.out.println("           --services  --services-gaming  --services-min  --services-restart");
        System.out.println("           --tasks-list  --tasks-disable  --tasks-enable  --boot-diag");
        System.out.println("  Disco: --disk-health  --disk-top [N]  --disk-large [MB]");
        System.out.println("         --disk-dupes [path]  --disk-frag [drive]  --chkdsk [drive]");
        System.out.println("         --shadows-list  --shadows-all  --ntfs-compress [path]  --backup [src] [dst]");
        System.out.println("  Rede: --net-test  --net-connections  --net-ports  --net-dns-set [prov]");
        System.out.println("        --net-wifi  --net-adapters  --net-proxy-reset  --net-trace [host]  --net-info");
        System.out.println("  Seguranca: --firewall  --defender-scan  --credentials  --users");
        System.out.println("             --check-updates  --bsod-history  --processes  --secure-boot  --driver-check");
        System.out.println("  Recuperacao: --restore-list  --restore-point  --restore-apply N");
        System.out.println("               --mbr-repair [mode]  --perms [path]  --file-assoc  --dotnet-check");
        System.out.println("  Ferramentas: --ram  --battery  --clipboard  --hibernate-on  --hibernate-off  --reg-check");
        System.out.println("  Relatorios: --report-html  --history  --schedule-daily  --schedule-weekly");
        System.out.println("              --schedule-list  --schedule-remove [daily|weekly|all]");
        System.out.println("  Ajuda: --help");
        System.out.println();
        System.out.println("  Exemplos:");
        System.out.println("    run.bat --quick");
        System.out.println("    run.bat --all --report-html");
        System.out.println("    run.bat --deep --privacy --dry-run");
        System.out.println("    run.bat --disk-top 30");
        System.out.println("    run.bat --net-dns-set cloudflare");
        System.out.println("    run.bat --schedule-daily 03:00 --quick");
        System.out.println();
    }

    private static void printBye(Logger log, int sessions) {
        System.out.println();
        System.out.println("  Obrigado por usar o TrashCleaner v" + VERSION + "!");
        System.out.println("  Log: " + log.getLogPath());
        System.out.println("  Sessoes anteriores: " + sessions);
        System.out.println();
    }

    private static void openReport(Logger log) {
        if (new File(log.getLogPath()).exists()) {
            try {
                new ProcessBuilder("notepad.exe", log.getLogPath())
                    .redirectErrorStream(true).start();
            } catch (IOException e) {
                System.out.println("  Nao foi possivel abrir: " + e.getMessage());
            }
        } else {
            System.out.println("  Nenhum relatorio encontrado. Execute uma operacao primeiro.");
        }
    }

    private static void clearScreen() {
        System.out.print("[H[2J");
        System.out.flush();
    }

    private static void pressEnter(Scanner sc) {
        System.out.println();
        System.out.print("  Pressione ENTER para continuar...");
        sc.nextLine();
    }

    // ---------------------------------------------------------------
    // Inicializacao
    // ---------------------------------------------------------------

    private static Logger buildLogger(SystemInfo si) {
        String dt = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new Logger(getJarDir() + "TrashCleaner_" + dt + ".log", si.supportsAnsi());
    }

    private static void writeHeader(SystemInfo si, Logger log) {
        log.log(rep('=', 60));
        log.log(" TRASHCLEANER v" + VERSION);
        log.log(" Sistema   : " + si.winName +
            " (" + si.winMajor + "." + si.winMinor + "." + si.winBuild + ")");
        log.log(" Maquina   : " + si.computerName + " / " + si.userName);
        log.log(" Data      : " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
        log.log(" Dry-run   : " + Config.dryRun);
        log.log(" Admin     : " + si.admin);
        log.log(rep('=', 60));
        log.log("Espaco livre ANTES: " + Logger.fmt(Utils.getDiskFree(si.systemDrive)));
        log.log("");
    }

    static String getJarDir() {
        try {
            String path = TrashCleaner.class
                .getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File f = new File(path);
            String parent = f.isFile() ? f.getParent() : path;
            if (!parent.endsWith(File.separator)) parent += File.separator;
            return parent.replace("/", File.separator).replaceFirst("^\\\\", "");
        } catch (Exception e) {
            return "";
        }
    }

    static String getJarPath() {
        try {
            String path = TrashCleaner.class
                .getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File f = new File(path);
            return f.isFile() ? f.getAbsolutePath() : path;
        } catch (Exception e) {
            return "TrashCleaner.jar";
        }
    }

    private static long getAvailableRam(SystemInfo si) {
        try {
            Process p = new ProcessBuilder(
                "wmic", "os", "get", "FreePhysicalMemory", "/VALUE")
                .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
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

    private static boolean allGlobal(String[] args) {
        for (String a : args) {
            if (!a.equals("--dry-run") && !a.equals("--silent") &&
                !a.equals("--no-progress")) return false;
        }
        return true;
    }

    private static boolean hasNonGlobal(String[] args) {
        for (String a : args) {
            if (!a.equals("--dry-run") && !a.equals("--silent") &&
                !a.equals("--no-progress")) return true;
        }
        return false;
    }

    private static long parseLong(String s) {
        try { return Long.parseLong(s == null ? "" : s.trim()); }
        catch (NumberFormatException e) { return 0L; }
    }

    private static String rep(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }
}
