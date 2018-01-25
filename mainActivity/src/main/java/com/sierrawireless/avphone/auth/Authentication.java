package com.sierrawireless.avphone.auth;

import java.util.Date;

public class Authentication {
    private String accessToken;
    private Date expirationDate;
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public boolean isExpired(Date date) {
        return date.after(this.expirationDate);
    }
    
    public boolean isExpired() {
        return isExpired(new Date());
    }
    
    public Date getExpirationDate() {
        return this.expirationDate;
    }
    
    public void setExpirationDate(Date date) {
        this.expirationDate = date;
    }
}
