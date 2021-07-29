package com.shuzijun.markdown.controller;

import com.intellij.openapi.vfs.VirtualFile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author shuzijun
 */
public class FileApplicationService {

    private final Map<String, VirtualFile> myVirtualFile = new ConcurrentHashMap<>();

    public void putVirtualFile(String fileName, String projectName, VirtualFile virtualFile) {
        myVirtualFile.put(fileName, virtualFile);
    }

    public VirtualFile getVirtualFile(String fileName, String projectName) {
        return myVirtualFile.get(fileName);
    }

    public void removeVirtualFile(String fileName, String projectName) {
        myVirtualFile.remove(fileName);
    }

}
