import java.io.*;
import java.util.regex.*;

/**
 * Detecta versao do Windows e define os caminhos de ambiente
 * com compatibilidade retroativa ate o Windows XP.
 */
public final class SystemInfo {

    public final int    winMajor;
    public final int    winMinor;
    public final int    winBuild;
    public final String winName;
    public final boolean admin;

    // Caminhos de ambiente (com fallback para XP)
    public final String localAppData;
    public final String appData;
    public final String programData;
    public final String userProfile;
    public final String temp;
    public final String systemRoot;
    public final String systemDrive;
    public final String computerName;
    public final String userName;

    public SystemInfo() {
        userProfile  = env("USERPROFILE",  "C:\\Users\\Default");
        systemRoot   = env("SystemRoot",   "C:\\Windows");
        systemDrive  = env("SystemDrive",  "C:");
        appData      = env("APPDATA",      userProfile + "\\AppData\\Roaming");
        temp         = env("TEMP",         systemRoot  + "\\Temp");
        computerName = env("COMPUTERNAME", "PC");
        userName     = env("USERNAME",     "Usuario");

        // LOCALAPPDATA nao existe no XP
        String lad = System.getenv("LOCALAPPDATA");
        localAppData = (lad != null)
            ? lad
            : userProfile + "\\Local Settings\\Application Data";

        // ProgramData nao existe no XP
        String pd = System.getenv("ProgramData");
        if (pd == null) {
            String ap = System.getenv("ALLUSERSPROFILE");
            pd = (ap != null) ? ap + "\\Application Data" : "C:\\ProgramData";
        }
        programData = pd;

        int[] ver = detectVersion();
        winMajor = ver[0];
        winMinor = ver[1];
        winBuild = ver[2];
        winName  = resolveName(winMajor, winMinor, winBuild);
        admin    = checkAdmin();
    }

    // --- versao via `ver` (mais confiavel que os.version no Win10+) ---
    private int[] detectVersion() {
        try {
            Process p = new ProcessBuilder("cmd.exe", "/c", "ver")
                .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            Pattern pat = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
            while ((line = br.readLine()) != null) {
                Matcher m = pat.matcher(line);
                if (m.find()) {
                    p.waitFor();
                    return new int[]{
                        Integer.parseInt(m.group(1)),
                        Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(3))
                    };
                }
            }
            p.waitFor();
        } catch (Exception ignored) {}

        // Fallback: propriedade da JVM
        String v = System.getProperty("os.version", "6.1.0");
        String[] parts = v.split("\\.");
        return new int[]{
            safeInt(parts, 0), safeInt(parts, 1), safeInt(parts, 2)
        };
    }

    private String resolveName(int major, int minor, int build) {
        if (major == 5) {
            if (minor == 0) return "Windows 2000";
            if (minor == 1) return "Windows XP";
            if (minor == 2) return "Windows XP x64 / Server 2003";
        } else if (major == 6) {
            if (minor == 0) return "Windows Vista";
            if (minor == 1) return "Windows 7";
            if (minor == 2) return "Windows 8";
            if (minor == 3) return "Windows 8.1";
        } else if (major == 10) {
            return (build >= 22000) ? "Windows 11" : "Windows 10";
        }
        return "Windows " + major + "." + minor;
    }

    private boolean checkAdmin() {
        try {
            Process p = new ProcessBuilder("net", "session")
                .redirectErrorStream(true).start();
            drain(p.getInputStream());
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // --- utilitarios estaticos ---

    static void drain(final InputStream is) {
        Thread t = new Thread(() -> {
            try { byte[] b = new byte[4096]; while (is.read(b) != -1) {} }
            catch (IOException ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v != null) ? v : fallback;
    }

    private static int safeInt(String[] arr, int idx) {
        try { return idx < arr.length ? Integer.parseInt(arr[idx].trim()) : 0; }
        catch (NumberFormatException e) { return 0; }
    }

    // --- predicados de versao ---
    public boolean isXP()             { return winMajor == 5; }
    public boolean isVistaPlus()      { return winMajor >= 6; }
    public boolean isWin7Plus()       { return winMajor > 6 || (winMajor == 6 && winMinor >= 1); }
    public boolean isWin8Plus()       { return winMajor > 6 || (winMajor == 6 && winMinor >= 2); }
    public boolean isWin10Plus()      { return winMajor >= 10; }
    public boolean supportsAnsi()     { return isWin10Plus(); }
    public boolean supportsDism()     { return isWin8Plus(); }
    public boolean supportsWevtutil() { return isVistaPlus(); }
}
