/*
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

import java.beans.*;

/**
 *    BeanInfo for <code>Pane</code>.
 */
public class PaneBeanInfo extends SimpleBeanInfo {
  // getAdditionalBeanInfo method allows to return any number of additional
  // BeanInfo objects which provide information about the Bean that this BeanInfo
  // describes.
  public BeanInfo[] getAdditionalBeanInfo() {
    try {
      BeanInfo[] bi = new BeanInfo[1];
      bi[0] = Introspector.getBeanInfo(beanClass.getSuperclass());
      return bi;
    }
    catch (IntrospectionException e) {
      throw new Error(e.toString());
    }
  }

  // getIcon returns an image object which can be used by toolboxes, toolbars
  // to represent this bean. Icon images are in GIF format.
  public java.awt.Image getIcon(int iconKind) {
    if (iconKind == BeanInfo.ICON_COLOR_16x16 ||
        iconKind == BeanInfo.ICON_MONO_16x16) {
      java.awt.Image img = loadImage("PaneIcon16.gif");
      return img;
    }
    if (iconKind == BeanInfo.ICON_COLOR_32x32 ||
        iconKind == BeanInfo.ICON_MONO_32x32) {
      java.awt.Image img = loadImage("PaneIcon32.gif");
      return img;
    }
    return null;
  }

  // getPropertyDescriptors returns an array of PropertyDescriptors which describe
  // the editable properties of this bean.
  public PropertyDescriptor[] getPropertyDescriptors() {
    try {
      PropertyDescriptor id = new PropertyDescriptor("id", beanClass);
      id.setBound(false);
      id.setDisplayName("Pane Identifier");

      PropertyDescriptor rv[] = {id};
      return rv;
    } catch (IntrospectionException e) {
      throw new Error(e.toString());
    }
  }

  public BeanDescriptor getBeanDescriptor() {
    return new BeanDescriptor(beanClass);
  }

  private final static Class beanClass = Pane.class;
}
