package gov.noaa.pfel.coastwatch;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
public class HelloWorld extends HttpServlet {
    public void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {

        res.setContentType("text/html");
        res.setEncoding("UTF-8");
        OutputStreamWriter out = new OutputStreamWriter(res.getOutputStream(), "UTF-8");
        out.write(
            "<html>\n" +
            "<head><title>Hello World</title></head>\n" +
            "<body>\n" +
            "<big>Hello World</big>\n" +
            "</body>\n" +
            "</html>\n");
    }
}
