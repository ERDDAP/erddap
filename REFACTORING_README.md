# Dataset Type Refactoring - Minimal Implementation

## What This Solves

1. ✅ **Use reflection to add new dataset types** - No need to modify GenerateDatasetsXml switch
2. ✅ **JSON input support** - Alternative to interactive Q&A mode
3. ✅ **XInclude XML output** - Write each dataset to its own file

## Files Created

- `DatasetTypeRegistry.java` - Simple registry using reflection (58 lines)
- `JsonInputHelper.java` - JSON parameter parsing (39 lines)
- `XmlOutputHelper.java` - Single file or XInclude output (50 lines)
- Modified `GenerateDatasetsXml.java` - Added registry check in default case (4 lines changed)

**Total: 3 new files, 1 modified file, ~150 lines of code**

## How to Add a New Dataset Type (Without Modifying GenerateDatasetsXml)

### Step 1: Create Your Dataset Class

```java
public class EDDTableFromMySource extends EDDTable {
  
  static {
    // Register on class load
    DatasetTypeRegistry.register(
      "EDDTableFromMySource",
      EDDTableFromMySource.class,
      "generateDatasetsXml"
    );
  }
  
  public static String generateDatasetsXml(String url, int reload, ...) throws Throwable {
    // Your implementation
    return "<dataset type=\"EDDTableFromMySource\">...</dataset>";
  }
}
```

### Step 2: That's It!

The registry will handle discovery. No need to modify GenerateDatasetsXml.

## JSON Input Example

Create a config file `dataset-config.json`:

```json
{
  "datasetType": "EDDGridFromNcFiles",
  "parentDirectory": "/data/nc",
  "fileNameRegex": ".*\\.nc",
  "reloadEveryNMinutes": 1440
}
```

Use it:

```java
JSONObject config = new JSONObject(File2.readFromFile("dataset-config.json")[1]);
String dir = JsonInputHelper.getString(config, "parentDirectory", "");
String regex = JsonInputHelper.getString(config, "fileNameRegex", ".*\\.nc");
int reload = JsonInputHelper.getInt(config, "reloadEveryNMinutes", 1440);

String xml = EDDGridFromNcFiles.generateDatasetsXml(dir, regex, ...);
```

## XInclude Output Example

### Single File (Traditional)

```java
String xml = generateDataset(...);
XmlOutputHelper.writeToSingleFile(xml, "datasets.xml");
```

### Multi-File with XInclude

```java
String xml = generateDataset(...);
XmlOutputHelper.writeWithXInclude(
  xml,
  "myDatasetId",
  "content/erddap/datasets",    // directory for individual files
  "content/erddap/datasets.xml"  // main file with XInclude refs
);
```

This creates:
- `datasets.xml` with `<xi:include href="datasets/myDatasetId.xml" />`
- `datasets/myDatasetId.xml` with the actual dataset XML

## Migration Path

**Current:** All 40+ dataset types in switch statement ✅ Still works!

**New Types:** Just register them - no switch modification needed ✅

**Gradual:** Migrate existing types one-by-one as needed ✅

## Example: Adding a New Type

```java
public class EDDTableFromAPI extends EDDTable {
  
  static {
    DatasetTypeRegistry.register("EDDTableFromAPI", 
                                 EDDTableFromAPI.class, 
                                 "generateDatasetsXml");
  }
  
  public static String generateDatasetsXml(String apiUrl, String apiKey) {
    return "<dataset type=\"EDDTableFromAPI\">" +
           "  <sourceUrl>" + apiUrl + "</sourceUrl>" +
           "  <apiKey>" + apiKey + "</apiKey>" +
           "</dataset>";
  }
}
```

Now in GenerateDatasetsXml, when someone enters "EDDTableFromAPI", it will be found in the registry!

## Summary

**Before:** To add a new dataset type, you had to:
1. Create the class
2. Modify GenerateDatasetsXml switch (add ~50 lines)
3. Add parameter prompts
4. Wire everything together

**After:** To add a new dataset type:
1. Create the class with `static { register(...) }` block
2. Done!

**Code changed:** 4 lines in GenerateDatasetsXml + 3 simple utility classes

**Backward compatible:** All existing dataset types still work unchanged
