package com.talktally.application.person.output;

import com.talktally.domain.PersonId;

public record PersonOutput(PersonId personId, String displayName) {
}
