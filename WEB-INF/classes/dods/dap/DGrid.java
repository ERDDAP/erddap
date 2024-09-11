/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1998, California Institute of Technology.
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged.
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Jake Hamby, NASA/Jet Propulsion Laboratory
//         Jake.Hamby@jpl.nasa.gov
/////////////////////////////////////////////////////////////////////////////

package dods.dap;

import java.io.*;
import java.util.Enumeration;
import java.util.Vector;

/**
 * This class holds a <code>DArray</code> and a set of "Map" vectors. The Map vectors are
 * one-dimensional arrays corresponding to each dimension of the central <code>Array</code>. Using
 * this scheme, a <code>Grid</code> can represent, in a rectilinear array, data which is not in
 * reality rectilinear. An example will help make this clear.
 *
 * <p>Assume that the following array contains measurements of some real quantity, conducted at nine
 * different points in space: <code><pre>
 * A = [ 1  2  3  4 ]
 *     [ 2  4  6  8 ]
 *     [ 3  6  9  12]
 * </pre></code> To locate this <code>Array</code> in the real world, we could note the location of
 * one corner of the grid, and the grid spacing. This would allow us to calculate the location of
 * any of the other points of the <code>Array</code>.
 *
 * <p>This approach will not work, however, unless the grid spacing is precisely regular. If the
 * distance between Row 1 and Row 2 is not the same as the distance between Row 2 and Row 3, the
 * scheme will break down. The solution is to equip the <code>Array</code> with two <code>Map</code>
 * vectors that define the location of each row or column of the array: <code><pre>
 *       A = [ 1  2  3  4 ] Row = [ 0 ]
 *           [ 2  4  6  8 ]       [ 3 ]
 *           [ 3  6  9  12]       [ 8 ]
 *
 *  Column = [ 0  2  8  27]
 * </pre></code> The real location of the point in the first row and column of the array is now
 * exactly fixed at (0,0), and the point in the last row and last column is at (8,27).
 *
 * @version $Revision: 1.21 $
 * @author jehamby
 * @see BaseType
 * @see DArray
 */
public class DGrid extends DConstructor implements ClientIO {
  /** The <code>Array</code> part of the <code>DGrid</code> */
  public static final int ARRAY = 1;

  /** The Map part of the <code>DGrid</code> */
  public static final int MAPS = 2;

  /** The Array component of this <code>DGrid</code>. */
  protected DArray arrayVar;

  /** The Map component of this <code>DGrid</code>. */
  protected Vector mapVars;

  /** Constructs a new <code>DGrid</code>. */
  public DGrid() {
    this(null);
  }

  /**
   * Constructs a new <code>DGrid</code> with name <code>n</code>.
   *
   * @param n the name of the variable.
   */
  public DGrid(String n) {
    super(n);
    mapVars = new Vector();
  }

  /**
   * Returns a clone of this <code>DGrid</code>. A deep copy is performed on all data inside the
   * variable.
   *
   * @return a clone of this <code>DGrid</code>.
   */
  @Override
  public Object clone() {
    DGrid g = (DGrid) super.clone();
    g.arrayVar = (DArray) arrayVar.clone();
    g.mapVars = new Vector();
    for (int i = 0; i < mapVars.size(); i++) {
      BaseType bt = (BaseType) mapVars.elementAt(i);
      g.mapVars.addElement(bt.clone());
    }
    return g;
  }

  /**
   * Returns the DODS type name of the class instance as a <code>String</code>.
   *
   * @return the DODS type name of the class instance as a <code>String</code>.
   */
  @Override
  public String getTypeName() {
    return "Grid";
  }

  /**
   * Returns the number of variables contained in this object. For simple and vector type variables,
   * it always returns 1. To count the number of simple-type variable in the variable tree rooted at
   * this variable, set <code>leaves</code> to <code>true</code>.
   *
   * @param leaves If true, count all the simple types in the `tree' of variables rooted at this
   *     variable.
   * @return the number of contained variables.
   */
  @Override
  public int elementCount(boolean leaves) {
    if (!leaves) return mapVars.size() + 1; // Number of Maps plus 1 Array component
    else {
      int count = 0;
      for (Enumeration e = mapVars.elements(); e.hasMoreElements(); ) {
        BaseType bt = (BaseType) e.nextElement();
        count += bt.elementCount(leaves);
      }
      count += arrayVar.elementCount(leaves);
      return count;
    }
  }

