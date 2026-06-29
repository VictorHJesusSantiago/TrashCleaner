import java.io.*;
import java.util.*;

/**
 * Exibe informacoes detalhadas do sistema: OS, CPU, GPU, RAM, discos, rede,
 * BIOS, programas instalados, drivers, atualizacoes e virtualizacao.
 */
public final class SystemInfoDisplay {

    private SystemInfoDisplay() {}

    public static void show(SystemInfo si, Logger log) {
        log.section("INFORMACOES DO SISTEMA");
        printOS(si, log);
        printBios(si, log);
        printCpu(log);
        printGpu(log);
        printRam(si, log);
        printDisks(si, log);
        printNetwork(si, log);
        printVirtualization(si, log);
        printMisc(si, log);
    }

    public static void showQuick(SystemInfo si, Logger log) {
        log.section("INFORMACOES DO SISTEMA (RESUMO)");
        printOS(si, log);
        printCpu(log);
        printRam(si, log);
        printDisks(si, log);
    }

    public static void showPrograms(SystemInfo si, Logger log) {
        log.section("PROGRAMAS INSTALADOS");
        log.println("  Coletando lista (pode demorar alguns segundos)...");
        log.println("");

        if (si.isVistaPlus()) {
            log.println("  --- Programas (64 bits) ---");
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "Get-ItemProperty HKLM:\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\* " +
                "| Where-Object {$_.DisplayName} | Sort-Object DisplayName " +
                "| Select-Object DisplayName,DisplayVersion,Publisher " +
                "| Format-Table -AutoSize");

            log.println("  --- Programas (32 bits em SO 64 bits) ---");
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "Get-ItemProperty 'HKLM:\\Software\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*' " +
                "| Where-Object {$_.DisplayName} | Sort-Object DisplayName " +
                "| Select-Object DisplayName,DisplayVersion,Publisher " +
                "| Format-Table -AutoSize");

