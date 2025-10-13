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

public class HomePageController implements Initializable {

    @FXML
    private ListView<String> fileListView;

    @FXML
    private ListView<String> historyListView;

    @FXML
    private TextField commitTitleField;

    @FXML
    private TextArea commitDescriptionField;

    // Centralized history log for all actions
    private final ObservableList<String> historyLog = FXCollections.observableArrayList();

    // Separate list to store committed changes
    private final ObservableList<String> committedChanges = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("Home Page initialized!");
        logAction("App initialized");
    }

    // =========================
    // EVENT HANDLERS
    // =========================

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

/*private void handleCommit(ActionEvent event) {
    String title = commitTitleField.getText().trim();
    String description = commitDescriptionField.getText().trim();

    if (title.isEmpty()) {
        showAlert("Error", "Please enter a commit title.");
        return;
    }
    // Format the commit entry
    String formattedCommit = "🔹 " + title + "\n ↪ " + description;
    // Log the commit to history
    logAction("Commit made: " + title);
    // Store the commit separately for fetch display
    committedChanges.add(0, formattedCommit);
    // ✅ Immediately update the fetch panel
    historyListView.getItems().clear();
    historyListView.getItems().addAll(committedChanges);
    // Clear input fields
    commitTitleField.clear();
    commitDescriptionField.clear();

    showAlert("Commit Successful", "Your changes have been committed.");
}*/

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
}
