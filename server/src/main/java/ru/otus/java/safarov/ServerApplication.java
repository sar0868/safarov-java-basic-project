package ru.otus.java.safarov;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerApplication {
    public static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) {
        int PORT = 8189;
        new Server(PORT).start();
    }
}