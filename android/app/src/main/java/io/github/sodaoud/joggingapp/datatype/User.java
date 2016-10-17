package io.github.sodaoud.joggingapp.datatype;

import java.io.Serializable;

/**
 * Created by sofiane on 8/19/16.
 */

public class User implements Serializable {
    private String id;
    private String username;
    private String[] roles;
    private Profile profile;

    public User() {
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

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User)) return false;
        if (getId() == null && ((User) o).getId() != null) return false;
        if (getId() != null && ((User) o).getId() == null) return false;
        if (getId() == null && ((User) o).getId() == null) return true;
        if (((User) o).getId().equals(getId())) return true;
        return super.equals(o);
    }

    @Override
    public String toString() {
        return username;
    }
}
