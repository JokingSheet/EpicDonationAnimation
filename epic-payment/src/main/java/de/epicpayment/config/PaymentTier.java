package de.epicpayment.config;

public class PaymentTier {
    public String id;
    public String title;
    public long threshold;
    public int color;
    public int accent;
    public PaymentTier() {}
    public PaymentTier(String id, String title, long threshold, int color, int accent) {
        this.id = id; this.title = title; this.threshold = threshold; this.color = color; this.accent = accent;
    }
}
