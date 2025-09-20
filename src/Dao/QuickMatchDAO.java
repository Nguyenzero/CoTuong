package Dao;

import java.sql.*;

public class QuickMatchDAO {
    private Connection conn;

    public QuickMatchDAO(Connection conn) {
        this.conn = conn;
    }

    // Thêm người chơi vào hàng chờ (có created_at)
    public void addWaitingPlayer(String username) throws SQLException {
        String sql = "INSERT INTO quick_match(username, status, created_at) VALUES(?, 'waiting', NOW())";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, username);
        ps.executeUpdate();
    }

    // Tìm người chơi đang chờ (không phải mình)
    public String findWaitingPlayer(String currentUser) throws SQLException {
        String sql = "SELECT username FROM quick_match " +
                "WHERE status='waiting' AND username<>? " +
                "ORDER BY created_at ASC LIMIT 1";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, currentUser);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getString("username");
        }
        return null;
    }

    // Cập nhật đối thủ khi ghép cặp thành công
    public void setOpponent(String username, String opponent) throws SQLException {
        String sql = "UPDATE quick_match SET opponent=?, status='matched' WHERE username=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, opponent);
        ps.setString(2, username);
        ps.executeUpdate();
    }

    // Lấy đối thủ của người chơi (dùng cho người vào trước, đang chờ)
    public String getOpponent(String username) throws SQLException {
        String sql = "SELECT opponent FROM quick_match WHERE username=? AND status='matched'";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, username);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getString("opponent");
        }
        return null;
    }

    // Xóa người chơi khỏi hàng chờ
    public void removeWaitingPlayer(String username) throws SQLException {
        String sql = "DELETE FROM quick_match WHERE username=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, username);
        ps.executeUpdate();
    }

    /**
     * Cố gắng ghép ngay với người chờ lâu nhất. Nếu không có ai, đưa currentUser vào hàng chờ.
     * Trả về đối thủ nếu ghép thành công ngay; nếu không, trả về null (đã vào hàng chờ).
     */
    public String tryMatchOrEnqueue(String currentUser) throws SQLException {
        String opponent = null;
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            // Xóa bản ghi cũ (nếu còn) để tránh trùng lặp
            try (PreparedStatement psDel = conn.prepareStatement(
                    "DELETE FROM quick_match WHERE username=?")) {
                psDel.setString(1, currentUser);
                psDel.executeUpdate();
            }

            // Tìm một người đang chờ (khóa hàng đợi để tránh tranh chấp)
            String selectSql = "SELECT username FROM quick_match " +
                    "WHERE status='waiting' AND username<>? " +
                    "ORDER BY created_at ASC LIMIT 1 FOR UPDATE";
            try (PreparedStatement psSel = conn.prepareStatement(selectSql)) {
                psSel.setString(1, currentUser);
                try (ResultSet rs = psSel.executeQuery()) {
                    if (rs.next()) {
                        opponent = rs.getString("username");

                        // Cập nhật người đối thủ sang 'matched'
                        try (PreparedStatement psUpd = conn.prepareStatement(
                                "UPDATE quick_match SET opponent=?, status='matched' WHERE username=? AND status='waiting'")) {
                            psUpd.setString(1, currentUser);
                            psUpd.setString(2, opponent);
                            psUpd.executeUpdate();
                        }

                        // Thêm bản ghi của currentUser là 'matched'
                        try (PreparedStatement psInsSelf = conn.prepareStatement(
                                "INSERT INTO quick_match(username, status, opponent, created_at) VALUES(?, 'matched', ?, NOW())")) {
                            psInsSelf.setString(1, currentUser);
                            psInsSelf.setString(2, opponent);
                            psInsSelf.executeUpdate();
                        }
                    } else {
                        // Không có ai -> thêm currentUser vào hàng chờ
                        try (PreparedStatement psIns = conn.prepareStatement(
                                "INSERT INTO quick_match(username, status, created_at) VALUES(?, 'waiting', NOW())")) {
                            psIns.setString(1, currentUser);
                            psIns.executeUpdate();
                        }
                    }
                }
            }

            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
        return opponent; // null nếu đang chờ
    }

    /** Hủy chờ nhanh (alias cho removeWaitingPlayer để rõ nghĩa). */
    public void cancelWaiting(String username) throws SQLException {
        removeWaitingPlayer(username);
    }
}