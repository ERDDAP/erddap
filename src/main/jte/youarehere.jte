@import gov.noaa.pfel.erddap.jte.YouAreHere

@param YouAreHere youAreHere

<h1 class="nowrap">
    <a title="${youAreHere.getErddapLinkTitle()}" rel="start"
        href="${youAreHere.getErddapUrl()}/index.html">
        ${youAreHere.getProgramName()}</a>
        &gt; ${youAreHere.getProtocol()} 
</h1>