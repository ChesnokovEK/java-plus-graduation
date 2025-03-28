CREATE TABLE IF NOT EXISTS hits
(
    id    BIGINT GENERATED BY DEFAULT AS IDENTITY      NOT NULL,
    app VARCHAR(512)                                   NOT NULL,
    uri VARCHAR(512)                                   NOT NULL,
    ip VARCHAR(32)                                     NOT NULL,
    ts TIMESTAMP WITHOUT TIME ZONE            NOT NULL,
    CONSTRAINT pk_Hit_id PRIMARY KEY (id)
) ;