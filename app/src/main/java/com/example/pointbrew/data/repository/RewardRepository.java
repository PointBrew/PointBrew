package com.example.pointbrew.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pointbrew.data.model.Reward;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RewardRepository {
    private static final String REWARDS_COLLECTION = "rewards";
    
    private final FirebaseFirestore firestore;
    private final MutableLiveData<List<Reward>> availableRewardsLiveData = new MutableLiveData<>();
    
    @Inject
    public RewardRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }
    
    /**
     * Load all available rewards from Firestore
     */
    public void loadAvailableRewards(RewardsCallback callback) {
        firestore.collection(REWARDS_COLLECTION)
                .whereEqualTo("isActive", true)
                .orderBy("pointsCost", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Reward> rewards = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Reward reward = Reward.fromMap(doc.getId(), doc.getData());
                        rewards.add(reward);
                    }
                    availableRewardsLiveData.setValue(rewards);
                    callback.onSuccess(rewards);
                })
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Get cached available rewards
     */
    public LiveData<List<Reward>> getAvailableRewardsLiveData() {
        return availableRewardsLiveData;
    }
    
    /**
     * Get a single reward by ID
     */
    public void getReward(String rewardId, RewardCallback callback) {
        firestore.collection(REWARDS_COLLECTION).document(rewardId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Reward reward = Reward.fromMap(documentSnapshot.getId(), documentSnapshot.getData());
                        callback.onSuccess(reward);
                    } else {
                        callback.onError(new Exception("Reward not found"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Create a new reward in Firestore (admin only)
     */
    public void createReward(Reward reward, SimpleCallback callback) {
        DocumentReference rewardRef = firestore.collection(REWARDS_COLLECTION).document();
        reward.setId(rewardRef.getId());
        
        rewardRef.set(reward.toMap())
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Update an existing reward (admin only)
     */
    public void updateReward(Reward reward, SimpleCallback callback) {
        DocumentReference rewardRef = firestore.collection(REWARDS_COLLECTION).document(reward.getId());
        
        rewardRef.update(reward.toMap())
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Toggle a reward's active status (admin only)
     */
    public void toggleRewardStatus(String rewardId, boolean isActive, SimpleCallback callback) {
        DocumentReference rewardRef = firestore.collection(REWARDS_COLLECTION).document(rewardId);
        
        rewardRef.update("isActive", isActive)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Delete a reward (admin only)
     */
    public void deleteReward(String rewardId, SimpleCallback callback) {
        DocumentReference rewardRef = firestore.collection(REWARDS_COLLECTION).document(rewardId);
        
        rewardRef.delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Initialize default rewards in Firestore for new installations
     */
    public void initializeDefaultRewards(SimpleCallback callback) {
        firestore.collection(REWARDS_COLLECTION)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Only initialize if no rewards exist
                    if (queryDocumentSnapshots.isEmpty()) {
                        Reward[] defaultRewards = Reward.getDefaultRewards();
                        initializeRewards(defaultRewards, 0, callback);
                    } else {
                        // Rewards already exist, no need to initialize
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Recursive helper method to initialize rewards one by one
     */
    private void initializeRewards(Reward[] rewards, int index, SimpleCallback callback) {
        if (index >= rewards.length) {
            callback.onSuccess();
            return;
        }
        
        Reward reward = rewards[index];
        DocumentReference rewardRef = firestore.collection(REWARDS_COLLECTION).document(reward.getId());
        
        rewardRef.set(reward.toMap())
                .addOnSuccessListener(aVoid -> initializeRewards(rewards, index + 1, callback))
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Get all rewards regardless of status (admin only)
     */
    public void getAllRewards(RewardsCallback callback) {
        firestore.collection(REWARDS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Reward> rewards = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Reward reward = Reward.fromMap(doc.getId(), doc.getData());
                        rewards.add(reward);
                    }
                    callback.onSuccess(rewards);
                })
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Callbacks for async operations
     */
    public interface RewardCallback {
        void onSuccess(Reward reward);
        void onError(Exception e);
    }
    
    public interface RewardsCallback {
        void onSuccess(List<Reward> rewards);
        void onError(Exception e);
    }
    
    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }
} 