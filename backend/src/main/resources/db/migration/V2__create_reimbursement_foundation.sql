CREATE TABLE person (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_person_user
        FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT uq_person_id_user UNIQUE (id, user_id),
    CONSTRAINT uq_person_user_normalized_name UNIQUE (user_id, normalized_name),
    CONSTRAINT ck_person_display_name_not_blank CHECK (TRIM(display_name) <> ''),
    CONSTRAINT ck_person_normalized_name_not_blank CHECK (TRIM(normalized_name) <> '')
);

CREATE INDEX idx_person_user_name ON person (user_id, normalized_name);

CREATE TABLE reimbursement_claim (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    expense_transaction_id UUID NOT NULL,
    person_id UUID NOT NULL,
    original_amount NUMERIC(19, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reimbursement_claim_expense_owner
        FOREIGN KEY (expense_transaction_id, user_id)
        REFERENCES financial_transaction (id, user_id),
    CONSTRAINT fk_reimbursement_claim_person_owner
        FOREIGN KEY (person_id, user_id)
        REFERENCES person (id, user_id),
    CONSTRAINT uq_reimbursement_claim_id_user UNIQUE (id, user_id),
    CONSTRAINT uq_reimbursement_claim_expense UNIQUE (expense_transaction_id),
    CONSTRAINT ck_reimbursement_claim_amount_positive CHECK (original_amount > 0),
    CONSTRAINT ck_reimbursement_claim_currency_length CHECK (CHAR_LENGTH(currency) = 3)
);

CREATE INDEX idx_reimbursement_claim_user_person
    ON reimbursement_claim (user_id, person_id);

CREATE TABLE reimbursement_payment (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    claim_id UUID NOT NULL,
    receipt_transaction_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    received_date DATE NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reimbursement_payment_claim_owner
        FOREIGN KEY (claim_id, user_id)
        REFERENCES reimbursement_claim (id, user_id),
    CONSTRAINT fk_reimbursement_payment_receipt_owner
        FOREIGN KEY (receipt_transaction_id, user_id)
        REFERENCES financial_transaction (id, user_id),
    CONSTRAINT uq_reimbursement_payment_receipt UNIQUE (receipt_transaction_id),
    CONSTRAINT ck_reimbursement_payment_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_reimbursement_payment_currency_length CHECK (CHAR_LENGTH(currency) = 3)
);

CREATE INDEX idx_reimbursement_payment_claim_date
    ON reimbursement_payment (claim_id, received_date);
