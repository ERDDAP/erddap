/*
 * HtmlWidgets Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file'ss directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.XML;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.File;
import java.text.MessageFormat;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * HtmlWidgets has methods to simplify creation of widgets in HTML forms.
 *
 * <p>"name" vs. "id" - for widgets, "name" is the required attributes. "id" is not required and
 * causes problems.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-09-04
 */
public class HtmlWidgets {

  /** Leave this set to false here, but change within a program if desired. */
  public static boolean debugMode = false;

  /**
   * This is the standard start of an HTML 5 document up to and including the 'head' and 'meta
   * charset' tags.
   */
  public static final String DOCTYPE_HTML =
      "<!DOCTYPE html>\n" + "<html lang=\"en-US\">\n" + "<head>\n" + "  <meta charset=\"UTF-8\">\n";

  /**
   * This is the standard start of an XHTML 1.0 document up to and including the 'head' tag. This is
   * specific to UTF-8. See comments about making xhtml render properly in browsers: Must use UTF-8;
   * remove ?xml header; use mime type text/html; https://www.w3.org/TR/xhtml1/#guidelines <br>
   * But it is more important that it be proper xml -- so leave xml tag in. <br>
   * More useful clues from https://www.w3.org/MarkUp/Forms/2003/xforms-for-html-authors which is
   * xhtml that renders correctly. It needs head/meta tags and head/style tags. (see below) And use
   * xhmtl validator at http://validator.w3.org/#validate_by_input for testing. <br>
   * In OutputStreamFromHttpResponse, don't use
   * response.setHeader("Content-Disposition","attachment;filename=" for xhtml files.
   */
  public static final String DOCTYPE_XHTML_TRANSITIONAL =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n"
          + "  \"https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
          + "<html xmlns=\"https://www.w3.org/1999/xhtml\">\n"
          + // from www.w3.org documents -- it's visible in all browsers!
          "<head>\n"
          + "  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n"; // +

  public static final String SANS_SERIF_STYLE =
      "font-family:Arial,Helvetica,sans-serif; font-size:85%; line-height:130%;";

  public static final String JAVASCRIPT_VOID = "javascript:void(0);";

  /**
   * This is the HTML for standalone Back button (which uses the browsers history to determine what
   * to go back to.
   */
  public static final String BACK_BUTTON =
      "&nbsp;\n"
          + "<br><button type=\"button\" onClick=\"history.go(-1);return true;\">Back</button>\n";

  public static final int BUTTONS_0n = -1, BUTTONS_1 = -2, BUTTONS_100 = -8, BUTTONS_1000 = -16;

  /** One line: white,grays,black, then one rainbow. */
  public static final String PALETTE17[] = {
    "FFFFFF", "CCCCCC", "999999", "666666", "000000", "FF0000", "FF9900", "FFFF00", "99FF00",
    "00FF00", "00FF99", "00FFFF", "0099FF", "0000FF", "9900FF", "FF00FF", "FF99FF"
  };

  /**
   * This will display a message to the user if JavaScript is not supported or disabled. Last
   * updated 2019-12-19. FUTURE: refer to https://enable-javascript.com/ ???
   */
  public static String ifJavaScriptDisabled =
      "<noscript><div style=\"color:red\"><strong>To work correctly, this web page requires that JavaScript be enabled in your browser.</strong> Please:\n"
          + "<br>1) Enable JavaScript in your browser:\n"
          + "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &bull; Chrome: \"Settings : Advanced : Privacy and security : Site Settings : JavaScript\"\n"
          +
          //        "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &bull; Edge: \n" +  ??? 2019-12-19 search
          // yielded: it should be supported but administrator may have disabled it.
          "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &bull; Firefox: (it should be always on!)\"\n"
          + "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &bull; Opera: \"Settings : Websites : JavaScript\"\n"
          + "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &bull; Safari: \"Safari : Preferences : Security : Enable JavaScript\"\n"
          + "<br>2) Reload this web page.\n"
          + "<br>&nbsp;</div>\n"
          + "</noscript>\n";

  /** The default tooltip for twoClickMap. */
  public static String twoClickMapDefaultTooltipAr[] =
      new String[] {
        "Specify a rectangle by clicking on two diagonal corners.  Do it again if needed."
      };

  public static String comboBoxAltAr[] =
      new String[] {"Hover here to see a list of options. Click on an option to select it."};
  public static String errorXWasntSpecifiedAr[] =
      new String[] {"Error: \"{0}\" wasn''t specified."};
  public static String errorXWasTooLongAr[] =
      new String[] {"\"{0}\" was more than {1} characters long."};

  /**
   * If you want to use Tip for big html tooltips (see below), include this in the 'body' of a web
   * page (preferably right after the body tag). General info: http://wztip.info/index.php/Main_Page
   * (was http://www.walterzorn.com/tooltip/tooltip_e.htm) (LGPL License -- so give credit). By
   * making this a reference to a file (not inline text), the .js file is downloaded once to the
   * user's browser, then cached if Expired header is set.
   *
   * <p>I last downloaded wz_tooltip.js on 2009-06-19 ver. 5.31. In wz_tooltip.js, I changed default
   * FontSize from 8pt to 10pt config. FontSize = '10pt' // E.g. '9pt' or '12px' - unit is mandatory
   *
   * @param dir a public directory from the web page's point of view (e.g.,
   *     EDStatic.imageDirUrl(loggedInAs, language)) where wz_tooltip.js can be found.
   */
  public static String htmlTooltipScript(String dir) {
    return "<!-- Big HTML tooltips are generated with wz_tooltip from \n"
        + "    http://wztip.info/index.php/Main_Page (LGPL license) -->\n"
        + "<script src=\""
        + dir
        + "wz_tooltip.js\""
        +
        // This is disabled for now because Chrome requires CORS header in order to validate
        // integrity.
        // " integrity=\"sha256-uXgD4ZNgdSBPIemQ5E8Vh5H9GEwMAQ3vXg4xJ6fhP/o=\"
        // crossorigin=\"anonymous\"" +
        "></script>\n"
        + "\n";
  }

  /**
   * HTML head stuff for leaflet.
   *
   * @param tErddapUrl e.g, from EDStatic.erddapUrl(loggedInAs, language): ending in "erddap",
   *     without the trailing slash
   */
  public static String leafletHead(String tErddapUrl) {
    return
    // leaflet.css assumes related images will be in /images/... within ERDDAP's /images
    // previously Leaflet v1.7.1?
    // 2022-07-08 Leaflet v1.8.0
    "  <link rel=\"stylesheet\" href=\""
        + tErddapUrl
        + "/images/leaflet.css\" >\n"
        + "  <script src=\""
        + tErddapUrl
        + "/images/leaflet.js\""
        +
        // This is disabled for now because Chrome requires CORS header in order to validate
        // integrity.
        // " integrity=\"sha256-UqKncSCrVxkH5z3QnoMGJ/98YTVI+LaVeUjXx/StJd4=\"
        // crossorigin=\"anonymous\"" +
        "></script>\n";
  }

  /**
   * This is the width and height of the sliderLeft.gif, sliderCenter.gif, and sliderRight.gif
   * images in imageDirUrl. sliderBg.gif must have sliderThumbHeight. (These were created in Paint,
   * not CoPlot.)
   */
  public static int sliderThumbWidth = 15;

  public static int sliderThumbHeight = 17;

  /**
   * This style removes the extra padding on the left and right of a button. But for ERDDAP, see CSS
   * style input.skinny in setup.xml.
   */
  public static String skinnyButtonStyle =
      "style=\"margin:0px; padding:0px 1px\" "; // top/bottom  left/right

  /**
   * This returns the html to include Walter Zorn's image dragDrop script for the 'body' of a web
   * page (preferably right after the body tag). General info: [website is gone!] (was
   * http://www.walterzorn.com/dragdrop/dragdrop_e.htm) (LGPL License -- so give credit). By making
   * this a reference to a file (not inline text), the .js file is downloaded once to the user's
   * browser, then cached if Expired header is set.
   *
   * @param dir a public directory from the web page's point of view (e.g., EDStatic.imageDirUrl())
   *     where wz_dragdrop.js can be found.
   */
  public static String dragDropScript(String dir) {
    return "<!-- Drag and Drop is performed by wz_dragdrop from\n"
        + "     http://www.walterzorn.com/dragdrop/dragdrop_e.htm (LGPL license) -->\n"
        + "<script src=\""
        + XML.encodeAsHTMLAttribute(dir)
        + "wz_dragdrop.js\""
        +
        // This is disabled for now because Chrome requires CORS header in order to validate
        // integrity.
        // " integrity=\"sha256-1i6DzhgAgCuch5/AP2XX+XTBm0B+0hV63hSDgqNuyoQ=\"
        // crossorigin=\"anonymous\"" +
        "></script>\n"
        + "\n";
  }

  public static String myMouseMove_SCRIPT =
      "<script> var myX, myY;\n"
          + "  function myMouseMove(e, obj) {e.preventDefault();\n"
          + "    if (e.buttons==1) {\n"
          + // 1=left button
          "      obj.parentElement.scrollLeft+=myX-e.screenX; \n"
          + "      obj.parentElement.scrollTop +=myY-e.screenY; }\n"
          + "    myX=e.screenX; myY=e.screenY; } \n"
          + "</script>\n";

  /**
   * This is a standalone javascript which does percent encoding of a string, similar to
   * SSR.minimalPercentEncode.
   */
  public static String PERCENT_ENCODE_JS =
      "<script> \n"
          + "function percentEncode(s) { \n"
          + "  var s2=\"\";\n"
          + "  for (var i = 0; i < s.length; i++) {\n"
          + "    var ch=s.charAt(i);\n"
          + "    if (ch == \"\\xA0\") s2+=\"%20\";\n"
          + // 0xA0=nbsp, see select(encodeSpaces)
          "    else s2+=encodeURIComponent(ch);\n"
          + "  }\n"
          + "  return s2;\n"
          + "}\n"
          + "</script>\n";

  // *********************** set by constructor  ****************************

  // these can be changed as needed while creating a form
  public boolean htmlTooltips;

  /** From EDStatic.imagedirUrl(loggedInAs, language). */
  public String imageDirUrl;

  // this is set by beginForm
  public String formName;

  /** If true, pressing Enter in a textField submits the form (default = false). */
  public boolean enterTextSubmitsForm = false;

  /** The default constructor. */
  public HtmlWidgets() {
    this(false, "");
  }

  /**
   * A constructor.
   *
   * @param tHtmlTooltips if true, tooltip text can be any HTML text of any length. If true, caller
   *     must have called htmlTooltipScript above. If false, tooltips are plain text and should be
   *     (less than 60 char on some browsers).
   * @param tImageDirUrl the public URL for the image dir which has the arrow, p... and m... .gif
   *     files for the 'select' buttons (or null or "" if not needed). Usually from
   *     EDStatic.imageDirUrl(loggedInAs, language)
   */
  public HtmlWidgets(boolean tHtmlTooltips, String tImageDirUrl) {
    htmlTooltips = tHtmlTooltips;
    imageDirUrl = tImageDirUrl;
  }

  /** This is mostly used internally; it returns the formatted style. */
  public String completeStyle(String tStyle) {
    // note space at beginning and end
    return tStyle == null || tStyle.length() == 0
        ? ""
        : " style=\"" + XML.encodeAsHTMLAttribute(tStyle) + "\" ";
  }

  /**
   * This is mostly used internally; it returns the formatted tooltip.
   *
   * @param tooltip If htmlTooltips is true, this is already html. If it is false, this is plain
   *     text. Or "" if no tooltip.
   * @return nothing if tooltip is nothing. Else an htmlTooltip or title= tooltip, with newline at
   *     start and space at end.
   */
  public String completeTooltip(String tooltip) {
    if (tooltip == null || tooltip.length() == 0) return "";
    // note space at beginning and end
    return htmlTooltips
        ? "\n  " + htmlTooltip(tooltip) + " "
        : "\n  title=\"" + XML.encodeAsHTMLAttribute(tooltip) + "\" ";
  }

