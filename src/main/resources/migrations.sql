--liquibase formatted sql

--changeset tkindy:1
CREATE TABLE "games" (
    "id" VARCHAR(6) PRIMARY KEY,
    "name" VARCHAR(32) NOT NULL
);

--changeset tkindy:2
CREATE TABLE "players" (
    "id" SERIAL PRIMARY KEY,
    "gameId" VARCHAR(6) NOT NULL,
    "characterId" SMALLINT NOT NULL,
    "gridX" INT NOT NULL,
    "gridY" INT NOT NULL,
    "speedIndex" SMALLINT NOT NULL,
    "mightIndex" SMALLINT NOT NULL,
    "sanityIndex" SMALLINT NOT NULL,
    "knowledgeIndex" SMALLINT NOT NULL
);

CREATE INDEX ON "players" ("gameId");

--changeset tkindy:3
CREATE TABLE "rooms" (
    "id" SERIAL PRIMARY KEY,
    "gameId" VARCHAR(6) NOT NULL,
    "roomDefId" SMALLINT NOT NULL,
    "gridX" INT NOT NULL,
    "gridY" INT NOT NULL,
    "rotation" SMALLINT NOT NULL
);

CREATE INDEX ON "rooms" ("gameId");

--changeset tkindy:4
CREATE TABLE "roomStacks" (
    "id" SERIAL PRIMARY KEY,
    "gameId" VARCHAR(6) NOT NULL,
    "curIndex" SMALLINT,
    "flipped" BOOLEAN
);

CREATE UNIQUE INDEX ON "roomStacks" ("gameId");

CREATE TABLE "roomStackContents" (
    "id" SERIAL PRIMARY KEY,
    "stackId" INT NOT NULL,
    "index" SMALLINT NOT NULL,
    "roomDefId" SMALLINT NOT NULL
);
