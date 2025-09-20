    import javafx.application.Application;
    import javafx.fxml.FXMLLoader;
    import javafx.scene.Scene;
    import javafx.stage.Stage;

    public class Main extends Application {

        @Override
        public void start(Stage stage) throws Exception {
            // Mở trang chủ thay vì board trực tiếp
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/LoginRegister.fxml"));
            Scene scene = new Scene(loader.load(), 700, 600); // Kích thước trang chủ
            stage.setTitle("Cờ Tướng Online - Trang chủ");
            stage.setScene(scene);
            stage.show();
        }

        public static void main(String[] args) {
            launch(args);
        }
    }
