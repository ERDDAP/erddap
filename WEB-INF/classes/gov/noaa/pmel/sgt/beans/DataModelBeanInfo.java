package gov.noaa.pmel.sgt.beans;

import java.beans.*;

/**
 * BeanInfo object for <code>DataModel</code>.
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2003/08/22 23:02:34 $
 * @since 3.0
 **/
public class DataModelBeanInfo extends SimpleBeanInfo {
  private Class beanClass = DataModel.class;
  private String iconColor16x16Filename = "DataModelIcon16.gif";
  private String iconColor32x32Filename = "DataModelIcon32.gif";
  private String iconMono16x16Filename;
  private String iconMono32x32Filename;

  public DataModelBeanInfo() {
  }
  public PropertyDescriptor[] getPropertyDescriptors() {
    try {
      PropertyDescriptor _page = new PropertyDescriptor("page", beanClass, "getPage", "setPage");
      PropertyDescriptor[] pds = new PropertyDescriptor[] {
        _page};
      return pds;
    }
    catch(IntrospectionException ex) {
      ex.printStackTrace();
      return null;
    }
  }
  public java.awt.Image getIcon(int iconKind) {
    switch (iconKind) {
      case BeanInfo.ICON_COLOR_16x16:
        return iconColor16x16Filename != null ? loadImage(iconColor16x16Filename) : null;
      case BeanInfo.ICON_COLOR_32x32:
        return iconColor32x32Filename != null ? loadImage(iconColor32x32Filename) : null;
      case BeanInfo.ICON_MONO_16x16:
        return iconMono16x16Filename != null ? loadImage(iconMono16x16Filename) : null;
      case BeanInfo.ICON_MONO_32x32:
        return iconMono32x32Filename != null ? loadImage(iconMono32x32Filename) : null;
    }
    return null;
  }
}