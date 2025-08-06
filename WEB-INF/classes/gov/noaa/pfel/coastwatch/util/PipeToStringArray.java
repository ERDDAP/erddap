/*
 * PipeToStringArray Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.util.MustBe;
import com.cohort.util.String2;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is used by SSR.cShell and dosShell to create a separate thread to grab info from the
 * out or err inputStream from runtime.exec(). The basic idea is from
 * http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html, but the code here is very
 * different.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-02-18
 */
public class PipeToStringArray extends PipeTo {
  private final ArrayList<String> arrayList = new ArrayList<>();

  /**
   * This method grabs all the info from inputStream and stores it in an internal ArrayList. The run
   * method is the standard starting place when the thread is run.
   */
  @Override
  public void run() {
    BufferedReader bufferedReader =
        new BufferedReader(
            new InputStreamReader(
                inputStream, StandardCharsets.UTF_8)); // uses default charset for this OS
    try {
      String s;
      while ((s = bufferedReader.readLine()) != null) {
        arrayList.add(s);
        // String2.log("PipeToStringArray: " + s);
      }
    } catch (Exception e) {
      String2.log(MustBe.throwableToString(e));
    }
  }

  /**
   * Print a message to the storage container.
   *
   * @param message
   */
  @Override
  public void print(String message) {
    String ar[] = String2.splitNoTrim(message, '\n');
    arrayList.addAll(Arrays.asList(ar));
  }

  /**
   * This returns the ArrayList with the info from the inputStream.
   *
   * @return the ArrayList with the info from the inputStream
   */
  public List<String> getArrayList() {
    return arrayList;
  }

  /**
   * This returns a String with the info from the inputStream.
   *
   * @return a String with the info from the inputStream
   */
  public String getString() {
    return String2.toNewlineString(arrayList.toArray(new String[0]));
  }
}
