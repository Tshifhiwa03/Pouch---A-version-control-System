package pouchvcs.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for generating secure SHA-256 hashes of content.
 * This is fundamental to Pouch's content-addressable storage model.
 */
public class HashUtil {

    /**
     * Generates a SHA-256 hash string for the given byte array content.
     * * @param content The raw byte content (e.g., from a file, Tree, or Commit object).
     * @return The 64-character hexadecimal SHA-256 hash string.
     * @throws RuntimeException if the SHA-256 algorithm is not available (highly unlikely).
     */
    public static String sha256Hash(byte[] content) {
        try {
            // 1. Get an instance of the SHA-256 message digest algorithm.
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // 2. Compute the hash digest of the content bytes.
            byte[] hashBytes = digest.digest(content);
            
            // 3. Convert the byte array into a hexadecimal string representation.
            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                // Convert the byte to an integer, mask with 0xff to handle negative values,
                // and convert to a hex string.
                String hex = Integer.toHexString(0xff & b);
                
                // Ensure two characters for every byte (e.g., 'f' becomes '0f').
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            // This is unlikely to happen as SHA-256 is a standard Java algorithm.
            throw new RuntimeException("SHA-256 algorithm not found.", e);
        }
    }
}
