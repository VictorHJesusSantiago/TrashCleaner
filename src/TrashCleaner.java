import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/**
 * Ponto de entrada do TrashCleaner.
 *
 * Modo interativo (sem argumentos):
 *   java -jar TrashCleaner.jar
 *   run.bat
 *
 * Modo CLI (com argumentos):
 *   java -jar TrashCleaner.jar --quick
 *   java -jar TrashCleaner.jar --deep --network
 *   run.bat --all
 *   run.bat --help
 */
public final class TrashCleaner {

    static final String VERSION = "1.0";

    public static void main(String[] args) throws Exception {
        // Garante execucao apenas no Windows
        if (!System.getProperty("os.name", "").toLowerCase().contains("windows")) {
            System.out.println("[ERRO] TrashCleaner e exclusivo para Windows.");
            System.exit(1);
        }

        SystemInfo si  = new SystemInfo();
        Logger     log = buildLogger(si);

        writeLogHeader(si, log);

        // Verificar privilegios
        if (!si.admin) {
            log.warn("Execute como Administrador!");
            log.println("");
            log.println("  Opcoes:");
            log.println("  - Clique com o botao direito em run.bat");
            log.println("    e selecione 'Executar como administrador'");
            log.println("  - Ou: run.bat --quick  (passando argumentos)");
            log.println("");
            log.println("  Pressione ENTER para sair...");
            System.in.read();
            log.close();
            System.exit(1);
        }

        if (args.length > 0) {
            runCLI(args, si, log);
        } else {
            runInteractive(si, log);
        }

        log.close();
    }

    // ---------------------------------------------------------------
    // Modo CLI
    // ---------------------------------------------------------------

    private static void runCLI(String[] args, SystemInfo si, Logger log) {
        long before = Utils.getDiskFree(si.systemDrive);
        boolean didWork = false;

        for (String arg : args) {
            switch (arg.toLowerCase()) {
                case "--quick":
                case "-q":
                    Cleaner.quickClean(si, log);
                    didWork = true;
                    break;
                case "--deep":
                case "-d":
                    Cleaner.deepClean(si, log);
                    didWork = true;
                    break;
                case "--optimize":
                case "--system":
                case "-s":
                    Optimizer.optimizeSystem(si, log);
                    didWork = true;
                    break;
                case "--network":
                case "-n":
                    Optimizer.optimizeNetwork(si, log);
                    didWork = true;
                    break;
                case "--check":
                case "-c":
                    SystemChecker.check(si, log);
                    didWork = true;
                    break;
                case "--all":
                case "-a":
                    Cleaner.deepClean(si, log);
                    Optimizer.optimizeSystem(si, log);
                    Optimizer.optimizeNetwork(si, log);
                    didWork = true;
                    break;
                case "--help":
                case "-h":
                    printHelp();
                    return;
                default:
                    log.warn("Opcao desconhecida: " + arg);
                    printHelp();
                    return;
            }
        }

        if (didWork) {
            long after = Utils.getDiskFree(si.systemDrive);
            log.showFreed(before, after);
            log.println("");
            log.println("  Log salvo em: " + log.getLogPath());
        }
    }

    // ---------------------------------------------------------------
    // Modo interativo
    // ---------------------------------------------------------------

    private static void runInteractive(SystemInfo si, Logger log) {
        Scanner sc = new Scanner(System.in);

        while (true) {
            clearScreen();
            printMenu(si);

            System.out.print("  >>> Escolha uma opcao: ");
            String choice = sc.nextLine().trim();
            System.out.println();

            long before = Utils.getDiskFree(si.systemDrive);

            switch (choice) {
                case "1":
                    Cleaner.quickClean(si, log);
                    break;
                case "2":
                    Cleaner.deepClean(si, log);
                    break;
                case "3":
                    Optimizer.optimizeSystem(si, log);
                    log.println("\n  ATENCAO: Reinicie o computador para aplicar todas as mudancas.");
                    break;
                case "4":
                    Optimizer.optimizeNetwork(si, log);
                    log.println("\n  ATENCAO: Reinicie para consolidar as mudancas na pilha de rede.");
                    break;
                case "5":
                    SystemChecker.check(si, log);
                    break;
                case "6":
                    runTotalClean(si, log, before, sc);
                    continue; // Menu ja foi tratado internamente
                case "7":
                    openReport(si, log);
                    continue;
                case "0":
                    printBye(log);
                    return;
                default:
                    log.warn("Opcao invalida. Tente novamente.");
                    pressEnter(sc);
                    continue;
            }

            pressEnter(sc);
        }
    }

    private static void runTotalClean(SystemInfo si, Logger log, long before, Scanner sc) {
        log.section("LIMPEZA TOTAL");
        log.println("  Aguarde - pode demorar varios minutos...");
        log.println("");

        Cleaner.deepClean(si, log);
        Optimizer.optimizeSystem(si, log);
        Optimizer.optimizeNetwork(si, log);

        long after = Utils.getDiskFree(si.systemDrive);
        log.showFreed(before, after);
        log.println("");
        log.println("  ================================================================");
        log.println("   LIMPEZA TOTAL CONCLUIDA!");
        log.println("   Log: " + log.getLogPath());
        log.println("  ================================================================");
        log.println("");

        System.out.print("  Deseja reiniciar o computador agora? (S para Sim): ");
        if (sc.nextLine().trim().equalsIgnoreCase("S")) {
            Utils.exec("shutdown", "/r", "/t", "60",
                "/c", "TrashCleaner: Reinicio para aplicar otimizacoes.");
            log.println("  Reiniciando em 60 segundos... (shutdown /a para cancelar)");
        }
        pressEnter(sc);
    }

