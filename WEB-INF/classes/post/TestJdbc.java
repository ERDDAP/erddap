package post;

import java.sql.*;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;

public class TestJdbc {
    public static void test() throws Exception {
        System.out.println("TestJdbc!");

        //this code derived from postgresql jdbc documentation

        //load the sql driver  (the actual driver .jar must be in the classpath)
        Class.forName("org.postgresql.Driver");

        //set up connection and query
        String url = "jdbc:postgresql://otter.pfeg.noaa.gov/posttest";  //database name
        String user = "postadmin";
        String password = String2.getPasswordFromSystemIn("Password for '" + user + "'? ");
        Connection con = DriverManager.getConnection(url, user, password);
        String query = "SELECT * FROM names";

        //method 1
        //Statement st = con.createStatement();
        //ResultSet rs = st.executeQuery(query);
        //while (rs.next()) {
        //    System.out.println(rs.getInt(1) + ", " + rs.getString(2) + ", " + rs.getString(3));
        //}
        //rs.close();
        //st.close();

        //method 2
        Table.verbose = true;
        Table.reallyVerbose = true;
        Table table = new Table();
        table.readSql(con, query);
        System.out.println(table);

        con.close();
    }
} 