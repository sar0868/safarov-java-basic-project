package ru.otus.java.safarov.consoleChat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final Scanner scanner;

    public Client(String ip, int port) throws IOException {
        this.scanner = new Scanner(System.in);
        this.socket = new Socket(ip, port);
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    public void start(){
        new Thread(() -> {
            try {
                while (true) {
                    String msg = in.readUTF();
                    if (msg.startsWith("/")) {
                        if (msg.startsWith("/exitok")) {
                            break;
                        }
                        if (msg.startsWith("/authok")) {
                            System.out.println("Вы прошли аутентификацию. Имя пользователя "
                                    + msg.split(" ")[1]);
                        }
                        if (msg.startsWith("/regok")) {
                            System.out.println("Вы зарегистрировались. Имя пользователя "
                                    + msg.split(" ")[1]);
                        }
                    } else {
                        System.out.println(msg);
                    }
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } finally {
                disconnect();
            }
        }).start();
        while (true) {
            String msg = scanner.nextLine();
            try {
                out.writeUTF(msg);
            } catch (IOException ex) {
                break;
            }
            if (msg.startsWith("/exit")) {
                break;
            }
        }
    }

    private void disconnect() {
        try {
            in.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
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
        System.exit(1);
    }
}
