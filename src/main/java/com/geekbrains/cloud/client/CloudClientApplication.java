package com.geekbrains.cloud.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

public class CloudClientApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(CloudClientApplication.class.getResource("auth.fxml"));
        Parent parent = loader.load();
        primaryStage.setTitle("Authorization");
        primaryStage.setScene(new Scene(parent));
        primaryStage.show();
    }

}
