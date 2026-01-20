package client;

import common.DataPacket;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import javax.swing.*;

public class ScreenReceiver extends JFrame {
    private ImagePanel panelEcran;
    private String targetName;

    public ScreenReceiver(String targetName) {
        this.targetName = targetName;
        this.setTitle("Contrôle : " + targetName);
        this.setSize(1000, 700);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH); 

        panelEcran = new ImagePanel();
        this.add(panelEcran);

        // --- GESTION DE LA SOURIS ---
        
        panelEcran.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                envoyerCoordonnees(e, DataPacket.Type.MOUSE_MOVE);
            }
            public void mouseDragged(MouseEvent e) {
                envoyerCoordonnees(e, DataPacket.Type.MOUSE_MOVE); 
            }
        });

        panelEcran.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                envoyerCoordonnees(e, DataPacket.Type.MOUSE_CLICK);
            }
        });

        this.setVisible(true);
    }

    // --- C'EST ICI QU'ON CORRIGE LA PRÉCISION ---
    private void envoyerCoordonnees(MouseEvent e, DataPacket.Type type) {
        if (MainClient.out == null || panelEcran.imageCourante == null) return;

        // 1. Récupérer la taille de l'image reçue (qui est réduite)
        double largeurImageRecue = panelEcran.imageCourante.getWidth();
        double hauteurImageRecue = panelEcran.imageCourante.getHeight();

        // 2. Facteur de zoom utilisé dans ScreenSender (0.6)
        // IMPORTANT : Si vous changez 0.6 dans ScreenSender, changez-le ici aussi !
        double scale = 0.5; 

        // 3. On retrouve la taille "Réelle" de l'écran distant
        double largeurReelle = largeurImageRecue / scale;
        double hauteurReelle = hauteurImageRecue / scale;

        // 4. Taille de votre fenêtre
        double largeurFenetre = panelEcran.getWidth();
        double hauteurFenetre = panelEcran.getHeight();

        // 5. Calcul du Ratio correct
        double ratioX = largeurReelle / largeurFenetre;
        double ratioY = hauteurReelle / hauteurFenetre;

        // 6. Coordonnées finales
        int xFinal = (int) (e.getX() * ratioX);
        int yFinal = (int) (e.getY() * ratioY);

        DataPacket p = new DataPacket(type, MainClient.myName);
        p.x = xFinal;
        p.y = yFinal;
        if (type == DataPacket.Type.MOUSE_CLICK) {
            p.mouseButton = e.getButton();
        }
        MainClient.send(p, targetName);
    }

    public void updateImage(byte[] data) {
        try {
            if (data == null) return;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
                BufferedImage img = ImageIO.read(bais);
                if (img != null) {
                    panelEcran.setImage(img);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    class ImagePanel extends JPanel {
        public BufferedImage imageCourante;

        public void setImage(BufferedImage img) {
            this.imageCourante = img;
            this.repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (imageCourante != null) {
                // Étirement de l'image pour remplir la fenêtre (Plein écran)
                g.drawImage(imageCourante, 0, 0, this.getWidth(), this.getHeight(), null);
            } else {
                g.drawString("En attente de vidéo...", 10, 20);
            }
        }
    }
}