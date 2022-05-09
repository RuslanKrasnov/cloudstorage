package com.geekbrains.cloud.netty.handler;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;

import com.geekbrains.cloud.netty.model.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class CloudMessageHandler extends SimpleChannelInboundHandler<CloudMessage> {
    private final int BUFFER_SIZE = 8192;
    private byte[] buf;
    private Path serverDir;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        serverDir = Paths.get("server");
        ctx.writeAndFlush(new ListMessage(serverDir.toAbsolutePath()));
        buf = new byte[BUFFER_SIZE];
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudMessage cloudMessage) throws Exception {
        switch (cloudMessage.getMessageType()) {
            case FILE:
                FilePartMessage filePart = (FilePartMessage) cloudMessage;
                if (!Files.exists(serverDir.resolve(filePart.getName()))) {
                    Files.createFile(serverDir.resolve(filePart.getName()));
                }
                if (filePart.getSize() > Files.size(serverDir.resolve(filePart.getName()))) {
                    Files.write(serverDir.resolve(filePart.getName()), filePart.getBytes(), StandardOpenOption.APPEND);
                }
                ctx.writeAndFlush(new ListMessage(serverDir));
                break;
            case FILE_REQUEST:
                FileRequest fr = (FileRequest) cloudMessage;
                String fileName = fr.getName();
                File selected = serverDir.resolve(fileName).toFile();
                try (InputStream fis = new FileInputStream(selected)) {
                    if (selected.length() > 0) {
                        while (fis.available() > 0) {
                            int readBytes = fis.read(buf);
                            FilePartMessage filePartMessage = new FilePartMessage(Arrays.copyOfRange(buf, 0, readBytes), selected.length(), fileName);
                            ctx.writeAndFlush(filePartMessage);
                        }
                    } else {
                        FilePartMessage filePartMessage = new FilePartMessage(new byte[0], 0, fileName);
                        ctx.writeAndFlush(filePartMessage);
                    }
                }
                break;
            case SERVER_LIST_REQUEST:
                ServerListRequest slr = (ServerListRequest) cloudMessage;
                String itemName = slr.getItem();
                if (itemName.equals("[ UP SERVER COMMAND ]")) {
                    Path upPath = serverDir.resolve("..").toAbsolutePath().normalize();
                    if (Files.exists(upPath)) {
                        serverDir = upPath;
                    }
                } else {
                    Path pathTo = serverDir.resolve(itemName).toAbsolutePath().normalize();
                    if (Files.isDirectory(pathTo)) {
                        serverDir = pathTo;
                    }
                }
                ctx.writeAndFlush(new ListMessage(serverDir));
                break;
            case NEW_SERVER_FOLDER:
                NewServerDirCommand nsdc = (NewServerDirCommand) cloudMessage;
                Path newFolder = Paths.get(serverDir.toString() + "\\" + nsdc.getFolderName());
                if (!Files.exists(newFolder)) {
                    try {
                        Files.createDirectory(newFolder);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    ctx.writeAndFlush(new ListMessage(serverDir));
                }
                break;
        }
    }
}
