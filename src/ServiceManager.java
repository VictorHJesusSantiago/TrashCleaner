import java.io.*;
import java.util.*;

/**
 * Gerenciador de servicos do Windows: listar, iniciar, parar e desabilitar
 * servicos via sc.exe. Inclui perfis predefinidos de otimizacao.
 */
public final class ServiceManager {

    private ServiceManager() {}

    // Servicos nao-essenciais comuns que podem ser desabilitados com seguranca
    // (dependendo do uso do computador)
    private static final String[][] OPTIONAL_SERVICES = {
        // {nome_servico, descricao, quando_desabilitar}
        {"DiagTrack",             "Telemetria do Windows",              "sempre"},
        {"dmwappushservice",      "Telemetria WAP Push",                "sempre"},
        {"WerSvc",                "Relatorio de Erros do Windows",      "se nao enviar relatorios"},
        {"SysMain",               "Superfetch/Prefetch",                "se tiver SSD"},
        {"WSearch",               "Windows Search (indexacao)",         "se nao usar busca do Explorer"},
        {"Fax",                   "Servico de Fax",                     "se nao usar fax"},
        {"PrintSpooler",          "Spooler de Impressao",               "se nao tiver impressora"},
        {"RemoteRegistry",        "Registro Remoto",                    "sempre (seguranca)"},
        {"SSDPSRV",               "Descoberta SSDP (UPnP)",            "se nao usar dispositivos UPnP"},
        {"upnphost",              "Host UPnP",                         "se nao usar dispositivos UPnP"},
        {"TabletInputService",    "Servico de Entrada de Tablet",       "se nao usar touch/pen"},
        {"WMPNetworkSvc",         "Windows Media Player Network",       "se nao usar WMP"},
        {"HomeGroupListener",     "HomeGroup Listener",                 "se nao usar Grupo Domestico"},
        {"HomeGroupProvider",     "HomeGroup Provider",                 "se nao usar Grupo Domestico"},
        {"MapsBroker",            "Mapas Baixados",                     "se nao usar app Mapas"},
        {"XblAuthManager",        "Xbox Live Auth Manager",             "se nao usar Xbox"},
        {"XblGameSave",           "Xbox Live Game Save",                "se nao usar Xbox"},
        {"XboxNetApiSvc",         "Xbox Live Networking",               "se nao usar Xbox"},
        {"lfsvc",                 "Localizacao Geografica",             "se nao usar GPS/localizacao"},
        {"RetailDemo",            "Modo de Demonstracao",               "sempre"},
    };

    // ---------------------------------------------------------------
    // Listar servicos
    // ---------------------------------------------------------------

    public static void listServices(Logger log, String filter) {
        log.section("SERVICOS DO WINDOWS" + (filter.isEmpty() ? "" : " [" + filter + "]"));

        if (filter.equals("all")) {
            log.println("  --- Todos os servicos ---");
            Utils.execPrint("sc", "query", "type=", "all", "state=", "all");
        } else if (filter.equals("running")) {
            log.println("  --- Servicos em execucao ---");
            Utils.execPrint("sc", "query", "type=", "all", "state=", "running");
        } else if (filter.equals("stopped")) {
            log.println("  --- Servicos parados ---");
            Utils.execPrint("sc", "query", "type=", "all", "state=", "inactive");
        } else {
            // Listar servicos opcionais com status
            log.println("  --- Servicos opcionais (avaliacao de otimizacao) ---");
            log.println("");
            log.println(String.format("  %-28s  %-10s  %s", "Servico", "Status", "Descricao"));
            log.println("  " + rep('-', 72));

            for (String[] svc : OPTIONAL_SERVICES) {
                String status = queryServiceStatus(svc[0]);
                log.println(String.format("  %-28s  %-10s  %s", svc[0], status, svc[1]));
                if (!svc[2].isEmpty()) {
                    log.println(String.format("  %-28s  %-10s  -> Desabilitar: %s", "", "", svc[2]));
                }
            }
            log.println("");
            log.println("  Use [G] Servicos > Gerenciar para iniciar/parar/desabilitar.");
        }
    }

