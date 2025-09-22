package Client;

import Controller.BoardController;
import java.io.*;
import java.net.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GameClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // 👇 Interface cho callback chat
    public interface ChatListener {
        void onChat(String message);
    }

    private ChatListener chatListener;
    // 👇 hàng đợi tin nhắn đến trước khi UI gán listener
    private final Queue<String> pendingChat = new ConcurrentLinkedQueue<>();

    public void setChatListener(ChatListener listener) {
        this.chatListener = listener;
        // flush các tin đã đệm nếu có
        if (listener != null) {
            String msg;
            while ((msg = pendingChat.poll()) != null) {
                final String m = msg;
                javafx.application.Platform.runLater(() -> listener.onChat(m));
            }
        }
    }

    public GameClient(String serverIp, int serverPort) throws IOException {
        socket = new Socket(serverIp, serverPort);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        // Lưu ý: KHÔNG startListening() ở đây để không mất tin nhắn trước khi UI set listener
    }

    // 👇 gửi tên hiển thị lên server để hỗ trợ chat riêng: /TênNơiNhận NộiDung
    public void identify(String username) {
        if (username != null && !username.isEmpty()) {
            out.println("HELLO;" + username);
        }
    }

    public void sendMove(String pieceId, int fromCol, int fromRow, int toCol, int toRow) {
        out.println("MOVE;" + pieceId + ";" + fromCol + ";" + fromRow + ";" + toCol + ";" + toRow);
    }

    public void sendResign(String side) {
        out.println("RESIGN;" + side);
    }

    // ✅ gửi chat
    public void sendChat(String message) {
        out.println("CHAT;" + message);
    }

    // 👇 giữ tham chiếu BoardController (có thể null khi ở Home)
    private volatile BoardController boardController;
    private volatile boolean listeningStarted = false;

    public void setBoardController(BoardController controller) {
        this.boardController = controller;
    }

    // 👉 cho controller biết trạng thái nghe để quyết định echo cục bộ tin đầu tiên
    public boolean isListening() {
        return listeningStarted;
    }

    // 👇 khởi động luồng đọc 1 lần, hoạt động cho cả Home và Board
    public synchronized void startListening() {
        if (listeningStarted) return;
        listeningStarted = true;

        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println("📩 Từ server: " + msg);

                    if (msg.startsWith("MOVE;")) {
                        BoardController bc = this.boardController;
                        if (bc != null) {
                            String[] p = msg.split(";");
                            javafx.application.Platform.runLater(() ->
                                    bc.movePiece(p[1],
                                            Integer.parseInt(p[2]),
                                            Integer.parseInt(p[3]),
                                            Integer.parseInt(p[4]),
                                            Integer.parseInt(p[5])));
                        }
                    } else if (msg.startsWith("ASSIGN;")) {
                        BoardController bc = this.boardController;
                        if (bc != null) {
                            String side = msg.substring(7);
                            javafx.application.Platform.runLater(() ->
                                    bc.setPlayerSide(side));
                        }
                    } else if (msg.startsWith("RESIGN;")) {
                        BoardController bc = this.boardController;
                        if (bc != null) {
                            String side = msg.substring(7).trim();
                            javafx.application.Platform.runLater(() ->
                                    bc.onResign(side));
                        }
                    } else if (msg.startsWith("CHAT;")) {
                        String chatMsg = msg.substring(5);
                        if (chatListener != null) {
                            javafx.application.Platform.runLater(() -> chatListener.onChat(chatMsg));
                        } else {
                            pendingChat.add(chatMsg);
                        }
                    } else if (msg.startsWith("PM;")) {
                        String pmMsg = "🔒 " + msg.substring(3);
                        if (chatListener != null) {
                            javafx.application.Platform.runLater(() -> chatListener.onChat(pmMsg));
                        } else {
                            pendingChat.add(pmMsg);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                listeningStarted = false;
            }
        }, "GameClient-Reader").start();
    }

    // vẫn giữ API cũ: gán BoardController rồi đảm bảo lắng nghe
    public void listen(BoardController boardController) {
        this.boardController = boardController;
        startListening();
    }
}