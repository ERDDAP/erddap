/*
 * $Id: SimpleFileFilter.java,v 1.2 2003/08/22 23:02:40 dwd Exp $
 */
package gov.noaa.pmel.util;

import java.io.File;
import javax.swing.filechooser.*;

public class SimpleFileFilter extends FileFilter {
  private String[] extensions;
  private String description;

  public SimpleFileFilter(String ext) {
    this(new String[] {ext}, null);
  }

  public SimpleFileFilter(String[] exts, String descr) {
    extensions = new String[exts.length];
    for (int i = exts.length - 1; i >= 0; i--) {
      extensions[i] = exts[i].toLowerCase();
    }
    description = (descr == null ? exts[0] + " files" : descr);
  }

  @Override
  public boolean accept(File f) {
    if (f.isDirectory()) {
      return true;
    }
    String name = f.getName().toLowerCase();
    for (int i = extensions.length - 1; i >= 0; i--) {
      if (name.endsWith(extensions[i])) {
        return true;
      }
    }
    return false;
  }

  public boolean hasExtension(String ext) {
    for (int i = 0; i < extensions.length; i++) {
      if (extensions[i].equals(ext)) return true;
    }
    return false;
  }

  public String getExtension() {
    return getExtension(0);
  }

  public String getExtension(int index) {
    int idx = index;
    if (idx < 0 || idx >= extensions.length) idx = 0;
    return extensions[idx];
  }

  @Override
  public String getDescription() {
    return description;
  }
}