    // ---------------------------------------------------------------
    // Gerenciar (menu interativo)
    // ---------------------------------------------------------------

    public static void manage(Logger log, Scanner sc) {
        log.section("GERENCIAR SERVICOS");

        while (true) {
            clearScreen();
            log.println("  --- Servicos opcionais e status atual ---");
            log.println("");
            log.println(String.format("  %-5s  %-28s  %-12s  %s",
                "Num", "Servico", "Status", "Descricao"));
            log.println("  " + rep('-', 75));

            for (int i = 0; i < OPTIONAL_SERVICES.length; i++) {
                String[] svc = OPTIONAL_SERVICES[i];
                String status = queryServiceStatus(svc[0]);
                log.println(String.format("  [%2d]  %-28s  %-12s  %s",
                    i+1, svc[0], status, svc[1]));
            }

            log.println("");
            log.println("  [I] Digitar nome de servico personalizado");
            log.println("  [P] Aplicar perfil Gaming (desabilitar Xbox, Fax, UPnP)");
            log.println("  [M] Aplicar perfil Minimo (desabilitar todos opcionais)");
            log.println("  [0] Voltar");
            System.out.print("  >>> Escolha (numero ou letra): ");

            String input = sc.nextLine().trim();

            if (input.equals("0")) break;

            if (input.equalsIgnoreCase("P")) {
                applyGamingProfile(log);
                pressEnter(sc);
                continue;
            }
            if (input.equalsIgnoreCase("M")) {
                applyMinimalProfile(log);
                pressEnter(sc);
                continue;
            }
            if (input.equalsIgnoreCase("I")) {
                manageCustomService(log, sc);
                continue;
            }

            try {
                int idx = Integer.parseInt(input) - 1;
                if (idx < 0 || idx >= OPTIONAL_SERVICES.length) {
                    log.warn("Numero invalido.");
                    Utils.sleep(700);
                    continue;
                }
                String svcName = OPTIONAL_SERVICES[idx][0];
                String svcDesc = OPTIONAL_SERVICES[idx][1];
                manageServiceMenu(log, sc, svcName, svcDesc);
            } catch (NumberFormatException ignored) {
                log.warn("Entrada invalida.");
                Utils.sleep(700);
            }
        }
    }

    private static void manageServiceMenu(Logger log, Scanner sc, String name, String desc) {
        clearScreen();
        String status = queryServiceStatus(name);
        log.println("  Servico : " + name);
        log.println("  Descricao: " + desc);
        log.println("  Status  : " + status);
        log.println("");
        log.println("  [1] Iniciar");
        log.println("  [2] Parar");
        log.println("  [3] Desabilitar (nao inicia automaticamente)");
        log.println("  [4] Habilitar (inicio automatico)");
        log.println("  [5] Inicio manual (sob demanda)");
        log.println("  [0] Voltar");
        System.out.print("  >>> ");
        String choice = sc.nextLine().trim();
        switch (choice) {
            case "1": Utils.exec("net", "start", name); log.ok("Servico iniciado."); break;
            case "2": Utils.exec("net", "stop",  name); log.ok("Servico parado."); break;
            case "3": Utils.exec("sc", "config", name, "start=", "disabled");
                      Utils.exec("net", "stop", name);
                      log.ok("Servico desabilitado."); break;
            case "4": Utils.exec("sc", "config", name, "start=", "auto");
                      log.ok("Servico configurado para inicio automatico."); break;
            case "5": Utils.exec("sc", "config", name, "start=", "demand");
                      log.ok("Servico configurado para inicio manual."); break;
        }
        if (!choice.equals("0")) pressEnter(sc);
    }

