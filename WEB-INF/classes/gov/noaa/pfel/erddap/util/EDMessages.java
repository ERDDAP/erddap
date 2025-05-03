package gov.noaa.pfel.erddap.util;

import com.cohort.array.Attributes;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.Units2;
import com.cohort.util.XML;
import com.google.common.io.Resources;
import gov.noaa.pfel.coastwatch.util.HtmlWidgets;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.TableWriterHtmlTable;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

public class EDMessages {
  // not translated
  public
  String // these are set by setup.xml (deprecated) and/or messages.xml and/or datasets.xml (v2.00+)
      DEFAULT_standardLicense;
  public String standardLicense;
  public String DEFAULT_startHeadHtml; // see xxx() methods
  public String startHeadHtml; // see xxx() methods
  public String legal;
  public String legendTitle1;
  public String legendTitle2;
  public String DEFAULT_palettes[] = null; // set when messages.xml is read
  public Set<String> DEFAULT_palettes_set = null; // set when messages.xml is read
  public String palettes[]; // an array of palettes
  public String palettes0[]; // the array of palettes with a blank [0] item inserted
  public String questionMarkImageFile;

  // NOT TRANSLATED
  public final String admKeywords;

  public final String admSubsetVariables;
  public final String advl_datasetID;
  public final String advr_cdm_data_type;
  public final String advr_class;
  public final String advr_dataStructure;
  public final String EDDChangedWasnt;
  public final String EDDChangedDifferentNVar;
  public final String EDDChanged2Different;
  public final String EDDChanged1Different;
  public final String EDDChangedCGADifferent;
  public final String EDDChangedAxesDifferentNVar;
  public final String EDDChangedAxes2Different;
  public final String EDDChangedAxes1Different;
  public final String EDDChangedNoValue;
  public final String EDDChangedTableToGrid;
  public final String EDDFgdc;
  public final String EDDIso19115;
  public final String EDDSimilarDifferentNVar;
  public final String EDDSimilarDifferent;
  public final String[] extensionsNoRangeRequests; // an array of extensions (not translated)
  public final String inotifyFixCommands;
  public final String sparqlP01toP02pre;
  public final String sparqlP01toP02post;

  public String // the unencoded EDDGrid...Example attributes
      EDDGridErddapUrlExample;
  public String EDDGridIdExample;
  public String EDDGridDimensionExample;
  public String EDDGridNoHyperExample;
  public String EDDGridDimNamesExample;
  public String EDDGridDataTimeExample;
  public String EDDGridDataValueExample;
  public String EDDGridDataIndexExample;
  public String EDDGridGraphExample;
  public String EDDGridMapExample;
  public String EDDGridMatlabPlotExample;

  public String // the unencoded EDDTable...Example attributes
      EDDTableErddapUrlExample;
  public String EDDTableIdExample;
  public String EDDTableVariablesExample;
  public String EDDTableConstraintsExample;
  public String EDDTableDataTimeExample;
  public String EDDTableDataValueExample;
  public String EDDTableGraphExample;
  public String EDDTableMapExample;
  public String EDDTableMatlabPlotExample;

  public String // variants encoded to be Html Examples
      EDDGridDimensionExampleHE;
  public String EDDGridDataIndexExampleHE;
  public String EDDGridDataValueExampleHE;
  public String EDDGridDataTimeExampleHE;
  public String EDDGridGraphExampleHE;
  public String EDDGridMapExampleHE;

  public String // variants encoded to be Html Attributes
      EDDGridDimensionExampleHA;
  public String EDDGridDataIndexExampleHA;
  public String EDDGridDataValueExampleHA;
  public String EDDGridDataTimeExampleHA;
  public String EDDGridGraphExampleHA;
  public String EDDGridMapExampleHA;
  public String EDDTableFromHttpGetDatasetDescription;
  public String EDDTableFromHttpGetAuthorDescription;
  public String EDDTableFromHttpGetTimestampDescription;

  public String // variants encoded to be Html Examples
      EDDTableConstraintsExampleHE;
  public String EDDTableDataTimeExampleHE;
  public String EDDTableDataValueExampleHE;
  public String EDDTableGraphExampleHE;
  public String EDDTableMapExampleHE;

  public String // variants encoded to be Html Attributes
      EDDTableConstraintsExampleHA;
  public String EDDTableDataTimeExampleHA;
  public String EDDTableDataValueExampleHA;
  public String EDDTableGraphExampleHA;
  public String EDDTableMapExampleHA;

  // translated
  public final String
          [] // these are set by setup.xml (deprecated) and/or messages.xml and/or datasets.xml
      // (v2.00+)
      DEFAULT_standardContactAr;
  public final String[] DEFAULT_standardDataLicensesAr;
  public final String[] DEFAULT_standardDisclaimerOfEndorsementAr;
  public final String[] DEFAULT_standardDisclaimerOfExternalLinksAr;
  public final String[] DEFAULT_standardGeneralDisclaimerAr;
  public final String[] DEFAULT_standardPrivacyPolicyAr;
  public final String[] DEFAULT_startBodyHtmlAr;
  public final String[] DEFAULT_theShortDescriptionHtmlAr;
  public final String[] DEFAULT_endBodyHtmlAr;
  public final String[] standardContactAr;
  public final String[] standardDataLicensesAr;
  public final String[] standardDisclaimerOfEndorsementAr;
  public final String[] standardDisclaimerOfExternalLinksAr;
  public final String[] standardGeneralDisclaimerAr;
  public final String[] standardPrivacyPolicyAr;
  public final String[] startBodyHtmlAr;
  public final String[] theShortDescriptionHtmlAr;
  public final String[] endBodyHtmlAr;
  public String[] // in messages.xml and perhaps in datasets.xml (v2.00+)
      commonStandardNames;
  public final String[] DEFAULT_commonStandardNames;

