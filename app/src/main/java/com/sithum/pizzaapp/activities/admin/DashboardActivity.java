package com.sithum.pizzaapp.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.sithum.pizzaapp.R;
import com.sithum.pizzaapp.activities.ProfileActivity;
import com.sithum.pizzaapp.activities.SigninActivity;

public class DashboardActivity extends AppCompatActivity {

    ImageView btnback;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        btnback = findViewById(R.id.btnBack);

        // Initialize views
        CardView cardOrderManagement = findViewById(R.id.cardOrderManagement);
        CardView cardBranchManagement = findViewById(R.id.cardBranchManagement);
        CardView cardProductManagement = findViewById(R.id.cardProductManagement);
        CardView cardUserManagement = findViewById(R.id.cardUserManagement);

        btnback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        // Set click listeners
        cardOrderManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Order Management Activity
                Intent intent = new Intent(DashboardActivity.this, OrderManageActivity.class);
                startActivity(intent);
            }
        });

        cardBranchManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Branch Management Activity
                Intent intent = new Intent(DashboardActivity.this, BranchManageActivity.class);
                startActivity(intent);
            }
        });

        cardProductManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Product Management Activity
                Intent intent = new Intent(DashboardActivity.this, ProductManageActivity.class);
                startActivity(intent);
            }
        });

        cardUserManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to User Management Activity
                Intent intent = new Intent(DashboardActivity.this, UserManageActivity.class);
                startActivity(intent);
            }
        });
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate to SigninActivity
        Intent intent = new Intent(DashboardActivity.this, SigninActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}