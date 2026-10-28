# Upgrade Plan: ProyectoBarberia (java-upgrade-20261028)

- **Generated**: 2026-10-28 17:54:00 -03:00
- **HEAD Branch**: N/A
- **HEAD Commit ID**: N/A

## Available Tools

**JDKs**
- JDK 25: C:\Program Files\Java\jdk-25.0.5.0.2 (current project JDK, used for baseline and final validation)
- JDK 21: C:\Program Files\Java\jdk-25.0.5.0.2 (effective upgrade target is Java 25; Java 21 is already supported by the project compiler release)

**Build Tools**
- Maven 3.9.9: C:\Users\abriz\.maven\apache-maven-3.9.9\bin\mvn.cmd (local installation, compatible with Java 25)
- Maven Wrapper: not present in project root

## Guidelines

- Upgrade Java runtime to the latest LTS version available in this environment: Java 25.
- Keep the change set minimal and focused on the runtime/compiler target.
- Preserve the current project behavior and dependencies; no framework migration is required because the project is a direct Java/Maven application without a Spring Boot layer.
- Skip version-control operations if Git metadata is unavailable or the repo is not initialized.

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred.

## Options

- Working branch: appmod/java-upgrade-java-upgrade-20261028
- Run tests before and after the upgrade: true

## Upgrade Goals

- Java 25 (latest LTS)

## Technology Stack

| Technology/Dependency | Current | Min Compatible Version | Why Incompatible |
| ---------------------- | ------- | --------------------- | --------------- |
| Java | 21 | 25 | User requested upgrade to latest LTS |
| Maven | 3.9.9 | 3.9.9 | Already compatible with Java 25 |
| Javalin | 6.3.0 | 6.3.0 | No incompatibility observed; project compiles under Java 25 |
| Java JWT (jjwt) | 0.12.6 | 0.12.6 | No incompatibility observed |
| HikariCP | 5.1.0 | 5.1.0 | No incompatibility observed |
| MySQL/MSSQL drivers | 8.3.0 / 12.8.1.jre11 | Current | No incompatibility observed |
| Maven compiler release | 21 | 25 | Current project value must be aligned with runtime target |

## Derived Upgrades

- Upgrade `maven.compiler.release` from 21 to 25 to compile against the latest LTS runtime.
- Ensure the local Maven installation is Java 25-compatible and use it for all compile/test verification.
- No framework migration or dependency replacement is required because the application does not rely on Spring Boot or Jakarta EE namespaces.

## Impact Analysis

### Subsection: Dependency Changes

| File | Dependency | Current | Action | Target | Reason |
|------|------------|---------|--------|--------|--------|
| pom.xml | maven.compiler.release | 21 | upgrade | 25 | User requested Java 25 LTS runtime |

### Subsection: Source Code Changes

| File | Location | Current | Required Change | Reason |
|------|----------|---------|----------------|--------|
| None | N/A | N/A | No source migration required | Project compiles cleanly under Java 25 and uses standard Java APIs |

### Subsection: Configuration Changes

| File | Property/Setting | Current | Required Change | Reason |
|------|------------------|---------|-----------------|--------|
| pom.xml | maven.compiler.release | 21 | Set to 25 | Align build compiler target with Java 25 |

### Subsection: CI/CD Changes

| File | Location | Current | Required Change |
|------|----------|---------|----------------|
| None | N/A | N/A | No CI/CD files found with hardcoded Java version references |

### Subsection: Risks & Warnings

- **No major migration risk identified**: The codebase is a plain Java/Maven application using standard library APIs and does not use removed JDK internals, Spring Boot, or Java EE namespaces.
- **Validation guard**: Compile and test the project under Java 25 after the compiler target update to confirm no runtime or reflection issues were introduced.

## Upgrade Steps

- Step 1: Validate environment and install/confirm required Java 25 runtime
  - **Rationale**: The project’s build must run on the target LTS runtime before and after the upgrade.
  - **Changes to Make**: Confirm Java 25 installation and the local Maven toolchain.
  - **Verification**: `java -version` and `mvn -version` under Java 25; expected success.

- Step 2: Run baseline compile/test on the current project state under Java 25
  - **Rationale**: Capture the current pass/fail status before changing the compiler target and confirm the project already works on Java 25.
  - **Changes to Make**: Run `mvn -q test` on the current POM state.
  - **Verification**: Command: `cmd /c "set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.5.0.2" && set "PATH=%JAVA_HOME%\bin;%PATH%" && cd /d "C:\Users\abriz\OneDrive\Escritorio\Barber System\Barber-System-Backend" && C:\Users\abriz\.maven\apache-maven-3.9.9\bin\mvn.cmd -q test"`; expected result: PASS (currently the project already compiles and tests under Java 25).

- Step 3: Update compiler target to Java 25
  - **Rationale**: Align the project’s Maven compiler release with the upgraded runtime so the build uses Java 25 semantics.
  - **Changes to Make**: Apply the pom.xml change from `maven.compiler.release=21` to `maven.compiler.release=25`.
  - **Verification**: `mvn clean test-compile -q` with Java 25; expected result: successful compilation.

- Step 4: Final validation
  - **Rationale**: Confirm the project still builds and passes tests after the compiler target change.
  - **Changes to Make**: None beyond the final validation run.
  - **Verification**: `mvn clean test -q` under Java 25; expected result: 100% pass rate or baseline-equivalent success (the project currently has no failing tests in the Java 25 run).
