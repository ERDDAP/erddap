@import gg.jte.Content
@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@param Content content
@param String tErddapUrl
@param int language
@param YouAreHere youAreHere
@param String endOfRequest
<!DOCTYPE html>
<html lang="&langCode;">
<head>
<meta charset="UTF-8">
<title>ERDDAP</title>
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
      <strong>ERDDAP</strong>
      <br><small><small><small>${TemplateHelper.getMessage(Message.EASIER_ACCESS_TO_SCIENTIFIC_DATA,language)}</small></small></small>
      </td>
      <!-- need to do further changes at the legal.html -->
    <td style="text-align:right; font-size:small;">
      &loginInfo; | <select name="language"
        onchange="window.location.href='/erddap/' + (this.selectedIndex == 0 ? '' : this[this.selectedIndex].value + '/') + '${endOfRequest}'">
        <option value="en">English</option>
        <option value="bn">বাংলা - Bengali</option>
        <option value="zh-CN">汉语 - Chinese (Simplified)</option>
        <option value="zh-TW">漢語 - Chinese (Transitional)</option>
        <option value="cs">Čeština - Czech</option>
        <option value="da">Dansk - Danish</option>
        <option value="nl">Nederlands - Dutch</option>
        <option value="fi">Suomi - Finnish</option>
        <option value="fr">Français - French</option>
        <option value="de">Deutsch - German</option>
        <option value="el">Ελληνικά - Greek</option>
        <option value="hi">हिन्दी - Hindi</option>
        <option value="hu">Magyar - Hungarian</option>
        <option value="id">Bahasa Indonesia - Indonesian</option>
        <option value="ga">Gaeilge - Irish</option>
        <option value="it">Italiano - Italian</option>
        <option value="ja">日本語 - Japanese</option>
        <option value="ko">한국어 - Korean</option>
        <option value="no">Norsk - Norwegian</option>
        <option value="pl">Polski - Polish</option>
        <option value="pt">Português - Portuguese</option>
        <option value="ro">Română - Romanian</option>
        <option value="ru">Русский - Russian</option>
        <option value="es">Español - Spanish</option>
        <option value="sv">Svenska - Swedish</option>
        <option value="tl">Wikang Tagalog - Tagalog</option>
        <option value="th">ภาษาไทย - Thai</option>
        <option value="tr">Türkçe - Turkish</option>
        <option value="uk">Українська - Ukrainian</option>
        <option value="ur">اُردُو - Urdu</option>
      </select> &nbsp; &nbsp;
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
@template.youarehere(youAreHere)
<!-- Content block -->
${content}

$unsafe{TemplateHelper.getEndBodyHtml(tErddapUrl, language)}

</body>
</html>