package com.example.pointbrew.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pointbrew.data.model.Reward;
import com.example.pointbrew.data.model.Transaction;
import com.example.pointbrew.data.model.User;
import com.example.pointbrew.data.repository.AuthRepository;
import com.example.pointbrew.data.repository.RewardRepository;
import com.example.pointbrew.data.repository.TransactionRepository;
import com.example.pointbrew.data.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MainViewModel extends ViewModel {
    private static final String TAG = "MainViewModel";
    
    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final RewardRepository rewardRepository;
    private final TransactionRepository transactionRepository;
    
    private final MutableLiveData<List<Transaction>> _userTransactions = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Reward>> _availableRewards = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<User>> _allUsers = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<OperationState> _operationState = new MutableLiveData<>(OperationState.IDLE);
    private final MutableLiveData<Integer> _totalEarnedPoints = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> _totalRedeemedPoints = new MutableLiveData<>(0);
    
    @Inject
    public MainViewModel(
            AuthRepository authRepository,
            UserRepository userRepository,
            RewardRepository rewardRepository,
            TransactionRepository transactionRepository) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
        this.rewardRepository = rewardRepository;
        this.transactionRepository = transactionRepository;
        
        // Initialize default rewards if needed
        initializeDefaultRewards();
    }
    
    /**
     * Initialize default rewards in Firestore for new installations
     */
    private void initializeDefaultRewards() {
        rewardRepository.initializeDefaultRewards(new RewardRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Default rewards initialized or already exist");
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error initializing default rewards", e);
            }
        });
    }
    
    /**
     * Get current user from Firebase Auth and Firestore
     */
    public LiveData<User> getCurrentUser() {
        return userRepository.getCurrentUserLiveData();
    }
    
    /**
     * Load user's transaction history
     */
    public void loadUserTransactions() {
        _operationState.setValue(OperationState.LOADING);
        
        User currentUser = userRepository.getCurrentUserLiveData().getValue();
        if (currentUser == null) {
            _operationState.setValue(OperationState.error("No user is currently logged in"));
            return;
        }
        
        String userId = currentUser.getUserId();
        transactionRepository.getUserTransactions(userId, new TransactionRepository.TransactionsCallback() {
            @Override
            public void onSuccess(List<Transaction> transactions) {
                _userTransactions.postValue(transactions);
                _operationState.postValue(OperationState.SUCCESS);
            }
            
            @Override
            public void onError(Exception e) {
                _operationState.postValue(OperationState.error(e.getMessage()));
            }
        });
    }
    
    /**
     * Load available rewards
     */
    public void loadAvailableRewards() {
        _operationState.setValue(OperationState.LOADING);
        
        rewardRepository.loadAvailableRewards(new RewardRepository.RewardsCallback() {
            @Override
            public void onSuccess(List<Reward> rewards) {
                _availableRewards.postValue(rewards);
                _operationState.postValue(OperationState.SUCCESS);
            }
            
            @Override
            public void onError(Exception e) {
                _operationState.postValue(OperationState.error(e.getMessage()));
            }
        });
    }
    
    /**
     * Add points to current user's account
     */
    public void addPointsToCurrentUser(int points, String notes) {
        _operationState.setValue(OperationState.LOADING);
        
        User currentUser = userRepository.getCurrentUserLiveData().getValue();
        if (currentUser == null) {
            _operationState.setValue(OperationState.error("No user is currently logged in"));
            return;
        }
        
        userRepository.addPoints(currentUser.getUserId(), points, notes, new UserRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                _operationState.postValue(OperationState.SUCCESS);
                // Refresh transaction history
                loadUserTransactions();
                // Refresh total points
                loadUserPointsSummary();
            }
            
            @Override
            public void onError(Exception e) {
                _operationState.postValue(OperationState.error(e.getMessage()));
            }
        });
    }
    
    /**
     * Redeem points for a reward
     */
    public void redeemReward(Reward reward) {
        _operationState.setValue(OperationState.LOADING);
        
        User currentUser = userRepository.getCurrentUserLiveData().getValue();
        if (currentUser == null) {
            _operationState.setValue(OperationState.error("No user is currently logged in"));
            return;
        }
        
        if (currentUser.getPoints() < reward.getPointsCost()) {
            _operationState.setValue(OperationState.error("Not enough points to redeem this reward"));
            return;
        }
        
        userRepository.redeemPoints(
                currentUser.getUserId(),
                reward.getPointsCost(),
                reward.getId(),
                reward.getName(),
                new UserRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        _operationState.postValue(OperationState.SUCCESS);
                        // Refresh transaction history
                        loadUserTransactions();
                        // Refresh total points
                        loadUserPointsSummary();
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        _operationState.postValue(OperationState.error(e.getMessage()));
                    }
                });
    }
    
    /**
     * Load and calculate user's points summary (earned and redeemed)
     */
    public void loadUserPointsSummary() {
        User currentUser = userRepository.getCurrentUserLiveData().getValue();
        if (currentUser == null) {
            return;
        }
        
        String userId = currentUser.getUserId();
        
        // Get total earned points
        transactionRepository.getUserTotalEarnedPoints(userId, new TransactionRepository.PointsCallback() {
            @Override
            public void onSuccess(int points) {
                _totalEarnedPoints.postValue(points);
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading total earned points", e);
            }
        });
        
        // Get total redeemed points
        transactionRepository.getUserTotalRedeemedPoints(userId, new TransactionRepository.PointsCallback() {
            @Override
            public void onSuccess(int points) {
                _totalRedeemedPoints.postValue(points);
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading total redeemed points", e);
            }
        });
    }
    
    /**
     * Load all users (admin only)
     */
    public void loadAllUsers() {
        _operationState.setValue(OperationState.LOADING);
        
        User currentUser = userRepository.getCurrentUserLiveData().getValue();
        if (currentUser == null || !currentUser.isAdmin()) {
            _operationState.setValue(OperationState.error("Not authorized to view all users"));
            return;
        }
        
        userRepository.getAllUsers(new UserRepository.UsersCallback() {
            @Override
            public void onSuccess(List<User> users) {
                _allUsers.postValue(users);
                _operationState.postValue(OperationState.SUCCESS);
            }
            
            @Override
            public void onError(Exception e) {
                _operationState.postValue(OperationState.error(e.getMessage()));
            }
        });
    }
    
    /**
     * Logout the current user
     */
    public void logout() {
        authRepository.signOut();
    }
    
    // Getters for LiveData
    public LiveData<List<Transaction>> getUserTransactions() {
        return _userTransactions;
    }
    
    public LiveData<List<Reward>> getAvailableRewards() {
        return _availableRewards;
    }
    
    public LiveData<List<User>> getAllUsers() {
        return _allUsers;
    }
    
    public LiveData<OperationState> getOperationState() {
        return _operationState;
    }
    
    public LiveData<Integer> getTotalEarnedPoints() {
        return _totalEarnedPoints;
    }
    
    public LiveData<Integer> getTotalRedeemedPoints() {
        return _totalRedeemedPoints;
    }
    
    /**
     * Operation state
     */
    public static class OperationState {
        public static final OperationState IDLE = new OperationState(Type.IDLE, null);
        public static final OperationState LOADING = new OperationState(Type.LOADING, null);
        public static final OperationState SUCCESS = new OperationState(Type.SUCCESS, null);
        
        public static OperationState error(String message) {
            return new OperationState(Type.ERROR, message);
        }
        
        private final Type type;
        private final String errorMessage;
        
        private OperationState(Type type, String errorMessage) {
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