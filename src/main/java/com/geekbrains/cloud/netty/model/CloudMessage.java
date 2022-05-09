package com.geekbrains.cloud.netty.model;

import java.io.Serializable;

public interface CloudMessage extends Serializable {
    MessageType getMessageType();
}
