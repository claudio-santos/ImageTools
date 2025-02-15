package main;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 *
 * @author Claudio Santos
 */
public class MyImage {

    private BufferedImage image;
    private final String path;
    private final File file;

    public MyImage(BufferedImage image, String path, File file) {
        this.image = image;
        this.path = path;
        this.file = file;
    }

    public MyImage(BufferedImage image, MyImage myImage) {
        this(image, myImage.getPath(), myImage.getFile());
    }

    public MyImage(MyImage myImage) {
        this(myImage.getImage(), myImage.getPath(), myImage.getFile());
    }

    public MyImage() {
        this(null, "", null);
    }

    public boolean isPathBlank() {
        return path == null || path.isBlank();
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public String getPath() {
        return path;
    }

    public File getFile() {
        return file;
    }

}
