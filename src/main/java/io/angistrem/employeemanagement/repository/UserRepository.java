package io.angistrem.employeemanagement.repository;

import io.angistrem.employeemanagement.model.User;
import io.angistrem.employeemanagement.util.PasswordUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserRepository {
    public static final String QUICK_ADMIN_LOGIN = "1";
    public static final String QUICK_ADMIN_PASSWORD = "1";
    private static final String QUICK_ADMIN_NAME = "Administrator";

    public void ensureQuickAccessAdmin() {
        String sql = "INSERT OR IGNORE INTO users(id, name, login, password) VALUES(?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, QUICK_ADMIN_NAME);
            stmt.setString(3, QUICK_ADMIN_LOGIN);
            stmt.setString(4, PasswordUtils.hash(QUICK_ADMIN_PASSWORD));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure quick-access admin user", e);
        }
    }

    public boolean save(User user) {
        String sql = "INSERT INTO users(id, name, login, password) VALUES(?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getId().toString());
            stmt.setString(2, user.getName());
            stmt.setString(3, user.getLogin());
            stmt.setString(4, user.getPassword());
            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.contains("UNIQUE constraint failed")) {
                return false;
            }
            throw new RuntimeException("Ошибка сохранения пользователя", e);
        }
    }

    public User findByLogin(String login) {
        String sql = "SELECT * FROM users WHERE login = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, login);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска пользователя по логину", e);
        }

        return null;
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection connection = DatabaseManager.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения списка пользователей", e);
        }

        return users;
    }

    public void update(User user) {
        String sql = "UPDATE users SET name = ?, login = ?, password = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, user.getName());
            stmt.setString(2, user.getLogin());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления пользователя", e);
        }
    }

    public void updatePassword(UUID userId, String passwordHash) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, passwordHash);
            stmt.setString(2, userId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user password", e);
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(UUID.fromString(rs.getString("id")));
        user.setName(rs.getString("name"));
        user.setLogin(rs.getString("login"));
        user.setPassword(rs.getString("password"));
        return user;
    }
}
