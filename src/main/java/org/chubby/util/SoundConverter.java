package org.chubby.util;

import java.io.File;
import java.io.IOException;

public class SoundConverter {

    public static void main(String[] args) {
        File folder = new File("assets/dragonmod/sounds"); // change if needed
        processFolder(folder);
        System.out.println("Done.");
    }

    private static void processFolder(File folder) {
        if (!folder.exists()) return;

        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                processFolder(file);
            } else if (file.getName().toLowerCase().endsWith(".wav")) {
                convertToOgg(file);
            }
        }
    }

    private static void convertToOgg(File wavFile) {
        String oggPath = wavFile.getAbsolutePath().replace(".wav", ".ogg");

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-y", // overwrite if exists
                "-i", wavFile.getAbsolutePath(),
                oggPath
        );

        try {
            System.out.println("Converting: " + wavFile.getName());

            Process process = pb.inheritIO().start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                if (wavFile.delete()) {
                    System.out.println("✔ Converted & deleted: " + wavFile.getName());
                }
            } else {
                System.out.println("✖ Failed: " + wavFile.getName());
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}