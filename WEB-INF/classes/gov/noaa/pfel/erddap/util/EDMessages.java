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
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EDMessages {
  public static int DEFAULT_LANGUAGE = 0;
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

  public String[] // in messages.xml and perhaps in datasets.xml (v2.00+)
      commonStandardNames;
  public final String[] DEFAULT_commonStandardNames;
  public final String[] updateUrlsSkipAttributes;
  public final String[] updateUrlsFrom;
  public final String[] updateUrlsTo;

  public enum Message {
    // Default messages (from setup.xml, messages.xml, datasets.xml)
    DEFAULT_STANDARD_CONTACT,
    DEFAULT_STANDARD_DATA_LICENSES,
    DEFAULT_STANDARD_DISCLAIMER_OF_ENDORSEMENT,
    DEFAULT_STANDARD_DISCLAIMER_OF_EXTERNAL_LINKS,
    DEFAULT_STANDARD_GENERAL_DISCLAIMER,
    DEFAULT_STANDARD_PRIVACY_POLICY,
    DEFAULT_START_BODY_HTML,
    DEFAULT_THE_SHORT_DESCRIPTION_HTML,
    DEFAULT_END_BODY_HTML,
    STANDARD_CONTACT,
    STANDARD_DATA_LICENSES,
    STANDARD_DISCLAIMER_OF_ENDORSEMENT,
    STANDARD_DISCLAIMER_OF_EXTERNAL_LINKS,
    STANDARD_GENERAL_DISCLAIMER,
    STANDARD_PRIVACY_POLICY,
    START_BODY_HTML,
    THE_SHORT_DESCRIPTION_HTML,
    END_BODY_HTML,

    // Translated messages
    ACCEPT_ENCODING_HTML,
    FILES_DOCUMENTATION,
    ACCESS_RESTFUL,
    ACRONYMS,
    ADD_CONSTRAINTS,
    ADD_VAR_WHERE_ATT_VALUE,
    ADD_VAR_WHERE,
    ADDITIONAL_LINKS,
    ADM_SUMMARY,
    ADM_TITLE,
    ADVC_ACCESSIBLE,
    ADVL_ACCESSIBLE,
    ADVL_INSTITUTION,
    ADVC_DATA_STRUCTURE,
    ADVL_DATA_STRUCTURE,
    ADVL_CDM_DATA_TYPE,
    ADVL_CLASS,
    ADVL_TITLE,
    ADVL_MIN_LONGITUDE,
    ADVL_MAX_LONGITUDE,
    ADVL_LONGITUDE_SPACING,
    ADVL_MIN_LATITUDE,
    ADVL_MAX_LATITUDE,
    ADVL_LATITUDE_SPACING,
    ADVL_MIN_ALTITUDE,
    ADVL_MAX_ALTITUDE,
    ADVL_MIN_TIME,
    ADVC_MAX_TIME,
    ADVL_MAX_TIME,
    ADVL_TIME_SPACING,
    ADVC_GRIDDAP,
    ADVL_GRIDDAP,
    ADVL_SUBSET,
    ADVC_TABLEDAP,
    ADVL_TABLEDAP,
    ADVL_MAKE_A_GRAPH,
    ADVC_SOS,
    ADVL_SOS,
    ADVL_WCS,
    ADVL_WMS,
    ADVC_FILES,
    ADVL_FILES,
    ADVC_FGDC,
    ADVL_FGDC,
    ADVC_ISO19115,
    ADVL_ISO19115,
    ADVC_METADATA,
    ADVL_METADATA,
    ADVL_SOURCE_URL,
    ADVL_INFO_URL,
    ADVL_RSS,
    ADVC_EMAIL,
    ADVL_EMAIL,
    ADVL_SUMMARY,
    ADVC_TEST_OUT_OF_DATE,
    ADVL_TEST_OUT_OF_DATE,
    ADVC_OUT_OF_DATE,
    ADVL_OUT_OF_DATE,
    ADVN_OUT_OF_DATE,
    ADVANCED_SEARCH,
    ADVANCED_SEARCH_RESULTS,
    ADVANCED_SEARCH_DIRECTIONS,
    ADVANCED_SEARCH_TOOLTIP,
    ADVANCED_SEARCH_BOUNDS,
    ADVANCED_SEARCH_MIN_LAT,
    ADVANCED_SEARCH_MAX_LAT,
    ADVANCED_SEARCH_MIN_LON,
    ADVANCED_SEARCH_MAX_LON,
    ADVANCED_SEARCH_MIN_MAX_LON,
    ADVANCED_SEARCH_MIN_TIME,
    ADVANCED_SEARCH_MAX_TIME,
    ADVANCED_SEARCH_CLEAR,
    ADVANCED_SEARCH_CLEAR_HELP,
    ADVANCED_SEARCH_CATEGORY_TOOLTIP,
    ADVANCED_SEARCH_RANGE_TOOLTIP,
    ADVANCED_SEARCH_MAP_TOOLTIP,
    ADVANCED_SEARCH_LON_TOOLTIP,
    ADVANCED_SEARCH_TIME_TOOLTIP,
    ADVANCED_SEARCH_WITH_CRITERIA,
    ADVANCED_SEARCH_FEWER_CRITERIA,
    ADVANCED_SEARCH_NO_CRITERIA,
    ADVANCED_SEARCH_ERROR_HANDLING,
    AUTO_REFRESH,
    BLACKLIST_MSG,
    BROUGHT_TO_YOU_BY,
    CATEGORY_TITLE_HTML,
    CATEGORY_HTML,
    CATEGORY3_HTML,
    CATEGORY_PICK_ATTRIBUTE,
    CATEGORY_SEARCH_HTML,
    CATEGORY_CLICK_HTML,
    CATEGORY_NOT_AN_OPTION,
    CAUGHT_INTERRUPTED,
    CDM_DATA_TYPE_HELP,
    CLICK_ACCESS,
    CLICK_BACKGROUND_INFO,
    CLICK_ERDDAP,
    CLICK_INFO,
    CLICK_TO_SUBMIT,
    CONVERT,
    CONVERT_BYPASS,
    CONVERT_COLORS,
    CONVERT_COLORS_MESSAGE,
    CONVERT_TO_A_FULL_NAME,
    CONVERT_TO_AN_ACRONYM,
    CONVERT_TO_A_COUNTY_NAME,
    CONVERT_TO_A_FIPS_CODE,
    CONVERT_TO_GCMD,
    CONVERT_TO_CF_STANDARD_NAMES,
    CONVERT_TO_NUMERIC_TIME,
    CONVERT_TO_STRING_TIME,
    CONVERT_ANY_STRING_TIME,
    CONVERT_TO_PROPER_TIME_UNITS,
    CONVERT_FROM_UDUNITS_TO_UCUM,
    CONVERT_FROM_UCUM_TO_UDUNITS,
    CONVERT_TO_UCUM,
    CONVERT_TO_UDUNITS,
    CONVERT_STANDARDIZE_UDUNITS,
    CONVERT_TO_FULL_NAME,
    CONVERT_TO_VARIABLE_NAME,
    CONVERTER_WEB_SERVICE,
    CONVERT_OA_ACRONYMS,
    CONVERT_OA_ACRONYMS_TO_FROM,
    CONVERT_OA_ACRONYMS_INTRO,
    CONVERT_OA_ACRONYMS_NOTES,
    CONVERT_OA_ACRONYMS_SERVICE,
    CONVERT_OA_VARIABLE_NAMES,
    CONVERT_OA_VARIABLE_NAMES_TO_FROM,
    CONVERT_OA_VARIABLE_NAMES_INTRO,
    CONVERT_OA_VARIABLE_NAMES_NOTES,
    CONVERT_OA_VARIABLE_NAMES_SERVICE,
    CONVERT_FIPS_COUNTY,
    CONVERT_FIPS_COUNTY_INTRO,
    CONVERT_FIPS_COUNTY_NOTES,
    CONVERT_FIPS_COUNTY_SERVICE,
    CONVERT_HTML,
    CONVERT_INTERPOLATE,
    CONVERT_INTERPOLATE_INTRO,
    CONVERT_INTERPOLATE_TLL_TABLE,
    CONVERT_INTERPOLATE_TLL_TABLE_HELP,
    CONVERT_INTERPOLATE_DATASET_ID_VARIABLE,
    CONVERT_INTERPOLATE_DATASET_ID_VARIABLE_HELP,
    CONVERT_INTERPOLATE_NOTES,
    CONVERT_INTERPOLATE_SERVICE,
    CONVERT_KEYWORDS,
    CONVERT_KEYWORDS_CF_TOOLTIP,
    CONVERT_KEYWORDS_GCMD_TOOLTIP,
    CONVERT_KEYWORDS_INTRO,
    CONVERT_KEYWORDS_NOTES,
    CONVERT_KEYWORDS_SERVICE,
    CONVERT_TIME,
    CONVERT_TIME_REFERENCE,
    CONVERT_TIME_INTRO,
    CONVERT_TIME_NOTES,
    CONVERT_TIME_SERVICE,
    CONVERT_TIME_NUMBER_TOOLTIP,
    CONVERT_TIME_STRING_TIME_TOOLTIP,
    CONVERT_TIME_UNITS_TOOLTIP,
    CONVERT_TIME_UNITS_HELP,
    CONVERT_TIME_ISO_FORMAT_ERROR,
    CONVERT_TIME_NO_SINCE_ERROR,
    CONVERT_TIME_NUMBER_ERROR,
    CONVERT_TIME_NUMERIC_TIME_ERROR,
    CONVERT_TIME_PARAMETERS_ERROR,
    CONVERT_TIME_STRING_FORMAT_ERROR,
    CONVERT_TIME_TWO_TIME_ERROR,
    CONVERT_TIME_UNITS_ERROR,
    CONVERT_UNITS,
    CONVERT_UNITS_COMPARISON,
    CONVERT_UNITS_FILTER,
    CONVERT_UNITS_INTRO,
    CONVERT_UNITS_NOTES,
    CONVERT_UNITS_SERVICE,
    CONVERT_URLS,
    CONVERT_URLS_INTRO,
    CONVERT_URLS_NOTES,
    CONVERT_URLS_SERVICE,
    COOKIES_HELP,
    COPY_IMAGE_TO_CLIPBOARD,
    COPY_TEXT_TO_CLIPBOARD,
    COPY_TO_CLIPBOARD_NOT_AVAILABLE,
    DAF,
    DAF_GRID_BYPASS_TOOLTIP,
    DAF_GRID_TOOLTIP,
    DAF_TABLE_BYPASS_TOOLTIP,
    DAF_TABLE_TOOLTIP,
    DAS_TITLE,
    DATABASE_UNABLE_TO_CONNECT,
    DATA_PROVIDER_FORM,
    DATA_PROVIDER_FORM_P1,
    DATA_PROVIDER_FORM_P2,
    DATA_PROVIDER_FORM_P3,
    DATA_PROVIDER_FORM_P4,
    DATA_PROVIDER_FORM_DONE,
    DATA_PROVIDER_FORM_SUCCESS,
    DATA_PROVIDER_FORM_SHORT_DESCRIPTION,
    DATA_PROVIDER_FORM_LONG_DESCRIPTION_HTML,
    DATA_PROVIDER_FORM_PART1,
    DATA_PROVIDER_FORM_PART2_HEADER,
    DATA_PROVIDER_FORM_PART2_GLOBAL_METADATA,
    DATA_PROVIDER_CONTACT_INFO,
    DATA_PROVIDER_DATA,
    DOCUMENTATION,
    DPF_SUBMIT,
    DPF_FIX_PROBLEM,
    DPF_TITLE,
    DPF_TITLE_TOOLTIP,
    DPF_SUMMARY,
    DPF_SUMMARY_TOOLTIP,
    DPF_CREATOR_NAME,
    DPF_CREATOR_NAME_TOOLTIP,
    DPF_CREATOR_TYPE,
    DPF_CREATOR_TYPE_TOOLTIP,
    DPF_CREATOR_EMAIL,
    DPF_CREATOR_EMAIL_TOOLTIP,
    DPF_INSTITUTION,
    DPF_INSTITUTION_TOOLTIP,
    DPF_INFO_URL,
    DPF_INFO_URL_TOOLTIP,
    DPF_LICENSE,
    DPF_LICENSE_TOOLTIP,
    DPF_PROVIDE_IF_AVAILABLE,
    DPF_ACKNOWLEDGEMENT,
    DPF_ACKNOWLEDGEMENT_TOOLTIP,
    DPF_HISTORY,
    DPF_HISTORY_TOOLTIP,
    DPF_ID_TOOLTIP,
    DPF_NAMING_AUTHORITY,
    DPF_NAMING_AUTHORITY_TOOLTIP,
    DPF_PRODUCT_VERSION,
    DPF_PRODUCT_VERSION_TOOLTIP,
    DPF_REFERENCES,
    DPF_REFERENCES_TOOLTIP,
    DPF_COMMENT,
    DPF_COMMENT_TOOLTIP,
    DPF_DATA_TYPE_HELP,
    DPF_IOOS_CATEGORY,
    DPF_IOOS_CATEGORY_HELP,
    DPF_PART3_HEADER,
    DPF_VARIABLE_METADATA,
    DPF_SOURCE_NAME,
    DPF_SOURCE_NAME_TOOLTIP,
    DPF_DESTINATION_NAME,
    DPF_DESTINATION_NAME_TOOLTIP,
    DPF_LONG_NAME,
    DPF_LONG_NAME_TOOLTIP,
    DPF_STANDARD_NAME,
    DPF_STANDARD_NAME_TOOLTIP,
    DPF_DATA_TYPE,
    DPF_FILL_VALUE,
    DPF_FILL_VALUE_TOOLTIP,
    DPF_UNITS,
    DPF_UNITS_TOOLTIP,
    DPF_RANGE,
    DPF_RANGE_TOOLTIP,
    DPF_PART4_HEADER,
    DPF_OTHER_COMMENT,
    DPF_FINISH_PART4,
    DPF_CONGRATULATION,
    DISABLED,
    DISTINCT_VALUES_TOOLTIP,
    DO_WITH_GRAPHS,
    DT_ACCESSIBLE,
    DT_ACCESSIBLE_PUBLIC,
    DT_ACCESSIBLE_YES,
    DT_ACCESSIBLE_GRAPHS,
    DT_ACCESSIBLE_NO,
    DT_ACCESSIBLE_LOG_IN,
    DT_LOG_IN,
    DT_DAF,
    DT_FILES,
    DT_MAG,
    DT_SOS,
    DT_SUBSET,
    DT_WCS,
    DT_WMS,
    EASIER_ACCESS_TO_SCIENTIFIC_DATA,
    EDD_DATASET_ID,
    EDD_FGDC_METADATA,
    EDD_FILES,
    EDD_ISO19115_METADATA,
    EDD_METADATA,
    EDD_BACKGROUND,
    EDD_INSTITUTION,
    EDD_INFORMATION,
    EDD_SUMMARY,
    EDD_DATASET_TITLE,
    EDD_MAKE_A_GRAPH,
    EDD_FILE_TYPE,
    EDD_FILE_TYPE_INFORMATION,
    EDD_SELECT_FILE_TYPE,
    EDD_MINIMUM,
    EDD_MAXIMUM,
    EDD_CONSTRAINT,
    EDD_GRID_DAP_DESCRIPTION,
    EDD_GRID_DAP_LONG_DESCRIPTION,
    EDD_GRID_DOWNLOAD_DATA_TOOLTIP,
    EDD_GRID_DIMENSION,
    EDD_GRID_DIMENSION_RANGES,
    EDD_GRID_START,
    EDD_GRID_STOP,
    EDD_GRID_START_STOP_TOOLTIP,
    EDD_GRID_STRIDE,
    EDD_GRID_N_VALUES,
    EDD_GRID_N_VALUES_HTML,
    EDD_GRID_SPACING,
    EDD_GRID_JUST_ONE_VALUE,
    EDD_GRID_EVEN,
    EDD_GRID_UNEVEN,
    EDD_GRID_DIMENSION_TOOLTIP,
    EDD_GRID_VAR_HAS_DIM_TOOLTIP,
    EDD_GRID_SSS_TOOLTIP,
    EDD_GRID_START_TOOLTIP,
    EDD_GRID_STOP_TOOLTIP,
    EDD_GRID_STRIDE_TOOLTIP,
    EDD_GRID_SPACING_TOOLTIP,
    EDD_GRID_DOWNLOAD_TOOLTIP,
    EDD_GRID_GRID_VARIABLE_HTML,
    EDD_GRID_CHECK_ALL,
    EDD_GRID_CHECK_ALL_TOOLTIP,
    EDD_GRID_UNCHECK_ALL,
    EDD_GRID_UNCHECK_ALL_TOOLTIP,
    EDD_TABLE_CONSTRAINTS,
    EDD_TABLE_TABULAR_DATASET_TOOLTIP,
    EDD_TABLE_VARIABLE,
    EDD_TABLE_CHECK_ALL,
    EDD_TABLE_CHECK_ALL_TOOLTIP,
    EDD_TABLE_UNCHECK_ALL,
    EDD_TABLE_UNCHECK_ALL_TOOLTIP,
    EDD_TABLE_MINIMUM_TOOLTIP,
    EDD_TABLE_MAXIMUM_TOOLTIP,
    EDD_TABLE_CHECK_THE_VARIABLES,
    EDD_TABLE_SELECT_AN_OPERATOR,
    EDD_TABLE_FROM_EDD_GRID_SUMMARY,
    EDD_TABLE_OPT_CONSTRAINT1_HTML,
    EDD_TABLE_OPT_CONSTRAINT2_HTML,
    EDD_TABLE_OPT_CONSTRAINT_VAR,
    EDD_TABLE_NUMERIC_CONSTRAINT_TOOLTIP,
    EDD_TABLE_STRING_CONSTRAINT_TOOLTIP,
    EDD_TABLE_TIME_CONSTRAINT_TOOLTIP,
    EDD_TABLE_CONSTRAINT_TOOLTIP,
    EDD_TABLE_SELECT_CONSTRAINT_TOOLTIP,
    EDD_TABLE_DAP_DESCRIPTION,
    EDD_TABLE_DAP_LONG_DESCRIPTION,
    EDD_TABLE_DOWNLOAD_DATA_TOOLTIP,
    ERDDAP_IS,
    ERDDAP_VERSION_HTML,
    ERROR_TITLE,
    ERROR_FILE_NOT_FOUND,
    ERROR_FILE_NOT_FOUND_IMAGE,
    ERROR_INTERNAL,
    ERROR_JSONP_FUNCTION_NAME,
    ERROR_JSONP_NOT_ALLOWED,
    ERROR_MORE_THAN_2GB,
    ERROR_NOT_FOUND,
    ERROR_NOT_FOUND_IN,
    ERROR_ODV_LLT_GRID,
    ERROR_ODV_LLT_TABLE,
    ERROR_ON_WEB_PAGE,
    EXTERNAL_LINK,
    EXTERNAL_WEB_SITE,
    FILE_HELP_ASC,
    FILE_HELP_CROISSANT,
    FILE_HELP_CSV,
    FILE_HELP_CSVP,
    FILE_HELP_CSV0,
    FILE_HELP_DATA_TABLE,
    FILE_HELP_DAS,
    FILE_HELP_DDS,
    FILE_HELP_DODS,
    FILE_HELP_GRID_ESRI_ASCII,
    FILE_HELP_TABLE_ESRI_CSV,
    FILE_HELP_FGDC,
    FILE_HELP_GEO_JSON,
    FILE_HELP_HTML,
    FILE_HELP_HTML_TABLE,
    FILE_HELP_ISO19115,
    FILE_HELP_ITX_GRID,
    FILE_HELP_ITX_TABLE,
    FILE_HELP_JSON,
    FILE_HELP_JSONL_CSV1,
    FILE_HELP_JSONL_CSV,
    FILE_HELP_JSONL_KVP,
    FILE_HELP_MAT,
    FILE_HELP_GRID_NC3,
    FILE_HELP_GRID_NC4,
    FILE_HELP_TABLE_NC3,
    FILE_HELP_TABLE_NC4,
    FILE_HELP_NC3_HEADER,
    FILE_HELP_NC4_HEADER,
    FILE_HELP_NCCSV,
    FILE_HELP_NCCSV_METADATA,
    FILE_HELP_NC_CF,
    FILE_HELP_NC_CF_HEADER,
    FILE_HELP_NC_CFMA,
    FILE_HELP_NC_CFMA_HEADER,
    FILE_HELP_NCML,
    FILE_HELP_NCO_JSON,
    FILE_HELP_GRID_ODV_TXT,
    FILE_HELP_TABLE_ODV_TXT,
    FILE_HELP_PARQUET,
    FILE_HELP_PARQUET_WITH_META,
    FILE_HELP_TIME_GAPS,
    FILE_HELP_TSV,
    FILE_HELP_TSVP,
    FILE_HELP_TSV0,
    FILE_HELP_WAV,
    FILE_HELP_XHTML,
    FILE_HELP_GEOTIF,
    FILE_HELP_GRID_KML,
    FILE_HELP_TABLE_KML,
    FILE_HELP_SMALL_PDF,
    FILE_HELP_PDF,
    FILE_HELP_LARGE_PDF,
    FILE_HELP_SMALL_PNG,
    FILE_HELP_PNG,
    FILE_HELP_LARGE_PNG,
    FILE_HELP_TRANSPARENT_PNG,
    FILES_DESCRIPTION,
    FILES_WARNING,
    FIPS_COUNTY_CODES,
    FOR_SOS_USE,
    FOR_WCS_USE,
    FOR_WMS_USE,
    FUNCTIONS,
    FUNCTION_TOOLTIP,
    FUNCTION_DISTINCT_CHECK,
    FUNCTION_DISTINCT_TOOLTIP,
    FUNCTION_ORDER_BY_TOOLTIP,
    FUNCTION_ORDER_BY_SORT,
    FUNCTION_ORDER_BY_SORT1,
    FUNCTION_ORDER_BY_SORT2,
    FUNCTION_ORDER_BY_SORT3,
    FUNCTION_ORDER_BY_SORT4,
    FUNCTION_ORDER_BY_SORT_LEAST,
    GENERATED_AT,
    GEO_SERVICES_DESCRIPTION,
    GET_STARTED_HTML,
    HELP,
    HTML_TABLE_MAX_MESSAGE,
    IMAGE_DATA_COURTESY_OF,
    IMAGES_EMBED,
    INDEX_VIEW_ALL,
    INDEX_SEARCH_WITH,
    INDEX_DEVELOPERS_SEARCH,
    INDEX_PROTOCOL,
    INDEX_DESCRIPTION,
    INDEX_DATASETS,
    INDEX_DOCUMENTATION,
    INDEX_RESTFUL_SEARCH,
    INDEX_ALL_DATASETS_SEARCH,
    INDEX_OPEN_SEARCH,
    INDEX_SERVICES,
    INDEX_DESCRIBE_SERVICES,
    INDEX_METADATA,
    INDEX_WAF1,
    INDEX_WAF2,
    INDEX_CONVERTERS,
    INDEX_DESCRIBE_CONVERTERS,
    INFO_ABOUT_FROM,
    INFO_TABLE_TITLE_HTML,
    INFO_REQUEST_FORM,
    INFORMATION,
    INOTIFY_FIX,
    INTERPOLATE,
    JAVA_PROGRAMS_HTML,
    JUST_GENERATE_AND_VIEW,
    JUST_GENERATE_AND_VIEW_TOOLTIP,
    JUST_GENERATE_AND_VIEW_URL,
    JUST_GENERATE_AND_VIEW_GRAPH_URL_TOOLTIP,
    KEYWORDS,
    LANG_CODE,
    LEGAL_NOTICES,
    LEGAL_NOTICES_TITLE,
    LICENSE,
    LIKE_THIS,
    LIST_ALL,
    LIST_OF_DATASETS,
    LOGIN,
    LOGIN_HTML,
    LOGIN_ATTEMPT_BLOCKED,
    LOGIN_DESCRIBE_CUSTOM,
    LOGIN_DESCRIBE_EMAIL,
    LOGIN_DESCRIBE_GOOGLE,
    LOGIN_DESCRIBE_ORCID,
    LOGIN_DESCRIBE_OAUTH2,
    LOGIN_CAN_NOT,
    LOGIN_ARE_NOT,
    LOGIN_TO_LOG_IN,
    LOGIN_YOUR_EMAIL_ADDRESS,
    LOGIN_USER_NAME,
    LOGIN_PASSWORD,
    LOGIN_USER_NAME_AND_PASSWORD,
    LOGIN_GOOGLE_SIGN_IN,
    LOGIN_ORCID_SIGN_IN,
    LOGIN_AS,
    LOGIN_PARTWAY_AS,
    LOGIN_FAILED,
    LOGIN_SUCCEEDED,
    LOGIN_INVALID,
    LOGIN_NOT,
    LOGIN_BACK,
    LOGIN_PROBLEM_EXACT,
    LOGIN_PROBLEM_EXPIRE,
    LOGIN_PROBLEM_GOOGLE_AGAIN,
    LOGIN_PROBLEM_ORCID_AGAIN,
    LOGIN_PROBLEM_OAUTH2_AGAIN,
    LOGIN_PROBLEM_SAME_BROWSER,
    LOGIN_PROBLEM_3_TIMES,
    LOGIN_PROBLEMS,
    LOGIN_PROBLEMS_AFTER,
    LOGIN_PUBLIC_ACCESS,
    LOG_OUT,
    LOGOUT,
    LOGOUT_SUCCESS,
    MAG,
    MAG_AXIS_X,
    MAG_AXIS_Y,
    MAG_AXIS_COLOR,
    MAG_AXIS_STICK_X,
    MAG_AXIS_STICK_Y,
    MAG_AXIS_VECTOR_X,
    MAG_AXIS_VECTOR_Y,
    MAG_AXIS_HELP_GRAPH_X,
    MAG_AXIS_HELP_GRAPH_Y,
    MAG_AXIS_HELP_MARKER_COLOR,
    MAG_AXIS_HELP_SURFACE_COLOR,
    MAG_AXIS_HELP_STICK_X,
    MAG_AXIS_HELP_STICK_Y,
    MAG_AXIS_HELP_MAP_X,
    MAG_AXIS_HELP_MAP_Y,
    MAG_AXIS_HELP_VECTOR_X,
    MAG_AXIS_HELP_VECTOR_Y,
    MAG_AXIS_VAR_HELP,
    MAG_AXIS_VAR_HELP_GRID,
    MAG_CONSTRAINT_HELP,
    MAG_DOCUMENTATION,
    MAG_DOWNLOAD,
    MAG_DOWNLOAD_TOOLTIP,
    MAG_FILE_TYPE,
    MAG_GRAPH_TYPE,
    MAG_GRAPH_TYPE_TOOLTIP_GRID,
    MAG_GRAPH_TYPE_TOOLTIP_TABLE,
    MAG_GS,
    MAG_GS_MARKER_TYPE,
    MAG_GS_SIZE,
    MAG_GS_COLOR,
    MAG_GS_COLOR_BAR,
    MAG_GS_COLOR_BAR_TOOLTIP,
    MAG_GS_CONTINUITY,
    MAG_GS_CONTINUITY_TOOLTIP,
    MAG_GS_SCALE,
    MAG_GS_SCALE_TOOLTIP,
    MAG_GS_MIN,
    MAG_GS_MIN_TOOLTIP,
    MAG_GS_MAX,
    MAG_GS_MAX_TOOLTIP,
    MAG_GS_N_SECTIONS,
    MAG_GS_N_SECTIONS_TOOLTIP,
    MAG_GS_LAND_MASK,
    MAG_GS_LAND_MASK_TOOLTIP_GRID,
    MAG_GS_LAND_MASK_TOOLTIP_TABLE,
    MAG_GS_VECTOR_STANDARD,
    MAG_GS_VECTOR_STANDARD_TOOLTIP,
    MAG_GS_Y_ASCENDING_TOOLTIP,
    MAG_GS_Y_AXIS_MIN,
    MAG_GS_Y_AXIS_MAX,
    MAG_GS_Y_RANGE_MIN_TOOLTIP,
    MAG_GS_Y_RANGE_MAX_TOOLTIP,
    MAG_GS_Y_RANGE_TOOLTIP,
    MAG_GS_Y_SCALE_TOOLTIP,
    MAG_ITEM_FIRST,
    MAG_ITEM_PREVIOUS,
    MAG_ITEM_NEXT,
    MAG_ITEM_LAST,
    MAG_JUST_1_VALUE,
    MAG_RANGE,
    MAG_RANGE_TO,
    MAG_REDRAW,
    MAG_REDRAW_TOOLTIP,
    MAG_TIME_RANGE,
    MAG_TIME_RANGE_FIRST,
    MAG_TIME_RANGE_BACK,
    MAG_TIME_RANGE_FORWARD,
    MAG_TIME_RANGE_LAST,
    MAG_TIME_RANGE_TOOLTIP,
    MAG_TIME_RANGE_TOOLTIP2,
    MAG_TIMES_VARY,
    MAG_VIEW_URL,
    MAG_ZOOM,
    MAG_ZOOM_CENTER,
    MAG_ZOOM_CENTER_TOOLTIP,
    MAG_ZOOM_IN,
    MAG_ZOOM_IN_TOOLTIP,
    MAG_ZOOM_OUT,
    MAG_ZOOM_OUT_TOOLTIP,
    MAG_ZOOM_A_LITTLE,
    MAG_ZOOM_DATA,
    MAG_ZOOM_OUT_DATA,
    MAG_GRID_TOOLTIP,
    MAG_TABLE_TOOLTIP,
    METADATA_DOWNLOAD,
    MORE_INFORMATION,
    N_MATCHING_1,
    N_MATCHING,
    N_MATCHING_ALPHABETICAL,
    N_MATCHING_MOST_RELEVANT,
    N_MATCHING_PAGE,
    N_MATCHING_CURRENT,
    NO_DATA_FIXED_VALUE,
    NO_DATA_NO_LL,
    NO_DATASET_WITH,
    NO_PAGE_1,
    NO_PAGE_2,
    NOT_ALLOWED,
    NOT_AUTHORIZED,
    NOT_AUTHORIZED_FOR_DATA,
    NOT_AVAILABLE,
    NOTE,
    NO_XXX,
    NO_XXX_BECAUSE,
    NO_XXX_BECAUSE_2,
    NO_XXX_NOT_ACTIVE,
    NO_XXX_NO_AXIS_1,
    NO_XXX_NO_COLOR_BAR,
    NO_XXX_NO_CDM_DATA_TYPE,
    NO_XXX_NO_LL,
    NO_XXX_NO_LL_EVENLY_SPACED,
    NO_XXX_NO_LL_GT_1,
    NO_XXX_NO_LLT,
    NO_XXX_NO_LON_IN_180,
    NO_XXX_NO_NON_STRING,
    NO_XXX_NO_2_NON_STRING,
    NO_XXX_NO_STATION_ID,
    NO_XXX_NO_SUBSET_VARIABLES,
    NO_XXX_NO_OLL_SUBSET_VARIABLES,
    NO_XXX_ITS_GRIDDED,
    NO_XXX_ITS_TABULAR,
    ONE_REQUEST_AT_A_TIME,
    OPEN_SEARCH_DESCRIPTION,
    OPTIONAL,
    OPTIONS,
    OR_A_LIST_OF_VALUES,
    OR_REFINE_SEARCH_WITH,
    OR_COMMA,
    OTHER_FEATURES,
    OUT_OF_DATE_DATASETS,
    OUT_OF_DATE_KEEP_TRACK,
    OUT_OF_DATE_HTML,
    PATIENT_DATA,
    PATIENT_YOUR_GRAPH,
    PERCENT_ENCODE,
    PROTOCOL_SEARCH_HTML,
    PROTOCOL_SEARCH_2_HTML,
    PROTOCOL_CLICK,
    QUERY_ERROR,
    QUERY_ERROR_180,
    QUERY_ERROR_1_VALUE,
    QUERY_ERROR_1_VAR,
    QUERY_ERROR_2_VAR,
    QUERY_ERROR_ACTUAL_RANGE,
    QUERY_ERROR_ADJUSTED,
    QUERY_ERROR_CONSTRAINT_NAN,
    QUERY_ERROR_EQUAL_SPACING,
    QUERY_ERROR_EXPECTED_AT,
    QUERY_ERROR_FILE_TYPE,
    QUERY_ERROR_INVALID,
    QUERY_ERROR_LL,
    QUERY_ERROR_LL_GT_1,
    QUERY_ERROR_LLT,
    QUERY_ERROR_NEVER_TRUE,
    QUERY_ERROR_NEVER_BOTH_TRUE,
    QUERY_ERROR_NOT_AXIS,
    QUERY_ERROR_NOT_EXPECTED_AT,
    QUERY_ERROR_NOT_FOUND_AFTER,
    QUERY_ERROR_OCCURS_TWICE,
    QUERY_ERROR_ORDER_BY_CLOSEST,
    QUERY_ERROR_ORDER_BY_LIMIT,
    QUERY_ERROR_ORDER_BY_MEAN,
    QUERY_ERROR_ORDER_BY_SUM,
    QUERY_ERROR_ORDER_BY_VARIABLE,
    QUERY_ERROR_UNKNOWN_VARIABLE,
    QUERY_ERROR_GRID_1_AXIS,
    QUERY_ERROR_GRID_AMP,
    QUERY_ERROR_GRID_DIAGNOSTIC,
    QUERY_ERROR_GRID_BETWEEN,
    QUERY_ERROR_GRID_LESS_MIN,
    QUERY_ERROR_GRID_GREATER_MAX,
    QUERY_ERROR_GRID_MISSING,
    QUERY_ERROR_GRID_NO_AXIS_VAR,
    QUERY_ERROR_GRID_NO_DATA_VAR,
    QUERY_ERROR_GRID_NOT_IDENTICAL,
    QUERY_ERROR_LAST_END_P,
    QUERY_ERROR_LAST_EXPECTED,
    QUERY_ERROR_LAST_UNEXPECTED,
    QUERY_ERROR_LAST_PM_INVALID,
    QUERY_ERROR_LAST_PM_INTEGER,
    RANGES_FROM_TO,
    REQUIRED,
    RESET_THE_FORM,
    RESET_THE_FORM_WAS,
    RESOURCE_NOT_FOUND,
    RESTFUL_WEB_SERVICES,
    RESTFUL_HTML,
    RESTFUL_HTML_CONTINUED,
    RESTFUL_GET_ALL_DATASET,
    RESTFUL_PROTOCOLS,
    SOS_DOCUMENTATION,
    WCS_DOCUMENTATION,
    WMS_DOCUMENTATION,
    REQUEST_FORMAT_EXAMPLES_HTML,
    RESULTS_FORMAT_EXAMPLES_HTML,
    RESULTS_OF_SEARCH_FOR,
    RESTFUL_INFORMATION_FORMATS,
    RESTFUL_VIA_SERVICE,
    ROWS,
    RSS_NO,
    SEARCH_TITLE,
    SEARCH_DO_FULL_TEXT_HTML,
    SEARCH_FULL_TEXT_HTML,
    SEARCH_HINTS_LUCENE_TOOLTIP,
    SEARCH_HINTS_ORIGINAL_TOOLTIP,
    SEARCH_HINTS_TOOLTIP,
    SEARCH_BUTTON,
    SEARCH_CLICK_TIP,
    SEARCH_MULTIPLE_ERDDAPS,
    SEARCH_MULTIPLE_ERDDAPS_DESCRIPTION,
    SEARCH_NOT_AVAILABLE,
    SEARCH_TIP,
    SEARCH_SPELLING,
    SEARCH_FEWER_WORDS,
    SEARCH_WITH_QUERY,
    SEE_PROTOCOL_DOCUMENTATION,
    SELECT_NEXT,
    SELECT_PREVIOUS,
    SHIFT_X_ALL_THE_WAY_LEFT,
    SHIFT_X_LEFT,
    SHIFT_X_RIGHT,
    SHIFT_X_ALL_THE_WAY_RIGHT,
    SLIDE_SORTER,
    SOS,
    SOS_DESCRIPTION_HTML,
    SOS_LONG_DESCRIPTION_HTML,
    SOS_OVERVIEW_1,
    SOS_OVERVIEW_2,
    SS_USE,
    SS_USE_PLAIN,
    SS_BE_PATIENT,
    SS_INSTRUCTIONS_HTML,
    STANDARD_SHORT_DESCRIPTION_HTML,
    STATUS,
    STATUS_HTML,
    SUBMIT,
    SUBMIT_TOOLTIP,
    SUBSCRIPTION_OFFER_RSS,
    SUBSCRIPTION_OFFER_URL,
    SUBSCRIPTIONS_TITLE,
    SUBSCRIPTION_EMAIL_LIST,
    SUBSCRIPTION_ADD,
    SUBSCRIPTION_ADD_HTML,
    SUBSCRIPTION_VALIDATE,
    SUBSCRIPTION_VALIDATE_HTML,
    SUBSCRIPTION_LIST,
    SUBSCRIPTION_LIST_HTML,
    SUBSCRIPTION_REMOVE,
    SUBSCRIPTION_REMOVE_HTML,
    SUBSCRIPTION_ABUSE,
    SUBSCRIPTION_ADD_ERROR,
    SUBSCRIPTION_ADD_2,
    SUBSCRIPTION_ADD_SUCCESS,
    SUBSCRIPTION_EMAIL,
    SUBSCRIPTION_EMAIL_ON_BLACKLIST,
    SUBSCRIPTION_EMAIL_INVALID,
    SUBSCRIPTION_EMAIL_TOO_LONG,
    SUBSCRIPTION_EMAIL_UNSPECIFIED,
    SUBSCRIPTION_0_HTML,
    SUBSCRIPTION_1_HTML,
    SUBSCRIPTION_2_HTML,
    SUBSCRIPTION_ID_INVALID,
    SUBSCRIPTION_ID_TOO_LONG,
    SUBSCRIPTION_ID_UNSPECIFIED,
    SUBSCRIPTION_KEY_INVALID,
    SUBSCRIPTION_KEY_UNSPECIFIED,
    SUBSCRIPTION_LIST_ERROR,
    SUBSCRIPTION_LIST_SUCCESS,
    SUBSCRIPTION_REMOVE_ERROR,
    SUBSCRIPTION_REMOVE_2,
    SUBSCRIPTION_REMOVE_SUCCESS,
    SUBSCRIPTION_RSS,
    SUBSCRIPTION_URL_HTML,
    SUBSCRIPTION_URL_INVALID,
    SUBSCRIPTION_URL_TOO_LONG,
    SUBSCRIPTION_VALIDATE_ERROR,
    SUBSCRIPTION_VALIDATE_SUCCESS,
    SUBSET,
    SUBSET_SELECT,
    SUBSET_N_MATCHING,
    SUBSET_INSTRUCTIONS,
    SUBSET_OPTION,
    SUBSET_OPTIONS,
    SUBSET_REFINE_MAP_DOWNLOAD,
    SUBSET_REFINE_SUBSET_DOWNLOAD,
    SUBSET_CLICK_RESET_CLOSEST,
    SUBSET_CLICK_RESET_LL,
    SUBSET_METADATA,
    SUBSET_COUNT,
    SUBSET_PERCENT,
    SUBSET_VIEW_SELECT,
    SUBSET_VIEW_SELECT_DISTINCT_COMBOS,
    SUBSET_VIEW_SELECT_RELATED_COUNTS,
    SUBSET_WHEN,
    SUBSET_WHEN_NO_CONSTRAINTS,
    SUBSET_WHEN_COUNTS,
    SUBSET_N_VARIABLE_COMBOS,
    SUBSET_SHOWING_ALL_ROWS,
    SUBSET_SHOWING_N_ROWS,
    SUBSET_CHANGE_SHOWING,
    SUBSET_N_ROWS_RELATED_DATA,
    SUBSET_VIEW_RELATED_CHANGE,
    SUBSET_TOTAL_COUNT,
    SUBSET_VIEW,
    SUBSET_VIEW_CHECK,
    SUBSET_VIEW_CHECK_1,
    SUBSET_VIEW_DISTINCT_MAP,
    SUBSET_VIEW_RELATED_MAP,
    SUBSET_VIEW_DISTINCT_DATA_COUNTS,
    SUBSET_VIEW_DISTINCT_DATA,
    SUBSET_VIEW_RELATED_DATA_COUNTS,
    SUBSET_VIEW_RELATED_DATA,
    SUBSET_VIEW_DISTINCT_MAP_TOOLTIP,
    SUBSET_VIEW_RELATED_MAP_TOOLTIP,
    SUBSET_VIEW_DISTINCT_DATA_COUNTS_TOOLTIP,
    SUBSET_VIEW_DISTINCT_DATA_TOOLTIP,
    SUBSET_VIEW_RELATED_DATA_COUNTS_TOOLTIP,
    SUBSET_VIEW_RELATED_DATA_TOOLTIP,
    SUBSET_WARN,
    SUBSET_WARN_10000,
    SUBSET_TOOLTIP,
    SUBSET_NOT_SET_UP,
    SUBSET_LONG_NOT_SHOWN,
    TABLEDAP_VIDEO_INTRO,
    THE_DATASET_ID,
    THE_KEY,
    THE_SUBSCRIPTION_ID,
    THE_URL_ACTION,
    THIS_PARTICULAR_ERDDAP,
    TIME,
    TIMEOUT_OTHER_REQUESTS,
    UNITS,
    UNKNOWN_DATASET_ID,
    UNKNOWN_PROTOCOL,
    UNSUPPORTED_FILE_TYPE,
    USING_GRIDDAP,
    USING_TABLEDAP,
    VARIABLE_NAMES,
    WAIT_THEN_TRY_AGAIN,
    WARNING,
    WCS,
    WCS_DESCRIPTION_HTML,
    WCS_LONG_DESCRIPTION_HTML,
    WCS_OVERVIEW_1,
    WCS_OVERVIEW_2,
    WMS_DESCRIPTION_HTML,
    WMS_DOCUMENTATION_1,
    WMS_GET_CAPABILITIES,
    WMS_GET_MAP,
    WMS_NOTES,
    WMS_INSTRUCTIONS,
    WMS_LONG_DESCRIPTION_HTML,
    WMS_MANY_DATASETS,
    YOUR_EMAIL_ADDRESS,
    ZOOM_IN,
    ZOOM_OUT,
    THE_LONG_DESCRIPTION_HTML
  }

  private Map<Message, String[]> translatedMessages = new HashMap<>();
  public final int[] imageWidths;
  public final int[] imageHeights;
  public final int[] pdfWidths;
  public final int[] pdfHeights;
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
      URL messageFile = URI.create(TranslateMessages.translatedMessagesDir + tName).toURL();
      messagesAr[tl] = ResourceBundle2.fromXml(XML.parseXml(messageFile, false));
    }
    // read all the static Strings from messages.xml
    errorInMethod = "ERROR while reading from all the messages.xml files: ";
    translatedMessages.put(
        Message.ACCEPT_ENCODING_HTML,
        getNotNothingString(messagesAr, "acceptEncodingHtml", errorInMethod));
    translatedMessages.put(
        Message.ACCESS_RESTFUL, getNotNothingString(messagesAr, "accessRestful", errorInMethod));
    translatedMessages.put(
        Message.ACRONYMS, getNotNothingString(messagesAr, "acronyms", errorInMethod));
    translatedMessages.put(
        Message.ADD_CONSTRAINTS, getNotNothingString(messagesAr, "addConstraints", errorInMethod));
    translatedMessages.put(
        Message.ADD_VAR_WHERE_ATT_VALUE,
        getNotNothingString(messagesAr, "addVarWhereAttValue", errorInMethod));
    translatedMessages.put(
        Message.ADD_VAR_WHERE, getNotNothingString(messagesAr, "addVarWhere", errorInMethod));
    translatedMessages.put(
        Message.ADDITIONAL_LINKS,
        getNotNothingString(messagesAr, "additionalLinks", errorInMethod));

    admKeywords = messagesAr[0].getNotNothingString("admKeywords", errorInMethod);
    admSubsetVariables = messagesAr[0].getNotNothingString("admSubsetVariables", errorInMethod);
    translatedMessages.put(
        Message.ADM_SUMMARY, getNotNothingString(messagesAr, "admSummary", errorInMethod));
    translatedMessages.put(
        Message.ADM_TITLE, getNotNothingString(messagesAr, "admTitle", errorInMethod));
    advl_datasetID = messagesAr[0].getNotNothingString("advl_datasetID", errorInMethod);
    translatedMessages.put(
        Message.ADVC_ACCESSIBLE, getNotNothingString(messagesAr, "advc_accessible", errorInMethod));
    translatedMessages.put(
        Message.ADVL_ACCESSIBLE, getNotNothingString(messagesAr, "advl_accessible", errorInMethod));
    translatedMessages.put(
        Message.ADVL_INSTITUTION,
        getNotNothingString(messagesAr, "advl_institution", errorInMethod));
    translatedMessages.put(
        Message.ADVC_DATA_STRUCTURE,
        getNotNothingString(messagesAr, "advc_dataStructure", errorInMethod));
    translatedMessages.put(
        Message.ADVL_DATA_STRUCTURE,
        getNotNothingString(messagesAr, "advl_dataStructure", errorInMethod));
    advr_dataStructure = messagesAr[0].getNotNothingString("advr_dataStructure", errorInMethod);
    translatedMessages.put(
        Message.ADVL_CDM_DATA_TYPE,
        getNotNothingString(messagesAr, "advl_cdm_data_type", errorInMethod));
    advr_cdm_data_type = messagesAr[0].getNotNothingString("advr_cdm_data_type", errorInMethod);
    translatedMessages.put(
        Message.ADVL_CLASS, getNotNothingString(messagesAr, "advl_class", errorInMethod));
    advr_class = messagesAr[0].getNotNothingString("advr_class", errorInMethod);

    translatedMessages.put(
        Message.ADVL_TITLE, getNotNothingString(messagesAr, "advl_title", errorInMethod));
    translatedMessages.put(
        Message.ADVL_MIN_LONGITUDE,
        getNotNothingString(messagesAr, "advl_minLongitude", errorInMethod));
    translatedMessages.put(
        Message.ADVL_MAX_LONGITUDE,
        getNotNothingString(messagesAr, "advl_maxLongitude", errorInMethod));
    translatedMessages.put(
        Message.ADVL_LONGITUDE_SPACING,
        getNotNothingString(messagesAr, "advl_longitudeSpacing", errorInMethod));
    translatedMessages.put(
        Message.ADVL_MIN_LATITUDE,
        getNotNothingString(messagesAr, "advl_minLatitude", errorInMethod));
    translatedMessages.put(
        Message.ADVL_MAX_LATITUDE,
        getNotNothingString(messagesAr, "advl_maxLatitude", errorInMethod));
    translatedMessages.put(
        Message.ADVL_LATITUDE_SPACING,
        getNotNothingString(messagesAr, "advl_latitudeSpacing", errorInMethod));
    translatedMessages.put(
        Message.ADVL_MIN_ALTITUDE,
        getNotNothingString(messagesAr, "advl_minAltitude", errorInMethod));
    translatedMessages.put(
        Message.ADVL_MAX_ALTITUDE,
        getNotNothingString(messagesAr, "advl_maxAltitude", errorInMethod));
    translatedMessages.put(
        Message.ADVL_MIN_TIME, getNotNothingString(messagesAr, "advl_minTime", errorInMethod));
    translatedMessages.put(
        Message.ADVC_MAX_TIME, getNotNothingString(messagesAr, "advc_maxTime", errorInMethod));
    translatedMessages.put(
        Message.ADVL_MAX_TIME, getNotNothingString(messagesAr, "advl_maxTime", errorInMethod));
    translatedMessages.put(
        Message.ADVL_TIME_SPACING,
        getNotNothingString(messagesAr, "advl_timeSpacing", errorInMethod));
    translatedMessages.put(
        Message.ADVC_GRIDDAP, getNotNothingString(messagesAr, "advc_griddap", errorInMethod));
    translatedMessages.put(
        Message.ADVL_GRIDDAP, getNotNothingString(messagesAr, "advl_griddap", errorInMethod));
    translatedMessages.put(
        Message.ADVL_SUBSET, getNotNothingString(messagesAr, "advl_subset", errorInMethod));
    translatedMessages.put(
        Message.ADVC_TABLEDAP, getNotNothingString(messagesAr, "advc_tabledap", errorInMethod));
    translatedMessages.put(
        Message.ADVL_TABLEDAP, getNotNothingString(messagesAr, "advl_tabledap", errorInMethod));
    translatedMessages.put(
        Message.ADVL_MAKE_A_GRAPH,
        getNotNothingString(messagesAr, "advl_MakeAGraph", errorInMethod));
    translatedMessages.put(
        Message.ADVC_SOS, getNotNothingString(messagesAr, "advc_sos", errorInMethod));
    translatedMessages.put(
        Message.ADVL_SOS, getNotNothingString(messagesAr, "advl_sos", errorInMethod));
    translatedMessages.put(
        Message.ADVL_WCS, getNotNothingString(messagesAr, "advl_wcs", errorInMethod));
    translatedMessages.put(
        Message.ADVL_WMS, getNotNothingString(messagesAr, "advl_wms", errorInMethod));
    translatedMessages.put(
        Message.ADVC_FILES, getNotNothingString(messagesAr, "advc_files", errorInMethod));
    translatedMessages.put(
        Message.ADVL_FILES, getNotNothingString(messagesAr, "advl_files", errorInMethod));
    translatedMessages.put(
        Message.ADVC_FGDC, getNotNothingString(messagesAr, "advc_fgdc", errorInMethod));
    translatedMessages.put(
        Message.ADVL_FGDC, getNotNothingString(messagesAr, "advl_fgdc", errorInMethod));
    translatedMessages.put(
        Message.ADVC_ISO19115, getNotNothingString(messagesAr, "advc_iso19115", errorInMethod));
    translatedMessages.put(
        Message.ADVL_ISO19115, getNotNothingString(messagesAr, "advl_iso19115", errorInMethod));
    translatedMessages.put(
        Message.ADVC_METADATA, getNotNothingString(messagesAr, "advc_metadata", errorInMethod));
    translatedMessages.put(
        Message.ADVL_METADATA, getNotNothingString(messagesAr, "advl_metadata", errorInMethod));
    translatedMessages.put(
        Message.ADVL_SOURCE_URL, getNotNothingString(messagesAr, "advl_sourceUrl", errorInMethod));
    translatedMessages.put(
        Message.ADVL_INFO_URL, getNotNothingString(messagesAr, "advl_infoUrl", errorInMethod));
    translatedMessages.put(
        Message.ADVL_RSS, getNotNothingString(messagesAr, "advl_rss", errorInMethod));
    translatedMessages.put(
        Message.ADVC_EMAIL, getNotNothingString(messagesAr, "advc_email", errorInMethod));
    translatedMessages.put(
        Message.ADVL_EMAIL, getNotNothingString(messagesAr, "advl_email", errorInMethod));
    translatedMessages.put(
        Message.ADVL_SUMMARY, getNotNothingString(messagesAr, "advl_summary", errorInMethod));
    translatedMessages.put(
        Message.ADVC_TEST_OUT_OF_DATE,
        getNotNothingString(messagesAr, "advc_testOutOfDate", errorInMethod));
    translatedMessages.put(
        Message.ADVL_TEST_OUT_OF_DATE,
        getNotNothingString(messagesAr, "advl_testOutOfDate", errorInMethod));
    translatedMessages.put(
        Message.ADVC_OUT_OF_DATE, getNotNothingString(messagesAr, "advc_outOfDate", errorInMethod));
    translatedMessages.put(
        Message.ADVL_OUT_OF_DATE, getNotNothingString(messagesAr, "advl_outOfDate", errorInMethod));
    translatedMessages.put(
        Message.ADVN_OUT_OF_DATE, getNotNothingString(messagesAr, "advn_outOfDate", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH, getNotNothingString(messagesAr, "advancedSearch", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_RESULTS,
        getNotNothingString(messagesAr, "advancedSearchResults", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_DIRECTIONS,
        getNotNothingString(messagesAr, "advancedSearchDirections", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_TOOLTIP,
        getNotNothingString(messagesAr, "advancedSearchTooltip", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_BOUNDS,
        getNotNothingString(messagesAr, "advancedSearchBounds", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_MIN_LAT,
        getNotNothingString(messagesAr, "advancedSearchMinLat", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_MAX_LAT,
        getNotNothingString(messagesAr, "advancedSearchMaxLat", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_MIN_LON,
        getNotNothingString(messagesAr, "advancedSearchMinLon", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_MAX_LON,
        getNotNothingString(messagesAr, "advancedSearchMaxLon", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_MIN_MAX_LON,
        getNotNothingString(messagesAr, "advancedSearchMinMaxLon", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_MIN_TIME,
        getNotNothingString(messagesAr, "advancedSearchMinTime", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_MAX_TIME,
        getNotNothingString(messagesAr, "advancedSearchMaxTime", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_CLEAR,
        getNotNothingString(messagesAr, "advancedSearchClear", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_CLEAR_HELP,
        getNotNothingString(messagesAr, "advancedSearchClearHelp", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_CATEGORY_TOOLTIP,
        getNotNothingString(messagesAr, "advancedSearchCategoryTooltip", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_RANGE_TOOLTIP,
        getNotNothingString(messagesAr, "advancedSearchRangeTooltip", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_MAP_TOOLTIP,
        getNotNothingString(messagesAr, "advancedSearchMapTooltip", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_LON_TOOLTIP,
        getNotNothingString(messagesAr, "advancedSearchLonTooltip", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_TIME_TOOLTIP,
        getNotNothingString(messagesAr, "advancedSearchTimeTooltip", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_WITH_CRITERIA,
        getNotNothingString(messagesAr, "advancedSearchWithCriteria", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_FEWER_CRITERIA,
        getNotNothingString(messagesAr, "advancedSearchFewerCriteria", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_NO_CRITERIA,
        getNotNothingString(messagesAr, "advancedSearchNoCriteria", errorInMethod));
    translatedMessages.put(
        Message.ADVANCED_SEARCH_ERROR_HANDLING,
        getNotNothingString(messagesAr, "advancedSearchErrorHandling", errorInMethod));

    translatedMessages.put(
        Message.AUTO_REFRESH, getNotNothingString(messagesAr, "autoRefresh", errorInMethod));
    translatedMessages.put(
        Message.BLACKLIST_MSG, getNotNothingString(messagesAr, "blacklistMsg", errorInMethod));
    translatedMessages.put(
        Message.BROUGHT_TO_YOU_BY,
        getNotNothingString(messagesAr, "BroughtToYouBy", errorInMethod));

    translatedMessages.put(
        Message.CATEGORY_TITLE_HTML,
        getNotNothingString(messagesAr, "categoryTitleHtml", errorInMethod));
    translatedMessages.put(
        Message.CATEGORY_HTML, getNotNothingString(messagesAr, "categoryHtml", errorInMethod));
    translatedMessages.put(
        Message.CATEGORY3_HTML, getNotNothingString(messagesAr, "category3Html", errorInMethod));
    translatedMessages.put(
        Message.CATEGORY_PICK_ATTRIBUTE,
        getNotNothingString(messagesAr, "categoryPickAttribute", errorInMethod));
    translatedMessages.put(
        Message.CATEGORY_SEARCH_HTML,
        getNotNothingString(messagesAr, "categorySearchHtml", errorInMethod));
    translatedMessages.put(
        Message.CATEGORY_CLICK_HTML,
        getNotNothingString(messagesAr, "categoryClickHtml", errorInMethod));
    translatedMessages.put(
        Message.CATEGORY_NOT_AN_OPTION,
        getNotNothingString(messagesAr, "categoryNotAnOption", errorInMethod));
    translatedMessages.put(
        Message.CAUGHT_INTERRUPTED,
        getNotNothingString(messagesAr, "caughtInterrupted", errorInMethod));
    for (int tl = 0; tl < nLanguages; tl++) {
      translatedMessages.get(Message.CAUGHT_INTERRUPTED)[tl] =
          " " + translatedMessages.get(Message.CAUGHT_INTERRUPTED)[tl];
    }

    translatedMessages.put(
        Message.CDM_DATA_TYPE_HELP,
        getNotNothingString(messagesAr, "cdmDataTypeHelp", errorInMethod));

    translatedMessages.put(
        Message.CLICK_ACCESS, getNotNothingString(messagesAr, "clickAccess", errorInMethod));
    translatedMessages.put(
        Message.CLICK_BACKGROUND_INFO,
        getNotNothingString(messagesAr, "clickBackgroundInfo", errorInMethod));
    translatedMessages.put(
        Message.CLICK_ERDDAP, getNotNothingString(messagesAr, "clickERDDAP", errorInMethod));
    translatedMessages.put(
        Message.CLICK_INFO, getNotNothingString(messagesAr, "clickInfo", errorInMethod));
    translatedMessages.put(
        Message.CLICK_TO_SUBMIT, getNotNothingString(messagesAr, "clickToSubmit", errorInMethod));
    translatedMessages.put(
        Message.CONVERT, getNotNothingString(messagesAr, "convert", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_BYPASS, getNotNothingString(messagesAr, "convertBypass", errorInMethod));

    translatedMessages.put(
        Message.CONVERT_COLORS, getNotNothingString(messagesAr, "convertCOLORs", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_COLORS_MESSAGE,
        getNotNothingString(messagesAr, "convertCOLORsMessage", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TO_A_FULL_NAME,
        getNotNothingString(messagesAr, "convertToAFullName", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TO_AN_ACRONYM,
        getNotNothingString(messagesAr, "convertToAnAcronym", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TO_A_COUNTY_NAME,
        getNotNothingString(messagesAr, "convertToACountyName", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TO_A_FIPS_CODE,
        getNotNothingString(messagesAr, "convertToAFIPSCode", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TO_GCMD, getNotNothingString(messagesAr, "convertToGCMD", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TO_CF_STANDARD_NAMES,
        getNotNothingString(messagesAr, "convertToCFStandardNames", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TO_NUMERIC_TIME,
        getNotNothingString(messagesAr, "convertToNumericTime", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TO_STRING_TIME,
        getNotNothingString(messagesAr, "convertToStringTime", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_ANY_STRING_TIME,
        getNotNothingString(messagesAr, "convertAnyStringTime", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TO_PROPER_TIME_UNITS,
        getNotNothingString(messagesAr, "convertToProperTimeUnits", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_FROM_UDUNITS_TO_UCUM,
        getNotNothingString(messagesAr, "convertFromUDUNITSToUCUM", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_FROM_UCUM_TO_UDUNITS,
        getNotNothingString(messagesAr, "convertFromUCUMToUDUNITS", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TO_UCUM, getNotNothingString(messagesAr, "convertToUCUM", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TO_UDUNITS,
        getNotNothingString(messagesAr, "convertToUDUNITS", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_STANDARDIZE_UDUNITS,
        getNotNothingString(messagesAr, "convertStandardizeUDUNITS", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TO_FULL_NAME,
        getNotNothingString(messagesAr, "convertToFullName", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TO_VARIABLE_NAME,
        getNotNothingString(messagesAr, "convertToVariableName", errorInMethod));

    translatedMessages.put(
        Message.CONVERTER_WEB_SERVICE,
        getNotNothingString(messagesAr, "converterWebService", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_OA_ACRONYMS,
        getNotNothingString(messagesAr, "convertOAAcronyms", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_OA_ACRONYMS_TO_FROM,
        getNotNothingString(messagesAr, "convertOAAcronymsToFrom", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_OA_ACRONYMS_INTRO,
        getNotNothingString(messagesAr, "convertOAAcronymsIntro", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_OA_ACRONYMS_NOTES,
        getNotNothingString(messagesAr, "convertOAAcronymsNotes", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_OA_ACRONYMS_SERVICE,
        getNotNothingString(messagesAr, "convertOAAcronymsService", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_OA_VARIABLE_NAMES,
        getNotNothingString(messagesAr, "convertOAVariableNames", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_OA_VARIABLE_NAMES_TO_FROM,
        getNotNothingString(messagesAr, "convertOAVariableNamesToFrom", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_OA_VARIABLE_NAMES_INTRO,
        getNotNothingString(messagesAr, "convertOAVariableNamesIntro", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_OA_VARIABLE_NAMES_NOTES,
        getNotNothingString(messagesAr, "convertOAVariableNamesNotes", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_OA_VARIABLE_NAMES_SERVICE,
        getNotNothingString(messagesAr, "convertOAVariableNamesService", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_FIPS_COUNTY,
        getNotNothingString(messagesAr, "convertFipsCounty", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_FIPS_COUNTY_INTRO,
        getNotNothingString(messagesAr, "convertFipsCountyIntro", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_FIPS_COUNTY_NOTES,
        getNotNothingString(messagesAr, "convertFipsCountyNotes", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_FIPS_COUNTY_SERVICE,
        getNotNothingString(messagesAr, "convertFipsCountyService", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_HTML, getNotNothingString(messagesAr, "convertHtml", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_INTERPOLATE,
        getNotNothingString(messagesAr, "convertInterpolate", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_INTERPOLATE_INTRO,
        getNotNothingString(messagesAr, "convertInterpolateIntro", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_INTERPOLATE_TLL_TABLE,
        getNotNothingString(messagesAr, "convertInterpolateTLLTable", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_INTERPOLATE_TLL_TABLE_HELP,
        getNotNothingString(messagesAr, "convertInterpolateTLLTableHelp", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_INTERPOLATE_DATASET_ID_VARIABLE,
        getNotNothingString(messagesAr, "convertInterpolateDatasetIDVariable", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_INTERPOLATE_DATASET_ID_VARIABLE_HELP,
        getNotNothingString(messagesAr, "convertInterpolateDatasetIDVariableHelp", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_INTERPOLATE_NOTES,
        getNotNothingString(messagesAr, "convertInterpolateNotes", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_INTERPOLATE_SERVICE,
        getNotNothingString(messagesAr, "convertInterpolateService", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_KEYWORDS,
        getNotNothingString(messagesAr, "convertKeywords", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_KEYWORDS_CF_TOOLTIP,
        getNotNothingString(messagesAr, "convertKeywordsCfTooltip", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_KEYWORDS_GCMD_TOOLTIP,
        getNotNothingString(messagesAr, "convertKeywordsGcmdTooltip", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_KEYWORDS_INTRO,
        getNotNothingString(messagesAr, "convertKeywordsIntro", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_KEYWORDS_NOTES,
        getNotNothingString(messagesAr, "convertKeywordsNotes", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_KEYWORDS_SERVICE,
        getNotNothingString(messagesAr, "convertKeywordsService", errorInMethod));

    translatedMessages.put(
        Message.CONVERT_TIME, getNotNothingString(messagesAr, "convertTime", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TIME_REFERENCE,
        getNotNothingString(messagesAr, "convertTimeReference", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TIME_INTRO,
        getNotNothingString(messagesAr, "convertTimeIntro", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TIME_NOTES,
        getNotNothingString(messagesAr, "convertTimeNotes", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TIME_SERVICE,
        getNotNothingString(messagesAr, "convertTimeService", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TIME_NUMBER_TOOLTIP,
        getNotNothingString(messagesAr, "convertTimeNumberTooltip", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TIME_STRING_TIME_TOOLTIP,
        getNotNothingString(messagesAr, "convertTimeStringTimeTooltip", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TIME_UNITS_TOOLTIP,
        getNotNothingString(messagesAr, "convertTimeUnitsTooltip", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TIME_UNITS_HELP,
        getNotNothingString(messagesAr, "convertTimeUnitsHelp", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TIME_ISO_FORMAT_ERROR,
        getNotNothingString(messagesAr, "convertTimeIsoFormatError", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TIME_NO_SINCE_ERROR,
        getNotNothingString(messagesAr, "convertTimeNoSinceError", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TIME_NUMBER_ERROR,
        getNotNothingString(messagesAr, "convertTimeNumberError", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TIME_NUMERIC_TIME_ERROR,
        getNotNothingString(messagesAr, "convertTimeNumericTimeError", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TIME_PARAMETERS_ERROR,
        getNotNothingString(messagesAr, "convertTimeParametersError", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TIME_STRING_FORMAT_ERROR,
        getNotNothingString(messagesAr, "convertTimeStringFormatError", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TIME_TWO_TIME_ERROR,
        getNotNothingString(messagesAr, "convertTimeTwoTimeError", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_TIME_UNITS_ERROR,
        getNotNothingString(messagesAr, "convertTimeUnitsError", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_UNITS, getNotNothingString(messagesAr, "convertUnits", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_UNITS_COMPARISON,
        getNotNothingString(messagesAr, "convertUnitsComparison", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_UNITS_FILTER,
        getNotNothingString(messagesAr, "convertUnitsFilter", errorInMethod));
    for (int tl = 0; tl < nLanguages; tl++) {
      translatedMessages.get(Message.CONVERT_UNITS_COMPARISON)[tl] =
          translatedMessages
              .get(Message.CONVERT_UNITS_COMPARISON)[tl]
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
    translatedMessages.put(
        Message.CONVERT_UNITS_INTRO,
        getNotNothingString(messagesAr, "convertUnitsIntro", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_UNITS_NOTES,
        getNotNothingString(messagesAr, "convertUnitsNotes", errorInMethod));
    for (int tl = 0; tl < nLanguages; tl++) {
      translatedMessages.get(Message.CONVERT_UNITS_NOTES)[tl] =
          translatedMessages.get(Message.CONVERT_UNITS_NOTES)[tl].replace(
              "&unitsStandard;", EDStatic.config.units_standard);
    }
    translatedMessages.put(
        Message.CONVERT_UNITS_SERVICE,
        getNotNothingString(messagesAr, "convertUnitsService", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_URLS, getNotNothingString(messagesAr, "convertURLs", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_URLS_INTRO,
        getNotNothingString(messagesAr, "convertURLsIntro", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_URLS_NOTES,
        getNotNothingString(messagesAr, "convertURLsNotes", errorInMethod));
    translatedMessages.put(
        Message.CONVERT_URLS_SERVICE,
        getNotNothingString(messagesAr, "convertURLsService", errorInMethod));
    translatedMessages.put(
        Message.COOKIES_HELP, getNotNothingString(messagesAr, "cookiesHelp", errorInMethod));
    translatedMessages.put(
        Message.COPY_IMAGE_TO_CLIPBOARD,
        getNotNothingString(messagesAr, "copyImageToClipboard", errorInMethod));
    translatedMessages.put(
        Message.COPY_TEXT_TO_CLIPBOARD,
        getNotNothingString(messagesAr, "copyTextToClipboard", errorInMethod));
    translatedMessages.put(
        Message.COPY_TO_CLIPBOARD_NOT_AVAILABLE,
        getNotNothingString(messagesAr, "copyToClipboardNotAvailable", errorInMethod));

    translatedMessages.put(Message.DAF, getNotNothingString(messagesAr, "daf", errorInMethod));
    translatedMessages.put(
        Message.DAF_GRID_BYPASS_TOOLTIP,
        getNotNothingString(messagesAr, "dafGridBypassTooltip", errorInMethod));
    translatedMessages.put(
        Message.DAF_GRID_TOOLTIP, getNotNothingString(messagesAr, "dafGridTooltip", errorInMethod));
    translatedMessages.put(
        Message.DAF_TABLE_BYPASS_TOOLTIP,
        getNotNothingString(messagesAr, "dafTableBypassTooltip", errorInMethod));
    translatedMessages.put(
        Message.DAF_TABLE_TOOLTIP,
        getNotNothingString(messagesAr, "dafTableTooltip", errorInMethod));
    translatedMessages.put(
        Message.DAS_TITLE, getNotNothingString(messagesAr, "dasTitle", errorInMethod));
    translatedMessages.put(
        Message.DATABASE_UNABLE_TO_CONNECT,
        getNotNothingString(messagesAr, "databaseUnableToConnect", errorInMethod));
    translatedMessages.put(
        Message.DATA_PROVIDER_FORM,
        getNotNothingString(messagesAr, "dataProviderForm", errorInMethod));
    translatedMessages.put(
        Message.DATA_PROVIDER_FORM_P1,
        getNotNothingString(messagesAr, "dataProviderFormP1", errorInMethod));
    translatedMessages.put(
        Message.DATA_PROVIDER_FORM_P2,
        getNotNothingString(messagesAr, "dataProviderFormP2", errorInMethod));
    translatedMessages.put(
        Message.DATA_PROVIDER_FORM_P3,
        getNotNothingString(messagesAr, "dataProviderFormP3", errorInMethod));
    translatedMessages.put(
        Message.DATA_PROVIDER_FORM_P4,
        getNotNothingString(messagesAr, "dataProviderFormP4", errorInMethod));
    translatedMessages.put(
        Message.DATA_PROVIDER_FORM_DONE,
        getNotNothingString(messagesAr, "dataProviderFormDone", errorInMethod));
    translatedMessages.put(
        Message.DATA_PROVIDER_FORM_SUCCESS,
        getNotNothingString(messagesAr, "dataProviderFormSuccess", errorInMethod));
    translatedMessages.put(
        Message.DATA_PROVIDER_FORM_SHORT_DESCRIPTION,
        getNotNothingString(messagesAr, "dataProviderFormShortDescription", errorInMethod));
    translatedMessages.put(
        Message.DATA_PROVIDER_FORM_LONG_DESCRIPTION_HTML,
        getNotNothingString(messagesAr, "dataProviderFormLongDescriptionHTML", errorInMethod));
    translatedMessages.put(
        Message.DISABLED, getNotNothingString(messagesAr, "disabled", errorInMethod));
    translatedMessages.put(
        Message.DATA_PROVIDER_FORM_PART1,
        getNotNothingString(messagesAr, "dataProviderFormPart1", errorInMethod));
    translatedMessages.put(
        Message.DATA_PROVIDER_FORM_PART2_HEADER,
        getNotNothingString(messagesAr, "dataProviderFormPart2Header", errorInMethod));
    translatedMessages.put(
        Message.DATA_PROVIDER_FORM_PART2_GLOBAL_METADATA,
        getNotNothingString(messagesAr, "dataProviderFormPart2GlobalMetadata", errorInMethod));
    translatedMessages.put(
        Message.DATA_PROVIDER_CONTACT_INFO,
        getNotNothingString(messagesAr, "dataProviderContactInfo", errorInMethod));
    translatedMessages.put(
        Message.DATA_PROVIDER_DATA,
        getNotNothingString(messagesAr, "dataProviderData", errorInMethod));
    translatedMessages.put(
        Message.DOCUMENTATION, getNotNothingString(messagesAr, "documentation", errorInMethod));

    translatedMessages.put(
        Message.DPF_SUBMIT, getNotNothingString(messagesAr, "dpf_submit", errorInMethod));
    translatedMessages.put(
        Message.DPF_FIX_PROBLEM, getNotNothingString(messagesAr, "dpf_fixProblem", errorInMethod));
    translatedMessages.put(
        Message.DPF_TITLE, getNotNothingString(messagesAr, "dpf_title", errorInMethod));
    translatedMessages.put(
        Message.DPF_TITLE_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_titleTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_SUMMARY, getNotNothingString(messagesAr, "dpf_summary", errorInMethod));
    translatedMessages.put(
        Message.DPF_SUMMARY_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_summaryTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_CREATOR_NAME,
        getNotNothingString(messagesAr, "dpf_creatorName", errorInMethod));
    translatedMessages.put(
        Message.DPF_CREATOR_NAME_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_creatorNameTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_CREATOR_TYPE,
        getNotNothingString(messagesAr, "dpf_creatorType", errorInMethod));
    translatedMessages.put(
        Message.DPF_CREATOR_TYPE_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_creatorTypeTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_CREATOR_EMAIL,
        getNotNothingString(messagesAr, "dpf_creatorEmail", errorInMethod));
    translatedMessages.put(
        Message.DPF_CREATOR_EMAIL_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_creatorEmailTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_INSTITUTION, getNotNothingString(messagesAr, "dpf_institution", errorInMethod));
    translatedMessages.put(
        Message.DPF_INSTITUTION_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_institutionTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_INFO_URL, getNotNothingString(messagesAr, "dpf_infoUrl", errorInMethod));
    translatedMessages.put(
        Message.DPF_INFO_URL_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_infoUrlTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_LICENSE, getNotNothingString(messagesAr, "dpf_license", errorInMethod));
    translatedMessages.put(
        Message.DPF_LICENSE_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_licenseTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_PROVIDE_IF_AVAILABLE,
        getNotNothingString(messagesAr, "dpf_provideIfAvailable", errorInMethod));
    translatedMessages.put(
        Message.DPF_ACKNOWLEDGEMENT,
        getNotNothingString(messagesAr, "dpf_acknowledgement", errorInMethod));
    translatedMessages.put(
        Message.DPF_ACKNOWLEDGEMENT_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_acknowledgementTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_HISTORY, getNotNothingString(messagesAr, "dpf_history", errorInMethod));
    translatedMessages.put(
        Message.DPF_HISTORY_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_historyTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_ID_TOOLTIP, getNotNothingString(messagesAr, "dpf_idTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_NAMING_AUTHORITY,
        getNotNothingString(messagesAr, "dpf_namingAuthority", errorInMethod));
    translatedMessages.put(
        Message.DPF_NAMING_AUTHORITY_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_namingAuthorityTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_PRODUCT_VERSION,
        getNotNothingString(messagesAr, "dpf_productVersion", errorInMethod));
    translatedMessages.put(
        Message.DPF_PRODUCT_VERSION_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_productVersionTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_REFERENCES, getNotNothingString(messagesAr, "dpf_references", errorInMethod));
    translatedMessages.put(
        Message.DPF_REFERENCES_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_referencesTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_COMMENT, getNotNothingString(messagesAr, "dpf_comment", errorInMethod));
    translatedMessages.put(
        Message.DPF_COMMENT_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_commentTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_DATA_TYPE_HELP,
        getNotNothingString(messagesAr, "dpf_dataTypeHelp", errorInMethod));
    translatedMessages.put(
        Message.DPF_IOOS_CATEGORY,
        getNotNothingString(messagesAr, "dpf_ioosCategory", errorInMethod));
    translatedMessages.put(
        Message.DPF_IOOS_CATEGORY_HELP,
        getNotNothingString(messagesAr, "dpf_ioosCategoryHelp", errorInMethod));
    translatedMessages.put(
        Message.DPF_PART3_HEADER,
        getNotNothingString(messagesAr, "dpf_part3Header", errorInMethod));
    translatedMessages.put(
        Message.DPF_VARIABLE_METADATA,
        getNotNothingString(messagesAr, "dpf_variableMetadata", errorInMethod));
    translatedMessages.put(
        Message.DPF_SOURCE_NAME, getNotNothingString(messagesAr, "dpf_sourceName", errorInMethod));
    translatedMessages.put(
        Message.DPF_SOURCE_NAME_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_sourceNameTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_DESTINATION_NAME,
        getNotNothingString(messagesAr, "dpf_destinationName", errorInMethod));
    translatedMessages.put(
        Message.DPF_DESTINATION_NAME_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_destinationNameTooltip", errorInMethod));

    translatedMessages.put(
        Message.DPF_LONG_NAME, getNotNothingString(messagesAr, "dpf_longName", errorInMethod));
    translatedMessages.put(
        Message.DPF_LONG_NAME_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_longNameTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_STANDARD_NAME,
        getNotNothingString(messagesAr, "dpf_standardName", errorInMethod));
    translatedMessages.put(
        Message.DPF_STANDARD_NAME_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_standardNameTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_DATA_TYPE, getNotNothingString(messagesAr, "dpf_dataType", errorInMethod));
    translatedMessages.put(
        Message.DPF_FILL_VALUE, getNotNothingString(messagesAr, "dpf_fillValue", errorInMethod));
    translatedMessages.put(
        Message.DPF_FILL_VALUE_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_fillValueTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_UNITS, getNotNothingString(messagesAr, "dpf_units", errorInMethod));
    translatedMessages.put(
        Message.DPF_UNITS_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_unitsTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_RANGE, getNotNothingString(messagesAr, "dpf_range", errorInMethod));
    translatedMessages.put(
        Message.DPF_RANGE_TOOLTIP,
        getNotNothingString(messagesAr, "dpf_rangeTooltip", errorInMethod));
    translatedMessages.put(
        Message.DPF_PART4_HEADER,
        getNotNothingString(messagesAr, "dpf_part4Header", errorInMethod));
    translatedMessages.put(
        Message.DPF_OTHER_COMMENT,
        getNotNothingString(messagesAr, "dpf_otherComment", errorInMethod));
    translatedMessages.put(
        Message.DPF_FINISH_PART4,
        getNotNothingString(messagesAr, "dpf_finishPart4", errorInMethod));
    translatedMessages.put(
        Message.DPF_CONGRATULATION,
        getNotNothingString(messagesAr, "dpf_congratulation", errorInMethod));

    translatedMessages.put(
        Message.DISTINCT_VALUES_TOOLTIP,
        getNotNothingString(messagesAr, "distinctValuesTooltip", errorInMethod));
    translatedMessages.put(
        Message.DO_WITH_GRAPHS, getNotNothingString(messagesAr, "doWithGraphs", errorInMethod));

    translatedMessages.put(
        Message.DT_ACCESSIBLE, getNotNothingString(messagesAr, "dtAccessible", errorInMethod));
    translatedMessages.put(
        Message.DT_ACCESSIBLE_PUBLIC,
        getNotNothingString(messagesAr, "dtAccessiblePublic", errorInMethod));
    translatedMessages.put(
        Message.DT_ACCESSIBLE_YES,
        getNotNothingString(messagesAr, "dtAccessibleYes", errorInMethod));
    translatedMessages.put(
        Message.DT_ACCESSIBLE_GRAPHS,
        getNotNothingString(messagesAr, "dtAccessibleGraphs", errorInMethod));
    translatedMessages.put(
        Message.DT_ACCESSIBLE_NO, getNotNothingString(messagesAr, "dtAccessibleNo", errorInMethod));
    translatedMessages.put(
        Message.DT_ACCESSIBLE_LOG_IN,
        getNotNothingString(messagesAr, "dtAccessibleLogIn", errorInMethod));
    translatedMessages.put(
        Message.DT_LOG_IN, getNotNothingString(messagesAr, "dtLogIn", errorInMethod));
    translatedMessages.put(Message.DT_DAF, getNotNothingString(messagesAr, "dtDAF", errorInMethod));
    translatedMessages.put(
        Message.DT_FILES, getNotNothingString(messagesAr, "dtFiles", errorInMethod));
    translatedMessages.put(Message.DT_MAG, getNotNothingString(messagesAr, "dtMAG", errorInMethod));
    translatedMessages.put(Message.DT_SOS, getNotNothingString(messagesAr, "dtSOS", errorInMethod));
    translatedMessages.put(
        Message.DT_SUBSET, getNotNothingString(messagesAr, "dtSubset", errorInMethod));
    translatedMessages.put(Message.DT_WCS, getNotNothingString(messagesAr, "dtWCS", errorInMethod));
    translatedMessages.put(Message.DT_WMS, getNotNothingString(messagesAr, "dtWMS", errorInMethod));

    translatedMessages.put(
        Message.EASIER_ACCESS_TO_SCIENTIFIC_DATA,
        getNotNothingString(messagesAr, "EasierAccessToScientificData", errorInMethod));
    translatedMessages.put(
        Message.EDD_DATASET_ID, getNotNothingString(messagesAr, "EDDDatasetID", errorInMethod));
    EDDFgdc = messagesAr[0].getNotNothingString("EDDFgdc", errorInMethod);
    translatedMessages.put(
        Message.EDD_FGDC_METADATA,
        getNotNothingString(messagesAr, "EDDFgdcMetadata", errorInMethod));
    translatedMessages.put(
        Message.EDD_FILES, getNotNothingString(messagesAr, "EDDFiles", errorInMethod));
    EDDIso19115 = messagesAr[0].getNotNothingString("EDDIso19115", errorInMethod);
    translatedMessages.put(
        Message.EDD_ISO19115_METADATA,
        getNotNothingString(messagesAr, "EDDIso19115Metadata", errorInMethod));
    translatedMessages.put(
        Message.EDD_METADATA, getNotNothingString(messagesAr, "EDDMetadata", errorInMethod));
    translatedMessages.put(
        Message.EDD_BACKGROUND, getNotNothingString(messagesAr, "EDDBackground", errorInMethod));
    translatedMessages.put(
        Message.EDD_INFORMATION, getNotNothingString(messagesAr, "EDDInformation", errorInMethod));
    translatedMessages.put(
        Message.EDD_INSTITUTION, getNotNothingString(messagesAr, "EDDInstitution", errorInMethod));
    translatedMessages.put(
        Message.EDD_SUMMARY, getNotNothingString(messagesAr, "EDDSummary", errorInMethod));
    translatedMessages.put(
        Message.EDD_DATASET_TITLE,
        getNotNothingString(messagesAr, "EDDDatasetTitle", errorInMethod));
    translatedMessages.put(
        Message.EDD_MAKE_A_GRAPH, getNotNothingString(messagesAr, "EDDMakeAGraph", errorInMethod));
    translatedMessages.put(
        Message.EDD_FILE_TYPE, getNotNothingString(messagesAr, "EDDFileType", errorInMethod));
    translatedMessages.put(
        Message.EDD_FILE_TYPE_INFORMATION,
        getNotNothingString(messagesAr, "EDDFileTypeInformation", errorInMethod));
    translatedMessages.put(
        Message.EDD_SELECT_FILE_TYPE,
        getNotNothingString(messagesAr, "EDDSelectFileType", errorInMethod));
    translatedMessages.put(
        Message.EDD_MINIMUM, getNotNothingString(messagesAr, "EDDMinimum", errorInMethod));
    translatedMessages.put(
        Message.EDD_MAXIMUM, getNotNothingString(messagesAr, "EDDMaximum", errorInMethod));
    translatedMessages.put(
        Message.EDD_CONSTRAINT, getNotNothingString(messagesAr, "EDDConstraint", errorInMethod));

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

    translatedMessages.put(
        Message.EDD_GRID_DOWNLOAD_TOOLTIP,
        getNotNothingString(messagesAr, "EDDGridDownloadTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_DAP_DESCRIPTION,
        getNotNothingString(messagesAr, "EDDGridDapDescription", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_DAP_LONG_DESCRIPTION,
        getNotNothingString(messagesAr, "EDDGridDapLongDescription", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_DOWNLOAD_DATA_TOOLTIP,
        getNotNothingString(messagesAr, "EDDGridDownloadDataTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_DIMENSION,
        getNotNothingString(messagesAr, "EDDGridDimension", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_DIMENSION_RANGES,
        getNotNothingString(messagesAr, "EDDGridDimensionRanges", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_START, getNotNothingString(messagesAr, "EDDGridStart", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_STOP, getNotNothingString(messagesAr, "EDDGridStop", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_START_STOP_TOOLTIP,
        getNotNothingString(messagesAr, "EDDGridStartStopTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_STRIDE, getNotNothingString(messagesAr, "EDDGridStride", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_N_VALUES,
        getNotNothingString(messagesAr, "EDDGridNValues", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_N_VALUES_HTML,
        getNotNothingString(messagesAr, "EDDGridNValuesHtml", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_SPACING, getNotNothingString(messagesAr, "EDDGridSpacing", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_JUST_ONE_VALUE,
        getNotNothingString(messagesAr, "EDDGridJustOneValue", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_EVEN, getNotNothingString(messagesAr, "EDDGridEven", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_UNEVEN, getNotNothingString(messagesAr, "EDDGridUneven", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_DIMENSION_TOOLTIP,
        getNotNothingString(messagesAr, "EDDGridDimensionTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_VAR_HAS_DIM_TOOLTIP,
        getNotNothingString(messagesAr, "EDDGridVarHasDimTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_SSS_TOOLTIP,
        getNotNothingString(messagesAr, "EDDGridSSSTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_START_TOOLTIP,
        getNotNothingString(messagesAr, "EDDGridStartTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_STOP_TOOLTIP,
        getNotNothingString(messagesAr, "EDDGridStopTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_STRIDE_TOOLTIP,
        getNotNothingString(messagesAr, "EDDGridStrideTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_SPACING_TOOLTIP,
        getNotNothingString(messagesAr, "EDDGridSpacingTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_GRID_VARIABLE_HTML,
        getNotNothingString(messagesAr, "EDDGridGridVariableHtml", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_CHECK_ALL,
        getNotNothingString(messagesAr, "EDDGridCheckAll", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_CHECK_ALL_TOOLTIP,
        getNotNothingString(messagesAr, "EDDGridCheckAllTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_UNCHECK_ALL,
        getNotNothingString(messagesAr, "EDDGridUncheckAll", errorInMethod));
    translatedMessages.put(
        Message.EDD_GRID_UNCHECK_ALL_TOOLTIP,
        getNotNothingString(messagesAr, "EDDGridUncheckAllTooltip", errorInMethod));

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

    translatedMessages.put(
        Message.EDD_TABLE_CONSTRAINTS,
        getNotNothingString(messagesAr, "EDDTableConstraints", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_DAP_DESCRIPTION,
        getNotNothingString(messagesAr, "EDDTableDapDescription", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_DAP_LONG_DESCRIPTION,
        getNotNothingString(messagesAr, "EDDTableDapLongDescription", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_DOWNLOAD_DATA_TOOLTIP,
        getNotNothingString(messagesAr, "EDDTableDownloadDataTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_TABULAR_DATASET_TOOLTIP,
        getNotNothingString(messagesAr, "EDDTableTabularDatasetTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_VARIABLE,
        getNotNothingString(messagesAr, "EDDTableVariable", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_CHECK_ALL,
        getNotNothingString(messagesAr, "EDDTableCheckAll", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_CHECK_ALL_TOOLTIP,
        getNotNothingString(messagesAr, "EDDTableCheckAllTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_UNCHECK_ALL,
        getNotNothingString(messagesAr, "EDDTableUncheckAll", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_UNCHECK_ALL_TOOLTIP,
        getNotNothingString(messagesAr, "EDDTableUncheckAllTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_MINIMUM_TOOLTIP,
        getNotNothingString(messagesAr, "EDDTableMinimumTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_MAXIMUM_TOOLTIP,
        getNotNothingString(messagesAr, "EDDTableMaximumTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_CHECK_THE_VARIABLES,
        getNotNothingString(messagesAr, "EDDTableCheckTheVariables", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_SELECT_AN_OPERATOR,
        getNotNothingString(messagesAr, "EDDTableSelectAnOperator", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_FROM_EDD_GRID_SUMMARY,
        getNotNothingString(messagesAr, "EDDTableFromEDDGridSummary", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_OPT_CONSTRAINT1_HTML,
        getNotNothingString(messagesAr, "EDDTableOptConstraint1Html", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_OPT_CONSTRAINT2_HTML,
        getNotNothingString(messagesAr, "EDDTableOptConstraint2Html", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_OPT_CONSTRAINT_VAR,
        getNotNothingString(messagesAr, "EDDTableOptConstraintVar", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_NUMERIC_CONSTRAINT_TOOLTIP,
        getNotNothingString(messagesAr, "EDDTableNumericConstraintTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_STRING_CONSTRAINT_TOOLTIP,
        getNotNothingString(messagesAr, "EDDTableStringConstraintTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_TIME_CONSTRAINT_TOOLTIP,
        getNotNothingString(messagesAr, "EDDTableTimeConstraintTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_CONSTRAINT_TOOLTIP,
        getNotNothingString(messagesAr, "EDDTableConstraintTooltip", errorInMethod));
    translatedMessages.put(
        Message.EDD_TABLE_SELECT_CONSTRAINT_TOOLTIP,
        getNotNothingString(messagesAr, "EDDTableSelectConstraintTooltip", errorInMethod));

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

    translatedMessages.put(
        Message.ERROR_TITLE, getNotNothingString(messagesAr, "errorTitle", errorInMethod));
    translatedMessages.put(
        Message.ERDDAP_IS, getNotNothingString(messagesAr, "erddapIs", errorInMethod));
    translatedMessages.put(
        Message.ERDDAP_VERSION_HTML,
        getNotNothingString(messagesAr, "erddapVersionHTML", errorInMethod));
    translatedMessages.put(
        Message.ERROR_FILE_NOT_FOUND,
        getNotNothingString(messagesAr, "errorFileNotFound", errorInMethod));
    translatedMessages.put(
        Message.ERROR_FILE_NOT_FOUND_IMAGE,
        getNotNothingString(messagesAr, "errorFileNotFoundImage", errorInMethod));
    translatedMessages.put(
        Message.ERROR_INTERNAL, getNotNothingString(messagesAr, "errorInternal", errorInMethod));
    for (int tl = 0; tl < nLanguages; tl++)
      translatedMessages.get(Message.ERROR_INTERNAL)[tl] += " ";

    translatedMessages.put(
        Message.ERROR_JSONP_FUNCTION_NAME,
        getNotNothingString(messagesAr, "errorJsonpFunctionName", errorInMethod));
    translatedMessages.put(
        Message.ERROR_JSONP_NOT_ALLOWED,
        getNotNothingString(messagesAr, "errorJsonpNotAllowed", errorInMethod));
    translatedMessages.put(
        Message.ERROR_MORE_THAN_2GB,
        getNotNothingString(messagesAr, "errorMoreThan2GB", errorInMethod));
    translatedMessages.put(
        Message.ERROR_NOT_FOUND, getNotNothingString(messagesAr, "errorNotFound", errorInMethod));
    translatedMessages.put(
        Message.ERROR_NOT_FOUND_IN,
        getNotNothingString(messagesAr, "errorNotFoundIn", errorInMethod));
    translatedMessages.put(
        Message.ERROR_ODV_LLT_GRID,
        getNotNothingString(messagesAr, "errorOdvLLTGrid", errorInMethod));
    translatedMessages.put(
        Message.ERROR_ODV_LLT_TABLE,
        getNotNothingString(messagesAr, "errorOdvLLTTable", errorInMethod));
    translatedMessages.put(
        Message.ERROR_ON_WEB_PAGE,
        getNotNothingString(messagesAr, "errorOnWebPage", errorInMethod));

    extensionsNoRangeRequests =
        StringArray.arrayFromCSV(
            messagesAr[0].getNotNothingString("extensionsNoRangeRequests", errorInMethod),
            ",",
            true,
            false); // trim, keepNothing

    translatedMessages.put(
        Message.EXTERNAL_LINK, getNotNothingString(messagesAr, "externalLink", errorInMethod));
    for (int tl = 0; tl < nLanguages; tl++)
      translatedMessages.get(Message.EXTERNAL_LINK)[tl] =
          " " + translatedMessages.get(Message.EXTERNAL_LINK)[tl];

    translatedMessages.put(
        Message.EXTERNAL_WEB_SITE,
        getNotNothingString(messagesAr, "externalWebSite", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_ASC, getNotNothingString(messagesAr, "fileHelp_asc", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_CROISSANT,
        getNotNothingString(messagesAr, "fileHelp_croissant", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_CSV, getNotNothingString(messagesAr, "fileHelp_csv", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_CSVP, getNotNothingString(messagesAr, "fileHelp_csvp", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_CSV0, getNotNothingString(messagesAr, "fileHelp_csv0", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_DATA_TABLE,
        getNotNothingString(messagesAr, "fileHelp_dataTable", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_DAS, getNotNothingString(messagesAr, "fileHelp_das", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_DDS, getNotNothingString(messagesAr, "fileHelp_dds", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_DODS, getNotNothingString(messagesAr, "fileHelp_dods", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_GRID_ESRI_ASCII,
        getNotNothingString(messagesAr, "fileHelpGrid_esriAscii", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_TABLE_ESRI_CSV,
        getNotNothingString(messagesAr, "fileHelpTable_esriCsv", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_FGDC, getNotNothingString(messagesAr, "fileHelp_fgdc", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_GEO_JSON,
        getNotNothingString(messagesAr, "fileHelp_geoJson", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_HTML, getNotNothingString(messagesAr, "fileHelp_html", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_HTML_TABLE,
        getNotNothingString(messagesAr, "fileHelp_htmlTable", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_ISO19115,
        getNotNothingString(messagesAr, "fileHelp_iso19115", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_ITX_GRID,
        getNotNothingString(messagesAr, "fileHelp_itxGrid", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_ITX_TABLE,
        getNotNothingString(messagesAr, "fileHelp_itxTable", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_JSON, getNotNothingString(messagesAr, "fileHelp_json", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_JSONL_CSV1,
        getNotNothingString(messagesAr, "fileHelp_jsonlCSV1", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_JSONL_CSV,
        getNotNothingString(messagesAr, "fileHelp_jsonlCSV", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_JSONL_KVP,
        getNotNothingString(messagesAr, "fileHelp_jsonlKVP", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_MAT, getNotNothingString(messagesAr, "fileHelp_mat", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_GRID_NC3,
        getNotNothingString(messagesAr, "fileHelpGrid_nc3", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_GRID_NC4,
        getNotNothingString(messagesAr, "fileHelpGrid_nc4", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_TABLE_NC3,
        getNotNothingString(messagesAr, "fileHelpTable_nc3", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_TABLE_NC4,
        getNotNothingString(messagesAr, "fileHelpTable_nc4", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_NC3_HEADER,
        getNotNothingString(messagesAr, "fileHelp_nc3Header", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_NC4_HEADER,
        getNotNothingString(messagesAr, "fileHelp_nc4Header", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_NCCSV, getNotNothingString(messagesAr, "fileHelp_nccsv", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_NCCSV_METADATA,
        getNotNothingString(messagesAr, "fileHelp_nccsvMetadata", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_NC_CF, getNotNothingString(messagesAr, "fileHelp_ncCF", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_NC_CF_HEADER,
        getNotNothingString(messagesAr, "fileHelp_ncCFHeader", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_NC_CFMA,
        getNotNothingString(messagesAr, "fileHelp_ncCFMA", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_NC_CFMA_HEADER,
        getNotNothingString(messagesAr, "fileHelp_ncCFMAHeader", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_NCML, getNotNothingString(messagesAr, "fileHelp_ncml", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_NCO_JSON,
        getNotNothingString(messagesAr, "fileHelp_ncoJson", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_GRID_ODV_TXT,
        getNotNothingString(messagesAr, "fileHelpGrid_odvTxt", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_TABLE_ODV_TXT,
        getNotNothingString(messagesAr, "fileHelpTable_odvTxt", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_PARQUET,
        getNotNothingString(messagesAr, "fileHelp_parquet", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_PARQUET_WITH_META,
        getNotNothingString(messagesAr, "fileHelp_parquet_with_meta", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_TIME_GAPS,
        getNotNothingString(messagesAr, "fileHelp_timeGaps", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_TSV, getNotNothingString(messagesAr, "fileHelp_tsv", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_TSVP, getNotNothingString(messagesAr, "fileHelp_tsvp", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_TSV0, getNotNothingString(messagesAr, "fileHelp_tsv0", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_WAV, getNotNothingString(messagesAr, "fileHelp_wav", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_XHTML, getNotNothingString(messagesAr, "fileHelp_xhtml", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_GEOTIF,
        getNotNothingString(messagesAr, "fileHelp_geotif", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_GRID_KML,
        getNotNothingString(messagesAr, "fileHelpGrid_kml", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_TABLE_KML,
        getNotNothingString(messagesAr, "fileHelpTable_kml", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_SMALL_PDF,
        getNotNothingString(messagesAr, "fileHelp_smallPdf", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_PDF, getNotNothingString(messagesAr, "fileHelp_pdf", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_LARGE_PDF,
        getNotNothingString(messagesAr, "fileHelp_largePdf", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_SMALL_PNG,
        getNotNothingString(messagesAr, "fileHelp_smallPng", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_PNG, getNotNothingString(messagesAr, "fileHelp_png", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_LARGE_PNG,
        getNotNothingString(messagesAr, "fileHelp_largePng", errorInMethod));
    translatedMessages.put(
        Message.FILE_HELP_TRANSPARENT_PNG,
        getNotNothingString(messagesAr, "fileHelp_transparentPng", errorInMethod));
    translatedMessages.put(
        Message.FILES_DESCRIPTION,
        getNotNothingString(messagesAr, "filesDescription", errorInMethod));
    translatedMessages.put(
        Message.FILES_DOCUMENTATION,
        getNotNothingString(messagesAr, "filesDocumentation", errorInMethod));
    translatedMessages.put(
        Message.FILES_WARNING, getNotNothingString(messagesAr, "filesWarning", errorInMethod));
    translatedMessages.put(
        Message.FIPS_COUNTY_CODES,
        getNotNothingString(messagesAr, "FIPSCountyCodes", errorInMethod));
    translatedMessages.put(
        Message.FOR_SOS_USE, getNotNothingString(messagesAr, "forSOSUse", errorInMethod));
    translatedMessages.put(
        Message.FOR_WCS_USE, getNotNothingString(messagesAr, "forWCSUse", errorInMethod));
    translatedMessages.put(
        Message.FOR_WMS_USE, getNotNothingString(messagesAr, "forWMSUse", errorInMethod));
    translatedMessages.put(
        Message.FUNCTIONS, getNotNothingString(messagesAr, "functions", errorInMethod));
    translatedMessages.put(
        Message.FUNCTION_TOOLTIP,
        getNotNothingString(messagesAr, "functionTooltip", errorInMethod));
    for (int tl = 0; tl < nLanguages; tl++) {
      translatedMessages.get(Message.FUNCTION_TOOLTIP)[tl] =
          MessageFormat.format(translatedMessages.get(Message.FUNCTION_TOOLTIP)[tl], "distinct()");
    }

    translatedMessages.put(
        Message.FUNCTION_DISTINCT_CHECK,
        getNotNothingString(messagesAr, "functionDistinctCheck", errorInMethod));
    translatedMessages.put(
        Message.FUNCTION_DISTINCT_TOOLTIP,
        getNotNothingString(messagesAr, "functionDistinctTooltip", errorInMethod));
    for (int tl = 0; tl < nLanguages; tl++) {
      translatedMessages.get(Message.FUNCTION_DISTINCT_TOOLTIP)[tl] =
          MessageFormat.format(
              translatedMessages.get(Message.FUNCTION_DISTINCT_TOOLTIP)[tl], "distinct()");
    }

    translatedMessages.put(
        Message.FUNCTION_ORDER_BY_TOOLTIP,
        getNotNothingString(messagesAr, "functionOrderByTooltip", errorInMethod));
    translatedMessages.put(
        Message.FUNCTION_ORDER_BY_SORT,
        getNotNothingString(messagesAr, "functionOrderBySort", errorInMethod));
    translatedMessages.put(
        Message.FUNCTION_ORDER_BY_SORT1,
        getNotNothingString(messagesAr, "functionOrderBySort1", errorInMethod));
    translatedMessages.put(
        Message.FUNCTION_ORDER_BY_SORT2,
        getNotNothingString(messagesAr, "functionOrderBySort2", errorInMethod));
    translatedMessages.put(
        Message.FUNCTION_ORDER_BY_SORT3,
        getNotNothingString(messagesAr, "functionOrderBySort3", errorInMethod));
    translatedMessages.put(
        Message.FUNCTION_ORDER_BY_SORT4,
        getNotNothingString(messagesAr, "functionOrderBySort4", errorInMethod));
    translatedMessages.put(
        Message.FUNCTION_ORDER_BY_SORT_LEAST,
        getNotNothingString(messagesAr, "functionOrderBySortLeast", errorInMethod));
    translatedMessages.put(
        Message.GENERATED_AT, getNotNothingString(messagesAr, "generatedAt", errorInMethod));
    translatedMessages.put(
        Message.GEO_SERVICES_DESCRIPTION,
        getNotNothingString(messagesAr, "geoServicesDescription", errorInMethod));
    translatedMessages.put(
        Message.GET_STARTED_HTML, getNotNothingString(messagesAr, "getStartedHtml", errorInMethod));
    translatedMessages.put(Message.HELP, getNotNothingString(messagesAr, "help", errorInMethod));
    translatedMessages.put(
        Message.HTML_TABLE_MAX_MESSAGE,
        getNotNothingString(messagesAr, "htmlTableMaxMessage", errorInMethod));

    translatedMessages.put(
        Message.IMAGE_DATA_COURTESY_OF,
        getNotNothingString(messagesAr, "imageDataCourtesyOf", errorInMethod));
    imageWidths =
        String2.toIntArray(
            String2.split(messagesAr[0].getNotNothingString("imageWidths", errorInMethod), ','));
    imageHeights =
        String2.toIntArray(
            String2.split(messagesAr[0].getNotNothingString("imageHeights", errorInMethod), ','));
    translatedMessages.put(
        Message.IMAGES_EMBED, getNotNothingString(messagesAr, "imagesEmbed", errorInMethod));
    translatedMessages.put(
        Message.INDEX_VIEW_ALL, getNotNothingString(messagesAr, "indexViewAll", errorInMethod));
    translatedMessages.put(
        Message.INDEX_SEARCH_WITH,
        getNotNothingString(messagesAr, "indexSearchWith", errorInMethod));
    translatedMessages.put(
        Message.INDEX_DEVELOPERS_SEARCH,
        getNotNothingString(messagesAr, "indexDevelopersSearch", errorInMethod));
    translatedMessages.put(
        Message.INDEX_PROTOCOL, getNotNothingString(messagesAr, "indexProtocol", errorInMethod));
    translatedMessages.put(
        Message.INDEX_DESCRIPTION,
        getNotNothingString(messagesAr, "indexDescription", errorInMethod));
    translatedMessages.put(
        Message.INDEX_DATASETS, getNotNothingString(messagesAr, "indexDatasets", errorInMethod));
    translatedMessages.put(
        Message.INDEX_DOCUMENTATION,
        getNotNothingString(messagesAr, "indexDocumentation", errorInMethod));
    translatedMessages.put(
        Message.INDEX_RESTFUL_SEARCH,
        getNotNothingString(messagesAr, "indexRESTfulSearch", errorInMethod));
    translatedMessages.put(
        Message.INDEX_ALL_DATASETS_SEARCH,
        getNotNothingString(messagesAr, "indexAllDatasetsSearch", errorInMethod));
    translatedMessages.put(
        Message.INDEX_OPEN_SEARCH,
        getNotNothingString(messagesAr, "indexOpenSearch", errorInMethod));
    translatedMessages.put(
        Message.INDEX_SERVICES, getNotNothingString(messagesAr, "indexServices", errorInMethod));
    translatedMessages.put(
        Message.INDEX_DESCRIBE_SERVICES,
        getNotNothingString(messagesAr, "indexDescribeServices", errorInMethod));
    translatedMessages.put(
        Message.INDEX_METADATA, getNotNothingString(messagesAr, "indexMetadata", errorInMethod));
    translatedMessages.put(
        Message.INDEX_WAF1, getNotNothingString(messagesAr, "indexWAF1", errorInMethod));
    translatedMessages.put(
        Message.INDEX_WAF2, getNotNothingString(messagesAr, "indexWAF2", errorInMethod));
    translatedMessages.put(
        Message.INDEX_CONVERTERS,
        getNotNothingString(messagesAr, "indexConverters", errorInMethod));
    translatedMessages.put(
        Message.INDEX_DESCRIBE_CONVERTERS,
        getNotNothingString(messagesAr, "indexDescribeConverters", errorInMethod));
    translatedMessages.put(
        Message.INFO_ABOUT_FROM, getNotNothingString(messagesAr, "infoAboutFrom", errorInMethod));
    translatedMessages.put(
        Message.INFO_TABLE_TITLE_HTML,
        getNotNothingString(messagesAr, "infoTableTitleHtml", errorInMethod));
    translatedMessages.put(
        Message.INFO_REQUEST_FORM,
        getNotNothingString(messagesAr, "infoRequestForm", errorInMethod));
    translatedMessages.put(
        Message.INFORMATION, getNotNothingString(messagesAr, "information", errorInMethod));
    translatedMessages.put(
        Message.INOTIFY_FIX, getNotNothingString(messagesAr, "inotifyFix", errorInMethod));
    inotifyFixCommands = messagesAr[0].getNotNothingString("inotifyFixCommands", errorInMethod);
    for (int tl = 0; tl < nLanguages; tl++) {
      translatedMessages.get(Message.INOTIFY_FIX)[tl] =
          MessageFormat.format(translatedMessages.get(Message.INOTIFY_FIX)[tl], inotifyFixCommands);
    }
    translatedMessages.put(
        Message.INTERPOLATE, getNotNothingString(messagesAr, "interpolate", errorInMethod));
    translatedMessages.put(
        Message.JAVA_PROGRAMS_HTML,
        getNotNothingString(messagesAr, "javaProgramsHTML", errorInMethod));
    translatedMessages.put(
        Message.JUST_GENERATE_AND_VIEW,
        getNotNothingString(messagesAr, "justGenerateAndView", errorInMethod));
    translatedMessages.put(
        Message.JUST_GENERATE_AND_VIEW_TOOLTIP,
        getNotNothingString(messagesAr, "justGenerateAndViewTooltip", errorInMethod));
    translatedMessages.put(
        Message.JUST_GENERATE_AND_VIEW_URL,
        getNotNothingString(messagesAr, "justGenerateAndViewUrl", errorInMethod));
    translatedMessages.put(
        Message.JUST_GENERATE_AND_VIEW_GRAPH_URL_TOOLTIP,
        getNotNothingString(messagesAr, "justGenerateAndViewGraphUrlTooltip", errorInMethod));
    translatedMessages.put(
        Message.KEYWORDS, getNotNothingString(messagesAr, "keywords", errorInMethod));
    translatedMessages.put(
        Message.LANG_CODE, getNotNothingString(messagesAr, "langCode", errorInMethod));

    legal = messagesAr[0].getNotNothingString("legal", errorInMethod);
    legal = getSetupEVString(setup, ev, "legal", legal); // optionally in setup.xml
    translatedMessages.put(
        Message.LEGAL_NOTICES, getNotNothingString(messagesAr, "legalNotices", errorInMethod));
    translatedMessages.put(
        Message.LEGAL_NOTICES_TITLE,
        getNotNothingString(messagesAr, "legalNoticesTitle", errorInMethod));

    legendTitle1 = messagesAr[0].getString("legendTitle1", "");
    legendTitle2 = messagesAr[0].getString("legendTitle2", "");
    legendTitle1 =
        getSetupEVString(setup, ev, "legendTitle1", legendTitle1); // optionally in setup.xml
    legendTitle2 =
        getSetupEVString(setup, ev, "legendTitle2", legendTitle2); // optionally in setup.xml

    translatedMessages.put(
        Message.LICENSE, getNotNothingString(messagesAr, "license", errorInMethod));
    translatedMessages.put(
        Message.LIKE_THIS, getNotNothingString(messagesAr, "likeThis", errorInMethod));
    translatedMessages.put(
        Message.LIST_ALL, getNotNothingString(messagesAr, "listAll", errorInMethod));
    translatedMessages.put(
        Message.LIST_OF_DATASETS, getNotNothingString(messagesAr, "listOfDatasets", errorInMethod));

    translatedMessages.put(Message.LOGIN, getNotNothingString(messagesAr, "login", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_HTML, getNotNothingString(messagesAr, "loginHTML", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_ATTEMPT_BLOCKED,
        getNotNothingString(messagesAr, "loginAttemptBlocked", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_DESCRIBE_CUSTOM,
        getNotNothingString(messagesAr, "loginDescribeCustom", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_DESCRIBE_EMAIL,
        getNotNothingString(messagesAr, "loginDescribeEmail", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_DESCRIBE_GOOGLE,
        getNotNothingString(messagesAr, "loginDescribeGoogle", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_DESCRIBE_ORCID,
        getNotNothingString(messagesAr, "loginDescribeOrcid", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_DESCRIBE_OAUTH2,
        getNotNothingString(messagesAr, "loginDescribeOauth2", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_CAN_NOT, getNotNothingString(messagesAr, "loginCanNot", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_ARE_NOT, getNotNothingString(messagesAr, "loginAreNot", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_TO_LOG_IN, getNotNothingString(messagesAr, "loginToLogIn", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_YOUR_EMAIL_ADDRESS,
        getNotNothingString(messagesAr, "loginYourEmailAddress", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_USER_NAME, getNotNothingString(messagesAr, "loginUserName", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_PASSWORD, getNotNothingString(messagesAr, "loginPassword", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_USER_NAME_AND_PASSWORD,
        getNotNothingString(messagesAr, "loginUserNameAndPassword", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_GOOGLE_SIGN_IN,
        getNotNothingString(messagesAr, "loginGoogleSignIn", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_ORCID_SIGN_IN,
        getNotNothingString(messagesAr, "loginOrcidSignIn", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_AS, getNotNothingString(messagesAr, "loginAs", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_PARTWAY_AS, getNotNothingString(messagesAr, "loginPartwayAs", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_FAILED, getNotNothingString(messagesAr, "loginFailed", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_SUCCEEDED, getNotNothingString(messagesAr, "loginSucceeded", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_INVALID, getNotNothingString(messagesAr, "loginInvalid", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_NOT, getNotNothingString(messagesAr, "loginNot", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_BACK, getNotNothingString(messagesAr, "loginBack", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_PROBLEM_EXACT,
        getNotNothingString(messagesAr, "loginProblemExact", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_PROBLEM_EXPIRE,
        getNotNothingString(messagesAr, "loginProblemExpire", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_PROBLEM_GOOGLE_AGAIN,
        getNotNothingString(messagesAr, "loginProblemGoogleAgain", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_PROBLEM_ORCID_AGAIN,
        getNotNothingString(messagesAr, "loginProblemOrcidAgain", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_PROBLEM_OAUTH2_AGAIN,
        getNotNothingString(messagesAr, "loginProblemOauth2Again", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_PROBLEM_SAME_BROWSER,
        getNotNothingString(messagesAr, "loginProblemSameBrowser", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_PROBLEM_3_TIMES,
        getNotNothingString(messagesAr, "loginProblem3Times", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_PROBLEMS, getNotNothingString(messagesAr, "loginProblems", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_PROBLEMS_AFTER,
        getNotNothingString(messagesAr, "loginProblemsAfter", errorInMethod));
    translatedMessages.put(
        Message.LOGIN_PUBLIC_ACCESS,
        getNotNothingString(messagesAr, "loginPublicAccess", errorInMethod));
    translatedMessages.put(
        Message.LOG_OUT, getNotNothingString(messagesAr, "LogOut", errorInMethod));
    translatedMessages.put(
        Message.LOGOUT, getNotNothingString(messagesAr, "logout", errorInMethod));
    translatedMessages.put(
        Message.LOGOUT_SUCCESS, getNotNothingString(messagesAr, "logoutSuccess", errorInMethod));
    translatedMessages.put(Message.MAG, getNotNothingString(messagesAr, "mag", errorInMethod));
    translatedMessages.put(
        Message.MAG_AXIS_X, getNotNothingString(messagesAr, "magAxisX", errorInMethod));
    translatedMessages.put(
        Message.MAG_AXIS_Y, getNotNothingString(messagesAr, "magAxisY", errorInMethod));
    translatedMessages.put(
        Message.MAG_AXIS_COLOR, getNotNothingString(messagesAr, "magAxisColor", errorInMethod));
    translatedMessages.put(
        Message.MAG_AXIS_STICK_X, getNotNothingString(messagesAr, "magAxisStickX", errorInMethod));
    translatedMessages.put(
        Message.MAG_AXIS_STICK_Y, getNotNothingString(messagesAr, "magAxisStickY", errorInMethod));
    translatedMessages.put(
        Message.MAG_AXIS_VECTOR_X,
        getNotNothingString(messagesAr, "magAxisVectorX", errorInMethod));
    translatedMessages.put(
        Message.MAG_AXIS_VECTOR_Y,
        getNotNothingString(messagesAr, "magAxisVectorY", errorInMethod));
    translatedMessages.put(
        Message.MAG_AXIS_HELP_GRAPH_X,
        getNotNothingString(messagesAr, "magAxisHelpGraphX", errorInMethod));
    translatedMessages.put(
        Message.MAG_AXIS_HELP_GRAPH_Y,
        getNotNothingString(messagesAr, "magAxisHelpGraphY", errorInMethod));
    translatedMessages.put(
        Message.MAG_AXIS_HELP_MARKER_COLOR,
        getNotNothingString(messagesAr, "magAxisHelpMarkerColor", errorInMethod));
    translatedMessages.put(
        Message.MAG_AXIS_HELP_SURFACE_COLOR,
        getNotNothingString(messagesAr, "magAxisHelpSurfaceColor", errorInMethod));
    translatedMessages.put(
        Message.MAG_AXIS_HELP_STICK_X,
        getNotNothingString(messagesAr, "magAxisHelpStickX", errorInMethod));
    translatedMessages.put(
        Message.MAG_AXIS_HELP_STICK_Y,
        getNotNothingString(messagesAr, "magAxisHelpStickY", errorInMethod));
    translatedMessages.put(
        Message.MAG_AXIS_HELP_MAP_X,
        getNotNothingString(messagesAr, "magAxisHelpMapX", errorInMethod));
    translatedMessages.put(
        Message.MAG_AXIS_HELP_MAP_Y,
        getNotNothingString(messagesAr, "magAxisHelpMapY", errorInMethod));
    translatedMessages.put(
        Message.MAG_AXIS_HELP_VECTOR_X,
        getNotNothingString(messagesAr, "magAxisHelpVectorX", errorInMethod));
    translatedMessages.put(
        Message.MAG_AXIS_HELP_VECTOR_Y,
        getNotNothingString(messagesAr, "magAxisHelpVectorY", errorInMethod));
    translatedMessages.put(
        Message.MAG_AXIS_VAR_HELP,
        getNotNothingString(messagesAr, "magAxisVarHelp", errorInMethod));
    translatedMessages.put(
        Message.MAG_AXIS_VAR_HELP_GRID,
        getNotNothingString(messagesAr, "magAxisVarHelpGrid", errorInMethod));
    translatedMessages.put(
        Message.MAG_CONSTRAINT_HELP,
        getNotNothingString(messagesAr, "magConstraintHelp", errorInMethod));
    translatedMessages.put(
        Message.MAG_DOCUMENTATION,
        getNotNothingString(messagesAr, "magDocumentation", errorInMethod));
    translatedMessages.put(
        Message.MAG_DOWNLOAD, getNotNothingString(messagesAr, "magDownload", errorInMethod));
    translatedMessages.put(
        Message.MAG_DOWNLOAD_TOOLTIP,
        getNotNothingString(messagesAr, "magDownloadTooltip", errorInMethod));
    translatedMessages.put(
        Message.MAG_FILE_TYPE, getNotNothingString(messagesAr, "magFileType", errorInMethod));
    translatedMessages.put(
        Message.MAG_GRAPH_TYPE, getNotNothingString(messagesAr, "magGraphType", errorInMethod));
    translatedMessages.put(
        Message.MAG_GRAPH_TYPE_TOOLTIP_GRID,
        getNotNothingString(messagesAr, "magGraphTypeTooltipGrid", errorInMethod));
    translatedMessages.put(
        Message.MAG_GRAPH_TYPE_TOOLTIP_TABLE,
        getNotNothingString(messagesAr, "magGraphTypeTooltipTable", errorInMethod));
    translatedMessages.put(Message.MAG_GS, getNotNothingString(messagesAr, "magGS", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_MARKER_TYPE,
        getNotNothingString(messagesAr, "magGSMarkerType", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_SIZE, getNotNothingString(messagesAr, "magGSSize", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_COLOR, getNotNothingString(messagesAr, "magGSColor", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_COLOR_BAR, getNotNothingString(messagesAr, "magGSColorBar", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_COLOR_BAR_TOOLTIP,
        getNotNothingString(messagesAr, "magGSColorBarTooltip", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_CONTINUITY,
        getNotNothingString(messagesAr, "magGSContinuity", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_CONTINUITY_TOOLTIP,
        getNotNothingString(messagesAr, "magGSContinuityTooltip", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_SCALE, getNotNothingString(messagesAr, "magGSScale", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_SCALE_TOOLTIP,
        getNotNothingString(messagesAr, "magGSScaleTooltip", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_MIN, getNotNothingString(messagesAr, "magGSMin", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_MIN_TOOLTIP,
        getNotNothingString(messagesAr, "magGSMinTooltip", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_MAX, getNotNothingString(messagesAr, "magGSMax", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_MAX_TOOLTIP,
        getNotNothingString(messagesAr, "magGSMaxTooltip", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_N_SECTIONS,
        getNotNothingString(messagesAr, "magGSNSections", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_N_SECTIONS_TOOLTIP,
        getNotNothingString(messagesAr, "magGSNSectionsTooltip", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_LAND_MASK, getNotNothingString(messagesAr, "magGSLandMask", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_LAND_MASK_TOOLTIP_GRID,
        getNotNothingString(messagesAr, "magGSLandMaskTooltipGrid", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_LAND_MASK_TOOLTIP_TABLE,
        getNotNothingString(messagesAr, "magGSLandMaskTooltipTable", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_VECTOR_STANDARD,
        getNotNothingString(messagesAr, "magGSVectorStandard", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_VECTOR_STANDARD_TOOLTIP,
        getNotNothingString(messagesAr, "magGSVectorStandardTooltip", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_Y_ASCENDING_TOOLTIP,
        getNotNothingString(messagesAr, "magGSYAscendingTooltip", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_Y_AXIS_MIN, getNotNothingString(messagesAr, "magGSYAxisMin", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_Y_AXIS_MAX, getNotNothingString(messagesAr, "magGSYAxisMax", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_Y_RANGE_MIN_TOOLTIP,
        getNotNothingString(messagesAr, "magGSYRangeMinTooltip", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_Y_RANGE_MAX_TOOLTIP,
        getNotNothingString(messagesAr, "magGSYRangeMaxTooltip", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_Y_RANGE_TOOLTIP,
        getNotNothingString(messagesAr, "magGSYRangeTooltip", errorInMethod));
    translatedMessages.put(
        Message.MAG_GS_Y_SCALE_TOOLTIP,
        getNotNothingString(messagesAr, "magGSYScaleTooltip", errorInMethod));
    translatedMessages.put(
        Message.MAG_ITEM_FIRST, getNotNothingString(messagesAr, "magItemFirst", errorInMethod));
    translatedMessages.put(
        Message.MAG_ITEM_PREVIOUS,
        getNotNothingString(messagesAr, "magItemPrevious", errorInMethod));
    translatedMessages.put(
        Message.MAG_ITEM_NEXT, getNotNothingString(messagesAr, "magItemNext", errorInMethod));
    translatedMessages.put(
        Message.MAG_ITEM_LAST, getNotNothingString(messagesAr, "magItemLast", errorInMethod));
    translatedMessages.put(
        Message.MAG_JUST_1_VALUE, getNotNothingString(messagesAr, "magJust1Value", errorInMethod));
    translatedMessages.put(
        Message.MAG_RANGE, getNotNothingString(messagesAr, "magRange", errorInMethod));
    translatedMessages.put(
        Message.MAG_RANGE_TO, getNotNothingString(messagesAr, "magRangeTo", errorInMethod));
    translatedMessages.put(
        Message.MAG_REDRAW, getNotNothingString(messagesAr, "magRedraw", errorInMethod));
    translatedMessages.put(
        Message.MAG_REDRAW_TOOLTIP,
        getNotNothingString(messagesAr, "magRedrawTooltip", errorInMethod));
    translatedMessages.put(
        Message.MAG_TIME_RANGE, getNotNothingString(messagesAr, "magTimeRange", errorInMethod));
    translatedMessages.put(
        Message.MAG_TIME_RANGE_FIRST,
        getNotNothingString(messagesAr, "magTimeRangeFirst", errorInMethod));
    translatedMessages.put(
        Message.MAG_TIME_RANGE_BACK,
        getNotNothingString(messagesAr, "magTimeRangeBack", errorInMethod));
    translatedMessages.put(
        Message.MAG_TIME_RANGE_FORWARD,
        getNotNothingString(messagesAr, "magTimeRangeForward", errorInMethod));
    translatedMessages.put(
        Message.MAG_TIME_RANGE_LAST,
        getNotNothingString(messagesAr, "magTimeRangeLast", errorInMethod));
    translatedMessages.put(
        Message.MAG_TIME_RANGE_TOOLTIP,
        getNotNothingString(messagesAr, "magTimeRangeTooltip", errorInMethod));
    translatedMessages.put(
        Message.MAG_TIME_RANGE_TOOLTIP2,
        getNotNothingString(messagesAr, "magTimeRangeTooltip2", errorInMethod));
    translatedMessages.put(
        Message.MAG_TIMES_VARY, getNotNothingString(messagesAr, "magTimesVary", errorInMethod));
    translatedMessages.put(
        Message.MAG_VIEW_URL, getNotNothingString(messagesAr, "magViewUrl", errorInMethod));
    translatedMessages.put(
        Message.MAG_ZOOM, getNotNothingString(messagesAr, "magZoom", errorInMethod));
    translatedMessages.put(
        Message.MAG_ZOOM_CENTER, getNotNothingString(messagesAr, "magZoomCenter", errorInMethod));
    translatedMessages.put(
        Message.MAG_ZOOM_CENTER_TOOLTIP,
        getNotNothingString(messagesAr, "magZoomCenterTooltip", errorInMethod));
    translatedMessages.put(
        Message.MAG_ZOOM_IN, getNotNothingString(messagesAr, "magZoomIn", errorInMethod));
    translatedMessages.put(
        Message.MAG_ZOOM_IN_TOOLTIP,
        getNotNothingString(messagesAr, "magZoomInTooltip", errorInMethod));
    translatedMessages.put(
        Message.MAG_ZOOM_OUT, getNotNothingString(messagesAr, "magZoomOut", errorInMethod));
    translatedMessages.put(
        Message.MAG_ZOOM_OUT_TOOLTIP,
        getNotNothingString(messagesAr, "magZoomOutTooltip", errorInMethod));
    translatedMessages.put(
        Message.MAG_ZOOM_A_LITTLE,
        getNotNothingString(messagesAr, "magZoomALittle", errorInMethod));
    translatedMessages.put(
        Message.MAG_ZOOM_DATA, getNotNothingString(messagesAr, "magZoomData", errorInMethod));
    translatedMessages.put(
        Message.MAG_ZOOM_OUT_DATA,
        getNotNothingString(messagesAr, "magZoomOutData", errorInMethod));
    translatedMessages.put(
        Message.MAG_GRID_TOOLTIP, getNotNothingString(messagesAr, "magGridTooltip", errorInMethod));
    translatedMessages.put(
        Message.MAG_TABLE_TOOLTIP,
        getNotNothingString(messagesAr, "magTableTooltip", errorInMethod));

    translatedMessages.put(
        Message.METADATA_DOWNLOAD,
        getNotNothingString(messagesAr, "metadataDownload", errorInMethod));
    translatedMessages.put(
        Message.MORE_INFORMATION,
        getNotNothingString(messagesAr, "moreInformation", errorInMethod));

    translatedMessages.put(
        Message.N_MATCHING_1, getNotNothingString(messagesAr, "nMatching1", errorInMethod));
    translatedMessages.put(
        Message.N_MATCHING, getNotNothingString(messagesAr, "nMatching", errorInMethod));
    translatedMessages.put(
        Message.N_MATCHING_ALPHABETICAL,
        getNotNothingString(messagesAr, "nMatchingAlphabetical", errorInMethod));
    translatedMessages.put(
        Message.N_MATCHING_MOST_RELEVANT,
        getNotNothingString(messagesAr, "nMatchingMostRelevant", errorInMethod));
    translatedMessages.put(
        Message.N_MATCHING_PAGE, getNotNothingString(messagesAr, "nMatchingPage", errorInMethod));
    translatedMessages.put(
        Message.N_MATCHING_CURRENT,
        getNotNothingString(messagesAr, "nMatchingCurrent", errorInMethod));
    translatedMessages.put(
        Message.NO_DATA_FIXED_VALUE,
        getNotNothingString(messagesAr, "noDataFixedValue", errorInMethod));
    translatedMessages.put(
        Message.NO_DATA_NO_LL, getNotNothingString(messagesAr, "noDataNoLL", errorInMethod));
    translatedMessages.put(
        Message.NO_DATASET_WITH, getNotNothingString(messagesAr, "noDatasetWith", errorInMethod));
    translatedMessages.put(
        Message.NO_PAGE_1, getNotNothingString(messagesAr, "noPage1", errorInMethod));
    translatedMessages.put(
        Message.NO_PAGE_2, getNotNothingString(messagesAr, "noPage2", errorInMethod));
    translatedMessages.put(
        Message.NOT_ALLOWED, getNotNothingString(messagesAr, "notAllowed", errorInMethod));
    translatedMessages.put(
        Message.NOT_AUTHORIZED, getNotNothingString(messagesAr, "notAuthorized", errorInMethod));
    translatedMessages.put(
        Message.NOT_AUTHORIZED_FOR_DATA,
        getNotNothingString(messagesAr, "notAuthorizedForData", errorInMethod));
    translatedMessages.put(
        Message.NOT_AVAILABLE, getNotNothingString(messagesAr, "notAvailable", errorInMethod));
    translatedMessages.put(Message.NOTE, getNotNothingString(messagesAr, "note", errorInMethod));
    translatedMessages.put(Message.NO_XXX, getNotNothingString(messagesAr, "noXxx", errorInMethod));
    translatedMessages.put(
        Message.NO_XXX_BECAUSE, getNotNothingString(messagesAr, "noXxxBecause", errorInMethod));
    translatedMessages.put(
        Message.NO_XXX_BECAUSE_2, getNotNothingString(messagesAr, "noXxxBecause2", errorInMethod));
    translatedMessages.put(
        Message.NO_XXX_NOT_ACTIVE,
        getNotNothingString(messagesAr, "noXxxNotActive", errorInMethod));
    translatedMessages.put(
        Message.NO_XXX_NO_AXIS_1, getNotNothingString(messagesAr, "noXxxNoAxis1", errorInMethod));
    translatedMessages.put(
        Message.NO_XXX_NO_CDM_DATA_TYPE,
        getNotNothingString(messagesAr, "noXxxNoCdmDataType", errorInMethod));
    translatedMessages.put(
        Message.NO_XXX_NO_COLOR_BAR,
        getNotNothingString(messagesAr, "noXxxNoColorBar", errorInMethod));
    translatedMessages.put(
        Message.NO_XXX_NO_LL, getNotNothingString(messagesAr, "noXxxNoLL", errorInMethod));
    translatedMessages.put(
        Message.NO_XXX_NO_LL_EVENLY_SPACED,
        getNotNothingString(messagesAr, "noXxxNoLLEvenlySpaced", errorInMethod));
    translatedMessages.put(
        Message.NO_XXX_NO_LL_GT_1, getNotNothingString(messagesAr, "noXxxNoLLGt1", errorInMethod));
    translatedMessages.put(
        Message.NO_XXX_NO_LLT, getNotNothingString(messagesAr, "noXxxNoLLT", errorInMethod));
    translatedMessages.put(
        Message.NO_XXX_NO_LON_IN_180,
        getNotNothingString(messagesAr, "noXxxNoLonIn180", errorInMethod));
    translatedMessages.put(
        Message.NO_XXX_NO_NON_STRING,
        getNotNothingString(messagesAr, "noXxxNoNonString", errorInMethod));
    translatedMessages.put(
        Message.NO_XXX_NO_2_NON_STRING,
        getNotNothingString(messagesAr, "noXxxNo2NonString", errorInMethod));
    translatedMessages.put(
        Message.NO_XXX_NO_STATION_ID,
        getNotNothingString(messagesAr, "noXxxNoStationID", errorInMethod));
    translatedMessages.put(
        Message.NO_XXX_NO_SUBSET_VARIABLES,
        getNotNothingString(messagesAr, "noXxxNoSubsetVariables", errorInMethod));
    translatedMessages.put(
        Message.NO_XXX_NO_OLL_SUBSET_VARIABLES,
        getNotNothingString(messagesAr, "noXxxNoOLLSubsetVariables", errorInMethod));
    translatedMessages.put(
        Message.NO_XXX_ITS_GRIDDED,
        getNotNothingString(messagesAr, "noXxxItsGridded", errorInMethod));
    translatedMessages.put(
        Message.NO_XXX_ITS_TABULAR,
        getNotNothingString(messagesAr, "noXxxItsTabular", errorInMethod));
    translatedMessages.put(
        Message.ONE_REQUEST_AT_A_TIME,
        getNotNothingString(messagesAr, "oneRequestAtATime", errorInMethod));
    translatedMessages.put(
        Message.OPEN_SEARCH_DESCRIPTION,
        getNotNothingString(messagesAr, "openSearchDescription", errorInMethod));
    translatedMessages.put(
        Message.OPTIONAL, getNotNothingString(messagesAr, "optional", errorInMethod));
    translatedMessages.put(
        Message.OPTIONS, getNotNothingString(messagesAr, "options", errorInMethod));
    translatedMessages.put(
        Message.OR_A_LIST_OF_VALUES,
        getNotNothingString(messagesAr, "orAListOfValues", errorInMethod));
    translatedMessages.put(
        Message.OR_REFINE_SEARCH_WITH,
        getNotNothingString(messagesAr, "orRefineSearchWith", errorInMethod));
    translatedMessages.put(
        Message.OR_COMMA, getNotNothingString(messagesAr, "orComma", errorInMethod));
    for (int tl = 0; tl < nLanguages; tl++) {
      translatedMessages.get(Message.OR_REFINE_SEARCH_WITH)[tl] += " ";
      translatedMessages.get(Message.OR_COMMA)[tl] += " ";
    }
    translatedMessages.put(
        Message.OTHER_FEATURES, getNotNothingString(messagesAr, "otherFeatures", errorInMethod));
    translatedMessages.put(
        Message.OUT_OF_DATE_DATASETS,
        getNotNothingString(messagesAr, "outOfDateDatasets", errorInMethod));
    translatedMessages.put(
        Message.OUT_OF_DATE_HTML, getNotNothingString(messagesAr, "outOfDateHtml", errorInMethod));
    translatedMessages.put(
        Message.OUT_OF_DATE_KEEP_TRACK,
        getNotNothingString(messagesAr, "outOfDateKeepTrack", errorInMethod));

    // just one set of palettes info (from messagesAr[0])
    palettes = String2.split(messagesAr[0].getNotNothingString("palettes", errorInMethod), ',');
    DEFAULT_palettes = palettes; // used by LoadDatasets if palettes tag is empty
    DEFAULT_palettes_set = String2.stringArrayToSet(palettes);
    palettes0 = new String[palettes.length + 1];
    palettes0[0] = "";
    System.arraycopy(palettes, 0, palettes0, 1, palettes.length);

    translatedMessages.put(
        Message.PATIENT_DATA, getNotNothingString(messagesAr, "patientData", errorInMethod));
    translatedMessages.put(
        Message.PATIENT_YOUR_GRAPH,
        getNotNothingString(messagesAr, "patientYourGraph", errorInMethod));

    pdfWidths =
        String2.toIntArray(
            String2.split(messagesAr[0].getNotNothingString("pdfWidths", errorInMethod), ','));
    pdfHeights =
        String2.toIntArray(
            String2.split(messagesAr[0].getNotNothingString("pdfHeights", errorInMethod), ','));

    translatedMessages.put(
        Message.PERCENT_ENCODE, getNotNothingString(messagesAr, "percentEncode", errorInMethod));
    translatedMessages.put(
        Message.PROTOCOL_SEARCH_HTML,
        getNotNothingString(messagesAr, "protocolSearchHtml", errorInMethod));
    translatedMessages.put(
        Message.PROTOCOL_SEARCH_2_HTML,
        getNotNothingString(messagesAr, "protocolSearch2Html", errorInMethod));
    translatedMessages.put(
        Message.PROTOCOL_CLICK, getNotNothingString(messagesAr, "protocolClick", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR, getNotNothingString(messagesAr, "queryError", errorInMethod));
    String[] queryErrorArray = translatedMessages.get(Message.QUERY_ERROR);
    for (int tl = 0; tl < nLanguages; tl++) {
      queryErrorArray[tl] += " ";
    }
    translatedMessages.put(
        Message.QUERY_ERROR_180, getNotNothingString(messagesAr, "queryError180", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_1_VALUE,
        getNotNothingString(messagesAr, "queryError1Value", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_1_VAR,
        getNotNothingString(messagesAr, "queryError1Var", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_2_VAR,
        getNotNothingString(messagesAr, "queryError2Var", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_ACTUAL_RANGE,
        getNotNothingString(messagesAr, "queryErrorActualRange", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_ADJUSTED,
        getNotNothingString(messagesAr, "queryErrorAdjusted", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_CONSTRAINT_NAN,
        getNotNothingString(messagesAr, "queryErrorConstraintNaN", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_EQUAL_SPACING,
        getNotNothingString(messagesAr, "queryErrorEqualSpacing", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_EXPECTED_AT,
        getNotNothingString(messagesAr, "queryErrorExpectedAt", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_FILE_TYPE,
        getNotNothingString(messagesAr, "queryErrorFileType", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_INVALID,
        getNotNothingString(messagesAr, "queryErrorInvalid", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_LL, getNotNothingString(messagesAr, "queryErrorLL", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_LL_GT_1,
        getNotNothingString(messagesAr, "queryErrorLLGt1", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_LLT, getNotNothingString(messagesAr, "queryErrorLLT", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_NEVER_TRUE,
        getNotNothingString(messagesAr, "queryErrorNeverTrue", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_NEVER_BOTH_TRUE,
        getNotNothingString(messagesAr, "queryErrorNeverBothTrue", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_NOT_AXIS,
        getNotNothingString(messagesAr, "queryErrorNotAxis", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_NOT_EXPECTED_AT,
        getNotNothingString(messagesAr, "queryErrorNotExpectedAt", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_NOT_FOUND_AFTER,
        getNotNothingString(messagesAr, "queryErrorNotFoundAfter", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_OCCURS_TWICE,
        getNotNothingString(messagesAr, "queryErrorOccursTwice", errorInMethod));

    translatedMessages.put(
        Message.QUERY_ERROR_ORDER_BY_CLOSEST,
        getNotNothingString(messagesAr, "queryErrorOrderByClosest", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_ORDER_BY_LIMIT,
        getNotNothingString(messagesAr, "queryErrorOrderByLimit", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_ORDER_BY_MEAN,
        getNotNothingString(messagesAr, "queryErrorOrderByMean", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_ORDER_BY_SUM,
        getNotNothingString(messagesAr, "queryErrorOrderBySum", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_ORDER_BY_VARIABLE,
        getNotNothingString(messagesAr, "queryErrorOrderByVariable", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_UNKNOWN_VARIABLE,
        getNotNothingString(messagesAr, "queryErrorUnknownVariable", errorInMethod));

    translatedMessages.put(
        Message.QUERY_ERROR_GRID_1_AXIS,
        getNotNothingString(messagesAr, "queryErrorGrid1Axis", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_GRID_AMP,
        getNotNothingString(messagesAr, "queryErrorGridAmp", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_GRID_DIAGNOSTIC,
        getNotNothingString(messagesAr, "queryErrorGridDiagnostic", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_GRID_BETWEEN,
        getNotNothingString(messagesAr, "queryErrorGridBetween", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_GRID_LESS_MIN,
        getNotNothingString(messagesAr, "queryErrorGridLessMin", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_GRID_GREATER_MAX,
        getNotNothingString(messagesAr, "queryErrorGridGreaterMax", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_GRID_MISSING,
        getNotNothingString(messagesAr, "queryErrorGridMissing", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_GRID_NO_AXIS_VAR,
        getNotNothingString(messagesAr, "queryErrorGridNoAxisVar", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_GRID_NO_DATA_VAR,
        getNotNothingString(messagesAr, "queryErrorGridNoDataVar", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_GRID_NOT_IDENTICAL,
        getNotNothingString(messagesAr, "queryErrorGridNotIdentical", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_LAST_END_P,
        getNotNothingString(messagesAr, "queryErrorLastEndP", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_LAST_EXPECTED,
        getNotNothingString(messagesAr, "queryErrorLastExpected", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_LAST_UNEXPECTED,
        getNotNothingString(messagesAr, "queryErrorLastUnexpected", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_LAST_PM_INVALID,
        getNotNothingString(messagesAr, "queryErrorLastPMInvalid", errorInMethod));
    translatedMessages.put(
        Message.QUERY_ERROR_LAST_PM_INTEGER,
        getNotNothingString(messagesAr, "queryErrorLastPMInteger", errorInMethod));

    questionMarkImageFile =
        messagesAr[0].getNotNothingString("questionMarkImageFile", errorInMethod);
    questionMarkImageFile =
        getSetupEVString(setup, ev, "questionMarkImageFile", questionMarkImageFile); // optional

    translatedMessages.put(
        Message.RANGES_FROM_TO, getNotNothingString(messagesAr, "rangesFromTo", errorInMethod));
    translatedMessages.put(
        Message.REQUIRED, getNotNothingString(messagesAr, "required", errorInMethod));
    translatedMessages.put(
        Message.REQUEST_FORMAT_EXAMPLES_HTML,
        getNotNothingString(messagesAr, "requestFormatExamplesHtml", errorInMethod));
    translatedMessages.put(
        Message.RESET_THE_FORM, getNotNothingString(messagesAr, "resetTheForm", errorInMethod));
    translatedMessages.put(
        Message.RESET_THE_FORM_WAS,
        getNotNothingString(messagesAr, "resetTheFormWas", errorInMethod));
    translatedMessages.put(
        Message.RESOURCE_NOT_FOUND,
        getNotNothingString(messagesAr, "resourceNotFound", errorInMethod));
    String[] resourceNotFoundArray = translatedMessages.get(Message.RESOURCE_NOT_FOUND);
    for (int tl = 0; tl < nLanguages; tl++) {
      resourceNotFoundArray[tl] += " ";
    }
    translatedMessages.put(
        Message.RESTFUL_WEB_SERVICES,
        getNotNothingString(messagesAr, "restfulWebServices", errorInMethod));
    translatedMessages.put(
        Message.RESTFUL_HTML, getNotNothingString(messagesAr, "restfulHTML", errorInMethod));
    translatedMessages.put(
        Message.RESTFUL_HTML_CONTINUED,
        getNotNothingString(messagesAr, "restfulHTMLContinued", errorInMethod));
    translatedMessages.put(
        Message.RESTFUL_GET_ALL_DATASET,
        getNotNothingString(messagesAr, "restfulGetAllDataset", errorInMethod));
    translatedMessages.put(
        Message.RESTFUL_PROTOCOLS,
        getNotNothingString(messagesAr, "restfulProtocols", errorInMethod));
    translatedMessages.put(
        Message.SOS_DOCUMENTATION,
        getNotNothingString(messagesAr, "SOSDocumentation", errorInMethod));
    translatedMessages.put(
        Message.WCS_DOCUMENTATION,
        getNotNothingString(messagesAr, "WCSDocumentation", errorInMethod));
    translatedMessages.put(
        Message.WMS_DOCUMENTATION,
        getNotNothingString(messagesAr, "WMSDocumentation", errorInMethod));
    translatedMessages.put(
        Message.RESULTS_FORMAT_EXAMPLES_HTML,
        getNotNothingString(messagesAr, "resultsFormatExamplesHtml", errorInMethod));
    translatedMessages.put(
        Message.RESULTS_OF_SEARCH_FOR,
        getNotNothingString(messagesAr, "resultsOfSearchFor", errorInMethod));
    translatedMessages.put(
        Message.RESTFUL_INFORMATION_FORMATS,
        getNotNothingString(messagesAr, "restfulInformationFormats", errorInMethod));
    translatedMessages.put(
        Message.RESTFUL_VIA_SERVICE,
        getNotNothingString(messagesAr, "restfulViaService", errorInMethod));
    translatedMessages.put(Message.ROWS, getNotNothingString(messagesAr, "rows", errorInMethod));
    translatedMessages.put(Message.RSS_NO, getNotNothingString(messagesAr, "rssNo", errorInMethod));

    translatedMessages.put(
        Message.SEARCH_TITLE, getNotNothingString(messagesAr, "searchTitle", errorInMethod));
    translatedMessages.put(
        Message.SEARCH_DO_FULL_TEXT_HTML,
        getNotNothingString(messagesAr, "searchDoFullTextHtml", errorInMethod));
    translatedMessages.put(
        Message.SEARCH_FULL_TEXT_HTML,
        getNotNothingString(messagesAr, "searchFullTextHtml", errorInMethod));
    translatedMessages.put(
        Message.SEARCH_BUTTON, getNotNothingString(messagesAr, "searchButton", errorInMethod));
    translatedMessages.put(
        Message.SEARCH_CLICK_TIP, getNotNothingString(messagesAr, "searchClickTip", errorInMethod));
    translatedMessages.put(
        Message.SEARCH_HINTS_LUCENE_TOOLTIP,
        getNotNothingString(messagesAr, "searchHintsLuceneTooltip", errorInMethod));
    translatedMessages.put(
        Message.SEARCH_HINTS_ORIGINAL_TOOLTIP,
        getNotNothingString(messagesAr, "searchHintsOriginalTooltip", errorInMethod));
    translatedMessages.put(
        Message.SEARCH_HINTS_TOOLTIP,
        getNotNothingString(messagesAr, "searchHintsTooltip", errorInMethod));
    translatedMessages.put(
        Message.SEARCH_MULTIPLE_ERDDAPS,
        getNotNothingString(messagesAr, "searchMultipleERDDAPs", errorInMethod));
    translatedMessages.put(
        Message.SEARCH_MULTIPLE_ERDDAPS_DESCRIPTION,
        getNotNothingString(messagesAr, "searchMultipleERDDAPsDescription", errorInMethod));
    translatedMessages.put(
        Message.SEARCH_NOT_AVAILABLE,
        getNotNothingString(messagesAr, "searchNotAvailable", errorInMethod));
    translatedMessages.put(
        Message.SEARCH_TIP, getNotNothingString(messagesAr, "searchTip", errorInMethod));
    translatedMessages.put(
        Message.SEARCH_SPELLING, getNotNothingString(messagesAr, "searchSpelling", errorInMethod));
    translatedMessages.put(
        Message.SEARCH_FEWER_WORDS,
        getNotNothingString(messagesAr, "searchFewerWords", errorInMethod));
    translatedMessages.put(
        Message.SEARCH_WITH_QUERY,
        getNotNothingString(messagesAr, "searchWithQuery", errorInMethod));

    translatedMessages.put(
        Message.SELECT_NEXT, getNotNothingString(messagesAr, "selectNext", errorInMethod));
    translatedMessages.put(
        Message.SELECT_PREVIOUS, getNotNothingString(messagesAr, "selectPrevious", errorInMethod));
    translatedMessages.put(
        Message.SHIFT_X_ALL_THE_WAY_LEFT,
        getNotNothingString(messagesAr, "shiftXAllTheWayLeft", errorInMethod));
    translatedMessages.put(
        Message.SHIFT_X_LEFT, getNotNothingString(messagesAr, "shiftXLeft", errorInMethod));
    translatedMessages.put(
        Message.SHIFT_X_RIGHT, getNotNothingString(messagesAr, "shiftXRight", errorInMethod));
    translatedMessages.put(
        Message.SHIFT_X_ALL_THE_WAY_RIGHT,
        getNotNothingString(messagesAr, "shiftXAllTheWayRight", errorInMethod));

    translatedMessages.put(
        Message.SEE_PROTOCOL_DOCUMENTATION,
        getNotNothingString(messagesAr, "seeProtocolDocumentation", errorInMethod));

    translatedMessages.put(
        Message.SLIDE_SORTER, getNotNothingString(messagesAr, "slideSorter", errorInMethod));
    translatedMessages.put(Message.SOS, getNotNothingString(messagesAr, "SOS", errorInMethod));
    translatedMessages.put(
        Message.SOS_DESCRIPTION_HTML,
        getNotNothingString(messagesAr, "sosDescriptionHtml", errorInMethod));
    translatedMessages.put(
        Message.SOS_LONG_DESCRIPTION_HTML,
        getNotNothingString(messagesAr, "sosLongDescriptionHtml", errorInMethod));
    translatedMessages.put(
        Message.SOS_OVERVIEW_1, getNotNothingString(messagesAr, "sosOverview1", errorInMethod));
    translatedMessages.put(
        Message.SOS_OVERVIEW_2, getNotNothingString(messagesAr, "sosOverview2", errorInMethod));

    sparqlP01toP02pre = messagesAr[0].getNotNothingString("sparqlP01toP02pre", errorInMethod);
    sparqlP01toP02post = messagesAr[0].getNotNothingString("sparqlP01toP02post", errorInMethod);

    translatedMessages.put(Message.SS_USE, getNotNothingString(messagesAr, "ssUse", errorInMethod));
    translatedMessages.put(
        Message.SS_USE_PLAIN,
        getNotNothingString(messagesAr, "ssUse", errorInMethod)); // start with this
    for (int tl = 0; tl < nLanguages; tl++) {
      translatedMessages.get(Message.SS_USE_PLAIN)[tl] =
          XML.removeHTMLTags(translatedMessages.get(Message.SS_USE_PLAIN)[tl]);
    }

    translatedMessages.put(
        Message.SS_BE_PATIENT, getNotNothingString(messagesAr, "ssBePatient", errorInMethod));
    translatedMessages.put(
        Message.SS_INSTRUCTIONS_HTML,
        getNotNothingString(messagesAr, "ssInstructionsHtml", errorInMethod));

    translatedMessages.put(
        Message.STATUS, getNotNothingString(messagesAr, "status", errorInMethod));
    translatedMessages.put(
        Message.STATUS_HTML, getNotNothingString(messagesAr, "statusHtml", errorInMethod));
    translatedMessages.put(
        Message.SUBMIT, getNotNothingString(messagesAr, "submit", errorInMethod));
    translatedMessages.put(
        Message.SUBMIT_TOOLTIP, getNotNothingString(messagesAr, "submitTooltip", errorInMethod));

    translatedMessages.put(
        Message.SUBSCRIPTION_OFFER_RSS,
        getNotNothingString(messagesAr, "subscriptionOfferRss", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_OFFER_URL,
        getNotNothingString(messagesAr, "subscriptionOfferUrl", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTIONS_TITLE,
        getNotNothingString(messagesAr, "subscriptionsTitle", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_EMAIL_LIST,
        getNotNothingString(messagesAr, "subscriptionEmailList", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_ADD,
        getNotNothingString(messagesAr, "subscriptionAdd", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_VALIDATE,
        getNotNothingString(messagesAr, "subscriptionValidate", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_LIST,
        getNotNothingString(messagesAr, "subscriptionList", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_REMOVE,
        getNotNothingString(messagesAr, "subscriptionRemove", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_0_HTML,
        getNotNothingString(messagesAr, "subscription0Html", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_1_HTML,
        getNotNothingString(messagesAr, "subscription1Html", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_2_HTML,
        getNotNothingString(messagesAr, "subscription2Html", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_ABUSE,
        getNotNothingString(messagesAr, "subscriptionAbuse", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_ADD_ERROR,
        getNotNothingString(messagesAr, "subscriptionAddError", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_ADD_HTML,
        getNotNothingString(messagesAr, "subscriptionAddHtml", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_ADD_2,
        getNotNothingString(messagesAr, "subscriptionAdd2", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_ADD_SUCCESS,
        getNotNothingString(messagesAr, "subscriptionAddSuccess", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_EMAIL,
        getNotNothingString(messagesAr, "subscriptionEmail", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_EMAIL_ON_BLACKLIST,
        getNotNothingString(messagesAr, "subscriptionEmailOnBlacklist", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_EMAIL_INVALID,
        getNotNothingString(messagesAr, "subscriptionEmailInvalid", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_EMAIL_TOO_LONG,
        getNotNothingString(messagesAr, "subscriptionEmailTooLong", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_EMAIL_UNSPECIFIED,
        getNotNothingString(messagesAr, "subscriptionEmailUnspecified", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_ID_INVALID,
        getNotNothingString(messagesAr, "subscriptionIDInvalid", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_ID_TOO_LONG,
        getNotNothingString(messagesAr, "subscriptionIDTooLong", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_ID_UNSPECIFIED,
        getNotNothingString(messagesAr, "subscriptionIDUnspecified", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_KEY_INVALID,
        getNotNothingString(messagesAr, "subscriptionKeyInvalid", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_KEY_UNSPECIFIED,
        getNotNothingString(messagesAr, "subscriptionKeyUnspecified", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_LIST_ERROR,
        getNotNothingString(messagesAr, "subscriptionListError", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_LIST_HTML,
        getNotNothingString(messagesAr, "subscriptionListHtml", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_LIST_SUCCESS,
        getNotNothingString(messagesAr, "subscriptionListSuccess", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_REMOVE_ERROR,
        getNotNothingString(messagesAr, "subscriptionRemoveError", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_REMOVE_HTML,
        getNotNothingString(messagesAr, "subscriptionRemoveHtml", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_REMOVE_2,
        getNotNothingString(messagesAr, "subscriptionRemove2", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_REMOVE_SUCCESS,
        getNotNothingString(messagesAr, "subscriptionRemoveSuccess", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_RSS,
        getNotNothingString(messagesAr, "subscriptionRSS", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_URL_HTML,
        getNotNothingString(messagesAr, "subscriptionUrlHtml", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_URL_INVALID,
        getNotNothingString(messagesAr, "subscriptionUrlInvalid", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_URL_TOO_LONG,
        getNotNothingString(messagesAr, "subscriptionUrlTooLong", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_VALIDATE_ERROR,
        getNotNothingString(messagesAr, "subscriptionValidateError", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_VALIDATE_HTML,
        getNotNothingString(messagesAr, "subscriptionValidateHtml", errorInMethod));
    translatedMessages.put(
        Message.SUBSCRIPTION_VALIDATE_SUCCESS,
        getNotNothingString(messagesAr, "subscriptionValidateSuccess", errorInMethod));

    translatedMessages.put(
        Message.SUBSET, getNotNothingString(messagesAr, "subset", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_SELECT, getNotNothingString(messagesAr, "subsetSelect", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_N_MATCHING,
        getNotNothingString(messagesAr, "subsetNMatching", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_INSTRUCTIONS,
        getNotNothingString(messagesAr, "subsetInstructions", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_OPTION, getNotNothingString(messagesAr, "subsetOption", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_OPTIONS, getNotNothingString(messagesAr, "subsetOptions", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_REFINE_MAP_DOWNLOAD,
        getNotNothingString(messagesAr, "subsetRefineMapDownload", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_REFINE_SUBSET_DOWNLOAD,
        getNotNothingString(messagesAr, "subsetRefineSubsetDownload", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_CLICK_RESET_CLOSEST,
        getNotNothingString(messagesAr, "subsetClickResetClosest", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_CLICK_RESET_LL,
        getNotNothingString(messagesAr, "subsetClickResetLL", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_METADATA, getNotNothingString(messagesAr, "subsetMetadata", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_COUNT, getNotNothingString(messagesAr, "subsetCount", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_PERCENT, getNotNothingString(messagesAr, "subsetPercent", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_VIEW_SELECT,
        getNotNothingString(messagesAr, "subsetViewSelect", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_VIEW_SELECT_DISTINCT_COMBOS,
        getNotNothingString(messagesAr, "subsetViewSelectDistinctCombos", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_VIEW_SELECT_RELATED_COUNTS,
        getNotNothingString(messagesAr, "subsetViewSelectRelatedCounts", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_WHEN, getNotNothingString(messagesAr, "subsetWhen", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_WHEN_NO_CONSTRAINTS,
        getNotNothingString(messagesAr, "subsetWhenNoConstraints", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_WHEN_COUNTS,
        getNotNothingString(messagesAr, "subsetWhenCounts", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_N_VARIABLE_COMBOS,
        getNotNothingString(messagesAr, "subsetNVariableCombos", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_SHOWING_ALL_ROWS,
        getNotNothingString(messagesAr, "subsetShowingAllRows", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_SHOWING_N_ROWS,
        getNotNothingString(messagesAr, "subsetShowingNRows", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_CHANGE_SHOWING,
        getNotNothingString(messagesAr, "subsetChangeShowing", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_N_ROWS_RELATED_DATA,
        getNotNothingString(messagesAr, "subsetNRowsRelatedData", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_VIEW_RELATED_CHANGE,
        getNotNothingString(messagesAr, "subsetViewRelatedChange", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_TOTAL_COUNT,
        getNotNothingString(messagesAr, "subsetTotalCount", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_VIEW, getNotNothingString(messagesAr, "subsetView", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_VIEW_CHECK,
        getNotNothingString(messagesAr, "subsetViewCheck", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_VIEW_CHECK_1,
        getNotNothingString(messagesAr, "subsetViewCheck1", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_VIEW_DISTINCT_MAP,
        getNotNothingString(messagesAr, "subsetViewDistinctMap", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_VIEW_RELATED_MAP,
        getNotNothingString(messagesAr, "subsetViewRelatedMap", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_VIEW_DISTINCT_DATA_COUNTS,
        getNotNothingString(messagesAr, "subsetViewDistinctDataCounts", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_VIEW_DISTINCT_DATA,
        getNotNothingString(messagesAr, "subsetViewDistinctData", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_VIEW_RELATED_DATA_COUNTS,
        getNotNothingString(messagesAr, "subsetViewRelatedDataCounts", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_VIEW_RELATED_DATA,
        getNotNothingString(messagesAr, "subsetViewRelatedData", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_VIEW_DISTINCT_MAP_TOOLTIP,
        getNotNothingString(messagesAr, "subsetViewDistinctMapTooltip", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_VIEW_RELATED_MAP_TOOLTIP,
        getNotNothingString(messagesAr, "subsetViewRelatedMapTooltip", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_VIEW_DISTINCT_DATA_COUNTS_TOOLTIP,
        getNotNothingString(messagesAr, "subsetViewDistinctDataCountsTooltip", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_VIEW_DISTINCT_DATA_TOOLTIP,
        getNotNothingString(messagesAr, "subsetViewDistinctDataTooltip", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_VIEW_RELATED_DATA_COUNTS_TOOLTIP,
        getNotNothingString(messagesAr, "subsetViewRelatedDataCountsTooltip", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_VIEW_RELATED_DATA_TOOLTIP,
        getNotNothingString(messagesAr, "subsetViewRelatedDataTooltip", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_WARN, getNotNothingString(messagesAr, "subsetWarn", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_WARN_10000,
        getNotNothingString(messagesAr, "subsetWarn10000", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_TOOLTIP, getNotNothingString(messagesAr, "subsetTooltip", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_NOT_SET_UP,
        getNotNothingString(messagesAr, "subsetNotSetUp", errorInMethod));
    translatedMessages.put(
        Message.SUBSET_LONG_NOT_SHOWN,
        getNotNothingString(messagesAr, "subsetLongNotShown", errorInMethod));

    translatedMessages.put(
        Message.TABLEDAP_VIDEO_INTRO,
        getNotNothingString(messagesAr, "tabledapVideoIntro", errorInMethod));
    translatedMessages.put(
        Message.THE_DATASET_ID, getNotNothingString(messagesAr, "theDatasetID", errorInMethod));
    translatedMessages.put(
        Message.THE_KEY, getNotNothingString(messagesAr, "theKey", errorInMethod));
    translatedMessages.put(
        Message.THE_SUBSCRIPTION_ID,
        getNotNothingString(messagesAr, "theSubscriptionID", errorInMethod));
    translatedMessages.put(
        Message.THE_URL_ACTION, getNotNothingString(messagesAr, "theUrlAction", errorInMethod));

    translatedMessages.put(
        Message.THE_LONG_DESCRIPTION_HTML,
        getNotNothingString(messagesAr, "theLongDescriptionHtml", errorInMethod));
    translatedMessages.put(Message.TIME, getNotNothingString(messagesAr, "time", errorInMethod));
    translatedMessages.put(
        Message.THIS_PARTICULAR_ERDDAP,
        getNotNothingString(messagesAr, "thisParticularErddap", errorInMethod));
    translatedMessages.put(
        Message.TIMEOUT_OTHER_REQUESTS,
        getNotNothingString(messagesAr, "timeoutOtherRequests", errorInMethod));

    translatedMessages.put(Message.UNITS, getNotNothingString(messagesAr, "units", errorInMethod));
    translatedMessages.put(
        Message.UNKNOWN_DATASET_ID,
        getNotNothingString(messagesAr, "unknownDatasetID", errorInMethod));
    translatedMessages.put(
        Message.UNKNOWN_PROTOCOL,
        getNotNothingString(messagesAr, "unknownProtocol", errorInMethod));
    translatedMessages.put(
        Message.UNSUPPORTED_FILE_TYPE,
        getNotNothingString(messagesAr, "unsupportedFileType", errorInMethod));
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

    translatedMessages.put(
        Message.USING_GRIDDAP, getNotNothingString(messagesAr, "usingGriddap", errorInMethod));
    translatedMessages.put(
        Message.USING_TABLEDAP, getNotNothingString(messagesAr, "usingTabledap", errorInMethod));
    translatedMessages.put(
        Message.VARIABLE_NAMES, getNotNothingString(messagesAr, "variableNames", errorInMethod));
    translatedMessages.put(
        Message.WAIT_THEN_TRY_AGAIN,
        getNotNothingString(messagesAr, "waitThenTryAgain", errorInMethod));
    translatedMessages.put(
        Message.WARNING, getNotNothingString(messagesAr, "warning", errorInMethod));
    translatedMessages.put(Message.WCS, getNotNothingString(messagesAr, "WCS", errorInMethod));
    translatedMessages.put(
        Message.WCS_DESCRIPTION_HTML,
        getNotNothingString(messagesAr, "wcsDescriptionHtml", errorInMethod));
    translatedMessages.put(
        Message.WCS_LONG_DESCRIPTION_HTML,
        getNotNothingString(messagesAr, "wcsLongDescriptionHtml", errorInMethod));
    translatedMessages.put(
        Message.WCS_OVERVIEW_1, getNotNothingString(messagesAr, "wcsOverview1", errorInMethod));
    translatedMessages.put(
        Message.WCS_OVERVIEW_2, getNotNothingString(messagesAr, "wcsOverview2", errorInMethod));
    translatedMessages.put(
        Message.WMS_DESCRIPTION_HTML,
        getNotNothingString(messagesAr, "wmsDescriptionHtml", errorInMethod));
    translatedMessages.put(
        Message.WMS_INSTRUCTIONS,
        getNotNothingString(messagesAr, "wmsInstructions", errorInMethod));
    translatedMessages.put(
        Message.WMS_LONG_DESCRIPTION_HTML,
        getNotNothingString(messagesAr, "wmsLongDescriptionHtml", errorInMethod));
    translatedMessages.put(
        Message.WMS_MANY_DATASETS,
        getNotNothingString(messagesAr, "wmsManyDatasets", errorInMethod));
    translatedMessages.put(
        Message.WMS_DOCUMENTATION_1,
        getNotNothingString(messagesAr, "WMSDocumentation1", errorInMethod));
    translatedMessages.put(
        Message.WMS_GET_CAPABILITIES,
        getNotNothingString(messagesAr, "WMSGetCapabilities", errorInMethod));
    translatedMessages.put(
        Message.WMS_GET_MAP, getNotNothingString(messagesAr, "WMSGetMap", errorInMethod));
    for (int tl = 0; tl < nLanguages; tl++) {
      translatedMessages.get(Message.WMS_GET_CAPABILITIES)[tl] =
          translatedMessages
              .get(Message.WMS_GET_CAPABILITIES)[tl] // some things should stay in English
              .replaceAll("&serviceWMS;", "service=WMS")
              .replaceAll("&version;", "version")
              .replaceAll("&requestGetCapabilities;", "request=GetCapabilities");
      translatedMessages.get(Message.WMS_GET_MAP)[tl] =
          translatedMessages
              .get(Message.WMS_GET_MAP)[tl] // lots of things should stay in English
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

    translatedMessages.put(
        Message.WMS_NOTES, getNotNothingString(messagesAr, "WMSNotes", errorInMethod));

    for (int tl = 0; tl < nLanguages; tl++) {
      translatedMessages.get(Message.WMS_NOTES)[tl] =
          translatedMessages.get(Message.WMS_NOTES)[tl].replace(
              "&WMSSEPARATOR;", Character.toString(EDD.WMS_SEPARATOR));
    }

    translatedMessages.put(
        Message.YOUR_EMAIL_ADDRESS,
        getNotNothingString(messagesAr, "yourEmailAddress", errorInMethod));
    translatedMessages.put(
        Message.ZOOM_IN, getNotNothingString(messagesAr, "zoomIn", errorInMethod));
    translatedMessages.put(
        Message.ZOOM_OUT, getNotNothingString(messagesAr, "zoomOut", errorInMethod));

    lazyInitializeStatics(errorInMethod, messagesAr);

    for (int tl = 0; tl < nLanguages; tl++) {
      translatedMessages.get(Message.BLACKLIST_MSG)[tl] =
          MessageFormat.format(
              translatedMessages.get(Message.BLACKLIST_MSG)[tl], EDStatic.config.adminEmail);
    }

    translatedMessages.put(
        Message.STANDARD_SHORT_DESCRIPTION_HTML,
        getNotNothingString(messagesAr, "standardShortDescriptionHtml", errorInMethod));
    String[] standardShortDescriptionHtmlArray =
        translatedMessages.get(Message.STANDARD_SHORT_DESCRIPTION_HTML);
    for (int tl = 0; tl < nLanguages; tl++) {
      standardShortDescriptionHtmlArray[tl] =
          String2.replaceAll(
              standardShortDescriptionHtmlArray[tl],
              "&convertTimeReference;",
              EDStatic.config.convertersActive
                  ? translatedMessages.get(Message.CONVERT_TIME_REFERENCE)[tl]
                  : "");
      standardShortDescriptionHtmlArray[tl] =
          String2.replaceAll(
              standardShortDescriptionHtmlArray[tl],
              "&wmsManyDatasets;",
              EDStatic.config.wmsActive
                  ? translatedMessages.get(Message.WMS_MANY_DATASETS)[tl]
                  : "");
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
    translatedMessages.put(
        Message.DEFAULT_STANDARD_CONTACT,
        getNotNothingString(messagesAr, "standardContact", errorInMethod));
    translatedMessages.put(
        Message.STANDARD_CONTACT,
        getSetupEVString(
            setup,
            ev,
            "standardContact",
            translatedMessages.get(Message.DEFAULT_STANDARD_CONTACT)));
    translatedMessages.put(
        Message.DEFAULT_STANDARD_DATA_LICENSES,
        getNotNothingString(messagesAr, "standardDataLicenses", errorInMethod));
    translatedMessages.put(
        Message.STANDARD_DATA_LICENSES,
        getSetupEVString(
            setup,
            ev,
            "standardDataLicenses",
            translatedMessages.get(Message.DEFAULT_STANDARD_DATA_LICENSES)));
    translatedMessages.put(
        Message.DEFAULT_STANDARD_DISCLAIMER_OF_EXTERNAL_LINKS,
        getNotNothingString(messagesAr, "standardDisclaimerOfExternalLinks", errorInMethod));
    translatedMessages.put(
        Message.STANDARD_DISCLAIMER_OF_EXTERNAL_LINKS,
        getSetupEVString(
            setup,
            ev,
            "standardDisclaimerOfExternalLinks",
            translatedMessages.get(Message.DEFAULT_STANDARD_DISCLAIMER_OF_EXTERNAL_LINKS)));
    translatedMessages.put(
        Message.DEFAULT_STANDARD_DISCLAIMER_OF_ENDORSEMENT,
        getNotNothingString(messagesAr, "standardDisclaimerOfEndorsement", errorInMethod));
    translatedMessages.put(
        Message.STANDARD_DISCLAIMER_OF_ENDORSEMENT,
        getSetupEVString(
            setup,
            ev,
            "standardDisclaimerOfEndorsement",
            translatedMessages.get(Message.DEFAULT_STANDARD_DISCLAIMER_OF_ENDORSEMENT)));
    translatedMessages.put(
        Message.DEFAULT_STANDARD_GENERAL_DISCLAIMER,
        getNotNothingString(messagesAr, "standardGeneralDisclaimer", errorInMethod));
    translatedMessages.put(
        Message.STANDARD_GENERAL_DISCLAIMER,
        getSetupEVString(
            setup,
            ev,
            "standardGeneralDisclaimer",
            translatedMessages.get(Message.DEFAULT_STANDARD_GENERAL_DISCLAIMER)));
    translatedMessages.put(
        Message.DEFAULT_STANDARD_PRIVACY_POLICY,
        getNotNothingString(messagesAr, "standardPrivacyPolicy", errorInMethod));
    translatedMessages.put(
        Message.STANDARD_PRIVACY_POLICY,
        getSetupEVString(
            setup,
            ev,
            "standardPrivacyPolicy",
            translatedMessages.get(Message.DEFAULT_STANDARD_PRIVACY_POLICY)));

    DEFAULT_startHeadHtml = messagesAr[0].getNotNothingString("startHeadHtml5", errorInMethod);
    startHeadHtml = getSetupEVString(setup, ev, "startHeadHtml5", DEFAULT_startHeadHtml);
    translatedMessages.put(
        Message.DEFAULT_START_BODY_HTML,
        getNotNothingString(messagesAr, "startBodyHtml5", errorInMethod));
    translatedMessages.put(
        Message.START_BODY_HTML,
        getSetupEVString(
            setup, ev, "startBodyHtml5", translatedMessages.get(Message.DEFAULT_START_BODY_HTML)));

    translatedMessages.put(
        Message.DEFAULT_THE_SHORT_DESCRIPTION_HTML,
        getNotNothingString(messagesAr, "theShortDescriptionHtml", errorInMethod));
    translatedMessages.put(
        Message.THE_SHORT_DESCRIPTION_HTML,
        getSetupEVString(
            setup,
            ev,
            "theShortDescriptionHtml",
            translatedMessages.get(Message.DEFAULT_THE_SHORT_DESCRIPTION_HTML)));
    translatedMessages.put(
        Message.DEFAULT_END_BODY_HTML,
        getNotNothingString(messagesAr, "endBodyHtml5", errorInMethod));
    translatedMessages.put(
        Message.END_BODY_HTML,
        getSetupEVString(
            setup, ev, "endBodyHtml5", translatedMessages.get(Message.DEFAULT_END_BODY_HTML)));

    // ensure HTML5
    Test.ensureTrue(
        startHeadHtml.startsWith("<!DOCTYPE html>"),
        "<startHeadHtml5> must start with \"<!DOCTYPE html>\".");
    for (int tl = 0; tl < nLanguages; tl++) {
      translatedMessages.get(Message.DEFAULT_STANDARD_DATA_LICENSES)[tl] =
          String2.replaceAll(
              translatedMessages.get(Message.DEFAULT_STANDARD_DATA_LICENSES)[tl],
              "&license;",
              "<kbd>license</kbd>"); // so not translated
      translatedMessages.get(Message.STANDARD_DATA_LICENSES)[tl] =
          String2.replaceAll(
              translatedMessages.get(Message.STANDARD_DATA_LICENSES)[tl],
              "&license;",
              "<kbd>license</kbd>");
      translatedMessages.get(Message.STANDARD_CONTACT)[tl] =
          String2.replaceAll(
              translatedMessages.get(Message.STANDARD_CONTACT)[tl],
              "&adminEmail;",
              SSR.getSafeEmailAddress(EDStatic.config.adminEmail));
      translatedMessages.get(Message.START_BODY_HTML)[tl] =
          String2.replaceAll(
              translatedMessages.get(Message.START_BODY_HTML)[tl],
              "&erddapVersion;",
              EDStatic.erddapVersion.getVersion());

      translatedMessages.get(Message.END_BODY_HTML)[tl] =
          String2.replaceAll(
              translatedMessages.get(Message.END_BODY_HTML)[tl],
              "&erddapVersion;",
              EDStatic.erddapVersion.getVersion());
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
      translatedMessages.get(Message.SEARCH_HINTS_TOOLTIP)[tl] =
          "<div class=\"standard_max_width\">"
              + translatedMessages.get(Message.SEARCH_HINTS_TOOLTIP)[tl]
              + "\n"
              + (EDStatic.config.useLuceneSearchEngine
                  ? translatedMessages.get(Message.SEARCH_HINTS_LUCENE_TOOLTIP)[tl]
                  : translatedMessages.get(Message.SEARCH_HINTS_ORIGINAL_TOOLTIP)[tl])
              + "</div>";
      translatedMessages.get(Message.ADVANCED_SEARCH_DIRECTIONS)[tl] =
          String2.replaceAll(
              translatedMessages.get(Message.ADVANCED_SEARCH_DIRECTIONS)[tl],
              "&searchButton;",
              translatedMessages.get(Message.SEARCH_BUTTON)[tl]);

      translatedMessages.get(Message.LOGIN_PROBLEMS)[tl] =
          String2.replaceAll(
              translatedMessages.get(Message.LOGIN_PROBLEMS)[tl],
              "&cookiesHelp;",
              translatedMessages.get(Message.COOKIES_HELP)[tl]);
      translatedMessages.get(Message.LOGIN_PROBLEMS)[tl] =
          String2.replaceAll(
                  translatedMessages.get(Message.LOGIN_PROBLEMS)[tl],
                  "&adminContact;",
                  EDStatic.adminContact())
              + "\n\n";
      translatedMessages.get(Message.LOGIN_PROBLEMS_AFTER)[tl] =
          String2.replaceAll(
                  translatedMessages.get(Message.LOGIN_PROBLEMS_AFTER)[tl],
                  "&adminContact;",
                  EDStatic.adminContact())
              + "\n\n";

      translatedMessages.get(Message.LOGIN_PUBLIC_ACCESS)[tl] += "\n";
      translatedMessages.get(Message.LOGOUT_SUCCESS)[tl] += "\n";

      translatedMessages.get(Message.FILES_DOCUMENTATION)[tl] =
          String2.replaceAll(
              translatedMessages.get(Message.FILES_DOCUMENTATION)[tl], "&adminEmail;", tEmail);

      translatedMessages.get(Message.DO_WITH_GRAPHS)[tl] =
          String2.replaceAll(
              translatedMessages.get(Message.DO_WITH_GRAPHS)[tl],
              "&ssUse;",
              EDStatic.config.slideSorterActive ? translatedMessages.get(Message.SS_USE)[tl] : "");

      translatedMessages.get(Message.THE_LONG_DESCRIPTION_HTML)[tl] =
          String2.replaceAll(
              translatedMessages.get(Message.THE_LONG_DESCRIPTION_HTML)[tl],
              "&ssUse;",
              EDStatic.config.slideSorterActive ? translatedMessages.get(Message.SS_USE)[tl] : "");
      translatedMessages.get(Message.THE_LONG_DESCRIPTION_HTML)[tl] =
          String2.replaceAll(
              translatedMessages.get(Message.THE_LONG_DESCRIPTION_HTML)[tl],
              "&requestFormatExamplesHtml;",
              translatedMessages.get(Message.REQUEST_FORMAT_EXAMPLES_HTML)[tl]);
      translatedMessages.get(Message.THE_LONG_DESCRIPTION_HTML)[tl] =
          String2.replaceAll(
              translatedMessages.get(Message.THE_LONG_DESCRIPTION_HTML)[tl],
              "&resultsFormatExamplesHtml;",
              translatedMessages.get(Message.RESULTS_FORMAT_EXAMPLES_HTML)[tl]);
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
    MustBe.InternalError = messagesAr[0].getNotNothingString("MustBeInternalError", errorInMethod);
    MustBe.OutOfMemoryError =
        messagesAr[0].getNotNothingString("MustBeOutOfMemoryError", errorInMethod);

    Attributes.signedToUnsignedAttNames =
        StringArray.arrayFromCSV(
            messagesAr[0].getNotNothingString("signedToUnsignedAttNames", errorInMethod));
    HtmlWidgets.twoClickMapDefaultTooltipAr =
        getNotNothingString(messagesAr, "twoClickMapDefaultTooltip", errorInMethod);
    gov.noaa.pfel.erddap.dataset.WaitThenTryAgainException.waitThenTryAgain =
        translatedMessages.get(Message.WAIT_THEN_TRY_AGAIN)[0];
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
            translatedMessages.get(Message.ACCEPT_ENCODING_HTML)[language],
            "&headingType;",
            "<" + headingType + ">");
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
        translatedMessages.get(Message.FILES_DOCUMENTATION)[language],
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
        + translatedMessages.get(Message.EXTERNAL_LINK)[language]
        + "\"\n"
        + "    title=\""
        + translatedMessages.get(Message.EXTERNAL_WEB_SITE)[language]
        + "\">";
  }

  public String theLongDescriptionHtml(int language, String tErddapUrl) {
    return String2.replaceAll(
        translatedMessages.get(Message.THE_LONG_DESCRIPTION_HTML)[language],
        "&erddapUrl;",
        tErddapUrl);
  }

  public String theShortDescriptionHtml(int language, String tErddapUrl) {
    String s =
        translatedMessages
            .get(Message.THE_SHORT_DESCRIPTION_HTML)[
            0]; // from datasets.xml or messages.xml.  Always use English, but parts (most) will be
    // translated.
    s = String2.replaceAll(s, "&erddapIs;", translatedMessages.get(Message.ERDDAP_IS)[language]);
    s =
        String2.replaceAll(
            s,
            "&thisParticularErddap;",
            translatedMessages.get(Message.THIS_PARTICULAR_ERDDAP)[language]);
    s =
        String2.replaceAll(
            s,
            "[standardShortDescriptionHtml]",
            translatedMessages.get(Message.STANDARD_SHORT_DESCRIPTION_HTML)[language]);
    s =
        String2.replaceAll(
            s,
            "&requestFormatExamplesHtml;",
            translatedMessages.get(Message.REQUEST_FORMAT_EXAMPLES_HTML)[language]);
    s = String2.replaceAll(s, "&erddapUrl;", tErddapUrl); // do last
    return s;
  }

  public String get(Message message, int language) {
    String[] messages = translatedMessages.get(message);
    if (messages == null || language >= messages.length) {
      // Return default or throw exception
      return "Message not found";
    }
    return messages[language];
  }

  public void setDefault(Message toSet, String message, Message fallback) {
    translatedMessages.get(toSet)[0] =
        String2.isSomething(message) ? message : translatedMessages.get(fallback)[0];
  }
}
