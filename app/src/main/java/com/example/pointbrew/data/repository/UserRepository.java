package com.example.pointbrew.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pointbrew.data.model.Transaction;
import com.example.pointbrew.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction.Function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserRepository {
    private static final String USERS_COLLECTION = "users";
    private static final String TRANSACTIONS_COLLECTION = "transactions";
    
    // Hardcoded admin email - this could be moved to a secure config
    private static final List<String> ADMIN_EMAILS = Arrays.asList("admin@pointbrew.com");
    
    private final FirebaseFirestore firestore;
    private final FirebaseAuth firebaseAuth;
    private final MutableLiveData<User> currentUserLiveData = new MutableLiveData<>();
    
    @Inject
    public UserRepository(FirebaseFirestore firestore, FirebaseAuth firebaseAuth) {
        this.firestore = firestore;
        this.firebaseAuth = firebaseAuth;
    }
    
    /**
     * Load the current user data from Firestore
     */
    public void loadCurrentUser(UserCallback callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onError(new Exception("No user is currently signed in"));
            return;
        }
        
        String userId = firebaseUser.getUid();
        firestore.collection(USERS_COLLECTION).document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = User.fromMap(userId, documentSnapshot.getData());
                        
                        // Check if this user's email is in the admin list
                        if (ADMIN_EMAILS.contains(user.getEmail()) && !"admin".equals(user.getRole())) {
                            // Upgrade to admin if necessary
                            user.setRole("admin");
                            updateUserRole(user, "admin", new SimpleCallback() {
                                @Override
                                public void onSuccess() {
                                    currentUserLiveData.setValue(user);
                                    callback.onSuccess(user);
                                }

                                @Override
                                public void onError(Exception e) {
                                    // Still return the user even if role update fails
                                    currentUserLiveData.setValue(user);
                                    callback.onSuccess(user);
                                }
                            });
                        } else {
                            currentUserLiveData.setValue(user);
                            callback.onSuccess(user);
                        }
                    } else {
                        // User document doesn't exist - create a basic one
                        User newUser = new User(
                                userId,
                                firebaseUser.getEmail(),
                                firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "",
                                "",
                                "",
                                ADMIN_EMAILS.contains(firebaseUser.getEmail()) ? "admin" : "user",
                                0,
                                System.currentTimeMillis()
                        );
                        
                        createNewUser(newUser, callback);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e));
    }
    
    /**
     * Create a new user document in Firestore
     */
    private void createNewUser(User user, UserCallback callback) {
        firestore.collection(USERS_COLLECTION).document(user.getUserId())
                .set(user.toMap())
                .addOnSuccessListener(aVoid -> {
                    currentUserLiveData.setValue(user);
                    callback.onSuccess(user);
                })
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Get the cached current user
     */
    public LiveData<User> getCurrentUserLiveData() {
        return currentUserLiveData;
    }
    
    /**
     * Add points to a user's account and record the transaction
     */
    public void addPoints(String userId, int points, String notes, SimpleCallback callback) {
        // Get a reference to the user document
        DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(userId);
        
        // Create a transaction object
        Transaction transaction = Transaction.createEarnTransaction(userId, points, notes);
        
        // Run a Firestore transaction to update both documents atomically
        firestore.runTransaction((Function<Void>) transaction1 -> {
            DocumentSnapshot userSnapshot = transaction1.get(userRef);
            User user = User.fromMap(userId, userSnapshot.getData());
            
            // Calculate new points total
            int currentPoints = user.getPoints();
            int newTotal = currentPoints + points;
            
            // Update user document
            transaction1.update(userRef, "points", newTotal);
            
            // Add transaction document
            DocumentReference transactionRef = firestore.collection(TRANSACTIONS_COLLECTION).document();
            transaction.setId(transactionRef.getId());
            transaction1.set(transactionRef, transaction.toMap());
            
            // Update local cached user
            User cachedUser = currentUserLiveData.getValue();
            if (cachedUser != null && cachedUser.getUserId().equals(userId)) {
                cachedUser.setPoints(newTotal);
                currentUserLiveData.postValue(cachedUser);
            }
            
            return null;
        }).addOnSuccessListener(aVoid -> callback.onSuccess())
          .addOnFailureListener(callback::onError);
    }
    
    /**
     * Redeem points for a reward and record the transaction
     */
    public void redeemPoints(String userId, int points, String rewardId, String rewardName, SimpleCallback callback) {
        // Get a reference to the user document
        DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(userId);
        
        // Create a transaction object
        Transaction transaction = Transaction.createRedeemTransaction(
                userId, points, rewardId, "Redeemed for: " + rewardName);
        
        // Run a Firestore transaction to update both documents atomically
        firestore.runTransaction((Function<Void>) transaction1 -> {
            DocumentSnapshot userSnapshot = transaction1.get(userRef);
            User user = User.fromMap(userId, userSnapshot.getData());
            
            // Check if user has enough points
            int currentPoints = user.getPoints();
            if (currentPoints < points) {
                throw new Exception("Not enough points to redeem this reward");
            }
            
            // Calculate new points total
            int newTotal = currentPoints - points;
            
            // Update user document
            transaction1.update(userRef, "points", newTotal);
            
            // Add transaction document
            DocumentReference transactionRef = firestore.collection(TRANSACTIONS_COLLECTION).document();
            transaction.setId(transactionRef.getId());
            transaction1.set(transactionRef, transaction.toMap());
            
            // Update local cached user
            User cachedUser = currentUserLiveData.getValue();
            if (cachedUser != null && cachedUser.getUserId().equals(userId)) {
                cachedUser.setPoints(newTotal);
                currentUserLiveData.postValue(cachedUser);
            }
            
            return null;
        }).addOnSuccessListener(aVoid -> callback.onSuccess())
          .addOnFailureListener(callback::onError);
    }
    
    /**
     * Update a user's role (admin only)
     */
    public void updateUserRole(User user, String newRole, SimpleCallback callback) {
        DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(user.getUserId());
        userRef.update("role", newRole)
                .addOnSuccessListener(aVoid -> {
                    user.setRole(newRole);
                    
                    // If this is the current user, update the cache
                    User currentUser = currentUserLiveData.getValue();
                    if (currentUser != null && currentUser.getUserId().equals(user.getUserId())) {
                        currentUserLiveData.setValue(user);
                    }
                    
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Get a list of all users (admin only)
     */
    public void getAllUsers(UsersCallback callback) {
        firestore.collection(USERS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        User user = User.fromMap(doc.getId(), doc.getData());
                        users.add(user);
                    }
                    callback.onSuccess(users);
                })
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Get the transaction history for a user
     */
    public void getUserTransactions(String userId, TransactionsCallback callback) {
        firestore.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Transaction> transactions = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Transaction transaction = Transaction.fromMap(doc.getId(), doc.getData());
                        transactions.add(transaction);
                    }
                    callback.onSuccess(transactions);
                })
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Add an admin email to the hardcoded list
     */
    public void addAdminEmail(String email) {
        // Note: In a real app, this should be secured better
        if (!ADMIN_EMAILS.contains(email)) {
            ADMIN_EMAILS.add(email);
        }
    }
    
    /**
     * Callbacks for async operations
     */
    public interface UserCallback {
        void onSuccess(User user);
        void onError(Exception e);
    }
    
    public interface UsersCallback {
        void onSuccess(List<User> users);
        void onError(Exception e);
    }
    
    public interface TransactionsCallback {
        void onSuccess(List<Transaction> transactions);
        void onError(Exception e);
    }
    
    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }
} 