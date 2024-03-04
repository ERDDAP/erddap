package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.PrimitiveArray;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;

class EDDGridFromNcFilesTests {
  /** This prints time, lat, and lon values from an .ncml dataset. */
  public static String dumpTimeLatLon(String ncmlName) throws Exception {

    String latName = "latitude";
    String lonName = "longitude";
    StringBuilder sb = new StringBuilder();
    sb.append("ncmlName=" + ncmlName + "\n");
    NetcdfFile nc = NcHelper.openFile(ncmlName);
    try {
      Variable v = nc.findVariable(latName);
      PrimitiveArray pa = NcHelper.getPrimitiveArray(v);
      sb.append(latName +
          " [0]=" + pa.getString(0) +
          " [1]=" + pa.getString(1) +
          " [" + (pa.size() - 1) + "]=" + pa.getString(pa.size() - 1) + "\n");

      v = nc.findVariable(lonName);
      pa = NcHelper.getPrimitiveArray(v);
      sb.append(lonName +
          " [0]=" + pa.getString(0) +
          " [1]=" + pa.getString(1) +
          " [" + (pa.size() - 1) + "]=" + pa.getString(pa.size() - 1) + "\n");

      v = nc.findVariable("time");
      pa = NcHelper.getPrimitiveArray(v);
      sb.append("time" +
          " [0]=" + pa.getString(0) +
          " [1]=" + pa.getString(1) +
          " [" + (pa.size() - 1) + "]=" + pa.getString(pa.size() - 1) + "\n");

    } catch (Throwable t) {
      String2.log(sb.toString());
      String2.log(MustBe.throwableToString(t));
    } finally {
      try {
        if (nc != null)
          nc.close();
      } catch (Exception e9) {
      }
    }

    return sb.toString();
  }

  /** test reading an .ncml file */
  @org.junit.jupiter.api.Test
  void testNcml() throws Throwable {

    // String2.log("\n*** EDDGridFromNcFiles.testNcml");
    int language = 0;

    // 2022-02-07 These were the simplest test I could create to demonstrate
    // problems reading .ncml file
    // when using individual netcdf modules, instead of netcdf-java.
    // String2.log(NcHelper.ncdump(EDDGridFromNcFilesTests.class.getResource("/largeFiles/viirs/MappedMonthly4km/V20120012012031.L3m_MO_NPP_CHL_chlor_a_4km").getPath(),
    // "-h")); //suceeds. an hdf5 file
    // String2.log(NcHelper.ncdump(EDDGridFromNcFilesTests.class.getResource("/largeFiles/viirs/MappedMonthly4km/V20120322012060.L3m_MO_NPP_CHL_chlor_a_4km").getPath(),
    // "-h")); //suceeds. an hdf5 file
    // String2.log(NcHelper.ncdump(EDDGridFromNcFilesTests.class.getResource("/largeFiles/viirs/MappedMonthly4km/LatLon.nc").getPath(),
    // "-h"));
    // String2.log(NcHelper.ncdump(EDDGridFromNcFilesTests.class.getResource("/largeFiles/viirs/MappedMonthly4km/m4.ncml").getPath(),
    // "-h")); // fails

    // 2013-10-25 netcdf-java's reading of the source HDF4 file changed
    // dramatically.
    // Attributes that had internal spaces now have underscores.
    // So the previous m4.ncml was making changes to atts that no longer existed
    // and making changes that no longer need to be made.
    // So I renamed old version as m4R20131025.ncml and revised m4.ncml.
    // String2.log("\nOne of the source files that will be aggregated and
    // modified:\n" +
    // NcHelper.ncdump(
    // "/u00/data/viirs/MappedMonthly4km/V20120012012031.L3m_MO_NPP_CHL_chlor_a_4km",
    // "-h") + "\n");

    // 2022-07-07 code in section below fails after I tried switching to netcdf
    // modules,
    // so try to isolate problem so reportable to netcdf-java people.
    // The exception is (Zarr?!)
    /*
     * Exception in thread "main" java.lang.NullPointerException: Cannot invoke
     * "String.contains(java.lang.CharSequence)" because "location" is null
     * at thredds.inventory.zarr.MFileZip$Provider.canProvide(MFileZip.java:200)
     * at thredds.inventory.MFiles.create(MFiles.java:37)
     * at ucar.nc2.internal.ncml.AggDataset.<init>(AggDataset.java:74)
     * at ucar.nc2.internal.ncml.Aggregation.makeDataset(Aggregation.java:453)
     * at
     * ucar.nc2.internal.ncml.Aggregation.addExplicitDataset(Aggregation.java:136)
     * at ucar.nc2.internal.ncml.NcmlReader.readAgg(NcmlReader.java:1476)
     * at ucar.nc2.internal.ncml.NcmlReader.readNetcdf(NcmlReader.java:521)
     * at ucar.nc2.internal.ncml.NcmlReader.readNcml(NcmlReader.java:478)
     * at ucar.nc2.internal.ncml.NcmlReader.readNcml(NcmlReader.java:397)
     * at ucar.nc2.internal.ncml.NcmlNetcdfFileProvider.open(NcmlNetcdfFileProvider.
     * java:24)
     * at
     * ucar.nc2.dataset.NetcdfDatasets.openProtocolOrFile(NetcdfDatasets.java:431)
     * at ucar.nc2.dataset.NetcdfDatasets.openDataset(NetcdfDatasets.java:152)
     * at ucar.nc2.dataset.NetcdfDatasets.openDataset(NetcdfDatasets.java:135)
     * at ucar.nc2.dataset.NetcdfDatasets.openDataset(NetcdfDatasets.java:118)
     * at ucar.nc2.dataset.NetcdfDatasets.openDataset(NetcdfDatasets.java:104)
     */
    try {
      String fileName = EDDGridFromNcFilesTests.class.getResource("/largeFiles/viirs/MappedMonthly4km/m4.ncml")
          .getPath();
      NetcdfFile nc = NetcdfDatasets.openDataset(fileName);

      String results = dumpTimeLatLon(fileName);
      String expected = "ncmlName=" + fileName + "\n" +
          "latitude [0]=89.97916666666667 [1]=89.9375 [4319]=-89.97916666666664\n" +
          "longitude [0]=-179.97916666666666 [1]=-179.9375 [8639]=179.97916666666666\n" +
          "time [0]=15340 [1]=15371 [1]=15371\n";
      // "time [0]=15340.0 [1]=15371.0 [1]=15371.0\n"; //2021-01-07 with netcdf java
      // pre 5.4.1
      Test.ensureEqual(results, expected, "results=\n" + results);
    } catch (Exception e) {
      Test.knownProblem("2022-07-07 This fails with switch to netcdf v5.5.3 and modules (not netcdfAll). " +
          "I reported to netcdf-java people.", e);
    }
  }

}
