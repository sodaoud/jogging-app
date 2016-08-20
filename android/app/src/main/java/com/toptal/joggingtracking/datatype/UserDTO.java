package com.toptal.joggingtracking.datatype;

import java.io.Serializable;

/**
 * Created by sofiane on 8/19/16.
 */

public class UserDTO implements Serializable {
    private String username;
    private String password;
    private String[] roles;

    public UserDTO() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String[] getRoles() {
        return roles;
    }

    public void setRoles(String[] roles) {
        this.roles = roles;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
