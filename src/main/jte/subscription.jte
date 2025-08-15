@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.util.EDStatic
@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import gov.noaa.pfel.erddap.util.Subscriptions
@import jakarta.servlet.http.HttpServletRequest;
@param YouAreHere youAreHere
@param int language
@param String tErddapUrl
@param String loggedInAs
@param String endOfRequest
@param HttpServletRequest request
@template.layout.main(
    tErddapUrl=tErddapUrl,
    language=language,
    youAreHere=youAreHere,
    endOfRequest=endOfRequest,
    loggedInAs=loggedInAs,
    request=request,
    content=@`
<div class ="standard_width">
    ${TemplateHelper.getMessage(Message.SUBSCRIPTION_0_HTML,language)}
        $unsafe{TemplateHelper.getFormated(TemplateHelper.getMessage(Message.SUBSCRIPTION_1_HTML,language), youAreHere.getErddapUrl())}

    <p>
        <strong>${TemplateHelper.getMessage(Message.OPTIONS,language)}:</strong>
    </p>
    <ul>
        <li>
            <a rel="bookmark" href="${youAreHere.getErddapUrl()}/${Subscriptions.ADD_HTML}">
                ${TemplateHelper.getMessage(Message.SUBSCRIPTION_ADD,language)}
            </a>
        </li>
        <li>
            <a rel="bookmark" href="${youAreHere.getErddapUrl()}/${Subscriptions.VALIDATE_HTML}">
                ${TemplateHelper.getMessage(Message.SUBSCRIPTION_VALIDATE,language)}
            </a>
        </li>
        <li>
            <a rel="bookmark" href="${youAreHere.getErddapUrl()}/${Subscriptions.LIST_HTML}">
                ${TemplateHelper.getMessage(Message.SUBSCRIPTION_LIST,language)}
            </a>
        </li>
        <li>
            <a rel="bookmark" href="${youAreHere.getErddapUrl()}/${Subscriptions.REMOVE_HTML}">
                ${TemplateHelper.getMessage(Message.SUBSCRIPTION_REMOVE,language)}
            </a>
        </li>
    </ul>
</div>`)
