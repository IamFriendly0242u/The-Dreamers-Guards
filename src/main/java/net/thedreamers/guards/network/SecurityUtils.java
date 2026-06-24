package net.thedreamers.guards.network;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SecurityUtils {

    private static final String SECRET_KEY = "TheDreamersGuardsSecretEncryptionMatrixKey";

    public static String encrypt(String plainText) {
        try {
            byte[] textBytes = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = new byte[textBytes.length];
            for (int i = 0; i < textBytes.length; i++) {
                encrypted[i] = (byte) (textBytes[i] ^ keyBytes[i % keyBytes.length]);
            }
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            return "ENCRYPTION_ERROR";
        }
    }

    public static String decrypt(String encryptedData) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
            byte[] decrypted = new byte[decoded.length];
            for (int i = 0; i < decoded.length; i++) {
                decrypted[i] = (byte) (decoded[i] ^ keyBytes[i % keyBytes.length]);
            }
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "CORRUPTED_PACKET";
        }
    }
}