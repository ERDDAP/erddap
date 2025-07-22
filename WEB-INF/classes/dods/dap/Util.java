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

import java.util.ArrayList;
import java.util.List;

/**
 * The Util class holds static methods used by this package.
 *
 * @version $Revision: 1.1.1.1 $
 * @author jehamby
 */
class Util {
  /**
   * Compares elements in a <code>List</code> of <code>BaseType</code>s and throw a <code>
   * BadSemanticsException</code> if there are any duplicate elements.
   *
   * @param v The <code>List</code> to check
   * @param varName the name of the variable which called us
   * @param typeName the type name of the variable which called us
   * @exception BadSemanticsException if there are duplicate elements
   * @exception IndexOutOfBoundsException if size doesn't match the number of elements in the <code>
   *     Enumeration</code>
   */
  static void uniqueNames(List<BaseType> v, String varName, String typeName)
      throws BadSemanticsException {
    List<String> nameList = new ArrayList<>();
    for (BaseType bt : v) {
      nameList.add(bt.getName());
    }
    nameList.sort(null);

    for (int i = 1; i < nameList.size(); i++) {
      if (nameList.get(i - 1).equals(nameList.get(i))) {
        throw new BadSemanticsException(
            "The variable `"
                + nameList.get(i)
                + "' is used more than once in "
                + typeName
                + " `"
                + varName
                + "'");
      }
    }
  }

  /**
   * This function escapes non-printable characters and quotes. This is used to make <code>printVal
   * </code> output <code>DString</code> data in the same way as the C++ version. Since Java
   * supports Unicode, this will need to be altered if it's desired to print <code>DString</code> as
   * UTF-8 or some other character encoding.
   *
   * @param s the input <code>String</code>.
   * @return the escaped <code>String</code>.
   */
  static String escattr(String s) {
    StringBuilder buf = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == ' ' || (c >= '!' && c <= '~')) {
        // printable ASCII character
        buf.append(c);
      } else {
        // non-printable ASCII character: print as unsigned octal integer
        // padded with leading zeros
        buf.append('\\');
        String numVal = Integer.toString((int) c & 0xFF, 8);
        buf.append("0".repeat(Math.max(0, (3 - numVal.length()))));
        buf.append(numVal);
      }
    }
    return buf.toString();
  }
}
