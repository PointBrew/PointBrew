package com.example.pointbrew.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.pointbrew.R;
import com.example.pointbrew.data.model.User;
import com.example.pointbrew.viewmodel.MainViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    private MainViewModel viewModel;
    private TextView pointsTextView;
    private TextView welcomeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize views
        pointsTextView = findViewById(R.id.tvPoints);
        welcomeTextView = findViewById(R.id.tvWelcome);
        FloatingActionButton earnPointsFab = findViewById(R.id.fabEarnPoints);
        
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        
        // Observe current user
        viewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                updateUserInterface(user);
            } else {
                // Handle case where user is null (should not happen)
                Log.e(TAG, "User is null in MainActivity");
            }
        });
        
        // Set up points earning button (this would typically be used by staff)
        earnPointsFab.setOnClickListener(v -> {
            // In a real app, this would open a scanner or other earning mechanism
            showAddPointsDialog();
        });
        
        // Load rewards
        viewModel.loadAvailableRewards();
        
        // Observe rewards
        viewModel.getAvailableRewards().observe(this, rewards -> {
            // Update rewards UI
            if (rewards != null) {
                Log.d(TAG, "Loaded " + rewards.size() + " rewards");
                // We would update a RecyclerView or similar here
            }
        });
    }
    
    private void updateUserInterface(User user) {
        // Update points display
        pointsTextView.setText(getString(R.string.points_display, user.getPoints()));
        
        // Update welcome message
        String displayName = user.getDisplayName();
        if (displayName.isEmpty()) {
            displayName = user.getEmail();
        }
        welcomeTextView.setText(getString(R.string.welcome_message, displayName));
        
        // Update admin UI elements visibility based on role
        boolean isAdmin = user.isAdmin();
        findViewById(R.id.fabEarnPoints).setVisibility(isAdmin ? android.view.View.VISIBLE : android.view.View.GONE);
    }
    
    private void showAddPointsDialog() {
        // In a real app, this would show a dialog to scan a QR code or manually enter a user ID
        // For now, just add points to the current user for demonstration
        viewModel.addPointsToCurrentUser(10, "Demo points addition");
        Toast.makeText(this, "Added 10 points", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            viewModel.logout();
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_transaction_history) {
            // Open transaction history
            Toast.makeText(this, "Transaction history", Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getItemId() == R.id.action_profile) {
            // Open user profile
            Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 