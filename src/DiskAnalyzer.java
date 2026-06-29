import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Analisador de disco: top pastas, arquivos grandes, duplicados,
 * fragmentacao, CHKDSK agendado, shadow copies, backup e compressao NTFS.
 */
public final class DiskAnalyzer {

    private DiskAnalyzer() {}

    // ---------------------------------------------------------------
    // Top N pastas maiores
    // ---------------------------------------------------------------

    public static void topFolders(Logger log, String drive, int topN) {
        log.section("TOP " + topN + " PASTAS MAIORES EM " + drive);
        log.println("  Analisando... (pode demorar dependendo do tamanho do disco)");
        log.println("");

        File root = new File(drive + "\\");
        if (!root.exists()) {
            log.warn("Drive " + drive + " nao encontrado.");
            return;
        }

        // Mapear diretorios de primeiro nivel + segundo nivel para ter granularidade
        Map<String,Long> sizeMap = new LinkedHashMap<>();
        File[] firstLevel = root.listFiles(f -> f.isDirectory() && !isSystem(f));
        if (firstLevel == null) {
            log.warn("Sem permissao para listar " + drive);
            return;
        }

        int step = 0;
        for (File dir : firstLevel) {
            log.progress("Analisando " + dir.getName(), Math.min(95, (step * 100) / Math.max(1, firstLevel.length)));
            step++;
            try {
                File[] subs = dir.listFiles(f -> f.isDirectory());
                if (subs != null && subs.length > 0) {
                    // Adicionar subdiretorios individualmente para melhor granularidade
                    for (File sub : subs) {
                        long size = Utils.calcDirSize(sub);
                        if (size > 1024L * 1024) {  // apenas > 1 MB
                            sizeMap.put(sub.getAbsolutePath(), size);
                        }
                    }
                }
                // Tambem adicionar o proprio diretorio de nivel 1
                long dirSize = Utils.calcDirSize(dir);
                sizeMap.put(dir.getAbsolutePath(), dirSize);
            } catch (Exception ignored) {}
        }

        log.progressDone();

        // Ordenar por tamanho e mostrar top N
        List<Map.Entry<String,Long>> sorted = new ArrayList<>(sizeMap.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        log.println(String.format("  %-10s  %s", "Tamanho", "Pasta"));
        log.println("  " + Logger.rep('-', 65));
        int shown = 0;
        for (Map.Entry<String,Long> e : sorted) {
            if (shown++ >= topN) break;
            log.println(String.format("  %-10s  %s",
                Logger.fmt(e.getValue()), e.getKey()));
        }
        log.println("");
        log.ok("Analise de pastas concluida. " + sizeMap.size() + " diretorios verificados.");
    }

    // ---------------------------------------------------------------
    // Localizador de arquivos grandes
    // ---------------------------------------------------------------

    public static void findLargeFiles(Logger log, String drive, long minSizeMb, int topN) {
        log.section("ARQUIVOS GRANDES (>= " + minSizeMb + " MB) EM " + drive);
        log.println("  Escaneando... (pode demorar varios minutos em discos grandes)");
        log.println("");

        long minBytes = minSizeMb * 1024L * 1024L;
        File root = new File(drive + "\\");
        List<long[]> results = new ArrayList<>();  // [size, hash]
        List<String> paths   = new ArrayList<>();

        try {
            final int[] count = {0};
            Files.walkFileTree(root.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    long size = attrs.size();
                    if (size >= minBytes) {
                        results.add(new long[]{size});
                        paths.add(file.toString());
                        count[0]++;
                        if (count[0] % 10 == 0) {
                            log.progress("Encontrados " + count[0] + " arquivos grandes", 50);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (isSystemPath(dir.toString())) return FileVisitResult.SKIP_SUBTREE;
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {}

        log.progressDone();

        // Ordenar por tamanho (decrescente)
        List<int[]> indices = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            indices.add(new int[]{i});
        }
        indices.sort((a, b) -> Long.compare(results.get(b[0])[0], results.get(a[0])[0]));

        log.println("  Encontrados " + results.size() + " arquivo(s) >= " + minSizeMb + " MB");
        log.println("");
        log.println(String.format("  %-12s  %s", "Tamanho", "Arquivo"));
        log.println("  " + Logger.rep('-', 72));

        int shown = 0;
        for (int[] idx : indices) {
            if (shown++ >= topN) break;
            int i = idx[0];
            log.println(String.format("  %-12s  %s",
                Logger.fmt(results.get(i)[0]), paths.get(i)));
        }

        if (results.size() > topN) {
            log.println("  ... e mais " + (results.size() - topN) + " arquivo(s).");
        }
        log.println("");
        log.ok("Busca de arquivos grandes concluida.");
    }

    // ---------------------------------------------------------------
    // Localizador de arquivos duplicados (por nome + tamanho)
    // ---------------------------------------------------------------

    public static void findDuplicates(Logger log, String searchPath) {
        log.section("LOCALIZADOR DE ARQUIVOS DUPLICADOS");
        log.println("  Caminho: " + searchPath);
        log.println("  Escaneando (comparando por nome + tamanho)...");
        log.println("");

        File root = new File(searchPath);
        if (!root.exists()) {
            log.warn("Caminho nao encontrado: " + searchPath);
            return;
        }

        Map<String, List<String>> byNameSize = new LinkedHashMap<>();
        try {
            final int[] count = {0};
            Files.walkFileTree(root.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile() && attrs.size() > 1024) {
                        String key = file.getFileName().toString() + "|" + attrs.size();
                        byNameSize.computeIfAbsent(key, k -> new ArrayList<>())
                            .add(file.toString());
                        if (++count[0] % 500 == 0) {
                            log.progress("Analisados " + count[0] + " arquivos", 50);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {}

        log.progressDone();

        int dupeGroups = 0;
        long wastedSpace = 0L;
        for (Map.Entry<String, List<String>> e : byNameSize.entrySet()) {
            List<String> files = e.getValue();
            if (files.size() < 2) continue;

            dupeGroups++;
            String[] parts = e.getKey().split("\\|");
            long size = 0;
            try { size = Long.parseLong(parts[parts.length - 1]); } catch (Exception ignored) {}
            wastedSpace += size * (files.size() - 1);

            log.println("  [DUPLICADO] " + parts[0] + "  (" + Logger.fmt(size) + " x" + files.size() + ")");
            for (String f : files) {
                log.println("    -> " + f);
            }
            log.println("");
        }

        if (dupeGroups == 0) {
            log.ok("Nenhum arquivo duplicado encontrado (por nome + tamanho).");
        } else {
            log.println("  Grupos duplicados : " + dupeGroups);
            log.println("  Espaco desperdicado: " + Logger.fmt(wastedSpace) + " (estimativa)");
            log.println("");
            log.ok("Analise de duplicados concluida. Remova manualmente os desnecessarios.");
        }
    }

    // ---------------------------------------------------------------
    // Verificar fragmentacao (defrag /A)
    // ---------------------------------------------------------------

    public static void checkFragmentation(SystemInfo si, Logger log, String drive) {
        log.section("VERIFICAR FRAGMENTACAO DE " + drive);
        log.println("  Executando analise de fragmentacao (sem desfragmentar)...");
        log.println("");
        Utils.execPrint("defrag", drive, "/A", "/U", "/V");
        log.ok("Analise de fragmentacao concluida.");
        log.println("  Para desfragmentar: Otimizar Sistema > Defrag HDD");
    }

    // ---------------------------------------------------------------
    // Agendar CHKDSK no proximo boot
    // ---------------------------------------------------------------

    public static void scheduleChkdsk(Logger log, String drive) {
        log.section("AGENDAR CHKDSK EM " + drive);
        log.println("  CHKDSK sera executado no proximo reinicio do Windows.");
        log.println("  Ele verificara e corrigira erros no sistema de arquivos.");
        log.println("");

        // fsutil dirty set marca o volume como 'sujo', forcando CHKDSK no boot
        int r = Utils.exec("fsutil", "dirty", "set", drive);
        if (r == 0) {
            log.ok("CHKDSK agendado para o drive " + drive + " no proximo boot.");
        } else {
            // Metodo alternativo via chkntfs
            Utils.exec("chkntfs", "/c", drive);
            log.ok("CHKDSK agendado via chkntfs para " + drive + ".");
        }
        log.println("  Reinicie o computador para executar o CHKDSK.");
    }

    // ---------------------------------------------------------------
    // Listar e gerenciar Shadow Copies (VSS)
    // ---------------------------------------------------------------

    public static void listShadowCopies(SystemInfo si, Logger log) {
        log.section("SHADOW COPIES (VSS)");
        if (!si.isVistaPlus()) {
            log.warn("Shadow copies gerenciaveis somente no Vista+.");
            return;
        }
        log.println("  Shadow copies existentes:");
        log.println("");
        Utils.execPrint("vssadmin", "list", "shadows");
        log.println("");
        log.println("  Espaco utilizado pelas shadow copies:");
        Utils.execPrint("vssadmin", "list", "shadowstorage");
    }

    public static void deleteShadowCopies(SystemInfo si, Logger log, boolean allExceptNewest) {
        log.section("REMOVER SHADOW COPIES");
        if (!si.isVistaPlus()) {
            log.warn("Vista+ necessario.");
            return;
        }
        if (allExceptNewest) {
            log.info("Removendo shadow copies mais antigas (mantendo a mais recente por drive)...");
            for (char d = 'A'; d <= 'Z'; d++) {
                if (!new File(d + ":\\").exists()) continue;
                Utils.exec("vssadmin", "delete", "shadows",
                    "/for=" + d + ":", "/oldest", "/quiet");
            }
            log.ok("Shadow copies antigas removidas.");
        } else {
            log.info("Removendo TODAS as shadow copies...");
            log.warn("ATENCAO: Pontos de restauracao do sistema serao perdidos!");
            for (char d = 'A'; d <= 'Z'; d++) {
                if (!new File(d + ":\\").exists()) continue;
                Utils.exec("vssadmin", "delete", "shadows",
                    "/for=" + d + ":", "/all", "/quiet");
            }
            log.ok("Todas as shadow copies removidas.");
        }
    }

    // ---------------------------------------------------------------
    // Backup de arquivos importantes
    // ---------------------------------------------------------------

    public static void backupFiles(Logger log, String[] sourcePaths, String destDir) {
        log.section("BACKUP DE ARQUIVOS");
        log.println("  Destino: " + destDir);
        log.println("");

        File dest = new File(destDir);
        if (!Config.dryRun) dest.mkdirs();

        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        long total = 0L;
        int count  = 0;

        for (String src : sourcePaths) {
            File srcFile = new File(src);
            if (!srcFile.exists()) {
                log.warn("Nao encontrado: " + src);
                continue;
            }
            File dstFile = new File(dest, stamp + "_" + srcFile.getName());
            log.info("Copiando: " + srcFile.getName() + " ...");
            log.progress(srcFile.getName(), (count * 100) / Math.max(1, sourcePaths.length));

            long copied = Utils.copyDir(srcFile, dstFile);
            total += copied;
            count++;
            log.ok("  " + Logger.fmt(copied) + " copiados -> " + dstFile.getName());
        }

        log.progressDone();
        log.println("");
        log.println("  Total copiado : " + Logger.fmt(total));
        log.println("  Arquivos      : " + count + " de " + sourcePaths.length);
        log.ok("Backup concluido em: " + destDir);
    }

    // ---------------------------------------------------------------
    // Compressao NTFS
    // ---------------------------------------------------------------

    public static void compressFolder(Logger log, String path) {
        log.section("COMPRESSAO NTFS");
        log.println("  Pasta: " + path);
        log.println("  Aplicando compressao NTFS recursivamente...");
        log.println("  (Pode demorar varios minutos para pastas grandes)");
        log.println("");

        int r = Utils.exec("compact", "/c", "/s:" + path, "/a", "/i");
        if (r == 0) {
            log.ok("Compressao NTFS aplicada em: " + path);
            log.println("  Arquivos novos adicionados a esta pasta tambem serao comprimidos.");
        } else {
            log.warn("Compressao nao aplicada (talvez o volume nao seja NTFS ou sem permissao).");
        }
    }

    public static void decompressFolder(Logger log, String path) {
        log.section("DESCOMPRESSAO NTFS");
        log.info("Removendo compressao NTFS de: " + path);
        Utils.exec("compact", "/u", "/s:" + path, "/a", "/i");
        log.ok("Compressao NTFS removida de: " + path);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static boolean isSystem(File f) {
        String name = f.getName().toLowerCase();
        return name.equals("windows") || name.equals("$recycle.bin") ||
            name.equals("system volume information") || name.startsWith("$");
    }

    private static boolean isSystemPath(String path) {
        String lower = path.toLowerCase();
        return lower.contains("\\windows\\") ||
            lower.contains("\\$recycle.bin") ||
            lower.contains("\\system volume information");
    }
}
