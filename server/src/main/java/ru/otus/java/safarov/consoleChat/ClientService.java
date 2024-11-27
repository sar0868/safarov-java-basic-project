package ru.otus.java.safarov.consoleChat;

import java.sql.SQLException;
import java.util.List;

public interface ClientService extends AutoCloseable{
    List<User> getAll();

    String getUsername(String login, String password);

    boolean addUser(int id, User user) throws SQLException;

    boolean isLogin(String login);

    String getRole(String username);

    boolean isUserName(String username);

    boolean setUserName(String username, String newUsername);

    int getUserID(String username);

    int getMaxID(String table);

    int getGroupID(String title);

    boolean addGroup(String title, String username);

    int insertUsersToGroups(int userId, int groupID);

    List<String> getGroupTitle();

    boolean isMemberGroup(String username, int groupID);

    boolean addRequestAddGroup(String username, int groupID);

    boolean isExistRequestAddGroup(String username, int groupID);

    String getUsernameManagerGroup(String groupTitle);

    List<String> getUsernameSentRequest(String groupTitle);

    int deleteRequestAddUserToGroup(String groupTitle);

    List<String> getUsersToGroup(String groupTitle);

    void addMessageForUserToGroup(String groupTitle, String username, String msg);

    List<String> getListMsgForGroup(String groupTitle, String username);

    void removeUserToGroup(String groupTitle, String username);

    boolean updatePassword(String username, String password);

    boolean deleteUser(String username);

    boolean deleteGroup(String groupTitle);
}
