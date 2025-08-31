package com.sithum.pizzaapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sithum.pizzaapp.R;

public class ProfileActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvUserName, tvUserEmail;
    private Button logoutBtn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        // Handle edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize views
        initViews();

        // Set up click listeners
        setupClickListeners();

        // Load user data
        loadUserData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvUserName = findViewById(R.id.txtname);
        tvUserEmail = findViewById(R.id.txtemail);
        logoutBtn = findViewById(R.id.logoutBtn);

        // Set default user data
        if (currentUser != null) {
            tvUserEmail.setText(currentUser.getEmail());
            tvUserName.setText(currentUser.getDisplayName() != null ?
                    currentUser.getDisplayName() : "User");
        }
    }

    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Logout button
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });
    }

    private void loadUserData() {
        if (currentUser != null) {
            // Get user data from Firestore
            db.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(Task<DocumentSnapshot> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    // Update UI with user data
                                    String fullName = document.getString("fullName");
                                    String email = document.getString("email");
                                    String phone = document.getString("phone");

                                    if (fullName != null && !fullName.isEmpty()) {
                                        tvUserName.setText(fullName);
                                    }

                                    if (email != null && !email.isEmpty()) {
                                        tvUserEmail.setText(email);
                                    }
                                }
                            } else {
                                // Use auth data as fallback
                                if (currentUser.getDisplayName() != null) {
                                    tvUserName.setText(currentUser.getDisplayName());
                                }
                                tvUserEmail.setText(currentUser.getEmail());
                            }
                        }
                    });
        }
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate to SigninActivity
        Intent intent = new Intent(ProfileActivity.this, SigninActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Add animation if desired
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in
        if (mAuth.getCurrentUser() == null) {
            // User is not signed in, redirect to login
            Intent intent = new Intent(this, SigninActivity.class);
            startActivity(intent);
            finish();
        }
    }
}