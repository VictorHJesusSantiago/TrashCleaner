import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Agenda execucoes automaticas do TrashCleaner via Windows Task Scheduler.
 * Suporte a agendamentos diarios e semanais com opcoes de hora e operacao.
 */
public final class AutoScheduler {

    private AutoScheduler() {}

    private static final String TASK_NAME_DAILY  = "TrashCleaner_Daily";
    private static final String TASK_NAME_WEEKLY = "TrashCleaner_Weekly";
    private static final String TASK_NAME_CUSTOM = "TrashCleaner_Custom";

    // ---------------------------------------------------------------
    // Criar agendamento diario
    // ---------------------------------------------------------------

    public static void scheduleDaily(Logger log, String jarPath, String time, String args) {
        log.section("AGENDAR LIMPEZA DIARIA");

        if (!isValidTime(time)) {
            log.warn("Horario invalido: '" + time + "'. Use o formato HH:MM (ex: 03:00).");
            return;
        }

        log.println("  JAR     : " + jarPath);
        log.println("  Horario : " + time);
        log.println("  Operacao: java -jar TrashCleaner.jar " + args);
        log.println("");

        // Verificar se java esta no PATH
        String javaExe = resolveJava();
        String command = "\"" + javaExe + "\" -jar \"" + jarPath + "\" " + args + " --silent";

        deleteTask(TASK_NAME_DAILY);

        int r = Utils.exec("schtasks", "/create",
            "/tn",  TASK_NAME_DAILY,
            "/tr",  command,
            "/sc",  "DAILY",
            "/st",  time,
            "/rl",  "HIGHEST",
            "/ru",  "SYSTEM",
            "/f");

        if (r == 0) {
            log.ok("Limpeza diaria agendada para as " + time + ".");
            log.println("  Nome da tarefa: " + TASK_NAME_DAILY);
            log.println("  Para cancelar: run.bat --schedule-remove-daily");
        } else {
            log.warn("Nao foi possivel criar a tarefa agendada.");
            log.println("  Verifique se tem privilegios de Administrador.");
        }
    }

    // ---------------------------------------------------------------
    // Criar agendamento semanal
    // ---------------------------------------------------------------

    public static void scheduleWeekly(Logger log, String jarPath, String dayOfWeek,
        String time, String args) {
        log.section("AGENDAR LIMPEZA SEMANAL");

        if (!isValidDay(dayOfWeek)) {
            log.warn("Dia da semana invalido: '" + dayOfWeek + "'. Use: MON/TUE/WED/THU/FRI/SAT/SUN.");
            return;
        }
        if (!isValidTime(time)) {
            log.warn("Horario invalido: '" + time + "'. Use o formato HH:MM (ex: 03:00).");
            return;
        }

        log.println("  JAR     : " + jarPath);
        log.println("  Dia     : " + dayOfWeek);
        log.println("  Horario : " + time);
        log.println("  Operacao: java -jar TrashCleaner.jar " + args);
        log.println("");

        String javaExe = resolveJava();
        String command = "\"" + javaExe + "\" -jar \"" + jarPath + "\" " + args + " --silent";

        deleteTask(TASK_NAME_WEEKLY);

        int r = Utils.exec("schtasks", "/create",
            "/tn",  TASK_NAME_WEEKLY,
            "/tr",  command,
            "/sc",  "WEEKLY",
            "/d",   dayOfWeek,
            "/st",  time,
            "/rl",  "HIGHEST",
            "/ru",  "SYSTEM",
            "/f");

        if (r == 0) {
            log.ok("Limpeza semanal agendada para " + dayOfWeek + " as " + time + ".");
            log.println("  Nome da tarefa: " + TASK_NAME_WEEKLY);
            log.println("  Para cancelar: run.bat --schedule-remove-weekly");
        } else {
            log.warn("Nao foi possivel criar a tarefa agendada.");
        }
    }

    // ---------------------------------------------------------------
    // Criar agendamento personalizado (com entrada do usuario)
    // ---------------------------------------------------------------

