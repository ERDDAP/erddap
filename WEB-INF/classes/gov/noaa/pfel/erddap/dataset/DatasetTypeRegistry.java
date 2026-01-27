/*
 * DatasetTypeRegistry Copyright 2026, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class DatasetTypeRegistry {
  private static final Map<String, Method> registry = new ConcurrentHashMap<>();
  
  public static void register(String type, Class<?> cls, String method) {
    try {
      for (Method m : cls.getMethods()) {
        if (m.getName().equals(method) && 
            java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
          registry.put(type, m);
          return;
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to register " + type, e);
    }
  }
  
  public static boolean isRegistered(String type) {
    return registry.containsKey(type);
  }
  
  public static Method getGenerator(String type) {
    return registry.get(type);
  }
}
