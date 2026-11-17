@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message

@param int language = 0
@param String dasContent = ""

<hr>
<h2><a class="selfLink" id="DAS" href="#DAS" rel="bookmark">${TemplateHelper.getMessage(Message.DAS_TITLE, language)}</a></h2>
<pre style="white-space:pre-wrap;">$unsafe{dasContent}</pre>

