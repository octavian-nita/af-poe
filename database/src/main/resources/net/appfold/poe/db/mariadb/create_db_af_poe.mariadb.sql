CREATE SCHEMA af_poe CHARACTER SET = 'utf8' COLLATE = 'utf8_general_ci';

CREATE USER af_poe@localhost IDENTIFIED BY 'YourPasswordIsIncorrect:!)';

GRANT ALL PRIVILEGES ON af_poe.* to af_poe;
