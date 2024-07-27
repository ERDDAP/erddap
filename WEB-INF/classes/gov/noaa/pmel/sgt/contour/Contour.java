/*
 * $Id: Contour.java,v 1.15 2001/08/03 22:50:12 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt.contour;

import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.ContourLevelNotFoundException;
import gov.noaa.pmel.sgt.ContourLevels;
import gov.noaa.pmel.sgt.ContourLineAttribute;
import gov.noaa.pmel.sgt.DefaultContourLineAttribute;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.sgt.dm.SGTGrid;
import gov.noaa.pmel.util.Debug;
import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Range2D;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Contour constructs a set of <code>ContourLine</code> objects based on the <code>ContourLevels
 * </code>, <code>SGTGrid</code>, and mask supplied. Used by <code>GridCartesianRenderer</code> for
 * <code>GridAttribute</code> types of CONTOUR.
 *
 * @author D. W. Denbo
 * @version $Revision: 1.15 $, $Date: 2001/08/03 22:50:12 $
 * @since 2.0
 * @see ContourLine
 * @see ContourLevels
 * @see gov.noaa.pmel.sgt.GridCartesianRenderer
 * @see gov.noaa.pmel.sgt.GridAttribute
 * @see ContourLineAttribute
 * @see DefaultContourLineAttribute
 */
public class Contour implements PropertyChangeListener {
  /**
   * @label grid
   */
  private SGTGrid grid_;

  /**
   * A non-zero mask value will cause a point to be excluded.
   *
   * @label mask
   */
  private SGTGrid mask_;

  private CartesianGraph cg_;

  /**
   * @label contourLevels
   */
  private ContourLevels contourLevels_;

  private double zmin_;
  private double zmax_;
  private boolean upToDate_;
  private GeoDate tref_ = null;
  private boolean xTime_ = false;
  private boolean yTime_ = false;
  private double[] px_;
  private double[] py_;
  private double[] z_;
  private double[] xx_;
  private double[] yy_;
  private double[] zz_;
  private boolean[] used_;
  private int[] kabov_;
  private int[] isin_ = {0, 1, 0, -1};
  private double weezee_;
  private int nx_;
  private int ny_;

  /**
   * @associates <oiref:gov.noaa.pmel.sgt.contour.ContourLine:oiref>
   * @link aggregation
   * @supplierCardinality 1..*
   * @label contourLines
   */
  private Vector contourLines_;

  /**
   * @link aggregationByValue
   * @supplierCardinality 1
   * @label sides
   */
  private Sides sides_;

  /**
   * @link aggregationByValue
   * @supplierCardinality 1
   * @label gridFlag
   */
  private GridFlag gridFlag_;

  //
  private static double DSLAB = 2.0;
  private static double SLAB1F = 0.4;

