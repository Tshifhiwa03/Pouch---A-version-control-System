/*
 * PouchVCS.java
 *
 * This class acts as the main service layer for the Pouch Version Control System.
 * It orchestrates all high-level commands (init, add, commit, checkout) by interacting
 * with the PouchRepository (storage) and PouchIndex (staging).
 */
package pouchvcs.service;

import pouchvcs.model.Blob;
import pouchvcs.model.Commit;
import pouchvcs.model.Tree;
import pouchvcs.model.Tree.Entry;
import pouchvcs.util.HashUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PouchVCS {
    // --- Constants and Core Components ---
    private static final String POUCH_DIR = ".pouch";
    private static final String HEAD_FILE = POUCH_DIR + "/HEAD";
    private static final String WORKING_DIR_PATH = System.getProperty("user.dir");

    private final PouchRepository repository;
    private final PouchIndex index;

    public PouchVCS() throws IOException {
        Path pouchPath = Paths.get(WORKING_DIR_PATH, POUCH_DIR);
        
        // Initialize repository and index only if .pouch directory exists
        if (Files.exists(pouchPath)) {
            this.repository = new PouchRepository(WORKING_DIR_PATH);
            this.index = new PouchIndex(WORKING_DIR_PATH);
            this.index.load(); // Load staged files state
        } else {
            // If repository not initialized, these will be null until init() is called
            this.repository = null;
            this.index = null;
        }
    }

    /**
     * Initializes a new Pouch repository in the current working directory.
     * Creates the .pouch directory, objects folder, and the HEAD file.
     * @throws IOException if directory creation fails.
     */
    public void init() throws IOException {
        Path pouchPath = Paths.get(WORKING_DIR_PATH, POUCH_DIR);
        if (Files.exists(pouchPath)) {
            System.out.println("Pouch repository already initialized in " + WORKING_DIR_PATH);
            return;
        }

        // 1. Create directory structure
        Files.createDirectories(pouchPath);
        Files.createDirectories(Paths.get(WORKING_DIR_PATH, POUCH_DIR, "objects"));
        
        // 2. Create HEAD file pointing to a null commit (no history yet)
        Path headPath = Paths.get(WORKING_DIR_PATH, HEAD_FILE);
        Files.writeString(headPath, "ref: refs/heads/master"); // Standard Git head reference

        // Re-initialize components now that the directory exists
        // Note: In a real implementation, you'd handle this better, but for simplicity:
        // PouchVCS should probably be initialized after init is called, or designed to be injectable.
        // For now, we rely on the caller to handle exceptions if methods are called before init.

        System.out.println("Initialized empty Pouch repository in " + pouchPath.toAbsolutePath());
    }
    
    /**
     * Finds the absolute path of the root directory containing the .pouch folder.
     * This method simulates finding the project root, which is the current working directory
     * in this simple implementation.
     * @return The absolute path of the project root.
     */
    public Path findPouchRoot() {
        // For simplicity, we assume the current working directory is the root.
        // In a complex app, this would walk up the directory tree until it finds .pouch
        return Paths.get(WORKING_DIR_PATH);
    }

    /**
     * Adds a file to the staging area (index).
     * @param filePath The path to the file to be staged, relative to the working directory.
     * @throws IOException if file reading or repository storage fails.
     * @throws IllegalStateException if the repository is not initialized.
     */
    public void addFile(Path filePath) throws IOException, IllegalStateException {
        if (repository == null || index == null) {
            throw new IllegalStateException("Pouch repository not initialized. Run init() first.");
        }
        
        // 1. Read the file content
        byte[] content = Files.readAllBytes(filePath);

        // 2. Create a Blob object
        Blob blob = new Blob(content);
        
        // 3. Store the Blob in the repository (content-addressable storage)
        String hash = repository.storeObject(blob);
        
        // 4. Add the entry (file path and its hash) to the index
        index.addEntry(filePath, hash);
        index.save();
        
        System.out.println("Staged: " + filePath + " with hash " + hash);
    }

    /**
     * Creates a new commit based on the current staging area.
     * @param message The commit message.
     * @param author The commit author's name.
     * @throws IOException if file operations fail.
     * @throws IllegalStateException if index is empty or repository is not initialized.
     * @return The hash of the newly created Commit object.
     */
    public String commit(String message, String author) throws IOException, IllegalStateException {
        if (repository == null || index == null) {
            throw new IllegalStateException("Pouch repository not initialized. Run init() first.");
        }
        if (index.getEntries().isEmpty()) {
            throw new IllegalStateException("Nothing to commit, working directory clean.");
        }

        // 1. Build the Tree from the staged files in the index
        Tree rootTree = buildTreeFromIndex();
        String treeHash = repository.storeObject(rootTree);
        
        // 2. Get the hash of the current HEAD commit (this will be the parent)
        String parentHash = getHeadCommitHash();

        // 3. Create the Commit object
        Commit commit = new Commit(
            treeHash, 
            parentHash.isEmpty() ? null : parentHash, // Parent is null if first commit
            author, 
            message, 
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        
        // 4. Store the Commit object
        String commitHash = repository.storeObject(commit);

        // 5. Update HEAD to point to the new commit
        writeCommitRef(commitHash);
        
        // 6. Clear the staging area
        index.clearIndex();
        index.save();
        
        System.out.println("Committed! Hash: " + commitHash);
        return commitHash;
    }

    /**
     * Recursively builds the Tree structure from the flat list of staged files in the index.
     * @return The root Tree object representing the project snapshot.
     * @throws IOException
     */
    private Tree buildTreeFromIndex() throws IOException {
        // Group files by directory to build the tree hierarchy (simplified for single-level directories)
        Map<String, String> stagedEntries = index.getEntries();
        List<Entry> entries = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : stagedEntries.entrySet()) {
            Path path = Paths.get(entry.getKey());
            // For simplicity, we treat all staged files as entries in the root tree
            entries.add(new Entry(path.getFileName().toString(), entry.getValue(), "blob"));
        }

        // In a real Git, this method would recursively build sub-trees.
        // For Pouch, we assume all documents are tracked in the root for simplicity.
        return new Tree(entries);
    }

    /**
     * Retrieves the hash of the commit currently pointed to by HEAD.
     * @return The commit hash, or an empty string if it's the first commit.
     * @throws IOException
     */
    private String getHeadCommitHash() throws IOException {
        Path headPath = Paths.get(WORKING_DIR_PATH, HEAD_FILE);
        String headContent = Files.readString(headPath).trim();
        
        // This is a simplification; Git handles branches (refs/)
        if (headContent.startsWith("ref:")) {
            Path refPath = Paths.get(WORKING_DIR_PATH, POUCH_DIR, "refs", "heads", "master");
            if (Files.exists(refPath)) {
                return Files.readString(refPath).trim();
            }
            return ""; // Initial state, no commits yet
        }
        return headContent; // Direct commit hash (detached HEAD, simplified)
    }

    /**
     * Updates the reference that HEAD points to (simplified: always updates master branch).
     * @param commitHash The hash of the new commit.
     * @throws IOException
     */
    private void writeCommitRef(String commitHash) throws IOException {
        Path refDir = Paths.get(WORKING_DIR_PATH, POUCH_DIR, "refs", "heads");
        Files.createDirectories(refDir);
        Path refPath = refDir.resolve("master");
        Files.writeString(refPath, commitHash + "\n");
    }

    /**
     * Rolls back the working directory to the state of a specific commit.
     * @param commitHash The hash of the commit to checkout.
     * @throws IOException
     * @throws IllegalArgumentException if the commit hash is invalid.
     */
    public void checkout(String commitHash) throws IOException, IllegalArgumentException {
        if (repository == null) {
            throw new IllegalStateException("Pouch repository not initialized.");
        }
        
        // 1. Load the Commit object
        Commit targetCommit = repository.loadObject(commitHash, Commit.class);
        if (targetCommit == null) {
            throw new IllegalArgumentException("Invalid commit hash: " + commitHash);
        }

        // 2. Load the root Tree object
        Tree rootTree = repository.loadObject(targetCommit.getTreeHash(), Tree.class);
        
        Path rootPath = findPouchRoot();

        // 3. Clear the working directory (WARNING: This is destructive!)
        // In a real system, you'd check for uncommitted changes first.
        
        // Simple file recreation based on the Tree entries
        for (Entry entry : rootTree.getEntries()) {
            if (entry.getType().equals("blob")) {
                // Load the Blob content
                Blob blob = repository.loadObject(entry.getHash(), Blob.class);
                
                // Write the content back to the working directory file
                Path filePath = rootPath.resolve(entry.getName());
                Files.write(filePath, blob.getContent());
                System.out.println("Restored: " + entry.getName());
            }
        }
        
        // 4. Update the HEAD (Simplified)
        Path headPath = Paths.get(WORKING_DIR_PATH, HEAD_FILE);
        Files.writeString(headPath, commitHash + "\n");
        index.clearIndex();
        index.save();
        
        System.out.println("Checked out to commit: " + commitHash);
    }
    
    /**
     * Retrieves the history of commits starting from HEAD.
     * @return A list of Commit objects from newest to oldest.
     * @throws IOException
     * @throws IllegalStateException if repository not initialized.
     */
    public List<Commit> getHistory() throws IOException, IllegalStateException {
        if (repository == null) {
            throw new IllegalStateException("Pouch repository not initialized.");
        }
        List<Commit> history = new ArrayList<>();
        String currentHash = getHeadCommitHash();

        while (!currentHash.isEmpty()) {
            Commit commit = repository.loadObject(currentHash, Commit.class);
            if (commit == null) break; 
            
            history.add(commit);
            currentHash = commit.getParentHash(); // Move to the parent
        }
        
        return history;
    }
}
