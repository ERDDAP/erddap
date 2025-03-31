package gov.noaa.pfel.erddap.util;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

/**
 * This class is designed to be a queue for requests that only need to happen once. If an item is
 * added that matches an item currently queued it will be ignored. The original request will still
 * happen when it reaches the front of the queue. This should make sure the requests are handled
 * while minimizing duplicated work.
 */
public class RequestQueue<T> {

  private final Queue<T> queue = new ArrayDeque<T>();
  private final Set<T> set = new HashSet<T>();
  private int removed = 0;

  public boolean add(T t) {
    // Only add element to queue if the set does not contain the specified element.
    if (set.add(t)) {
      queue.add(t);
    }
    return true;
  }

  public T getNext() throws NoSuchElementException {
    T ret = queue.remove();
    set.remove(ret);
    removed++;
    return ret;
  }

  public boolean hasNext() {
    return !queue.isEmpty();
  }

  /**
   * This is slightly weird behavior but is done to maintain the same size values as the old
   * approach.
   *
   * @return the size of the current set + the number of used (removed) entries.
   */
  public int size() {
    return removed + set.size();
  }
}