  /**
   * This returns the html which displays an image (from a local file!) in a htmlTooltip represented
   * by a '?' icon.
   *
   * @param localFileName
   * @param xmlEncodedUrlFileName
   * @param questionMarkImageUrl
   * @return the html which displays an image (from a local file!) in a htmlTooltip represented by a
   *     '?' icon (or "" if trouble).
   */
  public static String imageInTooltip(
      String localFileName, String xmlEncodedUrlFileName, String questionMarkImageUrl) {

    // Problem: image initially isn't loaded, so tooltip size is too small.
    // I couldn't figure out how to use image onload= to trigger redraw (once).
    // In any case, that doesn't deal with huge images that need to be scaled down.
    // So I get the image size here and explicitly set <img width= height= >.
    try {
      int width = 0, height = 0;
      // from
      // https://stackoverflow.com/questions/1559253/java-imageio-getting-image-dimensions-without-reading-the-entire-file
      try (ImageInputStream in = ImageIO.createImageInputStream(new File(localFileName))) {
        if (in != null) {
          Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
          if (readers.hasNext()) {
            ImageReader reader = readers.next();
            try {
              reader.setInput(in);
              width = reader.getWidth(0);
              height = reader.getHeight(0);
            } finally {
              reader.dispose();
            }
          }
          if (width <= 0) return "";

          int max = Math.max(width, height);
          if (max > 600) {
            // scale to max=600
            double scale = 600.0 / max;
            width = Math2.roundToInt(width * scale);
            height = Math2.roundToInt(height * scale);
          }
          return htmlTooltipImage(
              questionMarkImageUrl,
              "?",
              "<img src=\""
                  + xmlEncodedUrlFileName
                  + "\" alt=\""
                  + xmlEncodedUrlFileName
                  + "\" width=\""
                  + width
                  + "px\" height=\""
                  + height
                  + "px\">",
              // a failed attempt to have this work without pre-specifying width and height:
              // " onload=\"var ps = this.parentElement.style; ps.width=this.style.width+'px';
              // ps.height=this.style.height+'px'; \">",
              ""); // other
        }
      }
    } catch (Exception e2) {
      String2.log(
          "Caught Exception while making imageInTooltip for "
              + localFileName
              + ":\n"
              + MustBe.throwableToString(e2));
    }
    return "";
  }

  /**
   * This generates the HTML to add a cssTooltip to the specified item.
   *
   * @param itemHtml The html for the thing (e.g., "?"-image) that is always visible
   * @param spanOther other parameters (e.g., style) for the inner span
   * @param tooltipHtml
   * @return the HTML to add a cssTooltip to the specified item.
   */
  public static String cssTooltip(String itemHtml, String spanOther, String tooltipHtml) {
    return "<span class=\"cssTooltip\">"
        + itemHtml
        + "<span class=\"cssTooltipInner\""
        + (spanOther.length() == 0 ? "" : " " + spanOther)
        + ">"
        + tooltipHtml
        + "</span></span>";
  }

  /**
   * This creates the html for an image ('?') with a cssTooltip with a deferred-load second image.
   * This is based on CSS-only image tooltip at https://codepen.io/electricalbah/pen/eJRLVd (URL
   * works in browser but not in link tests)
   *
   * @param img1Url for the image for the user to hover over, e.g., questionMarkImageUrl. It must be
   *     in the images directory.
   * @param img1Alt alt text for the imgUrl, e.g., "?". It will be encoded as HTML attribute.
   * @param img1Other additional parameter=values for img1. It should end with a space.
   * @param img2Url
   * @param img2ID a unique (for the document) id, e.g., img1. Just simple chars -- it isn't further
   *     encoded.
   * @return the html for an image ('?') with a cssTooltip with a deferred-load second image.
   */
  public static String cssTooltipImage(
      String img1Url, String img1Alt, String img1Other, String img2Url, String img2ID) {

    String img1UrlEncoded = XML.encodeAsHTMLAttribute(img1Url);
    String loadingUrl = File2.getDirectory(img1Url) + "loading.png";
    String loadingUrlEncoded = XML.encodeAsHTMLAttribute(loadingUrl);
    return

    // the original: a popup, no bigger than 90% of screen width
    // In Chrome, a big image is shrunk to 90%. In Firefox, big image is cropped.
    cssTooltip(
        "<img "
            + img1Other
            + "src=\""
            + img1UrlEncoded
            + "\" alt=\""
            + XML.encodeAsHTMLAttribute(img1Alt)
            + "\"\n"
            + "  onmouseover=\"var el=document.getElementById('"
            + img2ID
            + "'); el.setAttribute('src',el.getAttribute('data-src'));\">", // I use data-src to
        // tell browser to delay
        // loading this image
        // until it is visible
        "style=\"padding:0px; max-width:90%;\"",
        "<img style=\"max-width:100%;\" "
            + "id=\""
            + img2ID
            + "\" class=\"B\" src=\""
            + loadingUrlEncoded
            + "\"\n"
            + // vertical-align: 'b'ottom
            "  data-src=\""
            + XML.encodeAsHTMLAttribute(img2Url)
            + // I use data-src to tell browser to delay loading this image until it is visible
            "\" alt=\""
            + XML.encodeAsHTMLAttribute(File2.getNameAndExtension(img2Url))
            + "\">");

    /*
    //EXPERIMENT with leaflet
    //This requires leaflet .js and .css in &gt;head&lt;.
    //Leaflet appears, but the image doesn't show up in this context.
    //It takes <div> to sort of work. But I need to use <span> here.
    //Otherwise, Leaflet is best: supports mouse and touch.
    "<div class=\"cssTooltip\">" +

    "<img " + img1Other + "src=\"" + img1UrlEncoded +
        "\" alt=\"" + XML.encodeAsHTMLAttribute(img1Alt) + "\"\n" +
        //"onmouseover=\"var el=document.getElementById('" + img2ID + "'); el.setAttribute('src',el.getAttribute('data-src'));\"" +
        ">" +

    "<div class=\"cssTooltipInner\">\n" +
    "  <div id=\"map\" style=\"width:600px; height:400px;\" ></div>\n" +
    "  <script>\n" +
    "    var map = L.map('map', {crs: L.CRS.Simple, minZoom:-10});\n" +
    "    var bounds = [[0,0], [1000,1000]];\n" +
    "    var image = L.imageOverlay('" + img2Url + //XML.encodeAsHTMLAttribute(img2Url) +
        "', bounds).addTo(map);\n" +
    "    map.fitBounds(bounds);\n" +
    "  </script>\n" +

    "</div></div>";  */

    /*
    //EXPERIMENT with bottons to zoom in/out.
    //This requires myMouseMove_SCRIPT in &gt;head&lt;.
    //The scrollbars on innermost span only show up in Chrome.
    //<div> would work but can't be used within <span> as here.
    //And this doesn't support touch yet. Leaflet is better.
    "<span class=\"cssTooltip\">" +

    "<img " + img1Other + "src=\"" + img1UrlEncoded +
        "\" alt=\"" + XML.encodeAsHTMLAttribute(img1Alt) + "\"\n" +
        "onmouseover=\"var el=document.getElementById('" + img2ID + "'); el.setAttribute('src',el.getAttribute('data-src'));\">" +

    "<span class=\"cssTooltipInner\" " +
        "style=\"padding:0px; width:600px; height:400px; overflow:hidden; \" >" +

    "Zoom:\n" +
    "<button type=\"button\" onclick=\"var el=document.getElementById('" + img2ID + "'); el.style.maxWidth=(parseInt(el.style.maxWidth) * 2)  +'px'; \">+</button>\n" +
    "<button type=\"button\" onclick=\"var el=document.getElementById('" + img2ID + "'); el.style.maxWidth=(parseInt(el.style.maxWidth) * 0.5)+'px'; \">-</button>\n" +
    "<br><span style=\"width:588px; height:374px; border:0px; overflow:auto; \">" +
        "<img " +
          "onmousemove=\"myMouseMove(event, this);\" " +
          "style=\"max-width:588px;\" " +
          "id=\"" + img2ID + "\" class=\"B\" src=\"" + loadingUrlEncoded + "\"\n" + //vertical-align: 'b'ottom
          "data-src=\"" + XML.encodeAsHTMLAttribute(img2Url) +
          "\" alt=\"" + XML.encodeAsHTMLAttribute(File2.getNameAndExtension(img2Url)) + "\"" +
        ">" +
    "</span>\n" +

    "</span></span>"; */

  }

  /**
   * This creates the html for an base64 encoded image ('?') with a cssTooltip with a deferred-load
   * second image. This is based on CSS-only image tooltip at
   * https://codepen.io/electricalbah/pen/eJRLVd (URL works in browser but not in link tests) This
   * code was contributed by Marco Alba and slightly modified (so the image data occurs only once in
   * the HTML document) by Bob Simons.
   *
   * @param img1Url for the image for the user to hover over, e.g., questionMarkImageUrl. It must be
   *     in the images directory.
   * @param img1Alt alt text for the imgUrl, e.g., "?". It will be encoded as HTML attribute.
   * @param img1Other additional parameter=values for img1. It should end with a space.
   * @param img2Url The base64 encoded image, starting with "data:image/png;base64,".
   * @param img2ID a unique (for the document) id, e.g., img1. Just simple chars -- it isn't further
   *     encoded.
   * @param language the index of the selected language
   * @return the html for an image ('?') with a cssTooltip with a deferred-load second image.
   */
  public static String cssTooltipImageBase64(
      String img1Url,
      String img1Alt,
      String img1Other,
      String img2Url,
      String img2ID,
      int language) {

    String img1UrlEncoded = XML.encodeAsHTMLAttribute(img1Url);
    String loadingUrl = File2.getDirectory(img1Url) + "loading.png";
    String loadingUrlEncoded = XML.encodeAsHTMLAttribute(loadingUrl);
    return

    // the original: a popup, no bigger than 90% of screen width
    // In Chrome, a big image is shrunk to 90%. In Firefox, big image is cropped.
    cssTooltip(
            "<img "
                + img1Other
                + " src=\""
                + img1UrlEncoded
                + "\" alt=\""
                + XML.encodeAsHTMLAttribute(img1Alt)
                + "\"\n"
                + "  onmouseover=\"var el=document.getElementById('"
                + img2ID
                + "'); el.setAttribute('src',el.getAttribute('data-src'));\"/>", // I use data-src
            // to tell browser
            // to delay loading
            // this image until
            // it is visible
            "style=\"padding:0px; max-width:90%;\"",
            "<img style=\"max-width:100%;\" "
                + "id=\""
                + img2ID
                + "\" class=\"B\" src=\""
                + loadingUrlEncoded
                + "\"\n"
                + // vertical-align: 'b'ottom
                " data-src=\""
                + img2Url
                + "\""
                + // I use data-src to tell browser to delay loading this image until it is visible
                " alt=\"data:image/png;base64\">")
        +

        // "copy text/image to clipboard" buttons
        "\ndata:image/png;base64\n"
        + "<button type=\"button\" onclick=\"javascript:if(navigator.clipboard==undefined){alert('"
        + EDStatic.copyToClipboardNotAvailableAr[language]
        + "');return false};navigator.clipboard.writeText("
        + "document.getElementById('"
        + img2ID
        + "').getAttribute('data-src')"
        + // reuse img2Url data from img2ID.data-src
        ");\" style=\"cursor: pointer; cursor: hand;\" >"
        + EDStatic.copyTextToClipboardAr[language]
        + "</button>\n"
        + "<button type=\"button\" onclick=\"javascript:if(navigator.clipboard==undefined){alert('"
        + EDStatic.copyToClipboardNotAvailableAr[language]
        + "');return false};"
        + " var img = new Image();"
        + " img.onload = () => {"
        + " const c = document.createElement('canvas');"
        + " const ctx = c.getContext('2d');"
        + " c.width = img.naturalWidth;"
        + " c.height = img.naturalHeight;"
        + " ctx.drawImage(img, 0, 0);"
        + " const blob=c.toBlob((blob) => {const item = new ClipboardItem({ 'image/png': blob });navigator.clipboard.write([item]);},'image/png',0.75);};"
        + " img.src = document.getElementById('"
        + img2ID
        + "').getAttribute('data-src');"
        + // reuse img2Url data from img2ID.data-src
        " return false;\""
        + " style=\"cursor: pointer; cursor: hand;\" >"
        + EDStatic.copyImageToClipboardAr[language]
        + "</button>";
  }

