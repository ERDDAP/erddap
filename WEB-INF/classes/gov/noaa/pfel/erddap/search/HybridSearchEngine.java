package gov.noaa.pfel.erddap.search;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.util.CfToFromGcmd;
import gov.noaa.pfel.erddap.util.EDMessages;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.TranslateMessages;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Zero-dependency in-memory hybrid search engine (Fuzzy BM25 + dense vector cosine similarity fused
 * via Reciprocal Rank Fusion) for ERDDAP dataset discovery, with sparse per-language overlays for
 * localized dataset metadata.
 */
public class HybridSearchEngine {

  private static final String EMBEDDING_RESOURCE =
      "/gov/noaa/pfel/erddap/search/erddap_embeddings.bin";
  private static final int FALLBACK_DIM = 256;
  private static final double BM25_K1 = 1.2;
  private static final double BM25_B = 0.75;
  private static final double RRF_K = 60.0;
  private static final long DEFAULT_REBUILD_DEBOUNCE_MILLIS = 1500L;

  private static volatile IndexSnapshot currentSnapshot = null;
  private static final ConcurrentHashMap<String, CachedVector> vectorCache =
      new ConcurrentHashMap<>();
  private static volatile StaticEmbeddingModel embeddingModel = null;
  private static final Object modelLock = new Object();
  private static volatile DomainLexicon domainLexicon = null;
  private static final Object lexiconLock = new Object();

  private static final Object scheduleLock = new Object();
  private static ScheduledExecutorService rebuildExecutor = null;
  private static ScheduledFuture<?> pendingRebuildTask = null;
  private static volatile Map<String, EDDGrid> pendingGridDatasets = null;
  private static volatile Map<String, EDDTable> pendingTableDatasets = null;

  private static final ThreadLocal<QueryScratch> queryScratch =
      ThreadLocal.withInitial(QueryScratch::new);

  private static final class QueryScratch {
    double[] bm25 = new double[0];
    double[] vec = new double[0];
    double[] rrf = new double[0];
    final BitSet eligible = new BitSet();
    final BitSet candidates = new BitSet();

    void prepare(int nDocs) {
      if (bm25.length < nDocs) {
        int cap = Math.max(nDocs, bm25.length * 2);
        bm25 = new double[cap];
        vec = new double[cap];
        rrf = new double[cap];
      } else {
        Arrays.fill(bm25, 0, nDocs, 0.0);
        Arrays.fill(vec, 0, nDocs, 0.0);
        Arrays.fill(rrf, 0, nDocs, 0.0);
      }
      eligible.clear();
      candidates.clear();
    }
  }

  private static final class CachedVector {
    final long hashHigh;
    final long hashLow;
    final float[] unitVector;

    CachedVector(long hashHigh, long hashLow, float[] unitVector) {
      this.hashHigh = hashHigh;
      this.hashLow = hashLow;
      this.unitVector = unitVector;
    }
  }

  private static final class PostingList {
    final int[] docIndices;
    final float[] weightedTfs;

    PostingList(int[] docIndices, float[] weightedTfs) {
      this.docIndices = docIndices;
      this.weightedTfs = weightedTfs;
    }
  }

  private static final class VocabIndex {
    final Map<String, PostingList> postings;
    final String[][] vocabByLength; // sorted lexicographically within each length bucket
    final int[][]
        vocabMasksByLength; // 32-bit character presence bitmasks parallel to vocabByLength

    VocabIndex(
        Map<String, PostingList> postings, String[][] vocabByLength, int[][] vocabMasksByLength) {
      this.postings = postings;
      this.vocabByLength = vocabByLength;
      this.vocabMasksByLength = vocabMasksByLength;
    }
  }

  /**
   * Sparse per-language index storing only datasets that actually override default-language
   * metadata for a given language index.
   */
  private static final class LanguageOverlay {
    final int[]
        docIndices; // sorted ascending global docIdx values for datasets localized in this lang
    final String[] localizedTitles; // parallel to docIndices
    final float[] flatVectors; // [docIndices.length * dim], L2-normalized
    final VocabIndex vocabIndex;

    LanguageOverlay(
        int[] docIndices, String[] localizedTitles, float[] flatVectors, VocabIndex vocabIndex) {
      this.docIndices = docIndices;
      this.localizedTitles = localizedTitles;
      this.flatVectors = flatVectors;
      this.vocabIndex = vocabIndex;
    }

    int findLocalIndex(int globalDocIdx) {
      return Arrays.binarySearch(docIndices, globalDocIdx);
    }
  }

  private static final class IndexSnapshot {
    final String[] datasetIDs;
    final String[] lowerDatasetIDs;
    final String[] defaultTitles;
    final Map<String, Integer> idToDocIndex;
    final VocabIndex defaultVocab;
    final float[] docLengths;
    final float avgDocLength;
    final float[] flatVectors; // [numDocs * dim], L2-normalized
    final int dim;
    final LanguageOverlay[]
        langOverlays; // sparse; null entries for languages with no localized data

    IndexSnapshot(
        String[] datasetIDs,
        String[] lowerDatasetIDs,
        String[] defaultTitles,
        Map<String, Integer> idToDocIndex,
        VocabIndex defaultVocab,
        float[] docLengths,
        float avgDocLength,
        float[] flatVectors,
        int dim,
        LanguageOverlay[] langOverlays) {
      this.datasetIDs = datasetIDs;
      this.lowerDatasetIDs = lowerDatasetIDs;
      this.defaultTitles = defaultTitles;
      this.idToDocIndex = idToDocIndex;
      this.defaultVocab = defaultVocab;
      this.docLengths = docLengths;
      this.avgDocLength = avgDocLength;
      this.flatVectors = flatVectors;
      this.dim = dim;
      this.langOverlays = langOverlays;
    }
  }

  public static boolean isReady() {
    return currentSnapshot != null;
  }

  static boolean isUsingExternalEmbeddings() {
    return getOrLoadModel().isExternalModel;
  }

  static boolean hasLanguageOverlay(int language) {
    IndexSnapshot snap = currentSnapshot;
    return snap != null
        && language >= 0
        && language < snap.langOverlays.length
        && snap.langOverlays[language] != null;
  }

  /**
   * Schedules a coalesced background rebuild after {@link #DEFAULT_REBUILD_DEBOUNCE_MILLIS}. Safe
   * to call in tight loops (such as batch unloads in {@code LoadDatasets.tryToUnload}); rapid calls
   * collapse into a single rebuild once the burst settles.
   */
  public static void scheduleRebuild(
      Map<String, EDDGrid> gridDatasets, Map<String, EDDTable> tableDatasets) {
    scheduleRebuild(gridDatasets, tableDatasets, DEFAULT_REBUILD_DEBOUNCE_MILLIS);
  }

  static void scheduleRebuild(
      Map<String, EDDGrid> gridDatasets, Map<String, EDDTable> tableDatasets, long delayMillis) {
    synchronized (scheduleLock) {
      pendingGridDatasets = gridDatasets;
      pendingTableDatasets = tableDatasets;
      if (rebuildExecutor == null || rebuildExecutor.isShutdown()) {
        rebuildExecutor =
            Executors.newSingleThreadScheduledExecutor(
                r -> {
                  Thread t = new Thread(r, "erddap-hybrid-search-rebuilder");
                  t.setDaemon(true);
                  return t;
                });
      }
      if (pendingRebuildTask != null && !pendingRebuildTask.isDone()) {
        pendingRebuildTask.cancel(false);
      }
      pendingRebuildTask =
          rebuildExecutor.schedule(
              () -> {
                Map<String, EDDGrid> g;
                Map<String, EDDTable> tb;
                synchronized (scheduleLock) {
                  g = pendingGridDatasets;
                  tb = pendingTableDatasets;
                  pendingRebuildTask = null;
                }
                if (g != null && tb != null) {
                  try {
                    rebuild(g, tb);
                  } catch (Throwable t) {
                    String2.log("WARNING: Debounced HybridSearchEngine.rebuild failed: " + t);
                  }
                }
              },
              Math.max(0L, delayMillis),
              TimeUnit.MILLISECONDS);
    }
  }

