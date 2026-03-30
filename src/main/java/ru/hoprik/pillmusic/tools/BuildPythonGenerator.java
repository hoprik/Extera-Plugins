package ru.hoprik.pillmusic.tools;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class BuildPythonGenerator {
    public static void main(String[] args) throws Exception {
        byte[] dexBytes = Files.readAllBytes(Paths.get("classes.dex"));
        String base64 = Base64.getEncoder().encodeToString(dexBytes);
        String template = new String(Files.readAllBytes(Paths.get("plugin.py")));

        StringBuilder formattedBase64 = new StringBuilder();
        formattedBase64.append("\"").append(base64.toString()).append("\"");
        formattedBase64.append("\ndex_hash = \"").append(hashSHA256(dexBytes)).append("\"");
        String buildContent = template.replace("# DEX_DATA_HERE #", formattedBase64.toString());

        Files.write(Paths.get("build.plugin"), buildContent.getBytes());

        System.out.println("✓ build.py успешно сгенерирован");
        System.out.println("✓ DEX размер: " + dexBytes.length + " байт");
        System.out.println("✓ Base64 размер: " + base64.length() + " символов");
    }

    private static String hashSHA256(byte[] data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}