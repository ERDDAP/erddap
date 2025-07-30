@import gov.noaa.pfel.erddap.jte.OutOfDateDatasets
@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.TableOptions
@import gov.noaa.pfel.erddap.util.EDStatic;
@param YouAreHere youAreHere
@param int language
@param TableOptions tableOptions

@template.youarehere(youAreHere)

${EDStatic.messages.advc_outOfDateAr[language]}

@if(tableOptions.getNRows() == 0)
    [${OutOfDateDatasets.getFormated(EDStatic.messages.nMatchingAr[language],"0")}<br>
    ${EDStatic.messages.advc_outOfDateAr[language]}]
@else
    ${OutOfDateDatasets.getFormated(EDStatic.messages.nMatchingAr[language],""+tableOptions.getNRows())} <br>
    ${OutOfDateDatasets.getFormated(EDStatic.messages.generatedAtAr[language],OutOfDateDatasets.getCurrentTimeZulu())}<br>
    <br>
    @template.Table(tableOptions)
@endif