  /** Shuts down the background rebuild scheduler (called during {@code EDStatic.destroy}). */
  public static void shutdown() {
    synchronized (scheduleLock) {
      if (pendingRebuildTask != null) {
        pendingRebuildTask.cancel(false);
        pendingRebuildTask = null;
      }
      pendingGridDatasets = null;
      pendingTableDatasets = null;
      if (rebuildExecutor != null) {
        rebuildExecutor.shutdownNow();
        rebuildExecutor = null;
      }
    }
  }

  /**
   * Rebuilds the immutable in-memory search snapshot synchronously from the currently active grid
   * and table datasets and atomically swaps it into place. Cancels any pending debounced rebuild.
   */
  public static synchronized void rebuild(
      Map<String, EDDGrid> gridDatasets, Map<String, EDDTable> tableDatasets) {
    synchronized (scheduleLock) {
      if (pendingRebuildTask != null && !pendingRebuildTask.isDone()) {
        pendingRebuildTask.cancel(false);
      }
      pendingRebuildTask = null;
      pendingGridDatasets = null;
      pendingTableDatasets = null;
    }

    long t0 = System.currentTimeMillis();
    StaticEmbeddingModel model = getOrLoadModel();
    DomainLexicon lexicon = getOrLoadLexicon();
    int dim = model.dim;

    List<EDD> allDatasets = new ArrayList<>(gridDatasets.size() + tableDatasets.size());
    allDatasets.addAll(gridDatasets.values());
    allDatasets.addAll(tableDatasets.values());

    int nDocs = allDatasets.size();
    String[] datasetIDs = new String[nDocs];
    String[] lowerDatasetIDs = new String[nDocs];
    String[] defaultTitles = new String[nDocs];
    Map<String, Integer> idToDocIndex = new HashMap<>(Math.max(16, (int) (nDocs * 1.4f)));
    float[] docLengths = new float[nDocs];
    float[] flatVectors = new float[nDocs * dim];

    Map<String, List<Integer>> termDocLists = new HashMap<>();
    Map<String, List<Float>> termTfLists = new HashMap<>();
    Set<String> activeCacheKeys = new HashSet<>(Math.max(16, (int) (nDocs * 1.6f)));

    int defaultLang = EDMessages.DEFAULT_LANGUAGE;
    int nLangs = TranslateMessages.languageCodeList.size();

    // Sparse per-language accumulators (allocated only if at least one dataset localizes that lang)
    @SuppressWarnings("unchecked")
    List<Integer>[] langDocIndices = new List[nLangs];
    @SuppressWarnings("unchecked")
    List<String>[] langTitlesList = new List[nLangs];
    @SuppressWarnings("unchecked")
    List<float[]>[] langVectorsList = new List[nLangs];
    @SuppressWarnings("unchecked")
    Map<String, List<Integer>>[] langTermDocLists = new Map[nLangs];
    @SuppressWarnings("unchecked")
    Map<String, List<Float>>[] langTermTfLists = new Map[nLangs];

    double totalDocLen = 0.0;
    int localizedEntryCount = 0;

    for (int docIdx = 0; docIdx < nDocs; docIdx++) {
      EDD edd = allDatasets.get(docIdx);
      String id = String2.canonical(edd.datasetID());
      String lowerId = id.toLowerCase(Locale.ROOT);
      String rawDefTitle = edd.title(defaultLang);
      String defTitle = rawDefTitle != null ? rawDefTitle : id;
      String defSummary = edd.summary(defaultLang);
      String defInstitution = edd.institution(defaultLang);

      datasetIDs[docIdx] = id;
      lowerDatasetIDs[docIdx] = lowerId;
      defaultTitles[docIdx] = defTitle;
      idToDocIndex.put(id, docIdx);

      // Build default-language lexical terms and semantic text
      Map<String, Float> tfMap = new HashMap<>();
      addWeightedTokens(tfMap, lowerId, 4.0f, true);
      addWeightedTokens(tfMap, defTitle, 3.0f, false);

      StringBuilder semanticText = new StringBuilder(512);
      semanticText.append(lexicon.expandText(defTitle, false, 6)).append(". ");
      String idExpansion = lexicon.expandIdentifierTokens(id);
      if (!idExpansion.isEmpty()) {
        semanticText.append(idExpansion).append(". ");
      }

      if (defSummary != null && !defSummary.isEmpty()) {
        addWeightedTokens(tfMap, defSummary, 1.0f, false);
        semanticText.append(lexicon.expandText(defSummary, false, 8)).append(". ");
      }

      if (defInstitution != null && !defInstitution.isEmpty()) {
        addWeightedTokens(tfMap, defInstitution, 1.5f, false);
        semanticText.append(lexicon.expandText(defInstitution, false, 4)).append(". ");
      }

      EDV[] dataVars = edd.dataVariables();
      String[] defVarLongNames = null;
      String[] defVarStdNames = null;
      if (dataVars != null) {
        defVarLongNames = new String[dataVars.length];
        defVarStdNames = new String[dataVars.length];
        Set<String> gcmdSet = new LinkedHashSet<>();
        for (int vi = 0; vi < dataVars.length; vi++) {
          EDV v = dataVars[vi];
          if (v == null) continue;
          String dName = v.destinationName();
          String lName =
              v.combinedAttributes() != null
                  ? v.combinedAttributes().getString(defaultLang, "long_name")
                  : v.longName();
          String stdName =
              v.combinedAttributes() != null
                  ? v.combinedAttributes().getString(defaultLang, "standard_name")
                  : null;
          defVarLongNames[vi] = lName;
          defVarStdNames[vi] = stdName;

          if (dName != null) {
            addWeightedTokens(tfMap, dName, 2.0f, false);
            semanticText.append(dName.replace('_', ' ')).append(' ');
            String varFull = lexicon.lookupVariableName(dName);
            if (varFull != null) {
              semanticText.append(varFull).append(' ');
            }
          }
          if (lName != null) {
            addWeightedTokens(tfMap, lName, 2.0f, false);
            semanticText.append(lName).append(' ');
          }
          if (stdName != null) {
            addWeightedTokens(tfMap, stdName, 2.5f, false);
            semanticText.append(stdName.replace('_', ' ')).append(' ');
            lexicon.collectGcmdKeywords(stdName, gcmdSet);
          }
        }
        for (String gcmdTerm : gcmdSet) {
          semanticText.append(gcmdTerm).append(' ');
        }
      }

      // Include full searchString at base weight so protocol=..., keywords, units still match
      String fullSearchStr = edd.searchString(defaultLang);
      if (fullSearchStr != null) {
        addWeightedTokens(tfMap, fullSearchStr, 0.5f, false);
      }

      float docLen = 0.0f;
      for (Map.Entry<String, Float> entry : tfMap.entrySet()) {
        String term = entry.getKey();
        float tf = entry.getValue();
        docLen += tf;
        termDocLists.computeIfAbsent(term, k -> new ArrayList<>()).add(docIdx);
        termTfLists.computeIfAbsent(term, k -> new ArrayList<>()).add(tf);
      }
      docLengths[docIdx] = Math.max(1.0f, docLen);
      totalDocLen += docLengths[docIdx];

      // 128-bit collision-resistant incremental embedding cache lookup for default language
      activeCacheKeys.add(id);
      float[] vec = getOrComputeEmbedding(id, semanticText.toString(), model, dim);
      System.arraycopy(vec, 0, flatVectors, docIdx * dim, dim);

      // Check non-default languages and only index/embed languages with distinct localized metadata
      for (int lang = 0; lang < nLangs; lang++) {
        if (lang == defaultLang) continue;

        String langTitle = edd.title(lang);
        String langSummary = edd.summary(lang);
        String langInstitution = edd.institution(lang);

        boolean hasLocalizedMeta =
            !Objects.equals(langTitle, rawDefTitle)
                || !Objects.equals(langSummary, defSummary)
                || !Objects.equals(langInstitution, defInstitution);

        if (!hasLocalizedMeta && dataVars != null) {
          for (int vi = 0; vi < dataVars.length; vi++) {
            EDV v = dataVars[vi];
            if (v == null || v.combinedAttributes() == null) continue;
            String vLong = v.combinedAttributes().getString(lang, "long_name");
            String vStd = v.combinedAttributes().getString(lang, "standard_name");
            if (!Objects.equals(vLong, defVarLongNames[vi])
                || !Objects.equals(vStd, defVarStdNames[vi])) {
              hasLocalizedMeta = true;
              break;
            }
          }
        }

        if (!hasLocalizedMeta) continue;

        String effectiveLangTitle =
            (langTitle != null && !langTitle.isEmpty()) ? langTitle : defTitle;
        Map<String, Float> langTfMap = new HashMap<>();
        StringBuilder langSemText = new StringBuilder(384);

        if (effectiveLangTitle != null
            && !effectiveLangTitle.isEmpty()
            && !Objects.equals(effectiveLangTitle, defTitle)) {
          addWeightedTokens(langTfMap, effectiveLangTitle, 3.0f, false);
        }
        if (effectiveLangTitle != null && !effectiveLangTitle.isEmpty()) {
          langSemText.append(lexicon.expandText(effectiveLangTitle, false, 6)).append(". ");
        }
        if (langSummary != null
            && !langSummary.isEmpty()
            && !Objects.equals(langSummary, defSummary)) {
          addWeightedTokens(langTfMap, langSummary, 1.0f, false);
        }
        if (langSummary != null && !langSummary.isEmpty()) {
          langSemText.append(lexicon.expandText(langSummary, false, 8)).append(". ");
        }
        if (langInstitution != null
            && !langInstitution.isEmpty()
            && !Objects.equals(langInstitution, defInstitution)) {
          addWeightedTokens(langTfMap, langInstitution, 1.5f, false);
        }
        if (langInstitution != null && !langInstitution.isEmpty()) {
          langSemText.append(lexicon.expandText(langInstitution, false, 4)).append(". ");
        }

        if (dataVars != null) {
          Set<String> gcmdSet = new LinkedHashSet<>();
          for (int vi = 0; vi < dataVars.length; vi++) {
            EDV v = dataVars[vi];
            if (v == null) continue;
            String dName = v.destinationName();
            String vLong =
                v.combinedAttributes() != null
                    ? v.combinedAttributes().getString(lang, "long_name")
                    : v.longName();
            String vStd =
                v.combinedAttributes() != null
                    ? v.combinedAttributes().getString(lang, "standard_name")
                    : null;

            if (dName != null) {
              langSemText.append(dName.replace('_', ' ')).append(' ');
            }
            if (vLong != null && !vLong.isEmpty()) {
              if (!Objects.equals(vLong, defVarLongNames[vi])) {
                addWeightedTokens(langTfMap, vLong, 2.0f, false);
              }
              langSemText.append(vLong).append(' ');
            }
            if (vStd != null && !vStd.isEmpty()) {
              if (!Objects.equals(vStd, defVarStdNames[vi])) {
                addWeightedTokens(langTfMap, vStd, 2.5f, false);
              }
              langSemText.append(vStd.replace('_', ' ')).append(' ');
              lexicon.collectGcmdKeywords(vStd, gcmdSet);
            }
          }
          for (String gcmdTerm : gcmdSet) {
            langSemText.append(gcmdTerm).append(' ');
          }
        }

        if (langDocIndices[lang] == null) {
          langDocIndices[lang] = new ArrayList<>();
          langTitlesList[lang] = new ArrayList<>();
          langVectorsList[lang] = new ArrayList<>();
          langTermDocLists[lang] = new HashMap<>();
          langTermTfLists[lang] = new HashMap<>();
        }

        for (Map.Entry<String, Float> entry : langTfMap.entrySet()) {
          String term = entry.getKey();
          float tf = entry.getValue();
          langTermDocLists[lang].computeIfAbsent(term, k -> new ArrayList<>()).add(docIdx);
          langTermTfLists[lang].computeIfAbsent(term, k -> new ArrayList<>()).add(tf);
        }

        String cacheKey = id + "\0" + TranslateMessages.languageCodeList.get(lang);
        activeCacheKeys.add(cacheKey);
        float[] langVec = getOrComputeEmbedding(cacheKey, langSemText.toString(), model, dim);

        langDocIndices[lang].add(docIdx);
        langTitlesList[lang].add(effectiveLangTitle);
        langVectorsList[lang].add(langVec);
        localizedEntryCount++;
      }
    }

    // Evict removed datasets and removed translations from vector cache
    vectorCache.keySet().retainAll(activeCacheKeys);

    VocabIndex defaultVocab = buildVocabIndex(termDocLists, termTfLists);

    LanguageOverlay[] langOverlays = new LanguageOverlay[nLangs];
    int activeLanguages = 0;
    for (int lang = 0; lang < nLangs; lang++) {
      if (langDocIndices[lang] == null || langDocIndices[lang].isEmpty()) continue;
      List<Integer> dList = langDocIndices[lang];
      List<String> tList = langTitlesList[lang];
      List<float[]> vList = langVectorsList[lang];
      int k = dList.size();
      int[] docArr = new int[k];
      String[] titleArr = new String[k];
      float[] vecArr = new float[k * dim];
      for (int i = 0; i < k; i++) {
        docArr[i] = dList.get(i);
        titleArr[i] = tList.get(i);
        System.arraycopy(vList.get(i), 0, vecArr, i * dim, dim);
      }
      VocabIndex langVocab = buildVocabIndex(langTermDocLists[lang], langTermTfLists[lang]);
      langOverlays[lang] = new LanguageOverlay(docArr, titleArr, vecArr, langVocab);
      activeLanguages++;
    }

    float avgDocLen = nDocs > 0 ? (float) (totalDocLen / nDocs) : 1.0f;
    currentSnapshot =
        new IndexSnapshot(
            datasetIDs,
            lowerDatasetIDs,
            defaultTitles,
            idToDocIndex,
            defaultVocab,
            docLengths,
            avgDocLen,
            flatVectors,
            dim,
            langOverlays);

    String2.log(
        "HybridSearchEngine.rebuild() finished: nDatasets="
            + nDocs
            + " vocabSize="
            + defaultVocab.postings.size()
            + " localizedLangs="
            + activeLanguages
            + " localizedDocs="
            + localizedEntryCount
            + " dim="
            + dim
            + " time="
            + (System.currentTimeMillis() - t0)
            + "ms");
  }

