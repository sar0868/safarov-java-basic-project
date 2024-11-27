package ru.otus.java.safarov.consoleChat;

public class User {
        private final String login;
        private final String password;
        private final String username;
//        private Role role;

        public User(String login, String password, String username) {
            this.login = login;
            this.password = password;
            this.username = username;
//            this.role = Role.USER;
        }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

}
