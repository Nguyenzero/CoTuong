    package Controller;

    import Dao.DatabaseConnection;
    import Dao.RoomDAO;
    import Model.Room;
    import javafx.collections.FXCollections;
    import javafx.collections.ObservableList;
    import javafx.fxml.FXML;
    import javafx.scene.control.*;

    import java.sql.Connection;
    import java.sql.ResultSet;

    public class RoomListController {

        @FXML
        private TableView<Room> roomTable;
        @FXML
        private TableColumn<Room, String> idColumn;
        @FXML
        private TableColumn<Room, String> nameColumn;
        @FXML
        private TableColumn<Room, String> hostColumn;
        @FXML
        private TableColumn<Room, String> statusColumn;

        @FXML
        private TextField searchField;
        @FXML
        private Button searchButton;
        @FXML
        private Button refreshButton;

        @FXML
        private Button backButton;

        private ObservableList<Room> roomList = FXCollections.observableArrayList();
        private ObservableList<Room> filteredList = FXCollections.observableArrayList();

        private RoomDAO roomDAO;

        private String currentUser;

        public RoomListController() {
            // Initialize RoomDAO with a database connection
            Connection conn = DatabaseConnection.getConnection();
            roomDAO = new RoomDAO(conn);
        }

        @FXML
        public void initialize() {
            // Bind table columns to Room properties

            nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
            hostColumn.setCellValueFactory(cellData -> cellData.getValue().hostProperty());
            statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

            // Load room list from the database
            loadRoomList();

            // Set up search functionality
            searchButton.setOnAction(e -> searchRoom());

            // Refresh room list
            refreshButton.setOnAction(e -> loadRoomList());

            // Join room


            // Back action
            backButton.setOnAction(e -> backAction());
        }

        private void loadRoomList() {
            try {
                roomList.clear();
                ResultSet rs = roomDAO.getWaitingRooms();
                while (rs.next()) {
                    String name = rs.getString("room_name");
                    String host = rs.getString("host");
                    String password = rs.getString("password");

                    // Nếu có mật khẩu thì hiện khóa 🔒, ngược lại hiện 🔓
                    String status = (password != null && !password.isEmpty()) ? "🔒" : "🔓";

                    roomList.add(new Room(name, host, status));
                }
                filteredList.setAll(roomList);
                roomTable.setItems(filteredList);
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Không thể tải danh sách phòng!");
                alert.showAndWait();
            }
        }


        private void searchRoom() {
            String keyword = searchField.getText().toLowerCase().trim();
            if (keyword.isEmpty()) {
                filteredList.setAll(roomList);
            } else {
                filteredList.setAll(roomList.filtered(
                        room -> room.getName().toLowerCase().contains(keyword)
                ));
            }
        }

        private void joinRoom() {
            Room selectedRoom = roomTable.getSelectionModel().getSelectedItem();
            if (selectedRoom != null) {
                System.out.println("Đang vào phòng: " + selectedRoom.getName());
                // TODO: xử lý logic vào phòng
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Vui lòng chọn một phòng trước!");
                alert.showAndWait();
            }
        }

        private void backAction() {
            try {
                // Lấy stage hiện tại từ backButton
                javafx.stage.Stage stage = (javafx.stage.Stage) backButton.getScene().getWindow();
                stage.close(); // Đóng cửa sổ hiện tại

                // Load lại màn hình Home
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/View/Home.fxml"));
                javafx.scene.Parent root = loader.load();

                // Lấy HomeController để truyền user
                Controller.HomeController homeController = loader.getController();
                homeController.setCurrentUser(currentUser); // Truyền user đã đăng nhập

                // Mở cửa sổ Home
                javafx.stage.Stage homeStage = new javafx.stage.Stage();
                homeStage.setTitle("Trang chủ");
                homeStage.setScene(new javafx.scene.Scene(root));
                homeStage.show();
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Không thể quay lại trang trước!");
                alert.showAndWait();
            }
        }


        // Method to set the current user
        public void setCurrentUser(String username) {
            this.currentUser = username;
            System.out.println("Current user set to: " + username);
            // You can use this variable to customize the room list or perform user-specific actions
        }
    }