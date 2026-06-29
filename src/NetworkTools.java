import java.io.*;
import java.util.*;

/**
 * Ferramentas avancadas de rede: teste de conectividade, netstat, portas,
 * configuracao de DNS, perfis Wi-Fi, adaptadores, proxy e traceroute.
 */
public final class NetworkTools {

    private NetworkTools() {}

    // ---------------------------------------------------------------
    // Teste de conectividade
    // ---------------------------------------------------------------

    public static void testConnectivity(Logger log) {
        log.section("TESTE DE CONECTIVIDADE");

        String[] targets = {
            "8.8.8.8",         // Google DNS
            "1.1.1.1",         // Cloudflare DNS
            "208.67.222.222",  // OpenDNS
            "google.com",      // Resolucao DNS + HTTP
            "microsoft.com"    // Resolucao DNS + HTTP
        };

        log.println(String.format("  %-22s  %-10s  %s", "Destino", "Status", "Latencia media"));
        log.println("  " + rep('-', 55));

        for (String target : targets) {
            log.progress("Pingando " + target, 20);
            try {
                Process p = new ProcessBuilder("ping", "-n", "4", target)
                    .redirectErrorStream(true).start();
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String output = "";
                String line;
                while ((line = br.readLine()) != null) output += line + "\n";
                p.waitFor();

                String status, latency;
                if (output.contains("Perdidos = 0") || output.contains("Lost = 0") ||
                    output.contains("Received = 4") || output.contains("Recebidos = 4")) {
                    status = "OK";
                    // Extrair latencia media
                    String lat = extractLatency(output);
                    latency = lat.isEmpty() ? "?" : lat + " ms";
                } else if (output.contains("Request timed out") || output.contains("Esgotado") ||
                    output.contains("100%")) {
                    status = "TIMEOUT";
                    latency = "---";
                } else {
                    status = "FALHA";
                    latency = "---";
                }
                log.progressDone();
                log.println(String.format("  %-22s  %-10s  %s", target, status, latency));
            } catch (Exception e) {
                log.progressDone();
                log.println(String.format("  %-22s  %-10s  %s", target, "ERRO", e.getMessage()));
            }
        }

        log.println("");
        log.println("  DNS local atual:");
        Utils.execPrint("ipconfig", "/displaydns");
        log.ok("Teste de conectividade concluido.");
    }

    // ---------------------------------------------------------------
    // Listar conexoes ativas (netstat)
    // ---------------------------------------------------------------

