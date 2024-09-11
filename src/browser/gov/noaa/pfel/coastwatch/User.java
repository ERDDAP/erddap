/* 
 * User Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;

/**
 * This is a collection of the things unique to a user's CWBrowser session.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-12
 */
public abstract class User  {


    protected OneOf oneOf;
    protected Shared shared;
    protected HttpSession session;
    protected long lastAccessTime;
    protected boolean doTally;

    /** 
     * The constructor for User. 
     */
    public User(OneOf oneOf, Shared shared, HttpSession session, boolean doTally) {
        this.oneOf = oneOf;
        this.shared = shared;
        this.session = session;
        this.doTally = doTally;
        resetLastAccessTime();

    }


    /**
     * This resets the Shared info for this user.
     * Because this is handled by one method (with one value passed in), 
     * it has the effect of synchronizing everything done within it.
     *
     * @param shared the new Shared object
     */
    public abstract void setShared(Shared shared);

    /** This returns the current Shared object. */
    public Shared shared() {return shared;}

    /** This returns the user's session object. */
    public HttpSession session() {return session;}

    /** This returns the user's lastAccessTime. */
    public long lastAccessTime() {return lastAccessTime;}

    /** This resets the user's lastAccessTime to the current time. */
    public void resetLastAccessTime() {lastAccessTime = System.currentTimeMillis();}


    /**
     * This handles a "request" from a user, storing incoming attributes
     * as session values.
     * This updates totalNRequests, totalProcessingTime, maxProcessingTime.
     *
     * @param request 
     * @return true if all the values on the form are valid
     */
    public abstract boolean processRequest(HttpServletRequest request) throws Exception;

    /** 
     * This does most of the work (validate each screen, generate html,
     * and create the files).
     *
     * @param request is a request from a user
     * @param StringBuilder htmlSB
     * @return true if successful (no exceptions thrown)
     */
    public abstract boolean getHTMLForm(HttpServletRequest request, StringBuilder htmlSB, long startTime);

}
