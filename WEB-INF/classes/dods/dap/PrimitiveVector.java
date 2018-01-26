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

import dods.dap.Server.*;

/**
 * A helper class for <code>DVector</code>.  It allows <code>DVector</code>,
 * and by extension, <code>DArray</code> and <code>DList</code>, to use more
 * efficient primitive types to hold large arrays of data.
 * A <code>PrimitiveVector</code> class is defined for each
 * primitive type supported in DODS, and a
 * <code>BaseTypePrimitiveVector</code> class handles <code>DArray</code>s
 * and <code>DList</code>s of compound types.
 *
 * @version $Revision: 1.12 $
 * @author jehamby
 * @see BaseType
 * @see DVector
 */
abstract public class PrimitiveVector implements ClientIO, Cloneable {
  /**
   * Template variable to use for <code>printDecl</code> and
   * <code>deserialize</code> (<code>BaseTypePrimitiveVector</code> only).
   */
  private BaseType var;

  /**
   * Constructs a new <code>PrimitiveVector</code>.
   * @param var the template <code>BaseType</code> to use.
   */
  public PrimitiveVector(BaseType var) {
    this.var = var;
  }

  /**
   * Returns a clone of this <code>PrimitiveVector</code>.  A deep copy is
   * performed on all data inside the variable.
   *
   * @return a clone of this <code>PrimitiveVector</code>.
   */
  public Object clone() {
    try {
      PrimitiveVector v = (PrimitiveVector)super.clone();
      v.var = (BaseType)var.clone();
      return v;
    }
    catch (CloneNotSupportedException e) {
      // this shouldn't happen, since we are Cloneable
      throw new InternalError();
    }
  }

  /**
   * Returns the template variable for this vector.
   * @return the template variable for this vector.
   * @see BaseTypePrimitiveVector#deserialize(DataInputStream, ServerVersion, StatusUI)
   */
  public final BaseType getTemplate() {
    return var;
  }

  /**
   * Returns the number of elements in the array.
   * @return the number of elements in the array.
   */
  abstract public int getLength();

  /**
   * Sets the number of elements in the array.  Allocates a new primitive
   * array of the desired size.  Note that if this is called multiple times,
   * the old array and its contents will be lost.
   * <p>
   * Only called inside of <code>deserialize</code> method or in derived
   * classes on server.
   *
   * @param len the number of elements in the array.
   */
  abstract public void setLength(int len);

  /**
   * Reads data from a <code>DataInputStream</code>. This method is only used
   * on the client side of the DODS client/server connection.
   *
   * @param source a <code>DataInputStream</code> to read from.
   * @param sv The <code>ServerVersion</code> returned by the server.
   *    (used by <code>DSequence</code> to determine which protocol version was
   *    used).
   * @param statusUI The <code>StatusUI</code> object to use for GUI updates
   *    and user cancellation notification (may be null).
   * @exception DataReadException when invalid data is read, or if the user
   *     cancels the download.
   * @exception EOFException if EOF is found before the variable is completely
   *     deserialized.
   * @exception IOException thrown on any other InputStream exception.
   * @see ClientIO#deserialize(DataInputStream, ServerVersion, StatusUI)
   */
  abstract public void deserialize(DataInputStream source, ServerVersion sv,
                                   StatusUI statusUI)
       throws IOException, EOFException, DataReadException;

  /**
     * Writes data to a <code>DataOutputStream</code>. This method is used
     * primarily by GUI clients which need to download DODS data, manipulate
     * it, and then re-save it as a binary file.
     *
     * @param sink a <code>DataOutputStream</code> to write to.
     * @exception IOException thrown on any <code>OutputStream</code>
     * exception.
     */
  abstract public void externalize(DataOutputStream sink) throws IOException;

