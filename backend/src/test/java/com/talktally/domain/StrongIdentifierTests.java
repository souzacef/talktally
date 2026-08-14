package com.talktally.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StrongIdentifierTests {

	@Test
	void generatesIdentifiers() {
		UserId firstUserId = UserId.generate();
		UserId secondUserId = UserId.generate();

		assertNotNull(firstUserId.value());
		assertNotEquals(firstUserId, secondUserId);
		assertNotNull(TransactionId.generate().value());
		assertNotNull(CategoryId.generate().value());
	}

	@Test
	void reconstructsIdentifiersFromExistingUuids() {
		UUID value = UUID.randomUUID();

		assertEquals(value, UserId.from(value).value());
		assertEquals(value, TransactionId.from(value).value());
		assertEquals(value, CategoryId.from(value).value());
	}

	@Test
	void rejectsNullIdentifierValues() {
		assertThrows(NullPointerException.class, () -> UserId.from(null));
		assertThrows(NullPointerException.class, () -> TransactionId.from(null));
		assertThrows(NullPointerException.class, () -> CategoryId.from(null));
	}
}
