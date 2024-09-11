/* This file is Copyright (c) 2010, NOAA.
 * See the MIT/X-like license in LICENSE.txt.
 * For more information, email BobSimons2.00@gmail.com.
 */
package com.cohort.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A HashMap/cache where, when size &gt; tMaxSize and a new element is added, the
 * Least-Recently-Used element will be removed.
 *
 * <p>This not thread-safe. To make it thread-safe, use <tt>Map cache =
 * Collections.synchronizedMap(new LRUCache(maxSize));</tt>
 */
public class LRUCache extends LinkedHashMap {

  int maxSize;

  /**
   * Constructor
   *
   * @param tMaxSize the maximum number of elements you want this to cache. When size &gt; tMaxSize
   *     and a new element is added, the Least-Recently-Used element will be removed.
   */
  public LRUCache(int tMaxSize) {
    super(
        tMaxSize + 1,
        0.75f,
        true); // true means 'eldest' based on when last accessed (not when inserted)
    maxSize = tMaxSize;
  }

  /** RemoveEldestEntry is over-ridden to enforce maxSize rule. */
  @Override
  protected boolean removeEldestEntry(Map.Entry eldest) {
    return size() > maxSize;
  }
}
