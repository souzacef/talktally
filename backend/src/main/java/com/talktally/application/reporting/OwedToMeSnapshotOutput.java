package com.talktally.application.reporting;

import java.math.BigDecimal;

public record OwedToMeSnapshotOutput(
		BigDecimal outstanding,
		long openClaims) {
}
