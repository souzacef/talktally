package com.talktally.infrastructure.ai;

import com.talktally.application.category.CategoryCodeNotFoundException;
import com.talktally.application.category.FindVisibleCategoryByCodeUseCase;
import com.talktally.application.exception.InvalidTransactionInputException;
import com.talktally.application.person.FindPersonByNameUseCase;
import com.talktally.application.person.GetPersonReimbursementSummaryUseCase;
import com.talktally.application.person.ResolveOrCreatePersonUseCase;
import com.talktally.application.person.exception.InvalidPersonInputException;
import com.talktally.application.person.output.PersonOutput;
import com.talktally.application.person.output.PersonReimbursementSummaryOutput;
import com.talktally.application.reimbursement.CreateReimbursableExpenseUseCase;
import com.talktally.application.reimbursement.ListReimbursementsUseCase;
import com.talktally.application.reimbursement.NoOpenReimbursementClaimException;
import com.talktally.application.reimbursement.RecordReimbursementPaymentUseCase;
import com.talktally.application.reimbursement.ResolveOpenReimbursementClaimUseCase;
import com.talktally.application.reimbursement.exception.AmbiguousReimbursementClaimException;
import com.talktally.application.reimbursement.exception.InvalidReimbursementInputException;
import com.talktally.application.reimbursement.exception.ReimbursementClaimNotFoundException;
import com.talktally.application.reimbursement.input.CreateReimbursableExpenseInput;
import com.talktally.application.reimbursement.input.ListReimbursementsInput;
import com.talktally.application.reimbursement.input.RecordReimbursementPaymentInput;
import com.talktally.application.reimbursement.output.CreateReimbursableExpenseOutput;
import com.talktally.application.reimbursement.output.RecordReimbursementPaymentOutput;
import com.talktally.application.reimbursement.output.ReimbursementClaimOutput;
import com.talktally.application.reimbursement.output.ReimbursementPageOutput;
import com.talktally.domain.CategoryId;
import com.talktally.domain.ReimbursementClaimId;
import com.talktally.domain.ReimbursementStatus;
import com.talktally.domain.UserId;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Component
public class ReimbursementAssistantTools {

	private static final String DEFAULT_CATEGORY_CODE = "OTHER";
	private static final int DEFAULT_PAGE_SIZE = 20;

	private final CreateReimbursableExpenseUseCase createExpenseUseCase;
	private final ListReimbursementsUseCase listReimbursementsUseCase;
	private final RecordReimbursementPaymentUseCase recordPaymentUseCase;
	private final ResolveOpenReimbursementClaimUseCase resolveOpenClaimUseCase;
	private final GetPersonReimbursementSummaryUseCase getPersonSummaryUseCase;
	private final ResolveOrCreatePersonUseCase resolveOrCreatePersonUseCase;
	private final FindPersonByNameUseCase findPersonByNameUseCase;
	private final FindVisibleCategoryByCodeUseCase findCategoryByCodeUseCase;
	private final Clock clock;

