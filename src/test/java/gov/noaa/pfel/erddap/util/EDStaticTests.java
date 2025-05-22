package gov.noaa.pfel.erddap.util;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.cohort.array.Attributes;
import com.cohort.util.String2;
import com.cohort.util.Test;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.mockito.Mockito;
import testDataset.Initialization;

public class EDStaticTests {
  @org.junit.jupiter.api.BeforeAll
  static void beforeAll() {
    Initialization.edStatic();
  }

  @org.junit.jupiter.api.Test
  void testUpdateUrls() throws Exception {
    String2.log("\n***** EDStatic.testUpdateUrls");
    Attributes source = new Attributes();
    Attributes add = new Attributes();
    source.set("a", "http://coastwatch.pfel.noaa.gov"); // purposely out-of-date
    source.set("nine", 9.0);
    add.set("b", "http://www.whoi.edu"); // purposely out-of-date
    add.set("ten", 10.0);
    add.set("sourceUrl", "http://coastwatch.pfel.noaa.gov"); // purposely out-of-date
    EDStatic.updateUrls(source, add);
    String results = add.toString();
    String expected =
        "    a=https://coastwatch.pfeg.noaa.gov\n"
            + "    b=https://www.whoi.edu\n"
            + "    sourceUrl=http://coastwatch.pfel.noaa.gov\n"
            + // unchanged
            "    ten=10.0d\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    source = new Attributes();
    add = new Attributes();
    add.set("a", "http://coastwatch.pfel.noaa.gov");
    add.set("b", "http://www.whoi.edu");
    add.set("nine", 9.0);
    add.set("sourceUrl", "http://coastwatch.pfel.noaa.gov");
    EDStatic.updateUrls(null, add);
    results = add.toString();
    expected =
        "    a=https://coastwatch.pfeg.noaa.gov\n"
            + "    b=https://www.whoi.edu\n"
            + "    nine=9.0d\n"
            + "    sourceUrl=http://coastwatch.pfel.noaa.gov\n"; // unchanged
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /**
   * Test EDStatic methods returing ERDDAP URL prefixes, which respect the request's scheme and host
   * header if useHeadersForUrl feature flag is true.
   *
   * <p>General expectations: If useHeadersForUrl is false, erddapUrl or erddapHttpsUrl are used
   * (legacy behavior). If the request is null, erddapUrl or erddapHttpsUrl are used (legacy
   * behavior). If useHeadersForUrl is true and request is not null, scheme and host header from
   * request are used. If useHeadersForUrl true, request not null, and request scheme is http,
   * EDStatic.erddapHttpsUrl() uses https scheme with request host as long as request host does not
   * contain a port (i.e. http url are upgraded to https with the same host). If host does contain a
   * port, erddapHttpsUrl is returned instead. * @throws Exception
   */
  @org.junit.jupiter.api.Test
  void testErddapUrls() throws Exception {
    String2.log("\n***** EDStatic.testErddapUrls");

    // FIXME changing global config state makes tests brittle
    boolean cachedUseHeadersForUrlConfig = EDStatic.config.useHeadersForUrl;

    checkUrlExpectation(
        "EDStatic.getErddapUrlPrefix with null request and null loggedInAs",
        EDStatic.erddapUrl,
        EDStatic.getErddapUrlPrefix(null, null));

    checkUrlExpectation(
        "EDStatic.getErddapUrlPrefix with null request and loggedInAs",
        EDStatic.erddapHttpsUrl,
        EDStatic.getErddapUrlPrefix(null, "fakeLoggedInAs"));

    String requestHost = "erddap.requesthost.org";
    String requestHostHttpUrl = "http://" + requestHost + "/erddap";
    String requestHostHttpsUrl = "https://" + requestHost + "/erddap";

    HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpRequest.getScheme()).thenReturn("http");
    Mockito.when(httpRequest.getHeader("Host")).thenReturn(requestHost);

    HttpServletRequest httpRequestWithPort = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpRequestWithPort.getScheme()).thenReturn("http");
    Mockito.when(httpRequestWithPort.getHeader("Host")).thenReturn(requestHost + ":8080");

