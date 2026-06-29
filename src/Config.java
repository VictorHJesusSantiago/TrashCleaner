import java.util.ArrayList;
import java.util.List;

/**
 * Configuracoes globais da sessao.
 * Acesso estatico universal sem injecao de dependencia.
 */
public final class Config {

    private Config() {}

    // --- Modos de execucao ---

    /** Quando true: nenhuma operacao e executada, apenas exibida no console. */
    public static boolean dryRun   = false;

    /** Quando true: sem prompts interativos; opera sem esperar input do usuario. */
    public static boolean silent   = false;

    /** Exibir barra de progresso inline (sobrescreve a mesma linha via \r). */
    public static boolean showProgress = true;

    // --- Estatisticas de sessao (para relatorio antes/depois) ---

    public static long   sessionStartFree  = 0L;
    public static long   sessionStartRam   = 0L;
    public static long   sessionStartTime  = 0L;
    public static long   sessionEndFree    = 0L;
    public static long   sessionEndRam     = 0L;

    /** Lista de operacoes realizadas na sessao (para ReportGenerator). */
    public static final List<String> sessionOps = new ArrayList<>();

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

    public static void addOp(String desc) {
        sessionOps.add(desc);
    }

    public static long elapsedSeconds() {
        if (sessionStartTime == 0) return 0L;
        return (System.currentTimeMillis() - sessionStartTime) / 1000L;
    }
}