  // TRANSLATED
  private final String[] // private to force use via methods, e.g., acceptEncodingHtml()
      acceptEncodingHtmlAr;
  private final String[] filesDocumentationAr;
  public final String[] accessRESTFULAr;
  public final String[] acronymsAr;
  public final String[] addConstraintsAr;
  public final String[] addVarWhereAttNameAr;
  public final String[] addVarWhereAttValueAr;
  public final String[] addVarWhereAr;
  public final String[] additionalLinksAr;
  public final String[] admSummaryAr;
  public final String[] admTitleAr;
  public final String[] advc_accessibleAr;
  public final String[] advl_accessibleAr;
  public final String[] advl_institutionAr;
  public final String[] advc_dataStructureAr;
  public final String[] advl_dataStructureAr;
  public final String[] advl_cdm_data_typeAr;
  public final String[] advl_classAr;
  public final String[] advl_titleAr;
  public final String[] advl_minLongitudeAr;
  public final String[] advl_maxLongitudeAr;
  public final String[] advl_longitudeSpacingAr;
  public final String[] advl_minLatitudeAr;
  public final String[] advl_maxLatitudeAr;
  public final String[] advl_latitudeSpacingAr;
  public final String[] advl_minAltitudeAr;
  public final String[] advl_maxAltitudeAr;
  public final String[] advl_minTimeAr;
  public final String[] advc_maxTimeAr;
  public final String[] advl_maxTimeAr;
  public final String[] advl_timeSpacingAr;
  public final String[] advc_griddapAr;
  public final String[] advl_griddapAr;
  public final String[] advl_subsetAr;
  public final String[] advc_tabledapAr;
  public final String[] advl_tabledapAr;
  public final String[] advl_MakeAGraphAr;
  public final String[] advc_sosAr;
  public final String[] advl_sosAr;
  public final String[] advl_wcsAr;
  public final String[] advl_wmsAr;
  public final String[] advc_filesAr;
  public final String[] advl_filesAr;
  public final String[] advc_fgdcAr;
  public final String[] advl_fgdcAr;
  public final String[] advc_iso19115Ar;
  public final String[] advl_iso19115Ar;
  public final String[] advc_metadataAr;
  public final String[] advl_metadataAr;
  public final String[] advl_sourceUrlAr;
  public final String[] advl_infoUrlAr;
  public final String[] advl_rssAr;
  public final String[] advc_emailAr;
  public final String[] advl_emailAr;
  public final String[] advl_summaryAr;
  public final String[] advc_testOutOfDateAr;
  public final String[] advl_testOutOfDateAr;
  public final String[] advc_outOfDateAr;
  public final String[] advl_outOfDateAr;
  public final String[] advn_outOfDateAr;
  public final String[] advancedSearchAr;
  public final String[] advancedSearchResultsAr;
  public final String[] advancedSearchDirectionsAr;
  public final String[] advancedSearchTooltipAr;
  public final String[] advancedSearchBoundsAr;
  public final String[] advancedSearchMinLatAr;
  public final String[] advancedSearchMaxLatAr;
  public final String[] advancedSearchMinLonAr;
  public final String[] advancedSearchMaxLonAr;
  public final String[] advancedSearchMinMaxLonAr;
  public final String[] advancedSearchMinTimeAr;
  public final String[] advancedSearchMaxTimeAr;
  public final String[] advancedSearchClearAr;
  public final String[] advancedSearchClearHelpAr;
  public final String[] advancedSearchCategoryTooltipAr;
  public final String[] advancedSearchRangeTooltipAr;
  public final String[] advancedSearchMapTooltipAr;
  public final String[] advancedSearchLonTooltipAr;
  public final String[] advancedSearchTimeTooltipAr;
  public final String[] advancedSearchWithCriteriaAr;
  public final String[] advancedSearchFewerCriteriaAr;
  public final String[] advancedSearchNoCriteriaAr;
  public final String[] advancedSearchErrorHandlingAr;
  public final String[] autoRefreshAr;
  public final String[] blacklistMsgAr;
  public final String[] BroughtToYouByAr;
  public final String[] categoryTitleHtmlAr;
  public final String[] categoryHtmlAr;
  public final String[] category3HtmlAr;
  public final String[] categoryPickAttributeAr;
  public final String[] categorySearchHtmlAr;
  public final String[] categorySearchDifferentHtmlAr;
  public final String[] categoryClickHtmlAr;
  public final String[] categoryNotAnOptionAr;
  public final String[] caughtInterruptedAr;
  public final String[] cdmDataTypeHelpAr;
  public final String[] clickAccessAr;
  public final String[] clickBackgroundInfoAr;
  public final String[] clickERDDAPAr;
  public final String[] clickInfoAr;
  public final String[] clickToSubmitAr;
  public final String[] convertAr;
  public final String[] convertBypassAr;
  public final String[] convertCOLORsAr;
  public final String[] convertCOLORsMessageAr;
  public final String[] convertToAFullNameAr;
  public final String[] convertToAnAcronymAr;
  public final String[] convertToACountyNameAr;
  public final String[] convertToAFIPSCodeAr;
  public final String[] convertToGCMDAr;
  public final String[] convertToCFStandardNamesAr;
  public final String[] convertToNumericTimeAr;
  public final String[] convertToStringTimeAr;
  public final String[] convertAnyStringTimeAr;
  public final String[] convertToProperTimeUnitsAr;
  public final String[] convertFromUDUNITSToUCUMAr;
  public final String[] convertFromUCUMToUDUNITSAr;
  public final String[] convertToUCUMAr;
  public final String[] convertToUDUNITSAr;
  public final String[] convertStandardizeUDUNITSAr;
  public final String[] convertToFullNameAr;
  public final String[] convertToVariableNameAr;
  public final String[] converterWebServiceAr;
  public final String[] convertOAAcronymsAr;
  public final String[] convertOAAcronymsToFromAr;
  public final String[] convertOAAcronymsIntroAr;
  public final String[] convertOAAcronymsNotesAr;
  public final String[] convertOAAcronymsServiceAr;
  public final String[] convertOAVariableNamesAr;
  public final String[] convertOAVariableNamesToFromAr;
  public final String[] convertOAVariableNamesIntroAr;
  public final String[] convertOAVariableNamesNotesAr;
  public final String[] convertOAVariableNamesServiceAr;
  public final String[] convertFipsCountyAr;
  public final String[] convertFipsCountyIntroAr;
  public final String[] convertFipsCountyNotesAr;
  public final String[] convertFipsCountyServiceAr;
  public final String[] convertHtmlAr;
  public final String[] convertInterpolateAr;
  public final String[] convertInterpolateIntroAr;
  public final String[] convertInterpolateTLLTableAr;
  public final String[] convertInterpolateTLLTableHelpAr;
  public final String[] convertInterpolateDatasetIDVariableAr;
  public final String[] convertInterpolateDatasetIDVariableHelpAr;
  public final String[] convertInterpolateNotesAr;
  public final String[] convertInterpolateServiceAr;
  public final String[] convertKeywordsAr;
  public final String[] convertKeywordsCfTooltipAr;
  public final String[] convertKeywordsGcmdTooltipAr;
  public final String[] convertKeywordsIntroAr;
  public final String[] convertKeywordsNotesAr;
  public final String[] convertKeywordsServiceAr;
  public final String[] convertTimeAr;
  public final String[] convertTimeReferenceAr;
  public final String[] convertTimeIntroAr;
  public final String[] convertTimeNotesAr;
  public final String[] convertTimeServiceAr;
  public final String[] convertTimeNumberTooltipAr;
  public final String[] convertTimeStringTimeTooltipAr;
  public final String[] convertTimeUnitsTooltipAr;
  public final String[] convertTimeUnitsHelpAr;
  public final String[] convertTimeIsoFormatErrorAr;
  public final String[] convertTimeNoSinceErrorAr;
  public final String[] convertTimeNumberErrorAr;
  public final String[] convertTimeNumericTimeErrorAr;
  public final String[] convertTimeParametersErrorAr;
  public final String[] convertTimeStringFormatErrorAr;
  public final String[] convertTimeTwoTimeErrorAr;
  public final String[] convertTimeUnitsErrorAr;
  public final String[] convertUnitsAr;
  public final String[] convertUnitsComparisonAr;
  public final String[] convertUnitsFilterAr;
  public final String[] convertUnitsIntroAr;
  public final String[] convertUnitsNotesAr;
  public final String[] convertUnitsServiceAr;
  public final String[] convertURLsAr;
  public final String[] convertURLsIntroAr;
  public final String[] convertURLsNotesAr;
  public final String[] convertURLsServiceAr;
  public final String[] cookiesHelpAr;
  public final String[] copyImageToClipboardAr;
  public final String[] copyTextToClipboardAr;
  public final String[] copyToClipboardNotAvailableAr;
  public final String[] dafAr;
  public final String[] dafGridBypassTooltipAr;
  public final String[] dafGridTooltipAr;
  public final String[] dafTableBypassTooltipAr;
  public final String[] dafTableTooltipAr;
  public final String[] dasTitleAr;
  public final String[] dataAccessNotAllowedAr;
  public final String[] databaseUnableToConnectAr;
  public final String[] dataProviderFormAr;
  public final String[] dataProviderFormP1Ar;
  public final String[] dataProviderFormP2Ar;
  public final String[] dataProviderFormP3Ar;
  public final String[] dataProviderFormP4Ar;
  public final String[] dataProviderFormDoneAr;
  public final String[] dataProviderFormSuccessAr;
  public final String[] dataProviderFormShortDescriptionAr;
  public final String[] dataProviderFormLongDescriptionHTMLAr;
  public final String[] dataProviderFormPart1Ar;
  public final String[] dataProviderFormPart2HeaderAr;
  public final String[] dataProviderFormPart2GlobalMetadataAr;
  public final String[] dataProviderContactInfoAr;
  public final String[] dataProviderDataAr;
  public final String[] documentationAr;
  public final String[] dpf_submitAr;
  public final String[] dpf_fixProblemAr;
  public final String[] dpf_yourNameAr;
  public final String[] dpf_emailAddressAr;
  public final String[] dpf_TimestampAr;
  public final String[] dpf_frequencyAr;
  public final String[] dpf_titleAr;
  public final String[] dpf_titleTooltipAr;
  public final String[] dpf_summaryAr;
  public final String[] dpf_summaryTooltipAr;
  public final String[] dpf_creatorNameAr;
  public final String[] dpf_creatorNameTooltipAr;
  public final String[] dpf_creatorTypeAr;
  public final String[] dpf_creatorTypeTooltipAr;
  public final String[] dpf_creatorEmailAr;
  public final String[] dpf_creatorEmailTooltipAr;
  public final String[] dpf_institutionAr;
  public final String[] dpf_institutionTooltipAr;
  public final String[] dpf_infoUrlAr;
  public final String[] dpf_infoUrlTooltipAr;
  public final String[] dpf_licenseAr;
  public final String[] dpf_licenseTooltipAr;
  public final String[] dpf_howYouStoreDataAr;
  public final String[] dpf_provideIfAvailableAr;
  public final String[] dpf_acknowledgementAr;
  public final String[] dpf_acknowledgementTooltipAr;
  public final String[] dpf_historyAr;
  public final String[] dpf_historyTooltipAr;
  public final String[] dpf_idTooltipAr;
  public final String[] dpf_namingAuthorityAr;
  public final String[] dpf_namingAuthorityTooltipAr;
  public final String[] dpf_productVersionAr;
  public final String[] dpf_productVersionTooltipAr;
  public final String[] dpf_referencesAr;
  public final String[] dpf_referencesTooltipAr;
  public final String[] dpf_commentAr;
  public final String[] dpf_commentTooltipAr;
  public final String[] dpf_dataTypeHelpAr;
  public final String[] dpf_ioosCategoryAr;
  public final String[] dpf_ioosCategoryHelpAr;
  public final String[] dpf_part3HeaderAr;
  public final String[] dpf_variableMetadataAr;
  public final String[] dpf_sourceNameAr;
  public final String[] dpf_sourceNameTooltipAr;
  public final String[] dpf_destinationNameAr;
  public final String[] dpf_destinationNameTooltipAr;
  public final String[] dpf_longNameAr;
  public final String[] dpf_longNameTooltipAr;
  public final String[] dpf_standardNameAr;
  public final String[] dpf_standardNameTooltipAr;
  public final String[] dpf_dataTypeAr;
  public final String[] dpf_fillValueAr;
  public final String[] dpf_fillValueTooltipAr;
  public final String[] dpf_unitsAr;
  public final String[] dpf_unitsTooltipAr;
  public final String[] dpf_rangeAr;
  public final String[] dpf_rangeTooltipAr;
  public final String[] dpf_part4HeaderAr;
  public final String[] dpf_otherCommentAr;
  public final String[] dpf_finishPart4Ar;
  public final String[] dpf_congratulationAr;
  public final String[] disabledAr;
  public final String[] distinctValuesTooltipAr;
  public final String[] doWithGraphsAr;
  public final String[] dtAccessibleAr;
  public final String[] dtAccessiblePublicAr;
  public final String[] dtAccessibleYesAr;
  public final String[] dtAccessibleGraphsAr;
  public final String[] dtAccessibleNoAr;
  public final String[] dtAccessibleLogInAr;
  public final String[] dtLogInAr;
  public final String[] dtDAFAr;
  public final String[] dtFilesAr;
  public final String[] dtMAGAr;
  public final String[] dtSOSAr;
  public final String[] dtSubsetAr;
  public final String[] dtWCSAr;
  public final String[] dtWMSAr;
  public final String[] EasierAccessToScientificDataAr;
  public final String[] EDDDatasetIDAr;
  public final String[] EDDFgdcMetadataAr;
  public final String[] EDDFilesAr;
  public final String[] EDDIso19115MetadataAr;
  public final String[] EDDMetadataAr;
  public final String[] EDDBackgroundAr;
  public final String[] EDDClickOnSubmitHtmlAr;
  public final String[] EDDInstitutionAr;
  public final String[] EDDInformationAr;
  public final String[] EDDSummaryAr;
  public final String[] EDDDatasetTitleAr;
  public final String[] EDDDownloadDataAr;
  public final String[] EDDMakeAGraphAr;
  public final String[] EDDMakeAMapAr;
  public final String[] EDDFileTypeAr;
  public final String[] EDDFileTypeInformationAr;
  public final String[] EDDSelectFileTypeAr;
  public final String[] EDDMinimumAr;
  public final String[] EDDMaximumAr;
  public final String[] EDDConstraintAr;
  public final String[] EDDGridDapDescriptionAr;
  public final String[] EDDGridDapLongDescriptionAr;
  public final String[] EDDGridDownloadDataTooltipAr;
  public final String[] EDDGridDimensionAr;
  public final String[] EDDGridDimensionRangesAr;
  public final String[] EDDGridFirstAr;
  public final String[] EDDGridLastAr;
  public final String[] EDDGridStartAr;
  public final String[] EDDGridStopAr;
  public final String[] EDDGridStartStopTooltipAr;
  public final String[] EDDGridStrideAr;
  public final String[] EDDGridNValuesAr;
  public final String[] EDDGridNValuesHtmlAr;
  public final String[] EDDGridSpacingAr;
  public final String[] EDDGridJustOneValueAr;
  public final String[] EDDGridEvenAr;
  public final String[] EDDGridUnevenAr;
  public final String[] EDDGridDimensionTooltipAr;
  public final String[] EDDGridDimensionFirstTooltipAr;
  public final String[] EDDGridDimensionLastTooltipAr;
  public final String[] EDDGridVarHasDimTooltipAr;
  public final String[] EDDGridSSSTooltipAr;
  public final String[] EDDGridStartTooltipAr;
  public final String[] EDDGridStopTooltipAr;
  public final String[] EDDGridStrideTooltipAr;
  public final String[] EDDGridSpacingTooltipAr;
  public final String[] EDDGridDownloadTooltipAr;
  public final String[] EDDGridGridVariableHtmlAr;
  public final String[] EDDGridCheckAllAr;
  public final String[] EDDGridCheckAllTooltipAr;
  public final String[] EDDGridUncheckAllAr;
  public final String[] EDDGridUncheckAllTooltipAr;
  public final String[] EDDTableConstraintsAr;
  public final String[] EDDTableTabularDatasetTooltipAr;
  public final String[] EDDTableVariableAr;
  public final String[] EDDTableCheckAllAr;
  public final String[] EDDTableCheckAllTooltipAr;
  public final String[] EDDTableUncheckAllAr;
  public final String[] EDDTableUncheckAllTooltipAr;
  public final String[] EDDTableMinimumTooltipAr;
  public final String[] EDDTableMaximumTooltipAr;
  public final String[] EDDTableCheckTheVariablesAr;
  public final String[] EDDTableSelectAnOperatorAr;
  public final String[] EDDTableFromEDDGridSummaryAr;
  public final String[] EDDTableOptConstraint1HtmlAr;
  public final String[] EDDTableOptConstraint2HtmlAr;
  public final String[] EDDTableOptConstraintVarAr;
  public final String[] EDDTableNumericConstraintTooltipAr;
  public final String[] EDDTableStringConstraintTooltipAr;
  public final String[] EDDTableTimeConstraintTooltipAr;
  public final String[] EDDTableConstraintTooltipAr;
  public final String[] EDDTableSelectConstraintTooltipAr;
  public final String[] EDDTableDapDescriptionAr;
  public final String[] EDDTableDapLongDescriptionAr;
  public final String[] EDDTableDownloadDataTooltipAr;
  public final String[] erddapIsAr;
  public final String[] erddapVersionHTMLAr;
  public final String[] errorTitleAr;
  public final String[] errorRequestUrlAr;
  public final String[] errorRequestQueryAr;
  public final String[] errorTheErrorAr;
  public final String[] errorCopyFromAr;
  public final String[] errorFileNotFoundAr;
  public final String[] errorFileNotFoundImageAr;
  public final String[] errorInternalAr;
  public final String[] errorJsonpFunctionNameAr;
  public final String[] errorJsonpNotAllowedAr;
  public final String[] errorMoreThan2GBAr;
  public final String[] errorNotFoundAr;
  public final String[] errorNotFoundInAr;
  public final String[] errorOdvLLTGridAr;
  public final String[] errorOdvLLTTableAr;
  public final String[] errorOnWebPageAr;
  public final String[] externalLinkAr;
  public final String[] externalWebSiteAr;
  public final String[] fileHelp_ascAr;
  public final String[] fileHelp_csvAr;
  public final String[] fileHelp_csvpAr;
  public final String[] fileHelp_csv0Ar;
  public final String[] fileHelp_dataTableAr;
  public final String[] fileHelp_dasAr;
  public final String[] fileHelp_ddsAr;
  public final String[] fileHelp_dodsAr;
  public final String[] fileHelpGrid_esriAsciiAr;
  public final String[] fileHelpTable_esriCsvAr;
  public final String[] fileHelp_fgdcAr;
  public final String[] fileHelp_geoJsonAr;
  public final String[] fileHelp_graphAr;
  public final String[] fileHelpGrid_helpAr;
  public final String[] fileHelpTable_helpAr;
  public final String[] fileHelp_htmlAr;
  public final String[] fileHelp_htmlTableAr;
  public final String[] fileHelp_iso19115Ar;
  public final String[] fileHelp_itxGridAr;
  public final String[] fileHelp_itxTableAr;
  public final String[] fileHelp_jsonAr;
  public final String[] fileHelp_jsonlCSV1Ar;
  public final String[] fileHelp_jsonlCSVAr;
  public final String[] fileHelp_jsonlKVPAr;
  public final String[] fileHelp_matAr;
  public final String[] fileHelpGrid_nc3Ar;
  public final String[] fileHelpGrid_nc4Ar;
  public final String[] fileHelpTable_nc3Ar;
  public final String[] fileHelpTable_nc4Ar;
  public final String[] fileHelp_nc3HeaderAr;
  public final String[] fileHelp_nc4HeaderAr;
  public final String[] fileHelp_nccsvAr;
  public final String[] fileHelp_nccsvMetadataAr;
  public final String[] fileHelp_ncCFAr;
  public final String[] fileHelp_ncCFHeaderAr;
  public final String[] fileHelp_ncCFMAAr;
  public final String[] fileHelp_ncCFMAHeaderAr;
  public final String[] fileHelp_ncmlAr;
  public final String[] fileHelp_ncoJsonAr;
  public final String[] fileHelpGrid_odvTxtAr;
  public final String[] fileHelpTable_odvTxtAr;
  public final String[] fileHelp_parquetAr;
  public final String[] fileHelp_parquet_with_metaAr;
  public final String[] fileHelp_subsetAr;
  public final String[] fileHelp_timeGapsAr;
  public final String[] fileHelp_tsvAr;
  public final String[] fileHelp_tsvpAr;
  public final String[] fileHelp_tsv0Ar;
  public final String[] fileHelp_wavAr;
  public final String[] fileHelp_xhtmlAr;
  public final String[] fileHelp_geotifAr; // graphical
  public final String[] fileHelpGrid_kmlAr;
  public final String[] fileHelpTable_kmlAr;
  public final String[] fileHelp_smallPdfAr;
  public final String[] fileHelp_pdfAr;
  public final String[] fileHelp_largePdfAr;
  public final String[] fileHelp_smallPngAr;
  public final String[] fileHelp_pngAr;
  public final String[] fileHelp_largePngAr;
  public final String[] fileHelp_transparentPngAr;
  public final String[] filesDescriptionAr;
  public final String[] filesSortAr;
  public final String[] filesWarningAr;
  public final String[] findOutChangeAr;
  public final String[] FIPSCountyCodesAr;
  public final String[] forSOSUseAr;
  public final String[] forWCSUseAr;
  public final String[] forWMSUseAr;
  public final String[] functionsAr;
  public final String[] functionTooltipAr;
  public final String[] functionDistinctCheckAr;
  public final String[] functionDistinctTooltipAr;
  public final String[] functionOrderByExtraAr;
  public final String[] functionOrderByTooltipAr;
  public final String[] functionOrderBySortAr;
  public final String[] functionOrderBySort1Ar;
  public final String[] functionOrderBySort2Ar;
  public final String[] functionOrderBySort3Ar;
  public final String[] functionOrderBySort4Ar;
  public final String[] functionOrderBySortLeastAr;
  public final String[] functionOrderBySortRowMaxAr;
  public final String[] generatedAtAr;
  public final String[] geoServicesDescriptionAr;
  public final String[] getStartedHtmlAr;
  public final String[] helpAr;
  public final String[] htmlTableMaxMessageAr;
  public final String[] imageDataCourtesyOfAr;
  public final String[] imagesEmbedAr;
  public final String[] indexViewAllAr;
  public final String[] indexSearchWithAr;
  public final String[] indexDevelopersSearchAr;
  public final String[] indexProtocolAr;
  public final String[] indexDescriptionAr;
  public final String[] indexDatasetsAr;
  public final String[] indexDocumentationAr;
  public final String[] indexRESTfulSearchAr;
  public final String[] indexAllDatasetsSearchAr;
  public final String[] indexOpenSearchAr;
  public final String[] indexServicesAr;
  public final String[] indexDescribeServicesAr;
  public final String[] indexMetadataAr;
  public final String[] indexWAF1Ar;
  public final String[] indexWAF2Ar;
  public final String[] indexConvertersAr;
  public final String[] indexDescribeConvertersAr;
  public final String[] infoAboutFromAr;
  public final String[] infoTableTitleHtmlAr;
  public final String[] infoRequestFormAr;
  public final String[] informationAr;
  public final String[] inotifyFixAr;
  public final String[] interpolateAr;
  public final String[] javaProgramsHTMLAr;
  public final String[] justGenerateAndViewAr;
  public final String[] justGenerateAndViewTooltipAr;
  public final String[] justGenerateAndViewUrlAr;
  public final String[] justGenerateAndViewGraphUrlTooltipAr;
  public final String[] keywordsAr;
  public final String[] langCodeAr;
  public final String[] legalNoticesAr;
  public final String[] legalNoticesTitleAr;
  public final String[] licenseAr;
  public final String[] likeThisAr;
  public final String[] listAllAr;
  public final String[] listOfDatasetsAr;
  public final String[] LogInAr;
  public final String[] loginAr;
  public final String[] loginHTMLAr;
  public final String[] loginAttemptBlockedAr;
  public final String[] loginDescribeCustomAr;
  public final String[] loginDescribeEmailAr;
  public final String[] loginDescribeGoogleAr;
  public final String[] loginDescribeOrcidAr;
  public final String[] loginDescribeOauth2Ar;
  public final String[] loginErddapAr;
  public final String[] loginCanNotAr;
  public final String[] loginAreNotAr;
  public final String[] loginToLogInAr;
  public final String[] loginEmailAddressAr;
  public final String[] loginYourEmailAddressAr;
  public final String[] loginUserNameAr;
  public final String[] loginPasswordAr;
  public final String[] loginUserNameAndPasswordAr;
  public final String[] loginGoogleSignInAr;
  public final String[] loginOrcidSignInAr;
  public final String[] loginOpenIDAr;
  public final String[] loginOpenIDOrAr;
  public final String[] loginOpenIDCreateAr;
  public final String[] loginOpenIDFreeAr;
  public final String[] loginOpenIDSameAr;
  public final String[] loginAsAr;
  public final String[] loginPartwayAsAr;
  public final String[] loginFailedAr;
  public final String[] loginSucceededAr;
  public final String[] loginInvalidAr;
  public final String[] loginNotAr;
  public final String[] loginBackAr;
  public final String[] loginProblemExactAr;
  public final String[] loginProblemExpireAr;
  public final String[] loginProblemGoogleAgainAr;
  public final String[] loginProblemOrcidAgainAr;
  public final String[] loginProblemOauth2AgainAr;
  public final String[] loginProblemSameBrowserAr;
  public final String[] loginProblem3TimesAr;
  public final String[] loginProblemsAr;
  public final String[] loginProblemsAfterAr;
  public final String[] loginPublicAccessAr;
  public final String[] LogOutAr;
  public final String[] logoutAr;
  public final String[] logoutOpenIDAr;
  public final String[] logoutSuccessAr;
  public final String[] magAr;
  public final String[] magAxisXAr;
  public final String[] magAxisYAr;
  public final String[] magAxisColorAr;
  public final String[] magAxisStickXAr;
  public final String[] magAxisStickYAr;
  public final String[] magAxisVectorXAr;
  public final String[] magAxisVectorYAr;
  public final String[] magAxisHelpGraphXAr;
  public final String[] magAxisHelpGraphYAr;
  public final String[] magAxisHelpMarkerColorAr;
  public final String[] magAxisHelpSurfaceColorAr;
  public final String[] magAxisHelpStickXAr;
  public final String[] magAxisHelpStickYAr;
  public final String[] magAxisHelpMapXAr;
  public final String[] magAxisHelpMapYAr;
  public final String[] magAxisHelpVectorXAr;
  public final String[] magAxisHelpVectorYAr;
  public final String[] magAxisVarHelpAr;
  public final String[] magAxisVarHelpGridAr;
  public final String[] magConstraintHelpAr;
  public final String[] magDocumentationAr;
  public final String[] magDownloadAr;
  public final String[] magDownloadTooltipAr;
  public final String[] magFileTypeAr;
  public final String[] magGraphTypeAr;
  public final String[] magGraphTypeTooltipGridAr;
  public final String[] magGraphTypeTooltipTableAr;
  public final String[] magGSAr;
  public final String[] magGSMarkerTypeAr;
  public final String[] magGSSizeAr;
  public final String[] magGSColorAr;
  public final String[] magGSColorBarAr;
  public final String[] magGSColorBarTooltipAr;
  public final String[] magGSContinuityAr;
  public final String[] magGSContinuityTooltipAr;
  public final String[] magGSScaleAr;
  public final String[] magGSScaleTooltipAr;
  public final String[] magGSMinAr;
  public final String[] magGSMinTooltipAr;
  public final String[] magGSMaxAr;
  public final String[] magGSMaxTooltipAr;
  public final String[] magGSNSectionsAr;
  public final String[] magGSNSectionsTooltipAr;
  public final String[] magGSLandMaskAr;
  public final String[] magGSLandMaskTooltipGridAr;
  public final String[] magGSLandMaskTooltipTableAr;
  public final String[] magGSVectorStandardAr;
  public final String[] magGSVectorStandardTooltipAr;
  public final String[] magGSYAscendingTooltipAr;
  public final String[] magGSYAxisMinAr;
  public final String[] magGSYAxisMaxAr;
  public final String[] magGSYRangeMinTooltipAr;
  public final String[] magGSYRangeMaxTooltipAr;
  public final String[] magGSYRangeTooltipAr;
  public final String[] magGSYScaleTooltipAr;
  public final String[] magItemFirstAr;
  public final String[] magItemPreviousAr;
  public final String[] magItemNextAr;
  public final String[] magItemLastAr;
  public final String[] magJust1ValueAr;
  public final String[] magRangeAr;
  public final String[] magRangeToAr;
  public final String[] magRedrawAr;
  public final String[] magRedrawTooltipAr;
  public final String[] magTimeRangeAr;
  public final String[] magTimeRangeFirstAr;
  public final String[] magTimeRangeBackAr;
  public final String[] magTimeRangeForwardAr;
  public final String[] magTimeRangeLastAr;
  public final String[] magTimeRangeTooltipAr;
  public final String[] magTimeRangeTooltip2Ar;
  public final String[] magTimesVaryAr;
  public final String[] magViewUrlAr;
  public final String[] magZoomAr;
  public final String[] magZoomCenterAr;
  public final String[] magZoomCenterTooltipAr;
  public final String[] magZoomInAr;
  public final String[] magZoomInTooltipAr;
  public final String[] magZoomOutAr;
  public final String[] magZoomOutTooltipAr;
  public final String[] magZoomALittleAr;
  public final String[] magZoomDataAr;
  public final String[] magZoomOutDataAr;
  public final String[] magGridTooltipAr;
  public final String[] magTableTooltipAr;
  public final String[] metadataDownloadAr;
  public final String[] moreInformationAr;
  public final String[] nMatching1Ar;
  public final String[] nMatchingAr;
  public final String[] nMatchingAlphabeticalAr;
  public final String[] nMatchingMostRelevantAr;
  public final String[] nMatchingPageAr;
  public final String[] nMatchingCurrentAr;
  public final String[] noDataFixedValueAr;
  public final String[] noDataNoLLAr;
  public final String[] noDatasetWithAr;
  public final String[] noPage1Ar;
  public final String[] noPage2Ar;
  public final String[] notAllowedAr;
  public final String[] notAuthorizedAr;
  public final String[] notAuthorizedForDataAr;
  public final String[] notAvailableAr;
  public final String[] noteAr;
  public final String[] noXxxAr;
  public final String[] noXxxBecauseAr;
  public final String[] noXxxBecause2Ar;
  public final String[] noXxxNotActiveAr;
  public final String[] noXxxNoAxis1Ar;
  public final String[] noXxxNoColorBarAr;
  public final String[] noXxxNoCdmDataTypeAr;
  public final String[] noXxxNoLLAr;
  public final String[] noXxxNoLLEvenlySpacedAr;
  public final String[] noXxxNoLLGt1Ar;
  public final String[] noXxxNoLLTAr;
  public final String[] noXxxNoLonIn180Ar;
  public final String[] noXxxNoNonStringAr;
  public final String[] noXxxNo2NonStringAr;
  public final String[] noXxxNoStationAr;
  public final String[] noXxxNoStationIDAr;
  public final String[] noXxxNoSubsetVariablesAr;
  public final String[] noXxxNoOLLSubsetVariablesAr;
  public final String[] noXxxNoMinMaxAr;
  public final String[] noXxxItsGriddedAr;
  public final String[] noXxxItsTabularAr;
  public final String[] oneRequestAtATimeAr;
  public final String[] openSearchDescriptionAr;
  public final String[] optionalAr;
  public final String[] optionsAr;
  public final String[] orAListOfValuesAr;
  public final String[] orRefineSearchWithAr;
  public final String[] orSearchWithAr;
  public final String[] orCommaAr;
  public final String[] otherFeaturesAr;
  public final String[] outOfDateDatasetsAr;
  public final String[] outOfDateKeepTrackAr;
  public final String[] outOfDateHtmlAr;
  public final String[] patientDataAr;
  public final String[] patientYourGraphAr;
  public final String[] percentEncodeAr;
  public final String[] pickADatasetAr;
  public final String[] protocolSearchHtmlAr;
  public final String[] protocolSearch2HtmlAr;
  public final String[] protocolClickAr;
  public final String[] queryErrorAr;
  public final String[] queryError180Ar;
  public final String[] queryError1ValueAr;
  public final String[] queryError1VarAr;
  public final String[] queryError2VarAr;
  public final String[] queryErrorActualRangeAr;
  public final String[] queryErrorAdjustedAr;
  public final String[] queryErrorAscendingAr;
  public final String[] queryErrorConstraintNaNAr;
  public final String[] queryErrorEqualSpacingAr;
  public final String[] queryErrorExpectedAtAr;
  public final String[] queryErrorFileTypeAr;
  public final String[] queryErrorInvalidAr;
  public final String[] queryErrorLLAr;
  public final String[] queryErrorLLGt1Ar;
  public final String[] queryErrorLLTAr;
  public final String[] queryErrorNeverTrueAr;
  public final String[] queryErrorNeverBothTrueAr;
  public final String[] queryErrorNotAxisAr;
  public final String[] queryErrorNotExpectedAtAr;
  public final String[] queryErrorNotFoundAfterAr;
  public final String[] queryErrorOccursTwiceAr;
  public final String[] queryErrorOrderByClosestAr;
  public final String[] queryErrorOrderByLimitAr;
  public final String[] queryErrorOrderByMeanAr;
  public final String[] queryErrorOrderBySumAr;
  public final String[] queryErrorOrderByVariableAr;
  public final String[] queryErrorUnknownVariableAr;
  public final String[] queryErrorGrid1AxisAr;
  public final String[] queryErrorGridAmpAr;
  public final String[] queryErrorGridDiagnosticAr;
  public final String[] queryErrorGridBetweenAr;
  public final String[] queryErrorGridLessMinAr;
  public final String[] queryErrorGridGreaterMaxAr;
  public final String[] queryErrorGridMissingAr;
  public final String[] queryErrorGridNoAxisVarAr;
  public final String[] queryErrorGridNoDataVarAr;
  public final String[] queryErrorGridNotIdenticalAr;
  public final String[] queryErrorGridSLessSAr;
  public final String[] queryErrorLastEndPAr;
  public final String[] queryErrorLastExpectedAr;
  public final String[] queryErrorLastUnexpectedAr;
  public final String[] queryErrorLastPMInvalidAr;
  public final String[] queryErrorLastPMIntegerAr;
  public final String[] rangesFromToAr;
  public final String[] requiredAr;
  public final String[] resetTheFormAr;
  public final String[] resetTheFormWasAr;
  public final String[] resourceNotFoundAr;
  public final String[] restfulWebServicesAr;
  public final String[] restfulHTMLAr;
  public final String[] restfulHTMLContinuedAr;
  public final String[] restfulGetAllDatasetAr;
  public final String[] restfulProtocolsAr;
  public final String[] SOSDocumentationAr;
  public final String[] WCSDocumentationAr;
  public final String[] WMSDocumentationAr;
  public final String[] requestFormatExamplesHtmlAr;
  public final String[] resultsFormatExamplesHtmlAr;
  public final String[] resultsOfSearchForAr;
  public final String[] restfulInformationFormatsAr;
  public final String[] restfulViaServiceAr;
  public final String[] rowsAr;
  public final String[] rssNoAr;
  public final String[] searchTitleAr;
  public final String[] searchDoFullTextHtmlAr;
  public final String[] searchFullTextHtmlAr;
  public final String[] searchHintsLuceneTooltipAr;
  public final String[] searchHintsOriginalTooltipAr;
  public final String[] searchHintsTooltipAr;
  public final String[] searchButtonAr;
  public final String[] searchClickTipAr;
  public final String[] searchMultipleERDDAPsAr;
  public final String[] searchMultipleERDDAPsDescriptionAr;
  public final String[] searchNotAvailableAr;
  public final String[] searchTipAr;
  public final String[] searchSpellingAr;
  public final String[] searchFewerWordsAr;
  public final String[] searchWithQueryAr;
  public final String[] seeProtocolDocumentationAr;
  public final String[] selectNextAr;
  public final String[] selectPreviousAr;
  public final String[] shiftXAllTheWayLeftAr;
  public final String[] shiftXLeftAr;
  public final String[] shiftXRightAr;
  public final String[] shiftXAllTheWayRightAr;
  public final String[] slideSorterAr;
  public final String[] SOSAr;
  public final String[] sosDescriptionHtmlAr;
  public final String[] sosLongDescriptionHtmlAr;
  public final String[] sosOverview1Ar;
  public final String[] sosOverview2Ar;
  public final String[] ssUseAr;
  public final String[] ssUsePlainAr;
  public final String[] ssBePatientAr;
  public final String[] ssInstructionsHtmlAr;
  public final String[] standardShortDescriptionHtmlAr;
  public final String[] statusAr;
  public final String[] statusHtmlAr;
  public final String[] submitAr;
  public final String[] submitTooltipAr;
  public final String[] subscriptionOfferRssAr;
  public final String[] subscriptionOfferUrlAr;
  public final String[] subscriptionsTitleAr;
  public final String[] subscriptionEmailListAr;
  public final String[] subscriptionAddAr;
  public final String[] subscriptionAddHtmlAr;
  public final String[] subscriptionValidateAr;
  public final String[] subscriptionValidateHtmlAr;
  public final String[] subscriptionListAr;
  public final String[] subscriptionListHtmlAr;
  public final String[] subscriptionRemoveAr;
  public final String[] subscriptionRemoveHtmlAr;
  public final String[] subscriptionAbuseAr;
  public final String[] subscriptionAddErrorAr;
  public final String[] subscriptionAdd2Ar;
  public final String[] subscriptionAddSuccessAr;
  public final String[] subscriptionEmailAr;
  public final String[] subscriptionEmailOnBlacklistAr;
  public final String[] subscriptionEmailInvalidAr;
  public final String[] subscriptionEmailTooLongAr;
  public final String[] subscriptionEmailUnspecifiedAr;
  public final String[] subscription0HtmlAr;
  public final String[] subscription1HtmlAr;
  public final String[] subscription2HtmlAr;
  public final String[] subscriptionIDInvalidAr;
  public final String[] subscriptionIDTooLongAr;
  public final String[] subscriptionIDUnspecifiedAr;
  public final String[] subscriptionKeyInvalidAr;
  public final String[] subscriptionKeyUnspecifiedAr;
  public final String[] subscriptionListErrorAr;
  public final String[] subscriptionListSuccessAr;
  public final String[] subscriptionRemoveErrorAr;
  public final String[] subscriptionRemove2Ar;
  public final String[] subscriptionRemoveSuccessAr;
  public final String[] subscriptionRSSAr;
  public final String[] subscriptionsNotAvailableAr;
  public final String[] subscriptionUrlHtmlAr;
  public final String[] subscriptionUrlInvalidAr;
  public final String[] subscriptionUrlTooLongAr;
  public final String[] subscriptionValidateErrorAr;
  public final String[] subscriptionValidateSuccessAr;
  public final String[] subsetAr;
  public final String[] subsetSelectAr;
  public final String[] subsetNMatchingAr;
  public final String[] subsetInstructionsAr;
  public final String[] subsetOptionAr;
  public final String[] subsetOptionsAr;
  public final String[] subsetRefineMapDownloadAr;
  public final String[] subsetRefineSubsetDownloadAr;
  public final String[] subsetClickResetClosestAr;
  public final String[] subsetClickResetLLAr;
  public final String[] subsetMetadataAr;
  public final String[] subsetCountAr;
  public final String[] subsetPercentAr;
  public final String[] subsetViewSelectAr;
  public final String[] subsetViewSelectDistinctCombosAr;
  public final String[] subsetViewSelectRelatedCountsAr;
  public final String[] subsetWhenAr;
  public final String[] subsetWhenNoConstraintsAr;
  public final String[] subsetWhenCountsAr;
  public final String[] subsetComboClickSelectAr;
  public final String[] subsetNVariableCombosAr;
  public final String[] subsetShowingAllRowsAr;
  public final String[] subsetShowingNRowsAr;
  public final String[] subsetChangeShowingAr;
  public final String[] subsetNRowsRelatedDataAr;
  public final String[] subsetViewRelatedChangeAr;
  public final String[] subsetTotalCountAr;
  public final String[] subsetViewAr;
  public final String[] subsetViewCheckAr;
  public final String[] subsetViewCheck1Ar;
  public final String[] subsetViewDistinctMapAr;
  public final String[] subsetViewRelatedMapAr;
  public final String[] subsetViewDistinctDataCountsAr;
  public final String[] subsetViewDistinctDataAr;
  public final String[] subsetViewRelatedDataCountsAr;
  public final String[] subsetViewRelatedDataAr;
  public final String[] subsetViewDistinctMapTooltipAr;
  public final String[] subsetViewRelatedMapTooltipAr;
  public final String[] subsetViewDistinctDataCountsTooltipAr;
  public final String[] subsetViewDistinctDataTooltipAr;
  public final String[] subsetViewRelatedDataCountsTooltipAr;
  public final String[] subsetViewRelatedDataTooltipAr;
  public final String[] subsetWarnAr;
  public final String[] subsetWarn10000Ar;
  public final String[] subsetTooltipAr;
  public final String[] subsetNotSetUpAr;
  public final String[] subsetLongNotShownAr;
  public final String[] tabledapVideoIntroAr;
  public final String[] theDatasetIDAr;
  public final String[] theKeyAr;
  public final String[] theSubscriptionIDAr;
  public final String[] theUrlActionAr;
  public final String[] ThenAr;
  public final String[] thisParticularErddapAr;
  public final String[] timeAr;
  public final String[] timeoutOtherRequestsAr;
  public final String[] unitsAr;
  public final String[] unknownDatasetIDAr;
  public final String[] unknownProtocolAr;
  public final String[] unsupportedFileTypeAr;
  public final String[] updateUrlsFrom; // not Ar. They were arrays before and now
  public final String[] updateUrlsTo; // not Ar
  public final String[] updateUrlsSkipAttributes; // not Ar
  public final String[] usingGriddapAr;
  public final String[] usingTabledapAr;
  public final String[] variableNamesAr;
  public final String[] viewAllDatasetsHtmlAr;
  public final String[] waitThenTryAgainAr;
  public final String[] warningAr;
  public final String[] WCSAr;
  public final String[] wcsDescriptionHtmlAr;
  public final String[] wcsLongDescriptionHtmlAr;
  public final String[] wcsOverview1Ar;
  public final String[] wcsOverview2Ar;
  public final String[] wmsDescriptionHtmlAr;
  public final String[] WMSDocumentation1Ar;
  public final String[] WMSGetCapabilitiesAr;
  public final String[] WMSGetMapAr;
  public final String[] WMSNotesAr;
  public final String[] wmsInstructionsAr;
  public final String[] wmsLongDescriptionHtmlAr;
  public final String[] wmsManyDatasetsAr;
  public final String[] yourEmailAddressAr;
  public final String[] zoomInAr;
  public final String[] zoomOutAr;
  public final int[] imageWidths;
  public final int[] imageHeights;
  public final int[] pdfWidths;
  public final int[] pdfHeights;
  private final String[] theLongDescriptionHtmlAr; // see the xxx() methods
  public static final String errorFromDataSource = String2.ERROR + " from data source: ";
  public final int nLanguages = TranslateMessages.languageList.size();

