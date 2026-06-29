import java.io.*;
import java.util.*;

/**
 * Gerencia programas de inicializacao do Windows.
 * Leitura via reg query; ativacao/desativacao via StartupApproved (Win8+)
 * ou backup de chave (XP/7).
 */
public final class StartupManager {

    private StartupManager() {}

    // Chaves de registro dos programas de inicializacao
    private static final String HKCU_RUN = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String HKLM_RUN = "HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String HKCU_APPROVED =
        "HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Explorer\\StartupApproved\\Run";
    private static final String HKLM_APPROVED =
        "HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Explorer\\StartupApproved\\Run";
    private static final String HKCU_BACKUP =
        "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run_TC_Disabled";
    private static final String HKLM_BACKUP =
        "HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run_TC_Disabled";

    // ---------------------------------------------------------------
    // Modelo de dado
    // ---------------------------------------------------------------

    static class Entry {
        final String  name;
        final String  command;
        final String  regKey;
        final boolean hkcu;
        boolean       enabled;

        Entry(String name, String command, String regKey, boolean hkcu, boolean enabled) {
            this.name    = name;
            this.command = command;
            this.regKey  = regKey;
            this.hkcu    = hkcu;
            this.enabled = enabled;
        }
    }

    // ---------------------------------------------------------------
    // Listar (modo CLI / informativo)
    // ---------------------------------------------------------------

