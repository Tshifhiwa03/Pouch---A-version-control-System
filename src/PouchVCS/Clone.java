package PouchVCS;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.stage.Stage;
import javafx.stage.DirectoryChooser;
import javafx.scene.control.TextInputDialog;
import java.util.Optional;



public class Clone {

    public static String targetFolderPath;
    public static String mainRepoPath;

    private static ArrayList<CloneUnit> cloneList = new ArrayList<>();
    public static ArrayList<FileMeta> lastCloneFileList = new ArrayList<>();
    public static ArrayList<FileMeta> currentFileList = new ArrayList<>();
    public static ArrayList<String> fileHashCodes = new ArrayList<>();

    public static final String YELLOW_COLOR = "\033[33;1m";
    public static final String BLUE_COLOR = "\033[34;1m";
    public static final String RED_COLOR = "\033[31;1m";
    public static final String RESET = "\033[0m";

    /** Initialize the clone repository */
     public static void startWithGUI(Stage primaryStage) throws IOException {
        // Step 1: Let the user choose the target folder
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder for New Repository");
        File selectedFolder = directoryChooser.showDialog(primaryStage);

        if (selectedFolder == null) {
            System.out.println("No folder selected. Repository creation canceled.");
            return;
        }

        targetFolderPath = selectedFolder.getAbsolutePath();

        // Step 2: Ask the user for the repository name
        TextInputDialog dialog = new TextInputDialog("MyRepo");
        dialog.setTitle("New Repository");
        dialog.setHeaderText("Enter repository name:");
        dialog.setContentText("Repository Name:");

        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent() || result.get().trim().isEmpty()) {
            System.out.println("No repository name provided. Repository creation canceled.");
            return;
        }

        String repoName = result.get().trim();

        // Step 3: Initialize the repo folders and files (same as CLI start)
        mainRepoPath = targetFolderPath + "/.clone_" + repoName + "/";
        String[] ignorePaths = {"", "clones", "clones/filedata", "madedata", ".ignoreclone"};
        for (String ignorePath : ignorePaths) {
            File fileRef = new File(mainRepoPath + ignorePath);
            fileRef.mkdirs();
        }

        String[] repoFiles = {"clones/cloneList.clone", "clones/headhash.clone", "uniqueclone.clone", "madedata/currentfilelist.clone"};
        for (String repoFile : repoFiles) {
            File file = new File(mainRepoPath + repoFile);
            file.createNewFile();
        }

        String repoNamePath = mainRepoPath + "clones/repoName.clone";
        writeFileContent(repoNamePath, repoName.getBytes());

