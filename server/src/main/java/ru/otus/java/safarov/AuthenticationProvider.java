package ru.otus.java.safarov;

import java.sql.SQLException;
import java.util.List;

import static ru.otus.java.safarov.ServerApplication.logger;

public class AuthenticationProvider implements AuthenticatedProvider {

    private final Server server;
    private ClientDAO clientDAO;

    public AuthenticationProvider(Server server) {
        this.server = server;
    }

    @Override
    public void initialize() {
        try {
            clientDAO = new ClientDAO();
            logger.info("Сервис аутентификации запущен. DB режим");
        } catch (SQLException e) {
            logger.info("Нет соединения с базой данных");
        }
    }

    private synchronized String getUserNameByLoginAndPassword(String login, String password) {
        return clientDAO.getUsername(login, password);
    }

    @Override
    public synchronized boolean authenticate(ClientHandler clientHandler, String login, String password) {
        String authName = getUserNameByLoginAndPassword(login, password);
        if (authName == null) {
            clientHandler.sendMessage("Некорректный логин/пароль");
            return false;
        }
        if (server.isName(authName)) {
            clientHandler.sendMessage("Имя пользователя занято.");
            return false;
        }
        clientHandler.setName(authName);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/authok " + authName);
        return true;
    }


    @Override
    public synchronized boolean registration(ClientHandler clientHandler, String login, String password, String username) {
        if (login.trim().length() < 3 || password.trim().length() < 6
            || username.trim().length() < 2) {
            clientHandler.sendMessage("""
                    Логин должен быть 3 более символов,
                    длина пароля 6 и более символов,
                    имя пользователя длиной 2 и более символов.""");
            return false;
        }
        if (isLoginAlreadyExist(login)) {
            clientHandler.sendMessage("Указанный логин уже занят.");
            return false;
        }
        if (isUserNameAlreadyExist(username)) {
            clientHandler.sendMessage("Указанное имя пользователя уже занято.");
            return false;
        }
        if (clientDAO.addUser(clientDAO.getAll().size() + 1, new User(login, password, username)) == -1) {
            String msgError = username + " не зарегистрирован.";
            logger.info(msgError);
            clientHandler.sendMessage(msgError);
            return false;
        }
        clientHandler.setName(username);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/regok " + username);
        return true;
    }

    private synchronized boolean isUserNameAlreadyExist(String username) {
        return clientDAO.isUserName(username);
    }


    private synchronized boolean isLoginAlreadyExist(String login) {
        return clientDAO.isLogin(login);
    }

    private synchronized Role getRole(String username) {
        if (clientDAO.getRole(username).equals("ADMIN")) {
            return Role.ADMIN;
        }
        return Role.USER;
    }

    @Override
    public synchronized boolean isAdmin(ClientHandler clientHandler) {
        return getRole(clientHandler.getName()) == Role.ADMIN;
    }

    @Override
    public synchronized boolean changeUsername(ClientHandler clientHandler, String username) {
        return clientDAO.setUserName(clientHandler.getName(), username);
    }

    @Override
    public synchronized boolean addGroup(ClientHandler clientHandler, String title) {
        if (title.trim().length() < 2) {
            clientHandler.sendMessage(" Название группы должно быть более 2 символов");
            return false;
        }
        if (isGroupAlreadyExist(title) != -1) {
            clientHandler.sendMessage("Указанная группа уже существует.");
            return false;
        }
        if (clientDAO.addGroup(title, clientHandler.getName()) == -1) {
            String msgError = title + " не создан.";
            logger.info(msgError);
            clientHandler.sendMessage(msgError);
            return false;
        }
        clientHandler.sendMessage("/groupok " + title);
        return true;
    }

    private synchronized int isGroupAlreadyExist(String title) {
        return clientDAO.getGroupID(title);
    }

    @Override
    public synchronized List<String> getGroupTitle(ClientHandler clientHandler) {
        return clientDAO.getGroupTitle();
    }

    @Override
    public synchronized boolean enterGroup(ClientHandler clientHandler, String groupTitle) {
        int groupID = isGroupAlreadyExist(groupTitle);
        if (groupID == -1) {
            clientHandler.sendMessage("Группы " + groupTitle + " не существует");
            return false;
        }
        if (!isMemberGroup(clientHandler.getName(), groupID)) {
            clientHandler.sendMessage("Вы не являетесь членом группы, можете отправить запрос на " +
                                      "добавление в группу /requestaddgroup <имя группы>");
            return false;
        }
        clientHandler.setGroupTitle(groupTitle);
        server.subscribeGroup(clientHandler);
        clientHandler.sendMessage("Вы вошли в группу " + groupTitle);
        return true;
    }

