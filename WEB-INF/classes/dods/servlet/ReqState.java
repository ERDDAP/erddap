// $Id: ReqState.java,v 1.1.2.2 2004/08/26 21:00:35 ndp Exp $
/*
 * Copyright 1997-2000 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package dods.servlet;

import java.util.Enumeration;
import java.util.StringTokenizer;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;

/**
 * User request information gets cached here for easy access by downstream
 * code. This object may not be immutable/thread safe.
 *
 * @author Nathan Potter
 */

public class ReqState {


    /**
     * ************************************************************************
     * Default directory for the cached DDS files. This
     * presupposes that the server is going to use locally
     * cached DDS files.
     *
     */
    private String defaultDDScache = "/usr/local/DODS/datasets/dds";


    /**
     * ************************************************************************
     * Default directory for the cached DAS files. This
     * presupposes that the server is going to use locally
     * cached DAS files.
     *
     */
    private String defaultDAScache = "/usr/local/DODS/datasets/das";


    /**
     * ************************************************************************
     * Default directory for the cached INFO files. This
     * presupposes that the server is going to use locally
     * cached INFO files.
     *
     */
    private String defaultINFOcache = "/usr/local/DODS/datasets/info";


    private String dataSetName;
    private String requestSuffix;
    private String CE;
    private boolean acceptsCompressed;
    private Object obj = null;
    private String serverClassName;

    private ServletConfig myServletConfig;
    private HttpServletRequest myHttpRequest;

    /**
     * Functionality has beed moved from the servlet into ReqState. This machinery
     * negates the need for this constructor with it's complex method signature.
     *
     * @param dataSetName
     * @param requestSuffix
     * @param CE
     * @param acceptsCompressed
     * @param sc
     * @param serverClassName
     * @Deprecated
     */
    public ReqState(String dataSetName,
                        String requestSuffix,
                        String CE,
                        boolean acceptsCompressed,
                        ServletConfig sc,
                        String serverClassName) {

        this.dataSetName = dataSetName;
        this.requestSuffix = requestSuffix;
        this.CE = CE;
        this.acceptsCompressed = acceptsCompressed;
        this.myServletConfig = sc;
        this.serverClassName = serverClassName;
    }

    /**
     * @param myRequest       The HttpServletRequest object asscoicated with this client request.
     * @param sc
     * @param serverClassName
     * @throws BadURLException
     */
    public ReqState(HttpServletRequest myRequest,
                        ServletConfig sc,
                        String serverClassName) throws BadURLException {

        this.myServletConfig = sc;
        this.myHttpRequest = myRequest;
        this.serverClassName = serverClassName;



        // Get the constraint expression from the request object and
        // convert all those special characters denoted by a % sign
        this.CE = prepCE(myHttpRequest.getQueryString());

        // If there was simply no constraint then prepCE() should have returned
        // a CE equal "", the empty string. A null return indicates an error.
        if (this.CE == null) {
            throw new BadURLException();
        }


        processDodsURL();


        String servletPath = myHttpRequest.getServletPath();
        defaultDDScache = this.myServletConfig.getServletContext().getRealPath("datasets" +
                servletPath + "/dds") + "/";
        defaultDAScache = this.myServletConfig.getServletContext().getRealPath("datasets" +
                servletPath + "/das") + "/";
        defaultINFOcache = this.myServletConfig.getServletContext().getRealPath("datasets" +
                servletPath + "/info") + "/";


    }


