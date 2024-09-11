/*
 * PipeToStringArray Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.util.MustBe;
import com.cohort.util.String2;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * This class is used by SSR.cShell and dosShell to create a separate thread to grab info from the
 * out or err inputStream from runtime.exec(). The basic idea is from
 * http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html, but the code here is very
 * different.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-02-18
 */
public class PipeToStringArray extends PipeTo {
  private BufferedReader bufferedReader;
  private ArrayList<String> arrayList = new ArrayList();

  /**
   * This method grabs all the info from inputStream and stores it in an internal ArrayList. The run
   * method is the standard starting place when the thread is run.
   */
  @Override
  public void run() {
    bufferedReader =
        new BufferedReader(new InputStreamReader(inputStream)); // uses default charset for this OS
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
    for (int i = 0; i < ar.length; i++) arrayList.add(ar[i]);
  }

  /**
   * This returns the ArrayList with the info from the inputStream.
   *
   * @return the ArrayList with the info from the inputStream
   */
  public ArrayList<String> getArrayList() {
    return arrayList;
  }

  /**
   * This returns the StringArray with the info from the inputStream.
   *
   * @return the StringArray with the info from the inputStream
   */
  public String[] getStringArray() {
    return arrayList.toArray(new String[0]);
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
