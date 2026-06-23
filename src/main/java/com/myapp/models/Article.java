package com.myapp.models;

public class Article {
    private int id;
    private String code;
    private String designation;
    private double prix;
    private int stock;
    private int tva;

    public Article(int id, String code, String designation, double prix, int stock, int tva) {
        this.id = id;
        this.code = code;
        this.designation = designation;
        this.prix = prix;
        this.stock = stock;
        this.tva = tva;
    }

    public int getId() { return id; }
    public String getCode() { return code; }
    public String getDesignation() { return designation; }
    public double getPrix() { return prix; }
    public int getStock() { return stock; }
    public int getTVA() { return tva; }
    
    @Override
    public String toString() { return designation; }
}