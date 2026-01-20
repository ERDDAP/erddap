@import gg.jte.Content
@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import jakarta.servlet.http.HttpServletRequest
@import gov.noaa.pfel.erddap.util.TranslateMessages
@param Content content
@param String tErddapUrl
@param int language
@param YouAreHere youAreHere
@param String endOfRequest
@param String loggedInAs
@param HttpServletRequest request
<!DOCTYPE html>
<html lang="&langCode;">
<head>
<meta charset="UTF-8">
<title>ERDDAP™</title>
<link rel="shortcut icon" href="${tErddapUrl}/images/favicon.ico">
<link href="${tErddapUrl}/images/erddap2.css" rel="stylesheet" type="text/css">
<meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<body>
  <script src="${tErddapUrl}/images/wz_tooltip.js"></script>
  <table class="compact nowrap" style="width:100%; background-color:#128CB5;">
  <tr>
    <td style="text-align:center; width:80px;"><a rel="bookmark"
      href="https://www.noaa.gov/"><img
      title="National Oceanic and Atmospheric Administration"
      src="${tErddapUrl}/images/noaab.png" alt="NOAA"
      style="vertical-align:middle;"></a></td>
    <td style="text-align:left; font-size:x-large; color:#FFFFFF; ">
      <strong>ERDDAP™</strong>
      <br><small><small><small>${TemplateHelper.getMessage(Message.EASIER_ACCESS_TO_SCIENTIFIC_DATA,language)}</small></small></small>
      </td>
      <!-- need to do further changes at the legal.html -->
    <td style="text-align:right; font-size:small;">
      ${TemplateHelper.getLoginHtml(request,language,loggedInAs)} |
      <select name="language"
              onchange="window.location.href='/erddap/' + (this.value == 'en' ? '' : this.value + '/') + '${endOfRequest}'">
          @for(int i = 0; i < TranslateMessages.languageList.size(); i++)
              <option value="${TranslateMessages.languageCodeList.get(i)}" selected="${language == i}">
                  ${TranslateMessages.languageList.get(i)}
              </option>
          @endfor
      </select>
      &nbsp; &nbsp;
      <br>${TemplateHelper.getMessage(Message.BROUGHT_TO_YOU_BY,language)}
      <a title="National Oceanic and Atmospheric Administration" rel="bookmark"
      href="https://www.noaa.gov">NOAA</a>
      <a title="National Marine Fisheries Service" rel="bookmark"
      href="https://www.fisheries.noaa.gov">NMFS</a>
      <a title="Southwest Fisheries Science Center" rel="bookmark"
      href="https://www.fisheries.noaa.gov/about/southwest-fisheries-science-center">SWFSC</a>
      <a title="Environmental Research Division" rel="bookmark"
      href="https://www.fisheries.noaa.gov/about/environmental-research-division-southwest-fisheries-science-center">ERD</a>
      &nbsp; &nbsp;
      </td>
  </tr>
</table>
@template.components.youarehere(youAreHere)
<!-- Content block -->
${content}

$unsafe{TemplateHelper.getEndBodyHtml(tErddapUrl, language)}

</body>
</html>