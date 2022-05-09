package com.geekbrains.cloud.netty.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;

@Data
public class ListMessage implements CloudMessage {

    private List<FileInfo> files;
    private String serverPath;

    public ListMessage(Path path) {
        this.serverPath = path.toAbsolutePath().toString();
        try {
            this.files = Files.list(path).map(path1 -> new FileInfo(path1)).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Error of scan");
        }
    }

    public String getServerPath() {
        return serverPath;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.LIST;
    }
}
