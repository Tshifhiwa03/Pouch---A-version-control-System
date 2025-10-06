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

/**
 * Controller for the LoginPage.FXML.
 * Handles the login button action and transitions to the Home page.
 */
public class LoginPageController {
    // FXML fields for input and button (optional, but good practice)
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    /**
     * Handles the action when the Login button is clicked.
     * In a real application, this is where you would validate credentials.
     * For this example, it immediately switches to the Home page.
     * @param event The ActionEvent triggered by the button click.
     */
    @FXML
    private void handleLoginButtonAction(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        if (authenticateUser(username, password)) {
        try {
            // 1. Load the FXML for the Home page
            Parent homePageRoot = FXMLLoader.load(getClass().getResource("HomePage.fxml"));
            // 2. Get the current Stage object from the button that was clicked
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            // 3. Create a new Scene for the Home page
            Scene homeScene = new Scene(homePageRoot);
            // 4. Set the new Scene and update the stage title
            stage.setScene(homeScene);
            stage.setTitle("Home Page");
            stage.show();
        } catch (IOException e) {
            // Handle the error if Home.FXML can't be found or loaded
            System.err.println("Failed to load Home.FXML.");
            e.printStackTrace();
        }
    }
       else {
            showAlert("Login Failed", "Invalid username or password.");
        } 
    }
     private boolean authenticateUser(String username, String password) {
        String query = "SELECT * FROM user_account WHERE username = ? AND password = ?";

        try (Connection conn = new DataBaseConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            return rs.next(); // true if a match is found
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Displays an alert dialog with a given title and message.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
}
