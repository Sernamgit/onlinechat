package ru.sernam.june.chat;

import java.sql.SQLException;

public interface AuthenticationProvider {
    void initialize() throws ClassNotFoundException, SQLException;

    boolean authenticate(ClientHandler clientHandler, String login, String password);

    boolean registration(ClientHandler clientHandler, String login, String password, String username);
}
