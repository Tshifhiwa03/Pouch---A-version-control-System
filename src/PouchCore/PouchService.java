package PouchCore;

import PouchVCS.Clone;
import PouchVCS.CloneUnit;
import PouchVCS.FileMeta;
import PouchVCS.MyFileVisitor;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PouchService {
    private static PouchService instance;
    private Map<String, CloneUnit> commits = new HashMap<>();
    
    public static PouchService getInstance() {
        if (instance == null) {
            instance = new PouchService();
        }
        return instance;
    }
    
    // Replace CLI "start" command
    public boolean createNewProject(String projectName, String projectPath) {
        try {
            Clone.targetFolderPath = projectPath;
            Clone.mainRepoPath = projectPath + File.separator + ".clone_" + projectName + File.separator;
            Clone.start(projectName);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Replace CLI "make" command
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
    
    // Replace CLI "save" command
    public String createCommit(String title, String description, String repositoryPath) {
        try {
            Clone.targetFolderPath = repositoryPath;
            Clone.detectCloneFolder();
            
            // Get current file list
            Clone.currentFileList.clear();
            Files.walkFileTree(Paths.get(repositoryPath), new MyFileVisitor());
            
            // Create commit
            ArrayList<FileMeta> filesToCommit = new ArrayList<>();
            for (FileMeta f : Clone.currentFileList) {
                filesToCommit.add(new FileMeta(f.getFilePath(), f.getHashcode()));
            }
            
            String commitHash = String.valueOf((title + System.currentTimeMillis()).hashCode());
            CloneUnit commit = new CloneUnit(filesToCommit, commitHash);
            
            // Save commit
            File cloneFolder = new File(Clone.targetFolderPath + File.separator + ".clone_");
            File commitsFolder = new File(cloneFolder, "commits");
            if (!commitsFolder.exists()) commitsFolder.mkdirs();
            
            File commitFile = new File(commitsFolder, commitHash + ".clone");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(commitFile))) {
                oos.writeObject(commit);
            }
            
            // Store in memory
            commits.put(commitHash, commit);
            
            return commitHash;
            
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Get commit history for GUI
    public ArrayList<String> getCommitHistory() {
        ArrayList<String> history = new ArrayList<>();
        for (CloneUnit commit : commits.values()) {
            history.add(commit.getCloneHashcode() + " - " + commit.getFileList().size() + " files");
        }
        return history;
    }
    
    // Get current files for display
    public ArrayList<String> getCurrentFiles() {
        ArrayList<String> fileNames = new ArrayList<>();
        for (FileMeta file : Clone.currentFileList) {
            fileNames.add(file.getFilePath().replace(Clone.targetFolderPath + "/", ""));
        }
        return fileNames;
    }
}