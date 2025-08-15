@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.TableOptions
@import gov.noaa.pfel.erddap.util.EDStatic
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@param YouAreHere youAreHere
@param int language
@param TableOptions tableOptions

@template.youarehere(youAreHere)
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
