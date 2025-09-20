package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.regex.Pattern;
import Dao.UserDAO;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginRegister {

    // Đăng nhập
    @FXML private TextField txtLoginUsername;
    @FXML private PasswordField txtLoginPassword;

    // Đăng ký
    @FXML private TextField txtRegUsername;
    @FXML private TextField txtRegEmail;
    @FXML private PasswordField txtRegPassword;
    @FXML private PasswordField txtRegConfirm;

    @FXML private VBox loginForm, registerForm;

    @FXML
    private void showRegister() {
        loginForm.setVisible(false);
        loginForm.setManaged(false);
        registerForm.setVisible(true);
        registerForm.setManaged(true);
    }

    @FXML
    private void showLogin() {
        registerForm.setVisible(false);
        registerForm.setManaged(false);
        loginForm.setVisible(true);
        loginForm.setManaged(true);
    }
    // Đăng nhập
    @FXML
    private void login() {
        String username = txtLoginUsername.getText().trim();
        String password = txtLoginPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("❌ Vui lòng nhập đầy đủ thông tin đăng nhập!");
            return;
        }

        boolean success = UserDAO.checkLogin(username, password);
        if (success) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/Home.fxml"));
                Parent root = loader.load();

                // Lấy HomeController
                HomeController controller = loader.getController();
                controller.setUsername(username);   // truyền username
                controller.loadStats(username);     // load thêm thống kê nếu cần

                // Lấy stage hiện tại và chuyển scene
                Stage stage = (Stage) txtLoginUsername.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("CỜ TƯỚNG ONLINE - Trang Chủ");
                stage.show();

            } catch (IOException e) {
                e.printStackTrace();
                showAlert("❌ Lỗi khi mở trang Home!");
            }
        } else {
            showAlert("❌ Sai tài khoản hoặc mật khẩu!");
        }
    }

    // Đăng ký
    @FXML
    private void register() {
        String username = txtRegUsername.getText().trim();
        String email = txtRegEmail.getText().trim();
        String password = txtRegPassword.getText().trim();
        String confirm = txtRegConfirm.getText().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showAlert("❌ Vui lòng nhập đầy đủ thông tin đăng ký!");
            return;
        }
        if (!isValidEmail(email)) {
            showAlert("❌ Email không hợp lệ!");
            return;
        }
        if (password.length() < 6) {
            showAlert("❌ Mật khẩu phải có ít nhất 6 ký tự!");
            return;
        }
        if (!password.equals(confirm)) {
            showAlert("❌ Mật khẩu xác nhận không khớp!");
            return;
        }

        boolean success = UserDAO.addUser(username, password, email);
        if (success) {
            showAlert("✅ Đăng ký thành công!");
        } else {
            showAlert("❌ Tài khoản hoặc email đã tồn tại!");
        }
    }




    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private boolean isValidEmail(String email) {
        String re = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return Pattern.compile(re).matcher(email).matches();
    }
}
