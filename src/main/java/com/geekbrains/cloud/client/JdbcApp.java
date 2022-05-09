package com.geekbrains.cloud.client;

import java.sql.*;

public class JdbcApp {
    private Connection connection;
    private Statement stmt;

    public void connect() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:users.db");
        stmt = connection.createStatement();
    }

    public void disconnect() {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ResultSet getUser(String login, String pass) {
        ResultSet rs = null;
        try {
            rs = stmt.executeQuery("SELECT * FROM users WHERE login = '" + login + "' AND password = '" + pass + "';");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return rs;
    }

}