  private static float[] getOrComputeEmbedding(
      String cacheKey, String semanticText, StaticEmbeddingModel model, int dim) {
    long hashHigh = fnv1a64High(semanticText);
    long hashLow = fnv1a64Low(semanticText);
    CachedVector cached = vectorCache.get(cacheKey);
    if (cached != null
        && cached.hashHigh == hashHigh
        && cached.hashLow == hashLow
        && cached.unitVector.length == dim) {
      return cached.unitVector;
    }
    float[] vec = model.embed(semanticText);
    vectorCache.put(cacheKey, new CachedVector(hashHigh, hashLow, vec));
    return vec;
  }

  private static VocabIndex buildVocabIndex(
      Map<String, List<Integer>> termDocLists, Map<String, List<Float>> termTfLists) {
    Map<String, PostingList> postings =
        new HashMap<>(Math.max(16, (int) (termDocLists.size() * 1.4f)));
    int maxLen = 0;
    for (String term : termDocLists.keySet()) {
      if (term.length() > maxLen) maxLen = term.length();
    }
    @SuppressWarnings("unchecked")
    List<String>[] buckets = new List[Math.min(maxLen + 1, 65)];

    for (Map.Entry<String, List<Integer>> entry : termDocLists.entrySet()) {
      String term = entry.getKey();
      List<Integer> dList = entry.getValue();
      List<Float> fList = termTfLists.get(term);
      int m = dList.size();
      int[] docs = new int[m];
      float[] tfs = new float[m];
      for (int i = 0; i < m; i++) {
        docs[i] = dList.get(i);
        tfs[i] = fList.get(i);
      }
      postings.put(term, new PostingList(docs, tfs));

      if (term.length() < buckets.length) {
        if (buckets[term.length()] == null) buckets[term.length()] = new ArrayList<>();
        buckets[term.length()].add(term);
      }
    }

    String[][] vocabByLength = new String[buckets.length][];
    int[][] vocabMasksByLength = new int[buckets.length][];
    for (int len = 0; len < buckets.length; len++) {
      if (buckets[len] == null) {
        vocabByLength[len] = new String[0];
        vocabMasksByLength[len] = new int[0];
      } else {
        String[] sorted = buckets[len].toArray(new String[0]);
        Arrays.sort(sorted);
        int[] masks = new int[sorted.length];
        for (int i = 0; i < sorted.length; i++) {
          masks[i] = computeCharMask(sorted[i]);
        }
        vocabByLength[len] = sorted;
        vocabMasksByLength[len] = masks;
      }
    }
    return new VocabIndex(postings, vocabByLength, vocabMasksByLength);
  }

