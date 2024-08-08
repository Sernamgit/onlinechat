package ru.sernam.june.chat;

import java.sql.*;

public class JDBCAuthenticationProvider implements AuthenticationProvider {
    private Server server;
    private Connection connection;

    public JDBCAuthenticationProvider(Server server, String dbUrl, String dbUsername, String dbPassword) throws SQLException {
        this.server = server;
        this.connection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
    }

    @Override
    public void initialize() {
        System.out.println("Сервис аутентификации запущен: JDBC режим (PostgreSQL)");
    }

    private String getUsernameByLoginAndPassword(String login, String password) {
        String query = "SELECT username FROM Users WHERE login = ? AND passsword = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("username");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isLoginAlreadyExist(String login) {
        String query = "SELECT COUNT(*) FROM Users WHERE login = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, login);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isUsernameAlreadyExist(String username) {
        String query = "SELECT COUNT(*) FROM Users WHERE username = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public synchronized boolean authenticate(ClientHandler clientHandler, String login, String password) {
        String authUsername = getUsernameByLoginAndPassword(login, password);
        if (authUsername == null) {
            clientHandler.sendMessage("Некорректный логин/пароль");
            return false;
        }
        if (server.isUsernameBusy(authUsername)) {
            clientHandler.sendMessage("Указанная учетная запись уже занята");
            return false;
        }
        clientHandler.setUsername(authUsername);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/authok " + authUsername);
        return true;
    }

    @Override
    public boolean registration(ClientHandler clientHandler, String login, String password, String username) {
        if (login.trim().length() < 3 || password.trim().length() < 6 || username.trim().length() < 1) {
            clientHandler.sendMessage("Логин 3+ символа, Пароль 6+ символов, Имя пользователя 1+ символ");
            return false;
        }
        if (isLoginAlreadyExist(login)) {
            clientHandler.sendMessage("Указанный логин уже занят");
            return false;
        }
        if (isUsernameAlreadyExist(username)) {
            clientHandler.sendMessage("Указанное имя пользователя уже занято");
            return false;
        }

        try {
            // добавляем пользователя
            String insertUserQuery = "INSERT INTO Users (login, passsword, username) VALUES (?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertUserQuery, Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setString(1, login);
                preparedStatement.setString(2, password);
                preparedStatement.setString(3, username);
                preparedStatement.executeUpdate();

                // получаем id
                ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int userId = generatedKeys.getInt(1);

                    // назначаем роль
                    String insertUserRoleQuery = "INSERT INTO user_to_role (user_id, role_id) VALUES (?, (SELECT id FROM Role WHERE role = 'user'))";
                    try (PreparedStatement preparedStatementRole = connection.prepareStatement(insertUserRoleQuery)) {
                        preparedStatementRole.setInt(1, userId);
                        preparedStatementRole.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            clientHandler.sendMessage("Ошибка при регистрации пользователя");
            return false;
        }

        clientHandler.setUsername(username);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/regok " + username);
        return true;
    }
}
