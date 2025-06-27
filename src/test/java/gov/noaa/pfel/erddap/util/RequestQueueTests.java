package gov.noaa.pfel.erddap.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class RequestQueueTests {

  @Test
  void basicTests() {
    RequestQueue<String> queue = new RequestQueue<>();
    assertFalse(queue.hasNext());

    queue.add("first");
    assertTrue(queue.hasNext());
    assertEquals(1, queue.size());

    queue.add("first");
    assertTrue(queue.hasNext());
    assertEquals(1, queue.size());

    queue.add("second");
    assertEquals(2, queue.size());

    queue.add("third");
    queue.add("first");
    queue.add("second");
    queue.add("third");
    assertEquals(3, queue.size());
    String value = queue.getNext();
    assertEquals("first", value);
    value = queue.getNext();
    assertEquals("second", value);
    value = queue.getNext();
    assertEquals("third", value);
    assertFalse(queue.hasNext());
    assertEquals(3, queue.size());
    queue.add("first");
    queue.add("second");
    queue.add("third");
    assertEquals(6, queue.size());
    value = queue.getNext();
    assertEquals("first", value);
    queue.add("first");
    queue.add("second");
    queue.add("third");
    assertEquals(7, queue.size());
    value = queue.getNext();
    assertEquals("second", value);
    assertEquals(7, queue.size());
  }
}
