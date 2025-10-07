package pouchvcs.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the Pouch staging area (or index).
 * This class handles reading and writing the list of files and their associated
 * Blob hashes that are currently staged and ready for the next commit.
 */
public class PouchIndex {

    private static final Logger LOGGER = Logger.getLogger(PouchIndex.class.getName());
    private static final String INDEX_FILE_NAME = "INDEX";

    // Maps the file's relative path (key) to its Blob hash (value)
    private final Map<Path, String> stagedFiles;
    private final Path indexFilePath;

    /**
     * Constructs the PouchIndex manager.
     * The index file is assumed to reside in the .pouch directory.
     * @param pouchDir The path to the .pouch metadata directory.
     */
    public PouchIndex(Path pouchDir) {
        this.indexFilePath = pouchDir.resolve(INDEX_FILE_NAME);
        this.stagedFiles = new HashMap<>();
        load();
    }

    /**
     * Loads the contents of the INDEX file into memory (stagedFiles map).
     * Format of INDEX file: [blobHash] [filePath]
     */
    private void load() {
        if (!Files.exists(indexFilePath)) {
            LOGGER.log(Level.INFO, "Index file not found, starting with empty index.");
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(indexFilePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ", 2);
                if (parts.length == 2) {
                    String hash = parts[0];
                    // IMPORTANT: The path is stored as a String but converted back to Path
                    // for consistent internal use.
                    Path filePath = Paths.get(parts[1]);
                    stagedFiles.put(filePath, hash);
                } else {
                    LOGGER.log(Level.WARNING, "Skipping malformed index line: {0}", line);
                }
            }
            LOGGER.log(Level.INFO, "Index loaded successfully with {0} entries.", stagedFiles.size());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load index file.", e);
        }
    }

    /**
     * Writes the current contents of the stagedFiles map back to the INDEX file.
     */
    public void save() {
        try (BufferedWriter writer = Files.newBufferedWriter(indexFilePath, StandardCharsets.UTF_8)) {
            for (Map.Entry<Path, String> entry : stagedFiles.entrySet()) {
                // Format: [blobHash] [filePath]
                String line = entry.getValue() + " " + entry.getKey().toString();
                writer.write(line);
                writer.newLine();
            }
            LOGGER.log(Level.INFO, "Index saved successfully with {0} entries.", stagedFiles.size());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save index file.", e);
        }
    }

    /**
     * Adds a file entry to the staging area.
     * @param filePath The path to the file relative to the project root.
     * @param hash The SHA-256 hash of the file's content (the Blob hash).
     */
    public void addEntry(Path filePath, String hash) {
        stagedFiles.put(filePath, hash);
        save(); // Save immediately after modification
        LOGGER.log(Level.INFO, "Staged file: {0} with hash: {1}", new Object[]{filePath, hash});
    }

    /**
     * Clears all entries from the staging area. Should be called after a successful commit.
     */
    public void clearIndex() {
        stagedFiles.clear();
        save(); // Save the cleared index
        LOGGER.log(Level.INFO, "Index cleared after successful commit.");
    }

    /**
     * Gets an unmodifiable map of the currently staged files.
     * @return A map from file Path to its Blob hash.
     */
    public Map<Path, String> getEntries() {
        return Collections.unmodifiableMap(stagedFiles);
    }
}
