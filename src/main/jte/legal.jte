@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.Legal
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@param String tErddapUrl
@param int language
@param YouAreHere youAreHere
@template.youarehere(youAreHere)
<div>$unsafe{Legal.getMessage(Message.LEGAL_NOTICES, language)}</div>
<div>$unsafe{Legal.getMessage(Message.STANDARD_GENERAL_DISCLAIMER, language)}</div>
<div>$unsafe{Legal.getLegal(language,tErddapUrl)}</div>