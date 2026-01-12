@import gov.noaa.pfel.coastwatch.util.HtmlWidgets
@import gov.noaa.pfel.erddap.util.EDStatic
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import java.text.MessageFormat
@param int language
@param jakarta.servlet.http.HttpServletRequest request
@param String loggedInAs
@param String pretext
@param String posttext
@param String searchFor
@param String itemsPerPage
@param gov.noaa.pfel.coastwatch.util.HtmlWidgets widgets

$unsafe{pretext + EDStatic.messages.get(Message.SEARCH_DO_FULL_TEXT_HTML, language) + posttext}

$unsafe{widgets.beginForm("search", "GET", EDStatic.erddapUrl(request, loggedInAs, language) + "/search/index.html", "")}
$unsafe{widgets.hidden("page", "1")}
$unsafe{widgets.hidden("itemsPerPage", itemsPerPage)}
@if(searchFor == null)
  $unsafe{widgets.textField("searchFor", MessageFormat.format(EDStatic.messages.get(Message.SEARCH_TIP, language), "noaa wind"), 40, 255, "", "")}
@else
  $unsafe{widgets.textField("searchFor", MessageFormat.format(EDStatic.messages.get(Message.SEARCH_TIP, language), "noaa wind"), 40, 255, searchFor, "")}
@endif
$unsafe{EDStatic.htmlTooltipImage(request, language, loggedInAs, EDStatic.messages.get(Message.SEARCH_HINTS_TOOLTIP, language))}
$unsafe{widgets.htmlButton("submit", null, null, EDStatic.messages.get(Message.SEARCH_CLICK_TIP, language), EDStatic.messages.get(Message.SEARCH_BUTTON, language), "")}

$unsafe{widgets.endForm()}
