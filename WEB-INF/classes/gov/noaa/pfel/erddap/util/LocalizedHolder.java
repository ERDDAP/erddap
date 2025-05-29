package gov.noaa.pfel.erddap.util;

import java.util.ArrayList;
import java.util.List;

public class LocalizedHolder<T> {
  // Hold localized information, allow requesting information, and returning localized if available,
  // otherwise return the default.

  private T defaultValue;
  private List<T> localizedValues = new ArrayList<>();

  public LocalizedHolder(T defaultValue) {
    this.defaultValue = defaultValue;
  }

  public void set(int language, T value) {
    if (language == EDMessages.DEFAULT_LANGUAGE || language < 0) {
      defaultValue = value;
    } else {
      safeSetLangValue(language, value);
    }
  }

  public T get(int language) {
    T langValue = safeGetLangValue(language);
    if (langValue != null) {
      return langValue;
    }
    return defaultValue;
  }

  private T safeGetLangValue(int language) {
    if (localizedValues.size() > language) {
      return localizedValues.get(language);
    }
    return null;
  }

  private void safeSetLangValue(int language, T value) {
    if (localizedValues.size() > language) {
      localizedValues.set(language, value);
    }
    for (int i = 0; i < language; i++) {
      if (localizedValues.size() <= i) {
        localizedValues.add(null);
      }
    }
    localizedValues.add(value);
  }
}
