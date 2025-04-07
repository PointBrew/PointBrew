package com.example.pointbrew.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repository class that handles authentication operations
 */
@Singleton
public class AuthRepository {
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private static final String USERS_COLLECTION = "users";

    @Inject
    public AuthRepository(FirebaseAuth firebaseAuth, FirebaseFirestore firestore) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
    }

    /**
     * Get current logged in user
     */
    @Nullable
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Check if user is authenticated
     */
    public boolean isUserAuthenticated() {
        return firebaseAuth.getCurrentUser() != null;
    }

    /**
     * Login with email and password
     */
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(Exception e);
    }

    public void login(String email, String password, AuthCallback callback) {
        // Check if this email is associated with a Google account
        firebaseAuth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener(result -> {
                    if (result.getSignInMethods() != null && !result.getSignInMethods().isEmpty()) {
                        // Email exists - check if it's only a Google account
                        if (result.getSignInMethods().contains("google.com") && 
                            !result.getSignInMethods().contains("password")) {
                            callback.onError(new Exception("This email is registered with Google. Please sign in with Google instead."));
                            return;
                        }
                    }
                    
                    // Proceed with normal email/password login
                    firebaseAuth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener(authResult -> {
                                if (authResult.getUser() != null) {
                                    callback.onSuccess(authResult.getUser());
                                } else {
                                    callback.onError(new Exception("Authentication failed"));
                                }
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Register with email and password
     */
    public void register(String email, String password, AuthCallback callback) {
        // Check if this email is already used with a Google Sign-In account
        firebaseAuth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener(result -> {
                    if (result.getSignInMethods() != null && !result.getSignInMethods().isEmpty()) {
                        // Email exists - check if it's a Google account
                        if (result.getSignInMethods().contains("google.com")) {
                            callback.onError(new Exception("This email is already associated with a Google account. Please sign in with Google."));
                            return;
                        }
                    }
                    
                    // Proceed with registration
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener(authResult -> {
                                if (authResult.getUser() != null) {
                                    // Create user document in Firestore with default role
                                    createUserInFirestore(authResult.getUser(), null, null, null, callback);
                                } else {
                                    callback.onError(new Exception("Registration failed"));
                                }
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Register with email, password and additional user data
     */
    public void registerWithUserData(String email, String password, String firstName, 
                                     String lastName, String dateOfBirth, AuthCallback callback) {
        // Check if this email is already used with a Google Sign-In account
        firebaseAuth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener(result -> {
                    if (result.getSignInMethods() != null && !result.getSignInMethods().isEmpty()) {
                        // Email exists - check if it's a Google account
                        if (result.getSignInMethods().contains("google.com")) {
                            callback.onError(new Exception("This email is already associated with a Google account. Please sign in with Google."));
                            return;
                        }
                    }
                    
                    // Proceed with registration
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener(authResult -> {
                                if (authResult.getUser() != null) {
                                    // Create user document in Firestore with default role and user data
                                    createUserInFirestore(authResult.getUser(), firstName, lastName, dateOfBirth, callback);
                                } else {
                                    callback.onError(new Exception("Registration failed"));
                                }
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Create user document in Firestore
     */
    private void createUserInFirestore(FirebaseUser user, String firstName, String lastName, 
                                      String dateOfBirth, AuthCallback callback) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("role", "user"); // Default role
        userData.put("createdAt", System.currentTimeMillis());
        
        // Add optional user data if provided
        if (firstName != null) userData.put("firstName", firstName);
        if (lastName != null) userData.put("lastName", lastName);
        if (dateOfBirth != null) userData.put("dateOfBirth", dateOfBirth);
        
        // Save to Firestore
        firestore.collection(USERS_COLLECTION).document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> callback.onSuccess(user))
                .addOnFailureListener(e -> {
                    // If Firestore fails, still return success but log the error
                    // because the Firebase Auth user was created successfully
                    callback.onSuccess(user);
                });
    }

    /**
     * Sign in with Google
     */
    public void signInWithGoogle(GoogleSignInAccount acct, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

        // First check if this Google email already exists with email/password authentication
        firebaseAuth.fetchSignInMethodsForEmail(acct.getEmail())
                .addOnSuccessListener(result -> {
                    if (result.getSignInMethods() != null && !result.getSignInMethods().isEmpty()) {
                        // Email exists but not with Google - it's an email/password account
                        if (!result.getSignInMethods().contains("google.com") && 
                            result.getSignInMethods().contains("password")) {
                            // Link accounts by signing in with the credential
                            firebaseAuth.signInWithCredential(credential)
                                    .addOnSuccessListener(authResult -> {
                                        // Success - now the Google account is linked
                                        callback.onSuccess(authResult.getUser());
                                    })
                                    .addOnFailureListener(e -> {
                                        callback.onError(new Exception("This email is already registered with password. Please sign in with your password."));
                                    });
                            return;
                        }
                    }
                    
                    // Normal Google sign-in flow
                    firebaseAuth.signInWithCredential(credential)
                            .addOnSuccessListener(authResult -> {
                                if (authResult.getUser() != null) {
                                    // Check if this is a new user
                                    if (authResult.getAdditionalUserInfo() != null && 
                                        authResult.getAdditionalUserInfo().isNewUser()) {
                                        // New Google user - create Firestore record
                                        String firstName = acct.getGivenName();
                                        String lastName = acct.getFamilyName();
                                        createUserInFirestore(authResult.getUser(), firstName, lastName, null, callback);
                                    } else {
                                        // Existing user - just return success
                                        callback.onSuccess(authResult.getUser());
                                    }
                                } else {
                                    callback.onError(new Exception("Google authentication failed"));
                                }
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Send password reset email
     */
    public interface ResetPasswordCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public void sendPasswordResetEmail(String email, ResetPasswordCallback callback) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    /**
     * Sign out
     */
    public void signOut() {
        firebaseAuth.signOut();
    }

    /**
     * Check if a user's account is linked to multiple providers
     */
    public interface AccountProviderCallback {
        void onResult(boolean isGoogleLinked, boolean isEmailPasswordLinked);
        void onError(Exception e);
    }
    
    /**
     * Check which authentication providers are linked to the current user's account
     */
    public void checkLinkedAuthProviders(AccountProviderCallback callback) {
        FirebaseUser currentUser = getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("No user is currently signed in"));
            return;
        }
        
        currentUser.getIdToken(true)
                .addOnSuccessListener(result -> {
                    // Get the user's email
                    String email = currentUser.getEmail();
                    if (email == null) {
                        callback.onError(new Exception("User has no email address"));
                        return;
                    }
                    
                    // Fetch sign-in methods for this email
                    firebaseAuth.fetchSignInMethodsForEmail(email)
                            .addOnSuccessListener(methods -> {
                                if (methods.getSignInMethods() != null) {
                                    boolean isGoogleLinked = methods.getSignInMethods().contains("google.com");
                                    boolean isEmailPasswordLinked = methods.getSignInMethods().contains("password");
                                    callback.onResult(isGoogleLinked, isEmailPasswordLinked);
                                } else {
                                    callback.onError(new Exception("Could not determine linked providers"));
                                }
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }
} 