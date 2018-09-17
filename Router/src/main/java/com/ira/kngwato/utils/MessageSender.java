package com.ira.kngwato.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

public abstract class MessageSender {
    private MessageSender nexSender;
    public abstract void sendMessage(HashMap<String, SocketChannel> markets, HashMap<String, SocketChannel> brokers,
                            HashMap<String, String> message, SocketChannel socketChannel, ByteBuffer buffer) throws IOException;
    public void setNexSender(MessageSender nexSender) {
        this.nexSender = nexSender;
    }
    public MessageSender getNexSender() {
        return nexSender;
    }
}
