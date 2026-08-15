package com.talktally.infrastructure.web.person;

import com.talktally.application.person.CreatePersonUseCase;
import com.talktally.application.person.GetPersonReimbursementSummaryUseCase;
import com.talktally.application.person.ListPeopleUseCase;
import com.talktally.application.person.output.PersonOutput;
import com.talktally.domain.PersonId;
import com.talktally.domain.UserId;
import com.talktally.infrastructure.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/people")
public class PeopleController {

	private static final String BASE_PATH = "/api/v1/people";

	private final CreatePersonUseCase createPersonUseCase;
	private final ListPeopleUseCase listPeopleUseCase;
	private final GetPersonReimbursementSummaryUseCase summaryUseCase;
	private final AuthenticatedUserProvider authenticatedUserProvider;

	public PeopleController(
			CreatePersonUseCase createPersonUseCase,
			ListPeopleUseCase listPeopleUseCase,
			GetPersonReimbursementSummaryUseCase summaryUseCase,
			AuthenticatedUserProvider authenticatedUserProvider) {
		this.createPersonUseCase = Objects.requireNonNull(
				createPersonUseCase, "create person use case must not be null");
		this.listPeopleUseCase = Objects.requireNonNull(
				listPeopleUseCase, "list people use case must not be null");
		this.summaryUseCase = Objects.requireNonNull(
				summaryUseCase, "summary use case must not be null");
		this.authenticatedUserProvider = Objects.requireNonNull(
				authenticatedUserProvider, "authenticated user provider must not be null");
	}

	@PostMapping
	public ResponseEntity<PersonResponse> create(@Valid @RequestBody PersonRequest request) {
		UserId actorId = authenticatedUserProvider.currentUserId();
		PersonOutput output = createPersonUseCase.execute(actorId, request.toInput());
		PersonResponse response = PersonResponse.from(output);
		return ResponseEntity
				.created(URI.create(BASE_PATH + "/" + response.id()))
				.body(response);
	}

	@GetMapping
	public List<PersonResponse> list() {
		UserId actorId = authenticatedUserProvider.currentUserId();
		return listPeopleUseCase.execute(actorId).stream()
				.map(PersonResponse::from)
				.toList();
	}

	@GetMapping("/{personId}/reimbursements/summary")
	public PersonReimbursementSummaryResponse summary(@PathVariable UUID personId) {
		UserId actorId = authenticatedUserProvider.currentUserId();
		return PersonReimbursementSummaryResponse.from(
				summaryUseCase.execute(actorId, PersonId.from(personId)));
	}
}
