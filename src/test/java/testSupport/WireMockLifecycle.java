package testSupport;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * JUnit lifecycle support to start and stop the WireMock server for tests. Add this class to test
 * sources so JUnit will run its @BeforeAll/@AfterAll when the class is discovered by the test
 * runner.
 */
public class WireMockLifecycle {

  @BeforeAll
  public static void beforeAll() {
    try {
      System.out.println("Starting WireMock for tests.");
      WireMockStarter.start();
    } catch (Throwable t) {
      System.out.println("WireMockStarter.start() failed: " + t);
    }
  }

  @AfterAll
  public static void afterAll() {
    try {
      System.out.println("Stopping WireMock for tests.");
      WireMockStarter.stop();
    } catch (Throwable t) {
      System.out.println("WireMockStarter.stop() failed: " + t);
    }
  }
}
