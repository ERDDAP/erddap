@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@param int language
@param String errorHead
@param String errorTail

<span class="warningColor">${TemplateHelper.getMessage(Message.ERROR_TITLE, language)}: $unsafe{errorHead}</span>$unsafe{errorTail}<br>&nbsp;
