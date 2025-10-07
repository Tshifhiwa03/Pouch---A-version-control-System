package pouchvcs.model;

import pouchvcs.util.HashUtil;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a Commit object, which is a snapshot of the repository at a specific time.
 * A commit contains metadata and references to a Tree object (project state) and 
 * its parent commit (history).
 */
public class Commit implements VCSObject, Serializable {

    private String hash;
    private final String treeHash; // SHA-256 hash of the top-level Tree object
    private final String parentHash; // SHA-256 hash of the previous Commit (null for initial commit)
    private final String author;
    private final LocalDateTime timestamp;
    private final String message;

    /**
     * Constructor for creating a new Commit.
     */
    public Commit(String treeHash, String parentHash, String author, String message) {
        this.treeHash = Objects.requireNonNull(treeHash, "Tree hash cannot be null.");
        this.parentHash = parentHash; // Can be null
        this.author = Objects.requireNonNull(author, "Author cannot be null.");
        this.message = Objects.requireNonNull(message, "Message cannot be null.");
        this.timestamp = LocalDateTime.now();
        
        // Calculate hash immediately after setting all content fields
        this.hash = calculateHash();
    }
    
    // Private constructor for deserialization (loading from disk)
    private Commit(String hash, String treeHash, String parentHash, String author, LocalDateTime timestamp, String message) {
        this.hash = hash;
        this.treeHash = treeHash;
        this.parentHash = parentHash;
        this.author = author;
        this.timestamp = timestamp;
        this.message = message;
        // Integrity check: Optional, but good practice to verify hash against content here
    }

    /**
     * Serializes the Commit object's metadata into a standard format (similar to Git)
     * and returns the byte array representation. This byte array is what is hashed and stored.
     */
    @Override
    public byte[] serialize() {
        // We construct a structured header and body, delimited by newlines.
        StringBuilder content = new StringBuilder();
        
        content.append("tree ").append(this.treeHash).append("\n");
        
        if (this.parentHash != null && !this.parentHash.isEmpty()) {
            content.append("parent ").append(this.parentHash).append("\n");
        }
        
        content.append("author ").append(this.author).append("\n");
        // Using ISO date format for consistent serialization
        content.append("date ").append(this.timestamp.toString()).append("\n");
        
        // Commit message is separated by an empty line
        content.append("\n").append(this.message).append("\n");
        
        return content.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * Calculates the SHA-256 hash based on the serialized content of the commit.
     * @return The calculated hash string.
     */
    private String calculateHash() {
        return HashUtil.sha256Hash(this.serialize());
    }

    /**
     * Deserializes raw bytes (from the repository) into a Commit object.
     */
    public static Commit deserialize(byte[] data) {
        String content = new String(data, StandardCharsets.UTF_8);
        String[] lines = content.split("\n");
        
        // Default values
        String treeHash = null;
        String parentHash = null;
        String author = null;
        LocalDateTime timestamp = null;
        String message = "";
        
        boolean messageStarted = false;
        
        for (String line : lines) {
            if (line.isEmpty() && !messageStarted) {
                // Separator between metadata and message
                messageStarted = true;
                continue;
            }
            
            if (!messageStarted) {
                // Process metadata fields
                String[] parts = line.split(" ", 2);
                if (parts.length < 2) continue; // Skip malformed lines
                String key = parts[0];
                String value = parts[1];
                
                switch (key) {
                    case "tree":
                        treeHash = value;
                        break;
                    case "parent":
                        parentHash = value;
                        break;
                    case "author":
                        author = value;
                        break;
                    case "date":
                        timestamp = LocalDateTime.parse(value);
                        break;
                }
            } else {
                // Accumulate the commit message body
                if (!message.isEmpty()) {
                    message += "\n";
                }
                message += line;
            }
        }
        
        // Recalculate the hash from the byte array to ensure we use the same hash 
        // that was used to store the file.
        String hash = HashUtil.sha256Hash(data);
        
        return new Commit(hash, treeHash, parentHash, author, timestamp, message);
    }

    @Override
    public String getHash() { return hash; }
    public String getTreeHash() { return treeHash; }
    public String getParentHash() { return parentHash; }
    public String getAuthor() { return author; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getMessage() { return message; }
}
