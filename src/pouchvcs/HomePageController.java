package pouchvcs;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import pouchvcs.model.User;
import pouchvcs.service.PouchVCS;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the HomePage.FXML. Handles all core VCS operations:
 * init, add, commit, checkout, and displaying history/status.
 */
public class HomePageController {

    // --- UI Elements ---
    @FXML private Label welcomeLabel;
    @FXML private Label currentDirectoryLabel;
    @FXML private TextArea statusTextArea;
    @FXML private ListView<String> historyListView;
    @FXML private TextField commitMessageField;

    // --- Services and State ---
    private User currentUser;
    private final PouchVCS pouchVCS = new PouchVCS();
    private Path workingDirectory; // Represents the local folder being tracked

    /**
     * Initializes the controller. Called after FXML elements are loaded.
     */
    @FXML
    public void initialize() {
        // Default to the user's home directory until a project is selected
        workingDirectory = Paths.get(System.getProperty("user.home"), "PouchProjects");
        updateWorkingDirectoryLabel(workingDirectory);

        // Ensure the base project directory exists
        if (!workingDirectory.toFile().exists()) {
            workingDirectory.toFile().mkdirs();
        }

        // Check if a Pouch repo already exists in this default directory
        try {
            if (pouchVCS.isInitialized(workingDirectory)) {
                appendStatus("Repository found. Ready for operations.");
                updateHistory();
            } else {
                appendStatus("No Pouch repository found. Use 'Init Repository' to start.");
            }
        } catch (Exception e) {
            appendError("Initialization check failed: " + e.getMessage());
        }
    }

    /**
     * Sets the logged-in user and updates the welcome label.
     * Called by LoginPageController after successful login.
     * @param user The authenticated user object.
     */
    public void setUser(User user) {
        this.currentUser = user;
        welcomeLabel.setText("Welcome, " + user.getUsername() + "!");
    }

    // --- VCS Actions ---

    /**
     * Handles the "Initialize Repository" button action.
     */
    @FXML
    private void handleInitRepository() {
        try {
            pouchVCS.init(workingDirectory);
            appendStatus("Successfully initialized Pouch repository in: " + workingDirectory.toString());
            updateHistory();
        } catch (Exception e) {
            appendError("Failed to initialize repository: " + e.getMessage());
        }
    }

    /**
     * Handles the "Add File" button action. Opens a file chooser.
     */
    @FXML
    private void handleAddFile() {
        if (!checkVCSReady()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Word Document (.docx) to Add");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Word Documents", "*.docx")
        );

        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            try {
                // Ensure the file path is relative to the working directory
                Path relativePath = workingDirectory.relativize(file.toPath());
                pouchVCS.addFile(file.toPath());
                appendStatus("Added file to staging: " + relativePath.toString());
                // In a real app, you'd update a 'Staging' view here
            } catch (Exception e) {
                appendError("Failed to add file: " + e.getMessage());
            }
        }
    }

    /**
     * Handles the "Commit Changes" button action.
     */
    @FXML
    private void handleCommit() {
        if (!checkVCSReady()) return;

        String message = commitMessageField.getText().trim();
        if (message.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Message", "Please enter a commit message.");
            return;
        }

        try {
            String commitHash = pouchVCS.commit(workingDirectory, currentUser.getUsername(), message);
            appendStatus("New commit created: " + commitHash);
            commitMessageField.clear();
            updateHistory();
        } catch (Exception e) {
            appendError("Commit failed: " + e.getMessage());
        }
    }

    /**
     * Handles the "Checkout Version" action from the history list.
     */
    @FXML
    private void handleCheckout() {
        String selectedCommit = historyListView.getSelectionModel().getSelectedItem();

        if (selectedCommit == null || !checkVCSReady()) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a commit from the history list to check out.");