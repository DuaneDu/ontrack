-- 25. Build package versions

CREATE TABLE BUILD_PACKAGE_VERSIONS
(
  BUILD           INTEGER      NOT NULL,
  PACKAGE_TYPE    VARCHAR(150) NOT NULL,
  PACKAGE_ID      VARCHAR(400) NOT NULL,
  PACKAGE_VERSION VARCHAR(150) NOT NULL,
  CREATION        VARCHAR(24)  NOT NULL,
  TARGET          INTEGER      NULL,
  CONSTRAINT BUILD_PACKAGE_VERSIONS_FK_BUILD FOREIGN KEY (BUILD) REFERENCES BUILDS (ID) ON DELETE CASCADE,
  CONSTRAINT BUILD_PACKAGE_VERSIONS_FK_TARGET FOREIGN KEY (TARGET) REFERENCES BUILDS (ID) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS BUILD_PACKAGE_VERSIONS_IX_PACKAGE_ID ON BUILD_PACKAGE_VERSIONS (PACKAGE_TYPE, PACKAGE_ID);
CREATE INDEX IF NOT EXISTS BUILD_PACKAGE_VERSIONS_IX_PACKAGE_VERSION ON BUILD_PACKAGE_VERSIONS (PACKAGE_TYPE, PACKAGE_ID, PACKAGE_VERSION);
