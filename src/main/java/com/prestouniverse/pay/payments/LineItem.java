package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.exception.PrestoPayConfigException;
import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;

public final class LineItem {

    private final String itemDesc;
    private final int quantity;
    private final int unitAmount;
    private final int totalAmount;
    private final String imageUrl;
    private final String itemUrl;
    private final String category;
    private final String categoryDesc;
    private final String supplier;
    private final String supplierDesc;
    private final String supplierUrl;

    private LineItem(Builder builder) {
        this.itemDesc = builder.itemDesc;
        this.quantity = builder.quantity;
        this.unitAmount = builder.unitAmount;
        this.totalAmount = builder.totalAmount;
        this.imageUrl = builder.imageUrl;
        this.itemUrl = builder.itemUrl;
        this.category = builder.category;
        this.categoryDesc = builder.categoryDesc;
        this.supplier = builder.supplier;
        this.supplierDesc = builder.supplierDesc;
        this.supplierUrl = builder.supplierUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    JsonObject toJson() {
        JsonObject node = JsonCodec.newObject();
        node.put("itemDesc", itemDesc);
        node.put("quantity", quantity);
        node.put("unitAmount", unitAmount);
        node.put("totalAmount", totalAmount);
        JsonCodec.putIfPresent(node, "imageUrl", imageUrl);
        JsonCodec.putIfPresent(node, "itemUrl", itemUrl);
        JsonCodec.putIfPresent(node, "category", category);
        JsonCodec.putIfPresent(node, "categoryDesc", categoryDesc);
        JsonCodec.putIfPresent(node, "supplier", supplier);
        JsonCodec.putIfPresent(node, "supplierDesc", supplierDesc);
        JsonCodec.putIfPresent(node, "supplierUrl", supplierUrl);
        return node;
    }

    public static final class Builder {

        private String itemDesc;
        private int quantity;
        private int unitAmount;
        private int totalAmount;
        private String imageUrl;
        private String itemUrl;
        private String category;
        private String categoryDesc;
        private String supplier;
        private String supplierDesc;
        private String supplierUrl;

        private Builder() {
        }

        public Builder itemDesc(String itemDesc) {
            this.itemDesc = itemDesc;
            return this;
        }

        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder unitAmount(int unitAmount) {
            this.unitAmount = unitAmount;
            return this;
        }

        public Builder totalAmount(int totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder itemUrl(String itemUrl) {
            this.itemUrl = itemUrl;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder categoryDesc(String categoryDesc) {
            this.categoryDesc = categoryDesc;
            return this;
        }

        public Builder supplier(String supplier) {
            this.supplier = supplier;
            return this;
        }

        public Builder supplierDesc(String supplierDesc) {
            this.supplierDesc = supplierDesc;
            return this;
        }

        public Builder supplierUrl(String supplierUrl) {
            this.supplierUrl = supplierUrl;
            return this;
        }

        public LineItem build() {
            if (itemDesc == null) {
                throw new PrestoPayConfigException("itemDesc", "itemDesc is required");
            }
            if (quantity <= 0) {
                throw new PrestoPayConfigException("quantity", "quantity must be greater than 0");
            }
            return new LineItem(this);
        }
    }
}
