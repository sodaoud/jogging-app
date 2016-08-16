package com.toptal.joggingtracking.datatype;

import com.google.gson.Gson;

/**
 * Created by sofiane on 8/14/16.
 */

public class TokenUtil {
    private String token;
    private String[] roles;

    public TokenUtil() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String[] getRoles() {
        return roles;
    }

    public void setRoles(String[] roles) {
        this.roles = roles;
    }

    public static TokenUtil getFromString(String string) {
        Gson gson = new Gson();
        return gson.fromJson(string, TokenUtil.class);
    }
}