  /**
   * Executes hybrid (fuzzy BM25 + semantic vector) search restricted to {@code candidateIDs} and
   * datasets accessible to {@code roles}, incorporating localized metadata when available for
   * {@code language}.
   */
  public static StringArray search(
      int language,
      String[] roles,
      StringArray candidateIDs,
      String searchFor,
      Map<String, EDDGrid> gridDatasets,
      Map<String, EDDTable> tableDatasets) {

    IndexSnapshot snap = currentSnapshot;
    if (snap == null || snap.datasetIDs.length == 0) {
      return new StringArray();
    }

    LanguageOverlay overlay =
        (language >= 0
                && language < snap.langOverlays.length
                && language != EDMessages.DEFAULT_LANGUAGE)
            ? snap.langOverlays[language]
            : null;

    DomainLexicon lexicon = getOrLoadLexicon();

    // Parse ERDDAP search syntax preserving original case and quote flags
    List<QueryTerm> queryTerms = parseQueryTerms(searchFor);
    List<String> positiveTokens = new ArrayList<>();
    List<String> strictSubstrings = new ArrayList<>(); // quoted phrases or key=value filters
    List<String> negativeSubstrings = new ArrayList<>();
    StringBuilder semanticQuery = new StringBuilder();

    for (QueryTerm qt : queryTerms) {
      String origPart = qt.text().trim();
      String lowerPart = origPart.toLowerCase(Locale.ROOT);
      if (lowerPart.isEmpty()) continue;

      if (qt.isNeg()) {
        negativeSubstrings.add(lowerPart);
      } else {
        if (qt.wasQuoted() || lowerPart.indexOf('=') >= 0) {
          strictSubstrings.add(lowerPart);
        }
        String normalizedOrig = origPart.replace('_', ' ');
        semanticQuery.append(lexicon.expandText(normalizedOrig, true, 6)).append(' ');
        for (String tok : tokenize(lowerPart, false)) {
          positiveTokens.add(tok);
        }
      }
    }

    int nDocs = snap.datasetIDs.length;
    QueryScratch scratch = queryScratch.get();
    scratch.prepare(nDocs);
    BitSet eligible = scratch.eligible;
    for (int i = 0; i < candidateIDs.size(); i++) {
      Integer idx = snap.idToDocIndex.get(candidateIDs.get(i));
      if (idx != null) eligible.set(idx);
    }
    if (eligible.isEmpty()) return new StringArray();

    // If query only had negative exclusions (e.g. "-DEPRECATED"), filter and sort by title
    if (positiveTokens.isEmpty() && strictSubstrings.isEmpty() && !negativeSubstrings.isEmpty()) {
      StringArray filtered = new StringArray();
      for (int docIdx = eligible.nextSetBit(0);
          docIdx >= 0;
          docIdx = eligible.nextSetBit(docIdx + 1)) {
        String id = snap.datasetIDs[docIdx];
        EDD edd = gridDatasets.get(id);
        if (edd == null) edd = tableDatasets.get(id);
        if (edd == null) continue;
        if (!EDStatic.config.listPrivateDatasets
            && !edd.isAccessibleTo(roles)
            && !edd.graphsAccessibleToPublic()) continue;
        if (!matchesStrictConstraints(edd, language, strictSubstrings, negativeSubstrings))
          continue;
        filtered.add(id);
      }
      return filtered;
    }

    // 1. Lexical + O(log V) Prefix + Bitmask-Pruned Substring/Fuzzy BM25 Scoring
    double[] bm25Scores = scratch.bm25;
    scoreVocabIndex(
        snap.defaultVocab,
        positiveTokens,
        bm25Scores,
        eligible,
        nDocs,
        snap.docLengths,
        snap.avgDocLength);
    if (overlay != null) {
      scoreVocabIndex(
          overlay.vocabIndex,
          positiveTokens,
          bm25Scores,
          eligible,
          nDocs,
          snap.docLengths,
          snap.avgDocLength);
    }

    // 2. Dense Vector Cosine Similarity Scan (uses localized vector when available, else default)
    StaticEmbeddingModel model = getOrLoadModel();
    float[] qVec = model.embed(semanticQuery.toString());
    int dim = snap.dim;
    float[] defaultFlat = snap.flatVectors;
    double[] vecScores = scratch.vec;

    for (int docIdx = eligible.nextSetBit(0);
        docIdx >= 0;
        docIdx = eligible.nextSetBit(docIdx + 1)) {
      float[] vecSource = defaultFlat;
      int base = docIdx * dim;
      if (overlay != null) {
        int localIdx = overlay.findLocalIndex(docIdx);
        if (localIdx >= 0) {
          vecSource = overlay.flatVectors;
          base = localIdx * dim;
        }
      }
      float dot = 0.0f;
      for (int d = 0; d < dim; d++) {
        dot += qVec[d] * vecSource[base + d];
      }
      vecScores[docIdx] = dot;
    }

    // 3. Build rank lists and fuse via Reciprocal Rank Fusion (RRF)
    List<Integer> lexRanked = new ArrayList<>();
    List<Integer> vecRanked = new ArrayList<>();
    for (int docIdx = eligible.nextSetBit(0);
        docIdx >= 0;
        docIdx = eligible.nextSetBit(docIdx + 1)) {
      if (bm25Scores[docIdx] > 0.0) lexRanked.add(docIdx);
      if (vecScores[docIdx] > 0.22) vecRanked.add(docIdx);
    }

    lexRanked.sort((a, b) -> Double.compare(bm25Scores[b], bm25Scores[a]));
    vecRanked.sort((a, b) -> Double.compare(vecScores[b], vecScores[a]));

    double[] rrfScores = scratch.rrf;
    BitSet candidates = scratch.candidates;

    // Keep ALL lexical/prefix/fuzzy matches (no top-500 truncation)
    for (int r = 0; r < lexRanked.size(); r++) {
      int docIdx = lexRanked.get(r);
      rrfScores[docIdx] += 1.2 / (RRF_K + r + 1);
      candidates.set(docIdx);
    }

    int vecLimit = Math.min(vecRanked.size(), 250);
    for (int r = 0; r < vecLimit; r++) {
      int docIdx = vecRanked.get(r);
      if (bm25Scores[docIdx] > 0.0 || vecScores[docIdx] >= 0.34) {
        rrfScores[docIdx] += 0.8 / (RRF_K + r + 1);
        candidates.set(docIdx);
      }
    }

    // Guarantee 100% inclusion of any exact substring match (e.g. 1-2 char terms or punctuation)
    int nRaw = queryTerms.size();
    boolean[] isNegArr = new boolean[nRaw];
    byte[][] wordsB = new byte[nRaw][];
    int[][] jumpB = new int[nRaw][];
    for (int w = 0; w < nRaw; w++) {
      QueryTerm qt = queryTerms.get(w);
      isNegArr[w] = qt.isNeg();
      wordsB[w] = String2.stringToUtf8Bytes(qt.text().trim().toLowerCase(Locale.ROOT));
      jumpB[w] = String2.makeJumpTable(wordsB[w]);
    }
    for (int docIdx = eligible.nextSetBit(0);
        docIdx >= 0;
        docIdx = eligible.nextSetBit(docIdx + 1)) {
      if (candidates.get(docIdx)) continue;
      String id = snap.datasetIDs[docIdx];
      EDD edd = gridDatasets.get(id);
      if (edd == null) edd = tableDatasets.get(id);
      if (edd != null && edd.searchRank(language, isNegArr, wordsB, jumpB) < Integer.MAX_VALUE) {
        rrfScores[docIdx] += 1.0 / (RRF_K + lexRanked.size() + 1);
        candidates.set(docIdx);
      }
    }

    // Exact / substring datasetID boost
    String trimmedLowerQuery = searchFor.trim().toLowerCase(Locale.ROOT);
    for (int docIdx = eligible.nextSetBit(0);
        docIdx >= 0;
        docIdx = eligible.nextSetBit(docIdx + 1)) {
      String lowerId = snap.lowerDatasetIDs[docIdx];
      if (lowerId.equals(trimmedLowerQuery)) {
        rrfScores[docIdx] += 10.0;
        candidates.set(docIdx);
      } else if (trimmedLowerQuery.length() >= 4
          && trimmedLowerQuery.indexOf(' ') < 0
          && lowerId.contains(trimmedLowerQuery)) {
        rrfScores[docIdx] += 0.5;
        candidates.set(docIdx);
      }
    }

    // 4. Filter by ACLs, strict quoted/key=value/negation predicates, and sort descending by RRF
    List<Integer> finalDocs = new ArrayList<>();
    for (int docIdx = candidates.nextSetBit(0);
        docIdx >= 0;
        docIdx = candidates.nextSetBit(docIdx + 1)) {
      String id = snap.datasetIDs[docIdx];
      EDD edd = gridDatasets.get(id);
      if (edd == null) edd = tableDatasets.get(id);
      if (edd == null) continue;
      if (!EDStatic.config.listPrivateDatasets
          && !edd.isAccessibleTo(roles)
          && !edd.graphsAccessibleToPublic()) continue;

      if (!matchesStrictConstraints(edd, language, strictSubstrings, negativeSubstrings)) continue;

      String titleForLang = edd.title(language);
      if (titleForLang != null && titleForLang.contains("DEPRECATED")) {
        rrfScores[docIdx] *= 0.25;
      }
      finalDocs.add(docIdx);
    }

    final LanguageOverlay activeOverlay = overlay;
    finalDocs.sort(
        Comparator.comparingDouble((Integer idx) -> rrfScores[idx])
            .reversed()
            .thenComparing(
                idx -> resolveDisplayTitle(snap, activeOverlay, idx),
                String.CASE_INSENSITIVE_ORDER));

    StringArray out = new StringArray(finalDocs.size(), false);
    for (int docIdx : finalDocs) {
      out.add(snap.datasetIDs[docIdx]);
    }
    return out;
  }

