package client;

import common.DataPacket;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;

public class MainClient extends JFrame {
    public static String myName;
    public static ObjectOutputStream out;
    public static ObjectInputStream in;
    
    // Référence vers la fenêtre de visionnage (si on est le contrôleur)
    public static ScreenReceiver currentViewer; 

    private Socket socket;

    public MainClient() {
        myName = JOptionPane.showInputDialog("Entrez le nom de ce PC :");
        this.setTitle("Groupe36 - Client : " + myName);
        this.setSize(400, 150);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new FlowLayout());

        JTextField txtTarget = new JTextField("NomCible", 10);
        JButton btnConnect = new JButton("1. Connexion Serveur");
        JButton btnControl = new JButton("2. Demander Contrôle");

        this.add(btnConnect);
        this.add(new JLabel("Cible :"));
        this.add(txtTarget);
        this.add(btnControl);

        // Action 1 : Se connecter au serveur
        btnConnect.addActionListener(e -> connectToServer());

        // Action 2 : Demander à contrôler l'autre PC
        btnControl.addActionListener(e -> {
            send(new DataPacket(DataPacket.Type.REQUEST_CONTROL, myName), txtTarget.getText());
        });

        this.setVisible(true);

        // Fermer proprement les streams/sockets si la fenêtre se ferme
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (out != null) out.close();
                } catch (IOException ex) { /* ignore */ }
                try {
                    if (in != null) in.close();
                } catch (IOException ex) { /* ignore */ }
                try {
                    if (socket != null && !socket.isClosed()) socket.close();
                } catch (IOException ex) { /* ignore */ }
            }
        });
    }

    private void connectToServer() {
        try {
            // METS L'IP DU SERVEUR ICI (127.0.0.1 pour local, ou l'IP réelle)
            socket = new Socket("127.0.0.1", 9999); 
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            // On s'identifie
            send(new DataPacket(DataPacket.Type.LOGIN, myName), null);
            
            // On lance l'écoute des messages entrants
            new Thread(this::listen).start();
            JOptionPane.showMessageDialog(this, "Connecté au serveur !");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erreur connexion : " + e.getMessage());
        }
    }

    // Boucle d'écoute (Thread séparé)
    private void listen() {
        try {
            Robot robot = new Robot(); // réutiliser le même Robot
            while (true) {
                DataPacket msg = (DataPacket) in.readObject();
                
                // Cas 1 : Quelqu'un veut me contrôler
                if (msg.type == DataPacket.Type.REQUEST_CONTROL) {
                    int rep = JOptionPane.showConfirmDialog(this, 
                        msg.sender + " veut prendre le contrôle. Accepter ?", 
                        "Demande de contrôle", JOptionPane.YES_NO_OPTION);
                        
                    if (rep == JOptionPane.YES_OPTION) {
                        // Je dis OUI
                        DataPacket resp = new DataPacket(DataPacket.Type.ACCEPT_CONTROL, myName);
                        send(resp, msg.sender);
                        
                        // Je lance le partage de MON écran vers LUI
                        new Thread(new ScreenSender(msg.sender)).start();
                    }
                } 
                // Cas 2 : La cible a accepté ma demande
                else if (msg.type == DataPacket.Type.ACCEPT_CONTROL) {
                    // J'ouvre la fenêtre pour voir son écran
                    currentViewer = new ScreenReceiver(msg.sender);
                }
                // Cas 3 : Je reçois une image de l'écran
                else if (msg.type == DataPacket.Type.SCREEN_DATA) {
                    if (currentViewer != null) {
                        currentViewer.updateImage(msg.imageBytes);
                    }
                }
                // Cas 4 : On me demande de bouger la souris
                else if (msg.type == DataPacket.Type.MOUSE_MOVE) {
                    robot.mouseMove(msg.x, msg.y);
                }
                // Cas 5 : On me demande de cliquer
                else if (msg.type == DataPacket.Type.MOUSE_CLICK) {
                    int mask = java.awt.event.InputEvent.getMaskForButton(msg.mouseButton);
                    robot.mousePress(mask);
                    robot.mouseRelease(mask);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Fonction utilitaire pour envoyer un message
    public static void send(DataPacket p, String target) {
        try {
            if (out == null) {
                System.err.println("Non connecté au serveur : impossible d'envoyer le paquet");
                return;
            }
            if (target != null) p.target = target;
            out.writeObject(p);
            out.flush();
            out.reset(); // Important !
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static void main(String[] args) {
        new MainClient();
    }
}