package Controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WaitingRoomController {

    @FXML
    private Label roomNameLabel;

    @FXML
    private Label hostLabel;

    @FXML
    private Label passwordLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Button cancelButton;

    private int roomId;
    private String roomName;
    private String host;
    private String password;
    private ScheduledExecutorService scheduler;

    public void init(int roomId, String roomName, String password, String host) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.password = password;
        this.host = host;

        roomNameLabel.setText(roomName);
        hostLabel.setText(host);
        passwordLabel.setText((password == null || password.isEmpty()) ? "(không mật khẩu)" : "********");
        statusLabel.setText("Tạo phòng thành công. Đang chờ người chơi khác...");

        startPolling();
    }

    private void startPolling() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try (Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/cotuong", "root", "");
                 PreparedStatement ps = conn.prepareStatement("SELECT guest FROM rooms WHERE id=?")) {

                ps.setInt(1, roomId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String guest = rs.getString("guest");
                        if (guest != null && !guest.isEmpty()) {
                            Platform.runLater(() -> {
                                statusLabel.setText("Người chơi " + guest + " đã vào phòng. Đang bắt đầu...");
                                stopPolling();
                                // TODO: chuyển sang màn chơi thực tế
                                closeWindow();
                            });
                        }
                    } else {
                        Platform.runLater(() -> statusLabel.setText("Phòng không tồn tại nữa."));
                        stopPolling();
                    }
                }
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Lỗi kết nối khi chờ người chơi."));
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void stopPolling() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    @FXML
    private void handleCancel() {
        stopPolling();

        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/cotuong", "root", "");
             PreparedStatement ps = conn.prepareStatement("DELETE FROM rooms WHERE id=? AND host=?")) {
            ps.setInt(1, roomId);
            ps.setString(2, host);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Sau khi xoá phòng → quay về Home
        goToHome();
    }

    private void goToHome() {
        try {
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/Home.fxml"));
            Parent root = loader.load();

            HomeController homeController = loader.getController();
            homeController.setUsername(host); // ✅ host chính là currentUser
            homeController.loadStats(host);

            stage.setScene(new Scene(root));
            stage.setTitle("Trang chủ");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
