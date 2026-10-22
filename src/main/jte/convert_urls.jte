@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import com.cohort.util.String2
@import jakarta.servlet.http.HttpServletRequest
@import java.text.MessageFormat

@param YouAreHere youAreHere
@param int language
@param String tErddapUrl
@param String loggedInAs = ""
@param String endOfRequest = "convert/urls.html"
@param HttpServletRequest request = null
@param String inputText
@param String resultText

@template.layout.main(
    tErddapUrl=tErddapUrl,
    language=language,
    youAreHere=youAreHere,
    endOfRequest=endOfRequest,
    loggedInAs=loggedInAs,
    request=request,
    content=@`
<div class="standard_width">
    <h2>${TemplateHelper.getMessage(Message.CONVERT_URLS, language)}</h2>
    $unsafe{TemplateHelper.getMessage(Message.CONVERT_URLS_INTRO, language)}

    <p>
    <form method="GET" action="${tErddapUrl}/convert/urls.html" name="convertURLs">
        <strong>${TemplateHelper.getMessage(Message.CONVERT_URLS, language)}</strong>
        <br>
        <textarea name="text" cols="80" rows="6" maxlength="1000" wrap="soft">${inputText}</textarea>
        <br>
        <button type="submit" name="submit" value="Convert">
            <strong>${TemplateHelper.getMessage(Message.CONVERT, language)}</strong>
        </button>
        @if(String2.isSomething(resultText))
            <br><span class="successColor">${resultText}</span>
        @else
            <br>&nbsp;
        @endif
    </form>
    </p>

    $unsafe{TemplateHelper.getMessage(Message.CONVERT_URLS_NOTES, language)}
    $unsafe{MessageFormat.format(TemplateHelper.getMessage(Message.CONVERT_URLS_SERVICE, language), tErddapUrl)}
</div>
`
)
