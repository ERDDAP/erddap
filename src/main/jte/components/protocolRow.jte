@import gov.noaa.pfel.erddap.jte.Index
@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message

@param String url
@param String protocolName
@param String description
@param String docUrl
@param Index data

<tr>
    <td>
        <a rel="bookmark" href="${url}" title="${TemplateHelper.getFormated(TemplateHelper.getMessage(Message.PROTOCOL_CLICK, data.language), protocolName)}">
            $unsafe{TemplateHelper.getFormated(TemplateHelper.getMessage(Message.INDEX_DATASETS, data.language), protocolName)}
        </a>
    </td>
    <td>
        $unsafe{description}
        <a rel="help" href="${docUrl}">
            $unsafe{TemplateHelper.getFormated(TemplateHelper.getMessage(Message.INDEX_DOCUMENTATION, data.language), protocolName)}
        </a>
    </td>
</tr>