    @Override
    public synchronized List<String> getUsersToGroup(String groupTitle) {
        return clientDAO.getUsersToGroup(groupTitle);
    }

    @Override
    public synchronized boolean isManagerGroup(ClientHandler clientHandler, String groupTitle) {
        return clientHandler.getName().equals(clientDAO.getUsernameManagerGroup(groupTitle));
    }

    @Override
    public synchronized boolean addRequestAddGroup(ClientHandler clientHandler, String groupTitle) {
        int groupID = isGroupAlreadyExist(groupTitle);
        if (groupID == -1) {
            clientHandler.sendMessage("Группы " + groupTitle + " не существует");
            return false;
        }
        if (isMemberGroup(clientHandler.getName(), groupID)) {
            clientHandler.sendMessage("Вы уже являетесь членом группы " + groupTitle +
                                      ".\nДля входа в группу введите /enter " + groupTitle);
            return false;
        }
        if (isExistRequest(clientHandler.getName(), groupID)) {
            clientHandler.sendMessage("Вы уже направляли запрос на добавление в группу " + groupTitle +
                                      ". Ваш запрос еще не рассмотрен.");
            return false;
        }
        return clientDAO.addRequestAddGroup(clientHandler.getName(), groupID);
    }

    @Override
    public synchronized List<String> getListRequest(ClientHandler clientHandler, String groupTitle) {
        return clientDAO.getUsernameSentRequest(groupTitle);
    }

    private synchronized boolean isExistRequest(String username, int groupID) {
        return clientDAO.isExistRequestAddGroup(username, groupID);
    }


    private synchronized boolean isMemberGroup(String username, int groupID) {
        return clientDAO.isMemberGroup(username, groupID);
    }

    @Override
    public synchronized void addUsersToGroup(ClientHandler clientHandler, List<String> addUsers) {
        int groupID = clientDAO.getGroupID(clientHandler.getGroupTitle());
        for (String username : addUsers) {
            int userID = clientDAO.getUserID(username);
            clientDAO.insertUsersToGroups(userID, groupID);
        }
    }

    @Override
    public synchronized void removeRequestAddUserToGroup(ClientHandler clientHandler) {
        if (clientDAO.deleteRequestAddUserToGroup(clientHandler.getGroupTitle()) == 0) {
            logger.info("Failed clear request add users to group");
        }
    }

    @Override
    public synchronized void addMsgToGroup(String groupTitle, List<String> users, String msg) {
        for (String user : users) {
            clientDAO.addMessageForUserToGroup(groupTitle, user, msg);
        }
    }

    @Override
    public synchronized List<String> getListMsgForGroup(ClientHandler clientHandler) {
        return clientDAO.getListMsgForGroup(clientHandler.getGroupTitle(), clientHandler.getName());
    }

    @Override
    public synchronized void kickUserFromGroup(ClientHandler clientHandler, String username) {
        if(!isManagerGroup(clientHandler, clientHandler.getGroupTitle())){
            clientHandler.sendMessage("Вы не являетесь владельцем группы.");
        } else if (!getUsersToGroup(clientHandler.getGroupTitle()).contains(username)){
            clientHandler.sendMessage(username + " не являетесь членом группы.");
        } else {
            clientDAO.removeUserToGroup(clientHandler.getGroupTitle(), username);
            server.unsubscribeUserFromGroup(clientHandler.getGroupTitle(), username);
        }
    }

    @Override
    public synchronized boolean changePassword(ClientHandler clientHandler, String password) {
        if (password.trim().length() < 6) {
            clientHandler.sendMessage("Длина пароля должна быть 6 и более символов");
            return false;
        }
        return clientDAO.updatePassword(clientHandler.getName(), password);
    }

    @Override
    public synchronized boolean deleteUser(String username) {
        return clientDAO.deleteUser(username);
    }

    @Override
    public synchronized boolean delGroup(String groupTitle) {
        if(clientDAO.deleteGroup(groupTitle)){
            server.delGroup(groupTitle);
            return true;
        }
        return false;
    }
}
