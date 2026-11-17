@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import com.cohort.util.XML
@import jakarta.servlet.http.HttpServletRequest

@param String protocol = "tabledap"
@param String tErddapUrl = ""
@param int language = 0
@param String loggedInAs = ""
@param HttpServletRequest request = null
@param String javaScript = ""

!{
    String genViewTooltipRaw = TemplateHelper.getMessage(Message.JUST_GENERATE_AND_VIEW_TOOLTIP, language)
        .replace("&protocolName;", protocol);
    String genViewHtml = "<div class=\"standard_max_width\">" + genViewTooltipRaw + "</div>";
    String escapedGenViewTip = TemplateHelper.escapeTooltipForTip(genViewHtml);
    String escapedUrlTip = TemplateHelper.escapeTooltipForTip(TemplateHelper.getMessage(Message.JUST_GENERATE_AND_VIEW_URL, language));
    String escapedSubmitTip = TemplateHelper.escapeTooltipForTip(TemplateHelper.getMessage(Message.SUBMIT_TOOLTIP, language));
}

<br>
<input type="button" name="getUrl" value="${TemplateHelper.getMessage(Message.JUST_GENERATE_AND_VIEW, language)}" onclick="${javaScript}" onmouseover="Tip('${escapedGenViewTip}')" onmouseout="UnTip()" >
<input type="text" name="tUrl" value="" size="60" maxlength="1000" onmouseover="Tip('${escapedUrlTip}')" onmouseout="UnTip()" >
<br>(<a rel="help" href="${tErddapUrl}/${protocol}/documentation.html" title="${protocol} documentation">Documentation&nbsp;/&nbsp;Bypass&nbsp;this&nbsp;form</a>
@template.components.tooltip_image(request=request, language=language, loggedInAs=loggedInAs, html=genViewHtml))
<br>&nbsp;
<br>
<button type="button" name="submit1" onmouseover="Tip('${escapedSubmitTip}')" onmouseout="UnTip()" onclick="${javaScript}if (result.length > 0) window.location=result;">
  <span style="font-size:large;"><strong>${TemplateHelper.getMessage(Message.SUBMIT, language)}</strong></span>
</button>
${TemplateHelper.getMessage(Message.PATIENT_DATA, language)}

