package ru.otus.java.safarov;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.*;

import static ru.otus.java.safarov.ServerApplication.logger;

public class ClientHandler {
    private final Server server;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private String name;
    private String groupTitle;

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.groupTitle = "";
        new Thread(() -> {
            try {
                while (true) {
                    String msg = in.readUTF();
                    if (msg.startsWith("/")) {
                        if (msg.startsWith("/exit")) {
                            exit();
                            break;
                        }
                        if (msg.startsWith("/auth ")) {
                            if (authClient(msg)) {
                                logger.info("Клиент {} прошел аутентификацию.", name);
                                break;
                            }
                            continue;
                        }
                        if (msg.startsWith("/register ")) {
                            if (regClient(msg)) {
                                logger.info("Клиент {} зарегистрирован.", name);
                                break;
                            }
                            continue;
                        }
                        if (msg.startsWith("/help")) {
                            sendHelp();
                            continue;
                        }
                    }
                    sendMessage("Отправка и получение сообщений доступна\n" +
                            "только после аутентификации (команда: /auth login passowrd)\n" +
                            "или после регистрации (команда: /register login password username)");
                }
                while (true) {
                    String msg = in.readUTF();
                    if (msg.startsWith("/")) {
                        if (msg.startsWith("/exit")) {
                            exit();
                            break;
                        } else if (msg.startsWith("/w ")) {
                            personalMsg(msg);
                        } else if (msg.startsWith("/activelist")) {
                            server.sendList(this);
                        } else if (msg.startsWith("/kick ")) {
                            kickUser(msg);
                        } else if (msg.startsWith("/changenick")) {
                            String oldName = getName();
                            if (changeNick(msg)) {
                                server.changeNick(this, oldName);
                                String infoMsg = "Клиент " + oldName + " изменил username на " + name;
                                logger.info(infoMsg);
                                sendMessage(infoMsg);
                            } else {
                                String infoMsg = "Не удалось изменить имя клиента " + oldName;
                                logger.info(infoMsg);
                            }
                        } else if (msg.startsWith("/changepassword")) {
                            if (changePassword(msg)) {
                                sendMessage("Пароль успешно изменен");
                            } else {
                                logger.info("Не удалось сменить пароль для пользователя {}", name);
                            }
                        } else if (msg.startsWith("/shutdown")) {
                            if (shutdownServer()) {
                                server.shutdown();
                                disconnect();
                                System.exit(0);
                            }
                        } else if (msg.startsWith("/group ")) {
                            if (createGroup(msg)) {
                                String newTitleGroup = msg.trim().split("\\s+")[1];
                                String resultAddGroup = "Группа " + newTitleGroup + " создана";
                                logger.info(resultAddGroup);
                                sendMessage(resultAddGroup);
                                enterGroup("/enter " + newTitleGroup);
                            }
                        } else if (msg.startsWith("/enter ")) {
                            enterGroup(msg);
                        } else if (msg.startsWith("/groupslist")) {
                            getTitlesGroups();
                        } else if (msg.startsWith("/requestaddgroup ")) {
                            requestAddGroup(msg);
                        } else if (msg.startsWith("/leavegroup")) {
                            leaveGroup();
                        } else if (msg.startsWith("/review")) {
                            acceptReview(msg);
                        } else if (msg.startsWith("/wg ")) {
                            groupMsg(msg);
                        } else if (msg.startsWith("/gkick ")) {
                            kickUserFromGroup(msg);
                        } else if (msg.startsWith("/deluser")) {
                            if (delUser(msg)) {
                                sendMessage("Пользователь удален");
                            } else {
                                logger.info("Не удалось удалить пользователя");
                            }
                        } else if (msg.startsWith("/delgroup")) {
                            if (delGroup(msg)) {
                                sendMessage("Группа удалена");
                            } else {
                                logger.info("Не удалось удалить группу");
                            }
                        } else if (msg.startsWith("/help")) {
                            sendHelp();
                        } else {
                            sendMessage("Не корректный ввод: " + msg);
                        }
                    } else {
                        server.broadcastMessage(name + ": " + msg);
                    }
                }
                disconnect();
            } catch (IOException e) {
                if (name == null) {
                    logger.info("Не аутентифицированный клиент отключился");
                } else {
                    logger.info("{} отключился", name);
                }
            } finally {
                disconnect();
            }
        }).start();
    }

    private void sendHelp() {
        String msg = "- /register – регистрация \n" +
                "- /auth – аутентификация \n" +
                "- /w – личное сообщение \n" +
                "- /exit – выход \n" +
                "- /activelist – список активных клиентов \n" +
                "- /changenick – смена имени пользователя \n" +
                "- /wg - сообщение группе \n" +
                "- /requestaddgroup (группа) - запрос на добавление в группу \n" +
                "- /review (пользователь)- добавление менеджером в группу \n" +
                "- /groupslist - получить список групп \n" +
                "- /leavegroup - выйти из группы \n" +
                "- /enter (имя группы) - войти в группу \n" +
                "- /gkick (пользователь) - менеджер группы удаляет пользователя из группы \n" +
                "- /changepassword (новый пароль) - изменение пароля для пользователя +\n" +
                "- /deluser - удаление пользователя \n" +
                "- /delgroup (имя группы) - удаление группы";
        sendMessage(msg);
    }

    private boolean delGroup(String msg) {
        String[] array = msg.trim().split("\\s+");
        if (array.length != 2) {
            sendMessage("Некорректный формат ввода команды /delgroup");
            return false;
        }
        if (server.getAuthenticatedProvider().isAdmin(this) ||
                server.getAuthenticatedProvider().isManagerGroup(this, array[1])) {
            return server.getAuthenticatedProvider().delGroup(array[1]);
        }
        sendMessage("Вы не являетесь администратором или создателем группы");
        return false;
    }

    private boolean delUser(String msg) {
        String[] array = msg.trim().split("\\s+");
        if (array.length > 2) {
            sendMessage("Некорректный формат ввода команды /deluser");
            return false;
        }
        String username;
        if (array.length == 1) {
            username = name;
        } else if (!server.getAuthenticatedProvider().isAdmin(this)) {
            sendMessage("Вы не являетесь администратором");
            return false;
        } else {
            username = array[1];
        }
        if (server.getAuthenticatedProvider().deleteUser(username)) {
            if (array.length == 1) {
                sendMessage("/exitok");
                disconnect();
            } else {
                kickUser("/kick " + username);
                return true;
            }
        }
        return false;
    }

    private boolean changePassword(String msg) {
        String[] array = msg.trim().split("\\s+");
        if (array.length != 2) {
            sendMessage("Некорректный формат ввода команды /changepassword");
            return false;
        }
        return server.getAuthenticatedProvider().changePassword(this, array[1]);
    }

    private void kickUserFromGroup(String msg) {
        String[] array = msg.trim().split(("\\s+"));
        if (array.length != 2) {
            sendMessage("Некорректный формат ввода /gkick");
        } else {
            server.getAuthenticatedProvider().kickUserFromGroup(this, array[1]);
            sendMessage("Пользователь " + array[1] + " удален из группы " + groupTitle);
        }
    }

    private void groupMsg(String msg) {
        msg = msg.trim().replaceAll("\\s+", " ");
        String[] array = msg.split(" ");
        if (array.length < 2) {
            sendMessage("Некорректный формат ввода");
        } else {
            String msgToGroup = String.join(", ", Arrays.stream(array, 1, array.length)
                    .toArray(String[]::new));
            List<String> users = server.getAuthenticatedProvider().getUsersToGroup(groupTitle);
            server.sendMessageGroup(this, groupTitle, msgToGroup, users);
        }
    }

    private void acceptReview(String msg) {
        String[] array = msg.trim().split(("\\s+"));
        if(!server.getAuthenticatedProvider().isManagerGroup(this, groupTitle)){
            sendMessage("Вы не являетесь создателем группы");
        } else {
            if (array.length > 1) {
                List<String> requestsAddGroups = server.getAuthenticatedProvider().getListRequest(this, groupTitle);
                List<String> addUsers = new ArrayList<>();
                for (int i = 1; i < array.length; i++) {
                    if (requestsAddGroups.contains(array[i])) {
                        addUsers.add(array[i]);
                    }
                }
                server.getAuthenticatedProvider().addUsersToGroup(this, addUsers);
            }
            server.getAuthenticatedProvider().removeRequestAddUserToGroup(this);
        }
    }

    public void leaveGroup() {
        if (groupTitle.isEmpty()) {
            sendMessage("Вы не входили ни в одну из групп");
        } else {
            server.unsubscribeGroup(this);
        }
    }

    private void requestAddGroup(String msg) {
        String[] array = msg.trim().split(("\\s+"));
        if (array.length != 2) {
            sendMessage("Некорректный формат ввода команды /addgroup");
        } else {
            if (!server.getAuthenticatedProvider().addRequestAddGroup(this, array[1])) {
                logger.info("Запрос на добавление группы {} не создан", array[1]);
            } else {
                sendMessage("Ваш запрос на добавление в группу " + array[1] + " создан");
                String groupOwner = server.getAuthenticatedProvider().getGroupOwner(array[1]);
                String msgGroupOwner = "review: " + name;
                server.sendMessageClient(this, groupOwner, msgGroupOwner);
            }
        }

    }

    private void enterGroup(String msg) {
        // /enter groupTitle
        String[] array = msg.trim().split("\\s+");
        if (array.length != 2) {
            sendMessage("Некорректный формат ввода команды /enter");
        } else {
            if (groupTitle.equals(array[1])) {
                sendMessage("Вы уже находитесь в группе " + groupTitle);
            } else {
                if (!groupTitle.isEmpty()) {
                    leaveGroup();
                }
                if (server.getAuthenticatedProvider().enterGroup(this, array[1])) {
                    getDeferredMessages();
                    reviewrequest();
                } else {
                    logger.info("Пользователю {} не удалось сменить группу на {}", getName(), array[1]);
                    sendMessage("Не удалось сменить группу на " + array[1]);
                }
            }
        }
    }

    private void getDeferredMessages() {
        List<String> messages = server.getAuthenticatedProvider().getListMsgForGroup(this);
        String msg = String.join("\n", messages);
        sendMessage(msg);
    }

    private void reviewrequest() {
        if (server.getAuthenticatedProvider().isManagerGroup(this, groupTitle)) {
            List<String> usersSentRequest = server.getAuthenticatedProvider().getListRequest(this, groupTitle);
//            requestsAddGroups.put(groupTitle, usersSentRequest);
            if (!usersSentRequest.isEmpty()) {
                sendMessage("review: " + String.join(" ", usersSentRequest));
            }
        }
    }

    public String getGroupTitle() {
        return groupTitle;
    }

    private void getTitlesGroups() {
        List<String> titles = server.getAuthenticatedProvider().getGroupTitle(this);
        sendMessage("list groups: " + String.join(" ", titles));
    }

    private boolean createGroup(String msg) {
        String[] array = msg.trim().split("\\s+");
        if (array.length != 2) {
            sendMessage("Некорректный формат ввода команды /group");
            return false;
        }
        return server.getAuthenticatedProvider().addGroup(this, array[1]);
    }

    private boolean shutdownServer() {
        if (server.getAuthenticatedProvider().isAdmin(this)) {
            server.broadcastMessage("/exitok");
            return true;
        }
        sendMessage("Вы не являетесь администратором");
        return false;
    }

    private boolean changeNick(String msg) {
        String[] array = msg.trim().split("\\s+");
        if (array.length != 2) {
            sendMessage("Некорректный формат ввода команды /auth");
            return false;
        }
        if (server.getAuthenticatedProvider().changeUsername(this, array[1])) {
            setName(array[1]);
            return true;
        }
        return false;
    }

    protected void disconnect() {
        server.unsubscribe(this);
        server.unsubscribeGroup(this);
        try {
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getName() {
        return name;
    }

    public void sendMessage(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setGroupTitle(String groupTitle) {
        this.groupTitle = groupTitle;
    }

    protected void exit() {
        sendMessage("/exitok");
        if (name != null) {
            logger.info("Клиенту {} отравлено сообщение о закрытии", name);
        }
    }

    private void personalMsg(String msg) {
        msg = msg.trim().replaceAll("\\s+", " ");
        String[] array = msg.split(" ");
        if (array.length < 3) {
            sendMessage("Некорректный формат ввода");
        } else {
            String recipient = array[1];
            String msgToPersonal = String.join(", ", Arrays.stream(array, 2, array.length)
                    .toArray(String[]::new));
            server.sendMessageClient(this, recipient, msgToPersonal);
        }
    }

    private boolean authClient(String msg) {
        String[] array = msg.trim().split("\\s+");
        if (array.length != 3) {
            sendMessage("Некорректный формат ввода команды /auth");
            return false;
        }
        return server.getAuthenticatedProvider().authenticate(this, array[1], array[2]);
    }

    private boolean regClient(String msg) {
        String[] array = msg.trim().split("\\s+");
        if (array.length != 4) {
            sendMessage("Некорректный формат ввода команды /register");
            return false;
        }
        return server.getAuthenticatedProvider().registration(this, array[1], array[2], array[3]);
    }

    private void kickUser(String msg) {
        if (server.getAuthenticatedProvider().isAdmin(this)) {
            String[] array = msg.trim().split("\\s+");
            if (array.length != 2) {
                sendMessage("Некорректный формат ввода команды /kick");
            } else if (server.closeUser(array[1])) {
                sendMessage("Пользователь " + array[1] + " отключен");
            } else {
                sendMessage("Пользователя " + array[1] + " нет в сети.");
            }
        } else {
            sendMessage("Вы не являетесь администратором");
        }
    }
}
