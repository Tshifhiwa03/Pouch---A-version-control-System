package PouchUI;

import PouchDatabase.DataBaseConnection;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javafx.scene.control.Alert;
import java.sql.SQLException;

/**
 * Controller for LoginPage.fxml.
 * Handles user login and navigation to the HomePage.
 */
public class LoginPageController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    /**
     * Handles login button click event.
     */
    @FXML
    private void handleLoginButtonAction(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Input Error", "Please enter both username and password.");
            return;
        }

        if (authenticateUser(username, password)) {
            switchToHomePage(event);
        } else {
            showAlert("Login Failed", "Invalid username or password.");
        }
    }

    /**
     * Attempts to authenticate the user across multiple databases.
     */
    private boolean authenticateUser(String username, String password) {
        String query = "SELECT * FROM user_account WHERE username = ? AND password = ?";

        // List of databases to try
        String[][] dbConfigs = {
            {"users", "root", "DrTnet@170621"},
            {"softwareprogramming", "root", "GhRyawbU@6"},
            {"logins", "root", "Leandra@mysql24"}, 
            {"entry", "root", "Badbich_11"}
        };

        DataBaseConnection dbConn = new DataBaseConnection();
        for (String[] config : dbConfigs) {
    String dbName = config[0];
    String dbUser = config[1];
    String dbPass = config[2];

    try (Connection conn = dbConn.getConnection(dbName, dbUser, dbPass);
         PreparedStatement stmt = conn.prepareStatement(query)) {

        stmt.setString(1, username);
        stmt.setString(2, password);

        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                System.out.println("Authenticated on database: " + dbName);
                return true;
            }
        }

    } catch (SQLException e) {
        // Skip databases that cannot be connected to
        System.out.println("Skipping unavailable database: " + dbName);
    }
}

        return false;
    }

    /**
     * Navigates to the HomePage scene.
     */
    private void switchToHomePage(ActionEvent event) {
        try {
            Parent homePageRoot = FXMLLoader.load(getClass().getResource("HomePage.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene homeScene = new Scene(homePageRoot);
            stage.setScene(homeScene);
            stage.setTitle("Home Page");
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load HomePage.fxml.");
            e.printStackTrace();
            showAlert("Error", "Could not load Home Page.");
        }
    }

    /**
     * Displays an alert with given title and message.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
