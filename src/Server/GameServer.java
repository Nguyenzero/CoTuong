package Server;

import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int PORT = 12345;
    private static final Set<Socket> clients = Collections.synchronizedSet(new HashSet<>());
    private static final Map<Socket, String> clientColors = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, Socket> userMap = Collections.synchronizedMap(new HashMap<>());
    private static String nextColor = "RED";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("🚀 Game server on port " + PORT);

            while (true) {
                Socket client = serverSocket.accept();
                clients.add(client);

                // gán RED / BLACK
                String assigned = nextColor;
                nextColor = "RED".equals(nextColor) ? "BLACK" : "RED";
                clientColors.put(client, assigned);
                userMap.put(assigned, client); // tạm dùng RED/BLACK cho tới khi nhận HELLO

                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                out.println("ASSIGN;" + assigned);

                System.out.println("✅ Client connected: " + assigned);
                new Thread(new ClientHandler(client, assigned)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private final String assignedColor; // RED/BLACK
        private String currentName;         // tên hiển thị, mặc định = RED/BLACK, sẽ cập nhật khi nhận HELLO
        private BufferedReader in;

        public ClientHandler(Socket socket, String username) {
            this.socket = socket;
            this.assignedColor = username;
            this.currentName = username;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) { e.printStackTrace(); }
        }

        @Override
        public void run() {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println("📩 Received: " + msg);

                    // 👇 client báo tên hiển thị: HELLO;username
                    if (msg.startsWith("HELLO;")) {
                        String newName = msg.substring(6).trim();
                        if (!newName.isEmpty()) {
                            synchronized (userMap) {
                                userMap.remove(currentName);
                                userMap.put(newName, socket);
                            }
                            System.out.println("ℹ️ " + assignedColor + " is now known as: " + newName);
                            currentName = newName;
                        }
                        continue;
                    }

                    if (msg.startsWith("CHAT;")) {
                        String content = msg.substring(5).trim();

                        if (content.startsWith("/")) {
                            int spaceIdx = content.indexOf(" ");
                            if (spaceIdx > 1) {
                                String target = content.substring(1, spaceIdx);
                                String text = content.substring(spaceIdx + 1);
                                sendPrivate(target, "Tin nhắn riêng từ " + currentName + ": " + text);
                            } else {
                                sendPrivate(currentName, "Sai cú pháp! Hãy nhập: /TênNgườiNhận NộiDung");
                            }
                        } else {
                            broadcast("CHAT;" + currentName + ": " + content);
                        }
                    } else {
                        // MOVE, RESIGN ... → broadcast
                        broadcast(msg);
                    }
                }
            } catch (IOException e) {
                System.out.println("❌ Client error: " + currentName);
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
                clients.remove(socket);
                clientColors.remove(socket);
                userMap.remove(currentName);
                System.out.println("❌ Client disconnected: " + currentName);
            }
        }

        private void broadcast(String msg) {
            synchronized (clients) {
                for (Socket s : clients) {
                    if (!s.isClosed()) {
                        try {
                            PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                            out.println(msg);
                        } catch (IOException e) { e.printStackTrace(); }
                    }
                }
            }
        }

        private void sendPrivate(String target, String msg) {
            Socket s = userMap.get(target);
            if (s != null && !s.isClosed()) {
                try {
                    PrintWriter toTarget = new PrintWriter(s.getOutputStream(), true);
                    toTarget.println("Tin nhắn riêng ;" + msg);
                } catch (IOException e) { e.printStackTrace(); }
            } else {
                try {
                    PrintWriter back = new PrintWriter(socket.getOutputStream(), true);
                    back.println("PM;Người dùng '" + target + "' không tồn tại!");
                } catch (IOException e) { e.printStackTrace(); }
            }
        }
    }
}
