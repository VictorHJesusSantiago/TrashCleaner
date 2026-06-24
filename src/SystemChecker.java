/**
 * Verificacao e reparo de integridade do sistema:
 * SFC (todas as versoes) + DISM (Win8+).
 */
public final class SystemChecker {

    private SystemChecker() {}

    public static void check(SystemInfo si, Logger log) {
        log.section("VERIFICACAO E REPARO DO SISTEMA");
        log.println("  (Isso pode levar de 10 a 30 minutos - nao feche esta janela)");
        log.println("");

        log.info("Verificando integridade dos arquivos (SFC /scannow)...");
        log.println("  --- Saida do SFC ---");
        Utils.execPrint("sfc", "/scannow");
        log.println("  --- Fim do SFC ---");
        log.ok("SFC concluido - veja o log em %SystemRoot%\\Logs\\CBS\\CBS.log");

        if (si.supportsDism()) {
            log.println("");
            log.info("Verificando imagem do Windows (DISM)...");
            log.println("  --- Saida do DISM ---");
            Utils.execPrint("dism", "/online", "/cleanup-image", "/checkhealth");
            Utils.execPrint("dism", "/online", "/cleanup-image", "/scanhealth");
            Utils.execPrint("dism", "/online", "/cleanup-image", "/restorehealth");
            log.println("  --- Fim do DISM ---");
            log.ok("DISM concluido.");
        } else {
            log.println("");
            log.println("  [INFO] DISM nao disponivel nesta versao do Windows (requer Win8+).");
        }

        log.println("");
        log.ok("Verificacao de sistema concluida!");
        log.println("  Consulte o relatorio completo do SFC em:");
        log.println("  " + si.systemRoot + "\\Logs\\CBS\\CBS.log");
    }
}
