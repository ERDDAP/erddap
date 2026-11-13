@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import com.cohort.util.XML
@import jakarta.servlet.http.HttpServletRequest
@import java.text.MessageFormat

@param YouAreHere youAreHere
@param int language
@param String tErddapUrl
@param String loggedInAs = ""
@param String endOfRequest = "convert/interpolate.html"
@param HttpServletRequest request = null

@param String idVarExample = "jplMURSST41/analysed_sst/Bilinear/4"
@param String tllTableHelpTooltip = ""
@param String tllTableText = ""
@param String datasetIdVarHelpTooltip = ""
@param String requestCsvValue = ""
@param String datasetIDVarOptionsSelectHtml = ""
@param String algorithmOptionsSelectHtml = ""
@param String nearbyOptionsSelectHtml = ""
@param String fileTypeSelectHtml = ""
@param String serviceDocumentation = ""

@template.layout.main(
    tErddapUrl=tErddapUrl,
    language=language,
    youAreHere=youAreHere,
    endOfRequest=endOfRequest,
    loggedInAs=loggedInAs,
    request=request,
    content=@`
<div class="standard_width">
    <h2>${TemplateHelper.getMessage(Message.CONVERT_INTERPOLATE, language)}</h2>
    $unsafe{MessageFormat.format(TemplateHelper.getMessage(Message.CONVERT_INTERPOLATE_INTRO, language), idVarExample)}

    @template.components.noscript()

    <form method="GET" action="${tErddapUrl}/convert/interpolate.html" name="f1">
        <table class="compact">
            <tr>
                <td>
                    ${TemplateHelper.getMessage(Message.CONVERT_INTERPOLATE_TLL_TABLE, language)}
                    $unsafe{tllTableHelpTooltip}
                </td>
                <td>
                    <textarea name="TimeLatLonTable" cols="66" rows="6" maxlength="4000" wrap="soft">${tllTableText}</textarea>
                </td>
            </tr>
            <tr>
                <td>
                    ${TemplateHelper.getMessage(Message.CONVERT_INTERPOLATE_DATASET_ID_VARIABLE, language)}
                    $unsafe{datasetIdVarHelpTooltip}
                </td>
                <td class="N">
                    <input type="text" name="requestCSV" size="64" maxlength="500" value="$unsafe{XML.encodeAsHTMLAttribute(requestCsvValue)}">
                </td>
            </tr>
            <tr>
                <td>
                    &nbsp;&nbsp;&nbsp;${TemplateHelper.getMessage(Message.OPTIONS, language)}:
                </td>
                <td class="N">
                    &nbsp;&nbsp;&nbsp;$unsafe{datasetIDVarOptionsSelectHtml}
                    $unsafe{algorithmOptionsSelectHtml}
                    $unsafe{nearbyOptionsSelectHtml}
                    <button type="button" name="comma" title="If you click this button, a comma will be appended to the Request CSV above." onclick="document.f1.requestCSV.value+=',';">,</button>
                </td>
            </tr>
            <tr>
                <td>
                    ${TemplateHelper.getMessage(Message.EDD_FILE_TYPE, language)}
                </td>
                <td>
                    $unsafe{fileTypeSelectHtml}
                </td>
            </tr>
            <tr>
                <td>
                    <button type="button" value="Convert" title="${TemplateHelper.getMessage(Message.CLICK_TO_SUBMIT, language)}" onclick="var d = document; window.location='${tErddapUrl}/convert/interpolate' + d.f1.fileType.options[d.f1.fileType.selectedIndex].text + '?TimeLatLonTable=' + encodeURIComponent(d.f1.TimeLatLonTable.value) + '&amp;requestCSV=' + encodeURIComponent(d.f1.requestCSV.value);">
                        <strong>${TemplateHelper.getMessage(Message.CONVERT, language)}</strong>
                    </button>
                </td>
                <td></td>
            </tr>
        </table>
    </form>

    <br>&nbsp;
    <p>$unsafe{TemplateHelper.getMessage(Message.CONVERT_BYPASS, language)}</p>
    $unsafe{TemplateHelper.getMessage(Message.CONVERT_INTERPOLATE_NOTES, language)}
    $unsafe{serviceDocumentation}
</div>
`
)