  /**
   * Adds a variable to the container.
   *
   * @param v the variable to add.
   * @param part the part of the <code>DGrid</code> to be modified. Allowed values are <code>ARRAY
   *     </code> or <code>MAPS</code>.
   * @exception IllegalArgumentException if an invalid part was given.
   */
  @Override
  public void addVariable(BaseType v, int part) {

    if (!(v instanceof DArray))
      throw new IllegalArgumentException(
          "Grid `" + getName() + "'s' member `" + arrayVar.getName() + "' must be an array");

    v.setParent(this);

    switch (part) {
      case ARRAY:
        arrayVar = (DArray) v;
        return;

      case MAPS:
        mapVars.addElement(v);
        return;

      default:
        throw new IllegalArgumentException("addVariable(): Unknown Grid part");
    }
  }

  /**
   * Returns the named variable.
   *
   * @param name the name of the variable.
   * @return the named variable.
   * @exception NoSuchVariableException if the named variable does not exist in this container.
   */
  @Override
  public BaseType getVariable(String name) throws NoSuchVariableException {

    int dotIndex = name.indexOf('.');

    if (dotIndex != -1) { // name contains "."

      String aggregate = name.substring(0, dotIndex);
      String field = name.substring(dotIndex + 1);

      BaseType aggRef = getVariable(aggregate);
      if (aggRef instanceof DConstructor)
        return ((DConstructor) aggRef).getVariable(field); // recurse
      else
        ; // fall through to throw statement
    } else {
      if (arrayVar.getName().equals(name)) return arrayVar;

      for (Enumeration e = mapVars.elements(); e.hasMoreElements(); ) {
        BaseType v = (BaseType) e.nextElement();
        if (v.getName().equals(name)) return v;
      }
    }
    throw new NoSuchVariableException("DGrid.getVariable() No such variable: '" + name + "'");
  }

  /**
   * Gets the indexed variable. For a DGrid the index 0 returns the <code>DArray</code> and indexes
   * 1 and higher return the associated map <code>Vector</code>s.
   *
   * @param index the index of the variable in the <code>Vector</code> Vars.
   * @return the indexed variable.
   * @exception NoSuchVariableException if the named variable does not exist in this container.
   */
  @Override
  public BaseType getVar(int index) throws NoSuchVariableException {
    if (index == 0) {
      return arrayVar;
    } else {
      int i = index - 1;
      if (i < mapVars.size()) return ((BaseType) mapVars.elementAt(i));
      else
        throw new NoSuchVariableException(
            "DGrid.getVariable() No Such variable: " + index + " - 1)");
    }
  }

  /**
   * Private class for implemantation of the Enumeration. Because DStructure and DSequence are
   * simpler classes and use a single Vector, their implementations of getVariables aren't as fancy.
   */
  class EnumerateDGrid implements Enumeration {
    boolean array;
    Enumeration e;

    EnumerateDGrid() {
      array = false; // true when the array is/has being/been
      // visited
      e = mapVars.elements();
    }

    @Override
    public boolean hasMoreElements() {
      return (array == false) || e.hasMoreElements();
    }

    @Override
    public Object nextElement() {
      if (!array) {
        array = true;
        return arrayVar;
      } else {
        return e.nextElement();
      }
    }
  }

  /**
   * Return an Enumeration that can be used to iterate over the members of a Structure. This
   * implementation provides access to the elements of the Structure. Each Object returned by the
   * Enumeration can be cast to a BaseType.
   *
   * @return An Enumeration
   */
  @Override
  public Enumeration getVariables() {
    return new EnumerateDGrid();
  }

  /**
   * Checks for internal consistency. For <code>DGrid</code>, verify that the map variables have
   * unique names and match the number of dimensions of the array variable.
   *
   * @param all for complex constructor types, this flag indicates whether to check the semantics of
   *     the member variables, too.
   * @exception BadSemanticsException if semantics are bad, explains why.
   * @see BaseType#checkSemantics(boolean)
   */
  @Override
  public void checkSemantics(boolean all) throws BadSemanticsException {
    super.checkSemantics(all);

    Util.uniqueNames(mapVars, getName(), getTypeName());

    if (arrayVar == null)
      throw new BadSemanticsException(
          "DGrid.checkSemantics(): Null grid base array in `" + getName() + "'");

    // check semantics of array variable
    arrayVar.checkSemantics(all);

    // enough maps?
    if (mapVars.size() != arrayVar.numDimensions())
      throw new BadSemanticsException(
          "DGrid.checkSemantics(): The number of map variables for grid `"
              + getName()
              + "' does not match the number of dimensions of `"
              + arrayVar.getName()
              + "'");

    // ----- I added this next test 12/3/99. As soon as I did I questioned whether or not
    // ----- it adds any value. ie: Can it ever happen that this test fails? I don't think
    // ----- so now that I have written it...  ndp 12/3/99

    // Is the size of the maps equal to the size of the cooresponding dimensions?
    Enumeration emap = mapVars.elements();

    Enumeration edims = arrayVar.getDimensions();
    int dim = 0;
    while (emap.hasMoreElements() && edims.hasMoreElements()) {

      DArray thisMapArray = (DArray) emap.nextElement();
      Enumeration ema = thisMapArray.getDimensions();
      DArrayDimension thisMapDim = (DArrayDimension) ema.nextElement();

      DArrayDimension thisArrayDim = (DArrayDimension) edims.nextElement();

      if (thisMapDim.getSize() != thisArrayDim.getSize()) {

        throw new BadSemanticsException(
            "In grid '"
                + getName()
                + " The size of dimension "
                + dim
                + " in the array component '"
                + arrayVar.getName()
                + "is not equal to the size of the coresponding map vector '"
                + thisMapArray.getName()
                + ".");
      }
      dim++;
    }
    // ----- end  ndp 12/3/99

  }

