package com.ira.kngwato;

import com.ira.kngwato.utils.Logger;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        String id = "";
        try {
            SocketChannel broker = SocketChannel.open();
            broker.configureBlocking(false);

            ReadableByteChannel in = Channels.newChannel(System.in);

            broker.connect(new InetSocketAddress(5000));

            Selector selector = Selector.open();
            broker.register(selector, SelectionKey.OP_READ);

            printWelcome();

            ByteBuffer b = ByteBuffer.allocate(1024);
            while (broker.finishConnect()) {
                System.out.println("Waiting for response...\n");
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                for (SelectionKey key : keys) {
                    if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        broker.read(buffer);
                        buffer.flip();
                        HashMap<String, String> message = formatMessage(new String(StandardCharsets.UTF_8.decode(buffer).array()));
                        Logger.log(Logger.INFO, "New Message from: " + message.get("49"));
                        buffer.flip();
                        Logger.log(Logger.MESSAGE, new String(StandardCharsets.UTF_8.decode(buffer).array()));
                        if (message.get("49").equalsIgnoreCase("ADMIN")) {
                            if (message.get("35").equalsIgnoreCase("ID")) {
                                id = message.get("56");
                                Logger.log(Logger.INFO, "Your connection ID is: " + id);
                            }else if(message.get("35").equalsIgnoreCase("ERROR")) {
                                Logger.log(Logger.ERROR, "Internal Server Error");
                            }else if(message.get("35").equalsIgnoreCase("OK")) {
                                Logger.log(Logger.INFO, "Message Received");
                            }
                        }else {
                            if (message.get("39").equalsIgnoreCase("2")) {
                                Logger.log(Logger.INFO, "Order Executed");
                            }else if(message.get("39").equalsIgnoreCase("8")) {
                                Logger.log(Logger.INFO, "Order Rejected");
                            }else{
                                Logger.log(Logger.ERROR, "Message Format Error.");
                            }
                        }
                    }
                    keys.remove(key);
                }
                System.out.print("\nEnter ID: ");
                in.read(b);
                b.flip();
                CharBuffer charBuffer = CharBuffer.allocate(1024);
                StringBuilder message = (new StringBuilder())
                        .append("35=D;49="+id+";")
                        .append("56="+new String(StandardCharsets.UTF_8.decode(b).array()).trim())//ID of Market
                        .append(";55=GOOGLE;54=SELL;38=2;44=300;");
                int messageLength = calculateMessageLength(formatMessage(message.toString()));
                int checksum = calculateChecksum(formatMessage(message.toString()));
                String checksumStr = "";
                if (checksum < 10) checksumStr += "0";
                if (checksum < 100) checksumStr += "0";
                charBuffer.put("8=FIX.4.2;9="+messageLength+";"+message.toString()+"10="+checksumStr+checksum);
                charBuffer.flip();
                broker.write(StandardCharsets.UTF_8.encode(charBuffer));
                b.clear();
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
        System.out.println("##################### BROKER INITIATED #####################");
        System.out.println("############################################################\n\n");
    }
}
