package client;

import common.DataPacket;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import javax.swing.*;

public class MainClient extends JFrame {
    public static String myName;
    public static ObjectOutputStream out;
    public static ObjectInputStream in;
    public static ScreenReceiver currentViewer;

    // Éléments graphiques
    private JComboBox<String> cmbClients; // Liste déroulante des PC
    private JButton btnControl;

    public MainClient() {
        myName = JOptionPane.showInputDialog("Entrez le nom de ce PC :");
        if (myName == null || myName.trim().isEmpty()) System.exit(0);

        this.setTitle("Groupe36 - Client : " + myName);
        this.setSize(450, 200);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout());

        // Panel Haut : Connexion
        JPanel panelTop = new JPanel();
        JButton btnConnect = new JButton("Connexion Serveur");
        panelTop.add(btnConnect);
        this.add(panelTop, BorderLayout.NORTH);

        // Panel Centre : Sélection Cible
        JPanel panelCenter = new JPanel(new GridLayout(3, 1));
        panelCenter.add(new JLabel("Liste des PC connectés :", SwingConstants.CENTER));
        
        cmbClients = new JComboBox<>(); // La liste vide au début
        panelCenter.add(cmbClients);
        
        btnControl = new JButton("DEMANDER LE CONTRÔLE");
        btnControl.setEnabled(false); // Désactivé tant qu'on n'est pas connecté
        panelCenter.add(btnControl);
        
        this.add(panelCenter, BorderLayout.CENTER);

        // --- ACTIONS ---

        btnConnect.addActionListener(e -> connectToServer());

        btnControl.addActionListener(e -> {
            String target = (String) cmbClients.getSelectedItem();
            if (target != null && !target.equals(myName)) {
                send(new DataPacket(DataPacket.Type.REQUEST_CONTROL, myName), target);
                JOptionPane.showMessageDialog(this, "Demande envoyée à " + target + "...\nEn attente de sa permission.");
            } else {
                JOptionPane.showMessageDialog(this, "Veuillez choisir un autre PC que vous-même.");
            }
        });

        this.setVisible(true);
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket("172.20.10.4", 9999);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            send(new DataPacket(DataPacket.Type.LOGIN, myName), null);
            
            new Thread(this::listen).start();
            JOptionPane.showMessageDialog(this, "Connecté !");
            btnControl.setEnabled(true);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Serveur introuvable : " + e.getMessage());
        }
    }

    private void listen() {
        try {
            while (true) {
                DataPacket msg = (DataPacket) in.readObject();

                // 1. Mise à jour de la liste des clients
                if (msg.type == DataPacket.Type.UPDATE_LIST) {
                    SwingUtilities.invokeLater(() -> {
                        cmbClients.removeAllItems();
                        for (String user : msg.connectedUsers) {
                            // On ajoute une petite étoile si c'est nous
                            if (user.equals(myName)) cmbClients.addItem(user + " (Moi)");
                            else cmbClients.addItem(user);
                        }
                    });
                }
                
                // 2. Demande de contrôle (LEGAL : Permission requise)
                else if (msg.type == DataPacket.Type.REQUEST_CONTROL) {
                    int rep = JOptionPane.showConfirmDialog(this, 
                        "ATTENTION : " + msg.sender + " veut contrôler votre PC.\nAcceptez-vous ?", 
                        "Demande d'accès", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        
                    if (rep == JOptionPane.YES_OPTION) {
                        DataPacket resp = new DataPacket(DataPacket.Type.ACCEPT_CONTROL, myName);
                        send(resp, msg.sender);
                        new Thread(new ScreenSender(msg.sender)).start();
                    }
                } 
                
                // 3. Réponse positive
                else if (msg.type == DataPacket.Type.ACCEPT_CONTROL) {
                    currentViewer = new ScreenReceiver(msg.sender);
                }
                
                // 4. Flux Vidéo / Souris
                else if (msg.type == DataPacket.Type.SCREEN_DATA && currentViewer != null) {
                    currentViewer.updateImage(msg.imageBytes);
                }
                else if (msg.type == DataPacket.Type.MOUSE_MOVE) {
                    new Robot().mouseMove(msg.x, msg.y);
                }
                else if (msg.type == DataPacket.Type.MOUSE_CLICK) {
                    Robot robot = new Robot();
                    int mask = java.awt.event.InputEvent.getMaskForButton(msg.mouseButton);
                    robot.mousePress(mask);
                    robot.mouseRelease(mask);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void send(DataPacket p, String target) {
        try {
            if(target != null) p.target = target;
            out.writeObject(p);
            out.flush();
            out.reset();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static void main(String[] args) {
        new MainClient();
    }
}