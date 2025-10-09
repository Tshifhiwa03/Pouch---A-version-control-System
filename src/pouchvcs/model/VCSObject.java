package pouchvcs.model;

/**
 * VCSObject.java - Interface for all storable VCS objects: Blob, Tree, and Commit.
 * They must be serializable to bytes for storage and hashing.
 * * All concrete implementations must provide a way to calculate a unique ID 
 * (hash) based on the serialized content.
 */
public interface VCSObject {

    /**
     * Serializes the object's content and metadata (excluding the hash itself) 
     * into a byte array for storage and hashing.
     * * @return A byte array representing the storable content of the object.
     * @return 
     */
    byte[] serialize();

    /**
     * Deserializes a byte array back into the object's internal state.
     * This is used when loading an object from the VCS object store.
     * * @param content The byte array containing the serialized object data.
     * @param content
     */
    void deserialize(byte[] content);
    
    /**
     * Retrieves the unique identifier (hash, e.g., SHA-256) of the object.
     * This hash is typically calculated based on the output of the serialize() method.
     * * @return The hash string (e.g., a 64-character SHA-256 string).
     * @return 
     */
    String getHash();
}