    /**
     * *************************************************************************
     * This method is used to convert special characters into their
     * actual byte values.
     * <p/>
     * For example, in a URL the space character
     * is represented as "%20" this method will replace that with a
     * space charater. (a single value of 0x20)
     *
     * @param ce The constraint expresion string as collected from the request
     *           object with <code>getQueryString()</code>
     * @return A string containing the prepared constraint expression. If there
     *         is a problem with the constraint expression a <code>null</code> is returned.
     */
    private String prepCE(String ce) {

        int index;

        //System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ");
        //System.out.println("Prepping: \""+ce+"\"");

        if (ce == null) {
            ce = "";
            //System.out.println("null Constraint expression.");
        } else if (!ce.equals("")) {

            //System.out.println("Searching for:  %");
            index = ce.indexOf("%");
            //System.out.println("index of %: "+index);

            if (index == -1)
                return (ce);

            if (index > (ce.length() - 3))
                return (null);

            while (index >= 0) {
                //System.out.println("Found % at character " + index);

                String specChar = ce.substring(index + 1, index + 3);
                //System.out.println("specChar: \"" + specChar + "\"");

                // Convert that bad boy!
                char val = (char) Byte.parseByte(specChar, 16);
                //System.out.println("                val: '" + val + "'");
                //System.out.println("String.valueOf(val): \"" + String.valueOf(val) + "\"");


                ce = ce.substring(0, index) + String.valueOf(val) + ce.substring(index + 3, ce.length());
                //System.out.println("ce: \"" + ce + "\"");

                index = ce.indexOf("%");
                if (index > (ce.length() - 3))
                    return (null);
            }
        }

//      char ca[] = ce.toCharArray();
//      for(int i=0; i<ca.length ;i++)
//	        System.out.print("'"+(byte)ca[i]+"' ");
//	    System.out.println("");
//	    System.out.println(ce);
//	    System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ");
//      System.out.println("Returning CE: \""+ce+"\"");


        return (ce);
    }
    /***************************************************************************/










    /**
     * *************************************************************************
     * Processes an incoming <code>HttpServletRequest</code>. Uses the content of
     * the <code>HttpServletRequest</code>to create a <code>RequestState</code>
     * object in that caches the values for:
     * <ul>
     * <li> <strong>dataSet</strong> The data set name.(Accessible using
     * <code> setDataSet() </code>
     * and <code>getDataSet()</code>)</li>
     * <li> <strong>CE</strong> The constraint expression.(Accessible using
     * <code> setCE() </code>
     * and <code>getCE()</code>)</li>
     * <li> <strong>requestSuffix</strong> The request suffix, used by DODS to indicate
     * the type of response desired by the client.
     * (Accessible using
     * <code> setRequestSuffix() </code>
     * and <code>getRequestSuffix()</code>)</li>
     * <li> <strong>isClientCompressed</strong> Does the requesting client
     * accept a compressed response?</li>
     * <li> <strong>ServletConfig</strong> The <code>ServletConfig</code> object
     * for this servlet.</li>
     * <li> <strong>ServerName</strong> The class name of this server.</li>
     * <li> <strong>RequestURL</strong> THe URL that that was used to call thye servlet.</li>
     * </ul>
     *
     * @see ReqState
     */

    private void processDodsURL() {

        // Figure out the data set name.
        this.dataSetName = myHttpRequest.getPathInfo();
        this.requestSuffix = null;
        if (this.dataSetName != null) {
            // Break the path up and find the last (terminal)
            // end.
            StringTokenizer st = new StringTokenizer(this.dataSetName, "/");
            String endOPath = "";
            while (st.hasMoreTokens()) {
                endOPath = st.nextToken();
            }

            // Check the last element in the path for the
            // character "."
            int index = endOPath.lastIndexOf('.');

            //System.out.println("last index of . in \""+ds+"\": "+index);

            // If a dot is found take the stuff after it as the DODS suffix
            if (index >= 0) {
                // pluck the DODS suffix off of the end
                requestSuffix = endOPath.substring(index + 1);

                // Set the data set name to the entire path minus the
                // suffix which we know exists in the last element
                // of the path.
                this.dataSetName = this.dataSetName.substring(1, this.dataSetName.lastIndexOf('.'));
            } else { // strip the leading slash (/) from the dataset name and set the suffix to an empty string
                requestSuffix = "";
                this.dataSetName = this.dataSetName.substring(1, this.dataSetName.length());
            }
        }
    }