    /**
   * Write a subset of the data to a <code>DataOutputStream</code>.
   *
   * @param sink a <code>DataOutputStream</code> to write to.
   * @param start starting index (i=start)
   * @param stop ending index (i<=stop)
   * @param stride index stride (i+=stride)
   * @exception IOException thrown on any <code>OutputStream</code> exception.
   */
  // abstract public void externalize(DataOutputStream sink, int start, int stop, int stride) throws IOException;


    /**
    * Write the variable's declaration in a C-style syntax. This
    * method is used to create textual representation of the Data
    * Descriptor Structure (DDS).
    *
    * @param os The <code>PrintWriter</code> on which to print the
    *    declaration.
    * @param space Each line of the declaration will begin with the
    *    characters in this string.  Usually used for leading spaces.
    * @param print_semi a boolean value indicating whether to print a
    *    semicolon at the end of the declaration.
    * @param constrained a boolean value indicating whether to print
    *    the declartion dependent on the projection information. <strong>This
    *    is only used by Server side code.</strong>
    *
    * @see BaseType#printDecl(PrintWriter, String, boolean, boolean)
    */
    public final void printDecl(PrintWriter os, String space,
                    boolean print_semi, boolean constrained) {

                //os.println("PrimitiveVector.printDecl()");
                //os.println(var.getTypeName()+".isProject():"+((ServerMethods)var).isProject());
                var.printDecl(os, space, print_semi,constrained);
    }

    /**
    * Write the variable's declaration in a C-style syntax. This
    * method is used to create textual representation of the Data
    * Descriptor Structure (DDS).
    *
    * @param os The <code>PrintWriter</code> on which to print the
    *    declaration.
    * @param space Each line of the declaration will begin with the
    *    characters in this string.  Usually used for leading spaces.
    * @param print_semi a boolean value indicating whether to print a
    *    semicolon at the end of the declaration.
    * @see BaseType#printDecl(PrintWriter, String, boolean)
    */
    public final void printDecl(PrintWriter os, String space,
                    boolean print_semi) {
        printDecl(os,space, print_semi,false);
    }

  /**
   * Prints the value of all variables in this vector.  This
   * method is primarily intended for debugging DODS applications and
   * text-based clients such as geturl.
   *
   * @param os the <code>PrintWriter</code> on which to print the value.
   * @param space this value is passed to the <code>printDecl</code> method,
   *    and controls the leading spaces of the output.
   * @see BaseType#printVal(PrintWriter, String, boolean)
   */
  abstract public void printVal(PrintWriter os, String space);

  /**
   * Prints the value of a single variable in this vector.
   * method is used by <code>DArray</code>'s <code>printVal</code> method.
   *
   * @param os the <code>PrintWriter</code> on which to print the value.
   * @param index the index of the variable to print.
   * @see DArray#printVal(PrintWriter, String, boolean)
   */
  abstract public void printSingleVal(PrintWriter os, int index);



    /**
    * Returns (a reference to) the internal storage for PrimitiveVector.
    * <h2><i>WARNING:</i></h2>
    * Because this method breaks encapsulation rules the user must beware!
    * If we (the DODS prgramming team) choose to change the internal
    * representation(s) of these types your code will probably break.
    * <p>
    * This method is provided as an optimization to eliminate massive
    * copying of data.
    */
    abstract public Object getInternalStorage();

    /**
    * Set the internal storage for PrimitiveVector.
    * <h2><i>WARNING:</i></h2>
    * Because this method breaks encapsulation rules the user must beware!
    * If we (the DODS prgramming team) choose to change the internal
    * representation(s) of these types your code will probably break.
    * <p>
    * This method is provided as an optimization to eliminate massive
    * copying of data.
    */
    abstract public void setInternalStorage(Object o);

  /**
   * Create a new primitive vector using a subset of the data.
   *
   * @param start starting index (i=start)
   * @param stop ending index (i<=stop)
   * @param stride index stride (i+=stride)
   * @return new primitive vector
   */
   abstract public PrimitiveVector subset( int start, int stop, int stride);

}
