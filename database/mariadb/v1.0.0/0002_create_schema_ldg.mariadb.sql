USE af_ledger;

CREATE TABLE t_ldg_entry (
  id         BIGINT UNSIGNED         NOT NULL UNIQUE AUTO_INCREMENT            COMMENT 'The entry technical identifier',
  entry_date DATETIME                NOT NULL        DEFAULT CURRENT_TIMESTAMP COMMENT 'The date the entry has been created (may be different from the actual transaction date)',
  amount     NUMERIC(19, 6) UNSIGNED NOT NULL        DEFAULT 0                 COMMENT 'The entry amount, always positive',
  type       ENUM('CREDIT', 'DEBIT') NOT NULL        DEFAULT 'CREDIT'          COMMENT 'The entry type, either credit or debit',
  foreseen   ENUM('Y', 'N')                                                    COMMENT 'Whether an entry was foreseen or not (if yes, some calculations might not consider it)',
  details    VARCHAR(1000)                                                     COMMENT 'Additional / optional details on the specific entry',
  PRIMARY KEY (id)
) AUTO_INCREMENT = 1;

SHOW FULL COLUMNS FROM t_ldg_entry;

CREATE TABLE t_ldg_category (
  id      VARCHAR(255)  NOT NULL UNIQUE COMMENT 'A category code for an entry, e.g. OTHER, GROC from groceries, etc. (useful for reporting, grouping, insights, etc.); also a technical identifier',
  details VARCHAR(1000)                 COMMENT 'Additional / optional details on the specific category',
  PRIMARY KEY (id)
);

SHOW FULL COLUMNS FROM t_ldg_category;

CREATE TABLE t_ldg_entry_category (
  entry_id    BIGINT UNSIGNED NOT NULL UNIQUE,
  category_id VARCHAR(255)    NOT NULL UNIQUE,
  FOREIGN KEY (entry_id)    REFERENCES t_ldg_entry (id),
  FOREIGN KEY (category_id) REFERENCES t_ldg_category (id),
  PRIMARY KEY (entry_id, category_id)
);

SHOW FULL COLUMNS FROM t_ldg_entry_category;
