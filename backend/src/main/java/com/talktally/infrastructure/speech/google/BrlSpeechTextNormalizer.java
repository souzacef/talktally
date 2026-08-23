package com.talktally.infrastructure.speech.google;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BrlSpeechTextNormalizer {

	private static final String ENGLISH_GROUPED = "\\d{1,3}(?:,\\d{3})+(?:\\.\\d{2})?";
	private static final String PORTUGUESE_GROUPED = "\\d{1,3}(?:\\.\\d{3})+(?:,\\d{2})?";
	private static final String UNGROUPED = "\\d+(?:[.,]\\d{2})?";
	private static final Pattern BRL_AMOUNT = Pattern.compile(
			"(?<![\\p{L}\\p{N}])(?:R\\$\\s*|BRL\\s+)(?<amount>"
					+ ENGLISH_GROUPED + "|" + PORTUGUESE_GROUPED + "|" + UNGROUPED
					+ ")(?!\\d|[.,]\\d)");
	private static final Pattern ENGLISH_GROUPED_AMOUNT = Pattern.compile(ENGLISH_GROUPED);
	private static final Pattern PORTUGUESE_GROUPED_AMOUNT = Pattern.compile(PORTUGUESE_GROUPED);

	private BrlSpeechTextNormalizer() {
	}

	static String normalize(String text) {
		Matcher matcher = BRL_AMOUNT.matcher(text);
		StringBuilder normalized = new StringBuilder(text.length());
		while (matcher.find()) {
			matcher.appendReplacement(normalized, Matcher.quoteReplacement(spokenAmount(matcher.group("amount"))));
		}
		return matcher.appendTail(normalized).toString();
	}

	private static String spokenAmount(String amount) {
		AmountParts parts = parse(amount);
		String wholeValue = canonical(parts.whole());
		String realUnit = isOne(wholeValue) ? "real" : "reais";
		String whole = wholeValue + (parts.portugueseStyle() ? " " : " Brazilian ") + realUnit;
		if (parts.cents() == null) {
			return whole;
		}
		String centsValue = canonical(parts.cents());
		String centavoUnit = isOne(centsValue) ? "centavo" : "centavos";
		return whole + (parts.portugueseStyle() ? " e " : " and ")
				+ centsValue + " " + centavoUnit;
	}

	private static AmountParts parse(String amount) {
		if (ENGLISH_GROUPED_AMOUNT.matcher(amount).matches()) {
			int decimalIndex = amount.lastIndexOf('.');
			return decimalIndex < 0
					? new AmountParts(amount.replace(",", ""), null, false)
					: new AmountParts(
							amount.substring(0, decimalIndex).replace(",", ""),
							amount.substring(decimalIndex + 1),
							false);
		}
		if (PORTUGUESE_GROUPED_AMOUNT.matcher(amount).matches()) {
			int decimalIndex = amount.lastIndexOf(',');
			return decimalIndex < 0
					? new AmountParts(amount.replace(".", ""), null, true)
					: new AmountParts(
							amount.substring(0, decimalIndex).replace(".", ""),
							amount.substring(decimalIndex + 1),
							true);
		}
		int decimalPoint = amount.indexOf('.');
		if (decimalPoint >= 0) {
			return new AmountParts(
					amount.substring(0, decimalPoint), amount.substring(decimalPoint + 1), false);
		}
		int decimalComma = amount.indexOf(',');
		if (decimalComma >= 0) {
			return new AmountParts(
					amount.substring(0, decimalComma), amount.substring(decimalComma + 1), true);
		}
		return new AmountParts(amount, null, false);
	}

	private static boolean isOne(String value) {
		return "1".equals(value);
	}

	private static String canonical(String value) {
		return new BigInteger(value).toString();
	}

	private record AmountParts(String whole, String cents, boolean portugueseStyle) {
	}
}