  private static String resolveDisplayTitle(
      IndexSnapshot snap, LanguageOverlay overlay, int docIdx) {
    if (overlay != null) {
      int localIdx = overlay.findLocalIndex(docIdx);
      if (localIdx >= 0) return overlay.localizedTitles[localIdx];
    }
    return snap.defaultTitles[docIdx];
  }

  private static void scoreVocabIndex(
      VocabIndex vocab,
      List<String> positiveTokens,
      double[] bm25Scores,
      BitSet eligible,
      int nDocs,
      float[] docLengths,
      float avgDocLength) {
    for (String qTok : positiveTokens) {
      int qLen = qTok.length();
      int qMask = computeCharMask(qTok);

      PostingList exactPl = vocab.postings.get(qTok);
      if (exactPl != null) {
        accumulateBm25(bm25Scores, eligible, exactPl, nDocs, docLengths, avgDocLength, 1.0);
      }

      // O(log V) binary-search prefix expansion (e.g., "precip" -> "precipitation")
      if (qLen >= 3) {
        for (int len = qLen + 1; len < vocab.vocabByLength.length; len++) {
          String[] bucket = vocab.vocabByLength[len];
          if (bucket.length == 0) continue;
          int pos = Arrays.binarySearch(bucket, qTok);
          if (pos < 0) pos = -pos - 1;
          while (pos < bucket.length && bucket[pos].startsWith(qTok)) {
            PostingList pl = vocab.postings.get(bucket[pos]);
            if (pl != null) {
              accumulateBm25(bm25Scores, eligible, pl, nDocs, docLengths, avgDocLength, 0.85);
            }
            pos++;
          }
        }
      }

      // Bitmask-pruned interior substring expansion
      if (qLen >= 4) {
        for (int len = qLen + 1; len < vocab.vocabByLength.length; len++) {
          String[] bucket = vocab.vocabByLength[len];
          int[] masks = vocab.vocabMasksByLength[len];
          for (int i = 0; i < bucket.length; i++) {
            if ((masks[i] & qMask) != qMask) continue;
            String cand = bucket[i];
            if (cand.startsWith(qTok)) continue;
            if (cand.contains(qTok)) {
              PostingList pl = vocab.postings.get(cand);
              if (pl != null) {
                accumulateBm25(bm25Scores, eligible, pl, nDocs, docLengths, avgDocLength, 0.70);
              }
            }
          }
        }
      }

      // Bitmask-pruned Damerau-Levenshtein fuzzy expansion for typos
      if (qLen >= 4) {
        int maxDist = qLen >= 7 ? 2 : 1;
        int minLen = Math.max(3, qLen - maxDist);
        int maxLen = Math.min(vocab.vocabByLength.length - 1, qLen + maxDist);
        for (int len = minLen; len <= maxLen; len++) {
          String[] bucket = vocab.vocabByLength[len];
          int[] masks = vocab.vocabMasksByLength[len];
          for (int i = 0; i < bucket.length; i++) {
            int cMask = masks[i];
            if (Integer.bitCount(qMask & ~cMask) > maxDist
                || Integer.bitCount(cMask & ~qMask) > maxDist) {
              continue;
            }
            String cand = bucket[i];
            if (cand.equals(qTok) || cand.startsWith(qTok)) continue;
            if (len > qLen && cand.contains(qTok)) continue;
            int dist = boundedDamerauLevenshtein(qTok, cand, maxDist);
            if (dist > 0 && dist <= maxDist) {
              PostingList pl = vocab.postings.get(cand);
              if (pl != null) {
                double fuzzyWeight = 0.65 / (1.0 + dist);
                accumulateBm25(
                    bm25Scores, eligible, pl, nDocs, docLengths, avgDocLength, fuzzyWeight);
              }
            }
          }
        }
      }
    }
  }

