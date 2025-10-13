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
        
        historyListView.setOnMouseClicked(event -> {
        String selectedCommit = historyListView.getSelectionModel().getSelectedItem();
        if (selectedCommit != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Commit Details");
            alert.setHeaderText("Selected Commit");
            alert.setContentText(selectedCommit);
            alert.showAndWait();
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
    Alert overwriteAlert = new Alert(Alert.AlertType.CONFIRMATION);
    overwriteAlert.setTitle("Overwrite Project?");
    overwriteAlert.setHeaderText("A project with this name already exists.");
    overwriteAlert.setContentText("Do you want to overwrite it?");
    Optional<ButtonType> answer = overwriteAlert.showAndWait();
    if (answer.isEmpty() || answer.get() != ButtonType.OK) return;
    try {
        Clone.deleteDirectory(newCloneFolder);
    } catch (IOException ex) {
        showAlert("Error", "Failed to delete existing project: " + ex.getMessage());
        return;
    }
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
private void handleCommit(ActionEvent event) {
    ObservableList<CheckBox> selectedFiles = fileSelectionListView.getItems();
    ArrayList<FileMeta> filesToCommit = new ArrayList<>();

    for (CheckBox cb : selectedFiles) {
        if (cb.isSelected()) {
            File file = new File(Clone.targetFolderPath, cb.getText());
            try {
                String hash = MyFileVisitor.generateHashForFile(file);  // Compute current hash
                filesToCommit.add(new FileMeta(file.getPath(), hash));
            } catch (IOException e) {
                showAlert("Error", "Failed to generate hash for file: " + file.getName() + "\n" + e.getMessage());
                return; // stop committing if one file fails
            }
        }
    }

    if (filesToCommit.isEmpty()) {
        showAlert("No files selected", "Please select at least one file to commit.");
        return;
    }

    try {
        Clone.saveCommit(filesToCommit, commitTitleField.getText(), commitDescriptionField.getText());
        showAlert("Success", "Commit saved successfully.");
        updateHistoryListView();  // Refresh history view
        commitTitleField.clear();
        commitDescriptionField.clear();
    } catch (Exception e) {
        showAlert("Commit Error", "Failed to save commit:\n" + e.getMessage());
    }
}


/*private void handleCommit(ActionEvent e) {
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

    // Extract hash from the commit text
    String hash = selectedCommit.lines()
                                .filter(line -> line.startsWith("   🔑 Hash: "))
                                .findFirst()
                                .map(line -> line.replace("   🔑 Hash: ", ""))
                                .orElse(null);

    if (hash == null) {
        showAlert("Error", "Could not extract hash from commit.");
        return;
    }

    // Confirm rollback
    Alert confirm = new Alert(AlertType.CONFIRMATION);
    confirm.setTitle("Confirm Rollback");
    confirm.setHeaderText("Rollback to commit: " + hash);
    confirm.setContentText("This will overwrite current files. Continue?");
    Optional<ButtonType> result = confirm.showAndWait();
    if (result.isEmpty() || result.get() != ButtonType.OK) return;

    try {
        Clone.selectClone(hash);
        showAlert("Rollback Successful", "Rolled back to commit: " + hash);
        refreshFileSelectionList();
        refreshFileListView();
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
    ObservableList<CheckBox> boxes = FXCollections.observableArrayList();
    for (FileMeta file : Clone.currentFileList) {
        CheckBox cb = new CheckBox(file.getFilePath().replace(Clone.targetFolderPath + "/", ""));
        boxes.add(cb);
    }
    fileSelectionListView.setItems(boxes);
}

@FXML
private void handlePreviewChanges(ActionEvent event) {
    ObservableList<CheckBox> selectedFiles = fileSelectionListView.getItems();
    boolean anySelected = false;

    for (CheckBox cb : selectedFiles) {
        if (cb.isSelected()) {
            anySelected = true;
            File file = new File(Clone.targetFolderPath, cb.getText());
            try {
                showDiffDialog(file);
            } catch (IOException e) {
                showAlert("Error", "Failed to preview file: " + file.getName() + "\n" + e.getMessage());
            }
        }
    }

    if (!anySelected) {
        showAlert("No File Selected", "Please select at least one file to preview.");
    }
}

@FXML
private void handleDeleteProject(ActionEvent event) {
    if (currentProject.isEmpty()) {
        showAlert("Error", "No project selected to delete.");
        return;
    }

    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
    confirmAlert.setTitle("Delete Project");
    confirmAlert.setHeaderText("Are you sure you want to delete project: " + currentProject + "?");
    confirmAlert.setContentText("This action cannot be undone.");
    Optional<ButtonType> result = confirmAlert.showAndWait();

    if (result.isPresent() && result.get() == ButtonType.OK) {
        // Delete project folder
        File projectFolder = new File(Clone.targetFolderPath, ".clone_" + currentProject);
        if (projectFolder.exists()) {
            try {
                Clone.deleteDirectory(projectFolder);
            } catch (IOException e) {
                showAlert("Error", "Failed to delete project folder: " + e.getMessage());
                return;
            }
        }

        // Remove from ComboBox & map
        projectsComboBox.getItems().remove(currentProject);
        projectFilesMap.remove(currentProject);
        currentProject = "";
        fileListView.getItems().clear();
        historyListView.getItems().clear();
        centerTitleLabel.setText("Select a project");
        centerSubtitleLabel.setText("Project deleted successfully!");
    }
}



    // ========================= Helpers ===========================

    private void switchToProject(String projectName) {
    if (hasUnsavedChanges()) {
        Alert unsavedAlert = new Alert(Alert.AlertType.CONFIRMATION);
        unsavedAlert.setTitle("Unsaved Changes");
        unsavedAlert.setHeaderText("You have unsaved changes!");
        unsavedAlert.setContentText("Do you want to continue without saving?");
        Optional<ButtonType> result = unsavedAlert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;
    }

    currentProject = projectName;
    refreshFileListView();
    refreshFileSelectionList();
    centerTitleLabel.setText("Project: " + projectName);
    centerSubtitleLabel.setText("Switched to project: " + projectName);
    logAction("Switched to project: " + projectName);
}

private boolean hasUnsavedChanges() {
    return !commitTitleField.getText().trim().isEmpty() || !commitDescriptionField.getText().trim().isEmpty();
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
    sb.append("🔹 Commit: ").append(title).append("\n");
    sb.append("   ⏱ ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
    sb.append("   🔑 Hash: ").append(hash).append("\n");

    if (!files.isEmpty()) {
        sb.append("   🗂 Files: ");
        for (int i = 0; i < files.size(); i++) {
            sb.append(files.get(i).getFilePath().replace(Clone.targetFolderPath + "/", ""));
            if (i < files.size() - 1) sb.append(", ");
        }
    }

    committedChanges.add(0, sb.toString());
    historyListView.getItems().setAll(committedChanges);
}
   
   private void showDiffDialog(File file) throws IOException {
    if (!file.exists()) {
        showAlert("Error", "File does not exist: " + file.getName());
        return;
    }

    // Read current content
    String content = Files.readString(file.toPath());

    // Create dialog
    Alert diffAlert = new Alert(Alert.AlertType.INFORMATION);
    diffAlert.setTitle("File Diff: " + file.getName());
    diffAlert.setHeaderText("Changes detected in file:");

    // Display content in non-editable TextArea
    TextArea diffArea = new TextArea(content);
    diffArea.setEditable(false);
    diffArea.setWrapText(true);
    diffArea.setPrefWidth(600);
    diffArea.setPrefHeight(400);

    diffAlert.getDialogPane().setContent(diffArea);
    diffAlert.showAndWait();
}

   private void updateHistoryListView() {
    ObservableList<String> items = FXCollections.observableArrayList();
    try {
        for (CloneUnit clone : Clone.getCloneList()) {
            String display = "🔹 " + clone.getTitle() + " [" + clone.getCloneHashcode().substring(0, 7) + "]";
            items.add(display);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    historyListView.setItems(items);
}

}
