package com.shuzijun.markdown.util;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author shuzijun
 */
public class FileUtils {
    public static String separator() {
        if (File.separator.equals("\\")) {
            return "/";
        } else {
            return "";
        }
    }

    public static void saveFile(File file, String body) throws IOException {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file, Boolean.FALSE);
        fileOutputStream.write(body.getBytes("UTF-8"));
        fileOutputStream.close();
    }
}