  /**
   * Write the variable's declaration in a C-style syntax. This function is used to create textual
   * representation of the Data Descriptor Structure (DDS). See <em>The DODS User Manual</em> for
   * information about this structure.
   *
   * @param os The <code>PrintWriter</code> on which to print the declaration.
   * @param space Each line of the declaration will begin with the characters in this string.
   *     Usually used for leading spaces.
   * @param print_semi a boolean value indicating whether to print a semicolon at the end of the
   *     declaration.
   * @see BaseType#printDecl(PrintWriter, String, boolean)
   */
  @Override
  public void printDecl(PrintWriter os, String space, boolean print_semi, boolean constrained) {
    os.println(space + getTypeName() + " {");
    os.println(space + " ARRAY:");
    arrayVar.printDecl(os, space + "    ", true);
    os.println(space + " MAPS:");
    for (Enumeration e = mapVars.elements(); e.hasMoreElements(); ) {
      BaseType bt = (BaseType) e.nextElement();
      bt.printDecl(os, space + "    ", true);
    }
    os.print(space + "} " + getName());
    if (print_semi) os.println(";");
  }

  /**
   * Prints the value of the variable, with its declaration. This function is primarily intended for
   * debugging DODS applications and text-based clients such as geturl.
   *
   * @param os the <code>PrintWriter</code> on which to print the value.
   * @param space this value is passed to the <code>printDecl</code> method, and controls the
   *     leading spaces of the output.
   * @param print_decl_p a boolean value controlling whether the variable declaration is printed as
   *     well as the value.
   * @see BaseType#printVal(PrintWriter, String, boolean)
   */
  @Override
  public void printVal(PrintWriter os, String space, boolean print_decl_p) {

    if (print_decl_p) {
      printDecl(os, space, false);
      os.print(" = ");
    }

    os.print("{ ARRAY: ");
    arrayVar.printVal(os, "", false);

    os.print(" MAPS: ");
    for (Enumeration e = mapVars.elements(); e.hasMoreElements(); ) {
      BaseType bt = (BaseType) e.nextElement();
      bt.printVal(os, "", false);
      if (e.hasMoreElements()) os.print(", ");
    }
    os.print(" }");

    if (print_decl_p) os.println(";");
  }

  /**
   * Reads data from a <code>DataInputStream</code>. This method is only used on the client side of
   * the DODS client/server connection.
   *
   * @param source a <code>DataInputStream</code> to read from.
   * @param sv the <code>ServerVersion</code> returned by the server.
   * @param statusUI the <code>StatusUI</code> object to use for GUI updates and user cancellation
   *     notification (may be null).
   * @exception EOFException if EOF is found before the variable is completely deserialized.
   * @exception IOException thrown on any other InputStream exception.
   * @exception DataReadException if an unexpected value was read.
   * @see ClientIO#deserialize(DataInputStream, ServerVersion, StatusUI)
   */
  @Override
  public synchronized void deserialize(DataInputStream source, ServerVersion sv, StatusUI statusUI)
      throws IOException, DataReadException {
    arrayVar.deserialize(source, sv, statusUI);
    for (Enumeration e = mapVars.elements(); e.hasMoreElements(); ) {
      if (statusUI != null && statusUI.userCancelled())
        throw new DataReadException("DGrid.deserialize(): User cancelled");
      ClientIO bt = (ClientIO) e.nextElement();
      bt.deserialize(source, sv, statusUI);
    }
  }

  /**
   * Writes data to a <code>DataOutputStream</code>. This method is used primarily by GUI clients
   * which need to download DODS data, manipulate it, and then re-save it as a binary file.
   *
   * @param sink a <code>DataOutputStream</code> to write to.
   * @exception IOException thrown on any <code>OutputStream</code> exception.
   */
  @Override
  public void externalize(DataOutputStream sink) throws IOException {
    arrayVar.externalize(sink);
    for (Enumeration e = mapVars.elements(); e.hasMoreElements(); ) {
      ClientIO bt = (ClientIO) e.nextElement();
      bt.externalize(sink);
    }
  }
}
