package com.example.pointbrew.data.model;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing a user of the PointBrew app
 */
public class User {
    private String userId;
    private String email;
    private String displayName;
    private String phoneNumber;
    private String photoUrl;
    private String role;  // "user" or "admin"
    private int points;
    private long createdAt;
    
    // Empty constructor for Firestore
    public User() {
    }
    
    public User(String userId, String email, String displayName, String phoneNumber,
               String photoUrl, String role, int points, long createdAt) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
        this.photoUrl = photoUrl;
        this.role = role;
        this.points = points;
        this.createdAt = createdAt;
    }
    
    /**
     * Create a User object from a Firestore document map
     */
    public static User fromMap(String userId, Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        
        String email = (String) map.get("email");
        String displayName = (String) map.get("displayName");
        String phoneNumber = (String) map.get("phoneNumber");
        String photoUrl = (String) map.get("photoUrl");
        String role = (String) map.get("role");
        Integer points = map.get("points") instanceof Long 
                ? ((Long) map.get("points")).intValue() 
                : (Integer) map.get("points");
        Long createdAt = (Long) map.get("createdAt");
        
        return new User(
                userId,
                email != null ? email : "",
                displayName != null ? displayName : "",
                phoneNumber != null ? phoneNumber : "",
                photoUrl != null ? photoUrl : "",
                role != null ? role : "user",
                points != null ? points : 0,
                createdAt != null ? createdAt : System.currentTimeMillis()
        );
    }
    
    /**
     * Convert User object to a map for Firestore storage
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        map.put("displayName", displayName);
        map.put("phoneNumber", phoneNumber);
        map.put("photoUrl", photoUrl);
        map.put("role", role);
        map.put("points", points);
        map.put("createdAt", createdAt);
        return map;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getPhotoUrl() {
        return photoUrl;
    }
    
    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public int getPoints() {
        return points;
    }
    
    public void setPoints(int points) {
        this.points = points;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isAdmin() {
        return "admin".equals(role);
    }
    
    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", displayName='" + displayName + '\'' +
                ", role='" + role + '\'' +
                ", points=" + points +
                '}';
    }
} 