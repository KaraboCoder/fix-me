package com.ira.kngwato.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class MarketSender extends MessageSender {
    @Override
    public void sendMessage(HashMap<String, SocketChannel> markets, HashMap<String, SocketChannel> brokers,
                            HashMap<String, String> message, SocketChannel socketChannel, ByteBuffer buffer) throws IOException {
        if(markets.containsKey(message.get("56")) && !markets.containsKey(message.get("49"))) {
            markets.get(message.get("56")).write(buffer);
        }else {
            String errorMessage = "8=FIX.4.2;35=ERROR;49=ADMIN;56="+message.get("49");
            socketChannel.write(StandardCharsets.UTF_8.encode(errorMessage));
            Logger.log(Logger.ERROR, "Target ID: " + message.get("56") + " Not Found");
        }
    }
}
