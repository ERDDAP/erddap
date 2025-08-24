@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.Legal
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@param String tErddapUrl
@param int language
@param YouAreHere youAreHere
@param String endOfRequest
@template.layout.main(
    tErddapUrl=tErddapUrl,
    language=language,
    youAreHere=youAreHere,
    endOfRequest=endOfRequest,
    content=@`
        <div class="standard_width">
            <div>$unsafe{Legal.getMessage(Message.LEGAL_NOTICES, language)}</div>
            <div>$unsafe{Legal.getMessage(Message.STANDARD_GENERAL_DISCLAIMER, language)}</div>
            <div>$unsafe{Legal.getLegal(language,tErddapUrl)}</div>
        </div>
        `
)