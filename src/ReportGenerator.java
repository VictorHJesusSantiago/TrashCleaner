import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Gera relatorio HTML completo da sessao com informacoes de antes/depois,
 * operacoes realizadas, grafico de espaco e estatisticas.
 */
public final class ReportGenerator {

    private ReportGenerator() {}

    /**
     * Gera um relatorio HTML da sessao atual.
     * @return caminho do arquivo HTML gerado, ou null se falhou
     */
    public static String generate(SystemInfo si, Logger log, String outputDir) {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String path  = outputDir + "TrashCleaner_Report_" + stamp + ".html";

        long diskBefore  = Config.getSessionStartFree();
        long diskAfter   = Config.getSessionEndFree();
        long ramBefore   = Config.getSessionStartRam();
        long ramAfter    = Config.getSessionEndRam();
        long elapsed     = Config.elapsedSeconds();
        List<String> ops = new ArrayList<>(Config.getSessionOps());

        try (PrintWriter pw = new PrintWriter(new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(path), "UTF-8")))) {

            long freed = Math.max(0, diskAfter - diskBefore);
            long ramFreed = Math.max(0, ramAfter - ramBefore);

            // Calcular porcentagem de uso do disco
            long diskTotal = Utils.getDiskTotal(si.systemDrive);
            int diskUsePct = diskTotal > 0 ?
                (int)(((diskTotal - diskAfter) * 100L) / diskTotal) : 0;

            pw.println("<!DOCTYPE html>");
            pw.println("<html lang='pt-BR'><head>");
            pw.println("<meta charset='UTF-8'>");
            pw.println("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            pw.println("<title>TrashCleaner Report - " + stamp + "</title>");
            pw.println("<style>");
            pw.println("  * { box-sizing: border-box; margin: 0; padding: 0; }");
            pw.println("  body { font-family: 'Segoe UI', Tahoma, Arial, sans-serif; background: #1a1a2e; color: #e0e0e0; }");
            pw.println("  .container { max-width: 960px; margin: 0 auto; padding: 20px; }");
            pw.println("  h1 { color: #00d4ff; font-size: 2em; text-align: center; margin: 20px 0; text-shadow: 0 0 10px #00d4ff55; }");
            pw.println("  h2 { color: #00d4ff; font-size: 1.3em; margin: 20px 0 10px; border-bottom: 1px solid #333; padding-bottom: 6px; }");
            pw.println("  .card { background: #16213e; border-radius: 8px; padding: 20px; margin: 15px 0; border: 1px solid #0f3460; box-shadow: 0 2px 8px #0004; }");
            pw.println("  .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }");
            pw.println("  .grid3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 15px; }");
            pw.println("  .stat { text-align: center; padding: 15px; background: #0f3460; border-radius: 8px; }");
            pw.println("  .stat .value { font-size: 2em; font-weight: bold; color: #00ff88; }");
            pw.println("  .stat .label { font-size: 0.85em; color: #aaa; margin-top: 4px; }");
            pw.println("  .stat.warn .value { color: #ffaa00; }");
            pw.println("  .stat.info .value { color: #00d4ff; }");
            pw.println("  .bar-outer { background: #0f3460; border-radius: 4px; height: 20px; margin: 6px 0; overflow: hidden; }");
            pw.println("  .bar-inner { height: 100%; border-radius: 4px; transition: width 0.5s; }");
            pw.println("  .bar-green  { background: linear-gradient(90deg, #00ff88, #00aa55); }");
            pw.println("  .bar-blue   { background: linear-gradient(90deg, #00d4ff, #0088aa); }");
            pw.println("  .bar-orange { background: linear-gradient(90deg, #ffaa00, #ff6600); }");
            pw.println("  .bar-red    { background: linear-gradient(90deg, #ff4444, #aa0000); }");
            pw.println("  table { width: 100%; border-collapse: collapse; margin: 10px 0; font-size: 0.9em; }");
            pw.println("  th { background: #0f3460; padding: 8px 10px; text-align: left; color: #00d4ff; }");
            pw.println("  td { padding: 7px 10px; border-bottom: 1px solid #2a2a4a; }");
            pw.println("  tr:hover td { background: #1e2d5a; }");
            pw.println("  .ok { color: #00ff88; } .warn { color: #ffaa00; } .info { color: #00d4ff; }");
            pw.println("  .tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 0.8em; font-weight: bold; }");
            pw.println("  .tag-green { background: #00ff8822; color: #00ff88; border: 1px solid #00ff8844; }");
            pw.println("  .tag-blue  { background: #00d4ff22; color: #00d4ff; border: 1px solid #00d4ff44; }");
            pw.println("  footer { text-align: center; color: #666; margin-top: 30px; font-size: 0.8em; }");
            pw.println("  @media (max-width: 640px) { .grid,.grid3 { grid-template-columns: 1fr; } }");
            pw.println("</style></head><body>");
            pw.println("<div class='container'>");

            // HEADER
            pw.println("<h1>TrashCleaner v2.0 - Relatorio de Sessao</h1>");
            pw.println("<div class='card' style='text-align:center;color:#aaa;'>");
            pw.println("  Computador: <b>" + esc(si.computerName) + "</b> &nbsp;|&nbsp;");
            pw.println("  Usuario: <b>" + esc(si.userName) + "</b> &nbsp;|&nbsp;");
            pw.println("  Sistema: <b>" + esc(si.winName) + "</b> &nbsp;|&nbsp;");
            pw.println("  Data: <b>" + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()) + "</b>");
            pw.println("</div>");

            // STATS PRINCIPAIS
            pw.println("<h2>Resumo da Sessao</h2>");
            pw.println("<div class='grid3'>");
            pw.println("  <div class='stat'><div class='value'>" + Logger.fmt(freed) + "</div><div class='label'>Espaco Liberado</div></div>");
            pw.println("  <div class='stat info'><div class='value'>" + ops.size() + "</div><div class='label'>Operacoes Realizadas</div></div>");
            pw.println("  <div class='stat warn'><div class='value'>" + elapsed + "s</div><div class='label'>Tempo de Sessao</div></div>");
            pw.println("</div>");

            // COMPARATIVO ANTES / DEPOIS
            pw.println("<h2>Comparativo Antes / Depois</h2>");
            pw.println("<div class='card'>");
            pw.println("<table><tr><th>Recurso</th><th>Antes</th><th>Depois</th><th>Diferenca</th></tr>");

            // Disco
            long diskDiff = diskAfter - diskBefore;
            String diskDiffStr = (diskDiff >= 0 ? "+" : "") + Logger.fmt(diskDiff);
            String diskCls = diskDiff > 0 ? "ok" : diskDiff < 0 ? "warn" : "";
            pw.println("<tr><td>Espaco livre (disco)</td><td>" + Logger.fmt(diskBefore) +
                "</td><td>" + Logger.fmt(diskAfter) +
                "</td><td class='" + diskCls + "'>" + diskDiffStr + "</td></tr>");

            // RAM
            if (ramBefore > 0) {
                long ramDiff = ramAfter - ramBefore;
                String ramDiffStr = (ramDiff >= 0 ? "+" : "") + Logger.fmt(ramDiff);
                String ramCls = ramDiff > 0 ? "ok" : ramDiff < 0 ? "warn" : "";
                pw.println("<tr><td>RAM livre</td><td>" + Logger.fmt(ramBefore) +
                    "</td><td>" + Logger.fmt(ramAfter) +
                    "</td><td class='" + ramCls + "'>" + ramDiffStr + "</td></tr>");
            }
            pw.println("</table>");

            // Barra de uso do disco
            if (diskTotal > 0) {
                int beforePct = (int)(((diskTotal - diskBefore) * 100L) / diskTotal);
                int afterPct  = (int)(((diskTotal - diskAfter)  * 100L) / diskTotal);
                String barColor = afterPct > 90 ? "bar-red" : afterPct > 75 ? "bar-orange" : "bar-green";
                pw.println("<div style='margin-top:15px;'>");
                pw.println("  <div style='color:#aaa;font-size:0.9em;'>Uso do disco " + si.systemDrive +
                    " : Antes " + beforePct + "% → Depois " + afterPct + "% (Total: " + Logger.fmt(diskTotal) + ")</div>");
                pw.println("  <div class='bar-outer'><div class='bar-inner " + barColor +
                    "' style='width:" + afterPct + "%'></div></div>");
                pw.println("</div>");
            }
            pw.println("</div>");

            // OPERACOES REALIZADAS
            if (!ops.isEmpty()) {
                pw.println("<h2>Operacoes Realizadas</h2>");
                pw.println("<div class='card'><table><tr><th>#</th><th>Operacao</th></tr>");
                for (int i = 0; i < ops.size(); i++) {
                    pw.println("<tr><td>" + (i+1) + "</td><td><span class='tag tag-green'>" +
                        esc(ops.get(i)) + "</span></td></tr>");
                }
                pw.println("</table></div>");
            }

            // INFORMACOES DO SISTEMA
            pw.println("<h2>Sistema</h2>");
            pw.println("<div class='card'><table>");
            pw.println("<tr><th>Item</th><th>Valor</th></tr>");
            pw.println("<tr><td>Sistema Operacional</td><td>" + esc(si.winName) + "</td></tr>");
            pw.println("<tr><td>Build</td><td>" + si.winMajor + "." + si.winMinor + "." + si.winBuild + "</td></tr>");
            pw.println("<tr><td>Computador</td><td>" + esc(si.computerName) + "</td></tr>");
            pw.println("<tr><td>Usuario</td><td>" + esc(si.userName) + "</td></tr>");
            pw.println("<tr><td>Drive do sistema</td><td>" + esc(si.systemDrive) + "</td></tr>");
            pw.println("<tr><td>Admin</td><td>" + (si.admin ? "<span class='ok'>Sim</span>" : "<span class='warn'>Nao</span>") + "</td></tr>");
            pw.println("<tr><td>Java</td><td>" + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")</td></tr>");
            pw.println("<tr><td>Log da sessao</td><td>" + esc(log.getLogPath()) + "</td></tr>");
            pw.println("</table></div>");

            // FOOTER
            pw.println("<footer>");
            pw.println("  Gerado por TrashCleaner v2.0 em " +
                new SimpleDateFormat("dd/MM/yyyy 'as' HH:mm:ss").format(new Date()));
            pw.println("</footer>");
            pw.println("</div></body></html>");

        } catch (IOException e) {
            log.warn("Erro ao gerar relatorio HTML: " + e.getMessage());
            return null;
        }

        log.ok("Relatorio HTML gerado: " + path);
        return path;
    }

    /** Abre o relatorio HTML no navegador padrao. */
    public static void open(String htmlPath) {
        try {
            new ProcessBuilder("cmd", "/c", "start", "", htmlPath)
                .redirectErrorStream(true).start();
        } catch (IOException ignored) {}
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
