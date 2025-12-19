package com.daifeng.plugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.time.LocalDate;

public class DatabaseManager {

    // 填你的数据库信息
    private static final String URL = "jdbc:mysql://localhost:3306/daifeng_community?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    private static final String USER = "root";
    private static final String PASSWORD = "123456"; // 别忘了改！

    // 获取连接
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // 给玩家加钱 (UPSERT操作: 有则更新，无则插入)
    public double addMoney(String playerName, double amount) {
        String sql = "INSERT INTO mc_economy (username, money) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE money = money + ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, playerName);
            ps.setDouble(2, amount);
            ps.setDouble(3, amount);
            ps.executeUpdate();

            return getBalance(playerName); // 返回最新余额
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    // 查询余额
    public double getBalance(String playerName) {
        String sql = "SELECT money FROM mc_economy WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("money");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }
    // 检查今天是否已签到
    public boolean hasSignedInToday(String playerName) {
        String sql = "SELECT last_sign_date FROM mc_economy WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Date lastDate = rs.getDate("last_sign_date");
                    if (lastDate != null) {
                        // 比较日期是否是今天
                        return lastDate.toLocalDate().equals(LocalDate.now());
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 执行签到 (更新日期 + 加钱)
    public void doSign(String playerName) {
        // 这里用事务更好，为了简单演示先分开写
        String sql = "INSERT INTO mc_economy (username, money, last_sign_date) VALUES (?, 100, ?) " +
                "ON DUPLICATE KEY UPDATE money = money + 100, last_sign_date = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName);
            Date today = Date.valueOf(LocalDate.now());
            ps.setDate(2, today);
            ps.setDate(3, today);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}