  private static int computeCharMask(String s) {
    int mask = 0;
    int n = s.length();
    for (int i = 0; i < n; i++) {
      char c = s.charAt(i);
      if (c >= 'a' && c <= 'z') {
        mask |= (1 << (c - 'a'));
      } else if (c >= '0' && c <= '9') {
        mask |= (1 << (26 + ((c - '0') % 5)));
      } else {
        mask |= (1 << 31);
      }
    }
    return mask;
  }

  private static long fnv1a64High(String s) {
    long h = 0xcbf29ce484222325L;
    int n = s.length();
    for (int i = 0; i < n; i++) {
      h ^= s.charAt(i);
      h *= 0x100000001b3L;
    }
    return h;
  }

  private static long fnv1a64Low(String s) {
    long h = 0x84222325cbf29ce4L;
    int n = s.length();
    for (int i = n - 1; i >= 0; i--) {
      h ^= (s.charAt(i) * 31L + i);
      h *= 0x100000001b3L;
    }
    return h;
  }

  private static boolean matchesStrictConstraints(
      EDD edd, int language, List<String> strictSubstrings, List<String> negativeSubstrings) {
    if (strictSubstrings.isEmpty() && negativeSubstrings.isEmpty()) return true;
    byte[] searchBytes = edd.searchBytes(language);
    for (String req : strictSubstrings) {
      byte[] needle = String2.stringToUtf8Bytes(req);
      if (String2.indexOf(searchBytes, needle, String2.makeJumpTable(needle)) < 0) return false;
    }
    for (String neg : negativeSubstrings) {
      byte[] needle = String2.stringToUtf8Bytes(neg);
      if (String2.indexOf(searchBytes, needle, String2.makeJumpTable(needle)) >= 0) return false;
    }
    return true;
  }

  private static void accumulateBm25(
      double[] scores,
      BitSet eligible,
      PostingList pl,
      int nDocs,
      float[] docLengths,
      float avgDocLen,
      double weight) {
    int df = pl.docIndices.length;
    double idf = Math.log(1.0 + (nDocs - df + 0.5) / (df + 0.5));
    int[] docs = pl.docIndices;
    float[] tfs = pl.weightedTfs;
    for (int i = 0; i < df; i++) {
      int docIdx = docs[i];
      if (!eligible.get(docIdx)) continue;
      double tf = tfs[i];
      double denom = tf + BM25_K1 * (1.0 - BM25_B + BM25_B * (docLengths[docIdx] / avgDocLen));
      scores[docIdx] += weight * idf * ((tf * (BM25_K1 + 1.0)) / denom);
    }
  }

  private static void addWeightedTokens(
      Map<String, Float> tfMap, String text, float weight, boolean keepRawId) {
    if (text == null || text.isEmpty()) return;
    if (keepRawId) {
      String raw = text.trim().toLowerCase(Locale.ROOT);
      if (raw.length() >= 2) tfMap.merge(raw, weight * 1.5f, Float::sum);
    }
    for (String tok : tokenize(text, true)) {
      tfMap.merge(tok, weight, Float::sum);
    }
  }

  /**
   * Splits underscores, punctuation, and camelCase/digit boundaries (e.g., jplMURSST41 -> jpl, mur,
   * sst, 41).
   */
  private static List<String> tokenize(String text, boolean splitCamelCase) {
    List<String> tokens = new ArrayList<>();
    int n = text.length();
    StringBuilder cur = new StringBuilder();
    for (int i = 0; i < n; i++) {
      char c = text.charAt(i);
      if (Character.isLetterOrDigit(c)) {
        if (splitCamelCase && cur.length() > 0) {
          char prev = cur.charAt(cur.length() - 1);
          boolean boundary =
              (Character.isLowerCase(prev) && Character.isUpperCase(c))
                  || (Character.isLetter(prev) && Character.isDigit(c))
                  || (Character.isDigit(prev) && Character.isLetter(c));
          if (boundary) {
            flushToken(cur, tokens);
          }
        }
        cur.append(Character.toLowerCase(c));
      } else {
        flushToken(cur, tokens);
      }
    }
    flushToken(cur, tokens);
    return tokens;
  }

  private static void flushToken(StringBuilder cur, List<String> tokens) {
    if (cur.length() >= 2) {
      String s = cur.toString();
      if (!isStopWord(s)) tokens.add(s);
    }
    cur.setLength(0);
  }

  private static boolean isStopWord(String s) {
    switch (s) {
      case "the":
      case "and":
      case "for":
      case "from":
      case "with":
      case "that":
      case "this":
      case "are":
      case "was":
      case "were":
      case "have":
      case "has":
      case "per":
      case "into":
      case "all":
        return true;
      default:
        return false;
    }
  }

  /** Bounded Damerau-Levenshtein distance with early exit if distance exceeds {@code maxDist}. */
  private static int boundedDamerauLevenshtein(String a, String b, int maxDist) {
    int la = a.length();
    int lb = b.length();
    if (Math.abs(la - lb) > maxDist) return maxDist + 1;

    int[][] dp = new int[la + 1][lb + 1];
    for (int i = 0; i <= la; i++) dp[i][0] = i;
    for (int j = 0; j <= lb; j++) dp[0][j] = j;

    for (int i = 1; i <= la; i++) {
      int rowMin = maxDist + 1;
      char ca = a.charAt(i - 1);
      for (int j = 1; j <= lb; j++) {
        char cb = b.charAt(j - 1);
        int cost = (ca == cb) ? 0 : 1;
        int val = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
        if (i > 1 && j > 1 && ca == b.charAt(j - 2) && a.charAt(i - 2) == cb) {
          val = Math.min(val, dp[i - 2][j - 2] + 1);
        }
        dp[i][j] = val;
        if (val < rowMin) rowMin = val;
      }
      if (rowMin > maxDist) return maxDist + 1;
    }
    return dp[la][lb];
  }

