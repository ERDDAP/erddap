package com.cohort.util;

class LRUCacheTests {
  /** Test this class. */
  @org.junit.jupiter.api.Test
  void basicTest() {
    String2.log("\n*** LRUCache.basicTest");
    LRUCache cache = new LRUCache(5);
    for (int i = 0; i < 5; i++) cache.put("" + i, "" + (11 * i));
    Test.ensureEqual(cache.size(), 5, "");
    Test.ensureNotNull(cache.get("0"), ""); // 0 was eldest. Now accessed so 1 is eldest

    // knock "1" out of cache
    cache.put("6", "66");
    Test.ensureEqual(cache.size(), 5, "");
    Test.ensureTrue(cache.get("1") == null, "");
    Test.ensureNotNull(cache.get("2"), "");

    // knock "3" out of cache
    cache.put("7", "77");
    Test.ensureEqual(cache.size(), 5, "");
    Test.ensureTrue(cache.get("3") == null, "");
    Test.ensureNotNull(cache.get("4"), "");

    String2.log("LRUCache.basicTest finished");
  }
}
