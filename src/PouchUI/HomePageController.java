package PouchUI;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import PouchVCS.Clone;
import PouchVCS.CloneUnit;
import PouchVCS.FileMeta;
import PouchVCS.MyFileVisitor;
import javafx.stage.DirectoryChooser;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import java.util.Optional;
import javafx.scene.control.ButtonType;
import java.util.HashMap;
import java.util.Map;

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
        // Handle clicking on a folder or file in the Project Files panel
        fileListView.setOnMouseClicked(event -> {
            String selectedItem = fileListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                handleFileListClick(selectedItem);
            }
        });
        // Listen for project selection changes
        projectsComboBox.setOnAction(event -> {
            String selectedProject = projectsComboBox.getSelectionModel().getSelectedItem();
            if (selectedProject != null && !selectedProject.equals(currentProject)) {
                switchToProject(selectedProject);
            }
        });
    }
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
                e.printStackTrace();
            }

            // Instead of clearing, add files to current project
            String projectName = selectedFolder.getName();
            ObservableList<String> projectFiles = FXCollections.observableArrayList();
            
            for (FileMeta file : Clone.currentFileList) {
                projectFiles.add("File" + file.getFilePath().replace(Clone.targetFolderPath + "/", ""));
            }
            
            projectFilesMap.put(projectName, projectFiles);
            refreshFileListView();
            // Add to projects combo if not already there
            if (!projectsComboBox.getItems().contains(projectName)) {
                projectsComboBox.getItems().add(projectName);
            }
            projectsComboBox.setValue(projectName);
            currentProject = projectName;

            showAlert("Repository Opened", "Opened repository: " + projectName);
        }
    }
    @FXML
    private void handleViewHistory(ActionEvent event) {
        historyListView.getItems().clear();
        historyListView.getItems().addAll(historyLog);
        showAlert("History Loaded", "Full activity history loaded successfully.");
    }
    @FXML
    private void handleSettings(ActionEvent event) {
        showAlert("Settings", "Settings page under development!");
        logAction("Accessed settings");
    }
    @FXML
    private void handleCommit(ActionEvent event) {
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
            File cloneFolder = new File(Clone.targetFolderPath + File.separator + ".clone_");
            File commitsFolder = new File(cloneFolder, "commits");
            if (!commitsFolder.exists()) commitsFolder.mkdirs();

            File commitFile = new File(commitsFolder, commitHash + ".clone");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(commitFile))) {
                oos.writeObject(commit);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to save commit!");
            return;
        }

        String formattedCommit = "🔹 " + title + "\n ↪ " + description;
        committedChanges.add(0, formattedCommit);
        historyListView.getItems().clear();
        historyListView.getItems().addAll(committedChanges);

        commitTitleField.clear();
        commitDescriptionField.clear();

        showAlert("Commit Successful", "Your changes have been committed!");
    }
    @FXML
    private void handleFetch(ActionEvent event) {
        showAlert("Fetch", "Fetching updates from remote repository...");
        logAction("Fetched updates from remote repository");
        historyListView.getItems().clear();
        historyListView.getItems().addAll(committedChanges);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void logAction(String action) {
        historyLog.add(0, timestamped(action));
    }

    private String timestamped(String action) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return "[" + timestamp + "] " + action;
    }

    @FXML
    private void handleOpenSelectedProject(ActionEvent event) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Folder to Create New Project");
        File selectedFolder = dirChooser.showDialog(new Stage());

        if (selectedFolder != null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("New Project");
            dialog.setHeaderText("Enter a name for your new project");
            dialog.setContentText("Project name:");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                String projectName = result.get().trim();

                File cloneFolder = new File(selectedFolder, ".clone_" + projectName);
                if (!cloneFolder.exists()) {
                    if (!cloneFolder.mkdir()) {
                        showAlert("Error", "Failed to create project folder.");
                        return;
                    }
                }

                Clone.targetFolderPath = cloneFolder.getAbsolutePath();
                try {
                    Clone.initializeNewClone();
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert("Error", "Failed to initialize new project: " + e.getMessage());
                    return;
                }

                // Store project files in the map
                ObservableList<String> projectFiles = FXCollections.observableArrayList();
                projectFiles.add("<>" + projectName + " (root)");
                projectFilesMap.put(projectName, projectFiles);
                
                refreshFileListView();

                centerTitleLabel.setText("Project: " + projectName);
                centerSubtitleLabel.setText("New project initialized successfully!");
                showAlert("Project Created", "New project created at: " + cloneFolder.getAbsolutePath());

                ObservableList<String> projects = projectsComboBox.getItems();
                if (!projects.contains(projectName)) {
                    projects.add(projectName);
                }
                projectsComboBox.setValue(projectName);
                currentProject = projectName;
            }
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
            Clone.deleteDirectory(newCloneFolder);
        }

        try {
            Clone.targetFolderPath = parentFolder.getAbsolutePath();
            Clone.mainRepoPath = newCloneFolder.getAbsolutePath() + "/";
            newCloneFolder.mkdir();
            Clone.start(repoName);

            // Store project files in the map
            ObservableList<String> projectFiles = FXCollections.observableArrayList();
            projectFiles.add("<>" + repoName + " (root)");
            projectFilesMap.put(repoName, projectFiles);
            
            refreshFileListView();

            ObservableList<String> projects = projectsComboBox.getItems();
            if (!projects.contains(repoName)) {
                projects.add(repoName);
            }
            projectsComboBox.setValue(repoName);
            currentProject = repoName;

            centerTitleLabel.setText("Project: " + repoName);
            centerSubtitleLabel.setText("New project initialized successfully!");

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Project Created");
            success.setHeaderText("New project created!");
            success.setContentText("Path: " + newCloneFolder.getAbsolutePath());
            success.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Error");
            error.setHeaderText("Failed to create new project");
            error.setContentText(e.getMessage());
            error.showAndWait();
        }
    }
    // ==============================================================
    // 🔹 Switch to a different project
    // ==============================================================
    private void switchToProject(String projectName) {
        currentProject = projectName;
        refreshFileListView();
        centerTitleLabel.setText("Project: " + projectName);
        centerSubtitleLabel.setText("Switched to project: " + projectName);
        logAction("Switched to project: " + projectName);
    }

    // ==============================================================
    // 🔹 Refresh the file list view with all projects and their files
    // ==============================================================
    private void refreshFileListView() {
        fileListView.getItems().clear();
        
        // Add all projects and their files
        for (Map.Entry<String, ObservableList<String>> entry : projectFilesMap.entrySet()) {
            String projectName = entry.getKey();
            ObservableList<String> files = entry.getValue();
            
            // Add project header
            fileListView.getItems().add(" > Project: " + projectName);
            
            // Add all files for this project
            fileListView.getItems().addAll(files);
            
            // Add separator between projects
            fileListView.getItems().add("────────────────────");
        }
        
        // Remove the last separator if there are items
        if (!fileListView.getItems().isEmpty()) {
            fileListView.getItems().remove(fileListView.getItems().size() - 1);
        }
    }

    // ==============================================================
    // 🔹 Handles clicks on folders/files inside Project Files panel
    // ==============================================================
    private void handleFileListClick(String selectedItem) {
        // If it's a project folder name, you can expand/collapse it
        if (selectedItem.startsWith("> Project: ")) {
            String projectName = selectedItem.replace(" > Project: ", "");
            // You can implement expand/collapse logic here if needed
        }
        // else you could later add openFile(selectedItem) logic
    }
}