        System.out.println("Repository '" + repoName + "' created at " + mainRepoPath);
    }


    /** Load the current file list */
    public static void getCurrentFileList() {
        String filePath = mainRepoPath + "madedata/currentfilelist.clone";
        File file = new File(filePath);

        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
                currentFileList = new ArrayList<>();
                writeFileContent(filePath, currentFileList);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create currentfilelist.clone", e);
            }
        }

        try {
            currentFileList = (ArrayList<FileMeta>) readFileContent(filePath);
        } catch (EOFException e) {
            currentFileList = new ArrayList<>();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Load or initialize clone list */
    public static void takeClones() throws IOException {
        String filePath = mainRepoPath + "clones/cloneList.clone";
        File cloneListFile = new File(filePath);

        if (!cloneListFile.exists()) {
            cloneListFile.getParentFile().mkdirs();
            cloneList = new ArrayList<>();
            writeFileContent(filePath, cloneList);
            return;
        }

        try {
            cloneList = (ArrayList<CloneUnit>) readFileContent(filePath);
        } catch (EOFException e) {
            cloneList = new ArrayList<>();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Commit files with title and description */
    public static void saveCommit(ArrayList<FileMeta> filesToCommit, String title, String description) throws IOException, NoSuchAlgorithmException {
        if (filesToCommit == null || filesToCommit.isEmpty()) return;

        currentFileList = filesToCommit;
        saveNewFiles();
        String hashCode = generateHashCode();

        CloneUnit newCloneUnit = new CloneUnit(currentFileList, hashCode, title, description);
        cloneList.add(newCloneUnit);

        writeFileContent(mainRepoPath + "clones/cloneList.clone", cloneList);
        setHeadClone(hashCode);
    }

    /** Save the files to the repository */
    public static void saveNewFiles() {
        String folderPathOfContent = mainRepoPath + "clones/filedata/";
        String filePath = mainRepoPath + "content-hashcodes/contenthashcodes.clone";

        try {
            fileHashCodes = (ArrayList<String>) readFileContent(filePath);
        } catch (IOException e) {
            fileHashCodes = new ArrayList<>();
        }

        try {
            for (FileMeta currentFile : currentFileList) {
                if (!fileHashCodes.contains(currentFile.getHashcode())) {
                    byte[] buffer = MyFileVisitor.getBytes(Paths.get(currentFile.getFilePath()));
                    Path targetPath = Paths.get(folderPathOfContent + currentFile.getHashcode() + ".clone");
                    MyFileVisitor.saveBytes(targetPath, buffer);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Show new, edited, deleted files for preview */
    public static void show(Path targetFolder) throws IOException {
        Files.walkFileTree(targetFolder, new MyFileVisitor());

        if (cloneList.size() == 0) showNewFiles(targetFolderPath);
        else {
            lastCloneFileList = cloneList.get(cloneList.size() - 1).getFileList();
            showNewFiles(targetFolderPath);
            showEditedFiles(targetFolderPath);
            showDeletedFiles(targetFolderPath);
        }
    }

    public static void showNewFiles(String targetFolderPath) {
        boolean firstTime = true;
        for (FileMeta file : currentFileList) {
            boolean found = cloneList.size() > 0 && lastCloneFileList.stream().anyMatch(f -> f.getFilePath().equals(file.getFilePath()));
            if (!found) {
                if (firstTime) {
                    System.out.println("\n\t" + BLUE_COLOR + "New Files\n");
                    firstTime = false;
                }
                System.out.println("\t" + file.getFilePath().replace(targetFolderPath + "/", ""));
            }
        }
    }

    public static void showEditedFiles(String targetFolderPath) {
        boolean firstTime = true;
        for (FileMeta file : currentFileList) {
            for (FileMeta lastFile : lastCloneFileList) {
                if (file.getFilePath().equals(lastFile.getFilePath()) && !file.getHashcode().equals(lastFile.getHashcode())) {
                    if (firstTime) {
                        System.out.println("\n\t" + YELLOW_COLOR + "Edited Files\n");
                        firstTime = false;
                    }
                    System.out.println("\t" + file.getFilePath().replace(targetFolderPath + "/", ""));
                }
            }
        }
    }

    public static void showDeletedFiles(String targetFolderPath) {
        boolean firstTime = true;
        for (FileMeta lastFile : lastCloneFileList) {
            boolean found = currentFileList.stream().anyMatch(f -> f.getFilePath().equals(lastFile.getFilePath()));
            if (!found) {
                if (firstTime) {
                    System.out.println("\n\t" + RED_COLOR + "Deleted Files\n");
                    firstTime = false;
                }
                System.out.println("\t" + lastFile.getFilePath().replace(targetFolderPath + "/", ""));
            }
        }
    }

    /** Activate a clone */
    public static void selectClone(String hashCode) throws IOException {
        if (cloneList.size() == 0) takeClones();

        for (CloneUnit cloneUnit : cloneList) {
            if (hashCode.equals(cloneUnit.getCloneHashcode().substring(0, 7))) {
                destroyPresent(new File(targetFolderPath));
                activateClone(cloneUnit);
                return;
            }
        }
    }

    public static void activateClone(CloneUnit clone) throws IOException {
        String folderPathOfContent = mainRepoPath + "clones/filedata/";
        for (FileMeta fileMeta : clone.getFileList()) {
            String fileName = "/[.]?[A-Za-z0-9_[-] ]+[.][A-Za-z]+$";
            Matcher matcher = Pattern.compile(fileName).matcher(fileMeta.getFilePath());
            matcher.find();
            String directoryPath = fileMeta.getFilePath().substring(0, matcher.start());
            File directory = new File(directoryPath);
            if (!directory.exists()) directory.mkdirs();

            Path filePath = Paths.get(folderPathOfContent + fileMeta.getHashcode() + ".clone");
            byte[] buffer = MyFileVisitor.getBytes(filePath);
            MyFileVisitor.saveBytes(Paths.get(fileMeta.getFilePath()), buffer);
        }
        setHeadClone(clone.getCloneHashcode());
    }

    public static void destroyPresent(File file) throws IOException {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                if (!child.getName().startsWith(".clone")) destroyPresent(child);
            }
        } else {
            file.delete();
        }
    }

    public static void setHeadClone(String headCloneCode) throws IOException {
        writeFileContent(mainRepoPath + "clones/headhash.clone", headCloneCode.getBytes());
    }

    /** Utility methods for reading/writing objects */
    public static Object readFileContent(String filePath) throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filePath)))) {
            return ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeFileContent(String filePath, Object content) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)))) {
            oos.writeObject(content);
        }
    }

    /** Generate hashcode for current file list */
    public static String generateHashCode() throws IOException, NoSuchAlgorithmException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(currentFileList);
        }
        return calculateHashCode(baos.toByteArray());
    }

    public static String calculateHashCode(byte[] byteArray) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashArray = digest.digest(byteArray);
        StringBuilder hexStringCode = new StringBuilder();
        for (byte b : hashArray) {
            hexStringCode.append(String.format("%02X", b));
        }
        return hexStringCode.toString();
    }
    
    /** Detect the clone folder inside the target directory */
/** Detect the clone folder inside the target directory (uses targetFolderPath) */
public static void detectCloneFolder() {
    detectCloneFolder(targetFolderPath);
}

/** Detect the clone folder inside a specific directory */
public static void detectCloneFolder(String targetFolder) {
    File target = new File(targetFolder);
    File[] files = target.listFiles();

    if (files != null) {
        for (File f : files) {
            if (f.isDirectory() && f.getName().startsWith(".clone_")) {
                mainRepoPath = f.getAbsolutePath() + "/";
                return;
            }
        }
    }

    mainRepoPath = targetFolder + "/.clone/";
}

/** Recursively delete a directory or file */
public static void deleteDirectory(File dir) throws IOException {
    if (dir == null || !dir.exists()) return;

    if (dir.isDirectory()) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteDirectory(file);
            }
        }
    }

    if (!dir.delete()) {
        throw new IOException("Failed to delete: " + dir.getAbsolutePath());
    }
}

/** Save the current state of files (like commit without title/description) */
public static void save() throws IOException, NoSuchAlgorithmException {
    if (currentFileList == null || currentFileList.isEmpty()) return;

    saveNewFiles();
    String hashCode = generateHashCode();

    CloneUnit newCloneUnit = new CloneUnit(currentFileList, hashCode, "Auto-commit", "Saved via GUI");
    cloneList.add(newCloneUnit);

    writeFileContent(mainRepoPath + "clones/cloneList.clone", cloneList);
    setHeadClone(hashCode);
}

/** Return the list of all clones */
public static ArrayList<CloneUnit> getCloneList() {
    return cloneList;
}
}
