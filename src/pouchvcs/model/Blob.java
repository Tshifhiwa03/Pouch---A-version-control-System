package pouchvcs.model;

import pouchvcs.util.HashUtil;
import java.io.Serializable;
import java.util.Arrays;

/**
 * Represents a Blob object, which stores the raw, compressed content 
 * of a file (in this case, a Word document) and its SHA-256 hash ID.
 * Blobs are immutable; once created, their content (and hash) never changes.
 */
public class Blob implements VCSObject, Serializable {

    // Unique ID of the object, derived from its content.
    private String hash; 
    
    // The raw byte content of the Word document file.
    private final byte[] content; 
    
    /**
     * Constructor for creating a new Blob from file content.
     * @param content The byte array of the file data.
     */
    public Blob(byte[] content) {
        this.content = content;
        // The hash is calculated immediately based on the content.
        this.hash = HashUtil.sha256Hash(content);
    }
    
    /**
     * Reconstructs a Blob from serialized data (used when loading from the repository).
     * The hash must be recalculated to verify integrity.
     * @param content The raw byte array loaded from the repository.
     */
    public Blob(byte[] content, boolean fromRepository) {
        this.content = content;
        // Verify the hash when loading from the repository.
        this.hash = HashUtil.sha256Hash(content); 
    }

    /**
     * @return The unique SHA-256 hash that identifies this Blob.
     */
    @Override
    public String getHash() {
        return hash;
    }

    /**
     * @return The raw byte content of the file.
     */
    public byte[] getContent() {
        return content;
    }
    
    /**
     * Serializes the object into a byte array for storage in the repository.
     * For a Blob, the serialized form is simply the raw file content.
     * @return The byte array representation of the object.
     */
    @Override
    public byte[] serialize() {
        // We might add a header in the future (e.g., 'blob 123\0' + content)
        // but for now, we'll keep it simple: the raw content bytes.
        return content;
    }

    /**
     * Deserializes raw bytes into a Blob object.
     * @param data The byte array loaded from the repository.
     * @return A new Blob object.
     */
    public static Blob deserialize(byte[] data) {
        // Use the dedicated constructor for loading from repository
        return new Blob(data, true);
    }

    // Standard equals and hashCode methods to correctly compare Blob objects
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Blob blob = (Blob) o;
        return Arrays.equals(content, blob.content);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(content);
    }
}
