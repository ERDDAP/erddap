package gov.noaa.pfel.erddap.dataset.metadata;

import java.util.Locale;
import org.opengis.util.InternationalString;

public class EDDInternationalString implements InternationalString {

  private String string;

  public EDDInternationalString(String string) {
    this.string = string;
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
    return string;
  }

  @Override
  public String toString() {
    return string;
  }
}
