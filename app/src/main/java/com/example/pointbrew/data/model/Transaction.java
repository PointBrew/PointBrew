package com.example.pointbrew.data.model;

import java.util.HashMap;
import java.util.Map;

public class Transaction {
    public static final String TYPE_EARN = "earn";
    public static final String TYPE_REDEEM = "redeem";
    
    private String id;
    private String userId;
    private String type; // "earn" or "redeem"
    private int points;
    private String rewardId; // only for redeem transactions
    private String notes;
    private long createdAt;
    
    // Empty constructor for Firestore
    public Transaction() {
    }
    
    public Transaction(String id, String userId, String type, int points, 
                      String rewardId, String notes, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.points = points;
        this.rewardId = rewardId;
        this.notes = notes;
        this.createdAt = createdAt;
    }
    
    public static Transaction fromMap(String id, Map<String, Object> data) {
        return new Transaction(
            id,
            (String) data.get("userId"),
            (String) data.get("type"),
            data.get("points") != null ? ((Long) data.get("points")).intValue() : 0,
            (String) data.get("rewardId"),
            (String) data.get("notes"),
            data.get("createdAt") != null ? (Long) data.get("createdAt") : 0
        );
    }
    
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("type", type);
        map.put("points", points);
        if (rewardId != null) map.put("rewardId", rewardId);
        if (notes != null) map.put("notes", notes);
        map.put("createdAt", createdAt);
        return map;
    }
    
    // Factory methods for easy creation
    public static Transaction createEarnTransaction(String userId, int points, String notes) {
        return new Transaction(
            null, 
            userId, 
            TYPE_EARN, 
            points, 
            null, 
            notes, 
            System.currentTimeMillis()
        );
    }
    
    public static Transaction createRedeemTransaction(String userId, int points, 
                                                     String rewardId, String notes) {
        return new Transaction(
            null, 
            userId, 
            TYPE_REDEEM, 
            points, 
            rewardId, 
            notes, 
            System.currentTimeMillis()
        );
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public int getPoints() {
        return points;
    }
    
    public void setPoints(int points) {
        this.points = points;
    }
    
    public String getRewardId() {
        return rewardId;
    }
    
    public void setRewardId(String rewardId) {
        this.rewardId = rewardId;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isEarnType() {
        return TYPE_EARN.equals(type);
    }
    
    public boolean isRedeemType() {
        return TYPE_REDEEM.equals(type);
    }
} 