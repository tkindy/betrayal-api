--liquibase formatted sql

--changeset tkindy:1
CREATE TABLE games (
    id VARCHAR(6) PRIMARY KEY,
    name VARCHAR(32) NOT NULL
);

--changeset tkindy:2
CREATE TABLE players (
    id SERIAL PRIMARY KEY,
    gameId VARCHAR(6) NOT NULL,
    characterId SMALLINT NOT NULL,
    speedIndex SMALLINT NOT NULL,
    mightIndex SMALLINT NOT NULL,
    sanityIndex SMALLINT NOT NULL,
    knowledgeIndex SMALLINT NOT NULL
);

CREATE INDEX ON players (gameId);
