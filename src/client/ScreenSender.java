package client;

import common.DataPacket;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

public class ScreenSender implements Runnable {

    private final String target;

    // === PARAMÈTRES SIMPLES ===
    private static final double SCALE = 0.35; // plus petit = plus rapide
    private static final int FPS = 12;         // fluide pour écran distant

    public ScreenSender(String target) {
        this.target = target;
    }

    @Override
    public void run() {
        try {
            ImageIO.setUseCache(false);
            Robot robot = new Robot();

            Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle screenRect = new Rectangle(size);

            int w = (int) (size.width * SCALE);
            int h = (int) (size.height * SCALE);

            BufferedImage smallImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = smallImg.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            ByteArrayOutputStream baos = new ByteArrayOutputStream(150_000);
            long frameTime = 1000 / FPS;

            while (true) {
                long start = System.currentTimeMillis();

                try {
                    // 1. Capture écran
                    BufferedImage fullImg = robot.createScreenCapture(screenRect);

                    // 2. Redimensionnement
                    g.drawImage(fullImg, 0, 0, w, h, null);

                    // 3. JPEG simple
                    baos.reset();
                    ImageIO.write(smallImg, "jpg", baos);

                    // 4. Envoi
                    if (MainClient.out != null) {
                        DataPacket p = new DataPacket(DataPacket.Type.SCREEN_DATA, MainClient.myName);
                        p.imageBytes = baos.toByteArray();
                        MainClient.send(p, target);
                    }

                } catch (Exception e) {
                    System.err.println("Erreur screen: " + e.getMessage());
                }

                // 5. FPS stable
                long sleep = frameTime - (System.currentTimeMillis() - start);
                if (sleep > 0) Thread.sleep(sleep);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
