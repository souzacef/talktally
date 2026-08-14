package com.talktally.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {

	private static final Currency BRL = Currency.getInstance("BRL");

	public Money {
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(currency, "currency must not be null");

		if (amount.signum() < 0) {
			throw new IllegalArgumentException("amount must not be negative");
		}

		int fractionDigits = currency.getDefaultFractionDigits();
		if (fractionDigits < 0) {
			throw new IllegalArgumentException("currency must define a fixed number of fraction digits");
		}
		if (amount.scale() > fractionDigits) {
			throw new IllegalArgumentException(
					"amount has more fraction digits than " + currency.getCurrencyCode() + " allows");
		}

		amount = amount.setScale(fractionDigits, RoundingMode.UNNECESSARY);
	}

	public static Money of(BigDecimal amount, Currency currency) {
		return new Money(amount, currency);
	}

	public static Money brl(BigDecimal amount) {
		return new Money(amount, BRL);
	}

	public static Money zero(Currency currency) {
		return new Money(BigDecimal.ZERO, currency);
	}

	public Money add(Money other) {
		requireSameCurrency(other);
		return new Money(amount.add(other.amount), currency);
	}

	public Money subtract(Money other) {
		requireSameCurrency(other);
		return new Money(amount.subtract(other.amount), currency);
	}

	public boolean isZero() {
		return amount.signum() == 0;
	}

	public boolean isPositive() {
		return amount.signum() > 0;
	}

	private void requireSameCurrency(Money other) {
		Objects.requireNonNull(other, "other money must not be null");
		if (!currency.equals(other.currency)) {
			throw new IllegalArgumentException("money currencies must match");
		}
	}
}
