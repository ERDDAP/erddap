package gov.noaa.pfel.coastwatch.sgt;

import gov.noaa.pmel.util.Range2D;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.TreeSet;

public class HtmlColorMapRenderer {
  // Helper to convert java.awt.Color to "rgb(r,g,b)" string
  private static String toHtmlColor(Color color) {
    if (color == null) {
      return "rgb(128,128,128)"; // Default to gray if color is null
    }
    return String.format("rgb(%d,%d,%d)", color.getRed(), color.getGreen(), color.getBlue());
  }

  // Helper to clamp a double value within a min/max range
  private static double clamp(double val, double min, double max) {
    return Math.max(min, Math.min(val, max));
  }

  // Helper to format double values for tick labels
  private static String formatDouble(double d) {
    // Avoid scientific notation for common ranges, show some decimal places
    if (Double.isNaN(d) || Double.isInfinite(d)) {
      return String.valueOf(d);
    }
    DecimalFormat df;
    if (Math.abs(d) >= 10000 || (Math.abs(d) < 0.001 && d != 0)) {
      df = new DecimalFormat("0.###E0"); // Scientific notation for very large/small
    } else if (Math.abs(d) >= 100) {
      df = new DecimalFormat("#,##0"); // Integer for large numbers
    } else if (Math.abs(d) >= 1) {
      df = new DecimalFormat("#,##0.##"); // Up to 2 decimal places
    } else {
      df = new DecimalFormat("0.###"); // More precision for small numbers
    }
    return df.format(d);
  }

