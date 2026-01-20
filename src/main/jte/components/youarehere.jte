@import gov.noaa.pfel.erddap.jte.YouAreHere

@param YouAreHere youAreHere

@if(youAreHere.getBreadcrumbs().size() > 0)
<h1 class="nowrap">
    <a title="${youAreHere.getErddapLinkTitle()}" rel="start"
        href="${youAreHere.getErddapUrl()}/index.html">
        ${youAreHere.getProgramName()}</a>
        @for(int i = 0; i < youAreHere.getBreadcrumbs().size(); i++)
            @if(youAreHere.getBreadcrumbLinks().size() > i)
                &gt; <a href="${youAreHere.getBreadcrumbLinks().get(i)}" > ${youAreHere.getBreadcrumbs().get(i)} </a>
            @else
                &gt; ${youAreHere.getBreadcrumbs().get(i)}
            @endif
        @endfor
</h1>
@endif