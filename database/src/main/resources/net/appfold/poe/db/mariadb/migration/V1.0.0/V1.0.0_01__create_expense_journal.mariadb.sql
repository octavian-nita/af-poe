--
-- Entries in a simple EXpense Journal.
--
CREATE TABLE journal_entry (

    id          INTEGER UNSIGNED        NOT NULL UNIQUE AUTO_INCREMENT            COMMENT 'The entry technical identifier',
    posted_on   DATETIME                NOT NULL        DEFAULT CURRENT_TIMESTAMP COMMENT 'The date (and time) the entry has been posted on; may be different from the actual transaction date, usually soon after',
    amount      NUMERIC(19, 6) UNSIGNED NOT NULL        DEFAULT 0                 COMMENT 'The amount of the transaction; always positive',
    type        ENUM('DEBIT', 'CREDIT') NOT NULL        DEFAULT 'CREDIT'          COMMENT 'The entry type, either DEBIT or CREDIT',
    foreseen    ENUM('Y', 'N')                                                    COMMENT 'Whether the entry was foreseen or not; if yes, some calculations might discard the entry amount',
    description VARCHAR(1000)                                                     COMMENT 'Optional description / details for the entry',

    PRIMARY KEY (id)
) AUTO_INCREMENT 1;

--
-- Categories for the expense journal entries.
--
CREATE TABLE entry_category (

    id          VARCHAR(24)  NOT NULL UNIQUE COMMENT 'A category code for an entry, e.g. OTHER, GROC for groceries... useful for reporting, grouping, insights, etc.; also a technical identifier',
    description VARCHAR(1000)                COMMENT 'Optional description / details for the category',

    PRIMARY KEY (id)
);

--
-- Associates expense journal entries with categories (many-to-many).
--
CREATE TABLE entry_category_mapping (

    entry_id    INTEGER UNSIGNED NOT NULL UNIQUE,
    category_id VARCHAR(255)     NOT NULL UNIQUE,

    FOREIGN KEY (entry_id)    REFERENCES journal_entry (id),
    FOREIGN KEY (category_id) REFERENCES entry_category (id),
    PRIMARY KEY (entry_id, category_id)
);
