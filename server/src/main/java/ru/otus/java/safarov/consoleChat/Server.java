package ru.otus.java.safarov.consoleChat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import static ru.otus.java.safarov.consoleChat.ServerApplication.logger;

public class Server {
    private final int port;
    private final Map<String, ClientHandler> clients;
    private final Map<String, List<ClientHandler>> groups;
    private final AuthenticatedProvider authenticatedProvider;

    public Server(int port) {
        this.port = port;
        clients = new HashMap<>();
        groups = new HashMap<>();
        authenticatedProvider = new AuthenticationProvider(this);
        authenticatedProvider.initialize();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Сервер запущен. Порт: {}", port);
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public AuthenticatedProvider getAuthenticatedProvider() {
        return authenticatedProvider;
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.put(clientHandler.getName(), clientHandler);
    }

    public synchronized void changeNick(ClientHandler clientHandler, String oldUsername){
        clients.put(clientHandler.getName(), clientHandler);
        clients.remove(oldUsername);
    }

    public synchronized void broadcastMessage(String msg) {
        for (Map.Entry<String, ClientHandler> client : clients.entrySet()) {
            client.getValue().sendMessage(msg + " time: " + new Date());
        }
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler.getName());
    }

    public synchronized void sendMessageClient(ClientHandler clientHandler, String recipient, String msgToPersonal) {
        if (clients.containsKey(recipient)) {
            clients.get(recipient).sendMessage(clientHandler.getName() + ": " + msgToPersonal + " time: " +
                    new Date());
        } else {
            clientHandler.sendMessage("Клиента с ником " + recipient + " нет в сети." +
                    " time: " + new Date());
        }
    }

    public synchronized void sendList(ClientHandler clientHandler) {
        clientHandler.sendMessage("active users: " + String.join(" ", clients.keySet()));
    }

    public synchronized boolean isName(String name) {
        return clients.containsKey(name);
    }

    public boolean closeUser(String name) {
        ClientHandler closeClient = clients.get(name);
        if (closeClient == null) {
            return false;
        }
        closeClient.sendMessage("/exitok");
        clients.remove(name);
        return true;
    }

    public void shutdown() {
        for(ClientHandler client: clients.values()){
            if (client.getName().equals("admin")){
                continue;
            }
            client.exit();
            client.disconnect();
        }
    }

    public synchronized void sendMessageGroup(ClientHandler clientHandler, String groupTitle,
                                              String msgToGroup, List<String> users) {
        String msg = groupTitle + "-" + clientHandler.getName() + ": " + msgToGroup + ". time: " + new Date();
        for (ClientHandler user : groups.get(groupTitle)) {
            users.remove(user.getName());
            user.sendMessage(msg);
        }
        authenticatedProvider.addMsgToGroup(groupTitle, users, msg);

    }

    public synchronized void subscribeGroup(ClientHandler clientHandler) {
        if(groups.containsKey(clientHandler.getGroupTitle())){
            groups.get(clientHandler.getGroupTitle()).add(clientHandler);
        } else {
            groups.put(clientHandler.getGroupTitle(), new ArrayList<>(List.of(clientHandler)));
        }
    }

    public synchronized void unsubscribeGroup(ClientHandler clientHandler) {
        if (!clientHandler.getGroupTitle().isEmpty()) {
            groups.get(clientHandler.getGroupTitle()).remove(clientHandler);
            clientHandler.sendMessage("Вы вышли из группы " + clientHandler.getGroupTitle());
            clientHandler.setGroupTitle("");
        }
    }

    public synchronized void unsubscribeUserFromGroup(String groupTitle, String username) {
        for (ClientHandler clientHandler : groups.get(groupTitle)) {
            if(clientHandler.getName().equals(username)){
                unsubscribeGroup(clientHandler);
                break;
            }
        }
    }

    public synchronized void delGroup(String groupTitle) {
        if (groups.containsKey(groupTitle)){
            for (ClientHandler clientHandler : groups.get(groupTitle)) {
                clientHandler.leaveGroup();
            }
            groups.remove(groupTitle);
        }
    }
}
