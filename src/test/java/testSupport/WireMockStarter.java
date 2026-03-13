package testSupport;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Simple WireMock starter for tests. Starts on port 8089 and stubs a few endpoints. */
public class WireMockStarter {
  private static WireMockServer server;

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

    // Stub basic SODA responses from resources/mock/apdrc/
    stubFromResourceDap(
        "/dods/public_data/SODA/soda_pop2.2.4.das", "/mock/apdrc/soda_pop2.2.4.das");
    stubFromResourceDap(
        "/dods/public_data/SODA/soda_pop2.2.4.dds", "/mock/apdrc/soda_pop2.2.4.dds");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?time", "/mock/apdrc/soda_pop2.2.4_time.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.dods?lat", "/mock/apdrc/soda_pop2.2.4_lat.dods");
    stubFromResourceData(
        "/dods/public_data/SODA/soda_pop2.2.4.asc?lat", "/mock/apdrc/soda_pop2.2.4.asc_lat.txt");
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

    // also stub base path requests
    WireMock.stubFor(
        WireMock.get(WireMock.urlEqualTo("/dods/public_data/SODA/soda_pop2.2.4"))
            .willReturn(WireMock.aResponse().withStatus(200).withBody("APDRC SODA mock root")));
  }

  private static void stubFromResource(String urlPath, String resourcePath) {
    try (InputStream is = WireMockStarter.class.getResourceAsStream(resourcePath)) {
      if (is == null) return;
      String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      WireMock.stubFor(
          WireMock.get(WireMock.urlEqualTo(urlPath))
              .willReturn(WireMock.aResponse().withStatus(200).withBody(body)));
    } catch (IOException e) {
      // ignore
    }
  }

  private static void stubFromResourceDap(String urlPath, String resourcePath) {
    try (InputStream is = WireMockStarter.class.getResourceAsStream(resourcePath)) {
      if (is == null) return;
      String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      WireMock.stubFor(
          WireMock.get(WireMock.urlEqualTo(urlPath))
              .willReturn(
                  WireMock.aResponse()
                      .withStatus(200)
                      .withBody(body)
                      .withHeader("XDODS-Server", "dods/3.7")
                      .withHeader("Content-Description", "dods_das")));
    } catch (IOException e) {
      // ignore
    }
  }

  private static void stubFromResourceData(String urlPath, String resourcePath) {
    try (InputStream is = WireMockStarter.class.getResourceAsStream(resourcePath)) {
      if (is == null) return;
      byte[] body = is.readAllBytes();
      WireMock.stubFor(
          WireMock.get(WireMock.urlEqualTo(urlPath))
              .willReturn(
                  WireMock.aResponse()
                      .withStatus(200)
                      .withBody(body)
                      .withHeader("XDODS-Server", "dods/3.7")
                      .withHeader("Content-Description", "dods_data")));
    } catch (IOException e) {
      // ignore
    }
  }

  public static synchronized void stop() {
    if (server != null) server.stop();
    server = null;
  }
}
