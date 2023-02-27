/* 
 * Screen Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.ema.EmaAttribute;
import com.cohort.ema.EmaClass;
import com.cohort.util.Math2;
import com.cohort.util.String2;

import gov.noaa.pfel.coastwatch.util.IntObject;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * This is the super class of all screens.
 * A screen is the part of the form in a Browser (usually below the "Edit" line).
 * Each User has its own instance of each relevant screen.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-12
 */
public abstract class Screen  {

    protected int editOption; //the edit option# for this screen
    protected OneOf oneOf;
    protected Shared shared;
    protected EmaClass emaClass;
    protected boolean doTally;
    final static boolean displayErrorMessages = false;
    final static protected String expandingBlankRow = 
        "    <tr style=\"background-color:#FFFFFF;\" >\n" +  //was: height:80%;
        "      <td>&nbsp;</td>\n" +
        "      <td style=\"width:90%;\">&nbsp;</td>\n" +
        "    </tr>\n"; 

    /**
     * Get the screens "Edit : " option number.
     */
    public int editOption() {
        return editOption;
    }

    /**
     * Reset the shared class.
     */
    public void setShared(Shared shared) {
        this.shared = shared;
    }

    /**
     * This determines if the submitter's name matches the name of 
     * a Get button (e.g., getAsc).
     *
     * @param submitter
     * @return true if the submitter's name matches the name of 
     *    a Get button (e.g., getAsc); else false. 
     */
    public abstract boolean submitterIsAGetButton(String submitter);

    /**
     * This is a convenience method which calls emaClass.setBeginRow,
     * calls emaAttribute.setLabel, and htmlSB.append(emaAttribute.getTableEntry...).
     */
    public void addTableEntry(EmaAttribute emaAttribute, String originalLabel, String value, 
            IntObject rowNumber, IntObject step, StringBuilder htmlSB) {
        emaClass.setBeginRow(oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)));
        emaAttribute.setLabel(String2.substitute(originalLabel, "" + (step.i++), null, null));
        htmlSB.append(emaAttribute.getTableEntry(value, displayErrorMessages));
    }

}
