package PouchVCS;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * Repository - an object-oriented replacement for the old static Clone class.
 *
 * Responsibilities:
 * - Manage repository paths (.clone_/ equivalents)
 * - Scan working tree and produce FileMeta lists
 * - Store file contents (blobs) in an object store
 * - Create commits (serialized CloneUnit objects to maintain compatibility)
 * - Checkout commits (restore files from blobs)
 * - Provide simple log & status operations
 *
 * Notes:
 * - This class intentionally keeps a small, clear API. It is thread-safe for
 *   operations that use the internal executor for hashing but file-system
 *   operations are not globally synchronized; use external repo locking for
 *   concurrent GUI calls if needed.
 */
public class Repository {
    private final Path workTree;      // user project folder
    private final Path repoDir;       // hidden repo folder (.clone_<name> or .clone_)
    private final Path objectsDir;    // where blobs are stored
    private final Path commitsDir;    // where commit objects are stored
    private final Path headFile;      // file storing current HEAD hash
    private Path repoPath;
    private String repoName;
    private final List<FileMeta> trackedFiles = new ArrayList<>();
    private final List<String> historyEntries = new ArrayList<>();

    private final ExecutorService hashingPool;
    private final int HASH_THREADS = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), 4));

    /**
     * Create or open a repository for a given work tree. repoDirName is the name of hidden folder (eg. .clone_myproj)
     */
    public Repository(Path workTree, String repoDirName) throws IOException {
        this.workTree = workTree.toAbsolutePath().normalize();
        this.repoDir = workTree.resolve(repoDirName).toAbsolutePath().normalize();
        this.objectsDir = repoDir.resolve("objects");
        this.commitsDir = repoDir.resolve("commits");
        this.headFile = repoDir.resolve("HEAD");

        // create directories if missing
        if (!Files.exists(repoDir)) {
            Files.createDirectories(repoDir);
        }
        if (!Files.exists(objectsDir)) Files.createDirectories(objectsDir);
        if (!Files.exists(commitsDir)) Files.createDirectories(commitsDir);

        this.hashingPool = Executors.newFixedThreadPool(HASH_THREADS);
    }

    public Path getWorkTree() { return workTree; }
    public Path getRepoDir() { return repoDir; }

    /**
     * Shutdown the repository (stops internal thread pool).
     */
    public void shutdown() {
        hashingPool.shutdown();
    }

    /**
     * Scan the workTree and return a list of FileMeta representing files to track.
     * Ignores the repoDir itself and its contents.
     */
    public List<FileMeta> scan() throws IOException, InterruptedException, ExecutionException {
        final List<Path> files = new ArrayList<>();

        Files.walkFileTree(workTree, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                // skip files inside the repoDir
                if (file.startsWith(repoDir)) return FileVisitResult.CONTINUE;
                files.add(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (dir.equals(repoDir)) return FileVisitResult.SKIP_SUBTREE;
                return FileVisitResult.CONTINUE;
            }
        });

        if (files.isEmpty()) return Collections.emptyList();

        List<Future<FileMeta>> futures = new ArrayList<>();
        for (Path p : files) {
            futures.add(hashingPool.submit(() -> {
                byte[] data = Files.readAllBytes(p);
                String hash = sha256Hex(data);
                return new FileMeta(p.toAbsolutePath().toString(), hash);
            }));
        }

        List<FileMeta> metas = new ArrayList<>();
        for (Future<FileMeta> f : futures) {
            metas.add(f.get());
        }

        // sort by path to keep a deterministic order
        metas.sort((a,b) -> a.getFilePath().compareTo(b.getFilePath()));
        return metas;
    }

    /**
     * Store a blob in the object store. If already exists, it will not be rewritten.
     * Returns the SHA-256 hash of the data.
     */
    public String storeBlob(byte[] data) throws IOException {
        String hash = sha256Hex(data);
        Path objPath = objectsDir.resolve(hash + ".obj");
        if (!Files.exists(objPath)) {
            Files.write(objPath, data);
        }
        return hash;
    }

    /**
     * Read a blob from the object store by its hash.
     */
    public byte[] readBlob(String hash) throws IOException {
        Path objPath = objectsDir.resolve(hash + ".obj");
        if (!Files.exists(objPath)) throw new FileNotFoundException("Blob not found: " + hash);
        return Files.readAllBytes(objPath);
    }

    /**
     * Commit the provided file metas as a new CloneUnit (commit). Returns the commit hash.
     * The commit object is serialized using Java serialization into the commits directory.
     */
    public String commit(List<FileMeta> fileList) throws IOException, NoSuchAlgorithmException {
        // ensure blobs exist in object store
        for (FileMeta fm : fileList) {
            Path p = Paths.get(fm.getFilePath());
            if (Files.exists(p)) {
                byte[] data = Files.readAllBytes(p);
                // store blob (this will deduplicate by hash)
                storeBlob(data);
            }
        }

        // create a commit hash based on serialized file list + time
        // We'll serialize the list into bytes and hash it
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(fileList);
            oos.flush();
        }
        byte[] commitBytes = baos.toByteArray();
        String commitHash = sha256Hex(commitBytes);

        // create and save CloneUnit for backward compatibility
        CloneUnit unit = new CloneUnit(new ArrayList<>(fileList), commitHash);
        Path commitPath = commitsDir.resolve(commitHash + ".clone");
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(commitPath)))) {
            oos.writeObject(unit);
        }

        // update HEAD
        Files.writeString(headFile, commitHash, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return commitHash;
    }

    /**
     * Checkout a commit (restore files to working tree). This will overwrite existing files
     * in the working tree (except the repo directory). It will create parent directories as needed.
     */
    public void checkout(String commitHash) throws IOException, ClassNotFoundException {
        Path commitPath = commitsDir.resolve(commitHash + ".clone");
        if (!Files.exists(commitPath)) throw new FileNotFoundException("Commit not found: " + commitHash);

        CloneUnit unit;
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(commitPath)))) {
            unit = (CloneUnit) ois.readObject();
        }

        // Remove files that are present in working tree but not in commit
        // For simplicity we will only overwrite/create files in commit and leave others untouched.
        for (FileMeta fm : unit.getFileList()) {
            Path dest = Paths.get(fm.getFilePath());
            if (!dest.startsWith(workTree)) {
                // not inside working directory? Skip for safety
                continue;
            }
            Path parent = dest.getParent();
            if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
            byte[] data = readBlob(fm.getHashcode());
            // Atomic write: write to temp file then move
            Path tmp = parent.resolve(dest.getFileName().toString() + ".tmp");
            Files.write(tmp, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }

        // update HEAD
        Files.writeString(headFile, commitHash, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Return a list of commit hashes available in the repository (newest last).
     */
    public List<String> listCommits() throws IOException {
        if (!Files.exists(commitsDir)) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(commitsDir, "*.clone")) {
            for (Path p : ds) {
                String name = p.getFileName().toString();
                if (name.endsWith(".clone")) out.add(name.substring(0, name.length() - ".clone".length()));
            }
        }
        Collections.sort(out);
        return out;
    }

    /**
     * Read current HEAD commit hash if present, otherwise return null.
     */
    public String readHead() throws IOException {
        if (!Files.exists(headFile)) return null;
        return Files.readString(headFile).trim();
    }

    /**
     * Utility: compute SHA-256 as lowercase hex string
     */
    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
