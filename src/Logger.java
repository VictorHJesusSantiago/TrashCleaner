import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Logger duplo: escreve no console e em arquivo .log simultaneamente.
 * Suporte a cores ANSI no Windows 10+ e barra de progresso inline.
 */
public final class Logger {

    // Codigos ANSI — ESC () seguido de sequencia
    private static final String ESC = "";
    private static final String R   = ESC + "[0m";
    private static final String GR  = ESC + "[32m";
    private static final String YL  = ESC + "[33m";
    private static final String CY  = ESC + "[36m";
    private static final String RD  = ESC + "[31m";
    private static final String BD  = ESC + "[1m";
    private static final String MGN = ESC + "[35m";

    private final PrintWriter file;
    private final PrintWriter jsonFile; // JSON Lines: um objeto JSON por linha de log
    private final boolean ansi;
    private final String logPath;
    private final SimpleDateFormat sdf     = new SimpleDateFormat("HH:mm:ss");
    private final SimpleDateFormat isoSdf  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    /** Se true, a proxima chamada a qualquer metodo de saida deve limpar a linha de progresso. */
    private boolean progressActive = false;

    public Logger(String logPath, boolean ansi) {
        this.logPath = logPath;
        this.ansi    = ansi;
        PrintWriter pw = null, jw = null;
        try {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(logPath)));
            jw = new PrintWriter(new BufferedWriter(new FileWriter(logPath + ".jsonl")));
        } catch (IOException e) {
            System.err.println("[AVISO] Nao foi possivel criar o log: " + e.getMessage());
        }
        this.file     = pw;
        this.jsonFile = jw;
    }

    // ---------------------------------------------------------------
    // Saida principal
    // ---------------------------------------------------------------

    public void section(String title) {
        clearProgressLine();
        String sep = "  " + rep('=', 60);
        rawPrintln("");
        rawPrintln(sep);
        if (ansi) System.out.println(BD + CY + "   " + title + R);
        else       System.out.println("   " + title);
        rawPrintln(sep);
        rawPrintln("");
        log("=== " + title + " ===");
        Config.addOp(title);
    }

    public void info(String msg) {
        clearProgressLine();
        String line = "  [-] " + msg;
        if (ansi) System.out.println(YL + line + R);
        else       System.out.println(line);
        log("[INFO] " + msg);
    }

    public void ok(String msg) {
        clearProgressLine();
        String line = "  [OK] " + msg;
        if (ansi) System.out.println(GR + line + R);
        else       System.out.println(line);
        log("[OK] " + msg);
    }

    public void warn(String msg) {
        clearProgressLine();
        String line = "  [!] " + msg;
        if (ansi) System.out.println(RD + line + R);
        else       System.out.println(line);
        log("[AVISO] " + msg);
    }

    public void println(String msg) {
        clearProgressLine();
        rawPrintln(msg);
    }

    public void log(String msg) {
        Date now = new Date();
        if (file != null) {
            file.println(sdf.format(now) + "  " + msg);
            file.flush();
        }
        if (jsonFile != null) {
            jsonFile.println(jsonLine(now, deriveLevel(msg), msg));
            jsonFile.flush();
        }
    }

    private static String deriveLevel(String msg) {
        if (msg.startsWith("[OK]"))    return "OK";
        if (msg.startsWith("[AVISO]")) return "WARN";
        if (msg.startsWith("[INFO]"))  return "INFO";
        if (msg.startsWith("==="))     return "SECTION";
        return "DEBUG";
    }

    private String jsonLine(Date ts, String level, String msg) {
        String escaped = msg.replace("\\", "\\\\").replace("\"", "\\\"")
                            .replace("\n", "\\n").replace("\r", "");
        return String.format(Locale.ROOT,
            "{\"ts\":\"%s\",\"level\":\"%s\",\"msg\":\"%s\"}",
            isoSdf.format(ts), level, escaped);
    }

    // ---------------------------------------------------------------
    // Barra de progresso inline (sobrescreve a mesma linha via \r)
    // ---------------------------------------------------------------

    /**
     * Exibe/atualiza uma barra de progresso na linha atual do console.
     * Nao grava no arquivo de log (evita poluicao).
     * @param task  descricao curta da operacao em andamento
     * @param pct   0-100
     */
    public void progress(String task, int pct) {
        if (!Config.isShowProgress()) return;
        pct = Math.max(0, Math.min(100, pct));
        int width  = 28;
        int filled = (pct * width) / 100;
        StringBuilder bar = new StringBuilder(width);
        for (int i = 0; i < width; i++) bar.append(i < filled ? '#' : '-');

        String line = "\r  [" + bar + "] " + String.format("%3d%%", pct)
            + "  " + trunc(task, 35) + "          ";

        if (ansi) System.out.print(MGN + line + R);
        else       System.out.print(line);
        System.out.flush();
        progressActive = true;
    }

    /** Termina a linha de progresso atual com uma quebra de linha. */
    public void progressDone() {
        if (progressActive) {
            System.out.println();
            progressActive = false;
        }
    }

    /** Garante que a barra de progresso e apagada antes de imprimir texto normal. */
    private void clearProgressLine() {
        if (progressActive) {
            // Apaga a linha de progresso sobrescrevendo com espacos
            System.out.print("\r" + rep(' ', 80) + "\r");
            System.out.flush();
            progressActive = false;
        }
    }

    private void rawPrintln(String msg) {
        System.out.println(msg);
    }

    // ---------------------------------------------------------------
    // Espaco liberado (sumario)
    // ---------------------------------------------------------------

    public void showFreed(long before, long after) {
        clearProgressLine();
        rawPrintln("");
        rawPrintln("  " + rep('-', 60));
        rawPrintln("   Espaco livre ANTES : " + fmt(before));
        rawPrintln("   Espaco livre APOS  : " + fmt(after));
        long diff = after - before;
        if (diff > 0) {
            String freed = "   Espaco LIBERADO    : " + fmt(diff);
            if (ansi) System.out.println(GR + BD + freed + R);
            else       System.out.println(freed);
        }
        rawPrintln("  " + rep('-', 60));
        log("Antes: " + before + " | Apos: " + after + " | Liberado: " + Math.max(0, diff));
    }

    /** Exibe comparativo completo antes/depois de RAM e disco. */
    public void showComparison(long diskBefore, long diskAfter, long ramBefore, long ramAfter) {
        clearProgressLine();
        rawPrintln("");
        rawPrintln("  " + rep('=', 62));
        rawPrintln("   COMPARATIVO ANTES / DEPOIS");
        rawPrintln("  " + rep('-', 62));
        rawPrintln(String.format("   %-20s  %-14s  %-14s  %s",
            "Recurso", "Antes", "Depois", "Diferenca"));
        rawPrintln("  " + rep('-', 62));

        long diskDiff = diskAfter - diskBefore;
        String diskDiffStr = (diskDiff >= 0 ? "+" : "") + fmt(diskDiff);
        rawPrintln(String.format("   %-20s  %-14s  %-14s  %s",
            "Espaco livre (disco)", fmt(diskBefore), fmt(diskAfter), diskDiffStr));

        if (ramBefore > 0 && ramAfter > 0) {
            long ramDiff = ramAfter - ramBefore;
            String ramDiffStr = (ramDiff >= 0 ? "+" : "") + fmt(ramDiff);
            rawPrintln(String.format("   %-20s  %-14s  %-14s  %s",
                "RAM livre", fmt(ramBefore), fmt(ramAfter), ramDiffStr));
        }

        rawPrintln("  " + rep('-', 62));
        long elapsed = Config.elapsedSeconds();
        rawPrintln("   Tempo de sessao : " + elapsed + " segundos");
        rawPrintln("   Operacoes       : " + Config.sessionOpCount());
        rawPrintln("  " + rep('=', 62));
        log("Comparativo - Disco liberado: " + fmt(Math.max(0, diskDiff))
            + " | RAM liberada: " + fmt(Math.max(0, ramAfter - ramBefore)));
    }

    public String getLogPath() { return logPath; }

    public void close() {
        progressDone();
        if (file     != null) file.close();
        if (jsonFile != null) jsonFile.close();
    }

    // ---------------------------------------------------------------
    // Formatacao de bytes
    // ---------------------------------------------------------------

    public static String fmt(long bytes) {
        if (bytes < 0)            return (bytes < -1_000_000L ? "-" + fmt(-bytes) : "N/A");
        if (bytes < 1024L)        return bytes + " B";
        if (bytes < 1024L * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L << 20)  return String.format("%.1f MB", bytes / 1048576.0);
        return String.format("%.2f GB", bytes / 1073741824.0);
    }

    // ---------------------------------------------------------------
    // Utilitarios internos
    // ---------------------------------------------------------------

    static String rep(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    private static String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
