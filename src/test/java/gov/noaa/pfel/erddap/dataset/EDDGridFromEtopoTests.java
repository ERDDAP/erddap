package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Image2Tests;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import tags.TagImageComparison;
import testDataset.Initialization;

class EDDGridFromEtopoTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * This tests the methods in this class.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testBasic() throws Throwable {
    boolean doGraphicsTests = true;
    String2.log("\n****************** EDDGridFromEtopo.test() *****************\n");
    int language = 0;
    // verbose = true;
    // reallyVerbose = true;
    // GridDataAccessor.verbose = true;
    // GridDataAccessor.reallyVerbose = true;
    String name, tName, axisDapQuery, userDapQuery, results, expected, error;
    int tPo;
    EDDGridFromEtopo data180 = new EDDGridFromEtopo("etopo180", true, true, -1, true);
    EDDGridFromEtopo data360 = new EDDGridFromEtopo("etopo360", true, true, -1, true);
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to
    // check min:sec.

    // *** test getting .nc for entire dataset
    String2.log("\n****************** EDDGridFromEtopo test entire dataset\n");
    tName =
        data180.makeNewFileForDapQuery(
            language,
            null,
            null,
            "altitude[(-90):500:(90)][(-180):500:(180)]",
            EDStatic.fullTestCacheDirectory,
            data180.className() + "_Entire",
            ".nc");
    results = NcHelper.ncdump(EDStatic.fullTestCacheDirectory + tName, "");
    expected =
        // " latitude = 11;\n" + // (has coord.var)\n" + //changed when switched to
        // netcdf-java 4.0, 2009-02-23
        // " longitude = 22;\n" + // (has coord.var)\n" +
        "netcdf EDDGridFromEtopo_Entire.nc {\n"
            + "  dimensions:\n"
            + "    latitude = 22;\n"
            + "    longitude = 44;\n"
            + "  variables:\n"
            + "    double latitude(latitude=22);\n"
            + "      :_CoordinateAxisType = \"Lat\";\n"
            + "      :actual_range = -90.0, 85.0; // double\n"
            + "      :axis = \"Y\";\n"
            + "      :ioos_category = \"Location\";\n"
            + "      :long_name = \"Latitude\";\n"
            + "      :standard_name = \"latitude\";\n"
            + "      :units = \"degrees_north\";\n"
            + "\n"
            + "    double longitude(longitude=44);\n"
            + "      :_CoordinateAxisType = \"Lon\";\n"
            + "      :actual_range = -180.0, 178.33333333333331; // double\n"
            + "      :axis = \"X\";\n"
            + "      :ioos_category = \"Location\";\n"
            + "      :long_name = \"Longitude\";\n"
            + "      :standard_name = \"longitude\";\n"
            + "      :units = \"degrees_east\";\n"
            + "\n"
            + "    short altitude(latitude=22, longitude=44);\n"
            + "      :_FillValue = 32767S; // short\n"
            + "      :colorBarMaximum = 8000.0; // double\n"
            + "      :colorBarMinimum = -8000.0; // double\n"
            + "      :colorBarPalette = \"Topography\";\n"
            + "      :coordsys = \"geographic\";\n"
            + "      :ioos_category = \"Location\";\n"
            + "      :long_name = \"Altitude\";\n"
            + "      :missing_value = 32767S; // short\n"
            + "      :positive = \"up\";\n"
            + "      :standard_name = \"altitude\";\n"
            + "      :units = \"m\";\n"
            + "\n"
            + "  // global attributes:\n"
            + "  :acknowledgement = \"NOAA NGDC\";\n"
            + "  :cdm_data_type = \"Grid\";\n"
            + "  :contributor_name = \"GLOBE, SRTM30, Baltic Sea Bathymetry, Caspian Sea Bathymetry, Great Lakes Bathymetry, Gulf of California Bathymetry, IBCAO, JODC Bathymetry, Mediterranean Sea Bathymetry, U.S. Coastal Relief Model (CRM), Antarctica RAMP Topography, Antarctic Digital Database, GSHHS\";\n"
            + "  :contributor_role = \"source data\";\n"
            + "  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "  :creator_email = \"Barry.Eakins@noaa.gov \";\n"
            + "  :creator_name = \"NOAA NGDC\";\n"
            + "  :creator_url = \"https://www.ngdc.noaa.gov/mgg/global/global.html\";\n"
            + "  :data_source = \"NOAA NGDC ETOPO1\";\n"
            + "  :drawLandMask = \"under\";\n"
            + "  :Easternmost_Easting = 178.33333333333331; // double\n"
            + "  :geospatial_lat_max = 85.0; // double\n"
            + "  :geospatial_lat_min = -90.0; // double\n"
            + "  :geospatial_lat_resolution = 0.016666666666666666; // double\n"
            + "  :geospatial_lat_units = \"degrees_north\";\n"
            + "  :geospatial_lon_max = 178.33333333333331; // double\n"
            + "  :geospatial_lon_min = -180.0; // double\n"
            + "  :geospatial_lon_resolution = 0.016666666666666666; // double\n"
            + "  :geospatial_lon_units = \"degrees_east\";\n"
            + "  :history = \"2011-03-14 Downloaded https://www.ngdc.noaa.gov/mgg/global/relief/ETOPO1/data/ice_surface/grid_registered/binary/etopo1_ice_g_i2.zip\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // today + " (local file)\n" +
    // today +
    // http://127.0.0.1:8080/cwexperimental/
    expected =
        "griddap/etopo180.nc?altitude[(-90):500:(90)][(-180):500:(180)]\";\n"
            + "  :id = \"etopo180\";\n"
            + "  :infoUrl = \"https://www.ngdc.noaa.gov/mgg/global/global.html\";\n"
            + "  :institution = \"NOAA NGDC\";\n"
            + "  :keywords = \"Earth Science > Oceans > Bathymetry/Seafloor Topography > Bathymetry\";\n"
            + "  :keywords_vocabulary = \"GCMD Science Keywords\";\n"
            + "  :license = \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "  :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n"
            + "  :Northernmost_Northing = 85.0; // double\n"
            + "  :project = \"NOAA NGDC ETOPO\";\n"
            + "  :projection = \"geographic\";\n"
            + "  :projection_type = \"mapped\";\n"
            + "  :references = \"Amante, C. and B. W. Eakins, ETOPO1 1 Arc-Minute Global Relief Model: Procedures, Data Sources and Analysis. NOAA Technical Memorandum NESDIS NGDC-24, 19 pp, March 2009.\";\n"
            + "  :sourceUrl = \"(local file)\";\n"
            + "  :Southernmost_Northing = -90.0; // double\n"
            + "  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n"
            + "  :summary = \"ETOPO1 is a 1 arc-minute global relief model of Earth's surface that integrates land topography and ocean bathymetry. It was built from numerous global and regional data sets. This is the 'Ice Surface' version, with the top of the Antarctic and Greenland ice sheets. The horizontal datum is WGS-84, the vertical datum is Mean Sea Level. Keywords: Bathymetry, Digital Elevation. This is the grid/node-registered version: the dataset's latitude and longitude values mark the centers of the cells.\";\n"
            + "  :title = \"Topography, ETOPO1, 0.0166667 degrees, Global (longitude -180 to 180), (Ice Sheet Surface)\";\n"
            + "  :Westernmost_Easting = -180.0; // double\n"
            + "\n"
            + "  data:\n"
            + "    latitude = \n"
            + "      {-90.0, -81.66666666666667, -73.33333333333333, -65.0, -56.666666666666664, -48.333333333333336, -40.0, -31.666666666666664, -23.33333333333333, -15.0, -6.666666666666671, 1.6666666666666714, 10.0, 18.33333333333333, 26.66666666666667, 35.0, 43.33333333333334, 51.66666666666666, 60.0, 68.33333333333334, 76.66666666666666, 85.0}\n"
            + "    longitude = \n"
            + "      {-180.0, -171.66666666666666, -163.33333333333334, -155.0, -146.66666666666666, -138.33333333333334, -130.0, -121.66666666666666, -113.33333333333333, -105.0, -96.66666666666667, -88.33333333333333, -80.0, -71.66666666666667, -63.33333333333333, -55.0, -46.66666666666666, -38.33333333333334, -30.0, -21.666666666666657, -13.333333333333343, -5.0, 3.333333333333343, 11.666666666666657, 20.0, 28.333333333333343, 36.66666666666666, 45.0, 53.33333333333334, 61.66666666666666, 70.0, 78.33333333333331, 86.66666666666669, 95.0, 103.33333333333331, 111.66666666666669, 120.0, 128.33333333333331, 136.66666666666669, 145.0, 153.33333333333331, 161.66666666666669, 170.0, 178.33333333333331}\n"
            + "    altitude = \n"
            + "      {\n"
            + "        {2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745, 2745},\n"
            + "        {6, 5, 13, 21, 559, 438, 695, 974, 1502, 2090, 2074, 1613, 776, 300, 93, 105, 98, 685, 1199, 1419, 1862, 2005, 2269, 2498, 2699, 3012, 3328, 3578, 3728, 3860, 3996, 3981, 3764, 3620, 3359, 3161, 2954, 2654, 2454, 2159, 1703, 652, 2, 6},\n"
            + "        {-422, -3404, -3912, -4123, -4025, -3108, -515, -613, -520, -428, 181, 370, 134, 12, 1244, -351, -521, -2647, -3283, -1369, 394, 2092, 2728, 3217, 3051, 2838, 3016, 3162, 3019, 1802, 1121, 2765, 3286, 3484, 3150, 2967, 3094, 3128, 2715, 2377, 2279, 2392, -475, -445},\n"
            + "        {-3039, -2797, -3511, -3746, -3952, -4403, -4702, -4916, -4978, -4943, -4848, -4665, -4221, -3502, -364, -264, -4187, -4535, -4914, -5023, -5028, -5084, -2394, -4307, -4761, -5053, -4761, -3835, -3175, -3936, -2970, -3472, -3303, -126, -605, -2387, -2727, -1714, -1994, -3223, -3330, -2541, -2976, -3257},\n"
            + "        {-4976, -5262, -4172, -3519, -2926, -3181, -3796, -4143, -3954, -4447, -4379, -5352, -4337, -3780, -4085, -3901, -3943, -3031, -3371, -4253, -4342, -2035, -3724, -5167, -4986, -5452, -5293, -5122, -5306, -4855, -2507, -1878, -4368, -4215, -4172, -4495, -4772, -4691, -3596, -2729, -3593, -3803, -5183, -4882},\n"
            + "        {-766, -5237, -5045, -4865, -4378, -4578, -4477, -4362, -2536, -4073, -4439, -4663, -4146, 675, -117, -4804, -5965, -5718, -4473, -3878, -3225, -4258, -4255, -4063, -4502, -5275, -4391, -4005, -4052, -3706, -169, -3407, -3883, -3371, -3160, -3621, -3913, -3957, -3665, -4272, -4685, -4117, -970, -861},\n"
            + "        {-2927, -4585, -5026, -4797, -5346, -5791, -4835, -3879, -3254, -4051, -3533, -3657, -3816, 1273, 38, -1746, -5215, -5199, -4405, -3843, -2820, -3923, -4924, -4741, -5120, -3705, -4025, -2366, -3720, -5244, -4139, -2571, -4028, -3660, -4267, -4386, -4806, -5376, -4999, -62, -4512, -4779, -844, -2056},\n"
            + "        {-3172, -5393, -5980, -5254, -5122, -4357, -3357, -3651, -1960, -3391, -3797, -3625, -3978, -257, 215, 215, -3420, -4182, -3617, -4212, -2649, -4252, -4933, -4291, 1163, 811, -4296, -1964, -4625, -4959, -4362, -3042, -3036, -2759, -5328, -5063, 445, 101, 109, 153, -1707, -1271, -3578, -3646},\n"
            + "        {-2693, -5666, -5256, -4707, -5100, -4230, -3254, -3645, -3415, -3709, -3851, -4141, -4616, -5241, 246, 337, 792, -3600, -5402, -4905, -2670, -5007, -4896, -2976, 1278, 1021, -2426, 433, -4788, -3865, -3588, -4277, -3977, -5126, -5795, -3337, 587, 482, 245, 242, -362, -1213, -3357, -3889},\n"
            + "        {-2684, -4407, -5261, -4485, -2703, -4192, -3979, -3788, -3111, -3594, -3549, -4089, -4550, 4291, 222, 704, 787, -2614, -5086, -5702, -1958, -4242, -5516, -1873, 1238, 1142, 581, -3308, -4607, -2898, -3758, -4999, -1810, -4830, -5765, -5281, -1767, -1, -2, 298, -4406, -4525, -3417, -2582},\n"
            + "        {-5527, -5445, -5575, -5180, -4828, -4423, -4604, -4146, -4157, -3672, -3815, -4189, 8, 212, 80, 269, 388, 297, -5259, -5386, -3197, -5566, -5323, -447, 769, 752, 816, -4341, -3671, -3954, -3724, -5173, -5186, -5169, -2108, -17, -1447, -4530, -35, 968, -4665, -2382, -4009, -5351},\n"
            + "        {-5228, -5474, -5115, -4704, -4574, -4158, -4540, -4471, -3834, -3477, -3179, -2375, -1846, 253, 126, 360, -1395, -4061, -2935, -4568, -4994, -4993, -4156, 653, 367, 802, 1337, -383, -5118, -4273, -4292, -4360, -4400, -4677, 13, 177, -2743, -1367, -4195, -4535, -3506, -3836, -4289, -5484},\n"
            + "        {-6092, -5436, -4317, -5293, -5180, -4942, -4776, -4388, -3667, -3065, -3880, -3324, -2054, -4, 190, -3975, -4851, -4595, -5280, -5440, 96, 279, 404, 297, 402, 407, 2022, 1537, -4401, -4688, -4559, 136, -3487, -1703, -4, -3745, -1462, -5616, -4861, -2312, -5580, -4006, -4329, -5779},\n"
            + "        {-3550, -4399, -5336, -5153, -5104, -5183, -5005, -4012, -3610, -3045, 75, 0, -2633, 695, -617, -5174, -3521, -5411, -4672, -3222, 85, 315, 425, 434, 413, 413, 770, 967, 293, -3735, -2360, 527, -2374, 154, 162, -1669, -3185, -5566, -4961, -3806, -5715, -4897, -5213, -4229},\n"
            + "        {-5423, -4905, -5036, -5422, -5126, -4803, -4630, -4021, -28, 1711, -75, -2559, -109, -5371, -5410, -5361, -3859, -4882, -5320, -4790, 95, 451, 279, 573, 246, 129, 272, 671, -85, 1047, 78, 164, 237, 904, 2621, 170, 258, -170, -5407, -5115, -6032, -5967, -5755, -5363},\n"
            + "        {-3825, -5754, -5746, -5753, -5276, -5124, -4814, -1577, 1516, 1626, 280, 126, 99, -4362, -5055, -5358, -4366, -3269, -3366, -5175, -2363, 1098, 849, -18, -2988, -3029, 373, 465, 1170, 861, 2062, 5462, 4841, 4662, 3522, 380, -40, 22, -3, -5822, -6030, -4625, -5486, -3941},\n"
            + "        {-5940, -5485, -5674, -5728, -4484, -4174, -3303, 1732, 1590, 1452, 406, 315, 256, 154, -158, -4111, -4117, -4113, -2550, -3994, -5223, 301, 30, 497, 1048, -13, -2030, 404, 259, 62, 976, 2108, 3374, 1224, 1572, 931, 389, 550, -3602, 44, -5254, -5581, -1929, -5556},\n"
            + "        {-1623, -3786, -4787, -4647, -4516, -3631, -360, 1174, 862, 525, 232, 315, -3, 546, 597, -139, -4036, -3523, -3638, -2676, -941, 58, -14, 155, 197, 137, 240, 179, 114, 275, 323, 157, 963, 839, 910, 913, 709, 238, 75, -857, -711, -5607, -4438, -206},\n"
            + "        {-2641, -81, 4, 410, -87, 1609, 1123, 495, 272, 464, 176, -175, -67, 190, -174, -3295, -626, -2988, -1415, -2743, -909, -376, -244, 293, -1, -6, 151, 169, 181, 79, 58, 52, 183, 499, 339, 448, 272, 415, 773, 359, 197, 206, -39, -3018},\n"
            + "        {224, -51, 626, 1080, 1358, 457, 240, 463, -89, -1, -7, 49, -24, 276, -1464, -446, 1841, 2787, -26, -978, -1750, -3519, -1988, -144, 406, 250, 268, 110, -1, 104, 24, 19, 54, 1037, 310, 325, 322, 1708, 559, 31, 36, 30, 92, 795},\n"
            + "        {-1169, -2221, -901, -896, -3763, -3632, -2753, -21, -219, -77, -25, 333, 1071, -530, 1339, 2027, 2693, 2581, 2159, 17, -210, -2138, -3137, -1768, -191, -146, -200, -258, -235, -176, -200, -102, -27, -15, 243, -1, -60, -59, -25, -35, -43, -83, -253, -1181},\n"
            + "        {-2192, -1676, -1998, -1924, -2164, -1795, -2219, -1842, -1451, -1393, -1764, -1978, -1291, -626, -560, -1157, -2733, -2293, -1400, -1160, -3957, -3945, -3087, -3454, -4301, -3977, -3677, -3903, -3888, -3809, -3754, -3710, -3639, -3928, -3511, -4254, -4296, -4254, -4074, -2583, -1403, -3518, -3220, -2210}\n"
            + "      }\n"
            + "}\n";
    tPo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    tName =
        data360.makeNewFileForDapQuery(
            language,
            null,
            null,
            "altitude[(-90):2000:(90)][(0):2000:(360)]",
            EDStatic.fullTestCacheDirectory,
            data360.className() + "_Entire",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "latitude,longitude,altitude\n"
            + "degrees_north,degrees_east,m\n"
            + "-90.0,0.0,2745\n"
            + "-90.0,33.333333333333336,2745\n"
            + "-90.0,66.66666666666667,2745\n"
            + "-90.0,100.0,2745\n"
            + "-90.0,133.33333333333334,2745\n"
            + "-90.0,166.66666666666666,2745\n"
            + "-90.0,200.0,2745\n"
            + "-90.0,233.33333333333334,2745\n"
            + "-90.0,266.6666666666667,2745\n"
            + "-90.0,300.0,2745\n"
            + "-90.0,333.3333333333333,2745\n"
            + "-56.666666666666664,0.0,-4105\n"
            + "-56.666666666666664,33.333333333333336,-5355\n"
            + "-56.666666666666664,66.66666666666667,-2241\n"
            + "-56.666666666666664,100.0,-4082\n"
            + "-56.666666666666664,133.33333333333334,-4333\n"
            + "-56.666666666666664,166.66666666666666,-5031\n"
            + "-56.666666666666664,200.0,-4173\n"
            + "-56.666666666666664,233.33333333333334,-3863\n"
            + "-56.666666666666664,266.6666666666667,-5386\n"
            + "-56.666666666666664,300.0,-3917\n"
            + "-56.666666666666664,333.3333333333333,-2202\n"
            + "-23.33333333333333,0.0,-5172\n"
            + "-23.33333333333333,33.333333333333336,60\n"
            + "-23.33333333333333,66.66666666666667,-3611\n"
            + "-23.33333333333333,100.0,-5902\n"
            + "-23.33333333333333,133.33333333333334,640\n"
            + "-23.33333333333333,166.66666666666666,-1783\n"
            + "-23.33333333333333,200.0,-4649\n"
            + "-23.33333333333333,233.33333333333334,-3416\n"
            + "-23.33333333333333,266.6666666666667,-3665\n"
            + "-23.33333333333333,300.0,134\n"
            + "-23.33333333333333,333.3333333333333,-5658\n"
            + "10.0,0.0,182\n"
            + "10.0,33.333333333333336,416\n"
            + "10.0,66.66666666666667,-4437\n"
            + "10.0,100.0,-23\n"
            + "10.0,133.33333333333334,-5788\n"
            + "10.0,166.66666666666666,-4527\n"
            + "10.0,200.0,-5274\n"
            + "10.0,233.33333333333334,-4678\n"
            + "10.0,266.6666666666667,-3805\n"
            + "10.0,300.0,-129\n"
            + "10.0,333.3333333333333,-5169\n"
            + "43.33333333333334,0.0,332\n"
            + "43.33333333333334,33.333333333333336,-2131\n"
            + "43.33333333333334,66.66666666666667,179\n"
            + "43.33333333333334,100.0,1538\n"
            + "43.33333333333334,133.33333333333334,289\n"
            + "43.33333333333334,166.66666666666666,-4940\n"
            + "43.33333333333334,200.0,-5457\n"
            + "43.33333333333334,233.33333333333334,-2945\n"
            + "43.33333333333334,266.6666666666667,376\n"
            + "43.33333333333334,300.0,-1564\n"
            + "43.33333333333334,333.3333333333333,-2967\n"
            + "76.66666666666666,0.0,-3262\n"
            + "76.66666666666666,33.333333333333336,-152\n"
            + "76.66666666666666,66.66666666666667,234\n"
            + "76.66666666666666,100.0,-28\n"
            + "76.66666666666666,133.33333333333334,-34\n"
            + "76.66666666666666,166.66666666666666,-195\n"
            + "76.66666666666666,200.0,-2111\n"
            + "76.66666666666666,233.33333333333334,-1020\n"
            + "76.66666666666666,266.6666666666667,132\n"
            + "76.66666666666666,300.0,1468\n"
            + "76.66666666666666,333.3333333333333,1886\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    tName =
        data360.makeNewFileForDapQuery(
            language,
            null,
            null,
            "altitude[(-90):2000:(90)][(0):2000:(360)]",
            EDStatic.fullTestCacheDirectory,
            data360.className() + "_timeGaps",
            ".timeGaps");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    expected = "Time gaps: (none, because there is no time axis variable)\n" + "nGaps=0\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    if (doGraphicsTests) {
      String obsDir = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR);
      String baseName = data180.className() + "_Map180";
      tName =
          data180.makeNewFileForDapQuery(
              language,
              null,
              null,
              "altitude[(-90):(90)][(-180):(180)]"
                  + "&.vars=longitude|latitude|altitude&.colorBar=Ocean|C|Linear|-8000|0&.drawLand=Over",
              obsDir,
              baseName,
              ".png");
      // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
      Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

      baseName = data360.className() + "_Map360";
      tName =
          data360.makeNewFileForDapQuery(
              language, null, null, "altitude[(-90):(90)][(0):(360)]", obsDir, baseName, ".png");
      // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
      Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

      baseName = data360.className() + "_TopoUnder";
      tName =
          data360.makeNewFileForDapQuery(
              language,
              null,
              null,
              "altitude[][]"
                  + "&.vars=longitude|latitude|altitude&.colorBar=Topography|C|Linear|-8000|8000&.land=under",
              obsDir,
              baseName,
              ".png");
      // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
      Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

      // same data subset. Is cached file used?
      baseName = data360.className() + "_TopoOver";
      tName =
          data360.makeNewFileForDapQuery(
              language,
              null,
              null,
              "altitude[][]"
                  + "&.vars=longitude|latitude|altitude&.colorBar=Topography|C|Linear|-8000|8000&.land=over",
              obsDir,
              baseName,
              ".png");
      // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
      Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");
    }

    String2.log("\n*** EDDGridFromEtopo.test finished.");
  }
}
