package com.cohort.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class String2Tests {

  private static class LinkResult {
    String text;
    boolean isUrl;

    LinkResult(String text, boolean isUrl) {
      this.text = text;
      this.isUrl = isUrl;
    }
  }

  private List<LinkResult> linkify(String input) {
    List<LinkResult> results = new ArrayList<>();
    LinkHelper.linkify(input, (text, isUrl) -> results.add(new LinkResult(text, isUrl)));
    return results;
  }

  private void assertNoUrl(String input) {
    List<LinkResult> results = linkify(input);
    for (LinkResult res : results) {
      assertFalse(res.isUrl, "Found unexpected URL: " + res.text);
    }
  }

  private void assertUrl(String input, String expectedUrl) {
    List<LinkResult> results = linkify(input);
    boolean found = false;
    for (LinkResult res : results) {
      if (res.isUrl) {
        assertEquals(expectedUrl, res.text);
        found = true;
      }
    }
    assertTrue(found, "Expected URL not found: " + expectedUrl);
  }

  @Test
  void testLinkify() {
    assertNoUrl("No url here. But there are multiple sentences!");
    assertNoUrl("https://this is definitely not valid URL! \"{><}\"");

    // check a basic whole string url
    assertUrl("http://www.example.com", "http://www.example.com");
    assertUrl("http:\\\\www.example.com", "http:\\\\www.example.com");
    assertUrl("https://www.example.com", "https://www.example.com");
    assertUrl("https:\\\\www.example.com", "https:\\\\www.example.com");

    // check for ip address instead of domain
    assertUrl(
        "http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day",
        "http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day");

    // check for no protocol (but a www)
    assertUrl("www.something.com", "www.something.com");

    // check for . within a path
    assertUrl(
        "https://tds.hycom.org/thredds/dodsC/GLBu0.08/expt_90.9.html",
        "https://tds.hycom.org/thredds/dodsC/GLBu0.08/expt_90.9.html");
    assertUrl("https://doi.org/10.25921/RE9P-PT57", "https://doi.org/10.25921/RE9P-PT57");

    // check for ? within a fragment
    assertUrl(
        "https://data.cencoos.org/#search?type_group=all&query=hab&page=1",
        "https://data.cencoos.org/#search?type_group=all&query=hab&page=1");

    // check a complex url (including spaces inside quotes of query)
    assertUrl(
        "https://data-erddap.emodnet-physics.eu/erddap/tabledap/EP_PLATFORMS_METADATA.htmlTable?&PLATFORMCODE=%22Casey Skiway%22&integrator_id=%22aad%22&distinct()",
        "https://data-erddap.emodnet-physics.eu/erddap/tabledap/EP_PLATFORMS_METADATA.htmlTable?&PLATFORMCODE=%22Casey Skiway%22&integrator_id=%22aad%22&distinct()");

    assertUrl(
        "https://data-erddap.emodnet-physics.eu/erddap/tabledap/EP_PLATFORMS_METADATA.htmlTable?&PLATFORMCODE=\"Casey Skiway\"&integrator_id=%22aad%22&distinct()",
        "https://data-erddap.emodnet-physics.eu/erddap/tabledap/EP_PLATFORMS_METADATA.htmlTable?&PLATFORMCODE=\"Casey Skiway\"&integrator_id=%22aad%22&distinct()");

    assertUrl(
        "https://kgs.uky.edu/usgin/services/aasggeothermal/WVBoreholeTemperatures/MapServer/WFSServer?request=GetFeature&service=WFS&typename=aasg:BoreholeTemperature&format=%22text/xml;%20subType=gml/3.1.1/profiles/gmlsf/1.0.0/0%22",
        "https://kgs.uky.edu/usgin/services/aasggeothermal/WVBoreholeTemperatures/MapServer/WFSServer?request=GetFeature&service=WFS&typename=aasg:BoreholeTemperature&format=%22text/xml;%20subType=gml/3.1.1/profiles/gmlsf/1.0.0/0%22");

    // gopher
    assertUrl(
        "gopher://sdf.org/1/users/YOUR-USERNAME/cgi-bin/ls.cgi",
        "gopher://sdf.org/1/users/YOUR-USERNAME/cgi-bin/ls.cgi");
    assertUrl(
        "gopher:\\\\sdfeu.org/1/users/YOUR-USERNAME/ls.cgi?date",
        "gopher:\\\\sdfeu.org/1/users/YOUR-USERNAME/ls.cgi?date");

    // File
    assertUrl("file://some/example/file/path.ext", "file://some/example/file/path.ext");
    assertUrl("file:\\\\other\\file\\path\\with.jpg", "file:\\\\other\\file\\path\\with.jpg");

    // telnet
    assertUrl("telnet://host.edu/", "telnet://host.edu/");
    assertUrl("telnet:\\\\user:pass@sub.host.edu:8080", "telnet:\\\\user:pass@sub.host.edu:8080");
    assertUrl("telnet://user@host.edu:8080", "telnet://user@host.edu:8080");
    assertUrl("telnet://user@host.edu:8080/", "telnet://user@host.edu:8080/");

    // smtp
    assertUrl("smtp:\\\\user:pass@sub.host.edu:8080", "smtp:\\\\user:pass@sub.host.edu:8080");
    assertUrl("smtp://user@host.edu", "smtp://user@host.edu");

    // ftp
    assertUrl(
        "ftp:\\\\user:pass@sub.host.edu:8080\\some\\file\\path",
        "ftp:\\\\user:pass@sub.host.edu:8080\\some\\file\\path");
    assertUrl("ftp://user@host.edu/some/other/file.ext", "ftp://user@host.edu/some/other/file.ext");

    // smb
    assertUrl(
        "smb:\\\\user:pass@sub.host.edu:8080\\some\\file\\path",
        "smb:\\\\user:pass@sub.host.edu:8080\\some\\file\\path");
    assertUrl("smb://user@host.edu/some/other/file.ext", "smb://user@host.edu/some/other/file.ext");
  }

  @Test
  void testLinkify_complex() {
    // two urls
    List<LinkResult> results = linkify("http://cencoos.org/, https://www.axiomdatascience.com");
    assertEquals(3, results.size());
    assertEquals("http://cencoos.org/", results.get(0).text);
    assertTrue(results.get(0).isUrl);
    assertEquals(", ", results.get(1).text);
    assertFalse(results.get(1).isUrl);
    assertEquals("https://www.axiomdatascience.com", results.get(2).text);
    assertTrue(results.get(2).isUrl);

    results = linkify("https://marinescience.ucdavis.edu/ and https://cordellbank.noaa.gov/");
    assertEquals(3, results.size());
    assertEquals("https://marinescience.ucdavis.edu/", results.get(0).text);
    assertTrue(results.get(0).isUrl);
    assertEquals(" and ", results.get(1).text);
    assertFalse(results.get(1).isUrl);
    assertEquals("https://cordellbank.noaa.gov/", results.get(2).text);
    assertTrue(results.get(2).isUrl);

    // url embedded in a sentence
    results = linkify("Find the url that is www.somewhere.com in this sentence.");
    assertEquals(3, results.size());
    assertEquals("Find the url that is ", results.get(0).text);
    assertFalse(results.get(0).isUrl);
    assertEquals("www.somewhere.com", results.get(1).text);
    assertTrue(results.get(1).isUrl);
    assertEquals(" in this sentence.", results.get(2).text);
    assertFalse(results.get(2).isUrl);

    results = linkify("Find the url that is (inside a www.parenthetical.com) in this sentence.");
    assertEquals(3, results.size());
    assertEquals("Find the url that is (inside a ", results.get(0).text);
    assertFalse(results.get(0).isUrl);
    assertEquals("www.parenthetical.com", results.get(1).text);
    assertTrue(results.get(1).isUrl);
    assertEquals(") in this sentence.", results.get(2).text);
    assertFalse(results.get(2).isUrl);

    results =
        linkify(
            "Here a http:\\\\www.sentence.com with multiple ftp://user:pass@links.inside, can we find the right one?");
    assertEquals(5, results.size());
    assertEquals("Here a ", results.get(0).text);
    assertFalse(results.get(0).isUrl);
    assertEquals("http:\\\\www.sentence.com", results.get(1).text);
    assertTrue(results.get(1).isUrl);
    assertEquals(" with multiple ", results.get(2).text);
    assertFalse(results.get(2).isUrl);
    assertEquals("ftp://user:pass@links.inside", results.get(3).text);
    assertTrue(results.get(3).isUrl);
    assertEquals(", can we find the right one?", results.get(4).text);
    assertFalse(results.get(4).isUrl);
  }
}
