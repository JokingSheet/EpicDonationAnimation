package de.epicpayment.detect;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PaymentParser {
    public record Payment(long amount, String player) {}
    private static final String NUM = "([0-9][0-9.,]*(?:\\s*[kKmMbBtT])?)";
    private static final Pattern RECEIVED = Pattern.compile("(?iu)\\bdu\\s+hast\\s+" + NUM + "\\s*(?:[$€£¥]|coins?|geld)?\\s*(?:von\\s+([\\p{L}0-9_]{2,16})\\s*)?(?:erhalten|bekommen|empfangen)\\b");
    private static final Pattern GIVEN_TO_YOU = Pattern.compile("(?iu)\\b([\\p{L}0-9_]{2,16})\\s+hat\\s+dir\\s+" + NUM + "\\s*(?:[$€£¥]|coins?|geld)?\\s+(?:gegeben|gesendet|überwiesen|gezahlt)\\b");
    private static final Pattern PLUS = Pattern.compile("(?iu)(?:^|\\s)\\+\\s*" + NUM + "\\s*(?:[$€£¥]|coins?|geld)\\b");
    private static final Pattern OUTGOING = Pattern.compile("(?iu)\\b(du hast|you (?:sent|paid)|gesendet an|überwiesen an|bezahlt an)\\b");
    private PaymentParser() {}

    public static Optional<Payment> parse(String raw) { return parse(raw, List.of()); }

    public static Optional<Payment> parse(String raw, List<String> customPatterns) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String text = raw.replaceAll("§[0-9A-FK-ORa-fk-or]", "").trim();
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.matches(".*\\b(du hast|you)\\b.*\\b(an|to)\\b.*\\b(gesendet|überwiesen|bezahlt|sent|paid)\\b.*")) return Optional.empty();
        if (customPatterns != null) {
            for (String expression : customPatterns) {
                if (expression == null || expression.length() > 240) continue;
                try {
                    Matcher custom = Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text);
                    if (custom.find() && custom.groupCount() >= 1) {
                        String sender = custom.groupCount() >= 2 ? custom.group(2) : "";
                        Optional<Payment> result = payment(custom.group(1), sender);
                        if (result.isPresent()) return result;
                    }
                } catch (RuntimeException ignored) { /* Ignore invalid user regexes. */ }
            }
        }
        Matcher m = RECEIVED.matcher(text);
        if (m.find()) return payment(m.group(1), m.group(2));
        m = GIVEN_TO_YOU.matcher(text);
        if (m.find()) return payment(m.group(2), m.group(1));
        m = PLUS.matcher(text);
        if (m.find() && !OUTGOING.matcher(text).find() && !Pattern.compile("(?iu)\\b(?:an|to)\\s+[\\p{L}0-9_]{2,16}\\b").matcher(text).find()) return payment(m.group(1), "");
        return Optional.empty();
    }
    private static Optional<Payment> payment(String number, String player) {
        try {
            String n = number.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
            BigDecimal multiplier = BigDecimal.ONE;
            if (n.matches(".*[KMBT]$")) {
                char suffix = n.charAt(n.length() - 1); n = n.substring(0, n.length() - 1);
                multiplier = switch (suffix) { case 'K' -> BigDecimal.valueOf(1_000); case 'M' -> BigDecimal.valueOf(1_000_000); case 'B' -> BigDecimal.valueOf(1_000_000_000L); case 'T' -> BigDecimal.valueOf(1_000_000_000_000L); default -> BigDecimal.ONE; };
            }
            if (n.contains(".") && n.contains(",")) {
                if (n.lastIndexOf(',') > n.lastIndexOf('.')) n = n.replace(".", "").replace(',', '.');
                else n = n.replace(",", "");
            } else if (n.contains(",") || n.contains(".")) {
                char sep = n.contains(",") ? ',' : '.';
                String[] parts = n.split(Pattern.quote(String.valueOf(sep)), -1);
                if (parts.length > 1 && parts[parts.length - 1].length() == 3) n = n.replace(String.valueOf(sep), "");
                else n = n.replace(sep, '.');
            }
            long amount = new BigDecimal(n).multiply(multiplier).setScale(0, RoundingMode.HALF_UP).longValueExact();
            if (amount <= 0) return Optional.empty();
            return Optional.of(new Payment(amount, player == null ? "" : player));
        } catch (Exception ignored) { return Optional.empty(); }
    }
}
