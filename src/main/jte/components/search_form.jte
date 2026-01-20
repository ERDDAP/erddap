@import gov.noaa.pfel.erddap.jte.TemplateHelper
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

$unsafe{pretext + TemplateHelper.getMessage(Message.SEARCH_DO_FULL_TEXT_HTML, language) + posttext}

<form name="search" id="search" method="GET" action="${EDStatic.erddapUrl(request, loggedInAs, language)}/search/index.html">
  <input type="hidden" name="page" value="1">
  <input type="hidden" name="itemsPerPage" value="${itemsPerPage}">
  @template.components.search_input(
    request = request,
    language = language,
    loggedInAs = loggedInAs,
    searchFor = searchFor
  )
  <button type="submit" title="${TemplateHelper.getMessage(Message.SEARCH_CLICK_TIP, language)}">${TemplateHelper.getMessage(Message.SEARCH_BUTTON, language)}</button>
</form>
