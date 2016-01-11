CREATE TABLE ProofOfWorkTask (
  initial_hash BINARY(64)  NOT NULL PRIMARY KEY,
  client       VARCHAR(40) NOT NULL,
  target       BINARY(32),
  nonce        BINARY(8),
  timestamp    BIGINT      NOT NULL,
);