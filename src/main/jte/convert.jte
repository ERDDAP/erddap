@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.jte.Index.ConverterLink
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import jakarta.servlet.http.HttpServletRequest
@import java.util.List
@param YouAreHere youAreHere
@param int language
@param String tErddapUrl
@param String loggedInAs = ""
@param String endOfRequest
@param HttpServletRequest request = null
@param List<ConverterLink> converterLinks
@template.layout.main(
    tErddapUrl=tErddapUrl,
    language=language,
    youAreHere=youAreHere,
    endOfRequest=endOfRequest,
    loggedInAs=loggedInAs,
    request=request,
    content=@`
<div class="standard_width">
    <p>${TemplateHelper.getMessage(Message.CONVERT_HTML, language)}</p>
    <ul>
        @for(var link : converterLinks)
            <li><a rel="bookmark" href="${link.url}"><strong>${link.text}</strong></a> - ${link.description}</li>
        @endfor
    </ul>
</div>
`
)
