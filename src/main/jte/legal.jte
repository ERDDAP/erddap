@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.Legal
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import jakarta.servlet.http.HttpServletRequest;
@param String tErddapUrl
@param int language
@param YouAreHere youAreHere
@param String endOfRequest
@param String loggedInAs
@param HttpServletRequest request
@template.layout.main(
    tErddapUrl=tErddapUrl,
    language=language,
    youAreHere=youAreHere,
    endOfRequest=endOfRequest,
    loggedInAs=loggedInAs,
    request=request,
    content=@`
        <div class="standard_width">
            <div>$unsafe{Legal.getMessage(Message.LEGAL_NOTICES, language)}</div>
            <div>$unsafe{Legal.getMessage(Message.STANDARD_GENERAL_DISCLAIMER, language)}</div>
            <div>$unsafe{Legal.getLegal(language,tErddapUrl)}</div>
        </div>
        `
)