  /** Bob Simons added this to avoid memory leak problems. */
  public void releaseResources() throws Exception {
    try {
      if (grid_ != null) {
        SGTGrid o = grid_; // done this way to avoid infinite loop
        grid_ = null;
        o.releaseResources();
      }
      if (mask_ != null) {
        SGTGrid o = mask_; // done this way to avoid infinite loop
        mask_ = null;
        o.releaseResources();
      }
      cg_ = null; // not releaseResources() else infinite loop
      if (contourLevels_ != null) {
        ContourLevels o = contourLevels_; // done this way to avoid infinite loop
        contourLevels_ = null;
        o.releaseResources();
      }
      if (contourLines_ != null) {
        Vector o = contourLines_; // done this way to avoid infinite loop
        contourLines_ = null;
        Enumeration en = o.elements();
        while (en.hasMoreElements()) ((ContourLine) en.nextElement()).releaseResources();
        o.clear();
      }
      sides_ = null;
      gridFlag_ = null;
      if (JPane.debug) String2.log("sgt.contour.Contour.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /**
   * Construct a <code>Contour</code> object using a range to define the <code>ContourLevels</code>.
   */
  public Contour(CartesianGraph cg, SGTGrid grid, Range2D range) {
    cg_ = cg;
    grid_ = grid;
    contourLevels_ = ContourLevels.getDefault(range);
    init();
    upToDate_ = false;
  }

  /**
   * Construct a <code>Contour</code> object using an array of levels to define the <code>
   * ContourLevels</code>.
   */
  public Contour(CartesianGraph cg, SGTGrid grid, double[] levels) {
    cg_ = cg;
    grid_ = grid;
    contourLevels_ = ContourLevels.getDefault(levels);
    init();
    upToDate_ = false;
  }

  /** Construct a <code>Contour</code> object using a <code>ContourLevels</code> object. */
  public Contour(CartesianGraph cg, SGTGrid grid, ContourLevels conLevels) {
    cg_ = cg;
    grid_ = grid;
    contourLevels_ = conLevels;
    contourLevels_.addPropertyChangeListener(this);
    init();
    upToDate_ = false;
  }

  /** Get a reference to the <code>ContourLevels</code> object. */
  public ContourLevels getContourLevels() {
    return contourLevels_;
  }

  /**
   * Set a <code>SGTGrid</code> object to be used to mask the data grid. The Z values are used to
   * determine the masking, values of NaN and non-zero are set as MISSING.
   */
  public void setMask(SGTGrid mask) {
    if (mask_ == null || !mask_.equals(mask)) upToDate_ = false;
    mask_ = mask;
  }

  /** Get the mask. */
  public SGTGrid getMask() {
    return mask_;
  }

  /**
   * Return the <code>Enumeration</code> of a <code>Vector</code> containing the <code>ContourLine
   * </code> objects.
   */
  public Enumeration elements() {
    if (!upToDate_) generateContourLines();
    return contourLines_.elements();
  }

  /** Reponds to changes in the <code>ContourLevels</code> object. */
  @Override
  public void propertyChange(PropertyChangeEvent event) {}

  private void init() {
    GeoDate[] ttemp;
    if (grid_.isXTime()) {
      xTime_ = true;
      ttemp = grid_.getTimeArray();
      tref_ = ttemp[0];
      px_ = getTimeOffsetArray(ttemp, tref_);
      nx_ = grid_.getTSize();
    } else {
      px_ = grid_.getXArray();
      nx_ = grid_.getXSize();
    }
    if (grid_.isYTime()) {
      yTime_ = true;
      ttemp = grid_.getTimeArray();
      tref_ = ttemp[0];
      py_ = getTimeOffsetArray(ttemp, tref_);
      ny_ = grid_.getTSize();
    } else {
      py_ = grid_.getYArray();
      ny_ = grid_.getYSize();
    }
    if (Debug.CONTOUR) {
      System.out.println("nx_ = " + nx_ + ", ny_ = " + ny_);
    }
    /** side definitions ________ | 2 | | | 3| |1 | | |________| 0 */
    sides_ = new Sides(nx_, ny_);
    //
    // make the corner arrays index from 0 to 3
    //
    xx_ = new double[4];
    yy_ = new double[4];
    zz_ = new double[4];
    used_ = new boolean[4];
    kabov_ = new int[4];
    z_ = grid_.getZArray();
  }

  private double[] getTimeOffsetArray(GeoDate[] tarray, GeoDate tref) {
    double[] array = new double[tarray.length];
    for (int i = 0; i < tarray.length; i++) {
      array[i] = tarray[i].offset(tref);
    }
    return array;
  }

  /**
   * Given the current <code>ContourLevels</code>, mask, and <code>SGTGrid</code> generate the
   * <code>ContourLine</code>s.
   */
  public void generateContourLines() {
    double zrange;
    double zc;
    int kabij, kabip1, kabjp1;
    int used0, used3;
    int ll;
    int i = 0;
    int j = 0;
    int ii, jj;
    ContourLine cl;
    int lin, k, kmax, nseg;
    boolean reversed;
    double xt, yt, frac;
    double flm1, flp1;
    int lp1, lp2, lm1;
    int kda, kdb;
    int exit;

    if (upToDate_) return;

    contourLines_ = new Vector();
    upToDate_ = true;
    computeMinMax();
    //
    // compute a small non-zero value for testing
    //
    zrange = (zmax_ - zmin_) * 1.1;
    if (zrange <= 0.0) return;
    weezee_ = zrange * 0.0002;
    //
    // loop over all contour levels
    //
    Enumeration lenum = contourLevels_.levelElements();
    while (lenum.hasMoreElements()) {
      //
      // initialize for level
      //
      zc = ((Double) lenum.nextElement()).doubleValue();
      if (Debug.CONTOUR) {
        System.out.println("zc = " + zc);
      }
      if (zc <= zmin_ || zc >= zmax_) continue;
      sides_.clear();
      if (mask_ != null) {
        gridFlag_ = new GridFlag(grid_, mask_, zc);
      } else {
        gridFlag_ = new GridFlag(grid_, zc);
      }
      for (ii = 0; ii < nx_; ii++) {
        /* 1001 */
        loop1000:
        for (jj = 0; jj < ny_; jj++) {
          /* 1000 */
          i = ii;
          j = jj;
          kabij = gridFlag_.getValue(i, j);
          if (i < nx_ - 1) {
            kabip1 = gridFlag_.getValue(i + 1, j);
          } else {
            kabip1 = 10;
          }
          if (j < ny_ - 1) {
            kabjp1 = gridFlag_.getValue(i, j + 1);
          } else {
            kabjp1 = 10;
          }
          used0 = sides_.getSide(i, j, 0);
          used3 = sides_.getSide(i, j, 3);
          ll = 0; /*  bottom side */
          if (kabij + kabip1 + used0 == 0) {
            computeCorners(i, j, zc);
          } else if (kabij + kabjp1 + used3 == 0) {
            ll = 3; /*  left side */
            computeCorners(i, j, zc);
          } else {
            continue;
          }
          //
          // setup for new contour line
          //
          lin = ll;
          k = 0;
          nseg = 1;
          reversed = false;
          cl = new ContourLine();
          try {
            cl.setAttributes(
                contourLevels_.getDefaultContourLineAttribute(),
                contourLevels_.getContourLineAttribute(zc));
          } catch (ContourLevelNotFoundException e) {
            System.out.println(e);
          }
          cl.setLevel(zc);
          cl.setCartesianGraph(cg_);
          cl.setTime(tref_, xTime_, yTime_);
          contourLines_.addElement(cl); /* add to list */
          cl.addPoint(0.0, 0.0); /* dummy k=1 point */
          //
          // Given entrance to square(i,j) on side lin,
          // record the entrance point x(k), y(k).
          // Set lin to used
          //
          //    lin    lp1    lp2    lm1
          //   _________________________
          //     0      1      2      3
          //     1      2      3      0
          //     2      3      0      1
          //     3      0      1      2
          //
          loop350:
          while (true) {
            /* 350 */
            lp1 = lin + 1 - ((lin + 1) / 4) * 4;
            lp2 = lp1 + 1 - ((lp1 + 1) / 4) * 4;
            lm1 = lp2 + 1 - ((lp2 + 1) / 4) * 4;
            if (!reversed) {
              k = k + 1;
              frac = (zc - zz_[lin]) / (zz_[lp1] - zz_[lin]);
              xt = xx_[lin] + (xx_[lp1] - xx_[lin]) * frac;
              yt = yy_[lin] + (yy_[lp1] - yy_[lin]) * frac;
              cl.addPoint(xt, yt);
              sides_.setSideUsed(i, j, lin, true);
            }
            //
            // See if an exit exists on side l-1, l+1, or l+2.
            // If so choose the one closest to side l. If the
            // exit already used terminate x,y.
            //
            reversed = false;
            exit = lm1;
            if (kabov_[lin] + kabov_[lm1] == 0) {
              if (kabov_[lp1] + kabov_[lp2] == 0) {
                flm1 = (zc - zz_[lin]) / (zz_[lm1] - zc);
                flp1 = (zc - zz_[lp1]) / (zz_[lp2] - zc);
                if (!(used_[lp1] || (flm1 <= flp1 && !used_[lm1]))) {
                  exit = lp1;
                }
              }
            } else {
              if (kabov_[lp1] + kabov_[lp2] == 0) {
                exit = lp1;
              } else {
                exit = lp2;
                if (kabov_[lp2] + kabov_[lm1] != 0) {
                  /* 470 */
                  if (kabov_[lp2] + kabov_[lm1] <= 15) {
                    kda = lin;
                    kdb = lp2;
                    if (kabov_[lp2] > 5) {
                      kda = lm1;
                      kdb = lp1;
                    }
                    k = k + 1;
                    frac = (zc - zz_[kda]) / (zz_[kdb] - zz_[kda]);
                    xt = xx_[kda] + (xx_[kdb] - xx_[kda]) * frac;
                    yt = yy_[kda] + (yy_[kdb] - yy_[kda]) * frac;
                    cl.addPoint(xt, yt);
                  }
                  if (nseg > 1) {
                    kmax = k;
                    //
                    //       pt(1) = pt(2)
                    //  pt(kmax+1) = pt(kmax)
                    //
                    cl.setElementAt((Point2D.Double) cl.elementAt(1), 0);
                    cl.addPoint((Point2D.Double) cl.elementAt(k));
                    cl.setClosed(false);
                    cl.setKmax(kmax);
                    continue loop1000;
                  }
                  reversed = true;
                  nseg = 2;
                  cl.reverseElements(k);
                  i = ii;
                  j = jj;
                  exit = ll;
                  //
                  // Find square entered by present exit.
                  //
                  //    exit    i     j     lin
                  //   _________________________
                  //     0      i    j-1     2
                  //     1     i+1    j      3
                  //     2      i    j+1     0
                  //     3     i-1    j      1
                  //
                  i = i + isin_[exit];
                  j = j + isin_[3 - exit];
                  lin = exit + 2 - ((exit + 2) / 4) * 4;
                  computeCorners(i, j, zc);
                  continue loop350;
                }
              }
            }
            if (used_[exit]) {
              kmax = k + 1;
              //
              //     pt(kmax) = pt(2)
              //        pt(1) = pt(k)
              //   pt(kmax+1) = pt(3)
              //
              cl.addPoint((Point2D.Double) cl.elementAt(1));
              cl.setElementAt((Point2D.Double) cl.elementAt(k), 0);
              cl.addPoint((Point2D.Double) cl.elementAt(2));
              cl.setClosed(true);
              cl.setKmax(kmax);
              continue loop1000;
            }
            i = i + isin_[exit];
            j = j + isin_[3 - exit];
            lin = exit + 2 - ((exit + 2) / 4) * 4;
            computeCorners(i, j, zc);
          } /* 350 */
        } /* 1000 */
      } /* 1001 */
    } /* levels */
  }

  private void computeCorners(int i, int j, double zc) {
    int jl, lp1, il;
    boolean[] used = {false, false, false, false};
    //
    // Get xx,yy,zz,kabov for each corner and
    // used for each side.
    //
    //   lcorner     il    jl    lp1
    //      0        i     j      1
    //      1       i+1    j      2
    //      2       i+1   j+1     3
    //      3        i    j+1     0
    //
    //
    // lcorner definitions:
    // (i,j+1) __________ (i+1,j+1)
    //        |    2     |
    //        |          |
    //       3|          |1
    //        |          |
    //        |__________|
    //  (i,j)      0      (i+1,j)
    //
    for (int lcorner = 0; lcorner < 4; lcorner++) {
      /* 620 */
      jl = j + lcorner / 2;
      lp1 = lcorner + 1 - ((lcorner + 1) / 4) * 4;
      il = i + lp1 / 2;
      zz_[lcorner] = Double.NaN;
      kabov_[lcorner] = 10;
      if (((il + 1) * (nx_ - il) > 0)
          && ((jl + 1) * (ny_ - jl) > 0)
          && !gridFlag_.isMissing(il, jl)) {
        used[lcorner] = sides_.isSideUsed(i, j, lcorner);
        zz_[lcorner] = setZ(z(il, jl), zc);
        if (zz_[lcorner] < zc) {
          kabov_[lcorner] = -1;
        } else {
          kabov_[lcorner] = 1;
        }
      }
      xx_[lcorner] = px_[Math.max(0, Math.min(il, nx_ - 1))];
      yy_[lcorner] = py_[Math.max(0, Math.min(jl, ny_ - 1))];
    } /* 620 */
    used_[0] = used[0];
    used_[1] = used[1];
    used_[2] = used[2];
    used_[3] = used[3];
  }

  private double z(int i, int j) {
    return z_[j + i * ny_];
  }

  private double setZ(double z, double zc) {
    double diff = z - zc;
    if (Math.abs(diff) < weezee_) {
      return zc + weezee_ * (diff > 0.0 ? 1.0 : -1.0);
    } else {
      return z;
    }
  }

  private void computeMinMax() {
    double[] grid = grid_.getZArray();
    double[] mask = null;
    boolean haveMask = mask_ != null;
    if (haveMask) mask = mask_.getZArray();

    zmin_ = Double.POSITIVE_INFINITY;
    zmax_ = Double.NEGATIVE_INFINITY;

    for (int i = 0; i < grid.length; i++) {
      if (!Double.isNaN(grid[i]) && !(haveMask && (mask[i] == 0.0))) {
        zmin_ = Math.min(zmin_, grid[i]);
        zmax_ = Math.max(zmax_, grid[i]);
      }
    }
  }

  /**
   * Given the computed <code>ContourLine</code>s and the <code>ContourLineAttribute</code> generate
   * the contour labels. Must be only invoked after generateConourLines().
   */
  public void generateContourLabels(Graphics g) {
    int i, j, k, kk, km1, kp1;
    int nx, ny;
    double dx, dy, dzdx, dzdy, dzdg;
    GeoDate tref = null;
    GeoDate time;
    double[] x, y, z, s;
    double dxx, dyy, smax;
    double space = 0.0;
    double ark = 1.0;
    boolean roomFound;
    double slab1, stest;
    double width, hgt;
    double xa, xb, ya, yb, aa, bb, cc, zxy;
    double xendl, yendl;
    //    double xst, yst, xstp, ystp;
    double xlab, ylab, hhgt, angle;
    SGLabel label;
    double[] px, py;
    boolean xIncreasing, yIncreasing;
    double dlev, cspace;
    Enumeration elem = contourLines_.elements();
    ContourLine cl;
    while (elem.hasMoreElements()) {
      cl = (ContourLine) elem.nextElement();
      int kmax = cl.getKmax();
      int lev = contourLevels_.getIndex(cl.getLevel());
      if (Debug.CONTOUR) {
        // System.out.println("Contour.drawLabelContourLine: lev = " + lev +
        //		 ", level = " + cl.getLevel());
      }
      DefaultContourLineAttribute cattr = cl.getDefaultContourLineAttribute();
      cattr.setContourLineAttribute(cl.getContourLineAttribute());
      if (kmax <= 1 || !cattr.isLabelEnabled()) continue;
      // System.out.println("Contour.drawLabelContourLine: lev = " + lev + //bob added
      //		 ", level = " + cl.getLevel() + ", label=" + cattr.getLabelText());
      //
      // create SGLabel at a dummy location and no rotation
      //
      label =
          new SGLabel(
              "CLevel",
              cattr.getLabelText(),
              cattr.getLabelHeightP(),
              new Point2D.Double(0.0, 0.0),
              SGLabel.BOTTOM,
              SGLabel.LEFT);
      if (cattr.getLabelFont() != null) label.setFont(cattr.getLabelFont());
      label.setColor(cattr.getLabelColor());
      label.setLayer(cg_.getLayer());
      //
      // compute hgt and width from font
      //
      int swidth = (int) label.getStringWidth(g);
      int sheight = (int) label.getStringHeight(g);
      width = cg_.getLayer().getXDtoP(swidth) - cg_.getLayer().getXDtoP(0);
      //
      hgt = cg_.getLayer().getYDtoP(0) - cg_.getLayer().getYDtoP(sheight);
      //
      hhgt = hgt * 0.5;
      width = width + hhgt;
      if (Debug.CONTOUR) {
        //      System.out.println("drawLabeledContourLine: hhgt,width = " +
        //			 hhgt + ", " + width);
      }
      //
      px = xArrayP();
      py = yArrayP();
      z = grid_.getZArray();
      xIncreasing = px[0] < px[1];
      yIncreasing = py[0] < py[1];
      nx = px.length;
      ny = py.length;
      //
      x = new double[kmax + 1];
      y = new double[kmax + 1];
      s = new double[kmax + 1];
      if (cl.isXTime() || cl.isYTime()) {
        tref = cl.getReferenceTime();
      }
      s[1] = 0.0;
      //
      // convert ContourLine to physical units
      //
      x = cl.getXArrayP();
      y = cl.getYArrayP();
      //
      // compute s[k]
      //
      for (k = 2; k <= kmax; k++) {
        dxx = x[k] - x[k - 1];
        dyy = y[k] - y[k - 1];
        s[k] = s[k - 1] + Math.sqrt(dxx * dxx + dyy * dyy);
      }
      smax = s[kmax];
      slab1 = smax * SLAB1F;
      stest = Math.max(0.0, DSLAB - slab1);
      k = 1;
      //
      // check conditions for labelling
      //
      while (k < kmax) {
        /* 755 */
        km1 = Math.max(k - 1, 1);
        stest = stest + s[k] - s[km1];
        //
        // Test if there is enough room for a label
        //
        if (stest < DSLAB || (smax - s[k]) <= 2.0 * width) {
          //	  drawLineSegment(g, x[k], y[k], x[k+1], y[k+1]);
          k = k + 1;
          continue;
        }
        kp1 = k + 1;
        //
        // gradient test
        //
        if (lev != 0) {
          try {
            dlev = Math.abs(contourLevels_.getLevel(lev) - contourLevels_.getLevel(lev - 1));
            if (xIncreasing) {
              for (i = 0; i < nx - 1; i++) {
                if (x[k] >= px[i] && x[k] <= px[i + 1]) break;
              }
            } else {
              for (i = 0; i < nx - 1; i++) {
                if (x[k] <= px[i] && x[k] >= px[i + 1]) break;
              }
            }
            if (yIncreasing) {
              for (j = 0; j < ny - 1; j++) {
                if (y[k] >= py[j] && y[k] <= py[j + 1]) break;
              }
            } else {
              for (j = 0; j < ny - 1; j++) {
                if (y[k] <= py[j] && y[k] >= py[j + 1]) break;
              }
            }
            i = Math.min(i, nx - 2);
            j = Math.min(j, ny - 2);
            dx = px[i + 1] - px[i];
            dy = py[j + 1] - py[j];
            if (Double.isNaN(z[j + (i + 1) * ny])) {
              dzdx = 0.0;
            } else {
              dzdx = (z[j + (i + 1) * ny] - z[j + i * ny]) / dx;
            }
            if (Double.isNaN(z[j + 1 + i * ny])) {
              dzdy = 0.0;
            } else {
              dzdy = (z[j + 1 + i * ny] - z[j + i * ny]) / dy;
            }
            //
            // dzdg = sqrt(dzdx*dzdx+dzdy*dzdy)
            //
            // replace with less prone to overflow
            // calculation
            //
            dzdg = Math.abs(dzdx) + Math.abs(dzdy);
            if (dzdg != 0.0) {
              cspace = dlev / dzdg;
              //
              // is there room for label height?
              // (was 0.75)
              if (cspace < hgt * 1.0) {
                //		drawLineSegment(g, x[k], y[k], x[k+1], y[k+1]);
                k = k + 1;
                continue;
              }
            }
          } catch (ContourLevelNotFoundException e) {
            System.out.println(e);
          }
        }
        //
        // test line arc
        //
        roomFound = false;
        for (kk = kp1; kk <= kmax; kk++) {
          dxx = x[kk] - x[k];
          dyy = y[kk] - y[k];
          space = Math.sqrt(dxx * dxx + dyy * dyy);
          ark = s[kk] - s[k];
          if (space >= width) {
            roomFound = true;
            break;
          }
        }
        if (space / ark < 0.80 || !roomFound) {
          //	  drawLineSegment(g, x[k], y[k], x[k+1], y[k+1]);
          k = k + 1;
          continue;
        } else {
          //
          // add label to contour line
          //
          cl.addLabel(k, (SGLabel) label.copy(), hgt, width);
          //
          // draw the label
          //
          stest = 0.0; /* 810 */
          //  	  xa = x[kk-1] - x[k];
          //  	  xb = x[kk] - x[kk-1];
          //  	  ya = y[kk-1] - y[k];
          //  	  yb = y[kk] - y[kk-1];
          //  	  aa = xb*xb + yb*yb;
          //  	  bb = xa*xb + ya*yb;
          //  	  cc = xa*xa + ya*ya - width*width;
          //  	  zxy = (-bb + Math.sqrt(bb*bb - aa*cc))/aa;
          //  	  dxx = xa + xb*zxy;
          //  	  dyy = ya + yb*zxy;
          //  	  xendl = x[k] + dxx;
          //  	  yendl = y[k] + dyy;
          //  	  //
          //  	  // compute label angle
          //  	  //
          //  	  angle = 90.0;
          //  	  if(dyy < 0.0) angle = -90.0;
          //  	  if(dxx != 0.0) {
          //  	    angle = Math.atan(dyy/dxx)*180.0/Math.PI;
          //  	  }
          //  	  //
          //  	  // compute label position
          //  	  //
          //  	  if(dxx >= 0) {
          //  	    xlab = x[k] + hhgt*(0.5*dxx + dyy)/width;
          //  	    ylab = y[k] + hhgt*(0.5*dyy - dxx)/width;
          //  	  } else {
          //  	    xlab = xendl - hhgt*(0.5*dxx + dyy)/width;
          //  	    ylab = yendl - hhgt*(0.5*dyy - dxx)/width;
          //  	  }
          //  	  label.setAngle(angle);
          //  	  label.setLocationP(new Point2D.Double(xlab, ylab));
          //  	  try {
          //  	    label.draw(g);
          //  	    //	  drawRotatedRectangle(g, angle, xlab, ylab, width-hhgt, hgt);
          //  	  } catch (LayerNotFoundException e) {
          //  	    System.out.println(e);
          //  	  }
          //  	  drawLineSegment(g, xendl, yendl, x[kk], y[kk]);
          k = kk;
        }
      }
    }
  }

  //    private void drawLineSegment(Graphics g, double x0, double y0,
  //  			       double x1, double y1) { /* 900 */
  //      int xd0, yd0, xd1, yd1;
  //      xd0 = cg_.getLayer().getXPtoD(x0);
  //      yd0 = cg_.getLayer().getYPtoD(y0);
  //      xd1 = cg_.getLayer().getXPtoD(x1);
  //      yd1 = cg_.getLayer().getYPtoD(y1);
  //      g.drawLine(xd0, yd0, xd1, yd1);
  //    }

  private double[] xArrayP() {
    int i;
    double[] p;
    if (grid_.isXTime()) {
      GeoDate[] t = grid_.getTimeArray();
      p = new double[t.length];
      for (i = 0; i < t.length; i++) {
        p[i] = cg_.getXUtoP(t[i]);
      }
    } else {
      double[] x = grid_.getXArray();
      p = new double[x.length];
      for (i = 0; i < x.length; i++) {
        p[i] = cg_.getXUtoP(x[i]);
      }
    }
    return p;
  }

  private double[] yArrayP() {
    int i;
    double[] p;
    if (grid_.isYTime()) {
      GeoDate[] t = grid_.getTimeArray();
      p = new double[t.length];
      for (i = 0; i < t.length; i++) {
        p[i] = cg_.getYUtoP(t[i]);
      }
    } else {
      double[] y = grid_.getYArray();
      p = new double[y.length];
      for (i = 0; i < y.length; i++) {
        p[i] = cg_.getYUtoP(y[i]);
      }
    }
    return p;
  }

  //    private void drawContourLine(Graphics g, ContourLine cl) {
  //      GeoDate tref, time;
  //      Point2D.Double[] pt;
  //      int i;
  //      //    int size = cl.size();
  //      int size = cl.getKmax() + 1;
  //      if(size <= 3) return;
  //      int[] xp = new int[size];
  //      int[] yp = new int[size];
  //      pt = new Point2D.Double[size];
  //      for(i=0; i < size; i++) {
  //        pt[i] = (Point2D.Double)cl.elementAt(i);
  //      }
  //      if(cl.isXTime()) {
  //        tref = cl.getReferenceTime();
  //        for(i=0; i < size; i++) {
  //  	time = (new GeoDate(tref)).increment(pt[i].x, GeoDate.DAYS);
  //  	xp[i] = cg_.getXUtoD(time);
  //        }
  //      } else {
  //        for(i=0; i < size; i++) {
  //  	xp[i] = cg_.getXUtoD(pt[i].x);
  //        }
  //      }
  //      if(cl.isYTime()) {
  //        tref = cl.getReferenceTime();
  //        for(i=0; i < size; i++) {
  //  	time = (new GeoDate(tref)).increment(pt[i].y, GeoDate.DAYS);
  //  	yp[i] = cg_.getYUtoD(time);
  //        }
  //      } else {
  //        for(i=0; i < size; i++) {
  //  	yp[i] = cg_.getYUtoD(pt[i].y);
  //        }
  //      }
  //      g.drawPolyline(xp, yp, size);
  //    }
}
