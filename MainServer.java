package server;

import common.DataPacket;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MainServer {
    private static final int PORT = 9999;
    // Map pour stocker les clients connectés : Nom -> Thread de gestion
    private static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Démarrage du serveur sur le port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serveur prêt ! En attente de connexions...");

            while (true) {
                // On accepte une nouvelle connexion
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nouvelle connexion : " + clientSocket.getInetAddress());
                
                // On lance un thread dédié pour ce client
                ClientHandler handler = new ClientHandler(clientSocket);
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Gestionnaire pour un client unique ---
    private static class ClientHandler extends Thread {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // Création des flux (Out avant In est important en Java Sockets)
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                // 1. Attente du premier message : LOGIN
                DataPacket firstMsg = (DataPacket) in.readObject();
                if (firstMsg.type == DataPacket.Type.LOGIN) {
                    this.clientName = firstMsg.sender;
                    
                    // Si le nom existe déjà, on pourrait rejeter, mais ici on écrase
                    clients.put(clientName, this);
                    System.out.println("Client identifié : " + clientName);
                    
                    // Dire à tout le monde que la liste a changé
                    broadcastUserList();
                }

                // 2. Boucle principale : recevoir et relayer les messages
                while (true) {
                    DataPacket msg = (DataPacket) in.readObject();
                    
                    // RELAIS : Si le message a une cible, on lui transmet
                    if (msg.target != null && clients.containsKey(msg.target)) {
                        ClientHandler targetHandler = clients.get(msg.target);
                        targetHandler.send(msg);
                    } 
                }
            } catch (Exception e) {
                System.out.println("Déconnexion de " + clientName);
            } finally {
                // Nettoyage quand le client part
                if (clientName != null) {
                    clients.remove(clientName);
                    broadcastUserList();
                }
                try { socket.close(); } catch (IOException e) {}
            }
        }

        // Envoyer un paquet à ce client
        public synchronized void send(DataPacket p) {
            try {
                out.writeObject(p);
                out.flush();
                out.reset(); // Important pour vider le cache d'objets
            } catch (IOException e) {
                // Client probablement parti
            }
        }
    }

    // Diffuser la liste des pseudos à tout le monde (pour mettre à jour les listes déroulantes)
    private static void broadcastUserList() {
        DataPacket p = new DataPacket();
        p.type = DataPacket.Type.UPDATE_LIST;
        p.connectedUsers = new ArrayList<>(clients.keySet());

        for (ClientHandler handler : clients.values()) {
            handler.send(p);
        }
    }
}