package ru.sernam.june.chat;


import java.sql.SQLException;

public class ServerApplication {
    public static void main(String[] args) {
        try {
            new Server(8189).start();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
