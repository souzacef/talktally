package com.talktally.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTests {

	private static final Currency BRL = Currency.getInstance("BRL");
	private static final Currency USD = Currency.getInstance("USD");

	@ParameterizedTest
	@ValueSource(strings = {"10", "10.0", "10.00"})
	void normalizesAcceptedBrlAmountsToTwoDecimalPlaces(String input) {
		Money money = Money.brl(new BigDecimal(input));

		assertEquals(new BigDecimal("10.00"), money.amount());
		assertEquals(BRL, money.currency());
	}

	@Test
	void acceptsZeroForCalculations() {
		Money zero = Money.zero(BRL);

		assertEquals(new BigDecimal("0.00"), zero.amount());
		assertTrue(zero.isZero());
		assertFalse(zero.isPositive());
	}

	@Test
	void rejectsNegativeAmounts() {
		assertThrows(IllegalArgumentException.class, () -> Money.brl(new BigDecimal("-0.01")));
	}

	@Test
	void rejectsExcessiveBrlPrecisionWithoutRounding() {
		assertThrows(IllegalArgumentException.class, () -> Money.brl(new BigDecimal("10.001")));
	}

	@Test
	void performsSameCurrencyArithmetic() {
		Money first = Money.brl(new BigDecimal("10.25"));
		Money second = Money.brl(new BigDecimal("2.25"));

		assertEquals(Money.brl(new BigDecimal("12.50")), first.add(second));
		assertEquals(Money.brl(new BigDecimal("8.00")), first.subtract(second));
	}

	@Test
	void rejectsCrossCurrencyArithmetic() {
		Money reais = Money.brl(new BigDecimal("10.00"));
		Money dollars = Money.of(new BigDecimal("10.00"), USD);

		assertThrows(IllegalArgumentException.class, () -> reais.add(dollars));
		assertThrows(IllegalArgumentException.class, () -> reais.subtract(dollars));
	}

	@Test
	void numericEqualityDoesNotDependOnInputScale() {
		assertEquals(Money.brl(new BigDecimal("10")), Money.brl(new BigDecimal("10.00")));
		assertEquals(
				Money.brl(new BigDecimal("10")).hashCode(),
				Money.brl(new BigDecimal("10.00")).hashCode());
	}

	@Test
	void rejectsNullAmountAndCurrency() {
		assertThrows(NullPointerException.class, () -> Money.of(null, BRL));
		assertThrows(NullPointerException.class, () -> Money.of(BigDecimal.ONE, null));
	}
}