  /**
   * This creates the html for an image ('?') with a cssTooltip with a deferred-load video player.
   * This is based on CSS-only image tooltip at https://codepen.io/electricalbah/pen/eJRLVd (URL
   * works in browser but not in link tests)
   *
   * @param imgUrl for the image for the user to hover over, e.g., questionMarkImageUrl
   * @param imgAlt alt text for the imgUrl, e.g., "?". It will be encoded as HTML attribute.
   * @param imgOther additional parameter=values for the img. It should end with a space.
   * @param vidUrl works for me in Chrome with .mp4, .ogv, .webm, but not .3gp
   * @return the html for an image ('?') with a cssTooltip with a video player.
   */
  public static String cssTooltipVideo(
      String imgUrl, String imgAlt, String imgOther, String vidUrl) {

    return cssTooltip(
        "<img "
            + imgOther
            + "src=\""
            + XML.encodeAsHTMLAttribute(imgUrl)
            + "\" alt=\""
            + XML.encodeAsHTMLAttribute(imgAlt)
            + "\">",
        "style=\"padding:0px; max-width:90%;\"",
        "<video style=\"max-width:100%;\" "
            + "controls preload=\"none\" class=\"B\">"
            + // vertical-align: 'b'ottom
            "<source src=\""
            + XML.encodeAsHTMLAttribute(vidUrl)
            + "\">"
            + "</video>");
  }

  /**
   * This creates the html for an image ('?') with a cssTooltip with a deferred-load audio player.
   * This is based on CSS-only image tooltip at https://codepen.io/electricalbah/pen/eJRLVd (URL
   * works in browser but not in link tests)
   *
   * @param imgUrl for the image for the user to hover over, e.g., questionMarkImageUrl
   * @param imgAlt alt text for the imgUrl, e.g., "?". It will be encoded as HTML attribute.
   * @param imgOther additional parameter=values for img. It should end with a space.
   * @param audUrl works for me in Chrome with .wav
   * @return the html for an image ('?') with a cssTooltip with a audio player, or "" if the audio
   *     file isn't a suitable type for the audio player.
   */
  public static String cssTooltipAudio(
      String imgUrl, String imgAlt, String imgOther, String audUrl) {

    return cssTooltip(
        "<img "
            + imgOther
            + "src=\""
            + XML.encodeAsHTMLAttribute(imgUrl)
            + "\" alt=\""
            + XML.encodeAsHTMLAttribute(imgAlt)
            + "\">",
        "style=\"padding:0px;\"", // css default is 2px
        htmlAudioControl(XML.encodeAsHTMLAttribute(audUrl)));
  }

  /**
   * This returns an HTML audio control.
   *
   * @param htmlAttEncodedUrlFileName
   * @return an HTML audio control (or "" if trouble)
   */
  public static String htmlAudioControl(String htmlAttEncodedUrlFileName) {
    String mime = audioMimeType(File2.getExtension(htmlAttEncodedUrlFileName));
    // FUTURE: make my own control with a longer time bar and no volume/download option?
    //  or use https://howlerjs.com/

    return
    // HTML oddity: <source> has no close tag or even /> at end
    "<audio controls preload=\"none\" class=\"B\">"
        + // loop?  //vertical-align: 'b'ottom
        "<source src=\""
        + htmlAttEncodedUrlFileName
        + "\""
        + (mime.length() == 0 ? "" : " type=\"audio/" + mime + "\"")
        + ">"
        + "</audio>";
  }

  /**
   * This returns the mime type for an audio file.
   *
   * @param ext the file extension (e.g., .wav)
   * @return the mime type for an audio file (or "" if you shouldn't show an audio control)
   */
  public static String audioMimeType(String ext) {
    String extLC = ext.toLowerCase();
    return extLC.startsWith(".aif")
        ? "x-aiff"
        : // aiff, aifc
        extLC.equals(".au")
            ? "basic"
            : extLC.equals(".mp3")
                ? "mpeg"
                : extLC.equals(".ogg")
                    ? "ogg"
                    : extLC.equals(".wav") || extLC.equals(".wave") ? "wav" : "";
  }

  /**
   * This creates the HTML code for the beginning of a form.
   *
   * @param name make it unique for this page
   * @param method usually GET or POST
   * @param url the url to which the form information will be submitted
   * @param other other attributes (or "" if none)
   * @return the HTML code for the beginning of a form.
   */
  public String beginForm(String name, String method, String url, String other) {
    formName = name;

    return "<form name=\""
        + XML.encodeAsHTMLAttribute(name)
        + "\" method=\""
        + XML.encodeAsHTMLAttribute(method)
        + "\"\n"
        + "  action=\""
        + (String2.isSomething(url) ? XML.encodeAsHTMLAttribute(url) : JAVASCRIPT_VOID)
        + "\" "
        + other
        + " >\n";
  }

  /**
   * This creates the HTML code for the end of a form.
   *
   * @return the HTML code for the end of a form.
   */
  public String endForm() {
    return "</form>\n";
  }

  /**
   * This creates the HTML code to begin a table.
   *
   * @param lineWidth width of lines (in pixels, 0 = none)
   * @param otherStyle e.g., width:2%;
   * @param otherAtts e.g., class="b0p0"
   * @return the HTML code to begin a table.
   */
  public String beginTable(int lineWidth, String otherStyle, String otherAtts) {
    // second param was int padding
    return "<table style=\"border:"
        + lineWidth
        + "px; "
        + "border-collapse:collapse; "
        + otherStyle
        + "\" "
        + otherAtts
        + ">\n";
  }

  /**
   * This creates the HTML code to begin a table.
   *
   * @param otherAtts attributes e.g., "style=\"width:100%;\" class=\"b0p0\""
   * @return the HTML code to begin a table.
   */
  public String beginTable(String otherAtts) {
    return "<table " + otherAtts + ">\n";
  }

  /**
   * This creates the HTML code to end a table.
   *
   * @return the HTML code to end a table.
   */
  public String endTable() {
    return "</table>\n";
  }

  /**
   * This creates the HTML code for a button using the 'input' tag. "labelText" is displayed and
   * name=labelText is returned if clicked
   *
   * @param type "submit" or "button". "submit" buttons submit the form to the form's url. "button"
   *     buttons do "other" things (via 'other').
   * @param name of the widget (name=labelText is returned when the form is submitted) (or null or
   *     "" if you don't need anything submitted with the form)
   * @param tooltip If htmlTooltips is true, this is already html. If it is false, this is plain
   *     text. Or "" if no tooltip.
   * @param labelText the plain text label visible to the user
   * @param other e.g., "onclick=\"pleaseWait();\""
   * @return the HTML code for a button.
   */
  public String button(String type, String name, String tooltip, String labelText, String other) {
    return
    // note old-style "input" tag
    "<input type=\""
        + type
        + "\""
        + (name != null && name.length() > 0
            ? " name=\"" + XML.encodeAsHTMLAttribute(name) + "\""
            : "")
        + " value=\""
        + XML.encodeAsHTMLAttribute(labelText)
        + "\""
        + " "
        + other
        + completeTooltip(tooltip)
        + " >\n";
  }

  /**
   * This creates the HTML code for a button widget using the 'button' tag. The labelTextHtml is
   * displayed and name=labelText is returned if clicked. This can be used by itself, without a
   * &lt;form&gt;.
   *
   * @param type "submit" or "button". "submit" buttons submit the form to the form's url. "button"
   *     buttons do "other" things (via 'other').
   * @param name of the widget (name=labelText is returned when the form is submitted) (or null or
   *     "" if you don't need anything submitted with the form)
   * @param value is the value returned when the form is submitted (name=value) (or null or "" if
   *     you don't need anything submitted with the form)
   * @param tooltip If htmlTooltips is true, this is already html. If it is false, this is plain
   *     text. Or "" if no tooltip.
   * @param labelHtml the html which will label the button (e.g.,an img tag)
   * @param other e.g., "onclick=\"pleaseWait();\""
   * @return the HTML code for the button widget.
   */
  public String htmlButton(
      String type, String name, String value, String tooltip, String labelHtml, String other) {
    return
    // note new-style "button" tag
    "<button type=\""
        + type
        + "\""
        + (name != null && name.length() > 0
            ? " name=\"" + XML.encodeAsHTMLAttribute(name) + "\""
            : "")
        + (value != null && value.length() > 0
            ? " value=\"" + XML.encodeAsHTMLAttribute(value) + "\""
            : "")
        + " "
        + other
        + completeTooltip(tooltip)
        + " >"
        + labelHtml
        + "</button>\n";
  }

  /**
   * This creates the HTML code for a checkbox. This also makes a "hidden" widget called
   * "previous_"+name with value=[checked] since un-checked checkboxes don't return info with the
   * form.
   *
   * @param name the name of the widget (if checked, name=value is returned when the form is
   *     submitted)
   * @param tooltip If htmlTooltips is true, this is already html. If it is false, this is plain
   *     text. Or "" if no tooltip.
   * @param checked the initial state of the checkbox
   * @param value (sometimes just "true")
   * @param rightLabelHtml "" if none
   * @param other e.g., "onclick=\"pleaseWait();\"". For checkbox(), use onclick, not onchange
   *     (which IE doesn't generate).
   * @return the HTML code for a checkbox.
   */
  public String checkbox(
      String name,
      String tooltip,
      boolean checked,
      String value,
      String rightLabelHtml,
      String other) {

    return "<input type=\"checkbox\" name=\""
        + XML.encodeAsHTMLAttribute(name)
        + "\""
        + " value=\""
        + XML.encodeAsHTMLAttribute(value)
        + "\""
        + completeTooltip(tooltip)
        + (checked ? "\n  checked=\"checked\"" : "")
        + " "
        + other
        + ">&nbsp;"
        + rightLabelHtml
        + "\n"
        +

        // add hidden so processRequest can distinguish notChecked from
        // first time user (default)
        hidden("previous_" + name, "" + checked);
  }

  /**
   * This create the HTML code for a comment.
   *
   * @param comment
   * @return the HTML code for a comment.
   */
  public String comment(String comment) {
    return "<!-- " + XML.encodeAsHTML(comment) + " -->\n";
  }

  /**
   * This creates the HTML code for a hidden widget.
   *
   * @param name the name of the widget (name=value is returned when the form is submitted)
   * @param value is the value of this attribute
   * @return the HTML code for a hidden widget.
   */
  public String hidden(String name, String value) {
    return "<input type=\"hidden\" name=\""
        + XML.encodeAsHTMLAttribute(name)
        + "\""
        + " value=\""
        + XML.encodeAsHTMLAttribute(value)
        + "\" >\n";
  }

