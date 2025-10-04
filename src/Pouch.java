/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMain.java to edit this template
 */

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main application class for the Pouch application.
 * It loads the UI definition from LoginPage.FXML.
 */
public class Pouch extends Application {
    
    @Override
    public void start(Stage primaryStage) throws IOException {
        // 1. Load the FXML file. Ensure "LoginPage.FXML" is in the correct classpath location.
        Parent root = FXMLLoader.load(getClass().getResource("LoginPage.FXML")); 
        
        // 2. Create the Scene using the root node loaded from FXML.
        // The size (300, 250) can be adjusted or often ignored if the FXML
        // root element has preferred size properties set.
        Scene scene = new Scene(root, 300, 250); 
        
        // 3. Set the requested title.
        primaryStage.setTitle("Login");
        
        // 4. Set the scene on the stage and display it.
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
