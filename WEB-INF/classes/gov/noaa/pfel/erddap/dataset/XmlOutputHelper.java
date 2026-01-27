/*
 * XmlOutputHelper Copyright 2026, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.File2;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class XmlOutputHelper {
  
  public static void writeToSingleFile(String xml, String path) throws Exception {
    File2.appendFile(path, xml);
  }
  
  public static void writeWithXInclude(String xml, String id, 
                                       String dir, String main) throws Exception {
    Path d = Paths.get(dir);
    if (!Files.exists(d)) {
      Files.createDirectories(d);
    }
    
    String file = dir + "/" + id.replaceAll("[^a-zA-Z0-9._-]", "_") + ".xml";
    File2.writeToFile(file, xml, File2.UTF_8);
    
    String rel = Paths.get(main).getParent()
        .relativize(Paths.get(file)).toString();
    String inc = "  <xi:include href=\"" + rel + "\" xmlns:xi=\"http://www.w3.org/2001/XInclude\" />\n";
    File2.appendFile(main, inc);
  }
}
