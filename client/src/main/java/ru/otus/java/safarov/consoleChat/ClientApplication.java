package ru.otus.java.safarov.consoleChat;

import java.io.IOException;

public class ClientApplication {
    private final static String IP = "localhost";
    private final static int PORT = 8189;
    public static void main(String[] args) throws IOException {
        new Client(IP, PORT).start();
    }
}