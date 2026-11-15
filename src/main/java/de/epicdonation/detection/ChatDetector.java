package de.epicdonation.detection;

import de.epicdonation.config.DonationConfig;
import de.epicdonation.render.DonationHud;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatDetector {
    private static final Pattern AMOUNT = Pattern.compile(
        "(?i)(?:\\+\\s*)?([0-9][0-9.,\\s]*\\s*[kmbt]?)\\s*(?:[$€£¥]|coins?|geld|credits?)?"
    );
    private static final Pattern[] POSITIVE_CONTEXT = {
        Pattern.compile("(?i)\\bdu hast\\b"),
        Pattern.compile("(?i)\\b(?:erhalten|bekommen|gutgeschrieben)\\b"),
        Pattern.compile("(?i)\\bhat dir\\b"),
        Pattern.compile("(?i)\\bgegeben\\b"),
        Pattern.compile("(?i)\\bpay(?:ment)? received\\b"),
        Pattern.compile("(?i)\\breceived\\b"),
        Pattern.compile("(?i)\\bcoins? added\\b"),
        Pattern.compile("(?i)^\\s*\\+")
    };
    private static final Pattern NEGATIVE_CONTEXT = Pattern.compile(
        "(?i)\\b(?:du hast|ich habe)\\b.{0,50}\\b(?:gesendet|bezahlt|überwiesen|gegeben|versendet|sent|paid|transferred)\\b"
    );
    private static final Map<String, Long> RECENT = new LinkedHashMap<>() {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > 64;
        }
    };
    private ChatDetector() {}
    public static void init() {}
    public static void inspect(String raw) {
        if (raw == null || raw.isBlank()) return;
        String text = raw.replaceAll("§[0-9A-FK-ORa-fk-or]", "").trim();
        String lower = text.toLowerCase(Locale.ROOT);
        if (NEGATIVE_CONTEXT.matcher(text).find()
                || Pattern.compile("(?i)\\bvon dir\\b").matcher(text).find()
                || Pattern.compile("(?i)\\byou sent\\b|\\byou paid\\b").matcher(text).find()) return;
        boolean positive = false;
        for (Pattern p : POSITIVE_CONTEXT) {
            if (p.matcher(text).find()) { positive = true; break; }
        }
        if (!positive) return;
        Matcher matcher = AMOUNT.matcher(text);
        String amountText = null;
        boolean senderFirst = Pattern.compile("(?i)^\\s*[\\p{L}0-9_]{2,16}\\s+hat dir\\b").matcher(text).find();
        if (senderFirst) {
            while (matcher.find()) amountText = matcher.group(1);
        } else if (matcher.find()) {
            amountText = matcher.group(1);
        }
        if (amountText == null) return;
        double amount = parseAmount(amountText);
        if (!Double.isFinite(amount) || amount < DonationConfig.get().minimumAmount) return;
        long now = System.currentTimeMillis();
        String fingerprint = lower.replaceAll("\\s+", " ") + "|" + Math.round(amount);
        synchronized (RECENT) {
            RECENT.entrySet().removeIf(e -> now - e.getValue() > 2500);
            if (RECENT.containsKey(fingerprint)) return;
            RECENT.put(fingerprint, now);
        }
        String sender = extractSender(text);
        DonationHud.showDonation(sender, amount);
    }
    private static String extractSender(String text) {
        Matcher m = Pattern.compile("(?i)(?:von|from)\\s+([\\p{L}0-9_]{2,16})").matcher(text);
        if (m.find()) return m.group(1);
        Matcher m2 = Pattern.compile("(?i)^\\s*([\\p{L}0-9_]{2,16})\\s+hat dir").matcher(text);
        if (m2.find()) return m2.group(1);
        return "Unbekannter Spieler";
    }
    public static double parseAmount(String input) {
        if (input == null) return 0;
        String s = input.toUpperCase(Locale.ROOT).replaceAll("[^0-9.,KMBT]", "").replaceAll("\\s", "");
        if (s.isEmpty()) return 0;
        double multiplier = 1;
        char last = s.charAt(s.length() - 1);
        if (last == 'K' || last == 'M' || last == 'B' || last == 'T') {
            multiplier = switch (last) { case 'K' -> 1e3; case 'M' -> 1e6; case 'B' -> 1e9; default -> 1e12; };
            s = s.substring(0, s.length() - 1);
        }
        if (s.isEmpty()) return 0;
        if (s.contains(".") && s.contains(",")) {
            if (s.lastIndexOf(',') > s.lastIndexOf('.')) s = s.replace(".", "").replace(',', '.');
            else s = s.replace(",", "");
        } else if (s.contains(",") || s.contains(".")) {
            char sep = s.contains(",") ? ',' : '.';
            String[] parts = s.split("\\Q" + sep + "\\E", -1);
            if (parts.length > 2 || (parts.length == 2 && parts[1].length() == 3 && parts[0].length() > 0)) {
                s = s.replace(String.valueOf(sep), "");
            } else {
                s = s.replace(',', '.');
            }
        }
        try { return Double.parseDouble(s) * multiplier; }
        catch (NumberFormatException ignored) { return 0; }
    }
}
