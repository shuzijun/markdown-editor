package com.shuzijun.markdown.util;


import java.io.File;

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
}
