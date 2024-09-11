/*
 * $Id: PanelModelBeanInfo.java,v 1.5 2003/09/18 17:31:44 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt.beans;

import java.beans.*;

/**
 * @author Donald Denbo
 * @version $Revision: 1.5 $, $Date: 2003/09/18 17:31:44 $
 * @since 3.0
 */
public class PanelModelBeanInfo extends SimpleBeanInfo {
  private Class beanClass = PanelModel.class;
  private Class customizerClass = PanelModelCustomizer.class;
  private String iconColor16x16Filename = "PanelModelIcon16.gif";
  private String iconColor32x32Filename = "PanelModelIcon32.gif";
  private String iconMono16x16Filename;
  private String iconMono32x32Filename;

  public PanelModelBeanInfo() {
  }

  public BeanDescriptor getBeanDescriptor() {
    return new BeanDescriptor(beanClass, customizerClass);
  }

  public PropertyDescriptor[] getPropertyDescriptors() {
    try {
      PropertyDescriptor _batch = new PropertyDescriptor("batch", beanClass, null, null);
      _batch.setValue("transient", Boolean.TRUE);
      PropertyDescriptor _dpi = new PropertyDescriptor("dpi", beanClass, "getDpi", "setDpi");
      PropertyDescriptor _modified = new PropertyDescriptor("modified", beanClass, null, null);
      _modified.setValue("transient", Boolean.TRUE);
      PropertyDescriptor _page = new PropertyDescriptor("page", beanClass, "getPage", "setPage");
      _page.setValue("transient", Boolean.TRUE);
      PropertyDescriptor _panelList = new PropertyDescriptor("panelList", beanClass, "getPanelList", "setPanelList");
      PropertyDescriptor _panelCount = new PropertyDescriptor("panelCount", beanClass, "getPanelCount", null);
      PropertyDescriptor _pageSize = new PropertyDescriptor("pageSize", beanClass, "getPageSize", "setPageSize");
      PropertyDescriptor _pageBackgroundColor =
          new PropertyDescriptor("pageBackgroundColor", beanClass, "getPageBackgroundColor", "setPageBackgroundColor");
      PropertyDescriptor _printWhitePage =
          new PropertyDescriptor("printWhitePage", beanClass, "isPrintWhitePage", "setPrintWhitePage");
      PropertyDescriptor _printBorders =
          new PropertyDescriptor("printBorders", beanClass, "isPrintBorders", "setPrintBorders");
      PropertyDescriptor _pageHAlign =
          new PropertyDescriptor("printHAlign", beanClass, "getPrintHAlign", "setPrintHAlign");
      PropertyDescriptor _pageVAlign =
          new PropertyDescriptor("printVAlign", beanClass, "getPrintVAlign", "setPrintVAlign");
      PropertyDescriptor _printMode =
          new PropertyDescriptor("printScaleMode", beanClass, "getPrintScaleMode", "setPrintScaleMode");
      PropertyDescriptor _pageOrigin =
          new PropertyDescriptor("printOrigin", beanClass, "getPrintOrigin", "setPrintOrigin");
      PropertyDescriptor[] pds = new PropertyDescriptor[] {
        _batch,
        _dpi,
        _modified,
        _page,
        _pageBackgroundColor,
        _pageSize,
        _panelList,
        _panelCount,
        _printWhitePage,
        _printBorders,
        _pageHAlign,
        _pageVAlign,
        _printMode,
        _pageOrigin};
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