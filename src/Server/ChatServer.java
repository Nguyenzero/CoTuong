package Server;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static Set<Socket> clientSockets = new HashSet<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("🚀 Server đang chạy trên cổng " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientSockets.add(clientSocket);
                System.out.println("✅ Client kết nối: " + clientSocket);

                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Lớp xử lý client
    static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println("💬 Client gửi: " + msg);
                    broadcast(msg, socket);
                    if (msg.equalsIgnoreCase("exit")) break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                    clientSockets.remove(socket);
                    System.out.println("❌ Client thoát: " + socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Gửi tin nhắn cho tất cả client khác
        private void broadcast(String msg, Socket sender) {
            for (Socket s : clientSockets) {
                if (s != sender) {
                    try {
                        PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                        out.println(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
