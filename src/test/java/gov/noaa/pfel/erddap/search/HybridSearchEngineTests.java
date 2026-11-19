package gov.noaa.pfel.erddap.search;

import static org.junit.jupiter.api.Assertions.*;

import com.cohort.array.Attributes;
import com.cohort.array.StringArray;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.util.EDMessages;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testDataset.Initialization;

class HybridSearchEngineTests {

  private ConcurrentHashMap<String, EDDGrid> gridDatasets;
  private ConcurrentHashMap<String, EDDTable> tableDatasets;
  private boolean origUseHybridSearch;
  private boolean origListPrivateDatasets;

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  @BeforeEach
  void setUp() throws Throwable {
    origUseHybridSearch = EDStatic.config.useHybridSearch;
    origListPrivateDatasets = EDStatic.config.listPrivateDatasets;
    EDStatic.config.useHybridSearch = true;
    EDStatic.config.listPrivateDatasets = false;

    gridDatasets = new ConcurrentHashMap<>();
    tableDatasets = new ConcurrentHashMap<>();

    // 1. Sea Surface Temperature dataset (camelCase ID + CF standard_name)
    tableDatasets.put(
        "jplMURSST41",
        new StubTableDataset(
            "jplMURSST41",
            "Multi-scale Ultra-high Resolution (MUR) SST Analysis",
            "Daily global sea surface temperature fields from satellite radiometers.",
            "NASA JPL",
            new VarSpec[] {
              new VarSpec(
                  "analysed_sst", "Analysed Sea Surface Temperature", "sea_water_temperature")
            },
            null));

    // 2. Precipitation dataset (for prefix matching: "precip" -> "precipitation")
    tableDatasets.put(
        "ncepPrecipDaily",
        new StubTableDataset(
            "ncepPrecipDaily",
            "NCEP Global Daily Precipitation Rate",
            "Gridded rainfall and surface precipitation flux observations.",
            "NOAA NCEP",
            new VarSpec[] {
              new VarSpec("prate", "Surface Precipitation Rate", "precipitation_flux")
            },
            null));

    // 3. Chlorophyll / Ocean Color dataset (for fuzzy typo + semantic matching)
    tableDatasets.put(
        "erdMBchla8day",
        new StubTableDataset(
            "erdMBchla8day",
            "Chlorophyll-a, Aqua MODIS, NPP, Global, 8-Day",
            "Satellite ocean color measurements of near-surface chlorophyll concentration.",
            "NOAA CoastWatch",
            new VarSpec[] {
              new VarSpec(
                  "chlorophyll",
                  "Concentration of Chlorophyll in Sea Water",
                  "mass_concentration_of_chlorophyll_a_in_sea_water")
            },
            null));

    // 4. Salinity dataset (for fuzzy typo: "salinty" -> "salinity")
    tableDatasets.put(
        "argoSalinityProfile",
        new StubTableDataset(
            "argoSalinityProfile",
            "Argo Float Practical Salinity Profiles",
            "In-situ ocean salinity and conductivity observations from profiling floats.",
            "IFREMER",
            new VarSpec[] {
              new VarSpec("psal", "Practical Salinity", "sea_water_practical_salinity")
            },
            null));

    // 5. Deprecated SST dataset (should be down-ranked below active SST dataset)
    tableDatasets.put(
        "legacySST_v1",
        new StubTableDataset(
            "legacySST_v1",
            "DEPRECATED Pathfinder Sea Surface Temperature v1",
            "Historical sea surface temperature archive.",
            "NOAA NODC",
            new VarSpec[] {new VarSpec("sst", "Sea Surface Temperature", "sea_water_temperature")},
            null));

    // 6. Restricted private dataset (for ACL enforcement)
    tableDatasets.put(
        "privateBuoyData",
        new StubTableDataset(
            "privateBuoyData",
            "Proprietary Coastal Wave and Temperature Buoy",
            "Restricted sea surface temperature and wave height telemetry.",
            "Private Lab",
            new VarSpec[] {
              new VarSpec("water_temp", "Sea Water Temperature", "sea_water_temperature")
            },
            "ocean_team"));

    HybridSearchEngine.rebuild(gridDatasets, tableDatasets);
  }

  @AfterEach
  void tearDown() {
    EDStatic.config.useHybridSearch = origUseHybridSearch;
    EDStatic.config.listPrivateDatasets = origListPrivateDatasets;
  }

