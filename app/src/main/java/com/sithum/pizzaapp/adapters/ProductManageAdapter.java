package com.sithum.pizzaapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sithum.pizzaapp.R;
import com.sithum.pizzaapp.models.Product;

import java.util.List;

public class ProductManageAdapter extends RecyclerView.Adapter<ProductManageAdapter.ProductViewHolder> {

    private List<Product> productList;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onProductEditClick(Product product);
        void onProductDeleteClick(Product product);
    }

    public ProductManageAdapter(List<Product> productList, OnProductClickListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_manage, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product, listener);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        private TextView productName, productDescription, productPrice, productCategory, productStatus;
        private ImageView editButton, deleteButton;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            productDescription = itemView.findViewById(R.id.productDescription);
            productPrice = itemView.findViewById(R.id.productPrice);
            productCategory = itemView.findViewById(R.id.productCategory);
            productStatus = itemView.findViewById(R.id.productStatus);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        public void bind(Product product, OnProductClickListener listener) {
            productName.setText(product.getName());
            productDescription.setText(product.getDescription());
            productPrice.setText("LKR " + product.getPrice());
            productCategory.setText(product.getCategory());

            // Set status
            if ("available".equals(product.getStatus())) {
                productStatus.setText("Available");
                productStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.green));
            } else {
                productStatus.setText("Unavailable");
                productStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.red));
            }

            // Set click listeners
            itemView.setOnClickListener(v -> listener.onProductClick(product));
            editButton.setOnClickListener(v -> listener.onProductEditClick(product));
            deleteButton.setOnClickListener(v -> listener.onProductDeleteClick(product));
        }
    }
}