  /**
   * This creates the HTML code for a series of radio buttons.
   *
   * @param name the name of the widget (name=optionText is returned when the form is submitted)
   * @param tooltip If htmlTooltips is true, this is already html. If it is false, this is plain
   *     text. Or "" if no tooltip.
   * @param onARow if true, the radio buttons will all be put on one row. Otherwise, they are put on
   *     separate lines separated by a linebreak.
   * @param optionTexts which provides the plain text to be displayed for each of the options
   *     (special characters will be html-encoded).
   * @param selected the index of the selected item or -1 if none
   * @param other e.g., "onclick=\"pleaseWait();\"" For radioButtons(), use onclick, not onchange
   *     (which IE doesn't generate).
   * @return the HTML code for a series of radio buttons.
   */
  public String radioButtons(
      String name,
      String tooltip,
      boolean onARow,
      String optionTexts[],
      int selected,
      String other) {

    StringBuilder sb = new StringBuilder();
    String br = onARow ? "    " : "    <br>";
    for (int i = 0; i < optionTexts.length; i++) {
      sb.append(i == 0 ? "    " : br);
      sb.append(radioButton(name, tooltip, optionTexts[i], i == selected, other));
    }
    return sb.toString();
  }

  /**
   * This creates the HTML code for one radio button.
   *
   * @param name the name of the widget (name=optionText is returned when the form is submitted)
   * @param tooltip If htmlTooltips is true, this is already html. If it is false, this is plain
   *     text. Or "" if no tooltip.
   * @param optionText which provides the plain text to be displayed for this option.
   * @param selected indicates if this item is selected
   * @param other e.g., "onclick=\"pleaseWait();\"" For radioButton(), use onclick, not onchange
   *     (which IE doesn't generate).
   * @return the HTML code for one radio button.
   */
  public String radioButton(
      String name, String tooltip, String optionText, boolean selected, String other) {

    String s = XML.encodeAsHTMLAttribute(optionText);
    return
    // <span> avoids check box and value being separated by newline when lots of options
    "<span class=\"nowrap\"><input type=\"radio\" name=\""
        + XML.encodeAsHTMLAttribute(name)
        + "\""
        + " value=\""
        + s
        + "\""
        + (selected ? " checked" : "")
        + completeTooltip(tooltip)
        + " "
        + other
        + " >"
        + s
        + "</span>\n";
  }

  /**
   * This creates the HTML code to display 17 radio buttons with colored backgrounds.
   *
   * @return the HTML code to display 17 radio buttons with colored backgrounds.
   */
  public String color17(String htmlLabel, String name, String tooltip, int selected, String other) {
    return color(PALETTE17, 17, htmlLabel, name, tooltip, selected, other);
  }

  /**
   * This creates the HTML code to display radio buttons with colored backgrounds.
   *
   * @param htmlLabel is an optional label (e.g., "Color: "), or use "" for nothing.
   * @param colors e.g., PALETTE17, each must be an RRGGBB value
   * @param perRow e.g., 17
   * @param selected can be -1 if none should be selected initially
   * @param other e.g., "onclick=\"pleaseWait();\"" For color(), use onclick, not onchange (which IE
   *     doesn't generate).
   * @return the HTML code to display radio buttons with colored backgrounds.
   */
  public String color(
      String[] colors,
      int perRow,
      String htmlLabel,
      String name,
      String tooltip,
      int selected,
      String other) {

    StringBuilder sb = new StringBuilder();
    sb.append("<table class=\"compact\">\n" + "  <tr>\n");
    boolean hasLabel = htmlLabel != null && htmlLabel.length() > 0;
    if (hasLabel) sb.append("<td class=\"N\">" + htmlLabel + "</td>\n");
    int inRow = 0;
    for (int i = 0; i < colors.length; i++) {
      // a radio button with the appropriate background color
      String checked = i == selected ? " checked" : "";
      sb.append(
          "<td style=\"padding-top:3px; background-color:#"
              + colors[i]
              + "\">"
              + "<input type=\"radio\" name=\""
              + XML.encodeAsHTMLAttribute(name)
              + "\" value=\""
              + colors[i]
              + "\""
              + checked
              + " "
              + completeTooltip(tooltip)
              + " "
              + other
              + "></td>\n");

      // new row?
      if (++inRow % perRow == 0 && i != colors.length - 1)
        sb.append("  </tr>\n" + "  <tr>\n" + (hasLabel ? "    <td>&nbsp;</td>\n" : ""));
    }
    sb.append("  </tr>\n" + "</table>\n");
    return sb.toString();
  }

  /**
   * This creates the HTML code for a list in a box or a dropdown list. If nRows &lt; 0, this uses a
   * table to bring the elements close together, and so may need to be in an enclosing table if you
   * want other items on same line.
   *
   * @param name the name of the widget (name=value is returned when the form is submitted)
   * @param tooltip If htmlTooltips is true, this is already html. If it is false, this is plain
   *     text. Or "" if no tooltip.
   * @param nRows &gt; 1 (e.g. 8) to display many options, <br>
   *     1=1 row with no buttons, <br>
   *     nRows is negative implies some combination of BUTTONS_0n, BUTTONS_1, BUTTONS_100,
   *     BUTTONS_1000
   * @param options which provides the plain text to be displayed for each of the options.
   * @param selected the index of the selected item or -1 if none
   * @param other e.g., "onchange=\"pleaseWait();\"" For select(), use onchange, not onclick.
   * @return the HTML code for a list in a box or a dropdown list.
   */
  public String select(
      String name, String tooltip, int nRows, String options[], int selected, String other) {

    return select(name, tooltip, nRows, options, null, selected, other, false, "");
  }

  /**
   * This variant of select deals with a special case where JavaScript code that transfers an option
   * to a text field e.g., document.form1.val0_0.value=this.options[this.selectedIndex].text
   * converts internal &gt;1 space ("ab c") into 1 space ("ab c"). This also solves the problem of
   * leading and trailing spaces being trimmed by the JavaScript code. Set encodeSpaces=true for
   * these special situations. This requires the little change I made in percentEncode above: nbsp
   * (char A0) is now percent encoded as %20 (a space).
   */
  public String select(
      String name,
      String tooltip,
      int nRows,
      String options[],
      int selected,
      String other,
      boolean encodeSpaces) {

    return select(name, tooltip, nRows, options, null, selected, other, encodeSpaces, "");
  }

  /**
   * This variant of select deals with a special case where JavaScript code that transfers an option
   * to a textField e.g., document.form1.val0_0.value=this.options[this.selectedIndex].text converts
   * internal &gt;1 space ("ab c") into 1 space ("ab c"). This also solves the problem of leading
   * and trailing spaces being trimmed by the JavaScript code. Set encodeSpaces=true for these
   * special situations. This requires the little change I made in percentEncode above: nbsp (char
   * A0) is now percent encoded as %20 (a space).
   */
  public String select(
      String name,
      String tooltip,
      int nRows,
      String options[],
      int selected,
      String other,
      boolean encodeSpaces,
      String buttonJS) {

    return select(name, tooltip, nRows, options, null, selected, other, encodeSpaces, buttonJS);
  }

  /**
   * This variant of select adds a values parameter.
   *
   * @param values this should parallel options, or be null (usually).
   * @param buttonJS is addtional javascript to be done when a button is pushed, most commonly "
   *     sel.onchange();" which triggers the select widget's onchange javascript.
   */
  public String select(
      String name,
      String tooltip,
      int nRows,
      String options[],
      String values[],
      int selected,
      String other,
      boolean encodeSpaces,
      String buttonJS) {

    StringBuilder sb = new StringBuilder();
    int nOptions = options.length;
    if (selected < 0 || selected >= nOptions) selected = -1;
    if (nRows > nOptions) nRows = nOptions;

    // if buttons visible, put select and buttons in a table (to pack close together)
    if (nRows < 0 && nOptions > 1) {
      sb.append("<table class=\"compact\">\n");
      sb.append("  <tr><td class=\"B\">"); // td for <select>  //vertical-align: 'b'ottom
    }

    // the main 'select' widget
    sb.append(
        "<select name=\""
            + XML.encodeAsHTMLAttribute(name)
            + "\" size=\""
            + Math.max(1, nRows)
            + "\""
            + completeTooltip(tooltip)
            + " "
            + other
            + " >\n");

    String spacer =
        nOptions < 20 ? "" : ""; // save space if lots of options //was 6 spaces if few options
    for (int i = 0; i < nOptions; i++) {
      // Security issue: the options are not user-specified.  And they are not HTML attributes.
      // I don't think encodeAsHTMLAttributes is warranted and I know it will cause problems with
      // encodeSpaces.
      String opt = XML.encodeAsHTML(options[i]);
      if (encodeSpaces) opt = XML.minimalEncodeSpaces(opt);
      sb.append(
          spacer
              + "<option"
              + (values == null ? "" : " value=\"" + XML.encodeAsHTML(values[i]) + "\"")
              + (i == selected ? " selected=\"selected\"" : "")
              +
              // If option is "", Win IE 7 needs 'value' to be explicitly set
              //  for (name).value to work in JavaScript
              //  and for value to be returned when form is submitted.
              //  I tried 'label' to be used as label, text, and value; didn't work.
              // Leading and trailing spaces are always ignored.
              //  Adding value= attribute doesn't solve the problem
              //  ((options[i].startsWith(" ") || options[i].endsWith(" "))?
              //    " value=\"" + XML.encodeAsHTML(options[i]) + "\"" : "") +
              // 2010-07-28 I changed from .text to .value in javascript [bad idea!].
              // 2010-08-24 I changed back to .text in javascript,
              //  see http://jszen.blogspot.com/2007/01/ie6-select-value-gotcha.html
              //  so I removed
              //  (opt.length() == 0? " value=\"\"" : "") +
              // 2012-04-18 javascript code that transfers an option to a textField
              //  e.g., document.form1.val0_0.value=this.options[this.selectedIndex].text
              //  converts internal >1 space ("ab   c") into 1 space ("ab c").
              ">"
              + opt
              + ("".equals(opt) ? "</option>" : "")
              +
              // </option> is often not used and is not required.
              "\n");
    }
    sb.append("</select>");

    // the buttons
    if (nRows < 0 && nOptions > 1) {
      sb.append("  </td>\n"); // end of <select>'s td

      //        selectButton(name, nRows, test,         nOptions, minNOptions, incr,            img,
      //             tooltip);
      sb.append(
          selectButton(
              name,
              nRows,
              BUTTONS_0n,
              nOptions,
              2,
              Integer.MIN_VALUE,
              "arrowLL.gif",
              "Select the first item.",
              buttonJS));
      sb.append(
          selectButton(
              name,
              nRows,
              BUTTONS_1000,
              nOptions,
              1100,
              -1000,
              "minus1000.gif",
              "Jump back 1000 items.",
              buttonJS));
      sb.append(
          selectButton(
              name,
              nRows,
              BUTTONS_100,
              nOptions,
              110,
              -100,
              "minus100.gif",
              "Jump back 100 items.",
              buttonJS));
      sb.append(
          selectButton(
              name,
              nRows,
              BUTTONS_1,
              nOptions,
              2,
              -1,
              "minus.gif",
              "Select the previous item.",
              buttonJS));
      sb.append(
          selectButton(
              name,
              nRows,
              BUTTONS_1,
              nOptions,
              2,
              1,
              "plus.gif",
              "Select the next item.",
              buttonJS));
      sb.append(
          selectButton(
              name,
              nRows,
              BUTTONS_100,
              nOptions,
              110,
              100,
              "plus100.gif",
              "Jump forward 100 items.",
              buttonJS));
      sb.append(
          selectButton(
              name,
              nRows,
              BUTTONS_1000,
              nOptions,
              1100,
              1000,
              "plus1000.gif",
              "Jump forward 1000 items.",
              buttonJS));
      sb.append(
          selectButton(
              name,
              nRows,
              BUTTONS_0n,
              nOptions,
              2,
              Integer.MAX_VALUE,
              "arrowRR.gif",
              "Select the last item.",
              buttonJS));

      sb.append("\n  </tr>" + "\n</table>\n");
    }
    return sb.toString();
  }

