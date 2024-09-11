package esf;

/**
 * This class stores a date in the format "YYYY-MM-DD".
 */
public class EsfDate extends EsfFormattedString {

    /**
     * A constructor.
     *
     * @param newRequired indicates if this information must be provided.
     * @param newStringForLabel the stringForLabel for this attribute.
     *     It is always html text. If null, the default is used.
     * @param newToolTip the toolTip for this attribute.
     *     It is always plain text. If null, the default is used.
     * @param newString the initial value for the string stored by this object.
     *     If null, the default is used.
     */
    public EsfDate(boolean newRequired, 
            String newStringForLabel, String newToolTip, 
            String newString) {
        super(newRequired, 
            newStringForLabel == null?
                "Date: " : 
                newStringForLabel, 
            newToolTip == null? 
                "The date must have the format \"YYYY-MM-DD\"." : 
                newToolTip, 
            newString == null? 
                "" : 
                newString, 
            11, //fieldsize 
            "(1|2)\\d{3}\\-(1|2\\d{2}\\-\\d{4}");
    }
}