@import gov.noaa.pfel.coastwatch.util.HtmlWidgets
@import gov.noaa.pfel.erddap.util.EDStatic
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import jakarta.servlet.http.HttpServletRequest
@import java.text.MessageFormat
@param HtmlWidgets widgets
@param HttpServletRequest request
@param int language
@param String loggedInAs
@param String tErddapUrl
@param String formName
@param String page
@param String itemsPerPage
@param String searchFor
@param String[] protocols
@param int whichProtocol
@param String[] catAttsInURLs
@param String[][] catSAs
@param int[] whichCatSAIndex
@param String minLon
@param String maxLon
@param String minLat
@param String maxLat
@param String[] twoClickMap
@param String minTime
@param String maxTime
  $unsafe{EDStatic.messages.get(Message.ADVANCED_SEARCH_DIRECTIONS, language)}
    $unsafe{HtmlWidgets.ifJavaScriptDisabled}
    $unsafe{widgets.beginForm(formName, "GET", tErddapUrl + "/search/advanced.html", "")}

    $unsafe{widgets.hidden("page", page)}
    $unsafe{widgets.hidden("itemsPerPage", itemsPerPage)}

    <p><strong>${EDStatic.messages.get(Message.SEARCH_FULL_TEXT_HTML, language)}</strong>
    $unsafe{EDStatic.htmlTooltipImage(request, language, loggedInAs, EDStatic.messages.get(Message.SEARCH_HINTS_TOOLTIP, language))}
    <br>
    $unsafe{widgets.textField("searchFor", MessageFormat.format(EDStatic.messages.get(Message.SEARCH_TIP, language), "noaa wind"), 70, 255, searchFor, "")}
    </p>

    &nbsp;
    $unsafe{widgets.beginTable("class=\"compact nowrap\"")}
    <tr>
      <td colspan="2"><strong>${EDStatic.messages.get(Message.CATEGORY_TITLE_HTML, language)}</strong>
      $unsafe{EDStatic.htmlTooltipImage(request, language, loggedInAs, "<" + "div class=\"narrow_max_width\">" + EDStatic.messages.get(Message.ADVANCED_SEARCH_CATEGORY_TOOLTIP, language) + "</" + "div>")}
      </td>
    </tr>
    <tr>
      <td class="N" style="width:20%;">protocol
      $unsafe{EDStatic.htmlTooltipImage(request, language, loggedInAs, "<" + "div class=\"standard_max_width\">protocol tooltip</" + "div>")}
      </td>
      <td style="width:80%;">&nbsp;=&nbsp;
      $unsafe{widgets.select("protocol", "", 1, protocols, whichProtocol, "")}
      </td>
    </tr>

    @for(int ca=0; ca<catAttsInURLs.length; ca++)
      @if(catSAs[ca].length > 1)
        <tr>
          <td class="N">${catAttsInURLs[ca]}</td>
          <td>&nbsp;=&nbsp;$unsafe{widgets.select(catAttsInURLs[ca], "", 1, catSAs[ca], whichCatSAIndex[ca], "")}</td>
        </tr>
      @endif
    @endfor

    <tr><td colspan="2">&nbsp;</td></tr>
    <tr>
      <td colspan="2"><strong>${EDStatic.messages.get(Message.ADVANCED_SEARCH_BOUNDS, language)}</strong>
      $unsafe{EDStatic.htmlTooltipImage(request, language, loggedInAs, "<" + "div class=\"standard_max_width\">" + EDStatic.messages.get(Message.ADVANCED_SEARCH_RANGE_TOOLTIP, language) + "</" + "div>")}
      </td>
    </tr>

    <tr>
      <td class="N">${EDStatic.messages.get(Message.ADVANCED_SEARCH_MAX_LAT, language)}</td>
      <td>&nbsp;=&nbsp;$unsafe{widgets.textField("maxLat", EDStatic.messages.get(Message.ADVANCED_SEARCH_MAX_LAT, language) + " (-90 to 90)" + "<" + "p>" + EDStatic.messages.get(Message.ADVANCED_SEARCH_MAP_TOOLTIP, language), 8, 12, maxLat, "")}</td>
    </tr>

    <tr>
      <td class="N">${EDStatic.messages.get(Message.ADVANCED_SEARCH_MIN_MAX_LON, language)}</td>
      <td>&nbsp;=&nbsp;$unsafe{widgets.textField("minLon", "<" + "div class=\"standard_max_width\">" + EDStatic.messages.get(Message.ADVANCED_SEARCH_MIN_LON, language) + "<" + "p>" + EDStatic.messages.get(Message.ADVANCED_SEARCH_MAP_TOOLTIP, language) + "</" + "div>", 8, 12, minLon, "")}
      $unsafe{widgets.textField("maxLon", "<" + "div class=\"standard_max_width\">" + EDStatic.messages.get(Message.ADVANCED_SEARCH_MAX_LON, language) + "<" + "p>" + EDStatic.messages.get(Message.ADVANCED_SEARCH_MAP_TOOLTIP, language) + "</" + "div>", 8, 12, maxLon, "")}
      </td>
    </tr>

    <tr>
      <td class="N">${EDStatic.messages.get(Message.ADVANCED_SEARCH_MIN_LAT, language)}</td>
      <td>&nbsp;=&nbsp;$unsafe{widgets.textField("minLat", EDStatic.messages.get(Message.ADVANCED_SEARCH_MIN_LAT, language) + " (-90 to 90)" + "<" + "p>" + EDStatic.messages.get(Message.ADVANCED_SEARCH_MAP_TOOLTIP, language), 8, 12, minLat, "")}
      $unsafe{widgets.htmlButton("button", "", "", EDStatic.messages.get(Message.ADVANCED_SEARCH_CLEAR_HELP, language), EDStatic.messages.get(Message.ADVANCED_SEARCH_CLEAR, language), "onClick='f1.minLon.value=\"\"; f1.maxLon.value=\"\"; f1.minLat.value=\"\"; f1.maxLat.value=\"\"; ((document.all)? document.all.rubberBand : document.getElementById(\"rubberBand\")).style.visibility=\"hidden\";'")}
      </td>
    </tr>

    <tr>
      <td colspan="2" class="N">$unsafe{twoClickMap[0]}$unsafe{EDStatic.htmlTooltipImage(request, language, loggedInAs, EDStatic.messages.get(Message.ADVANCED_SEARCH_MAP_TOOLTIP, language))}$unsafe{twoClickMap[1]}</td>
    </tr>

    <tr><td colspan="2">&nbsp;</td></tr>

    <tr>
      <td class="N">${EDStatic.messages.get(Message.ADVANCED_SEARCH_MIN_TIME, language)}</td>
      <td>&nbsp;=&nbsp;$unsafe{widgets.textField("minTime", EDStatic.messages.get(Message.ADVANCED_SEARCH_MIN_TIME, language) + "<" + "p>" + EDStatic.messages.get(Message.ADVANCED_SEARCH_TIME_TOOLTIP, language), 27, 40, minTime, "")}</td>
    </tr>
    <tr>
      <td class="N">${EDStatic.messages.get(Message.ADVANCED_SEARCH_MAX_TIME, language)}</td>
      <td>&nbsp;=&nbsp;$unsafe{widgets.textField("maxTime", EDStatic.messages.get(Message.ADVANCED_SEARCH_MAX_TIME, language) + "<" + "p>" + EDStatic.messages.get(Message.ADVANCED_SEARCH_TIME_TOOLTIP, language), 27, 40, maxTime, "")}</td>
    </tr>

    </table>

    <p>$unsafe{widgets.htmlButton("submit", null, null, EDStatic.messages.get(Message.SEARCH_CLICK_TIP, language), "<" + "span style=\"font-size:large;\"><" + "strong>" + EDStatic.messages.get(Message.SEARCH_BUTTON, language) + "</" + "strong></" + "span>", "")}</p>

    $unsafe{widgets.endForm()}
    $unsafe{twoClickMap[2]}
