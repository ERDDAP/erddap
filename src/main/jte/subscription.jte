@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.util.EDStatic
@import gov.noaa.pfel.erddap.jte.Subscription
@import gov.noaa.pfel.erddap.util.EDMessages.Message;
@param YouAreHere youAreHere
@param int language
@template.youarehere(youAreHere)
${Subscription.getMessage(Message.SUBSCRIPTION_0_HTML,language)}
    $unsafe{Subscription.getFormated(Subscription.getMessage(Message.SUBSCRIPTION_1_HTML,language), youAreHere.getErddapUrl())}

<p>
    <strong>
        ${Subscription.getMessage(Message.OPTIONS,language)}
        :
    </strong>
</p>
<ul>
    <li>
        <a rel="bookmark" href="${youAreHere.getErddapUrl()}/${Subscription.getAddHtml()}">
            ${Subscription.getMessage(Message.SUBSCRIPTION_ADD,language)}
        </a>
    </li>
    <li>
        <a rel="bookmark" href="${youAreHere.getErddapUrl()}/${Subscription.getValidateHtml()}">
            ${Subscription.getMessage(Message.SUBSCRIPTION_VALIDATE,language)}
        </a>
    </li>
    <li>
        <a rel="bookmark" href="${youAreHere.getErddapUrl()}/${Subscription.getListHtml()}">
            ${Subscription.getMessage(Message.SUBSCRIPTION_LIST,language)}
        </a>
    </li>
    <li>
        <a rel="bookmark" href="${youAreHere.getErddapUrl()}/${Subscription.getRemoveHtml()}">
            ${Subscription.getMessage(Message.SUBSCRIPTION_REMOVE,language)}
        </a>
    </li>
</ul>