  private static DomainLexicon getOrLoadLexicon() {
    DomainLexicon lex = domainLexicon;
    if (lex != null) return lex;
    synchronized (lexiconLock) {
      if (domainLexicon == null) {
        domainLexicon = DomainLexicon.load();
      }
      return domainLexicon;
    }
  }

  /**
   * Lazily caches ERDDAP's OceanicAtmosphericAcronyms, OceanicAtmosphericVariableNames, and
   * CfToFromGcmd tables for domain-aware semantic enrichment at index and query time.
   */
  private static final class DomainLexicon {
    final Map<String, String> acronyms; // uppercase acronym -> fullName
    final Map<String, String> variableNames; // lowercase variableName -> fullName

    private DomainLexicon(Map<String, String> acronyms, Map<String, String> variableNames) {
      this.acronyms = acronyms;
      this.variableNames = variableNames;
    }

    static DomainLexicon load() {
      Map<String, String> acrMap = new HashMap<>();
      try {
        Table t = EDStatic.oceanicAtmosphericAcronymsTable();
        StringArray col0 = (StringArray) t.getColumn(0);
        StringArray col1 = (StringArray) t.getColumn(1);
        int n = t.nRows();
        for (int i = 0; i < n; i++) {
          String k = col0.get(i);
          String v = col1.get(i);
          if (k != null && !k.isEmpty() && v != null && !v.isEmpty()) {
            acrMap.putIfAbsent(k.trim().toUpperCase(Locale.ROOT), v.trim());
          }
        }
      } catch (Throwable t) {
        String2.log("WARNING: Could not load oceanicAtmosphericAcronymsTable: " + t);
      }

      Map<String, String> varMap = new HashMap<>();
      try {
        Table t = EDStatic.oceanicAtmosphericVariableNamesTable();
        StringArray col0 = (StringArray) t.getColumn(0);
        StringArray col1 = (StringArray) t.getColumn(1);
        int n = t.nRows();
        for (int i = 0; i < n; i++) {
          String k = col0.get(i);
          String v = col1.get(i);
          if (k != null && !k.isEmpty() && v != null && !v.isEmpty()) {
            varMap.putIfAbsent(k.trim().toLowerCase(Locale.ROOT), v.trim());
          }
        }
      } catch (Throwable t) {
        String2.log("WARNING: Could not load oceanicAtmosphericVariableNamesTable: " + t);
      }

      String2.log(
          "HybridSearchEngine loaded DomainLexicon (acronyms="
              + acrMap.size()
              + ", variableNames="
              + varMap.size()
              + ")");
      return new DomainLexicon(
          Collections.unmodifiableMap(acrMap), Collections.unmodifiableMap(varMap));
    }

    String lookupVariableName(String varName) {
      if (varName == null || varName.isEmpty()) return null;
      String lower = varName.trim().toLowerCase(Locale.ROOT);
      String full = variableNames.get(lower);
      if (full != null) return full;
      // Strip trailing digits (e.g., "sst1" -> "sst")
      int end = lower.length();
      while (end > 2 && Character.isDigit(lower.charAt(end - 1))) end--;
      if (end < lower.length()) {
        full = variableNames.get(lower.substring(0, end));
        if (full != null) return full;
      }
      if (varName.length() >= 3) {
        return acronyms.get(varName.trim().toUpperCase(Locale.ROOT));
      }
      return null;
    }

    String expandIdentifierTokens(String datasetId) {
      if (datasetId == null || datasetId.isEmpty()) return "";
      Set<String> expansions = new LinkedHashSet<>();
      int n = datasetId.length();
      StringBuilder cur = new StringBuilder();
      for (int i = 0; i <= n; i++) {
        char c = (i < n) ? datasetId.charAt(i) : '_';
        if (Character.isLetterOrDigit(c)) {
          if (cur.length() > 0) {
            char prev = cur.charAt(cur.length() - 1);
            boolean boundary =
                (Character.isLowerCase(prev) && Character.isUpperCase(c))
                    || (Character.isLetter(prev) && Character.isDigit(c))
                    || (Character.isDigit(prev) && Character.isLetter(c));
            if (boundary) {
              maybeAddTokenExpansion(cur.toString(), true, expansions);
              cur.setLength(0);
            }
          }
          cur.append(c);
        } else {
          if (cur.length() > 0) {
            maybeAddTokenExpansion(cur.toString(), true, expansions);
            cur.setLength(0);
          }
        }
      }
      return String.join(" ", expansions);
    }

    String expandText(String text, boolean allowVariableLookup, int maxExpansions) {
      if (text == null || text.isEmpty()) return "";
      Set<String> expansions = new LinkedHashSet<>();
      int n = text.length();
      int start = -1;
      for (int i = 0; i <= n; i++) {
        char c = (i < n) ? text.charAt(i) : ' ';
        if (Character.isLetterOrDigit(c)) {
          if (start < 0) start = i;
        } else if (start >= 0) {
          String rawTok = text.substring(start, i);
          start = -1;
          maybeAddTokenExpansion(rawTok, allowVariableLookup, expansions);
          if (expansions.size() >= maxExpansions) break;
        }
      }
      if (expansions.isEmpty()) return text;
      return text + " " + String.join(" ", expansions);
    }

    private void maybeAddTokenExpansion(
        String rawTok, boolean allowVariableLookup, Set<String> expansions) {
      int len = rawTok.length();
      if (len < 2 || len > 16) return;
      String upper = rawTok.toUpperCase(Locale.ROOT);
      if (isAmbiguousCommonWord(upper)) return;

      boolean isAllUpper = rawTok.equals(upper) && rawTok.chars().anyMatch(Character::isLetter);
      if (len >= 3 || isAllUpper) {
        String acrFull = acronyms.get(upper);
        if (acrFull != null) {
          expansions.add(acrFull);
          return;
        }
      }
      if (allowVariableLookup && len >= 3) {
        String varFull = variableNames.get(rawTok.toLowerCase(Locale.ROOT));
        if (varFull != null) {
          expansions.add(varFull);
        }
      }
    }

    void collectGcmdKeywords(String standardName, Set<String> outTerms) {
      if (standardName == null || standardName.isEmpty()) return;
      try {
        String[] gcmds = CfToFromGcmd.cfToGcmd(standardName);
        for (String gcmd : gcmds) {
          if (gcmd == null || gcmd.isEmpty()) continue;
          String cleaned = gcmd;
          if (cleaned.regionMatches(true, 0, "EARTH SCIENCE > ", 0, 16)) {
            cleaned = cleaned.substring(16);
          }
          cleaned = cleaned.replace('>', ' ').replace('/', ' ').trim();
          if (!cleaned.isEmpty()) {
            outTerms.add(cleaned);
          }
        }
      } catch (Throwable ignored) {
        // Safe fallback if CfToGcmd.txt is unavailable in stripped test environments
      }
    }

