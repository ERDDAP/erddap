@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import jakarta.servlet.http.HttpServletRequest
@import java.text.MessageFormat
@import java.util.List

@param YouAreHere youAreHere
@param int language
@param String tErddapUrl
@param String loggedInAs = ""
@param String endOfRequest = "convert/keywords.html"
@param HttpServletRequest request = null

@param String cfSelectHtml = ""
@param boolean toGCMD = false
@param String answerCF = ""
@param List<String> answerGcmdList = List.of()

@param String gcmdSelectHtml = ""
@param boolean toCF = false
@param String answerGCMD = ""
@param List<String> answerCfList = List.of()

@param String cfToGcmdFileTypes = ""
@param String cfFileTypes = ""
@param String gcmdFileTypes = ""
@param String externalLinkHtml = ""
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
    <h2>${TemplateHelper.getMessage(Message.CONVERT_KEYWORDS, language)}</h2>
    $unsafe{TemplateHelper.getMessage(Message.CONVERT_KEYWORDS_INTRO, language)}

    @template.components.noscript()

    <form method="GET" action="${tErddapUrl}/convert/keywords.html" name="getGCMD">
        $unsafe{MessageFormat.format(
            TemplateHelper.getMessage(Message.CONVERT_TO_GCMD, language),
            "</strong>\n<br>" + cfSelectHtml + "\n<br><strong>"
        )}
        @if(toGCMD)
            @if(tError == null)
                <br><span class="successColor">${answerCF} =<br>
                @for(var item : answerGcmdList)
                    ${item}<br>
                @endfor
                </span>
            @else
                <br><span class="warningColor">${tError}</span>
            @endif
        @else
            <br>&nbsp;
        @endif
        <br>
    </form>

    <form method="GET" action="${tErddapUrl}/convert/keywords.html" name="getCF">
        $unsafe{MessageFormat.format(
            TemplateHelper.getMessage(Message.CONVERT_TO_CF_STANDARD_NAMES, language),
            "</strong>\n<br>" + gcmdSelectHtml + "\n<br><strong>"
        )}
        @if(toCF)
            @if(tError == null)
                <br><span class="successColor">${answerGCMD} =<br>
                @for(var item : answerCfList)
                    ${item}<br>
                @endfor
                </span>
            @else
                <br><span class="warningColor">${tError}</span>
            @endif
        @else
            <br>&nbsp;
        @endif
        <br>
    </form>

    <p><strong>Other Options</strong></p>
    <ul>
        <li><a rel="bookmark" href="${tErddapUrl}/convert/keywords.html">${TemplateHelper.getMessage(Message.RESET_THE_FORM, language)}</a><br>&nbsp;</li>
        <li>$unsafe{TemplateHelper.getMessage(Message.CONVERT_BYPASS, language)}<br>&nbsp;</li>
        <li>View/download a file which has all of the CF to GCMD conversion information:<br>
        $unsafe{cfToGcmdFileTypes}<br>The GCMD to CF conversion information can be derived from this.<br>&nbsp;</li>
        <li>View/download the entire CF Standard Names list in these file types:<br>
        $unsafe{cfFileTypes}<br>Source: <a rel="bookmark" href="https://cfconventions.org/Data/cf-standard-names/18/build/cf-standard-name-table.html">Version 18, dated 22 July 2011$unsafe{externalLinkHtml}</a>.<br>&nbsp;</li>
        <li>View/download the entire GCMD Science Keywords list in these file types:<br>
        $unsafe{gcmdFileTypes}<br>Source: <a rel="bookmark" href="https://wiki.earthdata.nasa.gov/display/CMR/GCMD+Keyword+Access">the version dated 2008-02-05$unsafe{externalLinkHtml}</a>.
        The requested citation is<kbd><br>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester,<br>H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau,<br>M. Holland, T. Northcutt, R. A. Restrepo, 2007 . NASA/Global Change<br>Master Directory (GCMD) Earth Science Keywords. Version 6.0.0.0.0 </kbd></li>
    </ul>

    $unsafe{TemplateHelper.getMessage(Message.CONVERT_KEYWORDS_NOTES, language)}
    $unsafe{MessageFormat.format(TemplateHelper.getMessage(Message.CONVERT_KEYWORDS_SERVICE, language), tErddapUrl)}
</div>
`
)
