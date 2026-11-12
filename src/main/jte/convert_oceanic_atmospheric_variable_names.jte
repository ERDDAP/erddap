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
@param String endOfRequest = "convert/oceanicAtmosphericVariableNames.html"
@param HttpServletRequest request = null

@param String variableNameValue = "sst"
@param String variableNameTooltip = "The oceanic/atmospheric variable name, for example, \"sst\"."
@param boolean toFullName = false
@param String answerVariableName = ""
@param String answerFullName = ""

@param String fullNameSelectHtml = ""
@param boolean toVariableName = false

@param String variableNameFileTypes = ""
@param String tError = null

@template.layout.main(
    tErddapUrl=tErddapUrl,
    language=language,
    youAreHere=youAreHere,
    endOfRequest=endOfRequest,
    loggedInAs=loggedInAs,
    request=request,
    content=@`
<div class="standard_width">
    <h2>${TemplateHelper.getMessage(Message.CONVERT_OA_VARIABLE_NAMES_TO_FROM, language)}</h2>
    $unsafe{TemplateHelper.getMessage(Message.CONVERT_OA_VARIABLE_NAMES_INTRO, language)}

    @template.components.noscript()

    <form method="GET" action="${tErddapUrl}/convert/oceanicAtmosphericVariableNames.html" name="getFullName">
        $unsafe{MessageFormat.format(
            TemplateHelper.getMessage(Message.CONVERT_TO_FULL_NAME, language),
            "</strong>\n<input type=\"text\" name=\"variableName\" value=\"" + XML.encodeAsHTMLAttribute(variableNameValue) + "\" size=\"20\" maxlength=\"40\" title=\"" + XML.encodeAsHTMLAttribute(variableNameTooltip) + "\">\n<strong>"
        )}
        &nbsp;&nbsp;
        <button type="submit" value="Convert">
            <strong>${TemplateHelper.getMessage(Message.CONVERT, language)}</strong>
        </button>
        @if(toFullName)
            @if(tError == null)
                <br><span class="successColor">${answerVariableName} = ${answerFullName}</span>
            @else
                <br><span class="warningColor">${tError}</span>
            @endif
        @else
            <br>&nbsp;
        @endif
        <br>
    </form>

    <form method="GET" action="${tErddapUrl}/convert/oceanicAtmosphericVariableNames.html" name="getVariableName">
        $unsafe{MessageFormat.format(
            TemplateHelper.getMessage(Message.CONVERT_TO_VARIABLE_NAME, language),
            "</strong>\n" + fullNameSelectHtml + "\n<strong>"
        )}
        @if(toVariableName)
            @if(tError == null)
                <br><span class="successColor">${answerFullName} = ${answerVariableName}</span>
            @else
                <br><span class="warningColor">${tError}</span>
            @endif
        @else
            <br>&nbsp;
        @endif
        <br>
    </form>

    <p><a rel="bookmark" href="${tErddapUrl}/convert/oceanicAtmosphericVariableNames.html">${TemplateHelper.getMessage(Message.RESET_THE_FORM, language)}</a></p>
    <p>$unsafe{TemplateHelper.getMessage(Message.CONVERT_BYPASS, language)}</p>
    <p>Or, view/download the entire oceanic/atmospheric variable names list in these file types:<br>
    $unsafe{variableNameFileTypes}</p>
    $unsafe{TemplateHelper.getMessage(Message.CONVERT_OA_VARIABLE_NAMES_NOTES, language)}
    $unsafe{MessageFormat.format(TemplateHelper.getMessage(Message.CONVERT_OA_VARIABLE_NAMES_SERVICE, language), tErddapUrl)}
</div>
`
)