  /**
   * Renders the given CompoundColorMap as an HTML string. The HTML includes a color bar, ticks, and
   * labels.
   *
   * @param colorMap The CompoundColorMap to render.
   * @param uniqueId A unique string to make CSS IDs unique if multiple maps are on a page.
   * @return An HTML string representing the color map.
   */
  public static String renderToHtml(CompoundColorMap colorMap, String uniqueId) {
    StringBuilder html = new StringBuilder();

    Range2D colorRange = colorMap.getRange();
    double minPreference = colorRange.start;
    double maxPreference = colorRange.end;

    String containerId = "colormap-container-" + uniqueId;

    html.append(String.format("<div id='%s' class='compound-color-map-container'>\n", containerId));

    // Inline CSS for styling. Scoped using the containerId.
    html.append("<style>\n");
    html.append(
        String.format(
            "#%s { position: relative; width: 100%%; margin-bottom: 35px; font-family: Arial, sans-serif; }\n",
            containerId));
    html.append(
        String.format(
            "#%s .color-bar { width: 100%%; height: 30px; border: 1px solid #777; box-sizing: border-box; }\n",
            containerId));
    html.append(
        String.format(
            "#%s .ticks { position: relative; width: 100%%; height: 25px; margin-top: 4px; }\n",
            containerId));
    html.append(
        String.format(
            "#%s .tick { position: absolute; bottom: 15px; width: 1px; height: 8px; background-color: #333; }\n",
            containerId));
    html.append(
        String.format(
            "#%s .tick-label { position: absolute; top: 10px; /* Adjusted for label below tick line */ font-size: 10px; white-space: nowrap; color: #333; }\n",
            containerId));
    html.append("</style>\n");

    double displayRange = maxPreference - minPreference;
    StringBuilder gradientCss = new StringBuilder();

    if (displayRange <= 0) { // Handle cases with zero or negative range (single color)
      Color solidColor = colorMap.getColor(minPreference);
      String htmlSolidColor = toHtmlColor(solidColor);
      gradientCss.append(
          String.format(
              "linear-gradient(to right, %s 0%%, %s 100%%)", htmlSolidColor, htmlSolidColor));
    } else {
      gradientCss.append("linear-gradient(to right");

      for (int i = 0; i < colorMap.rangeLow.length; i++) {
        double segmentStartVal = colorMap.rangeLow[i];
        double segmentEndVal = colorMap.rangeHigh[i];

        if (segmentStartVal >= segmentEndVal) { // Skip zero-width or invalid segments
          continue;
        }
        double startPercent = ((segmentStartVal - minPreference) / displayRange) * 100.0;
        double endPercent = ((segmentEndVal - minPreference) / displayRange) * 100.0;

        startPercent = clamp(startPercent, 0, 100);
        endPercent = clamp(endPercent, 0, 100);

        // Ensure segment has actual width on the bar after clamping
        if (Math.abs(startPercent - endPercent) < 0.0001
            && !(startPercent == 0 && endPercent == 0)
            && !(startPercent == 100 && endPercent == 100)) {
          continue;
        }
        if (colorMap.continuous) {
          // Determine color for this segment (e.g., by checking midpoint)
          String htmlSegmentStartColor = toHtmlColor(colorMap.getColor(segmentStartVal));
          String htmlSegmentMidColor =
              toHtmlColor(colorMap.getColor((segmentStartVal + segmentEndVal) / 2.0));
          String htmlSegmentEndColor = toHtmlColor(colorMap.getColor(segmentEndVal));
          // CSS linear-gradient syntax for a solid color band from startPercent to endPercent
          gradientCss.append(
              String.format(
                  Locale.US,
                  ", %s %.4f%%, %s %.4f%%, %s %.4f%%",
                  htmlSegmentStartColor,
                  startPercent,
                  htmlSegmentMidColor,
                  (endPercent + startPercent) / 2.0,
                  htmlSegmentEndColor,
                  endPercent));
        } else {
          // Determine color for this segment (e.g., by checking midpoint)
          double midPoint = (segmentEndVal + segmentStartVal) / 2.0;
          Color segmentColor = colorMap.getColor(midPoint);
          String stringColor = toHtmlColor(segmentColor);
          // CSS linear-gradient syntax for a solid color band from startPercent to endPercent
          gradientCss.append(
              String.format(
                  Locale.US,
                  ", %s %.4f%%, %s %.4f%%",
                  stringColor,
                  startPercent,
                  stringColor,
                  endPercent));
        }
      }
      gradientCss.append(")");
    }

    html.append(
        String.format(
            "<div class='color-bar' style='background: %s;'></div>\n", gradientCss.toString()));

    // Add Ticks and Labels
    html.append("<div class='ticks'>\n");
    TreeSet<Double> tickDisplayValues = new TreeSet<>();
    tickDisplayValues.add(minPreference);
    tickDisplayValues.add(maxPreference);
    for (double value : colorMap.rangeLow) {
      if (value >= minPreference && value <= maxPreference) {
        tickDisplayValues.add(value);
      }
    }

    for (double tickVal : tickDisplayValues) {
      double tickPercentPosition = 0;
      if (displayRange > 0) {
        tickPercentPosition = ((tickVal - minPreference) / displayRange) * 100.0;
      } else { // If range is 0, position depends on relation to minPreference
        if (tickVal == minPreference) tickPercentPosition = 0;
        else if (tickVal > minPreference) tickPercentPosition = 100;
        // else tickVal < minPreference (should be filtered out), position 0
      }
      tickPercentPosition = clamp(tickPercentPosition, 0, 100);

      String labelAlignmentStyle;
      if (Math.abs(tickPercentPosition - 100.0) < 0.01) { // Tick is at 100%
        labelAlignmentStyle = "transform: translateX(-100%); text-align: right;";
      } else if (Math.abs(tickPercentPosition - 0.0) < 0.01
          && tickDisplayValues.size() > 1) { // Tick is at 0%
        labelAlignmentStyle = "transform: translateX(0%); text-align: left;";
      } else { // Default centered transform
        labelAlignmentStyle = "transform: translateX(-50%); text-align: center;";
      }

      html.append(
          String.format(
              Locale.US,
              "<div class='tick' style='left: %.4f%%;'><span class='tick-label' style='%s'>%s</span></div>\n",
              tickPercentPosition,
              labelAlignmentStyle,
              formatDouble(tickVal)));
    }
    html.append("</div>\n"); // End .ticks

    html.append("</div>\n"); // End .compound-color-map-container
    return html.toString();
  }
}
