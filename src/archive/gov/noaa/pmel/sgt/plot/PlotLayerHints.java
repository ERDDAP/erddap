/*
 * $Id: PlotLayerHints.java,v 1.3 2003/08/22 23:02:39 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */
package gov.noaa.pmel.sgt.plot;

import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @since 2.x
 */
public class PlotLayerHints implements Map, Cloneable {
  HashMap hintmap = new HashMap(5);

  /**
   * PlotKey Type hint key
   */
  public static final String KEY_PLOTKEY_TYPE = "PlotKeyType";
  /**
   * PlotKey Type hint values -- on layer
   */
  public static final String VALUE_PLOTKEY_ON_LAYER = "OnLayer";
  /**
   * PlotKey Type hint values -- on seperate layer
   */
  public static final String VALUE_PLOTKEY_ON_SEPERATE_LAYER = "OnSeperateLayer";
  /**
   * PlotKey Type hint values -- in pop-up window
   */
  public static final String VALUE_PLOTKEY_IN_POPUP = "InPopUp";
  /**
   * PlotKey Type hint values -- in JTable
   */
  public static final String VALUE_PLOTKEY_IN_TABLE = "InTable";
  /**
   * PlotKey Type hint values -- no key
   */
  public static final String VALUE_PLOTKEY_NONE = "None";

  /**
   * PlotKey location hint key
   */
  public static final String KEY_PLOTKEY_LOCATION = "PlotKeyLocation";

  /**
   * Layer Placement hint key
   */
  public static final String KEY_LAYER_PLACEMENT = "LayerPlacement";
  /**
   * Layer Placement hint values -- overlay
   */
  public static final String VALUE_LAYER_PLACEMENT_OVERLAY = "Overlay";

  /**
   * AspectRatio hint key
   */
  public static final String KEY_ASPECT_RATIO = "AspectRatio";
  /**
   * AspecRatio hint values -- lock X and Y scales
   */
  public static final String VALUE_ASPECT_RATIO_LOCK = "Lock";
  /**
   * AspecRatio hint values -- dont lock X and Y scales (during resize)
   */
  public static final String VALUE_ASPECT_RATIO_NO_LOCK = "NoLock";

  /**
   * Axis location should actually go through a series of steps
   *
   * X Axis Location
   * 1) bottom of plot region
   * 2) top of plot region
   * 3) below bottom axis (increase border if needed)
   * 4) above top axis (increase border if needed)
   *
   * Y Axis Locations
   * 1) left of plot region
   * 2) right of plot region
   * 3) outside left axis (increase border if needed)
   * 4) outside right axis (increase border if needed)
   *
   *
   * X Axis Location hint key
   */
  public static final String KEY_X_AXIS_LOCATION = "XAxisLocation";
  /**
   * X Axis Location hint values -- default
   * First try bottom, top, below bottom, then above top
   */
  public static final String VALUE_X_AXIS_LOCATION_DEFAULT = "Default";
  /**
   * X Axis Location hint values -- bottom
   */
  public static final String VALUE_X_AXIS_LOCATION_BOTTOM = "Bottom";
  /**
   * X Axis Location hint values -- top
   */
  public static final String VALUE_X_AXIS_LOCATION_TOP = "Top";

  /**
   * Y Axis Location hint key
   */
  public static final String KEY_Y_AXIS_LOCATION = "YAxis Location";
  /**
   * Y Axis Location hint values -- default
   * First try left, right, outside right, then outside left
   */
  public static final String VALUE_Y_AXIS_LOCATION_DEFAULT = "Default";
  /**
   * Y Axis Location hint values -- left
   */
  public static final String VALUE_Y_AXIS_LOCATION_LEFT = "Left";
  /**
   * Y Axis Location hint values -- right
   */
  public static final String VALUE_Y_AXIS_LOCATION_RIGHT = "Right";