  /**
   * ComboBox lets users choose from drop down list or enter any text. This requires HTML5+. As of
   * 2019-05-10, this doesn't work in Microsoft Edge: it is as if onChange is never called (and the
   * popup disappears). The javascript is like other places. Is it because the list is in a popup?
   *
   * <p>Enter never submits the form. This ignores htmlWidgets.enterSubmitsForm setting.
   *
   * <p>Note current lack of encodeSpace option to deal with multiple adjacent spaces in an option.
   *
   * @param language the index of the selected language
   * @param fieldLength the size of the field, in mspaces(?), e.g., 10. If the fieldLength <= 0, no
   *     'size' value is specified in the html.
   * @param maxLength usually 255
   * @param initialTextValue the initial text value, or null or "" if none
   * @param imgUrl is the URL of the downArrow image
   * @param options One of the options can be "".
   * @param other Other attributes for the textField (usually "")
   * @param onChange the onChange parameter for the options (use null for the standard value).
   */
  public String comboBox(
      int language,
      String formName,
      String name,
      String tooltip,
      int fieldLength,
      int maxLength,
      String initialTextValue,
      String options[],
      String other,
      String onChange) {

    StringBuilder sb = new StringBuilder();
    int nOptions = options.length;

    // the cssTooltip approach
    sb.append("<span class=\"nowrap\">");
    sb.append(textField(name, tooltip, fieldLength, maxLength, initialTextValue, other));
    sb.append(
        cssTooltip(
            "<img "
                + // imgOther + //there could be additional attributes for the image
                " style=\"vertical-align:top;\" "
                + completeTooltip(comboBoxAltAr[language])
                + "\n  src=\""
                + XML.encodeAsHTMLAttribute(imageDirUrl + "arrowD.gif")
                + "\"\n"
                + "  alt=\""
                + XML.encodeAsHTMLAttribute(comboBoxAltAr[language])
                + "\"\n"
                + ">",
            "style=\"padding:0px; max-width:90%; margin-left:-19px;\"",
            select(
                name + "TooltipSelect",
                "",
                Math.min(nOptions, 10),
                options,
                -1,
                // !!! This javascript is identical to other places (except within popup).
                // It works in all browsers except MS Edge (item is selected, but value not copied
                // to 'name' textfield).
                "\n  "
                    + (onChange == null
                        ? "onChange=\"document."
                            + formName
                            + "."
                            + name
                            + ".value=this.options[this.selectedIndex].text; this.selectedIndex=-1;\"\n"
                        : onChange))));
    sb.append("</span>");

    /*
            //the <input> widget and the <datalist> approach
            //The problem is that the list only shows options that match the start of the textfield text,
            //so once you select something, all options go away. (It's the auto-complete feature.)
            //Web has various work-arounds but none are good.
            //see https://www.w3schools.com/tags/tag_datalist.asp
            sb.append(
                "<input type=\"text\" " +
                  "name=\"" + XML.encodeAsHTMLAttribute(name) + "\" " +
                  "list=\"" + XML.encodeAsHTMLAttribute(name) + "_list\" " +
                  completeTooltip(tooltip) +
                  " value=\"" +
                  (initialTextValue == null? "" : XML.encodeAsHTMLAttribute(initialTextValue)) +
                  "\" " +
                  (fieldLength > 0? "size=\"" + fieldLength + "\" " : "") +
                  "maxlength=\"" + maxLength + "\" " +
                  //"onmousedown=\"value='';\" " + //so when user clicks, previous value disappears  NOT GOOD
                  other + " >\n" +  //there is no need for </input> tag
                "<datalist id=\"" + XML.encodeAsHTMLAttribute(name) + "_list\">\n");
            for (int i = 0; i < nOptions; i++) {
                //Security issue: the options are not user-specified.  And they are not HTML attributes.
                //I don't think encodeAsHTMLAttributes is warranted and I know it will cause problems with encodeSpaces.
                //if (encodeSpaces)
                //    opt = XML.minimalEncodeSpaces(opt); //I think this requires a javascript handler to decode the %20's. No javascript here.
                sb.append("<option value=\"" + XML.encodeAsHTML(options[i]) + "\">\n");
            }
            sb.append("</datalist>");
    */

    return sb.toString();
  }

  /**
   * This creates the HTML to make a button to assist select(). This is used by select() to make the
   * e.g., -100, +100 buttons.
   *
   * @param name the name of the select widget
   * @param nRows &gt; 1 (e.g. 8) to display many options, <br>
   *     1=1 row with no buttons, <br>
   *     nRows is negative implies some combination of BUTTONS_0n, BUTTONS_1, BUTTONS_100,
   *     BUTTONS_1000
   * @param test works with nRows to determine if this button should be created
   * @param nOptions select.length
   * @param minNOptions the minimum number of options for this to be shown
   * @param incr the effect of the button, forward or back incr items
   * @param imageName e.g., plus100.gif
   * @param tooltip If htmlTooltips is true, this is already html. If it is false, this is plain
   *     text. Or "" if no tooltip.
   * @param buttonJS additional javascript for the button
   * @return the HTML to make a button to assist select().
   */
  private String selectButton(
      String name,
      int nRows,
      int test,
      int nOptions,
      int minNOptions,
      int incr,
      String imageName,
      String tooltip,
      String buttonJS) {

    StringBuilder sb = new StringBuilder();
    if ((-nRows & -test) != 0 && nOptions >= minNOptions) {
      String rhs = "0";
      if (incr == Integer.MIN_VALUE) {
      } // stay with 0
      else if (incr < 0) rhs = "Math.max(0,sel.selectedIndex-" + -incr + ")";
      else if (incr == Integer.MAX_VALUE) rhs = "sel.length-1";
      else if (incr > 0) rhs = "Math.min(sel.length-1, sel.selectedIndex+" + incr + ")";
      sb.append(
          "<td class=\"B\">"
              + // vertical-align: 'b'ottom
              "<img class=\"B\" src=\""
              + // vertical-align: 'b'ottom
              XML.encodeAsHTMLAttribute(imageDirUrl + imageName)
              + "\" "
              + completeTooltip(tooltip)
              + " alt=\""
              + (nRows == Integer.MIN_VALUE
                  ? "|&lt;"
                  : nRows == Integer.MAX_VALUE ? "&gt;|" : incr >= 0 ? "+" + incr : "" + incr)
              + "\"\n"
              +
              // onMouseUp works much better than onClick and onDblClick
              "  onMouseUp=\"var sel="
              + formName
              + "."
              + name
              + "; sel.selectedIndex="
              + rhs
              + ";"
              + buttonJS
              + " \"\n"
              + "></td>");
    }
    return sb.toString();
  }

  /*    private String selectButton(String name, int nRows, int test,
          int nOptions, int minNOptions, int incr, String imageName, String tooltip) {

          StringBuilder sb = new StringBuilder();
          if ((-nRows & -test) != 0 && nOptions >= minNOptions) {
              String si = formName + "." + name + ".selectedIndex";
              String rhs = "0";
              if (incr == Integer.MIN_VALUE)      {}  //stay 0
              else if (incr < 0)                  rhs  = "Math.max(0," + si + "-" + -incr + ")";
              else if (incr == Integer.MAX_VALUE) rhs  = "" + (nOptions-1);
              else if (incr > 0)                  rhs  = "Math.min(" + (nOptions-1) + "," + si + "+" + incr + ")";
              sb.append(
                  "<td>" +
                  "<img src=\"" + imageDirUrl + imageName + "\"\n" +
                  "  " + completeTooltip(tooltip) +
                  "  alt=\"" +
                      (nRows == Integer.MIN_VALUE? "|&lt;" : nRows == Integer.MAX_VALUE? "&gt;|" : "" + nRows) + "\"" +
                      " onMouseUp=\"" + si + "=" + rhs + ";\" >\n" + //works much better than onClick and onDblClick
                  "</td>\n");
          }
          return sb.toString();
      }
  */
  /**
   * This create the HTML code for a textField. If user pressed Enter, enterTextSubmitsForm
   * determines if the form is submitted.
   *
   * <p>You can make the edges disappear with <tt>style=\"border:0px; background:" + bgColor + "\"
   * </tt>
   *
   * <p>You can make the text bold with <tt>style=\"font-weight:bold;"\" </tt>
   *
   * @param name the name of the widget (name=newValue is returned when the form is submitted)
   * @param tooltip If htmlTooltips is true, this is already html. If it is false, this is plain
   *     text. Or "" if no tooltip.
   * @param fieldLength the size of the field, in mspaces(?), e.g., 10. If the fieldLength <= 0, no
   *     'size' value is specified in the html.
   * @param maxLength usually 255
   * @param initialTextValue the initial text value, or null or "" if none
   * @param other e.g., "onchange=\"pleaseWait();\"" For textField(), use onchange, not onclick.
   * @return the HTML code for a textField.
   */
  public String textField(
      String name,
      String tooltip,
      int fieldLength,
      int maxLength,
      String initialTextValue,
      String other) {

    return "<input type=\"text\" name=\""
        + XML.encodeAsHTMLAttribute(name)
        + "\""
        +
        // An accessibility aid. USGS requested to pass Acunetix scan.
        // But W3C validator says no: alt is for <img>, not <input>.
        // " alt=\"" + XML.encodeAsHTMLAttribute(name) + "\"" +
        " value=\""
        + (initialTextValue == null ? "" : XML.encodeAsHTMLAttribute(initialTextValue))
        + "\""
        +
        // "\n  onkeypress=\"return !enter(event);\"" +
        // supress Enter->submit
        // was the keypress event's keycode 'Enter'?
        // see http://www.mredkj.com/tutorials/validate.html
        (enterTextSubmitsForm
            ? ""
            : "\n  onkeypress='"
                + // supress submission
                " var key = window.event? event.keyCode : event.which? event.which : 0; "
                + // '?' deals with IE vs. Netscape vs. ?
                " return key != 13;' ")
        + completeTooltip(tooltip)
        + "\n  "
        + (fieldLength > 0 ? "size=\"" + fieldLength + "\" " : "")
        + "maxlength=\""
        + maxLength
        + "\""
        + " "
        + other
        + " >";
  }

  /**
   * This creates the html to draw an image (e.g., question mark) that has a big html tooltip.
   * htmlTooltipScript (see above) must be already in the document. See tip().
   *
   * @param imageRef the reference for the question mark image (e.g.,
   *     EDStatic.imageDirUrl(loggedInAs, language) + EDStatic.questionMarkImageFile)
   * @param alt the alt text to be displayed, e.g., "?" (not yet encoded)
   * @param html the html tooltip text, e.g., "Hi,<br>
   *     there!". It needs explicit br tags to set window width correctly. For plain text, use
   *     XML.encodeAsPreHTML(plainText, 82).
   * @param other e.g., "onclick=\"pleaseWait();\""
   * @return the html to draw an image (e.g., question mark) that has a big html tooltip.
   */
  public static String htmlTooltipImage(String imageRef, String alt, String html, String other) {
    return "<img src=\""
        + XML.encodeAsHTMLAttribute(imageRef)
        + "\" alt=\""
        + XML.encodeAsHTMLAttribute(alt)
        + "\""
        + htmlTooltip(html)
        + " "
        + other
        + ">\n";
  }

