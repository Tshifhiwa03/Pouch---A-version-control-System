package pouchvcs.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * WordDocUtil.java - Utility class for handling Word Documents (.docx).
 * In this simplified VCS, we treat the Word document as a raw file,
 * reading its full byte content for hashing and storage.
 * * NOTE: For advanced features (like diffs), this is where Apache POI 
 * would be integrated to parse the XML structure of a DOCX file.
 */
public class WordDocUtil {

    /**
     * Reads the entire content of a file (expected to be a .docx) into a byte array.
     * This byte array is the content payload for a VCS Blob object.
     *
     * @param filePath The Path to the Word document.
     * @return The raw byte array content of the file.
     * @throws IOException If the file cannot be read.
     */
    public static byte[] readFileBytes(Path filePath) throws IOException {
        // Simple and robust way to read the full file content
        return Files.readAllBytes(filePath);
    }

    /**
     * Writes the byte content back to a specified file path.
     * Used primarily during the checkout operation.
     *
     * @param filePath The Path where the content should be written.
     * @param content The byte array content to write.
     * @throws IOException If the file cannot be written.
     */
    public static void writeFileBytes(Path filePath, byte[] content) throws IOException {
        // Ensure parent directories exist before writing
        if (filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath, content);
    }
}