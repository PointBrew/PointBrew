package com.example.pointbrew.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pointbrew.data.model.User;
import com.example.pointbrew.data.repository.AuthRepository;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class LoginViewModel extends ViewModel {
    
    private final AuthRepository authRepository;
    
    private final MutableLiveData<LoginState> _loginState = new MutableLiveData<>();
    private final MutableLiveData<ResetPasswordState> _resetPasswordState = new MutableLiveData<>();
    
    public LiveData<LoginState> getLoginState() {
        return _loginState;
    }
    
    public LiveData<ResetPasswordState> getResetPasswordState() {
        return _resetPasswordState;
    }
    
    @Inject
    public LoginViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
        _loginState.setValue(LoginState.IDLE);
        _resetPasswordState.setValue(ResetPasswordState.IDLE);
    }
    
    /**
     * Login with email and password
     */
    public void login(String email, String password) {
        _loginState.setValue(LoginState.LOADING);
        
        if (email.isEmpty() || password.isEmpty()) {
            _loginState.setValue(LoginState.error("Email and password cannot be empty"));
            return;
        }
        
        authRepository.signInWithEmailAndPassword(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                _loginState.postValue(LoginState.SUCCESS);
            }
            
            @Override
            public void onError(Exception e) {
                _loginState.postValue(LoginState.error(e.getMessage() != null ? e.getMessage() : "Unknown error occurred"));
            }
        });
    }
    
    /**
     * Register with email and password
     */
    public void register(String email, String password, String displayName) {
        _loginState.setValue(LoginState.LOADING);
        
        if (email.isEmpty() || password.isEmpty()) {
            _loginState.setValue(LoginState.error("Email and password cannot be empty"));
            return;
        }
        
        if (displayName.isEmpty()) {
            displayName = email.substring(0, email.indexOf('@'));
        }
        
        final String finalDisplayName = displayName;
        
        authRepository.registerWithEmailAndPassword(email, password, finalDisplayName, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                _loginState.postValue(LoginState.SUCCESS);
            }
            
            @Override
            public void onError(Exception e) {
                _loginState.postValue(LoginState.error(e.getMessage() != null ? e.getMessage() : "Unknown error occurred"));
            }
        });
    }
    
    /**
     * Sign in with Google
     */
    public void signInWithGoogle(GoogleSignInAccount account) {
        if (account == null) {
            _loginState.setValue(LoginState.error("Google sign in failed"));
            return;
        }
        
        _loginState.setValue(LoginState.LOADING);
        
        authRepository.signInWithGoogle(account.getIdToken(), new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                _loginState.postValue(LoginState.SUCCESS);
            }
            
            @Override
            public void onError(Exception e) {
                _loginState.postValue(LoginState.error(e.getMessage() != null ? e.getMessage() : "Google sign in failed"));
            }
        });
    }
    
    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String email) {
        _resetPasswordState.setValue(ResetPasswordState.LOADING);
        
        if (email.isEmpty()) {
            _resetPasswordState.setValue(ResetPasswordState.error("Email cannot be empty"));
            return;
        }
        
        authRepository.sendPasswordResetEmail(email, new AuthRepository.ResetPasswordCallback() {
            @Override
            public void onSuccess() {
                _resetPasswordState.postValue(ResetPasswordState.SUCCESS);
            }
            
            @Override
            public void onError(Exception e) {
                _resetPasswordState.postValue(ResetPasswordState.error(e.getMessage() != null ? e.getMessage() : "Unknown error occurred"));
            }
        });
    }
    
    /**
     * Reset login state
     */
    public void resetLoginState() {
        _loginState.setValue(LoginState.IDLE);
    }
    
    /**
     * Reset password reset state
     */
    public void resetPasswordResetState() {
        _resetPasswordState.setValue(ResetPasswordState.IDLE);
    }
    
    /**
     * Check if user is already logged in
     */
    public boolean isUserLoggedIn() {
        return authRepository.isLoggedIn();
    }
    
    /**
     * Login state
     */
    public static class LoginState {
        public static final LoginState IDLE = new LoginState(Type.IDLE, null);
        public static final LoginState LOADING = new LoginState(Type.LOADING, null);
        public static final LoginState SUCCESS = new LoginState(Type.SUCCESS, null);
        
        public static LoginState error(String message) {
            return new LoginState(Type.ERROR, message);
        }
        
        private final Type type;
        private final String errorMessage;
        
        private LoginState(Type type, String errorMessage) {
            this.type = type;
            this.errorMessage = errorMessage;
        }
        
        public Type getType() {
            return type;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public enum Type {
            IDLE, LOADING, SUCCESS, ERROR
        }
    }
    
    /**
     * Reset password state
     */
    public static class ResetPasswordState {
        public static final ResetPasswordState IDLE = new ResetPasswordState(Type.IDLE, null);
        public static final ResetPasswordState LOADING = new ResetPasswordState(Type.LOADING, null);
        public static final ResetPasswordState SUCCESS = new ResetPasswordState(Type.SUCCESS, null);
        
        public static ResetPasswordState error(String message) {
            return new ResetPasswordState(Type.ERROR, message);
        }
        
        private final Type type;
        private final String errorMessage;
        
        private ResetPasswordState(Type type, String errorMessage) {
            this.type = type;
            this.errorMessage = errorMessage;
        }
        
        public Type getType() {
            return type;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public enum Type {
            IDLE, LOADING, SUCCESS, ERROR
        }
    }
} 