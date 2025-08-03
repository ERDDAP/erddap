@import gov.noaa.pfel.erddap.jte.OutOfDateDatasets
@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.TableOptions
@import gov.noaa.pfel.erddap.util.EDStatic
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@param YouAreHere youAreHere
@param int language
@param TableOptions tableOptions

@template.youarehere(youAreHere)

${OutOfDateDatasets.getMessage(Message.ADVC_OUT_OF_DATE,language)}

@if(tableOptions.getNRows() == 0)
    [${OutOfDateDatasets.getFormated(OutOfDateDatasets.getMessage(Message.N_MATCHING,language),"0")}<br>
    ${OutOfDateDatasets.getMessage(Message.ADVC_OUT_OF_DATE,language)}]
@else
    ${OutOfDateDatasets.getFormated(OutOfDateDatasets.getMessage(Message.N_MATCHING,language),""+tableOptions.getNRows())} <br>
    ${OutOfDateDatasets.getFormated(OutOfDateDatasets.getMessage(Message.GENERATED_AT,language),OutOfDateDatasets.getCurrentTimeZulu())}<br>
    <br>
    @template.Table(tableOptions)
@endif
