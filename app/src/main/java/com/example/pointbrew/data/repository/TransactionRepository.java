package com.example.pointbrew.data.repository;

import com.example.pointbrew.data.model.Transaction;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TransactionRepository {
    private static final String TRANSACTIONS_COLLECTION = "transactions";
    
    private final FirebaseFirestore firestore;
    
    @Inject
    public TransactionRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }
    
    /**
     * Record a new transaction in Firestore
     */
    public void recordTransaction(Transaction transaction, SimpleCallback callback) {
        DocumentReference transactionRef;
        
        if (transaction.getId() != null && !transaction.getId().isEmpty()) {
            transactionRef = firestore.collection(TRANSACTIONS_COLLECTION).document(transaction.getId());
        } else {
            transactionRef = firestore.collection(TRANSACTIONS_COLLECTION).document();
            transaction.setId(transactionRef.getId());
        }
        
        transactionRef.set(transaction.toMap())
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Get all transactions for a user
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
     * Get all transactions for a specific reward
     */
    public void getRewardTransactions(String rewardId, TransactionsCallback callback) {
        firestore.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("rewardId", rewardId)
                .whereEqualTo("type", Transaction.TYPE_REDEEM)
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
     * Get a user's total earned points (lifetime)
     */
    public void getUserTotalEarnedPoints(String userId, PointsCallback callback) {
        firestore.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", Transaction.TYPE_EARN)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalEarned = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Transaction transaction = Transaction.fromMap(doc.getId(), doc.getData());
                        totalEarned += transaction.getPoints();
                    }
                    callback.onSuccess(totalEarned);
                })
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Get a user's total redeemed points (lifetime)
     */
    public void getUserTotalRedeemedPoints(String userId, PointsCallback callback) {
        firestore.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", Transaction.TYPE_REDEEM)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalRedeemed = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Transaction transaction = Transaction.fromMap(doc.getId(), doc.getData());
                        totalRedeemed += transaction.getPoints();
                    }
                    callback.onSuccess(totalRedeemed);
                })
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Delete a transaction (admin only)
     */
    public void deleteTransaction(String transactionId, SimpleCallback callback) {
        DocumentReference transactionRef = firestore.collection(TRANSACTIONS_COLLECTION).document(transactionId);
        
        transactionRef.delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Get all transactions (admin only)
     */
    public void getAllTransactions(TransactionsCallback callback) {
        firestore.collection(TRANSACTIONS_COLLECTION)
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
     * Get all transactions for a specific date range
     */
    public void getTransactionsInDateRange(long startDate, long endDate, TransactionsCallback callback) {
        firestore.collection(TRANSACTIONS_COLLECTION)
                .whereGreaterThanOrEqualTo("createdAt", startDate)
                .whereLessThanOrEqualTo("createdAt", endDate)
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
     * Callbacks for async operations
     */
    public interface TransactionsCallback {
        void onSuccess(List<Transaction> transactions);
        void onError(Exception e);
    }
    
    public interface PointsCallback {
        void onSuccess(int points);
        void onError(Exception e);
    }
    
    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }
} 