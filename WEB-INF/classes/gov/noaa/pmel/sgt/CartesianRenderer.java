/*
 * $Id: CartesianRenderer.java,v 1.13 2003/08/22 23:02:31 dwd Exp $
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

import gov.noaa.pfel.coastwatch.sgt.SGTPointsVector;
import gov.noaa.pfel.coastwatch.sgt.VectorPointsRenderer;
import gov.noaa.pmel.sgt.dm.Annotation;
import gov.noaa.pmel.sgt.dm.Collection;
import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.sgt.dm.SGTGrid;
import gov.noaa.pmel.sgt.dm.SGTLine;
import gov.noaa.pmel.sgt.dm.SGTPoint;
import gov.noaa.pmel.sgt.dm.SGTVector;
import java.awt.Graphics;
import java.awt.Point;
import java.beans.PropertyChangeListener;

/**
 * <code>CartesianRenderer</code> defines an interface to enable data to be rendered on a <code>
 * CartesianGraph</code>.
 *
 * @see CartesianGraph
 * @author Donald Denbo
 * @version $Revision: 1.13 $, $Date: 2003/08/22 23:02:31 $
 * @since 1.0
 */
public abstract class CartesianRenderer implements PropertyChangeListener {

  /** Bob Simons added this to avoid memory leak problems. */
  public abstract void releaseResources() throws Exception;

  /**
   * Factory method to create a new Renderer instance given the <code>SGTData</code> object and
   * <code>Attribute</code>. For example, a <code>LineCartesianRenderer</code> is created if <code>
   * SGTData</code> object is a <code>SGTLine</code>.
   *
   * <p>A renderer is constucted based on the two arguements.
   *
   * <p>
   *
   * <TABLE style=\"border:1px; padding:2px; background-color:white;">
   * <TR>
   * 	<TH WIDTH="25%" BGCOLOR="#FFFFCC">
   * 		<P>SGTData
   * 	</TH>
   * 	<TH WIDTH="25%" BGCOLOR="#FFFFCC">
   * 		<P>Attribute
   * 	</TH>
   * 	<TH WIDTH="50%" BGCOLOR="#FFFFCC">
   * 		<P>CartesianRenderer
   * 	</TH>
   * </TR>
   * <TR>
   * 	<TD WIDTH="25%">SGTPoint</TD>
   * 	<TD WIDTH="25%">PointAttribute</TD>
   * 	<TD WIDTH="50%">PointCartesianRenderer</TD>
   * </TR>
   * <TR>
   * 	<TD WIDTH="25%">SGTLine</TD>
   * 	<TD WIDTH="25%">LineAttribute</TD>
   * 	<TD WIDTH="50%">LineCartesianRenderer</TD>
   * </TR>
   * <TR>
   * 	<TD WIDTH="25%">SGTGrid</TD>
   * 	<TD WIDTH="25%">GridAttribute</TD>
   * 	<TD WIDTH="50%">GridCartesianRenderer</TD>
   * </TR>
   * <TR>
   * 	<TD WIDTH="25%">SGTVector</TD>
   * 	<TD WIDTH="25%">VectorAttribute</TD>
   * 	<TD WIDTH="50%">VectorCartesianRenderer</TD>
   * </TR>
   * <TR>
   * 	<TD WIDTH="25%">Collection</TD>
   * 	<TD WIDTH="25%">PointAttribute</TD>
   * 	<TD WIDTH="50%">PointCartesianRenderer</TD>
   * </TR>
   * <TR>
   * 	<TD WIDTH="25%">Collection</TD>
   * 	<TD WIDTH="25%">LineAttribute</TD>
   * 	<TD WIDTH="50%">LineCartesianRenderer</TD>
   * </TR>
   * <TR>
   * 	<TD WIDTH="25%">Collection</TD>
   * 	<TD WIDTH="25%">VectorAttribute</TD>
   * 	<TD WIDTH="50%">VectorCartesianRenderer</TD>
   * </TR>
   * <TR>
   * 	<TD WIDTH="25%">Annotation</TD>
   * 	<TD WIDTH="25%">n/a</TD>
   * 	<TD WIDTH="50%">AnnotationCartesianRenderer</TD>
   * </TR>
   * </TABLE>
   *
   * <p>
   *
   * @param dmo DataModel object
   */
  public static CartesianRenderer getRenderer(CartesianGraph cg, SGTData dmo, Attribute attr) {
    if (dmo instanceof SGTPoint) {
      return new PointCartesianRenderer(cg, (SGTPoint) dmo, (PointAttribute) attr);
    } else if (dmo instanceof SGTLine) {
      return new LineCartesianRenderer(cg, (SGTLine) dmo, (LineAttribute) attr);
    } else if (dmo instanceof SGTGrid) {
      return new GridCartesianRenderer(cg, (SGTGrid) dmo, (GridAttribute) attr);
    } else if (dmo instanceof SGTVector) {
      return new VectorCartesianRenderer(cg, (SGTVector) dmo, (VectorAttribute) attr);
    } else if (dmo instanceof SGTPointsVector) {
      return new VectorPointsRenderer(
          cg, (SGTPointsVector) dmo, (gov.noaa.pfel.coastwatch.sgt.VectorAttribute2) attr);
    } else if (dmo instanceof Collection) {
      Object fe = ((Collection) dmo).firstElement();
      if (fe instanceof SGTPoint) {
        return new PointCartesianRenderer(cg, (Collection) dmo, (PointAttribute) attr);
      } else if (fe instanceof SGTLine) {
        return new LineCartesianRenderer(cg, (Collection) dmo, (LineAttribute) attr);
      } else if (fe instanceof SGTVector) {
        return new VectorCartesianRenderer(cg, (Collection) dmo, (VectorAttribute) attr);
      }
    } else if (dmo instanceof Annotation) {
      return new AnnotationCartesianRenderer(cg, (Annotation) dmo, null);
    }
    return null;
  }

  /**
   * Render the <code>SGTData</code> object. This method should never be called directly.
   *
   * @see Pane#draw
   */
  public abstract void draw(Graphics g);

  /**
   * Get the <code>Attribute</code> associated with the renderer.
   *
   * @return the <code>Attribute</code>
   */
  public abstract Attribute getAttribute();

  /**
   * Get the <code>CartesianGraph</code> associated with the renderer.
   *
   * @since 2.0
   * @return the <code>CartesianGraph</code>
   */
  public abstract CartesianGraph getCartesianGraph();

  /**
   * @directed
   * @label cg
   */
  protected CartesianGraph cg_;

  /**
   * Get parent pane.
   *
   * @since 2.0
   */
  public AbstractPane getPane() {
    return cg_.getPane();
  }

  /**
   * For internal sgt use.
   *
   * @since 2.0
   */
  public void modified(String mess) {
    if (cg_ != null) cg_.modified(mess);
  }

  /**
   * Find data object.
   *
   * @since 3.0
   */
  public SGTData getDataAt(int x, int y) {
    return getDataAt(new Point(x, y));
  }

  /**
   * Find data object.
   *
   * @since 3.0
   */
  public abstract SGTData getDataAt(Point pt);
}
