/*
 * $Id: Units.java,v 1.5 2001/02/09 18:42:31 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package  gov.noaa.pmel.util;

import gov.noaa.pmel.sgt.dm.SGTMetaData;
import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.sgt.dm.SGTLine;
import gov.noaa.pmel.sgt.dm.SGTGrid;
import gov.noaa.pmel.sgt.dm.SimpleLine;
import gov.noaa.pmel.sgt.dm.SimpleGrid;

import java.lang.reflect.Constructor;

/**
 * Units is a static class for converting the units of DependentVariables.
 *
 * @author Donald Denbo
 * @version $Revision: 1.5 $, $Date: 2001/02/09 18:42:31 $
 * @since sgt 1.0
**/
public class Units implements java.io.Serializable {
  /** No base units selected  */
  public static final int NONE = 0;
  /** base units are temperature  */
  public static final int TEMPERATURE = 1;
  /** base units are velocity  */
  public static final int VELOCITY = 2;
  /** base units are distance  */
  public static final int DISTANCE = 3;
  /** check X MetaData for units  */
  public static final int X_AXIS = 0;
  /** check Y MetaData for units  */
  public static final int Y_AXIS = 1;
  /** check Z MetaData for units */
  public static final int Z_AXIS = 2;
  /**
   * Return the base unit for the meta data.
   *
   * @return TEMPERATURE, VELOCITY, DISTANCE, or NONE
   */
  public static int getBaseUnit(SGTMetaData meta) {
    if(Temperature.isBaseUnit(meta)) {
      return TEMPERATURE;
    } else if(Velocity.isBaseUnit(meta)) {
      return VELOCITY;
    } else if(Distance.isBaseUnit(meta)) {
      return DISTANCE;
    } else {
      return NONE;
    }
  }
  /**
   * Convert data to common units.
   *
   * @param grid the data
   * @param base the base units, TEMPERATURE, VELOCITY, DISTANCE, or NONE
   * @param comp grid component, X_AXIS, Y_AXIS, or Z_AXIS
   */
  public static SGTData convertToBaseUnit(SGTData grid, int base, int comp) {
    switch(base) {
      case TEMPERATURE: return Temperature.convertToBaseUnit(grid, comp);
      case VELOCITY: return Velocity.convertToBaseUnit(grid, comp);
      case DISTANCE: return Distance.convertToBaseUnit(grid, comp);
      default:
      case NONE: return grid;
    }
  }
}
/**
 * Supports temperature conversions.
 */
