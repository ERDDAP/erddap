@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.util.EDStatic
@import gov.noaa.pfel.erddap.jte.Subscription
@param YouAreHere youAreHere
@param int language
@template.youarehere(youAreHere)
${EDStatic.messages.subscription0HtmlAr[language]}
    $unsafe{Subscription.getFormated(EDStatic.messages.subscription1HtmlAr[language], youAreHere.getErddapUrl())}

<p>
    <strong>
        ${EDStatic.messages.optionsAr[language]}
        :
    </strong>
</p>
<ul>
    <li>
        <a rel="bookmark" href="${youAreHere.getErddapUrl()}/${Subscription.getAddHtml()}">
            ${EDStatic.messages.subscriptionAddAr[language]}
        </a>
    </li>
    <li>
        <a rel="bookmark" href="${youAreHere.getErddapUrl()}/${Subscription.getValidateHtml()}">
            ${EDStatic.messages.subscriptionValidateAr[language]}
        </a>
    </li>
    <li>
        <a rel="bookmark" href="${youAreHere.getErddapUrl()}/${Subscription.getListHtml()}">
            ${EDStatic.messages.subscriptionListAr[language]}
        </a>
    </li>
    <li>
        <a rel="bookmark" href="${youAreHere.getErddapUrl()}/${Subscription.getRemoveHtml()}">
            ${EDStatic.messages.subscriptionRemoveAr[language]}
        </a>
    </li>
</ul>