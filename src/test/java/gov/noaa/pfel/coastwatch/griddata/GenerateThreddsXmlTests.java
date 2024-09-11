package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.StringArray;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import tags.TagMissingFile;

class GenerateThreddsXmlTests {
  /**
   * This tests shortening all the boldTitles in DataSet.properties, which is only useful while
   * testing shortenBoldTitles.
   */
  @org.junit.jupiter.api.Test
  void testShortenBoldTitles() {
    String2.log("\n*** GenerateThreddsXml.testShortenBoldTitles");
    ResourceBundle2 dataSetRB2 = new ResourceBundle2("gov.noaa.pfel.coastwatch.DataSet");
    String validDataSets[] = String2.split(dataSetRB2.getString("validDataSets", null), '`');
    // String2.log("validDataSets n=" + validDataSets.length);
    for (int i = 0; i < validDataSets.length; i++)
      GenerateThreddsXml.shortenBoldTitle(
          dataSetRB2.getString(validDataSets[i].substring(1) + "BoldTitle", null));
    // String2.log(GenerateThreddsXml.shortenBoldTitle(dataSetRB2.getString(
    //     validDataSets[i].substring(1) + "BoldTitle", null)));
  }

  /** This does a little test of this class -- specific to Bob's computer. */
  @org.junit.jupiter.api.Test
  @TagMissingFile
  void basicTest() throws Exception {
    // verbose = true;
    StringArray sa =
        GenerateThreddsXml.generateThreddsXml(
            "c:/u00/",
            "satellite/",
            "C:/programs/_tomcat/webapps/cwexperimental/WEB-INF/incompleteMainCatalog.xml",
            "c:/u00/xml/");
    String2.log("first catalog.xml=" + sa.get(0));
    //        Test.displayInBrowser("file://" + sa.get(0));  //.xml
    //        Test.displayInBrowser("file://f:/u00/xml/catalog.xml");
  }
}