    public static void scheduleInteractive(Logger log, String jarPath, java.util.Scanner sc) {
        log.section("AGENDAR LIMPEZA AUTOMATICA");

        log.println("  Operacoes disponiveis:");
        log.println("  --quick   : Limpeza rapida");
        log.println("  --deep    : Limpeza profunda");
        log.println("  --all     : Limpeza total");
        log.println("  --privacy : Tweaks de privacidade");
        log.println("");

        System.out.print("  Operacao (ex: --quick): ");
        String args = sc.nextLine().trim();
        if (args.isEmpty()) args = "--quick";

        System.out.print("  Frequencia ([1] Diaria  [2] Semanal): ");
        String freq = sc.nextLine().trim();

        System.out.print("  Horario (HH:MM, ex: 03:00): ");
        String time = sc.nextLine().trim();
        if (time.isEmpty()) time = "03:00";

        if (freq.equals("2")) {
            System.out.print("  Dia da semana (MON/TUE/WED/THU/FRI/SAT/SUN): ");
            String day = sc.nextLine().trim().toUpperCase();
            if (day.isEmpty()) day = "SUN";
            scheduleWeekly(log, jarPath, day, time, args);
        } else {
            scheduleDaily(log, jarPath, time, args);
        }
    }

    // ---------------------------------------------------------------
    // Listar agendamentos do TrashCleaner
    // ---------------------------------------------------------------

    public static void listSchedules(Logger log) {
        log.section("AGENDAMENTOS DO TRASHCLEANER");

        String[] tasks = {TASK_NAME_DAILY, TASK_NAME_WEEKLY, TASK_NAME_CUSTOM};
        boolean found = false;

        for (String task : tasks) {
            String info = Utils.execCapture("schtasks", "/query", "/tn", task, "/FO", "LIST");
            if (info != null && !info.contains("ERROR") && !info.contains("ERRO") &&
                !info.isEmpty()) {
                log.println("  --- " + task + " ---");
                for (String line : info.split("\n")) {
                    String t = line.trim();
                    if (!t.isEmpty()) log.println("  " + t);
                }
                log.println("");
                found = true;
            }
        }

        if (!found) {
            log.println("  Nenhum agendamento do TrashCleaner encontrado.");
            log.println("  Use --schedule-daily ou --schedule-weekly para criar um.");
        }

        log.ok("Listagem de agendamentos concluida.");
    }

    // ---------------------------------------------------------------
    // Remover agendamentos
    // ---------------------------------------------------------------

    public static void removeSchedule(Logger log, String type) {
        log.section("REMOVER AGENDAMENTO");

        switch (type.toLowerCase()) {
            case "daily":
                deleteTask(TASK_NAME_DAILY);
                log.ok("Agendamento diario removido: " + TASK_NAME_DAILY);
                break;
            case "weekly":
                deleteTask(TASK_NAME_WEEKLY);
                log.ok("Agendamento semanal removido: " + TASK_NAME_WEEKLY);
                break;
            case "all":
                deleteTask(TASK_NAME_DAILY);
                deleteTask(TASK_NAME_WEEKLY);
                deleteTask(TASK_NAME_CUSTOM);
                log.ok("Todos os agendamentos do TrashCleaner removidos.");
                break;
            default:
                deleteTask(type);
                log.ok("Tarefa removida: " + type);
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /** Valida formato HH:MM (00:00 – 23:59). */
    static boolean isValidTime(String time) {
        if (time == null || !time.matches("\\d{2}:\\d{2}")) return false;
        String[] parts = time.split(":");
        int h = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);
        return h >= 0 && h <= 23 && m >= 0 && m <= 59;
    }

    /** Valida abreviacao de dia da semana aceita pelo schtasks. */
    static boolean isValidDay(String day) {
        if (day == null) return false;
        switch (day.toUpperCase()) {
            case "MON": case "TUE": case "WED": case "THU":
            case "FRI": case "SAT": case "SUN": return true;
            default: return false;
        }
    }

    private static void deleteTask(String taskName) {
        Utils.exec("schtasks", "/delete", "/tn", taskName, "/f");
    }

    private static String resolveJava() {
        // Tentar localizar java.exe no PATH ou no JAVA_HOME
        String javaHome = System.getProperty("java.home");
        if (javaHome != null && !javaHome.isEmpty()) {
            File javaExe = new File(javaHome, "bin\\java.exe");
            if (javaExe.exists()) return javaExe.getAbsolutePath();
        }
        // Fallback: apenas "java" e esperar que esteja no PATH
        return "java";
    }
}
