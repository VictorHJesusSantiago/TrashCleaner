import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Utilitarios compartilhados: execucao de processos, manipulacao de
 * arquivos e operacoes de registro.
 * Suporte a modo dry-run via Config.dryRun.
 */
public final class Utils {

    private Utils() {}

    // ---------------------------------------------------------------
    // Execucao de processos
    // ---------------------------------------------------------------

    /** Executa um comando e aguarda termino. Retorna o exit code. */
    public static int exec(String... cmd) {
        if (Config.dryRun) {
            System.out.println("  [SIMULACAO] " + String.join(" ", cmd));
            return 0;
        }
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
     * Executa um comando e imprime a saida formatada no console.
     * Usado para comandos cujo output o usuario precisa ver.
     */
    public static int execPrint(String... cmd) {
        if (Config.dryRun) {
            System.out.println("  [SIMULACAO] " + String.join(" ", cmd));
            return 0;
        }
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

    /**
     * Executa um comando e retorna a saida como String.
     * Util para capturar resultados de comandos.
     */
    public static String execCapture(String... cmd) {
        if (Config.dryRun) return "";
        try {
            Process p = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();
            BufferedReader br = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            p.waitFor();
            return sb.toString();
        } catch (Exception e) {
            return "";
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
     * Em modo dry-run: apenas calcula o tamanho sem apagar.
     * Arquivos em uso sao ignorados silenciosamente.
     */
    public static long wipeDir(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return 0L;
        if (Config.dryRun) return calcDirSize(dir);

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

    /** Calcula o tamanho de um diretorio sem apagar nada. */
    public static long calcDirSize(File dir) {
        if (dir == null || !dir.exists()) return 0L;
        long size = 0L;
        try {
            final long[] total = {0L};
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    total[0] += attrs.size();
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
            size = total[0];
        } catch (IOException ignored) {}
        return size;
    }

    /** Apaga arquivos que combinam com um padrao glob simples (* e .). */
    public static void deleteGlob(File dir, String pattern) {
        if (dir == null || !dir.exists()) return;
        final String regex = pattern.replace(".", "\\.").replace("*", ".*");
        File[] matches = dir.listFiles((d, name) -> name.matches(regex));
        if (matches == null) return;
        for (File f : matches) {
            if (Config.dryRun) {
                System.out.println("  [SIMULACAO] delete " + f.getAbsolutePath());
                continue;
            }
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

    /** Copia um arquivo de src para dst, criando diretorios intermediarios. */
    public static boolean copyFile(File src, File dst) {
        if (!src.exists() || !src.isFile()) return false;
        if (Config.dryRun) {
            System.out.println("  [SIMULACAO] copy " + src + " -> " + dst);
            return true;
        }
        try {
            dst.getParentFile().mkdirs();
            Files.copy(src.toPath(), dst.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Copia recursivamente src para dst. Retorna total de bytes copiados. */
    public static long copyDir(File src, File dst) {
        if (src == null || !src.exists()) return 0L;
        long total = 0L;
        if (src.isFile()) {
            if (copyFile(src, dst)) total += src.length();
        } else {
            dst.mkdirs();
            File[] children = src.listFiles();
            if (children != null) {
                for (File child : children) {
                    total += copyDir(child, new File(dst, child.getName()));
                }
            }
        }
        return total;
    }

    /** Retorna o espaco livre no drive informado (ex: "C:"). */
    public static long getDiskFree(String drive) {
        return new File(drive + "\\").getFreeSpace();
    }

    /** Retorna o espaco total do drive informado. */
    public static long getDiskTotal(String drive) {
        return new File(drive + "\\").getTotalSpace();
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

    /** Remove um valor especifico do Registro. */
    public static void regDeleteValue(String key, String name) {
        exec("reg", "delete", key, "/v", name, "/f");
    }

    /** Consulta um valor do Registro e retorna como String, ou null se nao encontrado. */
    public static String regQuery(String key, String name) {
        try {
            Process p = new ProcessBuilder("reg", "query", key, "/v", name)
                .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
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

    // ---------------------------------------------------------------
    // Utilitarios gerais
    // ---------------------------------------------------------------

    /** Aguarda N milissegundos, ignorando interrupcoes. */
    public static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
