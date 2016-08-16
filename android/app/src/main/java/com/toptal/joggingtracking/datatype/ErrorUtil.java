package com.toptal.joggingtracking.datatype;

import com.google.gson.Gson;

/**
 * Created by sofiane on 8/16/16.
 */

public class ErrorUtil {
    private String error;
    private String message;

    public ErrorUtil() {
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static ErrorUtil getFromString(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, ErrorUtil.class);
    }
}
