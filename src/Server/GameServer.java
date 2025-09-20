package Server;

import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int PORT = 12345;
    private static final Set<Socket> clients = Collections.synchronizedSet(new HashSet<>());
    private static final Map<Socket, String> clientColors = Collections.synchronizedMap(new HashMap<>());
    private static String nextColor = "RED";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("🚀 Game server on port " + PORT);

            while (true) {
                Socket client = serverSocket.accept();
                clients.add(client);

                // Assign side to client
                String assigned = nextColor;
                nextColor = "RED".equals(nextColor) ? "BLACK" : "RED";
                clientColors.put(client, assigned);
                try {
                    PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                    out.println("ASSIGN;" + assigned);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("✅ Client connected: " + client + " as " + assigned);
                new Thread(new ClientHandler(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println("📩 Received: " + msg);
                    broadcast(msg);
                }
            } catch (IOException e) {
                System.out.println("❌ Client error: " + socket);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clients.remove(socket);
                clientColors.remove(socket);
                System.out.println("❌ Client disconnected: " + socket);
            }
        }

        private void broadcast(String msg) {
            synchronized (clients) {
                for (Socket s : clients) {
                    if (!s.isClosed()) {
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
}
