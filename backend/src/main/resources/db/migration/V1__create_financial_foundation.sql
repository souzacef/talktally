CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    default_currency CHAR(3) NOT NULL DEFAULT 'BRL',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_user_email UNIQUE (email),
    CONSTRAINT ck_app_user_email_not_blank CHECK (TRIM(email) <> ''),
    CONSTRAINT ck_app_user_display_name_not_blank CHECK (TRIM(display_name) <> ''),
    CONSTRAINT ck_app_user_default_currency_length CHECK (CHAR_LENGTH(default_currency) = 3)
);

CREATE TABLE category (
    id UUID PRIMARY KEY,
    owner_user_id UUID,
    code VARCHAR(50) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    allowed_kind VARCHAR(32) NOT NULL,
    built_in BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_category_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES app_user (id),
    CONSTRAINT uq_category_code UNIQUE (code),
    CONSTRAINT ck_category_code_not_blank CHECK (TRIM(code) <> ''),
    CONSTRAINT ck_category_display_name_not_blank CHECK (TRIM(display_name) <> ''),
    CONSTRAINT ck_category_allowed_kind CHECK (
        allowed_kind IN ('INCOME', 'EXPENSE', 'REIMBURSEMENT_RECEIPT', 'ANY')
    ),
    CONSTRAINT ck_category_owner_matches_type CHECK (
        (built_in = TRUE AND owner_user_id IS NULL)
        OR (built_in = FALSE AND owner_user_id IS NOT NULL)
    )
);

CREATE INDEX idx_category_owner_user ON category (owner_user_id);

INSERT INTO category (id, code, display_name, allowed_kind, built_in) VALUES
    ('00000000-0000-0000-0000-000000000001', 'SALARY', 'Salary', 'INCOME', TRUE),
    ('00000000-0000-0000-0000-000000000002', 'FREELANCE', 'Freelance', 'INCOME', TRUE),
    ('00000000-0000-0000-0000-000000000003', 'FOOD_DINING', 'Food and dining', 'EXPENSE', TRUE),
    ('00000000-0000-0000-0000-000000000004', 'GROCERIES', 'Groceries', 'EXPENSE', TRUE),
    ('00000000-0000-0000-0000-000000000005', 'HOUSING', 'Housing', 'EXPENSE', TRUE),
    ('00000000-0000-0000-0000-000000000006', 'UTILITIES', 'Utilities', 'EXPENSE', TRUE),
    ('00000000-0000-0000-0000-000000000007', 'TRANSPORT', 'Transport', 'EXPENSE', TRUE),
    ('00000000-0000-0000-0000-000000000008', 'HEALTH', 'Health', 'EXPENSE', TRUE),
    ('00000000-0000-0000-0000-000000000009', 'EDUCATION', 'Education', 'EXPENSE', TRUE),
    ('00000000-0000-0000-0000-000000000010', 'ENTERTAINMENT', 'Entertainment', 'EXPENSE', TRUE),
    ('00000000-0000-0000-0000-000000000011', 'SHOPPING', 'Shopping', 'EXPENSE', TRUE),
    ('00000000-0000-0000-0000-000000000012', 'TRAVEL', 'Travel', 'EXPENSE', TRUE),
    ('00000000-0000-0000-0000-000000000013', 'TAXES_FEES', 'Taxes and fees', 'EXPENSE', TRUE),
    ('00000000-0000-0000-0000-000000000014', 'REIMBURSEMENT', 'Reimbursement', 'REIMBURSEMENT_RECEIPT', TRUE),
    ('00000000-0000-0000-0000-000000000015', 'OTHER', 'Other', 'ANY', TRUE);

CREATE TABLE financial_transaction (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    kind VARCHAR(32) NOT NULL,
    description VARCHAR(500) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    category_id UUID NOT NULL,
    event_date DATE NOT NULL,
    source VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_financial_transaction_user
        FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_financial_transaction_category
        FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT uq_financial_transaction_id_user UNIQUE (id, user_id),
    CONSTRAINT ck_financial_transaction_kind CHECK (
        kind IN ('EXPENSE', 'INCOME', 'REIMBURSEMENT_RECEIPT')
    ),
    CONSTRAINT ck_financial_transaction_description_not_blank CHECK (TRIM(description) <> ''),
    CONSTRAINT ck_financial_transaction_total_positive CHECK (total_amount > 0),
    CONSTRAINT ck_financial_transaction_currency_length CHECK (CHAR_LENGTH(currency) = 3),
    CONSTRAINT ck_financial_transaction_source CHECK (
        source IN ('MANUAL', 'ASSISTANT_TEXT', 'VOICE')
    )
);

CREATE INDEX idx_financial_transaction_user_event
    ON financial_transaction (user_id, event_date);
CREATE INDEX idx_financial_transaction_user_kind_event
    ON financial_transaction (user_id, kind, event_date);
CREATE INDEX idx_financial_transaction_user_category_event
    ON financial_transaction (user_id, category_id, event_date);

CREATE TABLE transaction_occurrence (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    user_id UUID NOT NULL,
    sequence_number INTEGER NOT NULL,
    effective_date DATE NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    CONSTRAINT fk_transaction_occurrence_transaction_owner
        FOREIGN KEY (transaction_id, user_id)
        REFERENCES financial_transaction (id, user_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_transaction_occurrence_sequence UNIQUE (transaction_id, sequence_number),
    CONSTRAINT ck_transaction_occurrence_sequence_positive CHECK (sequence_number > 0),
    CONSTRAINT ck_transaction_occurrence_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_transaction_occurrence_currency_length CHECK (CHAR_LENGTH(currency) = 3)
);

CREATE INDEX idx_transaction_occurrence_user_effective
    ON transaction_occurrence (user_id, effective_date);