            log.println("  --- Programas do usuario atual ---");
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "Get-ItemProperty HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\* " +
                "| Where-Object {$_.DisplayName} | Sort-Object DisplayName " +
                "| Select-Object DisplayName,DisplayVersion | Format-Table -AutoSize");
        } else {
            Utils.execPrint("wmic", "product", "get", "Name,Version,Vendor", "/FORMAT:TABLE");
        }
    }

    public static void showDrivers(SystemInfo si, Logger log) {
        log.section("DRIVERS INSTALADOS");
        log.println("  --- Todos os drivers (driverquery) ---");
        Utils.execPrint("driverquery", "/FO", "TABLE");

        if (si.isWin8Plus()) {
            log.println("");
            log.println("  --- Drivers de terceiros (nao-inbox) ---");
            Utils.execPrint("powershell", "-noprofile", "-Command",
                "try { Get-WindowsDriver -Online -All " +
                "| Where-Object {$_.Inbox -eq $false} " +
                "| Select-Object Driver,Version,Date,ProviderName " +
                "| Format-Table -AutoSize } catch { Write-Host 'N/A' }");
        }
    }

    public static void showUpdates(SystemInfo si, Logger log) {
        log.section("ATUALIZACOES INSTALADAS");

        if (si.isVistaPlus()) {
            log.println("  --- Historico de atualizacoes (WMIC) ---");
            Utils.execPrint("wmic", "qfe", "list", "brief", "/FORMAT:TABLE");

            if (si.isWin8Plus()) {
                log.println("");
                log.println("  --- Atualizacoes (PowerShell, ordem cronologica) ---");
                Utils.execPrint("powershell", "-noprofile", "-Command",
                    "Get-HotFix | Sort-Object InstalledOn -Descending " +
                    "| Select-Object HotFixID,InstalledOn,Description " +
                    "| Format-Table -AutoSize");
            }
        } else {
            Utils.execPrint("wmic", "qfe", "list", "brief");
        }
    }

    // ---------------------------------------------------------------
    // Secoes internas
    // ---------------------------------------------------------------

    private static void printOS(SystemInfo si, Logger log) {
        log.println("  SISTEMA OPERACIONAL");
        log.println("  " + line('-', 58));
        log.println("  Sistema      : " + si.winName);
        log.println("  Versao       : " + si.winMajor + "." + si.winMinor +
            "  (Build " + si.winBuild + ")");
        log.println("  Maquina      : " + si.computerName);
        log.println("  Usuario      : " + si.userName);
        log.println("  Admin        : " + (si.admin ? "Sim" : "Nao"));
        log.println("  Drive sistema: " + si.systemDrive);
        log.println("  Java         : " + System.getProperty("java.version") +
            " (" + System.getProperty("java.vendor") + ")");

        String installDate = wmicGet("os", "InstallDate");
        if (installDate != null && installDate.length() >= 8) {
            String d = installDate.substring(6,8) + "/" +
                installDate.substring(4,6) + "/" + installDate.substring(0,4);
            log.println("  Instalacao   : " + d);
        }

        String boot = wmicGet("os", "LastBootUpTime");
        if (boot != null && boot.length() >= 14) {
            String dy = boot.substring(6,8), mo = boot.substring(4,6), yr = boot.substring(0,4);
            String hr = boot.substring(8,10), mi = boot.substring(10,12), sc2 = boot.substring(12,14);
            log.println("  Ultimo boot  : " + dy + "/" + mo + "/" + yr +
                " " + hr + ":" + mi + ":" + sc2);
        }

        if (si.isVistaPlus()) {
            String key = Utils.execCapture("powershell", "-noprofile", "-Command",
                "try { (Get-WmiObject -query 'select * from SoftwareLicensingService').OA3xOriginalProductKey } catch {}");
            if (key != null) key = key.trim();
            if (key != null && key.length() >= 5) {
                String masked = "*****-*****-*****-*****-" + key.substring(key.length() - 5);
                log.println("  Chave prod.  : " + masked);
            }
        }
        log.println("");
    }

    private static void printBios(SystemInfo si, Logger log) {
        log.println("  BIOS / UEFI");
        log.println("  " + line('-', 58));
        try {
            Map<String,String> b = wmicListFirst("bios",
                "Manufacturer,Name,SMBIOSBIOSVersion,ReleaseDate");
            if (!b.isEmpty()) {
                log.println("  Fabricante : " + b.getOrDefault("Manufacturer", "?"));
                log.println("  Nome       : " + b.getOrDefault("Name", "?"));
                log.println("  Versao     : " + b.getOrDefault("SMBIOSBIOSVersion", "?"));
                String rd = b.getOrDefault("ReleaseDate", "");
                if (rd.length() >= 8) {
                    log.println("  Data       : " + rd.substring(6,8) + "/" +
                        rd.substring(4,6) + "/" + rd.substring(0,4));
                }
            }
        } catch (Exception ignored) {
            log.println("  (informacao nao disponivel)");
        }
        try {
            Map<String,String> mb = wmicListFirst("baseboard",
                "Manufacturer,Product,SerialNumber");
            if (!mb.isEmpty()) {
                log.println("  Placa-mae  : " + mb.getOrDefault("Manufacturer","?") +
                    " " + mb.getOrDefault("Product",""));
            }
        } catch (Exception ignored) {}
        log.println("");
    }

    private static void printCpu(Logger log) {
        log.println("  PROCESSADOR");
        log.println("  " + line('-', 58));
        try {
            Map<String,String> info = wmicListFirst("cpu",
                "Name,NumberOfCores,NumberOfLogicalProcessors,MaxClockSpeed,LoadPercentage,Architecture");
            if (!info.isEmpty()) {
                log.println("  Modelo     : " + info.getOrDefault("Name", "?"));
                log.println("  Nucleos    : " + info.getOrDefault("NumberOfCores", "?") +
                    " fisicos / " + info.getOrDefault("NumberOfLogicalProcessors", "?") + " logicos");
                String mhz = info.getOrDefault("MaxClockSpeed", "");
                if (!mhz.isEmpty()) {
                    double ghz = Double.parseDouble(mhz) / 1000.0;
                    log.println(String.format("  Clock max  : %.2f GHz", ghz));
                }
                String arch = info.getOrDefault("Architecture","");
                String archName = arch.equals("9") ? "x64" : arch.equals("0") ? "x86" :
                    arch.equals("12") ? "ARM64" : arch;
                if (!archName.isEmpty()) log.println("  Arquitetura: " + archName);
                String load = info.get("LoadPercentage");
                if (load != null && !load.isEmpty()) {
                    int pct = Integer.parseInt(load);
                    log.println("  Uso atual  : " + pct + "%  [" + bar(pct, 30) + "]");
                }
            }
        } catch (Exception ignored) {
            log.println("  (informacao nao disponivel)");
        }
        log.println("");
    }

    private static void printGpu(Logger log) {
        log.println("  GPU (PLACA DE VIDEO)");
        log.println("  " + line('-', 58));
        try {
            List<Map<String,String>> gpus = wmicListAll(
                "path Win32_VideoController",
                "Name,AdapterRAM,DriverVersion,CurrentHorizontalResolution,CurrentVerticalResolution");
            if (gpus.isEmpty()) {
                log.println("  (informacao nao disponivel)");
            } else {
                for (int i = 0; i < gpus.size(); i++) {
                    Map<String,String> g = gpus.get(i);
                    String name = g.getOrDefault("Name", "");
                    if (name.isEmpty()) continue;
                    log.println("  GPU " + (i+1) + "      : " + name);
                    String vram = g.getOrDefault("AdapterRAM","");
                    if (!vram.isEmpty()) {
                        try { log.println("  VRAM       : " + Logger.fmt(Long.parseLong(vram.trim()))); }
                        catch (NumberFormatException ignored2) {}
                    }
                    String drv = g.getOrDefault("DriverVersion","");
                    if (!drv.isEmpty()) log.println("  Driver     : " + drv);
                    String hw = g.getOrDefault("CurrentHorizontalResolution","");
                    String vr = g.getOrDefault("CurrentVerticalResolution","");
                    if (!hw.isEmpty() && !vr.isEmpty())
                        log.println("  Resolucao  : " + hw + " x " + vr);
                }
            }
        } catch (Exception ignored) {
            log.println("  (informacao nao disponivel)");
        }
        log.println("");
    }

    private static void printRam(SystemInfo si, Logger log) {
        log.println("  MEMORIA RAM");
        log.println("  " + line('-', 58));
        try {
            Map<String,String> info = wmicListFirst("os",
                "TotalVisibleMemorySize,FreePhysicalMemory,TotalVirtualMemorySize,FreeVirtualMemory");
            long totalKb = parseLong(info.get("TotalVisibleMemorySize"));
            long freeKb  = parseLong(info.get("FreePhysicalMemory"));
            long usedKb  = totalKb - freeKb;
            long vtKb    = parseLong(info.get("TotalVirtualMemorySize"));
            long vfKb    = parseLong(info.get("FreeVirtualMemory"));

            log.println("  Total      : " + Logger.fmt(totalKb * 1024L));
            log.println("  Usada      : " + Logger.fmt(usedKb  * 1024L));
            log.println("  Livre      : " + Logger.fmt(freeKb  * 1024L));
            if (totalKb > 0) {
                int pct = (int)((usedKb * 100L) / totalKb);
                log.println("  Uso        : " + pct + "%  [" + bar(pct, 30) + "]");
            }
            if (vtKb > 0) {
                log.println("  Virtual    : " + Logger.fmt((vtKb-vfKb)*1024L) +
                    " usados de " + Logger.fmt(vtKb*1024L));
            }
        } catch (Exception ignored) {}

        try {
            List<Map<String,String>> chips = wmicListAll("memorychip",
                "Speed,BankLabel,Capacity,Manufacturer");
            if (!chips.isEmpty()) {
                log.println("  Pentes:");
                for (Map<String,String> c : chips) {
                    String cap = c.getOrDefault("Capacity","");
                    if (cap.isEmpty() || cap.equals("0")) continue;
                    long capBytes = parseLong(cap);
                    String spd = c.getOrDefault("Speed","");
                    String bank = c.getOrDefault("BankLabel","");
                    String mfr = c.getOrDefault("Manufacturer","");
                    log.println("    " + bank + "  " + Logger.fmt(capBytes) +
                        (spd.isEmpty() ? "" : "  " + spd + " MHz") +
                        (mfr.isEmpty() || mfr.equalsIgnoreCase("unknown") ? "" : "  " + mfr));
                }
            }
        } catch (Exception ignored) {}
        log.println("");
    }

    private static void printDisks(SystemInfo si, Logger log) {
        log.println("  ARMAZENAMENTO");
        log.println("  " + line('-', 58));
        try {
            List<Map<String,String>> disks = wmicListAll("logicaldisk",
                "DeviceID,FreeSpace,Size,VolumeName,FileSystem,DriveType");
            for (Map<String,String> disk : disks) {
                String id    = disk.getOrDefault("DeviceID","?");
                String name  = disk.getOrDefault("VolumeName","");
                String fs    = disk.getOrDefault("FileSystem","");
                int    type  = (int) parseLong(disk.get("DriveType"));
                long   total = parseLong(disk.get("Size"));
                long   free  = parseLong(disk.get("FreeSpace"));
                if (total == 0) continue;
                String typeStr;
                switch(type) {
                    case 2: typeStr="Removivel"; break;
                    case 3: typeStr="Fixo";      break;
                    case 4: typeStr="Rede";      break;
                    case 5: typeStr="CD-ROM";    break;
                    default: typeStr="Outro";
                }
                long used = total - free;
                int pct = (int)((used*100L)/total);
                String label = id + (name.isEmpty() ? "" : " ["+name+"]");
                log.println(String.format("  %-14s %s livre de %s  %d%%",
                    label, Logger.fmt(free), Logger.fmt(total), pct));
                log.println(String.format("  %-14s [%s]  %s  %s",
                    "", bar(pct,30), typeStr, fs));
            }
        } catch (Exception ignored) {
            log.println("  " + si.systemDrive + "  Livre: " + Logger.fmt(Utils.getDiskFree(si.systemDrive)));
        }
        log.println("");
    }

    private static void printNetwork(SystemInfo si, Logger log) {
        log.println("  REDE");
        log.println("  " + line('-', 58));
        Utils.execPrint("ipconfig");
        log.println("");
    }

    private static void printVirtualization(SystemInfo si, Logger log) {
        log.println("  VIRTUALIZACAO / FIRMWARE");
        log.println("  " + line('-', 58));
        String model = wmicGet("computersystem","Model");
        String mfr   = wmicGet("computersystem","Manufacturer");
        if (model==null) model="";
        if (mfr==null)   mfr="";
        boolean inVm = model.toLowerCase().contains("virtual") ||
            mfr.toLowerCase().contains("vmware") || mfr.toLowerCase().contains("virtualbox") ||
            mfr.toLowerCase().contains("hyper-v") || mfr.toLowerCase().contains("qemu");
        log.println("  Fabricante   : " + (mfr.isEmpty() ? "?" : mfr));
        log.println("  Modelo       : " + (model.isEmpty() ? "?" : model));
        log.println("  Dentro de VM : " + (inVm ? "Sim (detectado)" : "Nao aparente"));

        if (si.isWin8Plus()) {
            String hvInfo = Utils.execCapture("powershell", "-noprofile", "-Command",
                "try{(Get-WmiObject -Class Win32_ComputerSystem).HypervisorPresent}catch{'N/A'}");
            if (hvInfo!=null) log.println("  Hypervisor   : " + hvInfo.trim());

            String sb = Utils.execCapture("powershell","-noprofile","-Command",
                "try{Confirm-SecureBootUEFI}catch{'N/A'}");
            if (sb!=null) {
                String s = sb.trim();
                log.println("  Secure Boot  : " + ("True".equalsIgnoreCase(s) ? "Habilitado" :
                    "False".equalsIgnoreCase(s) ? "Desabilitado" : s));
            }

            String tpm = Utils.execCapture("powershell","-noprofile","-Command",
                "try{$t=Get-Tpm;'Presente='+$t.TpmPresent+' Ativo='+$t.TpmEnabled}catch{'N/A'}");
            if (tpm!=null) log.println("  TPM          : " + tpm.trim());
        }
        log.println("");
    }

    private static void printMisc(SystemInfo si, Logger log) {
        log.println("  JVM");
        log.println("  " + line('-', 58));
        Runtime rt = Runtime.getRuntime();
        log.println("  Processadores : " + rt.availableProcessors());
        log.println("  Heap JVM      : " + Logger.fmt(rt.totalMemory()) +
            "  (" + Logger.fmt(rt.freeMemory()) + " livre)");
        log.println("  Arch JVM      : " + System.getProperty("os.arch"));
        log.println("");
    }

    // ---------------------------------------------------------------
    // WMIC helpers
    // ---------------------------------------------------------------

    static String wmicGet(String entity, String field) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("wmic");
            for (String t : entity.split("\\s+")) cmd.add(t);
            cmd.add("get"); cmd.add(field); cmd.add("/VALUE");
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String ln;
            while ((ln = br.readLine()) != null) {
                int eq = ln.indexOf('=');
                if (eq >= 0) {
                    String val = ln.substring(eq+1).trim();
                    if (!val.isEmpty()) { p.waitFor(); return val; }
                }
            }
            p.waitFor();
        } catch (Exception ignored) {}
        return null;
    }

    static Map<String,String> wmicListFirst(String entity, String fields) {
        List<Map<String,String>> all = wmicListAll(entity, fields);
        return all.isEmpty() ? new LinkedHashMap<>() : all.get(0);
    }

    static List<Map<String,String>> wmicListAll(String entity, String fields) {
        List<Map<String,String>> result = new ArrayList<>();
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("wmic");
            for (String t : entity.split("\\s+")) cmd.add(t);
            cmd.add("get"); cmd.add(fields); cmd.add("/FORMAT:LIST");
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            Map<String,String> current = new LinkedHashMap<>();
            String ln;
            while ((ln = br.readLine()) != null) {
                int eq = ln.indexOf('=');
                if (eq > 0) {
                    current.put(ln.substring(0,eq).trim(), ln.substring(eq+1).trim());
                } else if (ln.trim().isEmpty() && !current.isEmpty()) {
                    result.add(current);
                    current = new LinkedHashMap<>();
                }
            }
            if (!current.isEmpty()) result.add(current);
            p.waitFor();
        } catch (Exception ignored) {}
        return result;
    }

    static long parseLong(String s) {
        if (s==null||s.isEmpty()) return 0L;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return 0L; }
    }

    static String bar(int pct, int width) {
        pct = Math.max(0,Math.min(100,pct));
        int filled = (pct*width)/100;
        StringBuilder sb = new StringBuilder(width);
        for (int i=0; i<width; i++) sb.append(i<filled ? '#' : '-');
        return sb.toString();
    }

    static String line(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i=0; i<n; i++) sb.append(c);
        return sb.toString();
    }
}
