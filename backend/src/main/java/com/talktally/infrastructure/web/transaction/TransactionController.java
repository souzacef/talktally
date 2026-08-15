package com.talktally.infrastructure.web.transaction;

import com.talktally.application.input.ListTransactionsInput;
import com.talktally.application.output.TransactionOutput;
import com.talktally.application.output.TransactionPageOutput;
import com.talktally.application.transaction.CreateTransactionUseCase;
import com.talktally.application.transaction.DeleteTransactionUseCase;
import com.talktally.application.transaction.GetTransactionUseCase;
import com.talktally.application.transaction.ListTransactionsUseCase;
import com.talktally.application.transaction.UpdateTransactionUseCase;
import com.talktally.domain.CategoryId;
import com.talktally.domain.TransactionId;
import com.talktally.domain.TransactionKind;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import com.talktally.infrastructure.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

	private static final String BASE_PATH = "/api/v1/transactions";

	private final CreateTransactionUseCase createTransactionUseCase;
	private final GetTransactionUseCase getTransactionUseCase;
	private final ListTransactionsUseCase listTransactionsUseCase;
	private final UpdateTransactionUseCase updateTransactionUseCase;
	private final DeleteTransactionUseCase deleteTransactionUseCase;
	private final AuthenticatedUserProvider authenticatedUserProvider;

	public TransactionController(
			CreateTransactionUseCase createTransactionUseCase,
			GetTransactionUseCase getTransactionUseCase,
			ListTransactionsUseCase listTransactionsUseCase,
			UpdateTransactionUseCase updateTransactionUseCase,
			DeleteTransactionUseCase deleteTransactionUseCase,
			AuthenticatedUserProvider authenticatedUserProvider) {
		this.createTransactionUseCase = Objects.requireNonNull(
				createTransactionUseCase, "create transaction use case must not be null");
		this.getTransactionUseCase = Objects.requireNonNull(
				getTransactionUseCase, "get transaction use case must not be null");
		this.listTransactionsUseCase = Objects.requireNonNull(
				listTransactionsUseCase, "list transactions use case must not be null");
		this.updateTransactionUseCase = Objects.requireNonNull(
				updateTransactionUseCase, "update transaction use case must not be null");
		this.deleteTransactionUseCase = Objects.requireNonNull(
				deleteTransactionUseCase, "delete transaction use case must not be null");
		this.authenticatedUserProvider = Objects.requireNonNull(
				authenticatedUserProvider, "authenticated user provider must not be null");
	}

	@PostMapping
	public ResponseEntity<TransactionResponse> create(
			@Valid @RequestBody TransactionRequest request) {
		UserId actorId = authenticatedUserProvider.currentUserId();
		TransactionOutput output = createTransactionUseCase.execute(
				actorId, TransactionSource.MANUAL, request.toCreateInput());
		TransactionResponse response = TransactionResponse.from(output);
		return ResponseEntity
				.created(URI.create(BASE_PATH + "/" + response.id()))
				.body(response);
	}

	@GetMapping("/{transactionId}")
	public TransactionResponse get(@PathVariable UUID transactionId) {
		UserId actorId = authenticatedUserProvider.currentUserId();
		return TransactionResponse.from(getTransactionUseCase.execute(
				actorId, TransactionId.from(transactionId)));
	}

	@GetMapping
	public TransactionPageResponse list(
			@RequestParam(required = false) TransactionKind kind,
			@RequestParam(required = false) UUID categoryId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate to,
			@RequestParam(required = false) String search,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		UserId actorId = authenticatedUserProvider.currentUserId();
		TransactionPageOutput output = listTransactionsUseCase.execute(
				actorId,
				new ListTransactionsInput(
						kind,
						categoryId == null ? null : CategoryId.from(categoryId),
						from,
						to,
						search,
						page,
						size));
		return TransactionPageResponse.from(output);
	}

	@PutMapping("/{transactionId}")
	public TransactionResponse update(
			@PathVariable UUID transactionId,
			@Valid @RequestBody TransactionRequest request) {
		UserId actorId = authenticatedUserProvider.currentUserId();
		return TransactionResponse.from(updateTransactionUseCase.execute(
				actorId,
				TransactionId.from(transactionId),
				request.toUpdateInput()));
	}

	@DeleteMapping("/{transactionId}")
	public ResponseEntity<Void> delete(@PathVariable UUID transactionId) {
		UserId actorId = authenticatedUserProvider.currentUserId();
		deleteTransactionUseCase.execute(actorId, TransactionId.from(transactionId));
		return ResponseEntity.noContent().build();
	}
}
