package PouchUI;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import PouchVCS.Clone;
import PouchVCS.CloneUnit;
import PouchVCS.FileMeta;
import PouchVCS.MyFileVisitor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.control.CheckBox;

public class HomePageController implements Initializable {

    @FXML
    private ListView<String> fileListView;
    @FXML
    private ListView<String> historyListView;
    @FXML
    private TextField commitTitleField;
    @FXML
    private TextArea commitDescriptionField;
    @FXML
    private ComboBox<String> projectsComboBox;
    @FXML
    private Label centerTitleLabel;
    @FXML
    private Label centerSubtitleLabel;

    private final ObservableList<String> historyLog = FXCollections.observableArrayList();
    private final ObservableList<String> committedChanges = FXCollections.observableArrayList();
    private final Map<String, ObservableList<String>> projectFilesMap = new HashMap<>();
    private String currentProject = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("Home Page initialized!");
        logAction("App initialized");

        // Handle clicking on a folder or file
        fileListView.setOnMouseClicked(event -> {
            String selectedItem = fileListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) handleFileListClick(selectedItem);
        });

        // Project selection listener
        projectsComboBox.setOnAction(event -> {
            String selectedProject = projectsComboBox.getSelectionModel().getSelectedItem();
            if (selectedProject != null && !selectedProject.equals(currentProject)) {
                switchToProject(selectedProject);
            }
        });
    }

    // ========================= Actions =============================

    @FXML
    private void handleOpenRepo(ActionEvent event) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Repository Folder");
        File selectedFolder = dirChooser.showDialog(new Stage());

        if (selectedFolder != null) {
            Clone.targetFolderPath = selectedFolder.getAbsolutePath();
            Clone.detectCloneFolder();

            try {
                Clone.currentFileList.clear();
                Files.walkFileTree(selectedFolder.toPath(), new MyFileVisitor());
            } catch (IOException e) {
                showAlert("Error", "Failed to read repository: " + e.getMessage());
            }

            String projectName = selectedFolder.getName();
            ObservableList<String> projectFiles = FXCollections.observableArrayList();
            for (FileMeta file : Clone.currentFileList) {
                projectFiles.add("File" + file.getFilePath().replace(Clone.targetFolderPath + "/", ""));
            }
            projectFilesMap.put(projectName, projectFiles);

            if (!projectsComboBox.getItems().contains(projectName)) {
                projectsComboBox.getItems().add(projectName);
            }
            projectsComboBox.setValue(projectName);
            currentProject = projectName;

            refreshFileListView();
            showAlert("Repository Opened", "Opened repository: " + projectName);
        }
    }

    @FXML
    private void handleNewProjectButton(ActionEvent event) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select folder to create new project");
        File parentFolder = dirChooser.showDialog(new Stage());
        if (parentFolder == null) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Project");
        dialog.setHeaderText("Enter a name for your new project");
        dialog.setContentText("Project name:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().trim().isEmpty()) return;

        String repoName = result.get().trim();
        File newCloneFolder = new File(parentFolder, ".clone_" + repoName);
        if (newCloneFolder.exists()) {
            Alert overwriteAlert = new Alert(AlertType.CONFIRMATION);
            overwriteAlert.setTitle("Overwrite Project?");
            overwriteAlert.setHeaderText("A project with this name already exists.");
            overwriteAlert.setContentText("Do you want to overwrite it?");
            Optional<ButtonType> answer = overwriteAlert.showAndWait();
            if (answer.isEmpty() || answer.get() != ButtonType.OK) return;
            Clone.deleteDirectory(newCloneFolder);
        }

        try {
            newCloneFolder.mkdir();
            Clone.targetFolderPath = parentFolder.getAbsolutePath();
            Clone.mainRepoPath = newCloneFolder.getAbsolutePath() + "/";
            Clone.start(repoName);

            ObservableList<String> projectFiles = FXCollections.observableArrayList();
            projectFiles.add("<>" + repoName + " (root)");
            projectFilesMap.put(repoName, projectFiles);

            if (!projectsComboBox.getItems().contains(repoName)) {
                projectsComboBox.getItems().add(repoName);
            }
            projectsComboBox.setValue(repoName);
            currentProject = repoName;

            refreshFileListView();
            centerTitleLabel.setText("Project: " + repoName);
            centerSubtitleLabel.setText("New project initialized successfully!");

            showAlert("Project Created", "New project created at: " + newCloneFolder.getAbsolutePath());
        } catch (IOException e) {
            showAlert("Error", "Failed to create new project: " + e.getMessage());
        }
    }

    @FXML
