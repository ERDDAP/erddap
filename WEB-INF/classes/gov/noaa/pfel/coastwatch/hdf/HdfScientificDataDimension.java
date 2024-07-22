/*
 * HdfScientificDataDimension Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.hdf;

import com.cohort.util.String2;
import com.cohort.util.Test;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * This represents an HdfScientificDataDimension tag (DFTAG_SDD tagType = 0x02bd).
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-01
 */
public class HdfScientificDataDimension extends HdfTag {

  public short rank; // Number of dimensions
  public int dim_n[]; // Number of values along the nth dimension
  public short data_NT_ref; // Reference number of DFTAG_NT for data
  public short
      scale_NT_ref_n[]; // Reference number for DFTAG_NT for the scale for the nth dimension

  /**
   * The constructor used to prepare to write the data to a file.
   *
   * @param referenceNumber
   * @param rank the number of dimensions
   * @param dim_n the number of values along the nth dimension
   * @param data_NT_ref the reference number of DFTAG_NT for data
   * @param scale_NT_ref_n the reference numbers for DFTAG_NT's for the scale for the n dimensions
   */
  public HdfScientificDataDimension(
      short referenceNumber, short rank, int dim_n[], short data_NT_ref, short scale_NT_ref_n[]) {

    tagType = 0x02bd;
    this.referenceNumber = referenceNumber;
    this.rank = rank;
    this.dim_n = dim_n;
    this.data_NT_ref = data_NT_ref;
    this.scale_NT_ref_n = scale_NT_ref_n;
  }

  /**
   * This returns the length of the data (in bytes)
   *
   * @return length
   */
  @Override
  public int getLength() {
    return 6 + 8 * rank;
  }

  /**
   * This writes the class's information to the dataOutputSream.
   *
   * @param stream
   */
  @Override
  public void writeData(DataOutputStream stream) throws Exception {
    stream.writeShort(rank);
    for (int i = 0; i < rank; i++)
      stream.writeInt(dim_n[i]); // Number of values along the nth dimension
    stream.writeShort((short) 0x6a); // e.g., HdfNumericType tag #
    stream.writeShort(data_NT_ref); // Reference number of DFTAG_NT for data
    for (int i = 0; i < rank; i++) {
      stream.writeShort((short) 0x6a); // e.g., HdfNumericType tag #
      stream.writeShort(
          scale_NT_ref_n[i]); // Reference number for DFTAG_NT for the scale for the nth dimension
    }
  }

  /**
   * The constructor used to read data from a file.
   *
   * @param referenceNumber
   * @param length the number of bytes of data
   * @param stream
   * @throws Exception
   */
  public HdfScientificDataDimension(short referenceNumber, int length, DataInputStream stream)
      throws Exception {

    tagType = 0x02bd;
    this.referenceNumber = referenceNumber;
    rank = stream.readShort();
    dim_n = new int[rank];
    for (int i = 0; i < rank; i++)
      dim_n[i] = stream.readInt(); // Number of values along the nth dimension
    short tShort = stream.readShort();
    Test.ensureEqual(
        tShort, 0x6a, "Trouble in HdfScientificDataDimension."); // e.g., HdfNumericType tag #
    data_NT_ref = stream.readShort(); // Reference number of DFTAG_NT for data
    scale_NT_ref_n = new short[rank];
    for (int i = 0; i < rank; i++) {
      tShort = stream.readShort();
      Test.ensureEqual(
          tShort, 0x6a, "Trouble in HdfScientificDataDimension."); // e.g., HdfNumericType tag #
      scale_NT_ref_n[i] =
          stream.readShort(); // Reference number for DFTAG_NT for the scale for the nth dimension
    }
  }

  /**
   * This replaces the default toString with a better toString.
   *
   * @return a string describing this object
   */
  @Override
  public String toString() {

    return "HdfScientificDataDimension"
        + "[tagType=0x"
        + Integer.toHexString(tagType)
        + ", referenceNumber="
        + referenceNumber
        + ", rank="
        + rank
        + ", dim_n="
        + String2.toCSSVString(dim_n)
        + ", data_NT_ref_n="
        + data_NT_ref
        + ", scale_NT_ref_n="
        + String2.toCSSVString(scale_NT_ref_n)
        + "]";
  }
}
