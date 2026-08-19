package com.talktally.application.transaction;

import com.talktally.application.output.TransactionOccurrenceOutput;
import com.talktally.application.output.TransactionOutput;
import com.talktally.domain.FinancialTransaction;

final class TransactionOutputMapper {

	private TransactionOutputMapper() {
	}

	static TransactionOutput toOutput(FinancialTransaction transaction) {
		return new TransactionOutput(
				transaction.id(),
				transaction.kind(),
				transaction.description(),
				transaction.totalAmount().amount(),
				transaction.totalAmount().currency().getCurrencyCode(),
				transaction.categoryId(),
				transaction.eventDate(),
				transaction.firstOccurrenceDate(),
				transaction.source(),
				transaction.occurrences().size(),
				transaction.occurrences().stream()
						.map(occurrence -> new TransactionOccurrenceOutput(
								occurrence.sequenceNumber(),
								occurrence.effectiveDate(),
								occurrence.amount().amount(),
								occurrence.amount().currency().getCurrencyCode()))
						.toList());
	}
}
