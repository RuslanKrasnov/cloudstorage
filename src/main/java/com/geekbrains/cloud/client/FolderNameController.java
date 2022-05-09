package com.geekbrains.cloud.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class FolderNameController {
    @FXML
    public Button createBtn;
    @FXML
    private TextField newFolderNameField;
    private NettyClient nettyClient;

    public void createNewFolder(ActionEvent actionEvent) {
        String name = newFolderNameField.getText();
        if (name.trim().length() > 0) {
            nettyClient.createNewDirectory(name);
        }
        Stage stage = (Stage) createBtn.getScene().getWindow();
        stage.close();
    }

    public void setNettyClient(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }
}
