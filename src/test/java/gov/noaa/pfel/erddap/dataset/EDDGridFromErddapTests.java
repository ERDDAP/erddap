package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.String2;
import com.cohort.util.Test;
import tags.TagExternalERDDAP;
import testDataset.EDDTestDataset;

class EDDGridFromErddapTests {

  @org.junit.jupiter.api.Test
  @TagExternalERDDAP
  void testDataVarOrder() throws Throwable {
    String2.log("\n*** EDDGridFromErddap.testDataVarOrder()");
    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.gettestDataVarOrder();
    String results = String2.toCSSVString(eddGrid.dataVariableDestinationNames());
    String expected = "SST, mask, analysis_error";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);
  }

  /** This tests dealing with remote not having ioos_category, but local requiring it. */
  @org.junit.jupiter.api.Test
  void testGridNoIoosCat() throws Throwable {
    String2.log("\n*** EDDGridFromErddap.testGridNoIoosCat");

    // not active because no test dataset
    // this failed because trajectory didn't have ioos_category
    // EDDGrid edd = (EDDGrid)oneFromDatasetsXml(null, "testGridNoIoosCat");

  }
}
