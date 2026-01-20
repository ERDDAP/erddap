@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import jakarta.servlet.http.HttpServletRequest
@import com.cohort.util.XML
@import java.text.MessageFormat
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
  $unsafe{TemplateHelper.getMessage(Message.ADVANCED_SEARCH_DIRECTIONS, language)}
    <noscript><div style="color:red"><strong>To work correctly, this web page requires that JavaScript be enabled in your browser. Please enable JavaScript and reload this page.</strong></div></noscript>
    <form name="${formName}" id="${formName}" method="GET" action="${tErddapUrl}/search/advanced.html">

    <input type="hidden" name="page" value="${page}">
    <input type="hidden" name="itemsPerPage" value="${itemsPerPage}">

    <p><strong>${TemplateHelper.getMessage(Message.SEARCH_FULL_TEXT_HTML, language)}</strong>
    @template.components.search_input(
      request = request,
      language = language,
      loggedInAs = loggedInAs,
      searchFor = searchFor,
      size = 70
    )
    </p>

    &nbsp;
    <table class="compact nowrap">
    <tr>
      <td colspan="2"><strong>${TemplateHelper.getMessage(Message.CATEGORY_TITLE_HTML, language)}</strong>
      @template.components.tooltip_image(
        request = request,
        language = language,
        loggedInAs = loggedInAs,
        html = "<" + "div class=\"narrow_max_width\">" + TemplateHelper.getMessage(Message.ADVANCED_SEARCH_CATEGORY_TOOLTIP, language) + "</" + "div>"
      )
      </td>
    </tr>
    <tr>
      <td class="N" style="width:20%;">protocol
      @template.components.tooltip_image(
        request = request,
        language = language,
        loggedInAs = loggedInAs,
        html = "<" + "div class=\"standard_max_width\">protocol tooltip</" + "div>"
      )
      </td>
      <td style="width:80%;">&nbsp;=&nbsp;
      @template.components.generic_select(
        name = "protocol",
        options = protocols,
        selected = protocols[whichProtocol]
      )
      </td>
    </tr>

    @for(int ca=0; ca<catAttsInURLs.length; ca++)
      @if(catSAs[ca].length > 1)
        <tr>
          <td class="N">${catAttsInURLs[ca]}</td>
          <td>&nbsp;=&nbsp;
            @template.components.generic_select(
              name = catAttsInURLs[ca],
              options = catSAs[ca],
              selected = (whichCatSAIndex[ca] >= 0) ? catSAs[ca][whichCatSAIndex[ca]] : null
            )
          </td>
        </tr>
      @endif
    @endfor

    <tr><td colspan="2">&nbsp;</td></tr>
    <tr>
      <td colspan="2"><strong>${TemplateHelper.getMessage(Message.ADVANCED_SEARCH_BOUNDS, language)}</strong>
      @template.components.tooltip_image(
        request = request,
        language = language,
        loggedInAs = loggedInAs,
        html = "<" + "div class=\"standard_max_width\">" + TemplateHelper.getMessage(Message.ADVANCED_SEARCH_RANGE_TOOLTIP, language) + "</" + "div>"
      )
      </td>
    </tr>

    <tr>
      <td class="N">${TemplateHelper.getMessage(Message.ADVANCED_SEARCH_MAX_LAT, language)}</td>
      <td>&nbsp;=&nbsp;<input type="text" name="maxLat" size="8" maxlength="12" value="${maxLat}" title="${TemplateHelper.getMessage(Message.ADVANCED_SEARCH_MAX_LAT, language)} (-90 to 90) $unsafe{TemplateHelper.getMessage(Message.ADVANCED_SEARCH_MAP_TOOLTIP, language)}"></td>
    </tr>

    <tr>
      <td class="N">${TemplateHelper.getMessage(Message.ADVANCED_SEARCH_MIN_MAX_LON, language)}</td>
      <td>&nbsp;=&nbsp;<input type="text" name="minLon" size="8" maxlength="12" value="${minLon}" title="${TemplateHelper.getMessage(Message.ADVANCED_SEARCH_MIN_LON, language)} $unsafe{TemplateHelper.getMessage(Message.ADVANCED_SEARCH_MAP_TOOLTIP, language)}">
      <input type="text" name="maxLon" size="8" maxlength="12" value="${maxLon}" title="${TemplateHelper.getMessage(Message.ADVANCED_SEARCH_MAX_LON, language)} $unsafe{TemplateHelper.getMessage(Message.ADVANCED_SEARCH_MAP_TOOLTIP, language)}">
      </td>
    </tr>

    <tr>
      <td class="N">${TemplateHelper.getMessage(Message.ADVANCED_SEARCH_MIN_LAT, language)}</td>
      <td>&nbsp;=&nbsp;<input type="text" name="minLat" size="8" maxlength="12" value="${minLat}" title="${TemplateHelper.getMessage(Message.ADVANCED_SEARCH_MIN_LAT, language)} (-90 to 90) $unsafe{TemplateHelper.getMessage(Message.ADVANCED_SEARCH_MAP_TOOLTIP, language)}">
      <button type="button" title="${TemplateHelper.getMessage(Message.ADVANCED_SEARCH_CLEAR_HELP, language)}" onClick='document.forms["${formName}"].minLon.value=""; document.forms["${formName}"].maxLon.value=""; document.forms["${formName}"].minLat.value=""; document.forms["${formName}"].maxLat.value=""; ((document.all)? document.all.rubberBand : document.getElementById("rubberBand")).style.visibility="hidden";'>${TemplateHelper.getMessage(Message.ADVANCED_SEARCH_CLEAR, language)}</button>
      </td>
    </tr>

    <tr>
      <td colspan="2" class="N">$unsafe{twoClickMap[0]}
      @template.components.tooltip_image(
        request = request,
        language = language,
        loggedInAs = loggedInAs,
        html = TemplateHelper.getMessage(Message.ADVANCED_SEARCH_MAP_TOOLTIP, language)
      )
      $unsafe{twoClickMap[1]}</td>
    </tr>

    <tr><td colspan="2">&nbsp;</td></tr>

    <tr>
      <td class="N">${TemplateHelper.getMessage(Message.ADVANCED_SEARCH_MIN_TIME, language)}</td>
      <td>&nbsp;=&nbsp;<input type="text" name="minTime" size="27" maxlength="40" value="${minTime}" title="${TemplateHelper.getMessage(Message.ADVANCED_SEARCH_MIN_TIME, language)} $unsafe{TemplateHelper.getMessage(Message.ADVANCED_SEARCH_TIME_TOOLTIP, language)}"></td>
    </tr>
    <tr>
      <td class="N">${TemplateHelper.getMessage(Message.ADVANCED_SEARCH_MAX_TIME, language)}</td>
      <td>&nbsp;=&nbsp;<input type="text" name="maxTime" size="27" maxlength="40" value="${maxTime}" title="${TemplateHelper.getMessage(Message.ADVANCED_SEARCH_MAX_TIME, language)} $unsafe{TemplateHelper.getMessage(Message.ADVANCED_SEARCH_TIME_TOOLTIP, language)}"></td>
    </tr>

    </table>

    <p><button type="submit" title="${TemplateHelper.getMessage(Message.SEARCH_CLICK_TIP, language)}"><span style="font-size:large;"><strong>${TemplateHelper.getMessage(Message.SEARCH_BUTTON, language)}</strong></span></button></p>

    </form>
    $unsafe{twoClickMap[2]}
