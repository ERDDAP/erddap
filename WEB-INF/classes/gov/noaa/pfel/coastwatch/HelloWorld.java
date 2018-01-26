package gov.noaa.pfel.coastwatch;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
public class HelloWorld extends HttpServlet {
    public void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {

        res.setContentType("text/html");
        res.setEncoding(String2.UTF_8);
        OutputStreamWriter out = new OutputStreamWriter(res.getOutputStream(), String2.UTF_8);
        out.write(
            "<html>\n" +
            "<head><title>Hello World</title></head>\n" +
            "<body>\n" +
            "<h1>Hello World</h1>\n" +
            "</body>\n" +
            "</html>\n");
    }
}
