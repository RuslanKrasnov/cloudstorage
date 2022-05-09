package com.geekbrains.cloud.netty.model;

import lombok.Data;

@Data
public class NewServerDirCommand implements CloudMessage{

       private String folderName;

       public NewServerDirCommand(String folderName){
           this.folderName = folderName;
       }

    @Override
    public MessageType getMessageType() {
        return MessageType.NEW_SERVER_FOLDER;
    }
}
