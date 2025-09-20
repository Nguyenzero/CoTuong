package Dao;

import java.sql.*;

public class RoomDAO {
    private Connection conn;

    public RoomDAO(Connection conn) {
        this.conn = conn;
    }

    // Tạo phòng mới
    public void createRoom(String roomName, String host, String password) throws SQLException {
        String sql = "INSERT INTO rooms(room_name, host, password, status) VALUES(?, ?, ?, 'waiting')";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, roomName);
        ps.setString(2, host);
        ps.setString(3, password != null ? password : "");
        ps.executeUpdate();
    }

    // Lấy danh sách các phòng đang chờ
    public ResultSet getWaitingRooms() throws SQLException {
        String sql = "SELECT id, room_name, host, password FROM rooms WHERE status='waiting'";
        Statement st = conn.createStatement();
        return st.executeQuery(sql);
    }

    // Người chơi join phòng
    public boolean joinRoom(int roomId, String guest, String password) throws SQLException {
        String sql = "SELECT password FROM rooms WHERE id=? AND status='waiting'";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, roomId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            String realPassword = rs.getString("password");
            if (realPassword.equals(password == null ? "" : password)) {
                // Update guest + matched
                String update = "UPDATE rooms SET guest=?, status='matched' WHERE id=?";
                PreparedStatement ps2 = conn.prepareStatement(update);
                ps2.setString(1, guest);
                ps2.setInt(2, roomId);
                ps2.executeUpdate();
                return true;
            }
        }
        return false;
    }

    // Lấy thông tin 2 người chơi trong phòng
    public String[] getPlayers(int roomId) throws SQLException {
        String sql = "SELECT host, guest FROM rooms WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, roomId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new String[]{rs.getString("host"), rs.getString("guest")};
        }
        return null;
    }
}
