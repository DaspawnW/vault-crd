package de.koudingspawn.vault.vault.impl.dockercfg;

import java.util.Base64;

public class PullSecret {

    private String username;
    private String password;
    private String email;
    private String url;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAuth() {
        String concatedAuth = getUsername() + ":" + getPassword();
        return Base64.getEncoder().encodeToString(concatedAuth.getBytes());
    }
}
