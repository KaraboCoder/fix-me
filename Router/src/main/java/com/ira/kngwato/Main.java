package com.ira.kngwato;

import com.ira.kngwato.utils.BrokerSender;
import com.ira.kngwato.utils.Logger;
import com.ira.kngwato.utils.MarketSender;
import sun.rmi.runtime.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

public class Main {
    private static HashMap<String, SocketChannel> brokers = new HashMap<String, SocketChannel>();
    private static HashMap<String, SocketChannel> markets = new HashMap<String, SocketChannel>();
    private static BrokerSender brokerSender = new BrokerSender();
    private static MarketSender marketSender = new MarketSender();

    public static void main(String[] args) {
        brokerSender.setNexSender(marketSender);
        try {
            ServerSocketChannel marketServerSocketChannel = ServerSocketChannel.open();
            marketServerSocketChannel.configureBlocking(false);

            ServerSocketChannel brokerServerSocketChannel = ServerSocketChannel.open();
            brokerServerSocketChannel.configureBlocking(false);

            ServerSocket brokerSocket = brokerServerSocketChannel.socket();
            brokerSocket.bind(new InetSocketAddress(5000));

            ServerSocket marketSocket = marketServerSocketChannel.socket();
            marketSocket.bind(new InetSocketAddress(5001));

            Selector selector = Selector.open();
            brokerServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            marketServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            printWelcome();

            while (true) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();

                for (SelectionKey key : keys) {
                    if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        String id = generateID();
                        if (serverSocketChannel.getLocalAddress().toString().contains("5000")) {
                            brokers.put(id, socketChannel);
                            Logger.log(Logger.INFO, "New Broker Connected. ID: " + id);
                        }else if(serverSocketChannel.getLocalAddress().toString().contains("5001")){
                            markets.put(id, socketChannel);
                            Logger.log(Logger.INFO, "New Market Connected. ID: " + id);
                        }
                        sendIDToClient(id,socketChannel);
                        socketChannel.configureBlocking(false);
                        socketChannel.register(selector, SelectionKey.OP_READ);
                        keys.remove(key);
                    }else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ){
                        SocketChannel socketChannel = (SocketChannel)key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        socketChannel.read(buffer);
                        buffer.flip();
                        HashMap<String, String> message = formatMessage(new String(StandardCharsets.UTF_8.decode(buffer).array()));
                        Logger.log(Logger.INFO, "New Message from: " + message.get("49"));
                        buffer.flip();
                        Logger.log(Logger.MESSAGE, new String(StandardCharsets.UTF_8.decode(buffer).array()));

                        int checksum = calculateChecksum(message);
                        if (checksum == Integer.parseInt(message.get("10"))) {
                            Logger.log(Logger.INFO, "Checksum Validated.");
                            Logger.log(Logger.INFO, "Forwarding Message to: " + message.get("56"));
                            buffer.flip();
                            brokerSender.sendMessage(markets,brokers,message,socketChannel,buffer);
                        }else {
                            String errorMessage = "8=FIX.4.2;35=ERROR;49=ADMIN;56="+message.get("49");
                            socketChannel.write(StandardCharsets.UTF_8.encode(errorMessage));
                            Logger.log(Logger.ERROR, "Checksum Invalid");
                        }

                        buffer.clear();
                        keys.remove(key);
                    }
                }
            }
        }catch (Exception e) {
            Logger.log(Logger.ERROR, "Server Terminated");
        }
    }

    private static String generateID() {
        int[] numbers = (new Random()).ints(6,0,10).toArray();
        String id = "";
        for(int i : numbers) {
            id += i;
        }
        return id;
    }

    private static void sendIDToClient(String id, SocketChannel client) {
        CharBuffer buffer = CharBuffer.allocate(1024);
        buffer.put(id);
        buffer.flip();
        try {
            String message = "8=FIX.4.2;9=100;35=ID;49=ADMIN;56="+id+";10=222";
            client.write(StandardCharsets.UTF_8.encode(message));
        } catch (IOException e) {
            Logger.log(Logger.ERROR, "Could not send ID to client");
        }
    }

    private  static HashMap<String, String> formatMessage(String message) {
        String[] msg = message.split(";");
        HashMap<String, String> messageTable = new HashMap();
        for (String part : msg) {
            messageTable.put(part.split("=")[0], part.split("=")[1]);
        }
        return messageTable;
    }

    private static int calculateChecksum(HashMap<String, String> message) {
        HashMap<String, String> msg = (HashMap<String, String>) message.clone();
        msg.remove("8");
        msg.remove("9");
        msg.remove("10");
        int asciiValueOfAllChars = calculateAsciiValue(msg) + (msg.size() -1)*(int)';';
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

    private static void printWelcome() {
        System.out.println("############################################################");
        System.out.println("##################### ROUTER INITIATED #####################");
        System.out.println("############################################################");
        System.out.println("###### WAITING FOR CONNECTIONS ON PORT: 5000 AND 5001 ######");
        System.out.println("############################################################\n\n");
    }
}
