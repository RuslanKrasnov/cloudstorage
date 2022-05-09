package com.geekbrains.cloud.netty.model;

import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Data
public class FilePartMessage implements CloudMessage{
    private final byte[] bytes;
    private final long size;
    private final String name;

    public FilePartMessage(byte[] bytes, long size, String name) {
        this.bytes = bytes;
        this.size = size;
        this.name = name;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.FILE;
    }
}
