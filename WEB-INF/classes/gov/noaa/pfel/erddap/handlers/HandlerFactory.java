package gov.noaa.pfel.erddap.handlers;

import static gov.noaa.pfel.erddap.LoadDatasets.tryToUnload;

import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.HashSet;

public class HandlerFactory {
  private static int nTry = 0;

  public static State getHandlerFor(
      String datasetType,
      String datasetID,
      String active,
      State completeState,
      SaxHandler saxHandler,
      SaxParsingContext context,
      boolean isTopLevelDataset) {
    if (skipDataset(datasetID, active, context, isTopLevelDataset)) {
      return new SkipDatasetHandler(saxHandler, completeState);
    }

    long timeToLoadThisDataset = System.currentTimeMillis();
    EDStatic.cldNTry = context.getNTryAndDatasets()[0];
    EDStatic.cldStartMillis = timeToLoadThisDataset;
    EDStatic.cldDatasetID = datasetID;

    if (EDStatic.useEddReflection) {
      // use reflection to discover handlers
      EDD.EDDClassInfo eddClassInfo = EDD.EDD_CLASS_INFO_MAP.get(datasetType);
      if (eddClassInfo == null || !eddClassInfo.hasSaxHandlerClass()) {
        throw new IllegalArgumentException("Unknown dataset type: " + datasetType);
      }

      nTry++;
      context.getNTryAndDatasets()[0] = nTry;

      // TODO using the first constructor here, should we scan through all to find the most
      // appropriate one?
      Constructor<?> constructor = eddClassInfo.getSaxHandlerClass().get().getConstructors()[0];

      // Constructors for handlers don't have uniform signatures so we have to do some investigation
      // TODO: standardize the constructor signatures or change to accept a config object?
      Parameter[] parameters = constructor.getParameters();
      if (parameters.length < 3
          || !parameters[0].getType().equals(SaxHandler.class)
          || !parameters[1].getType().equals(String.class)
          || !parameters[2].getType().equals(State.class)) {
        throw new IllegalArgumentException(
            "First constructor for " + datasetType + " did not have a valid signature");
      }

      // build array of constructor arguments based on the constructor signature
      Object[] constructorArgs = new Object[parameters.length];
      constructorArgs[0] = saxHandler;
      constructorArgs[1] = datasetID;
      constructorArgs[2] = completeState;
      for (int i = 3; i < parameters.length; i++) {
        Class<?> parameterClass = parameters[i].getType();
        if (parameterClass.equals(SaxParsingContext.class)) {
          constructorArgs[i] = context;
        } else if (parameterClass.equals(String.class)) {
          constructorArgs[i] = datasetType;
        } else {
          throw new IllegalArgumentException(
              "Unknown parameter type for " + datasetType + ": " + parameterClass);
        }
      }

      try {
        return (State) constructor.newInstance(constructorArgs);
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException("Error creating handler for " + datasetType, e);
      }
    } else {
      // legacy hardcoded approach
      nTry++;
      context.getNTryAndDatasets()[0] = nTry;
      switch (datasetType) {
        case "EDDTableFromErddap" -> {
          return new EDDTableFromErddapHandler(saxHandler, datasetID, completeState);
        }
        case "EDDTableFromEDDGrid" -> {
          return new EDDTableFromEDDGridHandler(saxHandler, datasetID, completeState, context);
        }
        case "EDDGridFromDap" -> {
          return new EDDGridFromDapHandler(saxHandler, datasetID, completeState);
        }
        case "EDDGridLonPM180" -> {
          return new EDDGridLonPM180Handler(saxHandler, datasetID, completeState, context);
        }
        case "EDDGridFromErddap" -> {
          return new EDDGridFromErddapHandler(saxHandler, datasetID, completeState);
        }
        case "EDDTableFromAsciiFiles",
            "EDDTableFromNcFiles",
            "EDDTableFromAudioFiles",
            "EDDTableFromAwsXmlFiles",
            "EDDTableFromColumnarAsciiFiles",
            "EDDTableFromHttpGet",
            "EDDTableFromInvalidCRAFiles",
            "EDDTableFromJsonlCSVFiles",
            "EDDTableFromMultidimNcFiles",
            "EDDTableFromNcCFFiles",
            "EDDTableFromNccsvFiles",
            "EDDTableFromHyraxFiles",
            "EDDTableFromThreddsFiles",
            "EDDTableFromWFSFiles",
            "EDDTableFromParquetFiles" -> {
          return new EDDTableFromFilesHandler(saxHandler, datasetID, completeState, datasetType);
        }
        case "EDDGridAggregateExistingDimension" -> {
          return new EDDGridAggregateExistingDimensionHandler(
              saxHandler, datasetID, completeState, context);
        }
        case "EDDGridCopy" -> {
          return new EDDGridCopyHandler(saxHandler, datasetID, completeState, context);
        }
        case "EDDGridFromEDDTable" -> {
          return new EDDGridFromEDDTableHandler(saxHandler, datasetID, completeState, context);
        }
        case "EDDGridLon0360" -> {
          return new EDDGridLon0360Handler(saxHandler, datasetID, completeState, context);
        }
        case "EDDGridSideBySide" -> {
          return new EDDGridSideBySideHandler(saxHandler, datasetID, completeState, context);
        }
        case "EDDTableAggregateRows" -> {
          return new EDDTableAggregateRowsHandler(saxHandler, datasetID, completeState, context);
        }
        case "EDDTableCopy" -> {
          return new EDDTableCopyHandler(saxHandler, datasetID, completeState, context);
        }
        case "EDDTableFromCassandra" -> {
          return new EDDTableFromCassandraHandler(saxHandler, datasetID, completeState);
        }
        case "EDDTableFromDapSequence" -> {
          return new EDDTableFromDapSequenceHandler(saxHandler, datasetID, completeState);
        }
        case "EDDTableFromDatabase" -> {
          return new EDDTableFromDatabaseHandler(saxHandler, datasetID, completeState);
        }
        case "EDDTableFromAsciiService" -> {
          return new EDDTableFromAsciiServiceHandler(
              saxHandler, datasetID, completeState, datasetType);
        }
        case "EDDTableFromOBIS" -> {
          return new EDDTableFromOBISHandler(saxHandler, datasetID, completeState);
        }
        case "EDDTableFromSOS" -> {
          return new EDDTableFromSOSHandler(saxHandler, datasetID, completeState);
        }
        case "EDDTableFromFileNames" -> {
          return new EDDTableFromFileNamesHandler(saxHandler, datasetID, completeState);
        }
        case "EDDGridFromAudioFiles",
            "EDDGridFromNcFiles",
            "EDDGridFromNcFilesUnpacked",
            "EDDGridFromMergeIRFiles" -> {
          return new EDDGridFromFilesHandler(saxHandler, datasetID, completeState, datasetType);
        }
        case "EDDGridFromEtopo" -> {
          return new EDDGridFromEtopoHandler(saxHandler, datasetID, completeState);
        }
        default -> {
          nTry--;
          context.getNTryAndDatasets()[0] = nTry;
          throw new IllegalArgumentException("Unknown dataset type: " + datasetType);
        }
      }
    }
  }

