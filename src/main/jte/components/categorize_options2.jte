@import com.cohort.util.MustBe
@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDStatic
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

<h3>2) $unsafe{MessageFormat.format(TemplateHelper.getMessage(Message.CATEGORY_SEARCH_HTML, language), attributeInURL)}:&nbsp;
@template.components.tooltip_image(
      request = request,
      language = language,
      loggedInAs = loggedInAs,
      html = MessageFormat.format(TemplateHelper.getMessage(Message.CATEGORY_CLICK_HTML, language), "")
    )</h3>
@if(values.length == 0)
  ${MustBe.THERE_IS_NO_DATA}
@else
  @template.components.generic_select(
    name = "cat2",
    options = values,
    selected = value,
    onchange = "window.location='${tErddapUrl}/categorize/${attributeInURL}/' + this.options[this.selectedIndex].text + '/index.html?'+ '${EDStatic.encodedPassThroughPIppQueryPage1(request)}';",
    size = 12
  )
@endif
