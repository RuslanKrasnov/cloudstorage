package com.geekbrains.cloud.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.commons.io.FileUtils;
import com.geekbrains.cloud.netty.model.*;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import static org.apache.commons.io.FileUtils.deleteDirectory;

public class NettyClient implements Initializable {
    public TableView<FileInfo> clientView;
    public TableView<FileInfo> serverView;
    public TextField clientPath;
    public TextField serverPath;
    private Path clientDir;
    private int activeTable = 0;
    private ObjectEncoderOutputStream oos;
    private ObjectDecoderInputStream ois;
    private final int BUFFER_SIZE = 8192;
    private byte[] buf;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            Socket socket = new Socket("localhost", 8189);
            oos = new ObjectEncoderOutputStream(socket.getOutputStream());
            ois = new ObjectDecoderInputStream(socket.getInputStream());

            buf = new byte[BUFFER_SIZE];

            TableColumn<FileInfo, String> fileNameColumn = new TableColumn<>("Название");
            fileNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
            fileNameColumn.setPrefWidth(250.0f);

            TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Размер");
            fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
            fileSizeColumn.setCellFactory(column -> new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        if (item == -1L) {
                            text = "[ DIR ]";
                        }
                        setText(text);
                    }
                }
            });
            fileSizeColumn.setPrefWidth(120);

            TableColumn<FileInfo, String> fileDateColumn = new TableColumn("Дата изменения");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
            fileDateColumn.setPrefWidth(120);

            TableColumn<FileInfo, String> serverFileNameColumn = new TableColumn<>("Название");
            serverFileNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
            serverFileNameColumn.setPrefWidth(250.0f);

            TableColumn<FileInfo, Long> serverFileSizeColumn = new TableColumn<>("Размер");
            serverFileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
            serverFileSizeColumn.setCellFactory(column -> new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        if (item == -1L) {
                            text = "[ DIR ]";
                        }
                        setText(text);
                    }
                }
            });
            serverFileSizeColumn.setPrefWidth(120);

            TableColumn<FileInfo, String> serverFileDateColumn = new TableColumn("Дата изменения");
            DateTimeFormatter dtfserver = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            serverFileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtfserver)));
            serverFileDateColumn.setPrefWidth(120);

            clientView.getColumns().addAll(fileNameColumn, fileSizeColumn, fileDateColumn);
            serverView.getColumns().addAll(serverFileNameColumn, serverFileSizeColumn, serverFileDateColumn);

            clientDir = Paths.get("clientDir");
            updateClientView();
            clientView.setOnMouseClicked(e -> {
                activeTable = 0;
                if (e.getClickCount() == 2 && clientView.getSelectionModel().getSelectedItem() != null) {
                    Path selected = clientDir.resolve(clientView.getSelectionModel().getSelectedItem().getFileName());
                    if (Files.isDirectory(selected)) {
                        clientDir = selected;
                        updateClientView();
                    }
                }
            });
            serverView.setOnMouseClicked(e -> {
                activeTable = 1;
                if (e.getClickCount() == 2 && serverView.getSelectionModel().getSelectedItem() != null) {
                    String selectedServerItem = serverView.getSelectionModel().getSelectedItem().getFileName();
                    try {
                        updateServerView(selectedServerItem);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            });
            Thread readThread = new Thread(this::read);
            readThread.setDaemon(true);
            readThread.start();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public void download(ActionEvent actionEvent) throws IOException {
        if (serverView.getSelectionModel().getSelectedItem() != null) {
            oos.writeObject(new FileRequest(serverView.getSelectionModel().getSelectedItem().getFileName()));
        }
    }

    public void upload(ActionEvent actionEvent) throws IOException {
        FileInfo selectedItem = clientView.getSelectionModel().getSelectedItem();
        if ((selectedItem != null) && (!Files.isDirectory(clientDir.resolve(selectedItem.getFileName())))) {
            String fileName = clientView.getSelectionModel().getSelectedItem().getFileName();
            File selectedFile = clientDir.resolve(fileName).toFile();
            if (selectedFile.length() > 0) {
                try (InputStream fis = new FileInputStream(selectedFile)) {
                    while (fis.available() > 0) {
                        int readBytes = fis.read(buf);
                        FilePartMessage filePartMessage = new FilePartMessage(Arrays.copyOfRange(buf, 0, readBytes), clientView.getSelectionModel().getSelectedItem().getSize(), fileName);
                        oos.writeObject(filePartMessage);
                    }
                }
            } else {
                FilePartMessage filePartMessage = new FilePartMessage(new byte[0], 0, fileName);
                oos.writeObject(filePartMessage);
            }
        }
    }

    private void updateServerView(String path) throws IOException {
        oos.writeObject(new ServerListRequest(path));
    }

    public void updateClientView() {
        Platform.runLater(() -> {
            try {
                clientPath.setText(clientDir.normalize().toAbsolutePath().toString());
                clientView.getItems().clear();
                clientView.getItems().addAll(Files.list(clientDir).map(FileInfo::new).collect(Collectors.toList()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void read() {
        try {
            while (true) {
                CloudMessage msg = (CloudMessage) ois.readObject();
                switch (msg.getMessageType()) {
                    case FILE:
                        FilePartMessage fm = (FilePartMessage) msg;
                        if (!Files.exists(clientDir.resolve(fm.getName()))) {
                            Files.createFile(clientDir.resolve(fm.getName()));
                            Files.write(clientDir.resolve(fm.getName()), fm.getBytes(), StandardOpenOption.APPEND);
                        } else if (fm.getSize() > Files.size(clientDir.resolve(fm.getName()))) {
                            Files.write(clientDir.resolve(fm.getName()), fm.getBytes(), StandardOpenOption.APPEND);
                        }
                        updateClientView();
                        break;
                    case LIST:
                        ListMessage lm = (ListMessage) msg;
                        serverPath.setText(lm.getServerPath());
                        Platform.runLater(() -> {
                            serverView.getItems().clear();
                            serverView.getItems().addAll(lm.getFiles());
                        });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void upFolder(ActionEvent actionEvent) {
        Path upClientPath = clientDir.resolve("..").toAbsolutePath().normalize();
        if (Files.exists(upClientPath)) {
            clientDir = upClientPath;
            updateClientView();
        }
    }

    public void upFolderServer(ActionEvent actionEvent) throws IOException {
        updateServerView("[ UP SERVER COMMAND ]");
    }

    public void createFolder(ActionEvent actionEvent) {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("foldername.fxml"));
        try {
            loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Parent root = loader.getRoot();
        FolderNameController fnc = loader.getController();
        fnc.setNettyClient(this);
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Создать папку");
        stage.showAndWait();
    }

    public void createNewDirectory(String folderName) {
        if (activeTable == 0) {
            newClientDir(folderName);
        } else {
            newServerDir(folderName);
        }
    }

    private void newServerDir(String folderName) {
        try {
            oos.writeObject(new NewServerDirCommand(folderName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void newClientDir(String folderName) {
        Platform.runLater(() -> {
            Path newFolder = Paths.get(clientDir.toString() + "\\" + folderName);
            if (!Files.exists(newFolder)) {
                try {
                    Files.createDirectory(newFolder);
                    updateClientView();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void delete(ActionEvent actionEvent) {
        if (activeTable == 0) {
            deleteOnClient();
        } else {
            deleteOnServer();
        }
    }

    private void deleteOnServer() {
    }

    private void deleteOnClient() {
        FileInfo selected = clientView.getSelectionModel().getSelectedItem();
        Path pathToDelete = Paths.get(clientDir.resolve(selected.getFileName()).toString());
        try {
            Files.walkFileTree(pathToDelete, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws
                        IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateClientView();
    }
}