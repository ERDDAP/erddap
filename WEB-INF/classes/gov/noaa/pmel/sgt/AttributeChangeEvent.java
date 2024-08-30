/*
 * $Id: AttributeChangeEvent.java,v 1.3 2003/08/22 23:02:31 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */
package gov.noaa.pmel.sgt;

import java.beans.PropertyChangeEvent;

/**
 * A class for wrapping local and remote property change events for attributes.
 *
 * @author Donald Denbo
 * @version $Revision: 1.3 $
 * @since 3.0
 */
public class AttributeChangeEvent extends PropertyChangeEvent {
  private boolean local_ = true;

  public AttributeChangeEvent(
      Object source, String propertyName, Object oldValue, Object newValue) {
    this(source, propertyName, oldValue, newValue, true);
  }

  public AttributeChangeEvent(
      Object source, String propertyName, Object oldValue, Object newValue, boolean local) {
    super(source, propertyName, oldValue, newValue);
    local_ = local;
  }

  public boolean isLocal() {
    return local_;
  }
}
