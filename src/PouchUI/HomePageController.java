package PouchUI;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */

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

/**
 * FXML Controller class for the Home Page of the Version Control System
 *
 * @author tshif
 */
public class HomePageController implements Initializable {

    // Example UI elements that match your FXML IDs
    @FXML
    private ListView<String> fileListView;

    @FXML
    private ListView<String> historyListView;

    @FXML
    private TextField commitTitleField;

    @FXML
    private TextArea commitDescriptionField;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialization logic when the scene loads
        System.out.println("Home Page initialized!");
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
            System.out.println("Opened repository: " + selectedFile.getAbsolutePath());
            showAlert("Repository Opened", "Opened repository: " + selectedFile.getName());
        }
    }

    @FXML
    private void handleViewHistory(ActionEvent event) {
        System.out.println("View History clicked");
        historyListView.getItems().clear();
        historyListView.getItems().addAll(
                "Commit 1 - Initial project setup",
                "Commit 2 - Added new feature",
                "Commit 3 - Bug fixes and cleanup"
        );
        showAlert("History Loaded", "Commit history loaded successfully.");
    }

    @FXML
    private void handleSettings(ActionEvent event) {
        System.out.println("Settings clicked");
        showAlert("Settings", "Settings page under development!");
    }

    @FXML
    private void handleCommit(ActionEvent event) {
        String title = commitTitleField.getText();
        String description = commitDescriptionField.getText();

        if (title.isEmpty()) {
            showAlert("Error", "Please enter a commit title.");
            return;
        }

        System.out.println("Commit: " + title + " — " + description);
        historyListView.getItems().add(0, "New Commit: " + title);
        showAlert("Commit Successful", "Changes committed successfully!");
    }

    @FXML
    private void handleFetch(ActionEvent event) {
        System.out.println("Fetch Origin clicked");
        showAlert("Fetch", "Fetching updates from remote repository...");
    }

    // =========================
    // HELPER METHOD
    // =========================
    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
