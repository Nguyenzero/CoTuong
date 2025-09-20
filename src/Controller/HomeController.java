package Controller;

import Dao.QuickMatchDAO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.DriverManager;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.scene.control.DialogEvent;

public class HomeController {

    @FXML private Label lblUsername;
    @FXML private Label lblTotalGames;
    @FXML private Label lblWins;
    @FXML private Label lblDraws;
    @FXML private Label lblPoints;
    @FXML private Label lblRank;

    private String currentUser;

    private Timeline timeline;
    private Alert waitingAlert;
    private boolean isWaiting = false;

    public void setUsername(String username) {
        currentUser = username;
        lblUsername.setText(username);
    }

    public void loadStats(String username) {
        lblTotalGames.setText("Số ván: 10");
        lblWins.setText("Thắng: 5");
        lblDraws.setText("Hòa: 2");
        lblPoints.setText("Điểm: 1200");
        lblRank.setText("Hạng: 15");
    }

    // Xử lý khi nhấn nút "Chơi nhanh"
    @FXML
    private void playQuick() {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/cotuong", "root", ""
        )) {
            QuickMatchDAO dao = new QuickMatchDAO(conn);

            // Ghép ngay nếu có người chờ, nếu không thì vào hàng chờ
            String opponent = dao.tryMatchOrEnqueue(currentUser);

            if (opponent != null) {
                // Có người -> mở bàn cờ ngay
                openBoard(currentUser, opponent);
                return;
            }

            // Chưa có ai -> hiển thị trạng thái đang tìm phòng
            isWaiting = true;
            waitingAlert = new Alert(Alert.AlertType.INFORMATION);
            waitingAlert.setTitle("Ghép phòng");
            waitingAlert.setHeaderText(null);
            waitingAlert.setContentText("Đang tìm phòng, vui lòng chờ...");

            // Đổi nút OK thành "Dừng"
            ButtonType stopType = new ButtonType("Dừng");
            waitingAlert.getButtonTypes().setAll(stopType);
            waitingAlert.show();

            // Khi bấm "Dừng" -> xóa trạng thái chờ và dừng polling
            Node stopBtn = waitingAlert.getDialogPane().lookupButton(stopType);
            stopBtn.addEventFilter(ActionEvent.ACTION, ev -> {
                try (Connection c = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/cotuong", "root", ""
                )) {
                    new QuickMatchDAO(c).cancelWaiting(currentUser);
                } catch (Exception ignored) {}
                isWaiting = false;
                if (timeline != null) timeline.stop();
                waitingAlert.close();
                ev.consume(); // chặn xử lý mặc định
            });

            // Nếu đóng dialog bằng X/ESC thì cũng hủy chờ
            waitingAlert.setOnCloseRequest((DialogEvent ev) -> {
                try (Connection c = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/cotuong", "root", ""
                )) {
                    new QuickMatchDAO(c).cancelWaiting(currentUser);
                } catch (Exception ignored) {}
                isWaiting = false;
                if (timeline != null) timeline.stop();
            });

            // Dọn dẹp nếu đóng cửa sổ chính khi đang chờ
            Stage currentStage = (Stage) lblUsername.getScene().getWindow();
            currentStage.setOnCloseRequest(ev -> {
                try (Connection c = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/cotuong", "root", ""
                )) {
                    new QuickMatchDAO(c).cancelWaiting(currentUser);
                } catch (Exception ignored) {}
                if (timeline != null) timeline.stop();
            });

            // Poll kết quả ghép cặp
            timeline = new Timeline(new KeyFrame(Duration.seconds(2), ev -> {
                try (Connection checkConn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/cotuong", "root", ""
                )) {
                    QuickMatchDAO checkDao = new QuickMatchDAO(checkConn);
                    String foundOpponent = checkDao.getOpponent(currentUser);
                    if (foundOpponent != null) {
                        if (waitingAlert != null) waitingAlert.close();
                        isWaiting = false;
                        timeline.stop();
                        openBoard(currentUser, foundOpponent);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    // Khi nhấn "Tạo phòng"
    @FXML
    private void openCreateRoom() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/CreateRoom.fxml"));
            Parent root = loader.load();

            // Lấy controller của CreateRoom
            CreateRoomController controller = loader.getController();
            controller.setCurrentUser(currentUser); // ✅ Truyền user vào

            Stage stage = new Stage();
            stage.setTitle("Tạo phòng");
            stage.setScene(new Scene(root));
            stage.show();

            // Đóng Home nếu muốn
            Stage currentStage = (Stage) lblUsername.getScene().getWindow();
            currentStage.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    // Khi nhấn "Vào phòng"
    @FXML
    private void openRoomList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/RoomList.fxml"));
            Parent root = loader.load();

            // Lấy controller của RoomList
            RoomListController controller = loader.getController();
            controller.setCurrentUser(currentUser); // ✅ Truyền user vào

            Stage stage = new Stage();
            stage.setTitle("Danh sách phòng");
            stage.setScene(new Scene(root));
            stage.show();

            // Đóng Home nếu muốn
            Stage currentStage = (Stage) lblUsername.getScene().getWindow();
            currentStage.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    // Hàm mở bàn cờ
// Hàm mở bàn cờ
    private void openBoard(String player1, String player2) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/Board.fxml"));
            Parent root = loader.load();

            // ✅ Truyền user sang BoardController
            BoardController boardController = loader.getController();
            boardController.setCurrentUser(currentUser);

            Stage stage = new Stage();
            stage.setTitle("Ván cờ - " + player1 + " vs " + player2);
            stage.setScene(new Scene(root));
            stage.show();

            // Đóng trang Home hiện tại
            Stage currentStage = (Stage) lblUsername.getScene().getWindow();
            currentStage.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Method to set the current user
    public void setCurrentUser(String username) {
        this.currentUser = username;
        lblUsername.setText( username);
        System.out.println("Current user set to: " + username);
    }
}