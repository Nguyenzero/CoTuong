package Client;

import java.io.*;
import java.net.*;

public class ChatClient {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
            System.out.println("✅ Đã kết nối đến server");

            // Luồng nhận tin nhắn từ server
            new Thread(() -> {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        System.out.println("📩 Server: " + msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // Luồng gửi tin nhắn từ bàn phím
            BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String msg;
            while ((msg = keyboard.readLine()) != null) {
                out.println(msg);
                if (msg.equalsIgnoreCase("exit")) break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
