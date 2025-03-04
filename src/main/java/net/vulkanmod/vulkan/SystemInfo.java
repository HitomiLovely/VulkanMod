package net.vulkanmod.vulkan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class SystemInfo {
    public static final String cpuInfo = getCPUNameSafely();
    public static String getCPUNameSafely() {
        String tmp = getCPUNameFromProc();
        if (tmp == null) {
            tmp = getCPUNameFromProp() + " (SoC)";
            if (tmp == null) {
                return "Unknown CPU";
            }
        }

        return tmp;
    }

    private static String getCPUNameFromProc() {
        try (Stream<String> lines = Files.lines(Paths.get("/proc/cpuinfo"))) {
            return lines.filter(line -> line.startsWith("Hardware") || line.startsWith("model name"))
                .reduce((f, s) -> f.startsWith("H") ? f : s)
                .map(line -> {
                    String value = line.split(":")[1].strip();
                    return value.startsWith("H") ? value + " (SoC)" : value;
                }).orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private static String getCPUNameFromProp() {
        Set<String> manufacturer = shellExec("getprop ro.soc.manufacturer");
        Set<String> model = shellExec("getprop ro.soc.model");

        if (manufacturer.size() == 0) {
            // manu is null but model not null -> model. ex: "MT6833V/NZA"
            if (model.size() >= 1) return model.iterator().next();
        } else {
            // manu not null but model is null -> manu + " CPU". ex: "Mediatek CPU"
            if (model.size() == 0) return manufacturer.iterator().next() + " CPU";
            // best case scenario
            // manu not null and model not null -> manu + model. ex: "Mediatek MT6833V/NZA"
            else return manufacturer.iterator().next() + " " + model.iterator().next();
        }

        // worst case scenario
        // manu is null and model is null
        return null;
    }

     private static Set<String> shellExec(String commands) {
        Set<String> lines = new HashSet<>();
        
        // idk
        String[] args = {
            "sh", "-c", "\"" + commands + "\""
        };
        
        try {
            ProcessBuilder procBuilder = new ProcessBuilder(args);
            procBuilder.redirectErrorStream(true);

            Process proc = procBuilder.start();
            try (
                BufferedReader stdin = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            ) {
                String tmp = null;            
                while ((tmp = stdin.readLine()) != null) lines.add(tmp.strip());
            } catch (IOException e1) { }
        } catch (IOException e) { }

        return lines;
    }
}