private void handleCommit(ActionEvent e) {
    String title = commitTitleField.getText().trim();
    if (title.isEmpty()) {
        showAlert("Error", "Enter a commit title.");
        return;
    }

    ArrayList<FileMeta> filesToCommit = new ArrayList<>();
    for (CheckBox cb : fileSelectionListView.getItems()) {
    if (cb.isSelected()) {
        File f = new File(Clone.targetFolderPath, cb.getText());
        try {
            filesToCommit.add(new FileMeta(f.getPath(), MyFileVisitor.generateHashForFile(f)));
        } catch (IOException ex) {
            showAlert("Error", "Failed to hash file: " + f.getName());
        }
    }
}


    if (filesToCommit.isEmpty()) {
        showAlert("Error", "Select at least one file to commit.");
        return;
    }

    String hash = String.valueOf((title + System.currentTimeMillis()).hashCode());
    CloneUnit commit = new CloneUnit(filesToCommit, hash);
    saveCommit(commit, hash);
    addCommitToHistory(title, hash, commitDescriptionField.getText(), filesToCommit);
}

    /*private void handleCommit(ActionEvent event) {
        String title = commitTitleField.getText().trim();
        String description = commitDescriptionField.getText().trim();
        if (title.isEmpty()) {
            showAlert("Error", "Please enter a commit title.");
            return;
        }

        ArrayList<FileMeta> filesToCommit = new ArrayList<>();
        for (FileMeta f : Clone.currentFileList) {
            filesToCommit.add(new FileMeta(f.getFilePath(), f.getHashcode()));
        }

        String commitHash = String.valueOf((title + System.currentTimeMillis()).hashCode());
        CloneUnit commit = new CloneUnit(filesToCommit, commitHash);

        try {
            File commitsFolder = new File(Clone.targetFolderPath + File.separator + ".clone_/commits");
            if (!commitsFolder.exists()) commitsFolder.mkdirs();
            File commitFile = new File(commitsFolder, commitHash + ".clone");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(commitFile))) {
                oos.writeObject(commit);
            }
        } catch (IOException e) {
            showAlert("Error", "Failed to save commit: " + e.getMessage());
            return;
        }

        String formattedCommit = "🔹 " + title + " [" + commitHash + "]\n ↪ " + description;
        committedChanges.add(0, formattedCommit);
        historyListView.getItems().clear();
        historyListView.getItems().addAll(committedChanges);

        commitTitleField.clear();
        commitDescriptionField.clear();

        showAlert("Commit Successful", "Your changes have been committed!");
    }*/

    @FXML
    private void handleViewHistory(ActionEvent event) {
        historyListView.getItems().clear();
        historyListView.getItems().addAll(historyLog);
        showAlert("History Loaded", "Full activity history loaded successfully.");
    }

    @FXML
    private void handleRollback(ActionEvent event) {
        String selectedCommit = historyListView.getSelectionModel().getSelectedItem();
        if (selectedCommit == null) {
            showAlert("Error", "Please select a commit to roll back to.");
            return;
        }

        String hashCode = selectedCommit.replaceAll(".*\\[(\\w+)\\].*", "$1");
        try {
            Clone.selectClone(hashCode);
            showAlert("Rollback Successful", "Rolled back to commit: " + hashCode);
        } catch (IOException e) {
            showAlert("Error", "Rollback failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleSaveClone(ActionEvent event) {
        try {
            Clone.save();
            showAlert("Clone Saved", "Your current project state has been saved.");
        } catch (Exception e) {
            showAlert("Error", "Failed to save clone: " + e.getMessage());
        }
    }
    
    @FXML private ListView<CheckBox> fileSelectionListView;

private void refreshFileSelectionList() {
    fileSelectionListView.getItems().clear();
    ObservableList<CheckBox> boxes = FXCollections.observableArrayList();

    for (FileMeta file : Clone.currentFileList) {
        CheckBox cb = new CheckBox(file.getFilePath()
                                   .replace(Clone.targetFolderPath + "/", ""));
        boxes.add(cb);
    }
    fileSelectionListView.setItems(boxes);
}
    // ========================= Helpers ===========================

    private void switchToProject(String projectName) {
        currentProject = projectName;
        refreshFileListView();
        centerTitleLabel.setText("Project: " + projectName);
        centerSubtitleLabel.setText("Switched to project: " + projectName);
        logAction("Switched to project: " + projectName);
    }

    private void refreshFileListView() {
        fileListView.getItems().clear();
        for (Map.Entry<String, ObservableList<String>> entry : projectFilesMap.entrySet()) {
            fileListView.getItems().add(" > Project: " + entry.getKey());
            fileListView.getItems().addAll(entry.getValue());
            fileListView.getItems().add("────────────────────");
        }
        if (!fileListView.getItems().isEmpty())
            fileListView.getItems().remove(fileListView.getItems().size() - 1);
    }

    private void handleFileListClick(String selectedItem) {
        if (!selectedItem.startsWith("> Project: ") && !selectedItem.startsWith("────────────────────")) {
            File file = new File(Clone.targetFolderPath, selectedItem.replace("File", ""));
            if (file.exists()) {
                try {
                    Desktop.getDesktop().open(file);
                } catch (IOException e) {
                    showAlert("Error", "Cannot open file: " + e.getMessage());
                }
            }
        }
    }

    private void logAction(String action) {
        historyLog.add(0, timestamped(action));
    }

    private String timestamped(String action) {
        return "[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] " + action;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void saveCommit(CloneUnit commit, String hash) {
    try {
        File commitsFolder = new File(Clone.targetFolderPath + File.separator + ".clone_/commits");
        if (!commitsFolder.exists()) commitsFolder.mkdirs();

        File commitFile = new File(commitsFolder, hash + ".clone");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(commitFile))) {
            oos.writeObject(commit);
        }
    } catch (IOException e) {
        showAlert("Error", "Failed to save commit: " + e.getMessage());
    }
}
    
   private void addCommitToHistory(String title, String hash, String description, ArrayList<FileMeta> files) {
    StringBuilder sb = new StringBuilder();
    sb.append("🔹 ").append(title).append(" [").append(hash).append("]\n ↪ ").append(description);

    if (!files.isEmpty()) {
        sb.append("\n   Files:");
        for (FileMeta f : files) {
            sb.append("\n   • ").append(f.getFilePath());
        }
    }

    committedChanges.add(0, sb.toString());
    historyListView.getItems().setAll(committedChanges);
} 
}
