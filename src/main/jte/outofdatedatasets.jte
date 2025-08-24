@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.TableOptions
@import gov.noaa.pfel.erddap.util.EDStatic
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@param TableOptions tableOptions
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
<p>
${TemplateHelper.getMessage(Message.ADVC_OUT_OF_DATE,language)}

@if(tableOptions.getNRows() == 0)
    [${TemplateHelper.getFormated(TemplateHelper.getMessage(Message.N_MATCHING,language),"0")}<br>
    ${TemplateHelper.getMessage(Message.ADVN_OUT_OF_DATE,language)}]
@else
    ${TemplateHelper.getFormated(TemplateHelper.getMessage(Message.N_MATCHING,language),""+tableOptions.getNRows())} <br>
    ${TemplateHelper.getFormated(TemplateHelper.getMessage(Message.GENERATED_AT,language),TemplateHelper.getCurrentTimeZulu())}<br>
    <br>
    @template.table(tableOptions)
@endif
</p>
</div>
`)
