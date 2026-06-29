import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Gerencia o historico de sessoes anteriores do TrashCleaner.
 * Lista, abre e exporta logs .log gerados por sessoes passadas.
 */
public final class SessionHistory {

    private SessionHistory() {}

    /** Lista todos os logs de sessoes anteriores no diretorio do JAR. */
    public static void listSessions(Logger log, String jarDir, Scanner sc) {
        log.section("HISTORICO DE SESSOES ANTERIORES");

        File dir = new File(jarDir.isEmpty() ? "." : jarDir);
        File[] logs = dir.listFiles((d, n) ->
            n.startsWith("TrashCleaner_") && n.endsWith(".log"));

        if (logs == null || logs.length == 0) {
            log.println("  Nenhum log de sessao anterior encontrado em: " + dir.getAbsolutePath());
            log.println("  Os logs sao criados na mesma pasta do JAR.");
            return;
        }

        // Ordenar por data de modificacao (mais recente primeiro)
        Arrays.sort(logs, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        log.println("  Diretorio: " + dir.getAbsolutePath());
        log.println("");
        log.println(String.format("  %-4s  %-22s  %-10s  %s",
            "Num", "Data/hora", "Tamanho", "Arquivo"));
        log.println("  " + rep('-', 65));

        for (int i = 0; i < logs.length; i++) {
            File f = logs[i];
            String dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                .format(new Date(f.lastModified()));
            log.println(String.format("  [%2d] %-22s  %-10s  %s",
                i+1, dateStr, Logger.fmt(f.length()), f.getName()));
        }

        log.println("");

        if (Config.isSilent() || sc == null) return;

        log.println("  [A] Abrir log no Notepad    [X] Exportar para HTML");
        log.println("  [D] Deletar log selecionado [C] Deletar TODOS os logs");
        log.println("  [0] Voltar");
        System.out.print("  Numero do log (ou letra): ");

        String input = sc.nextLine().trim();
        if (input.equals("0")) return;

        if (input.equalsIgnoreCase("C")) {
            System.out.print("  Confirmar delecao de " + logs.length + " logs? [S]: ");
            if ("S".equalsIgnoreCase(sc.nextLine().trim())) {
                for (File f : logs) f.delete();
                log.ok("Todos os logs de sessao deletados.");
            }
            return;
        }

        // Tentar interpretar como numero
        File selected = null;
        try {
            int idx = Integer.parseInt(
                input.replaceAll("[AaDdXx]", "").trim()) - 1;
            if (idx >= 0 && idx < logs.length) {
                selected = logs[idx];
            }
        } catch (NumberFormatException ignored) {}

        if (selected == null && !input.isEmpty()) {
            // Pode ser que o usuario digitou "A 3" ou "3"
            for (String part : input.split("\\s+")) {
                try {
                    int idx = Integer.parseInt(part) - 1;
                    if (idx >= 0 && idx < logs.length) selected = logs[idx];
                } catch (NumberFormatException ignored) {}
            }
        }

        if (selected == null) {
            log.warn("Selecao invalida.");
            return;
        }

        if (input.toUpperCase().startsWith("D") ||
            (input.length() > 1 && input.toUpperCase().contains("D"))) {
            selected.delete();
            log.ok("Log deletado: " + selected.getName());
        } else if (input.toUpperCase().startsWith("X") ||
            input.toUpperCase().contains("X")) {
            exportLogToHtml(log, selected, jarDir);
        } else {
            // Abrir no Notepad (padrao)
            try {
                new ProcessBuilder("notepad.exe", selected.getAbsolutePath())
                    .redirectErrorStream(true).start();
                log.ok("Abrindo: " + selected.getName());
            } catch (IOException e) {
                log.warn("Erro ao abrir: " + e.getMessage());
            }
        }
    }

    /** Exporta um arquivo .log para HTML simples. */
    public static void exportLogToHtml(Logger log, File logFile, String outputDir) {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String outPath = outputDir + "Log_Export_" + stamp + ".html";

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile));
             PrintWriter pw = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outPath), "UTF-8")))) {

            pw.println("<!DOCTYPE html><html lang='pt-BR'><head>");
            pw.println("<meta charset='UTF-8'>");
            pw.println("<title>Log: " + logFile.getName() + "</title>");
            pw.println("<style>");
            pw.println("body{font-family:monospace;background:#111;color:#ddd;padding:20px;}");
            pw.println("h1{color:#00d4ff;border-bottom:1px solid #333;padding-bottom:8px;}");
            pw.println(".ok{color:#00ff88;}.warn{color:#ffaa00;}.section{color:#00d4ff;font-weight:bold;}");
            pw.println(".line{margin:2px 0;white-space:pre-wrap;word-break:break-all;}");
            pw.println("</style></head><body>");
            pw.println("<h1>TrashCleaner Log: " + esc(logFile.getName()) + "</h1>");
            pw.println("<div>");

            String line;
            while ((line = reader.readLine()) != null) {
                String cls = "";
                if (line.contains("[OK]"))    cls = "ok";
                else if (line.contains("[AVISO]") || line.contains("[!]")) cls = "warn";
                else if (line.contains("===")) cls = "section";

                String escaped = esc(line);
                if (cls.isEmpty()) {
                    pw.println("<div class='line'>" + escaped + "</div>");
                } else {
                    pw.println("<div class='line " + cls + "'>" + escaped + "</div>");
                }
            }

            pw.println("</div></body></html>");
            log.ok("Log exportado para HTML: " + outPath);

            // Abrir no navegador
            try {
                new ProcessBuilder("cmd", "/c", "start", "", outPath)
                    .redirectErrorStream(true).start();
            } catch (IOException ignored) {}

        } catch (IOException e) {
            log.warn("Erro ao exportar log: " + e.getMessage());
        }
    }

    /** Retorna o numero de sessoes anteriores registradas. */
    public static int countSessions(String jarDir) {
        File dir = new File(jarDir.isEmpty() ? "." : jarDir);
        File[] logs = dir.listFiles((d, n) ->
            n.startsWith("TrashCleaner_") && n.endsWith(".log"));
        return logs == null ? 0 : logs.length;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    private static String rep(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }
}
