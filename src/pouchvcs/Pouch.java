package pouchvcs;

import java.io.IOException;
import java.net.URL; 
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main application class for the Pouch VCS.
 * Initializes the JavaFX environment and loads the LoginPage UI.
 */
public class Pouch extends Application {
    
    private static final int SCENE_WIDTH = 600;
    private static final int SCENE_HEIGHT = 450;
    
    @Override
    public void start(Stage primaryStage) throws IOException {
        try {
            // --- 1. Load FXML ---
            URL fxmlUrl = getClass().getResource("LoginPage.FXML");
            if (fxmlUrl == null) {
                // If FXML is missing, we can't proceed.
                System.err.println("FATAL ERROR: LoginPage.FXML not found. Ensure it is in the 'pouchvcs' package folder.");
                return;
            }
            Parent root = FXMLLoader.load(fxmlUrl);
            
            // --- 2. Create Scene and Load CSS ---
            Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT); 
            
            // Safely load the loginpage.css file for the initial screen.
            URL cssUrl = getClass().getResource("loginpage.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                // Warning if the specific CSS file is missing.
                System.err.println("WARNING: loginpage.css not found. The app will run unstyled. Check resource path and ensure it is named 'loginpage.css'.");
            }
            
            // --- 3. Display Stage ---
            primaryStage.setTitle("Pouch VCS - User Login");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();
            
        } catch (Exception e) {
            System.err.println("Error during application start: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
