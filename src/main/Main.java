package main;

import javax.swing.UIManager;
import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author Claudio Santos
 */
public class Main {

    public static String title = "ImageTools";
    public static String version = "1.2";
    public static String titleVersion = title + " " + version;

    public static Image icon = new ImageIcon(Main.class.getResource("/icons/icon.png")).getImage();

    public static void main(String[] args) {
        FlatDarkLaf.setup();

        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException e) {
            System.out.println(e);
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Gui().setVisible(true);
            }
        });
    }

}
