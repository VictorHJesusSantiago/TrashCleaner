import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuracoes globais da sessao.
 * Campos privados com acessores estaticos: mutacao controlada, sem acesso direto externo.
 */
public final class Config {

    private Config() {}

    // --- Flags de execucao (definidas uma unica vez na inicializacao) ---

    private static boolean dryRun      = false;
    private static boolean silent      = false;
    private static boolean showProgress = true;

    public static boolean isDryRun()        { return dryRun; }
    public static boolean isSilent()        { return silent; }
    public static boolean isShowProgress()  { return showProgress; }

    public static void setDryRun(boolean v)       { dryRun = v; }
    public static void setSilent(boolean v)        { silent = v; }
    public static void setShowProgress(boolean v)  { showProgress = v; }

    // --- Estatisticas de sessao (gerenciadas via startSession/endSession) ---

    private static long sessionStartFree = 0L;
    private static long sessionStartRam  = 0L;
    private static long sessionStartTime = 0L;
    private static long sessionEndFree   = 0L;
    private static long sessionEndRam    = 0L;

    public static long getSessionStartFree() { return sessionStartFree; }
    public static long getSessionStartRam()  { return sessionStartRam; }
    public static long getSessionEndFree()   { return sessionEndFree; }
    public static long getSessionEndRam()    { return sessionEndRam; }

    // --- Operacoes da sessao ---

    private static final List<String> sessionOps = new ArrayList<>();

    /** Retorna copia imutavel da lista de operacoes realizadas na sessao. */
    public static List<String> getSessionOps() {
        return Collections.unmodifiableList(sessionOps);
    }

    public static int sessionOpCount() { return sessionOps.size(); }

    public static void startSession(long diskFree, long ramFree) {
        sessionStartFree = diskFree;
        sessionStartRam  = ramFree;
        sessionStartTime = System.currentTimeMillis();
        sessionOps.clear();
    }

    public static void endSession(long diskFree, long ramFree) {
        sessionEndFree = diskFree;
        sessionEndRam  = ramFree;
    }

    public static void addOp(String desc) { sessionOps.add(desc); }

    public static long elapsedSeconds() {
        if (sessionStartTime == 0) return 0L;
        return (System.currentTimeMillis() - sessionStartTime) / 1000L;
    }
}
