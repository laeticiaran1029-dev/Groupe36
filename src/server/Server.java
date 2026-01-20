package server;

import common.DataPacket;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    // On garde en mémoire chaque client : Nom -> ObjectOutputStream
    private static Map<String, ObjectOutputStream> clients = new HashMap<>();

    public static void main(String[] args) {
        try {
            // C'est LUI qui ouvre le port 9999
            ServerSocket serverSocket = new ServerSocket(9999);
            System.out.println("Serveur démarré sur le port 9999...");
            System.out.println("En attente de clients...");

            while (true) {
                // On accepte une nouvelle connexion
                Socket clientSocket = serverSocket.accept();
                // On lance un thread pour gérer ce client sans bloquer les autres
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            String clientName = null;

            while (true) {
                DataPacket msg = (DataPacket) in.readObject();

                // 1. Un client se connecte (LOGIN)
                if (msg.type == DataPacket.Type.LOGIN) {
                    clientName = msg.sender;
                    synchronized (clients) {
                        clients.put(clientName, out);
                    }
                    System.out.println(clientName + " est connecté.");
                    broadcastUserList();
                }
                // 2. Relayer les messages (SCREEN_DATA, MOUSE, etc.)
                else if (msg.target != null) {
                    forwardMessage(msg, msg.target);
                }
            }
        } catch (Exception e) {
            System.out.println("Client déconnecté.");
        }
    }

    // Transférer un message à un client précis
    private static void forwardMessage(DataPacket msg, String targetName) {
        synchronized (clients) {
            ObjectOutputStream targetOut = clients.get(targetName);
            if (targetOut != null) {
                try {
                    targetOut.writeObject(msg);
                    targetOut.flush();
                    targetOut.reset();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Envoyer la liste de tout le monde à tout le monde
    private static void broadcastUserList() {
        DataPacket p = new DataPacket();
        p.type = DataPacket.Type.UPDATE_LIST;
        synchronized (clients) {
            p.connectedUsers = new java.util.ArrayList<>(clients.keySet());
            for (ObjectOutputStream out : clients.values()) {
                try {
                    out.writeObject(p);
                    out.flush();
                    out.reset();
                } catch (IOException e) {}
            }
        }
    }
}