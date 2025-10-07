package pouchvcs.service;

import pouchvcs.model.Blob;
import pouchvcs.model.Commit;
import pouchvcs.model.Tree;
import pouchvcs.model.VCSObject;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the Pouch repository (.pouch directory) and all storage/retrieval 
 * of VCS objects (Blobs, Trees, Commits) on the file system.
 */
public class PouchRepository {
    
    // Constants for the repository structure
    public static final String REPO_NAME = ".pouch";
    public static final String OBJECTS_DIR = REPO_NAME + "/objects";
    public static final String HEAD_FILE = REPO_NAME + "/HEAD";
    public static final String INDEX_FILE = REPO_NAME + "/INDEX"; // Staging area
    
    private final Path repoPath;
    private final Path objectsPath;
    
    private static final Logger LOGGER = Logger.getLogger(PouchRepository.class.getName());

    /**
     * Finds and initializes a PouchRepository based on the provided directory.
     * @param directory The starting directory (typically the student's project root).
     * @throws IOException If repository initialization or finding fails.
     */
    public PouchRepository(Path directory) throws IOException {
        Path repoRoot = findRepoRoot(directory);
        if (repoRoot == null) {
            // If repo is not found, we assume the user wants to initialize a new one here.
            this.repoPath = directory.resolve(REPO_NAME);
            this.objectsPath = directory.resolve(OBJECTS_DIR);
            initialize();
        } else {
            this.repoPath = repoRoot.resolve(REPO_NAME);
            this.objectsPath = repoRoot.resolve(OBJECTS_DIR);
        }
    }
    
    /**
     * Initializes the core directory structure (.pouch, objects, HEAD file).
     * This is equivalent to running 'git init'.
     */
    private void initialize() throws IOException {
        LOGGER.log(Level.INFO, "Initializing Pouch repository at: {0}", this.repoPath);
        
        // 1. Create the .pouch directory
        Files.createDirectories(this.repoPath);

        // 2. Create the objects directory
        Files.createDirectories(this.objectsPath);
        
        // 3. Create the HEAD file, pointing to the branch where commits are made (e.g., 'main')
        Files.writeString(this.repoPath.resolve(HEAD_FILE), "ref: refs/heads/main\n");

        LOGGER.log(Level.INFO, "Pouch repository structure created successfully.");
    }
    
    /**
     * Helper method to search upwards for the .pouch directory.
     * @param startDir The directory to begin the search.
     * @return The root Path of the repository, or null if not found.
     */
    private Path findRepoRoot(Path startDir) {
        Path current = startDir;
        while (current != null) {
            if (Files.exists(current.resolve(REPO_NAME))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * Stores a VCSObject in the object database and returns its hash.
     * The file system structure uses Git's concept: objects/XX/XXXX...
     * @param object The Blob, Tree, or Commit object to store.
     * @return The SHA-256 hash of the stored object.
     */
    public String storeObject(VCSObject object) throws IOException {
        String hash = object.getHash();
        byte[] data = object.serialize();
        
        // 1. Determine the path: first 2 chars for directory, remaining for filename
        String dirName = hash.substring(0, 2);
        String fileName = hash.substring(2);
        
        Path objectDir = this.objectsPath.resolve(dirName);
        Path objectFile = objectDir.resolve(fileName);
        
        // 2. Create the directory if it doesn't exist
        Files.createDirectories(objectDir);
        
        // 3. Write the content to the file
        // We only write the object if it doesn't already exist (content-addressability)
        if (!Files.exists(objectFile)) {
            Files.write(objectFile, data);
            LOGGER.log(Level.FINE, "Stored object with hash: {0}", hash);
        } else {
            LOGGER.log(Level.FINE, "Object with hash {0} already exists. Skipping write.", hash);
        }
        
        return hash;
    }

    /**
     * Loads a VCSObject from the repository given its hash and expected type.
     * Uses reflection to call the static deserialize method on the target class.
     * @param hash The SHA-256 hash of the object.
     * @param type The Class type of the object (Blob, Tree, or Commit).
     * @return The reconstructed VCSObject.
     * @throws IOException If the file read fails.
     * @throws RuntimeException If the object type's deserialize method cannot be found or invoked.
     */
    public <T extends VCSObject> T loadObject(String hash, Class<T> type) throws IOException {
        String dirName = hash.substring(0, 2);
        String fileName = hash.substring(2);
        Path objectFile = this.objectsPath.resolve(dirName).resolve(fileName);

        if (!Files.exists(objectFile)) {
            throw new IOException("Object not found in repository: " + hash);
        }
        
        // Read all bytes from the object file
        byte[] data = Files.readAllBytes(objectFile);

        try {
            // Use reflection to call the static 'deserialize' method on the class.
            // This is a common pattern for reconstructing VCS objects.
            Method deserializeMethod = type.getMethod("deserialize", byte[].class);
            
            // The method should be static, so we pass 'null' for the instance.
            @SuppressWarnings("unchecked")
            T object = (T) deserializeMethod.invoke(null, data);
            
            // Re-hash and verify the loaded content if necessary (for integrity checks)
            // For now, we trust the hash stored in the filename.
            
            return object;
        } catch (Exception e) {
            // Catch NoSuchMethodException, IllegalAccessException, InvocationTargetException
            LOGGER.log(Level.SEVERE, "Failed to deserialize object with hash: " + hash + " into type " + type.getSimpleName(), e);
            throw new RuntimeException("Failed to deserialize VCS object.", e);
        }
    }
    
    // Getters
    public Path getRepoPath() { return repoPath; }
    public Path getObjectsPath() { return objectsPath; }
    
    // --- Helper methods for HEAD and Refs will go here later ---

}
