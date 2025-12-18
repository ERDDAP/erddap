@import gov.noaa.pfel.erddap.jte.YouAreHere
@import jakarta.servlet.http.HttpServletRequest
@import gov.noaa.pfel.erddap.jte.TableOptions
@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.Erddap
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@param TableOptions tableOptions
@param String tErddapUrl
@param int language
@param YouAreHere youAreHere
@param String loggedInAs
@param String endOfRequest
@param HttpServletRequest request
@param String secondLine
@param String nMatchingHtml
@param String errorLineOne
@param String errorLineTwo
@template.layout.main(
    tErddapUrl=tErddapUrl,
    language=language,
    youAreHere=youAreHere,
    endOfRequest=endOfRequest,
    loggedInAs=loggedInAs,
    request=request,
    content=@`
    <div class="standard_width">
    $unsafe{secondLine}
    @if(errorLineOne.length() > 0)
        <p><span class="warningColor">
        $unsafe{errorLineOne}
        $unsafe{errorLineTwo}
        </span>
        </p>
    @else
    $unsafe{nMatchingHtml}
    @if(tableOptions.getNRows()>0)
    @template.table(tableOptions)
    <p>
    ${TemplateHelper.getMessage(Message.RESTFUL_INFORMATION_FORMATS, language)}
    (${Erddap.plainFileTypesString})
    <a rel="help" href="${tErddapUrl}/rest.html">
    ${TemplateHelper.getMessage(Message.RESTFUL_VIA_SERVICE, language)}</a>
    </p>
    @endif
    @endif
    
    </div>
    `)
