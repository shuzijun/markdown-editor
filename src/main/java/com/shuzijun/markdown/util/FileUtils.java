package com.shuzijun.markdown.util;


import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ModalityUiUtil;

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

    public static void refreshProjectDirectory(Project project, String refreshPath) {
        ModalityUiUtil.invokeLaterIfNeeded(ModalityState.defaultModalityState(), new Runnable() {
            @Override
            public void run() {
                VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(refreshPath));
                if (virtualFile != null) {
                    virtualFile.refresh(true, true);
                }
            }
        });
    }
}
