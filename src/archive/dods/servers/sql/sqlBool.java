/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1999, COAS, Oregon State University
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged.
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Nathan Potter (ndp@oce.orst.edu)
//
//                        College of Oceanic and Atmospheric Scieneces
//                        Oregon State University
//                        104 Ocean. Admin. Bldg.
//                        Corvallis, OR 97331-5503
//
/////////////////////////////////////////////////////////////////////////////

package dods.servers.sql;

import java.io.*;
import java.sql.*;

import dods.dap.Server.*;
import dods.dap.*;

/**
 * Holds a DODS Server <code>Boolean</code> value.
 *
 * @version $Revision: 1.3.2.1 $
 * @author ndp
 * @see BaseType
 */
public class sqlBool extends SDBoolean {

    private static final boolean _Debug = false;


    /** Constructs a new <code>sqlBool</code>. */
    public sqlBool() {
        super();
    }

    /**
     * Constructs a new <code>sqlBool</code> with name <code>n</code>.
     * @param n the name of the variable.
     */
    public sqlBool(String n) {
        super(n);
    }


    /** Read a value from the named dataset for this variable.
     This implementation is intended to be reading data from
     a JDBC connection to a relational database. It is important to note
     that their are several layers of type translation happening in this:

     <p><strong>Database -> JDBC -> Java -> DODS</strong></p>

     The Database types are the native types for the particular database
     that is being read from. The translation from Database->JDBC is handled
     before we get to the data (most likely by the JDBC Drivers). Our mapping
     of JDBC type to DODS types (the intermediate Java types happen in
     the process) looks like this:

     <p><strong>Mapping from JDBC Types to DODS Types:</p></strong>
     <pre>

     TINYINT        DByte
     SMALLINT       DInt16
     INTEGER        DInt32
     BIGINT         DInt32   **NO SENSIBLE MAPPING (Need DInt64)
     REAL           DFloat32
     FLOAT          DFloat64
     DOUBLE         DFloat64
     DECIMAL        DFloat64 **NO SENSIBLE MAPPING (Need Some Kind Monsterous Floating point value)
     NUMERIC        DFloat64 **NO SENSIBLE MAPPING (ibid)
     BIT            DBoolean
     CHAR           DString
     VARCHAR        DString
     LONGVARCHAR    Implemented to be read into a DSTring, although it is a "BLOB" type
     and might be better represented as a DArray(of bytes).
     BINARY         DArray(of bytes)
     VARBINARY      DArray(of bytes)
     LONGVARBINARY  DArray(of bytes)
     DATE           DString
     TIME           DString
     TIMESTAMP      DString

     </pre>

     And are handled in this (the read()) method for each of the correspoonding
     DODS data types.
     <p>
     These read() methods must be sure to read from the current column
     in the sqlResponse object, and then bump the column counter when
     finished in order for the sqlResponse object to be ready for the
     next invocation of a read() method.

     @param datasetName String identifying the file or other data store
     from which to read a vaue for this variable.
     @param specialO This <code>Object</code> is used by this method. It is
     assumed to be of type sqlResponse, a container for the ResultSet and
     the index value of next column to evaluate.
     @return <code>true</code> if more data remains to be read, otherwise
     <code>false</code>. This is an abtsract method that must be implemented
     as part of the installation/localization of a DODS server.
     @exception IOException
     @exception EOFException
     */
    public boolean read(String datasetName, Object specialO)
            throws NoSuchVariableException, IOException, EOFException {

        // The Assumption is that the currentColumn in the sqlResponse
        // Is the one to read
        sqlResponse res = (sqlResponse) specialO;
        ResultSet result = res.getResultSet();
        boolean b;

        try {

            int column = res.getCurrentColumn();

            ResultSetMetaData rsmd = result.getMetaData();

            if (_Debug) {
                System.out.println("Reading " + getName() + " from column (" + column + ")");
                System.out.println("Column Name: " + rsmd.getColumnName(column));
                System.out.println("Column Type: " + rsmd.getColumnType(column));
                System.out.println("Column Type Name(rsmd): " + rsmd.getColumnTypeName(column));
            }

            switch (rsmd.getColumnType(column)) {

                case Types.BIT:

                    b = result.getBoolean(column);
                    break;

                default:
                    throw new NoSuchVariableException(
                            "Type Mismatch! Cannot represent the JDBC Type: " +
                            rsmd.getColumnType(column) +
                            " (" +
                            rsmd.getColumnTypeName(column) +
                            ") as the DODS type: DBoolean");

            }

            if (result.wasNull()) { // Was this column a null value?
                if (_Debug) System.out.println("NULL value in resultSet!");
            }

        } catch (SQLException sqle) {

            throw new IOException(sqle.toString());

        }


        if (_Debug) System.out.println("Boolean received from SQl Query: \"" + b + "\"");

        setValue(b);
        setRead(true);

        // We've read this column. Bump the currentColumn to the next one for
        // the next read algor.

        res.nextColumn();


        return (false);
    }


    /**
     * Server-side serialization for DODS variables (sub-classes of
     * <code>BaseType</code>).
     * We override the serialize() method of the parent SD<i>Class</i> in
     * order to stop the evaluation of the CEEvaluator's Clauses, as
     * this has been handled implicitly by the SQL Database that this
     * Server is designed to interrogate.
     *
     * @param sink a <code>DataOutputStream</code> to write to.
     * @exception IOException thrown on any <code>OutputStream</code> exception.
     * @see BaseType
     * @see DDS
     * @see ServerDDS */
    public void serialize(String dataset, DataOutputStream sink, CEEvaluator ce, Object specialO)
            throws NoSuchVariableException, SDODSException, IOException {

        if (!isRead())
            read(dataset, specialO);

        externalize(sink);

    }


}
