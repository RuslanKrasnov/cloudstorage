package com.geekbrains.cloud.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthController {
    private JdbcApp database;
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passField;
    @FXML
    public Button tryAuthBtn;
    @FXML
    void initialize() {
        database = new JdbcApp();
        loginField.setFocusTraversable(false);
        passField.setFocusTraversable(false);
    }

    public void loginUser(ActionEvent actionEvent) {
        String login = loginField.getText().trim();
        String pass = passField.getText().trim();
        if (!login.equals("") && !pass.equals("")) {
            int count = 0;
            try {
                database.connect();
                ResultSet result = database.getUser(login, pass);
                while (result.next()) {
                    count++;
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            } finally {
                database.disconnect();
            }
            if (count > 0) {
                Stage stageAuth = (Stage) tryAuthBtn.getScene().getWindow();
                stageAuth.close();
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("layout.fxml"));
                try {
                    loader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Parent root = loader.getRoot();
                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Simple Cloud Storage version 1.0");
                stage.show();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Такой пользователь не зарегистрирован!", ButtonType.OK);
                alert.showAndWait();
            }
        }
    }
}