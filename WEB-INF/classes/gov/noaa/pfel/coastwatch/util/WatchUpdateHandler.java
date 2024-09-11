package gov.noaa.pfel.coastwatch.util;

import com.cohort.array.StringArray;

public interface WatchUpdateHandler {
  public void doReload();

  public void handleUpdates(StringArray contexts) throws Throwable;
}
