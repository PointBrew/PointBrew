package com.example.pointbrew.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pointbrew.data.model.User;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AuthRepository {
    private static final String TAG = "AuthRepository";

    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;
    private final MutableLiveData<Boolean> isLoggedInLiveData = new MutableLiveData<>();

    @Inject
    public AuthRepository(FirebaseAuth firebaseAuth, UserRepository userRepository) {
        this.firebaseAuth = firebaseAuth;
        this.userRepository = userRepository;
        
        // Initialize logged in state
        isLoggedInLiveData.setValue(firebaseAuth.getCurrentUser() != null);
        
        // Listen for auth state changes
        firebaseAuth.addAuthStateListener(auth -> {
            boolean isLoggedIn = auth.getCurrentUser() != null;
            isLoggedInLiveData.postValue(isLoggedIn);
            
            // When user logs in, load their user data
            if (isLoggedIn) {
                loadCurrentUser();
            }
        });
    }
    
    /**
     * Load the current user's data from Firestore
     */
    private void loadCurrentUser() {
        userRepository.loadCurrentUser(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                Log.d(TAG, "User data loaded: " + user.getDisplayName());
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading user data", e);
            }
        });
    }

    /**
     * Sign in with email and password
     */
    public void signInWithEmailAndPassword(String email, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    loadUserData(user, callback);
                })
                .addOnFailureListener(e -> {
                    callback.onError(e);
                });
    }

    /**
     * Sign in with Google credential
     */
    public void signInWithGoogle(String idToken, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    loadUserData(user, callback);
                })
                .addOnFailureListener(e -> {
                    callback.onError(e);
                });
    }

    /**
     * Register with email and password
     */
    public void registerWithEmailAndPassword(String email, String password, String displayName, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        // Create a user profile update request to set the display name
                        firebaseUser.updateProfile(new UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName)
                                .build())
                                .addOnSuccessListener(aVoid -> loadUserData(firebaseUser, callback))
                                .addOnFailureListener(callback::onError);
                    } else {
                        callback.onError(new Exception("Failed to create user"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Helper method to load user data after authentication
     */
    private void loadUserData(FirebaseUser firebaseUser, AuthCallback callback) {
        if (firebaseUser == null) {
            callback.onError(new Exception("User authentication failed"));
            return;
        }
        
        userRepository.loadCurrentUser(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                callback.onSuccess(user);
            }

            @Override
            public void onError(Exception e) {
                // If we can't load the user, still return success for the authentication
                // but with a basic user object
                User basicUser = new User(
                        firebaseUser.getUid(),
                        firebaseUser.getEmail(),
                        firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "",
                        "",
                        firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "",
                        "user",
                        0,
                        System.currentTimeMillis()
                );
                callback.onSuccess(basicUser);
                
                // Log the error
                Log.e(TAG, "Error loading user data after auth", e);
            }
        });
    }

    /**
     * Sign out the current user
     */
    public void signOut() {
        firebaseAuth.signOut();
    }

    /**
     * Get the current logged-in state as LiveData
     */
    public LiveData<Boolean> getIsLoggedInLiveData() {
        return isLoggedInLiveData;
    }

    /**
     * Send a password reset email
     */
    public void sendPasswordResetEmail(String email, ResetPasswordCallback callback) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    /**
     * Check if user is currently logged in
     */
    public boolean isLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    /**
     * Get current Firebase user
     */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }
    
    /**
     * Get current app user (includes Firestore data)
     */
    public LiveData<User> getCurrentUserData() {
        return userRepository.getCurrentUserLiveData();
    }

    /**
     * Callbacks for auth operations
     */
    public interface AuthCallback {
        void onSuccess(User user);
        void onError(Exception e);
    }

    public interface ResetPasswordCallback {
        void onSuccess();
        void onError(Exception e);
    }
} 