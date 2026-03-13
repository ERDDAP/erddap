package gov.noaa.pfel.erddap.dataset.metadata;

import java.util.Locale;
import java.util.Map;
import org.opengis.util.InternationalString;

public record EDDInternationalString(String string, Map<Locale, String> localized)
    implements InternationalString {

  public EDDInternationalString(String string) {
    this(string, null);
  }

  @Override
  public int length() {
    return string.length();
  }

  @Override
  public char charAt(int index) {
    return string.charAt(index);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return string.subSequence(start, end);
  }

  @Override
  public int compareTo(InternationalString o) {
    return string.compareTo(o.toString());
  }

  @Override
  public String toString(Locale locale) {
    if (localized != null && localized.containsKey(locale)) {
      return localized.get(locale);
    }
    return string;
  }

  @Override
  public String toString() {
    return string;
  }
}