  /**
   * Decision to create a new transform or re-use an existing
   * transform should follow the following steps
   *
   * 1) Use transform from same LayerStack
   * 2) Use transform from same JPlotPane
   * 3) Create a new transform
   *
   * to use existing transform
   * 1) both must be space or both time (test cant be defeated)
   * 2) must have units that are convertable to existing transform
   * 3) must have identical units
   *
   *
   * X Transform hint key
   */
  public static final String KEY_X_TRANSFORM = "XTransform";
  /**
   * X Transform hint values -- default
   * First try LayerStack, JPlotPane, then create new transform
   */
  public static final String VALUE_X_TRANSFORM_DEFAULT = "Default";
  /**
   * X Transform hint values -- new
   */
  public static final String VALUE_X_TRANSFORM_NEW = "New";
  /**
   * X Transform hint values -- use JPlotPane
   */
  public static final String VALUE_X_TRANSFORM_USEPLOTPANE = "UsePlotPane";

  /**
   * Y Transform hint key
   */
  public static final String KEY_Y_TRANSFORM = "YTransform";
  /**
   * Y Transform hint values -- default
   * First try LayerStack, JPlotPane, then create new transform
   */
  public static final String VALUE_Y_TRANSFORM_DEFAULT = "Default";
  /**
   * Y Transform hint values -- new
   */
  public static final String VALUE_Y_TRANSFORM_NEW = "New";
  /**
   * Y Transform hint values -- use JPlotPane
   */
  public static final String VALUE_Y_TRANSFORM_USEPLOTPANE = "UsePlotPane";

  public PlotLayerHints(Map init) {
    if(init != null) {
      hintmap.putAll(init);
    }
  }

  public PlotLayerHints(String key, String value) {
    hintmap.put(key, value);
  }

  public int size() {
    return hintmap.size();
  }

  public boolean isEmpty() {
    return hintmap.isEmpty();
  }

  public boolean containsKey(Object key) {
    return hintmap.containsKey((String)key);
  }

  public boolean containsValue(Object value) {
    return hintmap.containsValue((String)value);
  }

  public Object get(Object key) {
    return hintmap.get((String)key);
  }

  public Object put(Object key, Object value) {
    return hintmap.put((String) key, (String)value);
  }

  public void add(PlotLayerHints hints) {
    hintmap.putAll(hints.hintmap);
  }

  public void clear() {
    hintmap.clear();
  }

  public Object remove(Object key) {
    return hintmap.remove((String)key);
  }

  public void putAll(Map m) {
    if(m instanceof PlotLayerHints) {
      hintmap.putAll(((PlotLayerHints)m).hintmap);
    } else {
      // Funnel each key/value pair though our method
      Iterator iter = m.entrySet().iterator();
      while(iter.hasNext()) {
        Map.Entry entry = (Map.Entry) iter.next();
        put(entry.getKey(), entry.getValue());
      }
    }
  }

  public Set keySet() {
    return hintmap.keySet();
  }

  public Collection values() {
    return hintmap.values();
  }

  public Set entrySet() {
    return Collections.unmodifiableMap(hintmap).entrySet();
  }

  public boolean equals(Object o) {
    if(o instanceof PlotLayerHints) {
      return hintmap.equals(((PlotLayerHints)o).hintmap);
    } else if(o instanceof Map) {
      return hintmap.equals(o);
    }
    return false;
  }

  public int hashCode() {
    return hintmap.hashCode();
  }

  public Object clone() {
    PlotLayerHints plh;
    try {
      plh = (PlotLayerHints) super.clone();
      if(hintmap != null) {
        plh.hintmap = (HashMap) hintmap.clone();
      }
    } catch (CloneNotSupportedException e) {
      // this shouldnt happend since we are Cloneable
      throw new InternalError();
    }
    return plh;
  }

  public String toString() {
    if(hintmap == null) {
      return getClass().getName() +
          "@" +
          Integer.toHexString(hashCode()) +
          " (0 hints)";
    }
    return hintmap.toString();
  }

}
