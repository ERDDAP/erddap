@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import java.text.MessageFormat
@import jakarta.servlet.http.HttpServletRequest
@param int language
@param HttpServletRequest request
@param String loggedInAs
@param String searchFor
@param int size = 40

@if(searchFor == null)
  <input type="text" name="searchFor" size="${(size > 0) ? size : 40}" maxlength="255" value="" title="${MessageFormat.format(TemplateHelper.getMessage(Message.SEARCH_TIP, language), "noaa wind")}">
@else
  <input type="text" name="searchFor" size="${(size > 0) ? size : 40}" maxlength="255" value="${searchFor}" title="${MessageFormat.format(TemplateHelper.getMessage(Message.SEARCH_TIP, language), "noaa wind")}">
@endif
@template.components.tooltip_image(
  request = request,
  language = language,
  loggedInAs = loggedInAs,
  html = TemplateHelper.getMessage(Message.SEARCH_HINTS_TOOLTIP, language)
)
