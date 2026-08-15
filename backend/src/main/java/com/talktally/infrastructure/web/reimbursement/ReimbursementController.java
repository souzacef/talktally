package com.talktally.infrastructure.web.reimbursement;

import com.talktally.application.reimbursement.CreateReimbursableExpenseUseCase;
import com.talktally.application.reimbursement.GetReimbursementUseCase;
import com.talktally.application.reimbursement.ListReimbursementsUseCase;
import com.talktally.application.reimbursement.RecordReimbursementPaymentUseCase;
import com.talktally.application.reimbursement.input.ListReimbursementsInput;
import com.talktally.application.reimbursement.output.CreateReimbursableExpenseOutput;
import com.talktally.application.reimbursement.output.RecordReimbursementPaymentOutput;
import com.talktally.domain.PersonId;
import com.talktally.domain.ReimbursementClaimId;
import com.talktally.domain.ReimbursementStatus;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import com.talktally.infrastructure.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reimbursements")
public class ReimbursementController {

	private static final String BASE_PATH = "/api/v1/reimbursements";

	private final CreateReimbursableExpenseUseCase createUseCase;
	private final GetReimbursementUseCase getUseCase;
	private final ListReimbursementsUseCase listUseCase;
	private final RecordReimbursementPaymentUseCase paymentUseCase;
	private final AuthenticatedUserProvider authenticatedUserProvider;

	public ReimbursementController(
			CreateReimbursableExpenseUseCase createUseCase,
			GetReimbursementUseCase getUseCase,
			ListReimbursementsUseCase listUseCase,
			RecordReimbursementPaymentUseCase paymentUseCase,
			AuthenticatedUserProvider authenticatedUserProvider) {
		this.createUseCase = Objects.requireNonNull(createUseCase, "create use case must not be null");
		this.getUseCase = Objects.requireNonNull(getUseCase, "get use case must not be null");
		this.listUseCase = Objects.requireNonNull(listUseCase, "list use case must not be null");
		this.paymentUseCase = Objects.requireNonNull(paymentUseCase, "payment use case must not be null");
		this.authenticatedUserProvider = Objects.requireNonNull(
				authenticatedUserProvider, "authenticated user provider must not be null");
	}

	@PostMapping
	public ResponseEntity<CreateReimbursementResponse> create(
			@Valid @RequestBody CreateReimbursementRequest request) {
		UserId actorId = authenticatedUserProvider.currentUserId();
		CreateReimbursableExpenseOutput output = createUseCase.execute(
				actorId, TransactionSource.MANUAL, request.toInput());
		CreateReimbursementResponse response = CreateReimbursementResponse.from(output);
		return ResponseEntity
				.created(URI.create(BASE_PATH + "/" + response.claim().id()))
				.body(response);
	}

	@GetMapping("/{claimId}")
	public ReimbursementClaimResponse get(@PathVariable UUID claimId) {
		UserId actorId = authenticatedUserProvider.currentUserId();
		return ReimbursementClaimResponse.from(
				getUseCase.execute(actorId, ReimbursementClaimId.from(claimId)));
	}

	@GetMapping
	public ReimbursementPageResponse list(
			@RequestParam(required = false) UUID personId,
			@RequestParam(required = false) ReimbursementStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		UserId actorId = authenticatedUserProvider.currentUserId();
		return ReimbursementPageResponse.from(listUseCase.execute(
				actorId,
				new ListReimbursementsInput(
						personId == null ? null : PersonId.from(personId),
						status,
						page,
						size)));
	}

	@PostMapping("/{claimId}/payments")
	public ResponseEntity<RecordReimbursementPaymentResponse> recordPayment(
			@PathVariable UUID claimId,
			@Valid @RequestBody ReimbursementPaymentRequest request) {
		UserId actorId = authenticatedUserProvider.currentUserId();
		RecordReimbursementPaymentOutput output = paymentUseCase.execute(
				actorId,
				TransactionSource.MANUAL,
				ReimbursementClaimId.from(claimId),
				request.toInput());
		RecordReimbursementPaymentResponse response =
				RecordReimbursementPaymentResponse.from(output);
		return ResponseEntity
				.created(URI.create(BASE_PATH + "/" + claimId + "/payments/" + response.paymentId()))
				.body(response);
	}
}
