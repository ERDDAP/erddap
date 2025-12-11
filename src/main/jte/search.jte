@import gov.noaa.pfel.erddap.jte.YouAreHere
@import jakarta.servlet.http.HttpServletRequest
@import gov.noaa.pfel.erddap.jte.TableOptions
@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@param TableOptions tableOptions
@param String tErddapUrl
@param int language
@param YouAreHere youAreHere
@param String loggedInAs
@param String endOfRequest
@param HttpServletRequest request
@param String searchFormHtml
@param String nMatchingHtml
@param String advancedSearchLink
@param String plainFileTypesString
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
    $unsafe{searchFormHtml}
    
    @if(errorLineOne.length() > 0)
        <strong>
        ${errorLineOne}
        </strong>
        <br>
        ${errorLineTwo}
    @else
        $unsafe{nMatchingHtml}
        <span class="N">(
            ${TemplateHelper.getMessage(Message.OR_REFINE_SEARCH_WITH, language)}
            $unsafe{advancedSearchLink}
            )</span> 
            <br>&nbsp;
        @if(tableOptions.getNRows()>0)
        @template.table(tableOptions)
        @endif
        <p>
        ${TemplateHelper.getMessage(Message.RESTFUL_INFORMATION_FORMATS, language)}
        (${plainFileTypesString})
        <a rel="help" href="${tErddapUrl}/rest.html">
        ${TemplateHelper.getMessage(Message.RESTFUL_VIA_SERVICE, language)}</a>
        </p>
    @endif
    </div>
    `)
