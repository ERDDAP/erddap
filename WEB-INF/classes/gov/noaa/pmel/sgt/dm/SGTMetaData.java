/**
 * $Id: SGTMetaData.java,v 1.3 2001/02/06 00:47:25 dwd Exp $
 *
 * <p>This software is provided by NOAA for full, free and open release. It is understood by the
 * recipient/user that NOAA assumes no liability for any errors contained in the code. Although this
 * software is released without conditions or restrictions in its use, it is expected that
 * appropriate credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an element in other product
 * development.
 */
package gov.noaa.pmel.sgt.dm;

import gov.noaa.pmel.util.GeoDate;
import java.util.Properties;

/**
 * MetaData container for the sgt datamodel.
 *
 * @author Donald Denbo
 * @version $Revision: 1.3 $, $Date: 2001/02/06 00:47:25 $
 * @since 1.0
 */
public class SGTMetaData implements java.io.Serializable {
  private String name_;
  private String units_;
  private boolean modulo_;
  private double moduloValue_ = 0.0;
  private GeoDate moduloTime_ = null;
  private boolean reversed_;
  private Properties props_ = null;

  /** Default constructor. */
  public SGTMetaData() {
    this("", "");
  }

  /**
   * SGTMetaData constructor.
   *
   * @param name variable name
   * @param units units of variable
   */
  public SGTMetaData(String name, String units) {
    this(name, units, false, false);
  }

  public SGTMetaData(String name, String units, boolean rev, boolean mod) {
    name_ = name;
    units_ = units;
    reversed_ = rev;
    modulo_ = mod;
  }

  /** Get the name associated with the variable or coordinate. */
  public String getName() {
    return name_;
  }

  /**
   * Axis values are reversed. This axis defines a left-hand coordinate system. For example,
   * northward, eastward, downward, is left-handed.
   */
  public boolean isReversed() {
    return reversed_;
  }

  /** Axis values are modulo. For example, 0 and 360 longitude are equivalent values. */
  public boolean isModulo() {
    return modulo_;
  }

  /** Set the modulo value. For example, 360 for longitude. */
  public void setModuloValue(double val) {
    moduloValue_ = val;
  }

  /** Set temporal modulo value. For example, 365 days, for yearly climatologies. */
  public void setModuloTime(GeoDate val) {
    moduloTime_ = val;
  }

  /** Get modulo value. */
  public double getModuloValue() {
    return moduloValue_;
  }

  /** Get temporal modulo value */
  public GeoDate getModuloTime() {
    return moduloTime_;
  }

  /** Set name of coordinate or variable */
  public void setName(String name) {
    name_ = name;
  }

  /** Set units of coordinate or variable */
  public void setUnits(String units) {
    units_ = units;
  }

  /** Set additional properties for the coordinate or variable. */
  public void setProperties(Properties props) {
    props_ = props;
  }

  /** Get variable or coordinate additional properties. */
  public Properties getProperties() {
    return props_;
  }

  /** Get property value given the key. */
  public String getProperty(String key) {
    return getProperty(key, "");
  }

  /** Get property value given the key, if key is not defined use the default value. */
  public String getProperty(String key, String defValue) {
    if (props_ != null) {
      return props_.getProperty(key, defValue);
    } else {
      return null;
    }
  }

  /** Set a property for the variable or coordinate. */
  public void setProperty(String key, String value) {
    if (props_ == null) {
      props_ = new Properties();
    }
    props_.put(key, value);
  }

  /** Get variable or coordinate units. */
  public String getUnits() {
    return units_;
  }
}
