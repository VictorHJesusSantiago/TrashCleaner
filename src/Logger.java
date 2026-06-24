import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Logger duplo: escreve no console e em arquivo .log simultaneamente.
 * Suporte a cores ANSI no Windows 10+.
 */
public final class Logger {

    // Codigos ANSI
    private static final String R  = "[0m";   // reset
    private static final String GR = "[32m";  // verde
    private static final String YL = "[33m";  // amarelo
    private static final String CY = "[36m";  // ciano
    private static final String RD = "[31m";  // vermelho
    private static final String BD = "[1m";   // negrito

    private final PrintWriter file;
    private final boolean ansi;
    private final String logPath;
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public Logger(String logPath, boolean ansi) {
        this.logPath = logPath;
        this.ansi    = ansi;
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(logPath)));
        } catch (IOException e) {
            System.err.println("[AVISO] Nao foi possivel criar o log: " + e.getMessage());
        }
        this.file = pw;
    }

    // --- saida principal ---

    public void section(String title) {
        String sep = "  " + "=".repeat(60);
        println("");
        println(sep);
        if (ansi) System.out.println(BD + CY + "   " + title + R);
        else       System.out.println("   " + title);
        println(sep);
        println("");
        log("=== " + title + " ===");
    }

    public void info(String msg) {
        String line = "  [-] " + msg;
        if (ansi) System.out.println(YL + line + R);
        else       System.out.println(line);
        log("[INFO] " + msg);
    }

    public void ok(String msg) {
        String line = "  [OK] " + msg;
        if (ansi) System.out.println(GR + line + R);
        else       System.out.println(line);
        log("[OK] " + msg);
    }

    public void warn(String msg) {
        String line = "  [!] " + msg;
        if (ansi) System.out.println(RD + line + R);
        else       System.out.println(line);
        log("[AVISO] " + msg);
    }

    public void println(String msg) {
        System.out.println(msg);
    }

    public void log(String msg) {
        if (file != null) {
            file.println(sdf.format(new Date()) + "  " + msg);
            file.flush();
        }
    }

    // --- espaco liberado ---

    public void showFreed(long before, long after) {
        println("");
        println("  " + "-".repeat(60));
        println("   Espaco livre ANTES : " + fmt(before));
        println("   Espaco livre APOS  : " + fmt(after));
        long diff = after - before;
        if (diff > 0) {
            String freed = "   Espaco LIBERADO    : " + fmt(diff);
            if (ansi) System.out.println(GR + BD + freed + R);
            else       System.out.println(freed);
        }
        println("  " + "-".repeat(60));
        log("Antes: " + before + " | Apos: " + after + " | Liberado: " + Math.max(0, diff));
    }

    public String getLogPath() { return logPath; }

    public void close() {
        if (file != null) file.close();
    }

    // --- formatacao de bytes ---

    public static String fmt(long bytes) {
        if (bytes < 0)               return "N/A";
        if (bytes < 1024L)           return bytes + " B";
        if (bytes < 1024L * 1024)    return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024)
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
