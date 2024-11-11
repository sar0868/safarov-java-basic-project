package ru.otus.java.safarov;

import java.util.List;

public interface AuthenticatedProvider {
    void initialize();

    boolean authenticate(ClientHandler clientHandler, String login, String password);

    boolean registration(ClientHandler clientHandler, String login, String password, String username);

    boolean isAdmin(ClientHandler clientHandler);

    boolean changeUsername(ClientHandler clientHandler, String username);

    boolean addGroup(ClientHandler clientHandler, String title);

    List<String> getGroupTitle(ClientHandler clientHandler);

    boolean enterGroup(ClientHandler clientHandler, String groupTitle);

    boolean addRequestAddGroup(ClientHandler clientHandler, String groupTitle);

    boolean isManagerGroup(ClientHandler clientHandler, String groupTitle);

    List<String> getListRequest(ClientHandler clientHandler, String groupTitle);

    void addUsersToGroup(ClientHandler clientHandler, List<String> addUsers);

    void removeRequestAddUserToGroup(ClientHandler clientHandler);

    List<String> getUsersToGroup(String groupTitle);

    void addMsgToGroup(String groupTitle, List<String> users, String msg);

    List<String> getListMsgForGroup(ClientHandler clientHandler);

    void kickUserFromGroup(ClientHandler clientHandler, String username);

    boolean changePassword(ClientHandler clientHandler, String password);

    boolean deleteUser(String username);

    boolean delGroup(String groupTitle);
}
