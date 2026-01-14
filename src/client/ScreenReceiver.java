package client;

import common.DataPacket;
import javax.swing.*;
import java.awt.event.*;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class ScreenReceiver extends JFrame {
    private JLabel labelImage;
    private String targetName;

    public ScreenReceiver(String targetName) {
        this.targetName = targetName;
        this.setTitle("Contrôle en cours sur : " + targetName);
        this.setSize(1000, 700);
        
        labelImage = new JLabel("En attente de vidéo...");
        this.add(new JScrollPane(labelImage));
        
        // --- GESTION DE LA SOURIS ---
        
        // 1. Mouvement
        labelImage.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                // Ne rien envoyer si non connecté
                if (MainClient.out == null) return;
                DataPacket p = new DataPacket(DataPacket.Type.MOUSE_MOVE, MainClient.myName);
                p.x = e.getX();
                p.y = e.getY();
                MainClient.send(p, targetName);
            }
        });

        // 2. Clics
        labelImage.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (MainClient.out == null) return;
                DataPacket p = new DataPacket(DataPacket.Type.MOUSE_CLICK, MainClient.myName);
                p.mouseButton = e.getButton();
                MainClient.send(p, targetName);
            }
        });

        this.setVisible(true);
    }

    // Appelé quand une nouvelle image arrive
    public void updateImage(byte[] data) {
        try {
            if (data == null) return;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
                BufferedImage img = ImageIO.read(bais);
                if (img != null) {
                    // enlever le texte initial si présent
                    labelImage.setText(null);
                    labelImage.setIcon(new ImageIcon(img));
                    labelImage.revalidate();
                    labelImage.repaint();
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}