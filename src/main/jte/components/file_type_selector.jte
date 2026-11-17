@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import java.util.List

@param String tErddapUrl = ""
@param String protocol = "tabledap"
@param int language = 0
@param List<String> fileTypeDescriptions = List.of()
@param String defaultFileTypeOption = ".htmlTable"

!{
    String tooltip = TemplateHelper.escapeTooltipForTip(TemplateHelper.getMessage(Message.EDD_SELECT_FILE_TYPE, language));
}

<p><strong>${TemplateHelper.getMessage(Message.EDD_FILE_TYPE, language)}</strong>
 (<a rel="help" href="${tErddapUrl}/${protocol}/documentation.html#fileType">${TemplateHelper.getMessage(Message.MORE_INFORMATION, language)}</a>)</p>

<select name="fileType" size="1" onmouseover="Tip('${tooltip}')" onmouseout="UnTip()">
  @for(String desc : fileTypeDescriptions)
    <option value="${desc}" selected="${defaultFileTypeOption != null && desc.startsWith(defaultFileTypeOption + " ")}">${desc}</option>
  @endfor
</select>

