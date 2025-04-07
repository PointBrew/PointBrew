package com.example.pointbrew.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.pointbrew.R;
import com.example.pointbrew.databinding.ActivityLoginBinding;
import com.example.pointbrew.viewmodel.LoginViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    
    // Views inside CardView
    private TabLayout tabLayout;
    private EditText etEmail;
    private EditText etPassword;
    private CheckBox cbRememberMe;
    private TextView tvForgotPassword;
    private Button btnLogin;
    private ImageButton btnTogglePassword;
    
    // Header and subtitle
    private TextView tvHeader;
    private TextView tvSubtitle;
    
    // Signup fields
    private View signupFields;
    private EditText etFirstName;
    private EditText etLastName;
    private EditText etDob;
    
    private boolean isPasswordVisible = false;
    
    @Inject
    LoginViewModel loginViewModel;
    
    private boolean isLoginMode = true;
    private GoogleSignInClient googleSignInClient;
    
    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    handleGoogleSignInResult(data);
                } else {
                    showError("Google sign in failed");
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // Setup view binding
            binding = ActivityLoginBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            
            // Log success
            Log.d(TAG, "Activity created successfully");
            
            // Find views inside the card
            initViews();
            
            // Configure Google Sign In
            configureGoogleSignIn();
            
            // Setup UI components
            setupTabLayout();
            setupClickListeners();
            observeViewModel();
            
            // Add animations last, after everything is set up
            animateUI();
            
        } catch (Exception e) {
            // Log any exceptions
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            e.printStackTrace();
            
            // Fallback to a simple layout if binding fails
            try {
                setContentView(R.layout.activity_login);
                Log.d(TAG, "Fallback to simple layout successful");
            } catch (Exception fallbackError) {
                Log.e(TAG, "Critical error: Even fallback layout failed: " + fallbackError.getMessage(), fallbackError);
                // At this point, show a simple Toast and finish the activity
                Toast.makeText(this, "Error starting app: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    
    private void initViews() {
        try {
            // Initialize header and subtitle - careful with direct findViewById
            tvHeader = findViewById(R.id.tv_header);
            tvSubtitle = findViewById(R.id.tv_subtitle);
            
            if (binding == null) {
                Log.e(TAG, "Binding is null in initViews");
                return;
            }
            
            if (binding.cardLogin == null) {
                Log.e(TAG, "Card login is null in binding");
                return;
            }
            
            // Initialize views directly from binding
            tabLayout = binding.cardLogin.findViewById(R.id.tab_layout);
            etEmail = binding.cardLogin.findViewById(R.id.et_email);
            etPassword = binding.cardLogin.findViewById(R.id.et_password);
            cbRememberMe = binding.cardLogin.findViewById(R.id.cb_remember_me);
            tvForgotPassword = binding.cardLogin.findViewById(R.id.tv_forgot_password);
            btnLogin = binding.cardLogin.findViewById(R.id.btn_login);
            btnTogglePassword = binding.cardLogin.findViewById(R.id.btn_toggle_password);
            
            // Initialize signup fields
            signupFields = binding.cardLogin.findViewById(R.id.signup_fields);
            etFirstName = binding.cardLogin.findViewById(R.id.et_first_name);
            etLastName = binding.cardLogin.findViewById(R.id.et_last_name);
            etDob = binding.cardLogin.findViewById(R.id.et_dob);
            
            // Ensure all views were found
            if (tabLayout == null || etEmail == null || etPassword == null || 
                cbRememberMe == null || tvForgotPassword == null || btnLogin == null) {
                Log.e(TAG, "Failed to find one or more views");
                Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "All views initialized successfully");
            }
        } catch (Exception e) {
            // Log exception
            Log.e(TAG, "Error in initViews: " + e.getMessage(), e);
            e.printStackTrace();
            Toast.makeText(this, "Error initializing UI: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }
    
    private void setupTabLayout() {
        // Set initial state
        toggleSignupFieldsVisibility();
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isLoginMode = tab.getPosition() == 0;
                updateButtonText();
                toggleSignupFieldsVisibility();
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Not needed
            }
        });
    }
    
    private void updateButtonText() {
        btnLogin.setText(isLoginMode ? getString(R.string.login) : getString(R.string.signup));
        // Update header and subtitle based on mode
        if (tvHeader != null) {
            tvHeader.setText(isLoginMode ? getString(R.string.login) : getString(R.string.signup));
        }
        if (tvSubtitle != null) {
            tvSubtitle.setText(isLoginMode ? 
                getString(R.string.login_subtitle) : 
                getString(R.string.signup_subtitle));
        }
    }
    
    private void toggleSignupFieldsVisibility() {
        if (signupFields != null) {
            signupFields.setVisibility(isLoginMode ? View.GONE : View.VISIBLE);
            tvForgotPassword.setVisibility(isLoginMode ? View.VISIBLE : View.GONE);
            cbRememberMe.setVisibility(isLoginMode ? View.VISIBLE : View.GONE);
        }
    }
    
    private void setupClickListeners() {
        // Login/Register button
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            
            if (isLoginMode) {
                loginViewModel.login(email, password);
            } else {
                // Get additional registration fields
                String firstName = etFirstName != null ? etFirstName.getText().toString().trim() : "";
                String lastName = etLastName != null ? etLastName.getText().toString().trim() : "";
                String dob = etDob != null ? etDob.getText().toString().trim() : "";
                
                // Validate additional fields
                if (firstName.isEmpty() || lastName.isEmpty() || dob.isEmpty()) {
                    showError("Please fill in all fields");
                    return;
                }
                
                // Proceed with registration including Firestore user data
                loginViewModel.registerWithUserData(email, password, firstName, lastName, dob);
            }
        });
        
        // Forgot password
        tvForgotPassword.setOnClickListener(v -> {
            showForgotPasswordDialog();
        });
        
        // Password toggle visibility
        btnTogglePassword.setOnClickListener(v -> {
            togglePasswordVisibility();
        });
        
        // Date of birth picker
        if (etDob != null) {
            etDob.setOnClickListener(v -> {
                showDatePickerDialog();
            });
            etDob.setFocusable(false);
        }
        
        // Google sign in
        binding.btnGoogle.setOnClickListener(v -> {
            signInWithGoogle();
        });
    }
    
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        
        if (isPasswordVisible) {
            // Show password
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
        } else {
            // Hide password
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(R.drawable.ic_visibility);
        }
        
        // Maintain cursor position
        etPassword.setSelection(etPassword.getText().length());
    }
    
    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }
    
    private void handleGoogleSignInResult(Intent data) {
        try {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            GoogleSignInAccount account = task.getResult(ApiException.class);
            loginViewModel.signInWithGoogle(account);
        } catch (ApiException e) {
            showError("Google sign in failed: " + e.getStatusCode());
        }
    }
    
    private void observeViewModel() {
        loginViewModel.getLoginState().observe(this, state -> {
            if (state.getType() == LoginViewModel.LoginState.Type.LOADING) {
                showLoading(true);
            } else if (state.getType() == LoginViewModel.LoginState.Type.SUCCESS) {
                showLoading(false);
                navigateToMain();
            } else if (state.getType() == LoginViewModel.LoginState.Type.ERROR) {
                showLoading(false);
                showError(state.getErrorMessage());
            } else {
                showLoading(false);
            }
        });
        
        loginViewModel.getResetPasswordState().observe(this, state -> {
            if (state.getType() == LoginViewModel.ResetPasswordState.Type.SUCCESS) {
                Toast.makeText(
                    this,
                    "Password reset email sent. Please check your inbox.",
                    Toast.LENGTH_LONG
                ).show();
                loginViewModel.resetPasswordResetState();
            } else if (state.getType() == LoginViewModel.ResetPasswordState.Type.ERROR) {
                Toast.makeText(
                    this,
                    "Failed to send reset email: " + state.getErrorMessage(),
                    Toast.LENGTH_LONG
                ).show();
                loginViewModel.resetPasswordResetState();
            }
            // Don't handle IDLE or LOADING states
        });
    }
    
    private void showForgotPasswordDialog() {
        // TODO: Implement forgot password dialog
        Toast.makeText(this, "Forgot password not implemented yet", Toast.LENGTH_SHORT).show();
    }
    
    private void showLoading(boolean isLoading) {
        // Disable UI elements during loading
        btnLogin.setEnabled(!isLoading);
        binding.btnGoogle.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
        btnTogglePassword.setEnabled(!isLoading);
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    
    private void navigateToMain() {
        // TODO: Navigate to main activity
        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
        // Intent intent = new Intent(this, MainActivity.class);
        // startActivity(intent);
        // finish();
    }
    
    private void showDatePickerDialog() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int year = calendar.get(java.util.Calendar.YEAR) - 18; // Default to 18 years ago
        int month = calendar.get(java.util.Calendar.MONTH);
        int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);
        
        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
            this, 
            (view, selectedYear, selectedMonth, selectedDay) -> {
                // Format the date as MM/DD/YYYY
                String formattedDate = String.format(
                    java.util.Locale.US, 
                    "%02d/%02d/%04d", 
                    selectedMonth + 1, 
                    selectedDay, 
                    selectedYear
                );
                etDob.setText(formattedDate);
            }, 
            year, 
            month, 
            day
        );
        
        // Set maximum date to today (no future birthdays)
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        
        // Show the dialog
        datePickerDialog.show();
    }
    
    private void animateUI() {
        try {
            // Temporarily disable animations to troubleshoot inflation issues
            Log.d(TAG, "Animations temporarily disabled for troubleshooting");
            /*
            // Animate the logo
            View logoContainer = findViewById(R.id.logo_container);
            if (logoContainer != null) {
                android.view.animation.Animation fadeIn = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fade_in);
                logoContainer.startAnimation(fadeIn);
            }
            
            // Animate the card
            View cardLogin = findViewById(R.id.card_login);
            if (cardLogin != null) {
                android.view.animation.Animation slideUp = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_up);
                cardLogin.startAnimation(slideUp);
            }
            
            // Animate the Google button (with slight delay)
            View btnGoogle = findViewById(R.id.btn_google);
            if (btnGoogle != null) {
                android.view.animation.Animation slideUp = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_up);
                slideUp.setStartOffset(300);
                btnGoogle.startAnimation(slideUp);
            }
            */
        } catch (Exception e) {
            Log.e(TAG, "Error in animateUI: " + e.getMessage());
            // Non-critical error, so we just log it and continue
        }
    }
} 