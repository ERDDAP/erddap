@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDStatic
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import java.text.MessageFormat
@param int language
@param jakarta.servlet.http.HttpServletRequest request
@param String loggedInAs
@param String attributeInURL
@param boolean homePage
@param String tCategoryHtml
@param String attributeInURLSelected
@param String[] links
@param String categoryList

@if(homePage)
  <h3>
    $unsafe{TemplateHelper.getMessage(Message.CATEGORY_TITLE_HTML, language)}
  </h3>
  @if(categoryList != null)
    $unsafe{MessageFormat.format(tCategoryHtml.replace("<br>", ""), categoryList)}
  @endif
  $unsafe{TemplateHelper.getMessage(Message.CATEGORY3_HTML, language)}
@else
  <h3>1) ${TemplateHelper.getMessage(Message.CATEGORY_PICK_ATTRIBUTE, language)} &nbsp;
  @template.components.tooltip_image(
      request = request,
      language = language,
      loggedInAs = loggedInAs,
      html = MessageFormat.format(tCategoryHtml, "")
    )</h3>
  @template.components.generic_select(
    name = "cat1",
    options = EDStatic.config.categoryAttributesInURLs,
    selected = attributeInURLSelected,
    onchange = "window.location='${EDStatic.erddapUrl(request, loggedInAs, language)}/categorize/' + this.options[this.selectedIndex].text + '/index.html?'+ '${EDStatic.encodedPassThroughPIppQueryPage1(request)}';",
    size = 12
  )
@endif
