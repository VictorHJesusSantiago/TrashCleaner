import java.io.*;
import java.util.*;

/**
 * Gerenciador de tarefas agendadas do Windows (Task Scheduler).
 * Lista, desabilita e reabilita tarefas via schtasks.exe.
 */
public final class ScheduledTaskManager {

    private ScheduledTaskManager() {}

    // Tarefas de telemetria e coleta de dados que podem ser desabilitadas com seguranca
    private static final String[] TELEMETRY_TASKS = {
        "\\Microsoft\\Windows\\Application Experience\\Microsoft Compatibility Appraiser",
        "\\Microsoft\\Windows\\Application Experience\\ProgramDataUpdater",
        "\\Microsoft\\Windows\\Application Experience\\AitAgent",
        "\\Microsoft\\Windows\\Customer Experience Improvement Program\\Consolidator",
        "\\Microsoft\\Windows\\Customer Experience Improvement Program\\KernelCEIPTask",
        "\\Microsoft\\Windows\\Customer Experience Improvement Program\\UsbCeip",
        "\\Microsoft\\Windows\\DiskDiagnostic\\Microsoft-Windows-DiskDiagnosticDataCollector",
        "\\Microsoft\\Windows\\Feedback\\Siuf\\DmClient",
        "\\Microsoft\\Windows\\Feedback\\Siuf\\DmClientOnScenarioDownload",
        "\\Microsoft\\Windows\\PI\\Sqm-Tasks",
        "\\Microsoft\\Windows\\Windows Error Reporting\\QueueReporting",
        "\\Microsoft\\Windows\\Autochk\\Proxy",
        "\\Microsoft\\Windows\\CloudExperienceHost\\CreateObjectTask",
        "\\Microsoft\\Windows\\Maps\\MapsUpdateTask",
        "\\Microsoft\\Windows\\Maps\\MapsToastTask",
        "\\Microsoft\\Windows\\Shell\\FamilySafetyMonitor",
        "\\Microsoft\\Windows\\Shell\\FamilySafetyRefresh",
        "\\Microsoft\\Windows\\License Manager\\TempSignedLicenseExchange",
        "\\Microsoft\\Windows\\WS\\WSTask",
    };

    // ---------------------------------------------------------------
    // Listar tarefas agendadas
    // ---------------------------------------------------------------

    public static void listTasks(Logger log, String filter) {
        log.section("TAREFAS AGENDADAS" + (filter.isEmpty() ? "" : " [" + filter + "]"));

        if (filter.equals("telemetry")) {
            log.println("  --- Tarefas de Telemetria e Coleta de Dados ---");
            log.println("");
            log.println(String.format("  %-62s  %s", "Tarefa", "Status"));
            log.println("  " + rep('-', 75));
            for (String task : TELEMETRY_TASKS) {
                String status = queryTaskStatus(task);
                log.println(String.format("  %-62s  %s", task, status));
            }
        } else if (filter.equals("all")) {
            log.println("  Listando todas as tarefas agendadas (pode demorar)...");
            log.println("");
            Utils.execPrint("schtasks", "/query", "/FO", "TABLE", "/NH");
        } else if (filter.equals("running")) {
            log.println("  Tarefas em execucao:");
            Utils.execPrint("schtasks", "/query", "/FO", "TABLE", "/NH", "/V");
        } else {
            // Padrao: listar tarefas do usuario e do sistema (nivel raiz)
            log.println("  --- Tarefas do usuario ---");
            Utils.execPrint("schtasks", "/query", "/FO", "TABLE");
        }

        log.ok("Listagem de tarefas concluida.");
    }

    // ---------------------------------------------------------------
    // Gerenciar tarefas (modo interativo)
    // ---------------------------------------------------------------

