package com.sithum.pizzaapp.activities.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.sithum.pizzaapp.R;
import com.sithum.pizzaapp.adapters.ProductManageAdapter;
import com.sithum.pizzaapp.models.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductManageActivity extends AppCompatActivity implements ProductManageAdapter.OnProductClickListener {

    // Firebase instance
    private FirebaseFirestore db;

    // UI Components
    private ImageView backButton;
    private Button addProductButton;
    private TextView tabAll, tabClassic, tabSpicy, tabDesserts;

    // Layout containers
    private FrameLayout deleteDialogOverlay, productDetailOverlay;

    // Product list
    private RecyclerView productsRecyclerView;
    private ProductManageAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();
    private List<Product> filteredProductList = new ArrayList<>();

    // Current selected product and filter
    private Product selectedProduct;
    private String currentFilter = "all";

    // Detail view components
    private TextView productDetailTitle, productDetailDescription, productDetailPrice;
    private TextView productDetailCategory, productDetailStatus;
    private ImageView closeDetailButton, deleteProductButton;
    private Button cancelButton, markUnavailableButton, editProductButton;
    private EditText newOptionNameEditText, newOptionPriceEditText;
    private Button addCustomizationButton;

    // Delete dialog components
    private Button cancelDeleteButton, confirmDeleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_manage);

        // Initialize Firebase instance
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        initUI();

        // Setup RecyclerView
        setupRecyclerView();

        // Load products from Firestore
        loadProducts();

        // Setup listeners
        setupListeners();
    }

    private void initUI() {
        backButton = findViewById(R.id.backButton);
        addProductButton = findViewById(R.id.addProductButton);

        // Category tabs
        tabAll = findViewById(R.id.tabAll);
        tabClassic = findViewById(R.id.tabClassic);
        tabSpicy = findViewById(R.id.tabSpicy);
        tabDesserts = findViewById(R.id.tabDesserts);

        // Overlay containers
        deleteDialogOverlay = findViewById(R.id.deleteDialogOverlay);
        productDetailOverlay = findViewById(R.id.productDetailOverlay);

        // Detail view components
        productDetailTitle = findViewById(R.id.productDetailTitle);
        productDetailDescription = findViewById(R.id.productDetailDescription);
        productDetailPrice = findViewById(R.id.productDetailPrice);
        productDetailCategory = findViewById(R.id.productDetailCategory);
        productDetailStatus = findViewById(R.id.productDetailStatus);
        closeDetailButton = findViewById(R.id.closeDetailButton);
        deleteProductButton = findViewById(R.id.deleteProductButton);
        cancelButton = findViewById(R.id.cancelButton);
        markUnavailableButton = findViewById(R.id.markUnavailableButton);
        editProductButton = findViewById(R.id.editProductButton);
        newOptionNameEditText = findViewById(R.id.newOptionNameEditText);
        newOptionPriceEditText = findViewById(R.id.newOptionPriceEditText);
        addCustomizationButton = findViewById(R.id.addCustomizationButton);

        // Delete dialog components
        cancelDeleteButton = findViewById(R.id.cancelDeleteButton);
        confirmDeleteButton = findViewById(R.id.confirmDeleteButton);
    }

    private void setupRecyclerView() {
        productsRecyclerView = findViewById(R.id.productsRecyclerView);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new ProductManageAdapter(filteredProductList, this);
        productsRecyclerView.setAdapter(productAdapter);
    }

    private void setupListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());

        // Add product button
        addProductButton.setOnClickListener(v -> showAddProductForm());

        // Category tabs
        tabAll.setOnClickListener(v -> filterProducts("all"));
        tabClassic.setOnClickListener(v -> filterProducts("classic"));
        tabSpicy.setOnClickListener(v -> filterProducts("spicy"));
        tabDesserts.setOnClickListener(v -> filterProducts("desserts"));

        // Detail view buttons
        closeDetailButton.setOnClickListener(v -> hideProductDetail());
        cancelButton.setOnClickListener(v -> hideProductDetail());
        markUnavailableButton.setOnClickListener(v -> toggleProductAvailability());
        editProductButton.setOnClickListener(v -> editProduct());
        deleteProductButton.setOnClickListener(v -> showDeleteDialog());

        // Customization buttons
        addCustomizationButton.setOnClickListener(v -> addCustomizationOption());

        // Delete dialog buttons
        cancelDeleteButton.setOnClickListener(v -> hideDeleteDialog());
        confirmDeleteButton.setOnClickListener(v -> deleteProduct());
    }

    private void loadProducts() {
        db.collection("products")
                .orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Toast.makeText(ProductManageActivity.this, "Error loading products: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        productList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Product product = doc.toObject(Product.class);
                            product.setId(doc.getId());
                            productList.add(product);
                        }

                        filterProducts(currentFilter);
                    }
                });
    }

    private void filterProducts(String filter) {
        currentFilter = filter;
        filteredProductList.clear();

        for (Product product : productList) {
            if (filter.equals("all")) {
                filteredProductList.add(product);
            } else if (filter.equals(product.getCategory())) {
                filteredProductList.add(product);
            }
        }

        productAdapter.notifyDataSetChanged();
        updateTabStyles(filter);
    }

    private void updateTabStyles(String selectedTab) {
        int selectedColor = getResources().getColor(R.color.red);
        int normalColor = getResources().getColor(android.R.color.darker_gray);

        tabAll.setTextColor(selectedTab.equals("all") ? selectedColor : normalColor);
        tabClassic.setTextColor(selectedTab.equals("classic") ? selectedColor : normalColor);
        tabSpicy.setTextColor(selectedTab.equals("spicy") ? selectedColor : normalColor);
        tabDesserts.setTextColor(selectedTab.equals("desserts") ? selectedColor : normalColor);

        tabAll.setBackgroundResource(selectedTab.equals("all") ? R.drawable.chip_bg : android.R.color.transparent);
        tabClassic.setBackgroundResource(selectedTab.equals("classic") ? R.drawable.chip_bg : android.R.color.transparent);
        tabSpicy.setBackgroundResource(selectedTab.equals("spicy") ? R.drawable.chip_bg : android.R.color.transparent);
        tabDesserts.setBackgroundResource(selectedTab.equals("desserts") ? R.drawable.chip_bg : android.R.color.transparent);
    }

    private void showProductDetail(Product product) {
        selectedProduct = product;

        // Fill detail view with product data
        productDetailTitle.setText(product.getName());
        productDetailDescription.setText(product.getDescription());
        productDetailPrice.setText("LKR " + product.getPrice());
        productDetailCategory.setText(product.getCategory());

        // Set status
        if ("available".equals(product.getStatus())) {
            productDetailStatus.setText("Available");
            productDetailStatus.setTextColor(getResources().getColor(R.color.green));
            markUnavailableButton.setText("Mark Unavailable");
        } else {
            productDetailStatus.setText("Unavailable");
            productDetailStatus.setTextColor(getResources().getColor(R.color.red));
            markUnavailableButton.setText("Mark Available");
        }

        productDetailOverlay.setVisibility(View.VISIBLE);
    }

    private void hideProductDetail() {
        productDetailOverlay.setVisibility(View.GONE);
        selectedProduct = null;
        clearCustomizationFields();
    }

    private void showDeleteDialog() {
        deleteDialogOverlay.setVisibility(View.VISIBLE);
    }

    private void hideDeleteDialog() {
        deleteDialogOverlay.setVisibility(View.GONE);
    }

    private void toggleProductAvailability() {
        if (selectedProduct == null) return;

        String newStatus = "available".equals(selectedProduct.getStatus()) ? "unavailable" : "available";
        String statusMessage = "available".equals(newStatus) ? "available" : "unavailable";

        db.collection("products").document(selectedProduct.getId())
                .update("status", newStatus, "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Product marked as " + statusMessage, Toast.LENGTH_SHORT).show();
                    hideProductDetail();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating product status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void editProduct() {
        // Navigate to edit product activity or show edit form
        Toast.makeText(this, "Edit product: " + selectedProduct.getName(), Toast.LENGTH_SHORT).show();
        // You would implement the actual edit functionality here
    }

    private void deleteProduct() {
        if (selectedProduct == null) return;

        db.collection("products").document(selectedProduct.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Product deleted successfully", Toast.LENGTH_SHORT).show();
                    hideDeleteDialog();
                    hideProductDetail();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    hideDeleteDialog();
                });
    }

    private void addCustomizationOption() {
        String optionName = newOptionNameEditText.getText().toString().trim();
        String optionPrice = newOptionPriceEditText.getText().toString().trim();

        if (TextUtils.isEmpty(optionName)) {
            newOptionNameEditText.setError("Option name is required");
            return;
        }

        if (TextUtils.isEmpty(optionPrice)) {
            newOptionPriceEditText.setError("Price is required");
            return;
        }

        // Add customization option to the product
        if (selectedProduct != null) {
            // Get existing customizations or create new list
            List<Map<String, Object>> customizations = selectedProduct.getCustomizations();
            if (customizations == null) {
                customizations = new ArrayList<>();
            }

            // Add new customization
            Map<String, Object> customization = new HashMap<>();
            customization.put("name", optionName);
            customization.put("price", Double.parseDouble(optionPrice));
            customizations.add(customization);

            // Update product in Firestore
            db.collection("products").document(selectedProduct.getId())
                    .update("customizations", customizations, "updatedAt", System.currentTimeMillis())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Customization added successfully", Toast.LENGTH_SHORT).show();
                        clearCustomizationFields();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error adding customization: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void clearCustomizationFields() {
        newOptionNameEditText.setText("");
        newOptionPriceEditText.setText("");
    }

    private void showAddProductForm() {
        // Navigate to add product activity
        Toast.makeText(this, "Add new product", Toast.LENGTH_SHORT).show();
        // You would implement the actual add product functionality here
    }

    @Override
    public void onProductClick(Product product) {
        showProductDetail(product);
    }

    @Override
    public void onProductEditClick(Product product) {
        selectedProduct = product;
        editProduct();
    }

    @Override
    public void onProductDeleteClick(Product product) {
        selectedProduct = product;
        showDeleteDialog();
    }
}