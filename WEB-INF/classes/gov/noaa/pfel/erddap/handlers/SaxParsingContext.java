package gov.noaa.pfel.erddap.handlers;

import com.cohort.array.StringArray;
import gov.noaa.pfel.erddap.Erddap;
import java.util.HashMap;
import java.util.HashSet;

public class SaxParsingContext {
  private int[] nTryAndDatasets;
  private StringArray changedDatasetIDs;
  private HashSet<String> orphanIDSet;
  private HashSet<String> datasetIDSet;
  private StringArray duplicateDatasetIDs;
  private StringBuilder datasetsThatFailedToLoadSB;
  private StringBuilder warningsFromLoadDatasets;
  private HashMap<String, Object[]> tUserHashMap;
  private boolean majorLoad;
  private Erddap erddap;
  private long lastLuceneUpdate;
  private String datasetsRegex;
  private boolean reallyVerbose;
  private StringBuilder failedDatasetsWithErrorsSB;

  public SaxParsingContext() {}

  public int[] getNTryAndDatasets() {
    return nTryAndDatasets;
  }

  public void setNTryAndDatasets(int[] nTryAndDatasets) {
    this.nTryAndDatasets = nTryAndDatasets;
  }

  public StringArray getChangedDatasetIDs() {
    return changedDatasetIDs;
  }

  public void setChangedDatasetIDs(StringArray changedDatasetIDs) {
    this.changedDatasetIDs = changedDatasetIDs;
  }

  public HashSet<String> getOrphanIDSet() {
    return orphanIDSet;
  }

  public void setOrphanIDSet(HashSet<String> orphanIDSet) {
    this.orphanIDSet = orphanIDSet;
  }

  public HashSet<String> getDatasetIDSet() {
    return datasetIDSet;
  }

  public void setDatasetIDSet(HashSet<String> datasetIDSet) {
    this.datasetIDSet = datasetIDSet;
  }

  public StringArray getDuplicateDatasetIDs() {
    return duplicateDatasetIDs;
  }

  public void setDuplicateDatasetIDs(StringArray duplicateDatasetIDs) {
    this.duplicateDatasetIDs = duplicateDatasetIDs;
  }

  public StringBuilder getWarningsFromLoadDatasets() {
    return warningsFromLoadDatasets;
  }

  public void setWarningsFromLoadDatasets(StringBuilder warningsFromLoadDatasets) {
    this.warningsFromLoadDatasets = warningsFromLoadDatasets;
  }

  public HashMap<String, Object[]> gettUserHashMap() {
    return tUserHashMap;
  }

  public void settUserHashMap(HashMap<String, Object[]> tUserHashMap) {
    this.tUserHashMap = tUserHashMap;
  }

  public boolean getMajorLoad() {
    return majorLoad;
  }

  public void setMajorLoad(boolean majorLoad) {
    this.majorLoad = majorLoad;
  }

  public Erddap getErddap() {
    return erddap;
  }

  public void setErddap(Erddap erddap) {
    this.erddap = erddap;
  }

  public long getLastLuceneUpdate() {
    return lastLuceneUpdate;
  }

  public void setLastLuceneUpdate(long lastLuceneUpdate) {
    this.lastLuceneUpdate = lastLuceneUpdate;
  }

  public String getDatasetsRegex() {
    return datasetsRegex;
  }

  public void setDatasetsRegex(String datasetsRegex) {
    this.datasetsRegex = datasetsRegex;
  }

  public boolean getReallyVerbose() {
    return reallyVerbose;
  }

  public void setReallyVerbose(boolean reallyVerbose) {
    this.reallyVerbose = reallyVerbose;
  }

  public StringBuilder getDatasetsThatFailedToLoadSB() {
    return datasetsThatFailedToLoadSB;
  }

  public void setDatasetsThatFailedToLoadSB(StringBuilder datasetsThatFailedToLoadSB) {
    this.datasetsThatFailedToLoadSB = datasetsThatFailedToLoadSB;
  }

  public StringBuilder getFailedDatasetsWithErrorsSB() {
    return failedDatasetsWithErrorsSB;
  }

  public void setFailedDatasetsWithErrorsSB(StringBuilder failedDatasetsWithErrors) {
    this.failedDatasetsWithErrorsSB = failedDatasetsWithErrors;
  }
}
