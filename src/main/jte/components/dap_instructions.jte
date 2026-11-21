@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message

@param String protocol = "tabledap"
@param int language = 0
@param String tErddapUrl = ""
@param String dapBase = ""
@param String exampleHref = ""
@param String exampleText = ""
@param String queryExplanationHtml = ""
@param String longDapDescription = ""

<br>&nbsp;
<hr>
<h2><a class="selfLink" id="instructions" href="#instructions" rel="bookmark">${protocol.equals("griddap") ? TemplateHelper.getMessage(Message.USING_GRIDDAP, language) : TemplateHelper.getMessage(Message.USING_TABLEDAP, language)}</a></h2>
$unsafe{longDapDescription}
<p><strong>${protocol.equals("tabledap") ? "Tabledap" : "griddap"} request URLs must be in the form</strong>
<br><kbd>${dapBase}<i><a rel="help" href="${dapBase}documentation.html#datasetID">datasetID</a></i>.<i><a rel="help" href="${dapBase}documentation.html#fileType">fileType</a></i>{?<i><a rel="help" href="${dapBase}documentation.html#query">query</a></i>}</kbd>
<br>For example,
<br><a href="${exampleHref}"><kbd>${exampleText}</kbd></a>
@if(protocol.equals("tabledap"))
    <br>Thus, the query is often a comma-separated list of desired variable names,
       followed by a collection of
      constraints (e.g., <kbd><i>variable</i>&lt;<i>value</i></kbd>),
      each preceded by '&amp;' (which is interpreted as "AND").
@elseif(queryExplanationHtml != null && !queryExplanationHtml.isEmpty())
    <br>$unsafe{queryExplanationHtml}
@endif
</p>
<p>For details, see the <a rel="help" href="${tErddapUrl}/${protocol}/documentation.html">${protocol} Documentation</a>.</p>