class Temperature implements java.io.Serializable {
  //
  // All conversions are to the default units of degC
  //
  // degC = scale*origUnits + offset
  //
  private static final String[] name =
  {"C", "degC", "K", "degK", "F", "degF", "k", "deg_c", "deg_k"};
  private static final double[] scale =
  {1.0, 1.0, 1.0, 1.0, 5.0/9.0, 5.0/9.0, 1.0, 1.0, 1.0};
  private static final double[] offset =
  {0.0, 0.0, -273.15, -273.15, 32.0*5.0/9.0, 32.0*5.0/9.0, -273.15, 0.0, -273.15};
  //
  private static final String timeArrayName = "[Lgov.noaa.pmel.util.GeoDate";
  private static final String doubleArrayName = "[D";
  private static final String stringName = "java.lang.String";
  /**
   *
   */
  static SGTData convertToBaseUnit(SGTData grid, int comp) {
    int unit, count;
    boolean hand;
    //    GeoVariable xDim, yDim, zDim;
    //    TimeVariable tDim;
    //    DependentVariable variable, new_variable;
    //    VarDesc new_vardesc;
    SGTData new_grid = null;
    SGTMetaData meta, newMeta;
    int projection;
    
    String units;
    switch(comp) {
    default:
    case Units.X_AXIS:
      meta = ((SGTLine)grid).getXMetaData();
      break;
    case Units.Y_AXIS:
      meta = ((SGTLine)grid).getYMetaData();
      break;
    case Units.Z_AXIS:
      meta = ((SGTGrid)grid).getZMetaData();
    }
    units = meta.getUnits();
    //
    // is it one of the changeable units?
    //
    for(unit = 0; unit < name.length; unit++) {
      if(units.equals(name[unit])) break;
    }
    
    if(unit >= name.length) return grid;
    //
    // is it already in the base units?
    //
    if(scale[unit] == 1.0 && offset[unit] == 0.0) return grid;
    //
    // convert the units
    //
    double values[], new_values[];
    switch(comp) {
    default:
    case Units.X_AXIS:
      values = ((SGTLine)grid).getXArray();
      break;
    case Units.Y_AXIS:
      values = ((SGTLine)grid).getYArray();
      break;
    case Units.Z_AXIS:
      values = ((SGTGrid)grid).getZArray();
    }
    new_values = new double[values.length];
    
    for(count=0; count < values.length; count++) {
      new_values[count] = scale[unit]*values[count] + offset[unit];
    }
    //
    // build the new dependent variable
    //
    newMeta = new SGTMetaData(meta.getName(), "degC", meta.isReversed(), meta.isModulo());
    newMeta.setModuloValue(meta.getModuloValue());
    newMeta.setModuloTime(meta.getModuloTime());

    if(grid instanceof SGTLine) {
      boolean simpleLine = grid instanceof SimpleLine;
      if(simpleLine) {
	new_grid = (SGTLine)grid.copy();
      }
      Class[] classArgs = new Class[3];
      Object[] constructArgs = new Object[3];
      Class gridClass = grid.getClass();
      Constructor gridConstruct;
      switch(comp) {
      default:
      case Units.X_AXIS:
	if(simpleLine) {
	  ((SimpleLine)new_grid).setXArray(new_values);
	} else {
	  if(((SGTLine)grid).isYTime()) {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(timeArrayName);
	      classArgs[2] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = new_values;
	      constructArgs[1] = ((SGTLine)grid).getTimeArray();
	      constructArgs[2] = ((SGTLine)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Temperature conversion: " + e);
	    }
	  } else {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(doubleArrayName);
	      classArgs[2] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = new_values;
	      constructArgs[1] = ((SGTLine)grid).getYArray();
	      constructArgs[2] = ((SGTLine)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Temperature conversion: " + e);
	    }
	  }
	  ((SimpleLine)new_grid).setYMetaData(((SGTLine)grid).getYMetaData());
	}
	((SimpleLine)new_grid).setXMetaData(newMeta);
	break;
      case Units.Y_AXIS:
	if(simpleLine) {
	  ((SimpleLine)new_grid).setYArray(new_values);
	} else {
	  if(((SGTLine)grid).isXTime()) {
	    try {
	      classArgs[0] = Class.forName(timeArrayName);
	      classArgs[1] = Class.forName(doubleArrayName);
	      classArgs[2] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = ((SGTLine)grid).getTimeArray();
	      constructArgs[1] = new_values;
	      constructArgs[2] = ((SGTLine)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Temperature conversion: " + e);
	    }
	  } else {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(doubleArrayName);
	      classArgs[2] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = ((SGTLine)grid).getXArray();
	      constructArgs[1] = new_values;
	      constructArgs[2] = ((SGTLine)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Temperature conversion: " + e);
	    }
	  }
	  ((SimpleLine)new_grid).setXMetaData(((SGTLine)grid).getXMetaData());
	}
	((SimpleLine)new_grid).setYMetaData(newMeta);
      }
    } else if(grid instanceof SGTGrid) {
      boolean simpleGrid = grid instanceof SimpleGrid;
      if(simpleGrid) {
	new_grid = (SGTGrid)grid.copy();
      }
      Class[] classArgs = new Class[4];
      Object[] constructArgs = new Object[4];
      Class gridClass = grid.getClass();
      Constructor gridConstruct;
      switch(comp) {
      default:
      case Units.X_AXIS:
	if(simpleGrid) {
	  ((SimpleGrid)new_grid).setXArray(new_values);
	} else {
	  if(((SGTGrid)grid).isYTime()) {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(doubleArrayName);
	      classArgs[2] = Class.forName(timeArrayName);
	      classArgs[3] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = ((SGTGrid)grid).getZArray();
	      constructArgs[1] = new_values;
	      constructArgs[2] = ((SGTGrid)grid).getTimeArray();
	      constructArgs[2] = ((SGTGrid)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Temperature conversion: " + e);
	    }
	  } else {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(doubleArrayName);
	      classArgs[2] = Class.forName(doubleArrayName);
	      classArgs[3] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = ((SGTGrid)grid).getZArray();
	      constructArgs[1] = new_values;
	      constructArgs[2] = ((SGTGrid)grid).getYArray();
	      constructArgs[2] = ((SGTGrid)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Temperature conversion: " + e);
	    }
	  }
	  ((SimpleGrid)new_grid).setYMetaData(((SGTGrid)grid).getYMetaData());
	  ((SimpleGrid)new_grid).setZMetaData(((SGTGrid)grid).getZMetaData());
	}
	((SimpleGrid)new_grid).setXMetaData(newMeta);
	break;
      case Units.Y_AXIS:
	if(simpleGrid) {
	  ((SimpleGrid)new_grid).setYArray(new_values);
	} else {
	  if(((SGTGrid)grid).isXTime()) {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(timeArrayName);
	      classArgs[2] = Class.forName(doubleArrayName);
	      classArgs[3] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = ((SGTGrid)grid).getZArray();
	      constructArgs[1] = ((SGTGrid)grid).getTimeArray();
	      constructArgs[2] = new_values;
	      constructArgs[2] = ((SGTGrid)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Temperature conversion: " + e);
	    }
	  } else {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(doubleArrayName);
	      classArgs[2] = Class.forName(doubleArrayName);
	      classArgs[3] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = ((SGTGrid)grid).getZArray();
	      constructArgs[1] = ((SGTGrid)grid).getXArray();
	      constructArgs[2] = new_values;
	      constructArgs[2] = ((SGTGrid)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Temperature conversion: " + e);
	    }
	  }
	  ((SimpleGrid)new_grid).setXMetaData(((SGTGrid)grid).getXMetaData());
	  ((SimpleGrid)new_grid).setZMetaData(((SGTGrid)grid).getZMetaData());
	}
	((SimpleGrid)new_grid).setYMetaData(newMeta);
	break;
      case Units.Z_AXIS:
	if(simpleGrid) {
	  ((SimpleGrid)new_grid).setZArray(new_values);
	} else {
	  if(((SGTGrid)grid).isXTime()) {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(timeArrayName);
	      classArgs[2] = Class.forName(doubleArrayName);
	      classArgs[3] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = new_values;
	      constructArgs[1] = ((SGTGrid)grid).getTimeArray();
	      constructArgs[2] = ((SGTGrid)grid).getYArray();
	      constructArgs[2] = ((SGTGrid)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Temperature conversion: " + e);
	    }
	  } else if(((SGTGrid)grid).isYTime()) {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(doubleArrayName);
	      classArgs[2] = Class.forName(timeArrayName);
	      classArgs[3] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = new_values;
	      constructArgs[1] = ((SGTGrid)grid).getXArray();
	      constructArgs[2] = ((SGTGrid)grid).getTimeArray();
	      constructArgs[2] = ((SGTGrid)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Temperature conversion: " + e);
	    }
	  } else {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(doubleArrayName);
	      classArgs[2] = Class.forName(doubleArrayName);
	      classArgs[3] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = new_values;
	      constructArgs[1] = ((SGTGrid)grid).getXArray();
	      constructArgs[2] = ((SGTGrid)grid).getYArray();
	      constructArgs[2] = ((SGTGrid)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Temperature conversion: " + e);
	    }
	  }
	  ((SimpleGrid)new_grid).setXMetaData(((SGTGrid)grid).getXMetaData());
	  ((SimpleGrid)new_grid).setYMetaData(((SGTGrid)grid).getYMetaData());
	}
	((SimpleGrid)new_grid).setZMetaData(newMeta);
      }
    }
    return new_grid;
  }
  
  static boolean isBaseUnit(SGTMetaData meta) {
    int count;
    String units = meta.getUnits();
    
    for(count=0; count < name.length; count++) {
      if(units.equals(name[count])) return true;
    }
    return false;
  }
}

/**
 * Supports distance conversions.
 */
class Distance implements java.io.Serializable {
  static SGTData convertToBaseUnit(SGTData grid, int comp) {
    return grid;
  }
  static boolean isBaseUnit(SGTMetaData meta) {
    return false;
  }
}

/**
 * Supports velocity conversions.
 */
class Velocity implements java.io.Serializable {
  //
  // All conversions are to the default units of "m s-1"
  //
  // m s-1 = scale*origUnits + offset
  //
  private static final String[] name =
  {"m s-1", "m/s", "cm s-1", "cm/s"};
  private static final double[] scale =
  {1.0, 1.0, 0.01, 0.01};
  private static final double[] offset =
  {0.0, 0.0, 0.0, 0.0};
  //
  private static final String timeArrayName = "[Lgov.noaa.pmel.util.GeoDate";
  private static final String doubleArrayName = "[D";
  private static final String stringName = "java.lang.String";
  /**
   *
   */
  static SGTData convertToBaseUnit(SGTData grid, int comp) {
    int unit, count;
    boolean hand;
    //    GeoVariable xDim, yDim, zDim;
    //    TimeVariable tDim;
    //    DependentVariable variable, new_variable;
    //    VarDesc new_vardesc;
    SGTData new_grid = null;
    SGTMetaData meta, newMeta;
    int projection;
    
    String units;
    switch(comp) {
    default:
    case Units.X_AXIS:
      meta = ((SGTLine)grid).getXMetaData();
      break;
    case Units.Y_AXIS:
      meta = ((SGTLine)grid).getYMetaData();
      break;
    case Units.Z_AXIS:
      meta = ((SGTGrid)grid).getZMetaData();
    }
    units = meta.getUnits();
    //
    // is it one of the changeable units?
    //
    for(unit = 0; unit < name.length; unit++) {
      if(units.equals(name[unit])) break;
    }
    
    if(unit >= name.length) return grid;
    //
    // is it already in the base units?
    //
    if(scale[unit] == 1.0 && offset[unit] == 0.0) return grid;
    //
    // convert the units
    //
    double values[], new_values[];
    switch(comp) {
    default:
    case Units.X_AXIS:
      values = ((SGTLine)grid).getXArray();
      break;
    case Units.Y_AXIS:
      values = ((SGTLine)grid).getYArray();
      break;
    case Units.Z_AXIS:
      values = ((SGTGrid)grid).getZArray();
    }
    new_values = new double[values.length];
    
    for(count=0; count < values.length; count++) {
      new_values[count] = scale[unit]*values[count] + offset[unit];
    }
    //
    // build the new dependent variable
    //
    newMeta = new SGTMetaData(meta.getName(), 
                              "m s-1", 
                              meta.isReversed(), 
                              meta.isModulo());
    newMeta.setModuloValue(meta.getModuloValue());
    newMeta.setModuloTime(meta.getModuloTime());

    if(grid instanceof SGTLine) {
      boolean simpleLine = grid instanceof SimpleLine;
      if(simpleLine) {
	new_grid = (SGTLine)grid.copy();
      }
      Class[] classArgs = new Class[3];
      Object[] constructArgs = new Object[3];
      Class gridClass = grid.getClass();
      Constructor gridConstruct;
      switch(comp) {
      default:
      case Units.X_AXIS:
	if(simpleLine) {
	  ((SimpleLine)new_grid).setXArray(new_values);
	} else {
	  if(((SGTLine)grid).isYTime()) {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(timeArrayName);
	      classArgs[2] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = new_values;
	      constructArgs[1] = ((SGTLine)grid).getTimeArray();
	      constructArgs[2] = ((SGTLine)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Velocity conversion: " + e);
	    }
	    //              new_grid = new SimpleLine(new_values, 
	    //                                      ((SGTLine)grid).getTimeArray(), 
	    //                                      ((SGTLine)grid).getTitle());
	  } else {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(doubleArrayName);
	      classArgs[2] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = new_values;
	      constructArgs[1] = ((SGTLine)grid).getYArray();
	      constructArgs[2] = ((SGTLine)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Velocity conversion: " + e);
	    }
	    //              new_grid = new SimpleLine(new_values, 
	    //                                      ((SGTLine)grid).getYArray(), 
	    //                                      ((SGTLine)grid).getTitle());
	  }
	  ((SimpleLine)new_grid).setYMetaData(((SGTLine)grid).getYMetaData());
	}
	((SimpleLine)new_grid).setXMetaData(newMeta);
	break;
      case Units.Y_AXIS:
	if(simpleLine) {
	  ((SimpleLine)new_grid).setYArray(new_values);
	} else {
	  if(((SGTLine)grid).isXTime()) {
	    try {
	      classArgs[0] = Class.forName(timeArrayName);
	      classArgs[1] = Class.forName(doubleArrayName);
	      classArgs[2] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = ((SGTLine)grid).getTimeArray();
	      constructArgs[1] = new_values;
	      constructArgs[2] = ((SGTLine)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Velocity conversion: " + e);
	    }
	    //              new_grid = new SimpleLine(((SGTLine)grid).getTimeArray(),
	    //                                        new_values,
	    //                                        ((SGTLine)grid).getTitle());
	  } else {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(doubleArrayName);
	      classArgs[2] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = ((SGTLine)grid).getXArray();
	      constructArgs[1] = new_values;
	      constructArgs[2] = ((SGTLine)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Velocity conversion: " + e);
	    }
	    //              new_grid = new SimpleLine(((SGTLine)grid).getXArray(),
	    //                                        new_values,
	    //                                        ((SGTLine)grid).getTitle());
	  }
	  ((SimpleLine)new_grid).setXMetaData(((SGTLine)grid).getXMetaData());
	}
	((SimpleLine)new_grid).setYMetaData(newMeta);
      }
    } else if(grid instanceof SGTGrid) {
      boolean simpleGrid = grid instanceof SimpleGrid;
      if(simpleGrid) {
	new_grid = (SGTGrid)grid.copy();
      }
      Class[] classArgs = new Class[4];
      Object[] constructArgs = new Object[4];
      Class gridClass = grid.getClass();
      Constructor gridConstruct;
      switch(comp) {
      default:
      case Units.X_AXIS:
	if(simpleGrid) {
	  ((SimpleGrid)new_grid).setXArray(new_values);
	} else {
          if(((SGTGrid)grid).isYTime()) {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(doubleArrayName);
	      classArgs[2] = Class.forName(timeArrayName);
	      classArgs[3] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = ((SGTGrid)grid).getZArray();
	      constructArgs[1] = new_values;
	      constructArgs[2] = ((SGTGrid)grid).getTimeArray();
	      constructArgs[2] = ((SGTGrid)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Velocity conversion: " + e);
	    }
//              new_grid = new SimpleGrid(((SGTGrid)grid).getZArray(),
//                                      new_values, 
//                                      ((SGTGrid)grid).getTimeArray(), 
//                                      ((SGTGrid)grid).getTitle());
          } else {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(doubleArrayName);
	      classArgs[2] = Class.forName(doubleArrayName);
	      classArgs[3] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = ((SGTGrid)grid).getZArray();
	      constructArgs[1] = new_values;
	      constructArgs[2] = ((SGTGrid)grid).getYArray();
	      constructArgs[2] = ((SGTGrid)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Velocity conversion: " + e);
	    }
//              new_grid = new SimpleGrid(((SGTGrid)grid).getZArray(),
//                                      new_values, 
//                                      ((SGTGrid)grid).getYArray(), 
//                                      ((SGTGrid)grid).getTitle());
          }
          ((SimpleGrid)new_grid).setYMetaData(((SGTGrid)grid).getYMetaData());
          ((SimpleGrid)new_grid).setZMetaData(((SGTGrid)grid).getZMetaData());
	}
	((SimpleGrid)new_grid).setXMetaData(newMeta);
	break;
      case Units.Y_AXIS:
	if(simpleGrid) {
	  ((SimpleGrid)new_grid).setYArray(new_values);
	} else {
          if(((SGTGrid)grid).isXTime()) {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(timeArrayName);
	      classArgs[2] = Class.forName(doubleArrayName);
	      classArgs[3] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = ((SGTGrid)grid).getZArray();
	      constructArgs[1] = ((SGTGrid)grid).getTimeArray();
	      constructArgs[2] = new_values;
	      constructArgs[2] = ((SGTGrid)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Velocity conversion: " + e);
	    }
//              new_grid = new SimpleGrid(((SGTGrid)grid).getZArray(),
//                                        ((SGTGrid)grid).getTimeArray(),
//                                        new_values,
//                                        ((SGTGrid)grid).getTitle());
          } else {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(doubleArrayName);
	      classArgs[2] = Class.forName(doubleArrayName);
	      classArgs[3] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = ((SGTGrid)grid).getZArray();
	      constructArgs[1] = ((SGTGrid)grid).getXArray();
	      constructArgs[2] = new_values;
	      constructArgs[2] = ((SGTGrid)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Velocity conversion: " + e);
	    }
//              new_grid = new SimpleGrid(((SGTGrid)grid).getZArray(),
//                                        ((SGTGrid)grid).getXArray(),
//                                        new_values,
//                                        ((SGTGrid)grid).getTitle());
          }
          ((SimpleGrid)new_grid).setXMetaData(((SGTGrid)grid).getXMetaData());
          ((SimpleGrid)new_grid).setZMetaData(((SGTGrid)grid).getZMetaData());
	}
	((SimpleGrid)new_grid).setYMetaData(newMeta);
	break;
      case Units.Z_AXIS:
	if(simpleGrid) {
	  ((SimpleGrid)new_grid).setZArray(new_values);
	} else {
          if(((SGTGrid)grid).isXTime()) {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(timeArrayName);
	      classArgs[2] = Class.forName(doubleArrayName);
	      classArgs[3] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = new_values;
	      constructArgs[1] = ((SGTGrid)grid).getTimeArray();
	      constructArgs[2] = ((SGTGrid)grid).getYArray();
	      constructArgs[2] = ((SGTGrid)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Velocity conversion: " + e);
	    }
//              new_grid = new SimpleGrid(new_values,
//                                        ((SGTGrid)grid).getTimeArray(),
//                                        ((SGTGrid)grid).getYArray(),
//                                        ((SGTGrid)grid).getTitle());
          } else if(((SGTGrid)grid).isYTime()) {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(doubleArrayName);
	      classArgs[2] = Class.forName(timeArrayName);
	      classArgs[3] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = new_values;
	      constructArgs[1] = ((SGTGrid)grid).getXArray();
	      constructArgs[2] = ((SGTGrid)grid).getTimeArray();
	      constructArgs[2] = ((SGTGrid)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Velocity conversion: " + e);
	    }
//              new_grid = new SimpleGrid(new_values,
//                                        ((SGTGrid)grid).getXArray(),
//                                        ((SGTGrid)grid).getTimeArray(),
//                                        ((SGTGrid)grid).getTitle());
          } else {
	    try {
	      classArgs[0] = Class.forName(doubleArrayName);
	      classArgs[1] = Class.forName(doubleArrayName);
	      classArgs[2] = Class.forName(doubleArrayName);
	      classArgs[3] = Class.forName(stringName);
	      gridConstruct = gridClass.getConstructor(classArgs);
	      constructArgs[0] = new_values;
	      constructArgs[1] = ((SGTGrid)grid).getXArray();
	      constructArgs[2] = ((SGTGrid)grid).getYArray();
	      constructArgs[2] = ((SGTGrid)grid).getTitle();
	      new_grid = (SGTData)gridConstruct.newInstance(constructArgs);
	    } catch (Exception e) {
	      System.out.println("Velocity conversion: " + e);
	    }
//              new_grid = new SimpleGrid(new_values,
//                                        ((SGTGrid)grid).getXArray(),
//                                        ((SGTGrid)grid).getYArray(),
//                                        ((SGTGrid)grid).getTitle());
          }
          ((SimpleGrid)new_grid).setXMetaData(((SGTGrid)grid).getXMetaData());
          ((SimpleGrid)new_grid).setYMetaData(((SGTGrid)grid).getYMetaData());
	}
	((SimpleGrid)new_grid).setZMetaData(newMeta);
      }
      
    }
    return new_grid;
  }
  static boolean isBaseUnit(SGTMetaData meta) {
    int count;
    String units = meta.getUnits();
    
    for(count=0; count < name.length; count++) {
      if(units.equals(name[count])) return true;
    }
    return false;
  }
}
