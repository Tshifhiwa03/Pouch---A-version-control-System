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
        File selectedFile = fileChooser.showOpenDialog(new Stage());

        if (selectedFile != null) {
            String repoName = selectedFile.getName();
            System.out.println("Opened repository: " + selectedFile.getAbsolutePath());
            showAlert("Repository Opened", "Opened repository: " + repoName);
            logAction("Repository opened: " + repoName);
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

    // =========================
    // HELPER METHODS
    // =========================

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
