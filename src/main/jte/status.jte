@import gov.noaa.pfel.erddap.util.TemplateModel
@param TemplateModel model
<%-- need to change the below tag in the future versions need to use model.youarehere--%>
<h1 class="nowrap">
    <a title="Click here to return to the ERDDAP home page."rel="start" href="http://localhost:8080/erddap/index.html">ERDDAP</a>
    &gt; Status
</h1>
<pre>
    ${model.statistics}
</pre>
