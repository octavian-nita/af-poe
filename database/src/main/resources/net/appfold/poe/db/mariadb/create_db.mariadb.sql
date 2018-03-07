CREATE SCHEMA IF NOT EXISTS ${db.name} CHARACTER SET = 'utf8' COLLATE = 'utf8_general_ci';

CREATE USER IF NOT EXISTS ${db.username}@localhost IDENTIFIED BY '${db.password}';

GRANT ALL PRIVILEGES ON ${db.name}.* to ${db.username};