    /**
     * *************************************************************************
     * Evaluates the (private) request object to determine if the client that
     * sent the request accepts compressed return documents.
     *
     * @return True is the client accpets a compressed return document.
     *         False otherwise.
     */

    public boolean getAcceptsCompressed() {

        boolean isTiny;

        isTiny = false;
        String Encoding = this.myHttpRequest.getHeader("Accept-Encoding");

        if (Encoding != null)
            isTiny = Encoding.equalsIgnoreCase("deflate");
        else
            isTiny = false;

        return (isTiny);
    }

    /**
     * ***********************************************************************
     */


    public String getDataSet() {
        return dataSetName;
    }

    public String getServerName() {
        return serverClassName;
    }

    public String getServerClassName() {
        return serverClassName;
    }

    public String getRequestSuffix() {
        return requestSuffix;
    }

    public String getConstraintExpression() {
        return CE;
    }

    /**
     *  This method will attempt to get the DDS cache directory
     *  name from the servlet's InitParameters. Failing this it
     *  will return the default DDS cache directory name.
     *
     * @return The name of the DDS cache directory.
     */
    public String getDDSCache() {
        String cacheDir = getInitParameter("DDScache");
        if (cacheDir == null)
            cacheDir = defaultDDScache;
        return (cacheDir);
    }

    /**
     * Sets the default DDS Cache directory name to
     * the string <i>cachedir</i>. Note that if the servlet configuration
     * conatins an Init Parameter <i>DDSCache</i> the default
     * value will be ingnored.
     * @param cachedir
     */
    protected void setDefaultDDSCache(String cachedir){
        defaultDDScache = cachedir;
    }

    /**
     *  This method will attempt to get the DAS cache directory
     *  name from the servlet's InitParameters. Failing this it
     *  will return the default DAS cache directory name.
     *
     * @return The name of the DAS cache directory.
     */
    public String getDASCache() {
        String cacheDir = getInitParameter("DAScache");
        if (cacheDir == null)
            cacheDir = defaultDAScache;
        return (cacheDir);
    }

    /**
     * Sets the default DAS Cache directory name to
     * the string <i>cachedir</i>. Note that if the servlet configuration
     * conatins an Init Parameter <i>DASCache</i> the default
     * value will be ingnored.
     * @param cachedir
     */
    protected void setDefaultDASCache(String cachedir){
        defaultDAScache = cachedir;
    }


    /**
     *  This method will attempt to get the INFO cache directory
     *  name from the servlet's InitParameters. Failing this it
     *  will return the default INFO cache directory name.
     *
     * @return The name of the INFO cache directory.
     */
    public String getINFOCache() {
        String cacheDir = getInitParameter("INFOcache");
        if (cacheDir == null)
            cacheDir = defaultINFOcache;
        return (cacheDir);
    }

    /**
     * Sets the default INFO Cache directory name to
     * the string <i>cachedir</i>. Note that if the servlet configuration
     * conatins an Init Parameter <i>INFOcache</i> the default
     * value will be ingnored.
     * @param cachedir
     */
    protected void setDefaultINFOCache(String cachedir){
        defaultDAScache = cachedir;
    }



    public Enumeration getInitParameterNames() {
        return (myServletConfig.getInitParameterNames());
    }

    public String getInitParameter(String name) {
        return (myServletConfig.getInitParameter(name));
    }


    // for debugging, extra state, etc
    public Object getUserObject() {
        return obj;
    }

    public void setUserObject(Object userObj) {
        this.obj = userObj;
    }

    public String toString() {
        String ts;

        ts = "ReqState:\n";
        ts += "  dataset name: '" + dataSetName + "'\n";
        ts += "  suffix: '" + requestSuffix + "'\n";
        ts += "  CE: '" + CE + "'\n";
        ts += "  compressOK: " + acceptsCompressed + "\n";

        ts += "  InitParameters:\n";
        Enumeration e = getInitParameterNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            String value = getInitParameter(name);

            ts += "    " + name + ": '" + value + "'\n";
        }

        return (ts);
    }


}
