package Dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/cotuong";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Kết nối thành công!");
            return conn;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("❌ Kết nối thất bại!");
            return null;
        }
    }

    public static void main(String[] args) {
        getConnection();
    }
}
