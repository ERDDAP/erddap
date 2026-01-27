/*
 * JsonInputHelper Copyright 2026, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import org.json.JSONObject;

public class JsonInputHelper {
  
  public static String getString(JSONObject json, String key, String def) {
    return json.optString(key, def);
  }
  
  public static int getInt(JSONObject json, String key, int def) {
    return json.optInt(key, def);
  }
  
  public static boolean getBoolean(JSONObject json, String key, boolean def) {
    return json.optBoolean(key, def);
  }
}
