import java.io.*;
import java.nio.file.*;

/**
 * Suite de testes sem dependencias externas.
 * Cobre logica pura: Logger.fmt, validacoes de IP/horario/dia,
 * e comportamento de Utils.wipeDir / Utils.deleteGlob.
 *
 * Executar via: test.bat
 * Saida: [OK] / [FALHOU]; exit code 1 se qualquer teste falhar.
 */
public class TrashCleanerTest {

    private static int passou = 0;
    private static int falhou = 0;

    // ---------------------------------------------------------------
    // Runner
    // ---------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("=== TrashCleaner — Suite de Testes ===\n");

        testLoggerFmt();
        testIsValidIp();
        testIsValidTime();
        testIsValidDay();
        testWipeDir();
        testDeleteGlob();

        System.out.println("\n=== RESULTADO ===");
        System.out.println("  Passou : " + passou);
        System.out.println("  Falhou : " + falhou);

        if (falhou > 0) System.exit(1);
    }

    // ---------------------------------------------------------------
    // Logger.fmt — formatacao de bytes (pure)
    // ---------------------------------------------------------------

    static void testLoggerFmt() {
        System.out.println("--- Logger.fmt ---");

        assertEquals("0 bytes",          "0 B",      Logger.fmt(0));
        assertEquals("1 byte",           "1 B",      Logger.fmt(1));
        assertEquals("1023 bytes",       "1023 B",   Logger.fmt(1023));
        assertEquals("exato 1 KB",       "1.0 KB",   Logger.fmt(1024));
        assertEquals("1.5 KB",           "1.5 KB",   Logger.fmt(1536));
        assertEquals("1023 KB",          "1023.0 KB",Logger.fmt(1023L * 1024));
        assertEquals("exato 1 MB",       "1.0 MB",   Logger.fmt(1024L * 1024));
        assertEquals("1.5 MB",           "1.5 MB",   Logger.fmt(1024L * 1024 + 512 * 1024));
        assertEquals("exato 1 GB",       "1.00 GB",  Logger.fmt(1024L * 1024 * 1024));
        assertEquals("negativo pequeno", "N/A",      Logger.fmt(-1));
        assertEquals("negativo pequeno2","N/A",      Logger.fmt(-999_999));
        // negativo grande: -2 000 000 bytes -> representa "-X.Y MB"
        String neg2m = Logger.fmt(-2_000_000L);
        assertTrue("negativo grande comeca com -", neg2m.startsWith("-"));
    }

    // ---------------------------------------------------------------
    // NetworkTools.isValidIp — validacao de IPv4 (pure)
    // ---------------------------------------------------------------

    static void testIsValidIp() {
        System.out.println("--- NetworkTools.isValidIp ---");

        assertTrue("8.8.8.8 valido",             NetworkTools.isValidIp("8.8.8.8"));
        assertTrue("1.1.1.1 valido",             NetworkTools.isValidIp("1.1.1.1"));
        assertTrue("0.0.0.0 valido",             NetworkTools.isValidIp("0.0.0.0"));
        assertTrue("255.255.255.255 valido",     NetworkTools.isValidIp("255.255.255.255"));
        assertTrue("10.0.0.1 valido",            NetworkTools.isValidIp("10.0.0.1"));

        assertFalse("null invalido",             NetworkTools.isValidIp(null));
        assertFalse("vazio invalido",            NetworkTools.isValidIp(""));
        assertFalse("256.0.0.0 invalido",        NetworkTools.isValidIp("256.0.0.0"));
        assertFalse("1.2.3 (3 octetos)",         NetworkTools.isValidIp("1.2.3"));
        assertFalse("1.2.3.4.5 (5 octetos)",     NetworkTools.isValidIp("1.2.3.4.5"));
        assertFalse("texto invalido",            NetworkTools.isValidIp("google.com"));
        assertFalse("octet negativo",            NetworkTools.isValidIp("1.2.3.-1"));
        assertFalse("espacos invalidos",         NetworkTools.isValidIp("1.2.3. 4"));
        assertFalse("separador errado",          NetworkTools.isValidIp("1,2,3,4"));
    }

    // ---------------------------------------------------------------
    // AutoScheduler.isValidTime — formato HH:MM (pure)
    // ---------------------------------------------------------------

    static void testIsValidTime() {
        System.out.println("--- AutoScheduler.isValidTime ---");

        assertTrue("03:00 valido",  AutoScheduler.isValidTime("03:00"));
        assertTrue("00:00 valido",  AutoScheduler.isValidTime("00:00"));
        assertTrue("23:59 valido",  AutoScheduler.isValidTime("23:59"));
        assertTrue("12:30 valido",  AutoScheduler.isValidTime("12:30"));

        assertFalse("null invalido",   AutoScheduler.isValidTime(null));
        assertFalse("vazio invalido",  AutoScheduler.isValidTime(""));
        assertFalse("24:00 invalido",  AutoScheduler.isValidTime("24:00"));
        assertFalse("12:60 invalido",  AutoScheduler.isValidTime("12:60"));
        assertFalse("3:00 invalido",   AutoScheduler.isValidTime("3:00"));   // nao HH
        assertFalse("texto invalido",  AutoScheduler.isValidTime("abc"));
        assertFalse("sem separador",   AutoScheduler.isValidTime("0300"));
    }

    // ---------------------------------------------------------------
    // AutoScheduler.isValidDay — dias da semana (pure)
    // ---------------------------------------------------------------

    static void testIsValidDay() {
        System.out.println("--- AutoScheduler.isValidDay ---");

        assertTrue("MON valido",  AutoScheduler.isValidDay("MON"));
        assertTrue("TUE valido",  AutoScheduler.isValidDay("TUE"));
        assertTrue("WED valido",  AutoScheduler.isValidDay("WED"));
        assertTrue("THU valido",  AutoScheduler.isValidDay("THU"));
        assertTrue("FRI valido",  AutoScheduler.isValidDay("FRI"));
        assertTrue("SAT valido",  AutoScheduler.isValidDay("SAT"));
        assertTrue("SUN valido",  AutoScheduler.isValidDay("SUN"));
        assertTrue("minusculo ok", AutoScheduler.isValidDay("mon"));
        assertTrue("misto ok",    AutoScheduler.isValidDay("Wed"));

        assertFalse("null invalido",  AutoScheduler.isValidDay(null));
        assertFalse("vazio invalido", AutoScheduler.isValidDay(""));
        assertFalse("ABC invalido",   AutoScheduler.isValidDay("ABC"));
        assertFalse("MONDAY invalido",AutoScheduler.isValidDay("MONDAY"));
    }

    // ---------------------------------------------------------------
    // Utils.wipeDir — somente conta bytes apos delete() = true (I/O)
    // ---------------------------------------------------------------

    static void testWipeDir() {
        System.out.println("--- Utils.wipeDir ---");
        File tmp = null;
        try {
            tmp = Files.createTempDirectory("tc_test_").toFile();

            // Criar 3 arquivos de tamanhos conhecidos
            File f1 = newFile(tmp, "a.txt", 100);
            File f2 = newFile(tmp, "b.txt", 200);
            File f3 = newFile(tmp, "c.txt", 300);

            long esperado = f1.length() + f2.length() + f3.length();
            long liberado = Utils.wipeDir(tmp);

            assertLong("wipeDir libera soma correta dos arquivos", esperado, liberado);
            assertFalse("f1 apagado", f1.exists());
            assertFalse("f2 apagado", f2.exists());
            assertFalse("f3 apagado", f3.exists());

        } catch (IOException e) {
            fail("wipeDir setup IO", "(sem excecao)", e.getMessage());
        } finally {
            if (tmp != null && tmp.exists()) deleteRecursive(tmp);
        }
    }

    // ---------------------------------------------------------------
    // Utils.deleteGlob — deleta apenas arquivos que combinam (I/O)
    // ---------------------------------------------------------------

    static void testDeleteGlob() {
        System.out.println("--- Utils.deleteGlob ---");
        File tmp = null;
        try {
            tmp = Files.createTempDirectory("tc_glob_").toFile();

            File log1 = newFile(tmp, "sessao1.log", 10);
            File log2 = newFile(tmp, "sessao2.log", 10);
            File bat  = newFile(tmp, "script.bat",  10);
            File txt  = newFile(tmp, "notes.txt",   10);

            Utils.deleteGlob(tmp, "*.log");

            assertFalse("sessao1.log apagado", log1.exists());
            assertFalse("sessao2.log apagado", log2.exists());
            assertTrue("script.bat preservado", bat.exists());
            assertTrue("notes.txt preservado",  txt.exists());

        } catch (IOException e) {
            fail("deleteGlob setup IO", "(sem excecao)", e.getMessage());
        } finally {
            if (tmp != null && tmp.exists()) deleteRecursive(tmp);
        }
    }

    // ---------------------------------------------------------------
    // Helpers de asercao
    // ---------------------------------------------------------------

    static void assertEquals(String desc, String esperado, String obtido) {
        if (esperado.equals(obtido)) {
            passou++; System.out.println("  [OK] " + desc);
        } else {
            falhou++;
            System.out.println("  [FALHOU] " + desc);
            System.out.println("    Esperado : " + esperado);
            System.out.println("    Obtido   : " + obtido);
        }
    }

    static void assertLong(String desc, long esperado, long obtido) {
        if (esperado == obtido) {
            passou++; System.out.println("  [OK] " + desc);
        } else {
            falhou++;
            System.out.println("  [FALHOU] " + desc);
            System.out.println("    Esperado : " + esperado);
            System.out.println("    Obtido   : " + obtido);
        }
    }

    static void assertTrue(String desc, boolean cond) {
        if (cond) { passou++; System.out.println("  [OK] " + desc); }
        else      { falhou++; System.out.println("  [FALHOU] " + desc + " (esperado true)"); }
    }

    static void assertFalse(String desc, boolean cond) {
        if (!cond) { passou++; System.out.println("  [OK] " + desc); }
        else       { falhou++; System.out.println("  [FALHOU] " + desc + " (esperado false)"); }
    }

    static void fail(String desc, String esperado, String obtido) {
        falhou++;
        System.out.println("  [FALHOU] " + desc);
        System.out.println("    Esperado : " + esperado);
        System.out.println("    Obtido   : " + obtido);
    }

    // ---------------------------------------------------------------
    // Helpers de arquivo
    // ---------------------------------------------------------------

    private static File newFile(File dir, String name, int sizeBytes) throws IOException {
        File f = new File(dir, name);
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(new byte[sizeBytes]);
        }
        return f;
    }

    private static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) deleteRecursive(c);
        }
        f.delete();
    }
}
