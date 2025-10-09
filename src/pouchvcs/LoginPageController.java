package pouchvcs;

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
     * This method loads the Home.FXML file and sets it as the new scene 
     * on the current stage (window).
     * @param event The ActionEvent triggered by the button click.
     */
    @FXML
    private void handleLoginButtonAction(ActionEvent event) {
        try {
            // 1. Load the FXML for the Home page (assuming you have a file named Home.FXML)
            Parent homePageRoot = FXMLLoader.load(getClass().getResource("HomePage.FXML"));
            
            // 2. Get the current Stage object from the button that was clicked
            // This is the window that currently holds the Login screen.
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            // 3. Create a new Scene for the Home page
            Scene homeScene = new Scene(homePageRoot);
            
            // 4. Set the new Scene and update the stage title
            stage.setScene(homeScene);
            stage.setTitle("Home Page");
            stage.show();
            
        } catch (IOException e) {
            // Print an error if Home.FXML can't be found or loaded
            System.err.println("Failed to load Home.FXML. Make sure the file exists in your project resources.");
            e.printStackTrace();
        }
    }
}