    private static boolean isAmbiguousCommonWord(String upper) {
      switch (upper) {
        case "US":
        case "IN":
        case "ON":
        case "AT":
        case "TO":
        case "OR":
        case "AS":
        case "IS":
        case "IT":
        case "NO":
        case "SO":
        case "AN":
        case "BE":
        case "HE":
        case "ME":
        case "MY":
        case "WE":
        case "BY":
        case "OF":
        case "UP":
        case "THE":
        case "AND":
        case "FOR":
        case "ARE":
        case "WAS":
        case "PER":
        case "ALL":
        case "DAY":
        case "END":
        case "USE":
        case "NEW":
        case "OLD":
        case "LOW":
        case "SET":
        case "RUN":
        case "AIR":
        case "SEA":
        case "ICE":
        case "DATA":
        case "TIME":
        case "DATE":
        case "YEAR":
        case "TYPE":
        case "NAME":
        case "FILE":
        case "GRID":
        case "LINE":
        case "LIST":
        case "MODE":
        case "MEAN":
        case "HIGH":
        case "RATE":
        case "REAL":
        case "ROWS":
        case "SIZE":
        case "STEP":
        case "TEST":
        case "TEXT":
        case "UNIT":
        case "USER":
        case "VIEW":
        case "WORK":
        case "ZONE":
          return true;
        default:
          return false;
      }
    }
  }

  private static StaticEmbeddingModel getOrLoadModel() {
    StaticEmbeddingModel m = embeddingModel;
    if (m != null) return m;
    synchronized (modelLock) {
      if (embeddingModel == null) {
        embeddingModel = StaticEmbeddingModel.load();
      }
      return embeddingModel;
    }
  }

  private record QueryTerm(String text, boolean wasQuoted, boolean isNeg) {}

  private static List<QueryTerm> parseQueryTerms(String searchFor) {
    List<QueryTerm> terms = new ArrayList<>();
    if (searchFor == null) return terms;
    int n = searchFor.length();
    int po = 0;
    while (po < n) {
      char ch = searchFor.charAt(po);
      if (String2.isWhite(ch) || ch == ',') {
        po++;
        continue;
      }
      boolean neg = false;
      if (ch == '-' && po + 1 < n && !String2.isWhite(searchFor.charAt(po + 1))) {
        neg = true;
        po++;
      }
      if (po < n && searchFor.charAt(po) == '"') {
        int po2 = po + 1;
        while (po2 < n) {
          if (searchFor.charAt(po2) == '"') {
            if (po2 + 1 < n && searchFor.charAt(po2 + 1) == '"') po2 += 2;
            else break;
          } else {
            po2++;
          }
        }
        String raw = String2.replaceAll(searchFor.substring(po + 1, po2), "\"\"", "\"");
        if (!raw.isEmpty()) terms.add(new QueryTerm(raw, true, neg));
        po = po2 + 1;
      } else {
        int po2 = po + 1;
        while (po2 < n && !String2.isWhite(searchFor.charAt(po2)) && searchFor.charAt(po2) != ',') {
          po2++;
        }
        String raw = searchFor.substring(po, po2);
        if (!raw.isEmpty()) terms.add(new QueryTerm(raw, false, neg));
        po = po2;
      }
    }
    return terms;
  }

  /**
   * Loads an int8-quantized static token embedding table (e.g., Model2Vec potion-base-8M) from
   * {@link #EMBEDDING_RESOURCE} if present, otherwise uses a zero-resource domain-lexicon + subword
   * trigram hash embedder.
   */
  private static final class StaticEmbeddingModel {
    final int dim;
    final Map<String, Integer> vocab;
    final byte[] quantizedWeights; // [vocabSize * dim]
    final float[] tokenScales; // [vocabSize]
    final boolean isExternalModel;

    private StaticEmbeddingModel(
        int dim,
        Map<String, Integer> vocab,
        byte[] quantizedWeights,
        float[] tokenScales,
        boolean isExternalModel) {
      this.dim = dim;
      this.vocab = vocab;
      this.quantizedWeights = quantizedWeights;
      this.tokenScales = tokenScales;
      this.isExternalModel = isExternalModel;
    }

    static StaticEmbeddingModel load() {
      try (InputStream raw = HybridSearchEngine.class.getResourceAsStream(EMBEDDING_RESOURCE)) {
        if (raw != null) {
          DataInputStream in = new DataInputStream(new BufferedInputStream(raw));
          int magic = in.readInt(); // 0x45524456 ("ERDV")
          if (magic == 0x45524456) {
            int vocabSize = in.readInt();
            int dim = in.readInt();
            Map<String, Integer> vocab = new HashMap<>((int) (vocabSize * 1.4f));
            byte[] weights = new byte[vocabSize * dim];
            float[] scales = new float[vocabSize];
            for (int i = 0; i < vocabSize; i++) {
              int len = in.readUnsignedShort();
              byte[] wordBytes = new byte[len];
              in.readFully(wordBytes);
              vocab.put(new String(wordBytes, StandardCharsets.UTF_8), i);
              scales[i] = in.readFloat();
              in.readFully(weights, i * dim, dim);
            }
            String2.log(
                "Loaded static embedding model ("
                    + vocabSize
                    + " tokens, dim="
                    + dim
                    + ") from "
                    + EMBEDDING_RESOURCE);
            return new StaticEmbeddingModel(dim, vocab, weights, scales, true);
          }
        }
      } catch (Throwable t) {
        String2.log(
            "WARNING: Could not load "
                + EMBEDDING_RESOURCE
                + ", using built-in semantic embedder: "
                + t);
      }
      return new StaticEmbeddingModel(FALLBACK_DIM, null, null, null, false);
    }

    float[] embed(String text) {
      float[] out = new float[dim];
      if (text == null || text.isEmpty()) return out;

      if (isExternalModel) {
        // WordPiece / subword greedy lookup against distilled Model2Vec vocabulary
        for (String word : tokenize(text, false)) {
          Integer wholeId = vocab.get(word);
          if (wholeId != null) {
            addRow(out, wholeId);
            continue;
          }
          int pos = 0;
          int n = word.length();
          while (pos < n) {
            int end = n;
            Integer subId = null;
            while (end > pos) {
              String piece =
                  (pos == 0) ? word.substring(pos, end) : "##" + word.substring(pos, end);
              subId = vocab.get(piece);
              if (subId != null) break;
              end--;
            }
            if (subId == null) {
              pos++;
            } else {
              addRow(out, subId);
              pos = end;
            }
          }
        }
      } else {
        // Built-in fallback using ERDDAP's acronym/variable tables + subword trigram projection
        String expanded = getOrLoadLexicon().expandText(text, true, 12);
        for (String tok : tokenize(expanded, true)) {
          projectToken(out, tok, 1.0f);
          String padded = "<" + tok + ">";
          for (int i = 0; i + 3 <= padded.length(); i++) {
            projectToken(out, padded.substring(i, i + 3), 0.35f);
          }
        }
      }

      // L2 normalize
      double normSq = 0.0;
      for (float v : out) normSq += (double) v * v;
      if (normSq > 1e-12) {
        float invNorm = (float) (1.0 / Math.sqrt(normSq));
        for (int i = 0; i < dim; i++) out[i] *= invNorm;
      }
      return out;
    }

    private void addRow(float[] out, int rowIdx) {
      float scale = tokenScales[rowIdx];
      int base = rowIdx * dim;
      for (int d = 0; d < dim; d++) {
        out[d] += quantizedWeights[base + d] * scale;
      }
    }

    private void projectToken(float[] out, String feat, float weight) {
      int h = feat.hashCode();
      int idx1 = (h & 0x7fffffff) % dim;
      int idx2 = ((h * 0x9e3779b9) & 0x7fffffff) % dim;
      float sign1 = ((h >>> 31) == 0) ? 1.0f : -1.0f;
      float sign2 = (((h >>> 15) & 1) == 0) ? 1.0f : -1.0f;
      out[idx1] += sign1 * weight;
      out[idx2] += sign2 * weight;
    }
  }
}
