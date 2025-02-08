package main;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 *
 * @author Claudio Santos
 */
public class Config {

    public static final String PATH = "config.properties";

    public static String get(String key) {
        String res = "";
        try {
            Properties p = new Properties();
            p.load(new FileInputStream(PATH));
            res = p.getProperty(key);
        } catch (Exception e) {
            System.out.println(e);
        }
        return res;
    }

    public static void put(String key, String value) {
        try {
            Properties p = new Properties();
            p.put(key, value);
            p.store(new FileOutputStream(PATH), null);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

}
