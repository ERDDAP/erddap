/*
 * $Id: MRJUtil.java,v 1.3 2003/09/15 16:48:49 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.swing;

import javax.swing.UIManager;

/**
 * Uility methods for dealing with Aqua interfaces
 *
 * @author Donald Denbo
 * @version $Revision: 1.3 $, $Date: 2003/09/15 16:48:49 $
 * @since 3.0
 */
public class MRJUtil {

  public MRJUtil() {}

  /**
   * MacOS Look and feel test
   *
   * @return true if using Aqua Look n' Feel
   */
  public static boolean isAquaLookAndFeel() {
    return System.getProperty("mrj.version") != null
        && UIManager.getSystemLookAndFeelClassName()
            .equals(UIManager.getLookAndFeel().getClass().getName());
  }

  /** MacOS Java version test */
  public static boolean fixFontMetrics() {
    String rtVer = System.getProperty("java.runtime.version");
    return System.getProperty("mrj.version") != null
        && (rtVer.equals("1.4.1_01-39") || rtVer.equals("1.4.1_01-69.1"));
  }
}