  /**
   * This creates the onmouseover and onmouseout html to add a big html tooltip to any widget.
   * htmlTooltipScript (see above) must be already in the document.
   *
   * @param html the html tooltip text, e.g., "Hi,<br>
   *     there!". It needs explicit br tags to set window width correctly. For plain text, generate
   *     html from XML.encodeAsPreHTML(plainText, 82).
   * @return the onmouseover and onmouseout html to add a big html tooltip to any widget.
   */
  public static String htmlTooltip(String html) {
    if (html == null || html.length() == 0) return "";

    // example from http://www.walterzorn.com/tooltip/tooltip_e.htm:
    // <a href="index.htm" onmouseover="Tip('Text with <img src=\"pics/image.jpg\"
    // width=\"60\">image.')" onmouseout="UnTip()"> Homepage</a>
    // tooltip = XML.encodeAsHTML(tooltip); //didn't work
    StringBuilder sb = new StringBuilder();
    int n = html.length();
    for (int i = 0; i < n; i++) {
      char ch = html.charAt(i);
      if (ch == '\\') sb.append("\\\\");
      else if (ch == '\"') sb.append("&quot;");
      else if (ch == '\'') sb.append("&#39;");
      else if (ch == '\n') sb.append(' '); // causes problem: quotes not closed at end of line
      else sb.append(ch);
    }
    String2.replaceAll(sb, "&#39;", "\\&#39;");
    String2.replaceAll(sb, "  ", "&nbsp;&nbsp;");
    return " onmouseover=\"Tip('"
        + sb.toString()
        + "')\" onmouseout=\"UnTip()\" "; // with space at beginning and end
  }

  /**
   * This returns the html for a white image which takes up the specified space.
   *
   * @param width in pixels
   * @param height in pixels
   * @param other other html parameters, e.g., now "", was "style=\"text-align:right\""
   */
  public String spacer(int width, int height, String other) {
    StringBuilder sb =
        new StringBuilder(
            "    <img src=\""
                + XML.encodeAsHTMLAttribute(imageDirUrl + "spacer.gif")
                + "\" "
                + "width=\""
                + width
                + "\" height=\""
                + height
                + "\" "
                + other
                + " alt=\"");
    int n = Math.max(1, width / 8);
    for (int i = 0; i < n; i++) sb.append("&nbsp;");
    sb.append("\">\n");
    return sb.toString();
  }

  /**
   * This returns the html for the images which make up a 1-thumb slider. See also sliderScript.
   *
   * <p>The source images must be in imageDirUrl. Their sizes must be as defined by sliderThumbWidth
   * and sliderThumbHeight.
   *
   * @param sliderNumber e.g., 0.. n-1. In the HTML, the images will be named
   *     sliderLeft[sliderNumber], sliderBg[sliderNumber].
   * @param bgWidth the width of the track, in pixels (e.g., 500). All sliders on a form must use
   *     the same bgWidth.
   * @param other e.g., style=\"something\"
   * @return the html to describe a 1-thumb slider.
   */
  public String slider(int sliderNumber, int bgWidth, String other) {
    return // order is important
    "      <img id=\"sliderLeft"
        + sliderNumber
        + "\" src=\""
        + XML.encodeAsHTMLAttribute(imageDirUrl + "sliderCenter.gif")
        + "\" \n"
        + "        width=\""
        + sliderThumbWidth
        + "\" height=\""
        + sliderThumbHeight
        + "\" "
        + other
        + " alt=\""
        + XML.encodeAsHTMLAttribute("<")
        + "\">\n"
        + "      <img id=\"sliderBg"
        + sliderNumber
        + "\" src=\""
        + XML.encodeAsHTMLAttribute(imageDirUrl + "sliderBg.gif")
        + "\" \n"
        + "        width=\""
        + bgWidth
        + "\" height=\""
        + sliderThumbHeight
        + "\" "
        + other
        + " alt=\""
        + XML.encodeAsHTMLAttribute("<")
        + "\">\n";
  }

  /**
   * This returns the html for the images which make up a 2-thumb slider. See also sliderScript.
   *
   * <p>The source images must be in imageDirUrl. sliderLeft.gif and sliderRight.gif must have
   * width=15, height=17. sliderBg.gif must have height = 17.
   *
   * @param sliderNumber e.g., 0.. n-1. In the HTML, the images will be named
   *     sliderLeft[sliderNumber], sliderBg[sliderNumber], sliderRight[sliderNumber].
   * @param bgWidth the width of the track, in pixels (e.g., 500)
   * @param other e.g., was text-align:left; now float:left? nothing?
   * @return the html to describe a 2-thumb slider.
   */
  public String dualSlider(int sliderNumber, int bgWidth, String other) {
    return // order is important
    "      <img id=\"sliderLeft"
        + sliderNumber
        + "\" src=\""
        + XML.encodeAsHTMLAttribute(imageDirUrl + "sliderLeft.gif")
        + "\" \n"
        + "        width=\""
        + sliderThumbWidth
        + "\" height=\""
        + sliderThumbHeight
        + "\" "
        + other
        + " alt=\""
        + XML.encodeAsHTMLAttribute("<")
        + "\">\n"
        + "      <img id=\"sliderBg"
        + sliderNumber
        + "\" src=\""
        + XML.encodeAsHTMLAttribute(imageDirUrl + "sliderBg.gif")
        + "\" \n"
        + "        width=\""
        + bgWidth
        + "\" height=\""
        + sliderThumbHeight
        + "\" "
        + other
        + " alt=\""
        + XML.encodeAsHTMLAttribute("slider")
        + "\">\n"
        + "      <img id=\"sliderRight"
        + sliderNumber
        + "\" src=\""
        + XML.encodeAsHTMLAttribute(imageDirUrl + "sliderRight.gif")
        + "\" \n"
        + "        width=\""
        + sliderThumbWidth
        + "\" height=\""
        + sliderThumbHeight
        + "\" "
        + other
        + " alt=\""
        + XML.encodeAsHTMLAttribute(">")
        + "\">\n";
  }

  /**
   * This returns the html for the javascript which controls a series of sliders. <br>
   * dragDropScript() must be right after the 'body' tag. <br>
   * Use slider() or dualSlider() repeatedly in the form. <br>
   * Put sliderScript() right before the '/body' tag. <br>
   * See documentation in http://www.walterzorn.com/dragdrop/commands_e.htm and
   * http://www.walterzorn.com/dragdrop/api_e.htm .
   *
   * @param fromTextFields the names of all of the 'from' textFields, preceded by their formName +
   *     "." (e.g., "f1.from0", "f1.from1"). 1-thumb sliders connect to this textField only. For no
   *     slider, there should still be a placeholder entry (e.g., "").
   * @param toTextFields the names of all of the 'to' textFields , preceded by their formName + "."
   *     (e.g., "f1.to0", "f1.to1"). For no slider or 1-thumb sliders, there should still be a
   *     placeholder entry (e.g., "").
   * @param nThumbs is the number of thumbs for each slider (0=inactive, 1, or 2)
   * @param userValuesCsvs a csv list of values for each slider. <br>
   *     If a given nThumbs == 0, its csv is irrelevant. <br>
   *     Each list can be any length (at least 1), but only up to bgWidth+1 will be used.
   * @param initFromPositions the initial values for the left (or only) sliders. <br>
   *     If a given slider is inactive, there should still be a placeholder value. <br>
   *     These are usually calculated as Math.roung(index * bgWidth / (nIndices-1.0))
   * @param initToPositions the initial values for the right sliders. <br>
   *     If a given slider is inactive or has 1-thumb, there should still be a placeholder value.
   *     <br>
   *     These are usually calculated as Math.roung(index * bgWidth / (nIndices-1.0))
   * @param bgWidth the width of the track, in pixels (e.g., 500). All sliders on a form must use
   *     the same bgWidth.
   * @return the html for the javascript which controls a series of sliders.
   * @throws RuntimeException if trouble
   */
  public String sliderScript(
      String fromTextFields[],
      String toTextFields[],
      int nThumbs[],
      String userValuesCsvs[],
      int initFromPositions[],
      int initToPositions[],
      int bgWidth) {

    int nSliders = nThumbs.length;
    if (nSliders == 0) return "";
    if (nSliders != fromTextFields.length)
      throw new RuntimeException(
          "nThumbs.length=" + nSliders + " != fromTextFields.length=" + fromTextFields.length);
    if (nSliders != toTextFields.length)
      throw new RuntimeException(
          "nThumbs.length=" + nSliders + " != toTextFields.length=" + toTextFields.length);
    if (nSliders != userValuesCsvs.length)
      throw new RuntimeException(
          "nThumbs.length=" + nSliders + " != userValuesCsvs.length=" + userValuesCsvs.length);
    if (nSliders != initFromPositions.length)
      throw new RuntimeException(
          "nThumbs.length="
              + nSliders
              + " != initFromPositions.length="
              + initFromPositions.length);
    if (nSliders != initToPositions.length)
      throw new RuntimeException(
          "nThumbs.length=" + nSliders + " != initToPositions.length=" + initToPositions.length);
    StringBuilder sb = new StringBuilder();
    sb.append(
        "\n"
            + "<!-- start of sliderScript -->\n"
            + "<script> \n"
            + "<!--\n"
            + "var fromTextFields = [");
    for (int s = 0; s < nSliders; s++)
      sb.append(String2.toJson(fromTextFields[s]) + (s < nSliders - 1 ? ", " : "];\n"));
    sb.append("var toTextFields = [");
    for (int s = 0; s < nSliders; s++)
      sb.append(String2.toJson(toTextFields[s]) + (s < nSliders - 1 ? ", " : "];\n"));
    sb.append("var userValues = [\n");
    for (int s = 0; s < nSliders; s++)
      sb.append("  [" + userValuesCsvs[s] + "]" + (s < nSliders - 1 ? ",\n" : "];\n"));
    sb.append("var initFromPositions = [\n");
    for (int s = 0; s < nSliders; s++)
      sb.append(initFromPositions[s] + (s < nSliders - 1 ? ", " : "];\n"));
    sb.append("var initToPositions = [\n");
    for (int s = 0; s < nSliders; s++)
      sb.append(initToPositions[s] + (s < nSliders - 1 ? ", " : "];\n"));
    sb.append("\n");

    // SET_DHTML
    sb.append("SET_DHTML(CURSOR_DEFAULT, NO_ALT, SCROLL, \n");
    for (int s = 0; s < nSliders; s++) {
      if (nThumbs[s] > 0)
        sb.append(
            "  \"sliderBg"
                + s
                + "\"+NO_DRAG,\n"
                + "  \"sliderLeft"
                + s
                + "\"+HORIZONTAL+MAXOFFLEFT+0,\n");
      if (nThumbs[s] == 2)
        sb.append("  \"sliderRight" + s + "\"+HORIZONTAL+MAXOFFRIGHT+" + bgWidth + ",\n");
    }
    sb.setLength(sb.length() - 2); // remove ,\n
    sb.append(");\n" + "\n" + "var el = dd.elements;\n" + "\n");

    // log
    sb.append("function log(msg) {\n");
    if (debugMode)
      sb.append(
          "  if (typeof(console) != \"undefined\") console.log(msg);\n"); // for debugging only
    sb.append("}\n" + "\n");

    // toUserValue
    sb.append(
        "function toUserValue(which, val) {\n"
            + "  var nuv = userValues[which].length;\n"
            + "  var index = Math.floor((val * nuv) / "
            + bgWidth
            + ");\n"
            + "  return userValues[which][Math.min(Math.max(0, index), (nuv - 1))];\n"
            + "};\n"
            + "\n");

    // updateUI
    sb.append(
        "function updateUI(left, which) {\n"
            + "  log(\"left=\" + left + \" which=\" + which); \n"
            +
            // "  //get the widgets\n" +
            "  var leftS  = eval(\"el.sliderLeft\" + which);  \n"
            + "  var rightS = eval(\"el.sliderRight\" + which);\n"
            + "  if (left) {\n"
            + "    var val = leftS.x - leftS.defx;\n"
            +
            // "    log(\"x=\" + leftS.x + \" val=\" + val);  //for development only\n" +
            "    var fromW  = eval(\"document.\" + fromTextFields[which]);\n"
            + "    fromW.value = toUserValue(which, val);\n"
            + "    if (typeof(rightS) != \"undefined\") rightS.maxoffl = -val;\n"
            + // positive is to the left!
            "  } else {\n"
            + "    var val = rightS.x - rightS.defx;\n"
            +
            // "    log(\"x=\" + rightS.x + \" val=\" + val); \n" +
            "    var toW    = eval(\"document.\" + toTextFields[which]);\n"
            + "    toW.value = toUserValue(which, val);\n"
            + "    if (typeof(leftS) != \"undefined\") leftS.maxoffr = val;\n"
            + "  }\n"
            + "};\n"
            + "\n");

    // my_DragFunc
    sb.append("function my_DragFunc() {\n" + "  try {\n");
    for (int s = 0; s < nSliders; s++) {
      if (nThumbs[s] > 0)
        sb.append("    if (dd.obj.name == 'sliderLeft" + s + "') updateUI(true, " + s + ");\n");
      if (nThumbs[s] > 1)
        sb.append("    if (dd.obj.name == 'sliderRight" + s + "') updateUI(false, " + s + ");\n");
    }
    sb.append(
        "  } catch (ex) {\n"
            + "    "
            + (debugMode ? "alert" : "log")
            + "(ex.toString());\n"
            + "  }\n"
            + "}\n"
            + "\n");

    // initSlider    //do last
    sb.append(
        "function initSlider(which) {\n"
            + "  var bgS    = eval(\"el.sliderBg\" + which);  \n"
            + "  var leftS  = eval(\"el.sliderLeft\" + which);  \n"
            + "  var rightS = eval(\"el.sliderRight\" + which);\n"
            + " \n"
            + "  var oneThumb = typeof(rightS) == \"undefined\";\n"
            + "  leftS.setZ(bgS.z+1); \n"
            + "  bgS.addChild(\"sliderLeft\" + which); \n"
            + "  leftS.defx = bgS.x - Math.round("
            + sliderThumbWidth
            + " / (oneThumb? 2 : 1));\n"
            + "  leftS.moveTo(leftS.defx + initFromPositions[which], bgS.y); \n"
            + "  leftS.maxoffr = initToPositions[which];\n"
            + "\n"
            + "  if (!oneThumb) {\n"
            + "    rightS.setZ(bgS.z+1); \n"
            + "    bgS.addChild(\"sliderRight\" + which); \n"
            + "    rightS.defx = bgS.x;\n"
            + "    rightS.moveTo(rightS.defx + initToPositions[which], bgS.y); \n"
            + "    rightS.maxoffl = -initFromPositions[which];\n"
            + // positive is to the left!
            "  }\n"
            + "}\n"
            + "\n"
            + "try {\n");
    for (int s = 0; s < nSliders; s++) {
      if (nThumbs[s] > 0) sb.append("  initSlider(" + s + ");\n");
    }
    sb.append(
        "} catch (ex) {\n"
            + "  "
            + (debugMode ? "alert" : "log")
            + "(ex.toString());\n"
            + "}\n"
            + "\n"
            + "//-->\n"
            + "</script>\n"
            + "<!-- end of sliderScript -->\n"
            + "\n");
    return sb.toString();
  }