    // ---------------------------------------------------------------
    // UI helpers
    // ---------------------------------------------------------------

    private static void printMenu(SystemInfo si) {
        System.out.println();
        System.out.println("  ================================================================");
        System.out.println("   TRASHCLEANER v" + VERSION +
            " - Limpador e Otimizador de Windows");
        System.out.println("   Sistema: " + si.winName +
            "   |   " + si.computerName + " / " + si.userName);
        System.out.println("  ================================================================");
        System.out.println();
        System.out.println("   [1] Limpeza Rapida       Temp, Lixeira, IE/Edge, DNS");
        System.out.println("   [2] Limpeza Profunda     Browsers, WinUpdate, Logs, Dumps");
        System.out.println("   [3] Otimizar Sistema     Defrag/TRIM, Efeitos, Plano Energia");
        System.out.println("   [4] Otimizar Rede        DNS, ARP, Winsock, TCP/IP");
        System.out.println("   [5] Verificar Sistema    SFC + DISM (checar corrupcao)");
        System.out.println("   [6] LIMPEZA TOTAL        Todas as opcoes acima");
        System.out.println("   [7] Ver Relatorio        Abre o log desta sessao");
        System.out.println("   [0] Sair");
        System.out.println();
        System.out.println("  ================================================================");
        System.out.println();
    }

    private static void printHelp() {
        System.out.println();
        System.out.println("  TrashCleaner v" + VERSION + " - Uso em linha de comando");
        System.out.println();
        System.out.println("  Sintaxe:");
        System.out.println("    run.bat [opcao [opcao...]]");
        System.out.println("    java -jar TrashCleaner.jar [opcao [opcao...]]");
        System.out.println();
        System.out.println("  Opcoes:");
        System.out.println("    --quick    -q   Limpeza rapida (temp, lixeira, IE, DNS)");
        System.out.println("    --deep     -d   Limpeza profunda (browsers, WinUpdate, logs)");
        System.out.println("    --optimize -s   Otimizar sistema");
        System.out.println("    --network  -n   Otimizar rede");
        System.out.println("    --check    -c   Verificar integridade (SFC + DISM)");
        System.out.println("    --all      -a   Limpeza profunda + sistema + rede");
        System.out.println("    --help     -h   Mostrar esta ajuda");
        System.out.println();
        System.out.println("  Exemplos:");
        System.out.println("    run.bat --quick");
        System.out.println("    run.bat --all");
        System.out.println("    run.bat --deep --network");
        System.out.println("    java -jar TrashCleaner.jar --quick --optimize");
        System.out.println();
    }

    private static void printBye(Logger log) {
        System.out.println();
        System.out.println("  Obrigado por usar o TrashCleaner v" + VERSION + "!");
        System.out.println("  Log salvo em: " + log.getLogPath());
        System.out.println();
    }

    private static void clearScreen() {
        // ANSI clear; no Windows moderno funciona bem
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void pressEnter(Scanner sc) {
        System.out.println();
        System.out.print("  Pressione ENTER para continuar...");
        sc.nextLine();
    }

    private static void openReport(SystemInfo si, Logger log) {
        File f = new File(log.getLogPath());
        if (f.exists()) {
            Utils.exec("notepad.exe", log.getLogPath());
        } else {
            log.warn("Nenhum relatorio encontrado. Execute uma limpeza primeiro.");
        }
    }

    // ---------------------------------------------------------------
    // Inicializacao do logger
    // ---------------------------------------------------------------

    private static Logger buildLogger(SystemInfo si) {
        String dt  = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String dir = getJarDir();
        return new Logger(dir + "TrashCleaner_" + dt + ".log", si.supportsAnsi());
    }

    private static void writeLogHeader(SystemInfo si, Logger log) {
        log.log("================================================================");
        log.log(" TRASHCLEANER v" + VERSION);
        log.log(" Sistema : " + si.winName +
            " (" + si.winMajor + "." + si.winMinor + "." + si.winBuild + ")");
        log.log(" Maquina : " + si.computerName + " / " + si.userName);
        log.log(" Data    : " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
        log.log("================================================================");
        log.log("");
        long free = Utils.getDiskFree(si.systemDrive);
        log.log("Espaco livre ANTES: " + Logger.fmt(free) + " (" + free + " bytes)");
        log.log("");
    }

    private static String getJarDir() {
        try {
            String path = TrashCleaner.class
                .getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File f = new File(path);
            String parent = f.isFile() ? f.getParent() : path;
            if (!parent.endsWith(File.separator)) parent += File.separator;
            return parent;
        } catch (Exception e) {
            return "";
        }
    }
}