  @Test
  void rebuildMarksEngineReadyAndReflectsDatasetRemoval() {
    assertTrue(HybridSearchEngine.isReady());

    StringArray initial = searchHybrid("salinity", null);
    assertTrue(toList(initial).contains("argoSalinityProfile"));

    // Simulate unloading a dataset and rebuilding
    tableDatasets.remove("argoSalinityProfile");
    HybridSearchEngine.rebuild(gridDatasets, tableDatasets);

    StringArray afterUnload = searchHybrid("salinity", null);
    assertFalse(toList(afterUnload).contains("argoSalinityProfile"));
  }

  @Test
  void exactDatasetIdPinsToTopRank() {
    List<String> results = toList(searchHybrid("jplMURSST41", null));
    assertFalse(results.isEmpty());
    assertEquals("jplMURSST41", results.get(0));
  }

  @Test
  void camelCaseAndUnderscoreTokenizationMatches() {
    List<String> murResults = toList(searchHybrid("MUR", null));
    assertTrue(murResults.contains("jplMURSST41"));

    List<String> cfResults = toList(searchHybrid("sea_water_practical_salinity", null));
    assertTrue(cfResults.contains("argoSalinityProfile"));
  }

  @Test
  void prefixAndSubstringMatchingIncludesAllExactMatches() {
    // "precip" must match "precipitation" via prefix expansion
    List<String> hybridPrecip = toList(searchHybrid("precip", null));
    assertTrue(hybridPrecip.contains("ncepPrecipDaily"));

    // "flux" must match via lexical/substring expansion
    List<String> hybridFlux = toList(searchHybrid("flux", null));
    assertTrue(hybridFlux.contains("ncepPrecipDaily"));
  }

  @Test
  void fuzzySearchHandlesTypos() {
    // 1-edit deletion ("chlorophyl" -> "chlorophyll")
    List<String> chlResults = toList(searchHybrid("chlorophyl", null));
    assertTrue(chlResults.contains("erdMBchla8day"));

    // 1-edit deletion ("salinty" -> "salinity")
    List<String> salResults = toList(searchHybrid("salinty", null));
    assertTrue(salResults.contains("argoSalinityProfile"));
  }

  @Test
  void semanticVectorSearchMatchesZeroOverlapConcepts() {
    // Neither "algal" nor "bloom" appears in erdMBchla8day's metadata
    List<String> bloomResults = toList(searchHybrid("algal bloom phytoplankton", null));
    assertTrue(
        bloomResults.contains("erdMBchla8day"),
        "Expected semantic vector match for algal bloom -> chlorophyll dataset, got: "
            + bloomResults);

    // "ocean heat content" should surface SST dataset via dense embeddings
    List<String> heatResults = toList(searchHybrid("ocean heat content thermal", null));
    assertTrue(
        heatResults.contains("jplMURSST41"),
        "Expected semantic vector match for ocean heat -> SST dataset, got: " + heatResults);
  }

  @Test
  void deprecatedDatasetsAreDownRankedBelowActiveMatches() {
    List<String> results = toList(searchHybrid("sea surface temperature", null));
    int activeIdx = results.indexOf("jplMURSST41");
    int deprecatedIdx = results.indexOf("legacySST_v1");

    assertTrue(activeIdx >= 0, "Active SST dataset should match");
    assertTrue(deprecatedIdx >= 0, "Deprecated SST dataset should still be returned");
    assertTrue(
        activeIdx < deprecatedIdx,
        "Active dataset should rank higher than DEPRECATED dataset: " + results);
  }

  @Test
  void respectsQuotedPhrasesNegationsAndAllNegativeQueries() {
    // Exclusion removes legacySST_v1
    List<String> noDeprecated = toList(searchHybrid("temperature -DEPRECATED", null));
    assertTrue(noDeprecated.contains("jplMURSST41"));
    assertFalse(noDeprecated.contains("legacySST_v1"));

    // Quoted phrase enforces exact substring presence
    List<String> exactPhrase = toList(searchHybrid("\"Aqua MODIS\"", null));
    assertEquals(List.of("erdMBchla8day"), exactPhrase);

    // All-negative query returns all accessible datasets excluding the negated term
    List<String> allExceptSst = toList(searchHybrid("-temperature", null));
    assertTrue(allExceptSst.contains("ncepPrecipDaily"));
    assertTrue(allExceptSst.contains("argoSalinityProfile"));
    assertFalse(allExceptSst.contains("jplMURSST41"));
  }