  public static boolean skipDataset(
      String datasetID, String active, SaxParsingContext context, boolean isTopLevelDataset) {
    boolean majorLoad = context.getMajorLoad();
    HashSet<String> orphanIDSet = context.getOrphanIDSet();
    HashSet<String> datasetIDSet = context.getDatasetIDSet();
    boolean reallyVerbose = context.getReallyVerbose();
    StringArray duplicateDatasetIDs = context.getDuplicateDatasetIDs();
    String datasetsRegex = context.getDatasetsRegex();
    Erddap erddap = context.getErddap();
    long lastLuceneUpdate = context.getLastLuceneUpdate();
    StringArray changedDatasetIDs = context.getChangedDatasetIDs();

    // erddap == null implies we are in a load one dataset situation, only check the regex.
    if (erddap == null) {
      return !datasetID.matches(datasetsRegex);
    }

    if (majorLoad) {
      orphanIDSet.remove(datasetID);
    }

    boolean skip = false;
    if (isTopLevelDataset) {
      boolean isDuplicate = !datasetIDSet.add(datasetID);
      if (isDuplicate) {
        skip = true;
        duplicateDatasetIDs.add(datasetID);
        if (reallyVerbose) {
          String2.log("*** skipping datasetID=" + datasetID + " because it's a duplicate.");
        }
      }
    }

    // Test second: skip dataset because of datasetsRegex?
    if (!skip && !datasetID.matches(datasetsRegex)) {
      skip = true;
      if (reallyVerbose)
        String2.log("*** skipping datasetID=" + datasetID + " because of datasetsRegex.");
    }

    // Test third: look at flag/age  or active=false
    if (!skip) {
      // always check both flag locations
      boolean isFlagged = File2.delete(EDStatic.fullResetFlagDirectory + datasetID);
      boolean isBadFilesFlagged = File2.delete(EDStatic.fullBadFilesFlagDirectory + datasetID);
      boolean isHardFlagged = File2.delete(EDStatic.fullHardFlagDirectory + datasetID);
      if (isFlagged) {
        String2.log(
            "*** reloading datasetID=" + datasetID + " because it was in the flag directory.");

      } else if (isBadFilesFlagged) {
        String2.log(
            "*** reloading datasetID="
                + datasetID
                + " because it was in the badFilesFlag directory.");
        EDD oldEdd = erddap.gridDatasetHashMap.get(datasetID);
        if (oldEdd == null) oldEdd = erddap.tableDatasetHashMap.get(datasetID);
        if (oldEdd != null) {
          StringArray childDatasetIDs = oldEdd.childDatasetIDs();
          for (int cd = 0; cd < childDatasetIDs.size(); cd++) {
            String cid = childDatasetIDs.get(cd);
            EDD.deleteBadFilesFile(cid); // delete the children's info
          }
        }
        EDD.deleteBadFilesFile(datasetID); // the important difference

      } else if (isHardFlagged) {
        String2.log(
            "*** reloading datasetID=" + datasetID + " because it was in the hardFlag directory.");
        EDD oldEdd = erddap.gridDatasetHashMap.get(datasetID);
        if (oldEdd == null) oldEdd = erddap.tableDatasetHashMap.get(datasetID);
        if (oldEdd != null) {
          StringArray childDatasetIDs = oldEdd.childDatasetIDs();
          for (int cd = 0; cd < childDatasetIDs.size(); cd++) {
            String cid = childDatasetIDs.get(cd);
            EDD.deleteCachedDatasetInfo(cid); // delete the children's info
            FileVisitorDNLS.pruneCache(
                EDD.decompressedDirectory(cid), 2, 0.5); // remove as many files as possible
          }
        }
        tryToUnload(erddap, datasetID, new StringArray(), true); // needToUpdateLucene
        EDD.deleteCachedDatasetInfo(datasetID); // the important difference
        FileVisitorDNLS.pruneCache(
            EDD.decompressedDirectory(datasetID), 2, 0.5); // remove as many files as possible

      } else {
        // does the dataset already exist and is young?
        EDD oldEdd = erddap.gridDatasetHashMap.get(datasetID);
        if (oldEdd == null) oldEdd = erddap.tableDatasetHashMap.get(datasetID);
        if (oldEdd != null) {
          long minutesOld =
              oldEdd.creationTimeMillis() <= 0
                  ? // see edd.setCreationTimeTo0
                  Long.MAX_VALUE
                  : (System.currentTimeMillis() - oldEdd.creationTimeMillis()) / 60000;
          if (minutesOld < oldEdd.getReloadEveryNMinutes()) {
            // it exists and is young
            if (reallyVerbose)
              String2.log(
                  "*** skipping datasetID="
                      + datasetID
                      + ": it already exists and minutesOld="
                      + minutesOld
                      + " is less than reloadEvery="
                      + oldEdd.getReloadEveryNMinutes());
            skip = true;
          }
        }
      }

      // active="false"?  (very powerful)
      boolean tActive = active == null || !active.equals("false");
      if (!tActive) {
        // marked not active now; was it active?
        boolean needToUpdateLucene =
            System.currentTimeMillis() - lastLuceneUpdate > 5 * Calendar2.MILLIS_PER_MINUTE;
        if (tryToUnload(erddap, datasetID, changedDatasetIDs, needToUpdateLucene)) {
          // yes, it was unloaded
          String2.log("*** unloaded datasetID=" + datasetID + " because active=\"false\".");
          if (needToUpdateLucene)
            lastLuceneUpdate = System.currentTimeMillis(); // because Lucene was updated
        }

        skip = true;
      }
    }
    return skip;
  }
}