    private static void manageCustomService(Logger log, Scanner sc) {
        System.out.print("  Nome do servico (ex: Spooler): ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) return;
        manageServiceMenu(log, sc, name, "Servico personalizado");
    }

    // ---------------------------------------------------------------
    // Perfis predefinidos
    // ---------------------------------------------------------------

    public static void applyGamingProfile(Logger log) {
        log.section("PERFIL GAMING - Desabilitando servicos desnecessarios");
        String[] toDisable = {
            "DiagTrack", "dmwappushservice", "WerSvc", "XblAuthManager",
            "XblGameSave", "XboxNetApiSvc", "MapsBroker", "RetailDemo",
            "HomeGroupListener", "HomeGroupProvider", "WMPNetworkSvc"
        };
        for (String s : toDisable) {
            log.info("Desabilitando: " + s);
            Utils.exec("sc", "config", s, "start=", "disabled");
            Utils.exec("net", "stop", s);
        }
        log.ok("Perfil Gaming aplicado. Reinicie para efeito completo.");
    }

    public static void applyMinimalProfile(Logger log) {
        log.section("PERFIL MINIMO - Desabilitando todos os servicos opcionais");
        for (String[] svc : OPTIONAL_SERVICES) {
            log.info("Desabilitando: " + svc[0] + " (" + svc[1] + ")");
            Utils.exec("sc", "config", svc[0], "start=", "disabled");
            Utils.exec("net", "stop", svc[0]);
        }
        log.ok("Perfil Minimo aplicado. Reinicie para efeito completo.");
    }

    // ---------------------------------------------------------------
    // Reiniciar servicos criticos travados
    // ---------------------------------------------------------------

    public static void restartCritical(Logger log) {
        log.section("REINICIAR SERVICOS CRITICOS");

        String[][] critical = {
            {"Spooler",    "Spooler de Impressao"},
            {"wuauserv",   "Windows Update"},
            {"bits",       "BITS (Background Transfer)"},
            {"cryptsvc",   "Servicos Criptograficos"},
            {"dnscache",   "Cliente DNS"},
            {"lanmanworkstation", "Workstation"},
            {"WinDefend",  "Windows Defender"},
            {"EventLog",   "Log de Eventos do Windows"},
        };

        for (String[] svc : critical) {
            log.info("Reiniciando " + svc[1] + " (" + svc[0] + ")...");
            Utils.exec("net", "stop", svc[0]);
            Utils.sleep(500);
            int r = Utils.exec("net", "start", svc[0]);
            if (r == 0) log.ok(svc[1] + " reiniciado.");
            else log.println("    " + svc[1] + " nao pode ser iniciado (pode estar desabilitado).");
        }
        log.ok("Reinicializacao de servicos criticos concluida.");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static String queryServiceStatus(String name) {
        try {
            Process p = new ProcessBuilder("sc", "query", name)
                .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("STATE") || line.contains("ESTADO")) {
                    if (line.contains("RUNNING") || line.contains("EXECUTANDO")) return "RODANDO";
                    if (line.contains("STOPPED") || line.contains("PARADO"))    return "PARADO";
                    if (line.contains("PAUSED"))                                 return "PAUSADO";
                    if (line.contains("START_PENDING"))                          return "INICIANDO";
                    if (line.contains("STOP_PENDING"))                           return "PARANDO";
                }
            }
            p.waitFor();
            // Se o servico nao existe
            String cfg = Utils.execCapture("sc", "qc", name);
            if (cfg != null && cfg.contains("1060")) return "NAO_EXISTE";
            return "DESCONHECIDO";
        } catch (Exception ignored) {}
        return "?";
    }

    private static void clearScreen() {
        System.out.print("[H[2J");
        System.out.flush();
    }

    private static void pressEnter(Scanner sc) {
        System.out.print("\n  Pressione ENTER para continuar...");
        sc.nextLine();
    }

    private static String rep(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }
}
