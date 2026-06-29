/**
 * Operacoes de leitura e escrita no Registro do Windows.
 * Extraido de Utils para separar a responsabilidade de acesso ao registro
 * da execucao de processos e manipulacao de arquivos.
 */
public final class RegistryUtils {

    private RegistryUtils() {}

    /** Adiciona ou atualiza um valor no Registro. */
    public static void reg(String key, String name, String type, String value) {
        Utils.exec("reg", "add", key, "/v", name, "/t", type, "/d", value, "/f");
    }

    /** Remove uma chave inteira do Registro. */
    public static void regDelete(String key) {
        Utils.exec("reg", "delete", key, "/f");
    }

    /** Remove um valor especifico do Registro. */
    public static void regDeleteValue(String key, String name) {
        Utils.exec("reg", "delete", key, "/v", name, "/f");
    }

    /** Consulta um valor do Registro e retorna como String, ou null se nao encontrado. */
    public static String regQuery(String key, String name) {
        try {
            Process p = new ProcessBuilder("reg", "query", key, "/v", name)
                .redirectErrorStream(true).start();
            java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                String[] parts = line.split("\\s{2,}", 3);
                if (parts.length == 3 && parts[0].trim().equalsIgnoreCase(name)) {
                    p.waitFor();
                    return parts[2].trim();
                }
            }
            p.waitFor();
        } catch (Exception ignored) {}
        return null;
    }
}
