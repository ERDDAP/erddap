@import gov.noaa.pfel.erddap.util.EDStatic
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@param int language
@param String errorHead
@param String errorTail

<span class="warningColor">${EDStatic.messages.get(Message.ERROR_TITLE, language)}: $unsafe{errorHead}</span>$unsafe{errorTail}<br>&nbsp;
