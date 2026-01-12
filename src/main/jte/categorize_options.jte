@import gov.noaa.pfel.erddap.util.EDStatic
@import gov.noaa.pfel.coastwatch.util.HtmlWidgets
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import java.text.MessageFormat
@param int language
@param jakarta.servlet.http.HttpServletRequest request
@param String loggedInAs
@param String attributeInURL
@param boolean homePage
@param String tCategoryHtml
@param String attributeInURLSelected
@param gov.noaa.pfel.coastwatch.util.HtmlWidgets widgets
@param String[] links
@param String categoryList

@if(homePage)
  <h3>
    $unsafe{EDStatic.messages.get(Message.CATEGORY_TITLE_HTML, language)}
  </h3>
  @if(categoryList != null)
    $unsafe{MessageFormat.format(tCategoryHtml.replace("<br>", ""), categoryList)}
  @endif
  $unsafe{EDStatic.messages.get(Message.CATEGORY3_HTML, language)}
@else
  <h3>1) ${EDStatic.messages.get(Message.CATEGORY_PICK_ATTRIBUTE, language)} &nbsp;$unsafe{EDStatic.htmlTooltipImage(request, language, loggedInAs, MessageFormat.format(tCategoryHtml, "").replaceAll("  ", " "))}</h3>
  $unsafe{widgets.select("cat1", "", Math.min(EDStatic.config.categoryAttributesInURLs.length, 12), EDStatic.config.categoryAttributesInURLs, attributeInURLSelected == null ? 0 : java.util.Arrays.asList(EDStatic.config.categoryAttributesInURLs).indexOf(attributeInURLSelected), "onchange=\"window.location='" + EDStatic.erddapUrl(request, loggedInAs, language) + "/categorize/' + this.options[this.selectedIndex].text + '/index.html?" + EDStatic.encodedPassThroughPIppQueryPage1(request) + "';\"")}
@endif
