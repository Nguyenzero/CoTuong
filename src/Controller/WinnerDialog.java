package Controller;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public final class WinnerDialog {
    private WinnerDialog() {}

    // winnerLabel: "ĐỎ" or "ĐEN"; youWin: true if local player is the winner
    public static void showWinner(String winnerLabel, boolean youWin) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Kết thúc ván");
            alert.setHeaderText("Bên " + winnerLabel + " thắng");
            alert.setContentText(youWin ? "Bạn thắng!" : "Bạn thua!");
            alert.getButtonTypes().setAll(ButtonType.OK);
            alert.show();
        });
    }
}
