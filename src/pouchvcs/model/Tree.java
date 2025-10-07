package pouchvcs.model;

import pouchvcs.util.HashUtil;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Tree object, which models a directory structure (folder).
 * It maps file names to their corresponding Blob hashes and subdirectory names 
 * to their corresponding Tree hashes. This object forms the project snapshot 
 * referenced by a Commit.
 */
public class Tree implements VCSObject, Serializable {
    
    private static final Logger LOGGER = Logger.getLogger(Tree.class.getName());

    // Maps the file/directory name (key) to its corresponding Entry (value: hash and type).
    private final Map<String, Entry> entries;
    private String hash;
    
    /**
     * Simple inner class to hold the components of a tree entry.
     */
    public static class Entry implements Serializable {
        public final String type; // "blob" for files, "tree" for directories
        public final String hash; // The SHA-256 hash of the content (Blob) or structure (Tree)
        public final String name; // The name of the file or directory

        public Entry(String type, String hash, String name) {
            this.type = type;
            this.hash = hash;
            this.name = name;
        }
        
        // Helper method for serialization format: [type] [hash] [name]
        @Override
        public String toString() {
            return type + " " + hash + " " + name;
        }
    }

    /**
     * Constructor for creating a new Tree object.
     * @param entries A map of Tree.Entry objects representing the directory contents.
     */
    public Tree(Map<String, Entry> entries) {
        // Use an unmodifiable map to ensure immutability after creation
        this.entries = Collections.unmodifiableMap(new HashMap<>(entries));
        this.hash = calculateHash();
    }
    
    // Private constructor used for deserialization (loading from disk)
    private Tree(String calculatedHash, Map<String, Entry> entries) {
        this.hash = calculatedHash;
        this.entries = Collections.unmodifiableMap(entries);
    }
    
    /**
     * Calculates the SHA-256 hash based on the serialized content of the tree.
     * The sorting ensures the hash is always reproducible regardless of the map iteration order.
     * @return The calculated hash string.
     */
    private String calculateHash() {
        return HashUtil.sha256Hash(this.serialize());
    }

    /**
     * Serializes the Tree into a byte array for storage. 
     * Format: Sort entries by name, then list them line by line as: [type] [hash] [name]\n.
     */
    @Override
    public byte[] serialize() {
        StringBuilder content = new StringBuilder();
        
        // Sort entries by name before serialization to ensure consistent hash generation
        entries.values().stream()
               .sorted((e1, e2) -> e1.name.compareTo(e2.name))
               .forEach(entry -> content.append(entry.toString()).append("\n"));
        
        return content.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Deserializes raw bytes (from the repository) into a Tree object.
     */
    public static Tree deserialize(byte[] data) {
        String content = new String(data, StandardCharsets.UTF_8);
        Map<String, Entry> entries = new HashMap<>();
        
        for (String line : content.split("\n")) {
            if (line.trim().isEmpty()) continue;
            
            try {
                // Expected Format: [type] [hash] [name]
                String[] parts = line.split(" ", 3);
                if (parts.length == 3) {
                    String type = parts[0];
                    String hash = parts[1];
                    String name = parts[2];
                    
                    Entry entry = new Entry(type, hash, name);
                    entries.put(name, entry);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Skipping malformed tree entry line: {0}", line);
            }
        }
        
        // Recalculate hash for the deserialized content
        String calculatedHash = HashUtil.sha256Hash(data);
        return new Tree(calculatedHash, entries);
    }
    
    // Getters
    @Override
    public String getHash() { return hash; }
    
    /**
     * @return An unmodifiable map of the tree entries (name -> Entry).
     */
    public Map<String, Entry> getEntries() {
        return entries;
    }
    
    /**
     * Retrieves a specific entry by its name.
     */
    public Entry getEntry(String name) {
        return entries.get(name);
    }
}
