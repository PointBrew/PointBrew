package com.example.pointbrew.data.model;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing a reward that users can redeem using their points
 */
public class Reward {
    private String id;
    private String name;
    private String description;
    private int pointsCost;
    private String imageUrl;
    private boolean isActive;
    private long createdAt;
    
    // Empty constructor for Firestore
    public Reward() {
    }
    
    public Reward(String id, String name, String description, int pointsCost, 
                 String imageUrl, boolean isActive, long createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.pointsCost = pointsCost;
        this.imageUrl = imageUrl;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }
    
    /**
     * Create a Reward object from a Firestore document map
     */
    public static Reward fromMap(String id, Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        
        String name = (String) map.get("name");
        String description = (String) map.get("description");
        Integer pointsCost = map.get("pointsCost") instanceof Long 
                ? ((Long) map.get("pointsCost")).intValue() 
                : (Integer) map.get("pointsCost");
        String imageUrl = (String) map.get("imageUrl");
        Boolean isActive = (Boolean) map.get("isActive");
        Long createdAt = (Long) map.get("createdAt");
        
        return new Reward(
                id,
                name != null ? name : "",
                description != null ? description : "",
                pointsCost != null ? pointsCost : 0,
                imageUrl != null ? imageUrl : "",
                isActive != null ? isActive : true,
                createdAt != null ? createdAt : System.currentTimeMillis()
        );
    }
    
    /**
     * Convert Reward object to a map for Firestore storage
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("description", description);
        map.put("pointsCost", pointsCost);
        map.put("imageUrl", imageUrl);
        map.put("isActive", isActive);
        map.put("createdAt", createdAt);
        return map;
    }
    
    /**
     * Create example default rewards for new installations
     */
    public static Reward[] getDefaultRewards() {
        long now = System.currentTimeMillis();
        
        return new Reward[] {
            new Reward("1", "Free Coffee", "Enjoy a free coffee of your choice", 100, 
                    "", true, now),
            new Reward("2", "Coffee & Pastry", "A coffee and pastry of your choice", 200, 
                    "", true, now),
            new Reward("3", "Premium Drink", "Any premium or specialty drink", 150, 
                    "", true, now),
            new Reward("4", "Loyalty Tumbler", "Exclusive PointBrew branded tumbler", 500, 
                    "", true, now),
            new Reward("5", "Coffee Beans (250g)", "Take home our signature coffee beans", 300, 
                    "", true, now)
        };
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPointsCost() {
        return pointsCost;
    }

    public void setPointsCost(int pointsCost) {
        this.pointsCost = pointsCost;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @NonNull
    @Override
    public String toString() {
        return "Reward{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", pointsCost=" + pointsCost +
                ", isActive=" + isActive +
                '}';
    }
} 