    HttpServletRequest httpsRequest = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpsRequest.getScheme()).thenReturn("https");
    Mockito.when(httpsRequest.getHeader("Host")).thenReturn(requestHost);

    EDStatic.config.useHeadersForUrl = false;

    checkUrlExpectation(
        "EDStatic.getErddapUrlPrefix with http request, null loggedInAs, and useHeadersForUrl = false",
        EDStatic.erddapUrl,
        EDStatic.getErddapUrlPrefix(httpRequest, null));

    checkUrlExpectation(
        "EDStatic.getErddapUrlPrefix with http request, loggedInAs, and useHeadersForUrl = false",
        EDStatic.erddapHttpsUrl,
        EDStatic.getErddapUrlPrefix(httpRequest, "fakeLoggedInAs"));

    checkUrlExpectation(
        "EDStatic.erddapUrl with null request, null loggedInAs, language 0, and useHeadersForUrl = false",
        EDStatic.erddapUrl,
        EDStatic.erddapUrl(null, null, 0));

    checkUrlExpectation(
        "EDStatic.erddapUrl with null request, null loggedInAs, language 1, and useHeadersForUrl = false",
        EDStatic.erddapUrl + "/" + TranslateMessages.languageCodeList.get(1),
        EDStatic.erddapUrl(null, null, 1));

    checkUrlExpectation(
        "EDStatic.erddapUrl with http request, null loggedInAs, language 0, and useHeadersForUrl = false",
        EDStatic.erddapUrl,
        EDStatic.erddapUrl(httpRequest, null, 0));

    checkUrlExpectation(
        "EDStatic.erddapUrl with https request, null loggedInAs, language 1, and useHeadersForUrl = false",
        EDStatic.erddapUrl + "/" + TranslateMessages.languageCodeList.get(1),
        EDStatic.erddapUrl(httpsRequest, null, 1));

    checkUrlExpectation(
        "EDStatic.erddapUrl with null request, loggedInAs, language 0, and useHeadersForUrl = false",
        EDStatic.erddapHttpsUrl,
        EDStatic.erddapUrl(null, "fakeLoggedInAs", 0));

    checkUrlExpectation(
        "EDStatic.erddapUrl with http request, loggedInAs, language 0, and useHeadersForUrl = false",
        EDStatic.erddapHttpsUrl,
        EDStatic.erddapUrl(httpRequest, "fakeLoggedInAs", 0));

    checkUrlExpectation(
        "EDStatic.erddapHttpsUrl with null request, language 0, and useHeadersForUrl = false",
        EDStatic.erddapHttpsUrl,
        EDStatic.erddapHttpsUrl(null, 0));

    checkUrlExpectation(
        "EDStatic.erddapHttpsUrl with http request, language 0, and useHeadersForUrl = false",
        EDStatic.erddapHttpsUrl,
        EDStatic.erddapHttpsUrl(httpRequest, 0));

    checkUrlExpectation(
        "EDStatic.erddapHttpsUrl with null request, language 0, and useHeadersForUrl = false",
        EDStatic.erddapHttpsUrl,
        EDStatic.erddapHttpsUrl(null, 0));

    EDStatic.config.useHeadersForUrl = true;

    checkUrlExpectation(
        "EDStatic.getErddapUrlPrefix with http request, null loggedInAs, and useHeadersForUrl = true",
        requestHostHttpUrl,
        EDStatic.getErddapUrlPrefix(httpRequest, null));

    checkUrlExpectation(
        "EDStatic.getErddapUrlPrefix with http request, loggedInAs, and useHeadersForUrl = true",
        requestHostHttpUrl,
        EDStatic.getErddapUrlPrefix(httpRequest, "fakeLoggedInAs"));

    checkUrlExpectation(
        "EDStatic.getErddapUrlPrefix with https request, null loggedInAs, and useHeadersForUrl = true",
        requestHostHttpsUrl,
        EDStatic.getErddapUrlPrefix(httpsRequest, null));

    checkUrlExpectation(
        "EDStatic.getErddapUrlPrefix with https request, loggedInAs, and useHeadersForUrl = true",
        requestHostHttpsUrl,
        EDStatic.getErddapUrlPrefix(httpsRequest, "fakeLoggedInAs"));

    checkUrlExpectation(
        "EDStatic.erddapUrl with null request, null loggedInAs, language 0, and useHeadersForUrl = true",
        EDStatic.erddapUrl,
        EDStatic.erddapUrl(null, null, 0));

    checkUrlExpectation(
        "EDStatic.erddapUrl with null request, loggedInAs, language 0, and useHeadersForUrl = true",
        EDStatic.erddapHttpsUrl,
        EDStatic.erddapUrl(null, "fakeLoggedInAs", 0));

    checkUrlExpectation(
        "EDStatic.erddapUrl with http request, null loggedInAs, language 0, and useHeadersForUrl = true",
        requestHostHttpUrl,
        EDStatic.erddapUrl(httpRequest, null, 0));

    checkUrlExpectation(
        "EDStatic.erddapUrl with http request, loggedInAs, language 0, and useHeadersForUrl = true",
        requestHostHttpUrl,
        EDStatic.erddapUrl(httpRequest, "fakeLoggedInAs", 0));

    checkUrlExpectation(
        "EDStatic.erddapUrl with http request, null loggedInAs, language 1, and useHeadersForUrl = true",
        requestHostHttpUrl + "/" + TranslateMessages.languageCodeList.get(1),
        EDStatic.erddapUrl(httpRequest, null, 1));

    checkUrlExpectation(
        "EDStatic.erddapHttpsUrl with null request, language 0, and useHeadersForUrl = true",
        EDStatic.erddapHttpsUrl,
        EDStatic.erddapHttpsUrl(null, 0));

    checkUrlExpectation(
        "EDStatic.erddapHttpsUrl with https request, language 0, and useHeadersForUrl = true",
        requestHostHttpsUrl,
        EDStatic.erddapHttpsUrl(httpRequest, 0));

    checkUrlExpectation(
        "EDStatic.erddapHttpsUrl with https request, language 1, and useHeadersForUrl = true",
        requestHostHttpsUrl + "/" + TranslateMessages.languageCodeList.get(1),
        EDStatic.erddapHttpsUrl(httpRequest, 1));

    checkUrlExpectation(
        "EDStatic.erddapHttpsUrl with http request, language 0, and useHeadersForUrl = true",
        requestHostHttpsUrl,
        EDStatic.erddapHttpsUrl(httpsRequest, 0));

    checkUrlExpectation(
        "EDStatic.erddapHttpsUrl with http request containing port, language 0, and useHeadersForUrl = true",
        EDStatic.erddapHttpsUrl,
        EDStatic.erddapHttpsUrl(httpRequestWithPort, 0));

    // set useHeadersForUrl back to original value
    EDStatic.config.useHeadersForUrl = cachedUseHeadersForUrlConfig;

    for (HttpServletRequest request : List.of(httpRequest, httpRequestWithPort, httpsRequest)) {
      verify(request, atLeastOnce()).getHeader("Host");
      verify(request, atLeastOnce()).getScheme();
      verifyNoMoreInteractions(request);
    }
  }

  private void checkUrlExpectation(String message, String expected, String result) {
    Test.ensureEqual(result, expected, message + ": expected=" + expected + " got=" + result);
  }
}
