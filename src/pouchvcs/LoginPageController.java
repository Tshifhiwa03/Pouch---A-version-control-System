package pouchvcs;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import pouchvcs.service.DatabaseManager;
import pouchvcs.model.User;

import java.io.IOException;

/**
 * Controller for the LoginPage.FXML. Handles user login and registration.
 */
public class LoginPageController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Text messageText;

    private final DatabaseManager dbManager = new DatabaseManager();

    /**
     * Initializes the controller. Sets up the initial database connection.
     */
    @FXML
    public void initialize() {
        // Attempt to connect to the database when the controller loads.
        try {
            dbManager.connect();
            messageText.setText("Status: Database Connected.");
        } catch (Exception e) {
            messageText.setText("Error: Database connection failed.");
            System.err.println("DB Connection Error: " + e.getMessage());
        }
    }

    /**
     * Handles the login button action.
     */
    @FXML
    private void handleLoginAction() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Login Error", "Username and password are required.");
            return;
        }

        try {
            User user = dbManager.login(username, password);
            if (user != null) {
                // Success: Switch to the main application view
                messageText.setText("Login Successful for: " + user.getUsername());
                switchToHomePage(user);
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "System Error", "An error occurred during login.");
            System.err.println("Login Exception: " + e.getMessage());
        }
    }

    /**
     * Handles the register button action.
     */
    @FXML
    private void handleRegisterAction() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Registration Error", "Username and password are required.");
            return;
        }

        try {
            boolean success = dbManager.registerUser(username, password);
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Registration Success", "User registered successfully! You may now log in.");
            } else {
                showAlert(Alert.AlertType.WARNING, "Registration Failed", "Username already exists or database error occurred.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "System Error", "An error occurred during registration.");
            System.err.println("Registration Exception: " + e.getMessage());
        }
    }

    /**
     * Helper method to switch the scene to the HomePage.
     */
    private void switchToHomePage(User user) throws IOException {
        // Load the FXML for the Home Page
        FXMLLoader loader = new FXMLLoader(getClass().getResource("HomePage.FXML"));
        Parent root = loader.load();

        // Pass the logged-in user to the Home Page Controller
        HomePageController homeController = loader.getController();
        homeController.setUser(user);

        // Get the current stage and set the new scene
        Stage stage = (Stage) usernameField.getScene().getWindow();
        Scene scene = new Scene(root, 800, 600); // Set a larger size for the main app
        stage.setTitle("Pouch VCS - Welcome, " + user.getUsername());
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Helper method to show JavaFX Alert windows.
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}