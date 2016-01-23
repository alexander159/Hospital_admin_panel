package util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class FileManager {
    public static boolean createDirectory(String path) {
        File dir = new File(path);
        return dir.mkdirs();
    }

    public static boolean removeDirectory(String path) {
        File file = new File(path);
        try {
            FileUtils.deleteDirectory(file);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean isDirectoryExist(String path) {
        File dir = new File(path);
        return dir.exists();
    }
}
