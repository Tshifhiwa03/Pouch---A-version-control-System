package PouchCore;

import PouchVCS.Clone;
import PouchVCS.CloneUnit;
import PouchVCS.FileMeta;
import PouchVCS.MyFileVisitor;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javafx.stage.Stage;

public class PouchService {
    private static PouchService instance;
    private Map<String, CloneUnit> commits = new HashMap<>();

    public static PouchService getInstance() {
        if (instance == null) {
            instance = new PouchService();
        }
        return instance;
    }

    // GUI-compatible project creation
    public boolean createNewProject(String projectName, String projectPath, Stage primaryStage) {
        try {
            File newCloneFolder = new File(projectPath, ".clone_" + projectName);
            if (!newCloneFolder.exists()) newCloneFolder.mkdir();

            Clone.targetFolderPath = projectPath;
            Clone.mainRepoPath = newCloneFolder.getAbsolutePath() + File.separator;

            // Use GUI-based start
            Clone.startWithGUI(primaryStage);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Scan repository for changes
    public boolean scanForChanges(String repositoryPath) {
        try {
            Clone.targetFolderPath = repositoryPath;
            Clone.detectCloneFolder();
            Clone.currentFileList.clear();
            Files.walkFileTree(Paths.get(repositoryPath), new MyFileVisitor());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Create and save a commit
    public String createCommit(String title, String description, String repositoryPath) {
        try {
            Clone.targetFolderPath = repositoryPath;
            Clone.detectCloneFolder();

            Clone.currentFileList.clear();
            Files.walkFileTree(Paths.get(repositoryPath), new MyFileVisitor());

            ArrayList<FileMeta> filesToCommit = new ArrayList<>();
            for (FileMeta f : Clone.currentFileList) {
                filesToCommit.add(new FileMeta(f.getFilePath(), f.getHashcode()));
            }

            String commitHash = String.valueOf((title + System.currentTimeMillis()).hashCode());
            CloneUnit commit = new CloneUnit(filesToCommit, commitHash);

            File cloneFolder = new File(Clone.targetFolderPath + File.separator + ".clone_");
            File commitsFolder = new File(cloneFolder, "commits");
            if (!commitsFolder.exists()) commitsFolder.mkdirs();

            File commitFile = new File(commitsFolder, commitHash + ".clone");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(commitFile))) {
                oos.writeObject(commit);
            }

            commits.put(commitHash, commit);
            return commitHash;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Get commit history
    public ArrayList<String> getCommitHistory() {
        ArrayList<String> history = new ArrayList<>();
        for (CloneUnit commit : commits.values()) {
            history.add(commit.getCloneHashcode() + " - " + commit.getFileList().size() + " files");
        }
        return history;
    }

    // Get current files
    public ArrayList<String> getCurrentFiles() {
        ArrayList<String> fileNames = new ArrayList<>();
        for (FileMeta file : Clone.currentFileList) {
            fileNames.add(file.getFilePath().replace(Clone.targetFolderPath + File.separator, ""));
        }
        return fileNames;
    }
}
