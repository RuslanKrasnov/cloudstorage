package com.geekbrains.cloud.netty.model;

public class ServerListRequest implements CloudMessage{

    private String item;

    public ServerListRequest(String path) {
        this.item = path;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.SERVER_LIST_REQUEST;
    }

    public String getItem() {
        return item;
    }
}
