package gov.noaa.pfel.erddap.handlers;

import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class TopLevelHandler extends State {
  private StringBuilder data = new StringBuilder();
  private SaxParsingContext context;
  private boolean reallyVerbose;
  private StringBuilder warningsFromLoadDatasets;
  private int nDatasets = 0;

  public TopLevelHandler(SaxHandler saxHandler, SaxParsingContext context) {
    super(saxHandler);
    this.context = context;
    this.reallyVerbose = context.getReallyVerbose();
    this.warningsFromLoadDatasets = context.getWarningsFromLoadDatasets();
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) {
    data.setLength(0);

    switch (localName) {
      case "convertToPublicSourceUrl" -> {
        String tFrom = attributes.getValue("from");
        String tTo = attributes.getValue("to");
        int spo = EDStatic.convertToPublicSourceUrlFromSlashPo(tFrom);
        if (tFrom != null && tFrom.length() > 3 && spo == tFrom.length() - 1 && tTo != null) {
          EDStatic.convertToPublicSourceUrl.put(tFrom, tTo);
        }
      }
      case "user" -> {
        String tUsername = attributes.getValue("username");
        String tPassword = attributes.getValue("password");
        if (tUsername != null) {
          tUsername = tUsername.trim();
        }
        if (tPassword != null) {
          tPassword = tPassword.trim().toLowerCase();
        }
        String ttRoles = attributes.getValue("roles");
        String[] tRoles =
            StringArray.arrayFromCSV(
                (ttRoles == null ? "" : ttRoles + ",") + EDStatic.anyoneLoggedIn, ",", true, false);

        // is username nothing?
        if (!String2.isSomething(tUsername)) {
          warningsFromLoadDatasets.append(
              "datasets.xml error: A <user> tag in datasets.xml had no username=\"someName\" attribute.\n\n");

          // is username reserved?
        } else if (EDStatic.loggedInAsHttps.equals(tUsername)
            || EDStatic.anyoneLoggedIn.equals(tUsername)
            || EDStatic.loggedInAsSuperuser.equals(tUsername)) {
          warningsFromLoadDatasets.append(
              "datasets.xml error: <user> username=\""
                  + String2.annotatedString(tUsername)
                  + "\" is a reserved username.\n\n");

          // is username invalid?
        } else if (!String2.isPrintable(tUsername)) {
          warningsFromLoadDatasets.append(
              "datasets.xml error: <user> username=\""
                  + String2.annotatedString(tUsername)
                  + "\" has invalid characters.\n\n");

          // is password invalid?
        } else if (EDStatic.authentication.equals("custom")
            && // others in future
            !String2.isHexString(tPassword)) {
          warningsFromLoadDatasets.append(
              "datasets.xml error: The password for <user> username="
                  + tUsername
                  + " in datasets.xml isn't a hexadecimal string.\n\n");

          // a role is not allowed?
        } else if (String2.indexOf(tRoles, EDStatic.loggedInAsSuperuser) >= 0) {
          warningsFromLoadDatasets.append(
              "datasets.xml error: For <user> username="
                  + tUsername
                  + ", the superuser role isn't allowed for any user.\n\n");

          // add user info to tUserHashMap
        } else {
          Arrays.sort(tRoles);
          if ("email".equals(EDStatic.authentication) || "google".equals(EDStatic.authentication)) {
            tUsername = tUsername.toLowerCase();
          }
          if (reallyVerbose) {
            String2.log("user=" + tUsername + " roles=" + String2.toCSSVString(tRoles));
          }
          Object o = context.gettUserHashMap().put(tUsername, new Object[] {tPassword, tRoles});
          if (o != null) {
            warningsFromLoadDatasets.append(
                "datasets.xml error: There are two <user> tags in datasets.xml with username="
                    + tUsername
                    + "\nChange one of them.\n\n");
          }
        }
      }
      case "dataset" -> {
        this.nDatasets++;
        context.getNTryAndDatasets()[1] = nDatasets;

        String datasetType = attributes.getValue("type");
        String datasetID = attributes.getValue("datasetID");
        String active = attributes.getValue("active");

        State state =
            HandlerFactory.getHandlerFor(
                datasetType, datasetID, active, this, saxHandler, context, true);
        saxHandler.setState(state);
      }
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    data.append(new String(ch, start, length));
  }

  @Override
  public void endElement(String uri, String localName, String qName) {
    switch (localName) {
      case "angularDegreeUnits" -> {
        String ts = data.toString();
        if (!String2.isSomething(ts)) ts = EDStatic.DEFAULT_ANGULAR_DEGREE_UNITS;
        EDStatic.angularDegreeUnitsSet =
            new HashSet<String>(String2.toArrayList(StringArray.fromCSVNoBlanks(ts).toArray()));

        if (reallyVerbose) {
          String2.log("angularDegreeUnits=" + String2.toCSVString(EDStatic.angularDegreeUnitsSet));
        }
      }
      case "angularDegreeTrueUnits" -> {
        String ts = data.toString();
        if (!String2.isSomething(ts)) ts = EDStatic.DEFAULT_ANGULAR_DEGREE_TRUE_UNITS;
        EDStatic.angularDegreeTrueUnitsSet =
            new HashSet<String>(
                String2.toArrayList(StringArray.fromCSVNoBlanks(ts).toArray())); // so canonical

        if (reallyVerbose) {
          String2.log(
              "angularDegreeTrueUnits=" + String2.toCSVString(EDStatic.angularDegreeTrueUnitsSet));
        }
      }
      case "awsS3OutputBucketUrl" -> {
        String ts = data.toString();
        if (!String2.isSomething(ts)) ts = null;
        EDStatic.awsS3OutputBucketUrl = ts;

        if (reallyVerbose) {
          String2.log("awsS3OutputBucketUrl=" + ts);
        }
      }
      case "cacheMinutes" -> {
        int tnt = String2.parseInt(data.toString());
        EDStatic.cacheMillis =
            (tnt < 1 || tnt == Integer.MAX_VALUE ? EDStatic.DEFAULT_cacheMinutes : tnt)
                * Calendar2.MILLIS_PER_MINUTE;

        if (reallyVerbose) {
          String2.log("cacheMinutes=" + EDStatic.cacheMillis / Calendar2.MILLIS_PER_MINUTE);
        }
      }
      case "commonStandardNames" -> {
        String ts = data.toString();
        EDStatic.commonStandardNames =
            String2.isSomething(ts)
                ? String2.canonical(StringArray.arrayFromCSV(ts))
                : EDStatic.DEFAULT_commonStandardNames;

        if (reallyVerbose) {
          String2.log("commonStandardNames=" + String2.toCSSVString(EDStatic.commonStandardNames));
        }
      }
      case "decompressedCacheMaxGB" -> {
        int tnt = String2.parseInt(data.toString());
        EDStatic.decompressedCacheMaxGB =
            tnt < 1 || tnt == Integer.MAX_VALUE ? EDStatic.DEFAULT_decompressedCacheMaxGB : tnt;

        if (reallyVerbose) {
          String2.log("decompressedCacheMaxGB=" + EDStatic.decompressedCacheMaxGB);
        }
      }
      case "decompressedCacheMaxMinutesOld" -> {
        int tnt = String2.parseInt(data.toString());
        EDStatic.decompressedCacheMaxMinutesOld =
            tnt < 1 || tnt == Integer.MAX_VALUE
                ? EDStatic.DEFAULT_decompressedCacheMaxMinutesOld
                : tnt;

        if (reallyVerbose) {
          String2.log("decompressedCacheMaxMinutesOld=" + EDStatic.decompressedCacheMaxMinutesOld);
        }
      }
      case "drawLandMask" -> {
        String ts = data.toString();
        int tnt = String2.indexOf(SgtMap.drawLandMask_OPTIONS, ts);
        EDStatic.drawLandMask =
            tnt < 1 ? EDStatic.DEFAULT_drawLandMask : SgtMap.drawLandMask_OPTIONS[tnt];

        if (reallyVerbose) {
          String2.log("drawLandMask=" + EDStatic.drawLandMask);
        }
      }
      case "emailDiagnosticsToErdData" -> {
        String ts = data.toString();
        boolean ted = String2.isSomething(ts) ? String2.parseBoolean(ts) : true; // the default

        EDStatic.emailDiagnosticsToErdData = ted;

        if (reallyVerbose) {
          String2.log("emailDiagnosticsToErdData=" + ted);
        }
      }
      case "graphBackgroundColor" -> {
        String ts = data.toString();
        int tnt =
            String2.isSomething(ts)
                ? String2.parseInt(ts)
                : EDStatic.DEFAULT_graphBackgroundColorInt;
        EDStatic.graphBackgroundColor = new Color(tnt, true); // hasAlpha

        if (reallyVerbose) {
          String2.log("graphBackgroundColor=" + String2.to0xHexString(tnt, 8));
        }
      }
      case "ipAddressMaxRequests" -> {
        int tnt = String2.parseInt(data.toString());
        tnt = tnt < 6 || tnt > 1000 ? EDStatic.DEFAULT_ipAddressMaxRequests : tnt;
        EDStatic.ipAddressMaxRequests = tnt;

        if (reallyVerbose) {
          String2.log("ipAddressMaxRequests=" + tnt);
        }
      }
      case "ipAddressMaxRequestsActive" -> {
        int tnt = String2.parseInt(data.toString());
        tnt = tnt < 1 || tnt > 100 ? EDStatic.DEFAULT_ipAddressMaxRequestsActive : tnt;
        EDStatic.ipAddressMaxRequestsActive = tnt;

        if (reallyVerbose) {
          String2.log("ipAddressMaxRequestsActive=" + tnt);
        }
      }
      case "ipAddressUnlimited" -> {
        String ts = data.toString();
        String[] sar =
            StringArray.fromCSVNoBlanks(ts + EDStatic.DEFAULT_ipAddressUnlimited).toArray();
        EDStatic.ipAddressUnlimited = new HashSet<String>(String2.toArrayList(sar));
        for (String s : sar) {
          EDStatic.ipAddressQueue.remove(s);
        }

        if (reallyVerbose) {
          String2.log("ipAddressUnlimited=" + String2.toCSVString(EDStatic.ipAddressUnlimited));
        }
      }
      case "loadDatasetsMinMinutes" -> {
        int tnt = String2.parseInt(data.toString());
        EDStatic.loadDatasetsMinMillis =
            (tnt < 1 || tnt == Integer.MAX_VALUE ? EDStatic.DEFAULT_loadDatasetsMinMinutes : tnt)
                * Calendar2.MILLIS_PER_MINUTE;

        if (reallyVerbose) {
          String2.log(
              "loadDatasetsMinMinutes="
                  + EDStatic.loadDatasetsMinMillis / Calendar2.MILLIS_PER_MINUTE);
        }
      }
      case "loadDatasetsMaxMinutes" -> {
        int tnt = String2.parseInt(data.toString());
        EDStatic.loadDatasetsMaxMillis =
            (tnt < 1 || tnt == Integer.MAX_VALUE ? EDStatic.DEFAULT_loadDatasetsMaxMinutes : tnt)
                * Calendar2.MILLIS_PER_MINUTE;

        if (reallyVerbose) {
          String2.log(
              "loadDatasetsMaxMinutes="
                  + EDStatic.loadDatasetsMaxMillis / Calendar2.MILLIS_PER_MINUTE);
        }
      }
      case "logLevel" -> EDStatic.setLogLevel(data.toString());
      case "nGridThreads" -> {
        int tnt = String2.parseInt(data.toString());
        EDStatic.nGridThreads =
            tnt < 1 || tnt == Integer.MAX_VALUE ? EDStatic.DEFAULT_nGridThreads : tnt;

        if (reallyVerbose) {
          String2.log("nGridThreads=" + EDStatic.nGridThreads);
        }
      }
      case "nTableThreads" -> {
        int tnt = String2.parseInt(data.toString());
        EDStatic.nTableThreads =
            tnt < 1 || tnt == Integer.MAX_VALUE ? EDStatic.DEFAULT_nTableThreads : tnt;

        if (reallyVerbose) {
          String2.log("nTableThreads=" + EDStatic.nTableThreads);
        }
      }
      case "palettes" -> {
        String tContent = data.toString();
        String[] tPalettes =
            String2.isSomething(tContent)
                ? String2.split(tContent, ',')
                : EDStatic.DEFAULT_palettes;
        HashSet<String> newPaletteSet = String2.stringArrayToSet(tPalettes);
        if (!newPaletteSet.containsAll(EDStatic.DEFAULT_palettes_set))
          throw new RuntimeException(
              "The <palettes> tag MUST include all of the palettes listed in the <palettes> tag in messages.xml.");
        String[] tPalettes0 = new String[tPalettes.length + 1];
        tPalettes0[0] = "";
        System.arraycopy(tPalettes, 0, tPalettes0, 1, tPalettes.length);
        // then copy into place
        EDStatic.palettes = tPalettes;
        EDStatic.palettes0 = tPalettes0;

        if (reallyVerbose) {
          String2.log("palettes=" + String2.toCSSVString(tPalettes));
        }
      }
      case "partialRequestMaxBytes" -> {
        int tnt = String2.parseInt(data.toString());
        EDStatic.partialRequestMaxBytes =
            tnt < 1000000 || tnt == Integer.MAX_VALUE
                ? EDStatic.DEFAULT_partialRequestMaxBytes
                : tnt;

        if (reallyVerbose) {
          String2.log("partialRequestMaxBytes=" + EDStatic.partialRequestMaxBytes);
        }
      }
      case "partialRequestMaxCells" -> {
        int tnt = String2.parseInt(data.toString());
        EDStatic.partialRequestMaxCells =
            tnt < 1000 || tnt == Integer.MAX_VALUE ? EDStatic.DEFAULT_partialRequestMaxCells : tnt;

        if (reallyVerbose) {
          String2.log("partialRequestMaxCells=" + EDStatic.partialRequestMaxCells);
        }
      }
      case "requestBlacklist" -> EDStatic.setRequestBlacklist(data.toString());
      case "slowDownTroubleMillis" -> {
        int tms = String2.parseInt(data.toString());
        EDStatic.slowDownTroubleMillis = tms < 0 || tms > 1000000 ? 1000 : tms;

        if (reallyVerbose) {
          String2.log("slowDownTroubleMillis=" + EDStatic.slowDownTroubleMillis);
        }
      }
      case "subscriptionEmailBlacklist" -> {
        if (EDStatic.subscriptionSystemActive) {
          EDStatic.subscriptions.setEmailBlacklist(data.toString());
        }
      }
      case "standardLicense" -> {
        String ts = data.toString();
        EDStatic.standardLicense = String2.isSomething(ts) ? ts : EDStatic.DEFAULT_standardLicense;

        if (reallyVerbose) {
          String2.log("standardLicense was set.");
        }
      }
      case "standardContact" -> {
        String ts = data.toString();
        ts = String2.isSomething(ts) ? ts : EDStatic.DEFAULT_standardContactAr[0];
        ts = String2.replaceAll(ts, "&adminEmail;", SSR.getSafeEmailAddress(EDStatic.adminEmail));
        EDStatic.standardContactAr[0] = ts; // swap into place

        if (reallyVerbose) {
          String2.log("standardContact was set.");
        }
      }
      case "standardDataLicenses" -> {
        String ts = data.toString();
        EDStatic.standardDataLicensesAr[0] =
            String2.isSomething(ts) ? ts : EDStatic.DEFAULT_standardDataLicensesAr[0];

        if (reallyVerbose) {
          String2.log("standardDataLicenses was set.");
        }
      }
      case "standardDisclaimerOfEndorsement" -> {
        String ts = data.toString();
        EDStatic.standardDisclaimerOfEndorsementAr[0] =
            String2.isSomething(ts) ? ts : EDStatic.DEFAULT_standardDisclaimerOfEndorsementAr[0];

        if (reallyVerbose) {
          String2.log("standardDisclaimerOfEndorsement was set.");
        }
      }
      case "standardDisclaimerOfExternalLinks" -> {
        String ts = data.toString();
        EDStatic.standardDisclaimerOfExternalLinksAr[0] =
            String2.isSomething(ts) ? ts : EDStatic.DEFAULT_standardDisclaimerOfExternalLinksAr[0];

        if (reallyVerbose) {
          String2.log("standardDisclaimerOfExternalLinks was set.");
        }
      }
      case "standardGeneralDisclaimer" -> {
        String ts = data.toString();
        EDStatic.standardGeneralDisclaimerAr[0] =
            String2.isSomething(ts) ? ts : EDStatic.DEFAULT_standardGeneralDisclaimerAr[0];

        if (reallyVerbose) {
          String2.log("standardGeneralDisclaimer was set.");
        }
      }
      case "standardPrivacyPolicy" -> {
        String ts = data.toString();
        EDStatic.standardPrivacyPolicyAr[0] =
            String2.isSomething(ts) ? ts : EDStatic.DEFAULT_standardPrivacyPolicyAr[0];

        if (reallyVerbose) {
          String2.log("standardPrivacyPolicy was set.");
        }
      }
      case "startHeadHtml5" -> {
        String ts = data.toString();
        ts = String2.isSomething(ts) ? ts : EDStatic.DEFAULT_startHeadHtml;
        if (!ts.startsWith("<!DOCTYPE html>")) {
          String2.log(
              String2.ERROR
                  + " in datasets.xml: <startHeadHtml> must start with \"<!DOCTYPE html>\". Using default <startHeadHtml> instead.");
          ts = EDStatic.DEFAULT_startHeadHtml;
        }
        EDStatic.startHeadHtml = ts; // swap into place

        if (reallyVerbose) {
          String2.log("startHeadHtml5 was set.");
        }
      }
      case "startBodyHtml5" -> {
        String ts = data.toString();
        ts = String2.isSomething(ts) ? ts : EDStatic.DEFAULT_startBodyHtmlAr[0];
        EDStatic.startBodyHtmlAr[0] = ts; // swap into place

        if (reallyVerbose) {
          String2.log("startBodyHtml5 was set.");
        }
      }
      case "theShortDescriptionHtml" -> {
        String ts = data.toString();
        ts = String2.isSomething(ts) ? ts : EDStatic.DEFAULT_theShortDescriptionHtmlAr[0];
        EDStatic.theShortDescriptionHtmlAr[0] = ts; // swap into place

        if (reallyVerbose) {
          String2.log("theShortDescriptionHtml was set.");
        }
      }
      case "endBodyHtml5" -> {
        String ts = data.toString();
        EDStatic.endBodyHtmlAr[0] =
            String2.replaceAll(
                String2.isSomething(ts) ? ts : EDStatic.DEFAULT_endBodyHtmlAr[0],
                "&erddapVersion;",
                EDStatic.erddapVersion);

        if (reallyVerbose) {
          String2.log("endBodyHtml5 was set.");
        }
      }
      case "convertInterpolateRequestCSVExample" -> {
        EDStatic.convertInterpolateRequestCSVExample = data.toString();

        if (reallyVerbose) {
          String2.log("convertInterpolateRequestCSVExample=" + data);
        }
      }
      case "convertInterpolateDatasetIDVariableList" -> {
        String[] sar = StringArray.arrayFromCSV(data.toString());
        EDStatic.convertInterpolateDatasetIDVariableList = sar;

        if (reallyVerbose) {
          String2.log("convertInterpolateDatasetIDVariableList=" + String2.toCSVString(sar));
        }
      }
      case "unusualActivity" -> {
        int tnt = String2.parseInt(data.toString());
        EDStatic.unusualActivity =
            tnt < 1 || tnt == Integer.MAX_VALUE ? EDStatic.DEFAULT_unusualActivity : tnt;

        if (reallyVerbose) {
          String2.log("unusualActivity=" + EDStatic.unusualActivity);
        }
      }
      case "updateMaxEvents" -> {
        int tnt = String2.parseInt(data.toString());
        EDStatic.updateMaxEvents =
            tnt < 1 || tnt == Integer.MAX_VALUE ? EDStatic.DEFAULT_updateMaxEvents : tnt;

        if (reallyVerbose) {
          String2.log("updateMaxEvents=" + EDStatic.updateMaxEvents);
        }
      }
      case "unusualActivityFailPercent" -> {
        int tnt = String2.parseInt(data.toString());
        EDStatic.unusualActivityFailPercent =
            tnt < 0 || tnt > 100 || tnt == Integer.MAX_VALUE
                ? EDStatic.DEFAULT_unusualActivityFailPercent
                : tnt;

        if (reallyVerbose) {
          String2.log("unusualActivityFailPercent" + EDStatic.unusualActivityFailPercent);
        }
      }
    }
  }

  @Override
  public void handleDataset(EDD dataset) {
    context.getErddap().processDataset(dataset, context);
  }

  @Override
  public void popState() {
    String2.log("Attempt to pop top level handler. Something likely went wrong.");
  }
}
