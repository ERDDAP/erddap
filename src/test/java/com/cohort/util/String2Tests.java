package com.cohort.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class String2Tests {
  @Test
  void testFindUrl() {
    int[] results = String2.findUrl("No url here. But there are multiple sentences!");
    assertEquals(-1, results[0]);
    assertEquals(-1, results[1]);

    results = String2.findUrl("https://this is definitely not valid URL! \"{><}\"");
    assertEquals(-1, results[0]);
    assertEquals(-1, results[1]);

    // check a basic whole string url
    results = String2.findUrl("http://www.example.com");
    assertEquals(0, results[0]);
    assertEquals(22, results[1]);
    results = String2.findUrl("http:\\\\www.example.com");
    assertEquals(0, results[0]);
    assertEquals(22, results[1]);
    results = String2.findUrl("https://www.example.com");
    assertEquals(0, results[0]);
    assertEquals(23, results[1]);
    results = String2.findUrl("https:\\\\www.example.com");
    assertEquals(0, results[0]);
    assertEquals(23, results[1]);

    // check for ip address instead of domain
    results = String2.findUrl("http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day");
    assertEquals(0, results[0]);
    assertEquals(57, results[1]);

    // check for no protocol (but a www)
    results = String2.findUrl("www.something.com");
    assertEquals(0, results[0]);
    assertEquals(17, results[1]);

    // check for . within a path
    results = String2.findUrl("https://tds.hycom.org/thredds/dodsC/GLBu0.08/expt_90.9.html");
    assertEquals(0, results[0]);
    assertEquals(59, results[1]);
    results = String2.findUrl("https://doi.org/10.25921/RE9P-PT57");
    assertEquals(0, results[0]);
    assertEquals(34, results[1]);

    // check for ? within a fragment
    results = String2.findUrl("https://data.cencoos.org/#search?type_group=all&query=hab&page=1");
    assertEquals(0, results[0]);
    assertEquals(64, results[1]);

    // check a complex url (including spaces inside quotes of query)
    results =
        String2.findUrl(
            "https://data-erddap.emodnet-physics.eu/erddap/tabledap/EP_PLATFORMS_METADATA.htmlTable?&PLATFORMCODE=%22Casey Skiway%22&integrator_id=%22aad%22&distinct()");
    assertEquals(0, results[0]);
    assertEquals(154, results[1]);
    results =
        String2.findUrl(
            "https://data-erddap.emodnet-physics.eu/erddap/tabledap/EP_PLATFORMS_METADATA.htmlTable?&PLATFORMCODE=\"Casey Skiway\"&integrator_id=%22aad%22&distinct()");
    assertEquals(0, results[0]);
    assertEquals(150, results[1]);

    results =
        String2.findUrl(
            "https://kgs.uky.edu/usgin/services/aasggeothermal/WVBoreholeTemperatures/MapServer/WFSServer?request=GetFeature&service=WFS&typename=aasg:BoreholeTemperature&format=%22text/xml;%20subType=gml/3.1.1/profiles/gmlsf/1.0.0/0%22");
    assertEquals(0, results[0]);
    assertEquals(223, results[1]);

    // gopher
    results = String2.findUrl("gopher://sdf.org/1/users/YOUR-USERNAME/cgi-bin/ls.cgi");
    assertEquals(0, results[0]);
    assertEquals(53, results[1]);
    results = String2.findUrl("gopher:\\\\sdfeu.org/1/users/YOUR-USERNAME/ls.cgi?date");
    assertEquals(0, results[0]);
    assertEquals(52, results[1]);

    // File
    results = String2.findUrl("file://some/example/file/path.ext");
    assertEquals(0, results[0]);
    assertEquals(33, results[1]);
    results = String2.findUrl("file:\\\\other\\file\\path\\with.jpg");
    assertEquals(0, results[0]);
    assertEquals(31, results[1]);

    // telnet
    results = String2.findUrl("telnet://host.edu/");
    assertEquals(0, results[0]);
    assertEquals(18, results[1]);
    results = String2.findUrl("telnet:\\\\user:pass@sub.host.edu:8080");
    assertEquals(0, results[0]);
    assertEquals(36, results[1]);
    results = String2.findUrl("telnet://user@host.edu:8080");
    assertEquals(0, results[0]);
    assertEquals(27, results[1]);
    results = String2.findUrl("telnet://user@host.edu:8080/");
    assertEquals(0, results[0]);
    assertEquals(28, results[1]);

    // smtp
    results = String2.findUrl("smtp:\\\\user:pass@sub.host.edu:8080");
    assertEquals(0, results[0]);
    assertEquals(34, results[1]);
    results = String2.findUrl("smtp://user@host.edu");
    assertEquals(0, results[0]);
    assertEquals(20, results[1]);

    // ftp
    results = String2.findUrl("ftp:\\\\user:pass@sub.host.edu:8080\\some\\file\\path");
    assertEquals(0, results[0]);
    assertEquals(48, results[1]);
    results = String2.findUrl("ftp://user@host.edu/some/other/file.ext");
    assertEquals(0, results[0]);
    assertEquals(39, results[1]);

    // smb
    results = String2.findUrl("smb:\\\\user:pass@sub.host.edu:8080\\some\\file\\path");
    assertEquals(0, results[0]);
    assertEquals(48, results[1]);
    results = String2.findUrl("smb://user@host.edu/some/other/file.ext");
    assertEquals(0, results[0]);
    assertEquals(39, results[1]);
  }

  @Test
  void testFindUrl_complex() {
    // two urls, find the first
    int[] results = String2.findUrl("http://cencoos.org/, https://www.axiomdatascience.com");
    assertEquals(0, results[0]);
    assertEquals(19, results[1]);

    results =
        String2.findUrl("https://marinescience.ucdavis.edu/ and https://cordellbank.noaa.gov/");
    assertEquals(0, results[0]);
    assertEquals(34, results[1]);

    results =
        String2.findUrl(
            "https://marinescience.ucdavis.edu/ , https://cordellfoundation.org/hypoxia-studies-in-cordell-bank-national-marine-sanctuary-funded-by-cmsf/");
    assertEquals(0, results[0]);
    assertEquals(34, results[1]);

    // url embedded in a sentence
    results = String2.findUrl("Find the url that is www.somewhere.com in this sentence.");
    assertEquals(21, results[0]);
    assertEquals(38, results[1]);

    results =
        String2.findUrl("Find the url that is (inside a www.parenthetical.com) in this sentence.");
    assertEquals(31, results[0]);
    assertEquals(
        53, results[1]); // This should ideally be 52, but it's valid for urls to end with ')'

    results =
        String2.findUrl(
            "Here a http:\\\\www.sentence.com with multiple ftp://user:pass@links.inside, can we find the right one?");
    assertEquals(7, results[0]);
    assertEquals(30, results[1]);

    results =
        String2.findUrl(
            "Here a http:\\\\www.sentence.com with multiple ftp://user:pass@links.inside, can we find the right one?",
            30);
    assertEquals(45, results[0]);
    assertEquals(73, results[1]);
  }
}
