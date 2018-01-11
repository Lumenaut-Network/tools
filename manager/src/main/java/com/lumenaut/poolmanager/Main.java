package com.lumenaut.poolmanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * @Author Luca Vignaroli
 * @Email luca@burning.it
 * @Date 10/01/2018 - 2:30 AM
 */
public class Main extends Application {
    // Primary stage reference
    private Stage primaryStage;

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Store the primary stage reference
        this.primaryStage = primaryStage;

        // Build root
        final Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));

        // Initialize the primary stage and show it
        primaryStage.setTitle("Lumenaut Pool Manager");
        primaryStage.setScene(new Scene(root, 1080, 750));
        primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream("/utils.png")));
        primaryStage.show();
    }
}
