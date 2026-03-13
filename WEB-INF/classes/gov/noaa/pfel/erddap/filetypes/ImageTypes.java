package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.OutputStreamSourceSimple;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public abstract class ImageTypes extends CacheLockFiles {

  public ImageTypes() {
    super(false);
  }

  @Override
  protected void generateTableFile(DapRequestInfo requestInfo, String cacheFullName)
      throws Throwable {
    generateFile(requestInfo, cacheFullName, false);
  }

  @Override
  protected void generateGridFile(DapRequestInfo requestInfo, String cacheFullName)
      throws Throwable {
    generateFile(requestInfo, cacheFullName, true);
  }

  private void generateFile(DapRequestInfo requestInfo, String cacheFullName, boolean isGrid)
      throws Throwable {
    // create random file; and if error, only partial random file will be created
    int random = Math2.random(Integer.MAX_VALUE);
    OutputStream fos =
        new BufferedOutputStream(Files.newOutputStream(Paths.get(cacheFullName + random)));
    boolean ok;
    try {
      OutputStreamSourceSimple osss = new OutputStreamSourceSimple(fos);
      if (isGrid) {
        ok = gridToImage(requestInfo, osss);
      } else {
        ok = tableToImage(requestInfo, osss);
      }
    } finally {
      if (fos != null) {
        try {
          fos.close();
        } catch (Exception e) {
          String2.log("Error closing stream, log and continue: " + e.getMessage());
        }
      }
    }
    File2.rename(cacheFullName + random, cacheFullName);
    if (!ok) // make eligible to be removed from cache in 5 minutes
    File2.touch(
          cacheFullName,
          Math.max(0, EDStatic.config.cacheMillis - 5 * Calendar2.MILLIS_PER_MINUTE));

    File2.isFile(
        cacheFullName,
        5); // for possible waiting thread, wait till file is visible via operating system
  }

  protected abstract boolean tableToImage(DapRequestInfo requestInfo, OutputStreamSourceSimple osss)
      throws Throwable;

  protected abstract boolean gridToImage(DapRequestInfo requestInfo, OutputStreamSourceSimple osss)
      throws Throwable;
}
