package main;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import javax.swing.Icon;
import javax.swing.JLabel;

/**
 *
 * @author Claudio Santos
 */
public class ScaledIcon implements Icon {

    public static void set(JLabel jLabel, Image image) {
        ScaledIcon scaledIcon = new ScaledIcon(image);
        jLabel.setIcon(scaledIcon);
        jLabel.setPreferredSize(new Dimension(scaledIcon.getIconWidth(), scaledIcon.getIconHeight()));
    }

    private static final double scaleX;
    private static final double scaleY;

    static {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration();
        AffineTransform tx = gc.getDefaultTransform();
        scaleX = tx.getScaleX();
        scaleY = tx.getScaleY();
    }

    private final Image image;
    private final int baseWidth;
    private final int baseHeight;

    public ScaledIcon(Image image) {
        this.image = image;
        this.baseWidth = image.getWidth(null);
        this.baseHeight = image.getHeight(null);
    }

    @Override
    public int getIconWidth() {
        return (int) (baseWidth / scaleX);
    }

    @Override
    public int getIconHeight() {
        return (int) (baseHeight / scaleY);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.scale(1 / scaleX, 1 / scaleY);
        g2d.drawImage(image, x, y, c);
        g2d.dispose();
    }

}
