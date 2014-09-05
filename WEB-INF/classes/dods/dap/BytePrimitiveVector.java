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

/**
 * A vector of bytes.
 *
 * @version $Revision: 1.7 $
 * @author jehamby
 * @see PrimitiveVector
 */
public class BytePrimitiveVector extends PrimitiveVector implements Cloneable {
  /** the array of <code>byte</code> values. */
  private byte vals[];

  /**
   * Constructs a new <code>BytePrimitiveVector</code>.
   * @param var the template <code>BaseType</code> to use.
   */
  public BytePrimitiveVector(BaseType var) {
    super(var);
  }

  /**
   * Returns a clone of this <code>BytePrimitiveVector</code>.  A deep
   * copy is performed on all data inside the variable.
   *
   * @return a clone of this <code>BytePrimitiveVector</code>.
   */
  public Object clone() {
    BytePrimitiveVector v = (BytePrimitiveVector)super.clone();
    if (vals != null) {
      v.vals = new byte[vals.length];
      System.arraycopy(vals, 0, v.vals, 0, vals.length);
    }
    return v;
  }

  /**
   * Returns the number of elements in the array.
   * @return the number of elements in the array.
   */
  public int getLength() {
    return vals.length;
  }

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
  public void setLength(int len) {
    vals = new byte[len];
  }


  /**
   * Return the i'th value as a <code>byte</code>.
   * @param i the index of the value to return.
   * @return the i'th value.
   */
  public final byte getValue(int i) {
    return vals[i];
  }

  /**
   * Set the i'th value of the array.
   * @param i the index of the value to set.
   * @param newVal the new value.
   */
  public final void setValue(int i, byte newVal) {
    vals[i] = newVal;
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
  public void printVal(PrintWriter os, String space) {
    int len = vals.length;
    for(int i=0; i<len-1; i++) {
      // convert to unsigned and print
      os.print(((int)vals[i]) & 0xFF);
      os.print(", ");
    }
    // print last value, if any, without trailing comma
    if(len > 0)
      os.print(((int)vals[len-1]) & 0xFF);
  }

  /**
   * Prints the value of a single variable in this vector.
   * method is used by <code>DArray</code>'s <code>printVal</code> method.
   *
   * @param os the <code>PrintWriter</code> on which to print the value.
   * @param index the index of the variable to print.
   * @see DArray#printVal(PrintWriter, String, boolean)
   */
  public void printSingleVal(PrintWriter os, int index) {
    // convert to unsigned and print
    os.print(((int)vals[index]) & 0xFF);
  }

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
  public synchronized void deserialize(DataInputStream source,
                                       ServerVersion sv,
                                       StatusUI statusUI)
       throws IOException, EOFException, DataReadException {

    int modFour = vals.length%4;
    // number of bytes to pad
    int pad = (modFour != 0) ? (4-modFour) : 0;

    for(int i=0; i<vals.length; i++) {
      vals[i] = source.readByte();
      if (statusUI != null) {
        statusUI.incrementByteCount(1);
        if (statusUI.userCancelled())
          throw new DataReadException("User cancelled");
      }
    }
    // pad out to a multiple of four bytes
    byte unused;
    for(int i=0; i<pad; i++)
      unused = source.readByte();
    if (statusUI != null)
      statusUI.incrementByteCount(pad);
  }

  /**
   * Writes data to a <code>DataOutputStream</code>. This method is used
   * primarily by GUI clients which need to download DODS data, manipulate
   * it, and then re-save it as a binary file.
   *
   * @param sink a <code>DataOutputStream</code> to write to.
   * @exception IOException thrown on any <code>OutputStream</code>
   * exception.
   */
  public void externalize(DataOutputStream sink) throws IOException {
    int modFour = vals.length%4;
    // number of bytes to pad
    int pad = (modFour != 0) ? (4-modFour) : 0;

    for(int i=0; i<vals.length; i++) {
      sink.writeByte(vals[i]);
    }
    // pad out to a multiple of four bytes
    for(int i=0; i<pad; i++)
      sink.writeByte(0);
  }

  /**
   * Write a subset of the data to a <code>DataOutputStream</code>.
   *
   * @param sink a <code>DataOutputStream</code> to write to.
   * @param start starting index (i=start)
   * @param stop ending index (i<=stop)
   * @param stride index stride (i+=stride)
   * @exception IOException thrown on any <code>OutputStream</code> exception.
   */
  public void externalize(DataOutputStream sink, int start, int stop, int stride) throws IOException {
    int count = 0;
    for (int i=start; i<=stop; i+=stride) {
      sink.writeByte(vals[i]);
      count++;
    }

    // pad out to a multiple of four bytes
    int modFour = count%4;
    int pad = (modFour != 0) ? (4-modFour) : 0;
    for(int i=0; i<pad; i++)
      sink.writeByte(0);
  }

  /**
   * Create a new primitive vector using a subset of the data.
   *
   * @param start starting index (i=start)
   * @param stop ending index (i<=stop)
   * @param stride index stride (i+=stride)
   * @return new primitive vector, of type BytePrimitiveVector.
   */
  public PrimitiveVector subset( int start, int stop, int stride) {
    BytePrimitiveVector n = new BytePrimitiveVector(getTemplate());
    stride = Math.max( stride, 1);
    stop = Math.max( start, stop);
    int length = 1 + (stop - start) / stride;
    n.setLength( length);

    int count=0;
    for (int i=start; i<=stop; i+=stride) {
      n.setValue(count, vals[i]);
      count++;
    }
    return n;
  }


    /**
    * Returns (a reference to) the internal storage for this PrimitiveVector
    * object.
    * <h2>WARNING:</h2>
    * Because this method breaks encapsulation rules the user must beware!
    * If we (the DODS prgramming team) choose to change the internal
    * representation(s) of these types your code will probably break.
    * <p>
    * This method is provided as an optimization to eliminate massive
    * copying of data.
    *
    * @return The internal array of bytes.
    */
    public Object getInternalStorage() {
        return(vals);
    }

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
    public void setInternalStorage(Object o) {
      vals = (byte []) o;
    }


}