  @Test
  void respectsCandidateSubsetPreFiltering() {
    StringArray restrictedCandidates =
        new StringArray(new String[] {"ncepPrecipDaily", "argoSalinityProfile"});
    List<String> results =
        toList(
            HybridSearchEngine.search(
                EDMessages.DEFAULT_LANGUAGE,
                null,
                restrictedCandidates,
                "temperature",
                gridDatasets,
                tableDatasets));

    // jplMURSST41 is not in restrictedCandidates, so it must not appear
    assertFalse(results.contains("jplMURSST41"));
  }

  @Test
  void enforcesDatasetAccessControl() {
    // Anonymous user cannot see privateBuoyData
    List<String> publicResults = toList(searchHybrid("Proprietary Coastal Wave", null));
    assertFalse(publicResults.contains("privateBuoyData"));

    // User with "ocean_team" role can see privateBuoyData
    List<String> authResults =
        toList(searchHybrid("Proprietary Coastal Wave", new String[] {"ocean_team"}));
    assertTrue(authResults.contains("privateBuoyData"));
  }

  @Test
  void erddapGetSearchDatasetIDsRoutesBetweenHybridAndExactModes() throws Throwable {
    Erddap erddap = new Erddap();
    erddap.gridDatasetHashMap.putAll(gridDatasets);
    erddap.tableDatasetHashMap.putAll(tableDatasets);
    StringArray allIds = allCandidateIDs();

    // 1. Typo query fails in explicit "exact" mode, succeeds in "hybrid" mode
    List<String> exactTypo =
        toList(
            erddap.getSearchDatasetIDs(
                EDMessages.DEFAULT_LANGUAGE, null, allIds, "chloropyll", "exact"));
    assertTrue(exactTypo.isEmpty(), "Exact search should not match misspelled 'chloropyll'");

    List<String> hybridTypo =
        toList(
            erddap.getSearchDatasetIDs(
                EDMessages.DEFAULT_LANGUAGE, null, allIds, "chloropyll", "hybrid"));
    assertTrue(hybridTypo.contains("erdMBchla8day"));

    // 2. When EDStatic.config.useHybridSearch = false, default (4-arg) uses exact search,
    //    while explicit searchMode="hybrid" overrides the server default
    EDStatic.config.useHybridSearch = false;
    List<String> defaultWhenDisabled =
        toList(erddap.getSearchDatasetIDs(EDMessages.DEFAULT_LANGUAGE, null, allIds, "chloropyll"));
    assertTrue(defaultWhenDisabled.isEmpty());

    List<String> explicitHybridOverride =
        toList(
            erddap.getSearchDatasetIDs(
                EDMessages.DEFAULT_LANGUAGE, null, allIds, "chloropyll", "hybrid"));
    assertTrue(explicitHybridOverride.contains("erdMBchla8day"));

    // 3. Special "all" query returns all accessible datasets sorted by title
    List<String> allResults =
        toList(
            erddap.getSearchDatasetIDs(EDMessages.DEFAULT_LANGUAGE, null, allIds, "all", "hybrid"));
    assertEquals(5, allResults.size());
    assertFalse(allResults.contains("privateBuoyData"));
  }

  @Test
  void externalEmbeddingBinaryIsLoaded() {
    assertTrue(
        HybridSearchEngine.isUsingExternalEmbeddings(),
        "erddap_embeddings.bin did not load from classpath");
  }