  public EDMessages(String contentDirectory) throws Exception {
    String setupFileName =
        contentDirectory + "setup" + (EDStatic.config.developmentMode ? "2" : "") + ".xml";
    String errorInMethod;
    ResourceBundle2 setup = ResourceBundle2.fromXml(XML.parseXml(setupFileName, false));
    Map<String, String> ev = System.getenv();
    // **** messages.xml *************************************************************
    // This is read AFTER setup.xml. If that is a problem for something, defer reading it in setup
    // and add it below.
    // Read static messages from messages(2).xml in contentDirectory.
    ResourceBundle2[] messagesAr = new ResourceBundle2[nLanguages];
    String messagesFileName = contentDirectory + "messages.xml";
    if (File2.isFile(messagesFileName)) {
      String2.log("Using custom messages.xml from " + messagesFileName);
      // messagesAr[0] is either the custom messages.xml or the one provided by Erddap
      messagesAr[0] = ResourceBundle2.fromXml(XML.parseXml(messagesFileName, false));
    } else {
      // use default messages.xml
      String2.log("Custom messages.xml not found at " + messagesFileName);
      // use String2.getClass(), not ClassLoader.getSystemResource (which fails in Tomcat)
      URL messagesResourceFile = Resources.getResource("gov/noaa/pfel/erddap/util/messages.xml");
      // messagesAr[0] is either the custom messages.xml or the one provided by Erddap
      messagesAr[0] = ResourceBundle2.fromXml(XML.parseXml(messagesResourceFile, false));
      String2.log("Using default messages.xml from  " + messagesFileName);
    }

    for (int tl = 1; tl < nLanguages; tl++) {
      String tName = "messages-" + TranslateMessages.languageCodeList.get(tl) + ".xml";
      errorInMethod = "ERROR while reading " + tName + ": ";
      URL messageFile = new URL(TranslateMessages.translatedMessagesDir + tName);
      messagesAr[tl] = ResourceBundle2.fromXml(XML.parseXml(messageFile, false));
    }
    // read all the static Strings from messages.xml
    errorInMethod = "ERROR while reading from all the messages.xml files: ";
    acceptEncodingHtmlAr = getNotNothingString(messagesAr, "acceptEncodingHtml", errorInMethod);
    accessRESTFULAr = getNotNothingString(messagesAr, "accessRestful", errorInMethod);
    acronymsAr = getNotNothingString(messagesAr, "acronyms", errorInMethod);
    addConstraintsAr = getNotNothingString(messagesAr, "addConstraints", errorInMethod);
    addVarWhereAttNameAr = getNotNothingString(messagesAr, "addVarWhereAttName", errorInMethod);
    addVarWhereAttValueAr = getNotNothingString(messagesAr, "addVarWhereAttValue", errorInMethod);
    addVarWhereAr = getNotNothingString(messagesAr, "addVarWhere", errorInMethod);
    additionalLinksAr = getNotNothingString(messagesAr, "additionalLinks", errorInMethod);
    admKeywords = messagesAr[0].getNotNothingString("admKeywords", errorInMethod);
    admSubsetVariables = messagesAr[0].getNotNothingString("admSubsetVariables", errorInMethod);
    admSummaryAr = getNotNothingString(messagesAr, "admSummary", errorInMethod);
    admTitleAr = getNotNothingString(messagesAr, "admTitle", errorInMethod);
    advl_datasetID = messagesAr[0].getNotNothingString("advl_datasetID", errorInMethod);
    advc_accessibleAr = getNotNothingString(messagesAr, "advc_accessible", errorInMethod);
    advl_accessibleAr = getNotNothingString(messagesAr, "advl_accessible", errorInMethod);
    advl_institutionAr = getNotNothingString(messagesAr, "advl_institution", errorInMethod);
    advc_dataStructureAr = getNotNothingString(messagesAr, "advc_dataStructure", errorInMethod);
    advl_dataStructureAr = getNotNothingString(messagesAr, "advl_dataStructure", errorInMethod);
    advr_dataStructure = messagesAr[0].getNotNothingString("advr_dataStructure", errorInMethod);
    advl_cdm_data_typeAr = getNotNothingString(messagesAr, "advl_cdm_data_type", errorInMethod);
    advr_cdm_data_type = messagesAr[0].getNotNothingString("advr_cdm_data_type", errorInMethod);
    advl_classAr = getNotNothingString(messagesAr, "advl_class", errorInMethod);
    advr_class = messagesAr[0].getNotNothingString("advr_class", errorInMethod);
    advl_titleAr = getNotNothingString(messagesAr, "advl_title", errorInMethod);
    advl_minLongitudeAr = getNotNothingString(messagesAr, "advl_minLongitude", errorInMethod);
    advl_maxLongitudeAr = getNotNothingString(messagesAr, "advl_maxLongitude", errorInMethod);
    advl_longitudeSpacingAr =
        getNotNothingString(messagesAr, "advl_longitudeSpacing", errorInMethod);
    advl_minLatitudeAr = getNotNothingString(messagesAr, "advl_minLatitude", errorInMethod);
    advl_maxLatitudeAr = getNotNothingString(messagesAr, "advl_maxLatitude", errorInMethod);
    advl_latitudeSpacingAr = getNotNothingString(messagesAr, "advl_latitudeSpacing", errorInMethod);
    advl_minAltitudeAr = getNotNothingString(messagesAr, "advl_minAltitude", errorInMethod);
    advl_maxAltitudeAr = getNotNothingString(messagesAr, "advl_maxAltitude", errorInMethod);
    advl_minTimeAr = getNotNothingString(messagesAr, "advl_minTime", errorInMethod);
    advc_maxTimeAr = getNotNothingString(messagesAr, "advc_maxTime", errorInMethod);
    advl_maxTimeAr = getNotNothingString(messagesAr, "advl_maxTime", errorInMethod);
    advl_timeSpacingAr = getNotNothingString(messagesAr, "advl_timeSpacing", errorInMethod);
    advc_griddapAr = getNotNothingString(messagesAr, "advc_griddap", errorInMethod);
    advl_griddapAr = getNotNothingString(messagesAr, "advl_griddap", errorInMethod);
    advl_subsetAr = getNotNothingString(messagesAr, "advl_subset", errorInMethod);
    advc_tabledapAr = getNotNothingString(messagesAr, "advc_tabledap", errorInMethod);
    advl_tabledapAr = getNotNothingString(messagesAr, "advl_tabledap", errorInMethod);
    advl_MakeAGraphAr = getNotNothingString(messagesAr, "advl_MakeAGraph", errorInMethod);
    advc_sosAr = getNotNothingString(messagesAr, "advc_sos", errorInMethod);
    advl_sosAr = getNotNothingString(messagesAr, "advl_sos", errorInMethod);
    advl_wcsAr = getNotNothingString(messagesAr, "advl_wcs", errorInMethod);
    advl_wmsAr = getNotNothingString(messagesAr, "advl_wms", errorInMethod);
    advc_filesAr = getNotNothingString(messagesAr, "advc_files", errorInMethod);
    advl_filesAr = getNotNothingString(messagesAr, "advl_files", errorInMethod);
    advc_fgdcAr = getNotNothingString(messagesAr, "advc_fgdc", errorInMethod);
    advl_fgdcAr = getNotNothingString(messagesAr, "advl_fgdc", errorInMethod);
    advc_iso19115Ar = getNotNothingString(messagesAr, "advc_iso19115", errorInMethod);
    advl_iso19115Ar = getNotNothingString(messagesAr, "advl_iso19115", errorInMethod);
    advc_metadataAr = getNotNothingString(messagesAr, "advc_metadata", errorInMethod);
    advl_metadataAr = getNotNothingString(messagesAr, "advl_metadata", errorInMethod);
    advl_sourceUrlAr = getNotNothingString(messagesAr, "advl_sourceUrl", errorInMethod);
    advl_infoUrlAr = getNotNothingString(messagesAr, "advl_infoUrl", errorInMethod);
    advl_rssAr = getNotNothingString(messagesAr, "advl_rss", errorInMethod);
    advc_emailAr = getNotNothingString(messagesAr, "advc_email", errorInMethod);
    advl_emailAr = getNotNothingString(messagesAr, "advl_email", errorInMethod);
    advl_summaryAr = getNotNothingString(messagesAr, "advl_summary", errorInMethod);
    advc_testOutOfDateAr = getNotNothingString(messagesAr, "advc_testOutOfDate", errorInMethod);
    advl_testOutOfDateAr = getNotNothingString(messagesAr, "advl_testOutOfDate", errorInMethod);
    advc_outOfDateAr = getNotNothingString(messagesAr, "advc_outOfDate", errorInMethod);
    advl_outOfDateAr = getNotNothingString(messagesAr, "advl_outOfDate", errorInMethod);
    advn_outOfDateAr = getNotNothingString(messagesAr, "advn_outOfDate", errorInMethod);
    advancedSearchAr = getNotNothingString(messagesAr, "advancedSearch", errorInMethod);
    advancedSearchResultsAr =
        getNotNothingString(messagesAr, "advancedSearchResults", errorInMethod);
    advancedSearchDirectionsAr =
        getNotNothingString(messagesAr, "advancedSearchDirections", errorInMethod);
    advancedSearchTooltipAr =
        getNotNothingString(messagesAr, "advancedSearchTooltip", errorInMethod);
    advancedSearchBoundsAr = getNotNothingString(messagesAr, "advancedSearchBounds", errorInMethod);
    advancedSearchMinLatAr = getNotNothingString(messagesAr, "advancedSearchMinLat", errorInMethod);
    advancedSearchMaxLatAr = getNotNothingString(messagesAr, "advancedSearchMaxLat", errorInMethod);
    advancedSearchMinLonAr = getNotNothingString(messagesAr, "advancedSearchMinLon", errorInMethod);
    advancedSearchMaxLonAr = getNotNothingString(messagesAr, "advancedSearchMaxLon", errorInMethod);
    advancedSearchMinMaxLonAr =
        getNotNothingString(messagesAr, "advancedSearchMinMaxLon", errorInMethod);
    advancedSearchMinTimeAr =
        getNotNothingString(messagesAr, "advancedSearchMinTime", errorInMethod);
    advancedSearchMaxTimeAr =
        getNotNothingString(messagesAr, "advancedSearchMaxTime", errorInMethod);
    advancedSearchClearAr = getNotNothingString(messagesAr, "advancedSearchClear", errorInMethod);
    advancedSearchClearHelpAr =
        getNotNothingString(messagesAr, "advancedSearchClearHelp", errorInMethod);
    advancedSearchCategoryTooltipAr =
        getNotNothingString(messagesAr, "advancedSearchCategoryTooltip", errorInMethod);
    advancedSearchRangeTooltipAr =
        getNotNothingString(messagesAr, "advancedSearchRangeTooltip", errorInMethod);
    advancedSearchMapTooltipAr =
        getNotNothingString(messagesAr, "advancedSearchMapTooltip", errorInMethod);
    advancedSearchLonTooltipAr =
        getNotNothingString(messagesAr, "advancedSearchLonTooltip", errorInMethod);
    advancedSearchTimeTooltipAr =
        getNotNothingString(messagesAr, "advancedSearchTimeTooltip", errorInMethod);
    advancedSearchWithCriteriaAr =
        getNotNothingString(messagesAr, "advancedSearchWithCriteria", errorInMethod);
    advancedSearchFewerCriteriaAr =
        getNotNothingString(messagesAr, "advancedSearchFewerCriteria", errorInMethod);
    advancedSearchNoCriteriaAr =
        getNotNothingString(messagesAr, "advancedSearchNoCriteria", errorInMethod);
    advancedSearchErrorHandlingAr =
        getNotNothingString(messagesAr, "advancedSearchErrorHandling", errorInMethod);

    autoRefreshAr = getNotNothingString(messagesAr, "autoRefresh", errorInMethod);
    blacklistMsgAr = getNotNothingString(messagesAr, "blacklistMsg", errorInMethod);
    BroughtToYouByAr = getNotNothingString(messagesAr, "BroughtToYouBy", errorInMethod);

    categoryTitleHtmlAr = getNotNothingString(messagesAr, "categoryTitleHtml", errorInMethod);
    categoryHtmlAr = getNotNothingString(messagesAr, "categoryHtml", errorInMethod);
    category3HtmlAr = getNotNothingString(messagesAr, "category3Html", errorInMethod);
    categoryPickAttributeAr =
        getNotNothingString(messagesAr, "categoryPickAttribute", errorInMethod);
    categorySearchHtmlAr = getNotNothingString(messagesAr, "categorySearchHtml", errorInMethod);
    categorySearchDifferentHtmlAr =
        getNotNothingString(messagesAr, "categorySearchDifferentHtml", errorInMethod);
    categoryClickHtmlAr = getNotNothingString(messagesAr, "categoryClickHtml", errorInMethod);
    categoryNotAnOptionAr = getNotNothingString(messagesAr, "categoryNotAnOption", errorInMethod);
    caughtInterruptedAr = getNotNothingString(messagesAr, "caughtInterrupted", errorInMethod);
    for (int tl = 0; tl < nLanguages; tl++) caughtInterruptedAr[tl] = " " + caughtInterruptedAr[tl];

    cdmDataTypeHelpAr = getNotNothingString(messagesAr, "cdmDataTypeHelp", errorInMethod);

    clickAccessAr = getNotNothingString(messagesAr, "clickAccess", errorInMethod);
    clickBackgroundInfoAr = getNotNothingString(messagesAr, "clickBackgroundInfo", errorInMethod);
    clickERDDAPAr = getNotNothingString(messagesAr, "clickERDDAP", errorInMethod);
    clickInfoAr = getNotNothingString(messagesAr, "clickInfo", errorInMethod);
    clickToSubmitAr = getNotNothingString(messagesAr, "clickToSubmit", errorInMethod);
    convertAr = getNotNothingString(messagesAr, "convert", errorInMethod);
    convertBypassAr = getNotNothingString(messagesAr, "convertBypass", errorInMethod);

    convertCOLORsAr = getNotNothingString(messagesAr, "convertCOLORs", errorInMethod);
    convertCOLORsMessageAr = getNotNothingString(messagesAr, "convertCOLORsMessage", errorInMethod);
    convertToAFullNameAr = getNotNothingString(messagesAr, "convertToAFullName", errorInMethod);
    convertToAnAcronymAr = getNotNothingString(messagesAr, "convertToAnAcronym", errorInMethod);
    convertToACountyNameAr = getNotNothingString(messagesAr, "convertToACountyName", errorInMethod);
    convertToAFIPSCodeAr = getNotNothingString(messagesAr, "convertToAFIPSCode", errorInMethod);
    convertToGCMDAr = getNotNothingString(messagesAr, "convertToGCMD", errorInMethod);
    convertToCFStandardNamesAr =
        getNotNothingString(messagesAr, "convertToCFStandardNames", errorInMethod);
    convertToNumericTimeAr = getNotNothingString(messagesAr, "convertToNumericTime", errorInMethod);
    convertToStringTimeAr = getNotNothingString(messagesAr, "convertToStringTime", errorInMethod);
    convertAnyStringTimeAr = getNotNothingString(messagesAr, "convertAnyStringTime", errorInMethod);
    convertToProperTimeUnitsAr =
        getNotNothingString(messagesAr, "convertToProperTimeUnits", errorInMethod);
    convertFromUDUNITSToUCUMAr =
        getNotNothingString(messagesAr, "convertFromUDUNITSToUCUM", errorInMethod);
    convertFromUCUMToUDUNITSAr =
        getNotNothingString(messagesAr, "convertFromUCUMToUDUNITS", errorInMethod);
    convertToUCUMAr = getNotNothingString(messagesAr, "convertToUCUM", errorInMethod);
    convertToUDUNITSAr = getNotNothingString(messagesAr, "convertToUDUNITS", errorInMethod);
    convertStandardizeUDUNITSAr =
        getNotNothingString(messagesAr, "convertStandardizeUDUNITS", errorInMethod);
    convertToFullNameAr = getNotNothingString(messagesAr, "convertToFullName", errorInMethod);
    convertToVariableNameAr =
        getNotNothingString(messagesAr, "convertToVariableName", errorInMethod);

    converterWebServiceAr = getNotNothingString(messagesAr, "converterWebService", errorInMethod);
    convertOAAcronymsAr = getNotNothingString(messagesAr, "convertOAAcronyms", errorInMethod);
    convertOAAcronymsToFromAr =
        getNotNothingString(messagesAr, "convertOAAcronymsToFrom", errorInMethod);
    convertOAAcronymsIntroAr =
        getNotNothingString(messagesAr, "convertOAAcronymsIntro", errorInMethod);
    convertOAAcronymsNotesAr =
        getNotNothingString(messagesAr, "convertOAAcronymsNotes", errorInMethod);
    convertOAAcronymsServiceAr =
        getNotNothingString(messagesAr, "convertOAAcronymsService", errorInMethod);
    convertOAVariableNamesAr =
        getNotNothingString(messagesAr, "convertOAVariableNames", errorInMethod);
    convertOAVariableNamesToFromAr =
        getNotNothingString(messagesAr, "convertOAVariableNamesToFrom", errorInMethod);
    convertOAVariableNamesIntroAr =
        getNotNothingString(messagesAr, "convertOAVariableNamesIntro", errorInMethod);
    convertOAVariableNamesNotesAr =
        getNotNothingString(messagesAr, "convertOAVariableNamesNotes", errorInMethod);
    convertOAVariableNamesServiceAr =
        getNotNothingString(messagesAr, "convertOAVariableNamesService", errorInMethod);
    convertFipsCountyAr = getNotNothingString(messagesAr, "convertFipsCounty", errorInMethod);
    convertFipsCountyIntroAr =
        getNotNothingString(messagesAr, "convertFipsCountyIntro", errorInMethod);
    convertFipsCountyNotesAr =
        getNotNothingString(messagesAr, "convertFipsCountyNotes", errorInMethod);
    convertFipsCountyServiceAr =
        getNotNothingString(messagesAr, "convertFipsCountyService", errorInMethod);
    convertHtmlAr = getNotNothingString(messagesAr, "convertHtml", errorInMethod);
    convertInterpolateAr = getNotNothingString(messagesAr, "convertInterpolate", errorInMethod);
    convertInterpolateIntroAr =
        getNotNothingString(messagesAr, "convertInterpolateIntro", errorInMethod);
    convertInterpolateTLLTableAr =
        getNotNothingString(messagesAr, "convertInterpolateTLLTable", errorInMethod);
    convertInterpolateTLLTableHelpAr =
        getNotNothingString(messagesAr, "convertInterpolateTLLTableHelp", errorInMethod);
    convertInterpolateDatasetIDVariableAr =
        getNotNothingString(messagesAr, "convertInterpolateDatasetIDVariable", errorInMethod);
    convertInterpolateDatasetIDVariableHelpAr =
        getNotNothingString(messagesAr, "convertInterpolateDatasetIDVariableHelp", errorInMethod);
    convertInterpolateNotesAr =
        getNotNothingString(messagesAr, "convertInterpolateNotes", errorInMethod);
    convertInterpolateServiceAr =
        getNotNothingString(messagesAr, "convertInterpolateService", errorInMethod);
    convertKeywordsAr = getNotNothingString(messagesAr, "convertKeywords", errorInMethod);
    convertKeywordsCfTooltipAr =
        getNotNothingString(messagesAr, "convertKeywordsCfTooltip", errorInMethod);
    convertKeywordsGcmdTooltipAr =
        getNotNothingString(messagesAr, "convertKeywordsGcmdTooltip", errorInMethod);
    convertKeywordsIntroAr = getNotNothingString(messagesAr, "convertKeywordsIntro", errorInMethod);
    convertKeywordsNotesAr = getNotNothingString(messagesAr, "convertKeywordsNotes", errorInMethod);
    convertKeywordsServiceAr =
        getNotNothingString(messagesAr, "convertKeywordsService", errorInMethod);

    convertTimeAr = getNotNothingString(messagesAr, "convertTime", errorInMethod);
    convertTimeReferenceAr = getNotNothingString(messagesAr, "convertTimeReference", errorInMethod);
    convertTimeIntroAr = getNotNothingString(messagesAr, "convertTimeIntro", errorInMethod);
    convertTimeNotesAr = getNotNothingString(messagesAr, "convertTimeNotes", errorInMethod);
    convertTimeServiceAr = getNotNothingString(messagesAr, "convertTimeService", errorInMethod);
    convertTimeNumberTooltipAr =
        getNotNothingString(messagesAr, "convertTimeNumberTooltip", errorInMethod);
    convertTimeStringTimeTooltipAr =
        getNotNothingString(messagesAr, "convertTimeStringTimeTooltip", errorInMethod);
    convertTimeUnitsTooltipAr =
        getNotNothingString(messagesAr, "convertTimeUnitsTooltip", errorInMethod);
    convertTimeUnitsHelpAr = getNotNothingString(messagesAr, "convertTimeUnitsHelp", errorInMethod);
    convertTimeIsoFormatErrorAr =
        getNotNothingString(messagesAr, "convertTimeIsoFormatError", errorInMethod);
    convertTimeNoSinceErrorAr =
        getNotNothingString(messagesAr, "convertTimeNoSinceError", errorInMethod);
    convertTimeNumberErrorAr =
        getNotNothingString(messagesAr, "convertTimeNumberError", errorInMethod);
    convertTimeNumericTimeErrorAr =
        getNotNothingString(messagesAr, "convertTimeNumericTimeError", errorInMethod);
    convertTimeParametersErrorAr =
        getNotNothingString(messagesAr, "convertTimeParametersError", errorInMethod);
    convertTimeStringFormatErrorAr =
        getNotNothingString(messagesAr, "convertTimeStringFormatError", errorInMethod);
    convertTimeTwoTimeErrorAr =
        getNotNothingString(messagesAr, "convertTimeTwoTimeError", errorInMethod);
    convertTimeUnitsErrorAr =
        getNotNothingString(messagesAr, "convertTimeUnitsError", errorInMethod);
    convertUnitsAr = getNotNothingString(messagesAr, "convertUnits", errorInMethod);
    convertUnitsComparisonAr =
        getNotNothingString(messagesAr, "convertUnitsComparison", errorInMethod);
    convertUnitsFilterAr = getNotNothingString(messagesAr, "convertUnitsFilter", errorInMethod);
    for (int tl = 0; tl < nLanguages; tl++) {
      convertUnitsComparisonAr[tl] =
          convertUnitsComparisonAr[tl]
              .replaceAll(
                  "&C;",
                  "C") // these handled this way be cause you can't just avoid translating all
              // words with 'C'
              .replaceAll("&g;", "g") // "
              .replaceAll("&F;", "F") // "
              .replaceAll("&NTU;", "NTU")
              .replaceAll("&ntu;", "ntu")
              .replaceAll("&PSU;", "PSU")
              .replaceAll("&psu;", "psu");
    }

    convertUnitsIntroAr = getNotNothingString(messagesAr, "convertUnitsIntro", errorInMethod);
    convertUnitsNotesAr = getNotNothingString(messagesAr, "convertUnitsNotes", errorInMethod);
    for (int tl = 0; tl < nLanguages; tl++)
      convertUnitsNotesAr[tl] =
          convertUnitsNotesAr[tl].replace("&unitsStandard;", EDStatic.config.units_standard);
    convertUnitsServiceAr = getNotNothingString(messagesAr, "convertUnitsService", errorInMethod);
    convertURLsAr = getNotNothingString(messagesAr, "convertURLs", errorInMethod);
    convertURLsIntroAr = getNotNothingString(messagesAr, "convertURLsIntro", errorInMethod);
    convertURLsNotesAr = getNotNothingString(messagesAr, "convertURLsNotes", errorInMethod);
    convertURLsServiceAr = getNotNothingString(messagesAr, "convertURLsService", errorInMethod);
    cookiesHelpAr = getNotNothingString(messagesAr, "cookiesHelp", errorInMethod);
    copyImageToClipboardAr = getNotNothingString(messagesAr, "copyImageToClipboard", errorInMethod);
    copyTextToClipboardAr = getNotNothingString(messagesAr, "copyTextToClipboard", errorInMethod);
    copyToClipboardNotAvailableAr =
        getNotNothingString(messagesAr, "copyToClipboardNotAvailable", errorInMethod);

    dafAr = getNotNothingString(messagesAr, "daf", errorInMethod);
    dafGridBypassTooltipAr = getNotNothingString(messagesAr, "dafGridBypassTooltip", errorInMethod);
    dafGridTooltipAr = getNotNothingString(messagesAr, "dafGridTooltip", errorInMethod);
    dafTableBypassTooltipAr =
        getNotNothingString(messagesAr, "dafTableBypassTooltip", errorInMethod);
    dafTableTooltipAr = getNotNothingString(messagesAr, "dafTableTooltip", errorInMethod);
    dasTitleAr = getNotNothingString(messagesAr, "dasTitle", errorInMethod);
    dataAccessNotAllowedAr = getNotNothingString(messagesAr, "dataAccessNotAllowed", errorInMethod);
    databaseUnableToConnectAr =
        getNotNothingString(messagesAr, "databaseUnableToConnect", errorInMethod);
    dataProviderFormAr = getNotNothingString(messagesAr, "dataProviderForm", errorInMethod);
    dataProviderFormP1Ar = getNotNothingString(messagesAr, "dataProviderFormP1", errorInMethod);
    dataProviderFormP2Ar = getNotNothingString(messagesAr, "dataProviderFormP2", errorInMethod);
    dataProviderFormP3Ar = getNotNothingString(messagesAr, "dataProviderFormP3", errorInMethod);
    dataProviderFormP4Ar = getNotNothingString(messagesAr, "dataProviderFormP4", errorInMethod);
    dataProviderFormDoneAr = getNotNothingString(messagesAr, "dataProviderFormDone", errorInMethod);
    dataProviderFormSuccessAr =
        getNotNothingString(messagesAr, "dataProviderFormSuccess", errorInMethod);
    dataProviderFormShortDescriptionAr =
        getNotNothingString(messagesAr, "dataProviderFormShortDescription", errorInMethod);
    dataProviderFormLongDescriptionHTMLAr =
        getNotNothingString(messagesAr, "dataProviderFormLongDescriptionHTML", errorInMethod);
    disabledAr = getNotNothingString(messagesAr, "disabled", errorInMethod);
    dataProviderFormPart1Ar =
        getNotNothingString(messagesAr, "dataProviderFormPart1", errorInMethod);
    dataProviderFormPart2HeaderAr =
        getNotNothingString(messagesAr, "dataProviderFormPart2Header", errorInMethod);
    dataProviderFormPart2GlobalMetadataAr =
        getNotNothingString(messagesAr, "dataProviderFormPart2GlobalMetadata", errorInMethod);
    dataProviderContactInfoAr =
        getNotNothingString(messagesAr, "dataProviderContactInfo", errorInMethod);
    dataProviderDataAr = getNotNothingString(messagesAr, "dataProviderData", errorInMethod);
    documentationAr = getNotNothingString(messagesAr, "documentation", errorInMethod);

    dpf_submitAr = getNotNothingString(messagesAr, "dpf_submit", errorInMethod);
    dpf_fixProblemAr = getNotNothingString(messagesAr, "dpf_fixProblem", errorInMethod);
    dpf_yourNameAr = getNotNothingString(messagesAr, "dpf_yourName", errorInMethod);
    dpf_emailAddressAr = getNotNothingString(messagesAr, "dpf_emailAddress", errorInMethod);
    dpf_TimestampAr = getNotNothingString(messagesAr, "dpf_Timestamp", errorInMethod);
    dpf_frequencyAr = getNotNothingString(messagesAr, "dpf_frequency", errorInMethod);
    dpf_titleAr = getNotNothingString(messagesAr, "dpf_title", errorInMethod);
    dpf_titleTooltipAr = getNotNothingString(messagesAr, "dpf_titleTooltip", errorInMethod);
    dpf_summaryAr = getNotNothingString(messagesAr, "dpf_summary", errorInMethod);
    dpf_summaryTooltipAr = getNotNothingString(messagesAr, "dpf_summaryTooltip", errorInMethod);
    dpf_creatorNameAr = getNotNothingString(messagesAr, "dpf_creatorName", errorInMethod);
    dpf_creatorNameTooltipAr =
        getNotNothingString(messagesAr, "dpf_creatorNameTooltip", errorInMethod);
    dpf_creatorTypeAr = getNotNothingString(messagesAr, "dpf_creatorType", errorInMethod);
    dpf_creatorTypeTooltipAr =
        getNotNothingString(messagesAr, "dpf_creatorTypeTooltip", errorInMethod);
    dpf_creatorEmailAr = getNotNothingString(messagesAr, "dpf_creatorEmail", errorInMethod);
    dpf_creatorEmailTooltipAr =
        getNotNothingString(messagesAr, "dpf_creatorEmailTooltip", errorInMethod);
    dpf_institutionAr = getNotNothingString(messagesAr, "dpf_institution", errorInMethod);
    dpf_institutionTooltipAr =
        getNotNothingString(messagesAr, "dpf_institutionTooltip", errorInMethod);
    dpf_infoUrlAr = getNotNothingString(messagesAr, "dpf_infoUrl", errorInMethod);
    dpf_infoUrlTooltipAr = getNotNothingString(messagesAr, "dpf_infoUrlTooltip", errorInMethod);
    dpf_licenseAr = getNotNothingString(messagesAr, "dpf_license", errorInMethod);
    dpf_licenseTooltipAr = getNotNothingString(messagesAr, "dpf_licenseTooltip", errorInMethod);
    dpf_howYouStoreDataAr = getNotNothingString(messagesAr, "dpf_howYouStoreData", errorInMethod);
    dpf_provideIfAvailableAr =
        getNotNothingString(messagesAr, "dpf_provideIfAvailable", errorInMethod);
    dpf_acknowledgementAr = getNotNothingString(messagesAr, "dpf_acknowledgement", errorInMethod);
    dpf_acknowledgementTooltipAr =
        getNotNothingString(messagesAr, "dpf_acknowledgementTooltip", errorInMethod);
    dpf_historyAr = getNotNothingString(messagesAr, "dpf_history", errorInMethod);
    dpf_historyTooltipAr = getNotNothingString(messagesAr, "dpf_historyTooltip", errorInMethod);
    dpf_idTooltipAr = getNotNothingString(messagesAr, "dpf_idTooltip", errorInMethod);
    dpf_namingAuthorityAr = getNotNothingString(messagesAr, "dpf_namingAuthority", errorInMethod);
    dpf_namingAuthorityTooltipAr =
        getNotNothingString(messagesAr, "dpf_namingAuthorityTooltip", errorInMethod);
    dpf_productVersionAr = getNotNothingString(messagesAr, "dpf_productVersion", errorInMethod);
    dpf_productVersionTooltipAr =
        getNotNothingString(messagesAr, "dpf_productVersionTooltip", errorInMethod);
    dpf_referencesAr = getNotNothingString(messagesAr, "dpf_references", errorInMethod);
    dpf_referencesTooltipAr =
        getNotNothingString(messagesAr, "dpf_referencesTooltip", errorInMethod);
    dpf_commentAr = getNotNothingString(messagesAr, "dpf_comment", errorInMethod);
    dpf_commentTooltipAr = getNotNothingString(messagesAr, "dpf_commentTooltip", errorInMethod);
    dpf_dataTypeHelpAr = getNotNothingString(messagesAr, "dpf_dataTypeHelp", errorInMethod);
    dpf_ioosCategoryAr = getNotNothingString(messagesAr, "dpf_ioosCategory", errorInMethod);
    dpf_ioosCategoryHelpAr = getNotNothingString(messagesAr, "dpf_ioosCategoryHelp", errorInMethod);
    dpf_part3HeaderAr = getNotNothingString(messagesAr, "dpf_part3Header", errorInMethod);
    dpf_variableMetadataAr = getNotNothingString(messagesAr, "dpf_variableMetadata", errorInMethod);
    dpf_sourceNameAr = getNotNothingString(messagesAr, "dpf_sourceName", errorInMethod);
    dpf_sourceNameTooltipAr =
        getNotNothingString(messagesAr, "dpf_sourceNameTooltip", errorInMethod);
    dpf_destinationNameAr = getNotNothingString(messagesAr, "dpf_destinationName", errorInMethod);
    dpf_destinationNameTooltipAr =
        getNotNothingString(messagesAr, "dpf_destinationNameTooltip", errorInMethod);

    dpf_longNameAr = getNotNothingString(messagesAr, "dpf_longName", errorInMethod);
    dpf_longNameTooltipAr = getNotNothingString(messagesAr, "dpf_longNameTooltip", errorInMethod);
    dpf_standardNameAr = getNotNothingString(messagesAr, "dpf_standardName", errorInMethod);
    dpf_standardNameTooltipAr =
        getNotNothingString(messagesAr, "dpf_standardNameTooltip", errorInMethod);
    dpf_dataTypeAr = getNotNothingString(messagesAr, "dpf_dataType", errorInMethod);
    dpf_fillValueAr = getNotNothingString(messagesAr, "dpf_fillValue", errorInMethod);
    dpf_fillValueTooltipAr = getNotNothingString(messagesAr, "dpf_fillValueTooltip", errorInMethod);
    dpf_unitsAr = getNotNothingString(messagesAr, "dpf_units", errorInMethod);
    dpf_unitsTooltipAr = getNotNothingString(messagesAr, "dpf_unitsTooltip", errorInMethod);
    dpf_rangeAr = getNotNothingString(messagesAr, "dpf_range", errorInMethod);
    dpf_rangeTooltipAr = getNotNothingString(messagesAr, "dpf_rangeTooltip", errorInMethod);
    dpf_part4HeaderAr = getNotNothingString(messagesAr, "dpf_part4Header", errorInMethod);
    dpf_otherCommentAr = getNotNothingString(messagesAr, "dpf_otherComment", errorInMethod);
    dpf_finishPart4Ar = getNotNothingString(messagesAr, "dpf_finishPart4", errorInMethod);
    dpf_congratulationAr = getNotNothingString(messagesAr, "dpf_congratulation", errorInMethod);

    distinctValuesTooltipAr =
        getNotNothingString(messagesAr, "distinctValuesTooltip", errorInMethod);
    doWithGraphsAr = getNotNothingString(messagesAr, "doWithGraphs", errorInMethod);

    dtAccessibleAr = getNotNothingString(messagesAr, "dtAccessible", errorInMethod);
    dtAccessiblePublicAr = getNotNothingString(messagesAr, "dtAccessiblePublic", errorInMethod);
    dtAccessibleYesAr = getNotNothingString(messagesAr, "dtAccessibleYes", errorInMethod);
    dtAccessibleGraphsAr = getNotNothingString(messagesAr, "dtAccessibleGraphs", errorInMethod);
    dtAccessibleNoAr = getNotNothingString(messagesAr, "dtAccessibleNo", errorInMethod);
    dtAccessibleLogInAr = getNotNothingString(messagesAr, "dtAccessibleLogIn", errorInMethod);
    dtLogInAr = getNotNothingString(messagesAr, "dtLogIn", errorInMethod);
    dtDAFAr = getNotNothingString(messagesAr, "dtDAF", errorInMethod);
    dtFilesAr = getNotNothingString(messagesAr, "dtFiles", errorInMethod);
    dtMAGAr = getNotNothingString(messagesAr, "dtMAG", errorInMethod);
    dtSOSAr = getNotNothingString(messagesAr, "dtSOS", errorInMethod);
    dtSubsetAr = getNotNothingString(messagesAr, "dtSubset", errorInMethod);
    dtWCSAr = getNotNothingString(messagesAr, "dtWCS", errorInMethod);
    dtWMSAr = getNotNothingString(messagesAr, "dtWMS", errorInMethod);

    EasierAccessToScientificDataAr =
        getNotNothingString(messagesAr, "EasierAccessToScientificData", errorInMethod);
    EDDDatasetIDAr = getNotNothingString(messagesAr, "EDDDatasetID", errorInMethod);
    EDDFgdc = messagesAr[0].getNotNothingString("EDDFgdc", errorInMethod);
    EDDFgdcMetadataAr = getNotNothingString(messagesAr, "EDDFgdcMetadata", errorInMethod);
    EDDFilesAr = getNotNothingString(messagesAr, "EDDFiles", errorInMethod);
    EDDIso19115 = messagesAr[0].getNotNothingString("EDDIso19115", errorInMethod);
    EDDIso19115MetadataAr = getNotNothingString(messagesAr, "EDDIso19115Metadata", errorInMethod);
    EDDMetadataAr = getNotNothingString(messagesAr, "EDDMetadata", errorInMethod);
    EDDBackgroundAr = getNotNothingString(messagesAr, "EDDBackground", errorInMethod);
    EDDClickOnSubmitHtmlAr = getNotNothingString(messagesAr, "EDDClickOnSubmitHtml", errorInMethod);
    EDDInformationAr = getNotNothingString(messagesAr, "EDDInformation", errorInMethod);
    EDDInstitutionAr = getNotNothingString(messagesAr, "EDDInstitution", errorInMethod);
    EDDSummaryAr = getNotNothingString(messagesAr, "EDDSummary", errorInMethod);
    EDDDatasetTitleAr = getNotNothingString(messagesAr, "EDDDatasetTitle", errorInMethod);
    EDDDownloadDataAr = getNotNothingString(messagesAr, "EDDDownloadData", errorInMethod);
    EDDMakeAGraphAr = getNotNothingString(messagesAr, "EDDMakeAGraph", errorInMethod);
    EDDMakeAMapAr = getNotNothingString(messagesAr, "EDDMakeAMap", errorInMethod);
    EDDFileTypeAr = getNotNothingString(messagesAr, "EDDFileType", errorInMethod);
    EDDFileTypeInformationAr =
        getNotNothingString(messagesAr, "EDDFileTypeInformation", errorInMethod);
    EDDSelectFileTypeAr = getNotNothingString(messagesAr, "EDDSelectFileType", errorInMethod);
    EDDMinimumAr = getNotNothingString(messagesAr, "EDDMinimum", errorInMethod);
    EDDMaximumAr = getNotNothingString(messagesAr, "EDDMaximum", errorInMethod);
    EDDConstraintAr = getNotNothingString(messagesAr, "EDDConstraint", errorInMethod);

    EDDChangedWasnt = messagesAr[0].getNotNothingString("EDDChangedWasnt", errorInMethod);
    EDDChangedDifferentNVar =
        messagesAr[0].getNotNothingString("EDDChangedDifferentNVar", errorInMethod);
    EDDChanged2Different = messagesAr[0].getNotNothingString("EDDChanged2Different", errorInMethod);
    EDDChanged1Different = messagesAr[0].getNotNothingString("EDDChanged1Different", errorInMethod);
    EDDChangedCGADifferent =
        messagesAr[0].getNotNothingString("EDDChangedCGADifferent", errorInMethod);
    EDDChangedAxesDifferentNVar =
        messagesAr[0].getNotNothingString("EDDChangedAxesDifferentNVar", errorInMethod);
    EDDChangedAxes2Different =
        messagesAr[0].getNotNothingString("EDDChangedAxes2Different", errorInMethod);
    EDDChangedAxes1Different =
        messagesAr[0].getNotNothingString("EDDChangedAxes1Different", errorInMethod);
    EDDChangedNoValue = messagesAr[0].getNotNothingString("EDDChangedNoValue", errorInMethod);
    EDDChangedTableToGrid =
        messagesAr[0].getNotNothingString("EDDChangedTableToGrid", errorInMethod);

    EDDSimilarDifferentNVar =
        messagesAr[0].getNotNothingString("EDDSimilarDifferentNVar", errorInMethod);
    EDDSimilarDifferent = messagesAr[0].getNotNothingString("EDDSimilarDifferent", errorInMethod);

    EDDGridDownloadTooltipAr =
        getNotNothingString(messagesAr, "EDDGridDownloadTooltip", errorInMethod);
    EDDGridDapDescriptionAr =
        getNotNothingString(messagesAr, "EDDGridDapDescription", errorInMethod);
    EDDGridDapLongDescriptionAr =
        getNotNothingString(messagesAr, "EDDGridDapLongDescription", errorInMethod);
    EDDGridDownloadDataTooltipAr =
        getNotNothingString(messagesAr, "EDDGridDownloadDataTooltip", errorInMethod);
    EDDGridDimensionAr = getNotNothingString(messagesAr, "EDDGridDimension", errorInMethod);
    EDDGridDimensionRangesAr =
        getNotNothingString(messagesAr, "EDDGridDimensionRanges", errorInMethod);
    EDDGridFirstAr = getNotNothingString(messagesAr, "EDDGridFirst", errorInMethod);
    EDDGridLastAr = getNotNothingString(messagesAr, "EDDGridLast", errorInMethod);
    EDDGridStartAr = getNotNothingString(messagesAr, "EDDGridStart", errorInMethod);
    EDDGridStopAr = getNotNothingString(messagesAr, "EDDGridStop", errorInMethod);
    EDDGridStartStopTooltipAr =
        getNotNothingString(messagesAr, "EDDGridStartStopTooltip", errorInMethod);
    EDDGridStrideAr = getNotNothingString(messagesAr, "EDDGridStride", errorInMethod);
    EDDGridNValuesAr = getNotNothingString(messagesAr, "EDDGridNValues", errorInMethod);
    EDDGridNValuesHtmlAr = getNotNothingString(messagesAr, "EDDGridNValuesHtml", errorInMethod);
    EDDGridSpacingAr = getNotNothingString(messagesAr, "EDDGridSpacing", errorInMethod);
    EDDGridJustOneValueAr = getNotNothingString(messagesAr, "EDDGridJustOneValue", errorInMethod);
    EDDGridEvenAr = getNotNothingString(messagesAr, "EDDGridEven", errorInMethod);
    EDDGridUnevenAr = getNotNothingString(messagesAr, "EDDGridUneven", errorInMethod);
    EDDGridDimensionTooltipAr =
        getNotNothingString(messagesAr, "EDDGridDimensionTooltip", errorInMethod);
    EDDGridDimensionFirstTooltipAr =
        getNotNothingString(messagesAr, "EDDGridDimensionFirstTooltip", errorInMethod);
    EDDGridDimensionLastTooltipAr =
        getNotNothingString(messagesAr, "EDDGridDimensionLastTooltip", errorInMethod);
    EDDGridVarHasDimTooltipAr =
        getNotNothingString(messagesAr, "EDDGridVarHasDimTooltip", errorInMethod);
    EDDGridSSSTooltipAr = getNotNothingString(messagesAr, "EDDGridSSSTooltip", errorInMethod);
    EDDGridStartTooltipAr = getNotNothingString(messagesAr, "EDDGridStartTooltip", errorInMethod);
    EDDGridStopTooltipAr = getNotNothingString(messagesAr, "EDDGridStopTooltip", errorInMethod);
    EDDGridStrideTooltipAr = getNotNothingString(messagesAr, "EDDGridStrideTooltip", errorInMethod);
    EDDGridSpacingTooltipAr =
        getNotNothingString(messagesAr, "EDDGridSpacingTooltip", errorInMethod);
    EDDGridGridVariableHtmlAr =
        getNotNothingString(messagesAr, "EDDGridGridVariableHtml", errorInMethod);
    EDDGridCheckAllAr = getNotNothingString(messagesAr, "EDDGridCheckAll", errorInMethod);
    EDDGridCheckAllTooltipAr =
        getNotNothingString(messagesAr, "EDDGridCheckAllTooltip", errorInMethod);
    EDDGridUncheckAllAr = getNotNothingString(messagesAr, "EDDGridUncheckAll", errorInMethod);
    EDDGridUncheckAllTooltipAr =
        getNotNothingString(messagesAr, "EDDGridUncheckAllTooltip", errorInMethod);

    // default EDDGrid...Example
    EDDGridErddapUrlExample =
        messagesAr[0].getNotNothingString("EDDGridErddapUrlExample", errorInMethod);
    EDDGridIdExample = messagesAr[0].getNotNothingString("EDDGridIdExample", errorInMethod);
    EDDGridDimensionExample =
        messagesAr[0].getNotNothingString("EDDGridDimensionExample", errorInMethod);
    EDDGridNoHyperExample =
        messagesAr[0].getNotNothingString("EDDGridNoHyperExample", errorInMethod);
    EDDGridDimNamesExample =
        messagesAr[0].getNotNothingString("EDDGridDimNamesExample", errorInMethod);
    EDDGridDataTimeExample =
        messagesAr[0].getNotNothingString("EDDGridDataTimeExample", errorInMethod);
    EDDGridDataValueExample =
        messagesAr[0].getNotNothingString("EDDGridDataValueExample", errorInMethod);
    EDDGridDataIndexExample =
        messagesAr[0].getNotNothingString("EDDGridDataIndexExample", errorInMethod);
    EDDGridGraphExample = messagesAr[0].getNotNothingString("EDDGridGraphExample", errorInMethod);
    EDDGridMapExample = messagesAr[0].getNotNothingString("EDDGridMapExample", errorInMethod);
    EDDGridMatlabPlotExample =
        messagesAr[0].getNotNothingString("EDDGridMatlabPlotExample", errorInMethod);

    // admin provides EDDGrid...Example
    EDDGridErddapUrlExample =
        getSetupEVString(setup, ev, "EDDGridErddapUrlExample", EDDGridErddapUrlExample);
    EDDGridIdExample = getSetupEVString(setup, ev, "EDDGridIdExample", EDDGridIdExample);
    EDDGridDimensionExample =
        getSetupEVString(setup, ev, "EDDGridDimensionExample", EDDGridDimensionExample);
    EDDGridNoHyperExample =
        getSetupEVString(setup, ev, "EDDGridNoHyperExample", EDDGridNoHyperExample);
    EDDGridDimNamesExample =
        getSetupEVString(setup, ev, "EDDGridDimNamesExample", EDDGridDimNamesExample);
    EDDGridDataIndexExample =
        getSetupEVString(setup, ev, "EDDGridDataIndexExample", EDDGridDataIndexExample);
    EDDGridDataValueExample =
        getSetupEVString(setup, ev, "EDDGridDataValueExample", EDDGridDataValueExample);
    EDDGridDataTimeExample =
        getSetupEVString(setup, ev, "EDDGridDataTimeExample", EDDGridDataTimeExample);
    EDDGridGraphExample = getSetupEVString(setup, ev, "EDDGridGraphExample", EDDGridGraphExample);
    EDDGridMapExample = getSetupEVString(setup, ev, "EDDGridMapExample", EDDGridMapExample);
    EDDGridMatlabPlotExample =
        getSetupEVString(setup, ev, "EDDGridMatlabPlotExample", EDDGridMatlabPlotExample);

    // variants encoded to be Html Examples
    EDDGridDimensionExampleHE = XML.encodeAsHTML(EDDGridDimensionExample);
    EDDGridDataIndexExampleHE = XML.encodeAsHTML(EDDGridDataIndexExample);
    EDDGridDataValueExampleHE = XML.encodeAsHTML(EDDGridDataValueExample);
    EDDGridDataTimeExampleHE = XML.encodeAsHTML(EDDGridDataTimeExample);
    EDDGridGraphExampleHE = XML.encodeAsHTML(EDDGridGraphExample);
    EDDGridMapExampleHE = XML.encodeAsHTML(EDDGridMapExample);

    // variants encoded to be Html Attributes
    EDDGridDimensionExampleHA =
        XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDGridDimensionExample));
    EDDGridDataIndexExampleHA =
        XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDGridDataIndexExample));
    EDDGridDataValueExampleHA =
        XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDGridDataValueExample));
    EDDGridDataTimeExampleHA =
        XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDGridDataTimeExample));
    EDDGridGraphExampleHA = XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDGridGraphExample));
    EDDGridMapExampleHA = XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDGridMapExample));

    EDDTableConstraintsAr = getNotNothingString(messagesAr, "EDDTableConstraints", errorInMethod);
    EDDTableDapDescriptionAr =
        getNotNothingString(messagesAr, "EDDTableDapDescription", errorInMethod);
    EDDTableDapLongDescriptionAr =
        getNotNothingString(messagesAr, "EDDTableDapLongDescription", errorInMethod);
    EDDTableDownloadDataTooltipAr =
        getNotNothingString(messagesAr, "EDDTableDownloadDataTooltip", errorInMethod);
    EDDTableTabularDatasetTooltipAr =
        getNotNothingString(messagesAr, "EDDTableTabularDatasetTooltip", errorInMethod);
    EDDTableVariableAr = getNotNothingString(messagesAr, "EDDTableVariable", errorInMethod);
    EDDTableCheckAllAr = getNotNothingString(messagesAr, "EDDTableCheckAll", errorInMethod);
    EDDTableCheckAllTooltipAr =
        getNotNothingString(messagesAr, "EDDTableCheckAllTooltip", errorInMethod);
    EDDTableUncheckAllAr = getNotNothingString(messagesAr, "EDDTableUncheckAll", errorInMethod);
    EDDTableUncheckAllTooltipAr =
        getNotNothingString(messagesAr, "EDDTableUncheckAllTooltip", errorInMethod);
    EDDTableMinimumTooltipAr =
        getNotNothingString(messagesAr, "EDDTableMinimumTooltip", errorInMethod);
    EDDTableMaximumTooltipAr =
        getNotNothingString(messagesAr, "EDDTableMaximumTooltip", errorInMethod);
    EDDTableCheckTheVariablesAr =
        getNotNothingString(messagesAr, "EDDTableCheckTheVariables", errorInMethod);
    EDDTableSelectAnOperatorAr =
        getNotNothingString(messagesAr, "EDDTableSelectAnOperator", errorInMethod);
    EDDTableFromEDDGridSummaryAr =
        getNotNothingString(messagesAr, "EDDTableFromEDDGridSummary", errorInMethod);
    EDDTableOptConstraint1HtmlAr =
        getNotNothingString(messagesAr, "EDDTableOptConstraint1Html", errorInMethod);
    EDDTableOptConstraint2HtmlAr =
        getNotNothingString(messagesAr, "EDDTableOptConstraint2Html", errorInMethod);
    EDDTableOptConstraintVarAr =
        getNotNothingString(messagesAr, "EDDTableOptConstraintVar", errorInMethod);
    EDDTableNumericConstraintTooltipAr =
        getNotNothingString(messagesAr, "EDDTableNumericConstraintTooltip", errorInMethod);
    EDDTableStringConstraintTooltipAr =
        getNotNothingString(messagesAr, "EDDTableStringConstraintTooltip", errorInMethod);
    EDDTableTimeConstraintTooltipAr =
        getNotNothingString(messagesAr, "EDDTableTimeConstraintTooltip", errorInMethod);
    EDDTableConstraintTooltipAr =
        getNotNothingString(messagesAr, "EDDTableConstraintTooltip", errorInMethod);
    EDDTableSelectConstraintTooltipAr =
        getNotNothingString(messagesAr, "EDDTableSelectConstraintTooltip", errorInMethod);

    // default EDDGrid...Example
    EDDTableErddapUrlExample =
        messagesAr[0].getNotNothingString("EDDTableErddapUrlExample", errorInMethod);
    EDDTableIdExample = messagesAr[0].getNotNothingString("EDDTableIdExample", errorInMethod);
    EDDTableVariablesExample =
        messagesAr[0].getNotNothingString("EDDTableVariablesExample", errorInMethod);
    EDDTableConstraintsExample =
        messagesAr[0].getNotNothingString("EDDTableConstraintsExample", errorInMethod);
    EDDTableDataValueExample =
        messagesAr[0].getNotNothingString("EDDTableDataValueExample", errorInMethod);
    EDDTableDataTimeExample =
        messagesAr[0].getNotNothingString("EDDTableDataTimeExample", errorInMethod);
    EDDTableGraphExample = messagesAr[0].getNotNothingString("EDDTableGraphExample", errorInMethod);
    EDDTableMapExample = messagesAr[0].getNotNothingString("EDDTableMapExample", errorInMethod);
    EDDTableMatlabPlotExample =
        messagesAr[0].getNotNothingString("EDDTableMatlabPlotExample", errorInMethod);

    // admin provides EDDGrid...Example
    EDDTableErddapUrlExample =
        getSetupEVString(setup, ev, "EDDTableErddapUrlExample", EDDTableErddapUrlExample);
    EDDTableIdExample = getSetupEVString(setup, ev, "EDDTableIdExample", EDDTableIdExample);
    EDDTableVariablesExample =
        getSetupEVString(setup, ev, "EDDTableVariablesExample", EDDTableVariablesExample);
    EDDTableConstraintsExample =
        getSetupEVString(setup, ev, "EDDTableConstraintsExample", EDDTableConstraintsExample);
    EDDTableDataValueExample =
        getSetupEVString(setup, ev, "EDDTableDataValueExample", EDDTableDataValueExample);
    EDDTableDataTimeExample =
        getSetupEVString(setup, ev, "EDDTableDataTimeExample", EDDTableDataTimeExample);
    EDDTableGraphExample =
        getSetupEVString(setup, ev, "EDDTableGraphExample", EDDTableGraphExample);
    EDDTableMapExample = getSetupEVString(setup, ev, "EDDTableMapExample", EDDTableMapExample);
    EDDTableMatlabPlotExample =
        getSetupEVString(setup, ev, "EDDTableMatlabPlotExample", EDDTableMatlabPlotExample);

    // variants encoded to be Html Examples
    EDDTableConstraintsExampleHE = XML.encodeAsHTML(EDDTableConstraintsExample);
    EDDTableDataTimeExampleHE = XML.encodeAsHTML(EDDTableDataTimeExample);
    EDDTableDataValueExampleHE = XML.encodeAsHTML(EDDTableDataValueExample);
    EDDTableGraphExampleHE = XML.encodeAsHTML(EDDTableGraphExample);
    EDDTableMapExampleHE = XML.encodeAsHTML(EDDTableMapExample);

    // variants encoded to be Html Attributes
    EDDTableConstraintsExampleHA =
        XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDTableConstraintsExample));
    EDDTableDataTimeExampleHA =
        XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDTableDataTimeExample));
    EDDTableDataValueExampleHA =
        XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDTableDataValueExample));
    EDDTableGraphExampleHA =
        XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDTableGraphExample));
    EDDTableMapExampleHA = XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDTableMapExample));

    EDDTableFromHttpGetDatasetDescription =
        XML.decodeEntities( // because this is used as plain text
            messagesAr[0].getNotNothingString(
                "EDDTableFromHttpGetDatasetDescription", errorInMethod));
    EDDTableFromHttpGetAuthorDescription =
        messagesAr[0].getNotNothingString("EDDTableFromHttpGetAuthorDescription", errorInMethod);
    EDDTableFromHttpGetTimestampDescription =
        messagesAr[0].getNotNothingString("EDDTableFromHttpGetTimestampDescription", errorInMethod);

    errorTitleAr = getNotNothingString(messagesAr, "errorTitle", errorInMethod);
    erddapIsAr = getNotNothingString(messagesAr, "erddapIs", errorInMethod);
    erddapVersionHTMLAr = getNotNothingString(messagesAr, "erddapVersionHTML", errorInMethod);
    errorRequestUrlAr = getNotNothingString(messagesAr, "errorRequestUrl", errorInMethod);
    errorRequestQueryAr = getNotNothingString(messagesAr, "errorRequestQuery", errorInMethod);
    errorTheErrorAr = getNotNothingString(messagesAr, "errorTheError", errorInMethod);
    errorCopyFromAr = getNotNothingString(messagesAr, "errorCopyFrom", errorInMethod);
    errorFileNotFoundAr = getNotNothingString(messagesAr, "errorFileNotFound", errorInMethod);
    errorFileNotFoundImageAr =
        getNotNothingString(messagesAr, "errorFileNotFoundImage", errorInMethod);
    errorInternalAr = getNotNothingString(messagesAr, "errorInternal", errorInMethod);
    for (int tl = 0; tl < nLanguages; tl++) errorInternalAr[tl] += " ";

    errorJsonpFunctionNameAr =
        getNotNothingString(messagesAr, "errorJsonpFunctionName", errorInMethod);
    errorJsonpNotAllowedAr = getNotNothingString(messagesAr, "errorJsonpNotAllowed", errorInMethod);
    errorMoreThan2GBAr = getNotNothingString(messagesAr, "errorMoreThan2GB", errorInMethod);
    errorNotFoundAr = getNotNothingString(messagesAr, "errorNotFound", errorInMethod);
    errorNotFoundInAr = getNotNothingString(messagesAr, "errorNotFoundIn", errorInMethod);
    errorOdvLLTGridAr = getNotNothingString(messagesAr, "errorOdvLLTGrid", errorInMethod);
    errorOdvLLTTableAr = getNotNothingString(messagesAr, "errorOdvLLTTable", errorInMethod);
    errorOnWebPageAr = getNotNothingString(messagesAr, "errorOnWebPage", errorInMethod);

    extensionsNoRangeRequests =
        StringArray.arrayFromCSV(
            messagesAr[0].getNotNothingString("extensionsNoRangeRequests", errorInMethod),
            ",",
            true,
            false); // trim, keepNothing

    externalLinkAr = getNotNothingString(messagesAr, "externalLink", errorInMethod);
    for (int tl = 0; tl < nLanguages; tl++) externalLinkAr[tl] = " " + externalLinkAr[tl];

    externalWebSiteAr = getNotNothingString(messagesAr, "externalWebSite", errorInMethod);
    fileHelp_ascAr = getNotNothingString(messagesAr, "fileHelp_asc", errorInMethod);
    fileHelp_csvAr = getNotNothingString(messagesAr, "fileHelp_csv", errorInMethod);
    fileHelp_csvpAr = getNotNothingString(messagesAr, "fileHelp_csvp", errorInMethod);
    fileHelp_csv0Ar = getNotNothingString(messagesAr, "fileHelp_csv0", errorInMethod);
    fileHelp_dataTableAr = getNotNothingString(messagesAr, "fileHelp_dataTable", errorInMethod);
    fileHelp_dasAr = getNotNothingString(messagesAr, "fileHelp_das", errorInMethod);
    fileHelp_ddsAr = getNotNothingString(messagesAr, "fileHelp_dds", errorInMethod);
    fileHelp_dodsAr = getNotNothingString(messagesAr, "fileHelp_dods", errorInMethod);
    fileHelpGrid_esriAsciiAr =
        getNotNothingString(messagesAr, "fileHelpGrid_esriAscii", errorInMethod);
    fileHelpTable_esriCsvAr =
        getNotNothingString(messagesAr, "fileHelpTable_esriCsv", errorInMethod);
    fileHelp_fgdcAr = getNotNothingString(messagesAr, "fileHelp_fgdc", errorInMethod);
    fileHelp_geoJsonAr = getNotNothingString(messagesAr, "fileHelp_geoJson", errorInMethod);
    fileHelp_graphAr = getNotNothingString(messagesAr, "fileHelp_graph", errorInMethod);
    fileHelpGrid_helpAr = getNotNothingString(messagesAr, "fileHelpGrid_help", errorInMethod);
    fileHelpTable_helpAr = getNotNothingString(messagesAr, "fileHelpTable_help", errorInMethod);
    fileHelp_htmlAr = getNotNothingString(messagesAr, "fileHelp_html", errorInMethod);
    fileHelp_htmlTableAr = getNotNothingString(messagesAr, "fileHelp_htmlTable", errorInMethod);
    fileHelp_iso19115Ar = getNotNothingString(messagesAr, "fileHelp_iso19115", errorInMethod);
    fileHelp_itxGridAr = getNotNothingString(messagesAr, "fileHelp_itxGrid", errorInMethod);
    fileHelp_itxTableAr = getNotNothingString(messagesAr, "fileHelp_itxTable", errorInMethod);
    fileHelp_jsonAr = getNotNothingString(messagesAr, "fileHelp_json", errorInMethod);
    fileHelp_jsonlCSV1Ar = getNotNothingString(messagesAr, "fileHelp_jsonlCSV1", errorInMethod);
    fileHelp_jsonlCSVAr = getNotNothingString(messagesAr, "fileHelp_jsonlCSV", errorInMethod);
    fileHelp_jsonlKVPAr = getNotNothingString(messagesAr, "fileHelp_jsonlKVP", errorInMethod);
    fileHelp_matAr = getNotNothingString(messagesAr, "fileHelp_mat", errorInMethod);
    fileHelpGrid_nc3Ar = getNotNothingString(messagesAr, "fileHelpGrid_nc3", errorInMethod);
    fileHelpGrid_nc4Ar = getNotNothingString(messagesAr, "fileHelpGrid_nc4", errorInMethod);
    fileHelpTable_nc3Ar = getNotNothingString(messagesAr, "fileHelpTable_nc3", errorInMethod);
    fileHelpTable_nc4Ar = getNotNothingString(messagesAr, "fileHelpTable_nc4", errorInMethod);
    fileHelp_nc3HeaderAr = getNotNothingString(messagesAr, "fileHelp_nc3Header", errorInMethod);
    fileHelp_nc4HeaderAr = getNotNothingString(messagesAr, "fileHelp_nc4Header", errorInMethod);
    fileHelp_nccsvAr = getNotNothingString(messagesAr, "fileHelp_nccsv", errorInMethod);
    fileHelp_nccsvMetadataAr =
        getNotNothingString(messagesAr, "fileHelp_nccsvMetadata", errorInMethod);
    fileHelp_ncCFAr = getNotNothingString(messagesAr, "fileHelp_ncCF", errorInMethod);
    fileHelp_ncCFHeaderAr = getNotNothingString(messagesAr, "fileHelp_ncCFHeader", errorInMethod);
    fileHelp_ncCFMAAr = getNotNothingString(messagesAr, "fileHelp_ncCFMA", errorInMethod);
    fileHelp_ncCFMAHeaderAr =
        getNotNothingString(messagesAr, "fileHelp_ncCFMAHeader", errorInMethod);
    fileHelp_ncmlAr = getNotNothingString(messagesAr, "fileHelp_ncml", errorInMethod);
    fileHelp_ncoJsonAr = getNotNothingString(messagesAr, "fileHelp_ncoJson", errorInMethod);
    fileHelpGrid_odvTxtAr = getNotNothingString(messagesAr, "fileHelpGrid_odvTxt", errorInMethod);
    fileHelpTable_odvTxtAr = getNotNothingString(messagesAr, "fileHelpTable_odvTxt", errorInMethod);
    fileHelp_parquetAr = getNotNothingString(messagesAr, "fileHelp_parquet", errorInMethod);
    fileHelp_parquet_with_metaAr =
        getNotNothingString(messagesAr, "fileHelp_parquet_with_meta", errorInMethod);
    fileHelp_subsetAr = getNotNothingString(messagesAr, "fileHelp_subset", errorInMethod);
    fileHelp_timeGapsAr = getNotNothingString(messagesAr, "fileHelp_timeGaps", errorInMethod);
    fileHelp_tsvAr = getNotNothingString(messagesAr, "fileHelp_tsv", errorInMethod);
    fileHelp_tsvpAr = getNotNothingString(messagesAr, "fileHelp_tsvp", errorInMethod);
    fileHelp_tsv0Ar = getNotNothingString(messagesAr, "fileHelp_tsv0", errorInMethod);
    fileHelp_wavAr = getNotNothingString(messagesAr, "fileHelp_wav", errorInMethod);
    fileHelp_xhtmlAr = getNotNothingString(messagesAr, "fileHelp_xhtml", errorInMethod);
    fileHelp_geotifAr = getNotNothingString(messagesAr, "fileHelp_geotif", errorInMethod);
    fileHelpGrid_kmlAr = getNotNothingString(messagesAr, "fileHelpGrid_kml", errorInMethod);
    fileHelpTable_kmlAr = getNotNothingString(messagesAr, "fileHelpTable_kml", errorInMethod);
    fileHelp_smallPdfAr = getNotNothingString(messagesAr, "fileHelp_smallPdf", errorInMethod);
    fileHelp_pdfAr = getNotNothingString(messagesAr, "fileHelp_pdf", errorInMethod);
    fileHelp_largePdfAr = getNotNothingString(messagesAr, "fileHelp_largePdf", errorInMethod);
    fileHelp_smallPngAr = getNotNothingString(messagesAr, "fileHelp_smallPng", errorInMethod);
    fileHelp_pngAr = getNotNothingString(messagesAr, "fileHelp_png", errorInMethod);
    fileHelp_largePngAr = getNotNothingString(messagesAr, "fileHelp_largePng", errorInMethod);
    fileHelp_transparentPngAr =
        getNotNothingString(messagesAr, "fileHelp_transparentPng", errorInMethod);
    filesDescriptionAr = getNotNothingString(messagesAr, "filesDescription", errorInMethod);
    filesDocumentationAr = getNotNothingString(messagesAr, "filesDocumentation", errorInMethod);
    filesSortAr = getNotNothingString(messagesAr, "filesSort", errorInMethod);
    filesWarningAr = getNotNothingString(messagesAr, "filesWarning", errorInMethod);
    findOutChangeAr = getNotNothingString(messagesAr, "findOutChange", errorInMethod);
    FIPSCountyCodesAr = getNotNothingString(messagesAr, "FIPSCountyCodes", errorInMethod);
    forSOSUseAr = getNotNothingString(messagesAr, "forSOSUse", errorInMethod);
    forWCSUseAr = getNotNothingString(messagesAr, "forWCSUse", errorInMethod);
    forWMSUseAr = getNotNothingString(messagesAr, "forWMSUse", errorInMethod);
    functionsAr = getNotNothingString(messagesAr, "functions", errorInMethod);
    functionTooltipAr = getNotNothingString(messagesAr, "functionTooltip", errorInMethod);
    for (int tl = 0; tl < nLanguages; tl++)
      functionTooltipAr[tl] = MessageFormat.format(functionTooltipAr[tl], "distinct()");

    functionDistinctCheckAr =
        getNotNothingString(messagesAr, "functionDistinctCheck", errorInMethod);
    functionDistinctTooltipAr =
        getNotNothingString(messagesAr, "functionDistinctTooltip", errorInMethod);
    for (int tl = 0; tl < nLanguages; tl++)
      functionDistinctTooltipAr[tl] =
          MessageFormat.format(functionDistinctTooltipAr[tl], "distinct()");

    functionOrderByExtraAr = getNotNothingString(messagesAr, "functionOrderByExtra", errorInMethod);
    functionOrderByTooltipAr =
        getNotNothingString(messagesAr, "functionOrderByTooltip", errorInMethod);
    functionOrderBySortAr = getNotNothingString(messagesAr, "functionOrderBySort", errorInMethod);
    functionOrderBySort1Ar = getNotNothingString(messagesAr, "functionOrderBySort1", errorInMethod);
    functionOrderBySort2Ar = getNotNothingString(messagesAr, "functionOrderBySort2", errorInMethod);
    functionOrderBySort3Ar = getNotNothingString(messagesAr, "functionOrderBySort3", errorInMethod);
    functionOrderBySort4Ar = getNotNothingString(messagesAr, "functionOrderBySort4", errorInMethod);
    functionOrderBySortLeastAr =
        getNotNothingString(messagesAr, "functionOrderBySortLeast", errorInMethod);
    functionOrderBySortRowMaxAr =
        getNotNothingString(messagesAr, "functionOrderBySortRowMax", errorInMethod);
    generatedAtAr = getNotNothingString(messagesAr, "generatedAt", errorInMethod);
    geoServicesDescriptionAr =
        getNotNothingString(messagesAr, "geoServicesDescription", errorInMethod);
    getStartedHtmlAr = getNotNothingString(messagesAr, "getStartedHtml", errorInMethod);
    helpAr = getNotNothingString(messagesAr, "help", errorInMethod);
    htmlTableMaxMessageAr = getNotNothingString(messagesAr, "htmlTableMaxMessage", errorInMethod);

    imageDataCourtesyOfAr = getNotNothingString(messagesAr, "imageDataCourtesyOf", errorInMethod);
    imageWidths =
        String2.toIntArray(
            String2.split(messagesAr[0].getNotNothingString("imageWidths", errorInMethod), ','));
    imageHeights =
        String2.toIntArray(
            String2.split(messagesAr[0].getNotNothingString("imageHeights", errorInMethod), ','));
    imagesEmbedAr = getNotNothingString(messagesAr, "imagesEmbed", errorInMethod);
    indexViewAllAr = getNotNothingString(messagesAr, "indexViewAll", errorInMethod);
    indexSearchWithAr = getNotNothingString(messagesAr, "indexSearchWith", errorInMethod);
    indexDevelopersSearchAr =
        getNotNothingString(messagesAr, "indexDevelopersSearch", errorInMethod);
    indexProtocolAr = getNotNothingString(messagesAr, "indexProtocol", errorInMethod);
    indexDescriptionAr = getNotNothingString(messagesAr, "indexDescription", errorInMethod);
    indexDatasetsAr = getNotNothingString(messagesAr, "indexDatasets", errorInMethod);
    indexDocumentationAr = getNotNothingString(messagesAr, "indexDocumentation", errorInMethod);
    indexRESTfulSearchAr = getNotNothingString(messagesAr, "indexRESTfulSearch", errorInMethod);
    indexAllDatasetsSearchAr =
        getNotNothingString(messagesAr, "indexAllDatasetsSearch", errorInMethod);
    indexOpenSearchAr = getNotNothingString(messagesAr, "indexOpenSearch", errorInMethod);
    indexServicesAr = getNotNothingString(messagesAr, "indexServices", errorInMethod);
    indexDescribeServicesAr =
        getNotNothingString(messagesAr, "indexDescribeServices", errorInMethod);
    indexMetadataAr = getNotNothingString(messagesAr, "indexMetadata", errorInMethod);
    indexWAF1Ar = getNotNothingString(messagesAr, "indexWAF1", errorInMethod);
    indexWAF2Ar = getNotNothingString(messagesAr, "indexWAF2", errorInMethod);
    indexConvertersAr = getNotNothingString(messagesAr, "indexConverters", errorInMethod);
    indexDescribeConvertersAr =
        getNotNothingString(messagesAr, "indexDescribeConverters", errorInMethod);
    infoAboutFromAr = getNotNothingString(messagesAr, "infoAboutFrom", errorInMethod);
    infoTableTitleHtmlAr = getNotNothingString(messagesAr, "infoTableTitleHtml", errorInMethod);
    infoRequestFormAr = getNotNothingString(messagesAr, "infoRequestForm", errorInMethod);
    informationAr = getNotNothingString(messagesAr, "information", errorInMethod);
    inotifyFixAr = getNotNothingString(messagesAr, "inotifyFix", errorInMethod);
    inotifyFixCommands = messagesAr[0].getNotNothingString("inotifyFixCommands", errorInMethod);
    for (int tl = 0; tl < nLanguages; tl++)
      inotifyFixAr[tl] = MessageFormat.format(inotifyFixAr[tl], inotifyFixCommands);
    interpolateAr = getNotNothingString(messagesAr, "interpolate", errorInMethod);
    javaProgramsHTMLAr = getNotNothingString(messagesAr, "javaProgramsHTML", errorInMethod);
    justGenerateAndViewAr = getNotNothingString(messagesAr, "justGenerateAndView", errorInMethod);
    justGenerateAndViewTooltipAr =
        getNotNothingString(messagesAr, "justGenerateAndViewTooltip", errorInMethod);
    justGenerateAndViewUrlAr =
        getNotNothingString(messagesAr, "justGenerateAndViewUrl", errorInMethod);
    justGenerateAndViewGraphUrlTooltipAr =
        getNotNothingString(messagesAr, "justGenerateAndViewGraphUrlTooltip", errorInMethod);
    keywordsAr = getNotNothingString(messagesAr, "keywords", errorInMethod);
    langCodeAr = getNotNothingString(messagesAr, "langCode", errorInMethod);

    legal = messagesAr[0].getNotNothingString("legal", errorInMethod);
    legal = getSetupEVString(setup, ev, "legal", legal); // optionally in setup.xml
    legalNoticesAr = getNotNothingString(messagesAr, "legalNotices", errorInMethod);
    legalNoticesTitleAr = getNotNothingString(messagesAr, "legalNoticesTitle", errorInMethod);

    legendTitle1 = messagesAr[0].getString("legendTitle1", "");
    legendTitle2 = messagesAr[0].getString("legendTitle2", "");
    legendTitle1 =
        getSetupEVString(setup, ev, "legendTitle1", legendTitle1); // optionally in setup.xml
    legendTitle2 =
        getSetupEVString(setup, ev, "legendTitle2", legendTitle2); // optionally in setup.xml

    licenseAr = getNotNothingString(messagesAr, "license", errorInMethod);
    likeThisAr = getNotNothingString(messagesAr, "likeThis", errorInMethod);
    listAllAr = getNotNothingString(messagesAr, "listAll", errorInMethod);
    listOfDatasetsAr = getNotNothingString(messagesAr, "listOfDatasets", errorInMethod);
    LogInAr = getNotNothingString(messagesAr, "LogIn", errorInMethod);
    loginAr = getNotNothingString(messagesAr, "login", errorInMethod);
    loginHTMLAr = getNotNothingString(messagesAr, "loginHTML", errorInMethod);
    loginAttemptBlockedAr = getNotNothingString(messagesAr, "loginAttemptBlocked", errorInMethod);
    loginDescribeCustomAr = getNotNothingString(messagesAr, "loginDescribeCustom", errorInMethod);
    loginDescribeEmailAr = getNotNothingString(messagesAr, "loginDescribeEmail", errorInMethod);
    loginDescribeGoogleAr = getNotNothingString(messagesAr, "loginDescribeGoogle", errorInMethod);
    loginDescribeOrcidAr = getNotNothingString(messagesAr, "loginDescribeOrcid", errorInMethod);
    loginDescribeOauth2Ar = getNotNothingString(messagesAr, "loginDescribeOauth2", errorInMethod);
    loginCanNotAr = getNotNothingString(messagesAr, "loginCanNot", errorInMethod);
    loginAreNotAr = getNotNothingString(messagesAr, "loginAreNot", errorInMethod);
    loginToLogInAr = getNotNothingString(messagesAr, "loginToLogIn", errorInMethod);
    loginEmailAddressAr = getNotNothingString(messagesAr, "loginEmailAddress", errorInMethod);
    loginYourEmailAddressAr =
        getNotNothingString(messagesAr, "loginYourEmailAddress", errorInMethod);
    loginUserNameAr = getNotNothingString(messagesAr, "loginUserName", errorInMethod);
    loginPasswordAr = getNotNothingString(messagesAr, "loginPassword", errorInMethod);
    loginUserNameAndPasswordAr =
        getNotNothingString(messagesAr, "loginUserNameAndPassword", errorInMethod);
    loginGoogleSignInAr = getNotNothingString(messagesAr, "loginGoogleSignIn", errorInMethod);
    loginOrcidSignInAr = getNotNothingString(messagesAr, "loginOrcidSignIn", errorInMethod);
    loginErddapAr = getNotNothingString(messagesAr, "loginErddap", errorInMethod);
    loginOpenIDAr = getNotNothingString(messagesAr, "loginOpenID", errorInMethod);
    loginOpenIDOrAr = getNotNothingString(messagesAr, "loginOpenIDOr", errorInMethod);
    loginOpenIDCreateAr = getNotNothingString(messagesAr, "loginOpenIDCreate", errorInMethod);
    loginOpenIDFreeAr = getNotNothingString(messagesAr, "loginOpenIDFree", errorInMethod);
    loginOpenIDSameAr = getNotNothingString(messagesAr, "loginOpenIDSame", errorInMethod);
    loginAsAr = getNotNothingString(messagesAr, "loginAs", errorInMethod);
    loginPartwayAsAr = getNotNothingString(messagesAr, "loginPartwayAs", errorInMethod);
    loginFailedAr = getNotNothingString(messagesAr, "loginFailed", errorInMethod);
    loginSucceededAr = getNotNothingString(messagesAr, "loginSucceeded", errorInMethod);
    loginInvalidAr = getNotNothingString(messagesAr, "loginInvalid", errorInMethod);
    loginNotAr = getNotNothingString(messagesAr, "loginNot", errorInMethod);
    loginBackAr = getNotNothingString(messagesAr, "loginBack", errorInMethod);
    loginProblemExactAr = getNotNothingString(messagesAr, "loginProblemExact", errorInMethod);
    loginProblemExpireAr = getNotNothingString(messagesAr, "loginProblemExpire", errorInMethod);
    loginProblemGoogleAgainAr =
        getNotNothingString(messagesAr, "loginProblemGoogleAgain", errorInMethod);
    loginProblemOrcidAgainAr =
        getNotNothingString(messagesAr, "loginProblemOrcidAgain", errorInMethod);
    loginProblemOauth2AgainAr =
        getNotNothingString(messagesAr, "loginProblemOauth2Again", errorInMethod);
    loginProblemSameBrowserAr =
        getNotNothingString(messagesAr, "loginProblemSameBrowser", errorInMethod);
    loginProblem3TimesAr = getNotNothingString(messagesAr, "loginProblem3Times", errorInMethod);
    loginProblemsAr = getNotNothingString(messagesAr, "loginProblems", errorInMethod);
    loginProblemsAfterAr = getNotNothingString(messagesAr, "loginProblemsAfter", errorInMethod);
    loginPublicAccessAr = getNotNothingString(messagesAr, "loginPublicAccess", errorInMethod);
    LogOutAr = getNotNothingString(messagesAr, "LogOut", errorInMethod);
    logoutAr = getNotNothingString(messagesAr, "logout", errorInMethod);
    logoutOpenIDAr = getNotNothingString(messagesAr, "logoutOpenID", errorInMethod);
    logoutSuccessAr = getNotNothingString(messagesAr, "logoutSuccess", errorInMethod);
    magAr = getNotNothingString(messagesAr, "mag", errorInMethod);
    magAxisXAr = getNotNothingString(messagesAr, "magAxisX", errorInMethod);
    magAxisYAr = getNotNothingString(messagesAr, "magAxisY", errorInMethod);
    magAxisColorAr = getNotNothingString(messagesAr, "magAxisColor", errorInMethod);
    magAxisStickXAr = getNotNothingString(messagesAr, "magAxisStickX", errorInMethod);
    magAxisStickYAr = getNotNothingString(messagesAr, "magAxisStickY", errorInMethod);
    magAxisVectorXAr = getNotNothingString(messagesAr, "magAxisVectorX", errorInMethod);
    magAxisVectorYAr = getNotNothingString(messagesAr, "magAxisVectorY", errorInMethod);
    magAxisHelpGraphXAr = getNotNothingString(messagesAr, "magAxisHelpGraphX", errorInMethod);
    magAxisHelpGraphYAr = getNotNothingString(messagesAr, "magAxisHelpGraphY", errorInMethod);
    magAxisHelpMarkerColorAr =
        getNotNothingString(messagesAr, "magAxisHelpMarkerColor", errorInMethod);
    magAxisHelpSurfaceColorAr =
        getNotNothingString(messagesAr, "magAxisHelpSurfaceColor", errorInMethod);
    magAxisHelpStickXAr = getNotNothingString(messagesAr, "magAxisHelpStickX", errorInMethod);
    magAxisHelpStickYAr = getNotNothingString(messagesAr, "magAxisHelpStickY", errorInMethod);
    magAxisHelpMapXAr = getNotNothingString(messagesAr, "magAxisHelpMapX", errorInMethod);
    magAxisHelpMapYAr = getNotNothingString(messagesAr, "magAxisHelpMapY", errorInMethod);
    magAxisHelpVectorXAr = getNotNothingString(messagesAr, "magAxisHelpVectorX", errorInMethod);
    magAxisHelpVectorYAr = getNotNothingString(messagesAr, "magAxisHelpVectorY", errorInMethod);
    magAxisVarHelpAr = getNotNothingString(messagesAr, "magAxisVarHelp", errorInMethod);
    magAxisVarHelpGridAr = getNotNothingString(messagesAr, "magAxisVarHelpGrid", errorInMethod);
    magConstraintHelpAr = getNotNothingString(messagesAr, "magConstraintHelp", errorInMethod);
    magDocumentationAr = getNotNothingString(messagesAr, "magDocumentation", errorInMethod);
    magDownloadAr = getNotNothingString(messagesAr, "magDownload", errorInMethod);
    magDownloadTooltipAr = getNotNothingString(messagesAr, "magDownloadTooltip", errorInMethod);
    magFileTypeAr = getNotNothingString(messagesAr, "magFileType", errorInMethod);
    magGraphTypeAr = getNotNothingString(messagesAr, "magGraphType", errorInMethod);
    magGraphTypeTooltipGridAr =
        getNotNothingString(messagesAr, "magGraphTypeTooltipGrid", errorInMethod);
    magGraphTypeTooltipTableAr =
        getNotNothingString(messagesAr, "magGraphTypeTooltipTable", errorInMethod);
    magGSAr = getNotNothingString(messagesAr, "magGS", errorInMethod);
    magGSMarkerTypeAr = getNotNothingString(messagesAr, "magGSMarkerType", errorInMethod);
    magGSSizeAr = getNotNothingString(messagesAr, "magGSSize", errorInMethod);
    magGSColorAr = getNotNothingString(messagesAr, "magGSColor", errorInMethod);
    magGSColorBarAr = getNotNothingString(messagesAr, "magGSColorBar", errorInMethod);
    magGSColorBarTooltipAr = getNotNothingString(messagesAr, "magGSColorBarTooltip", errorInMethod);
    magGSContinuityAr = getNotNothingString(messagesAr, "magGSContinuity", errorInMethod);
    magGSContinuityTooltipAr =
        getNotNothingString(messagesAr, "magGSContinuityTooltip", errorInMethod);
    magGSScaleAr = getNotNothingString(messagesAr, "magGSScale", errorInMethod);
    magGSScaleTooltipAr = getNotNothingString(messagesAr, "magGSScaleTooltip", errorInMethod);
    magGSMinAr = getNotNothingString(messagesAr, "magGSMin", errorInMethod);
    magGSMinTooltipAr = getNotNothingString(messagesAr, "magGSMinTooltip", errorInMethod);
    magGSMaxAr = getNotNothingString(messagesAr, "magGSMax", errorInMethod);
    magGSMaxTooltipAr = getNotNothingString(messagesAr, "magGSMaxTooltip", errorInMethod);
    magGSNSectionsAr = getNotNothingString(messagesAr, "magGSNSections", errorInMethod);
    magGSNSectionsTooltipAr =
        getNotNothingString(messagesAr, "magGSNSectionsTooltip", errorInMethod);
    magGSLandMaskAr = getNotNothingString(messagesAr, "magGSLandMask", errorInMethod);
    magGSLandMaskTooltipGridAr =
        getNotNothingString(messagesAr, "magGSLandMaskTooltipGrid", errorInMethod);
    magGSLandMaskTooltipTableAr =
        getNotNothingString(messagesAr, "magGSLandMaskTooltipTable", errorInMethod);
    magGSVectorStandardAr = getNotNothingString(messagesAr, "magGSVectorStandard", errorInMethod);
    magGSVectorStandardTooltipAr =
        getNotNothingString(messagesAr, "magGSVectorStandardTooltip", errorInMethod);
    magGSYAscendingTooltipAr =
        getNotNothingString(messagesAr, "magGSYAscendingTooltip", errorInMethod);
    magGSYAxisMinAr = getNotNothingString(messagesAr, "magGSYAxisMin", errorInMethod);
    magGSYAxisMaxAr = getNotNothingString(messagesAr, "magGSYAxisMax", errorInMethod);
    magGSYRangeMinTooltipAr =
        getNotNothingString(messagesAr, "magGSYRangeMinTooltip", errorInMethod);
    magGSYRangeMaxTooltipAr =
        getNotNothingString(messagesAr, "magGSYRangeMaxTooltip", errorInMethod);
    magGSYRangeTooltipAr = getNotNothingString(messagesAr, "magGSYRangeTooltip", errorInMethod);
    magGSYScaleTooltipAr = getNotNothingString(messagesAr, "magGSYScaleTooltip", errorInMethod);
    magItemFirstAr = getNotNothingString(messagesAr, "magItemFirst", errorInMethod);
    magItemPreviousAr = getNotNothingString(messagesAr, "magItemPrevious", errorInMethod);
    magItemNextAr = getNotNothingString(messagesAr, "magItemNext", errorInMethod);
    magItemLastAr = getNotNothingString(messagesAr, "magItemLast", errorInMethod);
    magJust1ValueAr = getNotNothingString(messagesAr, "magJust1Value", errorInMethod);
    magRangeAr = getNotNothingString(messagesAr, "magRange", errorInMethod);
    magRangeToAr = getNotNothingString(messagesAr, "magRangeTo", errorInMethod);
    magRedrawAr = getNotNothingString(messagesAr, "magRedraw", errorInMethod);
    magRedrawTooltipAr = getNotNothingString(messagesAr, "magRedrawTooltip", errorInMethod);
    magTimeRangeAr = getNotNothingString(messagesAr, "magTimeRange", errorInMethod);
    magTimeRangeFirstAr = getNotNothingString(messagesAr, "magTimeRangeFirst", errorInMethod);
    magTimeRangeBackAr = getNotNothingString(messagesAr, "magTimeRangeBack", errorInMethod);
    magTimeRangeForwardAr = getNotNothingString(messagesAr, "magTimeRangeForward", errorInMethod);
    magTimeRangeLastAr = getNotNothingString(messagesAr, "magTimeRangeLast", errorInMethod);
    magTimeRangeTooltipAr = getNotNothingString(messagesAr, "magTimeRangeTooltip", errorInMethod);
    magTimeRangeTooltip2Ar = getNotNothingString(messagesAr, "magTimeRangeTooltip2", errorInMethod);
    magTimesVaryAr = getNotNothingString(messagesAr, "magTimesVary", errorInMethod);
    magViewUrlAr = getNotNothingString(messagesAr, "magViewUrl", errorInMethod);
    magZoomAr = getNotNothingString(messagesAr, "magZoom", errorInMethod);
    magZoomCenterAr = getNotNothingString(messagesAr, "magZoomCenter", errorInMethod);
    magZoomCenterTooltipAr = getNotNothingString(messagesAr, "magZoomCenterTooltip", errorInMethod);
    magZoomInAr = getNotNothingString(messagesAr, "magZoomIn", errorInMethod);
    magZoomInTooltipAr = getNotNothingString(messagesAr, "magZoomInTooltip", errorInMethod);
    magZoomOutAr = getNotNothingString(messagesAr, "magZoomOut", errorInMethod);
    magZoomOutTooltipAr = getNotNothingString(messagesAr, "magZoomOutTooltip", errorInMethod);
    magZoomALittleAr = getNotNothingString(messagesAr, "magZoomALittle", errorInMethod);
    magZoomDataAr = getNotNothingString(messagesAr, "magZoomData", errorInMethod);
    magZoomOutDataAr = getNotNothingString(messagesAr, "magZoomOutData", errorInMethod);
    magGridTooltipAr = getNotNothingString(messagesAr, "magGridTooltip", errorInMethod);
    magTableTooltipAr = getNotNothingString(messagesAr, "magTableTooltip", errorInMethod);

    metadataDownloadAr = getNotNothingString(messagesAr, "metadataDownload", errorInMethod);
    moreInformationAr = getNotNothingString(messagesAr, "moreInformation", errorInMethod);

    nMatching1Ar = getNotNothingString(messagesAr, "nMatching1", errorInMethod);
    nMatchingAr = getNotNothingString(messagesAr, "nMatching", errorInMethod);
    nMatchingAlphabeticalAr =
        getNotNothingString(messagesAr, "nMatchingAlphabetical", errorInMethod);
    nMatchingMostRelevantAr =
        getNotNothingString(messagesAr, "nMatchingMostRelevant", errorInMethod);
    nMatchingPageAr = getNotNothingString(messagesAr, "nMatchingPage", errorInMethod);
    nMatchingCurrentAr = getNotNothingString(messagesAr, "nMatchingCurrent", errorInMethod);
    noDataFixedValueAr = getNotNothingString(messagesAr, "noDataFixedValue", errorInMethod);
    noDataNoLLAr = getNotNothingString(messagesAr, "noDataNoLL", errorInMethod);
    noDatasetWithAr = getNotNothingString(messagesAr, "noDatasetWith", errorInMethod);
    noPage1Ar = getNotNothingString(messagesAr, "noPage1", errorInMethod);
    noPage2Ar = getNotNothingString(messagesAr, "noPage2", errorInMethod);
    notAllowedAr = getNotNothingString(messagesAr, "notAllowed", errorInMethod);
    notAuthorizedAr = getNotNothingString(messagesAr, "notAuthorized", errorInMethod);
    notAuthorizedForDataAr = getNotNothingString(messagesAr, "notAuthorizedForData", errorInMethod);
    notAvailableAr = getNotNothingString(messagesAr, "notAvailable", errorInMethod);
    noteAr = getNotNothingString(messagesAr, "note", errorInMethod);
    noXxxAr = getNotNothingString(messagesAr, "noXxx", errorInMethod);
    noXxxBecauseAr = getNotNothingString(messagesAr, "noXxxBecause", errorInMethod);
    noXxxBecause2Ar = getNotNothingString(messagesAr, "noXxxBecause2", errorInMethod);
    noXxxNotActiveAr = getNotNothingString(messagesAr, "noXxxNotActive", errorInMethod);
    noXxxNoAxis1Ar = getNotNothingString(messagesAr, "noXxxNoAxis1", errorInMethod);
    noXxxNoCdmDataTypeAr = getNotNothingString(messagesAr, "noXxxNoCdmDataType", errorInMethod);
    noXxxNoColorBarAr = getNotNothingString(messagesAr, "noXxxNoColorBar", errorInMethod);
    noXxxNoLLAr = getNotNothingString(messagesAr, "noXxxNoLL", errorInMethod);
    noXxxNoLLEvenlySpacedAr =
        getNotNothingString(messagesAr, "noXxxNoLLEvenlySpaced", errorInMethod);
    noXxxNoLLGt1Ar = getNotNothingString(messagesAr, "noXxxNoLLGt1", errorInMethod);
    noXxxNoLLTAr = getNotNothingString(messagesAr, "noXxxNoLLT", errorInMethod);
    noXxxNoLonIn180Ar = getNotNothingString(messagesAr, "noXxxNoLonIn180", errorInMethod);
    noXxxNoNonStringAr = getNotNothingString(messagesAr, "noXxxNoNonString", errorInMethod);
    noXxxNo2NonStringAr = getNotNothingString(messagesAr, "noXxxNo2NonString", errorInMethod);
    noXxxNoStationAr = getNotNothingString(messagesAr, "noXxxNoStation", errorInMethod);
    noXxxNoStationIDAr = getNotNothingString(messagesAr, "noXxxNoStationID", errorInMethod);
    noXxxNoSubsetVariablesAr =
        getNotNothingString(messagesAr, "noXxxNoSubsetVariables", errorInMethod);
    noXxxNoOLLSubsetVariablesAr =
        getNotNothingString(messagesAr, "noXxxNoOLLSubsetVariables", errorInMethod);
    noXxxNoMinMaxAr = getNotNothingString(messagesAr, "noXxxNoMinMax", errorInMethod);
    noXxxItsGriddedAr = getNotNothingString(messagesAr, "noXxxItsGridded", errorInMethod);
    noXxxItsTabularAr = getNotNothingString(messagesAr, "noXxxItsTabular", errorInMethod);
    oneRequestAtATimeAr = getNotNothingString(messagesAr, "oneRequestAtATime", errorInMethod);
    openSearchDescriptionAr =
        getNotNothingString(messagesAr, "openSearchDescription", errorInMethod);
    optionalAr = getNotNothingString(messagesAr, "optional", errorInMethod);
    optionsAr = getNotNothingString(messagesAr, "options", errorInMethod);
    orAListOfValuesAr = getNotNothingString(messagesAr, "orAListOfValues", errorInMethod);
    orRefineSearchWithAr = getNotNothingString(messagesAr, "orRefineSearchWith", errorInMethod);
    orSearchWithAr = getNotNothingString(messagesAr, "orSearchWith", errorInMethod);
    orCommaAr = getNotNothingString(messagesAr, "orComma", errorInMethod);
    for (int tl = 0; tl < nLanguages; tl++) {
      orRefineSearchWithAr[tl] += " ";
      orSearchWithAr[tl] += " ";
      orCommaAr[tl] += " ";
    }
    otherFeaturesAr = getNotNothingString(messagesAr, "otherFeatures", errorInMethod);
    outOfDateDatasetsAr = getNotNothingString(messagesAr, "outOfDateDatasets", errorInMethod);
    outOfDateHtmlAr = getNotNothingString(messagesAr, "outOfDateHtml", errorInMethod);
    outOfDateKeepTrackAr = getNotNothingString(messagesAr, "outOfDateKeepTrack", errorInMethod);

    // just one set of palettes info (from messagesAr[0])
    palettes = String2.split(messagesAr[0].getNotNothingString("palettes", errorInMethod), ',');
    DEFAULT_palettes = palettes; // used by LoadDatasets if palettes tag is empty
    DEFAULT_palettes_set = String2.stringArrayToSet(palettes);
    palettes0 = new String[palettes.length + 1];
    palettes0[0] = "";
    System.arraycopy(palettes, 0, palettes0, 1, palettes.length);

    patientDataAr = getNotNothingString(messagesAr, "patientData", errorInMethod);
    patientYourGraphAr = getNotNothingString(messagesAr, "patientYourGraph", errorInMethod);

    pdfWidths =
        String2.toIntArray(
            String2.split(messagesAr[0].getNotNothingString("pdfWidths", errorInMethod), ','));
    pdfHeights =
        String2.toIntArray(
            String2.split(messagesAr[0].getNotNothingString("pdfHeights", errorInMethod), ','));

    percentEncodeAr = getNotNothingString(messagesAr, "percentEncode", errorInMethod);
    pickADatasetAr = getNotNothingString(messagesAr, "pickADataset", errorInMethod);
    protocolSearchHtmlAr = getNotNothingString(messagesAr, "protocolSearchHtml", errorInMethod);
    protocolSearch2HtmlAr = getNotNothingString(messagesAr, "protocolSearch2Html", errorInMethod);
    protocolClickAr = getNotNothingString(messagesAr, "protocolClick", errorInMethod);
    queryErrorAr = getNotNothingString(messagesAr, "queryError", errorInMethod);
    for (int tl = 0; tl < nLanguages; tl++) queryErrorAr[tl] += " ";
    queryError180Ar = getNotNothingString(messagesAr, "queryError180", errorInMethod);
    queryError1ValueAr = getNotNothingString(messagesAr, "queryError1Value", errorInMethod);
    queryError1VarAr = getNotNothingString(messagesAr, "queryError1Var", errorInMethod);
    queryError2VarAr = getNotNothingString(messagesAr, "queryError2Var", errorInMethod);
    queryErrorActualRangeAr =
        getNotNothingString(messagesAr, "queryErrorActualRange", errorInMethod);
    queryErrorAdjustedAr = getNotNothingString(messagesAr, "queryErrorAdjusted", errorInMethod);
    queryErrorAscendingAr = getNotNothingString(messagesAr, "queryErrorAscending", errorInMethod);
    queryErrorConstraintNaNAr =
        getNotNothingString(messagesAr, "queryErrorConstraintNaN", errorInMethod);
    queryErrorEqualSpacingAr =
        getNotNothingString(messagesAr, "queryErrorEqualSpacing", errorInMethod);
    queryErrorExpectedAtAr = getNotNothingString(messagesAr, "queryErrorExpectedAt", errorInMethod);
    queryErrorFileTypeAr = getNotNothingString(messagesAr, "queryErrorFileType", errorInMethod);
    queryErrorInvalidAr = getNotNothingString(messagesAr, "queryErrorInvalid", errorInMethod);
    queryErrorLLAr = getNotNothingString(messagesAr, "queryErrorLL", errorInMethod);
    queryErrorLLGt1Ar = getNotNothingString(messagesAr, "queryErrorLLGt1", errorInMethod);
    queryErrorLLTAr = getNotNothingString(messagesAr, "queryErrorLLT", errorInMethod);
    queryErrorNeverTrueAr = getNotNothingString(messagesAr, "queryErrorNeverTrue", errorInMethod);
    queryErrorNeverBothTrueAr =
        getNotNothingString(messagesAr, "queryErrorNeverBothTrue", errorInMethod);
    queryErrorNotAxisAr = getNotNothingString(messagesAr, "queryErrorNotAxis", errorInMethod);
    queryErrorNotExpectedAtAr =
        getNotNothingString(messagesAr, "queryErrorNotExpectedAt", errorInMethod);
    queryErrorNotFoundAfterAr =
        getNotNothingString(messagesAr, "queryErrorNotFoundAfter", errorInMethod);
    queryErrorOccursTwiceAr =
        getNotNothingString(messagesAr, "queryErrorOccursTwice", errorInMethod);

    queryErrorOrderByClosestAr =
        getNotNothingString(messagesAr, "queryErrorOrderByClosest", errorInMethod);
    queryErrorOrderByLimitAr =
        getNotNothingString(messagesAr, "queryErrorOrderByLimit", errorInMethod);
    queryErrorOrderByMeanAr =
        getNotNothingString(messagesAr, "queryErrorOrderByMean", errorInMethod);
    queryErrorOrderBySumAr = getNotNothingString(messagesAr, "queryErrorOrderBySum", errorInMethod);

    queryErrorOrderByVariableAr =
        getNotNothingString(messagesAr, "queryErrorOrderByVariable", errorInMethod);
    queryErrorUnknownVariableAr =
        getNotNothingString(messagesAr, "queryErrorUnknownVariable", errorInMethod);

    queryErrorGrid1AxisAr = getNotNothingString(messagesAr, "queryErrorGrid1Axis", errorInMethod);
    queryErrorGridAmpAr = getNotNothingString(messagesAr, "queryErrorGridAmp", errorInMethod);
    queryErrorGridDiagnosticAr =
        getNotNothingString(messagesAr, "queryErrorGridDiagnostic", errorInMethod);
    queryErrorGridBetweenAr =
        getNotNothingString(messagesAr, "queryErrorGridBetween", errorInMethod);
    queryErrorGridLessMinAr =
        getNotNothingString(messagesAr, "queryErrorGridLessMin", errorInMethod);
    queryErrorGridGreaterMaxAr =
        getNotNothingString(messagesAr, "queryErrorGridGreaterMax", errorInMethod);
    queryErrorGridMissingAr =
        getNotNothingString(messagesAr, "queryErrorGridMissing", errorInMethod);
    queryErrorGridNoAxisVarAr =
        getNotNothingString(messagesAr, "queryErrorGridNoAxisVar", errorInMethod);
    queryErrorGridNoDataVarAr =
        getNotNothingString(messagesAr, "queryErrorGridNoDataVar", errorInMethod);
    queryErrorGridNotIdenticalAr =
        getNotNothingString(messagesAr, "queryErrorGridNotIdentical", errorInMethod);
    queryErrorGridSLessSAr = getNotNothingString(messagesAr, "queryErrorGridSLessS", errorInMethod);
    queryErrorLastEndPAr = getNotNothingString(messagesAr, "queryErrorLastEndP", errorInMethod);
    queryErrorLastExpectedAr =
        getNotNothingString(messagesAr, "queryErrorLastExpected", errorInMethod);
    queryErrorLastUnexpectedAr =
        getNotNothingString(messagesAr, "queryErrorLastUnexpected", errorInMethod);
    queryErrorLastPMInvalidAr =
        getNotNothingString(messagesAr, "queryErrorLastPMInvalid", errorInMethod);
    queryErrorLastPMIntegerAr =
        getNotNothingString(messagesAr, "queryErrorLastPMInteger", errorInMethod);

    questionMarkImageFile =
        messagesAr[0].getNotNothingString("questionMarkImageFile", errorInMethod);
    questionMarkImageFile =
        getSetupEVString(setup, ev, "questionMarkImageFile", questionMarkImageFile); // optional

    rangesFromToAr = getNotNothingString(messagesAr, "rangesFromTo", errorInMethod);
    requiredAr = getNotNothingString(messagesAr, "required", errorInMethod);
    requestFormatExamplesHtmlAr =
        getNotNothingString(messagesAr, "requestFormatExamplesHtml", errorInMethod);
    resetTheFormAr = getNotNothingString(messagesAr, "resetTheForm", errorInMethod);
    resetTheFormWasAr = getNotNothingString(messagesAr, "resetTheFormWas", errorInMethod);
    resourceNotFoundAr = getNotNothingString(messagesAr, "resourceNotFound", errorInMethod);
    for (int tl = 0; tl < nLanguages; tl++) resourceNotFoundAr[tl] += " ";
    restfulWebServicesAr = getNotNothingString(messagesAr, "restfulWebServices", errorInMethod);
    restfulHTMLAr = getNotNothingString(messagesAr, "restfulHTML", errorInMethod);
    restfulHTMLContinuedAr = getNotNothingString(messagesAr, "restfulHTMLContinued", errorInMethod);
    restfulGetAllDatasetAr = getNotNothingString(messagesAr, "restfulGetAllDataset", errorInMethod);
    restfulProtocolsAr = getNotNothingString(messagesAr, "restfulProtocols", errorInMethod);
    SOSDocumentationAr = getNotNothingString(messagesAr, "SOSDocumentation", errorInMethod);
    WCSDocumentationAr = getNotNothingString(messagesAr, "WCSDocumentation", errorInMethod);
    WMSDocumentationAr = getNotNothingString(messagesAr, "WMSDocumentation", errorInMethod);
    resultsFormatExamplesHtmlAr =
        getNotNothingString(messagesAr, "resultsFormatExamplesHtml", errorInMethod);
    resultsOfSearchForAr = getNotNothingString(messagesAr, "resultsOfSearchFor", errorInMethod);
    restfulInformationFormatsAr =
        getNotNothingString(messagesAr, "restfulInformationFormats", errorInMethod);
    restfulViaServiceAr = getNotNothingString(messagesAr, "restfulViaService", errorInMethod);
    rowsAr = getNotNothingString(messagesAr, "rows", errorInMethod);
    rssNoAr = getNotNothingString(messagesAr, "rssNo", errorInMethod);
    searchTitleAr = getNotNothingString(messagesAr, "searchTitle", errorInMethod);
    searchDoFullTextHtmlAr = getNotNothingString(messagesAr, "searchDoFullTextHtml", errorInMethod);
    searchFullTextHtmlAr = getNotNothingString(messagesAr, "searchFullTextHtml", errorInMethod);
    searchButtonAr = getNotNothingString(messagesAr, "searchButton", errorInMethod);
    searchClickTipAr = getNotNothingString(messagesAr, "searchClickTip", errorInMethod);
    searchHintsLuceneTooltipAr =
        getNotNothingString(messagesAr, "searchHintsLuceneTooltip", errorInMethod);
    searchHintsOriginalTooltipAr =
        getNotNothingString(messagesAr, "searchHintsOriginalTooltip", errorInMethod);
    searchHintsTooltipAr = getNotNothingString(messagesAr, "searchHintsTooltip", errorInMethod);
    searchMultipleERDDAPsAr =
        getNotNothingString(messagesAr, "searchMultipleERDDAPs", errorInMethod);
    searchMultipleERDDAPsDescriptionAr =
        getNotNothingString(messagesAr, "searchMultipleERDDAPsDescription", errorInMethod);
    searchNotAvailableAr = getNotNothingString(messagesAr, "searchNotAvailable", errorInMethod);
    searchTipAr = getNotNothingString(messagesAr, "searchTip", errorInMethod);
    searchSpellingAr = getNotNothingString(messagesAr, "searchSpelling", errorInMethod);
    searchFewerWordsAr = getNotNothingString(messagesAr, "searchFewerWords", errorInMethod);
    searchWithQueryAr = getNotNothingString(messagesAr, "searchWithQuery", errorInMethod);
    selectNextAr = getNotNothingString(messagesAr, "selectNext", errorInMethod);
    selectPreviousAr = getNotNothingString(messagesAr, "selectPrevious", errorInMethod);
    shiftXAllTheWayLeftAr = getNotNothingString(messagesAr, "shiftXAllTheWayLeft", errorInMethod);
    shiftXLeftAr = getNotNothingString(messagesAr, "shiftXLeft", errorInMethod);
    shiftXRightAr = getNotNothingString(messagesAr, "shiftXRight", errorInMethod);
    shiftXAllTheWayRightAr = getNotNothingString(messagesAr, "shiftXAllTheWayRight", errorInMethod);

    seeProtocolDocumentationAr =
        getNotNothingString(messagesAr, "seeProtocolDocumentation", errorInMethod);

    slideSorterAr = getNotNothingString(messagesAr, "slideSorter", errorInMethod);
    SOSAr = getNotNothingString(messagesAr, "SOS", errorInMethod);
    sosDescriptionHtmlAr = getNotNothingString(messagesAr, "sosDescriptionHtml", errorInMethod);
    sosLongDescriptionHtmlAr =
        getNotNothingString(messagesAr, "sosLongDescriptionHtml", errorInMethod);
    sosOverview1Ar = getNotNothingString(messagesAr, "sosOverview1", errorInMethod);
    sosOverview2Ar = getNotNothingString(messagesAr, "sosOverview2", errorInMethod);
    sparqlP01toP02pre = messagesAr[0].getNotNothingString("sparqlP01toP02pre", errorInMethod);
    sparqlP01toP02post = messagesAr[0].getNotNothingString("sparqlP01toP02post", errorInMethod);
    ssUseAr = getNotNothingString(messagesAr, "ssUse", errorInMethod);
    ssUsePlainAr = getNotNothingString(messagesAr, "ssUse", errorInMethod); // start with this
    for (int tl = 0; tl < nLanguages; tl++) ssUsePlainAr[tl] = XML.removeHTMLTags(ssUsePlainAr[tl]);

    ssBePatientAr = getNotNothingString(messagesAr, "ssBePatient", errorInMethod);
    ssInstructionsHtmlAr = getNotNothingString(messagesAr, "ssInstructionsHtml", errorInMethod);

    statusAr = getNotNothingString(messagesAr, "status", errorInMethod);
    statusHtmlAr = getNotNothingString(messagesAr, "statusHtml", errorInMethod);
    submitAr = getNotNothingString(messagesAr, "submit", errorInMethod);
    submitTooltipAr = getNotNothingString(messagesAr, "submitTooltip", errorInMethod);
    subscriptionOfferRssAr = getNotNothingString(messagesAr, "subscriptionOfferRss", errorInMethod);
    subscriptionOfferUrlAr = getNotNothingString(messagesAr, "subscriptionOfferUrl", errorInMethod);
    subscriptionsTitleAr = getNotNothingString(messagesAr, "subscriptionsTitle", errorInMethod);
    subscriptionEmailListAr =
        getNotNothingString(messagesAr, "subscriptionEmailList", errorInMethod);
    subscriptionAddAr = getNotNothingString(messagesAr, "subscriptionAdd", errorInMethod);
    subscriptionValidateAr = getNotNothingString(messagesAr, "subscriptionValidate", errorInMethod);
    subscriptionListAr = getNotNothingString(messagesAr, "subscriptionList", errorInMethod);
    subscriptionRemoveAr = getNotNothingString(messagesAr, "subscriptionRemove", errorInMethod);
    subscription0HtmlAr = getNotNothingString(messagesAr, "subscription0Html", errorInMethod);
    subscription1HtmlAr = getNotNothingString(messagesAr, "subscription1Html", errorInMethod);
    subscription2HtmlAr = getNotNothingString(messagesAr, "subscription2Html", errorInMethod);
    subscriptionAbuseAr = getNotNothingString(messagesAr, "subscriptionAbuse", errorInMethod);
    subscriptionAddErrorAr = getNotNothingString(messagesAr, "subscriptionAddError", errorInMethod);
    subscriptionAddHtmlAr = getNotNothingString(messagesAr, "subscriptionAddHtml", errorInMethod);
    subscriptionAdd2Ar = getNotNothingString(messagesAr, "subscriptionAdd2", errorInMethod);
    subscriptionAddSuccessAr =
        getNotNothingString(messagesAr, "subscriptionAddSuccess", errorInMethod);
    subscriptionEmailAr = getNotNothingString(messagesAr, "subscriptionEmail", errorInMethod);
    subscriptionEmailOnBlacklistAr =
        getNotNothingString(messagesAr, "subscriptionEmailOnBlacklist", errorInMethod);
    subscriptionEmailInvalidAr =
        getNotNothingString(messagesAr, "subscriptionEmailInvalid", errorInMethod);
    subscriptionEmailTooLongAr =
        getNotNothingString(messagesAr, "subscriptionEmailTooLong", errorInMethod);
    subscriptionEmailUnspecifiedAr =
        getNotNothingString(messagesAr, "subscriptionEmailUnspecified", errorInMethod);
    subscriptionIDInvalidAr =
        getNotNothingString(messagesAr, "subscriptionIDInvalid", errorInMethod);
    subscriptionIDTooLongAr =
        getNotNothingString(messagesAr, "subscriptionIDTooLong", errorInMethod);
    subscriptionIDUnspecifiedAr =
        getNotNothingString(messagesAr, "subscriptionIDUnspecified", errorInMethod);
    subscriptionKeyInvalidAr =
        getNotNothingString(messagesAr, "subscriptionKeyInvalid", errorInMethod);
    subscriptionKeyUnspecifiedAr =
        getNotNothingString(messagesAr, "subscriptionKeyUnspecified", errorInMethod);
    subscriptionListErrorAr =
        getNotNothingString(messagesAr, "subscriptionListError", errorInMethod);
    subscriptionListHtmlAr = getNotNothingString(messagesAr, "subscriptionListHtml", errorInMethod);
    subscriptionListSuccessAr =
        getNotNothingString(messagesAr, "subscriptionListSuccess", errorInMethod);
    subscriptionRemoveErrorAr =
        getNotNothingString(messagesAr, "subscriptionRemoveError", errorInMethod);
    subscriptionRemoveHtmlAr =
        getNotNothingString(messagesAr, "subscriptionRemoveHtml", errorInMethod);
    subscriptionRemove2Ar = getNotNothingString(messagesAr, "subscriptionRemove2", errorInMethod);
    subscriptionRemoveSuccessAr =
        getNotNothingString(messagesAr, "subscriptionRemoveSuccess", errorInMethod);
    subscriptionRSSAr = getNotNothingString(messagesAr, "subscriptionRSS", errorInMethod);
    subscriptionsNotAvailableAr =
        getNotNothingString(messagesAr, "subscriptionsNotAvailable", errorInMethod);
    subscriptionUrlHtmlAr = getNotNothingString(messagesAr, "subscriptionUrlHtml", errorInMethod);
    subscriptionUrlInvalidAr =
        getNotNothingString(messagesAr, "subscriptionUrlInvalid", errorInMethod);
    subscriptionUrlTooLongAr =
        getNotNothingString(messagesAr, "subscriptionUrlTooLong", errorInMethod);
    subscriptionValidateErrorAr =
        getNotNothingString(messagesAr, "subscriptionValidateError", errorInMethod);
    subscriptionValidateHtmlAr =
        getNotNothingString(messagesAr, "subscriptionValidateHtml", errorInMethod);
    subscriptionValidateSuccessAr =
        getNotNothingString(messagesAr, "subscriptionValidateSuccess", errorInMethod);
    subsetAr = getNotNothingString(messagesAr, "subset", errorInMethod);
    subsetSelectAr = getNotNothingString(messagesAr, "subsetSelect", errorInMethod);
    subsetNMatchingAr = getNotNothingString(messagesAr, "subsetNMatching", errorInMethod);
    subsetInstructionsAr = getNotNothingString(messagesAr, "subsetInstructions", errorInMethod);
    subsetOptionAr = getNotNothingString(messagesAr, "subsetOption", errorInMethod);
    subsetOptionsAr = getNotNothingString(messagesAr, "subsetOptions", errorInMethod);
    subsetRefineMapDownloadAr =
        getNotNothingString(messagesAr, "subsetRefineMapDownload", errorInMethod);
    subsetRefineSubsetDownloadAr =
        getNotNothingString(messagesAr, "subsetRefineSubsetDownload", errorInMethod);
    subsetClickResetClosestAr =
        getNotNothingString(messagesAr, "subsetClickResetClosest", errorInMethod);
    subsetClickResetLLAr = getNotNothingString(messagesAr, "subsetClickResetLL", errorInMethod);
    subsetMetadataAr = getNotNothingString(messagesAr, "subsetMetadata", errorInMethod);
    subsetCountAr = getNotNothingString(messagesAr, "subsetCount", errorInMethod);
    subsetPercentAr = getNotNothingString(messagesAr, "subsetPercent", errorInMethod);
    subsetViewSelectAr = getNotNothingString(messagesAr, "subsetViewSelect", errorInMethod);
    subsetViewSelectDistinctCombosAr =
        getNotNothingString(messagesAr, "subsetViewSelectDistinctCombos", errorInMethod);
    subsetViewSelectRelatedCountsAr =
        getNotNothingString(messagesAr, "subsetViewSelectRelatedCounts", errorInMethod);
    subsetWhenAr = getNotNothingString(messagesAr, "subsetWhen", errorInMethod);
    subsetWhenNoConstraintsAr =
        getNotNothingString(messagesAr, "subsetWhenNoConstraints", errorInMethod);
    subsetWhenCountsAr = getNotNothingString(messagesAr, "subsetWhenCounts", errorInMethod);
    subsetComboClickSelectAr =
        getNotNothingString(messagesAr, "subsetComboClickSelect", errorInMethod);
    subsetNVariableCombosAr =
        getNotNothingString(messagesAr, "subsetNVariableCombos", errorInMethod);
    subsetShowingAllRowsAr = getNotNothingString(messagesAr, "subsetShowingAllRows", errorInMethod);
    subsetShowingNRowsAr = getNotNothingString(messagesAr, "subsetShowingNRows", errorInMethod);
    subsetChangeShowingAr = getNotNothingString(messagesAr, "subsetChangeShowing", errorInMethod);
    subsetNRowsRelatedDataAr =
        getNotNothingString(messagesAr, "subsetNRowsRelatedData", errorInMethod);
    subsetViewRelatedChangeAr =
        getNotNothingString(messagesAr, "subsetViewRelatedChange", errorInMethod);
    subsetTotalCountAr = getNotNothingString(messagesAr, "subsetTotalCount", errorInMethod);
    subsetViewAr = getNotNothingString(messagesAr, "subsetView", errorInMethod);
    subsetViewCheckAr = getNotNothingString(messagesAr, "subsetViewCheck", errorInMethod);
    subsetViewCheck1Ar = getNotNothingString(messagesAr, "subsetViewCheck1", errorInMethod);
    subsetViewDistinctMapAr =
        getNotNothingString(messagesAr, "subsetViewDistinctMap", errorInMethod);
    subsetViewRelatedMapAr = getNotNothingString(messagesAr, "subsetViewRelatedMap", errorInMethod);
    subsetViewDistinctDataCountsAr =
        getNotNothingString(messagesAr, "subsetViewDistinctDataCounts", errorInMethod);
    subsetViewDistinctDataAr =
        getNotNothingString(messagesAr, "subsetViewDistinctData", errorInMethod);
    subsetViewRelatedDataCountsAr =
        getNotNothingString(messagesAr, "subsetViewRelatedDataCounts", errorInMethod);
    subsetViewRelatedDataAr =
        getNotNothingString(messagesAr, "subsetViewRelatedData", errorInMethod);
    subsetViewDistinctMapTooltipAr =
        getNotNothingString(messagesAr, "subsetViewDistinctMapTooltip", errorInMethod);
    subsetViewRelatedMapTooltipAr =
        getNotNothingString(messagesAr, "subsetViewRelatedMapTooltip", errorInMethod);
    subsetViewDistinctDataCountsTooltipAr =
        getNotNothingString(messagesAr, "subsetViewDistinctDataCountsTooltip", errorInMethod);
    subsetViewDistinctDataTooltipAr =
        getNotNothingString(messagesAr, "subsetViewDistinctDataTooltip", errorInMethod);
    subsetViewRelatedDataCountsTooltipAr =
        getNotNothingString(messagesAr, "subsetViewRelatedDataCountsTooltip", errorInMethod);
    subsetViewRelatedDataTooltipAr =
        getNotNothingString(messagesAr, "subsetViewRelatedDataTooltip", errorInMethod);
    subsetWarnAr = getNotNothingString(messagesAr, "subsetWarn", errorInMethod);
    subsetWarn10000Ar = getNotNothingString(messagesAr, "subsetWarn10000", errorInMethod);
    subsetTooltipAr = getNotNothingString(messagesAr, "subsetTooltip", errorInMethod);
    subsetNotSetUpAr = getNotNothingString(messagesAr, "subsetNotSetUp", errorInMethod);
    subsetLongNotShownAr = getNotNothingString(messagesAr, "subsetLongNotShown", errorInMethod);

    tabledapVideoIntroAr = getNotNothingString(messagesAr, "tabledapVideoIntro", errorInMethod);
    theDatasetIDAr = getNotNothingString(messagesAr, "theDatasetID", errorInMethod);
    theKeyAr = getNotNothingString(messagesAr, "theKey", errorInMethod);
    theSubscriptionIDAr = getNotNothingString(messagesAr, "theSubscriptionID", errorInMethod);
    theUrlActionAr = getNotNothingString(messagesAr, "theUrlAction", errorInMethod);
    theLongDescriptionHtmlAr =
        getNotNothingString(messagesAr, "theLongDescriptionHtml", errorInMethod);
    timeAr = getNotNothingString(messagesAr, "time", errorInMethod);
    ThenAr = getNotNothingString(messagesAr, "Then", errorInMethod);
    thisParticularErddapAr = getNotNothingString(messagesAr, "thisParticularErddap", errorInMethod);
    timeoutOtherRequestsAr = getNotNothingString(messagesAr, "timeoutOtherRequests", errorInMethod);

    unitsAr = getNotNothingString(messagesAr, "units", errorInMethod);
    unknownDatasetIDAr = getNotNothingString(messagesAr, "unknownDatasetID", errorInMethod);
    unknownProtocolAr = getNotNothingString(messagesAr, "unknownProtocol", errorInMethod);
    unsupportedFileTypeAr = getNotNothingString(messagesAr, "unsupportedFileType", errorInMethod);
    String tStandardizeUdunits[] =
        String2.split(
            messagesAr[0].getNotNothingString("standardizeUdunits", errorInMethod) + "\n",
            '\n'); // +\n\n since xml content is trimmed.
    String tUcumToUdunits[] =
        String2.split(
            messagesAr[0].getNotNothingString("ucumToUdunits", errorInMethod) + "\n",
            '\n'); // +\n\n since xml content is trimmed.
    String tUdunitsToUcum[] =
        String2.split(
            messagesAr[0].getNotNothingString("udunitsToUcum", errorInMethod) + "\n",
            '\n'); // +\n\n since xml content is trimmed.
    String tUpdateUrls[] =
        String2.split(
            messagesAr[0].getNotNothingString("updateUrls", errorInMethod) + "\n",
            '\n'); // +\n\n since xml content is trimmed.

    updateUrlsSkipAttributes =
        StringArray.arrayFromCSV(
            messagesAr[0].getNotNothingString("updateUrlsSkipAttributes", errorInMethod));

    usingGriddapAr = getNotNothingString(messagesAr, "usingGriddap", errorInMethod);
    usingTabledapAr = getNotNothingString(messagesAr, "usingTabledap", errorInMethod);
    variableNamesAr = getNotNothingString(messagesAr, "variableNames", errorInMethod);
    viewAllDatasetsHtmlAr = getNotNothingString(messagesAr, "viewAllDatasetsHtml", errorInMethod);
    waitThenTryAgainAr = getNotNothingString(messagesAr, "waitThenTryAgain", errorInMethod);
    warningAr = getNotNothingString(messagesAr, "warning", errorInMethod);
    WCSAr = getNotNothingString(messagesAr, "WCS", errorInMethod);
    wcsDescriptionHtmlAr = getNotNothingString(messagesAr, "wcsDescriptionHtml", errorInMethod);
    wcsLongDescriptionHtmlAr =
        getNotNothingString(messagesAr, "wcsLongDescriptionHtml", errorInMethod);
    wcsOverview1Ar = getNotNothingString(messagesAr, "wcsOverview1", errorInMethod);
    wcsOverview2Ar = getNotNothingString(messagesAr, "wcsOverview2", errorInMethod);
    wmsDescriptionHtmlAr = getNotNothingString(messagesAr, "wmsDescriptionHtml", errorInMethod);
    wmsInstructionsAr = getNotNothingString(messagesAr, "wmsInstructions", errorInMethod);
    wmsLongDescriptionHtmlAr =
        getNotNothingString(messagesAr, "wmsLongDescriptionHtml", errorInMethod);
    wmsManyDatasetsAr = getNotNothingString(messagesAr, "wmsManyDatasets", errorInMethod);
    WMSDocumentation1Ar = getNotNothingString(messagesAr, "WMSDocumentation1", errorInMethod);
    WMSGetCapabilitiesAr = getNotNothingString(messagesAr, "WMSGetCapabilities", errorInMethod);
    WMSGetMapAr = getNotNothingString(messagesAr, "WMSGetMap", errorInMethod);
    for (int tl = 0; tl < nLanguages; tl++) {
      WMSGetCapabilitiesAr[tl] =
          WMSGetCapabilitiesAr[tl] // some things should stay in English
              .replaceAll("&serviceWMS;", "service=WMS")
              .replaceAll("&version;", "version")
              .replaceAll("&requestGetCapabilities;", "request=GetCapabilities");
      WMSGetMapAr[tl] =
          WMSGetMapAr[tl] // lots of things should stay in English
              .replaceAll("&WMSSERVER;", EDD.WMS_SERVER)
              .replaceAll("&WMSSEPARATOR;", Character.toString(EDD.WMS_SEPARATOR))
              .replaceAll("&serviceWMS;", "service=WMS")
              .replaceAll("&version;", "version")
              .replaceAll("&requestGetMap;", "request=GetMap")
              .replaceAll("&TRUE;", "TRUE")
              .replaceAll("&FALSE;", "FALSE")
              .replaceAll("&layers;", "layers")
              .replaceAll("&styles;", "styles")
              .replaceAll("&width;", "width")
              .replaceAll("&height;", "height")
              .replaceAll("&format;", "format")
              .replaceAll("&transparentTRUEFALSE;", "transparent=<i>TRUE|FALSE</i>")
              .replaceAll("&bgcolor;", "bgcolor")
              .replaceAll("&exceptions;", "exceptions")
              .replaceAll("&time;", "time")
              .replaceAll("&elevation;", "elevation");
    }

    WMSNotesAr = getNotNothingString(messagesAr, "WMSNotes", errorInMethod);
    for (int tl = 0; tl < nLanguages; tl++)
      WMSNotesAr[tl] =
          WMSNotesAr[tl].replace("&WMSSEPARATOR;", Character.toString(EDD.WMS_SEPARATOR));

    yourEmailAddressAr = getNotNothingString(messagesAr, "yourEmailAddress", errorInMethod);
    zoomInAr = getNotNothingString(messagesAr, "zoomIn", errorInMethod);
    zoomOutAr = getNotNothingString(messagesAr, "zoomOut", errorInMethod);

    lazyInitializeStatics(errorInMethod, messagesAr);

    for (int tl = 0; tl < nLanguages; tl++) {
      blacklistMsgAr[tl] = MessageFormat.format(blacklistMsgAr[tl], EDStatic.config.adminEmail);
    }

    standardShortDescriptionHtmlAr =
        getNotNothingString(messagesAr, "standardShortDescriptionHtml", errorInMethod);
    for (int tl = 0; tl < nLanguages; tl++) {
      standardShortDescriptionHtmlAr[tl] =
          String2.replaceAll(
              standardShortDescriptionHtmlAr[tl],
              "&convertTimeReference;",
              EDStatic.config.convertersActive ? convertTimeReferenceAr[tl] : "");
      standardShortDescriptionHtmlAr[tl] =
          String2.replaceAll(
              standardShortDescriptionHtmlAr[tl],
              "&wmsManyDatasets;",
              EDStatic.config.wmsActive ? wmsManyDatasetsAr[tl] : "");
    }

    // just one
    DEFAULT_commonStandardNames =
        String2.canonical(
            StringArray.arrayFromCSV(
                messagesAr[0].getNotNothingString("DEFAULT_commonStandardNames", errorInMethod)));
    commonStandardNames = DEFAULT_commonStandardNames;
    DEFAULT_standardLicense = messagesAr[0].getNotNothingString("standardLicense", errorInMethod);
    standardLicense = getSetupEVString(setup, ev, "standardLicense", DEFAULT_standardLicense);

    // [language]
    DEFAULT_standardContactAr = getNotNothingString(messagesAr, "standardContact", errorInMethod);
    standardContactAr = getSetupEVString(setup, ev, "standardContact", DEFAULT_standardContactAr);
    DEFAULT_standardDataLicensesAr =
        getNotNothingString(messagesAr, "standardDataLicenses", errorInMethod);
    standardDataLicensesAr =
        getSetupEVString(setup, ev, "standardDataLicenses", DEFAULT_standardDataLicensesAr);
    DEFAULT_standardDisclaimerOfExternalLinksAr =
        getNotNothingString(messagesAr, "standardDisclaimerOfExternalLinks", errorInMethod);
    standardDisclaimerOfExternalLinksAr =
        getSetupEVString(
            setup,
            ev,
            "standardDisclaimerOfExternalLinks",
            DEFAULT_standardDisclaimerOfExternalLinksAr);
    DEFAULT_standardDisclaimerOfEndorsementAr =
        getNotNothingString(messagesAr, "standardDisclaimerOfEndorsement", errorInMethod);
    standardDisclaimerOfEndorsementAr =
        getSetupEVString(
            setup,
            ev,
            "standardDisclaimerOfEndorsement",
            DEFAULT_standardDisclaimerOfEndorsementAr);
    DEFAULT_standardGeneralDisclaimerAr =
        getNotNothingString(messagesAr, "standardGeneralDisclaimer", errorInMethod);
    standardGeneralDisclaimerAr =
        getSetupEVString(
            setup, ev, "standardGeneralDisclaimer", DEFAULT_standardGeneralDisclaimerAr);
    DEFAULT_standardPrivacyPolicyAr =
        getNotNothingString(messagesAr, "standardPrivacyPolicy", errorInMethod);
    standardPrivacyPolicyAr =
        getSetupEVString(setup, ev, "standardPrivacyPolicy", DEFAULT_standardPrivacyPolicyAr);

    DEFAULT_startHeadHtml = messagesAr[0].getNotNothingString("startHeadHtml5", errorInMethod);
    startHeadHtml = getSetupEVString(setup, ev, "startHeadHtml5", DEFAULT_startHeadHtml);
    DEFAULT_startBodyHtmlAr = getNotNothingString(messagesAr, "startBodyHtml5", errorInMethod);
    startBodyHtmlAr = getSetupEVString(setup, ev, "startBodyHtml5", DEFAULT_startBodyHtmlAr);
    DEFAULT_theShortDescriptionHtmlAr =
        getNotNothingString(messagesAr, "theShortDescriptionHtml", errorInMethod);
    theShortDescriptionHtmlAr =
        getSetupEVString(setup, ev, "theShortDescriptionHtml", DEFAULT_theShortDescriptionHtmlAr);
    DEFAULT_endBodyHtmlAr = getNotNothingString(messagesAr, "endBodyHtml5", errorInMethod);
    endBodyHtmlAr = getSetupEVString(setup, ev, "endBodyHtml5", DEFAULT_endBodyHtmlAr);

    // ensure HTML5
    Test.ensureTrue(
        startHeadHtml.startsWith("<!DOCTYPE html>"),
        "<startHeadHtml5> must start with \"<!DOCTYPE html>\".");
    for (int tl = 0; tl < nLanguages; tl++) {
      DEFAULT_standardDataLicensesAr[tl] =
          String2.replaceAll(
              DEFAULT_standardDataLicensesAr[tl],
              "&license;",
              "<kbd>license</kbd>"); // so not translated
      standardDataLicensesAr[tl] =
          String2.replaceAll(standardDataLicensesAr[tl], "&license;", "<kbd>license</kbd>");
      standardContactAr[tl] =
          String2.replaceAll(
              standardContactAr[tl],
              "&adminEmail;",
              SSR.getSafeEmailAddress(EDStatic.config.adminEmail));
      startBodyHtmlAr[tl] =
          String2.replaceAll(
              startBodyHtmlAr[tl], "&erddapVersion;", EDStatic.erddapVersion.getVersion());
      endBodyHtmlAr[tl] =
          String2.replaceAll(
              endBodyHtmlAr[tl], "&erddapVersion;", EDStatic.erddapVersion.getVersion());
    }

    Test.ensureEqual(imageWidths.length, 3, "imageWidths.length must be 3.");
    Test.ensureEqual(imageHeights.length, 3, "imageHeights.length must be 3.");
    Test.ensureEqual(pdfWidths.length, 3, "pdfWidths.length must be 3.");
    Test.ensureEqual(pdfHeights.length, 3, "pdfHeights.length must be 3.");

    int nStandardizeUdunits = tStandardizeUdunits.length / 3;
    for (int i = 0; i < nStandardizeUdunits; i++) {
      int i3 = i * 3;
      Test.ensureTrue(
          String2.isSomething(tStandardizeUdunits[i3]),
          "standardizeUdunits line #" + (i3 + 0) + " is empty.");
      Test.ensureTrue(
          String2.isSomething(tStandardizeUdunits[i3 + 1]),
          "standardizeUdunits line #" + (i3 + 1) + " is empty.");
      Test.ensureEqual(
          tStandardizeUdunits[i3 + 2].trim(),
          "",
          "standardizeUdunits line #" + (i3 + 2) + " isn't empty.");
      Units2.standardizeUdunitsHM.put(
          String2.canonical(tStandardizeUdunits[i3].trim()),
          String2.canonical(tStandardizeUdunits[i3 + 1].trim()));
    }

    int nUcumToUdunits = tUcumToUdunits.length / 3;
    for (int i = 0; i < nUcumToUdunits; i++) {
      int i3 = i * 3;
      Test.ensureTrue(
          String2.isSomething(tUcumToUdunits[i3]),
          "ucumToUdunits line #" + (i3 + 0) + " is empty.");
      Test.ensureTrue(
          String2.isSomething(tUcumToUdunits[i3 + 1]),
          "ucumToUdunits line #" + (i3 + 1) + " is empty.");
      Test.ensureEqual(
          tUcumToUdunits[i3 + 2].trim(), "", "ucumToUdunits line #" + (i3 + 2) + " isn't empty.");
      Units2.ucumToUdunitsHM.put(
          String2.canonical(tUcumToUdunits[i3].trim()),
          String2.canonical(tUcumToUdunits[i3 + 1].trim()));
    }

    int nUdunitsToUcum = tUdunitsToUcum.length / 3;
    for (int i = 0; i < nUdunitsToUcum; i++) {
      int i3 = i * 3;
      Test.ensureTrue(
          String2.isSomething(tUdunitsToUcum[i3]),
          "udunitsToUcum line #" + (i3 + 0) + " is empty.");
      Test.ensureTrue(
          String2.isSomething(tUdunitsToUcum[i3 + 1]),
          "udunitsToUcum line #" + (i3 + 1) + " is empty.");
      Test.ensureEqual(
          tUdunitsToUcum[i3 + 2].trim(), "", "udunitsToUcum line #" + (i3 + 2) + " isn't empty.");
      Units2.udunitsToUcumHM.put(
          String2.canonical(tUdunitsToUcum[i3].trim()),
          String2.canonical(tUdunitsToUcum[i3 + 1].trim()));
    }

    int nUpdateUrls = tUpdateUrls.length / 3;
    updateUrlsFrom = new String[nUpdateUrls];
    updateUrlsTo = new String[nUpdateUrls];
    for (int i = 0; i < nUpdateUrls; i++) {
      int i3 = i * 3;
      updateUrlsFrom[i] = String2.canonical(tUpdateUrls[i3].trim());
      updateUrlsTo[i] = String2.canonical(tUpdateUrls[i3 + 1].trim());
      Test.ensureTrue(
          String2.isSomething(tUpdateUrls[i3]), "updateUrls line #" + (i3 + 0) + " is empty.");
      Test.ensureTrue(
          String2.isSomething(tUpdateUrls[i3 + 1]), "updateUrls line #" + (i3 + 1) + " is empty.");
      Test.ensureEqual(
          tUpdateUrls[i3 + 2].trim(), "", "updateUrls line #" + (i3 + 0) + " isn't empty.");
    }

    for (String palette : palettes) {
      String tName = EDStatic.config.fullPaletteDirectory + palette + ".cpt";
      Test.ensureTrue(
          File2.isFile(tName),
          "\"" + palette + "\" is listed in <palettes>, but there is no file " + tName);
    }

    String tEmail = SSR.getSafeEmailAddress(EDStatic.config.adminEmail);
    for (int tl = 0; tl < nLanguages; tl++) {
      searchHintsTooltipAr[tl] =
          "<div class=\"standard_max_width\">"
              + searchHintsTooltipAr[tl]
              + "\n"
              + (EDStatic.config.useLuceneSearchEngine
                  ? searchHintsLuceneTooltipAr[tl]
                  : searchHintsOriginalTooltipAr[tl])
              + "</div>";
      advancedSearchDirectionsAr[tl] =
          String2.replaceAll(advancedSearchDirectionsAr[tl], "&searchButton;", searchButtonAr[tl]);

      loginProblemsAr[tl] =
          String2.replaceAll(loginProblemsAr[tl], "&cookiesHelp;", cookiesHelpAr[tl]);
      loginProblemsAr[tl] =
          String2.replaceAll(loginProblemsAr[tl], "&adminContact;", EDStatic.adminContact())
              + "\n\n";
      loginProblemsAfterAr[tl] =
          String2.replaceAll(loginProblemsAfterAr[tl], "&adminContact;", EDStatic.adminContact())
              + "\n\n";
      loginPublicAccessAr[tl] += "\n";
      logoutSuccessAr[tl] += "\n";

      filesDocumentationAr[tl] =
          String2.replaceAll(filesDocumentationAr[tl], "&adminEmail;", tEmail);

      doWithGraphsAr[tl] =
          String2.replaceAll(
              doWithGraphsAr[tl], "&ssUse;", EDStatic.config.slideSorterActive ? ssUseAr[tl] : "");

      theLongDescriptionHtmlAr[tl] =
          String2.replaceAll(
              theLongDescriptionHtmlAr[tl],
              "&ssUse;",
              EDStatic.config.slideSorterActive ? ssUseAr[tl] : "");
      theLongDescriptionHtmlAr[tl] =
          String2.replaceAll(
              theLongDescriptionHtmlAr[tl],
              "&requestFormatExamplesHtml;",
              requestFormatExamplesHtmlAr[tl]);
      theLongDescriptionHtmlAr[tl] =
          String2.replaceAll(
              theLongDescriptionHtmlAr[tl],
              "&resultsFormatExamplesHtml;",
              resultsFormatExamplesHtmlAr[tl]);
    }
  }

  private void lazyInitializeStatics(String errorInMethod, ResourceBundle2[] messagesAr) {
    PrimitiveArray.ArrayAddN = messagesAr[0].getNotNothingString("ArrayAddN", errorInMethod);
    PrimitiveArray.ArrayAppendTables =
        messagesAr[0].getNotNothingString("ArrayAppendTables", errorInMethod);
    PrimitiveArray.ArrayAtInsert =
        messagesAr[0].getNotNothingString("ArrayAtInsert", errorInMethod);
    PrimitiveArray.ArrayDiff = messagesAr[0].getNotNothingString("ArrayDiff", errorInMethod);
    PrimitiveArray.ArrayDifferentSize =
        messagesAr[0].getNotNothingString("ArrayDifferentSize", errorInMethod);
    PrimitiveArray.ArrayDifferentValue =
        messagesAr[0].getNotNothingString("ArrayDifferentValue", errorInMethod);
    PrimitiveArray.ArrayDiffString =
        messagesAr[0].getNotNothingString("ArrayDiffString", errorInMethod);
    PrimitiveArray.ArrayMissingValue =
        messagesAr[0].getNotNothingString("ArrayMissingValue", errorInMethod);
    PrimitiveArray.ArrayNotAscending =
        messagesAr[0].getNotNothingString("ArrayNotAscending", errorInMethod);
    PrimitiveArray.ArrayNotDescending =
        messagesAr[0].getNotNothingString("ArrayNotDescending", errorInMethod);
    PrimitiveArray.ArrayNotEvenlySpaced =
        messagesAr[0].getNotNothingString("ArrayNotEvenlySpaced", errorInMethod);
    PrimitiveArray.ArrayRemove = messagesAr[0].getNotNothingString("ArrayRemove", errorInMethod);
    PrimitiveArray.ArraySubsetStart =
        messagesAr[0].getNotNothingString("ArraySubsetStart", errorInMethod);
    PrimitiveArray.ArraySubsetStride =
        messagesAr[0].getNotNothingString("ArraySubsetStride", errorInMethod);

    HtmlWidgets.comboBoxAltAr = getNotNothingString(messagesAr, "comboBoxAlt", errorInMethod);
    HtmlWidgets.errorXWasntSpecifiedAr =
        getNotNothingString(messagesAr, "errorXWasntSpecified", errorInMethod);
    HtmlWidgets.errorXWasTooLongAr =
        getNotNothingString(messagesAr, "errorXWasTooLong", errorInMethod);
    TableWriterHtmlTable.htmlTableMaxMB =
        messagesAr[0].getInt("htmlTableMaxMB", TableWriterHtmlTable.htmlTableMaxMB);
    Math2.memory = messagesAr[0].getNotNothingString("memory", errorInMethod);
    Math2.memoryTooMuchData = messagesAr[0].getNotNothingString("memoryTooMuchData", errorInMethod);
    Math2.memoryArraySize = messagesAr[0].getNotNothingString("memoryArraySize", errorInMethod);
    Math2.memoryThanCurrentlySafe =
        messagesAr[0].getNotNothingString("memoryThanCurrentlySafe", errorInMethod);
    Math2.memoryThanSafe = messagesAr[0].getNotNothingString("memoryThanSafe", errorInMethod);
    MustBe.THERE_IS_NO_DATA =
        messagesAr[0].getNotNothingString("MustBeThereIsNoData", errorInMethod);
    MustBe.NotNull = messagesAr[0].getNotNothingString("MustBeNotNull", errorInMethod);
    MustBe.NotEmpty = messagesAr[0].getNotNothingString("MustBeNotEmpty", errorInMethod);
    MustBe.InternalError = messagesAr[0].getNotNothingString("MustBeInternalError", errorInMethod);
    MustBe.OutOfMemoryError =
        messagesAr[0].getNotNothingString("MustBeOutOfMemoryError", errorInMethod);

    Attributes.signedToUnsignedAttNames =
        StringArray.arrayFromCSV(
            messagesAr[0].getNotNothingString("signedToUnsignedAttNames", errorInMethod));
    HtmlWidgets.twoClickMapDefaultTooltipAr =
        getNotNothingString(messagesAr, "twoClickMapDefaultTooltip", errorInMethod);
    gov.noaa.pfel.erddap.dataset.WaitThenTryAgainException.waitThenTryAgain = waitThenTryAgainAr[0];
  }

  /**
   * A variant of getSetupEVString that works with an array of tDefault.
   *
   * @param setup from setup.xml
   * @param ev from System.getenv()
   * @param paramName If present in ev, it will be ERDDAP_paramName.
   * @param tDefault the default value
   * @return the desired value (or the default if it isn't defined anywhere)
   */
  private String[] getSetupEVString(
      ResourceBundle2 setup, Map<String, String> ev, String paramName, String tDefault[]) {
    String value = ev.get("ERDDAP_" + paramName);
    int n = tDefault.length;
    if (String2.isSomething(value)) {
      String2.log("got " + paramName + " from ERDDAP_" + paramName);
      for (int i = 0; i < n; i++) tDefault[i] = value;
      return tDefault;
    }
    for (int i = 0; i < n; i++) tDefault[i] = setup.getString(paramName, tDefault[i]);
    return tDefault;
  }

  /**
   * This gets a string from setup.xml or environmentalVariables (preferred).
   *
   * @param setup from setup.xml
   * @param ev from System.getenv()
   * @param paramName If present in ev, it will be ERDDAP_paramName.
   * @param tDefault the default value
   * @return the desired value (or the default if it isn't defined anywhere)
   */
  private String getSetupEVString(
      ResourceBundle2 setup, Map<String, String> ev, String paramName, String tDefault) {
    String value = ev.get("ERDDAP_" + paramName);
    if (String2.isSomething(value)) {
      String2.log("got " + paramName + " from ERDDAP_" + paramName);
      return value;
    }
    return setup.getString(paramName, tDefault);
  }

  /** This does getNotNothingString for each messages[]. */
  private String[] getNotNothingString(
      ResourceBundle2 messages[], String name, String errorInMethod) {

    int nMessages = messages.length;
    String ar[] = new String[nMessages];
    for (int i = 0; i < nMessages; i++)
      ar[i] = messages[i].getNotNothingString(name, errorInMethod + "When language=" + i + ", ");
    return ar;
  }

  /**
   * This returns the html documentation for acceptEncoding.
   *
   * @param language the index of the selected language
   * @param headingType e.g., h2 or h3
   * @param tErddapUrl
   * @return the html needed to document acceptEncodig.
   */
  public String acceptEncodingHtml(int language, String headingType, String tErddapUrl) {
    String s =
        String2.replaceAll(
            acceptEncodingHtmlAr[language], "&headingType;", "<" + headingType + ">");
    s = String2.replaceAll(s, "&sheadingType;", "</" + headingType + ">");
    return String2.replaceAll(s, "&externalLinkHtml;", externalLinkHtml(language, tErddapUrl));
  }

  /**
   * This returns the html documentation for the /files/ system.
   *
   * @param language the index of the selected language
   * @param tErddapUrl
   * @return the html needed to document acceptEncodig.
   */
  public String filesDocumentation(int language, String tErddapUrl) {
    return String2.replaceAll(
        filesDocumentationAr[language],
        "&acceptEncodingHtml;",
        acceptEncodingHtml(language, "h3", tErddapUrl));
  }

  /**
   * This returns the html needed to display the external.png image with the warning that the link
   * is to an external website.
   *
   * @param language the index of the selected language
   * @param tErddapUrl
   * @return the html needed to display the external.png image and messages.
   */
  public String externalLinkHtml(int language, String tErddapUrl) {
    return "<img\n"
        + "    src=\""
        + tErddapUrl
        + "/images/external.png\" "
        + "alt=\""
        + externalLinkAr[language]
        + "\"\n"
        + "    title=\""
        + externalWebSiteAr[language]
        + "\">";
  }

  public String theLongDescriptionHtml(int language, String tErddapUrl) {
    return String2.replaceAll(theLongDescriptionHtmlAr[language], "&erddapUrl;", tErddapUrl);
  }

  public String theShortDescriptionHtml(int language, String tErddapUrl) {
    String s =
        theShortDescriptionHtmlAr[
            0]; // from datasets.xml or messages.xml.  Always use English, but parts (most) will be
    // translated.
    s = String2.replaceAll(s, "&erddapIs;", erddapIsAr[language]);
    s = String2.replaceAll(s, "&thisParticularErddap;", thisParticularErddapAr[language]);
    s =
        String2.replaceAll(
            s, "[standardShortDescriptionHtml]", standardShortDescriptionHtmlAr[language]);
    s = String2.replaceAll(s, "&requestFormatExamplesHtml;", requestFormatExamplesHtmlAr[language]);
    s = String2.replaceAll(s, "&erddapUrl;", tErddapUrl); // do last
    return s;
  }
}
