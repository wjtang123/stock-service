package com.demo.stock.entity;

public class Stock {
    private Integer id;
    private String  productId;
    private String  productName;
    private Integer quantity;
    private Integer version;

    public Stock() {}
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getProductId() { return productId; }
    public void setProductId(String p) { this.productId = p; }
    public String getProductName() { return productName; }
    public void setProductName(String p) { this.productName = p; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer q) { this.quantity = q; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer v) { this.version = v; }
    @Override public String toString() {
        return "Stock{productId=" + productId + ", qty=" + quantity + ", v=" + version + "}";
    }
}
