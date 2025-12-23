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
@param String secondLine
@param String advancedSearchHtml
@param boolean advancedOpen
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
        <div id="searchSecondLineContainer">
        <div id="searchSecondLine">
            $unsafe{secondLine}
        </div>

        @if(advancedOpen)
        <details id="advancedSearchDetails" class="advanced_search_details" open>
            <summary>
                ${TemplateHelper.getMessage(Message.ADVANCED_SEARCH, language)}
            </summary>
            <div id="advancedSearchContent">
                $unsafe{advancedSearchHtml}
            </div>
        </details>
        @else
        <details id="advancedSearchDetails" class="advanced_search_details">
            <summary>
                ${TemplateHelper.getMessage(Message.ADVANCED_SEARCH, language)}
            </summary>
            <div id="advancedSearchContent">
                $unsafe{advancedSearchHtml}
            </div>
        </details>
        @endif
        </div>

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
        (${plainFileTypesString})
        <a rel="help" href="${tErddapUrl}/rest.html">
        ${TemplateHelper.getMessage(Message.RESTFUL_VIA_SERVICE, language)}
        </a>
        </p>
        <p>
        ${TemplateHelper.getMessage(Message.ADVANCED_SEARCH_ERROR_HANDLING, language)}
        </p>
        @endif
    @endif
    </div>
    `)
