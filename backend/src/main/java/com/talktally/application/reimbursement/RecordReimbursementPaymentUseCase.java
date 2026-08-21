package com.talktally.application.reimbursement;

import com.talktally.application.person.exception.PersonNotFoundException;
import com.talktally.application.reimbursement.exception.InvalidReimbursementInputException;
import com.talktally.application.reimbursement.exception.ReimbursementClaimNotFoundException;
import com.talktally.application.reimbursement.input.RecordReimbursementPaymentInput;
import com.talktally.application.reimbursement.output.RecordReimbursementPaymentOutput;
import com.talktally.domain.CategoryCatalog;
import com.talktally.domain.CategoryMetadata;
import com.talktally.domain.FinancialTransaction;
import com.talktally.domain.FinancialTransactionRepository;
import com.talktally.domain.Money;
import com.talktally.domain.Person;
import com.talktally.domain.PersonRepository;
import com.talktally.domain.ReimbursementClaim;
import com.talktally.domain.ReimbursementClaimId;
import com.talktally.domain.ReimbursementClaimRepository;
import com.talktally.domain.ReimbursementPayment;
import com.talktally.domain.ReimbursementPaymentId;
import com.talktally.domain.TransactionKind;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class RecordReimbursementPaymentUseCase {

	private static final String REIMBURSEMENT_CATEGORY_CODE = "REIMBURSEMENT";

	private final ReimbursementClaimRepository claimRepository;
	private final PersonRepository personRepository;
	private final FinancialTransactionRepository transactionRepository;
	private final CategoryCatalog categoryCatalog;
	private final ReimbursementClaimOutputAssembler outputAssembler;

	public RecordReimbursementPaymentUseCase(
			ReimbursementClaimRepository claimRepository,
			PersonRepository personRepository,
			FinancialTransactionRepository transactionRepository,
			CategoryCatalog categoryCatalog,
			ReimbursementClaimOutputAssembler outputAssembler) {
		this.claimRepository = Objects.requireNonNull(
				claimRepository, "claim repository must not be null");
		this.personRepository = Objects.requireNonNull(
				personRepository, "person repository must not be null");
		this.transactionRepository = Objects.requireNonNull(
				transactionRepository, "transaction repository must not be null");
		this.categoryCatalog = Objects.requireNonNull(
				categoryCatalog, "category catalog must not be null");
		this.outputAssembler = Objects.requireNonNull(
				outputAssembler, "output assembler must not be null");
	}

	@Transactional
	public RecordReimbursementPaymentOutput execute(
			UserId actorId,
			TransactionSource source,
			ReimbursementClaimId claimId,
			RecordReimbursementPaymentInput input) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		Objects.requireNonNull(source, "source must not be null");
		if (claimId == null) {
			throw invalid("claim id is required");
		}
		if (input == null || input.amount() == null || input.receivedDate() == null) {
			throw invalid("payment amount and received date are required");
		}
		ReimbursementClaim claim = claimRepository.findByIdForRepayment(actorId, claimId)
				.orElseThrow(() -> new ReimbursementClaimNotFoundException(claimId));
		Person person = personRepository.findById(actorId, claim.personId())
				.orElseThrow(() -> new PersonNotFoundException(claim.personId()));
		CategoryMetadata category = categoryCatalog.findBuiltInByCode(REIMBURSEMENT_CATEGORY_CODE)
				.filter(metadata -> metadata.allows(TransactionKind.REIMBURSEMENT_RECEIPT))
				.orElseThrow(() -> invalid("reimbursement category is unavailable"));
		Money amount;
		try {
			amount = Money.brl(input.amount());
			if (!amount.isPositive()) {
				throw new IllegalArgumentException("payment amount must be greater than zero");
			}
		}
		catch (IllegalArgumentException exception) {
			throw new InvalidReimbursementInputException("invalid payment amount", exception);
		}

		FinancialTransaction receipt = FinancialTransaction.createSingleOccurrence(
				actorId,
				TransactionKind.REIMBURSEMENT_RECEIPT,
				"Reimbursement from " + person.displayName(),
				amount,
				category.id(),
				input.receivedDate(),
				source);
		ReimbursementPayment payment;
		ReimbursementClaim updated;
		try {
			payment = new ReimbursementPayment(
					ReimbursementPaymentId.generate(),
					amount,
					input.receivedDate(),
					receipt.id(),
					input.note());
			updated = claim.addPayment(payment);
		}
		catch (IllegalArgumentException exception) {
			throw new InvalidReimbursementInputException(exception.getMessage(), exception);
		}
		transactionRepository.save(receipt);
		ReimbursementClaim saved = claimRepository.save(updated);
		return new RecordReimbursementPaymentOutput(
				payment.id(),
				receipt.id(),
				outputAssembler.assemble(actorId, saved, person));
	}

	private static InvalidReimbursementInputException invalid(String message) {
		return new InvalidReimbursementInputException(message);
	}
}
