package com.shuzijun.markdown.model;


import com.alibaba.fastjson.JSON;

/**
 * @author shuzijun
 */
public class MarkdownResponse {

    private boolean success = false;

    private String message;

    private String data;

    public static MarkdownResponse error(String message) {
        return new MarkdownResponse(false, message, null);
    }

    public static MarkdownResponse success(String data) {
        return new MarkdownResponse(true, null, data);
    }

    public static MarkdownResponse success(String data, String message) {
        return new MarkdownResponse(true, message, data);
    }

    public MarkdownResponse(boolean success, String message, String data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);

    }
}
