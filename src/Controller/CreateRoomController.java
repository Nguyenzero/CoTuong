package Controller;

            import javafx.fxml.FXML;
            import javafx.scene.control.PasswordField;
            import javafx.scene.control.TextField;
            import javafx.scene.control.Label;
            import javafx.stage.Stage;
            import javafx.fxml.FXMLLoader;
            import javafx.scene.Parent;
            import javafx.scene.Scene;

            import java.sql.Connection;
            import java.sql.DriverManager;
            import java.sql.PreparedStatement;
            import java.sql.Statement;
            import java.sql.ResultSet;

            public class CreateRoomController {

                @FXML
                private TextField roomNameField;

                @FXML
                private PasswordField passwordField;

                @FXML
                private Label statusLabel;

                private String currentUser; // host

                public void setCurrentUser(String username) {
                    this.currentUser = username;
                }

                @FXML
                private void handleCreateRoom() {
                    String roomName = roomNameField.getText().trim();
                    String password = passwordField.getText().trim();

                    if (roomName.isEmpty()) {
                        statusLabel.setText("Tên phòng không được để trống!");
                        return;
                    }

                    if (currentUser == null || currentUser.isEmpty()) {
                        statusLabel.setText("Lỗi: chưa xác định người tạo phòng!");
                        return;
                    }

                    try (Connection conn = DriverManager.getConnection(
                            "jdbc:mysql://localhost:3306/cotuong", "root", "")) {

                        String sql = "INSERT INTO rooms (room_name, password, host) VALUES (?, ?, ?)";
                        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                        ps.setString(1, roomName);
                        ps.setString(2, password.isEmpty() ? null : password);
                        ps.setString(3, currentUser);
                        ps.executeUpdate();

                        int roomId = -1;
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) roomId = rs.getInt(1);
                        }

                        // Fallback if driver does not return generated keys
                        if (roomId == -1) {
                            try (PreparedStatement ps2 = conn.prepareStatement(
                                    "SELECT id FROM rooms WHERE room_name=? AND host=? ORDER BY id DESC LIMIT 1")) {
                                ps2.setString(1, roomName);
                                ps2.setString(2, currentUser);
                                try (ResultSet rs2 = ps2.executeQuery()) {
                                    if (rs2.next()) roomId = rs2.getInt("id");
                                }
                            }
                        }

                        if (roomId <= 0) {
                            statusLabel.setText("Không lấy được mã phòng!");
                            return;
                        }

                        statusLabel.setText("Tạo phòng thành công!");
                        openWaitingRoom(roomId, roomName, password);

                    } catch (Exception e) {
                        e.printStackTrace();
                        statusLabel.setText("Lỗi khi tạo phòng!");
                    }
                }

                @FXML
                private void handleCancel() {
                    backToHome();
                }

                private void openWaitingRoom(int roomId, String roomName, String password) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/WaitingRoom.fxml"));
                        Parent root = loader.load();

                        WaitingRoomController waitingCtrl = loader.getController();
                        waitingCtrl.init(roomId, roomName, password, currentUser);

                        Stage stage = new Stage();
                        stage.setTitle("Phòng: " + roomName);
                        stage.setScene(new Scene(root));
                        stage.show();

                        // Close current window
                        Stage currentStage = (Stage) roomNameField.getScene().getWindow();
                        currentStage.close();

                    } catch (Exception e) {
                        e.printStackTrace();
                        statusLabel.setText("Không mở được màn hình chờ!");
                    }
                }

                private void backToHome() {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/Home.fxml"));
                        Parent root = loader.load();

                        HomeController homeController = loader.getController();
                        homeController.setUsername(currentUser);
                        homeController.loadStats(currentUser);

                        Stage stage = new Stage();
                        stage.setTitle("Trang chủ");
                        stage.setScene(new Scene(root));
                        stage.show();

                        Stage currentStage = (Stage) roomNameField.getScene().getWindow();
                        currentStage.close();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }