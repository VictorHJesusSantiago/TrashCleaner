import java.io.*;

/**
 * Utilitarios compartilhados: execucao de processos, manipulacao de
 * arquivos e operacoes de registro.
 */
public final class Utils {

    private Utils() {}

    // ---------------------------------------------------------------
    // Execucao de processos
    // ---------------------------------------------------------------

    /** Executa um comando e aguarda termino. Retorna o exit code. */
    public static int exec(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();
            drain(p.getInputStream());
            return p.waitFor();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Executa um comando via cmd /c, imprime a saida no console e
     * aguarda termino. Usado para comandos cujo output o usuario precisa ver.
     */
    public static int execPrint(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();
            BufferedReader br = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("    " + line);
            }
            return p.waitFor();
        } catch (Exception e) {
            return -1;
        }
    }

    /** Drena o InputStream em thread daemon para nao bloquear o processo. */
    public static void drain(final InputStream is) {
        Thread t = new Thread(() -> {
            try {
                byte[] b = new byte[4096];
                while (is.read(b) != -1) {}
            } catch (IOException ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    // ---------------------------------------------------------------
    // Manipulacao de arquivos
    // ---------------------------------------------------------------

    /**
     * Apaga recursivamente o conteudo de um diretorio.
     * Arquivos em uso sao ignorados silenciosamente.
     * O diretorio raiz permanece (apenas o conteudo e removido).
     */
    public static long wipeDir(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return 0L;
        long freed = 0L;
        File[] files = dir.listFiles();
        if (files == null) return 0L;
        for (File f : files) {
            if (f.isDirectory()) {
                freed += wipeDir(f);
                f.delete();
            } else {
                freed += f.length();
                f.delete();
            }
        }
        return freed;
    }

    /** Apaga arquivos que combinam com um padrao glob simples (* e .). */
    public static void deleteGlob(File dir, String pattern) {
        if (dir == null || !dir.exists()) return;
        final String regex = pattern.replace(".", "\\.").replace("*", ".*");
        File[] matches = dir.listFiles((d, name) -> name.matches(regex));
        if (matches == null) return;
        for (File f : matches) {
            if (f.isDirectory()) { wipeDir(f); f.delete(); }
            else f.delete();
        }
    }

    /**
     * Para cada subdiretorio em root (tipicamente um diretorio de perfis
     * de browser), apaga os subdiretorios especificados.
     */
    public static void wipeBrowserProfiles(File root, String... subdirs) {
        if (root == null || !root.exists()) return;
        File[] profiles = root.listFiles(f -> f.isDirectory());
        if (profiles == null) return;
        for (File profile : profiles) {
            for (String sub : subdirs) {
                wipeDir(new File(profile, sub));
            }
        }
    }

    /** Retorna o espaco livre no drive informado (ex: "C:"). */
    public static long getDiskFree(String drive) {
        return new File(drive + "\\").getFreeSpace();
    }

    // ---------------------------------------------------------------
    // Registro do Windows
    // ---------------------------------------------------------------

    /** Adiciona ou atualiza um valor no Registro. */
    public static void reg(String key, String name, String type, String value) {
        exec("reg", "add", key, "/v", name, "/t", type, "/d", value, "/f");
    }

    /** Remove uma chave inteira do Registro. */
    public static void regDelete(String key) {
        exec("reg", "delete", key, "/f");
    }
}
