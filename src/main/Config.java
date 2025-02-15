package main;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 *
 * @author Claudio Santos
 */
public class Config {

    private static final String PATH = "config.properties";

    private static Properties load() {
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(PATH)) {
            p.load(fis);
        } catch (Exception e) {
            System.out.println(e);
        }
        return p;
    }

    private static void store(Properties p) {
        try (FileOutputStream fos = new FileOutputStream(PATH)) {
            p.store(fos, null);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static String get(String key) {
        Properties p = load();
        return p.getProperty(key);
    }

    public static void put(String key, String value) {
        Properties p = load();
        p.put(key, value);
        store(p);
    }

}