  /**
   * A common use of twoClickMap to show a lon=-180 to 540 map.
   *
   * @param language the index of the selected language
   */
  public static String[] myTwoClickMap540Big(
      int language, String formName, String imageUrl, boolean debugInBrowser) {
    return twoClickMap(
        language,
        formName,
        "minLon",
        "maxLon",
        "minLat",
        "maxLat",
        imageUrl,
        785,
        278, // 2019-10-18 was 412, 155,  //image w, h
        21,
        9,
        748,
        251, // 2019-10-18 was 17, 12, 381, 128,  //map left, top, width, height  (via
        // subtraction+1)
        540,
        -180,
        180,
        90, // lonRange, lonMin, latRange, latMax
        null,
        debugInBrowser);
  }

  /**
   * A common use of twoClickMap to show a lon=-180 to 180 map.
   *
   * @param language the index of the selected language
   */
  public static String[] myTwoClickMap180Big(
      int language, String formName, String imageUrl, boolean debugInBrowser) {
    return twoClickMap(
        language,
        formName,
        "minLon",
        "maxLon",
        "minLat",
        "maxLat",
        imageUrl,
        285,
        155,
        17,
        12,
        254,
        128, // map left, top, width, height  (via subtraction+1)
        360,
        -180,
        180,
        90,
        null,
        debugInBrowser);
  }

  /**
   * A common use of twoClickMap to show a lon=0 to 360 map.
   *
   * @param language the index of the selected language
   */
  public static String[] myTwoClickMap360Big(
      int language, String formName, String imageUrl, boolean debugInBrowser) {
    return twoClickMap(
        language,
        formName,
        "minLon",
        "maxLon",
        "minLat",
        "maxLat",
        imageUrl,
        284,
        155,
        16,
        12,
        254,
        128, // map left, top, width, height  (via subtraction+1)
        360,
        0,
        180,
        90,
        null,
        debugInBrowser);
  }

  /**
   * A common use of twoClickMap to show a lon=-180 to 540 map.
   *
   * @param language the index of the selected language
   */
  public static String[] myTwoClickMap540(
      int language, String formName, String imageUrl, boolean debugInBrowser) {
    return twoClickMap(
        language,
        formName,
        "minLon",
        "maxLon",
        "minLat",
        "maxLat",
        imageUrl,
        293,
        113,
        18,
        10,
        261,
        87, // map left, top, width, height  (via subtraction+1)
        540,
        -180,
        180,
        90,
        null,
        debugInBrowser);
  }

  /** A common use of twoClickMap to show a lon=-180 to 180 map. */
  public static String[] myTwoClickMap180(
      int language, String formName, String imageUrl, boolean debugInBrowser) {
    return twoClickMap(
        language,
        formName,
        "minLon",
        "maxLon",
        "minLat",
        "maxLat",
        imageUrl,
        205,
        113,
        18,
        10,
        174,
        87, // map left, top, width, height  (via subtraction+1)
        360,
        -180,
        180,
        90,
        null,
        debugInBrowser);
  }

  /** A common use of twoClickMap to show a lon=0 to 360 map. */
  public static String[] myTwoClickMap360(
      int language, String formName, String imageUrl, boolean debugInBrowser) {
    return twoClickMap(
        language,
        formName,
        "minLon",
        "maxLon",
        "minLat",
        "maxLat",
        imageUrl,
        205,
        113,
        18,
        10,
        174,
        87, // map left, top, width, height  (via subtraction+1)
        360,
        0,
        180,
        90, // lonRange, lonMin, latRange, latMax
        null,
        debugInBrowser);
  }

