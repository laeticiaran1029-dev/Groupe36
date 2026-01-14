package client;

import common.DataPacket;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class ScreenSender implements Runnable {
    private String target;

    public ScreenSender(String target) {
        this.target = target;
    }

    @Override
    public void run() {
        try {
            Robot robot = new Robot();
            // On prend la taille de l'écran
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

            while (true) {
                // 1. Capture
                BufferedImage img = robot.createScreenCapture(screenRect);

                // 2. Compression en JPG (dans un tableau de bytes)
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "jpg", baos);

                // 3. Création du paquet
                DataPacket p = new DataPacket(DataPacket.Type.SCREEN_DATA, MainClient.myName);
                p.imageBytes = baos.toByteArray();
                
                // 4. Envoi via le MainClient
                MainClient.send(p, target);

                // 5. Pause pour ne pas surcharger (environ 15 FPS)
                Thread.sleep(60); 
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}