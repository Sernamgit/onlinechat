package ru.sernam.june.chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String username;

    private static int userCount = 0;

    public ClientHandler(Server server,Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        userCount++;
        this.username = "user" + userCount;
        new Thread(() -> {
            try{

                System.out.println("Подключился новый клиент");
                while (true){
                    String message = in.readUTF();
                    if (message.startsWith("/")){
                        if (message.equals("/exit")){
                            sendMessage("/exitok");
                            break;
                        }
                        if (message.startsWith("/w")) {
                            sendPrivateMessage(message);
                        }
                        continue;
                    }
                    server.broadcastMessage(username + ": " + message);

                }
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                disconnect();
            }

        }).start();
    }

    public void sendMessage(String message){
        try{
            out.writeUTF(message);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void sendPrivateMessage(String message){
        String withoutCommand = message.substring(3);
        int spaceIndex = withoutCommand.indexOf(" ");
        if (spaceIndex != -1){
            server.privateMessage(withoutCommand.substring(spaceIndex + 1), withoutCommand.substring(0, spaceIndex), this);
        } else {
            this.sendMessage("Не корректная команда");
        }

    }

    public void disconnect(){
        server.unsubscribe(this);
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }
}
