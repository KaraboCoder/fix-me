package com.ira.kngwato.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

public class BrokerSender extends MessageSender {
    @Override
    public void sendMessage(HashMap<String, SocketChannel> markets, HashMap<String, SocketChannel> brokers,
                            HashMap<String, String> message, SocketChannel socketChannel, ByteBuffer buffer) throws IOException {
        if (brokers.containsKey(message.get("56")) && !brokers.containsKey(message.get("49"))) {
            brokers.get(message.get("56")).write(buffer);
        }else {
            this.getNexSender().sendMessage(markets, brokers, message, socketChannel, buffer);
        }
    }
}