  /**
   * This generates the HTML and JavaScript to show a (world) map on image which the user can click
   * on twice to specify a rectangle, the coordinates of which will be put in 4 textFields (rounded
   * to nearest int).
   *
   * <p>There can be only one twoClickMap per web page.
   *
   * @param language the index of the selected language
   * @param formName e.g., f1
   * @param minLonTF the minLon textFieldName, e.g., minLon
   * @param maxLonTF the maxLon textFieldName
   * @param minLatTF the minLat textFieldName
   * @param maxLatTF the maxLat textFieldName
   * @param imageUrl
   * @param imageWidth
   * @param imageHeight
   * @param mapX theLeftPixelNumber of the map in the image
   * @param mapY theTopPixelNumber of the map in the image
   * @param mapWidth theRightPixel# - theLeftPixelNumber
   * @param mapHeight theBottomPixel# - theTopPixelNumber
   * @param lonRange e.g., 360 or 540
   * @param lonMin e.g., -180 or 0
   * @param latRange usually 180
   * @param latMax usually 90
   * @param tooltip use null for the default tooltip
   * @param debugInBrowser if true, debug code (using console.log() ) will be generated
   * @return [0] is the string to be put in the form (rowSpan=5) [1] is the string with the map
   *     (usually right after [0]) and [2] is the string to be put after the end of the form.
   */
  public static String[] twoClickMap(
      int language,
      String formName,
      String minLonTF,
      String maxLonTF,
      String minLatTF,
      String maxLatTF,
      String imageUrl,
      int imageWidth,
      int imageHeight,
      int mapX,
      int mapY,
      int mapWidth,
      int mapHeight,
      int lonRange,
      int lonMin,
      int latRange,
      int latMax,
      String tooltip,
      boolean debugInBrowser) {

    if (tooltip == null) tooltip = twoClickMapDefaultTooltipAr[language];

    StringBuilder sb0 = new StringBuilder();
    sb0.append(
        "\n"
            + "<!-- start of twoClickMap[0] -->\n"
            + "<img \n"
            + "   id=\"worldImage\" src=\""
            + XML.encodeAsHTMLAttribute(imageUrl)
            + "\" usemap=\"#worldImageMap\"\n"
            + "    alt=\"worldImage\"\n"
            + "  title=\""
            + tooltip
            + "\" \n"
            + "  style=\"cursor:crosshair; vertical-align:middle; border:0;\">\n"
            + "<!-- end of twoClickMap[0] -->\n");

    StringBuilder sb1 = new StringBuilder();
    sb1.append(
        "<!-- start of twoClickMap[1] -->\n"
            + "<map name=\"worldImageMap\">\n"
            + "  <area href=\""
            + JAVASCRIPT_VOID
            + "\" shape=RECT coords=\"0,0,"
            + (imageWidth - 1)
            + ","
            + (imageHeight - 1)
            + "\" style=\"cursor:crosshair;\"\n"
            + "    alt=\""
            + tooltip
            + "\"\n"
            + "  title=\""
            + tooltip
            + "\" \n"
            + "        onClick=\"return rubber(true, event)\" \n"
            + "    onMouseMove=\"return rubber(false, event)\" > \n"
            + "</map>\n"
            + "<!-- end of twoClickMap[1] -->\n");

    StringBuilder sb2 = new StringBuilder();
    sb2.append(
        "\n"
            + "<!-- start of twoClickMap[2] -->\n"
            + "<div id=\"rubberBand\" style=\"position:absolute; visibility:hidden; width:0px; height:0px; "
            + "font-size:1px; line-height:0; border:1px solid red; cursor:crosshair;\" \n"
            + "      onClick=\"return rubber(true, event)\" \n"
            + "  onMouseMove=\"return rubber(false, event)\" ></div>\n"
            + "\n"
            + "<script>\n"
            + "<!--\n"
            + "var tcNextI = 0;\n"
            + "var tcCx = new Array(0,0);\n"
            + "var tcCy = new Array(0,0);\n"
            + "\n");

    sb2.append(
        // basically, this finds the offsetXY of an element by adding up the offsets of all parent
        // elements
        // was "//findPos from http://blog.firetree.net/2005/07/04/javascript-find-position/\n" +
        // but that's gone
        // https://www.chestysoft.com/imagefile/javascript/get-coordinates.asp
        "function findPosXY(obj) {\n"
            + "  var curleft = 0;\n"
            + "  var curtop = 0;\n"
            + "  if(obj.offsetParent) {\n"
            + "    for (var i = 0; i < 20; i++) {\n"
            + // a 'while' loop is technically better but risks an infinite loop
            "      curleft += obj.offsetLeft;\n"
            + "      curtop  += obj.offsetTop;\n"
            + "      if(!obj.offsetParent)\n"
            + "        break;\n"
            + "      obj = obj.offsetParent;\n"
            + "    }\n"
            + "  } else {\n"
            + "    if(obj.x) curleft = obj.x;\n"
            + "    if(obj.y) curtop  = obj.y;\n"
            + "  }"
            + "  return [curleft, curtop];\n"
            + "}\n"
            + "\n"
            + "function rubber(clicked, evt) {\n"
            + (debugInBrowser ? "  console.log(\"click=\" + clicked);\n" : "")
            + "  if (!clicked && tcNextI == 0)\n"
            + "    return true;\n"
            + "  var cx = tcCx;\n"
            + // local cx should now point to global tcCx array
            "  var cy = tcCy;\n"
            + "  var IE = document.all? true: false;\n"
            + "  var im = IE? document.all.worldImage : document.getElementById('worldImage');\n"
            + "  var tx=-1, ty=-1;\n"
            + "  if (IE) {\n"
            + "    tx = document.body.scrollLeft + evt.x;\n"
            + "    ty = document.body.scrollTop  + evt.y;\n"
            + "  } else if (evt.pageX) {\n"
            + "    tx = evt.pageX;\n"
            + "    ty = evt.pageY;\n"
            + "  } else return true;\n"
            + "  var posxy = findPosXY(im);\n"
            + // upper left of image
            "  var imx = posxy[0];\n"
            + "  var imy = posxy[1];\n"
            + "  tx = Math.max(tx, imx + "
            + mapX
            + ");\n"
            + // constrain tx,ty to be in map (in image)
            "  tx = Math.min(tx, imx + "
            + mapX
            + " + "
            + (mapWidth - 1)
            + ");\n"
            + "  ty = Math.max(ty, imy + "
            + mapY
            + ");\n"
            + "  ty = Math.min(ty, imy + "
            + mapY
            + " + "
            + (mapHeight - 1)
            + ")\n"
            + "  cx[tcNextI] = tx;\n"
            + "  cy[tcNextI] = ty;\n"
            + (debugInBrowser
                ? "  console.log(\"  \" + (IE?\"\":\"non-\") + \"IE: tcNextI=\" + tcNextI + \" cx=\" + tx + \" cy=\" + ty);\n"
                : "")
            + "  if (clicked) {\n"
            + "    tcNextI = tcNextI == 0? 1 : 0;\n"
            + "    if (tcNextI == 1) {\n"
            + "      cx[1] = cx[0];\n"
            + "      cy[1] = cy[0];\n"
            + "    }\n"
            + "  }\n"
            + "\n"
            + "  updateRubber();\n"
            + "  document."
            + formName
            + "."
            + minLonTF
            + ".value = Math.round(((Math.min(cx[0], cx[1]) - (imx + "
            + mapX
            + ")) / "
            + (mapWidth - 1)
            + ") * "
            + lonRange
            + " + "
            + lonMin
            + ");\n"
            + "  document."
            + formName
            + "."
            + maxLonTF
            + ".value = Math.round(((Math.max(cx[0], cx[1]) - (imx + "
            + mapX
            + ")) / "
            + (mapWidth - 1)
            + ") * "
            + lonRange
            + " + "
            + lonMin
            + ");\n"
            + "  document."
            + formName
            + "."
            + minLatTF
            + ".value = Math.round(((Math.max(cy[0], cy[1]) - (imy + "
            + mapY
            + ")) / "
            + (mapHeight - 1)
            + ") * "
            + -latRange
            + " + "
            + latMax
            + ");\n"
            + "  document."
            + formName
            + "."
            + maxLatTF
            + ".value = Math.round(((Math.min(cy[0], cy[1]) - (imy + "
            + mapY
            + ")) / "
            + (mapHeight - 1)
            + ") * "
            + -latRange
            + " + "
            + latMax
            + ");\n"
            + (debugInBrowser ? "  console.log(\"  done\");\n" : "")
            + "  return true;\n"
            + "}\n"
            + "\n"
            + "function updateRubber() {\n"
            + "  var rb = (document.all)? document.all.rubberBand : document.getElementById('rubberBand');\n"
            + "  var rbs = rb.style;\n"
            + "  rbs.left = Math.min(tcCx[0], tcCx[1]) + 'px';\n"
            + "  rbs.top  = Math.min(tcCy[0], tcCy[1]) + 'px';\n"
            + "  rbs.width  = Math.max(0, Math.abs(tcCx[1] - tcCx[0]) - 1) + 'px';\n"
            + "  rbs.height = Math.max(0, Math.abs(tcCy[1] - tcCy[0]) - 1) + 'px';\n"
            + "  rbs.visibility = 'visible';\n"
            + "}\n"
            + "\n"
            + "function initRubber() {\n"
            + "  var initLon0 = parseFloat(document."
            + formName
            + "."
            + minLonTF
            + ".value);\n"
            + "  var initLon1 = parseFloat(document."
            + formName
            + "."
            + maxLonTF
            + ".value);\n"
            + "  var initLat0 = parseFloat(document."
            + formName
            + "."
            + minLatTF
            + ".value);\n"
            + "  var initLat1 = parseFloat(document."
            + formName
            + "."
            + maxLatTF
            + ".value);\n"
            + "  if (isFinite(initLon0) && isFinite(initLon1) &&\n"
            + "      isFinite(initLat0) && isFinite(initLat1)) {\n"
            + (debugInBrowser ? "    console.log(\"initRubber\");\n" : "")
            + "    var im = document.all? document.all.worldImage : document.getElementById('worldImage');\n"
            + "    var posxy = findPosXY(im);\n"
            + // upper left of image
            "    var imx = posxy[0];\n"
            + "    var imy = posxy[1];\n"
            + "    var lon0 = Math.min(1, Math.max(0, (initLon0 - "
            + lonMin
            + ") / "
            + lonRange
            + "));\n"
            + "    var lon1 = Math.min(1, Math.max(0, (initLon1 - "
            + lonMin
            + ") / "
            + lonRange
            + "));\n"
            + "    var lat0 = Math.min(1, Math.max(0, (initLat0 - "
            + latMax
            + ") / "
            + -latRange
            + "));\n"
            + "    var lat1 = Math.min(1, Math.max(0, (initLat1 - "
            + latMax
            + ") / "
            + -latRange
            + "));\n"
            + "    tcCx[0] = Math.round(lon0 * "
            + (mapWidth - 1)
            + " + "
            + mapX
            + " + imx);\n"
            + "    tcCx[1] = Math.round(lon1 * "
            + (mapWidth - 1)
            + " + "
            + mapX
            + " + imx);\n"
            + "    tcCy[0] = Math.round(lat0 * "
            + (mapHeight - 1)
            + " + "
            + mapY
            + " + imy);\n"
            + "    tcCy[1] = Math.round(lat1 * "
            + (mapHeight - 1)
            + " + "
            + mapY
            + " + imy);\n"
            + "    updateRubber();\n"
            + (debugInBrowser
                ? "    console.log(\"  init cx=\" + tcCx[0] + \", \" + tcCx[1] + \" cy=\" + tcCy[0] + \", \" + tcCy[1]);\n"
                : "")
            + "  }\n"
            + "}\n"
            + "\n"
            + "try {\n"
            + "  initRubber(); \n"
            + "} catch (ex) {\n"
            + (debugInBrowser ? "  console.log('initRubber exception: ' + ex.toString());\n" : "")
            + "}\n"
            + "--> \n"
            + "</script>\n"
            + "<!-- end of twoClickMap[2] -->\n"
            + "\n");

    return new String[] {sb0.toString(), sb1.toString(), sb2.toString()};
  }

  /* *  NOT FINISHED
       * This looks for cross site scripting attempts
       * (e.g., &lt;script&gt; &lt;object&gt; &lt;applet&gt; &lt;embed&gt; &lt;form&gt;
       * &lt;img&gt; &lt;iframe&gt; &lt;/a&gt; &lt;&gt;
       * in textField input or RESTful parameter value from a user.
       * See http://www.technicalinfo.net/papers/CSS.html
       *
       * @param text the text received from a user (usually via a textField or REST parameter).
       * @return either the original text (if no sign of trouble) or "[]" if there was any sign or trouble.
       *   null returns null.
       * /
      public static String cleanInput(String text) {
          if (text == null)
              return text;
          String s = text.toLowerCase();
          int sLength = s.length();
          StringBuilder sb = new StringBuilder(sLength);
          for (int i = 0; i< sLength; i++) {
              char c = s.charAt(i);
              if (String2.isWhite(c))
                  continue;
  */

  /**
   * This ensure that the string value isn't null and isn't too long.
   *
   * @param language the index of the selected language
   * @param cumulativeHtmlErrorMsg (may be null)
   * @return the corrected value (default (if it was null), previous value.trim(), or previous value
   *     shortened to maxLength)
   */
  public static String validateNotNullNotTooLong(
      int language,
      String name,
      String defaultValue,
      String value,
      int maxLength,
      StringBuilder cumulativeHtmlErrorMsg) {

    if (value == null) {
      if (cumulativeHtmlErrorMsg != null)
        cumulativeHtmlErrorMsg.append(
            "<br>&bull; "
                + MessageFormat.format(errorXWasntSpecifiedAr[language], XML.encodeAsHTML(name))
                + "\n");
      return defaultValue;
    } else {
      value = value.trim();
    }
    if (value.length() > maxLength) {
      if (cumulativeHtmlErrorMsg != null)
        cumulativeHtmlErrorMsg.append(
            "<br>&bull; "
                + MessageFormat.format(
                    errorXWasTooLongAr[language], XML.encodeAsHTML(name), "" + maxLength)
                + "\n");
      return value.substring(0, maxLength);
    }
    return value;
  }

  /**
   * This ensure that the string value isSomething and isn't too long.
   *
   * @param language the index of the selected language
   * @param cumulativeHtmlErrorMsg (may be null)
   * @return the corrected value (default (if value was null or ""), previous value.trim(), or
   *     previous value shortened to maxLength)
   */
  public static String validateIsSomethingNotTooLong(
      int language,
      String name,
      String defaultValue,
      String value,
      int maxLength,
      StringBuilder cumulativeHtmlErrorMsg) {

    if (!String2.isSomething(value)) {
      if (cumulativeHtmlErrorMsg != null)
        cumulativeHtmlErrorMsg.append(
            "<br>&bull; "
                + MessageFormat.format(errorXWasntSpecifiedAr[language], XML.encodeAsHTML(name))
                + "\n");
      return defaultValue;
    } else {
      value = value.trim();
    }
    if (value.length() > maxLength) {
      if (cumulativeHtmlErrorMsg != null)
        cumulativeHtmlErrorMsg.append(
            "<br>&bull; "
                + MessageFormat.format(
                    errorXWasTooLongAr[language], XML.encodeAsHTML(name), "" + maxLength)
                + "\n");
      return value.substring(0, maxLength);
    }
    return value;
  }

  /**
   * This ensure that the string value isn't too long.
   *
   * @param language the index of the selected language
   * @param cumulativeHtmlErrorMsg (may be null)
   * @return the corrected value (default (if value was null), previous value.trim(), or previous
   *     value shortened to maxLength)
   */
  public static String validateNotTooLong(
      int language,
      String name,
      String defaultValue,
      String value,
      int maxLength,
      StringBuilder cumulativeHtmlErrorMsg) {

    if (value == null) value = defaultValue;
    else value = value.trim();
    if (value.length() > maxLength) {
      if (cumulativeHtmlErrorMsg != null)
        cumulativeHtmlErrorMsg.append(
            "<br>&bull; "
                + MessageFormat.format(
                    errorXWasTooLongAr[language], XML.encodeAsHTML(name), "" + maxLength)
                + "\n");
      return value.substring(0, maxLength);
    }
    return value;
  }

  /**
   * This ensure that value is &gt;=0 and &lt;=maxValue.
   *
   * @param language the index of the selected language
   * @param cumulativeHtmlErrorMsg (may be null)
   * @return the corrected value (default (if it was null), previous value, or previous value
   *     shortened to maxLength)
   */
  public static int validate0ToMax(
      int language,
      String name,
      int defaultValue,
      int value,
      int max,
      StringBuilder cumulativeHtmlErrorMsg) {

    if (value < 0 || value > max) {
      if (cumulativeHtmlErrorMsg != null)
        cumulativeHtmlErrorMsg.append(
            "<br>&bull; "
                + MessageFormat.format(errorXWasntSpecifiedAr[language], XML.encodeAsHTML(name))
                + "\n");
      return defaultValue;
    }
    return value;
  }
}