  @Test
  void expandsAcronymsVariableNamesAndCfToGcmdKeywords() throws Throwable {
    tableDatasets.put(
        "mooringAdcpCurrents",
        new StubTableDataset(
            "mooringAdcpCurrents",
            "Coastal Mooring ADCP Velocity Profiles",
            "Subsurface acoustic current meter observations.",
            "NOAA PMEL",
            new VarSpec[] {
              new VarSpec("u_vel", "Eastward Current Speed", "eastward_sea_water_velocity")
            },
            null));
    HybridSearchEngine.rebuild(gridDatasets, tableDatasets);

    // 1. Acronym expansion ("acoustic doppler current profiler" -> "ADCP")
    List<String> adcpResults = toList(searchHybrid("acoustic doppler current profiler", null));
    assertTrue(
        adcpResults.contains("mooringAdcpCurrents"),
        "Expected ADCP acronym expansion match, got: " + adcpResults);

    // 2. Variable name table expansion ("prate" -> "Precipitation Rate")
    List<String> prateResults = toList(searchHybrid("prate", null));
    assertTrue(
        prateResults.contains("ncepPrecipDaily"),
        "Expected variable abbreviation 'prate' match, got: " + prateResults);

    // 3. CfToFromGcmd expansion ("mass_concentration_of_chlorophyll_a_in_sea_water" -> GCMD
    //    "OCEAN CHEMISTRY" / "PHYTOPLANKTON")
    List<String> gcmdResults = toList(searchHybrid("ocean chemistry phytoplankton", null));
    assertTrue(
        gcmdResults.contains("erdMBchla8day"),
        "Expected CfToFromGcmd enrichment match, got: " + gcmdResults);

    // 4. Variable name table expansion ("slp" -> "Sea Level Pressure" with zero lexical overlap)
    tableDatasets.put(
        "buoyPressure",
        new StubTableDataset(
            "buoyPressure",
            "Coastal Barometric Observations",
            "Hourly atmospheric pressure measurements.",
            "NOAA NDBC",
            new VarSpec[] {
              new VarSpec("air_pressure", "Sea Level Pressure", "air_pressure_at_mean_sea_level")
            },
            null));
    HybridSearchEngine.rebuild(gridDatasets, tableDatasets);

    List<String> slpResults = toList(searchHybrid("slp", null));
    assertTrue(
        slpResults.contains("buoyPressure"),
        "Expected variable abbreviation 'slp' -> 'Sea Level Pressure' match, got: " + slpResults);
  }

  // --- Helpers & Lightweight Test Fixture ---

  private StringArray allCandidateIDs() {
    StringArray ids = new StringArray();
    for (String id : gridDatasets.keySet()) ids.add(id);
    for (String id : tableDatasets.keySet()) ids.add(id);
    return ids;
  }

  private StringArray searchHybrid(String query, String[] roles) {
    return HybridSearchEngine.search(
        EDMessages.DEFAULT_LANGUAGE, roles, allCandidateIDs(), query, gridDatasets, tableDatasets);
  }

  private static List<String> toList(StringArray sa) {
    List<String> list = new ArrayList<>(sa.size());
    for (int i = 0; i < sa.size(); i++) {
      list.add(sa.get(i));
    }
    return list;
  }

  private record VarSpec(String name, String longName, String standardName) {}

  private static final class StubTableDataset extends EDDTable {
    StubTableDataset(
        String id,
        String title,
        String summary,
        String institution,
        VarSpec[] vars,
        String accessibleToRoles)
        throws Throwable {
      this.className = "StubTableDataset";
      this.datasetID = id;
      setAccessibleTo(accessibleToRoles);
      setGraphsAccessibleTo("auto");

      this.accessibleViaMAG = "no";
      this.accessibleViaSubset = "no";
      this.accessibleViaGeoServicesRest = "no";
      this.accessibleViaSOS = "no";
      this.accessibleViaWCS = "no";
      this.accessibleViaWMS = "no";
      this.accessibleViaNcCF = "no";
      this.accessibleViaFGDC = "no";
      this.accessibleViaISO19115 = "no";

      this.sourceGlobalAttributes = new Attributes();
      this.addGlobalAttributes = new LocalizedAttributes();
      this.combinedGlobalAttributes = new LocalizedAttributes();
      int lang = EDMessages.DEFAULT_LANGUAGE;
      this.combinedGlobalAttributes.set(lang, "title", title);
      this.combinedGlobalAttributes.set(lang, "summary", summary);
      this.combinedGlobalAttributes.set(lang, "institution", institution);

      this.dataVariables = new EDV[vars.length];
      for (int i = 0; i < vars.length; i++) {
        Attributes srcAtts = new Attributes();
        srcAtts.set("long_name", vars[i].longName());
        if (vars[i].standardName() != null) {
          srcAtts.set("standard_name", vars[i].standardName());
        }
        this.dataVariables[i] =
            new EDV(
                id,
                vars[i].name(),
                vars[i].name(),
                srcAtts,
                new LocalizedAttributes(),
                "float",
                null,
                null);
      }
      // Pre-populate cached searchString for exact-search comparisons
      searchString(lang);
    }

    @Override
    public void getDataForDapQuery(
        int language,
        String loggedInAs,
        String requestUrl,
        String userDapQuery,
        TableWriter tableWriter) {}
  }
}
