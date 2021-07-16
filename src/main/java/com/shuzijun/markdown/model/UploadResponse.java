package com.shuzijun.markdown.model;


import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author shuzijun
 */
public class UploadResponse {

    public static UploadResponse error(String msg) {
        UploadResponse uploadResponse = new UploadResponse();
        uploadResponse.setCode(1);
        uploadResponse.setMsg(msg);
        return uploadResponse;
    }

    public static UploadResponse success(Data data) {
        UploadResponse uploadResponse = new UploadResponse();
        uploadResponse.setCode(0);
        uploadResponse.setMsg("");
        uploadResponse.setData(data);
        return uploadResponse;
    }

    private String msg;

    private int code = 0;

    private Data data;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {

        List<String> errFiles = new ArrayList<>();

        Map<String, String> succMap = new HashMap<>();

        public List<String> getErrFiles() {
            return errFiles;
        }

        public void setErrFiles(List<String> errFiles) {
            this.errFiles = errFiles;
        }

        public void addErrFiles(String errFile) {
            this.errFiles.add(errFile);
        }

        public Map<String, String> getSuccMap() {
            return succMap;
        }

        public void setSuccMap(Map<String, String> succMap) {
            this.succMap = succMap;
        }

        public void addSuccMap(String fileName, String filePath) {
            this.succMap.put(fileName, filePath);
        }
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

}