    public static void listStartup(SystemInfo si, Logger log) {
        log.section("PROGRAMAS DE INICIALIZACAO");
        List<Entry> entries = collectEntries(si);

        if (entries.isEmpty()) {
            log.println("  Nenhuma entrada de inicializacao encontrada nas chaves Run do registro.");
            return;
        }

        log.println(String.format("  %-4s %-10s %-32s %s", "Num", "Status", "Nome", "Escopo"));
        log.println("  " + rep('-', 65));
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            String status = e.enabled ? "ATIVO    " : "INATIVO  ";
            log.println(String.format("  [%d] %s %-32s %s",
                i + 1, status, trunc(e.name, 32), e.hkcu ? "Usuario" : "Sistema"));
            log.println("       " + trunc(e.command, 66));
        }
        log.println("");
        log.println("  Use o menu interativo para ativar/desativar entradas.");
    }

    // ---------------------------------------------------------------
    // Gerenciar (modo interativo)
    // ---------------------------------------------------------------

    public static void manage(SystemInfo si, Logger log, Scanner sc) {
        log.section("GERENCIAR PROGRAMAS DE INICIALIZACAO");
        List<Entry> entries = collectEntries(si);

        while (true) {
            System.out.println();

            if (entries.isEmpty()) {
                System.out.println("  Nenhuma entrada de inicializacao encontrada.");
                break;
            }

            System.out.println("  " + rep('=', 65));
            System.out.println(String.format("  %-5s %-11s %-30s %s",
                "Num", "Status", "Nome", "Escopo"));
            System.out.println("  " + rep('-', 65));

            for (int i = 0; i < entries.size(); i++) {
                Entry e = entries.get(i);
                String tag = e.enabled ? "[ ATIVO ]" : "[INATIVO]";
                System.out.println(String.format("  [%d]  %s  %-30s %s",
                    i + 1, tag, trunc(e.name, 30), e.hkcu ? "Usuario" : "Sistema"));
                System.out.println("         " + trunc(e.command, 58));
            }

            System.out.println("  " + rep('=', 65));
            System.out.println("  Digite o numero para ATIVAR/DESATIVAR a entrada.");
            System.out.println("  [0] Voltar ao menu principal");
            System.out.print("  >>> ");

            String input = sc.nextLine().trim();
            if (input.equals("0")) break;

            try {
                int idx = Integer.parseInt(input) - 1;
                if (idx < 0 || idx >= entries.size()) {
                    System.out.println("  Numero invalido. Tente novamente.");
                    continue;
                }
                Entry e = entries.get(idx);
                if (e.enabled) {
                    disable(si, e);
                    e.enabled = false;
                    log.ok("\"" + e.name + "\" desabilitado na inicializacao.");
                } else {
                    enable(si, e);
                    e.enabled = true;
                    log.ok("\"" + e.name + "\" habilitado na inicializacao.");
                }
            } catch (NumberFormatException ignored) {
                System.out.println("  Digite apenas o numero da entrada.");
            }
        }
    }

    // ---------------------------------------------------------------
    // Coleta de entradas
    // ---------------------------------------------------------------

    static List<Entry> collectEntries(SystemInfo si) {
        // Descobrir quais nomes estao desabilitados via StartupApproved
        Set<String> disabledHkcu = getDisabledSet(HKCU_APPROVED);
        Set<String> disabledHklm = getDisabledSet(HKLM_APPROVED);

        List<Entry> list = new ArrayList<>();
        list.addAll(parseRunKey(HKCU_RUN, true,  disabledHkcu));
        list.addAll(parseRunKey(HKLM_RUN, false, disabledHklm));
        return list;
    }

    /** Retorna nomes que estao marcados como desabilitados no StartupApproved. */
    private static Set<String> getDisabledSet(String approvedKey) {
        Set<String> disabled = new HashSet<>();
        try {
            Process p = new ProcessBuilder("reg", "query", approvedKey)
                .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("HKEY")) continue;
                // "    Name    REG_BINARY    03..."
                String[] parts = line.split("\\s{2,}", 3);
                if (parts.length == 3 && parts[1].trim().equals("REG_BINARY")) {
                    // 03 = desabilitado, 02 = habilitado
                    if (parts[2].trim().startsWith("03")) {
                        disabled.add(parts[0].trim());
                    }
                }
            }
            p.waitFor();
        } catch (Exception ignored) {}
        return disabled;
    }

    /** Faz parse das entradas de uma chave Run. */
    private static List<Entry> parseRunKey(String regKey, boolean hkcu, Set<String> disabled) {
        List<Entry> result = new ArrayList<>();
        try {
            Process p = new ProcessBuilder("reg", "query", regKey)
                .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("HKEY")) continue;
                // "    Name    REG_SZ    C:\path\to\app.exe --args"
                String[] parts = line.split("\\s{2,}", 3);
                if (parts.length == 3) {
                    String type = parts[1].trim();
                    if (type.equals("REG_SZ") || type.equals("REG_EXPAND_SZ")) {
                        String name = parts[0].trim();
                        String cmd  = parts[2].trim();
                        if (!name.equals("(Default)") && !name.isEmpty()) {
                            result.add(new Entry(name, cmd, regKey, hkcu,
                                !disabled.contains(name)));
                        }
                    }
                }
            }
            p.waitFor();
        } catch (Exception ignored) {}
        return result;
    }

    // ---------------------------------------------------------------
    // Desabilitar / Habilitar
    // ---------------------------------------------------------------

    private static void disable(SystemInfo si, Entry e) {
        if (si.isWin8Plus()) {
            // Metodo moderno: StartupApproved com valor 03 (desabilitado)
            // 03 00 00 00 00 00 00 00 00 00 00 00 (12 bytes)
            Utils.exec("reg", "add",
                e.hkcu ? HKCU_APPROVED : HKLM_APPROVED,
                "/v", e.name, "/t", "REG_BINARY",
                "/d", "030000000000000000000000", "/f");
        } else {
            // XP/7: mover para chave de backup e remover do Run
            String backupKey = e.hkcu ? HKCU_BACKUP : HKLM_BACKUP;
            Utils.exec("reg", "add", backupKey, "/v", e.name,
                "/t", "REG_SZ", "/d", e.command, "/f");
            Utils.exec("reg", "delete", e.regKey, "/v", e.name, "/f");
        }
    }

    private static void enable(SystemInfo si, Entry e) {
        if (si.isWin8Plus()) {
            // Valor 02 = habilitado
            Utils.exec("reg", "add",
                e.hkcu ? HKCU_APPROVED : HKLM_APPROVED,
                "/v", e.name, "/t", "REG_BINARY",
                "/d", "020000000000000000000000", "/f");
        } else {
            // XP/7: restaurar do backup
            String backupKey = e.hkcu ? HKCU_BACKUP : HKLM_BACKUP;
            Utils.exec("reg", "add", e.regKey, "/v", e.name,
                "/t", "REG_SZ", "/d", e.command, "/f");
            Utils.exec("reg", "delete", backupKey, "/v", e.name, "/f");
        }
    }

    // ---------------------------------------------------------------
    // Utilitarios
    // ---------------------------------------------------------------

    private static String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private static String rep(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }
}
