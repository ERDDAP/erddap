/* This file is part of the EMA project and is 
 * Copyright (c) 2019 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.util;

import java.util.Comparator;

/**
 * This is used by StringArray to do a case-insensitive sort 
 * (better than String.CASE_INSENSITIVE_ORDER).
 */
public class StringHolderComparatorIgnoreCase implements Comparator {


    /**
     * This is required for the Comparator interface.
     *
     * @param o1 
     * @param o2 
     * @return returns a negative integer, zero, or a positive integer if the 
     *   value at index1 is less than, equal to, or greater than 
     *   the value at index2.  
     *   Think "o1 - o2".
     */
    public int compare(Object o1, Object o2) {
        if (o1 == null)
            return o2 == null? 0 : -1;
        if (o2 == null) 
            return 1;
        return ((StringHolder)o1).compareToIgnoreCase((StringHolder)o2); //it is fancy
    }

    /**
     * This is required for the Comparator interface.
     *
     * @param obj usually another RowComparator
     */
    public boolean equals(Object obj) {
        return obj != null && obj instanceof StringHolderComparatorIgnoreCase;
    }

}

