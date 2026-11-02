package io.angistrem.employeemanagement.repository;

import io.angistrem.employeemanagement.util.AppPaths;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {
    private static final String DB_FILE_NAME = "users.db";
    private static final String DB_DIR = AppPaths.getAppDirectory();
    private static final String DB_URL = "jdbc:sqlite:" + DB_DIR + File.separator + DB_FILE_NAME;

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        ensureAppDirectoryExists();
        return DriverManager.getConnection(DB_URL);
    }

    public static void initDatabase() {
        ensureAppDirectoryExists();

        String sql = "CREATE TABLE IF NOT EXISTS users ("
                + "id TEXT PRIMARY KEY,"
                + "name TEXT NOT NULL,"
                + "login TEXT UNIQUE NOT NULL,"
                + "password TEXT NOT NULL)";

        try (Connection connection = getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось инициализировать БД", e);
        }
    }

    public static String getDatabaseUrl() {
        return DB_URL;
    }

    public static String getDatabaseDirectory() {
        return DB_DIR;
    }

    private static void ensureAppDirectoryExists() {
        File directory = new File(DB_DIR);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new RuntimeException("Не удалось создать директорию приложения: " + DB_DIR);
        }
    }
}
