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
import java.nio.file.Path;
import java.util.ArrayList;          // for ArrayList
import java.io.FileOutputStream;      // for saving commit files
import java.io.ObjectOutputStream;    // for serializing CloneUnit
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import java.util.Optional;
import javafx.scene.control.ButtonType;
import java.util.Optional;

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

    // Centralized history log for all actions
    private final ObservableList<String> historyLog = FXCollections.observableArrayList();

    // Separate list to store committed changes
    private final ObservableList<String> committedChanges = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("Home Page initialized!");
        logAction("App initialized");
    }

    @FXML
private void handleOpenRepo(ActionEvent event) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open Repository Folder");

    // Open a folder selection dialog
    DirectoryChooser dirChooser = new DirectoryChooser();
    dirChooser.setTitle("Select Repository Folder");
    File selectedFolder = dirChooser.showDialog(new Stage());

    if (selectedFolder != null) {
        // 1️⃣ Save the folder path so your backend knows where to work
        Clone.targetFolderPath = selectedFolder.getAbsolutePath();
        Clone.detectCloneFolder(); // looks for any existing .clone_ folder

        // 2️⃣ Get files in that folder using MyFileVisitor
        try {
            Clone.currentFileList.clear(); // clear previous files
            Files.walkFileTree(selectedFolder.toPath(), new MyFileVisitor());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 3️⃣ Show the files in the ListView
        fileListView.getItems().clear();
        for (FileMeta file : Clone.currentFileList) {
            fileListView.getItems().add(file.getFilePath()
                .replace(Clone.targetFolderPath + "/", ""));
        }

        showAlert("Repository Opened", "Opened repository: " + selectedFolder.getName());
    }
}

    @FXML
    private void handleViewHistory(ActionEvent event) {
        System.out.println("View History clicked");
        historyListView.getItems().clear();
        historyListView.getItems().addAll(historyLog);
        showAlert("History Loaded", "Full activity history loaded successfully.");
    }

    @FXML
    private void handleSettings(ActionEvent event) {
        System.out.println("Settings clicked");
        showAlert("Settings", "Settings page under development!");
        logAction("⚙ Accessed settings");
    }

    @FXML
    private void handleCommit(ActionEvent event) {
    String title = commitTitleField.getText().trim();
    String description = commitDescriptionField.getText().trim();

    if (title.isEmpty()) {
        showAlert("Error", "Please enter a commit title.");
        return;
    }

    // Create a new CloneUnit
    ArrayList<FileMeta> filesToCommit = new ArrayList<>();
    for (FileMeta f : Clone.currentFileList) {
        filesToCommit.add(new FileMeta(f.getFilePath(), f.getHashcode()));
    }

    // Generate a simple commit hash using title + timestamp
    String commitHash = String.valueOf((title + System.currentTimeMillis()).hashCode());
    CloneUnit commit = new CloneUnit(filesToCommit, commitHash);

    // Save commit to .clone_/commits folder
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

    // Update GUI history
    String formattedCommit = "🔹 " + title + "\n ↪ " + description;
    committedChanges.add(0, formattedCommit);
    historyListView.getItems().clear();
    historyListView.getItems().addAll(committedChanges);

    // Clear input fields
    commitTitleField.clear();
    commitDescriptionField.clear();

    showAlert("Commit Successful", "Your changes have been committed!");
}

    @FXML
    private void handleFetch(ActionEvent event) {
        System.out.println("Fetch Origin clicked");
        showAlert("Fetch", "Fetching updates from remote repository...");

        // Log the fetch action
        logAction("Fetched updates from remote repository");

        // Display commits in the fetch panel (historyListView)
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
    // 1️⃣ Ask the user to select the parent folder
    DirectoryChooser dirChooser = new DirectoryChooser();
    dirChooser.setTitle("Select Folder to Create New Project");
    File selectedFolder = dirChooser.showDialog(new Stage());

    if (selectedFolder != null) {
        // 2️⃣ Ask the user for a project name
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Project");
        dialog.setHeaderText("Enter a name for your new project");
        dialog.setContentText("Project name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String projectName = result.get().trim();

            // 3️⃣ Create the new .clone_<projectName> folder
            File cloneFolder = new File(selectedFolder, ".clone_" + projectName);
            if (!cloneFolder.exists()) {
                if (!cloneFolder.mkdir()) {
                    showAlert("Error", "Failed to create project folder.");
                    return;
                }
            }

            // 4️⃣ Set backend tracking
            Clone.targetFolderPath = cloneFolder.getAbsolutePath();
            try {
                Clone.initializeNewClone(); // initialize empty project
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error", "Failed to initialize new project: " + e.getMessage());
                return;
            }

            // 5️⃣ Update GUI
            fileListView.getItems().clear();
            centerTitleLabel.setText("Project: " + projectName);
            centerSubtitleLabel.setText("New project initialized successfully!");
            showAlert("Project Created", "New project created at: " + cloneFolder.getAbsolutePath());

            // 6️⃣ Add project to the Projects dropdown
            ObservableList<String> projects = projectsComboBox.getItems();
            projects.add(projectName);
            projectsComboBox.setValue(projectName); // select the new project
        }
    }
}

@FXML
private void handleNewProjectButton(ActionEvent event) {
    // 1️⃣ Select parent folder
    DirectoryChooser dirChooser = new DirectoryChooser();
    dirChooser.setTitle("Select folder to create new project");
    File parentFolder = dirChooser.showDialog(new Stage());

    if (parentFolder == null) return;

    // 2️⃣ Ask for project name
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
        Clone.deleteDirectory(newCloneFolder); // remove existing folder
    }

    try {
        Clone.targetFolderPath = parentFolder.getAbsolutePath();
        Clone.mainRepoPath = newCloneFolder.getAbsolutePath() + "/";
        newCloneFolder.mkdir();
        Clone.start(repoName);

        // 3️⃣ Update GUI
        ObservableList<String> projects = projectsComboBox.getItems();
        projects.add(repoName);
        projectsComboBox.setValue(repoName); // select the new project

        centerTitleLabel.setText("Project: " + repoName);
        centerSubtitleLabel.setText("New project initialized successfully!");
        fileListView.getItems().clear();

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

}

