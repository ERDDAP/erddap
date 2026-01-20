@import com.cohort.util.XML
@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import jakarta.servlet.http.HttpServletRequest
@param HttpServletRequest request
@param int language
@param String loggedInAs
@param String html

<img src="${TemplateHelper.questionMarkImageUrl(request, language, loggedInAs)}" alt="${XML.encodeAsHTMLAttribute("?")}" onmouseover="Tip('${TemplateHelper.escapeTooltipForTip(html == null ? "" : html)}')" onmouseout="UnTip()">
