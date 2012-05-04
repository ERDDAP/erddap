/* 
 * OutputStreamSourceSimple Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import java.io.OutputStream;

/**
 * OutputStreamSourceSimple provides an OutputStream upon request.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2007-08-01
 */
public class OutputStreamSourceSimple implements OutputStreamSource {

    private OutputStream outputStream;

    /**
     * The constructor.
     *
     * @param os
     */
    public OutputStreamSourceSimple(OutputStream os) {
        outputStream = os;
    }

    /**
     * A variant of outputStream() for when the contentLength isn't known.
     */
    public OutputStream outputStream(String characterEncoding) throws Throwable {
        return outputStream(characterEncoding, -1);
    }

        
    /**
    /**
     * This returns an OutputStream.
     * If called repeatedly, this returns the same outputStream.
     *
     * @param characterEncoding e.g., "" (for none specified), "UTF-8", or "" (for DAP).
     *     This parameter only matters the first time this method is called.
     *     This only matters for some subclasses.
     * @param contentLength the number of bytes that will be sent (or -1 if not known).
     *     (In this subclass it is ignored.)
     * @throws Throwable if trouble
     */
    public OutputStream outputStream(String characterEncoding, long contentLength) 
        throws Throwable {

        return outputStream;
    }

}



