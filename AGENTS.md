# Architecture
Type: Java Servlet Web Application.

Container: Designed for Apache Tomcat / Jetty.

Forbidden Frameworks: Do not introduce Spring Boot, Quarkus, or Micronaut. Maintain the Servlet API structure.

# Build Environment
System: Maven. Run mvn clean install -DskipTests to prime dependencies.

JDK Target: Java 25 (Adoptium). Ensure language level compliance. Code should ideally be compileable in JDK 17. Ask before using any feature added since JDK 17.

Formatting: Strict adherence required. Run mvn git-code-format:format-code before submitting PRs.

# Testing Standards
Framework: JUnit 5.

Constraint: Tests must not rely on absolute file paths (e.g., C:\data). Use src/test/resources.

Mocking: Use Mockito for external network or filesystem calls.

# Security
Static Analysis: All code must pass SpotBugs checks defined in spotbugs-security-include.xml.

Ensure no new warnings are added.