-- 28. Security updates

ALTER TABLE ACCOUNT_GROUPS
    ADD AUTOJOIN BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE TOKENS
(
    ID          SERIAL PRIMARY KEY NOT NULL,
    ACCOUNT     INTEGER            NOT NULL,
    VALUE       VARCHAR(256)       NOT NULL,
    CREATION    VARCHAR(24)        NOT NULL,
    VALID_UNTIL VARCHAR(24)        NULL,
    CONSTRAINT TOKENS_FK_ACCOUNT FOREIGN KEY (ACCOUNT) REFERENCES ACCOUNTS (ID) ON DELETE CASCADE
);

CREATE UNIQUE INDEX TOKENS_UQ_ACCOUNT ON TOKENS (ACCOUNT);
CREATE INDEX IF NOT EXISTS TOKENS_IX_VALUE ON TOKENS (VALUE);