    public static void manage(Logger log, Scanner sc) {
        log.section("GERENCIAR TAREFAS AGENDADAS");

        while (true) {
            System.out.println("  " + rep('=', 65));
            System.out.println("  TAREFAS DE TELEMETRIA (recomendado desabilitar)");
            System.out.println("  " + rep('-', 65));
            System.out.println(String.format("  %-3s  %-12s  %s", "Num", "Status", "Tarefa"));
            System.out.println("  " + rep('-', 65));

            for (int i = 0; i < TELEMETRY_TASKS.length; i++) {
                String status = queryTaskStatus(TELEMETRY_TASKS[i]);
                String shortName = TELEMETRY_TASKS[i];
                // Pegar apenas o nome da tarefa (apos ultimo \)
                int idx = shortName.lastIndexOf('\\');
                if (idx >= 0) shortName = shortName.substring(idx+1);
                System.out.println(String.format("  [%2d] %-12s  %s",
                    i+1, status, shortName));
            }

            System.out.println("");
            System.out.println("  [D] Desabilitar TODAS as tarefas de telemetria");
            System.out.println("  [E] Habilitar TODAS as tarefas de telemetria");
            System.out.println("  [C] Desabilitar tarefa personalizada (informar caminho)");
            System.out.println("  [0] Voltar");
            System.out.print("  >>> ");

            String input = sc.nextLine().trim();

            if (input.equals("0")) break;

            if (input.equalsIgnoreCase("D")) {
                disableTelemetryTasks(log);
                pressEnter(sc);
            } else if (input.equalsIgnoreCase("E")) {
                enableTelemetryTasks(log);
                pressEnter(sc);
            } else if (input.equalsIgnoreCase("C")) {
                System.out.print("  Caminho da tarefa (ex: \\Microsoft\\Windows\\X): ");
                String task = sc.nextLine().trim();
                if (!task.isEmpty()) {
                    System.out.print("  [1] Desabilitar  [2] Habilitar: ");
                    String act = sc.nextLine().trim();
                    if (act.equals("1")) {
                        Utils.exec("schtasks", "/change", "/tn", task, "/disable");
                        log.ok("Tarefa desabilitada: " + task);
                    } else if (act.equals("2")) {
                        Utils.exec("schtasks", "/change", "/tn", task, "/enable");
                        log.ok("Tarefa habilitada: " + task);
                    }
                }
                pressEnter(sc);
            } else {
                try {
                    int num = Integer.parseInt(input) - 1;
                    if (num >= 0 && num < TELEMETRY_TASKS.length) {
                        String task = TELEMETRY_TASKS[num];
                        String curStatus = queryTaskStatus(task);
                        if (curStatus.contains("Habilitada") || curStatus.equalsIgnoreCase("Ready") ||
                            curStatus.equalsIgnoreCase("Running")) {
                            Utils.exec("schtasks", "/change", "/tn", task, "/disable");
                            log.ok("Desabilitada: " + task);
                        } else {
                            Utils.exec("schtasks", "/change", "/tn", task, "/enable");
                            log.ok("Habilitada: " + task);
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    // ---------------------------------------------------------------
    // Desabilitar / Habilitar tarefas de telemetria
    // ---------------------------------------------------------------

    public static void disableTelemetryTasks(Logger log) {
        log.section("DESABILITAR TAREFAS DE TELEMETRIA");
        for (String task : TELEMETRY_TASKS) {
            log.info("Desabilitando: " + task);
            Utils.exec("schtasks", "/change", "/tn", task, "/disable");
        }
        log.ok("Todas as tarefas de telemetria desabilitadas.");
    }

    public static void enableTelemetryTasks(Logger log) {
        log.section("HABILITAR TAREFAS DE TELEMETRIA");
        for (String task : TELEMETRY_TASKS) {
            log.info("Habilitando: " + task);
            Utils.exec("schtasks", "/change", "/tn", task, "/enable");
        }
        log.ok("Todas as tarefas de telemetria habilitadas.");
    }

    // ---------------------------------------------------------------
    // Diagnostico de tempo de boot
    // ---------------------------------------------------------------

    public static void bootDiagnostic(SystemInfo si, Logger log) {
        log.section("DIAGNOSTICO DE TEMPO DE BOOT");

        if (!si.supportsWevtutil()) {
            log.warn("Diagnostico de boot requer Windows Vista+.");
            return;
        }

        log.println("  --- Ultimas 5 medicoes de tempo de boot ---");
        log.println("");
        Utils.execPrint("powershell", "-noprofile", "-Command",
            "Get-WinEvent -ProviderName 'Microsoft-Windows-Diagnostics-Performance' " +
            "-ErrorAction SilentlyContinue " +
            "| Where-Object {$_.Id -eq 100} " +
            "| Select-Object -First 5 " +
            "  TimeCreated," +
            "  @{N='Boot(s)';E={$_.Properties[0].Value}} " +
            "| Format-Table -AutoSize");

        log.println("");
        log.println("  --- Servicos que atrasaram o boot (EventID 101) ---");
        Utils.execPrint("powershell", "-noprofile", "-Command",
            "Get-WinEvent -ProviderName 'Microsoft-Windows-Diagnostics-Performance' " +
            "-ErrorAction SilentlyContinue " +
            "| Where-Object {$_.Id -eq 101} " +
            "| Select-Object -First 10 TimeCreated,Message " +
            "| Format-List");

        log.ok("Diagnostico de boot concluido.");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static String queryTaskStatus(String taskPath) {
        try {
            Process p = new ProcessBuilder("schtasks", "/query", "/tn", taskPath, "/FO", "LIST")
                .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("Status") || line.contains("Estado")) {
                    String val = line.substring(line.indexOf(':') + 1).trim();
                    return val.isEmpty() ? "?" : val;
                }
            }
            p.waitFor();
            return "NAO_EXISTE";
        } catch (Exception ignored) {}
        return "?";
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
