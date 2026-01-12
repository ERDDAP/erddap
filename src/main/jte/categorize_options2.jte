@import com.cohort.util.MustBe
@import gov.noaa.pfel.erddap.util.EDStatic
@import gov.noaa.pfel.coastwatch.util.HtmlWidgets
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import java.text.MessageFormat
@import jakarta.servlet.http.HttpServletRequest
@param int language
@param HttpServletRequest request
@param String loggedInAs
@param String attribute
@param String attributeInURL
@param String value
@param String tErddapUrl
@param String[] values
@param gov.noaa.pfel.coastwatch.util.HtmlWidgets widgets

<h3>2) $unsafe{MessageFormat.format(EDStatic.messages.get(Message.CATEGORY_SEARCH_HTML, language), attributeInURL)}:&nbsp; $unsafe{EDStatic.htmlTooltipImage(request, language, loggedInAs, EDStatic.messages.get(Message.CATEGORY_CLICK_HTML, language))}</h3>
@if(values.length == 0)
  ${MustBe.THERE_IS_NO_DATA}
@else
  $unsafe{widgets.select("cat2", "", Math.min(values.length, 12), values, java.util.Arrays.asList(values).indexOf(value), "onchange=\"window.location='" + tErddapUrl + "/categorize/" + attributeInURL + "/' + this.options[this.selectedIndex].text + '/index.html?" + EDStatic.encodedPassThroughPIppQueryPage1(request) + "';\"")}
@endif
