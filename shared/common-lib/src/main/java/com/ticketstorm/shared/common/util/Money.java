package com.ticketstorm.shared.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    public static final String DEFAULT_CURRENCY = "PYG";

    private Money() {}

    public static BigDecimal create(BigDecimal amount, String currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        return amount.setScale(getScaleForCurrency(currency), RoundingMode.HALF_UP);
    }

    public static BigDecimal multiply(BigDecimal price, int quantity) {
        return price.multiply(BigDecimal.valueOf(quantity))
                .setScale(getScaleForCurrency(DEFAULT_CURRENCY), RoundingMode.HALF_UP);
    }

    private static int getScaleForCurrency(String currency) {
        return switch (currency) {
            case "PYG" -> 0;
            case "USD", "EUR" -> 2;
            default -> 2;
        };
    }
}