    public static void listConnections(Logger log) {
        log.section("CONEXOES DE REDE ATIVAS");
        log.println("  Proto  Endereco Local             Endereco Remoto          Estado");
        log.println("  " + rep('-', 70));

        try {
            Process p = new ProcessBuilder("netstat", "-ano")
                .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (t.startsWith("TCP") || t.startsWith("UDP")) {
                    log.println("  " + t);
                }
            }
            p.waitFor();
        } catch (Exception e) {
            log.warn("Erro ao listar conexoes: " + e.getMessage());
        }
        log.println("");
        log.ok("Listagem de conexoes concluida.");
    }

    // ---------------------------------------------------------------
    // Listar portas abertas (LISTENING)
    // ---------------------------------------------------------------

    public static void listOpenPorts(Logger log) {
        log.section("PORTAS ABERTAS (LISTENING)");
        log.println("  Proto  Porta    PID     Processo");
        log.println("  " + rep('-', 55));

        Map<String,String> pidToName = getPidToName();

        try {
            Process p = new ProcessBuilder("netstat", "-ano")
                .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            Set<String> seen = new HashSet<>();
            String line;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (!t.contains("LISTENING") && !t.contains("ESCUTANDO")) continue;
                String[] parts = t.split("\\s+");
                if (parts.length < 4) continue;
                String proto = parts[0];
                String local = parts[1];
                String pid   = parts[parts.length - 1];
                String port  = local.contains(":") ?
                    local.substring(local.lastIndexOf(':') + 1) : local;
                String key   = proto + "|" + port + "|" + pid;
                if (seen.contains(key)) continue;
                seen.add(key);
                String pname = pidToName.getOrDefault(pid, "PID " + pid);
                log.println(String.format("  %-6s %-8s %-7s %s", proto, port, pid, pname));
            }
            p.waitFor();
        } catch (Exception e) {
            log.warn("Erro: " + e.getMessage());
        }
        log.println("");
        log.ok("Listagem de portas concluida.");
    }

    // ---------------------------------------------------------------
    // Configurar DNS personalizado
    // ---------------------------------------------------------------

    public static void configureDns(SystemInfo si, Logger log, String provider) {
        log.section("CONFIGURAR DNS - " + provider.toUpperCase());

        String primary, secondary;
        switch (provider.toLowerCase()) {
            case "google":
                primary = "8.8.8.8"; secondary = "8.8.4.4";
                break;
            case "cloudflare":
                primary = "1.1.1.1"; secondary = "1.0.0.1";
                break;
            case "opendns":
                primary = "208.67.222.222"; secondary = "208.67.220.220";
                break;
            case "quad9":
                primary = "9.9.9.9"; secondary = "149.112.112.112";
                break;
            case "auto":
                log.info("Restaurando DNS automatico (DHCP) em todos os adaptadores...");
                restoreDnsAuto(log);
                return;
            default:
                // Permitir DNS customizado "1.2.3.4"
                String[] parts = provider.split(",");
                primary   = parts[0].trim();
                secondary = parts.length > 1 ? parts[1].trim() : "8.8.8.8";
        }

        log.println("  Servidor primario  : " + primary);
        log.println("  Servidor secundario: " + secondary);
        log.println("");

        log.info("Aplicando DNS em todos os adaptadores de rede ativos...");
        try {
            // Listar adaptadores via netsh
            Process p = new ProcessBuilder("netsh", "interface", "show", "interface")
                .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            List<String> adapters = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("Connected") || line.contains("Conectado")) {
                    // Extrair nome do adaptador (ultima coluna)
                    String[] cols = line.trim().split("\\s{2,}");
                    if (cols.length >= 4) {
                        adapters.add(cols[cols.length - 1]);
                    }
                }
            }
            p.waitFor();

            if (adapters.isEmpty()) {
                log.warn("Nenhum adaptador de rede ativo encontrado.");
            }

            for (String adapter : adapters) {
                log.info("Adaptador: " + adapter);
                Utils.exec("netsh", "interface", "ipv4", "set", "dnsservers",
                    "name=" + adapter, "static", primary, "primary");
                Utils.exec("netsh", "interface", "ipv4", "add", "dnsservers",
                    "name=" + adapter, secondary, "index=2");
            }
        } catch (Exception e) {
            log.warn("Erro ao configurar DNS: " + e.getMessage());
        }

        Utils.exec("ipconfig", "/flushdns");
        log.ok("DNS configurado: " + primary + " / " + secondary);
        log.println("  Para restaurar ao DHCP: --net-dns-set auto");
    }

    private static void restoreDnsAuto(Logger log) {
        try {
            Process p = new ProcessBuilder("netsh", "interface", "show", "interface")
                .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            List<String> adapters = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = line.trim().split("\\s{2,}");
                if (cols.length >= 4 &&
                    (line.contains("Connected") || line.contains("Conectado"))) {
                    adapters.add(cols[cols.length - 1]);
                }
            }
            p.waitFor();
            for (String adapter : adapters) {
                Utils.exec("netsh", "interface", "ipv4", "set", "dnsservers",
                    "name=" + adapter, "dhcp");
            }
        } catch (Exception ignored) {}
        Utils.exec("ipconfig", "/flushdns");
        log.ok("DNS restaurado para automatico (DHCP).");
    }

    // ---------------------------------------------------------------
    // Limpar perfis de Wi-Fi salvos
    // ---------------------------------------------------------------

    public static void manageWifiProfiles(Logger log, Scanner sc) {
        log.section("GERENCIAR PERFIS WI-FI SALVOS");

        // Listar perfis
        log.println("  Perfis de rede Wi-Fi salvos neste computador:");
        log.println("");

        List<String> profiles = new ArrayList<>();
        try {
            Process p = new ProcessBuilder("netsh", "wlan", "show", "profiles")
                .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                // "All User Profile     : NetworkName"
                if (line.contains(":")) {
                    String val = line.substring(line.lastIndexOf(':') + 1).trim();
                    if (!val.isEmpty() && !val.equalsIgnoreCase("NONE") &&
                        !line.toLowerCase().contains("interface")) {
                        profiles.add(val);
                    }
                }
            }
            p.waitFor();
        } catch (Exception e) {
            log.warn("Erro ao listar perfis Wi-Fi: " + e.getMessage());
        }

        if (profiles.isEmpty()) {
            log.println("  Nenhum perfil Wi-Fi encontrado.");
            return;
        }

        for (int i = 0; i < profiles.size(); i++) {
            log.println("  [" + (i+1) + "] " + profiles.get(i));
        }
        log.println("  [A] Remover TODOS os perfis");
        log.println("  [0] Cancelar");
        log.println("");

        if (!Config.silent && sc != null) {
            System.out.print("  Escolha: ");
            String choice = sc.nextLine().trim();

            if (choice.equalsIgnoreCase("A")) {
                log.info("Removendo todos os perfis Wi-Fi...");
                Utils.exec("netsh", "wlan", "delete", "profile", "name=*");
                log.ok("Todos os perfis Wi-Fi removidos.");
            } else if (!choice.equals("0")) {
                try {
                    int idx = Integer.parseInt(choice) - 1;
                    if (idx >= 0 && idx < profiles.size()) {
                        String name = profiles.get(idx);
                        Utils.exec("netsh", "wlan", "delete", "profile", "name=" + name);
                        log.ok("Perfil removido: " + name);
                    }
                } catch (NumberFormatException ignored) {
                    log.warn("Opcao invalida.");
                }
            }
        } else {
            log.println("  (Modo silencioso — nenhuma remocao realizada sem confirmacao)");
        }
    }

    // ---------------------------------------------------------------
    // Gerenciar adaptadores de rede
    // ---------------------------------------------------------------

    public static void manageAdapters(Logger log, Scanner sc) {
        log.section("ADAPTADORES DE REDE");

        log.println("  Status dos adaptadores:");
        log.println("");
        Utils.execPrint("netsh", "interface", "show", "interface");
        log.println("");

        if (!Config.silent && sc != null) {
            log.println("  Opcoes:");
            log.println("  [1] Desabilitar adaptador por nome");
            log.println("  [2] Habilitar adaptador por nome");
            log.println("  [0] Voltar");
            System.out.print("  Escolha: ");
            String choice = sc.nextLine().trim();

            if (choice.equals("1") || choice.equals("2")) {
                System.out.print("  Nome do adaptador: ");
                String name = sc.nextLine().trim();
                if (!name.isEmpty()) {
                    if (choice.equals("1")) {
                        Utils.exec("netsh", "interface", "set", "interface",
                            "name=" + name, "admin=disabled");
                        log.ok("Adaptador desabilitado: " + name);
                    } else {
                        Utils.exec("netsh", "interface", "set", "interface",
                            "name=" + name, "admin=enabled");
                        log.ok("Adaptador habilitado: " + name);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Redefinir proxy
    // ---------------------------------------------------------------

    public static void resetProxy(Logger log) {
        log.section("REDEFINIR CONFIGURACOES DE PROXY");

        log.info("Resetando proxy WinHTTP...");
        Utils.exec("netsh", "winhttp", "reset", "proxy");

        log.info("Desabilitando proxy no registro (IE/sistema)...");
        Utils.reg("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings",
            "ProxyEnable", "REG_DWORD", "0");
        Utils.regDeleteValue("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings",
            "ProxyServer");
        Utils.regDeleteValue("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings",
            "ProxyOverride");

        log.info("Limpando PAC automático...");
        Utils.regDeleteValue("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings",
            "AutoConfigURL");

        Utils.exec("ipconfig", "/flushdns");
        log.ok("Configuracoes de proxy redefinidas.");
    }

    // ---------------------------------------------------------------
    // Traceroute
    // ---------------------------------------------------------------

    public static void traceroute(Logger log, String target) {
        log.section("TRACEROUTE PARA " + target);
        log.println("  Rastreando rota de pacotes (pode demorar ate 30 segundos)...");
        log.println("");
        Utils.execPrint("tracert", "-d", "-h", "30", target);
        log.println("");
        log.ok("Traceroute concluido.");
    }

    // ---------------------------------------------------------------
    // Informacoes completas de rede
    // ---------------------------------------------------------------

    public static void showNetInfo(Logger log) {
        log.section("INFORMACOES COMPLETAS DE REDE");

        log.println("  --- Configuracao IP (ipconfig /all) ---");
        Utils.execPrint("ipconfig", "/all");
        log.println("");

        log.println("  --- Tabela de rotas ---");
        Utils.execPrint("route", "print");
        log.println("");

        log.println("  --- Tabela ARP ---");
        Utils.execPrint("arp", "-a");
        log.println("");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static String extractLatency(String pingOutput) {
        // Buscar "Medio = Xms" ou "Average = Xms"
        String[] lines = pingOutput.split("\n");
        for (String line : lines) {
            if (line.contains("Medio") || line.contains("Average") ||
                line.contains("Media") || line.contains("Avg")) {
                String[] parts = line.split("=");
                if (parts.length > 0) {
                    String last = parts[parts.length - 1].trim();
                    return last.replace("ms","").trim();
                }
            }
        }
        return "";
    }

    private static Map<String,String> getPidToName() {
        Map<String,String> map = new HashMap<>();
        try {
            Process p = new ProcessBuilder("tasklist", "/FO", "CSV", "/NH")
                .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                // "notepad.exe","12345","Console","1","4 MB"
                String[] parts = line.split("\",\"");
                if (parts.length >= 2) {
                    String name = parts[0].replace("\"","");
                    String pid  = parts[1].replace("\"","");
                    map.put(pid, name);
                }
            }
            p.waitFor();
        } catch (Exception ignored) {}
        return map;
    }

    private static String rep(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }
}
