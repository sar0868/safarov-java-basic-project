package ru.otus.java.safarov;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientDAO implements ClientService {
    private final String DATABASE_URL = "jdbc:postgresql://localhost:5432/chat";
    private final Connection connection;

    public ClientDAO() throws SQLException {
        connection = DriverManager.getConnection(DATABASE_URL, "username", "passwd");

    }

    @Override
    public List<User> getAll() {
        List<User> users = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            String USERS_QUERY = "select login, password, username from Users";
            try (ResultSet resultSet = statement.executeQuery(USERS_QUERY)) {
                while (resultSet.next()) {
                    String login = resultSet.getString("login");
                    String password = resultSet.getString("password");
                    String username = resultSet.getString("username");
                    users.add(new User(login, password, username));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return users;
    }

    @Override
    public String getUsername(String login, String password) {
        String username = null;
        String USER_QUERY = "select username from Users where login = ? and password = ?";
        try (PreparedStatement pst = connection.prepareStatement(USER_QUERY)) {
            pst.setString(1, login);
            pst.setString(2, password);
            try (ResultSet resultSet = pst.executeQuery()) {
                while (resultSet.next()) {
                    username = resultSet.getString("username");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return username;
    }

    @Override
    public int addUser(int id, User user) {
        int result = -1;
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        String INSERT_USER = "INSERT INTO Users (id, login, password, username) values (?,?,?,?)";
        try (PreparedStatement pst = connection.prepareStatement(INSERT_USER)) {
            pst.setInt(1, id);
            pst.setString(2, user.getLogin());
            pst.setString(3, user.getPassword());
            pst.setString(4, user.getUsername());
            result = pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (result != -1 && insertUsersToRoles(id) == -1) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return -1;
        }
        try {
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private int insertUsersToRoles(int id) {
        int result = -1;
        String INSERT_USERS_TO_ROLES = "INSERT INTO Users_to_Roles (userID, roleID) values (?, 2)";
        try (PreparedStatement pst = connection.prepareStatement(INSERT_USERS_TO_ROLES)) {
            pst.setInt(1, id);
            result = pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    @Override
    public boolean isLogin(String login) {
        int id = -1;
        String CHECK_LOGIN = "select id from Users where login = ?";
        try (PreparedStatement pst = connection.prepareStatement(CHECK_LOGIN)) {
            pst.setString(1, login);
            try (ResultSet resultSet = pst.executeQuery()) {
                while (resultSet.next()) {
                    id = resultSet.getInt("id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return id != -1;
    }

    @Override
    public String getRole(String username) {
        String role = null;
        String USER_ROLE = "SELECT role from Roles r " +
                           "inner join Users_to_Roles utr on r.id = utr.roleID " +
                           "INNER JOIN Users u on utr.userID = u.id " +
                           "where u.username = ?";
        try (PreparedStatement pst = connection.prepareStatement(USER_ROLE)) {
            pst.setString(1, username);
            try (ResultSet resultSet = pst.executeQuery()) {
                while (resultSet.next()) {
                    role = resultSet.getString("role");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return role;
    }

    @Override
    public boolean isUserName(String username) {
        String CHECK_USERNAME = "select id from Users where username = ?";
        int id = -1;
        try (PreparedStatement pst = connection.prepareStatement(CHECK_USERNAME)) {
            pst.setString(1, username);
            try (ResultSet resultSet = pst.executeQuery()) {
                while (resultSet.next()) {
                    id = resultSet.getInt("id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return id != -1;
    }

    @Override
    public boolean setUserName(String username, String newUsername) {
        int result = -1;
        String SET_USERNMAME = "update users set username = ? where username = ?";
        try (PreparedStatement pst = connection.prepareStatement(SET_USERNMAME)) {
            pst.setString(1, newUsername);
            pst.setString(2, username);
            result = pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result != -1;
    }

    @Override
    public int getUserID(String username) {
        int id = -1;
        String GET_USERID = "select id from users where username = ?";
        try (PreparedStatement pst = connection.prepareStatement(GET_USERID)) {
            pst.setString(1, username);
            try (ResultSet resultSet = pst.executeQuery()) {
                while (resultSet.next()) {
                    id = resultSet.getInt("id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return id;

    }

//    @Override
//    public int addDepartment(Department department, String username) {
//        int result = -1;
//        String table = "department";
//        int departmentID = getMaxID(table) + 1;
//        int userID = getUserID(username);
//        try {
//            connection.setAutoCommit(false);
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//        String INSERT_DEPARTMENT = "INSERT INTO department (id, title, managerid) values(?, ?, ?)";
//        try (PreparedStatement pst = connection.prepareStatement(INSERT_DEPARTMENT)) {
//            pst.setInt(1, departmentID);
//            pst.setString(2, department.getTitle());
//            pst.setInt(3, userID);
//            result = pst.executeUpdate();
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//        if (result != -1 && insertUsersToDepartment(userID, departmentID) == -1) {
//            try {
//                connection.rollback();
//            } catch (SQLException e) {
//                throw new RuntimeException(e);
//            }
//            return -1;
//        }
//        try {
//            connection.commit();
//            connection.setAutoCommit(true);
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//        return result;
//    }


    @Override
    public int getMaxID(String table) {
        int id = -1;
        String stmt = "select max(id) as id from " + table;
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(stmt)) {
                while (resultSet.next()) {
                    id = resultSet.getInt("id");
                }
            }
            return id;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

//    @Override
//    public int insertUsersToDepartment(int userId, int departmentID) {
//        int result = -1;
//        String INSERT_USERS_TO_DEPARTMENTS = "INSERT INTO users_to_departments (userID, departmentID) values(?, ?)";
//        try (PreparedStatement pst = connection.prepareStatement(INSERT_USERS_TO_DEPARTMENTS)) {
//            pst.setInt(1, userId);
//            pst.setInt(2, departmentID);
//            result = pst.executeUpdate();
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//        return result;
//    }

//    @Override
//    public boolean isDepartment(String title) {
//        int id = -1;
//        String isTitle = "select id from department where title = ?";
//        try (PreparedStatement pst = connection.prepareStatement(isTitle)) {
//            pst.setString(1, title);
//            try (ResultSet resultSet = pst.executeQuery()) {
//                while (resultSet.next()) {
//                    id = resultSet.getInt("id");
//                }
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//        return id != -1;
//    }
//
//    @Override
//    public Set<Department> getDepartments() {
//        Set<Department> departments = new HashSet<>();
//        try (Statement statement = connection.createStatement()) {
//            String GET_DEPARTMENTS = "SELECT title FROM department";
//            try (ResultSet resultSet = statement.executeQuery(GET_DEPARTMENTS)) {
//                while (resultSet.next()) {
//                    String title = resultSet.getString("title");
//                    departments.add(new Department(title));
//                }
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//        return departments;
//    }

    @Override
    public int getGroupID(String title) {
        int id = -1;
        String isTitle = "select id from groups where title = ?";
        try (PreparedStatement pst = connection.prepareStatement(isTitle)) {
            pst.setString(1, title);
            try (ResultSet resultSet = pst.executeQuery()) {
                while (resultSet.next()) {
                    id = resultSet.getInt("id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return id;
    }

    @Override
    public int addGroup(String title, String username) {
        int result = -1;
        int groupID = getMaxID("groups") + 1;
        int userID = getUserID(username);
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        String INSERT_DEPARTMENT = "insert into groups(id, title, adminid) values(?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(INSERT_DEPARTMENT)) {
            pst.setInt(1, groupID);
            pst.setString(2, title);
            pst.setInt(3, userID);
            result = pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (result != -1 && insertUsersToGroups(userID, groupID) == -1) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return -1;
        }
        try {
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;

    }

    @Override
    public int insertUsersToGroups(int userId, int groupID) {
        int result = -1;
        String INSERT_USERS_TO_DEPARTMENTS = "insert into users_to_groups (userID, groupID) values(?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(INSERT_USERS_TO_DEPARTMENTS)) {
            pst.setInt(1, userId);
            pst.setInt(2, groupID);
            result = pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public List<String> getGroupTitle() {
        List<String> titleGroups = new ArrayList<>();
        String GET_GROUP_TITLE = "select title from groups";
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(GET_GROUP_TITLE)) {
                while (resultSet.next()) {
                    String title = resultSet.getString("title");
                    titleGroups.add(title);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return titleGroups;
    }


    @Override
    public boolean isMemberGroup(String username, int groupID) {
        String stmt = "select count(userid) from users_to_groups where userid = ? and groupid = ?";
        int userid = getUserID(username);
        int result = 0;
        try (PreparedStatement pst = connection.prepareStatement(stmt)) {
            pst.setInt(1, userid);
            pst.setInt(2, groupID);
            try (ResultSet resultSet = pst.executeQuery()) {
                while (resultSet.next()) {
                    result = resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result != 0;
    }

    @Override
    public boolean addRequestAddGroup(String username, int groupID) {
        String stmt = "insert into request_add_group (id, userid, groupid) values(?, ?, ?)";
        int requestID = getMaxID("request_add_group") + 1;
        int userid = getUserID(username);
        int result = -1;
        try (PreparedStatement pst = connection.prepareStatement(stmt)) {
            pst.setInt(1, requestID);
            pst.setInt(2, userid);
            pst.setInt(3, groupID);
            result = pst.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result != -1;
    }

    @Override
    public boolean isExistRequestAddGroup(String username, int groupID) {
        String stmt = "select rag.id from request_add_group rag " +
                      "inner join users u on rag.userid = u.id " +
                      "where username = ? and rag.groupid = ?";
        int result = -1;
        try (PreparedStatement pst = connection.prepareStatement(stmt)) {
            pst.setString(1, username);
            pst.setInt(2, groupID);
            try (ResultSet resultSet = pst.executeQuery()) {
                while (resultSet.next()) {
                    result = resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result != -1;
    }

    @Override
    public String getUsernameManagerGroup(String groupTitle) {
        String stmt = "select username from groups g inner join users u on g.adminid = u.id where title = ?";
        String username = "";
        try (PreparedStatement pst = connection.prepareStatement(stmt)) {
            pst.setString(1, groupTitle);
            try (ResultSet resultSet = pst.executeQuery()) {
                while (resultSet.next()) {
                    username = resultSet.getString("username");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return username;
    }

    @Override
    public List<String> getUsernameSentRequest(String groupTitle) {
        String stmt = "select username from request_add_group rag " +
                      "inner join users u on rag.userid = u.id " +
                      "inner join groups g on rag.groupid  = g.id " +
                      " where title = ? ";
        List<String> requestList = new ArrayList<>();
        try (PreparedStatement pst = connection.prepareStatement(stmt)) {
            pst.setString(1, groupTitle);
            try (ResultSet resultSet = pst.executeQuery()) {
                while (resultSet.next()) {
                    String name = resultSet.getString(1);
                    requestList.add(name);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return requestList;
    }

    @Override
    public int deleteRequestAddUserToGroup(String groupTitle) {
        String stmt = "delete from request_add_group where groupid = (select id from groups where title = ?)";
        int result;
        try (PreparedStatement pst = connection.prepareStatement(stmt)) {
            pst.setString(1, groupTitle);
            result = pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public List<String> getUsersToGroup(String groupTitle) {
        String stmt = "select username from users_to_groups utg " +
                      " inner join users u on utg.userid = u.id " +
                      " inner join groups g on utg.groupid = g.id " +
                      " where g.title = ?";
        List<String> users = new ArrayList<>();
        try (PreparedStatement pst = connection.prepareStatement(stmt)) {
            pst.setString(1, groupTitle);
            try (ResultSet resultSet = pst.executeQuery()) {
                while (resultSet.next()) {
                    users.add(resultSet.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return users;
    }

    @Override
    public void addMessageForUserToGroup(String groupTitle, String username, String msg) {
        String stmt = "insert into messages(id, userid, groupid, msg) " +
                      " values( ?, (select id from users where username = ?), " +
                      " (select id from groups where title = ?), ?)";
        int id = getMaxID("messages") + 1;
        try (PreparedStatement pst = connection.prepareStatement(stmt)) {
            pst.setInt(1, id);
            pst.setString(2, username);
            pst.setString(3, groupTitle);
            pst.setString(4, msg);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getListMsgForGroup(String groupTitle, String username) {
        List<String> messages = new ArrayList<>();
        List<Integer> msgID = new ArrayList<>();
        String stmt = "select m.id, msg from messages m " +
                      " inner join users u on m.userid = u.id " +
                      " inner join groups g on m.groupid = g.id " +
                      " where u.username = ? and g.title = ? " +
                      " order by m.id";
        try (PreparedStatement pst = connection.prepareStatement(stmt)) {
            pst.setString(1, username);
            pst.setString(2, groupTitle);
            try (ResultSet resultSet = pst.executeQuery()) {
                while (resultSet.next()) {
                    msgID.add(resultSet.getInt(1));
                    messages.add(resultSet.getString(2));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        for (int i : msgID) {
            deleteMsgForUserToGroup(i);
        }
        return messages;
    }

    private void deleteMsgForUserToGroup(int id) {
        String stmt = "delete from messages where id = ?";
        try (PreparedStatement pst = connection.prepareStatement(stmt)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeUserToGroup(String groupTitle, String username) {
        String stmt = "delete from users_to_groups where userid = " +
                      "(select userid from users_to_groups ug " +
                      "inner join users u on ug.userid = u.id " +
                      "inner join groups g on ug.groupid = g.id " +
                      "where u.username = ? and g.title = ?)";
        try (PreparedStatement pst = connection.prepareStatement(stmt)){
            pst.setString(1, username);
            pst.setString(2, groupTitle);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean updatePassword(String username, String password) {
        int result;
        String stmt = "UPDATE users SET password = ? " +
                      "where username = ?";
        try (PreparedStatement pst = connection.prepareStatement(stmt)){
            pst.setString(1, password);
            pst.setString(2, username);
            result = pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result != 0;
    }

    @Override
    public boolean deleteUser(String username) {
        int result;
        String stmt = "delete from users where username = ?";
        try (PreparedStatement pst = connection.prepareStatement(stmt)){
            pst.setString(1, username);
            result = pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result != 0;
    }

    @Override
    public boolean deleteGroup(String groupTitle) {
        int result;
        String stmt = "delete from groups where title = ?";
        try (PreparedStatement pst = connection.prepareStatement(stmt)){
            pst.setString(1, groupTitle);
            result = pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result != 0;
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