	public ReimbursementAssistantTools(
			CreateReimbursableExpenseUseCase createExpenseUseCase,
			ListReimbursementsUseCase listReimbursementsUseCase,
			RecordReimbursementPaymentUseCase recordPaymentUseCase,
			ResolveOpenReimbursementClaimUseCase resolveOpenClaimUseCase,
			GetPersonReimbursementSummaryUseCase getPersonSummaryUseCase,
			ResolveOrCreatePersonUseCase resolveOrCreatePersonUseCase,
			FindPersonByNameUseCase findPersonByNameUseCase,
			FindVisibleCategoryByCodeUseCase findCategoryByCodeUseCase,
			Clock clock) {
		this.createExpenseUseCase = Objects.requireNonNull(
				createExpenseUseCase, "create reimbursable expense use case must not be null");
		this.listReimbursementsUseCase = Objects.requireNonNull(
				listReimbursementsUseCase, "list reimbursements use case must not be null");
		this.recordPaymentUseCase = Objects.requireNonNull(
				recordPaymentUseCase, "record reimbursement payment use case must not be null");
		this.resolveOpenClaimUseCase = Objects.requireNonNull(
				resolveOpenClaimUseCase, "resolve open claim use case must not be null");
		this.getPersonSummaryUseCase = Objects.requireNonNull(
				getPersonSummaryUseCase, "person reimbursement summary use case must not be null");
		this.resolveOrCreatePersonUseCase = Objects.requireNonNull(
				resolveOrCreatePersonUseCase, "resolve or create person use case must not be null");
		this.findPersonByNameUseCase = Objects.requireNonNull(
				findPersonByNameUseCase, "find person use case must not be null");
		this.findCategoryByCodeUseCase = Objects.requireNonNull(
				findCategoryByCodeUseCase, "find category use case must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	@Tool(name = "record_reimbursable_expense", description = "Record an expense and an amount owed by a named person. Missing required values must be clarified.")
	public ToolResult recordReimbursableExpense(
			@ToolParam(description = "Expense description", required = false) String description,
			@ToolParam(description = "Positive total expense amount in BRL", required = false) BigDecimal amount,
			@ToolParam(description = "Stable expense category code; defaults to OTHER", required = false) String categoryCode,
			@ToolParam(description = "Expense date; defaults to today", required = false) LocalDate eventDate,
			@ToolParam(description = "Date when the first occurrence affects cash flow. Defaults to eventDate when omitted.", required = false) LocalDate firstOccurrenceDate,
			@ToolParam(description = "Monthly installment count; defaults to 1", required = false) Integer installmentCount,
			@ToolParam(description = "Name of the person who owes the reimbursement", required = false) String personName,
			@ToolParam(description = "Amount owed in BRL; defaults to the full expense amount", required = false) BigDecimal amountOwed,
			@ToolParam(description = "Optional reimbursement note", required = false) String note,
			ToolContext toolContext) {
		UserId actorId = AssistantToolContext.requireActor(toolContext);
		if (description == null || description.isBlank()) {
			return ToolResult.clarification("What expense description should be recorded?");
		}
		if (amount == null) {
			return ToolResult.clarification("How much was the expense?");
		}
		if (personName == null || personName.isBlank()) {
			return ToolResult.clarification("Who owes this reimbursement?");
		}
		try {
			CategoryId categoryId = findCategoryByCodeUseCase.execute(
					actorId,
					categoryCode == null || categoryCode.isBlank()
							? DEFAULT_CATEGORY_CODE
							: categoryCode).id();
			PersonOutput person = resolveOrCreatePersonUseCase.execute(actorId, personName);
			CreateReimbursableExpenseOutput output = createExpenseUseCase.execute(
					actorId,
					AssistantToolContext.requireSource(toolContext),
					new CreateReimbursableExpenseInput(
							description,
							amount,
							categoryId,
							eventDate == null ? LocalDate.now(clock) : eventDate,
							firstOccurrenceDate,
							installmentCount == null ? 1 : installmentCount,
							person.personId(),
							amountOwed,
							note));
			return ToolResult.success(
					"Reimbursable expense recorded successfully.",
					new ReimbursableExpenseData(
							output.claim().claimId().value().toString(),
							output.claim().personDisplayName(),
							output.expense().amount(),
							output.claim().remainingAmount(),
							output.claim().currency(),
							output.expense().firstOccurrenceDate(),
							output.expense().installmentCount()));
		}
		catch (CategoryCodeNotFoundException exception) {
			return ToolResult.rejected("The requested category code is unavailable.");
		}
		catch (InvalidPersonInputException exception) {
			return ToolResult.rejected("The person's name is invalid.");
		}
		catch (InvalidReimbursementInputException | InvalidTransactionInputException exception) {
			return ToolResult.rejected("The reimbursable expense was rejected by financial validation.");
		}
	}

	@Tool(name = "list_reimbursements", description = "List the authenticated user's reimbursement claims with optional person and status filters.")
	public ToolResult listReimbursements(
			@ToolParam(description = "Optional person display name", required = false) String personName,
			@ToolParam(description = "Optional PENDING, PARTIALLY_PAID, or PAID status", required = false) String status,
			@ToolParam(description = "Zero-based page; defaults to 0", required = false) Integer page,
			@ToolParam(description = "Bounded page size; defaults to 20", required = false) Integer size,
			ToolContext toolContext) {
		UserId actorId = AssistantToolContext.requireActor(toolContext);
		PersonOutput person = null;
		if (personName != null && !personName.isBlank()) {
			person = findPersonByNameUseCase.execute(actorId, personName).orElse(null);
			if (person == null) {
				return ToolResult.notFound("No matching person was found.");
			}
		}
		try {
			ReimbursementStatus parsedStatus = status == null || status.isBlank()
					? null
					: ReimbursementStatus.valueOf(status.strip().toUpperCase(Locale.ROOT));
			ReimbursementPageOutput output = listReimbursementsUseCase.execute(
					actorId,
					new ListReimbursementsInput(
							person == null ? null : person.personId(),
							parsedStatus,
							page == null ? 0 : page,
							size == null ? DEFAULT_PAGE_SIZE : size));
			return ToolResult.success(
					"Reimbursements retrieved successfully.",
					new ReimbursementSearchData(
							output.content().stream().map(ReimbursementAssistantTools::summarize).toList(),
							output.page(), output.size(), output.totalElements(), output.totalPages()));
		}
		catch (IllegalArgumentException | InvalidReimbursementInputException exception) {
			return ToolResult.rejected("The reimbursement search filters are invalid.");
		}
	}

	@Tool(name = "get_amount_owed_by_person", description = "Get TalkTally's deterministic reimbursement totals for one named person.")
	public ToolResult getAmountOwedByPerson(
			@ToolParam(description = "Person display name", required = false) String personName,
			ToolContext toolContext) {
		if (personName == null || personName.isBlank()) {
			return ToolResult.clarification("Whose owed amount should be checked?");
		}
		UserId actorId = AssistantToolContext.requireActor(toolContext);
		return findPersonByNameUseCase.execute(actorId, personName)
				.map(person -> {
					PersonReimbursementSummaryOutput output = getPersonSummaryUseCase.execute(
							actorId, person.personId());
					return ToolResult.success(
							"Person reimbursement summary calculated by TalkTally.",
							new AmountOwedData(
									output.displayName(),
									output.totalOriginal(),
									output.totalReimbursed(),
									output.totalOutstanding(),
									output.currency(),
									output.openClaimCount()));
				})
				.orElseGet(() -> ToolResult.notFound("No matching person was found."));
	}

	@Tool(name = "record_reimbursement_payment", description = "Record one reimbursement payment from a named person against exactly one open claim. Never split across claims.")
	public ToolResult recordReimbursementPayment(
			@ToolParam(description = "Name of the person who paid", required = false) String personName,
			@ToolParam(description = "Positive payment amount in BRL", required = false) BigDecimal amount,
			@ToolParam(description = "Payment received date; defaults to today", required = false) LocalDate receivedDate,
			@ToolParam(description = "Optional payment note", required = false) String note,
			@ToolParam(description = "Optional claim UUID supplied only after explicit disambiguation", required = false) String claimId,
			ToolContext toolContext) {
		if (personName == null || personName.isBlank()) {
			return ToolResult.clarification("Who made the reimbursement payment?");
		}
		if (amount == null) {
			return ToolResult.clarification("How much was reimbursed?");
		}
		UserId actorId = AssistantToolContext.requireActor(toolContext);
		PersonOutput person = findPersonByNameUseCase.execute(actorId, personName).orElse(null);
		if (person == null) {
			return ToolResult.notFound("No matching person was found.");
		}
		ReimbursementClaimId explicitClaimId;
		try {
			explicitClaimId = claimId == null || claimId.isBlank()
					? null
					: ReimbursementClaimId.from(UUID.fromString(claimId.strip()));
		}
		catch (IllegalArgumentException exception) {
			return ToolResult.rejected("The supplied reimbursement claim selector is invalid.");
		}
		try {
			ReimbursementClaimOutput claim = resolveOpenClaimUseCase.execute(
					actorId, person.personId(), explicitClaimId);
			RecordReimbursementPaymentOutput output = recordPaymentUseCase.execute(
					actorId,
					AssistantToolContext.requireSource(toolContext),
					claim.claimId(),
					new RecordReimbursementPaymentInput(
							amount,
							receivedDate == null ? LocalDate.now(clock) : receivedDate,
							note));
			return ToolResult.success(
					"Reimbursement payment recorded successfully.",
					new ReimbursementPaymentData(
							output.claim().claimId().value().toString(),
							output.claim().personDisplayName(),
							amount,
							output.claim().remainingAmount(),
							output.claim().currency(),
							output.claim().status()));
		}
		catch (NoOpenReimbursementClaimException | ReimbursementClaimNotFoundException exception) {
			return ToolResult.notFound("No matching open reimbursement claim was found.");
		}
		catch (AmbiguousReimbursementClaimException exception) {
			List<ClaimCandidate> candidates = exception.candidates().stream()
					.map(claim -> new ClaimCandidate(
							claim.claimId().value().toString(),
							claim.originalAmount(),
							claim.remainingAmount(),
							claim.currency(),
							claim.status(),
							claim.note()))
					.toList();
			return ToolResult.clarification(
					"Multiple open claims match. Ask the user which claim should receive this payment.",
					candidates);
		}
		catch (InvalidReimbursementInputException exception) {
			return ToolResult.rejected("The reimbursement payment was rejected by financial validation.");
		}
	}

	private static ReimbursementSummary summarize(ReimbursementClaimOutput claim) {
		return new ReimbursementSummary(
				claim.claimId().value().toString(),
				claim.personDisplayName(),
				claim.originalAmount(),
				claim.amountReimbursed(),
				claim.remainingAmount(),
				claim.currency(),
				claim.status(),
				claim.note());
	}

	public record ReimbursableExpenseData(
			String claimId,
			String personName,
			BigDecimal expenseAmount,
			BigDecimal amountOwed,
			String currency,
			LocalDate firstOccurrenceDate,
			int installmentCount) {
	}

	public record ReimbursementSummary(
			String claimId,
			String personName,
			BigDecimal originalAmount,
			BigDecimal amountReimbursed,
			BigDecimal remainingAmount,
			String currency,
			ReimbursementStatus status,
			String note) {
	}

	public record ReimbursementSearchData(
			List<ReimbursementSummary> reimbursements,
			int page,
			int size,
			long totalElements,
			int totalPages) {
	}

	public record ClaimCandidate(
			String claimId,
			BigDecimal originalAmount,
			BigDecimal remainingAmount,
			String currency,
			ReimbursementStatus status,
			String note) {
	}

	public record ReimbursementPaymentData(
			String claimId,
			String personName,
			BigDecimal paymentAmount,
			BigDecimal remainingAmount,
			String currency,
			ReimbursementStatus status) {
	}

	public record AmountOwedData(
			String personName,
			BigDecimal totalOriginal,
			BigDecimal totalReimbursed,
			BigDecimal totalOutstanding,
			String currency,
			long openClaimCount) {
	}
}
