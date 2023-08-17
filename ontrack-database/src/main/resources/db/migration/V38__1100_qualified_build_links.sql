ALTER TABLE BUILD_LINKS
    DROP CONSTRAINT BUILD_LINKS_UQ;

ALTER TABLE BUILD_LINKS
    ADD COLUMN QUALIFIER VARCHAR(80) NOT NULL DEFAULT '';

ALTER TABLE BUILD_LINKS
    ADD CONSTRAINT BUILD_LINKS_UQ UNIQUE (BUILDID, TARGETBUILDID, QUALIFIER);
