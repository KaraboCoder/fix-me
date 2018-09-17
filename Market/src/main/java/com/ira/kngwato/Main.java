package com.ira.kngwato;

import com.ira.kngwato.utils.Logger;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        String id = "";
        String[] status = {"2", "8"};
        try {
            SocketChannel market = SocketChannel.open();
            market.configureBlocking(false);

            ReadableByteChannel in = Channels.newChannel(System.in);

            market.connect(new InetSocketAddress(5001));

            Selector selector = Selector.open();
            market.register(selector, SelectionKey.OP_READ);

            printWelcome();

            while (market.finishConnect()) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                for (SelectionKey key : keys) {
                    if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        market.read(buffer);
                        buffer.flip();
                        HashMap<String, String> message = formatMessage(new String(StandardCharsets.UTF_8.decode(buffer).array()));
                        Logger.log(Logger.INFO, "New Message from: " + message.get("49"));
                        buffer.flip();
                        Logger.log(Logger.MESSAGE, new String(StandardCharsets.UTF_8.decode(buffer).array()));
                        if (message.get("49").equalsIgnoreCase("ADMIN")) {
                            if (message.get("35").equalsIgnoreCase("ID")) {
                                id = message.get("56");
                                Logger.log(Logger.INFO, "Market ID is: " + id);
                            }else if(message.get("35").equalsIgnoreCase("ERROR")) {
                                Logger.log(Logger.ERROR, "Internal Server Error");
                            }else if(message.get("35").equalsIgnoreCase("OK")) {
                                Logger.log(Logger.INFO, "Message Received");
                            }
                        }else {
                            if (message.get("35").equalsIgnoreCase("D")) {
                                Logger.log(Logger.INFO, "New Order Received");

                                CharBuffer charBuffer = CharBuffer.allocate(1024);
                                StringBuilder newMessage = (new StringBuilder())
                                        .append("35=8;49="+id+";")
                                        .append("56="+message.get("49")+";")
                                        .append("39=" + status[(new Random().nextInt(2))] + ";");
                                int messageLength = calculateMessageLength(formatMessage(newMessage.toString()));
                                int checksum = calculateChecksum(formatMessage(newMessage.toString()));
                                String checksumStr = "";
                                if (checksum < 10) checksumStr += "0";
                                if (checksum < 100) checksumStr += "0";
                                charBuffer.put("8=FIX.4.2;9="+messageLength+";"+newMessage.toString()+"10="+checksumStr+checksum);
                                charBuffer.flip();
                                market.write(StandardCharsets.UTF_8.encode(charBuffer));
                            }else{
                                Logger.log(Logger.ERROR, "Message Format Error.");
                            }
                        }
                    }
                    keys.remove(key);
                }
            }
        }catch (Exception e) {
            Logger.log(Logger.ERROR, "Connection lost.");
            System.exit(1);
        }
    }

    private static HashMap<String, String> formatMessage(String message) {
        String[] msg = message.split(";");
        HashMap<String, String> messageTable = new HashMap();
        for (String part : msg) {
            messageTable.put(part.split("=")[0], part.split("=")[1]);
        }
        return messageTable;
    }

    private static int calculateChecksum(HashMap<String, String> message) {
        int asciiValueOfAllChars = calculateAsciiValue(message) + (message.size() -1)*(int)';';
        int checkSum = asciiValueOfAllChars % 256;
        return checkSum;
    }

    private static int calculateAsciiValue(HashMap<String, String> message) {
        Set<String> messageKeys = message.keySet();
        Collection<String> messageValues = message.values();
        int asciiValue = 0;
        for (String key : messageKeys) {
            char[] charArray = key.toCharArray();
            for (int i = 0; i < charArray.length; i++) {
                asciiValue += (int)charArray[i];
            }
        }
        for (String value : messageValues) {
            char[] charArray = value.toCharArray();
            for (int i = 0; i < charArray.length; i++) {
                asciiValue += (int)charArray[i];
            }
        }
        return asciiValue + 1;
    }

    private static int calculateMessageLength(HashMap<String, String> message) {
        int messageLength = 0;
        Set<String> messageKeys = message.keySet();
        Collection<String> messageValues = message.values();
        for (String key : messageKeys) {
            messageLength += key.length();
        }
        for (String value : messageValues) {
            messageLength += value.length();
        }
        return messageLength + message.size();
    }

    private static void printWelcome() {
        System.out.println("############################################################");
        System.out.println("##################### MARKET INITIATED #####################");
        System.out.println("############################################################\n\n");
    }
}
