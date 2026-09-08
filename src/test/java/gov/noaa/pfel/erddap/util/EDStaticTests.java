package gov.noaa.pfel.erddap.util;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

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
    boolean cachedVerify = EDStatic.config.verifyHostNameErddapUrl;
    EDStatic.config.verifyHostNameErddapUrl = false;

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
    String requestHostHttpUrlPort = "http://" + requestHost + ":8080/erddap";
    String requestHostHttpUrlPortSubpath = "http://" + requestHost + ":8080/subpath/erddap";
    String requestHostHttpsUrl = "https://" + requestHost + "/erddap";

    HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpRequest.getScheme()).thenReturn("http");
    Mockito.when(httpRequest.getHeader("Host")).thenReturn(requestHost);

    HttpServletRequest httpRequestWithPort = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpRequestWithPort.getScheme()).thenReturn("http");
    Mockito.when(httpRequestWithPort.getHeader("Host")).thenReturn(requestHost + ":8080");

    HttpServletRequest httpRequestWithPortAndSubpath = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpRequestWithPortAndSubpath.getScheme()).thenReturn("http");
    Mockito.when(httpRequestWithPortAndSubpath.getHeader("Host")).thenReturn(requestHost + ":8080");
    Mockito.when(httpRequestWithPortAndSubpath.getHeader("X-Forwarded-Prefix"))
        .thenReturn("/subpath");

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
        "EDStatic.getErddapUrlPrefix with http request, null loggedInAs, and useHeadersForUrl = true",
        requestHostHttpUrlPort,
        EDStatic.getErddapUrlPrefix(httpRequestWithPort, null));

    checkUrlExpectation(
        "EDStatic.getErddapUrlPrefix with http request, subpath, loggedInAs, and useHeadersForUrl = true",
        requestHostHttpUrlPortSubpath,
        EDStatic.getErddapUrlPrefix(httpRequestWithPortAndSubpath, "fakeLoggedInAs"));

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
    EDStatic.config.verifyHostNameErddapUrl = cachedVerify;

    for (HttpServletRequest request :
        List.of(httpRequest, httpRequestWithPort, httpRequestWithPortAndSubpath, httpsRequest)) {
      verify(request, atLeastOnce()).getHeader("Host");
      verify(request, atLeastOnce()).getHeader("X-Forwarded-Prefix");
      verify(request, atLeastOnce()).getScheme();
    }
  }

  @org.junit.jupiter.api.Test
  void testHostHeaderValidation() throws Exception {
    String2.log("\n***** EDStatic.testHostHeaderValidation");

    // Cache the original configuration
    boolean cachedVerify = EDStatic.config.verifyHostNameErddapUrl;
    java.util.Set<String> cachedAllowed = EDStatic.config.allowedHosts;

    try {
      // 1. Check legacy/bypass behavior when verifyHostNameErddapUrl is false
      EDStatic.config.verifyHostNameErddapUrl = false;
      EDStatic.config.useHeadersForUrl = true;

      HttpServletRequest reqLegacy = Mockito.mock(HttpServletRequest.class);
      Mockito.when(reqLegacy.getHeader("Host")).thenReturn("evil.com");
      Mockito.when(reqLegacy.getScheme()).thenReturn("http");

      // With verification disabled, it should return the legacy/unvalidated value
      String resultLegacy = EDStatic.baseUrl(reqLegacy, null);
      Test.ensureEqual(
          resultLegacy, "http://evil.com", "Legacy bypass should return the provided host");

      // Check legacy/bypass handles underscores safely (regression test)
      HttpServletRequest reqLegacyUnderscore = Mockito.mock(HttpServletRequest.class);
      Mockito.when(reqLegacyUnderscore.getHeader("Host")).thenReturn("local_dev:8080");
      Mockito.when(reqLegacyUnderscore.getScheme()).thenReturn("http");
      Test.ensureEqual(
          EDStatic.baseUrl(reqLegacyUnderscore, null),
          "http://local_dev:8080",
          "Legacy bypass must preserve local hostnames containing underscores");

      // 2. Enable host verification and setup allowed hosts
      EDStatic.config.verifyHostNameErddapUrl = true;
      EDStatic.config.allowedHosts =
          java.util.Collections.synchronizedSet(new java.util.HashSet<String>());
      EDStatic.config.allowedHosts.add("erddap.example.org");
      EDStatic.config.allowedHosts.add("proxy.example.org");

      // Test a valid host header (exact match)
      HttpServletRequest reqValid1 = Mockito.mock(HttpServletRequest.class);
      Mockito.when(reqValid1.getHeader("Host")).thenReturn("erddap.example.org");
      Mockito.when(reqValid1.getScheme()).thenReturn("http");
      Test.ensureEqual(
          EDStatic.baseUrl(reqValid1, null),
          "http://erddap.example.org",
          "Exact allowed host match");

      // Test a valid host header with trailing port and mixed case
      HttpServletRequest reqValid2 = Mockito.mock(HttpServletRequest.class);
      Mockito.when(reqValid2.getHeader("Host")).thenReturn("ERDDAP.EXAMPLE.ORG:8080");
      Mockito.when(reqValid2.getScheme()).thenReturn("http");
      Test.ensureEqual(
          EDStatic.baseUrl(reqValid2, null),
          "http://erddap.example.org:8080",
          "Case-insensitive allowed host match with port");

      // Test using X-Forwarded-Host
      HttpServletRequest reqValidXF = Mockito.mock(HttpServletRequest.class);
      Mockito.when(reqValidXF.getHeader("X-Forwarded-Host")).thenReturn("proxy.example.org");
      Mockito.when(reqValidXF.getHeader("Host")).thenReturn("evil.com");
      Mockito.when(reqValidXF.getScheme()).thenReturn("http");
      Test.ensureEqual(
          EDStatic.baseUrl(reqValidXF, null),
          "http://proxy.example.org",
          "X-Forwarded-Host takes precedence and matches allowlist");

      // Extract what the fallback host should be from actual baseUrl/baseHttpsUrl since they are
      // final
      String expectedFallbackHttpHost = "";
      try {
        java.net.URI uri = new java.net.URI(EDStatic.config.baseUrl);
        expectedFallbackHttpHost = uri.getHost();
        if (uri.getPort() != -1) {
          expectedFallbackHttpHost += ":" + uri.getPort();
        }
      } catch (Exception e) {
      }

      HttpServletRequest reqInvalidHttp = Mockito.mock(HttpServletRequest.class);
      Mockito.when(reqInvalidHttp.getHeader("Host")).thenReturn("evil.com");
      Mockito.when(reqInvalidHttp.getScheme()).thenReturn("http");
      Test.ensureEqual(
          EDStatic.baseUrl(reqInvalidHttp, null),
          "http://" + expectedFallbackHttpHost,
          "Invalid host on http should fallback to baseUrl host and port");

      String expectedFallbackHttpsHost = expectedFallbackHttpHost;
      if (EDStatic.config.baseHttpsUrl != null
          && !EDStatic.config.baseHttpsUrl.trim().isEmpty()
          && !EDStatic.config.baseHttpsUrl.equalsIgnoreCase("(not specified)")) {
        try {
          java.net.URI uri = new java.net.URI(EDStatic.config.baseHttpsUrl);
          String h = uri.getHost();
          if (h != null) {
            expectedFallbackHttpsHost = h;
            if (uri.getPort() != -1) {
              expectedFallbackHttpsHost += ":" + uri.getPort();
            }
          }
        } catch (Exception e) {
        }
      }

      // Test invalid host on HTTPS, fallback to baseHttpsUrl
      HttpServletRequest reqInvalidHttps = Mockito.mock(HttpServletRequest.class);
      Mockito.when(reqInvalidHttps.getHeader("Host")).thenReturn("evil.com");
      Mockito.when(reqInvalidHttps.getScheme()).thenReturn("https");
      Test.ensureEqual(
          EDStatic.baseUrl(reqInvalidHttps, null),
          "https://" + expectedFallbackHttpsHost,
          "Invalid host on https should fallback to baseHttpsUrl host and port");

      // 4. Test Chained Proxies / Comma-separated X-Forwarded-Host
      HttpServletRequest reqChainedProxy = Mockito.mock(HttpServletRequest.class);
      Mockito.when(reqChainedProxy.getHeader("X-Forwarded-Host"))
          .thenReturn("erddap.example.org, proxy1.example.org, proxy2.example.org");
      Mockito.when(reqChainedProxy.getHeader("Host")).thenReturn("erddap.example.org");
      Mockito.when(reqChainedProxy.getScheme()).thenReturn("http");
      Test.ensureEqual(
          EDStatic.baseUrl(reqChainedProxy, null),
          "http://erddap.example.org",
          "Chained proxy: first element picked and matched");

      // Check underscore hostname when verification is enabled
      EDStatic.config.allowedHosts.add("local_dev");
      HttpServletRequest reqUnderscoreValid = Mockito.mock(HttpServletRequest.class);
      Mockito.when(reqUnderscoreValid.getHeader("Host")).thenReturn("LOCAL_DEV:8080");
      Mockito.when(reqUnderscoreValid.getScheme()).thenReturn("http");
      Test.ensureEqual(
          EDStatic.baseUrl(reqUnderscoreValid, null),
          "http://local_dev:8080",
          "Verification enabled must support local hostnames containing underscores if allowed");

      // 5. Test Wildcard Allowed Hosts
      EDStatic.config.allowedHosts.add("*.allowed-wild.com");
      EDStatic.config.allowedHosts.add(".another-wild.org");

      HttpServletRequest reqWildcard1 = Mockito.mock(HttpServletRequest.class);
      Mockito.when(reqWildcard1.getHeader("Host")).thenReturn("sub.allowed-wild.com");
      Mockito.when(reqWildcard1.getScheme()).thenReturn("http");
      Test.ensureEqual(
          EDStatic.baseUrl(reqWildcard1, null),
          "http://sub.allowed-wild.com",
          "Wildcard prefix match (*.allowed-wild.com)");

      HttpServletRequest reqWildcard2 = Mockito.mock(HttpServletRequest.class);
      Mockito.when(reqWildcard2.getHeader("Host")).thenReturn("deep.sub.another-wild.org");
      Mockito.when(reqWildcard2.getScheme()).thenReturn("http");
      Test.ensureEqual(
          EDStatic.baseUrl(reqWildcard2, null),
          "http://deep.sub.another-wild.org",
          "Wildcard prefix match (.another-wild.org)");

      HttpServletRequest reqWildcardEvil = Mockito.mock(HttpServletRequest.class);
      Mockito.when(reqWildcardEvil.getHeader("Host")).thenReturn("evil-allowed-wild.com");
      Mockito.when(reqWildcardEvil.getScheme()).thenReturn("http");
      Test.ensureEqual(
          EDStatic.baseUrl(reqWildcardEvil, null),
          "http://" + expectedFallbackHttpHost,
          "Wildcard must not match substring without dot boundary");

      // 6. Test IPv6 Handling
      EDStatic.config.allowedHosts.add("[::1]");
      EDStatic.config.allowedHosts.add("[2001:db8::1]");

      HttpServletRequest reqIPv6_1 = Mockito.mock(HttpServletRequest.class);
      Mockito.when(reqIPv6_1.getHeader("Host")).thenReturn("[::1]");
      Mockito.when(reqIPv6_1.getScheme()).thenReturn("http");
      Test.ensureEqual(
          EDStatic.baseUrl(reqIPv6_1, null), "http://[::1]", "Safe bracketed IPv6 match");

      HttpServletRequest reqIPv6_2 = Mockito.mock(HttpServletRequest.class);
      Mockito.when(reqIPv6_2.getHeader("Host")).thenReturn("[2001:db8::1]:8080");
      Mockito.when(reqIPv6_2.getScheme()).thenReturn("http");
      Test.ensureEqual(
          EDStatic.baseUrl(reqIPv6_2, null),
          "http://[2001:db8::1]:8080",
          "Safe bracketed IPv6 with port match");

      // 7. Test Attack String Sanitation (cleanUrlChars and normalizeAndValidateHost)
      Test.ensureEqual(
          EDStatic.cleanUrlChars("evil.com/../path", true),
          "evil.com/../path",
          "cleanUrlChars keeps safe chars");
      // But normalizeAndValidateHost will discard anything with unsafe path segments or malformed
      // hosts
      Test.ensureEqual(
          EDStatic.normalizeAndValidateHost("evil.com/../path"),
          "",
          "normalizeAndValidateHost blocks path traversal in domain");
      Test.ensureEqual(
          EDStatic.normalizeAndValidateHost("evil.com%2e%2e%2fpath"),
          "",
          "normalizeAndValidateHost blocks encoded path traversal in domain");
      Test.ensureEqual(
          EDStatic.normalizeAndValidateHost("<script>alert(1)</script>.example.org"),
          "",
          "normalizeAndValidateHost blocks XSS in domain");

      // 3. Test Domain Extraction helper method
      Test.ensureEqual(
          String2.extractDomain("http://myhost.com:8080/erddap", false),
          "myhost.com",
          "extractDomain: standard HTTP with port");
      Test.ensureEqual(
          String2.extractDomain("http://myhost.com:8080/erddap", true),
          "myhost.com:8080",
          "extractDomain: standard HTTP with port");
      Test.ensureEqual(
          String2.extractDomain("https://sub.domain.com/path", false),
          "sub.domain.com",
          "extractDomain: standard HTTPS");
      Test.ensureEqual(
          String2.extractDomain("just.host.name", false),
          "just.host.name",
          "extractDomain: no protocol");
      Test.ensureEqual(
          String2.extractDomain("(not specified)", false), null, "extractDomain: (not specified)");
      Test.ensureEqual(String2.extractDomain(null, false), null, "extractDomain: null input");
      Test.ensureEqual(
          String2.extractDomain("http://[::1]:8080/erddap", false),
          "[::1]",
          "extractDomain: bracketed IPv6 with port");
      Test.ensureEqual(
          String2.extractDomain("http://[::1]:8080/erddap", true),
          "[::1]:8080",
          "extractDomain: bracketed IPv6 with port");

    } finally {
      // Restore cached configurations
      EDStatic.config.verifyHostNameErddapUrl = cachedVerify;
      EDStatic.config.allowedHosts = cachedAllowed;
    }
  }

  private void checkUrlExpectation(String message, String expected, String result) {
    Test.ensureEqual(result, expected, message + ": expected=" + expected + " got=" + result);
  }
}
