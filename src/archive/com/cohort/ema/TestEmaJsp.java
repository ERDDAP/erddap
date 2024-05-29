/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.ema;


/**
 * This is an example of a small Ema jsp.
 */
public class TestEmaJsp extends EmaClass  {


    /**
     * Constructor
     */
    public TestEmaJsp() {
        super("com.cohort.ema.TestEmaJsp"); 

        addAttribute(new EmaGroupBegin(this, "beginDateAndTimeGroup"));
        addAttribute(new EmaDateTimeSelect2(this, "selectDateTimeYMDHMS2"));
        addAttribute(new EmaDateTimeSelect2(this, "selectDateTimeYMD2"));
        addAttribute(new EmaDateTimeSelect(this, "selectDateTimeYMDHMSA"));
        addAttribute(new EmaDateTimeSelect(this, "selectDateTimeYMDHMS"));
        addAttribute(new EmaDateTimeSelect(this, "selectDate"));
        addAttribute(new EmaDateTimeSelect(this, "selectTime"));
        addAttribute(new EmaDateTimeSelect(this, "selectTimeHM"));
        addAttribute(new EmaDateTimeText(this, "textDateTimeYMDHMSA"));
        addAttribute(new EmaDateTimeText(this, "textDateTimeYMDHMS"));
        addAttribute(new EmaDateTimeText(this, "textDate"));
        addAttribute(new EmaDateTimeText(this, "textTime"));
        addAttribute(new EmaDateTimeText(this, "textTimeHM"));
        addAttribute(new EmaDateTimeText2(this, "textTime2"));
        addAttribute(new EmaMultipleSelect(this, "emaMSA"));
        addAttribute(new EmaMultipleSelect(this, "emaMSB"));
        addAttribute(new EmaMultipleSelect(this, "emaMSC"));
        addAttribute(new EmaSelect(this, "selectTabs"));
        addAttribute(new EmaButton(this, "submit1"));
        addAttribute(new EmaButton(this, "reset"));
        addAttribute(new EmaGroupEnd(this, "endDateAndTimeGroup"));

        addAttribute(new EmaSpacer(this, "spacer0")); 
        
        addAttribute(new EmaGroupBegin(this, "beginOtherAttributesGroup"));
        addAttribute(new EmaLabel(this, "label"));
        addAttribute(new EmaString(this, "name"));
//        addAttribute(new EmaString(this, "emailAddress",
//        addAttribute(new EmaString(this, "phoneNumber"));
//        addAttribute(new EmaString(this, "faxNumber"));
        addAttribute(new EmaStringBox(this, "mailingAddress"));
        addAttribute(new EmaColor(this, "color17"));
        addAttribute(new EmaColor(this, "color17dark"));
        addAttribute(new EmaSSN(this, "ssn"));
        addAttribute(new EmaColor(this, "color28"));
        addAttribute(new EmaInt(this, "int"));
        addAttribute(new EmaColor(this, "color42"));
        addAttribute(new EmaLong(this, "long"));
        addAttribute(new EmaDouble(this, "double"));
        addAttribute(new EmaInt(this, "intNoButtons"));
        addAttribute(new EmaLong(this, "longNoButtons"));
        addAttribute(new EmaDouble(this, "doubleNoButtons"));
        addAttribute(new EmaBoolean(this, "boolean"));
        addAttribute(new EmaHidden(this, "hidden"));
        addAttribute(new EmaSelect(this, "selectColumn"));
        addAttribute(new EmaSelect(this, "selectRow"));
        addAttribute(new EmaSelect(this, "select1"));
        addAttribute(new EmaSelect(this, "select3"));
        addAttribute(new EmaSelect(this, "selectNoOptions"));
        ((EmaSelect)getAttribute("selectNoOptions")).setOptions((String[])null);
        addAttribute(new EmaPassword(this, "password"));
        addAttribute(new EmaString(this, "enterSubmitsForm"));
//        addAttribute(button = new EmaButton(this, "button"));
        addAttribute(new EmaButton(this, "submit2"));
        addAttribute(new EmaGroupEnd(this, "endOtherAttributesGroup"));

        addAttribute(new EmaSpacer(this, "spacer1")); 

        //*************the wide groups
        addAttribute(new EmaGroupBegin(this, "beginWideDateAndTimeGroup"));
        addAttribute(new EmaDateTimeSelect(this, "wideDateTimeYMDHMSA"));
        addAttribute(new EmaDateTimeSelect(this, "wideDateTimeYMDHMS"));
        addAttribute(new EmaDateTimeSelect(this, "wideDate"));
        addAttribute(new EmaDateTimeSelect(this, "wideTime"));
        addAttribute(new EmaDateTimeSelect(this, "wideTimeHM"));
        addAttribute(new EmaButton(this, "submit3"));
        addAttribute(new EmaGroupEnd(this, "endWideDateAndTimeGroup"));

        addAttribute(new EmaSpacer(this, "spacer1b")); 
        
        addAttribute(new EmaGroupBegin(this, "beginWideOtherAttributesGroup"));
        addAttribute(new EmaString(this, "wideName"));
//        addAttribute(new EmaString(this, "wideEmailAddress",
//        addAttribute(new EmaString(this, "widePhoneNumber"));
//        addAttribute(new EmaString(this, "wideFaxNumber"));
        addAttribute(new EmaStringBox(this, "wideMailingAddress"));
        addAttribute(new EmaSSN(this, "wideSsn"));
        addAttribute(new EmaInt(this, "wideInt"));
        addAttribute(new EmaLong(this, "wideLong"));
        addAttribute(new EmaDouble(this, "wideDouble"));
        addAttribute(new EmaBoolean(this, "wideBoolean"));
        addAttribute(new EmaLabel(this, "wideLabel"));
        addAttribute(new EmaSelect(this, "wideSelectColumn"));
        addAttribute(new EmaSelect(this, "wideSelectRow"));
        addAttribute(new EmaSelect(this, "wideSelect1"));
        addAttribute(new EmaSelect(this, "wideSelect3"));
        addAttribute(new EmaGroupEnd(this, "endWideOtherAttributesGroup"));

        addAttribute(new EmaSpacer(this, "spacer2")); 
        addAttribute(new EmaButton(this, "submit0"));

    }
}
