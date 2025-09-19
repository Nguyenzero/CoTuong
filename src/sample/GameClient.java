package sample;

import java.io.*;
import java.net.*;

public class GameClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public GameClient(String serverIp, int serverPort) throws IOException {
        socket = new Socket(serverIp, serverPort);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    // Gửi nước đi
    public void sendMove(String pieceId, int fromCol, int fromRow, int toCol, int toRow) {
        String msg = "MOVE;" + pieceId + ";" + fromCol + ";" + fromRow + ";" + toCol + ";" + toRow;
        out.println(msg);
    }

    // Lắng nghe server (chạy trong thread riêng)
    public void listen(BoardController boardController) {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println("📩 Từ server: " + msg);
                    if (msg.startsWith("MOVE;")) {
                        String[] parts = msg.split(";");
                        String piece = parts[1];
                        int fromCol = Integer.parseInt(parts[2]);
                        int fromRow = Integer.parseInt(parts[3]);
                        int toCol = Integer.parseInt(parts[4]);
                        int toRow = Integer.parseInt(parts[5]);

                        javafx.application.Platform.runLater(() -> {
                            boardController.movePiece(piece, fromCol, fromRow, toCol, toRow);
                        });
                    } else if (msg.startsWith("ASSIGN;")) {
                        String side = msg.substring("ASSIGN;".length());
                        javafx.application.Platform.runLater(() -> {
                            boardController.setPlayerSide(side);
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}