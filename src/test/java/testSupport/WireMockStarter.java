package testSupport;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WireMock starter for ERDDAP tests using NetCDF-Java CDM / DAP2 datasets and SPARQL calls.
 * Flexible URL matching handles NetCDF-Java [start:1:stop] strides as well as URL-encoded query
 * parameter variations in SPARQL and DAP requests.
 */
public class WireMockStarter {
  private static WireMockServer server;

  public record Pair<L, R>(L left, R right) {}

  public static int port() {
    return 8089;
  }

  public static synchronized void start() {
    if (server != null && server.isRunning()) return;
    server = new WireMockServer(port());
    server.start();
    WireMock.configureFor("localhost", port());

    // Set system property so tests will use the mock base URL when they build URLs
    System.setProperty("test.apdrc.hawaiiUrl", "http://localhost:" + port());
    System.setProperty("test.coaps.fsuUrl", "http://localhost:" + port());
    System.setProperty("test.coastwatch.pfegUrl", "http://localhost:" + port());

    // Stub basic SODA responses from resources/mock/apdrc/
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?temp%5B1571:1571%5D%5B0:18%5D%5B197:197%5D%5B370:370%5D",
        "/mock/apdrc/soda_pop2.2.4_temp_subset.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?temp%5B1654:1654%5D%5B0:0%5D%5B0:278%5D%5B560:719%5D",
        "/mock/apdrc/soda_pop2.2.4_temp_subset2.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?temp%5B1654:1654%5D%5B0:0%5D%5B0:278%5D%5B0:160%5D",
        "/mock/apdrc/soda_pop2.2.4_temp_subset3.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?temp%5B1654:1654%5D%5B39:39%5D%5B0:278%5D%5B560:719%5D",
        "/mock/apdrc/soda_pop2.2.4_temp_subset4.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?temp%5B1654:1654%5D%5B39:39%5D%5B0:278%5D%5B0:160%5D",
        "/mock/apdrc/soda_pop2.2.4_temp_subset5.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?temp%5B1654:1654%5D%5B0:0%5D%5B1:100:301%5D%5B540:100:719%5D",
        "/mock/apdrc/soda_pop2.2.4_temp_subset6.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?temp%5B1654:1654%5D%5B0:0%5D%5B1:100:301%5D%5B20:100:120%5D",
        "/mock/apdrc/soda_pop2.2.4_temp_subset7.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?temp%5B1654:1654%5D%5B0:0%5D%5B1:301%5D%5B540:719%5D",
        "/mock/apdrc/soda_pop2.2.4_temp_subset8.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?temp%5B1654:1654%5D%5B0:0%5D%5B1:301%5D%5B0:127%5D",
        "/mock/apdrc/soda_pop2.2.4_temp_subset9.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?temp%5B1654:1654%5D%5B39:39%5D%5B1:301%5D%5B540:719%5D",
        "/mock/apdrc/soda_pop2.2.4_temp_subset10.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?temp%5B1654:1654%5D%5B39:39%5D%5B1:301%5D%5B0:127%5D",
        "/mock/apdrc/soda_pop2.2.4_temp_subset11.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?temp%5B1654:1654%5D%5B0:0%5D%5B0:311%5D%5B540:719%5D",
        "/mock/apdrc/soda_pop2.2.4_temp_subset12.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?temp%5B1654:1654%5D%5B0:0%5D%5B0:311%5D%5B0:127%5D",
        "/mock/apdrc/soda_pop2.2.4_temp_subset13.dods");

    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?salt%5B1571:1571%5D%5B0:18%5D%5B197:197%5D%5B370:370%5D",
        "/mock/apdrc/soda_pop2.2.4_salt_subset.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.asc?salt%5B177%5D%5B0%5D%5B8%3A2%3A10%5D%5B350%5D",
        "/mock/apdrc/soda_pop2.2.4.asc_salt_subset.txt");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?salt%5B177:177%5D%5B0:0%5D%5B8:2:10%5D%5B350:350%5D",
        "/mock/apdrc/soda_pop2.2.4_salt_subset2.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?salt%5B177%5D%5B0%5D%5B8%3A2%3A10%5D%5B350%5D",
        "/mock/apdrc/soda_pop2.2.4_salt_subset3.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?salt%5B170:190%5D%5B0:0%5D%5B300:300%5D%5B600:600%5D",
        "/mock/apdrc/soda_pop2.2.4_salt_subset4.dods");

    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?u%5B1571:1571%5D%5B0:18%5D%5B197:197%5D%5B370:370%5D",
        "/mock/apdrc/soda_pop2.2.4_u_subset.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?v%5B1571:1571%5D%5B0:18%5D%5B197:197%5D%5B370:370%5D",
        "/mock/apdrc/soda_pop2.2.4_v_subset.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?w%5B1571:1571%5D%5B0:18%5D%5B197:197%5D%5B370:370%5D",
        "/mock/apdrc/soda_pop2.2.4_w_subset.dods");

    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.asc?lat%5B10%3A2%3A20%5D",
        "/mock/apdrc/soda_pop2.2.4.asc_lat_subset.txt");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?lat%5B10%3A2%3A20%5D",
        "/mock/apdrc/soda_pop2.2.4_lat_subset.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?lon", "/mock/apdrc/soda_pop2.2.4_lon.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?lev", "/mock/apdrc/soda_pop2.2.4_lev.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?time", "/mock/apdrc/soda_pop2.2.4_time.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?lat", "/mock/apdrc/soda_pop2.2.4_lat.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.asc?lat", "/mock/apdrc/soda_pop2.2.4.asc_lat.txt");
    stubFromResourceDap(
        "/dods/public_data/SODA/soda_pop2.2.4.das", "/mock/apdrc/soda_pop2.2.4.das");
    stubFromResourceDap(
        "/dods/public_data/SODA/soda_pop2.2.4.dds", "/mock/apdrc/soda_pop2.2.4.dds");

    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?time,lev,lat,lon",
        "/mock/apdrc/soda_pop2.2.4_all_axes.dods");

    stubFromResource("/dods/public_data/SODA/soda_pop2.2.4.html", "/mock/apdrc/soda_pop2.2.4.html");

    stubFromResource(
        "/sparql/sparql?query=prefix+skos%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23%3E+prefix+rdf%3A%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23%3E+prefix+owl%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23%3E+prefix+dc%3A%3Chttp%3A%2F%2Fpurl.org%2Fdc%2Fterms%2F%3E+%0D%0A+%0D%0Aselect+distinct+%28%3Fdci+as+%3FIdentifier%29+%28%3Fpl+as+%3FPrefLabel%29+%28%3Fdefx+as+%3FDefinition%29+%28%3Fver+as+%3FVersion%29+%28%3Fsr+as+%3Frelated%29+%3FDate+%28%3Fdt+as+%3FUrl%29+where+%7B%3Chttp%3A%2F%2Fvocab.nerc.ac.uk%2Fcollection%2FP01%2Fcurrent%2F%3E++skos%3Amember+%3Fdt+.%0D%0AFILTER%28regex%28str%28%3Fdt%29%2C%22PSLTZZ01%22%29%29%0D%0A%3Fdt+dc%3Aidentifier+%3Fdci+.+optional%7B%3Fdt+skos%3Adefinition+%3Fdef+.FILTER%28langMatches%28lang%28%3Fdef%29%2C+%22en%22%29%29%7D+.+%3Fdt+skos%3AprefLabel+%3Fpl+.+FILTER%28langMatches%28lang%28%3Fpl%29%2C+%22en%22%29%29++%3Fdt+owl%3AversionInfo+%3Fver+%3B+dc%3Adate+%3FDate+%3B+owl%3Adeprecated+%3Fdeprecated+.+optional+%7B%3Fdt+skos%3AaltLabel+%3Falt+%7D.%0D%0A%3Fdt+skos%3Abroader+%3Fsr+.%0D%0AFILTER+%28regex%28str%28%3Fsr%29%2C+%22P02%22%2C%22i%22%29%29%0D%0A+%0D%0AFILTER%28%28str%28%3Fdeprecated%29%3D%22false%22%29%29%0D%0A+%0D%0ABIND%28if%28EXISTS%7B%3Fdt+skos%3Adefinition+%3Fdef%7D%2C%3Fdef%2C%22%22%29+as+%3Fdefx%29+%7D+order+by+%3Fpl+&output=csv&stylesheet=",
        "/mock/nerc/sparql_PSLTZZ01.csv");
    stubFromResource(
        "/sparql/sparql?query=prefix+skos%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23%3E+prefix+rdf%3A%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23%3E+prefix+owl%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23%3E+prefix+dc%3A%3Chttp%3A%2F%2Fpurl.org%2Fdc%2Fterms%2F%3E+%0D%0A+%0D%0Aselect+distinct+%28%3Fdci+as+%3FIdentifier%29+%28%3Fpl+as+%3FPrefLabel%29+%28%3Fdefx+as+%3FDefinition%29+%28%3Fver+as+%3FVersion%29+%28%3Fsr+as+%3Frelated%29+%3FDate+%28%3Fdt+as+%3FUrl%29+where+%7B%3Chttp%3A%2F%2Fvocab.nerc.ac.uk%2Fcollection%2FP01%2Fcurrent%2F%3E++skos%3Amember+%3Fdt+.%0D%0AFILTER%28regex%28str%28%3Fdt%29%2C%22Bob%22%29%29%0D%0A%3Fdt+dc%3Aidentifier+%3Fdci+.+optional%7B%3Fdt+skos%3Adefinition+%3Fdef+.FILTER%28langMatches%28lang%28%3Fdef%29%2C+%22en%22%29%29%7D+.+%3Fdt+skos%3AprefLabel+%3Fpl+.+FILTER%28langMatches%28lang%28%3Fpl%29%2C+%22en%22%29%29++%3Fdt+owl%3AversionInfo+%3Fver+%3B+dc%3Adate+%3FDate+%3B+owl%3Adeprecated+%3Fdeprecated+.+optional+%7B%3Fdt+skos%3AaltLabel+%3Falt+%7D.%0D%0A%3Fdt+skos%3Abroader+%3Fsr+.%0D%0AFILTER+%28regex%28str%28%3Fsr%29%2C+%22P02%22%2C%22i%22%29%29%0D%0A+%0D%0AFILTER%28%28str%28%3Fdeprecated%29%3D%22false%22%29%29%0D%0A+%0D%0ABIND%28if%28EXISTS%7B%3Fdt+skos%3Adefinition+%3Fdef%7D%2C%3Fdef%2C%22%22%29+as+%3Fdefx%29+%7D+order+by+%3Fpl+&output=csv&stylesheet=",
        "/mock/nerc/sparql_no_results.csv");

    stubFromResource(
        "/sparql/sparql?query=prefix+skos%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23%3E+prefix+rdf%3A%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23%3E+prefix+owl%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23%3E+prefix+dc%3A%3Chttp%3A%2F%2Fpurl.org%2Fdc%2Fterms%2F%3E+%0D%0A+%0D%0Aselect+distinct+%28%3Fdci+as+%3FIdentifier%29+%28%3Fpl+as+%3FPrefLabel%29+%28%3Fdefx+as+%3FDefinition%29+%28%3Fver+as+%3FVersion%29+%28%3Fsr+as+%3Frelated%29+%3FDate+%28%3Fdt+as+%3FUrl%29+where+%7B%3Chttp%3A%2F%2Fvocab.nerc.ac.uk%2Fcollection%2FP01%2Fcurrent%2F%3E++skos%3Amember+%3Fdt+.%0D%0AFILTER%28regex%28str%28%3Fdt%29%2C%22MBANZZZZ%22%29%29%0D%0A%3Fdt+dc%3Aidentifier+%3Fdci+.+optional%7B%3Fdt+skos%3Adefinition+%3Fdef+.FILTER%28langMatches%28lang%28%3Fdef%29%2C+%22en%22%29%29%7D+.+%3Fdt+skos%3AprefLabel+%3Fpl+.+FILTER%28langMatches%28lang%28%3Fpl%29%2C+%22en%22%29%29++%3Fdt+owl%3AversionInfo+%3Fver+%3B+dc%3Adate+%3FDate+%3B+owl%3Adeprecated+%3Fdeprecated+.+optional+%7B%3Fdt+skos%3AaltLabel+%3Falt+%7D.%0D%0A%3Fdt+skos%3Abroader+%3Fsr+.%0D%0AFILTER+%28regex%28str%28%3Fsr%29%2C+%22P02%22%2C%22i%22%29%29%0D%0A+%0D%0AFILTER%28%28str%28%3Fdeprecated%29%3D%22false%22%29%29%0D%0A+%0D%0ABIND%28if%28EXISTS%7B%3Fdt+skos%3Adefinition+%3Fdef%7D%2C%3Fdef%2C%22%22%29+as+%3Fdefx%29+%7D+order+by+%3Fpl+&output=csv&stylesheet=",
        "/mock/nerc/sparql_MBANZZZZ.csv");
    stubFromResource(
        "/sparql/sparql?query=prefix+skos%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23%3E+prefix+rdf%3A%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23%3E+prefix+owl%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23%3E+prefix+dc%3A%3Chttp%3A%2F%2Fpurl.org%2Fdc%2Fterms%2F%3E+%0D%0A+%0D%0Aselect+distinct+%28%3Fdci+as+%3FIdentifier%29+%28%3Fpl+as+%3FPrefLabel%29+%28%3Fdefx+as+%3FDefinition%29+%28%3Fver+as+%3FVersion%29+%28%3Fsr+as+%3Frelated%29+%3FDate+%28%3Fdt+as+%3FUrl%29+where+%7B%3Chttp%3A%2F%2Fvocab.nerc.ac.uk%2Fcollection%2FP01%2Fcurrent%2F%3E++skos%3Amember+%3Fdt+.%0D%0AFILTER%28regex%28str%28%3Fdt%29%2C%22ALONZZ01%22%29%29%0D%0A%3Fdt+dc%3Aidentifier+%3Fdci+.+optional%7B%3Fdt+skos%3Adefinition+%3Fdef+.FILTER%28langMatches%28lang%28%3Fdef%29%2C+%22en%22%29%29%7D+.+%3Fdt+skos%3AprefLabel+%3Fpl+.+FILTER%28langMatches%28lang%28%3Fpl%29%2C+%22en%22%29%29++%3Fdt+owl%3AversionInfo+%3Fver+%3B+dc%3Adate+%3FDate+%3B+owl%3Adeprecated+%3Fdeprecated+.+optional+%7B%3Fdt+skos%3AaltLabel+%3Falt+%7D.%0D%0A%3Fdt+skos%3Abroader+%3Fsr+.%0D%0AFILTER+%28regex%28str%28%3Fsr%29%2C+%22P02%22%2C%22i%22%29%29%0D%0A+%0D%0AFILTER%28%28str%28%3Fdeprecated%29%3D%22false%22%29%29%0D%0A+%0D%0ABIND%28if%28EXISTS%7B%3Fdt+skos%3Adefinition+%3Fdef%7D%2C%3Fdef%2C%22%22%29+as+%3Fdefx%29+%7D+order+by+%3Fpl+&output=csv&stylesheet=",
        "/mock/nerc/sparql_ALONZZ01.csv");
    stubFromResource(
        "/sparql/sparql?query=prefix+skos%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23%3E+prefix+rdf%3A%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23%3E+prefix+owl%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23%3E+prefix+dc%3A%3Chttp%3A%2F%2Fpurl.org%2Fdc%2Fterms%2F%3E+%0D%0A+%0D%0Aselect+distinct+%28%3Fdci+as+%3FIdentifier%29+%28%3Fpl+as+%3FPrefLabel%29+%28%3Fdefx+as+%3FDefinition%29+%28%3Fver+as+%3FVersion%29+%28%3Fsr+as+%3Frelated%29+%3FDate+%28%3Fdt+as+%3FUrl%29+where+%7B%3Chttp%3A%2F%2Fvocab.nerc.ac.uk%2Fcollection%2FP01%2Fcurrent%2F%3E++skos%3Amember+%3Fdt+.%0D%0AFILTER%28regex%28str%28%3Fdt%29%2C%22ALATZZ01%22%29%29%0D%0A%3Fdt+dc%3Aidentifier+%3Fdci+.+optional%7B%3Fdt+skos%3Adefinition+%3Fdef+.FILTER%28langMatches%28lang%28%3Fdef%29%2C+%22en%22%29%29%7D+.+%3Fdt+skos%3AprefLabel+%3Fpl+.+FILTER%28langMatches%28lang%28%3Fpl%29%2C+%22en%22%29%29++%3Fdt+owl%3AversionInfo+%3Fver+%3B+dc%3Adate+%3FDate+%3B+owl%3Adeprecated+%3Fdeprecated+.+optional+%7B%3Fdt+skos%3AaltLabel+%3Falt+%7D.%0D%0A%3Fdt+skos%3Abroader+%3Fsr+.%0D%0AFILTER+%28regex%28str%28%3Fsr%29%2C+%22P02%22%2C%22i%22%29%29%0D%0A+%0D%0AFILTER%28%28str%28%3Fdeprecated%29%3D%22false%22%29%29%0D%0A+%0D%0ABIND%28if%28EXISTS%7B%3Fdt+skos%3Adefinition+%3Fdef%7D%2C%3Fdef%2C%22%22%29+as+%3Fdefx%29+%7D+order+by+%3Fpl+&output=csv&stylesheet=",
        "/mock/nerc/sparql_ALATZZ01.csv");
    stubFromResource(
        "/sparql/sparql?query=prefix+skos%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23%3E+prefix+rdf%3A%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23%3E+prefix+owl%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23%3E+prefix+dc%3A%3Chttp%3A%2F%2Fpurl.org%2Fdc%2Fterms%2F%3E+%0D%0A+%0D%0Aselect+distinct+%28%3Fdci+as+%3FIdentifier%29+%28%3Fpl+as+%3FPrefLabel%29+%28%3Fdefx+as+%3FDefinition%29+%28%3Fver+as+%3FVersion%29+%28%3Fsr+as+%3Frelated%29+%3FDate+%28%3Fdt+as+%3FUrl%29+where+%7B%3Chttp%3A%2F%2Fvocab.nerc.ac.uk%2Fcollection%2FP01%2Fcurrent%2F%3E++skos%3Amember+%3Fdt+.%0D%0AFILTER%28regex%28str%28%3Fdt%29%2C%22CJDY1101%22%29%29%0D%0A%3Fdt+dc%3Aidentifier+%3Fdci+.+optional%7B%3Fdt+skos%3Adefinition+%3Fdef+.FILTER%28langMatches%28lang%28%3Fdef%29%2C+%22en%22%29%29%7D+.+%3Fdt+skos%3AprefLabel+%3Fpl+.+FILTER%28langMatches%28lang%28%3Fpl%29%2C+%22en%22%29%29++%3Fdt+owl%3AversionInfo+%3Fver+%3B+dc%3Adate+%3FDate+%3B+owl%3Adeprecated+%3Fdeprecated+.+optional+%7B%3Fdt+skos%3AaltLabel+%3Falt+%7D.%0D%0A%3Fdt+skos%3Abroader+%3Fsr+.%0D%0AFILTER+%28regex%28str%28%3Fsr%29%2C+%22P02%22%2C%22i%22%29%29%0D%0A+%0D%0AFILTER%28%28str%28%3Fdeprecated%29%3D%22false%22%29%29%0D%0A+%0D%0ABIND%28if%28EXISTS%7B%3Fdt+skos%3Adefinition+%3Fdef%7D%2C%3Fdef%2C%22%22%29+as+%3Fdefx%29+%7D+order+by+%3Fpl+&output=csv&stylesheet=",
        "/mock/nerc/sparql_CJDY1101.csv");
    stubFromResource(
        "/sparql/sparql?query=prefix+skos%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23%3E+prefix+rdf%3A%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23%3E+prefix+owl%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23%3E+prefix+dc%3A%3Chttp%3A%2F%2Fpurl.org%2Fdc%2Fterms%2F%3E+%0D%0A+%0D%0Aselect+distinct+%28%3Fdci+as+%3FIdentifier%29+%28%3Fpl+as+%3FPrefLabel%29+%28%3Fdefx+as+%3FDefinition%29+%28%3Fver+as+%3FVersion%29+%28%3Fsr+as+%3Frelated%29+%3FDate+%28%3Fdt+as+%3FUrl%29+where+%7B%3Chttp%3A%2F%2Fvocab.nerc.ac.uk%2Fcollection%2FP01%2Fcurrent%2F%3E++skos%3Amember+%3Fdt+.%0D%0AFILTER%28regex%28str%28%3Fdt%29%2C%22ADEPZZ01%22%29%29%0D%0A%3Fdt+dc%3Aidentifier+%3Fdci+.+optional%7B%3Fdt+skos%3Adefinition+%3Fdef+.FILTER%28langMatches%28lang%28%3Fdef%29%2C+%22en%22%29%29%7D+.+%3Fdt+skos%3AprefLabel+%3Fpl+.+FILTER%28langMatches%28lang%28%3Fpl%29%2C+%22en%22%29%29++%3Fdt+owl%3AversionInfo+%3Fver+%3B+dc%3Adate+%3FDate+%3B+owl%3Adeprecated+%3Fdeprecated+.+optional+%7B%3Fdt+skos%3AaltLabel+%3Falt+%7D.%0D%0A%3Fdt+skos%3Abroader+%3Fsr+.%0D%0AFILTER+%28regex%28str%28%3Fsr%29%2C+%22P02%22%2C%22i%22%29%29%0D%0A+%0D%0AFILTER%28%28str%28%3Fdeprecated%29%3D%22false%22%29%29%0D%0A+%0D%0ABIND%28if%28EXISTS%7B%3Fdt+skos%3Adefinition+%3Fdef%7D%2C%3Fdef%2C%22%22%29+as+%3Fdefx%29+%7D+order+by+%3Fpl+&output=csv&stylesheet=",
        "/mock/nerc/sparql_ADEPZZ01.csv");
    stubFromResource(
        "/sparql/sparql?query=prefix+skos%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23%3E+prefix+rdf%3A%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23%3E+prefix+owl%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23%3E+prefix+dc%3A%3Chttp%3A%2F%2Fpurl.org%2Fdc%2Fterms%2F%3E+%0D%0A+%0D%0Aselect+distinct+%28%3Fdci+as+%3FIdentifier%29+%28%3Fpl+as+%3FPrefLabel%29+%28%3Fdefx+as+%3FDefinition%29+%28%3Fver+as+%3FVersion%29+%28%3Fsr+as+%3Frelated%29+%3FDate+%28%3Fdt+as+%3FUrl%29+where+%7B%3Chttp%3A%2F%2Fvocab.nerc.ac.uk%2Fcollection%2FP01%2Fcurrent%2F%3E++skos%3Amember+%3Fdt+.%0D%0AFILTER%28regex%28str%28%3Fdt%29%2C%22ASLVZZ01%22%29%29%0D%0A%3Fdt+dc%3Aidentifier+%3Fdci+.+optional%7B%3Fdt+skos%3Adefinition+%3Fdef+.FILTER%28langMatches%28lang%28%3Fdef%29%2C+%22en%22%29%29%7D+.+%3Fdt+skos%3AprefLabel+%3Fpl+.+FILTER%28langMatches%28lang%28%3Fpl%29%2C+%22en%22%29%29++%3Fdt+owl%3AversionInfo+%3Fver+%3B+dc%3Adate+%3FDate+%3B+owl%3Adeprecated+%3Fdeprecated+.+optional+%7B%3Fdt+skos%3AaltLabel+%3Falt+%7D.%0D%0A%3Fdt+skos%3Abroader+%3Fsr+.%0D%0AFILTER+%28regex%28str%28%3Fsr%29%2C+%22P02%22%2C%22i%22%29%29%0D%0A+%0D%0AFILTER%28%28str%28%3Fdeprecated%29%3D%22false%22%29%29%0D%0A+%0D%0ABIND%28if%28EXISTS%7B%3Fdt+skos%3Adefinition+%3Fdef%7D%2C%3Fdef%2C%22%22%29+as+%3Fdefx%29+%7D+order+by+%3Fpl+&output=csv&stylesheet=",
        "/mock/nerc/sparql_ASLVZZ01.csv");
    stubFromResource(
        "/sparql/sparql?query=prefix+skos%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23%3E+prefix+rdf%3A%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23%3E+prefix+owl%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23%3E+prefix+dc%3A%3Chttp%3A%2F%2Fpurl.org%2Fdc%2Fterms%2F%3E+%0D%0A+%0D%0Aselect+distinct+%28%3Fdci+as+%3FIdentifier%29+%28%3Fpl+as+%3FPrefLabel%29+%28%3Fdefx+as+%3FDefinition%29+%28%3Fver+as+%3FVersion%29+%28%3Fsr+as+%3Frelated%29+%3FDate+%28%3Fdt+as+%3FUrl%29+where+%7B%3Chttp%3A%2F%2Fvocab.nerc.ac.uk%2Fcollection%2FP01%2Fcurrent%2F%3E++skos%3Amember+%3Fdt+.%0D%0AFILTER%28regex%28str%28%3Fdt%29%2C%22TEMPPR01%22%29%29%0D%0A%3Fdt+dc%3Aidentifier+%3Fdci+.+optional%7B%3Fdt+skos%3Adefinition+%3Fdef+.FILTER%28langMatches%28lang%28%3Fdef%29%2C+%22en%22%29%29%7D+.+%3Fdt+skos%3AprefLabel+%3Fpl+.+FILTER%28langMatches%28lang%28%3Fpl%29%2C+%22en%22%29%29++%3Fdt+owl%3AversionInfo+%3Fver+%3B+dc%3Adate+%3FDate+%3B+owl%3Adeprecated+%3Fdeprecated+.+optional+%7B%3Fdt+skos%3AaltLabel+%3Falt+%7D.%0D%0A%3Fdt+skos%3Abroader+%3Fsr+.%0D%0AFILTER+%28regex%28str%28%3Fsr%29%2C+%22P02%22%2C%22i%22%29%29%0D%0A+%0D%0AFILTER%28%28str%28%3Fdeprecated%29%3D%22false%22%29%29%0D%0A+%0D%0ABIND%28if%28EXISTS%7B%3Fdt+skos%3Adefinition+%3Fdef%7D%2C%3Fdef%2C%22%22%29+as+%3Fdefx%29+%7D+order+by+%3Fpl+&output=csv&stylesheet=",
        "/mock/nerc/sparql_TEMPPR01.csv");
    stubFromResource(
        "/sparql/sparql?query=prefix+skos%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23%3E+prefix+rdf%3A%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23%3E+prefix+owl%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23%3E+prefix+dc%3A%3Chttp%3A%2F%2Fpurl.org%2Fdc%2Fterms%2F%3E+%0D%0A+%0D%0Aselect+distinct+%28%3Fdci+as+%3FIdentifier%29+%28%3Fpl+as+%3FPrefLabel%29+%28%3Fdefx+as+%3FDefinition%29+%28%3Fver+as+%3FVersion%29+%28%3Fsr+as+%3Frelated%29+%3FDate+%28%3Fdt+as+%3FUrl%29+where+%7B%3Chttp%3A%2F%2Fvocab.nerc.ac.uk%2Fcollection%2FP01%2Fcurrent%2F%3E++skos%3Amember+%3Fdt+.%0D%0AFILTER%28regex%28str%28%3Fdt%29%2C%22PRESPR01%22%29%29%0D%0A%3Fdt+dc%3Aidentifier+%3Fdci+.+optional%7B%3Fdt+skos%3Adefinition+%3Fdef+.FILTER%28langMatches%28lang%28%3Fdef%29%2C+%22en%22%29%29%7D+.+%3Fdt+skos%3AprefLabel+%3Fpl+.+FILTER%28langMatches%28lang%28%3Fpl%29%2C+%22en%22%29%29++%3Fdt+owl%3AversionInfo+%3Fver+%3B+dc%3Adate+%3FDate+%3B+owl%3Adeprecated+%3Fdeprecated+.+optional+%7B%3Fdt+skos%3AaltLabel+%3Falt+%7D.%0D%0A%3Fdt+skos%3Abroader+%3Fsr+.%0D%0AFILTER+%28regex%28str%28%3Fsr%29%2C+%22P02%22%2C%22i%22%29%29%0D%0A+%0D%0AFILTER%28%28str%28%3Fdeprecated%29%3D%22false%22%29%29%0D%0A+%0D%0ABIND%28if%28EXISTS%7B%3Fdt+skos%3Adefinition+%3Fdef%7D%2C%3Fdef%2C%22%22%29+as+%3Fdefx%29+%7D+order+by+%3Fpl+&output=csv&stylesheet=",
        "/mock/nerc/sparql_PRESPR01.csv");

    // Stub base dataset paths
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathEqualTo("/dods/public_data/SODA/soda_pop2.2.4"))
            .willReturn(WireMock.aResponse().withStatus(200).withBody("APDRC SODA mock root")));
    WireMock.stubFor(
        WireMock.head(WireMock.urlPathEqualTo("/dods/public_data/SODA/soda_pop2.2.4"))
            .willReturn(WireMock.aResponse().withStatus(200)));

    stubFromResourceDap(
        "/thredds/dodsC/samos/data/research/WTEP/2012/WTEP_20120128v30001.nc.dds",
        "/mock/coaps/WTEP_20120128v30001.nc.dds");
    stubFromResourceDap("/erddap/tabledap/erdGlobecMoc1.dds", "/mock/coastwatch/erdGlobecMoc1.dds");
  }

  private static void stubFromResource(String requestUrl, String resourcePath) {
    try (InputStream is = WireMockStarter.class.getResourceAsStream(resourcePath)) {
      if (is == null) return;
      String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      String urlRegex = buildDapUrlRegex(requestUrl);

      registerGetAndHead(
          urlRegex,
          WireMock.aResponse().withStatus(200).withBody(body),
          requestUrl.contains("?") ? 10 : 100);
    } catch (IOException e) {
      // ignore
    }
  }

  private static void stubFromResourceDap(String requestUrl, String resourcePath) {
    try (InputStream is = WireMockStarter.class.getResourceAsStream(resourcePath)) {
      if (is == null) return;
      String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      String urlRegex = buildDapUrlRegex(requestUrl);

      String contentDesc = "dods_dds";
      if (requestUrl.endsWith(".das")) {
        contentDesc = "dods_das";
      }

      registerGetAndHead(
          urlRegex,
          WireMock.aResponse()
              .withStatus(200)
              .withBody(body)
              .withHeader("XDODS-Server", "dods/3.7")
              .withHeader("Content-Description", contentDesc),
          requestUrl.contains("?") ? 10 : 100);
    } catch (IOException e) {
      // ignore
    }
  }

  private static void stubFromResourceData(String requestUrl, String resourcePath) {
    try (InputStream is = WireMockStarter.class.getResourceAsStream(resourcePath)) {
      if (is == null) return;
      byte[] body = is.readAllBytes();
      String urlRegex = buildDapUrlRegex(requestUrl);

      registerGetAndHead(
          urlRegex,
          WireMock.aResponse()
              .withStatus(200)
              .withBody(body)
              .withHeader("XDODS-Server", "dods/3.7")
              .withHeader("Content-Description", "dods_data"),
          requestUrl.contains("?") ? 10 : 100);
    } catch (IOException e) {
      // ignore
    }
  }

  private static void registerGetAndHead(
      String urlRegex,
      com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder response,
      int priority) {
    WireMock.stubFor(
        WireMock.get(WireMock.urlMatching(urlRegex)).willReturn(response).atPriority(priority));
    WireMock.stubFor(
        WireMock.head(WireMock.urlMatching(urlRegex)).willReturn(response).atPriority(priority));
  }

  /**
   * Converts a canonical URL request path/query into a WireMock urlMatching regex pattern. Flexibly
   * matches array indexing: - OPeNDAP legacy style: [start:stop] - NetCDF-Java CDM style:
   * [start:1:stop] - Strict end bounds to prevent cross-matching different subset dimensions
   */
  private static String buildDapUrlRegex(String requestUrl) {
    int queryIndex = requestUrl.indexOf('?');
    if (queryIndex < 0) {
      return Pattern.quote(requestUrl) + "(?:\\?.*)?";
    }

    String path = requestUrl.substring(0, queryIndex);
    String rawQuery = requestUrl.substring(queryIndex + 1);

    StringBuilder regex = new StringBuilder("^");
    regex.append(Pattern.quote(path)).append("\\?");

    Pattern dapSlicePattern =
        Pattern.compile(
            "(?:\\[|%5[bB])(\\d+)(?:(?::|%3[aA])(\\d+))?(?:(?::|%3[aA])(\\d+))?(?:\\]|%5[dD])");
    Matcher matcher = dapSlicePattern.matcher(rawQuery);

    int lastEnd = 0;
    while (matcher.find()) {
      appendFlexibleQuerySnippet(regex, rawQuery.substring(lastEnd, matcher.start()));

      String g1 = matcher.group(1);
      String g2 = matcher.group(2);
      String g3 = matcher.group(3);

      String lb = "(?:\\[|%5[bB])";
      String rb = "(?:\\]|%5[dD])";
      String col = "(?::|%3[aA])";

      if (g2 == null) {
        // Single index [X] -> match [X], [X:X], [X:1:X]
        regex
            .append(lb)
            .append(g1)
            .append("(?:")
            .append(col)
            .append("(?:\\d+")
            .append(col)
            .append(")?")
            .append(g1)
            .append(")?")
            .append(rb);
      } else if (g3 == null) {
        // Two-bound index range [X:Y] -> match [X:Y], [X:1:Y], [X:stride:Y]
        regex
            .append(lb)
            .append(g1)
            .append("(?:")
            .append(col)
            .append("(?:\\d+")
            .append(col)
            .append(")?)?")
            .append(g2)
            .append(rb);
      } else {
        // Three-part index [X:S:Y] -> match start X, explicit stride S, and flexible stop bound
        // (CDM calculated bound)
        regex.append(lb).append(g1).append(col).append(g2).append(col).append("\\d+").append(rb);
      }
      lastEnd = matcher.end();
    }

    appendFlexibleQuerySnippet(regex, rawQuery.substring(lastEnd));
    regex.append("$");

    return regex.toString();
  }

  private static void appendFlexibleQuerySnippet(StringBuilder regex, String snippet) {
    if (snippet.isEmpty()) return;

    Pattern tokenPattern =
        Pattern.compile(
            "%0[dD]%0[aA]|[\r\n]+|\\+|%20|\\s|%3[aA]|:|%3[cC]|<|%3[eE]|>|%2[fF]|/|%23|#|%22|\"|%7[bB]|\\{|%7[dD]|\\}|%28|\\(|%29|\\)|%2[cC]|,|%3[bB]|;");

    Matcher m = tokenPattern.matcher(snippet);
    int last = 0;
    while (m.find()) {
      if (m.start() > last) {
        regex.append(Pattern.quote(snippet.substring(last, m.start())));
      }
      String tok = m.group();
      if (tok.equalsIgnoreCase("%0d%0a") || tok.contains("\n") || tok.contains("\r")) {
        regex.append("(?:%0[dD]%0[aA]|%0[aA]|[\\r\\n]*)");
      } else if (tok.equals("+")
          || tok.equalsIgnoreCase("%20")
          || Character.isWhitespace(tok.charAt(0))) {
        regex.append("(?:\\+|%20|\\s)");
      } else if (tok.equals(":") || tok.equalsIgnoreCase("%3a")) {
        regex.append("(?::|%3[aA])");
      } else if (tok.equals("<") || tok.equalsIgnoreCase("%3c")) {
        regex.append("(?:<|%3[cC])");
      } else if (tok.equals(">") || tok.equalsIgnoreCase("%3e")) {
        regex.append("(?:>|%3[eE])");
      } else if (tok.equals("/") || tok.equalsIgnoreCase("%2f")) {
        regex.append("(?:/|%2[fF])");
      } else if (tok.equals("#") || tok.equalsIgnoreCase("%23")) {
        regex.append("(?:#|%23)");
      } else if (tok.equals("\"") || tok.equalsIgnoreCase("%22")) {
        regex.append("(?::|%22)");
      } else if (tok.equals("{") || tok.equalsIgnoreCase("%7b")) {
        regex.append("(?:\\{|%7[bB])");
      } else if (tok.equals("}") || tok.equalsIgnoreCase("%7d")) {
        regex.append("(?:\\}|%7[dD])");
      } else if (tok.equals("(") || tok.equalsIgnoreCase("%28")) {
        regex.append("(?:\\(|%28)");
      } else if (tok.equals(")") || tok.equalsIgnoreCase("%29")) {
        regex.append("(?:\\)|%29)");
      } else if (tok.equals(",") || tok.equalsIgnoreCase("%2c")) {
        regex.append("(?:,|%2[cC])");
      } else if (tok.equals(";") || tok.equalsIgnoreCase("%3b")) {
        regex.append("(?:;|%3[bB])");
      } else {
        regex.append(Pattern.quote(tok));
      }
      last = m.end();
    }
    if (last < snippet.length()) {
      regex.append(Pattern.quote(snippet.substring(last)));
    }
  }

  public static synchronized void stop() {
    if (server != null) server.stop();
    server = null;
  }
}
