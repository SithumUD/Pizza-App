package com.sithum.pizzaapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sithum.pizzaapp.R;
import com.sithum.pizzaapp.adapters.CheckoutItemAdapter;
import com.sithum.pizzaapp.models.CartItem;
import com.sithum.pizzaapp.models.CustomizationOption;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CheckoutActivity extends AppCompatActivity {

    private static final String TAG = "CheckoutActivity";
    private static final String PREFS_NAME = "CartPrefs";
    private static final String KEY_CART_DATA = "cart_data";
    private static final String KEY_SUBTOTAL = "subtotal";
    private static final String KEY_DELIVERY_FEE = "delivery_fee";
    private static final String KEY_TOTAL = "total";
    private static final String KEY_BRANCH_ID = "branch_id";

    private TextView tvDeliveryAddress, tvTotal;
    private RadioGroup paymentMethodGroup;
    private RadioButton rbCashOnDelivery, rbCreditCard;
    private Button btnPlaceOrder;
    private RecyclerView itemRecyclerView;
    private ImageView btnBack;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private Gson gson;

    private double subtotal = 0;
    private double deliveryFee = 200;
    private double total = 0;
    private String branchId;
    private List<CartItem> cartItems = new ArrayList<>();
    private String deliveryAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_checkout);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase and SharedPreferences
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        gson = new Gson();

        initViews();
        setupRecyclerView();
        loadCartDataFromLocalStorage();
        loadDefaultAddress();
        setupClickListeners();
    }

    private void initViews() {
        tvDeliveryAddress = findViewById(R.id.tvDeliveryAddress);
        tvTotal = findViewById(R.id.tvTotal);
        paymentMethodGroup = findViewById(R.id.paymentMethodGroup);
        rbCashOnDelivery = findViewById(R.id.rbCashOnDelivery);
        rbCreditCard = findViewById(R.id.rbCreditCard);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        itemRecyclerView = findViewById(R.id.itemlist);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupRecyclerView() {
        itemRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadCartDataFromLocalStorage() {
        try {
            // Retrieve cart data from SharedPreferences
            String cartDataJson = sharedPreferences.getString(KEY_CART_DATA, "");

            if (cartDataJson.isEmpty()) {
                Toast.makeText(this, "No cart data found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Parse cart items from JSON
            Type cartItemListType = new TypeToken<List<CartItem>>(){}.getType();
            cartItems = gson.fromJson(cartDataJson, cartItemListType);

            if (cartItems == null) {
                cartItems = new ArrayList<>();
            }

            // Retrieve totals and branch ID
            subtotal = sharedPreferences.getFloat(KEY_SUBTOTAL, 0f);
            deliveryFee = sharedPreferences.getFloat(KEY_DELIVERY_FEE, 200f);
            total = sharedPreferences.getFloat(KEY_TOTAL, 0f);
            branchId = sharedPreferences.getString(KEY_BRANCH_ID, "");

            // Display total
            tvTotal.setText(String.format("LKR %d", (int) total));

            // Setup recycler view with cart items
            setupCheckoutItemsRecyclerView();

            Log.d(TAG, "Cart data loaded from local storage. Items: " + cartItems.size());

        } catch (Exception e) {
            Log.e(TAG, "Error loading cart data from local storage", e);
            Toast.makeText(this, "Error loading cart data", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupCheckoutItemsRecyclerView() {
        // Convert CartItem data to lists for the adapter
        List<String> productNames = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        List<Double> prices = new ArrayList<>();

        for (CartItem item : cartItems) {
            productNames.add(item.getProductName());
            quantities.add(item.getQuantity());
            prices.add(item.getTotalPrice());
        }

        CheckoutItemAdapter adapter = new CheckoutItemAdapter(productNames, quantities, prices);
        itemRecyclerView.setAdapter(adapter);
    }

    private void loadDefaultAddress() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to continue", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Show loading state
        tvDeliveryAddress.setText("Loading address...");

        // Query for default address
        db.collection("addresses")
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("isDefault", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Found default address
                        DocumentSnapshot addressDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String addressName = addressDoc.getString("name");
                        String fullAddress = addressDoc.getString("fullAddress");

                        if (fullAddress != null && !fullAddress.trim().isEmpty()) {
                            deliveryAddress = fullAddress;
                            // Display address with name if available
                            if (addressName != null && !addressName.trim().isEmpty()) {
                                tvDeliveryAddress.setText(addressName + "\n" + fullAddress);
                            } else {
                                tvDeliveryAddress.setText(fullAddress);
                            }
                        } else {
                            // Handle case where address data is incomplete
                            tvDeliveryAddress.setText("Address data incomplete. Please update your address.");
                            deliveryAddress = "";
                        }
                    } else {
                        // No default address found, try to get any address
                        loadAnyAvailableAddress(currentUser.getUid());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading default address", e);
                    tvDeliveryAddress.setText("Error loading address. Please try again.");
                    deliveryAddress = "";
                });
    }

    private void loadAnyAvailableAddress(String userId) {
        // If no default address, try to get the first available address
        db.collection("addresses")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot addressDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String addressName = addressDoc.getString("name");
                        String fullAddress = addressDoc.getString("fullAddress");

                        if (fullAddress != null && !fullAddress.trim().isEmpty()) {
                            deliveryAddress = fullAddress;
                            String displayText = "No default address set.\n";
                            if (addressName != null && !addressName.trim().isEmpty()) {
                                displayText += addressName + "\n" + fullAddress;
                            } else {
                                displayText += fullAddress;
                            }
                            tvDeliveryAddress.setText(displayText);
                        } else {
                            showNoAddressMessage();
                        }
                    } else {
                        showNoAddressMessage();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading any available address", e);
                    showNoAddressMessage();
                });
    }

    private void showNoAddressMessage() {
        tvDeliveryAddress.setText("No addresses found. Please add an address to continue.");
        deliveryAddress = "";
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        findViewById(R.id.btnEditAddress).setOnClickListener(v -> {
            // Navigate to AddressActivity
            Intent intent = new Intent(CheckoutActivity.this, AddressActivity.class);
            startActivity(intent);
        });

        btnPlaceOrder.setOnClickListener(v -> placeOrder());

        // Add click listeners for payment method containers
        findViewById(R.id.layoutCashOnDelivery).setOnClickListener(v -> {
            rbCashOnDelivery.setChecked(true);
        });

        findViewById(R.id.layoutCreditCard).setOnClickListener(v -> {
            rbCreditCard.setChecked(true);
        });

        // Set default selection
        rbCashOnDelivery.setChecked(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload address when returning from AddressActivity
        loadDefaultAddress();
    }

    private void placeOrder() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to place order", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cartItems.isEmpty()) {
            Toast.makeText(this, "No items in cart", Toast.LENGTH_SHORT).show();
            return;
        }

        if (deliveryAddress.isEmpty()) {
            Toast.makeText(this, "Please add a delivery address to continue", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected payment method
        String paymentMethod = "cash";
        String paymentStatus = "unpaid";

        if (rbCreditCard.isChecked()) {
            paymentMethod = "card";
            paymentStatus = "paid";
        }

        // Create order data
        String orderId = UUID.randomUUID().toString();
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", orderId);
        orderData.put("userId", currentUser.getUid());
        orderData.put("branchId", branchId);
        orderData.put("subtotal", subtotal);
        orderData.put("deliveryFee", deliveryFee);
        orderData.put("total", total);
        orderData.put("paymentMethod", paymentMethod);
        orderData.put("paymentStatus", paymentStatus);
        orderData.put("status", "pending"); // pending, preparing, out_for_delivery, delivered, cancelled
        orderData.put("deliveryAddress", deliveryAddress);
        orderData.put("createdAt", System.currentTimeMillis());

        // Add order items from cart data
        List<Map<String, Object>> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", cartItem.getProductId());
            item.put("productName", cartItem.getProductName());
            item.put("basePrice", cartItem.getBasePrice());
            item.put("quantity", cartItem.getQuantity());
            item.put("totalPrice", cartItem.getTotalPrice());

            // Convert customization options to string or keep as list
            List<String> optionNames = new ArrayList<>();
            if (cartItem.getSelectedOptions() != null) {
                for (CustomizationOption option : cartItem.getSelectedOptions()) {
                    optionNames.add(option.getName() + " (+LKR " + option.getPrice() + ")");
                }
            }
            item.put("options", optionNames);
            item.put("selectedOptions", cartItem.getSelectedOptions());

            orderItems.add(item);
        }
        orderData.put("items", orderItems);

        // Show loading
        btnPlaceOrder.setText("Placing Order...");
        btnPlaceOrder.setEnabled(false);

        // Save order to Firestore
        db.collection("orders")
                .document(orderId)
                .set(orderData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Order placed successfully: " + orderId);

                        // Clear cart after successful order
                        clearUserCart(currentUser.getUid());

                        // Clear cart data from local storage
                        clearCartDataFromLocalStorage();

                        // Navigate to order confirmation
                        navigateToOrderConfirmation(orderId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error placing order", e);
                        Toast.makeText(CheckoutActivity.this, "Failed to place order. Please try again.", Toast.LENGTH_SHORT).show();
                        btnPlaceOrder.setText("Place Order");
                        btnPlaceOrder.setEnabled(true);
                    }
                });
    }

    private void clearUserCart(String userId) {
        db.collection("cartItems")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        db.collection("cartItems").document(document.getId()).delete();
                    }
                    Log.d(TAG, "Cart cleared from Firestore after order");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error clearing cart from Firestore", e);
                });
    }

    private void clearCartDataFromLocalStorage() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(KEY_CART_DATA);
            editor.remove(KEY_SUBTOTAL);
            editor.remove(KEY_DELIVERY_FEE);
            editor.remove(KEY_TOTAL);
            editor.remove(KEY_BRANCH_ID);
            editor.apply();
            Log.d(TAG, "Cart data cleared from local storage after order");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing cart data from local storage", e);
        }
    }

    private void navigateToOrderConfirmation(String orderId) {
        //Intent intent = new Intent(this, OrderConfirmationActivity.class);
        //intent.putExtra("orderId", orderId);
        //intent.putExtra("total", total);
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        //startActivity(intent);
        //finish();
    }
}