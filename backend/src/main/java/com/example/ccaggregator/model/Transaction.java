package com.example.ccaggregator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Transaction {
    // Default constructor for Jackson
    public Transaction() {
        this.txTimestamp = System.currentTimeMillis();

    }

    private String txNumber;
    private String ccy;
    private double amount;
    private String txDate;
    private String ccType;
    private String merchantId;
    private long txTimestamp;
    private String ccNumber;
    private String merchantType;
    private TxLocation txLocation;

    // Constants for merchant types
    public static final String ONLINE = "ONLINE";
    public static final String GAMBLING = "GAMBLING";
    public static final String PHARMACY = "PHARMACY";
    public static final String GAS_STATION = "GAS_STATION";
    public static final String RETAIL = "RETAIL";
    public static final String HOSPITALITY = "HOSPITALITY";
    public static final String RESTAURANT = "RESTAURANT";
    public static final String AUTO = "AUTO";
    public static final String RECREATIONAL = "RECREATIONAL";
    
    public static final String[] MERCHANT_TYPES = {
        ONLINE, GAMBLING, PHARMACY, GAS_STATION, RETAIL, HOSPITALITY, RESTAURANT, AUTO, RECREATIONAL
    };

    public static class TxLocation {
        private double longitude;
        private double latitude;

        public TxLocation() {}

        public TxLocation(double longitude, double latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
        }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
    }

    public String getCcNumber() { return ccNumber; }
    public void setCcNumber(String ccNumber) { this.ccNumber = ccNumber; }
    public String getMerchantType() { return merchantType; }
    public void setMerchantType(String merchantType) { this.merchantType = merchantType; }
    public TxLocation getTxLocation() { return txLocation; }
    public void setTxLocation(TxLocation txLocation) { this.txLocation = txLocation; }

    // Getters and Setters
    public String getTxNumber() {
        return txNumber;
    }

    public void setTxNumber(String txNumber) {
        this.txNumber = txNumber;
    }

    public String getCcy() {
        return ccy;
    }

    public void setCcy(String ccy) {
        this.ccy = ccy;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getTxDate() {
        return txDate;
    }

    public void setTxDate(String txDate) {
        this.txDate = txDate;
    }

    public String getCcType() {
        return ccType;
    }

    public void setCcType(String ccType) {
        this.ccType = ccType;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public long getTxTimestamp() {
        return txTimestamp;
    }

    public void setTxTimestamp(long txTimestamp) {
        this.txTimestamp = txTimestamp;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "txNumber='" + txNumber + '\'' +
                ", ccy='" + ccy + '\'' +
                ", amount=" + amount +
                ", txDate=" + txDate +
                ", ccType='" + ccType + '\'' +
                ", merchantId='" + merchantId + '\'' +
                ", txTimestamp=" + txTimestamp +
                ", ccNumber='" + (ccNumber != null ? "**** **** **** " + ccNumber.substring(Math.max(0, ccNumber.length() - 4)) : "null") + '\'' +
                ", merchantType=" + merchantType +
                ", txLocation=" + (txLocation != null ? "[" + txLocation.getLatitude() + ", " + txLocation.getLongitude() + "]" : "null") +
                '}';
    }
}