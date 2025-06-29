@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.Legal
@param String tErddapUrl
@param int language
@param YouAreHere youAreHere
@template.youarehere(youAreHere)
<div>$unsafe{Legal.getLegalNoticesAr(language)}</div>
<div>$unsafe{Legal.getStandardGeneralDisclaimerAr(language)}</div>
<div>$unsafe{Legal.getLegal(language,tErddapUrl)}</div>