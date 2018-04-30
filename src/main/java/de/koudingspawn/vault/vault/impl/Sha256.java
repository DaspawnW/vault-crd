package de.koudingspawn.vault.vault.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Sha256 {

    public static String generateSha256(String ... args) {
        try {
            StringBuilder sb = new StringBuilder();
            for (String arg : args) {
                sb.append(arg).append(";");
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();

            return null;
        }
    }

}
