package com.talktally.application.transaction;

import com.talktally.application.exception.CategoryIncompatibleException;
import com.talktally.application.exception.CategoryUnavailableException;
import com.talktally.application.exception.InvalidTransactionInputException;
import com.talktally.application.exception.TransactionNotFoundException;
import com.talktally.application.input.CreateTransactionInput;
import com.talktally.application.input.ListTransactionsInput;
import com.talktally.application.input.UpdateTransactionInput;
import com.talktally.application.output.TransactionOutput;
import com.talktally.application.output.TransactionPageOutput;
import com.talktally.domain.CategoryCatalog;
import com.talktally.domain.CategoryId;
import com.talktally.domain.CategoryMetadata;
import com.talktally.domain.FinancialTransaction;
import com.talktally.domain.FinancialTransactionPage;
import com.talktally.domain.FinancialTransactionRepository;
import com.talktally.domain.FinancialTransactionSearchCriteria;
import com.talktally.domain.TransactionId;
import com.talktally.domain.TransactionKind;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionUseCasesTests {

	private static final UserId USER_A = user("10000000-0000-0000-0000-000000000001");
	private static final UserId USER_B = user("10000000-0000-0000-0000-000000000002");
	private static final CategoryId INCOME_CATEGORY = category("20000000-0000-0000-0000-000000000001");
	private static final CategoryId EXPENSE_CATEGORY = category("20000000-0000-0000-0000-000000000002");
	private static final CategoryId SECOND_EXPENSE_CATEGORY = category("20000000-0000-0000-0000-000000000003");
	private static final CategoryId ANY_CATEGORY = category("20000000-0000-0000-0000-000000000004");
	private static final CategoryId USER_A_CATEGORY = category("20000000-0000-0000-0000-000000000005");
	private static final LocalDate EVENT_DATE = LocalDate.of(2026, 8, 14);

	private InMemoryFinancialTransactionRepository repository;
	private InMemoryCategoryCatalog categoryCatalog;
	private CreateTransactionUseCase createUseCase;
	private GetTransactionUseCase getUseCase;
	private ListTransactionsUseCase listUseCase;
	private UpdateTransactionUseCase updateUseCase;
	private DeleteTransactionUseCase deleteUseCase;

	@BeforeEach
	void setUp() {
		repository = new InMemoryFinancialTransactionRepository();
		categoryCatalog = new InMemoryCategoryCatalog();
		categoryCatalog.addBuiltIn(INCOME_CATEGORY, EnumSet.of(TransactionKind.INCOME));
		categoryCatalog.addBuiltIn(EXPENSE_CATEGORY, EnumSet.of(TransactionKind.EXPENSE));
		categoryCatalog.addBuiltIn(SECOND_EXPENSE_CATEGORY, EnumSet.of(TransactionKind.EXPENSE));
		categoryCatalog.addBuiltIn(ANY_CATEGORY, EnumSet.allOf(TransactionKind.class));
		categoryCatalog.addCustom(USER_A_CATEGORY, USER_A, EnumSet.of(TransactionKind.EXPENSE));
		createUseCase = new CreateTransactionUseCase(repository, categoryCatalog);
		getUseCase = new GetTransactionUseCase(repository);
		listUseCase = new ListTransactionsUseCase(repository, categoryCatalog);
		updateUseCase = new UpdateTransactionUseCase(repository, categoryCatalog);
		deleteUseCase = new DeleteTransactionUseCase(repository);
	}

	@Test
	void createsSingleExpenseInBrl() {
		TransactionOutput output = create(
				USER_A, TransactionKind.EXPENSE, "  Groceries  ", "87.45",
				EXPENSE_CATEGORY, EVENT_DATE, 1, TransactionSource.MANUAL);

		assertAll(
				() -> assertEquals(TransactionKind.EXPENSE, output.kind()),
				() -> assertEquals("Groceries", output.description()),
				() -> assertEquals(new BigDecimal("87.45"), output.amount()),
				() -> assertEquals("BRL", output.currency()),
				() -> assertEquals(1, output.installmentCount()),
				() -> assertEquals(EVENT_DATE, output.occurrences().getFirst().effectiveDate()),
				() -> assertEquals(new BigDecimal("87.45"), output.occurrences().getFirst().amount()),
				() -> assertThrows(
						UnsupportedOperationException.class,
						() -> output.occurrences().clear()));
	}

	@Test
	void createsIncomeWithCompatibleCategory() {
		TransactionOutput output = create(
				USER_A, TransactionKind.INCOME, "Salary", "5000.00",
				INCOME_CATEGORY, EVENT_DATE, 1, TransactionSource.MANUAL);

		assertEquals(TransactionKind.INCOME, output.kind());
		assertEquals(INCOME_CATEGORY, output.categoryId());
	}

	@Test
	void createsThreeInstallmentsAndAllocatesOneHundredExactly() {
		TransactionOutput output = create(
				USER_A, TransactionKind.EXPENSE, "Purchase", "100.00",
				EXPENSE_CATEGORY, EVENT_DATE, 3, TransactionSource.MANUAL);

		assertEquals(3, output.installmentCount());
		assertEquals(
				List.of(new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("33.34")),
				output.occurrences().stream().map(occurrence -> occurrence.amount()).toList());
		assertEquals(
				List.of(EVENT_DATE, EVENT_DATE.plusMonths(1), EVENT_DATE.plusMonths(2)),
				output.occurrences().stream().map(occurrence -> occurrence.effectiveDate()).toList());
	}

	@Test
	void preservesTrustedCreationSourceOutsideInput() {
		TransactionOutput output = create(
				USER_A, TransactionKind.EXPENSE, "Spoken purchase", "10.00",
				EXPENSE_CATEGORY, EVENT_DATE, 1, TransactionSource.VOICE);

		assertEquals(TransactionSource.VOICE, output.source());
		assertEquals(
				TransactionSource.VOICE,
				repository.findById(USER_A, output.transactionId()).orElseThrow().source());
	}

	@Test
	void rejectsUnavailableAndIncompatibleCategories() {
		assertAll(
				() -> assertThrows(
						CategoryIncompatibleException.class,
						() -> create(USER_A, TransactionKind.EXPENSE, "Wrong category", "10.00",
								INCOME_CATEGORY, EVENT_DATE, 1, TransactionSource.MANUAL)),
				() -> assertThrows(
						CategoryIncompatibleException.class,
						() -> create(USER_A, TransactionKind.INCOME, "Wrong category", "10.00",
								EXPENSE_CATEGORY, EVENT_DATE, 1, TransactionSource.MANUAL)),
				() -> assertThrows(
						CategoryUnavailableException.class,
						() -> create(USER_B, TransactionKind.EXPENSE, "Private category", "10.00",
								USER_A_CATEGORY, EVENT_DATE, 1, TransactionSource.MANUAL)));
	}

	@Test
	void rejectsBlankDescription() {
		assertThrows(
				InvalidTransactionInputException.class,
				() -> create(USER_A, TransactionKind.EXPENSE, "  ", "10.00",
						EXPENSE_CATEGORY, EVENT_DATE, 1, TransactionSource.MANUAL));
	}

	@Test
	void rejectsNonPositiveAndUnsupportedPrecisionAmounts() {
		assertAll(
				() -> assertThrows(
						InvalidTransactionInputException.class,
						() -> create(USER_A, TransactionKind.EXPENSE, "Zero", "0.00",
								EXPENSE_CATEGORY, EVENT_DATE, 1, TransactionSource.MANUAL)),
				() -> assertThrows(
						InvalidTransactionInputException.class,
						() -> create(USER_A, TransactionKind.EXPENSE, "Precision", "1.001",
								EXPENSE_CATEGORY, EVENT_DATE, 1, TransactionSource.MANUAL)));
	}

	@Test
	void rejectsZeroInstallments() {
		assertThrows(
				InvalidTransactionInputException.class,
				() -> create(USER_A, TransactionKind.EXPENSE, "Purchase", "10.00",
						EXPENSE_CATEGORY, EVENT_DATE, 0, TransactionSource.MANUAL));
	}

	@Test
	void rejectsMoreThanOneHundredTwentyInstallments() {
		assertEquals(120, TransactionPolicy.MAX_INSTALLMENTS);
		assertThrows(
				InvalidTransactionInputException.class,
				() -> create(USER_A, TransactionKind.EXPENSE, "Purchase", "200.00",
						EXPENSE_CATEGORY, EVENT_DATE, 121, TransactionSource.MANUAL));
	}

	@Test
	void rejectsMathematicallyImpossibleSchedule() {
		assertThrows(
				InvalidTransactionInputException.class,
				() -> create(USER_A, TransactionKind.EXPENSE, "Tiny split", "0.01",
						EXPENSE_CATEGORY, EVENT_DATE, 2, TransactionSource.MANUAL));
	}

	@Test
	void getsOwnedTransaction() {
		TransactionOutput created = createDefault(USER_A, "Owned");

		assertEquals(created, getUseCase.execute(USER_A, created.transactionId()));
	}

	@Test
	void getTreatsCrossOwnerTransactionAsNotFound() {
		TransactionOutput created = createDefault(USER_A, "Private");

		assertThrows(
				TransactionNotFoundException.class,
				() -> getUseCase.execute(USER_B, created.transactionId()));
	}

	@Test
	void getTreatsNonexistentTransactionAsNotFound() {
		assertThrows(
				TransactionNotFoundException.class,
				() -> getUseCase.execute(USER_A, TransactionId.generate()));
	}

	@Test
	void listIsOwnerIsolated() {
		createDefault(USER_A, "A transaction");
		createDefault(USER_B, "B transaction");

		TransactionPageOutput page = list(USER_A, null, null, null, null, null, 0, 20);

		assertEquals(1, page.totalElements());
		assertEquals("A transaction", page.content().getFirst().description());
	}

	@Test
	void listFiltersByKind() {
		createDefault(USER_A, "Expense");
		create(USER_A, TransactionKind.INCOME, "Income", "50.00",
				INCOME_CATEGORY, EVENT_DATE, 1, TransactionSource.MANUAL);

		TransactionPageOutput page = list(
				USER_A, TransactionKind.INCOME, null, null, null, null, 0, 20);

		assertEquals(1, page.totalElements());
		assertEquals(TransactionKind.INCOME, page.content().getFirst().kind());
	}

	@Test
	void listFiltersByVisibleCategory() {
		createDefault(USER_A, "Groceries");
		create(USER_A, TransactionKind.EXPENSE, "Travel", "50.00",
				SECOND_EXPENSE_CATEGORY, EVENT_DATE, 1, TransactionSource.MANUAL);

		TransactionPageOutput page = list(
				USER_A, null, SECOND_EXPENSE_CATEGORY, null, null, null, 0, 20);

		assertEquals(1, page.totalElements());
		assertEquals(SECOND_EXPENSE_CATEGORY, page.content().getFirst().categoryId());
		assertThrows(
				CategoryUnavailableException.class,
				() -> list(USER_B, null, USER_A_CATEGORY, null, null, null, 0, 20));
	}

	@Test
	void listDateRangeMatchesEffectiveOccurrencesNotOnlyEventDate() {
		TransactionOutput installment = create(
				USER_A, TransactionKind.EXPENSE, "Installments", "90.00",
				EXPENSE_CATEGORY, EVENT_DATE, 3, TransactionSource.MANUAL);
		create(USER_A, TransactionKind.EXPENSE, "Outside", "10.00",
				EXPENSE_CATEGORY, EVENT_DATE.minusMonths(2), 1, TransactionSource.MANUAL);

		TransactionPageOutput page = list(
				USER_A, null, null, EVENT_DATE.plusMonths(1), EVENT_DATE.plusMonths(1), null, 0, 20);

		assertEquals(1, page.totalElements());
		assertEquals(installment.transactionId(), page.content().getFirst().transactionId());
		assertEquals(3, page.content().getFirst().occurrences().size());
	}

	@Test
	void listSearchesDescriptionCaseInsensitively() {
		createDefault(USER_A, "Weekend Groceries");
		createDefault(USER_A, "Electric bill");

		TransactionPageOutput page = list(USER_A, null, null, null, null, "groCER", 0, 20);

		assertEquals(1, page.totalElements());
		assertEquals("Weekend Groceries", page.content().getFirst().description());
	}

	@Test
	void listPaginatesInDeterministicLedgerOrder() {
		TransactionOutput oldest = create(
				USER_A, TransactionKind.EXPENSE, "Oldest", "10.00",
				EXPENSE_CATEGORY, EVENT_DATE.minusDays(2), 1, TransactionSource.MANUAL);
		TransactionOutput middle = create(
				USER_A, TransactionKind.EXPENSE, "Middle", "10.00",
				EXPENSE_CATEGORY, EVENT_DATE.minusDays(1), 1, TransactionSource.MANUAL);
		TransactionOutput newest = createDefault(USER_A, "Newest");

		TransactionPageOutput first = list(USER_A, null, null, null, null, null, 0, 2);
		TransactionPageOutput second = list(USER_A, null, null, null, null, null, 1, 2);

		assertEquals(List.of(newest.transactionId(), middle.transactionId()), ids(first));
		assertEquals(List.of(oldest.transactionId()), ids(second));
		assertEquals(3, first.totalElements());
		assertEquals(2, first.totalPages());
	}

	@Test
	void listRejectsInvalidPage() {
		assertThrows(
				InvalidTransactionInputException.class,
				() -> list(USER_A, null, null, null, null, null, -1, 20));
	}

	@Test
	void listRejectsInvalidAndOversizedPageSizes() {
		assertAll(
				() -> assertThrows(
						InvalidTransactionInputException.class,
						() -> list(USER_A, null, null, null, null, null, 0, 0)),
				() -> assertThrows(
						InvalidTransactionInputException.class,
						() -> list(USER_A, null, null, null, null, null, 0, 101)));
	}

	@Test
	void listRejectsReversedDateRange() {
		assertThrows(
				InvalidTransactionInputException.class,
				() -> list(USER_A, null, null, EVENT_DATE, EVENT_DATE.minusDays(1), null, 0, 20));
	}

	@Test
	void listReturnsInstallmentOnlyOnceWhenSeveralOccurrencesMatch() {
		create(USER_A, TransactionKind.EXPENSE, "Installments", "90.00",
				EXPENSE_CATEGORY, EVENT_DATE, 3, TransactionSource.MANUAL);

		TransactionPageOutput page = list(
				USER_A, null, null, EVENT_DATE, EVENT_DATE.plusMonths(2), null, 0, 20);

		assertEquals(1, page.totalElements());
		assertEquals(1, page.content().size());
	}

	@Test
	void listNormalizesBlankSearchToNoFilter() {
		createDefault(USER_A, "One");
		createDefault(USER_A, "Two");

		assertEquals(2, list(USER_A, null, null, null, null, "   ", 0, 20).totalElements());
	}

	@Test
	void updateReplacesEditableFieldsAndRegeneratesSchedule() {
		TransactionOutput created = createDefault(USER_A, "Original");
		LocalDate newDate = EVENT_DATE.plusDays(5);

		TransactionOutput updated = updateUseCase.execute(
				USER_A,
				created.transactionId(),
				new UpdateTransactionInput(
						TransactionKind.EXPENSE,
						"Updated",
						new BigDecimal("100.00"),
						SECOND_EXPENSE_CATEGORY,
						newDate,
						3));

		assertAll(
				() -> assertEquals("Updated", updated.description()),
				() -> assertEquals(new BigDecimal("100.00"), updated.amount()),
				() -> assertEquals(SECOND_EXPENSE_CATEGORY, updated.categoryId()),
				() -> assertEquals(3, updated.installmentCount()),
				() -> assertEquals(
						List.of(newDate, newDate.plusMonths(1), newDate.plusMonths(2)),
						updated.occurrences().stream()
								.map(occurrence -> occurrence.effectiveDate())
								.toList()),
				() -> assertEquals(
						List.of(new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("33.34")),
						updated.occurrences().stream().map(occurrence -> occurrence.amount()).toList()));
	}

	@Test
	void updatePreservesIdOwnerAndOriginalSource() {
		TransactionOutput created = create(
				USER_A, TransactionKind.EXPENSE, "Assistant-created", "10.00",
				EXPENSE_CATEGORY, EVENT_DATE, 1, TransactionSource.ASSISTANT_TEXT);

		TransactionOutput updated = updateUseCase.execute(
				USER_A,
				created.transactionId(),
				new UpdateTransactionInput(
						TransactionKind.EXPENSE,
						"Edited",
						new BigDecimal("20.00"),
						EXPENSE_CATEGORY,
						EVENT_DATE,
						1));

		FinancialTransaction persisted = repository
				.findById(USER_A, created.transactionId())
				.orElseThrow();
		assertEquals(created.transactionId(), updated.transactionId());
		assertEquals(USER_A, persisted.ownerId());
		assertEquals(TransactionSource.ASSISTANT_TEXT, updated.source());
	}

	@Test
	void updateValidatesCategoryCompatibility() {
		TransactionOutput created = createDefault(USER_A, "Original");

		assertThrows(
				CategoryIncompatibleException.class,
				() -> updateUseCase.execute(
						USER_A,
						created.transactionId(),
						new UpdateTransactionInput(
								TransactionKind.EXPENSE,
								"Wrong category",
								new BigDecimal("10.00"),
								INCOME_CATEGORY,
								EVENT_DATE,
								1)));
	}

	@Test
	void updateTreatsCrossOwnerTransactionAsNotFound() {
		TransactionOutput created = createDefault(USER_A, "Private");

		assertThrows(
				TransactionNotFoundException.class,
				() -> updateUseCase.execute(
						USER_B,
						created.transactionId(),
						new UpdateTransactionInput(
								TransactionKind.EXPENSE,
								"Attempt",
								new BigDecimal("10.00"),
								EXPENSE_CATEGORY,
								EVENT_DATE,
								1)));
	}

	@Test
	void ownerCanDeleteTransaction() {
		TransactionOutput created = createDefault(USER_A, "Delete me");

		deleteUseCase.execute(USER_A, created.transactionId());

		assertFalse(repository.findById(USER_A, created.transactionId()).isPresent());
	}

	@Test
	void crossOwnerCannotDeleteTransaction() {
		TransactionOutput created = createDefault(USER_A, "Private");

		assertThrows(
				TransactionNotFoundException.class,
				() -> deleteUseCase.execute(USER_B, created.transactionId()));
		assertTrue(repository.findById(USER_A, created.transactionId()).isPresent());
	}

	@Test
	void deletingNonexistentTransactionReturnsNotFound() {
		assertThrows(
				TransactionNotFoundException.class,
				() -> deleteUseCase.execute(USER_A, TransactionId.generate()));
	}

	private TransactionOutput createDefault(UserId ownerId, String description) {
		return create(
				ownerId,
				TransactionKind.EXPENSE,
				description,
				"25.00",
				EXPENSE_CATEGORY,
				EVENT_DATE,
				1,
				TransactionSource.MANUAL);
	}

	private TransactionOutput create(
			UserId ownerId,
			TransactionKind kind,
			String description,
			String amount,
			CategoryId categoryId,
			LocalDate eventDate,
			int installmentCount,
			TransactionSource source) {
		return createUseCase.execute(
				ownerId,
				source,
				new CreateTransactionInput(
						kind,
						description,
						new BigDecimal(amount),
						categoryId,
						eventDate,
						installmentCount));
	}

	private TransactionPageOutput list(
			UserId ownerId,
			TransactionKind kind,
			CategoryId categoryId,
			LocalDate from,
			LocalDate to,
			String searchText,
			int page,
			int size) {
		return listUseCase.execute(
				ownerId,
				new ListTransactionsInput(kind, categoryId, from, to, searchText, page, size));
	}

	private static List<TransactionId> ids(TransactionPageOutput page) {
		return page.content().stream().map(TransactionOutput::transactionId).toList();
	}

	private static UserId user(String value) {
		return UserId.from(UUID.fromString(value));
	}

	private static CategoryId category(String value) {
		return CategoryId.from(UUID.fromString(value));
	}

	private static final class InMemoryFinancialTransactionRepository
			implements FinancialTransactionRepository {

		private final Map<TransactionId, FinancialTransaction> transactions = new LinkedHashMap<>();

		@Override
		public FinancialTransaction save(FinancialTransaction transaction) {
			transactions.put(transaction.id(), transaction);
			return transaction;
		}

		@Override
		public Optional<FinancialTransaction> findById(UserId ownerId, TransactionId transactionId) {
			return Optional.ofNullable(transactions.get(transactionId))
					.filter(transaction -> transaction.ownerId().equals(ownerId));
		}

		@Override
		public FinancialTransactionPage search(
				UserId ownerId,
				FinancialTransactionSearchCriteria criteria) {
			List<FinancialTransaction> matching = transactions.values().stream()
					.filter(transaction -> transaction.ownerId().equals(ownerId))
					.filter(transaction -> criteria.kind()
							.map(kind -> transaction.kind() == kind)
							.orElse(true))
					.filter(transaction -> criteria.categoryId()
							.map(categoryId -> transaction.categoryId().equals(categoryId))
							.orElse(true))
					.filter(transaction -> criteria.searchText()
							.map(text -> transaction.description().toLowerCase(Locale.ROOT)
									.contains(text.toLowerCase(Locale.ROOT)))
							.orElse(true))
					.filter(transaction -> matchesEffectiveDate(transaction, criteria))
					.sorted(Comparator.comparing(FinancialTransaction::eventDate)
							.reversed()
							.thenComparing(transaction -> transaction.id().value()))
					.toList();

			long offset = (long) criteria.page() * criteria.size();
			int fromIndex = (int) Math.min(offset, matching.size());
			int toIndex = Math.min(fromIndex + criteria.size(), matching.size());
			return new FinancialTransactionPage(
					matching.subList(fromIndex, toIndex),
					criteria.page(),
					criteria.size(),
					matching.size());
		}

		@Override
		public boolean deleteById(UserId ownerId, TransactionId transactionId) {
			return findById(ownerId, transactionId)
					.map(transaction -> transactions.remove(transactionId, transaction))
					.orElse(false);
		}

		private static boolean matchesEffectiveDate(
				FinancialTransaction transaction,
				FinancialTransactionSearchCriteria criteria) {
			if (criteria.effectiveDateFrom().isEmpty() && criteria.effectiveDateTo().isEmpty()) {
				return true;
			}
			return transaction.occurrences().stream().anyMatch(occurrence ->
					criteria.effectiveDateFrom()
							.map(from -> !occurrence.effectiveDate().isBefore(from))
							.orElse(true)
							&& criteria.effectiveDateTo()
							.map(to -> !occurrence.effectiveDate().isAfter(to))
							.orElse(true));
		}
	}

	private static final class InMemoryCategoryCatalog implements CategoryCatalog {

		private final Map<CategoryId, CategoryEntry> categories = new HashMap<>();

		void addBuiltIn(CategoryId id, Set<TransactionKind> allowedKinds) {
			categories.put(id, new CategoryEntry(null, new CategoryMetadata(id, allowedKinds)));
		}

		void addCustom(CategoryId id, UserId ownerId, Set<TransactionKind> allowedKinds) {
			categories.put(id, new CategoryEntry(ownerId, new CategoryMetadata(id, allowedKinds)));
		}

		@Override
		public Optional<CategoryMetadata> findVisibleById(UserId ownerId, CategoryId categoryId) {
			return Optional.ofNullable(categories.get(categoryId))
					.filter(entry -> entry.ownerId() == null || entry.ownerId().equals(ownerId))
					.map(CategoryEntry::metadata);
		}
	}

	private record CategoryEntry(UserId ownerId, CategoryMetadata metadata) {
	}
}
