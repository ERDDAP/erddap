/* 
 * CWBrowserWW180 Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * The CoastWatch Browser. See info for Browser.java.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-02-10
 */
public class CWBrowserWW180 extends Browser {

    /**
     * Constructor
     */
    public CWBrowserWW180() throws Exception {
        super("gov.noaa.pfel.coastwatch.CWBrowserWW180");
    }

    /** 
     * This gets the user (making one if needed) associated with a request.
     *
     * @param request
     * @return the UserCW associated with the request (possibly a new user)
     */
    public User getUser(HttpServletRequest request) {
        
        HttpSession session = request.getSession();
        String sessionID = session.getId();
        User user = (User)userHashMap.get(sessionID);
        if (user == null) {

            //make a User for this new session
            boolean doTally = getDoTally(request.getHeader("x-forwarded-for"));
            user = new CWUser(oneOf, shared, session, doTally); //customized for this class
            userHashMap.put(sessionID, user);
            if (doTally) nUsers++;
        }
        return user;
    }


}
