package testDataset;

import java.nio.file.Path;

import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.util.EDStatic;

public class EDDTestDataset {
  public static EDD gethawaii_d90f_20ee_c4cb_LonPM180() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDGridLonPM180\" datasetID=\"hawaii_d90f_20ee_c4cb_LonPM180\" active=\"true\">\n" + //
            "    <dataset type=\"EDDGridFromErddap\" datasetID=\"hawaii_d90f_20ee_c4cb_LonPM180Child\">\n" + //
            "        <!-- SODA - POP 2.2.4 Monthly Means (At Depths)\n" + //
            "             minLon=0.25 maxLon=359.75 -->\n" + //
            "        <sourceUrl>https://coastwatch.pfeg.noaa.gov/erddap/griddap/hawaii_d90f_20ee_c4cb</sourceUrl>\n" + //
            "    </dataset>\n" + //
            "</dataset>");
  }

  public static EDD geterdMHchla8day() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDGridFromDap\" datasetID=\"erdMHchla8day\">\n" + //
        "    <sourceUrl>https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day</sourceUrl>\n" + //
        "    <reloadEveryNMinutes>11000</reloadEveryNMinutes>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"infoUrl\">https://coastwatch.pfeg.noaa.gov/infog/MH_chla_las.html</att>\n" + //
        "        <att name=\"creator_name\">NOAA NMFS SWFSC ERD</att>\n" + //
        "        <att name=\"creator_type\">institution</att>\n" + //
        "        <att name=\"creator_email\">erd.data@noaa.gov</att>\n" + //
        "        <att name=\"creator_url\">https://www.pfeg.noaa.gov</att>\n" + //
        "        <att name=\"publisher_name\">NOAA NMFS SWFSC ERD</att>\n" + //
        "        <att name=\"publisher_type\">institution</att>\n" + //
        "        <att name=\"publisher_email\">erd.data@noaa.gov</att>\n" + //
        "        <att name=\"publisher_url\">https://www.pfeg.noaa.gov</att>\n" + //
        "        <att name=\"institution\">NOAA NMFS SWFSC ERD</att>\n" + //
        "        <att name=\"keywords\">8-day,\n" + //
        "Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Chlorophyll,\n" + //
        "aqua, chemistry, chlorophyll, chlorophyll-a, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, day, degrees, global, modis, noaa, npp, ocean, ocean color, oceans, quality, science, science quality, sea, seawater, water, wcn</att>\n"
        + //
        "        <att name=\"license\">[standard]</att>\n" + //
        "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" + //
        "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
        "        <att name=\"title\">Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)</att>\n"
        + //
        "        <att name=\"cwhdf_version\" />\n" + //
        "        <att name=\"cols\" />\n" + //
        "        <att name=\"et_affine\" />\n" + //
        "        <att name=\"gctp_datum\" />\n" + //
        "        <att name=\"gctp_parm\" />\n" + //
        "        <att name=\"gctp_sys\" />\n" + //
        "        <att name=\"gctp_zone\" />\n" + //
        "        <att name=\"id\" />\n" + //
        "        <att name=\"pass_date\" />\n" + //
        "        <att name=\"polygon_latitude\" />\n" + //
        "        <att name=\"polygon_longitude\" />\n" + //
        "        <att name=\"rows\" />\n" + //
        "        <att name=\"start_time\" />\n" + //
        "    </addAttributes>\n" + //
        "    <axisVariable>\n" + //
        "        <sourceName>time</sourceName>\n" + //
        "    </axisVariable>\n" + //
        "    <axisVariable>\n" + //
        "        <sourceName>altitude</sourceName>\n" + //
        "    </axisVariable>\n" + //
        "    <axisVariable>\n" + //
        "        <sourceName>lat</sourceName>\n" + //
        "        <destinationName>latitude</destinationName>\n" + //
        "    </axisVariable>\n" + //
        "    <axisVariable>\n" + //
        "        <sourceName>lon</sourceName>\n" + //
        "        <destinationName>longitude</destinationName>\n" + //
        "    </axisVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>MHchla</sourceName>\n" + //
        "        <destinationName>chlorophyll</destinationName>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Ocean Color</att>\n" + //
        "            <att name=\"long_name\">Concentration Of Chlorophyll In Sea Water</att>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0.03</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">30</att>\n" + //
        "            <att name=\"colorBarScale\">Log</att>\n" + //
        "            <att name=\"actual_range\" />\n" + //
        "            <att name=\"numberOfObservations\" />\n" + //
        "            <att name=\"percentCoverage\" />\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD gethawaii_d90f_20ee_c4cb() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDGridFromDap\" datasetID=\"hawaii_d90f_20ee_c4cb\" active=\"true\">\n" + //
            "    <sourceUrl>http://apdrc.soest.hawaii.edu/dods/public_data/SODA/soda_pop2.2.4</sourceUrl>\n" + //
            "    <accessibleViaWMS>false</accessibleViaWMS>\n" + //
            "    <reloadEveryNMinutes>15000</reloadEveryNMinutes>\n" + //
            "    <defaultDataQuery>temp[last][0][0:last][0:last],salt[last][0][0:last][0:last],u[last][0][0:last][0:last],v[last][0][0:last][0:last],w[last][0][0:last][0:last]</defaultDataQuery>\n"
            + //
            "    <defaultGraphQuery>temp[last][0][0:last][0:last]&amp;.draw=surface&amp;.vars=longitude|latitude|temp</defaultGraphQuery>\n"
            + //
            "    <!-- sourceAttributes>\n" + //
            "        <att name=\"Conventions\">COARDS</att>\n" + //
            "        <att name=\"dataType\">Grid</att>\n" + //
            "        <att name=\"documentation\">http://apdrc.soest.hawaii.edu/datadoc/soda_2.2.4.php</att>\n" + //
            "        <att name=\"history\">Tue Feb 22 14:37:08 HST 2011 : imported by GrADS Data Server 2.0</att>\n" + //
            "        <att name=\"title\">SODA v2.2.4 monthly means</att>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"cdm_data_type\">Grid</att>\n" + //
            "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
            "        <att name=\"infoUrl\">https://www.atmos.umd.edu/~ocean/</att>\n" + //
            "        <att name=\"institution\">TAMU/UMD</att>\n" + //
            "        <att name=\"keywords\">\n" + //
            "Earth Science &gt; Oceans &gt; Ocean Circulation &gt; Ocean Currents,\n" + //
            "Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Water Temperature,\n" + //
            "Earth Science &gt; Oceans &gt; Salinity/Density &gt; Salinity,\n" + //
            "circulation, currents, density, depths, eastward, eastward_sea_water_velocity, means, monthly, northward, northward_sea_water_velocity, ocean, oceans, pop, salinity, sea, sea_water_practical_salinity, sea_water_temperature, seawater, soda, tamu, temperature, umd, upward, upward_sea_water_velocity, velocity, water</att>\n"
            + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"summary\">Simple Ocean Data Assimilation (SODA) version 2.2.4 - A reanalysis of ocean \n"
            + //
            "climate. SODA uses the GFDL modular ocean model version 2.2. The model is \n" + //
            "forced by observed surface wind stresses from the COADS data set (from 1958 \n" + //
            "to 1992) and from NCEP (after 1992). Note that the wind stresses were \n" + //
            "detrended before use due to inconsistencies with observed sea level pressure \n" + //
            "trends. The model is also constrained by constant assimilation of observed \n" + //
            "temperatures, salinities, and altimetry using an optimal data assimilation \n" + //
            "technique. The observed data comes from: 1) The World Ocean Atlas 1994 which \n" + //
            "contains ocean temperatures and salinities from mechanical \n" + //
            "bathythermographs, expendable bathythermographs and conductivity-temperature-\n" + //
            "depth probes. 2) The expendable bathythermograph archive 3) The TOGA-TAO \n" + //
            "thermistor array 4) The Soviet SECTIONS tropical program 5) Satellite \n" + //
            "altimetry from Geosat, ERS/1 and TOPEX/Poseidon. \n" + //
            "We are now exploring an eddy-permitting reanalysis based on the Parallel \n" + //
            "Ocean Program POP-1.4 model with 40 levels in the vertical and a 0.4x0.25 \n" + //
            "degree displaced pole grid (25 km resolution in the western North \n" + //
            "Atlantic).  The first version of this we will release is SODA1.2, a \n" + //
            "reanalysis driven by ERA-40 winds covering the period 1958-2001 (extended to \n" + //
            "the current year using available altimetry).</att>\n" + //
            "        <att name=\"title\">SODA - POP 2.2.4 Monthly Means, 1871-2010 (At Depths)</att>\n" + //
            "    </addAttributes>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>time</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"grads_dim\">t</att>\n" + //
            "            <att name=\"grads_mapping\">linear</att>\n" + //
            "            <att name=\"grads_min\">00z15jan1958</att>\n" + //
            "            <att name=\"grads_size\">612</att>\n" + //
            "            <att name=\"grads_step\">1mo</att>\n" + //
            "            <att name=\"long_name\">time</att>\n" + //
            "            <att name=\"maximum\">00z15dec2008</att>\n" + //
            "            <att name=\"minimum\">00z15jan1958</att>\n" + //
            "            <att name=\"resolution\" type=\"float\">30.436989</att>\n" + //
            "            <att name=\"units\">days since 1-1-1 00:00:0.0</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"grads_dim\"></att>\n" + //
            "            <att name=\"grads_mapping\"></att>\n" + //
            "            <att name=\"grads_min\"></att>\n" + //
            "            <att name=\"grads_size\"></att>\n" + //
            "            <att name=\"grads_step\"></att>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"long_name\">Centered Time</att>\n" + //
            "            <att name=\"maximum\"></att>\n" + //
            "            <att name=\"minimum\"></att>\n" + //
            "            <att name=\"resolution\"></att>\n" + //
            "            <att name=\"units\">days since 0001-01-01T00:00:00</att>\n" + //
            "            <att name=\"standard_name\">time</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>lev</sourceName>\n" + //
            "        <destinationName>depth</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"grads_dim\">z</att>\n" + //
            "            <att name=\"grads_mapping\">levels</att>\n" + //
            "            <att name=\"long_name\">altitude</att>\n" + //
            "            <att name=\"maximum\" type=\"double\">5374.0</att>\n" + //
            "            <att name=\"minimum\" type=\"double\">5.0</att>\n" + //
            "            <att name=\"name\">Depth</att>\n" + //
            "            <att name=\"positive\">down</att>\n" + //
            "            <att name=\"resolution\" type=\"float\">137.66667</att>\n" + //
            "            <att name=\"units\">meters</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"grads_dim\"></att>\n" + //
            "            <att name=\"grads_mapping\"></att>\n" + //
            "            <att name=\"long_name\">Depth</att>\n" + //
            "            <att name=\"maximum\"></att>\n" + //
            "            <att name=\"minimum\"></att>\n" + //
            "            <att name=\"name\"></att>\n" + //
            "            <att name=\"resolution\"></att>\n" + //
            "            <att name=\"units\">m</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>lat</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"grads_dim\">y</att>\n" + //
            "            <att name=\"grads_mapping\">linear</att>\n" + //
            "            <att name=\"grads_size\">330</att>\n" + //
            "            <att name=\"long_name\">latitude</att>\n" + //
            "            <att name=\"maximum\" type=\"double\">89.25</att>\n" + //
            "            <att name=\"minimum\" type=\"double\">-75.25</att>\n" + //
            "            <att name=\"resolution\" type=\"float\">0.5</att>\n" + //
            "            <att name=\"units\">degrees_north</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"grads_dim\"></att>\n" + //
            "            <att name=\"grads_mapping\"></att>\n" + //
            "            <att name=\"grads_size\"></att>\n" + //
            "            <att name=\"maximum\"></att>\n" + //
            "            <att name=\"minimum\"></att>\n" + //
            "            <att name=\"resolution\"></att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Latitude</att>\n" + //
            "            <att name=\"standard_name\">latitude</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>lon</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"grads_dim\">x</att>\n" + //
            "            <att name=\"grads_mapping\">linear</att>\n" + //
            "            <att name=\"grads_size\">720</att>\n" + //
            "            <att name=\"long_name\">longitude</att>\n" + //
            "            <att name=\"maximum\" type=\"double\">359.75</att>\n" + //
            "            <att name=\"minimum\" type=\"double\">0.25</att>\n" + //
            "            <att name=\"resolution\" type=\"float\">0.5</att>\n" + //
            "            <att name=\"units\">degrees_east</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"grads_dim\"></att>\n" + //
            "            <att name=\"grads_mapping\"></att>\n" + //
            "            <att name=\"grads_size\"></att>\n" + //
            "            <att name=\"long_name\"></att>\n" + //
            "            <att name=\"maximum\"></att>\n" + //
            "            <att name=\"minimum\"></att>\n" + //
            "            <att name=\"resolution\"></att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Longitude</att>\n" + //
            "            <att name=\"standard_name\">longitude</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>temp</sourceName>\n" + //
            "        <destinationName>temp</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-9.99E33</att>\n" + //
            "            <att name=\"long_name\">temperature [degc] </att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9.99E33</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">32</att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">Sea Water Temperature</att>\n" + //
            "            <att name=\"standard_name\">sea_water_temperature</att>\n" + //
            "            <att name=\"units\">degree_C</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>salt</sourceName>\n" + //
            "        <destinationName>salt</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-9.99E33</att>\n" + //
            "            <att name=\"long_name\">salinity [psu] </att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9.99E33</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n" + //
            "            <att name=\"ioos_category\">Salinity</att>\n" + //
            "            <att name=\"long_name\">Sea Water Practical Salinity</att>\n" + //
            "            <att name=\"standard_name\">sea_water_practical_salinity</att>\n" + //
            "            <att name=\"units\">PSU</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>u</sourceName>\n" + //
            "        <destinationName>u</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-9.99E33</att>\n" + //
            "            <att name=\"long_name\">zonal velocity [m/s] </att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9.99E33</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-0.5</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">0.5</att>\n" + //
            "            <att name=\"ioos_category\">Currents</att>\n" + //
            "            <att name=\"long_name\">Eastward Sea Water Velocity</att>\n" + //
            "            <att name=\"standard_name\">eastward_sea_water_velocity</att>\n" + //
            "            <att name=\"units\">m s-1</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>v</sourceName>\n" + //
            "        <destinationName>v</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-9.99E33</att>\n" + //
            "            <att name=\"long_name\">meridional velocity [m/s] </att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9.99E33</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-0.5</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">0.5</att>\n" + //
            "            <att name=\"ioos_category\">Currents</att>\n" + //
            "            <att name=\"long_name\">Northward Sea Water Velocity</att>\n" + //
            "            <att name=\"standard_name\">northward_sea_water_velocity</att>\n" + //
            "            <att name=\"units\">m s-1</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>w</sourceName>\n" + //
            "        <destinationName>w</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-9.99E33</att>\n" + //
            "            <att name=\"long_name\">vertical velocity [m/s] </att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9.99E33</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-1e-5</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">1e-5</att>\n" + //
            "            <att name=\"comment\">WARNING: Please use this variable's data with caution.</att>\n" + //
            "            <att name=\"ioos_category\">Currents</att>\n" + //
            "            <att name=\"long_name\">Upward Sea Water Velocity</att>\n" + //
            "            <att name=\"standard_name\">upward_sea_water_velocity</att>\n" + //
            "            <att name=\"units\">m s-1</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD gettestActualRange2() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDGridFromDap\" datasetID=\"testActualRange2\" active=\"true\">\n" + //
            "    <sourceUrl>https://psl.noaa.gov/thredds/dodsC/Datasets/noaa_hrc/hrc.nmissdays.nc</sourceUrl>\n" + //
            "    <reloadEveryNMinutes>43200</reloadEveryNMinutes>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "        <att name=\"Conventions\">CF-1.2</att>\n" + //
            "        <att name=\"dataset_title\">NOAA Highly Reflective Clouds</att>\n" + //
            "        <att name=\"history\">Created 1998/08/27 by Don Hooper from NCAR data</att>\n" + //
            "        <att name=\"platform\">Satellite</att>\n" + //
            "        <att name=\"References\">http://www.esrl.noaa.gov/psd/data/gridded/data.noaa.hrc.html</att>\n" + //
            "        <att name=\"title\">NOAA Highly Reflective Clouds, 25N-25S</att>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"cdm_data_type\">Grid</att>\n" + //
            "        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" + //
            "        <att name=\"creator_email\">esrl.psd.data@noaa.gov</att>\n" + //
            "        <att name=\"creator_name\">NOAA ESRL PSD</att>\n" + //
            "        <att name=\"creator_type\">institution</att>\n" + //
            "        <att name=\"creator_url\">https://www.esrl.noaa.gov/psd/</att>\n" + //
            "        <att name=\"id\">Datasets.noaa_hrc.hrc.nmissdays.nc</att>\n" + //
            "        <att name=\"infoUrl\">https://www.esrl.noaa.gov/psd/data/gridded/data.noaa.hrc.html</att>\n" + //
            "        <att name=\"institution\">NOAA ESRL</att>\n" + //
            "        <att name=\"keywords\">25n, 25n-25s, 25s, clouds, data, days, earth, esrl, highly, hrc, hrc.nmissdays, laboratory, latitude, longitude, meteorology, missing, month, monthly, noaa, reflective, research, system, time</att>\n"
            + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"References\">null</att>\n" + //
            "        <att name=\"references\">https://www.esrl.noaa.gov/psd/data/gridded/data.noaa.hrc.html</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"summary\">NOAA Highly Reflective Clouds, 25N-25S (noaa hrc, hrc.nmissdays)</att>\n" + //
            "        <att name=\"title\">NOAA Highly Reflective Clouds, 25N-25S (noaa hrc, hrc.nmissdays), 1.0&#xb0;, 1971-1985</att>\n"
            + //
            "    </addAttributes>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>time</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"doubleList\">62456.0 67904.0</att>\n" + //
            "            <att name=\"avg_period\">0000-01-00 00:00:00</att>\n" + //
            "            <att name=\"axis\">T</att>\n" + //
            "            <att name=\"delta_t\">0000-01-00 00:00:00</att>\n" + //
            "            <att name=\"long_name\">Time</att>\n" + //
            "            <att name=\"standard_name\">time</att>\n" + //
            "            <att name=\"units\">days since 1800-01-01 00:00:0.0</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"units\">days since 1800-01-01T00:00:00Z</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>lat</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">25.0 -25.0</att>\n" + //
            "            <att name=\"axis\">Y</att>\n" + //
            "            <att name=\"long_name\">Latitude</att>\n" + //
            "            <att name=\"standard_name\">latitude</att>\n" + //
            "            <att name=\"units\">degrees_north</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>lon</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">1.0 360.0</att>\n" + //
            "            <att name=\"axis\">X</att>\n" + //
            "            <att name=\"long_name\">Longitude</att>\n" + //
            "            <att name=\"standard_name\">longitude</att>\n" + //
            "            <att name=\"units\">degrees_east</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>hrc</sourceName>\n" + //
            "        <destinationName>hrc</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">0.0 21.0</att>\n" + //
            "            <att name=\"add_offset\" type=\"float\">0.0</att>\n" + //
            "            <att name=\"dataset\">NOAA Highly Reflective Clouds</att>\n" + //
            "            <att name=\"level_desc\">Entire Atmosphere Considered As a Single Layer</att>\n" + //
            "            <att name=\"long_name\">Highly Reflective Clouds Monthly Missing Days</att>\n" + //
            "            <att name=\"missing_value\" type=\"short\">32766</att>\n" + //
            "            <att name=\"parent_stat\">Individual Obs</att>\n" + //
            "            <att name=\"precision\" type=\"short\">0</att>\n" + //
            "            <att name=\"scale_factor\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"statistic\">Number of Missing Days</att>\n" + //
            "            <att name=\"unpacked_valid_range\" type=\"floatList\">0.0 31.0</att>\n" + //
            "            <att name=\"valid_range\" type=\"shortList\">0 31</att>\n" + //
            "            <att name=\"var_desc\">Highly Reflective Clouds</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"add_offset\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">25.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"dataset\">null</att>\n" + //
            "            <att name=\"ioos_category\">Meteorology</att>\n" + //
            "            <att name=\"scale_factor\">null</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD gethycom_GLBa008_tyx() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDGridFromDap\" datasetID=\"hycom_GLBa008_tyx\" active=\"true\">\n" + //
            "    <sourceUrl>https://tds.hycom.org/thredds/dodsC/glb_analysis</sourceUrl>\n" + //
            "    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "        <att name=\"Conventions\">CF-1.0</att>\n" + //
            "        <att name=\"experiment\">60.5</att>\n" + //
            "        <att name=\"history\">archv2ncdf2d</att>\n" + //
            "        <att name=\"institution\">Naval Research Laboratory</att>\n" + //
            "        <att name=\"source\">HYCOM archive file</att>\n" + //
            "        <att name=\"title\">HYCOM GLBa0.08</att>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"cdm_data_type\">Grid</att>\n" + //
            "        <att name=\"Conventions\">CF-1.0, COARDS, ACDD-1.3</att>\n" + //
            "        <att name=\"keywords\">60.5h,\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Radiation &gt; Heat Flux,\n" + //
            "Earth Science &gt; Oceans &gt; Ocean Circulation &gt; Fresh Water Flux,\n" + //
            "Earth Science &gt; Oceans &gt; Ocean Circulation &gt; Ocean Mixed Layer,\n" + //
            "Earth Science &gt; Oceans &gt; Salinity/Density &gt; Salinity,\n" + //
            "Earth Science &gt; Oceans &gt; Sea Surface Topography &gt; Sea Surface Height,\n" + //
            "air, atmosphere, atmospheric, bnd.layr.thickness, circulation, density, downward, elevation, flux, fresh, glba0.08, heat, heat flux, height, hycom, hydrology, into, laboratory, layer, level, mix.l., mix.layr.dens, mix.layr.saln, mix.layr.temp, mix.layr.thickness, mixed, mixed layer, mixl., naval, ocean, ocean_mixed_layer_thickness, oceanography, oceans, physical, physical oceanography, radiation, research, salinity, saln., sea, sea level, sea_surface_elevation, sea_water_practical_salinity, seawater, surf., surface, surface_downward_heat_flux_in_air, temp., temperature, thickness, topography, trend, u-velocity, v-velocity, velocity, water, water_flux_into_ocean</att>\n"
            + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"title\">HYCOM GLBa0.08 1/12 deg HYCOM + NCODA Global Hindcast Analysis (Combined), 2008-2018, [time][Y][X]</att>\n"
            + //
            "\n" + //
            "        <att name=\"creator_email\">George.Halliwell@noaa.gov</att>\n" + //
            "        <att name=\"creator_name\">Naval Research Laboratory, HYCOM</att>\n" + //
            "        <att name=\"creator_type\">institution</att>\n" + //
            "        <att name=\"infoUrl\">https://hycom.org/dataserver/glb-analysis/</att>\n" + //
            "        <att name=\"publisher_email\">hycomdata@coaps.fsu.edu</att>\n" + //
            "        <att name=\"publisher_name\">FSU COAPS</att>\n" + //
            "        <att name=\"publisher_url\">http://www.coaps.fsu.edu/</att>\n" + //
            "        <att name=\"summary\">HYbrid Coordinate Ocean Model (HYCOM) GLBa0.08 from Hycom.  The hybrid coordinate is one that is isopycnal in the open, stratified ocean, but smoothly reverts to a terrain-following coordinate in shallow coastal regions, and to z-level coordinates in the mixed layer and/or unstratified seas. The hybrid coordinate extends the geographic range of applicability of traditional isopycnic coordinate circulation models (the basis of the present hybrid code), such as the Miami Isopycnic Coordinate Ocean Model (MICOM) and the Navy Layered Ocean Model (NLOM), toward shallow coastal seas and unstratified parts of the world ocean.</att>\n"
            + //
            "    </addAttributes>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>MT</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"axis\">T</att>\n" + //
            "            <att name=\"calendar\">standard</att>\n" + //
            "            <att name=\"long_name\">time</att>\n" + //
            "            <att name=\"units\">days since 1900-12-31 00:00:00</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"standard_name\">time</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>Y</sourceName>\n" + //
            "        <destinationName>Y</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"axis\">Y</att>\n" + //
            "            <att name=\"point_spacing\">even</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Y</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>X</sourceName>\n" + //
            "        <destinationName>X</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"axis\">X</att>\n" + //
            "            <att name=\"point_spacing\">even</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">X</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <!-- mld, mlp, mixed_layer_* variables come and go\n" + //
            "      dataVariable>\n" + //
            "        <sourceName>mld</sourceName>\n" + //
            "        <destinationName>mld</destinationName>\n" + //
            "        <!- - sourceAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">1.2676506E30</att>\n" + //
            "            <att name=\"coordinates\">Longitude Latitude Date</att>\n" + //
            "            <att name=\"long_name\">MLT (0.20 degC)   [90.8H]</att>\n" + //
            "            <att name=\"standard_name\">ocean_mixed_layer_thickness</att>\n" + //
            "            <att name=\"units\">m</att>\n" + //
            "            <att name=\"valid_range\" type=\"floatList\">0.3195808 8764.178</att>\n" + //
            "        </sourceAttributes - ->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">1000.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Physical Oceanography</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>mlp</sourceName>\n" + //
            "        <destinationName>mlp</destinationName>\n" + //
            "        <!- - sourceAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">1.2676506E30</att>\n" + //
            "            <att name=\"coordinates\">Longitude Latitude Date</att>\n" + //
            "            <att name=\"long_name\">MLT (0.03 kg/m3)  [90.8H]</att>\n" + //
            "            <att name=\"standard_name\">ocean_mixed_layer_thickness</att>\n" + //
            "            <att name=\"units\">m</att>\n" + //
            "            <att name=\"valid_range\" type=\"floatList\">0.2655356 2048.9644</att>\n" + //
            "        </sourceAttributes - ->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">1000.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Physical Oceanography</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable -->\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>qtot</sourceName>\n" + //
            "        <destinationName>qtot</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">1.2676506E30</att>\n" + //
            "            <att name=\"coordinates\">Longitude Latitude Date</att>\n" + //
            "            <att name=\"long_name\">surf. heat flux   [90.8H]</att>\n" + //
            "            <att name=\"standard_name\">surface_downward_heat_flux_in_air</att>\n" + //
            "            <att name=\"units\">w/m2</att>\n" + //
            "            <att name=\"valid_range\" type=\"floatList\">-1589.504 1053.8087</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-100.0</att>\n" + //
            "            <att name=\"ioos_category\">Heat Flux</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>emp</sourceName>\n" + //
            "        <destinationName>emp</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">1.2676506E30</att>\n" + //
            "            <att name=\"coordinates\">Longitude Latitude Date</att>\n" + //
            "            <att name=\"long_name\">surf. water flux  [90.8H]</att>\n" + //
            "            <att name=\"standard_name\">water_flux_into_ocean</att>\n" + //
            "            <att name=\"units\">kg/m2/s</att>\n" + //
            "            <att name=\"valid_range\" type=\"floatList\">-1.0840473 0.19541827</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">1.0E-4</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Hydrology</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>surface_temperature_trend</sourceName>\n" + //
            "        <destinationName>surface_temperature_trend</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">1.2676506E30</att>\n" + //
            "            <att name=\"coordinates\">Longitude Latitude Date</att>\n" + //
            "            <att name=\"long_name\">surf. temp. trend [90.8H]</att>\n" + //
            "            <att name=\"units\">degC/day</att>\n" + //
            "            <att name=\"valid_range\" type=\"floatList\">-40.612576 23.588663</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">4</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-4</att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>surface_salinity_trend</sourceName>\n" + //
            "        <destinationName>surface_salinity_trend</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">1.2676506E30</att>\n" + //
            "            <att name=\"coordinates\">Longitude Latitude Date</att>\n" + //
            "            <att name=\"long_name\">surf. saln. trend [90.8H]</att>\n" + //
            "            <att name=\"units\">psu/day</att>\n" + //
            "            <att name=\"valid_range\" type=\"floatList\">-285.74814 58.42589</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">1</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-1</att>\n" + //
            "            <att name=\"ioos_category\">Salinity</att>\n" + //
            "            <att name=\"long_name\">Surface Salinity Trend [30.1H]</att>\n" + //
            "            <att name=\"standard_name\">tendency_of_sea_water_salinity</att>\n" + //
            "            <att name=\"units\">PSU/day</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>ssh</sourceName>\n" + //
            "        <destinationName>ssh</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">1.2676506E30</att>\n" + //
            "            <att name=\"coordinates\">Longitude Latitude Date</att>\n" + //
            "            <att name=\"long_name\">sea surf. height  [90.8H]</att>\n" + //
            "            <att name=\"standard_name\">sea_surface_elevation</att>\n" + //
            "            <att name=\"units\">m</att>\n" + //
            "            <att name=\"valid_range\" type=\"floatList\">-2.017284 1.4006238</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">2.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-2.0</att>\n" + //
            "            <att name=\"ioos_category\">Sea Level</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD geterdBAssta5day() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDGridFromDap\" datasetID=\"erdBAssta5day\">\n" + //
        "    <sourceUrl>https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/BA/ssta/5day</sourceUrl>\n" + //
        "    <accessibleViaWMS>false</accessibleViaWMS>\n" + //
        "    <reloadEveryNMinutes>14000</reloadEveryNMinutes>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"infoUrl\">https://coastwatch.pfeg.noaa.gov/infog/BA_ssta_las.html</att>\n" + //
        "        <att name=\"creator_name\">NOAA NMFS SWFSC ERD</att>\n" + //
        "        <att name=\"creator_type\">institution</att>\n" + //
        "        <att name=\"creator_email\">erd.data@noaa.gov</att>\n" + //
        "        <att name=\"creator_url\">https://www.pfeg.noaa.gov</att>\n" + //
        "        <att name=\"publisher_name\">NOAA NMFS SWFSC ERD</att>\n" + //
        "        <att name=\"publisher_type\">institution</att>\n" + //
        "        <att name=\"publisher_email\">erd.data@noaa.gov</att>\n" + //
        "        <att name=\"publisher_url\">https://www.pfeg.noaa.gov</att>\n" + //
        "        <att name=\"institution\">NOAA NMFS SWFSC ERD</att>\n" + //
        "        <att name=\"keywords\">5-day,\n" + //
        "Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature,\n" + //
        "blended, coastwatch, day, degrees, experimental, global, noaa, ocean, oceans, sea, sea_surface_temperature, sst, surface, temperature, wcn</att>\n"
        + //
        "        <att name=\"license\">[standard]</att>\n" + //
        "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" + //
        "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
        "        <att name=\"title\">SST, Blended, Global, 2002-2014, EXPERIMENTAL (5 Day Composite)</att>\n" + //
        "        <att name=\"cwhdf_version\" />\n" + //
        "        <att name=\"cols\" />\n" + //
        "        <att name=\"et_affine\" />\n" + //
        "        <att name=\"gctp_datum\" />\n" + //
        "        <att name=\"gctp_parm\" />\n" + //
        "        <att name=\"gctp_sys\" />\n" + //
        "        <att name=\"gctp_zone\" />\n" + //
        "        <att name=\"id\" />\n" + //
        "        <att name=\"pass_date\" />\n" + //
        "        <att name=\"polygon_latitude\" />\n" + //
        "        <att name=\"polygon_longitude\" />\n" + //
        "        <att name=\"rows\" />\n" + //
        "        <att name=\"start_time\" />\n" + //
        "    </addAttributes>\n" + //
        "    <axisVariable>\n" + //
        "        <sourceName>time</sourceName>\n" + //
        "    </axisVariable>\n" + //
        "    <axisVariable>\n" + //
        "        <sourceName>altitude</sourceName>\n" + //
        "    </axisVariable>\n" + //
        "    <axisVariable>\n" + //
        "        <sourceName>lat</sourceName>\n" + //
        "        <destinationName>latitude</destinationName>\n" + //
        "    </axisVariable>\n" + //
        "    <axisVariable>\n" + //
        "        <sourceName>lon</sourceName>\n" + //
        "        <destinationName>longitude</destinationName>\n" + //
        "    </axisVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>BAssta</sourceName>\n" + //
        "        <destinationName>sst</destinationName>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Temperature</att>\n" + //
        "            <att name=\"long_name\">Sea Surface Temperature</att>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">32</att>\n" + //
        "            <att name=\"actual_range\" />\n" + //
        "            <att name=\"numberOfObservations\" />\n" + //
        "            <att name=\"percentCoverage\" />\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD getNCOM_Region7_2D() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDGridFromDap\" datasetID=\"NCOM_Region7_2D\">\n" + //
        "     <sourceUrl>http://edac-dap.northerngulfinstitute.org/pydap/NCOM/region7/latest_ncom_glb_reg7.nc</sourceUrl>\n"
        + //
        "     <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" + //
        "     <addAttributes>\n" + //
        "         <att name=\"Conventions\">Global NCOM</att>\n" + //
        "         <att name=\"creator_email\">frank.bub@navy.mil</att>\n" + //
        "         <att name=\"creator_name\">Naval Research Lab (NRL)</att>\n" + //
        "         <att name=\"creator_url\">www7320.nrlssc.navy.mil/global_ncom/</att>\n" + //
        "         <att name=\"Metadata_Conventions\">null</att>\n" + //
        "         <att name=\"infoUrl\">http://edac-dap.northerngulfinstitute.org/dapnav/NCOM/region7/latest_ncom_glb_reg7.nc</att>\n"
        + //
        "         <att name=\"license\">[standard]</att>\n" + //
        "         <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
        "         <att name=\"summary\">Global NCOM for Region 7, 2D field</att>\n" + //
        "         <att name=\"title\">Global NCOM for Region 7, 2D</att>\n" + //
        "     </addAttributes>\n" + //
        "     <axisVariable>\n" + //
        "             <sourceName>time</sourceName>\n" + //
        "             <destinationName>time</destinationName>\n" + //
        "             <addAttributes></addAttributes>\n" + //
        "     </axisVariable>\n" + //
        "     <axisVariable>\n" + //
        "             <sourceName>lat</sourceName>\n" + //
        "             <destinationName>latitude</destinationName>\n" + //
        "             <addAttributes></addAttributes>\n" + //
        "     </axisVariable>\n" + //
        "     <axisVariable>\n" + //
        "             <sourceName>lon</sourceName>\n" + //
        "             <destinationName>longitude</destinationName>\n" + //
        "             <addAttributes></addAttributes>\n" + //
        "     </axisVariable>\n" + //
        "     <dataVariable>\n" + //
        "             <sourceName>surf_el</sourceName>\n" + //
        "             <destinationName>surf_el</destinationName>\n" + //
        "             <addAttributes>\n" + //
        "                     <att name=\"ioos_category\">Other</att>\n" + //
        "             </addAttributes>\n" + //
        "     </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD getjplNesdisG17v271() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDGridFromDap\" datasetID=\"jplNesdisG17v271\" active=\"true\">\n" + //
            "    <sourceUrl>https://thredds.jpl.nasa.gov/thredds/dodsC/OceanTemperature/ABI_G17-STAR-L3C-v2.71.nc</sourceUrl>\n"
            + //
            "    <reloadEveryNMinutes>180</reloadEveryNMinutes>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "        <att name=\"acknowledgement\">Please acknowledge the use of these data with the following statement: These data were provided by Group for High Resolution Sea Surface Temperature (GHRSST) and the National Oceanic and Atmospheric Administration (NOAA).</att>\n"
            + //
            "        <att name=\"cdm_data_type\">grid</att>\n" + //
            "        <att name=\"col_count\" type=\"int\">18000</att>\n" + //
            "        <att name=\"col_start\" type=\"int\">0</att>\n" + //
            "        <att name=\"comment\">SSTs are a weighted average of the SSTs of contributing pixels. WARNING: some applications are unable to properly handle signed byte values. If byte values &gt; 127 are encountered, subtract 256 from this reported value.</att>\n"
            + //
            "        <att name=\"Conventions\">CF-1.7</att>\n" + //
            "        <att name=\"creator_email\">Alex.Ignatov@noaa.gov</att>\n" + //
            "        <att name=\"creator_name\">Alex Ignatov</att>\n" + //
            "        <att name=\"creator_url\">http://www.star.nesdis.noaa.gov</att>\n" + //
            "        <att name=\"date_created\">20200708T034156Z</att>\n" + //
            "        <att name=\"easternmost_longitude\" type=\"float\">-55.81</att>\n" + //
            "        <att name=\"file_quality_level\" type=\"int\">2</att>\n" + //
            "        <att name=\"gds_version_id\">02.0</att>\n" + //
            "        <att name=\"geospatial_lat_resolution\" type=\"float\">0.02</att>\n" + //
            "        <att name=\"geospatial_lat_units\">degrees_north</att>\n" + //
            "        <att name=\"geospatial_lon_resolution\" type=\"float\">0.02</att>\n" + //
            "        <att name=\"geospatial_lon_units\">degrees_east</att>\n" + //
            "        <att name=\"history\">Created by the L2-to-L3 conversion tool,  which was developed and provided by NOAA/NESDIS/STAR and CCNY. The version is 4.2.6</att>\n"
            + //
            "        <att name=\"id\">ABI_G17-STAR-L3C-v2.71</att>\n" + //
            "        <att name=\"institution\">NOAA/NESDIS/STAR</att>\n" + //
            "        <att name=\"keywords\">Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature</att>\n" + //
            "        <att name=\"keywords_vocabulary\">NASA Global Change Master Directory (GCMD) Science Keywords</att>\n"
            + //
            "        <att name=\"license\">GHRSST protocol describes data use as free and open</att>\n" + //
            "        <att name=\"Metadata_Conventions\">Unidata Dataset Discovery v1.0</att>\n" + //
            "        <att name=\"metadata_link\">https://podaac.jpl.nasa.gov/ws/metadata/dataset/?format=iso&amp;shortName=ABI_G17-STAR-L3C-v2.71</att>\n"
            + //
            "        <att name=\"naming_authority\">org.ghrsst</att>\n" + //
            "        <att name=\"netcdf_version_id\">4.6.3 of Apr  2 2019 11:58:26 $</att>\n" + //
            "        <att name=\"northernmost_latitude\" type=\"float\">81.15</att>\n" + //
            "        <att name=\"platform\">GOES-17</att>\n" + //
            "        <att name=\"processing_level\">L3C</att>\n" + //
            "        <att name=\"product_version\">L2 algorithm V2.71; L3 algorithm V4.2.6</att>\n" + //
            "        <att name=\"project\">Group for High Resolution Sea Surface Temperature</att>\n" + //
            "        <att name=\"publisher_email\">ghrsst-po@nceo.ac.uk</att>\n" + //
            "        <att name=\"publisher_name\">The GHRSST Project Office</att>\n" + //
            "        <att name=\"publisher_url\">http://www.ghrsst.org</att>\n" + //
            "        <att name=\"references\">Data convention: GHRSST Data Specification (GDS) v2.0. Algorithms: ACSPO-ABI (NOAA/NESDIS/STAR)</att>\n"
            + //
            "        <att name=\"row_count\" type=\"int\">5920</att>\n" + //
            "        <att name=\"row_start\" type=\"int\">1540</att>\n" + //
            "        <att name=\"sensor\">ABI</att>\n" + //
            "        <att name=\"source\">l2p_source : 20200708020000-STAR-L2P_GHRSST-SSTsubskin-ABI_G17-ACSPO_V2.71-v02.0-fv01.0.nc</att>\n"
            + //
            "        <att name=\"southernmost_latitude\" type=\"float\">-81.15</att>\n" + //
            "        <att name=\"spatial_resolution\">0.02 deg</att>\n" + //
            "        <att name=\"sst_luts\">LUT_ABI_G17_L2C_DEPTH_DAYNIGHT_V01.00_20181002.txt</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table (v26, 08 November 2013)</att>\n" + //
            "        <att name=\"start_time\">20200708T020031Z</att>\n" + //
            "        <att name=\"stop_time\">20200708T020939Z</att>\n" + //
            "        <att name=\"Sub_Lon\" type=\"double\">-137.0</att>\n" + //
            "        <att name=\"summary\">Sea surface temperature retrievals produced by NOAA/NESDIS/STAR from the ABI sensor.</att>\n"
            + //
            "        <att name=\"time_coverage_end\">20200708T020939Z</att>\n" + //
            "        <att name=\"time_coverage_start\">20200708T020031Z</att>\n" + //
            "        <att name=\"title\">ABI L3C SST</att>\n" + //
            "        <att name=\"uuid\">0fa196d7-1ff3-42e5-a580-e7705d14ef31</att>\n" + //
            "        <att name=\"westernmost_longitude\" type=\"float\">141.81</att>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"cdm_data_type\">Grid</att>\n" + //
            "        <att name=\"Conventions\">CF-1.7, COARDS, ACDD-1.3</att>\n" + //
            "        <att name=\"creator_type\">person</att>\n" + //
            "        <att name=\"date_created\">2020-07-08T03:41:56Z</att>\n" + //
            "        <att name=\"easternmost_longitude\">null</att>\n" + //
            "        <att name=\"file_quality_level\">null</att>\n" + //
            "        <att name=\"grid_mapping_horizontal_datum_name\">World Geodetic System 1984</att>\n" + //
            "        <att name=\"grid_mapping_name\">latitude_longitude</att>\n" + //
            "        <att name=\"infoUrl\">https://podaac.jpl.nasa.gov/dataset/ABI_G17-STAR-L3C-v2.71</att>\n" + //
            "        <att name=\"keywords\">abi, angle, atmosphere, atmospheric, bias, contributing, data, deviation, difference, dt_analysis, earth, Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Surface Winds, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, environmental, error, estimate, flags, g17, g17-star-l3c-v2.71, information, l2p, l2p_flags, l2ps, l3c, latitude, level, longitude, national, nesdis, noaa, number, ocean, oceans, or_number_of_pixels, pixel, pixels, quality, quality_level, reference, satellite, satellite_zenith_angle, science, sea, sea_surface_subskin_temperature, sea_surface_temperature, sensor, service, single, skin, speed, sses, sses_bias, sses_standard_deviation, sst, sst_dtime, standard, star, statistics, sub, sub-skin, subskin, surface, temperature, time, v2.71, value, wind, wind_speed, winds, zenith</att>\n"
            + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"netcdf_version_id\">null</att>\n" + //
            "        <att name=\"northernmost_latitude\">null</att>\n" + //
            "        <att name=\"publisher_type\">group</att>\n" + //
            "        <att name=\"publisher_url\">https://www.ghrsst.org</att>\n" + //
            "        <att name=\"southernmost_latitude\">null</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"start_time\">null</att>\n" + //
            "        <att name=\"stop_time\">null</att>\n" + //
            "        <att name=\"summary\">Sea surface temperature and Wind Speed retrievals produced by NOAA/NESDIS/STAR from the ABI sensor on GOES-17.</att>\n"
            + //
            "        <att name=\"testOutOfDate\">now-3days</att>\n" + //
            "        <att name=\"time_coverage_end\">2020-07-08T02:09:39Z</att>\n" + //
            "        <att name=\"time_coverage_start\">2020-07-08T02:00:31Z</att>\n" + //
            "        <att name=\"title\">SST and Wind Speed, NOAA/NESDIS/STAR, ABI G17-STAR-L3C-v2.71, Pacific Ocean, 0.02&#xb0;, 2019-present, Hourly</att>\n"
            + //
            "        <att name=\"uuid\">null</att>\n" + //
            "        <att name=\"westernmost_longitude\">null</att>\n" + //
            "    </addAttributes>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>time</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"axis\">T</att>\n" + //
            "            <att name=\"calendar\">Gregorian</att>\n" + //
            "            <att name=\"comment\">Seconds since 1981-01-01 00:00:00</att>\n" + //
            "            <att name=\"long_name\">reference time of sst file</att>\n" + //
            "            <att name=\"standard_name\">time</att>\n" + //
            "            <att name=\"units\">seconds since 1981-01-01 00:00:00</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"units\">seconds since 1981-01-01T00:00:00Z</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>lat</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"int\">9000</att>\n" + //
            "            <att name=\"axis\">Y</att>\n" + //
            "            <att name=\"comment\">Latitudes for locating data</att>\n" + //
            "            <att name=\"long_name\">latitude</att>\n" + //
            "            <att name=\"standard_name\">latitude</att>\n" + //
            "            <att name=\"units\">degrees_north</att>\n" + //
            "            <att name=\"valid_max\" type=\"float\">90.0</att>\n" + //
            "            <att name=\"valid_min\" type=\"float\">-90.0</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Latitude</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>lon</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"int\">18000</att>\n" + //
            "            <att name=\"axis\">X</att>\n" + //
            "            <att name=\"comment\">Longitude for locating data</att>\n" + //
            "            <att name=\"long_name\">longitude</att>\n" + //
            "            <att name=\"standard_name\">longitude</att>\n" + //
            "            <att name=\"units\">degrees_east</att>\n" + //
            "            <att name=\"valid_max\" type=\"float\">180.0</att>\n" + //
            "            <att name=\"valid_min\" type=\"float\">-180.0</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Longitude</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sea_surface_temperature</sourceName>\n" + //
            "        <destinationName>sea_surface_temperature</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">1 1800 3600</att>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">-32768</att>\n" + //
            "            <att name=\"add_offset\" type=\"float\">273.15</att>\n" + //
            "            <att name=\"comment\">SST obtained by regression with buoy measurements, sensitive to skin SST. Further information at (Petrenko et al., JGR, 2014; doi:10.1002/2013JD020637)</att>\n"
            + //
            "            <att name=\"coordinates\">nj ni</att>\n" + //
            "            <att name=\"grid_mapping\">crs</att>\n" + //
            "            <att name=\"long_name\">sea surface sub-skin temperature</att>\n" + //
            "            <att name=\"scale_factor\" type=\"float\">0.01</att>\n" + //
            "            <att name=\"source\">NOAA</att>\n" + //
            "            <att name=\"standard_name\">sea_surface_subskin_temperature</att>\n" + //
            "            <att name=\"units\">kelvin</att>\n" + //
            "            <att name=\"valid_max\" type=\"short\">32767</att>\n" + //
            "            <att name=\"valid_min\" type=\"short\">-32767</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"add_offset\" type=\"float\">0</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"coordinates\">null</att>\n" + //
            "            <att name=\"grid_mapping\">null</att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"units\">degree_C</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>quality_level</sourceName>\n" + //
            "        <destinationName>quality_level</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">1 2250 4500</att>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">-128</att>\n" + //
            "            <att name=\"_Unsigned\">false</att>\n" + //
            "            <att name=\"comment\">SST quality levels: 5 corresponds to &#xe2;&#x80;&#x9c;clear-sky&#xe2;&#x80;&#x9d; pixels and is recommended for operational applications and validation.</att>\n"
            + //
            "            <att name=\"coordinates\">lon lat</att>\n" + //
            "            <att name=\"flag_meanings\">no_data bad_data not_used not_used not_used best_quality</att>\n" + //
            "            <att name=\"flag_values\" type=\"byteList\">1 2 3 4 5</att>\n" + //
            "            <att name=\"grid_mapping\">crs</att>\n" + //
            "            <att name=\"long_name\">quality level of SST pixel</att>\n" + //
            "            <att name=\"valid_max\" type=\"byte\">5</att>\n" + //
            "            <att name=\"valid_min\" type=\"byte\">0</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">6.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"coordinates\">null</att>\n" + //
            "            <att name=\"grid_mapping\">null</att>\n" + //
            "            <att name=\"ioos_category\">Quality</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>l2p_flags</sourceName>\n" + //
            "        <destinationName>l2p_flags</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">1 1800 3600</att>\n" + //
            "            <att name=\"comment\">L2P common flags in bits 1-6 and data provider flags (from ACSPO mask) in bits 9-16: bit01 (0=IR: 1=microwave); bit02 (0=ocean; 1=land); bit03 (0=no ice; 1=ice); bits04-07 (reserved,set to 0); bit08 (0=anti-solar; 1=solar); bit09 (0=radiance valid; 1=invalid); bit10 (0=night; 1=day); bit11 (0=ocean; 1=land); bit12 (0=good quality data; 1=degraded quality data due to &quot;twilight&quot; region); bit13 (0=no glint; 1=glint); bit14 (0=no snow/ice; 1=snow/ice); bits15-16 (00=clear; 01=probably clear; 10=cloudy; 11=clear-sky mask undefined)</att>\n"
            + //
            "            <att name=\"coordinates\">nj ni</att>\n" + //
            "            <att name=\"flag_masks\" type=\"shortList\">1 2 4 128 256 512 1024 2048 4096 8192 16384 -32768 -16384</att>\n"
            + //
            "            <att name=\"flag_meanings\">microwave land ice solar invalid day land twilight glint ice probably_clear cloudy mask_undefined</att>\n"
            + //
            "            <att name=\"grid_mapping\">crs</att>\n" + //
            "            <att name=\"long_name\">L2P flags</att>\n" + //
            "            <att name=\"valid_max\" type=\"short\">32767</att>\n" + //
            "            <att name=\"valid_min\" type=\"short\">-32768</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"ushort\">65535</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n"
            + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"_Unsigned\">true</att> <!-- interesting that they didn't add this -->\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">32000.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.1</att>\n" + //
            "            <att name=\"colorBarScale\">Log</att>\n" + //
            "            <att name=\"coordinates\">null</att>\n" + //
            "            <att name=\"grid_mapping\">null</att>\n" + //
            "            <att name=\"ioos_category\">Quality</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>or_number_of_pixels</sourceName>\n" + //
            "        <destinationName>or_number_of_pixels</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">1 1800 3600</att>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">0</att>\n" + //
            "            <att name=\"add_offset\" type=\"short\">0</att>\n" + //
            "            <att name=\"comment\">Original number of pixels from the L2Ps contributing to the SST value, not weighted</att>\n"
            + //
            "            <att name=\"coordinates\">lon lat</att>\n" + //
            "            <att name=\"grid_mapping\">crs</att>\n" + //
            "            <att name=\"long_name\">number of pixels from the L2Ps contributing to the SST value</att>\n" + //
            "            <att name=\"scale_factor\" type=\"short\">1</att>\n" + //
            "            <att name=\"units\">1</att>\n" + //
            "            <att name=\"valid_max\" type=\"short\">32767</att>\n" + //
            "            <att name=\"valid_min\" type=\"short\">1</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"add_offset\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">20.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"coordinates\">null</att>\n" + //
            "            <att name=\"grid_mapping\">null</att>\n" + //
            "            <att name=\"ioos_category\">Statistics</att>\n" + //
            "            <att name=\"scale_factor\">null</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>dt_analysis</sourceName>\n" + //
            "        <destinationName>dt_analysis</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">1 2250 4500</att>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">-128</att>\n" + //
            "            <att name=\"_Unsigned\">false</att>\n" + //
            "            <att name=\"add_offset\" type=\"float\">0.0</att>\n" + //
            "            <att name=\"comment\">Deviation from reference SST, i.e., dt_analysis = SST - reference SST</att>\n"
            + //
            "            <att name=\"coordinates\">nj ni</att>\n" + //
            "            <att name=\"grid_mapping\">crs</att>\n" + //
            "            <att name=\"long_name\">deviation from SST reference</att>\n" + //
            "            <att name=\"scale_factor\" type=\"float\">0.1</att>\n" + //
            "            <att name=\"source\">CMC0.1deg-CMC-L4-GLOB-v2.0</att>\n" + //
            "            <att name=\"units\">kelvin</att>\n" + //
            "            <att name=\"valid_max\" type=\"byte\">127</att>\n" + //
            "            <att name=\"valid_min\" type=\"short\">-127</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-5.0</att>\n" + //
            "            <att name=\"coordinates\">null</att>\n" + //
            "            <att name=\"grid_mapping\">null</att>\n" + //
            "            <att name=\"ioos_category\">Statistics</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>satellite_zenith_angle</sourceName>\n" + //
            "        <destinationName>satellite_zenith_angle</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">1 2250 4500</att>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">-128</att>\n" + //
            "            <att name=\"_Unsigned\">false</att>\n" + //
            "            <att name=\"add_offset\" type=\"float\">0.0</att>\n" + //
            "            <att name=\"comment\">satellite zenith angle</att>\n" + //
            "            <att name=\"coordinates\">nj ni</att>\n" + //
            "            <att name=\"grid_mapping\">crs</att>\n" + //
            "            <att name=\"long_name\">satellite zenith angle</att>\n" + //
            "            <att name=\"scale_factor\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"units\">degrees</att>\n" + //
            "            <att name=\"valid_max\" type=\"byte\">90</att>\n" + //
            "            <att name=\"valid_min\" type=\"short\">-90</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"add_offset\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" + //
            "            <att name=\"coordinates\">null</att>\n" + //
            "            <att name=\"grid_mapping\">null</att>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "            <att name=\"scale_factor\">null</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sses_bias</sourceName>\n" + //
            "        <destinationName>sses_bias</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">1 2250 4500</att>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">-128</att>\n" + //
            "            <att name=\"_Unsigned\">false</att>\n" + //
            "            <att name=\"add_offset\" type=\"float\">0.0</att>\n" + //
            "            <att name=\"comment\">Bias is derived against Piecewise Regression SST produced by local regressions with buoys. Subtracting sses_bias from sea_surface_temperature produces more accurate estimate of SST at the depth of buoys. Further information at (Petrenko et al., JTECH, 2016; doi:10.1175/JTECH-D-15-0166.1)</att>\n"
            + //
            "            <att name=\"coordinates\">nj ni</att>\n" + //
            "            <att name=\"grid_mapping\">crs</att>\n" + //
            "            <att name=\"long_name\">SSES bias estimate</att>\n" + //
            "            <att name=\"scale_factor\" type=\"float\">0.016</att>\n" + //
            "            <att name=\"units\">kelvin</att>\n" + //
            "            <att name=\"valid_max\" type=\"byte\">127</att>\n" + //
            "            <att name=\"valid_min\" type=\"short\">-127</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"coordinates\">null</att>\n" + //
            "            <att name=\"grid_mapping\">null</att>\n" + //
            "            <att name=\"ioos_category\">Statistics</att>\n" + //
            "            <att name=\"units\">degree_C</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sses_standard_deviation</sourceName>\n" + //
            "        <destinationName>sses_standard_deviation</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">1 2250 4500</att>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">-128</att>\n" + //
            "            <att name=\"_Unsigned\">false</att>\n" + //
            "            <att name=\"add_offset\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"comment\">Standard deviation of sea_surface_temperature from SST measured by drifting buoys. Further information at (Petrenko et al., JTECH, 2016; doi:10.1175/JTECH-D-15-0166.1)</att>\n"
            + //
            "            <att name=\"coordinates\">nj ni</att>\n" + //
            "            <att name=\"grid_mapping\">crs</att>\n" + //
            "            <att name=\"long_name\">SSES standard deviation</att>\n" + //
            "            <att name=\"scale_factor\" type=\"float\">0.01</att>\n" + //
            "            <att name=\"units\">kelvin</att>\n" + //
            "            <att name=\"valid_max\" type=\"byte\">127</att>\n" + //
            "            <att name=\"valid_min\" type=\"short\">-127</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"coordinates\">null</att>\n" + //
            "            <att name=\"grid_mapping\">null</att>\n" + //
            "            <att name=\"ioos_category\">Statistics</att>\n" + //
            "            <att name=\"units\">degree_C</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>wind_speed</sourceName>\n" + //
            "        <destinationName>wind_speed</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">1 2250 4500</att>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">-128</att>\n" + //
            "            <att name=\"_Unsigned\">false</att>\n" + //
            "            <att name=\"add_offset\" type=\"float\">0.0</att>\n" + //
            "            <att name=\"comment\">Typically represents surface winds (10 meters above the sea surface)</att>\n"
            + //
            "            <att name=\"coordinates\">nj ni</att>\n" + //
            "            <att name=\"grid_mapping\">crs</att>\n" + //
            "            <att name=\"height\">10 m</att>\n" + //
            "            <att name=\"long_name\">wind speed</att>\n" + //
            "            <att name=\"scale_factor\" type=\"float\">0.15</att>\n" + //
            "            <att name=\"source\">Wind speed from NCEP GFS data</att>\n" + //
            "            <att name=\"standard_name\">wind_speed</att>\n" + //
            "            <att name=\"units\">m s-1</att>\n" + //
            "            <att name=\"valid_max\" type=\"byte\">127</att>\n" + //
            "            <att name=\"valid_min\" type=\"short\">-127</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"coordinates\">null</att>\n" + //
            "            <att name=\"grid_mapping\">null</att>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sst_dtime</sourceName>\n" + //
            "        <destinationName>sst_dtime</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">1 1500 3000</att>\n" + //
            "            <att name=\"_FillValue\" type=\"int\">-2147483648</att>\n" + //
            "            <att name=\"add_offset\" type=\"float\">0.0</att>\n" + //
            "            <att name=\"comment\">time plus sst_dtime gives seconds since 1981-01-01 00:00:00 UTC</att>\n"
            + //
            "            <att name=\"coordinates\">nj ni</att>\n" + //
            "            <att name=\"grid_mapping\">crs</att>\n" + //
            "            <att name=\"long_name\">time difference from reference time</att>\n" + //
            "            <att name=\"scale_factor\" type=\"float\">0.25</att>\n" + //
            "            <att name=\"units\">seconds</att>\n" + //
            "            <att name=\"valid_max\" type=\"int\">2147483647</att>\n" + //
            "            <att name=\"valid_min\" type=\"int\">-2147483647</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">1000.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
            "            <att name=\"coordinates\">null</att>\n" + //
            "            <att name=\"grid_mapping\">null</att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD getusgsCeCrm10() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDGridFromDap\" datasetID=\"usgsCeCrm10\">\n" + //
        "    <sourceUrl>https://geoport.whoi.edu/thredds/dodsC/bathy/crm_vol10.nc</sourceUrl>\n" + //
        "    <reloadEveryNMinutes>15000</reloadEveryNMinutes>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"creator_email\">Barry.Eakins@noaa.gov</att>\n" + //
        "        <att name=\"infoUrl\">https://www.ngdc.noaa.gov/mgg/coastal/coastal.html</att>\n" + //
        "        <att name=\"institution\">NOAA NGDC</att>\n" + //
        "        <att name=\"keywords\">Oceans &gt; Bathymetry/Seafloor Topography &gt; Bathymetry,\n" + //
        "altitude, arc, atmosphere, bathymetry, coastal, earth science, hawaii, height, model, ngdc, noaa, oceans, relief, second, station, topography, vol.</att>\n"
        + //
        "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
        "        <att name=\"license\">[standard]</att>\n" + //
        "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" + //
        "        <att name=\"references\">Divins, D.L., and D. Metzger, NGDC Coastal Relief Model, https://www.ngdc.noaa.gov/mgg/coastal/coastal.html</att>\n"
        + //
        "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
        "        <att name=\"summary\">This Coastal Relief Gridded database provides the first comprehensive view of the US Coastal Zone; one that extends from the coastal state boundaries to as far offshore as the NOS hydrographic data will support a continuous view of the seafloor. In many cases, this seaward limit reaches out to, and in places even beyond the continental slope. The gridded database contains data for the entire coastal zone of the conterminous US, including Hawaii and Puerto Rico.</att>\n"
        + //
        "        <att name=\"title\">Topography, NOAA Coastal Relief Model, 3 arc second, Vol. 10 (Hawaii)</att>\n" + //
        "    </addAttributes>\n" + //
        "    <axisVariable>\n" + //
        "        <sourceName>lat</sourceName>\n" + //
        "        <destinationName>latitude</destinationName>\n" + //
        "    </axisVariable>\n" + //
        "    <axisVariable>\n" + //
        "        <sourceName>lon</sourceName>\n" + //
        "        <destinationName>longitude</destinationName>\n" + //
        "    </axisVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>topo</sourceName>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">8000</att>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">-8000</att>\n" + //
        "            <att name=\"colorBarPalette\">Topography</att>\n" + //
        "            <att name=\"ioos_category\">Location</att>\n" + //
        "            <att name=\"long_name\">Topography</att>\n" + //
        "            <att name=\"standard_name\">altitude</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD getmb7201adc() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDGridFromDap\" datasetID=\"mb7201adc\">\n" + //
        "    <sourceUrl>http://coast-enviro.er.usgs.gov/thredds/dodsC/DATAFILES/MYRTLEBEACH/7201adc-a.nc</sourceUrl>\n"
        + //
        "    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"cdm_data_type\">Grid</att>\n" + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"infoUrl\">http://stellwagen.er.usgs.gov/myrtlebeach.html</att>\n" + //
        "        <att name=\"institution\">USGS/CMGP</att>\n" + //
        "        <att name=\"license\">[standard]</att>\n" + //
        "        <att name=\"summary\">velocity data from the ADCP on mooring 720</att>\n" + //
        "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
        "        <att name=\"title\">South Carolina Coastal Erosion Study -adcp7201</att>\n" + //
        "    </addAttributes>\n" + //
        "    <axisVariable>\n" + //
        "        <sourceName>time</sourceName>\n" + //
        "    </axisVariable>\n" + //
        "    <axisVariable>\n" + //
        "        <sourceName>depth</sourceName>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </axisVariable>\n" + //
        "    <axisVariable>\n" + //
        "        <sourceName>lat</sourceName>\n" + //
        "        <destinationName>latitude</destinationName>\n" + //
        "    </axisVariable>\n" + //
        "    <axisVariable>\n" + //
        "        <sourceName>lon</sourceName>\n" + //
        "        <destinationName>longitude</destinationName>\n" + //
        "    </axisVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>u_1205</sourceName>\n" + //
        "        <destinationName>u_1205</destinationName>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Currents</att>\n" + //
        "            <att name=\"long_name\">Eastward Velocity</att>\n" + //
        "            <att name=\"standard_name\">eastward_current_velocity</att>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">-50</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">50</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>v_1206</sourceName>\n" + //
        "        <destinationName>v_1206</destinationName>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Currents</att>\n" + //
        "            <att name=\"long_name\">Northward Velocity (cm/s)</att>\n" + //
        "            <att name=\"standard_name\">northward_current_velocity</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>w_1204</sourceName>\n" + //
        "        <destinationName>w_1204</destinationName>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Currents</att>\n" + //
        "            <att name=\"long_name\">Vertical Velocity (cm/s)</att>\n" + //
        "            <att name=\"standard_name\">vertical_current_velocity</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>Werr_1201</sourceName>\n" + //
        "        <destinationName>Werr_1201</destinationName>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Unknown</att>\n" + //
        "            <att name=\"long_name\">Error Velocity</att>\n" + //
        "            <att name=\"standard_name\">none</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>AGC_1202</sourceName>\n" + //
        "        <destinationName>AGC_1202</destinationName>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Unknown</att>\n" + //
        "            <att name=\"long_name\">Average Echo Intensity (AGC)</att>\n" + //
        "            <att name=\"standard_name\">none</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>PGd_1203</sourceName>\n" + //
        "        <destinationName>PGd_1203</destinationName>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Unknown</att>\n" + //
        "            <att name=\"long_name\">Percent Good Pings</att>\n" + //
        "            <att name=\"standard_name\">none</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD getnodcPH2sstd1day() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDGridFromNcFiles\" datasetID=\"nodcPH2sstd1day\" active=\"true\">\n" + //
            "    <reloadEveryNMinutes>10000</reloadEveryNMinutes>\n" + //
            "    <fileDir>"
            + Path.of(EDDTestDataset.class.getResource("/largeFiles/satellite/PH2/sstd/1day/").toURI()).toString()
            + "</fileDir>\n" +
            "    <fileNameRegex>\\d{14}-NODC.*\\.nc.ncml</fileNameRegex>\n" + //
            "    <accessibleViaFiles>false</accessibleViaFiles> <!-- because .ncml -->\n" + //
            "    <recursive>false</recursive>\n" + //
            "    <pathRegex>.*</pathRegex>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <matchAxisNDigits>20</matchAxisNDigits>\n" + //
            "    <fileTableInMemory>false</fileTableInMemory>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "        <att name=\"acknowledgment\">Please acknowledge the use of these data with the following statement: These data were provided by GHRSST and the US National Oceanographic Data Center. This project was supported in part by a grant from the NOAA Climate Data Record (CDR) Program for satellites.</att>\n"
            + //
            "        <att name=\"cdm_data_type\">grid</att>\n" + //
            "        <att name=\"cdr_id\">gov.noaa.ncdc:C00804</att>\n" + //
            "        <att name=\"cdr_program\">NOAA Climate Data Record Program for satellites, FY 2011.</att>\n" + //
            "        <att name=\"cdr_variable\">sea_surface_temperature</att>\n" + //
            "        <att name=\"comment\">SST from AVHRR Pathfinder</att>\n" + //
            "        <att name=\"Conventions\">CF-1.5</att>\n" + //
            "        <att name=\"creator_email\">Kenneth.Casey@noaa.gov</att>\n" + //
            "        <att name=\"creator_name\">Kenneth S. Casey</att>\n" + //
            "        <att name=\"creator_url\">http://pathfinder.nodc.noaa.gov</att>\n" + //
            "        <att name=\"date_created\">20130426T025413Z</att>\n" + //
            "        <att name=\"day_or_night\">day</att>\n" + //
            "        <att name=\"easternmost_longitude\" type=\"double\">180.0</att>\n" + //
            "        <att name=\"file_quality_level\" type=\"int\">3</att>\n" + //
            "        <att name=\"gds_version_id\">2.0</att>\n" + //
            "        <att name=\"geospatial_lat_max\" type=\"double\">90.0</att>\n" + //
            "        <att name=\"geospatial_lat_min\" type=\"double\">-90.0</att>\n" + //
            "        <att name=\"geospatial_lat_resolution\">0.0417</att>\n" + //
            "        <att name=\"geospatial_lat_units\">degrees north</att>\n" + //
            "        <att name=\"geospatial_lon_max\" type=\"double\">180.0</att>\n" + //
            "        <att name=\"geospatial_lon_min\" type=\"double\">-180.0</att>\n" + //
            "        <att name=\"geospatial_lon_resolution\">0.0417</att>\n" + //
            "        <att name=\"geospatial_lon_units\">degrees east</att>\n" + //
            "        <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" + //
            "        <att name=\"history\">smigen_both ifile=2012365.b4kd3-pf5ap-n19-sst.hdf ofile=2012365.i4kd3-pf5ap-n19-sst.hdf prod=sst datamin=-3.0 datamax=40.0 precision=I projection=RECT resolution=4km gap_fill=2 ; ./hdf2nc_PFV52_L3C.x -v ./Data_PFV52/PFV52_HDF/2012/2012365.i4kd3-pf5ap-n19-sst.hdf</att>\n"
            + //
            "        <att name=\"id\">AVHRR_Pathfinder-NODC-L3C-v5.2</att>\n" + //
            "        <att name=\"institution\">NODC</att>\n" + //
            "        <att name=\"keywords\">Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature</att>\n" + //
            "        <att name=\"keywords_vocabulary\">NASA Global Change Master Directory (GCMD) Science Keywords</att>\n"
            + //
            "        <att name=\"license\">These data are available for use without restriction.</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"metadata_link\">http://pathfinder.nodc.noaa.gov/ISO-AVHRR_Pathfinder-NODC-L3C-v5.2.html</att>\n"
            + //
            "        <att name=\"naming_authority\">org.ghrsst</att>\n" + //
            "        <att name=\"netcdf_version_id\">4.1.2</att>\n" + //
            "        <att name=\"northernmost_latitude\" type=\"double\">90.0</att>\n" + //
            "        <att name=\"orbit_node\">ascending</att>\n" + //
            "        <att name=\"platform\">NOAA-19</att>\n" + //
            "        <att name=\"principal_year_day_for_collated_orbits\">2012365</att>\n" + //
            "        <att name=\"processing_level\">L3C</att>\n" + //
            "        <att name=\"product_version\">PFV5.2</att>\n" + //
            "        <att name=\"project\">Group for High Resolution Sea Surface Temperature</att>\n" + //
            "        <att name=\"publisher_email\">ghrsst-po@nceo.ac.uk</att>\n" + //
            "        <att name=\"publisher_name\">GHRSST Project Office</att>\n" + //
            "        <att name=\"publisher_url\">http://www.ghrsst.org</att>\n" + //
            "        <att name=\"references\">http://pathfinder.nodc.noaa.gov and Casey, K.S., T.B. Brandon, P. Cornillon, and R. Evans: The Past, Present and Future of the AVHRR Pathfinder SST Program, in Oceanography from Space: Revisited, eds. V. Barale, J.F.R. Gower, and L. Alberotanza, Springer, 2010. DOI: 10.1007/978-90-481-8681-5_16.</att>\n"
            + //
            "        <att name=\"sensor\">AVHRR_GAC</att>\n" + //
            "        <att name=\"source\">AVHRR_GAC-CLASS-L1B-NOAA_19-v1</att>\n" + //
            "        <att name=\"southernmost_latitude\" type=\"double\">-90.0</att>\n" + //
            "        <att name=\"spatial_resolution\">0.0417 degree</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"start_time\">20121230T001833Z</att>\n" + //
            "        <att name=\"stop_time\">20121231T065419Z</att>\n" + //
            "        <att name=\"summary\">This netCDF-4 file contains sea surface temperature (SST) data produced as part of the AVHRR Pathfinder SST Project. These data were created using Version 5.2 of the Pathfinder algorithm and the file is nearly but not completely compliant with the GHRSST Data Specifications V2.0 (GDS2).  The file does not encode time according to the GDS2 specifications, and the sses_bias and sses_standard_deviation variables are empty.  Full compliance with GDS2 specifications will be achieved in the future Pathfinder Version 6. These data were created as a partnership between the University of Miami and the US NOAA/National Oceanographic Data Center (NODC).</att>\n"
            + //
            "        <att name=\"time_coverage_end\">20121231T065419Z</att>\n" + //
            "        <att name=\"time_coverage_start\">20121230T001833Z</att>\n" + //
            "        <att name=\"title\">AVHRR Pathfinder Version 5.2 L3-Collated (L3C) sea surface temperature</att>\n"
            + //
            "        <att name=\"uuid\">AAF19A5A-0E08-4812-9CAB-B7B9FCAAF26E</att>\n" + //
            "        <att name=\"westernmost_longitude\" type=\"double\">-180.0</att>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"acknowledgement\">Please acknowledge the use of these data with the following statement: These data were provided by GHRSST and the US National Oceanographic Data Center. This project was supported in part by a grant from the NOAA Climate Data Record (CDR) Program for satellites.</att>\n"
            + //
            "        <att name=\"acknowledgment\">null</att>\n" + //
            "        <att name=\"easternmost_longitude\">null</att>\n" + //
            "        <att name=\"northernmost_latitude\">null</att>\n" + //
            "        <att name=\"southernmost_latitude\">null</att>\n" + //
            "        <att name=\"westernmost_longitude\">null</att>\n" + //
            "        <att name=\"cdm_data_type\">Grid</att>\n" + //
            "        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" + //
            "        <att name=\"creator_type\">person</att>\n" + //
            "        <att name=\"creator_url\">https://pathfinder.nodc.noaa.gov</att>\n" + //
            "        <att name=\"file_quality_level\">null</att>\n" + //
            "        <att name=\"history\">smigen_both ifile=2012365.b4kd3-pf5ap-n19-sst.hdf ofile=2012365.i4kd3-pf5ap-n19-sst.hdf prod=sst datamin=-3.0 datamax=40.0 precision=I projection=RECT resolution=4km gap_fill=2 ; ./hdf2nc_PFV52_L3C.x -v ./Data_PFV52/PFV52_HDF/2012/2012365.i4kd3-pf5ap-n19-sst.hdf\n"
            + //
            "2014-02-11 Files downloaded from https://data.nodc.noaa.gov/pathfinder/Version5.2 to NOAA NMFS SWFSC ERD by erd.data@noaa.gov . Aggregated with reference times from the dates in the file names. Removed other, misleading time values from the file names and the data files. Removed empty sses_bias and sses_standard_deviation variables. Removed aerosol_dynamic_indicator because of incorrect add_offset and scale_factor in files from 1981 through 2000.</att>\n"
            + //
            "        <att name=\"infoUrl\">https://www.nodc.noaa.gov/SatelliteData/pathfinder4km/</att>\n" + //
            "        <att name=\"institution\">NOAA NCEI</att>\n" + //
            "        <att name=\"keywords\">10m, aerosol, analysis, area, atmosphere,\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Surface Winds,\n" + //
            "atmospheric, avhrr, bias, climate, collated, cryosphere,\n" + //
            "Earth Science &gt; Cryosphere &gt; Sea Ice &gt; Ice Extent,\n" + //
            "data, deviation, difference, distribution, dynamic, estimate, extent, flag, flags, fraction, ice, ice distribution, indicator, l2p, l3-collated, l3c, last, measurement, noaa, nodc, ocean, oceans,\n"
            + //
            "Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature,\n" + //
            "Earth Science &gt; Oceans &gt; Sea Ice &gt; Ice Extent,\n" + //
            "pathfinder, quality, record, reference, sea, sea_ice_area_fraction, sea_surface_skin_temperature, sea_surface_temperature, skin, speed, sses, sst, standard, statistics, surface, temperature, time, version, wind, wind_speed, winds</att>\n"
            + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"license\">These data are available for use without restriction.  Please acknowledge the use of these data with the following statement: &quot;These data were provided by GHRSST and the US National Oceanographic Data Center. This project was supported in part by a grant from the NOAA Climate Data Record (CDR) Program for satellites.&quot; and cite the following publication:\n"
            + //
            "Casey, K.S., T.B. Brandon, P. Cornillon, and R. Evans (2010). &quot;The Past, Present and Future of the AVHRR Pathfinder SST Program&quot;, in Oceanography from Space: Revisited, eds. V. Barale, J.F.R. Gower, and L. Alberotanza, Springer. DOI: 10.1007/978-90-481-8681-5_16.\n"
            + //
            "which is available at https://pathfinder.nodc.noaa.gov/OFS_21_Cas_09Dec2009.pdf\n" + //
            "\n" + //
            "[standard]</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"netcdf_version_id\">null</att>\n" + //
            "        <att name=\"platform\">null</att>\n" + //
            "        <att name=\"principal_year_day_for_collated_orbits\">null</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"start_time\">null</att>\n" + //
            "        <att name=\"stop_time\">null</att>\n" + //
            "        <att name=\"source\">null</att>\n" + //
            "        <att name=\"summary\">Daytime measurements from the Advanced Very High Resolution Radiometer (AVHRR) Pathfinder Version 5.2 L3-Collated (L3C) sea surface temperature. This netCDF-4 file contains sea surface temperature (SST) data produced as part of the AVHRR Pathfinder SST Project. These data were created using Version 5.2 of the Pathfinder algorithm and the file is nearly but not completely compliant with the Global High-Resolution Sea Surface Temperature (GHRSST) Data Specifications V2.0 (GDS2).  The file does not encode time according to the GDS2 specifications, and the sses_bias and sses_standard_deviation variables are empty.  Full compliance with GDS2 specifications will be achieved in the future Pathfinder Version 6. These data were created as a partnership between the University of Miami and the US NOAA/National Oceanographic Data Center (NODC).\n"
            + //
            "        \n" + //
            "2014-02-11 Files downloaded from https://data.nodc.noaa.gov/pathfinder/Version5.2 to NOAA NMFS SWFSC ERD by erd.data@noaa.gov . Aggregated with reference times from the dates in the file names. Removed other, misleading time values from the file names and the data files. Removed empty sses_bias and sses_standard_deviation variables. Removed aerosol_dynamic_indicator because of incorrect add_offset and scale_factor in files from 1981 through 2000.</att> \n"
            + //
            "        <att name=\"title\">SST, Pathfinder Ver 5.2 (L3C), Day, Global, 0.0417&deg;, 1981-2012, Science Quality (1 Day Composite)</att>\n"
            + //
            "        <att name=\"uuid\">null</att>\n" + //
            "    </addAttributes>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>time</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSize\" type=\"int\">1</att>\n" + //
            "            <att name=\"axis\">T</att>\n" + //
            "            <att name=\"calendar\">Gregorian</att>\n" + //
            "            <att name=\"comment\">This is the reference time of the SST file. Add sst_dtime to this value to get pixel-by-pixel times. Note: in PFV5.2 that sst_dtime is empty. PFV6 will contain the correct sst_dtime values.</att>\n"
            + //
            "            <att name=\"long_name\">reference time of SST file</att>\n" + //
            "            <att name=\"standard_name\">time</att>\n" + //
            "            <att name=\"units\">seconds since 1981-01-01 00:00:00</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSize\">null</att>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"calendar\">Gregorian</att>\n" + //
            "            <att name=\"comment\"></att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">Centered Time</att>\n" + //
            "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>lat</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"axis\">Y</att>\n" + //
            "            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" + //
            "            <att name=\"reference_datum\">geographical coordinates, WGS84 projection</att>\n" + //
            "            <att name=\"standard_name\">latitude</att>\n" + //
            "            <att name=\"units\">degrees_north</att>\n" + //
            "            <att name=\"valid_max\" type=\"double\">90.0</att>\n" + //
            "            <att name=\"valid_min\" type=\"double\">-90.0</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Latitude</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>lon</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"axis\">X</att>\n" + //
            "            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" + //
            "            <att name=\"reference_datum\">geographical coordinates, WGS84 projection</att>\n" + //
            "            <att name=\"standard_name\">longitude</att>\n" + //
            "            <att name=\"units\">degrees_east</att>\n" + //
            "            <att name=\"valid_max\" type=\"double\">180.0</att>\n" + //
            "            <att name=\"valid_min\" type=\"double\">-180.0</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Longitude</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sea_surface_temperature</sourceName>\n" + //
            "        <destinationName>sea_surface_temperature</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSize\" type=\"intList\">1 540 540</att>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">-32768</att>\n" + //
            "            <att name=\"add_offset\" type=\"double\">273.15</att>\n" + //
            "            <att name=\"comment\">Skin temperature of the ocean</att>\n" + //
            "            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" + //
            "            <att name=\"long_name\">NOAA Climate Data Record of sea surface skin temperature</att>\n" + //
            "            <att name=\"scale_factor\" type=\"double\">0.01</att>\n" + //
            "            <att name=\"standard_name\">sea_surface_skin_temperature</att>\n" + //
            "            <att name=\"units\">kelvin</att>\n" + //
            "            <att name=\"valid_max\" type=\"short\">4500</att>\n" + //
            "            <att name=\"valid_min\" type=\"short\">-180</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSize\">null</att>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"add_offset\" type=\"float\">0.0</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">NOAA Climate Data Record of Sea Surface Skin Temperature</att>\n" + //
            "            <att name=\"scale_factor\" type=\"float\">0.01</att>\n" + //
            "            <att name=\"units\">degree_C</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "<!-- sst_dtime is likewise some file specific time\n" + //
            "  int sst_dtime(time=1, lat=4320, lon=8640);\n" + //
            "      :long_name = \"time difference from reference time\";\n" + //
            "      :grid_mapping = \"Equidistant Cylindrical\";\n" + //
            "      :units = \"second\";\n" + //
            "      :add_offset = 0; // int\n" + //
            "      :scale_factor = 1; // int\n" + //
            "      :valid_min = -2147483647; // int\n" + //
            "      :valid_max = 2147483647; // int\n" + //
            "      :_FillValue = -2147483648; // int\n" + //
            "      :comment = \"time plus sst_dtime gives seconds after 1981-01-01 00:00:00. Note: in PFV5.2 this sst_dtime i\n"
            + //
            "s empty. PFV6 will contain the correct sst_dtime values.\";\n" + //
            "      :_ChunkSizes = 1, 540, 540; // int  -->\n" + //
            "\n" + //
            "    <!-- no data dataVariable>\n" + //
            "        <sourceName>sses_bias</sourceName>\n" + //
            "        <destinationName>sses_bias</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <!- - sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">1 540 540</att>\n" + //
            "            <att name=\"_FillValue\" type=\"byte\">-128</att>\n" + //
            "            <att name=\"add_offset\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"comment\">Bias estimate derived using the techniques described at http://www.ghrsst.org/S\n"
            + //
            "SES-Description-of-schemes.html. Note: in PFV5.2 this sses_bias is empty. PFV6 will contain the correct sses_bi\n"
            + //
            "as values.</att>\n" + //
            "            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" + //
            "            <att name=\"long_name\">SSES bias estimate</att>\n" + //
            "            <att name=\"scale_factor\" type=\"double\">0.02</att>\n" + //
            "            <att name=\"units\">kelvin</att>\n" + //
            "            <att name=\"valid_max\" type=\"byte\">127</att>\n" + //
            "            <att name=\"valid_min\" type=\"byte\">-127</att>\n" + //
            "        </sourceAttributes - ->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Statistics</att>\n" + //
            "            <att name=\"units\">degree_C</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sses_standard_deviation</sourceName>\n" + //
            "        <destinationName>sses_standard_deviation</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <!- - sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">1 540 540</att>\n" + //
            "            <att name=\"_FillValue\" type=\"byte\">-128</att>\n" + //
            "            <att name=\"add_offset\" type=\"double\">2.54</att>\n" + //
            "            <att name=\"comment\">Standard deviation estimate derived using the techniques described at http://ww\n"
            + //
            "w.ghrsst.org/SSES-Description-of-schemes.html. Note: in PFV5.2 this sses_standard_deviation is empty. PFV6 will\n"
            + //
            " contain the correct sses_standard_deviation values.</att>\n" + //
            "            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" + //
            "            <att name=\"long_name\">SSES standard deviation</att>\n" + //
            "            <att name=\"scale_factor\" type=\"double\">0.02</att>\n" + //
            "            <att name=\"units\">kelvin</att>\n" + //
            "            <att name=\"valid_max\" type=\"byte\">127</att>\n" + //
            "            <att name=\"valid_min\" type=\"byte\">-127</att>\n" + //
            "        </sourceAttributes - ->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Statistics</att>\n" + //
            "            <att name=\"units\">degree_C</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable -->    \n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>dt_analysis</sourceName>\n" + //
            "        <destinationName>dt_analysis</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSize\" type=\"intList\">1 540 540</att>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">-128</att>\n" + //
            "            <att name=\"add_offset\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"comment\">The difference between this SST and the previous day&#39;s SST.</att>\n"
            + //
            "            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" + //
            "            <att name=\"long_name\">Deviation from last SST analysis</att>\n" + //
            "            <att name=\"reference\">AVHRR_OI, with inland values populated from AVHRR_Pathfinder daily climatological SST. For more information on this reference field see http://accession.nodc.noaa.gov/0071180.</att>\n"
            + //
            "            <att name=\"scale_factor\" type=\"double\">0.1</att>\n" + //
            "            <att name=\"units\">kelvin</att>\n" + //
            "            <att name=\"valid_max\" type=\"byte\">127</att>\n" + //
            "            <att name=\"valid_min\" type=\"short\">-127</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSize\">null</att>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-5.0</att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">Deviation from Last SST Analysis (NCDC Daily OI)</att>\n" + //
            "            <att name=\"units\">degree_C</att>\n" + //
            "            <att name=\"reference\">null</att>\n" + //
            "            <att name=\"references\">AVHRR_OI, with inland values populated from AVHRR_Pathfinder daily climatological SST. For more information on this reference field see https://data.nodc.noaa.gov/cgi-bin/iso?id=gov.noaa.nodc:0071180.</att>\n"
            + //
            "            <att name=\"valid_max\">null</att>\n" + //
            "            <att name=\"valid_min\">null</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>wind_speed</sourceName>\n" + //
            "        <destinationName>wind_speed</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSize\" type=\"intList\">1 540 540</att>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">-128</att>\n" + //
            "            <att name=\"add_offset\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"comment\">These wind speeds were created by NCEP-DOE Atmospheric Model Intercomparison Project (AMIP-II) reanalysis (R-2) and represent winds at 10 metres above the sea surface.</att>\n"
            + //
            "            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" + //
            "            <att name=\"height\">10 m</att>\n" + //
            "            <att name=\"long_name\">10m wind speed</att>\n" + //
            "            <att name=\"scale_factor\" type=\"double\">1.0</att>\n" + //
            "            <att name=\"source\">NCEP/DOE AMIP-II Reanalysis (Reanalysis-2): u_wind.10m.gauss.2012.nc, v_wind.10m.gauss.2012.nc</att>\n"
            + //
            "            <att name=\"standard_name\">wind_speed</att>\n" + //
            "            <att name=\"time_offset\" type=\"double\">6.0</att>\n" + //
            "            <att name=\"units\">m s-1</att>\n" + //
            "            <att name=\"valid_max\" type=\"byte\">127</att>\n" + //
            "            <att name=\"valid_min\" type=\"short\">-127</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSize\">null</att>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"add_offset\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "            <att name=\"long_name\">10m Wind Speed</att>\n" + //
            "            <att name=\"scale_factor\">null</att>\n" + //
            "            <att name=\"valid_max\">null</att>\n" + //
            "            <att name=\"valid_min\">null</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sea_ice_fraction</sourceName>\n" + //
            "        <destinationName>sea_ice_fraction</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSize\" type=\"intList\">1 540 540</att>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">-128</att>\n" + //
            "            <att name=\"add_offset\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"comment\">Sea ice concentration data are taken from the EUMETSAT Ocean and Sea Ice Satellite Application Facility (OSISAF) Global Daily Sea Ice Concentration Reprocessing Data Set (http://accession.nodc.noaa.gov/0068294) when these data are available. The data are reprojected and interpolated from their original polar stereographic projection at 10km spatial resolution to the 4km Pathfinder Version 5.2 grid. When the OSISAF data are not available for both hemispheres on a given day, the sea ice concentration data are taken from the sea_ice_fraction variable found in the L4 GHRSST DailyOI SST product from NOAA/NCDC, and are interpolated from the 25km DailyOI grid to the 4km Pathfinder Version 5.2 grid.</att>\n"
            + //
            "            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" + //
            "            <att name=\"long_name\">sea ice fraction</att>\n" + //
            "            <att name=\"reference\">Reynolds, et al.(2006) Daily High-resolution Blended Analyses. Available at ftp://eclipse.ncdc.noaa.gov/pub/OI-daily/daily-sst.pdf</att>\n"
            + //
            "            <att name=\"scale_factor\" type=\"double\">0.01</att>\n" + //
            "            <att name=\"source\">NOAA/NESDIS/NCDC Daily optimum interpolation(OI) SST on 1/4-degree grid: 20121230-NCDC-L4LRblend-GLOB-v01-fv02_0-AVHRR_OI.nc.gz</att>\n"
            + //
            "            <att name=\"standard_name\">sea_ice_area_fraction</att>\n" + //
            "            <att name=\"time_offset\" type=\"double\">12.0</att>\n" + //
            "            <att name=\"valid_max\" type=\"byte\">127</att>\n" + //
            "            <att name=\"valid_min\" type=\"short\">-127</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSize\">null</att>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">1.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"comment\">Sea ice concentration data are taken from the EUMETSAT Ocean and Sea Ice Satellite Application Facility (OSISAF) Global Daily Sea Ice Concentration Reprocessing Data Set (https://data.nodc.noaa.gov/cgi-bin/iso?id=gov.noaa.nodc:0068294) when these data are available. The data are reprojected and interpolated from their original polar stereographic projection at 10km spatial resolution to the 4km Pathfinder Version 5.2 grid. When the OSISAF data are not available for both hemispheres on a given day, the sea ice concentration data are taken from the sea_ice_fraction variable found in the L4 GHRSST DailyOI SST product from NOAA/NCDC, and are interpolated from the 25km DailyOI grid to the 4km Pathfinder Version 5.2 grid.</att>\n"
            + //
            "            <att name=\"ioos_category\">Ice Distribution</att>\n" + //
            "            <att name=\"long_name\">Sea Ice Fraction</att>\n" + //
            "            <att name=\"units\">1</att>\n" + //
            "            <att name=\"reference\">null</att>\n" + //
            "            <att name=\"references\">Reynolds, et al.(2006) Daily High-resolution Blended Analyses. Available at https://journals.ametsoc.org/doi/full/10.1175/2007JCLI1824.1</att>\n"
            + //
            "            <att name=\"valid_max\">null</att>\n" + //
            "            <att name=\"valid_min\">null</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "<!-- Files for 1981 through 2000-12-31 have add_offset = 0\n" + //
            "     Files for 2001-01-01 through 2012-12-31 have add_offset = 1.1 \n" + //
            "     I believe the 1981 through 2000 files are incorrect. \n" + //
            "     There is no easy way to fix this because I'm reading files through .ncml,\n" + //
            "       which applies add_offset and scale_factor.\n" + //
            "     So remove the variable.\n" + //
            "- ->\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aerosol_dynamic_indicator</sourceName>\n" + //
            "        <destinationName>aerosol_dynamic_indicator</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSize\" type=\"intList\">1 540 540</att>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">-128</att>\n" + //
            "            <att name=\"add_offset\" type=\"double\">1.1</att>\n" + //
            "            <att name=\"comment\">Aerosol optical thickness (100 KM) data are taken from the CLASS AERO100 products, which are created from AVHRR channel 1 optical thickness retrievals from AVHRR global area coverage (GAC) data. The aerosol optical thickness measurements are interpolated from their original 1 degree x 1 degree resolution to the 4km Pathfinder Version 5.2 grid.</att>\n"
            + //
            "            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" + //
            "            <att name=\"long_name\">aerosol dynamic indicator</att>\n" + //
            "            <att name=\"reference\">http://www.class.ncdc.noaa.gov/saa/products/search?sub_id=0&amp;datatype_family=AERO100&amp;submit.x=25&amp;submit.y=12</att>\n"
            + //
            "            <att name=\"scale_factor\" type=\"double\">0.01</att>\n" + //
            "            <att name=\"source\">CLASS_AERO100_AOT</att>\n" + //
            "            <att name=\"time_offset\" type=\"double\">-83.0</att>\n" + //
            "            <att name=\"valid_max\" type=\"byte\">127</att>\n" + //
            "            <att name=\"valid_min\" type=\"short\">-127</att>\n" + //
            "        </sourceAttributes - ->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSize\">null</att>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"add_offset\" type=\"double\">1.1</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">1.6</att>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "            <att name=\"long_name\">Aerosol Dynamic Indicator</att>\n" + //
            "            <att name=\"reference\">null</att>\n" + //
            "            <att name=\"references\">https://www.class.ncdc.noaa.gov/saa/products/search?sub_id=0&amp;datatype_family=AERO100&amp;submit.x=25&amp;submit.y=12</att>\n"
            + //
            "            <att name=\"scale_factor\" type=\"double\">0.01</att>\n" + //
            "            <att name=\"valid_max\">null</att>\n" + //
            "            <att name=\"valid_min\">null</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable -->\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>quality_level</sourceName>\n" + //
            "        <destinationName>quality_level</destinationName> \n" + //
            "        <dataType>byte</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSize\" type=\"intList\">1 540 540</att>\n" + //
            "            <att name=\"_FillValue\" type=\"byte\">0</att>\n" + //
            "            <att name=\"comment\">Note, the native Pathfinder processing system returns quality levels ranging from 0 to 7 (7 is best quality; -1 represents missing data) and has been converted to the extent possible into the six levels required by the GDS2 (ranging from 0 to 5, where 5 is best). Below is the conversion table:\n"
            + //
            " GDS2 required quality_level 5  =  native Pathfinder quality level 7 == best_quality\n" + //
            " GDS2 required quality_level 4  =  native Pathfinder quality level 4-6 == acceptable_quality\n" + //
            " GDS2 required quality_level 3  =  native Pathfinder quality level 2-3 == low_quality\n" + //
            " GDS2 required quality_level 2  =  native Pathfinder quality level 1 == worst_quality\n" + //
            " GDS2 required quality_level 1  =  native Pathfinder quality level 0 = bad_data\n" + //
            " GDS2 required quality_level 0  =  native Pathfinder quality level -1 = missing_data\n" + //
            " The original Pathfinder quality level is recorded in the optional variable pathfinder_quality_level.</att>\n"
            + //
            "            <att name=\"flag_meanings\">bad_data worst_quality low_quality acceptable_quality best_quality</att>\n"
            + //
            "            <att name=\"flag_values\" type=\"byteList\">1 2 3 4 5</att>\n" + //
            "            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" + //
            "            <att name=\"long_name\">SST measurement quality</att>\n" + //
            "            <att name=\"valid_max\" type=\"byte\">5</att>\n" + //
            "            <att name=\"valid_min\" type=\"byte\">1</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSize\">null</att>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Quality</att>\n" + //
            "            <att name=\"long_name\">SST Measurement Quality</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>pathfinder_quality_level</sourceName>\n" + //
            "        <destinationName>pathfinder_quality_level</destinationName>\n" + //
            "        <dataType>byte</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSize\" type=\"intList\">1 540 540</att>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">-1</att>\n" + //
            "            <att name=\"comment\">This variable contains the native Pathfinder processing system quality levels, ranging from 0 to 7, where 0 is worst and 7 is best. And value -1 represents missing data.</att>\n"
            + //
            "            <att name=\"flag_meanings\">bad_data worst_quality low_quality low_quality acceptable_quality acceptable_quality acceptable_quality best_quality</att>\n"
            + //
            "            <att name=\"flag_values\" type=\"byteList\">0 1 2 3 4 5 6 7</att>\n" + //
            "            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" + //
            "            <att name=\"long_name\">Pathfinder SST quality flag</att>\n" + //
            "            <att name=\"valid_max\" type=\"byte\">7</att>\n" + //
            "            <att name=\"valid_min\" type=\"byte\">0</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSize\">null</att>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">7.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Quality</att>\n" + //
            "            <att name=\"long_name\">Pathfinder SST Quality Flag</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>l2p_flags</sourceName>\n" + //
            "        <destinationName>l2p_flags</destinationName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSize\" type=\"intList\">1 540 540</att>\n" + //
            "            <att name=\"comment\">Bit zero (0) is always set to zero to indicate infrared data. Bit one (1) is set to zero for any pixel over water (ocean, lakes and rivers). Land pixels were determined by rasterizing the Global Self-consistent Hierarchical High-resolution Shoreline (GSHHS) Database from the NOAA National Geophysical Data Center. Any 4 km Pathfinder pixel whose area is 50&#37; or more covered by land has bit one (1) set to 1. Bit two (2) is set to 1 when the sea_ice_fraction is 0.15 or greater. Bits three (3) and four (4) indicate lake and river pixels, respectively, and were determined by rasterizing the US World Wildlife Fund&#39;s Global Lakes and Wetlands Database. Any 4 km Pathfinder pixel whose area is 50&#37; or more covered by lake has bit three (3) set to 1. Any 4 km Pathfinder pixel whose area is 50&#37; or more covered by river has bit four (4) set to 1.</att>\n"
            + //
            "            <att name=\"flag_masks\" type=\"shortList\">1 2 4 8 16 32 64 128 256</att>\n" + //
            "            <att name=\"flag_meanings\">microwave land ice lake river reserved_for_future_use unused_currently unused_currently unused_currently</att>\n"
            + //
            "            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" + //
            "            <att name=\"long_name\">L2P flags</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">32767</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n"
            + //
            "            <att name=\"_ChunkSize\">null</att>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">16.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Quality</att>\n" + //
            "            <att name=\"long_name\">L2P Flags</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD gettestDataVarOrder() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDGridFromErddap\" datasetID=\"testDataVarOrder\">\n" + //
        "    <sourceUrl>https://coastwatch.pfeg.noaa.gov/erddap/griddap/jplG1SST</sourceUrl>\n" + //
        "</dataset>");
  }

  public static EDD getetopo180() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDGridFromEtopo\" datasetID=\"etopo180\" />");
  }

  public static EDD getetopo360() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDGridFromEtopo\" datasetID=\"etopo360\" />");
  }

  public static EDD getndbcCWind41002() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDGridAggregateExistingDimension\" datasetID=\"ndbcCWind41002\">\n" + //
            "    <dataset type=\"in.valid\" datasetID=\"a.test\" active=\"false\"> <test> </test> </dataset>\n" + //
            "    <dataset type=\"EDDGridFromDap\"                datasetID=\"ndbcCWind41002A\">\n" + //
            "        <sourceUrl>https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c1989.nc</sourceUrl>\n" + //
            "        <reloadEveryNMinutes>43200</reloadEveryNMinutes>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"cdm_data_type\">Grid</att>\n" + //
            "            <!-- att name=\"comment\">S HATTERAS - 250 NM East of Charleston, SC</att -->\n" + //
            "            <att name=\"contributor_name\">NOAA NDBC</att>\n" + //
            "            <att name=\"contributor_role\">Source of data.</att>\n" + //
            "            <att name=\"conventions\">null</att>\n" + //
            "            <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
            "            <att name=\"Metadata_Conventions\">null</att>\n" + //
            "            <att name=\"infoUrl\">https://www.ndbc.noaa.gov/cwind.shtml</att>\n" + //
            "            <att name=\"institution\">NOAA NDBC</att>\n" + //
            "            <att name=\"keywords\">Atmosphere &gt; Atmospheric Winds &gt; Surface Winds</att>\n" + //
            "            <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "            <att name=\"license\">[standard]</att>\n" + //
            "            <!-- att name=\"location\">32.27 N 75.42 W</att-->\n" + //
            "            <!-- att name=\"quality\">Automated QC checks with manual editing and comprehensive monthly QC</att -- >\n"
            + //
            "            <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "            <!-- att name=\"station\">41002</att -->\n" + //
            "            <att name=\"summary\">These continuous wind measurements from the NOAA National Data Buoy Center (NDBC) stations are 10-minute average values of wind speed (in m/s) and direction (in degrees clockwise from North).</att>\n"
            + //
            "            <att name=\"title\">Wind Data from NDBC 41002</att>\n" + //
            "            <att name=\"url\">https://dods.ndbc.noaa.gov</att>\n" + //
            "        </addAttributes>\n" + //
            "\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>wind_dir</sourceName>\n" + //
            "            <destinationName>wind_direction</destinationName>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"_FillValue\" type=\"int\">999</att>\n" + //
            "                <att name=\"missing_value\" type=\"int\">999</att>\n" + //
            "                <att name=\"ioos_category\">Wind</att>\n" + //
            "                <att name=\"long_name\">Wind From Direction</att>\n" + //
            "                <att name=\"standard_name\">wind_from_direction</att>\n" + //
            "                <att name=\"units\">degrees_true</att>\n" + //
            "            </addAttributes>\n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>wind_spd</sourceName>\n" + //
            "            <destinationName>wind_speed</destinationName>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"_FillValue\" type=\"float\">99.0</att>\n" + //
            "                <att name=\"missing_value\" type=\"float\">99.0</att>\n" + //
            "                <att name=\"ioos_category\">Wind</att>\n" + //
            "                <att name=\"long_name\">Wind Speed</att>\n" + //
            "                <att name=\"standard_name\">wind_speed</att>\n" + //
            "                <att name=\"units\">m s-1</att>\n" + //
            "            </addAttributes>\n" + //
            "        </dataVariable>\n" + //
            "    </dataset>\n" + //
            "    <!-- Setting ensureAxisValuesAreEqual to true (the default)\n" + //
            "       tells ERDDAP to ensure that the axis values (other than\n" + //
            "       axis variable #0) are equal for all of the datasets.\n" + //
            "       In rare cases, you may want to set this to false because\n" + //
            "       the datasets have *slight* (but acceptable) variations in the axis values.\n" + //
            "       For this dataset, the 2004+ datasets have slightly different lon\n" + //
            "       (-75.42 != -75.35) and lat (32.27 != 32.31) values,\n" + //
            "       so I'm skipping the test of equality. -->\n" + //
            "    <ensureAxisValuesAreEqual>false</ensureAxisValuesAreEqual>\n" + //
            "\n" + //
            "    <sourceUrl>https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c1992.nc</sourceUrl>\n" + //
            "    <sourceUrl>https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c1993.nc</sourceUrl>\n" + //
            "    <sourceUrl>https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c1996.nc</sourceUrl>\n" + //
            "    <sourceUrl>https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c1997.nc</sourceUrl>\n" + //
            "    <sourceUrl>https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c1998.nc</sourceUrl>\n" + //
            "    <sourceUrl>https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c1999.nc</sourceUrl>\n" + //
            "    <sourceUrl>https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c2000.nc</sourceUrl>\n" + //
            "    <sourceUrl>https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c2001.nc</sourceUrl>\n" + //
            "    <sourceUrl>https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c2002.nc</sourceUrl>\n" + //
            "    <sourceUrl>https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c2003.nc</sourceUrl>\n" + //
            "    <sourceUrl>https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c2004.nc</sourceUrl>\n" + //
            "    <sourceUrl>https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c2005.nc</sourceUrl>\n" + //
            "    <!-- 2006 has non-ascending-order time values\n" + //
            "    <sourceUrl>https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c2006.nc</sourceUrl -->\n" + //
            "    <sourceUrl>https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c2007.nc</sourceUrl>\n" + //
            "    <sourceUrl>https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c2008.nc</sourceUrl>\n" + //
            "    <sourceUrl>https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c2009.nc</sourceUrl>\n" + //
            "    <sourceUrl>https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c2010.nc</sourceUrl>\n" + //
            "    <!-- changes so hard to test sourceUrl>https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c9999.nc</sourceUrl -->\n"
            + //
            "</dataset>");
  }

  public static EDD getcwwcNDBCMet() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"cwwcNDBCMet\">\n" + //
        "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" + //
        "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/largeFiles/points/ndbcMet2/").toURI()).toString()
        + "</fileDir>\n" +
        "    <recursive>true</recursive>\n" + //
        "    <fileNameRegex>NDBC_.*\\.nc</fileNameRegex>\n" + //
        "    <!--fileTableInMemory>true</fileTableInMemory-->\n" + //
        "    <metadataFrom>last</metadataFrom>\n" + //
        "    <!--preExtractRegex>^NDBC_</preExtractRegex>\n" + //
        "    <postExtractRegex>_met\\.nc$</postExtractRegex>\n" + //
        "    <extractRegex>.*</extractRegex>\n" + //
        "    <columnNameForExtract>station</columnNameForExtract -->\n" + //
        "    <sortedColumnSourceName>TIME</sortedColumnSourceName>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" + //
        "        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" + //
        "        <att name=\"id\">cwwcNDBCMet</att>\n" + //
        "        <att name=\"license\">[standard]</att>\n" + //
        "        <att name=\"summary\">The National Data Buoy Center (NDBC) distributes meteorological data from\n" + //
        "moored buoys maintained by NDBC and others. Moored buoys are the weather\n" + //
        "sentinels of the sea. They are deployed in the coastal and offshore waters\n" + //
        "from the western Atlantic to the Pacific Ocean around Hawaii, and from the\n" + //
        "Bering Sea to the South Pacific. NDBC's moored buoys measure and transmit\n" + //
        "barometric pressure; wind direction, speed, and gust; air and sea\n" + //
        "temperature; and wave energy spectra from which significant wave height,\n" + //
        "dominant wave period, and average wave period are derived. Even the\n" + //
        "direction of wave propagation is measured on many moored buoys. See\n" + //
        "https://www.ndbc.noaa.gov/measdes.shtml for a description of the measurements.\n" + //
        "\n" + //
        "The source data from NOAA NDBC has different column names, different units,\n" + //
        "and different missing values in different files, and other problems\n" + //
        "(notably, lots of rows with duplicate or different values for the same time\n" + //
        "point). This dataset is a standardized, reformatted, and lightly edited\n" + //
        "version of that source data, created by NOAA NMFS SWFSC ERD (email:\n" + //
        "erd.data at noaa.gov). Before 2020-01-29, this dataset only had the data\n" + //
        "that was closest to a given hour, rounded to the nearest hour. Now, this\n" + //
        "dataset has all of the data available from NDBC with the original time\n" + //
        "values. If there are multiple source rows for a given buoy for a given\n" + //
        "time, only the row with the most non-NaN data values is kept. If there is\n" + //
        "a gap in the data, a row of missing values is inserted (which causes a nice\n" + //
        "gap when the data is graphed). Also, some impossible data values are\n" + //
        "removed, but this data is not perfectly clean. This dataset is now updated\n" + //
        "every 5 minutes.\n" + //
        "\n" + //
        "This dataset has both historical data (quality controlled, before\n" + //
        "2022-01-01T00:00:00Z) and near real time data (less quality controlled,\n" + //
        "which may change at any time, from 2022-01-01T00:00:00Z on).</att>\n" + //
        "        <att name=\"testOutOfDate\">now-25minutes</att>    \n" + //
        "    </addAttributes>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>ID</sourceName>\n" + //
        "        <destinationName>station</destinationName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>LON</sourceName>\n" + //
        "        <destinationName>longitude</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>LAT</sourceName>\n" + //
        "        <destinationName>latitude</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <!-- removed 2009-07-26 This is misleading at best. It is different for different stations\n" + //
        "         and even for different parameters in the dataset.)\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>DEPTH</sourceName>\n" + //
        "        <destinationName>depth</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>  -->\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>TIME</sourceName>\n" + //
        "        <destinationName>time</destinationName>\n" + //
        "        <dataType>double</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WD</sourceName>\n" + //
        "        <destinationName>wd</destinationName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WSPD</sourceName>\n" + //
        "        <destinationName>wspd</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>GST</sourceName>\n" + //
        "        <destinationName>gst</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WVHT</sourceName>\n" + //
        "        <destinationName>wvht</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>DPD</sourceName>\n" + //
        "        <destinationName>dpd</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>APD</sourceName>\n" + //
        "        <destinationName>apd</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>MWD</sourceName>\n" + //
        "        <destinationName>mwd</destinationName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>BAR</sourceName>\n" + //
        "        <destinationName>bar</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>ATMP</sourceName>\n" + //
        "        <destinationName>atmp</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WTMP</sourceName>\n" + //
        "        <destinationName>wtmp</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>DEWP</sourceName>\n" + //
        "        <destinationName>dewp</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>VIS</sourceName>\n" + //
        "        <destinationName>vis</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>PTDY</sourceName>\n" + //
        "        <destinationName>ptdy</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>TIDE</sourceName>\n" + //
        "        <destinationName>tide</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WSPU</sourceName>\n" + //
        "        <destinationName>wspu</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WSPV</sourceName>\n" + //
        "        <destinationName>wspv</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD geterdCinpKfmSFNH() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"erdCinpKfmSFNH\">\n" + //
        "    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" + //
        "    <fileDir>"
        + Path.of(EDDTestDataset.class.getResource("/data/points/KFMSizeFrequencyNaturalHabitat/").toURI()).toString()
        + "</fileDir>\n" + //
        "    <recursive>false</recursive>\n" + //
        "    <fileNameRegex>KFMSizeFrequencyNaturalHabitat_.*\\.nc(|.gz)</fileNameRegex>\n" + //
        "    <metadataFrom>last</metadataFrom>\n" + //
        "    <!-- preExtractRegex>^KFMSizeFrequencyNaturalHabitat_</preExtractRegex>\n" + //
        "    <postExtractRegex>\\.nc$</postExtractRegex>\n" + //
        "    <extractRegex>.*</extractRegex>\n" + //
        "    <columnNameForExtract>station</columnNameForExtract -->\n" + //
        "    <sortedColumnSourceName>TIME</sortedColumnSourceName>\n" + //
        "    <sortFilesBySourceNames>ID TIME</sortFilesBySourceNames>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"cdm_data_type\">TimeSeries</att>\n" + //
        "        <att name=\"cdm_timeseries_variables\">id, longitude, latitude</att>\n" + //
        "        <att name=\"subsetVariables\">id, longitude, latitude, common_name, species_name</att>\n" + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
        "        <att name=\"title\">Channel Islands, Kelp Forest Monitoring, Size and Frequency, Natural Habitat, 1985-2007</att>\n"
        + //
        "        <att name=\"contributor_email\">David_Kushner@nps.gov</att>\n" + //
        "        <att name=\"creator_email\">erd.data@noaa.gov</att>\n" + //
        "        <att name=\"creator_type\">institution</att>\n" + //
        "        <att name=\"id\" />\n" + //
        "        <att name=\"infoUrl\">https://www.nps.gov/chis/naturescience/index.htm</att>\n" + //
        "        <att name=\"institution\">CINP</att>\n" + //
        "        <att name=\"keywords\">\n" + //
        "Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Coastal Habitat,\n" + //
        "Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Marine Habitat,\n" + //
        "aquatic, atmosphere, biology, biosphere, channel, cinp, coastal, common, depth, ecosystems, forest, frequency, habitat, height, identifier, islands, kelp, marine, monitoring, name, natural, size, species, station, taxonomy, time</att>\n"
        + //
        "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
        "        <att name=\"sourceUrl\">(local files)</att>\n" + //
        "    </addAttributes>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>ID</sourceName>\n" + //
        "        <destinationName>id</destinationName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"cf_role\">timeseries_id</att>\n" + //
        "            <att name=\"units\" />\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>LON</sourceName>\n" + //
        "        <destinationName>longitude</destinationName>\n" + //
        "        <dataType>double</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">-120.4</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">-118.4</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>LAT</sourceName>\n" + //
        "        <destinationName>latitude</destinationName>\n" + //
        "        <dataType>double</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">32.5</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">34.5</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>DEPTH</sourceName>\n" + //
        "        <destinationName>depth</destinationName>\n" + //
        "        <dataType>double</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">20</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>TIME</sourceName>\n" + //
        "        <destinationName>time</destinationName>\n" + //
        "        <dataType>double</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">4.89024e8</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">1.183248E9</att>\n" + //
        "            <att name=\"units\">sec since 1970-01-01T00:00:00Z</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>CommonName</sourceName>\n" + //
        "        <destinationName>common_name</destinationName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Taxonomy</att>\n" + //
        "            <att name=\"long_name\">Common Name</att>\n" + //
        "            <att name=\"units\" />\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>Species_Name</sourceName>\n" + //
        "        <destinationName>species_name</destinationName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Taxonomy</att>\n" + //
        "            <att name=\"units\" />\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>Size</sourceName>\n" + //
        "        <destinationName>size</destinationName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"_FillValue\" type=\"short\">32767</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n"
        + //
        "            <att name=\"ioos_category\">Biology</att>\n" + //
        "            <att name=\"long_name\">Size</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD gettestNc2d() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"testNc2D\">\n" + //
        "    <nDimensions>2</nDimensions>\n" + //
        "    <reloadEveryNMinutes>10</reloadEveryNMinutes>\n" + //
        "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/largeFiles/points/nc2d/").toURI()).toString()
        + "</fileDir>\n" +
        "    <recursive>false</recursive>\n" + //
        "    <fileNameRegex>NDBC_.*\\.nc</fileNameRegex>\n" + //
        "    <metadataFrom>last</metadataFrom>\n" + //
        "    <preExtractRegex>^NDBC_</preExtractRegex>\n" + //
        "    <postExtractRegex>_met\\.nc$</postExtractRegex>\n" + //
        "    <extractRegex>.*</extractRegex>\n" + //
        "    <columnNameForExtract>station</columnNameForExtract>\n" + //
        "    <sortedColumnSourceName>TIME</sortedColumnSourceName>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"cdm_data_type\">Other</att>\n" + //
        "        <att name=\"subsetVariables\">station, latitude</att>\n" + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"date_created\">null</att>\n" + //
        "        <att name=\"date_issued\">null</att>\n" + //
        "        <att name=\"id\">null</att>\n" + //
        "        <att name=\"infoUrl\">https://www.ndbc.noaa.gov/</att>\n" + //
        "        <att name=\"creator_name\">NOAA NMFS SWFSC ERD</att>\n" + //
        "        <att name=\"creator_type\">institution</att>\n" + //
        "        <att name=\"creator_email\">erd.data@noaa.gov</att>\n" + //
        "        <att name=\"creator_url\">https://www.pfeg.noaa.gov</att>\n" + //
        "        <att name=\"publisher_name\">NOAA NMFS SWFSC ERD</att>\n" + //
        "        <att name=\"publisher_type\">institution</att>\n" + //
        "        <att name=\"publisher_email\">erd.data@noaa.gov</att>\n" + //
        "        <att name=\"publisher_url\">https://www.pfeg.noaa.gov</att>\n" + //
        "        <att name=\"institution\">NOAA NMFS SWFSC ERD</att>\n" + //
        "        <att name=\"keywords\">Oceans</att>\n" + //
        "        <att name=\"license\">[standard]</att>\n" + //
        "        <att name=\"sourceUrl\">https://www.ndbc.noaa.gov/</att>\n" + //
        "        <att name=\"title\">NDBC Standard Meteorological Buoy Data</att>\n" + //
        "    </addAttributes>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>station</sourceName>\n" + //
        "        <destinationName>station</destinationName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>LAT</sourceName>\n" + //
        "        <destinationName>latitude</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>TIME</sourceName>\n" + //
        "        <destinationName>time</destinationName>\n" + //
        "        <dataType>double</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"point_spacing\">null</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WD</sourceName>\n" + //
        "        <destinationName>wd</destinationName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">360</att>\n" + //
        "            <att name=\"_FillValue\" type=\"short\">32767</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WSPD</sourceName>\n" + //
        "        <destinationName>wspd</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">15</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>GST</sourceName>\n" + //
        "        <destinationName>gst</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">30</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WVHT</sourceName>\n" + //
        "        <destinationName>wvht</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">10</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>DPD</sourceName>\n" + //
        "        <destinationName>dpd</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">20</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>APD</sourceName>\n" + //
        "        <destinationName>apd</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">20</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>MWD</sourceName>\n" + //
        "        <destinationName>mwd</destinationName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">360</att>\n" + //
        "            <att name=\"_FillValue\" type=\"short\">32767</att>\n" + //
        "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>BAR</sourceName>\n" + //
        "        <destinationName>bar</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">950</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">1050</att>\n" + //
        "            <att name=\"comment\">Air pressure (hPa). ('PRES' on some NDBC tables.) For C-MAN sites and Great Lakes buoys, the recorded pressure is reduced to sea level using the method described in NWS Technical Procedures Bulletin 291 (11/14/80).</att>\n"
        + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Pressure</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>ATMP</sourceName>\n" + //
        "        <destinationName>atmp</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">40</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Temperature</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WTMP</sourceName>\n" + //
        "        <destinationName>wtmp</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">32</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Temperature</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>DEWP</sourceName>\n" + //
        "        <destinationName>dewp</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">40</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Meteorology</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>VIS</sourceName>\n" + //
        "        <destinationName>vis</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">100</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Meteorology</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>PTDY</sourceName>\n" + //
        "        <destinationName>ptdy</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">-3</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">3</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Pressure</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>TIDE</sourceName>\n" + //
        "        <destinationName>tide</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">-5</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">5</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Sea Level</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WSPU</sourceName>\n" + //
        "        <destinationName>wspu</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">-15</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">15</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WSPV</sourceName>\n" + //
        "        <destinationName>wspv</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">-15</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">15</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD gettestNc3d() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"testNc3D\">\n" + //
        "    <nDimensions>3</nDimensions>\n" + //
        "    <reloadEveryNMinutes>10</reloadEveryNMinutes>\n" + //
        "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/largeFiles/points/nc3d/").toURI()).toString()
        + "</fileDir>\n" +
        "    <recursive>false</recursive>\n" + //
        "    <fileNameRegex>NDBC_.*\\.nc</fileNameRegex>\n" + //
        "    <metadataFrom>last</metadataFrom>\n" + //
        "    <preExtractRegex>^NDBC_</preExtractRegex>\n" + //
        "    <postExtractRegex>_met\\.nc$</postExtractRegex>\n" + //
        "    <extractRegex>.*</extractRegex>\n" + //
        "    <columnNameForExtract>station</columnNameForExtract>\n" + //
        "    <sortedColumnSourceName>TIME</sortedColumnSourceName>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"cdm_data_type\">TimeSeries</att>\n" + //
        "        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" + //
        "        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"date_created\">null</att>\n" + //
        "        <att name=\"date_issued\">null</att>\n" + //
        "        <att name=\"id\">null</att>\n" + //
        "        <att name=\"infoUrl\">https://www.ndbc.noaa.gov/</att>\n" + //
        "        <att name=\"creator_name\">NOAA NMFS SWFSC ERD</att>\n" + //
        "        <att name=\"creator_type\">institution</att>\n" + //
        "        <att name=\"creator_email\">erd.data@noaa.gov</att>\n" + //
        "        <att name=\"creator_url\">https://www.pfeg.noaa.gov</att>\n" + //
        "        <att name=\"publisher_name\">NOAA NMFS SWFSC ERD</att>\n" + //
        "        <att name=\"publisher_type\">institution</att>\n" + //
        "        <att name=\"publisher_email\">erd.data@noaa.gov</att>\n" + //
        "        <att name=\"publisher_url\">https://www.pfeg.noaa.gov</att>\n" + //
        "        <att name=\"institution\">NOAA NMFS SWFSC ERD</att>\n" + //
        "        <att name=\"keywords\">Oceans</att>\n" + //
        "        <att name=\"license\">[standard]</att>\n" + //
        "        <att name=\"sourceUrl\">https://www.ndbc.noaa.gov/</att>\n" + //
        "        <att name=\"title\">NDBC Standard Meteorological Buoy Data</att>\n" + //
        "    </addAttributes>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>station</sourceName>\n" + //
        "        <destinationName>station</destinationName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"cf_role\">timeseries_id</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>LON</sourceName>\n" + //
        "        <destinationName>longitude</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>LAT</sourceName>\n" + //
        "        <destinationName>latitude</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>TIME</sourceName>\n" + //
        "        <destinationName>time</destinationName>\n" + //
        "        <dataType>double</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"point_spacing\">null</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WD</sourceName>\n" + //
        "        <destinationName>wd</destinationName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">360</att>\n" + //
        "            <att name=\"_FillValue\" type=\"short\">32767</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WSPD</sourceName>\n" + //
        "        <destinationName>wspd</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">15</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>GST</sourceName>\n" + //
        "        <destinationName>gst</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">30</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WVHT</sourceName>\n" + //
        "        <destinationName>wvht</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">10</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>DPD</sourceName>\n" + //
        "        <destinationName>dpd</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">20</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>APD</sourceName>\n" + //
        "        <destinationName>apd</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">20</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>MWD</sourceName>\n" + //
        "        <destinationName>mwd</destinationName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">360</att>\n" + //
        "            <att name=\"_FillValue\" type=\"short\">32767</att>\n" + //
        "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>BAR</sourceName>\n" + //
        "        <destinationName>bar</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">950</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">1050</att>\n" + //
        "            <att name=\"comment\">Air pressure (hPa). ('PRES' on some NDBC tables.) For C-MAN sites and Great Lakes buoys, the recorded pressure is reduced to sea level using the method described in NWS Technical Procedures Bulletin 291 (11/14/80).</att>\n"
        + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Pressure</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>ATMP</sourceName>\n" + //
        "        <destinationName>atmp</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">40</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Temperature</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WTMP</sourceName>\n" + //
        "        <destinationName>wtmp</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">32</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Temperature</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>DEWP</sourceName>\n" + //
        "        <destinationName>dewp</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">40</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Meteorology</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>VIS</sourceName>\n" + //
        "        <destinationName>vis</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">100</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Meteorology</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>PTDY</sourceName>\n" + //
        "        <destinationName>ptdy</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">-3</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">3</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Pressure</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>TIDE</sourceName>\n" + //
        "        <destinationName>tide</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">-5</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">5</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Sea Level</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WSPU</sourceName>\n" + //
        "        <destinationName>wspu</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">-15</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">15</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WSPV</sourceName>\n" + //
        "        <destinationName>wspv</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">-15</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">15</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD getpmelTaoDySst() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"pmelTaoDySst\" active=\"true\">\n" + //
            "    <reloadEveryNMinutes>1000000</reloadEveryNMinutes>\n" + //
            "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/points/tao/daily/").toURI()).toString()
            + "</fileDir>\n" + //
            "    <recursive>false</recursive>\n" + //
            "    <fileNameRegex>sst[0-9].*_dy\\.cdf</fileNameRegex>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <sortedColumnSourceName>time</sortedColumnSourceName>\n" + //
            "    <sortFilesBySourceNames>global:site_code time</sortFilesBySourceNames>\n" + //
            "    <defaultGraphQuery>longitude,latitude,T_25&amp;time&gt;=now-7days</defaultGraphQuery>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "        <att name=\"_FillValue\" type=\"float\">1.0E35</att>\n" + //
            "        <att name=\"CREATION_DATE\">07:37 12-JUL-2011</att>\n" + //
            "        <att name=\"Data_info\">Contact Paul Freitag: 206-526-6727</att>\n" + //
            "        <att name=\"Data_Source\">GTMBA Project Office/NOAA/PMEL</att>\n" + //
            "        <att name=\"File_info\">Contact: Dai.C.McClurg@noaa.gov</att>\n" + //
            "        <att name=\"missing_value\" type=\"float\">1.0E35</att>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"_FillValue\"></att>\n" + //
            "        <att name=\"CREATION_DATE\"></att>\n" + //
            "        <att name=\"platform_code\"></att>\n" + //
            "        <att name=\"site_code\"></att>\n" + //
            "        <att name=\"cdm_data_type\">TimeSeries</att>\n" + //
            "        <att name=\"cdm_timeseries_variables\">array, station, wmo_platform_code, longitude, latitude, depth</att>\n"
            + //
            "        <att name=\"subsetVariables\">array, station, wmo_platform_code, longitude, latitude, depth</att>\n"
            + //
            "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
            "        <att name=\"creator_name\">GTMBA Project Office/NOAA/PMEL</att>\n" + //
            "        <att name=\"creator_email\">Dai.C.McClurg@noaa.gov</att>\n" + //
            "        <att name=\"creator_type\">group</att>\n" + //
            "        <att name=\"creator_url\">https://www.pmel.noaa.gov/gtmba/mission</att>\n" + //
            "        <att name=\"id\"></att>\n" + //
            "        <att name=\"infoUrl\">https://www.pmel.noaa.gov/gtmba/mission</att>\n" + //
            "        <att name=\"institution\">NOAA PMEL, TAO/TRITON, RAMA, PIRATA</att>\n" + //
            "        <att name=\"keywords\">\n" + //
            "Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature,\n" + //
            "buoys, centered, daily, depth, identifier, noaa, ocean, oceans, pirata, pmel, quality, rama, sea, sea_surface_temperature, source, station, surface, tao, temperature, time, triton</att>\n"
            + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"license\">Request for Acknowledgement: If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\n"
            + //
            "\n" + //
            "[standard]</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"missing_value\"></att>\n" + //
            "        <att name=\"project\">TAO/TRITON, RAMA, PIRATA</att>\n" + //
            "        <att name=\"sourceUrl\">(local files)</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"summary\">This dataset has daily Sea Surface Temperature (SST) data from the\n" + //
            "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\n" + //
            "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\n" + //
            "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\n" + //
            "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\n"
            + //
            "https://www.pmel.noaa.gov/gtmba/mission .</att>\n" + //
            "        <att name=\"testOutOfDate\">now-3days</att>\n" + //
            "        <att name=\"title\">TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature</att>\n"
            + //
            "    </addAttributes>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:array</sourceName>\n" + //
            "        <destinationName>array</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Array</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:site_code</sourceName>\n" + //
            "        <destinationName>station</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"cf_role\">timeseries_id</att>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Station</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:wmo_platform_code</sourceName>\n" + //
            "        <destinationName>wmo_platform_code</destinationName>\n" + //
            "        <dataType>int</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">WMO Platform Code</att>\n" + //
            "            <att name=\"missing_value\" type=\"int\">2147483647</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>lon</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"epic_code\" type=\"int\">502</att>\n" + //
            "            <att name=\"type\">EVEN</att>\n" + //
            "            <att name=\"units\">degree_east</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"float\">1e35</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Nominal Longitude</att>\n" + //
            "            <att name=\"standard_name\">longitude</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>lat</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"epic_code\" type=\"int\">500</att>\n" + //
            "            <att name=\"type\">EVEN</att>\n" + //
            "            <att name=\"units\">degree_north</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"float\">1e35</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Nominal Latitude</att>\n" + //
            "            <att name=\"standard_name\">latitude</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>time</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"type\">EVEN</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"long_name\">Centered Time</att>\n" + //
            "            <att name=\"standard_name\">time</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>depth</sourceName>\n" + //
            "        <destinationName>depth</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"epic_code\" type=\"int\">3</att>\n" + //
            "            <att name=\"type\">EVEN</att>\n" + //
            "            <att name=\"units\">m</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"float\">1e35</att>\n" + //
            "            <att name=\"_CoordinateAxisType\">Height</att>\n" + //
            "            <att name=\"_CoordinateZisPositive\">down</att>\n" + //
            "            <att name=\"axis\">Z</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Depth</att>\n" + //
            "            <att name=\"positive\">down</att>\n" + //
            "            <att name=\"standard_name\">depth</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>T_25</sourceName>\n" + //
            "        <destinationName>T_25</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"epic_code\" type=\"int\">25</att>\n" + //
            "            <att name=\"generic_name\">temp</att>\n" + //
            "            <att name=\"long_name\">SST (C)</att>\n" + //
            "            <att name=\"name\">T</att>\n" + //
            "            <att name=\"units\">C</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"float\">1e35</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">32</att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">Sea Surface Temperature</att>\n" + //
            "            <att name=\"standard_name\">sea_surface_temperature</att>\n" + //
            "            <att name=\"units\">degree_C</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>QT_5025</sourceName>\n" + //
            "        <destinationName>QT_5025</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"epic_code\" type=\"int\">5025</att>\n" + //
            "            <att name=\"generic_name\">qt</att>\n" + //
            "            <att name=\"long_name\">SEA SURFACE TEMP QUALITY</att>\n" + //
            "            <att name=\"name\">QT</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"float\">1e35</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">6</att>\n" + //
            "            <att name=\"colorBarContinuous\">false</att>\n" + //
            "            <att name=\"description\">Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QT_5025&gt;=1 and QT_5025&lt;=3.</att>\n"
            + //
            "            <att name=\"ioos_category\">Quality</att>\n" + //
            "            <att name=\"long_name\">Sea Surface Temperature Quality</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>ST_6025</sourceName>\n" + //
            "        <destinationName>ST_6025</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"epic_code\" type=\"int\">6025</att>\n" + //
            "            <att name=\"generic_name\">st</att>\n" + //
            "            <att name=\"long_name\">SEA SURFACE TEMP SOURCE</att>\n" + //
            "            <att name=\"name\">ST</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"float\">1e35</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">8</att>\n" + //
            "            <att name=\"colorBarContinuous\">false</att>\n" + //
            "            <att name=\"description\">Source Codes:\n" + //
            "0 = No Sensor, No Data\n" + //
            "1 = Real Time (Telemetered Mode)\n" + //
            "2 = Derived from Real Time\n" + //
            "3 = Temporally Interpolated from Real Time\n" + //
            "4 = Source Code Inactive at Present\n" + //
            "5 = Recovered from Instrument RAM (Delayed Mode)\n" + //
            "6 = Derived from RAM\n" + //
            "7 = Temporally Interpolated from RAM</att>\n" + //
            "            <att name=\"ioos_category\">Other</att>\n" + //
            "            <att name=\"long_name\">Sea Surface Temperature Source</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD gettestGlobal() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"testGlobal\">    \n" + //
        "    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" + //
        "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/points/erdCalcofiSub/").toURI()).toString()
        + "</fileDir>\n" + //
        "    <recursive>false</recursive>\n" + //
        "    <fileNameRegex>.*\\.nc</fileNameRegex>\n" + //
        "    <metadataFrom>last</metadataFrom>\n" + //
        "    <sortedColumnSourceName>time</sortedColumnSourceName>\n" + //
        "    <sortFilesBySourceNames>lineStation</sortFilesBySourceNames>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"cdm_data_type\">TimeSeriesProfile</att>\n" + //
        "        <att name=\"cdm_timeseries_variables\">line_station, line, station</att>\n" + //
        "        <att name=\"cdm_profile_variables\">longitude, latitude, time</att>\n" + //
        "        <att name=\"subsetVariables\">line_station, line, station, longitude, latitude, time</att>\n" + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"depth_range\">null</att>\n" + //
        "        <att name=\"infoUrl\">http://www.calcofi.org</att> <!-- more specific??? -->\n" + //
        "        <att name=\"institution\">CalCOFI</att>\n" + //
        "        <att name=\"keywords\">Oceans &gt; Salinity/Density &gt; Salinity</att>\n" + //
        "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
        "        <att name=\"lat_range\">null</att>\n" + //
        "        <att name=\"license\">[standard]</att>\n" + //
        "        <att name=\"lon_range\">null</att>\n" + //
        "        <att name=\"sourceUrl\">(local files)</att>\n" + //
        "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
        "        <att name=\"summary\">This is the CalCOFI subsurface physical data. Routine oceanographic sampling within the California Current System has occurred under the auspices of the California Cooperative Oceanic Fisheries Investigations (CalCOFI) since 1949, providing one of the longest existing time-series of the physics, chemistry and biology of a dynamic oceanic regime.</att>\n"
        + //
        "        <att name=\"time_range\">null</att>\n" + //
        "        <att name=\"title\">CalCOFI Subsurface Physical Data, 1949-1998</att>\n" + //
        "    </addAttributes>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>global:id</sourceName>\n" + //
        "        <destinationName>ID</destinationName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>lineStation</sourceName>\n" + //
        "        <destinationName>line_station</destinationName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"long_name\">CalCOFI Line + Station</att>\n" + //
        "            <att name=\"cf_role\">timeseries_id</att>\n" + //
        "            <att name=\"units\">null</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>stationline</sourceName>\n" + //
        "        <destinationName>line</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"long_name\">CalCOFI Line Number</att>\n" + //
        "            <att name=\"units\">null</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>stationnum</sourceName>\n" + //
        "        <destinationName>station</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"long_name\">CalCOFI Station Number</att>\n" + //
        "            <att name=\"units\">null</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>lon</sourceName>\n" + //
        "        <destinationName>longitude</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">200</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">250</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>lat</sourceName>\n" + //
        "        <destinationName>latitude</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">20</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">50</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>time</sourceName>\n" + //
        "        <dataType>double</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"standard_name\">time</att>\n" + //
        "            <att name=\"cf_role\">profile_id</att>\n" + //
        "            <att name=\"units\">seconds since 1948-01-01</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>depth</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">5000</att>\n" + //
        "            <att name=\"units\">m</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>chl</sourceName>\n" + //
        "        <destinationName>chlorophyll</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"has_data\"></att>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">.03</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">30</att>\n" + //
        "            <att name=\"colorBarScale\">Log</att>\n" + //
        "            <att name=\"ioos_category\">Ocean Color</att>\n" + //
        "            <att name=\"long_name\">Chlorophyll</att>\n" + //
        "            <att name=\"standard_name\">concentration_of_chlorophyll_in_sea_water</att>\n" + //
        "            <att name=\"units\">mg m-3</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>dark</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"has_data\"></att>\n" + //
        "            <att name=\"ioos_category\">Optical Properties</att>\n" + //
        "            <att name=\"long_name\">Dark</att>\n" + //
        "            <!-- att name=\"standard_name\">???</att -->\n" + //
        "            <att name=\"units\">mg m-3 experiment-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>lightpercent</sourceName>\n" + //
        "        <destinationName>light_percent</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"has_data\"></att>\n" + //
        "            <att name=\"ioos_category\">Optical Properties</att>\n" + //
        "            <att name=\"long_name\">Light Percent</att>\n" + //
        "            <!-- att name=\"standard_name\">???</att -->\n" + //
        "            <att name=\"units\">mg m-3 experiment-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>nh3</sourceName>\n" + //
        "        <destinationName>NH3</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"has_data\"></att>\n" + //
        "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n" + //
        "            <att name=\"long_name\">Ammonia</att>\n" + //
        "            <att name=\"standard_name\">mole_concentration_of_ammonium_in_sea_water</att> <!--should be ammonia-->\n"
        + //
        "            <att name=\"units\">ugram-atoms L-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>no2</sourceName>\n" + //
        "        <destinationName>NO2</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"has_data\"></att>\n" + //
        "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n" + //
        "            <att name=\"long_name\">Nitrite</att>\n" + //
        "            <att name=\"standard_name\">moles_of_nitrite_per_unit_mass_in_sea_water</att>\n" + //
        "            <att name=\"units\">ugram-atoms L-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>no3</sourceName>\n" + //
        "        <destinationName>NO3</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"has_data\"></att>\n" + //
        "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n" + //
        "            <att name=\"long_name\">Nitrate</att>\n" + //
        "            <att name=\"standard_name\">moles_of_nitrate_per_unit_mass_in_sea_water</att>\n" + //
        "            <att name=\"units\">ugram-atoms L-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>oxygen</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"has_data\"></att>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">10</att>\n" + //
        "            <att name=\"ioos_category\">Dissolved O2</att>\n" + //
        "            <att name=\"long_name\">Oxygen</att>\n" + //
        "            <att name=\"standard_name\">volume_fraction_of_oxygen_in_sea_water</att>\n" + //
        "            <att name=\"units\">mL L-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>po4</sourceName>\n" + //
        "        <destinationName>PO4</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"has_data\"></att>\n" + //
        "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n" + //
        "            <att name=\"long_name\">Phosphate</att>\n" + //
        "            <att name=\"standard_name\">moles_of_phosphate_per_unit_mass_in_sea_water</att>\n" + //
        "            <att name=\"units\">ugram-atoms L-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>pressure</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"has_data\"></att>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">5000.0</att>\n" + //
        "            <att name=\"ioos_category\">Pressure</att>\n" + //
        "            <att name=\"long_name\">Pressure</att>\n" + //
        "            <att name=\"standard_name\">sea_water_pressure</att>\n" + //
        "            <att name=\"units\">dbar</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>primprod</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"has_data\"></att>\n" + //
        "            <att name=\"ioos_category\">Productivity</att>\n" + //
        "            <att name=\"long_name\">Mean Primary Production (C14 Assimilation)</att>\n" + //
        "            <att name=\"standard_name\">net_primary_productivity_of_carbon</att>\n" + //
        "            <att name=\"units\">mg m-3 experiment-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>salinity</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"has_data\"></att>\n" + //
        "            <att name=\"ioos_category\">Salinity</att>\n" + //
        "            <att name=\"long_name\">Salinity</att>\n" + //
        "            <att name=\"standard_name\">sea_water_salinity</att>\n" + //
        "            <att name=\"units\">PSU</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>silicate</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"has_data\"></att>\n" + //
        "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n" + //
        "            <att name=\"long_name\">Silicate</att>\n" + //
        "            <att name=\"standard_name\">moles_of_silicate_per_unit_mass_in_sea_water</att>\n" + //
        "            <att name=\"units\">ugram-atoms L-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>temperature</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"has_data\"></att>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" + //
        "            <att name=\"ioos_category\">Temperature</att>\n" + //
        "            <att name=\"long_name\">Sea Water Temperature</att>\n" + //
        "            <att name=\"standard_name\">sea_water_temperature</att>\n" + //
        "            <att name=\"units\">degree_C</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD getminiNdbc() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"miniNdbc\">\n" + //
        "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" + //
        "    <updateEveryNMillis>1</updateEveryNMillis>\n" + //
        "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/miniNdbc/").toURI()).toString()
        + "</fileDir>\n" + //
        "    <recursive>false</recursive>\n" + //
        "    <fileNameRegex>NDBC_.*\\.nc</fileNameRegex>\n" + //
        "    <!--fileTableInMemory>true</fileTableInMemory-->\n" + //
        "    <metadataFrom>last</metadataFrom>\n" + //
        "    <preExtractRegex>^NDBC_</preExtractRegex>\n" + //
        "    <postExtractRegex>_met\\.nc$</postExtractRegex>\n" + //
        "    <extractRegex>.*</extractRegex>\n" + //
        "    <columnNameForExtract>station</columnNameForExtract>\n" + //
        "    <sortedColumnSourceName>TIME</sortedColumnSourceName>\n" + //
        "    <sortFilesBySourceNames>station TIME</sortFilesBySourceNames>\n" + //
        "    <addVariablesWhere>ioos_category, units</addVariablesWhere>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"cdm_data_type\">TimeSeries</att>\n" + //
        "        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" + //
        "        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"date_created\">null</att>\n" + //
        "        <att name=\"date_issued\">null</att>\n" + //
        "        <att name=\"id\">null</att>\n" + //
        "        <att name=\"infoUrl\">https://www.ndbc.noaa.gov/</att>\n" + //
        "        <att name=\"institution\">NOAA NDBC, CoastWatch WCN</att>\n" + //
        "        <att name=\"keywords\">\n" + //
        "Atmosphere &gt; Air Quality &gt; Visibility,\n" + //
        "Atmosphere &gt; Altitude &gt; Planetary Boundary Layer Height,\n" + //
        "Atmosphere &gt; Atmospheric Pressure &gt; Atmospheric Pressure Measurements,\n" + //
        "Atmosphere &gt; Atmospheric Pressure &gt; Pressure Tendency,\n" + //
        "Atmosphere &gt; Atmospheric Pressure &gt; Sea Level Pressure,\n" + //
        "Atmosphere &gt; Atmospheric Pressure &gt; Static Pressure,\n" + //
        "Atmosphere &gt; Atmospheric Temperature &gt; Air Temperature,\n" + //
        "Atmosphere &gt; Atmospheric Temperature &gt; Dew Point Temperature,\n" + //
        "Atmosphere &gt; Atmospheric Water Vapor &gt; Dew Point Temperature,\n" + //
        "Atmosphere &gt; Atmospheric Winds &gt; Surface Winds,\n" + //
        "Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature,\n" + //
        "Oceans &gt; Ocean Waves &gt; Significant Wave Height,\n" + //
        "Oceans &gt; Ocean Waves &gt; Swells,\n" + //
        "Oceans &gt; Ocean Waves &gt; Wave Period,\n" + //
        "air, air_pressure_at_sea_level, air_temperature, atmosphere, atmospheric, average, boundary, buoy, coastwatch, data, dew point, dew_point_temperature, direction, dominant, eastward, eastward_wind, from, gust, height, identifier, layer, level, measurements, meridional, meteorological, meteorology, name, ndbc, noaa, northward, northward_wind, ocean, oceans, period, planetary, pressure, quality, sea, sea level, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sea_surface_temperature, seawater, significant, speed, sst, standard, static, station, surface, surface waves, surface_altitude, swell, swells, temperature, tendency, tendency_of_air_pressure, time, vapor, visibility, visibility_in_air, water, wave, waves, wcn, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds, zonal</att>\n"
        + //
        "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
        "        <att name=\"license\">[standard]</att>\n" + //
        "        <att name=\"sourceUrl\">https://www.ndbc.noaa.gov/</att>\n" + //
        "        <att name=\"summary\">The National Data Buoy Center (NDBC) distributes meteorological data from\n" + //
        "moored buoys maintained by NDBC and others. Moored buoys are the weather\n" + //
        "sentinels of the sea. They are deployed in the coastal and offshore waters\n" + //
        "from the western Atlantic to the Pacific Ocean around Hawaii, and from the\n" + //
        "Bering Sea to the South Pacific. NDBC's moored buoys measure and transmit\n" + //
        "barometric pressure; wind direction, speed, and gust; air and sea\n" + //
        "temperature; and wave energy spectra from which significant wave height,\n" + //
        "dominant wave period, and average wave period are derived. Even the\n" + //
        "direction of wave propagation is measured on many moored buoys.\n" + //
        "\n" + //
        "The data is from NOAA NDBC. It has been reformatted by NOAA Coastwatch,\n" + //
        "West Coast Node. This dataset only has the data that is closest to a\n" + //
        "given hour. The time values in the dataset are rounded to the nearest hour.\n" + //
        "\n" + //
        "This dataset has both historical data (quality controlled, before\n" + //
        "2015-01-01T00:00:00Z) and near real time data (less quality controlled, from\n" + //
        "2015-01-01T00:00:00Z on).</att>\n" + //
        "        <att name=\"title\">NDBC Standard Meteorological Buoy Data</att>\n" + //
        "    </addAttributes>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>station</sourceName>\n" + //
        "        <destinationName>station</destinationName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"long_name\">Station Name</att>\n" + //
        "            <att name=\"cf_role\">timeseries_id</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>LON</sourceName>\n" + //
        "        <destinationName>longitude</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>LAT</sourceName>\n" + //
        "        <destinationName>latitude</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>TIME</sourceName>\n" + //
        "        <destinationName>time</destinationName>\n" + //
        "        <dataType>double</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"point_spacing\">null</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>=7</sourceName>\n" + //
        "        <destinationName>luckySeven</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"comment\">fixed value</att>\n" + //
        "            <att name=\"ioos_category\">Other</att>\n" + //
        "            <att name=\"units\">m</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>global:geospatial_lon_min</sourceName>\n" + //
        "        <destinationName>geolon</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Location</att>\n" + //
        "            <att name=\"units\">degrees_north</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>global:geospatial_lat_min</sourceName>\n" + //
        "        <destinationName>geolat</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Location</att>\n" + //
        "            <att name=\"units\">degrees_north</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WD</sourceName>\n" + //
        "        <destinationName>wd</destinationName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">360</att>\n" + //
        "            <att name=\"_FillValue\" type=\"short\">32767</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WSPD</sourceName>\n" + //
        "        <destinationName>wspd</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">15</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>GST</sourceName>\n" + //
        "        <destinationName>gst</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">30</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WVHT</sourceName>\n" + //
        "        <destinationName>wvht</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">10</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>DPD</sourceName>\n" + //
        "        <destinationName>dpd</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">20</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>APD</sourceName>\n" + //
        "        <destinationName>apd</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">20</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>MWD</sourceName>\n" + //
        "        <destinationName>mwd</destinationName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">360</att>\n" + //
        "            <att name=\"_FillValue\" type=\"short\">32767</att>\n" + //
        "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>BAR</sourceName>\n" + //
        "        <destinationName>bar</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">950</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">1050</att>\n" + //
        "            <!-- original comment had odd single quote characters -->\n" + //
        "            <att name=\"comment\">Air pressure (hPa). ('PRES' on some NDBC tables.) For C-MAN sites and Great Lakes buoys, the recorded pressure is reduced to sea level using the method described in NWS Technical Procedures Bulletin 291 (11/14/80).</att>\n"
        + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Pressure</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>ATMP</sourceName>\n" + //
        "        <destinationName>atmp</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">40</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Temperature</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WTMP</sourceName>\n" + //
        "        <destinationName>wtmp</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">32</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Temperature</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>DEWP</sourceName>\n" + //
        "        <destinationName>dewp</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">40</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Meteorology</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>VIS</sourceName>\n" + //
        "        <destinationName>vis</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">100</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Meteorology</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>PTDY</sourceName>\n" + //
        "        <destinationName>ptdy</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">-3</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">3</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Pressure</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>TIDE</sourceName>\n" + //
        "        <destinationName>tide</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">-5</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">5</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Sea Level</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WSPU</sourceName>\n" + //
        "        <destinationName>wspu</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">-15</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">15</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WSPV</sourceName>\n" + //
        "        <destinationName>wspv</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">-15</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">15</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD gettestGlobecBottle() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"testGlobecBottle\">\n" + //
        "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/points/globec/").toURI()).toString()
        + "</fileDir>\n" + //
        "    <fileNameRegex>Globec_bottle_data_2002\\.nc</fileNameRegex>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"title\">GLOBEC NEP Rosette Bottle Data (2002)</att>\n" + //
        "        <att name=\"cdm_data_type\">TrajectoryProfile</att>\n" + //
        "        <att name=\"cdm_trajectory_variables\">cruise_id, ship</att>\n" + //
        "        <att name=\"cdm_profile_variables\">cast, longitude, latitude, time</att>\n" + //
        "        <att name=\"subsetVariables\">cruise_id, ship, cast, longitude, latitude, time</att>\n" + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"infoUrl\">https://en.wikipedia.org/wiki/Global_Ocean_Ecosystem_Dynamics</att>\n" + //
        "        <att name=\"institution\">GLOBEC</att>\n" + //
        "        <att name=\"keywords\">10um,\n" + //
        "Biosphere &gt; Vegetation &gt; Photosynthetically Active Radiation,\n" + //
        "Oceans &gt; Ocean Chemistry &gt; Ammonia,\n" + //
        "Oceans &gt; Ocean Chemistry &gt; Chlorophyll,\n" + //
        "Oceans &gt; Ocean Chemistry &gt; Nitrate,\n" + //
        "Oceans &gt; Ocean Chemistry &gt; Nitrite,\n" + //
        "Oceans &gt; Ocean Chemistry &gt; Nitrogen,\n" + //
        "Oceans &gt; Ocean Chemistry &gt; Oxygen,\n" + //
        "Oceans &gt; Ocean Chemistry &gt; Phosphate,\n" + //
        "Oceans &gt; Ocean Chemistry &gt; Pigments,\n" + //
        "Oceans &gt; Ocean Chemistry &gt; Silicate,\n" + //
        "Oceans &gt; Ocean Optics &gt; Attenuation/Transmission,\n" + //
        "Oceans &gt; Ocean Temperature &gt; Water Temperature,\n" + //
        "Oceans &gt; Salinity/Density &gt; Salinity,\n" + //
        "active, after, ammonia, ammonium, attenuation, biosphere, bottle, cast, chemistry, chlorophyll, chlorophyll-a, color, concentration, concentration_of_chlorophyll_in_sea_water, cruise, data, density, dissolved, dissolved nutrients, dissolved o2, fluorescence, fraction, from, globec, identifier, mass, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, moles, moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water, n02, nep, nh4, nitrate, nitrite, nitrogen, no3, number, nutrients, o2, ocean, ocean color, oceans, optical, optical properties, optics, oxygen, passing, per, phaeopigments, phosphate, photosynthetically, pigments, plus, po4, properties, radiation, rosette, salinity, screen, sea, sea_water_practical_salinity, sea_water_temperature, seawater, sensor, sensors, ship, silicate, temperature, time, total, transmission, transmissivity, unit, vegetation, voltage, volume, volume_fraction_of_oxygen_in_sea_water, water</att>\n"
        + //
        "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
        "        <att name=\"license\">[standard]</att>\n" + //
        "        <att name=\"observationDimension\">null</att>\n" + //
        "        <att name=\"sourceUrl\">(local files; contact erd.data@noaa.gov)</att>\n" + //
        "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
        "        <att name=\"summary\">\n" + //
        "GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n" + //
        "Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).\n" + //
        "Notes:\n" + //
        "Physical data processed by Jane Fleischbein (OSU).\n" + //
        "Chlorophyll readings done by Leah Feinberg (OSU).\n" + //
        "Nutrient analysis done by Burke Hales (OSU).\n" + //
        "Sal00 - salinity calculated from primary sensors (C0,T0).\n" + //
        "Sal11 - salinity calculated from secondary sensors (C1,T1).\n" + //
        "secondary sensor pair was used in final processing of CTD data for\n" + //
        "most stations because the primary had more noise and spikes. The\n" + //
        "primary pair were used for cast #9, 24, 48, 111 and 150 due to\n" + //
        "multiple spikes or offsets in the secondary pair.\n" + //
        "Nutrient samples were collected from most bottles; all nutrient data\n" + //
        "developed from samples frozen during the cruise and analyzed ashore;\n" + //
        "data developed by Burke Hales (OSU).\n" + //
        "Operation Detection Limits for Nutrient Concentrations\n" + //
        "Nutrient  Range         Mean    Variable         Units\n" + //
        "PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\n" + //
        "N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\n" + //
        "Si        0.13-0.24     0.16    Silicate         micromoles per liter\n" + //
        "NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\n" + //
        "Dates and Times are UTC.\n" + //
        "\n" + //
        "For more information, see https://www.bco-dmo.org/dataset/2452\n" + //
        "\n" + //
        "Inquiries about how to access this data should be directed to\n" + //
        "Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\n" + //
        "</att>\n" + //
        "    </addAttributes>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>cruise_id</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"cf_role\">trajectory_id</att>\n" + //
        "            <att name=\"long_name\">Cruise ID</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>ship</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"long_name\">Ship</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>cast_no</sourceName>\n" + //
        "        <destinationName>cast</destinationName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">140</att>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"long_name\">Cast Number</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>lon100</sourceName>\n" + //
        "        <destinationName>longitude</destinationName>\n" + //
        "        <dataType>int</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"scale_factor\" type=\"float\">0.01</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>lat100</sourceName>\n" + //
        "        <destinationName>latitude</destinationName>\n" + //
        "        <dataType>int</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"scale_factor\" type=\"float\">0.01</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>=0</sourceName>\n" + //
        "        <destinationName>altitude</destinationName>\n" + //
        "        <dataType>int</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"_FillValue\">null</att>\n" + //
        "            <att name=\"long_name\">Altitude</att>\n" + //
        "            <att name=\"units\">m</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>datetime_epoch</sourceName>\n" + //
        "        <destinationName>time</destinationName>\n" + //
        "        <dataType>int</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"long_name\">Time</att>\n" + //
        "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" + //
        "            <att name=\"standard_name\">time</att>\n" + //
        "            <att name=\"cf_role\">profile_id</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>bottle_posn</sourceName>\n" + //
        "        <dataType>byte</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">12</att>\n" + //
        "            <att name=\"ioos_category\">Location</att>\n" + //
        "            <att name=\"long_name\">Bottle Number</att>\n" + //
        "            <att name=\"missing_value\" type=\"byte\">-128</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>chl_a_total</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">.03</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">30</att>\n" + //
        "            <att name=\"colorBarScale\">Log</att>\n" + //
        "            <att name=\"ioos_category\">Ocean Color</att>\n" + //
        "            <att name=\"long_name\">Chlorophyll-a</att>\n" + //
        "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
        "            <att name=\"standard_name\">concentration_of_chlorophyll_in_sea_water</att>\n" + //
        "            <att name=\"units\">ug L-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>chl_a_10um</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">.03</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">30</att>\n" + //
        "            <att name=\"colorBarScale\">Log</att>\n" + //
        "            <att name=\"ioos_category\">Ocean Color</att>\n" + //
        "            <att name=\"long_name\">Chlorophyll-a after passing 10um screen</att>\n" + //
        "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
        "            <att name=\"standard_name\">concentration_of_chlorophyll_in_sea_water</att>\n" + //
        "            <att name=\"units\">ug L-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>phaeo_total</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">.03</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">30</att>\n" + //
        "            <att name=\"colorBarScale\">Log</att>\n" + //
        "            <att name=\"ioos_category\">Ocean Color</att>\n" + //
        "            <att name=\"long_name\">Total Phaeopigments</att>\n" + //
        "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
        "            <att name=\"units\">ug L-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>phaeo_10um</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">.03</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">30</att>\n" + //
        "            <att name=\"colorBarScale\">Log</att>\n" + //
        "            <att name=\"ioos_category\">Ocean Color</att>\n" + //
        "            <att name=\"long_name\">Phaeopigments 10um</att>\n" + //
        "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
        "            <att name=\"units\">ug L-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>sal00</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n" + //
        "            <att name=\"ioos_category\">Salinity</att>\n" + //
        "            <att name=\"long_name\">Practical Salinity from T0 and C0 Sensors</att>\n" + //
        "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
        "            <att name=\"standard_name\">sea_water_practical_salinity</att>\n" + //
        "            <att name=\"units\">PSU</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>sal11</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n" + //
        "            <att name=\"ioos_category\">Salinity</att>\n" + //
        "            <att name=\"long_name\">Practical Salinity from T1 and C1 Sensors</att>\n" + //
        "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
        "            <att name=\"standard_name\">sea_water_practical_salinity</att>\n" + //
        "            <att name=\"units\">PSU</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>t0</sourceName>\n" + //
        "        <destinationName>temperature0</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" + //
        "            <att name=\"ioos_category\">Temperature</att>\n" + //
        "            <att name=\"long_name\">Sea Water Temperature from T0 Sensor</att>\n" + //
        "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
        "            <att name=\"standard_name\">sea_water_temperature</att>\n" + //
        "            <att name=\"units\">degree_C</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>t1</sourceName>\n" + //
        "        <destinationName>temperature1</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" + //
        "            <att name=\"ioos_category\">Temperature</att>\n" + //
        "            <att name=\"long_name\">Sea Water Temperature from T1 Sensor</att>\n" + //
        "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
        "            <att name=\"standard_name\">sea_water_temperature</att>\n" + //
        "            <att name=\"units\">degree_C</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>fluor_v</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">5</att>\n" + //
        "            <att name=\"ioos_category\">Ocean Color</att>\n" + //
        "            <att name=\"long_name\">Fluorescence Voltage</att>\n" + //
        "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
        "            <!-- att name=\"standard_name\"></att> //lots of radiance options; I don't know which -->\n" + //
        "            <att name=\"units\">volts</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>xmiss_v</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">5</att>\n" + //
        "            <att name=\"ioos_category\">Optical Properties</att>\n" + //
        "            <att name=\"long_name\">Transmissivity Voltage</att>\n" + //
        "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
        "            <!-- att name=\"standard_name\"></att> //lots of radiance options; I don't know which -->\n" + //
        "            <att name=\"units\">volts</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>po4</sourceName>\n" + //
        "        <destinationName>PO4</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">4</att>\n" + //
        "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n" + //
        "            <att name=\"long_name\">Phosphate</att>\n" + //
        "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
        "            <att name=\"standard_name\">mole_concentration_of_phosphate_in_sea_water</att>\n" + //
        "            <att name=\"units\">micromoles L-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>n_n</sourceName>\n" + //
        "        <destinationName>N_N</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">50</att>\n" + //
        "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n" + //
        "            <att name=\"long_name\">Nitrate plus Nitrite</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-99.0</att>\n" + //
        "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
        "            <att name=\"standard_name\">moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water</att>\n" + //
        "            <att name=\"units\">micromoles L-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>no3</sourceName>\n" + //
        "        <destinationName>NO3</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">50</att>\n" + //
        "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-99.0</att>\n" + //
        "            <att name=\"long_name\">Nitrate</att>\n" + //
        "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
        "            <att name=\"standard_name\">mole_concentration_of_nitrate_in_sea_water</att>\n" + //
        "            <att name=\"units\">micromoles L-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>si</sourceName>\n" + //
        "        <destinationName>Si</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">50</att>\n" + //
        "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n" + //
        "            <att name=\"long_name\">Silicate</att>\n" + //
        "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
        "            <att name=\"standard_name\">mole_concentration_of_silicate_in_sea_water</att>\n" + //
        "            <att name=\"units\">micromoles L-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>no2</sourceName>\n" + //
        "        <destinationName>NO2</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">1</att>\n" + //
        "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n" + //
        "            <att name=\"long_name\">Nitrite</att>\n" + //
        "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
        "            <att name=\"standard_name\">mole_concentration_of_nitrite_in_sea_water</att>\n" + //
        "            <att name=\"units\">micromoles L-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>nh4</sourceName>\n" + //
        "        <destinationName>NH4</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">5</att>\n" + //
        "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n" + //
        "            <att name=\"long_name\">Ammonium</att>\n" + //
        "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
        "            <att name=\"standard_name\">mole_concentration_of_ammonium_in_sea_water</att>\n" + //
        "            <att name=\"units\">micromoles L-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>oxygen</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">10</att>\n" + //
        "            <att name=\"ioos_category\">Dissolved O2</att>\n" + //
        "            <att name=\"long_name\">Oxygen</att>\n" + //
        "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
        "            <att name=\"standard_name\">volume_fraction_of_oxygen_in_sea_water</att>\n" + //
        "            <att name=\"units\">mL L-1</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>par</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">3</att>\n" + //
        "            <att name=\"ioos_category\">Ocean Color</att>\n" + //
        "            <att name=\"long_name\">Photosynthetically Active Radiation</att>\n" + //
        "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
        "            <!-- att name=\"standard_name\">lots of options, I don't know which</att -->\n" + //
        "            <att name=\"units\">volts</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD geterdGlobecBirds() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"erdGlobecBirds\">\n" + //
        "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/points/globec/").toURI()).toString()
        + "</fileDir>\n" + //
        "    <fileNameRegex>Globec_birds\\.nc</fileNameRegex>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"title\">GLOBEC NEP Northern California Current Bird Data NH0005, 2000-2000, 0007</att>\n" + //
        "        <att name=\"cdm_data_type\">Trajectory</att>\n" + //
        "        <att name=\"cdm_trajectory_variables\">trans_id, trans_no</att>\n" + //
        "        <att name=\"subsetVariables\">trans_id, trans_no, longitude, latitude, time, species, behav_code</att>\n"
        + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"infoUrl\">https://en.wikipedia.org/wiki/Global_Ocean_Ecosystem_Dynamics</att>\n" + //
        "        <att name=\"institution\">GLOBEC</att>\n" + //
        "        <att name=\"keywords\">\n" + //
        "Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Surface Winds,\n" + //
        "Earth Science &gt; Biological Classification &gt; Animals/Vertebrates &gt; Birds,\n" + //
        "Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Coastal Habitat,\n" + //
        "Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Marine Habitat,\n" + //
        "animals, aquatic, area, atmosphere, atmospheric, behavior, biological, biology, biosphere, bird, birds, california, classification, coastal, code, course, current, data, direction, ecosystems, flight, globec, habitat, identifier, marine, nep, nh0005, northern, number, ocean, recorded, ship, species, speed, surface, surveyed, time, transect, unadjusted, vertebrates, wind, wind_speed, winds</att>\n"
        + //
        "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
        "        <att name=\"license\">[standard]</att>\n" + //
        "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" + //
        "        <att name=\"observationDimension\">null</att>\n" + //
        "        <att name=\"references\">\n" + //
        "David G. Ainley, Larry B. Spear, Cynthia T. Tynan, John A. Barth, Stephen D. Pierce, R. Glenn Ford and Timothy J. Cowles, 2005. Physical and biological variables affecting seabird distributions during the upwelling season of the northern California Current. Deep Sea Research Part II: Topical Studies in Oceanography, Volume 52, Issues 1-2, January 2005, Pages 123-143\n"
        + //
        "\n" + //
        "L.B. Spear, N. Nur &amp; D.G. Ainley. 1992. Estimating absolute densities of flying seabirds using analyses of relative movement.  Auk 109:385-389.\n"
        + //
        "\n" + //
        "L.B. Spear &amp; D.G. Ainley. 1997. Flight behaviour of seabirds in relation to wind direction and wing morphology.  Ibis 139: 221-233.\n"
        + //
        "\n" + //
        "L.B. Spear &amp; D.G. Ainley.  1997. Flight speed of seabirds in relation to wind speed and direction. Ibis 139: 234-251.\n"
        + //
        "\n" + //
        "L.B. Spear, D.G. Ainley, B.D. Hardesty, S.N.G. Howell &amp; S.G. Webb. 2004. Reducing biases affecting at-sea surveys of seabirds: use of multiple observer teams. Marine Ornithology 32: 147?157.</att>\n"
        + //
        "        <att name=\"sourceUrl\">(local files; contact erd.data@noaa.gov)</att>\n" + //
        "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
        "        <att name=\"summary\">GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n" + //
        "Northern California Current Bird Data from R/V New Horizon cruises NH0005 and 0007.\n" + //
        "\n" + //
        "As a part of the GLOBEC-Northeast Pacific project, we investigated variation in the abundance of marine birds in the context of biological and physical habitat conditions in the northern portion of the California Current System (CCS) during cruises during the upwelling season 2000. Continuous surveys of seabirds were conducted simultaneously in June (onset of upwelling) and August (mature phase of upwelling).\n"
        + //
        "\n" + //
        "Seabird surveys were conducted continuously during daylight, using a 300-m-wide transect strip. Within that strip, birds were counted that occurred within the 90 degree quadrant off the ship's bow that offered the best observation conditions.\n"
        + //
        "\n" + //
        "Observed counts of seabirds recorded as flying in a steady direction were adjusted for the effect of flight speed and direction relative to that of the ship (Spear et al., 1992; Spear and Ainley, 1997b). The effect of such flux is the most serious bias encountered during seabird surveys at sea (Spear et al., 2005). Known as random directional movement (as opposed to nonrandom directional movement, which occurs when birds are attracted or repelled from the survey vessel), this problem usually results in density overestimation because most species fly faster than survey vessels; densities of birds that fly slower or at a similar speed as the survey vessel (e.g., storm-petrels), or are flying in the same direction, are usually underestimated (Spear et al., 1992)\n"
        + //
        "\n" + //
        "(extracted from: David G. Ainley, Larry B. Spear, Cynthia T. Tynan, John A. Barth, Stephen D. Pierce, R. Glenn Ford and Timothy J. Cowles, 2005. Physical and biological variables affecting seabird distributions during the upwelling season of the northern California Current. Deep Sea Research Part II: Topical Studies in Oceanography, Volume 52, Issues 1-2, January 2005, Pages 123-143)\n"
        + //
        "\n" + //
        "For more information, see\n" + //
        "http://cis.whoi.edu/science/bcodmo/dataset.cfm?id=10053&amp;flag=view\n" + //
        "or\n" + //
        "http://globec.whoi.edu/jg/info/globec/nep/ccs/birds%7Bdir=globec.whoi.edu/jg/dir/globec/nep/ccs/,data=globec.whoi.edu/jg/serv/globec/nep/ccs/birds.html0%7D\n"
        + //
        "\n" + //
        "Contact:\n" + //
        "Cynthia T. Tynan, ctynan@whoi.edu, Woods Hole Oceanographic Institution\n" + //
        "David G. Ainley, dainley@penguinscience.com, H.T. Harvey &amp; Associates\n" + //
        "</att>\n" + //
        "    </addAttributes>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>trans_no</sourceName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">10000</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">24000</att>\n" + //
        "            <att name=\"comment\"></att>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"long_name\">Transect Number</att>\n" + //
        "            <att name=\"cf_role\">trajectory_id</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>trans_id</sourceName>\n" + //
        "        <dataType>int</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">10000000</att>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"long_name\">ID Number for a Location on the Transect</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>lon1000</sourceName>\n" + //
        "        <destinationName>longitude</destinationName>\n" + //
        "        <dataType>int</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"scale_factor\" type=\"float\">0.001</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>lat1000</sourceName>\n" + //
        "        <destinationName>latitude</destinationName>\n" + //
        "        <dataType>int</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"scale_factor\" type=\"float\">0.001</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>date_epoch</sourceName>\n" + //
        "        <destinationName>time</destinationName>\n" + //
        "        <dataType>int</dataType>\n" + //
        "        <addAttributes> \n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">959644800</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">966038400</att>\n" + //
        "            <att name=\"long_name\" />\n" + //
        "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>area</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">2</att>\n" + //
        "            <att name=\"ioos_category\">Other</att>\n" + //
        "            <att name=\"long_name\">Ocean Area Surveyed in the Transect</att>\n" + //
        "            <att name=\"units\">km2</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>behav_code</sourceName>\n" + //
        "        <destinationName>behav_code</destinationName>\n" + //
        "        <dataType>byte</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">3</att>\n" + //
        "            <att name=\"comment\">\n" + //
        "Code    Description\n" + //
        "------  --------------------\n" + //
        "1       Flying directionally\n" + //
        "2       Sitting on water\n" + //
        "3       Feeding\n" + //
        "</att>\n" + //
        "            <att name=\"ioos_category\">Biology</att>\n" + //
        "            <att name=\"long_name\">Behavior Code</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>flight_dir</sourceName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"add_offset\" type=\"short\">0</att>\n" + //
        "            <att name=\"scale_factor\" type=\"short\">10</att>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">360</att>\n" + //
        "            <att name=\"comment\">to nearest 10 degrees</att>\n" + //
        "            <att name=\"ioos_category\">Biology</att>\n" + //
        "            <att name=\"long_name\">Flight Direction</att>\n" + //
        "            <att name=\"missing_value\" type=\"short\">40</att>\n" + //
        "            <att name=\"units\">degrees_true</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>head_c</sourceName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"add_offset\" type=\"short\">0</att>\n" + //
        "            <att name=\"scale_factor\" type=\"short\">10</att>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">360</att>\n" + //
        "            <att name=\"comment\">to nearest 10 degrees</att>\n" + //
        "            <att name=\"ioos_category\">Location</att>\n" + //
        "            <att name=\"long_name\">Ship Course</att>\n" + //
        "            <att name=\"units\">degrees_true</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>number</sourceName>\n" + //
        "        <dataType>byte</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">125</att>\n" + //
        "            <att name=\"ioos_category\">Biology</att>\n" + //
        "            <att name=\"long_name\">Unadjusted Number of Birds Recorded</att>\n" + //
        "            <att name=\"units\">count</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>number_adj</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">1400</att>\n" + //
        "            <att name=\"comment\">Number of birds recorded after adjustment for the effect of bird movement relative to that of the ship (flux).</att>\n"
        + //
        "            <att name=\"ioos_category\">Biology</att>\n" + //
        "            <att name=\"long_name\">Adjusted Number of Birds Recorded</att>\n" + //
        "            <att name=\"units\">count</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>species</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"comment\">\n" + //
        "Species Code    Description\n" + //
        "------------    --------------------\n" + //
        "AKCA            Cassin's Auklet\n" + //
        "AKPA            Parakeet Auklet\n" + //
        "AKRH            Rhinoceros Auklet\n" + //
        "ALBF            Black-footed Albatross\n" + //
        "ALLA            Laysan Albatross\n" + //
        "COBR            Brandt's Cormorant\n" + //
        "COPE            Pelagic Cormorant\n" + //
        "FUNO            Northern Fulmar\n" + //
        "GUCA            California Gull\n" + //
        "GUGW            Glaucous-winged Gull\n" + //
        "GUHR            Heermann's Gull\n" + //
        "GUPI            Pigeon Guillemot\n" + //
        "GURB            Ring-bill Gull\n" + //
        "GUSA            Sabine's Gull\n" + //
        "GUWE            Western Gull\n" + //
        "JALT            Long-tailed Jaeger\n" + //
        "JAPA            Parasitic Jaeger\n" + //
        "JAPO            Pomarine Jaeger\n" + //
        "LOAR            Pacific Loon\n" + //
        "LOCO            Common Loon\n" + //
        "MUCO            Common Murre\n" + //
        "MUMA            Marbled Murrelet\n" + //
        "MUXA            Xantus' Murrelet\n" + //
        "PELB            Brown Pelican\n" + //
        "PHNO            Red-necked Phalarope\n" + //
        "PHRE            Red Phalarope\n" + //
        "SHFF            Flesh-footed Shearwater\n" + //
        "SHPF            Pink-footed Shearwater\n" + //
        "SHSO            Sooty Shearwater\n" + //
        "SKMA            South Polar Skua\n" + //
        "STFT            Fork-tailed Storm-Petrel\n" + //
        "STLE            Leach's Storm-Petrel\n" + //
        "TEAR            Arctic Tern\n" + //
        "</att>\n" + //
        "            <att name=\"ioos_category\">Biology</att>\n" + //
        "            <att name=\"long_name\">Species</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>wspd</sourceName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">15</att>\n" + //
        "            <att name=\"comment\">Caution. Wind speed and direction may not be corrected for ship motion.</att>\n"
        + //
        "            <att name=\"_FillValue\" type=\"float\">-9999</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "            <att name=\"long_name\">Wind Speed</att>\n" + //
        "            <att name=\"units\">knots</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD geterdGlobecMoc1() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"erdGlobecMoc1\">\n" + //
        "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/points/globec/").toURI()).toString()
        + "</fileDir>\n" + //
        "    <fileNameRegex>Globec_moc1\\.nc</fileNameRegex>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"title\">GLOBEC NEP MOCNESS Plankton (MOC1) Data, 2000-2002</att>\n" + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"infoUrl\">https://en.wikipedia.org/wiki/Global_Ocean_Ecosystem_Dynamics</att>\n" + //
        "        <att name=\"institution\">GLOBEC</att>\n" + //
        "        <att name=\"keywords\">\n" + //
        "Earth Science &gt; Biological Classification &gt; Animals/Vertebrates &gt; Fish,\n" + //
        "Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Coastal Habitat,\n" + //
        "Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Marine Habitat,\n" + //
        "Earth Science &gt; Oceans &gt; Aquatic Sciences &gt; Fisheries,\n" + //
        "Earth Science &gt; Oceans &gt; Bathymetry/Seafloor Topography &gt; Bathymetry,\n" + //
        "abundance, animals, aquatic, area, bathymetry, below, biological, biology, biosphere, cast, classification, coastal, code, comments, counter, cruise, data, day, day/night, depth, ecosystems, filtered, fish, fisheries, flag, floor, gear, genus, globec, habitat, identifier, level, life, local, marine, maximum, mesh, minimum, moc1, mocness, mouth, nep, net, night, noaa, nodc, number, oceans, plankton, program, sample, sciences, sea, sea_floor_depth_below_sea_level, seafloor, seawater, size, species, stage, station, taxonomy, time, topography, type, vertebrates, volume, water</att>\n"
        + //
        "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
        "        <att name=\"license\">[standard]</att>\n" + //
        "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" + //
        "        <att name=\"references\">\n" + //
        "Tucker, G.H., 1951. Relation of fishes and other organisms to the scattering of underwater sound. Journal of Marine Research, 10: 215-238.\n"
        + //
        "\n" + //
        "Wiebe, P.H., K.H. Burt, S. H. Boyd, A.W. Morton, 1976. The multiple opening/clo sing net and environmental sensing system for sampling zooplankton. Journal of Marine Research, 34(3): 313-326\n"
        + //
        "\n" + //
        "Wiebe, P.H., A.W. Morton, A.M. Bradley, R.H. Backus, J.E. Craddock, V. Barber, T.J. Cowles and G.R. Flierl, 1985. New developments in the MOCNESS, an apparatu s for sampling zooplankton and micronekton. Marine Biology, 87: 313- 323.\n"
        + //
        "</att>\n" + //
        "        <att name=\"cdm_data_type\">Trajectory</att>\n" + //
        "        <att name=\"cdm_trajectory_variables\">cruise_id</att>\n" + //
        "        <att name=\"subsetVariables\">cruise_id, longitude, latitude, time, cast_no, station_id, abund_m3, comments, counter_id, d_n_flag, gear_area_m2, gear_mesh, gear_type, genus_species, life_stage, local_code, max_sample_depth, min_sample_depth, nodc_code, program, sample_id, vol_filt, water_depth</att>\n"
        + //
        "        <att name=\"observationDimension\">null</att>\n" + //
        "        <att name=\"sourceUrl\">(local files; contact erd.data@noaa.gov)</att>\n" + //
        "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
        "        <att name=\"summary\">\n" + //
        "        GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific) California Current Program\n" + //
        "MOCNESS Plankton (MOC1) Data\n" + //
        "The MOCNESS is based on the Tucker Trawl principle (Tucker, 1951). The MOCNESS-1 has nine rectangular nets (1m x 1.4 m) which are opened and closed sequentially by commands through conducting cable from the surface (Wiebe et al., 1976). In MOCNESS systems, \"the underwater unit sends a data frame, comprised of temperature, depth, conductivity, net-frame angle, flow count, time, number of open net, and net opening/closing, to the deck unit in a compressed hexadecimal format every 2 seconds and from the deck unit to a microcomputer every 4 seconds... Temperature (to approximately 0.01 deg C) and conductivity are measured with SEABIRD sensors. Normally, a modified T.S.K.-flowmeter is used... Both the temperature and conductivity sensors and the flowmeter are mounted on top of the frame so that they face horizontally when the frame is at a towing angle of 45deg... Calculations of salinity (to approximately 0.01 o/oo S), potential temperature (theta), potential density (sigma), the oblique and vertical velocities of the net, and the approximate volume filtered by each net are made after each string of data has been received by the computer.\" (Wiebe et al., 1985) In addition, depending on the particular configuration of the MOCNESS-1, data may have been collected from other sensors attached to the frame : (Transmissometer, Fluorometer, Downwelling light sensor, and the Oxygen sensor). A SeaBird underwater pump was also included in the sensor suit e.\n"
        + //
        "After retrieval to deck, the contents of the nets were rinsed into the codends a nd transferred to storage bottles, and fixed and preserved with formalin. In the shore laboratory, the contents of the bottles were subsampled and counts and biomass estimates made for selected taxa (see the Proc_Protocol info below). This data object reports only the count information.\n"
        + //
        "\n" + //
        "For more information, see\n" + //
        "http://cis.whoi.edu/science/bcodmo/dataset.cfm?id=10182&amp;flag=view\n" + //
        "or\n" + //
        "http://globec.whoi.edu/jg/info/globec/nep/ccs/MOC1%7Bdir=globec.whoi.edu/jg/dir/globec/nep/ccs/,data=globec.coas.oregonstate.edu/jg/serv/MOC1.html0%7D\n"
        + //
        "\n" + //
        "All inquiries about this data should be directed to Dr. William Peterson (bill.peterson@noaa.gov).\n" + //
        "Inquiries about how to access this data should be directed to Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\n"
        + //
        "</att>\n" + //
        "    </addAttributes>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>cruise_id</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <!-- att name=\"colorBarMaximum\" type=\"double\">?</att>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">?</att -->\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"long_name\">Cruise ID</att>\n" + //
        "            <att name=\"cf_role\">trajectory_id</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>lon10000</sourceName>\n" + //
        "        <destinationName>longitude</destinationName>\n" + //
        "        <dataType>int</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"scale_factor\" type=\"float\">0.0001</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>lat10000</sourceName>\n" + //
        "        <destinationName>latitude</destinationName>\n" + //
        "        <dataType>int</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"scale_factor\" type=\"float\">0.0001</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>datetime_utc_epoch</sourceName>\n" + //
        "        <destinationName>time</destinationName>\n" + //
        "        <dataType>int</dataType>\n" + //
        "        <addAttributes> \n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">955497600</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">1022803200</att>\n" + //
        "            <att name=\"long_name\" />\n" + //
        "            <att name=\"units\">sec since 1970-01-01T00:00:00Z</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>cast_no</sourceName>\n" + //
        "        <dataType>byte</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">30</att>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"long_name\">Cast Number</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>stn_id</sourceName>\n" + //
        "        <destinationName>station_id</destinationName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"long_name\">Station ID</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>abund_m3</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">1000</att>\n" + //
        "            <att name=\"comment\">Density [individuals per m3]</att>\n" + //
        "            <att name=\"ioos_category\">Other</att>\n" + //
        "            <att name=\"long_name\">Abundance</att>\n" + //
        "            <att name=\"units\">count m-3</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>comments</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"comment\">Misc. comments pertaining to sample</att>\n" + //
        "            <att name=\"ioos_category\">Other</att>\n" + //
        "            <att name=\"long_name\">Comments</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>counter_id</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"comment\">Initials of Plankton Taxonomist</att>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"long_name\">Counter ID</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>d_n_flag</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Other</att>\n" + //
        "            <att name=\"long_name\">Day/Night Flag</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>gear_area_m2</sourceName>\n" + //
        "        <dataType>byte</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">10</att>\n" + //
        "            <att name=\"ioos_category\">Other</att>\n" + //
        "            <att name=\"long_name\">Mouth Area of Net</att>\n" + //
        "            <att name=\"units\">m2</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>gear_mesh</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">1</att>\n" + //
        "            <att name=\"ioos_category\">Other</att>\n" + //
        "            <att name=\"long_name\">Net's Mesh Size</att>\n" + //
        "            <att name=\"units\">mm</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>gear_type</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Other</att>\n" + //
        "            <att name=\"long_name\">Gear Type</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>genus_species</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Taxonomy</att>\n" + //
        "            <att name=\"long_name\">Genus Species</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>life_stage</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"comment\">\n" + //
        "most are self explanatory; Male, Female, CV ==> Copepodite 5, Zoea, Nauplii, N2 => Nauplius 2, Egg--a few are not, esp. for the euphausiids (Thysanoessa and Euphausia)\n"
        + //
        "\n" + //
        "Life Stage Info Codes      Description\n" + //
        "(partial listing)\n" + //
        "------------------------   -------------------------------------------------\n" + //
        "F2                         Second Stage Furcilia (aka Furcilia 2)\n" + //
        "F3                         Third Stage Furcilia (aka Furcilia 3)\n" + //
        "F1_0                       First Stage Furcilia with 0 legs\n" + //
        "F2_32 or Furcilia_2_3L2S   Second Stage Furcilia with 3 pairs of legs total,\n" + //
        "                           with 2 pairs of legs having setae;\n" + //
        "                           this xLyS pattern is common, with x and y varying\n" + //
        "                           depending on stage of development</att>\n" + //
        "            <att name=\"ioos_category\">Biology</att>\n" + //
        "            <att name=\"long_name\">Life Stage</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>local_code</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"comment\">NODC taxonomic code modified for local use</att>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"long_name\">Local Code</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>max_sample_depth</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">500</att>\n" + //
        "            <att name=\"comment\">Maximum depth of vertical tow [meters]; estimated from wire out and wire angle</att>\n"
        + //
        "            <att name=\"ioos_category\">Location</att>\n" + //
        "            <att name=\"long_name\">Maximum Sample Depth</att>\n" + //
        "            <att name=\"units\">m</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>min_sample_depth</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">300</att>\n" + //
        "            <att name=\"comment\">Always \"0\" for vertical tows</att>\n" + //
        "            <att name=\"ioos_category\">Location</att>\n" + //
        "            <att name=\"long_name\">Minimum Sample Depth</att>\n" + //
        "            <att name=\"units\">m</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>nodc_code</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Other</att>\n" + //
        "            <att name=\"long_name\">NODC Code</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <!-- just one value: -9999   dataVariable>\n" + //
        "        <sourceName>perc_counted</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"comment\">Percentage of sample evaluated for this taxon [0-100%]</att>\n" + //
        "            <att name=\"ioos_category\">Other</att>\n" + //
        "            <att name=\"long_name\">Percent Counted</att>\n" + //
        "            <att name=\"units\">percent</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable -->\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>program</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"comment\">\n" + //
        "Program Codes  Description\n" + //
        "-------------  ----------------------------------------------------\n" + //
        "LTOP           samples collected on Long-term Observation\n" + //
        "               Program Cruises (ca. 4-6 cruises per year;\n" + //
        "               all sample the Newport Hydrographic (NH) Line;\n" + //
        "               some sample other standard lines further south)\n" + //
        "NH             more frequent, small vessel, nearshore sampling of\n" + //
        "               Newport Hydrographic Line\n" + //
        "MESO_1         samples collected from process cruise in June 2000\n" + //
        "MESO_2         samples collected from process cruise in August 2000\n" + //
        "</att>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"long_name\">Program</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>sample_id</sourceName>\n" + //
        "        <dataType>byte</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">10</att>\n" + //
        "            <att name=\"comment\">Always \"1\" for VPT casts, which have a single net only</att>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"long_name\">Sample ID</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>vol_filt</sourceName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">1400</att>\n" + //
        "            <att name=\"ioos_category\">Other</att>\n" + //
        "            <att name=\"long_name\">Volume Filtered</att>\n" + //
        "            <att name=\"units\">m3</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>water_depth</sourceName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">2000</att>\n" + //
        "            <att name=\"ioos_category\">Location</att>\n" + //
        "            <att name=\"long_name\">Water Depth</att>\n" + //
        "            <att name=\"standard_name\">sea_floor_depth_below_sea_level</att>\n" + //
        "            <att name=\"units\">m</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD getearthCubeKgsBoreTempWV() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"earthCubeKgsBoreTempWV\" active=\"true\">\n" + //
            "    <reloadEveryNMinutes>1000000</reloadEveryNMinutes>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <fileDir>"
            + Path.of(EDDTestDataset.class.getResource("/data/points/kgsBoreTempWV/").toURI()).toString()
            + "</fileDir>\n" + //
            "    <recursive>false</recursive>\n" + //
            "    <fileNameRegex>.*\\.tsv</fileNameRegex>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <columnNamesRow>1</columnNamesRow>\n" + //
            "    <firstDataRow>3</firstDataRow>\n" + //
            "    <preExtractRegex></preExtractRegex>\n" + //
            "    <postExtractRegex></postExtractRegex>\n" + //
            "    <extractRegex></extractRegex>\n" + //
            "    <columnNameForExtract></columnNameForExtract>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" + //
            "        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" + //
            "        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" + //
            "    -->\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"cdm_data_type\">Point</att>\n" + //
            "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
            "        <att name=\"creator_name\">John Saucer</att>\n" + //
            "        <att name=\"creator_email\">info@geosrv.wvnet.edu</att>\n" + //
            "        <att name=\"creator_type\">person</att>\n" + //
            "        <att name=\"creator_url\">https://www.wvgs.wvnet.edu/</att>\n" + //
            "        <att name=\"infoUrl\">https://www.wvgs.wvnet.edu/</att>\n" + //
            "        <att name=\"institution\">West Virginia Geological and Economic Survey</att>\n" + //
            "        <att name=\"keywords\">apino, bore, borehole, circulation, commodity, county, date, degree, depth, driller, drilling, elevation, ended, field, formation, function, geological, geothermal, header, interest, interval, kentucky, label, lease, length, long, measured, measurement, name, notes, observation, operator, point, procedure, producing, production, quality, reference, related, resource, shape, since, source, spud, srs, state, statement, statistics, status, survey, temperature, temperatures, time, total, true, type, uncertainty, units, URI, vertical, well, West Virginia</att>\n"
            + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"rowElementXPath\">/wfs:FeatureCollection/gml:featureMember</att>\n" + //
            "        <att name=\"sourceUrl\">https://kgs.uky.edu/usgin/services/aasggeothermal/WVBoreholeTemperatures/MapServer/WFSServer?request=GetFeature&amp;service=WFS&amp;typename=aasg:BoreholeTemperature&amp;format=&quot;text/xml;%20subType=gml/3.1.1/profiles/gmlsf/1.0.0/0&quot;</att>\n"
            + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"subsetVariables\">WellName,APINo,Label,Operator,WellType,Field,County,State,FormationTD,OtherName</att>\n"
            + //
            "        <att name=\"summary\">Borehole temperature measurements in West Virginia</att>\n" + //
            "        <att name=\"title\">West Virginia Borehole Temperatures, AASG State Geothermal Data, 1936-2010</att>\n"
            + //
            "    </addAttributes>\n" + //
            "    <!-- I think OBJECTID is an artifact of the WFS file.   \n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:OBJECTID</sourceName>\n" + //
            "        <destinationName>OBJECTID</destinationName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">OBJECTID</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable-->\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:ObservationURI</sourceName>\n" + //
            "        <destinationName>ObservationURI</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Observation URI</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:WellName</sourceName>\n" + //
            "        <destinationName>WellName</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Well Name</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:APINo</sourceName>\n" + //
            "        <destinationName>APINo</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">APINo</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:HeaderURI</sourceName>\n" + //
            "        <destinationName>HeaderURI</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Header URI</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:Label</sourceName>\n" + //
            "        <destinationName>Label</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Label</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:Operator</sourceName>\n" + //
            "        <destinationName>Operator</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Operator</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:SpudDate</sourceName>\n" + //
            "        <destinationName>SpudDate</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"long_name\">Spud Date</att>\n" + //
            "            <att name=\"time_precision\">1970-01-01</att>\n" + //
            "            <att name=\"units\">yyyy-MM-dd&#039;T&#039;HH:mm:ss</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:EndedDrillingDate</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"long_name\">Ended Drilling Date</att>\n" + //
            "            <att name=\"time_precision\">1970-01-01</att>\n" + //
            "            <att name=\"units\">yyyy-MM-dd&#039;T&#039;HH:mm:ss</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:WellType</sourceName>\n" + //
            "        <destinationName>WellType</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Well Type</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <!--  5 vars removed: all \"Missing\"\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:Status</sourceName>\n" + //
            "        <destinationName>Status</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Other</att>\n" + //
            "            <att name=\"long_name\">Status</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:CommodityOfInterest</sourceName>\n" + //
            "        <destinationName>CommodityOfInterest</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Commodity Of Interest</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:Function</sourceName>\n" + //
            "        <destinationName>Function</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Function</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:Production</sourceName>\n" + //
            "        <destinationName>Production</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Production</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:ProducingInterval</sourceName>\n" + //
            "        <destinationName>ProducingInterval</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Other</att>\n" + //
            "            <att name=\"long_name\">Producing Interval</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable> -->\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:Field</sourceName>\n" + //
            "        <destinationName>Field</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Field</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:County</sourceName>\n" + //
            "        <destinationName>County</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Statistics</att>\n" + //
            "            <att name=\"long_name\">County</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:State</sourceName>\n" + //
            "        <destinationName>State</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">State</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:LatDegree</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" + //
            "            <att name=\"LocationUncertaintyStatement\">Location recorded as received from official permit application converted to NAD83 if required</att>\n"
            + //
            "            <att name=\"SRS\">NAD 83</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Latitude</att>\n" + //
            "            <att name=\"standard_name\">latitude</att>\n" + //
            "            <att name=\"units\">degrees_north</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:LongDegree</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" + //
            "            <att name=\"LocationUncertaintyStatement\">Location recorded as received from official permit application converted to NAD83 if required</att>\n"
            + //
            "            <att name=\"SRS\">NAD 83</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Longitude</att>\n" + //
            "            <att name=\"standard_name\">longitude</att>\n" + //
            "            <att name=\"units\">degrees_east</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <!-- all \"NAD 83\" so moved to latitude and longitude srs\n" + //
            "    >dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:SRS</sourceName>\n" + //
            "        <destinationName>SRS</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">SRS</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>   all same, so moved to be comment for latitude and longitude\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:LocationUncertaintyStatement</sourceName>\n" + //
            "        <destinationName>LocationUncertaintyStatement</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Quality</att>\n" + //
            "            <att name=\"long_name\">Location Uncertainty Statement</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    -->\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:DrillerTotalDepth</sourceName>\n" + //
            "        <destinationName>DrillerTotalDepth</destinationName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">32767</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n"
            + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Driller Total Depth</att>\n" + //
            "            <att name=\"units\">ft</att> <!-- from LengthUnits -->\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:DepthReferencePoint</sourceName>\n" + //
            "        <destinationName>DepthReferencePoint</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Depth Reference Point</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <!-- all \"ft\", so I used that as units elsewhere\n" + //
            "    >dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:LengthUnits</sourceName>\n" + //
            "        <destinationName>LengthUnits</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Other</att>\n" + //
            "            <att name=\"long_name\">Length Units</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    -->\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:WellBoreShape</sourceName>\n" + //
            "        <destinationName>WellBoreShape</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Well Bore Shape</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:TrueVerticalDepth</sourceName>\n" + //
            "        <destinationName>TrueVerticalDepth</destinationName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">32767</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n"
            + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">True Vertical Depth</att>\n" + //
            "            <att name=\"units\">ft</att> <!-- from LengthUnits -->\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:ElevationGL</sourceName>\n" + //
            "        <destinationName>ElevationGL</destinationName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">32767</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n"
            + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Elevation GL</att>\n" + //
            "            <att name=\"units\">ft</att> <!-- from LengthUnits -->\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:FormationTD</sourceName>\n" + //
            "        <destinationName>FormationTD</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Formation TD</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:MeasuredTemperature</sourceName>\n" + //
            "        <destinationName>MeasuredTemperature</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">200</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">50</att>            \n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">Measured Temperature</att>\n" + //
            "            <att name=\"MeasurementProcedure\">Temperature log evaluated by WVGES staff for deepest stable log segment to extract data otherwise used given bottom hole temperature on log header if available</att>\n"
            + //
            "            <att name=\"MeasurementSource\">Well Temperature Log</att>\n" + //
            "            <att name=\"units\">degree_F</att> <!-- from TemperatureUnits -->\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <!-- all \"F\", so used as units above\n" + //
            "    dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:TemperatureUnits</sourceName>\n" + //
            "        <destinationName>TemperatureUnits</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">Temperature Units</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>    all same so used as attribute above\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:MeasurementProcedure</sourceName>\n" + //
            "        <destinationName>MeasurementProcedure</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Other</att>\n" + //
            "            <att name=\"long_name\">Measurement Procedure</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable -->\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:DepthOfMeasurement</sourceName>\n" + //
            "        <destinationName>DepthOfMeasurement</destinationName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">32767</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n"
            + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Depth Of Measurement</att>\n" + //
            "            <att name=\"units\">ft</att> <!-- from LengthUnits -->\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:MeasurementFormation</sourceName>\n" + //
            "        <destinationName>MeasurementFormation</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Other</att>\n" + //
            "            <att name=\"long_name\">Measurement Formation</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <!-- all \"Well Temperature Log\", so used as metadata above\n" + //
            "    dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:MeasurementSource</sourceName>\n" + //
            "        <destinationName>MeasurementSource</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Other</att>\n" + //
            "            <att name=\"long_name\">Measurement Source</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable -->\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:RelatedResource</sourceName>\n" + //
            "        <destinationName>RelatedResource</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Other</att>\n" + //
            "            <att name=\"long_name\">Related Resource</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <!--dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:Shape/gml:Point/latitude</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Latitude</att>\n" + //
            "            <att name=\"standard_name\">latitude</att>\n" + //
            "            <att name=\"units\">degrees_north</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:Shape/gml:Point/longitude</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Longitude</att>\n" + //
            "            <att name=\"standard_name\">longitude</att>\n" + //
            "            <att name=\"units\">degrees_east</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable-->\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:TimeSinceCirculation</sourceName>\n" + //
            "        <destinationName>TimeSinceCirculation</destinationName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">32767</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n"
            + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"long_name\">Time Since Circulation</att>\n" + //
            "            <att name=\"units\">?</att> \n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:OtherName</sourceName>\n" + //
            "        <destinationName>OtherName</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Other</att>\n" + //
            "            <att name=\"long_name\">Other Name</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:LeaseName</sourceName>\n" + //
            "        <destinationName>LeaseName</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Lease Name</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>aasg:BoreholeTemperature/aasg:Notes</sourceName>\n" + //
            "        <destinationName>Notes</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Other</att>\n" + //
            "            <att name=\"long_name\">Notes</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD gettestTableWithDepth() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"testTableWithDepth\" active=\"true\">\n" + //
            "    <reloadEveryNMinutes>60</reloadEveryNMinutes>\n" + //
            "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/points/tao/daily/").toURI()).toString()
            + "</fileDir>\n" +
            "    <recursive>false</recursive>\n" + //
            "    <fileNameRegex>airt[0-9].*_dy\\.cdf</fileNameRegex>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <sortedColumnSourceName>time</sortedColumnSourceName>\n" + //
            "    <sortFilesBySourceNames>global:site_code time</sortFilesBySourceNames>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "        <att name=\"_FillValue\" type=\"float\">1.0E35</att>\n" + //
            "        <att name=\"CREATION_DATE\">07:37 12-JUL-2011</att>\n" + //
            "        <att name=\"Data_info\">Contact Paul Freitag: 206-526-6727</att>\n" + //
            "        <att name=\"Data_Source\">GTMBA Project Office/NOAA/PMEL</att>\n" + //
            "        <att name=\"File_info\">Contact: Dai.C.McClurg@noaa.gov</att>\n" + //
            "        <att name=\"missing_value\" type=\"float\">1.0E35</att>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" + //
            "        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" + //
            "        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" + //
            "    -->\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"_FillValue\"></att>\n" + //
            "        <att name=\"platform_code\"></att>\n" + //
            "        <att name=\"site_code\"></att>\n" + //
            "        <att name=\"cdm_data_type\">TimeSeries</att>\n" + //
            "        <att name=\"cdm_timeseries_variables\">array, station, wmo_platform_code, longitude, latitude</att>\n"
            + //
            "        <att name=\"subsetVariables\">array, station, wmo_platform_code, longitude, latitude</att>\n" + //
            "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
            "        <att name=\"creator_name\">GTMBA Project Office/NOAA/PMEL</att>\n" + //
            "        <att name=\"creator_email\">Dai.C.McClurg@noaa.gov</att>\n" + //
            "        <att name=\"creator_url\">https://www.pmel.noaa.gov/gtmba/mission</att>\n" + //
            "        <att name=\"id\"></att>\n" + //
            "        <att name=\"infoUrl\">https://www.pmel.noaa.gov/gtmba/mission</att>\n" + //
            "        <att name=\"institution\">NOAA PMEL, TAO/TRITON, RAMA, PIRATA</att>\n" + //
            "        <att name=\"keywords\">air, air_temperature</att>\n" + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"missing_value\"></att>\n" + //
            "        <att name=\"observationDimension\">null</att>\n" + //
            "        <att name=\"project\">TAO/TRITON, RAMA, PIRATA</att>\n" + //
            "        <att name=\"sourceUrl\">(local files)</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"summary\">This is the summary</att>\n" + //
            "        <att name=\"title\">This is EDDTableWithDepth</att>\n" + //
            "    </addAttributes>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:array</sourceName>\n" + //
            "        <destinationName>array</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Array</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:site_code</sourceName>\n" + //
            "        <destinationName>station</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"cf_role\">timeseries_id</att>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Station</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:wmo_platform_code</sourceName>\n" + //
            "        <destinationName>wmo_platform_code</destinationName>\n" + //
            "        <dataType>int</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">WMO Platform Code</att>\n" + //
            "            <att name=\"missing_value\" type=\"int\">2147483647</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>lon</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"epic_code\" type=\"int\">502</att>\n" + //
            "            <att name=\"type\">EVEN</att>\n" + //
            "            <att name=\"units\">degree_east</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Nominal Longitude</att>\n" + //
            "            <att name=\"standard_name\">longitude</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>lat</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"epic_code\" type=\"int\">500</att>\n" + //
            "            <att name=\"type\">EVEN</att>\n" + //
            "            <att name=\"units\">degree_north</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Nominal Latitude</att>\n" + //
            "            <att name=\"standard_name\">latitude</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>time</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"type\">EVEN</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"long_name\">Centered Time</att>\n" + //
            "            <att name=\"standard_name\">time</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>depth</sourceName>\n" + //
            "        <destinationName>depth</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"epic_code\" type=\"int\">3</att>\n" + //
            "            <att name=\"type\">EVEN</att>\n" + //
            "            <att name=\"units\">m</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>AT_21</sourceName>\n" + //
            "        <destinationName>AT_21</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"epic_code\" type=\"int\">21</att>\n" + //
            "            <att name=\"generic_name\">atemp</att>\n" + //
            "            <att name=\"long_name\">AIR TEMPERATURE (C)</att>\n" + //
            "            <att name=\"name\">AT</att>\n" + //
            "            <att name=\"units\">C</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-10</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">40</att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">Air Temperature</att>\n" + //
            "            <att name=\"standard_name\">air_temperature</att>\n" + //
            "            <att name=\"units\">degree_C</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>QAT_5021</sourceName>\n" + //
            "        <destinationName>QAT_5021</destinationName>\n" + //
            "        <dataType>byte</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"epic_code\" type=\"int\">5021</att>\n" + //
            "            <att name=\"generic_name\">qat</att>\n" + //
            "            <att name=\"long_name\">AIRT QUALITY</att>\n" + //
            "            <att name=\"name\">QAT</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">6</att>\n" + //
            "            <att name=\"colorBarContinuous\">false</att>\n" + //
            "            <att name=\"description\">Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QAT_5021&gt;=1 and QAT_5021&lt;=3.</att>\n"
            + //
            "            <att name=\"ioos_category\">Quality</att>\n" + //
            "            <att name=\"long_name\">Air Temperature Quality</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>SAT_6021</sourceName>\n" + //
            "        <destinationName>SAT_6021</destinationName>\n" + //
            "        <dataType>byte</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"epic_code\" type=\"int\">6021</att>\n" + //
            "            <att name=\"generic_name\">sat</att>\n" + //
            "            <att name=\"long_name\">AIRT SOURCE</att>\n" + //
            "            <att name=\"name\">SAT</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">8</att>\n" + //
            "            <att name=\"colorBarContinuous\">false</att>\n" + //
            "            <att name=\"description\">Source Codes:\n" + //
            "0 = No Sensor, No Data\n" + //
            "1 = Real Time (Telemetered Mode)\n" + //
            "2 = Derived from Real Time\n" + //
            "3 = Temporally Interpolated from Real Time\n" + //
            "4 = Source Code Inactive at Present\n" + //
            "5 = Recovered from Instrument RAM (Delayed Mode)\n" + //
            "6 = Derived from RAM\n" + //
            "7 = Temporally Interpolated from RAM</att>\n" + //
            "            <att name=\"ioos_category\">Other</att>\n" + //
            "            <att name=\"long_name\">Air Temperature Source</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD geterdGtsppBest() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDTableFromNcCFFiles\" datasetID=\"erdGtsppBest\" active=\"true\">\n" + //
            "    <reloadEveryNMinutes>1000000</reloadEveryNMinutes>\n" + //
            "    <fileDir>"
            + Path.of(EDDTestDataset.class.getResource("/largeFiles/points/gtsppNcCF/").toURI()).toString()
            + "</fileDir>\n" +
            "    <recursive>false</recursive>\n" + //
            "    <fileNameRegex>.*\\.nc</fileNameRegex>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <sortedColumnSourceName>time</sortedColumnSourceName>\n" + //
            "    <sortFilesBySourceNames>time</sortFilesBySourceNames>   \n" + //
            "    <addAttributes>\n" + //
            "        <!-- change 2 dates here every month (2nd date is END of processing) -->\n" + //
            "        <att name=\"history\">2022-02-01 csun writeGTSPPnc40.f90 Version 1.8\n" + //
            ".tgz files from ftp.nodc.noaa.gov /pub/data.nodc/gtspp/bestcopy/netcdf (https://www.nodc.noaa.gov/GTSPP/)\n"
            + //
            "2022-02-10 Most recent ingest, clean, and reformat at ERD (erd.data at noaa.gov).</att>\n" + //
            "        <att name=\"id\">erdGtsppBest</att>\n" + //
            "        <att name=\"subsetVariables\">trajectory, org, type, platform, cruise</att>  \n" + //
            "    </addAttributes>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>trajectory</sourceName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>org</sourceName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>type</sourceName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>platform</sourceName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>cruise</sourceName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>station_id</sourceName>\n" + //
            "        <dataType>int</dataType>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>longitude</sourceName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>latitude</sourceName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>time</sourceName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>depth</sourceName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>temperature</sourceName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>salinity</sourceName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD geterdFedRockfishStation() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"erdFedRockfishStation\" active=\"true\">\n" + //
            "    <reloadEveryNMinutes>15000</reloadEveryNMinutes>\n" + //
            "    <fileDir>"
            + Path.of(EDDTestDataset.class.getResource("/data/points/rockfish20130409/").toURI()).toString()
            + "</fileDir>\n" + //
            "    <recursive>false</recursive>\n" + //
            "    <fileNameRegex>rockfish_header_.{4}\\.nc</fileNameRegex>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <sortedColumnSourceName>time</sortedColumnSourceName>\n" + //
            "    <sortFilesBySourceNames>time</sortFilesBySourceNames>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "        <att name=\"id\">ERD_CTD_HEADER_2008_to_2011</att>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"cdm_data_type\">Trajectory</att>\n" + //
            "        <att name=\"cdm_trajectory_variables\">cruise</att>\n" + //
            "        <att name=\"subsetVariables\">cruise,ctd_index,ctd_no,station,time,longitude,latitude</att>\n" + //
            "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
            "        <att name=\"creator_name\">Keith Sakuma</att>\n" + //
            "        <att name=\"creator_email\">Keith.Sakuma@noaa.gov</att>\n" + //
            "        <att name=\"creator_type\">person</att>\n" + //
            "        <att name=\"history\">2013-04-08 source files from Keith Sakuma (FED) to Lynn Dewitt (ERD) to Bob Simons (ERD)\n"
            + //
            "2013-04-09 Bob Simons (erd.data@noaa.gov) converted data files for 1987-2011 from .csv to .nc with Projects.convertRockfish().\n"
            + //
            "2017-02-03 Bob Simons (erd.data@noaa.gov) converted data files for 2012-2015 from xlsx to .nc with Projects.convertRockfish().</att>\n"
            + //
            "        <att name=\"id\">null</att>\n" + //
            "        <att name=\"infoUrl\">https://www.fisheries.noaa.gov/west-coast/science-data/molecular-ecology-and-genetic-analysis-california-salmon-and-groundfish</att>\n"
            + //
            "        <att name=\"institution\">NOAA SWFSC FED</att>\n" + //
            "        <att name=\"keywords\">bottom, bucket, California, coast, cruise, ctd, data, depth, FED, fisheries, fixed, fluorometer, header, hydrographic, index, juvenile, latitude, longitude, midwater, NMFS, NOAA, pacific, rockfish, salinity, species, station, survey, SWFSC, temperature, thermosalinometer, time, transimissometer, trawl</att>\n"
            + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"summary\">\n" + //
            "SWFSC FED Mid Water Trawl Juvenile Rockfish Survey: Station Information and Surface Data.\n" + //
            "Surveys have been conducted along the central California coast in May/June \n" + //
            "every year since 1983. In 2004 the survey area was expanded to cover the \n" + //
            "entire coast from San Diego to Cape Mendocino.  The survey samples a series \n" + //
            "of fixed trawl stations using a midwater trawl. The midwater trawl survey \n" + //
            "gear captures significant numbers of approximately 10 rockfish species during\n" + //
            "their pelagic juvenile stage (i.e., 50-150 days old), by which time annual\n" + //
            "reproductive success has been established. Catch-per-unit-effort data from\n" + //
            "the survey are analyzed and serve as the basis for predicting future \n" + //
            "recruitment to rockfish fisheries. Results for several species (e.g., \n" + //
            "bocaccio, chilipepper [S. goodei], and widow rockfish [S. entomelas]) have\n" + //
            "shown that the survey data can be useful in predicting year-class strength\n" + //
            "in age-based stock assessments.\n" + //
            "\n" + //
            "The survey's data on YOY Pacific whiting has also been used in the stock\n" + //
            "assessment process. To assist in obtaining additional northward spatial\n" + //
            "coverage of YOY Pacific whiting off Oregon and Washington, in 2001 the\n" + //
            "Pacific Whiting Conservation Cooperative in cooperation with the NOAA NMFS\n" + //
            "Northwest Fisheries Science Center began a midwater trawl survey patterned\n" + //
            "after the NOAA NMFS SWFSC Fisheries Ecology Division's (FED) existing survey. \n" + //
            "Both surveys work cooperatively together each year in order to resolve \n" + //
            "interannual abundance patterns of YOY rockfish and Pacific whiting on a \n" + //
            "coastwide basis, which provides expedient, critical information that can be \n" + //
            "used in the fisheries management process.\n" + //
            "\n" + //
            "The large quantity of physical data collected during the surveys (e.g., CTD\n" + //
            "with attached transimissometer and fluorometer, thermosalinometer, and ADCP)\n" + //
            "have provided a better understanding of the hydrographic conditions off the\n" + //
            "California coast and analysis of these data have been distributed through the\n" + //
            "publication of NOAA NMFS Technical Memoranda.\n" + //
            "\n" + //
            "For more information, see \n" + //
            "https://www.fisheries.noaa.gov/west-coast/science-data/molecular-ecology-and-genetic-analysis-california-salmon-and-groundfish \n"
            + //
            "and http://www.sanctuarysimon.org/projects/project_info.php?projectID=100118</att>\n" + //
            "        <att name=\"sourceUrl\">(local files; contact erd.data@noaa.gov)</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"title\">SWFSC FED Mid Water Trawl Juvenile Rockfish Survey, Surface Data, 1987-2015</att>\n"
            + //
            "    </addAttributes>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>cruise</sourceName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"cf_role\">trajectory_id</att>\n" + //
            "            <att name=\"comment\">The first two digits are the last two digits of the year and the last two are the consecutive cruise number for that particular vessel (01, 02, 03, ...).</att>\n"
            + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Cruise</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>ctd_index</sourceName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">32767</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n"
            + //
            "            <att name=\"colorBarMaximum\" type=\"double\">300</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">CTD Index</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>ctd_no</sourceName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">32767</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n"
            + //
            "            <att name=\"colorBarMaximum\" type=\"double\">7</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">1</att>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">CTD Number</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>station</sourceName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">32767</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n"
            + //
            "            <att name=\"colorBarMaximum\" type=\"double\">5000</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Station</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>time</sourceName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"long_name\">Time</att>\n" + //
            "            <att name=\"standard_name\">time</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>longitude</sourceName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">longitude</att>\n" + //
            "            <att name=\"units\">degrees_east</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>latitude</sourceName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">latitude</att>\n" + //
            "            <att name=\"units\">degrees_north</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>bottom_depth</sourceName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">32767</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n"
            + //
            "            <att name=\"colorBarMaximum\" type=\"double\">4000</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Bottom Depth</att>\n" + //
            "            <att name=\"positive\">down</att>\n" + //
            "            <att name=\"units\">meters</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>bucket_temperature</sourceName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">Bucket Temperature</att>\n" + //
            "            <att name=\"standard_name\">sea_water_temperature</att>\n" + //
            "            <att name=\"units\">degree_C</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>bucket_salinity</sourceName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">37</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">32</att>\n" + //
            "            <att name=\"ioos_category\">Salinity</att>\n" + //
            "            <att name=\"long_name\">Bucket Practical Salinity</att>\n" + //
            "            <att name=\"standard_name\">sea_water_practical_salinity</att>\n" + //
            "            <att name=\"units\">PSU</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>ts_temperature</sourceName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"comment\">The intake for the water for the thermosalinomter is usually a few meters below the surface.</att>\n"
            + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">Thermosalinomter Temperature</att>\n" + //
            "            <att name=\"standard_name\">sea_water_temperature</att>\n" + //
            "            <att name=\"units\">degree_C</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>ts_salinity</sourceName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">37</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">32</att>\n" + //
            "            <att name=\"comment\">The intake for the water for the thermosalinomter is usually a few meters below the surface.</att>\n"
            + //
            "            <att name=\"ioos_category\">Salinity</att>\n" + //
            "            <att name=\"long_name\">Thermosalinomter Practical Salinity</att>\n" + //
            "            <att name=\"standard_name\">sea_water_practical_salinity</att>\n" + //
            "            <att name=\"units\">PSU</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD gettestSimpleTestNcTable() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"testSimpleTestNcTable\" active=\"true\">\n" + //
            "    <reloadEveryNMinutes>90</reloadEveryNMinutes>\n" + //
            "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/").toURI()).toString() + "</fileDir>\n" + //
            "    <recursive>false</recursive>\n" + //
            "    <fileNameRegex>simpleTest\\.nc</fileNameRegex>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <fileTableInMemory>false</fileTableInMemory>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "        <att name=\"id\">simpleTest</att>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"cdm_data_type\">Point</att>\n" + //
            "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
            "        <att name=\"infoUrl\">???</att>\n" + //
            "        <att name=\"institution\">NOAA NMFS SWFSC ERD</att>\n" + //
            "        <att name=\"keywords\">data, local, longs, source, strings</att>\n" + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"sourceUrl\">(local files)</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"summary\">My summary.</att>\n" + //
            "        <att name=\"title\">My Title</att>\n" + //
            "    </addAttributes>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>days</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <dataType>byte</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "            <att name=\"time_precision\">1970-01-01</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>hours</sourceName>\n" + //
            "        <dataType>byte</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "            <att name=\"time_precision\">1970-01-01T00Z</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>minutes</sourceName>\n" + //
            "        <dataType>byte</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "            <att name=\"time_precision\">1970-01-01T00:00Z</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>seconds</sourceName>\n" + //
            "        <dataType>byte</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "            <att name=\"time_precision\">not valid</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>millis</sourceName>\n" + //
            "        <dataType>byte</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "            <att name=\"time_precision\">1970-01-01T00:00:00.000Z</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>bytes</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <dataType>byte</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>shorts</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>ints</sourceName>\n" + //
            "        <dataType>int</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>floats</sourceName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <!-- longs? -->\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>doubles</sourceName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>Strings</sourceName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD gettestTimeAxis() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"testTimeAxis\" active=\"true\">\n" + //
            "    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" + //
            "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/points/lisirdTss/").toURI()).toString()
            + "</fileDir>\n" + //
            "    <recursive>true</recursive>\n" + //
            "    <fileNameRegex>historical_tsi\\.csv</fileNameRegex>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <columnNamesRow>1</columnNamesRow>\n" + //
            "    <firstDataRow>2</firstDataRow>\n" + //
            "    <preExtractRegex></preExtractRegex>\n" + //
            "    <postExtractRegex></postExtractRegex>\n" + //
            "    <extractRegex></extractRegex>\n" + //
            "    <columnNameForExtract></columnNameForExtract>\n" + //
            "    <sortedColumnSourceName></sortedColumnSourceName>\n" + //
            "    <sortFilesBySourceNames></sortFilesBySourceNames>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" + //
            "        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" + //
            "        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" + //
            "    -->\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"cdm_data_type\">Other</att>\n" + //
            "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
            "        <att name=\"infoUrl\">http://lasp.colorado.edu/lisird/</att>\n" + //
            "        <att name=\"institution\">LISIRD</att>\n" + //
            "        <att name=\"keywords\">data, historical, interactive, irradiance, LASP, local, reconstruction, solar, source, time, total, tsi</att>\n"
            + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"sourceUrl\">http://lasp.colorado.edu/lisird/tss/historical_tsi.html</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"summary\">[The summary of testTimeAxis]</att>\n" + //
            "        <att name=\"title\">Historical Total Solar Irradiance Reconstruction -- testTimeAxis</att>\n" + //
            "    </addAttributes>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>time (years since 0000-01-01)</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"long_name\">Year</att>\n" + //
            "            <att name=\"standard_name\">time</att>\n" + //
            "            <att name=\"resolution\">P1Y</att>\n" + //
            "            <att name=\"precision\">1</att>\n" + //
            "            <att name=\"time_precision\">1970-01-01T00:00:00.000Z</att>\n" + //
            "            <att name=\"units\">years since 0000-01-01</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>Irradiance (W/m^2)</sourceName>\n" + //
            "        <destinationName>irradiance</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">1360</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">1362</att>\n" + //
            "            <att name=\"ioos_category\">Optical Properties</att>\n" + //
            "            <att name=\"long_name\">Total Solar Irradiance</att>\n" + //
            "            <att name=\"units\">W/m^2</att>\n" + //
            "            <att name=\"precision\">4</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD geterdGtsppBestNc() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"erdGtsppBestNc\" active=\"true\">\n" + //
            "    <reloadEveryNMinutes>1000000</reloadEveryNMinutes>\n" + //
            "    <fileDir>"
            + Path.of(EDDTestDataset.class.getResource("/largeFiles/gtspp/bestNcConsolidated/").toURI()).toString()
            + "</fileDir>\n" +
            "    <recursive>true</recursive>\n" + //
            "    <fileNameRegex>.*\\.nc</fileNameRegex>\n" + //
            "    <nDimensions>1</nDimensions>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <sortedColumnSourceName>time</sortedColumnSourceName>\n" + //
            "    <sortFilesBySourceNames>time trajectory</sortFilesBySourceNames>\n" + //
            "    <addAttributes>\n" + //
            "        <!-- acknowledgment, license come from source files -->      \n" + //
            "        <att name=\"cdm_data_type\">TrajectoryProfile</att>\n" + //
            "        <att name=\"cdm_altitude_proxy\">depth</att>\n" + //
            "        <att name=\"cdm_trajectory_variables\">trajectory, org, type, platform, cruise</att>\n" + //
            "        <att name=\"cdm_profile_variables\">station_id, longitude, latitude, time</att>\n" + //
            "        <att name=\"subsetVariables\">trajectory, org, type, platform, cruise</att>\n" + //
            "        <att name=\"crs\">EPSG:4326</att>\n" + //
            "        <att name=\"Conventions\">COARDS, WOCE, GTSPP, CF-1.6, ACDD-1.3</att>\n" + //
            "        <att name=\"creator_name\">NOAA NESDIS NODC (IN295)</att>\n" + //
            "        <att name=\"creator_url\">https://www.nodc.noaa.gov/GTSPP/</att>\n" + //
            "        <att name=\"creator_email\">nodc.gtspp@noaa.gov</att>   \n" + //
            "        <att name=\"defaultGraphQuery\">longitude,latitude,station_id&amp;time%3E=max(time)-7days&amp;time%3C=max(time)&amp;.draw=markers&amp;.marker=10|5</att>\n"
            + //
            "        <att name=\"file_source\">The GTSPP Continuously Managed Data Base</att>\n" + //
            "        <att name=\"gtspp_programVersion\">1.8</att>  <!-- temporary -->\n" + //
            "        <!-- change 2 dates here every month (2nd is date I finished processing) -->\n" + //
            "        <att name=\"history\">2022-02-01 csun writeGTSPPnc40.f90 Version 1.8\n" + //
            ".tgz files from ftp.nodc.noaa.gov /pub/data.nodc/gtspp/bestcopy/netcdf (https://www.nodc.noaa.gov/GTSPP/)\n"
            + //
            "2022-02-10 Most recent ingest, clean, and reformat at ERD (erd.data at noaa.gov).</att>\n" + //
            "        <att name=\"id\">erdGtsppBest</att>\n" + //
            "        <att name=\"infoUrl\">https://www.nodc.noaa.gov/GTSPP/</att>\n" + //
            "        <att name=\"institution\">NOAA NODC</att>\n" + //
            "        <att name=\"keywords\">\n" + //
            "Oceans &gt; Ocean Temperature &gt; Water Temperature,\n" + //
            "Oceans &gt; Salinity/Density &gt; Salinity,\n" + //
            "cruise, data, density, depth, global, gtspp, identifier, noaa, nodc, observation, ocean, oceans, organization, profile, program, salinity, sea, sea_water_practical_salinity, sea_water_temperature, seawater, station, temperature, temperature-salinity, time, type, water</att>\n"
            + //
            "        <att name=\"keywords_vocabulary\">NODC Data Types, CF Standard Names, GCMD Science Keywords</att>\n"
            + //
            "        <att name=\"LEXICON\">NODC_GTSPP</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"naming_authority\">gov.noaa.nodc</att>\n" + //
            "        <att name=\"project\">Joint IODE/JCOMM Global Temperature-Salinity Profile Programme</att>\n" + //
            "        <att name=\"references\">https://www.nodc.noaa.gov/GTSPP/</att>\n" + //
            "        <att name=\"sourceUrl\">(local files)</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <!-- change MONTH YEAR in 3rd paragraph every month -->\n" + //
            "        <att name=\"summary\">The Global Temperature-Salinity Profile Programme (GTSPP) develops and maintains a global ocean temperature and salinity resource with data that are both up-to-date and of the highest quality. It is a joint World Meteorological Organization (WMO) and Intergovernmental Oceanographic Commission (IOC) program.  It includes data from XBTs, CTDs, moored and drifting buoys, and PALACE floats. For information about organizations contributing data to GTSPP, see http://gosic.org/goos/GTSPP-data-flow.htm .  The U.S. National Oceanographic Data Center (NODC) maintains the GTSPP Continuously Managed Data Base and releases new 'best-copy' data once per month.\n"
            + //
            "\n" + //
            "WARNING: This dataset has a *lot* of data.  If you request too much data, your request will fail.\n" + //
            "* If you don't specify a longitude and latitude bounding box, don't request more than a month's data at a time.\n"
            + //
            "* If you do specify a longitude and latitude bounding box, you can request data for a proportionally longer time period.\n"
            + //
            "Requesting data for a specific station_id may be slow, but it works.\n" + //
            "\n" + //
            "*** This ERDDAP dataset has data for the entire world for all available times (currently, up to and including the January 2022 data) but is a subset of the original NODC 'best-copy' data.  It only includes data where the quality flags indicate the data is 1=CORRECT, 2=PROBABLY GOOD, or 5=MODIFIED. It does not include some of the metadata, any of the history data, or any of the quality flag data of the original dataset. You can always get the complete, up-to-date dataset (and additional, near-real-time data) from the source: https://www.nodc.noaa.gov/GTSPP/ .  Specific differences are:\n"
            + //
            "* Profiles with a position_quality_flag or a time_quality_flag other than 1|2|5 were removed.\n" + //
            "* Rows with a depth (z) value less than -0.4 or greater than 10000 or a z_variable_quality_flag other than 1|2|5 were removed.\n"
            + //
            "* Temperature values less than -4 or greater than 40 or with a temperature_quality_flag other than 1|2|5 were set to NaN.\n"
            + //
            "* Salinity values less than 0 or greater than 41 or with a salinity_quality_flag other than 1|2|5 were set to NaN.\n"
            + //
            "* Time values were converted from \"days since 1900-01-01 00:00:00\" to \"seconds since 1970-01-01T00:00:00\".\n"
            + //
            "\n" + //
            "See the Quality Flag definitions on page 5 and \"Table 2.1: Global Impossible Parameter Values\" on page 61 of\n"
            + //
            "https://www.nodc.noaa.gov/GTSPP/document/qcmans/GTSPP_RT_QC_Manual_20090916.pdf .\n" + //
            "The Quality Flag definitions are also at\n" + //
            "https://www.nodc.noaa.gov/GTSPP/document/qcmans/qcflags.htm .\n" + //
            "</att>\n" + //
            "        <att name=\"testOutOfDate\">now-45days</att>\n" + //
            "        <att name=\"title\">Global Temperature and Salinity Profile Programme (GTSPP) Data, 1985-present</att>\n"
            + //
            "    </addAttributes>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>trajectory</sourceName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Trajectory ID</att>\n" + //
            "            <att name=\"cf_role\">trajectory_id</att>\n" + //
            "            <att name=\"comment\">Constructed from org_type_platform_cruise</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>org</sourceName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Organization</att>\n" + //
            "            <att name=\"comment\">From the first 2 characters of stream_ident:\n" + //
            "Code  Meaning\n" + //
            "AD  Australian Oceanographic Data Centre\n" + //
            "AF  Argentina Fisheries (Fisheries Research and Development National Institute (INIDEP), Mar del Plata, Argentina\n"
            + //
            "AO  Atlantic Oceanographic and Meteorological Lab\n" + //
            "AP  Asia-Pacific (International Pacific Research Center/ Asia-Pacific Data-Research Center)\n" + //
            "BI  BIO Bedford institute of Oceanography\n" + //
            "CF  Canadian Navy\n" + //
            "CS  CSIRO in Australia\n" + //
            "DA  Dalhousie University\n" + //
            "FN  FNOC in Monterey, California\n" + //
            "FR  Orstom, Brest\n" + //
            "FW  Fresh Water Institute (Winnipeg)\n" + //
            "GE  BSH, Germany\n" + //
            "IC  ICES\n" + //
            "II  IIP\n" + //
            "IK  Institut fur Meereskunde, Kiel\n" + //
            "IM  IML\n" + //
            "IO  IOS in Pat Bay, BC\n" + //
            "JA  Japanese Meteorologocal Agency\n" + //
            "JF  Japan Fisheries Agency\n" + //
            "ME  EDS\n" + //
            "MO  Moncton\n" + //
            "MU  Memorial University\n" + //
            "NA  NAFC\n" + //
            "NO  NODC (Washington)\n" + //
            "NW  US National Weather Service\n" + //
            "OD  Old Dominion Univ, USA\n" + //
            "RU  Russian Federation\n" + //
            "SA  St Andrews\n" + //
            "SI  Scripps Institute of Oceanography\n" + //
            "SO  Southampton Oceanographic Centre, UK\n" + //
            "TC  TOGA Subsurface Data Centre (France)\n" + //
            "TI  Tiberon lab US\n" + //
            "UB  University of BC\n" + //
            "UQ  University of Quebec at Rimouski\n" + //
            "VL  Far Eastern Regional Hydromet. Res. Inst. of V\n" + //
            "WH  Woods Hole\n" + //
            "\n" + //
            "from https://www.nodc.noaa.gov/GTSPP/document/codetbls/gtsppcode.html#ref006\n" + //
            "</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>type</sourceName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Data Type</att>\n" + //
            "            <att name=\"comment\">From the 3rd and 4th characters of stream_ident:\n" + //
            "Code  Meaning\n" + //
            "AR  Animal mounted recorder\n" + //
            "BA  BATHY message\n" + //
            "BF  Undulating Oceanographic Recorder (e.g. Batfish CTD)\n" + //
            "BO  Bottle\n" + //
            "BT  general BT data\n" + //
            "CD  CTD down trace\n" + //
            "CT  CTD data, up or down\n" + //
            "CU  CTD up trace\n" + //
            "DB  Drifting buoy\n" + //
            "DD  Delayed mode drifting buoy data\n" + //
            "DM  Delayed mode version from originator\n" + //
            "DT  Digital BT\n" + //
            "IC  Ice core\n" + //
            "ID  Interpolated drifting buoy data\n" + //
            "IN  Ship intake samples\n" + //
            "MB  MBT\n" + //
            "MC  CTD and bottle data are mixed for the station\n" + //
            "MI  Data from a mixed set of instruments\n" + //
            "ML  Minilog\n" + //
            "OF  Real-time oxygen and fluorescence\n" + //
            "PF  Profiling float\n" + //
            "RM  Radio message\n" + //
            "RQ  Radio message with scientific QC\n" + //
            "SC  Sediment core\n" + //
            "SG  Thermosalinograph data\n" + //
            "ST  STD data\n" + //
            "SV  Sound velocity probe\n" + //
            "TE  TESAC message\n" + //
            "TG  Thermograph data\n" + //
            "TK  TRACKOB message\n" + //
            "TO  Towed CTD\n" + //
            "TR  Thermistor chain\n" + //
            "XB  XBT\n" + //
            "XC  Expendable CTD\n" + //
            "\n" + //
            "from https://www.nodc.noaa.gov/GTSPP/document/codetbls/gtsppcode.html#ref082\n" + //
            "</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>platform</sourceName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">GTSPP Platform Code</att>\n" + //
            "            <att name=\"comment\">See the list of platform codes (sorted in various ways) at https://www.nodc.noaa.gov/GTSPP/document/codetbls/calllist.html</att>\n"
            + //
            "            <att name=\"references\">https://www.nodc.noaa.gov/gtspp/document/codetbls/callist.html</att>\n"
            + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>cruise</sourceName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Cruise_ID</att>\n" + //
            "            <att name=\"comment\">Radio callsign + year for real time data, or NODC reference number for delayed mode data.  See\n"
            + //
            "https://www.nodc.noaa.gov/GTSPP/document/codetbls/calllist.html .\n" + //
            "'X' indicates a missing value.\n" + //
            "Two or more adjacent spaces in the original cruise names have been compacted to 1 space.</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>station_id</sourceName>\n" + //
            "        <destinationName>station_id</destinationName>\n" + //
            "        <dataType>int</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Station ID Number</att>\n" + //
            "            <att name=\"cf_role\">profile_id</att>\n" + //
            "            <att name=\"comment\">Identification number of the station (profile) in the GTSPP Continuously Managed Database</att>\n"
            + //
            "            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n" + //
            "            <att name=\"missing_value\" type=\"int\">2147483647</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>longitude</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"C_format\">%9.4f</att>\n" + //
            "            <att name=\"epic_code\" type=\"int\">502</att>\n" + //
            "            <att name=\"FORTRAN_format\">F9.4</att>\n" + //
            "            <att name=\"valid_max\" type=\"float\">180.0</att>\n" + //
            "            <att name=\"valid_min\" type=\"float\">-180.0</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Longitude</att>\n" + //
            "            <att name=\"standard_name\">longitude</att>\n" + //
            "            <att name=\"units\">degrees_east</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">NaN</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>latitude</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"C_format\">%8.4f</att>\n" + //
            "            <att name=\"epic_code\" type=\"int\">500</att>\n" + //
            "            <att name=\"FORTRAN_format\">F8.4</att>\n" + //
            "            <att name=\"valid_max\" type=\"float\">90.0</att>\n" + //
            "            <att name=\"valid_min\" type=\"float\">-90.0</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Latitude</att>\n" + //
            "            <att name=\"standard_name\">latitude</att>\n" + //
            "            <att name=\"units\">degrees_north</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">NaN</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>time</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"standard_name\">time</att>\n" + //
            "            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" + //
            "            <att name=\"missing_value\" type=\"double\">NaN</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>depth</sourceName>\n" + //
            "        <destinationName>depth</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"axis\">Z</att>\n" + //
            "            <att name=\"C_format\">%6.2f</att>\n" + //
            "            <att name=\"epic_code\" type=\"int\">3</att>\n" + //
            "            <att name=\"FORTRAN_format\">F6.2</att>\n" + //
            "            <att name=\"long_name\">Depth of the Observations</att>\n" + //
            "            <att name=\"standard_name\">depth</att>\n" + //
            "            <att name=\"positive\">down</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">5000</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"units\">m</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">NaN</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>temperature</sourceName>\n" + //
            "        <destinationName>temperature</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"C_format\">%9.4f</att>\n" + //
            "            <att name=\"epic_code\" type=\"int\">28</att>\n" + //
            "            <att name=\"FORTRAN_format\">F9.4</att>\n" + //
            "            <att name=\"coordinates\">time latitude longitude depth</att>\n" + //
            "            <att name=\"cell_methods\">time: point longitude: point latitude: point depth: point</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">Sea Water Temperature</att>\n" + //
            "            <att name=\"standard_name\">sea_water_temperature</att>\n" + //
            "            <att name=\"units\">degree_C</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">NaN</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>salinity</sourceName>\n" + //
            "        <destinationName>salinity</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"C_format\">%9.4f</att>\n" + //
            "            <att name=\"epic_code\" type=\"int\">41</att>\n" + //
            "            <att name=\"FORTRAN_format\">F9.4</att>\n" + //
            "            <att name=\"coordinates\">time latitude longitude depth</att>\n" + //
            "            <att name=\"cell_methods\">time: point longitude: point latitude: point depth: point</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n" + //
            "            <att name=\"ioos_category\">Salinity</att>\n" + //
            "            <att name=\"long_name\">Practical Salinity</att>\n" + //
            "            <att name=\"standard_name\">sea_water_practical_salinity</att>\n" + //
            "            <att name=\"salinity_scale\">PSU</att>\n" + //
            "            <att name=\"units\">PSU</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">NaN</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD geterdNph() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"erdNph\" active=\"true\">\n"
        + //
        "    <reloadEveryNMinutes>15000</reloadEveryNMinutes>\n" + //
        "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/points/isaac/").toURI()).toString()
        + "</fileDir>\n" + //
        "    <recursive>false</recursive>\n" + //
        "    <fileNameRegex>NPH_IDS\\.nc</fileNameRegex>\n" + //
        "    <metadataFrom>last</metadataFrom>\n" + //
        "    <sortedColumnSourceName>time</sortedColumnSourceName>\n" + //
        "    <sortFilesBySourceNames>time</sortFilesBySourceNames>\n" + //
        "    <fileTableInMemory>false</fileTableInMemory>\n" + //
        "    <defaultDataQuery>time,year,month,longitude,latitude,area,maxSLP</defaultDataQuery>\n" + //
        "    <defaultGraphQuery>longitude,latitude,month&amp;.draw=markers</defaultGraphQuery>\n" + //
        "    <!-- sourceAttributes>\n" + //
        "    </sourceAttributes -->\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"cdm_data_type\">Point</att>\n" + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"id\">erdNph</att>\n" + //
        "        <att name=\"infoUrl\">https://onlinelibrary.wiley.com/doi/10.1002/grl.50100/abstract</att>\n" + //
        "        <att name=\"institution\">NOAA ERD</att>\n" + //
        "        <att name=\"keywords\">area, areal, california, ccs, center, centered, contour, current, data, extent, high, hpa, level, maximum, month, north, nph, pacific, pressure, sea, system, time, year</att>\n"
        + //
        "        <att name=\"license\">[standard]</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"references\">Schroeder, Isaac D., Bryan A. Black, William J. Sydeman, Steven J. Bograd, Elliott L. Hazen, Jarrod A. Santora, and Brian K. Wells. \"The North Pacific High and wintertime pre-conditioning of California current productivity\", Geophys. Res. Letters, VOL. 40, 541-546, doi:10.1002/grl.50100, 2013</att>\n"
        + //
        "        <att name=\"sourceUrl\">(local files)</att>\n" + //
        "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
        "        <att name=\"subsetVariables\">time, year, month</att>\n" + //
        "        <att name=\"summary\">Variations in large-scale atmospheric forcing influence upwelling dynamics and ecosystem productivity in the California Current System (CCS). In this paper, we characterize interannual variability of the North Pacific High over 40 years and investigate how variation in its amplitude and position affect upwelling and biology. We develop a winter upwelling \"pre-conditioning\" index and demonstrate its utility to understanding biological processes. Variation in the winter NPH can be well described by its areal extent and maximum pressure, which in turn is predictive of winter upwelling. Our winter pre-conditioning index explained 64% of the variation in biological responses (fish and seabirds). Understanding characteristics of the NPH in winter is therefore critical to predicting biological responses in the CCS.</att>\n"
        + //
        "        <att name=\"title\">North Pacific High, 1967 - 2014</att>\n" + //
        "    </addAttributes>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>time</sourceName>\n" + //
        "        <destinationName>time</destinationName>\n" + //
        "        <dataType>double</dataType>\n" + //
        "        <!-- sourceAttributes>\n" + //
        "            <att name=\"long_name\">Centered Time</att>\n" + //
        "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" + //
        "        </sourceAttributes -->\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Time</att>\n" + //
        "            <att name=\"standard_name\">time</att>\n" + //
        "            <att name=\"time_precision\">1970-01-01</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>year</sourceName>\n" + //
        "        <destinationName>year</destinationName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <!-- sourceAttributes>\n" + //
        "            <att name=\"long_name\">Year</att>\n" + //
        "        </sourceAttributes -->\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"_FillValue\" type=\"short\">32767</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n"
        + //
        "            <att name=\"ioos_category\">Time</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>month</sourceName>\n" + //
        "        <destinationName>month</destinationName>\n" + //
        "        <dataType>byte</dataType>\n" + //
        "        <!-- sourceAttributes>\n" + //
        "            <att name=\"long_name\">Month (1 - 12)</att>\n" + //
        "        </sourceAttributes -->\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"_FillValue\" type=\"byte\">127</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n"
        + //
        "            <att name=\"ioos_category\">Time</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>    <dataVariable>\n" + //
        "        <sourceName>longitude</sourceName>\n" + //
        "        <destinationName>longitude</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <!-- sourceAttributes>\n" + //
        "            <att name=\"long_name\">Longitude of the Center of the NPH</att>\n" + //
        "            <att name=\"units\">degrees_east</att>\n" + //
        "        </sourceAttributes -->\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" + //
        "            <att name=\"ioos_category\">Location</att>\n" + //
        "            <att name=\"standard_name\">longitude</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>latitude</sourceName>\n" + //
        "        <destinationName>latitude</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <!-- sourceAttributes>\n" + //
        "            <att name=\"long_name\">Latitude of the Center of the NPH</att>\n" + //
        "            <att name=\"units\">degrees_north</att>\n" + //
        "        </sourceAttributes -->\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" + //
        "            <att name=\"ioos_category\">Location</att>\n" + //
        "            <att name=\"standard_name\">latitude</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>area</sourceName>\n" + //
        "        <destinationName>area</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <!-- sourceAttributes>\n" + //
        "            <att name=\"long_name\">Areal Extent of the 1020 hPa Contour</att>\n" + //
        "            <att name=\"units\">km2</att>\n" + //
        "        </sourceAttributes -->\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Pressure</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>maxSLP</sourceName>\n" + //
        "        <destinationName>maxSLP</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <!-- sourceAttributes>\n" + //
        "            <att name=\"long_name\">Maximum Sea Level Pressure</att>\n" + //
        "            <att name=\"units\">hPa</att>\n" + //
        "        </sourceAttributes -->\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Pressure</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD gettestTimePrecisionMillisTable() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"testTimePrecisionMillisTable\" active=\"true\">\n" + //
            "    <reloadEveryNMinutes>1</reloadEveryNMinutes>\n" + //
            "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/dominic2/").toURI()).toString()
            + "</fileDir>\n" + //
            "    <recursive>false</recursive>\n" + //
            "    <fileNameRegex>.*\\.nc</fileNameRegex>\n" + //
            "    <addAttributes> \n" + //
            "        <att name=\"cdm_data_type\">Other</att>\n" + //
            "        <att name=\"infoUrl\">http://www.goes.noaa.gov/</att>\n" + //
            "        <att name=\"sourceUrl\">(local files)</att>\n" + //
            "    </addAttributes>\n" + //
            "    <dataVariable>\n" + //
            "      <sourceName>OB_time</sourceName>\n" + //
            "      <destinationName>time</destinationName>\n" + //
            "      <dataType>double</dataType>\n" + //
            "      <addAttributes>\n" + //
            "        <att name=\"ioos_category\">Time</att>\n" + //
            "        <att name=\"time_precision\">1970-01-01T00:00:00.000Z</att>\n" + //
            "        <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" + //
            "      </addAttributes>\n" + //
            "    </dataVariable> \n" + //
            "    <dataVariable>\n" + //
            "      <sourceName>ECEF_X</sourceName>\n" + //
            "      <dataType>float</dataType>\n" + //
            "      <addAttributes>\n" + //
            "        <att name=\"ioos_category\">Other</att>\n" + //
            "      </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "      <sourceName>IB_time</sourceName>\n" + //
            "      <dataType>double</dataType>\n" + //
            "      <addAttributes>\n" + //
            "        <att name=\"ioos_category\">Other</att>\n" + //
            "        <att name=\"time_precision\">1970-01-01T00:00:00.000Z</att>\n" + //
            "        <att name=\"units\">seconds since 1980-01-01T00:00:00Z</att>\n" + //
            "      </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD getLiquidR_HBG3_2015_weather() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"LiquidR_HBG3_2015_weather\" active=\"true\">\n" + //
            "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" + //
            "    <defaultGraphQuery>longitude,latitude,temperature&amp;time&gt;=now-7days&amp;time&lt;=now&amp;.draw=markers&amp;.marker=10|5</defaultGraphQuery>\n"
            + //
            "    <fileDir>"
            + Path.of(EDDTestDataset.class.getResource("/data/points/LiquidR_HBG3_2015/").toURI()).toString()
            + "</fileDir>\n"
            + //
            "    <recursive>false</recursive>\n" + //
            "    <fileNameRegex>weather.*\\.csv</fileNameRegex>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <charset>ISO-8859-1</charset>\n" + //
            "    <columnNamesRow>1</columnNamesRow>\n" + //
            "    <firstDataRow>2</firstDataRow>\n" + //
            "    <preExtractRegex></preExtractRegex>\n" + //
            "    <postExtractRegex></postExtractRegex>\n" + //
            "    <extractRegex></extractRegex>\n" + //
            "    <columnNameForExtract></columnNameForExtract>\n" + //
            "    <sortedColumnSourceName>datetime</sortedColumnSourceName>\n" + //
            "    <sortFilesBySourceNames>datetime</sortFilesBySourceNames>\n" + //
            "    <fileTableInMemory>false</fileTableInMemory>\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"cdm_data_type\">Trajectory</att>\n" + //
            "        <att name=\"cdm_trajectory_variables\">vehicleName, weather, feed_version</att>\n" + //
            "        <att name=\"subsetVariables\">vehicleName, weather, feed_version</att>\n" + //
            "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
            "        <att name=\"contributor_name\">Liquid Robotics</att>\n" + //
            "        <att name=\"contributor_email\">sotiria.lampoudi@liquidr.com</att>\n" + //
            "        <att name=\"contributor_role\">provided the glider time and all operational support</att>\n" + //
            "        <att name=\"contributor_url\">http://liquidr.com/</att>\n" + //
            "        <att name=\"creator_name\">Tracy Villareal</att>\n" + //
            "        <att name=\"creator_email\">tracyv@austin.utexas.edu</att>\n" + //
            "        <att name=\"creator_type\">person</att>\n" + //
            "        <att name=\"creator_url\">https://www.utmsi.utexas.edu/component/cobalt/item/9-marine-science/330-villareal-tracy-a?Itemid=550</att>\n"
            + //
            "        <att name=\"history\">A script frequently retrieves new data from Liquid Robotics' Data Portal and stores it in a file at NOAA NMFS SWFSC ERD. (erd.data at noaa.gov)</att>\n"
            + //
            "        <att name=\"infoUrl\">https://oceanview.pfeg.noaa.gov/MAGI/</att>\n" + //
            "        <att name=\"institution\">Liquid Robotics, UT Austin, NOAA NMFS SWFSC ERD</att>\n" + //
            "        <att name=\"keywords\">atmosphere,\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Surface Winds,\n" + //
            "atmospheric, avg_wind_direction, avg_wind_speed, bar, data, datetime, direction, feed, feed_version, latitude, local, longitude, meteorology, name, pressure, source, speed, std_dev_wind_direction, std_dev_wind_speed, surface, temperature, time, vehicle, vehicleName, version, weather, wind, wind_from_direction, wind_speed, winds, liquid, robotics, liquid robotics, wave, glider, wave glider, Honey Badger, 2015, MAGI, chlorophyll, bloom, phytoplankton, nitrogen-fixing, diatom</att>\n"
            + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"sourceUrl\">(local files)</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"summary\">Liquid Robotics Wave Glider, Honey Badger (G3), 2015, Weather. The MAGI mission is to use the Wave Glider to sample the late summer chlorophyll bloom that develops near 30&deg;N, with the goal of using the camera and LISST-Holo to try to identify species in the blooms and then follow the development of phytoplankton aggregates. These aggregates have recently been shown to be a significant part of the total amount of carbon that sinks to the deep sea. Karl et al (2012) found that in each of the past 13 years, there was a flux of material to 4,000 m (the summer export pulse) that represented ~20% of the total annual flux. Work based on satellite ocean color data over the past decade has revealed the existence of large phytoplankton blooms in the Pacific Ocean that cover thousands of km2, persist for weeks or longer, and are often dominated by nitrogen-fixing diatom symbioses (Wilson et al. 2008). We hope to be able to examine whether this aggregation is occurring in the vast oceanic regions north and east of Hawai'i and provide a basin-scale context for the ALOHA observations. These events have proven difficult to study outside of the time series station ALOHA at Hawai'i.</att>\n"
            + //
            "        <att name=\"title\">Liquid Robotics Wave Glider, Honey Badger (G3), 2015, Weather</att>\n" + //
            "    </addAttributes>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>vehicleName</sourceName>\n" + //
            "        <destinationName>vehicleName</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"cf_role\">trajectory_id</att>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Vehicle Name</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>weather*</sourceName>\n" + //
            "        <destinationName>weather</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Meteorology</att>\n" + //
            "            <att name=\"long_name\">Weather</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>feed_version</sourceName>\n" + //
            "        <destinationName>feed_version</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Other</att>\n" + //
            "            <att name=\"long_name\">Feed Version</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>datetime</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"long_name\">Datetime</att>\n" + //
            "            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ssZ</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>latitude (decimal degrees)</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Latitude</att>\n" + //
            "            <att name=\"missing_value\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"standard_name\">latitude</att>\n" + //
            "            <att name=\"units\">degrees_north</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>longitude (decimal degrees)</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Longitude</att>\n" + //
            "            <att name=\"missing_value\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"standard_name\">longitude</att>\n" + //
            "            <att name=\"units\">degrees_east</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>temperature (C)</sourceName>\n" + //
            "        <destinationName>temperature</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">Temperature</att>\n" + //
            "            <att name=\"units\">degree_C</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>pressure (mBar)</sourceName>\n" + //
            "        <destinationName>pressure</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">950</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">1050</att>\n" + //
            "            <att name=\"ioos_category\">Pressure</att>\n" + //
            "            <att name=\"long_name\">Pressure</att>\n" + //
            "            <att name=\"units\">mBar</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>avg_wind_speed (kt)</sourceName>\n" + //
            "        <destinationName>avg_wind_speed</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "            <att name=\"long_name\">Wind Speed</att>\n" + //
            "            <att name=\"standard_name\">wind_speed</att>\n" + //
            "            <att name=\"units\">knots</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>std_dev_wind_speed (kt)</sourceName>\n" + //
            "        <destinationName>std_dev_wind_speed</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "            <att name=\"long_name\">Wind Speed</att>\n" + //
            "            <att name=\"standard_name\">wind_speed</att>\n" + //
            "            <att name=\"units\">knots</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>avg_wind_direction (degrees T)</sourceName>\n" + //
            "        <destinationName>avg_wind_direction</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "            <att name=\"long_name\">Wind From Direction</att>\n" + //
            "            <att name=\"standard_name\">wind_from_direction</att>\n" + //
            "            <att name=\"units\">degrees_true</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>std_dev_wind_direction (degrees T)</sourceName>\n" + //
            "        <destinationName>std_dev_wind_direction</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "            <att name=\"long_name\">Wind From Direction</att>\n" + //
            "            <att name=\"standard_name\">wind_from_direction</att>\n" + //
            "            <att name=\"units\">degrees_true</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD gettestPrecision() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDTableFromNcCFFiles\" datasetID=\"testPrecision\" active=\"true\">\n" + //
            "        <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" + //
            "        <updateEveryNMillis>10000</updateEveryNMillis>\n" + //
            "        <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/pacioos/").toURI()).toString()
            + "</fileDir>\n" + //
            "        <fileNameRegex>wqb04_.*\\.nc</fileNameRegex>\n" + //
            "        <recursive>false</recursive>\n" + //
            "        <pathRegex>.*</pathRegex>\n" + //
            "        <metadataFrom>last</metadataFrom>\n" + //
            "        <removeMVRows>true</removeMVRows>\n" + //
            "        <sortedColumnSourceName>time</sortedColumnSourceName>\n" + //
            "        <defaultGraphQuery>time,temperature&amp;time&gt;=max(time)-3days&amp;.draw=lines</defaultGraphQuery>\n"
            + //
            "        <defaultDataQuery>time,temperature,salinity,turbidity,chlorophyll,oxygen,oxygen_saturation&amp;time&gt;=max(time)-3days</defaultDataQuery>\n"
            + //
            "        <addAttributes>\n" + //
            "            <!-- Add/change NetCDF attributes: -->\n" + //
            "            <att name=\"date_created\">2013-05-12</att>\n" + //
            "            <att name=\"date_issued\">2013-05-12</att>\n" + //
            "            <att name=\"date_modified\">2019-04-29</att>\n" + //
            "            <att name=\"date_metadata_modified\">2019-04-29</att>\n" + //
            "            <att name=\"time_coverage_duration\"></att> <!-- remove: download duration != file duration -->\n"
            + //
            "            <!-- ERDDAP-specific: -->\n" + //
            "            <att name=\"infoUrl\">http://pacioos.org/water/wqbuoy-hilo/</att>\n" + //
            "            <att name=\"sourceUrl\">http://pacioos.org</att>\n" + //
            "            <att name=\"cdm_timeseries_variables\">station_name, longitude, latitude, depth</att>\n" + //
            "            <att name=\"testOutOfDate\">now-90minutes</att>\n" + //
            "        </addAttributes>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>time</sourceName>\n" + //
            "            <dataType>float</dataType>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"ioos_category\">Other</att>\n" + //
            "            </addAttributes>              \n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>latitude</sourceName>\n" + //
            "            <dataType>float</dataType>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"ioos_category\">Other</att>\n" + //
            "            </addAttributes>              \n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>longitude</sourceName>\n" + //
            "            <dataType>float</dataType>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"ioos_category\">Other</att>\n" + //
            "            </addAttributes>              \n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>depth</sourceName>\n" + //
            "            <dataType>float</dataType>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"ioos_category\">Other</att>\n" + //
            "            </addAttributes>              \n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>station_name</sourceName>\n" + //
            "            <dataType>int</dataType>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"ioos_category\">Other</att>\n" + //
            "            </addAttributes>              \n" + //
            "        </dataVariable>\n" + //
            "        <!-- Does not make sense to include in aggregation:\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>qc_flag</sourceName>\n" + //
            "            <dataType>int</dataType>\n" + //
            "        </dataVariable>\n" + //
            "        -->\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>temperature</sourceName>\n" + //
            "            <dataType>float</dataType>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"ioos_category\">Other</att>\n" + //
            "                <att name=\"add_offset\">null</att>\n" + //
            "                <att name=\"scale_factor\">null</att>\n" + //
            "            </addAttributes>              \n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>salinity</sourceName>\n" + //
            "            <dataType>float</dataType>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"ioos_category\">Other</att>\n" + //
            "            </addAttributes>              \n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>turbidity</sourceName>\n" + //
            "            <dataType>float</dataType>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"ioos_category\">Other</att>\n" + //
            "            </addAttributes>              \n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>chlorophyll</sourceName>\n" + //
            "            <dataType>float</dataType>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"ioos_category\">Other</att>\n" + //
            "            </addAttributes>              \n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>oxygen</sourceName>\n" + //
            "            <dataType>float</dataType>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"ioos_category\">Other</att>\n" + //
            "            </addAttributes>              \n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>oxygen_saturation</sourceName>\n" + //
            "            <dataType>float</dataType>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"ioos_category\">Other</att>\n" + //
            "            </addAttributes>              \n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>ph</sourceName>\n" + //
            "            <dataType>float</dataType>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"ioos_category\">Other</att>\n" + //
            "            </addAttributes>              \n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>temperature_raw</sourceName>\n" + //
            "            <dataType>float</dataType>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"ioos_category\">Other</att>\n" + //
            "            </addAttributes>              \n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>temperature_dm_qd</sourceName>\n" + //
            "            <dataType>int</dataType>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"ioos_category\">Other</att>\n" + //
            "            </addAttributes>              \n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>salinity_raw</sourceName>\n" + //
            "            <dataType>float</dataType>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"ioos_category\">Other</att>\n" + //
            "            </addAttributes>              \n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>salinity_dm_qd</sourceName>\n" + //
            "            <dataType>int</dataType>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"ioos_category\">Other</att>\n" + //
            "            </addAttributes>              \n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>turbidity_raw</sourceName>\n" + //
            "            <dataType>float</dataType>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"ioos_category\">Other</att>\n" + //
            "            </addAttributes>              \n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>turbidity_dm_qd</sourceName>\n" + //
            "            <dataType>int</dataType>\n" + //
            "            <addAttributes>\n" + //
            "                <att name=\"ioos_category\">Other</att>\n" + //
            "            </addAttributes>              \n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>chlorophyll_raw</sourceName>\n" + //
            "            <dataType>float</dataType>\n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>chlorophyll_dm_qd</sourceName>\n" + //
            "            <dataType>int</dataType>\n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>oxygen_raw</sourceName>\n" + //
            "            <dataType>float</dataType>\n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>oxygen_dm_qd</sourceName>\n" + //
            "            <dataType>int</dataType>\n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>oxygen_saturation_raw</sourceName>\n" + //
            "            <dataType>float</dataType>\n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>oxygen_saturation_dm_qd</sourceName>\n" + //
            "            <dataType>int</dataType>\n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>ph_raw</sourceName>\n" + //
            "            <dataType>float</dataType>\n" + //
            "        </dataVariable>\n" + //
            "        <dataVariable>\n" + //
            "            <sourceName>ph_dm_qd</sourceName>\n" + //
            "            <dataType>int</dataType>\n" + //
            "        </dataVariable>\n" + //
            "    </dataset>");
  }

  public static EDD geterdCAMarCatSY() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"erdCAMarCatSY\">\n" + //
        "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/points/erdCAMarCatSY/").toURI()).toString()
        + "</fileDir>\n" + //
        "    <recursive>true</recursive>\n" + //
        "    <fileNameRegex>.*\\.nc</fileNameRegex>\n" + //
        "    <metadataFrom>last</metadataFrom>\n" + //
        "    <sortedColumnSourceName>time</sortedColumnSourceName>\n" + //
        "    <sortFilesBySourceNames>fish port</sortFilesBySourceNames>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"cdm_data_type\">Other</att>\n" + //
        "        <att name=\"subsetVariables\">time, year, fish, port, landings</att>\n" + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"creator_name\">California Department of Fish and Game</att>\n" + //
        "        <att name=\"creator_type\">institution</att>\n" + //
        "        <att name=\"creator_url\">http://www.dfg.ca.gov/</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"infoUrl\">https://oceanview.pfeg.noaa.gov/las_fish1/doc/names_describe.html</att>\n" + //
        "        <att name=\"institution\">CA DFG, NOAA ERD</att>\n" + //
        "        <att name=\"keywords\">\n" + //
        "Earth Science &gt; Biological Classification &gt; Animals/Vertebrates &gt; Fish,\n" + //
        "Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Coastal Habitat,\n" + //
        "Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Marine Habitat,\n" + //
        "Earth Science &gt; Oceans &gt; Aquatic Sciences &gt; Fisheries,\n" + //
        "abundance, animals, aquatic, biological, biosphere, california, catch, centered, classification, coastal, dfg, ecosystems, erd, fish, fish abundance, fish species, fisheries, habitat, identifier, landings, list, marine, market, name, noaa, oceans, port, sciences, short, species, time, vertebrates, year, yearly</att>\n"
        + //
        "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
        "        <att name=\"license\">[standard]</att>\n" + //
        "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" + //
        "        <att name=\"publisher_name\">Janet Mason, NOAA/NMFS/SWFSC Environmental Research Division</att>\n" + //
        "        <att name=\"publisher_url\">https://www.pfeg.noaa.gov</att>\n" + //
        "        <att name=\"publisher_email\">Janet.Mason@noaa.gov</att>\n" + //
        "        <att name=\"sourceUrl\">https://oceanview.pfeg.noaa.gov/thredds/dodsC/CA_market_catch/ca_fish_grouped_short.nc</att>\n"
        + //
        "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
        "        <att name=\"summary\">Database of fish and invertebrates caught off California and landed in California, including commercial freshwater catches in California through but not after 1971 and some maricultured shellfish such as oysters through 1980.  For more information, see\n"
        + //
        "https://oceanview.pfeg.noaa.gov/las_fish1/doc/names_describe.html and\n" + //
        "https://oceanview.pfeg.noaa.gov/las_fish1/doc/marketlist.html .\n" + //
        "\n" + //
        "This dataset has the sums of the monthly values for each calendar year.\n" + //
        "</att>\n" + //
        "        <att name=\"title\">California Fish Market Catch Landings, Short List, 1928-2002, Yearly</att>\n" + //
        "        <att name=\"id\" />\n" + //
        "        <att name=\"observationDimension\" />\n" + //
        "    </addAttributes>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>time</sourceName>\n" + //
        "        <dataType>double</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"long_name\">Centered Time</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>year</sourceName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"_FillValue\" type=\"short\">32767</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n"
        + //
        "            <att name=\"ioos_category\">Time</att>\n" + //
        "            <att name=\"long_name\">Year</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>fish</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Fish Species</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>port</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>landings</sourceName>\n" + //
        "        <dataType>int</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Fish Abundance</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD geterdCAMarCatSM() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"erdCAMarCatSM\">\n" + //
        "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/points/erdCAMarCatSM/").toURI()).toString()
        + "</fileDir>\n" + //
        "    <recursive>true</recursive>\n" + //
        "    <fileNameRegex>.*\\.nc</fileNameRegex>\n" + //
        "    <metadataFrom>last</metadataFrom>\n" + //
        "    <sortedColumnSourceName>time</sortedColumnSourceName>\n" + //
        "    <sortFilesBySourceNames>fish port</sortFilesBySourceNames>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"cdm_data_type\">Other</att>\n" + //
        "        <att name=\"subsetVariables\">time, year, fish, port, landings</att>\n" + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"creator_name\">California Department of Fish and Game</att>\n" + //
        "        <att name=\"creator_type\">institution</att>\n" + //
        "        <att name=\"creator_url\">http://www.dfg.ca.gov/</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"infoUrl\">https://oceanview.pfeg.noaa.gov/las_fish1/doc/names_describe.html</att>\n" + //
        "        <att name=\"institution\">CA DFG, NOAA ERD</att>\n" + //
        "        <att name=\"keywords\">\n" + //
        "Earth Science &gt; Biological Classification &gt; Animals/Vertebrates &gt; Fish,\n" + //
        "Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Coastal Habitat,\n" + //
        "Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Marine Habitat,\n" + //
        "Earth Science &gt; Oceans &gt; Aquatic Sciences &gt; Fisheries,\n" + //
        "abundance, animals, aquatic, biological, biosphere, california, catch, centered, classification, coastal, dfg, ecosystems, erd, fish, fish abundance, fish species, fisheries, habitat, identifier, landings, list, marine, market, monthly, name, noaa, oceans, port, sciences, short, species, time, vertebrates, year</att>\n"
        + //
        "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
        "        <att name=\"license\">[standard]</att>\n" + //
        "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" + //
        "        <att name=\"publisher_name\">Janet Mason, NOAA/NMFS/SWFSC Environmental Research Division</att>\n" + //
        "        <att name=\"publisher_url\">https://www.pfeg.noaa.gov</att>\n" + //
        "        <att name=\"publisher_email\">Janet.Mason@noaa.gov</att>\n" + //
        "        <att name=\"sourceUrl\">https://oceanview.pfeg.noaa.gov/thredds/dodsC/CA_market_catch/ca_fish_grouped_short.nc</att>\n"
        + //
        "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
        "        <att name=\"summary\">Database of fish and invertebrates caught off California and landed in California, including commercial freshwater catches in California through but not after 1971 and some maricultured shellfish such as oysters through 1980.  For more information, see\n"
        + //
        "https://oceanview.pfeg.noaa.gov/las_fish1/doc/names_describe.html and\n" + //
        "https://oceanview.pfeg.noaa.gov/las_fish1/doc/marketlist.html .</att>\n" + //
        "        <att name=\"title\">California Fish Market Catch Landings, Short List, 1928-2002, Monthly</att>\n" + //
        "        <att name=\"id\" />\n" + //
        "        <att name=\"observationDimension\" />\n" + //
        "    </addAttributes>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>time</sourceName>\n" + //
        "        <dataType>double</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"long_name\">Centered Time</att>\n" + //
        "            <!--att name=\"time_precision\">1970-01-01</att-->\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>year</sourceName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"_FillValue\" type=\"short\">32767</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n"
        + //
        "            <att name=\"ioos_category\">Time</att>\n" + //
        "            <att name=\"long_name\">Year</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>fish</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Fish Species</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>port</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>landings</sourceName>\n" + //
        "        <dataType>int</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Fish Abundance</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD geterdCAMarCatLM() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"erdCAMarCatLM\">\n" + //
        "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/points/erdCAMarCatLM/").toURI()).toString()
        + "</fileDir>\n" + //
        "    <recursive>true</recursive>\n" + //
        "    <fileNameRegex>.*\\.nc</fileNameRegex>\n" + //
        "    <metadataFrom>last</metadataFrom>\n" + //
        "    <sortedColumnSourceName>time</sortedColumnSourceName>\n" + //
        "    <sortFilesBySourceNames>fish port</sortFilesBySourceNames>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"cdm_data_type\">Other</att>\n" + //
        "        <att name=\"subsetVariables\">fish, port</att>\n" + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"creator_name\">California Department of Fish and Game</att>\n" + //
        "        <att name=\"creator_type\">institution</att>\n" + //
        "        <att name=\"creator_url\">http://www.dfg.ca.gov/</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"infoUrl\">https://oceanview.pfeg.noaa.gov/las_fish1/doc/names_describe.html</att>\n" + //
        "        <att name=\"institution\">CA DFG, NOAA ERD</att>\n" + //
        "        <att name=\"keywords\">\n" + //
        "Earth Science &gt; Biological Classification &gt; Animals/Vertebrates &gt; Fish,\n" + //
        "Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Coastal Habitat,\n" + //
        "Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Marine Habitat,\n" + //
        "Earth Science &gt; Oceans &gt; Aquatic Sciences &gt; Fisheries,\n" + //
        "abundance, animals, aquatic, biological, biosphere, california, catch, centered, classification, coastal, dfg, ecosystems, erd, fish, fish abundance, fish species, fisheries, habitat, identifier, landings, list, long, marine, market, monthly, name, noaa, oceans, port, sciences, species, time, vertebrates, year</att>\n"
        + //
        "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
        "        <att name=\"license\">[standard]</att>\n" + //
        "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" + //
        "        <att name=\"publisher_name\">Janet Mason, NOAA/NMFS/SWFSC Environmental Research Division</att>\n" + //
        "        <att name=\"publisher_url\">https://www.pfeg.noaa.gov</att>\n" + //
        "        <att name=\"publisher_email\">Janet.Mason@noaa.gov</att>\n" + //
        "        <att name=\"sourceUrl\">https://oceanview.pfeg.noaa.gov/thredds/dodsC/CA_market_catch/ca_fish_grouped.nc</att>\n"
        + //
        "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
        "        <att name=\"summary\">Database of fish and invertebrates caught off California and landed in California, including commercial freshwater catches in California through but not after 1971 and some maricultured shellfish such as oysters through 1980.  For more information, see\n"
        + //
        "https://oceanview.pfeg.noaa.gov/las_fish1/doc/names_describe.html and\n" + //
        "https://oceanview.pfeg.noaa.gov/las_fish1/doc/marketlist.html .</att>\n" + //
        "        <att name=\"title\">California Fish Market Catch Landings, Long List, 1928-2002, Monthly</att>\n" + //
        "        <att name=\"id\" />\n" + //
        "        <att name=\"observationDimension\" />\n" + //
        "    </addAttributes>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>time</sourceName>\n" + //
        "        <dataType>double</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"long_name\">Centered Time</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>year</sourceName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"_FillValue\" type=\"short\">32767</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n"
        + //
        "            <att name=\"ioos_category\">Time</att>\n" + //
        "            <att name=\"long_name\">Year</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>fish</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Fish Species</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>port</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>landings</sourceName>\n" + //
        "        <dataType>int</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Fish Abundance</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD geterdCAMarCatLY() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"erdCAMarCatLY\">\n" + //
        "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/points/erdCAMarCatLY/").toURI()).toString()
        + "</fileDir>\n" + //
        "    <recursive>true</recursive>\n" + //
        "    <fileNameRegex>.*\\.nc</fileNameRegex>\n" + //
        "    <metadataFrom>last</metadataFrom>\n" + //
        "    <sortedColumnSourceName>time</sortedColumnSourceName>\n" + //
        "    <sortFilesBySourceNames>fish port</sortFilesBySourceNames>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"cdm_data_type\">Other</att>\n" + //
        "        <att name=\"subsetVariables\">time, year, fish, port, landings</att>\n" + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"creator_name\">California Department of Fish and Game</att>\n" + //
        "        <att name=\"creator_type\">institution</att>\n" + //
        "        <att name=\"creator_url\">http://www.dfg.ca.gov/</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"infoUrl\">https://oceanview.pfeg.noaa.gov/las_fish1/doc/names_describe.html</att>\n" + //
        "        <att name=\"keywords\">\n" + //
        "Earth Science &gt; Biological Classification &gt; Animals/Vertebrates &gt; Fish,\n" + //
        "Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Coastal Habitat,\n" + //
        "Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Marine Habitat,\n" + //
        "Earth Science &gt; Oceans &gt; Aquatic Sciences &gt; Fisheries,\n" + //
        "abundance, animals, aquatic, biological, biosphere, california, catch, centered, classification, coastal, dfg, ecosystems, erd, fish, fish abundance, fish species, fisheries, habitat, identifier, landings, list, long, marine, market, name, noaa, oceans, port, sciences, species, time, vertebrates, year, yearly</att>\n"
        + //
        "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
        "        <att name=\"institution\">CA DFG, NOAA ERD</att>\n" + //
        "        <att name=\"license\">[standard]</att>\n" + //
        "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" + //
        "        <att name=\"publisher_name\">Janet Mason, NOAA/NMFS/SWFSC Environmental Research Division</att>\n" + //
        "        <att name=\"publisher_url\">https://www.pfeg.noaa.gov</att>\n" + //
        "        <att name=\"publisher_email\">Janet.Mason@noaa.gov</att>\n" + //
        "        <att name=\"sourceUrl\">https://oceanview.pfeg.noaa.gov/thredds/dodsC/CA_market_catch/ca_fish_grouped.nc</att>\n"
        + //
        "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
        "        <att name=\"summary\">Database of fish and invertebrates caught off California and landed in California, including commercial freshwater catches in California through but not after 1971 and some maricultured shellfish such as oysters through 1980.  For more information, see\n"
        + //
        "https://oceanview.pfeg.noaa.gov/las_fish1/doc/names_describe.html and\n" + //
        "https://oceanview.pfeg.noaa.gov/las_fish1/doc/marketlist.html .\n" + //
        "\n" + //
        "This dataset has the sums of the monthly values for each calendar year.\n" + //
        "</att>\n" + //
        "        <att name=\"title\">California Fish Market Catch Landings, Long List, 1928-2002, Yearly</att>\n" + //
        "        <att name=\"id\" />\n" + //
        "        <att name=\"observationDimension\" />\n" + //
        "    </addAttributes>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>time</sourceName>\n" + //
        "        <dataType>double</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"long_name\">Centered Time</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>year</sourceName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"_FillValue\" type=\"short\">32767</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n"
        + //
        "            <att name=\"ioos_category\">Time</att>\n" + //
        "            <att name=\"long_name\">Year</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>fish</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Fish Species</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>port</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>landings</sourceName>\n" + //
        "        <dataType>int</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Fish Abundance</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD getnwioosCoral() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDTableFromDapSequence\" datasetID=\"nwioosCoral\" active=\"false\"> <!-- 2020-01-07 server is gone -->\n"
            + //
            "    <sourceUrl>http://nwioos.coas.oregonstate.edu:8080/dods/drds/Coral%201980-2005</sourceUrl>\n" + //
            "    <outerSequenceName>CORAL_1980_2005</outerSequenceName>\n" + //
            "    <innerSequenceName></innerSequenceName>\n" + //
            "    <sourceCanConstrainStringRegex></sourceCanConstrainStringRegex>\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"title\">NWFSC Coral Data Collected off West Coast of US (1980-2005)</att>\n" + //
            "        <att name=\"summary\">\n" + //
            "This data contains the locations of some observations of\n" + //
            "cold-water/deep-sea corals off the west coast of the United States.\n" + //
            "Records of coral catch originate from bottom trawl surveys conducted\n" + //
            "from 1980 to 2001 by the Alaska Fisheries Science Center (AFSC) and\n" + //
            "2001 to 2005 by the Northwest Fisheries Science Center (NWFSC).\n" + //
            "Locational information represent the vessel mid positions (for AFSC\n" + //
            "survey trawls) or \"best position\" (i.e., priority order: 1) gear\n" + //
            "midpoint 2) vessel midpoint, 3) vessel start point, 4) vessel end\n" + //
            "point, 5) station coordinates for NWFSC survey trawls) conducted as\n" + //
            "part of regular surveys of groundfish off the coasts of Washington,\n" + //
            "Oregon and California by NOAA Fisheries. Only records where corals\n" + //
            "were identified in the total catch are included. Each catch sample\n" + //
            "of coral was identified down to the most specific taxonomic level\n" + //
            "possible by the biologists onboard, therefore identification was\n" + //
            "dependent on their expertise. When positive identification was not\n" + //
            "possible, samples were sometimes archived for future identification\n" + //
            "by systematist experts. Data were compiled by the NWFSC, Fishery\n" + //
            "Resource Analysis &amp; Monitoring Division\n" + //
            "\n" + //
            "Purpose - Examination of the spatial and temporal distributions of\n" + //
            "observations of cold-water/deep-sea corals off the west coast of the\n" + //
            "United States, including waters off the states of Washington, Oregon,\n" + //
            "and California. It is important to note that these records represent\n" + //
            "only presence of corals in the area swept by the trawl gear. Since\n" + //
            "bottom trawls used during these surveys are not designed to sample\n" + //
            "epibenthic invertebrates, absence of corals in the catch does not\n" + //
            "necessary mean they do not occupy the area swept by the trawl gear.\n" + //
            "\n" + //
            "Data Credits - NOAA Fisheries, Alaska Fisheries Science Center,\n" + //
            "Resource Assessment &amp; Conservation Engineering Division (RACE) NOAA\n" + //
            "Fisheries, Northwest Fisheries Science Center, Fishery Resource\n" + //
            "Analysis &amp; Monitoring Division (FRAM)\n" + //
            "\n" + //
            "Contact: Curt Whitmire, NOAA NWFSC, Curt.Whitmire@noaa.gov\n" + //
            "</att>\n" + //
            "        <att name=\"institution\">NOAA NWFSC</att>\n" + //
            "        <att name=\"infoUrl\">http://nwioos.coas.oregonstate.edu:8080/dods/drds/Coral%201980-2005.info</att>\n"
            + //
            "        <att name=\"keywords\">\n" + //
            "Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Coastal Habitat,\n" + //
            "Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Marine Habitat,\n" + //
            "Earth Science &gt; Biological Classification &gt; Animals/Invertebrates &gt; Cnidarians &gt; Anthozoans/Hexacorals &gt; Hard Or Stony Corals,\n"
            + //
            "1980-2005, abbreviation, atmosphere, beginning, coast, code, collected, coral, data, depth, family, genus, height, identifier, institution, noaa, nwfsc, off, order, scientific, species, station, survey, taxa, taxonomic, taxonomy, time, west, west coast, year</att>\n"
            + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"cdm_data_type\">Point</att>\n" + //
            "        <att name=\"subsetVariables\">longitude, latitude, depth, time, institution, institution_id, species_code, taxa_scientific, taxonomic_order, order_abbreviation, taxonomic_family, family_abbreviation, taxonomic_genus</att>\n"
            + //
            "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "    </addAttributes>\n" + //
            "    <!-- actual_range info is from EDDTable.getEmpiricalMinMax 2007-12-03; found nRows=2598 -->\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>LONGITUDE_DD</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"doubleList\">-125.99 -117.2767</att>\n" + //
            "            <att name=\"Description\" />\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>LATITUDE_DD</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"doubleList\">32.5708 48.9691</att>\n" + //
            "            <att name=\"Description\" />\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>DEPTH_METERS</sourceName>\n" + //
            "        <destinationName>depth</destinationName>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"doubleList\">-11 -1543</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">1500</att>\n" + //
            "            <att name=\"Description\" />\n" + //
            "            <att name=\"long_name\">Depth</att>\n" + //
            "            <att name=\"scale_factor\" type=\"double\">-1</att>\n" + //
            "            <att name=\"units\">m</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>YEAR_SURVEY</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"long_name\">Time (Beginning of Survey Year)</att>\n" + //
            "            <att name=\"units\">years since 0000-01-01</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>INSTITUTION</sourceName>\n" + //
            "        <destinationName>institution</destinationName>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Institution</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>INSTITUTION_ID</sourceName>\n" + //
            "        <destinationName>institution_id</destinationName>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"doubleList\">38807.0 2.00503017472E11</att>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Institution ID</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>SPECIES_CODE</sourceName>\n" + //
            "        <destinationName>species_code</destinationName>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"doubleList\">41000.0 144115.0</att>\n" + //
            "            <att name=\"ioos_category\">Taxonomy</att>\n" + //
            "            <att name=\"long_name\">Species Code</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>TAXA_SCIENTIFIC</sourceName>\n" + //
            "        <destinationName>taxa_scientific</destinationName>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Taxonomy</att>\n" + //
            "            <att name=\"long_name\">Taxa Scientific</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>TAXONOMIC_ORDER</sourceName>\n" + //
            "        <destinationName>taxonomic_order</destinationName>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"Description\" />\n" + //
            "            <att name=\"ioos_category\">Taxonomy</att>\n" + //
            "            <att name=\"long_name\">Taxonomic Order</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>ORDER_ABBREVIATION</sourceName>\n" + //
            "        <destinationName>order_abbreviation</destinationName>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"Description\" />\n" + //
            "            <att name=\"ioos_category\">Taxonomy</att>\n" + //
            "            <att name=\"long_name\">Order Abbreviation</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>TAXONOMIC_FAMILY</sourceName>\n" + //
            "        <destinationName>taxonomic_family</destinationName>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"Description\" />\n" + //
            "            <att name=\"ioos_category\">Taxonomy</att>\n" + //
            "            <att name=\"long_name\">Taxonomic Family</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>FAMILY_ABBREVIATION</sourceName>\n" + //
            "        <destinationName>family_abbreviation</destinationName>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"Description\" />\n" + //
            "            <att name=\"ioos_category\">Taxonomy</att>\n" + //
            "            <att name=\"long_name\">Family Abbreviation</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>TAXONOMIC_GENUS</sourceName>\n" + //
            "        <destinationName>taxonomic_genus</destinationName>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Taxonomy</att>\n" + //
            "            <att name=\"long_name\">Taxonomic Genus</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD geterdCinpKfmT() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"erdCinpKfmT\">\n" + //
        "    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" + //
        "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/points/KFMTemperature/").toURI()).toString()
        + "</fileDir>\n" + //
        "    <recursive>false</recursive>\n" + //
        "    <fileNameRegex>KFMTemperature_.*\\.nc(|.gz)</fileNameRegex>\n" + //
        "    <metadataFrom>last</metadataFrom>\n" + //
        "    <preExtractRegex>^KFMTemperature_</preExtractRegex>\n" + //
        "    <postExtractRegex>\\.nc$</postExtractRegex>\n" + //
        "    <extractRegex>.*</extractRegex>\n" + //
        "    <columnNameForExtract>station</columnNameForExtract>\n" + //
        "    <sortedColumnSourceName>TIME</sortedColumnSourceName>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"cdm_data_type\">TimeSeries</att>\n" + //
        "        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" + //
        "        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
        "        <att name=\"title\">Channel Islands, Kelp Forest Monitoring, Sea Temperature, 1993-2007</att>\n" + //
        "        <att name=\"contributor_email\">David_Kushner@nps.gov</att>\n" + //
        "        <att name=\"creator_email\">erd.data@noaa.gov</att>\n" + //
        "        <att name=\"creator_type\">institution</att>\n" + //
        "        <att name=\"id\" />\n" + //
        "        <att name=\"infoUrl\">https://www.nps.gov/chis/naturescience/index.htm</att>\n" + //
        "        <att name=\"institution\">CINP</att>\n" + //
        "        <att name=\"keywords\">\n" + //
        "Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Coastal Habitat,\n" + //
        "Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Marine Habitat,\n" + //
        "Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Water Temperature,\n" + //
        "aquatic, atmosphere, biosphere, channel, cinp, coastal, depth, ecosystems, forest, habitat, height, identifier, islands, kelp, marine, monitoring, ocean, oceans, sea, sea_water_temperature, seawater, station, temperature, time, water</att>\n"
        + //
        "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
        "        <att name=\"sourceUrl\">(local files)</att>\n" + //
        "        <att name=\"summary\">This dataset from the Channel Islands National Park's Kelp Forest Monitoring Program has subtidal temperature data taken at permanent monitoring sites.  Since 1993, remote temperature loggers manufactured by Onset Computer Corporation were deployed at each site approximately 10-20 cm from the bottom in a underwater housing.  Since 1993, three models of temperature loggers (HoboTemp (tm), StowAway (R) and Tidbit(R)) were used to collect temperature data every 1-5 hours depending on the model used.</att>\n"
        + //
        "    </addAttributes>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>station</sourceName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"long_name\">Station</att>\n" + //
        "            <att name=\"cf_role\">timeseries_id</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>LON</sourceName>\n" + //
        "        <destinationName>longitude</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">-120.4</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">-118.4</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>LAT</sourceName>\n" + //
        "        <destinationName>latitude</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">32.5</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">34.5</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>DEPTH</sourceName>\n" + //
        "        <destinationName>depth</destinationName>\n" + //
        "        <dataType>double</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">20</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>TIME</sourceName>\n" + //
        "        <destinationName>time</destinationName>\n" + //
        "        <dataType>double</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">7.463865E8</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">1.1843511E9</att>\n" + //
        "            <att name=\"units\">sec since 1970-01-01T00:00:00Z</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>Temperature</sourceName>\n" + //
        "        <destinationName>temperature</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">32</att>\n" + //
        "            <att name=\"ioos_category\">Temperature</att>\n" + //
        "            <att name=\"long_name\">Sea Water Temperature</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>");
  }

  public static EDD getepaseamapTimeSeriesProfiles() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"epaseamapTimeSeriesProfiles\">\n" + //
            "    <!--nDimensions>5</nDimensions-->\n" + //
            "\n" + //
            "    <!-- HERE  How to say never need to reload? Use 1000000000 -->\n" + //
            "    <reloadEveryNMinutes>1000000000</reloadEveryNMinutes>\n" + //
            "    <fileDir>"
            + Path.of(EDDTestDataset.class.getResource("/data/epaseamapTimeSeriesProfiles/").toURI()).toString()
            + "</fileDir>\n" + //
            "    <recursive>false</recursive>\n" + //
            "    <!-- fileNameRegex>epa\\+seamap_.*\\.nc</fileNameRegex -->\n" + //
            "    <fileNameRegex>erddap_test_.*\\.nc</fileNameRegex>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <preExtractRegex>^erddap_test_</preExtractRegex>\n" + //
            "    <postExtractRegex>\\.nc$</postExtractRegex>\n" + //
            "    <extractRegex>.*</extractRegex>\n" + //
            "    <columnNameForExtract>station_name</columnNameForExtract>\n" + //
            "<!-- END station id is already in each the file as data -->\n" + //
            "<!-- HERE station id is already in each the file as data -->\n" + //
            "     <sortedColumnSourceName>time</sortedColumnSourceName>\n" + //
            "     <addAttributes>\n" + //
            "    <!-- HERE\n" + //
            "         <att name=\"Conventions\">CF-1.5, Unidata Dataset Discovery \n" + //
            "v1.0</att>\n" + //
            "         <att name=\"Metadata_Conventions\">null</att>\n" + //
            "Discovery v1.0</att>\n" + //
            "    -->\n" + //
            "        <att name=\"CF:featureType\">null</att>\n" + //
            "        <att name=\"station_name\"></att>\n" + //
            "        <att name=\"infoUrl\">http://www.epa.gov/gmpo/seamap/about.html</att>\n" + //
            "        <att name=\"institution\">EPA SEAMAP GOM</att>\n" + //
            "        <att name=\"keywords\">\n" + //
            "Oceans &gt; Ocean Chemistry &gt; Ammonia,\n" + //
            "Oceans &gt; Ocean Chemistry &gt; Nitrogen,\n" + //
            "Oceans &gt; Ocean Chemistry &gt; Phosphate,\n" + //
            "Oceans &gt; Ocean Chemistry &gt; Chlorophyll,\n" + //
            "Oceans &gt; Ocean Temperature &gt; Water Temperature,\n" + //
            "Oceans &gt; Salinity/Density &gt; Salinity\n" + //
            "</att>\n" + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att \n" + //
            "name=\"sourceUrl\">http://testbedapps.sura.org/thredds/catalog/shelf_hypoxia_scan/Observations/CTD/netcdf/catalog.html</att>\n"
            + //
            "        <att name=\"summary\">EPA Gulf of Mexico Program SEAMAP fixed \n" + //
            "station time series profiles with Hypoxia related observations at \n" + //
            "multiple depths.\n" + //
            "Temperature, Salinity, Dissovled Nitrogen, Chlorophyll, Ammonium and \n" + //
            "Phosphate for the period 2006 through 2008. Location is in the Gulf of \n" + //
            "Mexico.\n" + //
            "</att>\n" + //
            "        <att name=\"title\">EPA SeaMap water station profiles in Gulf of Mexico</att>\n" + //
            "        <att name=\"cdm_data_type\">TimeSeriesProfile</att>\n" + //
            "        <att name=\"cdm_timeseries_variables\">station_name, station, longitude, latitude</att>\n" + //
            "        <att name=\"cdm_profile_variables\">time</att>\n" + //
            "        <att name=\"subsetVariables\">station_name, station, longitude, latitude</att>\n" + //
            "    </addAttributes>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>station_name</sourceName> \n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>station</sourceName>\n" + //
            "        <destinationName>station</destinationName>\n" + //
            "        <dataType>int</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"cf_role\">timeseries_id</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>lat</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <addAttributes>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>lon</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <addAttributes>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>time</sourceName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"standard_name\">time</att>\n" + //
            "            <att name=\"cf_role\">profile_id</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>z</sourceName>\n" + //
            "        <destinationName>depth</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <addAttributes>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>temp</sourceName>\n" + //
            "        <destinationName>WaterTemperature</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">Sea Water Temperature</att>\n" + //
            "            <att name=\"standard_name\">sea_water_temperature</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>salt</sourceName>\n" + //
            "        <destinationName>salinity</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <addAttributes>\n" + //
            "             <att name=\"ioos_category\">Salinity</att>\n" + //
            "             <att name=\"long_name\">Salinity</att>\n" + //
            "             <att name=\"standard_name\">sea_water_salinity</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>chl</sourceName>\n" + //
            "        <destinationName>chlorophyll</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Ocean Color</att>\n" + //
            "            <att name=\"long_name\">Chlorophyll</att>\n" + //
            "            <att name=\"standard_name\">concentration_of_chlorophyll_in_sea_water</att>\n" + //
            "            <att name=\"units\">mg_m-3</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>NO23</sourceName>\n" + //
            "        <destinationName>Nitrogen</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n" + //
            "            <att name=\"long_name\">Nitrogen</att>\n" + //
            "            <att name=\"standard_name\">mass_concentration_of_inorganic_nitrogen_in_sea_water</att>\n" + //
            "            <att name=\"units\">percent</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>PO4</sourceName>\n" + //
            "        <destinationName>Phosphate</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n" + //
            "            <att name=\"long_name\">Phosphate</att>\n" + //
            "            <att name=\"standard_name\">mass_concentration_of_phosphate_in_sea_water</att>\n" + //
            "            <att name=\"units\">percent</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>NH4</sourceName>\n" + //
            "        <destinationName>Ammonium</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n" + //
            "            <att name=\"long_name\">Ammonium</att>\n" + //
            "            <att name=\"units\">percent</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD gettestEDDTableCopyFiles() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"testEDDTableCopyFiles\" active=\"true\">\n" + //
            "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" + //
            "    <updateEveryNMillis>10000</updateEveryNMillis>\n" + //
            "    <fileDir>"
            + Path.of(EDDTestDataset.class.getResource("/data/points/testEDDTableCopyFiles/").toURI()).toString()
            + "</fileDir>\n" + //
            "    <fileNameRegex>19\\d7\\.nc</fileNameRegex>\n" + //
            "    <recursive>true</recursive>\n" + //
            "    <pathRegex>.*</pathRegex>\n" + //
            "    <cacheFromUrl>https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/</cacheFromUrl>\n" + //
            "    <cachePartialPathRegex>.*/(|[3|4]/)</cachePartialPathRegex>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <standardizeWhat>0</standardizeWhat>\n" + //
            "    <preExtractRegex></preExtractRegex>\n" + //
            "    <postExtractRegex></postExtractRegex>\n" + //
            "    <extractRegex></extractRegex>\n" + //
            "    <columnNameForExtract></columnNameForExtract>\n" + //
            "    <sortedColumnSourceName>time</sortedColumnSourceName>\n" + //
            "    <sortFilesBySourceNames>region year</sortFilesBySourceNames>\n" + //
            "    <fileTableInMemory>false</fileTableInMemory>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "        <att name=\"id\">1977</att>\n" + //
            "        <att name=\"observationDimension\">row</att>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" + //
            "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n" + //
            "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n" + //
            "    -->\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"cdm_data_type\">Other</att>\n" + //
            "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
            "        <att name=\"infoUrl\">???</att>\n" + //
            "        <att name=\"institution\">???</att>\n" + //
            "        <att name=\"keywords\">area, block, category, caught, comments, data, description, group, imported, local, market, market_category, month, nominal, nominal_species, pounds, region, region_caught, row, source, species, species_group, taxonomy, time, year</att>\n"
            + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"sourceUrl\">(local files)</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"subsetVariables\">region, year, area, imported, region_caught, comments</att>\n" + //
            "        <att name=\"summary\">Data from a local source.</att>\n" + //
            "        <att name=\"title\">Data from a local source.</att>\n" + //
            "    </addAttributes>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>row</sourceName>\n" + //
            "        <destinationName>row</destinationName>\n" + //
            "        <dataType>byte</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "            <att name=\"long_name\">Row</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>region</sourceName>\n" + //
            "        <destinationName>region</destinationName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"int\">-9999</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Region</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>year</sourceName>\n" + //
            "        <destinationName>year</destinationName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"int\">-9999</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"long_name\">Year</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>market_category</sourceName>\n" + //
            "        <destinationName>market_category</destinationName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"int\">-9999</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "            <att name=\"long_name\">Market Category</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>month</sourceName>\n" + //
            "        <destinationName>month</destinationName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"int\">-9999</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"long_name\">Month</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>block</sourceName>\n" + //
            "        <destinationName>block</destinationName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"int\">-9999</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "            <att name=\"long_name\">Block</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>pounds</sourceName>\n" + //
            "        <destinationName>pounds</destinationName>\n" + //
            "        <dataType>int</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"int\">-9999</att>\n" + //
            "            <att name=\"units\">pounds</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "            <att name=\"long_name\">Pounds</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>area</sourceName>\n" + //
            "        <destinationName>area</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "            <att name=\"long_name\">Area</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>imported</sourceName>\n" + //
            "        <destinationName>imported</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "            <att name=\"long_name\">Imported</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>region_caught</sourceName>\n" + //
            "        <destinationName>region_caught</destinationName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"int\">-9999</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Region Caught</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>time</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"double\">-1.0E30</att>\n" + //
            "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"long_name\">Time</att>\n" + //
            "            <att name=\"standard_name\">time</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>description</sourceName>\n" + //
            "        <destinationName>description</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "            <att name=\"long_name\">Description</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>nominal_species</sourceName>\n" + //
            "        <destinationName>nominal_species</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Taxonomy</att>\n" + //
            "            <att name=\"long_name\">Nominal Species</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>species_group</sourceName>\n" + //
            "        <destinationName>species_group</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Taxonomy</att>\n" + //
            "            <att name=\"long_name\">Species Group</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>comments</sourceName>\n" + //
            "        <destinationName>comments</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "            <att name=\"long_name\">Comments</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD getndbcSosWaves() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDTableFromSOS\" datasetID=\"ndbcSosWaves\" active=\"false\">\n" + //
            "    <sourceUrl>https://sdf.ndbc.noaa.gov/sos/server.php</sourceUrl>\n" + //
            "    <sosVersion>1.0.0</sosVersion>\n" + //
            "    <sosServerType>IOOS_NDBC</sosServerType>\n" + //
            "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" + //
            "    <observationOfferingIdRegex>.+</observationOfferingIdRegex>\n" + //
            "    <requestObservedPropertiesSeparately>true</requestObservedPropertiesSeparately>\n" + //
            "    <bboxOffering>urn:ioos:network:noaa.nws.ndbc:all</bboxOffering>\n" + //
            "    <bboxParameter>featureofinterest=BBOX:</bboxParameter>\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"cdm_data_type\">TimeSeries</att>\n" + //
            "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude, sensor_id</att>\n" + //
            "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"infoUrl\">https://sdf.ndbc.noaa.gov/sos/</att>\n" + //
            "        <att name=\"institution\">NOAA NDBC</att>\n" + //
            "        <att name=\"keywords\">\n" + //
            "Earth Science &gt; Atmosphere &gt; Altitude &gt; Station Height,\n" + //
            "Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Water Temperature,\n" + //
            "Earth Science &gt; Oceans &gt; Ocean Waves &gt; Significant Wave Height,\n" + //
            "Earth Science &gt; Oceans &gt; Ocean Waves &gt; Swells,\n" + //
            "Earth Science &gt; Oceans &gt; Ocean Waves &gt; Wave Frequency,\n" + //
            "Earth Science &gt; Oceans &gt; Ocean Waves &gt; Wave Period,\n" + //
            "Earth Science &gt; Oceans &gt; Ocean Waves &gt; Wave Speed/Direction,\n" + //
            "Earth Science &gt; Oceans &gt; Ocean Waves &gt; Wind Waves,\n" + //
            "altitude, atmosphere, bandwidths, calculation, center, coordinate, direction, energy, frequencies, height, identifier, mean, mean_wave_direction, method, ndbc, noaa, number, ocean, oceans, peak, period, polar, principal, rate, sampling, sea, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sea_surface_wave_mean_period, sea_surface_wave_peak_period, sea_surface_wave_significant_height, sea_surface_wave_to_direction, sea_surface_wind_wave_period, sea_surface_wind_wave_significant_height, sea_surface_wind_wave_to_direction, sea_water_temperature, seawater, sensor, significant, sos, spectral, speed, station, surface, surface waves, swell, swells, temperature, time, water, wave, waves, wind</att>\n"
            + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"summary\">The NOAA NDBC SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have waves data.\n"
            + //
            "\n" + //
            "Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.</att>\n"
            + //
            "        <att name=\"title\">NOAA NDBC SOS, waves</att>\n" + //
            "    </addAttributes>\n" + //
            "    <longitudeSourceName>longitude (degree)</longitudeSourceName>\n" + //
            "    <latitudeSourceName>latitude (degree)</latitudeSourceName>\n" + //
            "    <altitudeSourceName>depth (m)</altitudeSourceName>\n" + //
            "    <altitudeMetersPerSourceUnit>-1</altitudeMetersPerSourceUnit>\n" + //
            "    <timeSourceName>date_time</timeSourceName>\n" + //
            "    <timeSourceFormat>yyyy-MM-dd'T'HH:mm:ssZ</timeSourceFormat>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sensor_id</sourceName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"comment\">Always check the quality_flags before using this data.</att>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Sensor ID</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sea_surface_wave_significant_height (m)</sourceName>\n" + //
            "        <destinationName>sea_surface_wave_significant_height</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Sea Surface Wave Significant Height</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"standard_name\">sea_surface_wave_significant_height</att>\n" + //
            "            <att name=\"units\">m</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sea_surface_wave_peak_period (s)</sourceName>\n" + //
            "        <destinationName>sea_surface_wave_peak_period</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Sea Surface Wave Peak Period</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"standard_name\">sea_surface_wave_peak_period</att>\n" + //
            "            <att name=\"units\">s</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sea_surface_wave_mean_period (s)</sourceName>\n" + //
            "        <destinationName>sea_surface_wave_mean_period</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Sea Surface Wave Mean Period</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"standard_name\">sea_surface_wave_mean_period</att>\n" + //
            "            <att name=\"units\">s</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sea_surface_swell_wave_significant_height (m)</sourceName>\n" + //
            "        <destinationName>sea_surface_swell_wave_significant_height</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Sea Surface Swell Wave Significant Height</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"standard_name\">sea_surface_swell_wave_significant_height</att>\n" + //
            "            <att name=\"units\">m</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sea_surface_swell_wave_period (s)</sourceName>\n" + //
            "        <destinationName>sea_surface_swell_wave_period</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">20.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Sea Surface Swell Wave Period</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"standard_name\">sea_surface_swell_wave_period</att>\n" + //
            "            <att name=\"units\">s</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sea_surface_wind_wave_significant_height (m)</sourceName>\n" + //
            "        <destinationName>sea_surface_wind_wave_significant_height</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Sea Surface Wind Wave Significant Height</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"standard_name\">sea_surface_wind_wave_significant_height</att>\n" + //
            "            <att name=\"units\">m</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sea_surface_wind_wave_period (s)</sourceName>\n" + //
            "        <destinationName>sea_surface_wind_wave_period</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Sea Surface Wind Wave Period</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"standard_name\">sea_surface_wind_wave_period</att>\n" + //
            "            <att name=\"units\">s</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sea_water_temperature (c)</sourceName>\n" + //
            "        <destinationName>sea_water_temperature</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">Sea Water Temperature</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"standard_name\">sea_water_temperature</att>\n" + //
            "            <att name=\"units\">degree_C</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sea_surface_wave_to_direction (degree)</sourceName>\n" + //
            "        <destinationName>sea_surface_wave_to_direction</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Sea Surface Wave To Direction</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"standard_name\">sea_surface_wave_to_direction</att>\n" + //
            "            <att name=\"units\">degrees_true</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sea_surface_swell_wave_to_direction (degree)</sourceName>\n" + //
            "        <destinationName>sea_surface_swell_wave_to_direction</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Sea Surface Swell Wave To Direction</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"standard_name\">sea_surface_swell_wave_to_direction</att>\n" + //
            "            <att name=\"units\">degrees_true</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sea_surface_wind_wave_to_direction (degree)</sourceName>\n" + //
            "        <destinationName>sea_surface_wind_wave_to_direction</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Sea Surface Wind Wave To Direction</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"standard_name\">sea_surface_wind_wave_to_direction</att>\n" + //
            "            <att name=\"units\">degrees_true</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>number_of_frequencies (count)</sourceName>\n" + //
            "        <destinationName>number_of_frequencies</destinationName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Number Of Frequencies</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"units\">count</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>center_frequencies (Hz)</sourceName>\n" + //
            "        <destinationName>center_frequencies</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Center Frequencies</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"units\">Hz</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>bandwidths (Hz)</sourceName>\n" + //
            "        <destinationName>bandwidths</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Bandwidths</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"units\">Hz</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>spectral_energy (m**2/Hz)</sourceName>\n" + //
            "        <destinationName>spectral_energy</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Spectral Energy</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"units\">m^2/Hz</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>mean_wave_direction (degree)</sourceName>\n" + //
            "        <destinationName>mean_wave_direction</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Mean Wave Direction</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"standard_name\">mean_wave_direction</att>\n" + //
            "            <att name=\"units\">degrees_true</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>principal_wave_direction (degree)</sourceName>\n" + //
            "        <destinationName>principal_wave_direction</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Principal Wave Direction</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"units\">degrees_true</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>polar_coordinate_r1 (1)</sourceName>\n" + //
            "        <destinationName>polar_coordinate_r1</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Polar Coordinate R1</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"units\">1</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>polar_coordinate_r2 (1)</sourceName>\n" + //
            "        <destinationName>polar_coordinate_r2</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Polar Coordinate R2</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"units\">1</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>calculation_method</sourceName>\n" + //
            "        <destinationName>calculation_method</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Calculation Method</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>sampling_rate (Hz)</sourceName>\n" + //
            "        <destinationName>sampling_rate</destinationName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Surface Waves</att>\n" + //
            "            <att name=\"long_name\">Sampling Rate</att>\n" + //
            "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" + //
            "            <att name=\"units\">Hz</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD gettestTablePseudoSourceNames() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"testTablePseudoSourceNames\">\n" + //
            "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" + //
            "    <updateEveryNMillis>1</updateEveryNMillis>\n" + //
            "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/miniNdbc/").toURI()).toString()
            + "</fileDir>\n" + //
            "    <recursive>false</recursive>\n" + //
            "    <fileNameRegex>NDBC_.*\\.nc</fileNameRegex>\n" + //
            "    <!--fileTableInMemory>true</fileTableInMemory-->\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <sortedColumnSourceName>TIME</sortedColumnSourceName>\n" + //
            "    <sortFilesBySourceNames>\"***fileName,NDBC_(.*)_met\\\\.nc,1\", TIME</sortFilesBySourceNames>\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"cdm_data_type\">TimeSeries</att>\n" + //
            "        <att name=\"cdm_timeseries_variables\">station, parentDir, longitude, latitude</att>\n" + //
            "        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" + //
            "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"date_created\">null</att>\n" + //
            "        <att name=\"date_issued\">null</att>\n" + //
            "        <att name=\"id\">null</att>\n" + //
            "        <att name=\"infoUrl\">https://www.ndbc.noaa.gov/</att>\n" + //
            "        <att name=\"institution\">NOAA NDBC, CoastWatch WCN</att>\n" + //
            "        <att name=\"keywords\">lots of keywords</att>\n" + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"sourceUrl\">https://www.ndbc.noaa.gov/</att>\n" + //
            "        <att name=\"summary\">A great summary.</att>\n" + //
            "        <att name=\"title\">A Great Title</att>\n" + //
            "    </addAttributes>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>***fileName,NDBC_(.*)_met\\.nc,1</sourceName>\n" + //
            "        <destinationName>station</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Station Name</att>\n" + //
            "            <att name=\"cf_role\">timeseries_id</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>***pathName,.*/([a-zA-Z]*)/NDBC_.*_met\\.nc,1</sourceName>\n" + //
            "        <destinationName>parentDir</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Parent Directory</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>LON</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <addAttributes>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>LAT</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <addAttributes>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>TIME</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"point_spacing\">null</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>=7</sourceName>\n" + //
            "        <destinationName>luckySeven</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"comment\">fixed value</att>\n" + //
            "            <att name=\"ioos_category\">Other</att>\n" + //
            "            <att name=\"units\">m</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:geospatial_lat_min</sourceName>\n" + //
            "        <destinationName>geoLatMin</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"units\">degrees_north</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:zztop</sourceName>\n" + //
            "        <destinationName>globalZztop</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Other</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>variable:LAT:actual_range</sourceName>\n" + //
            "        <destinationName>latActualRangeMin</destinationName>\n" + //
            "        <dataType>double</dataType> <!-- float in file -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>variable:DEPTH:positive</sourceName>\n" + //
            "        <destinationName>depthPositive</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>variable:DEPTH:zztop</sourceName>\n" + //
            "        <destinationName>depthZztop</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>WD</sourceName>\n" + //
            "        <destinationName>wd</destinationName>\n" + //
            "        <dataType>short</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">360</att>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">32767</att>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <!--This needn't be in the dataset!   \n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>WSPD</sourceName>\n" + //
            "        <destinationName>wspd</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">15</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable-->\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>variable:WSPD:actual_range</sourceName>\n" + //
            "        <destinationName>wspdRange</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD getfsuNoaaShipWTEP() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDTableFromThreddsFiles\" datasetID=\"fsuNoaaShipWTEP\" active=\"true\">\n" + //
            "    <defaultDataQuery>&amp;time&gt;=max(time)-7days&amp;time&lt;=max(time)&amp;flag=~\"ZZZ.*\"</defaultDataQuery>\n"
            + //
            "    <defaultGraphQuery>&amp;time&gt;=max(time)-7days&amp;time&lt;=max(time)&amp;flag=~\"ZZZ.*\"&amp;.marker=10|5</defaultGraphQuery>\n"
            + //
            "    <specialMode>SAMOS</specialMode>\n" + //
            "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" + //
            "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/fsuNoaaShipWTEP/").toURI()).toString()
            + "</fileDir>\n" + //
            "    <recursive>true</recursive>\n" + //
            "    <fileNameRegex>WTEP_.*\\.nc</fileNameRegex>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <preExtractRegex></preExtractRegex>\n" + //
            "    <postExtractRegex></postExtractRegex>\n" + //
            "    <extractRegex></extractRegex>\n" + //
            "    <columnNameForExtract></columnNameForExtract>\n" + //
            "    <sortedColumnSourceName>time</sortedColumnSourceName>\n" + //
            "    <sortFilesBySourceNames>time</sortFilesBySourceNames>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "        <att name=\"contact_email\">samos@coaps.fsu.edu</att>\n" + //
            "        <att name=\"contact_info\">Center for Ocean-Atmospheric Prediction Studies, The Florida State University, Tallahassee, FL, 32306-2840,\n"
            + //
            " USA</att>\n" + //
            "        <att name=\"Cruise_id\">Cruise_id undefined for now</att>\n" + //
            "        <att name=\"Data_modification_date\">16- 2-2012  0: 2: 4 UTC</att>\n" + //
            "        <att name=\"data_provider\">Timothy Salisbury</att>\n" + //
            "        <att name=\"elev\" type=\"short\">0</att>\n" + //
            "        <att name=\"end_date_time\">2012/02/15 - -  23:59  UTC</att>\n" + //
            "        <att name=\"EXPOCODE\">EXPOCODE undefined for now</att>\n" + //
            "        <att name=\"facility\">NOAA</att>\n" + //
            "        <att name=\"fsu_version\">100</att>\n" + //
            "        <att name=\"ID\">WTEP</att>\n" + //
            "        <att name=\"IMO\">009270335</att>\n" + //
            "        <att name=\"Metadata_modification_date\">15- 2-2012 19: 2: 4 EDT</att>\n" + //
            "        <att name=\"platform\">SCS</att>\n" + //
            "        <att name=\"platform_version\">4.0</att>\n" + //
            "        <att name=\"receipt_order\">02</att>\n" + //
            "        <att name=\"site\">OSCAR DYSON</att>\n" + //
            "        <att name=\"start_date_time\">2012/02/15 - -  00:01  UTC</att>\n" + //
            "        <att name=\"title\">OSCAR DYSON Meteorological Data</att>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" + //
            "        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" + //
            "        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" + //
            "    -->\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"end_date_time\">null</att>\n" + //
            "        <att name=\"start_date_time\">null</att>\n" + //
            "\n" + //
            "        <att name=\"cdm_data_type\">Trajectory</att>\n" + //
            "        <att name=\"cdm_trajectory_variables\">ID, site</att>\n" + //
            "        <att name=\"subsetVariables\">ID, site, IMO, cruise_id, expocode, facility, platform, platform_version</att>\n"
            + //
            "\n" + //
            "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
            "        <att name=\"creator_email\">samos@coaps.fsu.edu</att>\n" + //
            "        <att name=\"creator_name\">Shipboard Automated Meteorological and Oceanographic System (SAMOS)</att>\n"
            + //
            "        <att name=\"creator_type\">group</att>\n" + //
            "        <att name=\"creator_url\">https://samos.coaps.fsu.edu/html/</att>\n" + //
            "        <att name=\"infoUrl\">https://samos.coaps.fsu.edu/html/</att>\n" + //
            "        <att name=\"institution\">FSU</att>\n" + //
            "        <att name=\"keywords\">\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Pressure &gt; Atmospheric Pressure Measurements,\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Pressure &gt; Sea Level Pressure,\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Pressure &gt; Static Pressure,\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Temperature &gt; Air Temperature,\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Temperature &gt; Surface Air Temperature,\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Water Vapor &gt; Humidity,\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Surface Winds,\n" + //
            "Earth Science &gt; Oceans &gt; Salinity/Density &gt; Conductivity,\n" + //
            "Earth Science &gt; Oceans &gt; Salinity/Density &gt; Salinity,\n" + //
            "air, air_pressure, air_temperature, atmosphere, atmospheric, calender, conductivity, control, course, data, date, day, density, direction, dyson, earth, electrical, file, flags, from, fsu, ground, heading, history, humidity, information, level, measurements, meteorological, meteorology, oceans, oscar, over, platform, pressure, quality, relative, relative_humidity, salinity, sea, sea_water_electrical_conductivity, sea_water_practical_salinity, seawater, speed, static, surface, temperature, time, vapor, water, wind, wind_from_direction, wind_speed, winds</att>\n"
            + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"sourceUrl\">https://tds.coaps.fsu.edu/thredds/catalog/samos/data/research/WTEP/catalog.xml</att>\n"
            + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"summary\">NOAA Ship Oscar Dyson Underway Meteorological Data (delayed ~10 days for quality control) are from the Shipboard Automated Meteorological and Oceanographic System (SAMOS) program.\n"
            + //
            "\n" + //
            "IMPORTANT: ALWAYS USE THE QUALITY FLAG DATA! Each data variable's metadata includes a qcindex attribute which indicates a character number in the flag data.  ALWAYS check the flag data for each row of data to see which data is good (flag='Z') and which data isn't.  For example, to extract just data where time (qcindex=1), latitude (qcindex=2), longitude (qcindex=3), and airTemperature (qcindex=12) are 'good' data, include this constraint in your ERDDAP query:\n"
            + //
            "  flag=~\"ZZZ........Z.*\"\n" + //
            "in your query.\n" + //
            "'=~' indicates this is a regular expression constraint.\n" + //
            "The 'Z's are literal characters.  In this dataset, 'Z' indicates 'good' data.\n" + //
            "The '.'s say to match any character.\n" + //
            "The '*' says to match the previous character 0 or more times.\n" + //
            "(Don't include backslashes in your query.)\n" + //
            "See the tutorial for regular expressions at\n" + //
            "http://www.vogella.com/tutorials/JavaRegularExpressions/article.html</att>\n" + //
            "        <att name=\"title\">NOAA Ship Oscar Dyson Underway Meteorological Data, Quality Controlled</att>\n"
            + //
            "    </addAttributes>\n" + //
            "\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:ID</sourceName>\n" + //
            "        <destinationName>ID</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Call Sign</att>\n" + //
            "            <att name=\"cf_role\">trajectory_id</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:site</sourceName>\n" + //
            "        <destinationName>site</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Ship Name</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>    \n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:IMO</sourceName>\n" + //
            "        <destinationName>IMO</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:Cruise_id</sourceName>\n" + //
            "        <destinationName>cruise_id</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:EXPOCODE</sourceName>\n" + //
            "        <destinationName>expocode</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:facility</sourceName>\n" + //
            "        <destinationName>facility</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:platform</sourceName>\n" + //
            "        <destinationName>platform</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:platform_version</sourceName>\n" + //
            "        <destinationName>platform_version</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>time</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <dataType>int</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"intList\">16895521 16896959</att>\n" + //
            "            <att name=\"data_interval\" type=\"int\">60</att>\n" + //
            "            <att name=\"long_name\">time</att>\n" + //
            "            <att name=\"observation_type\">calculated</att>\n" + //
            "            <att name=\"original_units\">hhmmss UTC</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">1</att>\n" + //
            "            <att name=\"units\">minutes since 1-1-1980 00:00 UTC</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_FillValue\">null</att>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"standard_name\">time</att>\n" + //
            "            <att name=\"metadata_retrieved_from\">null</att>\n" + //
            "            <att name=\"original_units\">null</att>\n" + //
            "            <att name=\"units\">minutes since 1980-01-01T00:00:00Z</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>lat</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">54.44 54.79</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">latitude</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"original_units\">degrees (+N)</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">2</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"units\">degrees (+N)</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"metadata_retrieved_from\">null</att>\n" + //
            "            <att name=\"units\">degrees_north</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>lon</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">196.77 197.74</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">longitude</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"original_units\">degrees (-W/+E)</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">3</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"units\">degrees (+E)</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"metadata_retrieved_from\">null</att>\n" + //
            "            <att name=\"units\">degrees_east</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>P</sourceName>\n" + //
            "        <destinationName>airPressure</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">976.18 982.08</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">atmospheric pressure</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"mslp_indicator\">at sensor height</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"original_units\">hectopascal</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">11</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">0.1</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">millibar</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">1050.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">950.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Pressure</att>\n" + //
            "            <att name=\"long_name\">Atmospheric Pressure</att>\n" + //
            "            <att name=\"metadata_retrieved_from\">null</att>\n" + //
            "            <att name=\"standard_name\">air_pressure</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>T</sourceName>\n" + //
            "        <destinationName>airTemperature</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">1.01 4.8</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">air temperature</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"original_units\">celsius</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">12</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">celsius</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">Air Temperature</att>\n" + //
            "            <att name=\"standard_name\">air_temperature</att>\n" + //
            "            <att name=\"metadata_retrieved_from\">null</att>\n" + //
            "            <att name=\"original_units\">null</att>\n" + //
            "            <att name=\"units\">degree_C</att>            \n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>CNDC</sourceName>\n" + //
            "        <destinationName>conductivity</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">2.68 2.86</att>\n" + //
            "            <att name=\"average_center\">unknown</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">conductivity</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"original_units\">siemens meter-1</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">16</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">siemens meter-1</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">4</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Salinity</att>\n" + //
            "            <att name=\"long_name\">Conductivity</att>\n" + //
            "            <att name=\"metadata_retrieved_from\">null</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">16</att>\n" + //
            "            <att name=\"standard_name\">sea_water_electrical_conductivity</att>\n" + //
            "            <att name=\"units\">siemens meter-1</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>RH</sourceName>\n" + //
            "        <destinationName>relativeHumidity</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">66.0 99.0</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">relative humidity</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"original_units\">percent</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">13</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">percent</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Meteorology</att>\n" + //
            "            <att name=\"long_name\">Relative Humidity</att>\n" + //
            "            <att name=\"metadata_retrieved_from\">null</att>\n" + //
            "            <att name=\"standard_name\">relative_humidity</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>SSPS</sourceName>\n" + //
            "        <destinationName>salinity</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">31.08 31.76</att>\n" + //
            "            <att name=\"average_center\">unknown</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">-9999</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_interval\" type=\"int\">60</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">salinity</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">calculated</att>\n" + //
            "            <att name=\"original_units\">PSU</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">15</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">PSU</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Salinity</att>\n" + //
            "            <att name=\"long_name\">Salinity</att>\n" + //
            "            <att name=\"metadata_retrieved_from\">null</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">calculated</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">15</att>\n" + //
            "            <att name=\"long_name\">Sea Water Practical Salinity</att>\n" + //
            "            <att name=\"standard_name\">sea_water_practical_salinity</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>TS</sourceName>\n" + //
            "        <destinationName>seaTemperature</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">0.97 2.51</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">sea temperature</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"original_units\">celsius</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">14</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ts_sensor_category\" type=\"short\">12</att>\n" + //
            "            <att name=\"units\">celsius</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">Sea Water Temperature</att>\n" + //
            "            <att name=\"metadata_retrieved_from\">null</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"original_units\">celsius</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">14</att>\n" + //
            "            <att name=\"standard_name\">sea_water_temperature</att>\n" + //
            "            <att name=\"ts_sensor_category\" type=\"short\">12</att>\n" + //
            "            <att name=\"units\">degree_C</att>            \n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>DIR</sourceName>\n" + //
            "        <destinationName>windDirection</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">95.62 186.79</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_interval\" type=\"int\">-9999</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">earth relative wind direction</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">calculated</att>\n" + //
            "            <att name=\"original_units\">degrees (clockwise from true north)</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">6</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">degrees (clockwise from true north)</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "            <att name=\"long_name\">Earth Relative Wind Direction</att>\n" + //
            "            <att name=\"metadata_retrieved_from\">null</att>\n" + //
            "            <att name=\"standard_name\">wind_from_direction</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>SPD</sourceName>\n" + //
            "        <destinationName>windSpeed</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">2.793192 16.38364</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_interval\" type=\"int\">-9999</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">earth relative wind speed</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">calculated</att>\n" + //
            "            <att name=\"original_units\">knot</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">9</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">meter second-1</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "            <att name=\"long_name\">Earth Relative Wind Speed</att>\n" + //
            "            <att name=\"metadata_retrieved_from\">null</att>\n" + //
            "            <att name=\"standard_name\">wind_speed</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>PL_CRS</sourceName>\n" + //
            "        <destinationName>platformCourse</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">0.61 359.55</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">platform course</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">calculated</att>\n" + //
            "            <att name=\"original_units\">degrees (clockwise towards true north)</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">5</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">degrees (clockwise towards true north)</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "            <att name=\"long_name\">Platform Course</att>\n" + //
            "            <att name=\"metadata_retrieved_from\">null</att>\n" + //
            "            <att name=\"units\">degrees_true</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>PL_HD</sourceName>\n" + //
            "        <destinationName>platformHeading</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">1.42 358.77</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">platform heading</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">calculated</att>\n" + //
            "            <att name=\"original_units\">degrees (clockwise towards true north)</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">4</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">degrees (clockwise towards true north)</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "            <att name=\"long_name\">Platform Heading</att>\n" + //
            "            <att name=\"metadata_retrieved_from\">null</att>\n" + //
            "            <att name=\"units\">degrees_true</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>PL_SPD</sourceName>\n" + //
            "        <destinationName>platformSpeed</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">0.046296 6.6872</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">platform speed over ground</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">calculated</att>\n" + //
            "            <att name=\"original_units\">knot</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">8</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">0.5</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">meter second-1</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Other</att>\n" + //
            "            <att name=\"metadata_retrieved_from\">null</att>\n" + //
            "            <att name=\"long_name\">Platform Speed Over Ground</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>PL_WDIR</sourceName>\n" + //
            "        <destinationName>platformWindDirection</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">0.1 359.43</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_interval\" type=\"int\">-9999</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">platform relative wind direction</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"original_units\">degrees (clockwise from bow)</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">7</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">degrees (clockwise from bow)</att>\n" + //
            "            <att name=\"zero_line_reference\" type=\"float\">-9999.0</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "            <att name=\"metadata_retrieved_from\">null</att>\n" + //
            "            <att name=\"long_name\">Platform Relative Wind Direction</att>\n" + //
            "            <att name=\"standard_name\">wind_from_direction</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>PL_WSPD</sourceName>\n" + //
            "        <destinationName>platformWindSpeed</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">0.020576 17.30956</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_interval\" type=\"int\">-9999</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">platform relative wind speed</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"original_units\">knot</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">10</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">meter second-1</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "            <att name=\"metadata_retrieved_from\">null</att>\n" + //
            "            <att name=\"long_name\">Platform Relative Wind Speed</att>\n" + //
            "            <att name=\"standard_name\">wind_speed</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>flag</sourceName>\n" + //
            "        <destinationName>flag</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"A\">Units added</att>\n" + //
            "            <att name=\"B\">Data out of range</att>\n" + //
            "            <att name=\"C\">Non-sequential time</att>\n" + //
            "            <att name=\"D\">Failed T&gt;=Tw&gt;=Td</att>\n" + //
            "            <att name=\"E\">True wind error</att>\n" + //
            "            <att name=\"F\">Velocity unrealistic</att>\n" + //
            "            <att name=\"G\">Value &gt; 4 s. d. from climatology</att>\n" + //
            "            <att name=\"H\">Discontinuity</att>\n" + //
            "            <att name=\"I\">Interesting feature</att>\n" + //
            "            <att name=\"J\">Erroneous</att>\n" + //
            "            <att name=\"K\">Suspect - visual</att>\n" + //
            "            <att name=\"L\">Ocean platform over land</att>\n" + //
            "            <att name=\"long_name\">quality control flags</att>\n" + //
            "            <att name=\"M\">Instrument malfunction</att>\n" + //
            "            <att name=\"N\">In Port</att>\n" + //
            "            <att name=\"O\">Multiple original units</att>\n" + //
            "            <att name=\"P\">Movement uncertain</att>\n" + //
            "            <att name=\"Q\">Pre-flagged as suspect</att>\n" + //
            "            <att name=\"R\">Interpolated data</att>\n" + //
            "            <att name=\"S\">Spike - visual</att>\n" + //
            "            <att name=\"T\">Time duplicate</att>\n" + //
            "            <att name=\"U\">Suspect - statistial</att>\n" + //
            "            <att name=\"V\">Spike - statistical</att>\n" + //
            "            <att name=\"X\">Step - statistical</att>\n" + //
            "            <att name=\"Y\">Suspect between X-flags</att>\n" + //
            "            <att name=\"Z\">Good data</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Quality</att>\n" + //
            "            <att name=\"long_name\">Quality Control Flags</att>\n" + //
            "            <att name=\"metadata_retrieved_from\">null</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD getfsuNoaaShipWTEPnrt() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDTableFromThreddsFiles\" datasetID=\"fsuNoaaShipWTEPnrt\" active=\"true\">\n" + //
            "    <defaultDataQuery>&amp;time&gt;=max(time)-7days&amp;time&lt;=max(time)&amp;flag=~\"ZZZ.*\"</defaultDataQuery>\n"
            + //
            "    <defaultGraphQuery>&amp;time&gt;=max(time)-7days&amp;time&lt;=max(time)&amp;flag=~\"ZZZ.*\"&amp;.marker=10|5</defaultGraphQuery>\n"
            + //
            "    <specialMode>SAMOS</specialMode>\n" + //
            "    <reloadEveryNMinutes>240</reloadEveryNMinutes>\n" + //
            "    <!-- defaultDataQuery>&amp;time&gt;=now-1year</defaultDataQuery>\n" + //
            "    <defaultGraphQuery>longitude,latitude,seaTemperature&amp;time&gt;=now-1year&amp;.marker=10|5</defaultGraphQuery -->\n"
            + //
            "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/fsuNoaaShipWTEP/").toURI()).toString()
            + "</fileDir>\n" + //
            "    <recursive>true</recursive>\n" + //
            "    <fileNameRegex>WTEP_.*\\.nc</fileNameRegex>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <preExtractRegex></preExtractRegex>\n" + //
            "    <postExtractRegex></postExtractRegex>\n" + //
            "    <extractRegex></extractRegex>\n" + //
            "    <columnNameForExtract></columnNameForExtract>\n" + //
            "    <sortedColumnSourceName>time</sortedColumnSourceName>\n" + //
            "    <sortFilesBySourceNames>time</sortFilesBySourceNames>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "        <att name=\"contact_email\">samos@coaps.fsu.edu</att>\n" + //
            "        <att name=\"contact_info\">Center for Ocean-Atmospheric Prediction Studies, The Florida State University, Tallahassee, FL, 32306-2840,\n"
            + //
            " USA</att>\n" + //
            "        <att name=\"Cruise_id\">Cruise_id undefined for now</att>\n" + //
            "        <att name=\"Data_modification_date\">16- 2-2012  0: 2: 4 UTC</att>\n" + //
            "        <att name=\"data_provider\">Timothy Salisbury</att>\n" + //
            "        <att name=\"elev\" type=\"short\">0</att>\n" + //
            "        <att name=\"end_date_time\">2012/02/15 - -  23:59  UTC</att>\n" + //
            "        <att name=\"EXPOCODE\">EXPOCODE undefined for now</att>\n" + //
            "        <att name=\"facility\">NOAA</att>\n" + //
            "        <att name=\"fsu_version\">100</att>\n" + //
            "        <att name=\"ID\">WTEP</att>\n" + //
            "        <att name=\"IMO\">009270335</att>\n" + //
            "        <att name=\"Metadata_modification_date\">15- 2-2012 19: 2: 4 EDT</att>\n" + //
            "        <att name=\"platform\">SCS</att>\n" + //
            "        <att name=\"platform_version\">4.0</att>\n" + //
            "        <att name=\"receipt_order\">02</att>\n" + //
            "        <att name=\"site\">OSCAR DYSON</att>\n" + //
            "        <att name=\"start_date_time\">2012/02/15 - -  00:01  UTC</att>\n" + //
            "        <att name=\"title\">OSCAR DYSON Meteorological Data</att>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" + //
            "        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" + //
            "        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" + //
            "    -->\n" + //
            "    <addAttributes>\n" + //
            "\n" + //
            "        <att name=\"cdm_data_type\">Trajectory</att>\n" + //
            "        <att name=\"cdm_trajectory_variables\">ID, site</att>\n" + //
            "        <att name=\"subsetVariables\">ID, site, IMO, cruise_id, expocode, facility, platform, platform_version</att>\n"
            + //
            "\n" + //
            "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
            "        <att name=\"creator_email\">samos@coaps.fsu.edu</att>\n" + //
            "        <att name=\"creator_name\">Shipboard Automated Meteorological and Oceanographic System (SAMOS)</att>\n"
            + //
            "        <att name=\"creator_url\">https://samos.coaps.fsu.edu/html/</att>\n" + //
            "        <att name=\"creator_type\">institution</att>\n" + //
            "        <att name=\"infoUrl\">https://samos.coaps.fsu.edu/html/</att>\n" + //
            "        <att name=\"institution\">FSU</att>\n" + //
            "        <att name=\"keywords\">\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Pressure &gt; Atmospheric Pressure Measurements,\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Pressure &gt; Sea Level Pressure,\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Pressure &gt; Static Pressure,\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Temperature &gt; Air Temperature,\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Temperature &gt; Surface Air Temperature,\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Water Vapor &gt; Humidity,\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Surface Winds,\n" + //
            "Earth Science &gt; Oceans &gt; Salinity/Density &gt; Conductivity,\n" + //
            "Earth Science &gt; Oceans &gt; Salinity/Density &gt; Salinity,\n" + //
            "air, air_pressure, air_temperature, atmosphere, atmospheric, calender, conductivity, control, course, data, date, day, density, direction, Dyson, earth, electrical, file, flags, from, fsu, ground, heading, history, humidity, information, level, measurements, meteorological, meteorology, oceans, Oscar, over, platform, pressure, quality, relative, relative_humidity, salinity, sea, sea_water_electrical_conductivity, sea_water_practical_salinity, seawater, speed, static, surface, temperature, time, vapor, water, wind, wind_from_direction, wind_speed, winds</att>\n"
            + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"sourceUrl\">https://tds.coaps.fsu.edu/thredds/catalog/samos/data/quick/WTEP/2022/catalog.xml</att>\n"
            + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"summary\">NOAA Ship Oscar Dyson Underway Meteorological Data (Near Real Time, updated daily) are from the Shipboard Automated Meteorological and Oceanographic System (SAMOS) program.\n"
            + //
            "\n" + //
            "IMPORTANT: ALWAYS USE THE QUALITY FLAG DATA! Each data variable's metadata includes a qcindex attribute which indicates a character number in the flag data.  ALWAYS check the flag data for each row of data to see which data is good (flag='Z') and which data isn't.  For example, to extract just data where time (qcindex=1), latitude (qcindex=2), longitude (qcindex=3), and airTemperature (qcindex=12) are 'good' data, include this constraint in your ERDDAP query:\n"
            + //
            "  flag=~\"ZZZ........Z.*\"\n" + //
            "in your query.\n" + //
            "'=~' indicates this is a regular expression constraint.\n" + //
            "The 'Z's are literal characters.  In this dataset, 'Z' indicates 'good' data.\n" + //
            "The '.'s say to match any character.\n" + //
            "The '*' says to match the previous character 0 or more times.\n" + //
            "(Don't include backslashes in your query.)\n" + //
            "See the tutorial for regular expressions at\n" + //
            "http://www.vogella.com/tutorials/JavaRegularExpressions/article.html</att>\n" + //
            "        <att name=\"title\">NOAA Ship Oscar Dyson Underway Meteorological Data, Near Real Time</att>\n" + //
            "    </addAttributes>\n" + //
            "\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:ID</sourceName>\n" + //
            "        <destinationName>ID</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Call Sign</att>\n" + //
            "            <att name=\"cf_role\">trajectory_id</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:site</sourceName>\n" + //
            "        <destinationName>site</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Ship Name</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>    \n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:IMO</sourceName>\n" + //
            "        <destinationName>IMO</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:Cruise_id</sourceName>\n" + //
            "        <destinationName>cruise_id</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:EXPOCODE</sourceName>\n" + //
            "        <destinationName>expocode</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:facility</sourceName>\n" + //
            "        <destinationName>facility</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:platform</sourceName>\n" + //
            "        <destinationName>platform</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:platform_version</sourceName>\n" + //
            "        <destinationName>platform_version</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>time</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <dataType>int</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"intList\">16895521 16896959</att>\n" + //
            "            <att name=\"data_interval\" type=\"int\">60</att>\n" + //
            "            <att name=\"long_name\">time</att>\n" + //
            "            <att name=\"observation_type\">calculated</att>\n" + //
            "            <att name=\"original_units\">hhmmss UTC</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">1</att>\n" + //
            "            <att name=\"units\">minutes since 1-1-1980 00:00 UTC</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"int\">2147483647</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n"
            + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"standard_name\">time</att>\n" + //
            "            <att name=\"original_units\">null</att>\n" + //
            "            <att name=\"units\">minutes since 1980-01-01T00:00:00Z</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>lat</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">54.44 54.79</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">latitude</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"original_units\">degrees (+N)</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">2</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"units\">degrees (+N)</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"units\">degrees_north</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>lon</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">196.77 197.74</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">longitude</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"original_units\">degrees (-W/+E)</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">3</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"units\">degrees (+E)</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"units\">degrees_east</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>P</sourceName>\n" + //
            "        <destinationName>airPressure</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">976.18 982.08</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">atmospheric pressure</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"mslp_indicator\">at sensor height</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"original_units\">hectopascal</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">11</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">0.1</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">millibar</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">1050.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">950.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Pressure</att>\n" + //
            "            <att name=\"long_name\">Atmospheric Pressure</att>\n" + //
            "            <att name=\"standard_name\">air_pressure</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>T</sourceName>\n" + //
            "        <destinationName>airTemperature</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">1.01 4.8</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">air temperature</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"original_units\">celsius</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">12</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">celsius</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">Air Temperature</att>\n" + //
            "            <att name=\"standard_name\">air_temperature</att>\n" + //
            "            <att name=\"original_units\">null</att>\n" + //
            "            <att name=\"units\">degree_C</att>            \n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>CNDC</sourceName>\n" + //
            "        <destinationName>conductivity</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">2.68 2.86</att>\n" + //
            "            <att name=\"average_center\">unknown</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">conductivity</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"original_units\">siemens meter-1</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">16</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">siemens meter-1</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">4</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Salinity</att>\n" + //
            "            <att name=\"long_name\">Conductivity</att>\n" + //
            "            <att name=\"standard_name\">sea_water_electrical_conductivity</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>RH</sourceName>\n" + //
            "        <destinationName>relativeHumidity</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">66.0 99.0</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">relative humidity</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"original_units\">percent</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">13</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">percent</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Meteorology</att>\n" + //
            "            <att name=\"long_name\">Relative Humidity</att>\n" + //
            "            <att name=\"standard_name\">relative_humidity</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>SSPS</sourceName>\n" + //
            "        <destinationName>salinity</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">31.08 31.76</att>\n" + //
            "            <att name=\"average_center\">unknown</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">-9999</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_interval\" type=\"int\">60</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">salinity</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">calculated</att>\n" + //
            "            <att name=\"original_units\">PSU</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">15</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">PSU</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Salinity</att>\n" + //
            "            <att name=\"long_name\">Sea Water Practical Salinity</att>\n" + //
            "            <att name=\"standard_name\">sea_water_practical_salinity</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>TS</sourceName>\n" + //
            "        <destinationName>seaTemperature</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">0.97 2.51</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">sea temperature</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"original_units\">celsius</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">14</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ts_sensor_category\" type=\"short\">12</att>\n" + //
            "            <att name=\"units\">celsius</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">Sea Water Temperature</att>\n" + //
            "            <att name=\"standard_name\">sea_water_temperature</att>\n" + //
            "            <att name=\"original_units\">null</att>\n" + //
            "            <att name=\"units\">degree_C</att>            \n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>DIR</sourceName>\n" + //
            "        <destinationName>windDirection</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">95.62 186.79</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_interval\" type=\"int\">-9999</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">earth relative wind direction</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">calculated</att>\n" + //
            "            <att name=\"original_units\">degrees (clockwise from true north)</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">6</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">degrees (clockwise from true north)</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "            <att name=\"long_name\">Earth Relative Wind Direction</att>\n" + //
            "            <att name=\"standard_name\">wind_from_direction</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>SPD</sourceName>\n" + //
            "        <destinationName>windSpeed</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">2.793192 16.38364</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_interval\" type=\"int\">-9999</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">earth relative wind speed</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">calculated</att>\n" + //
            "            <att name=\"original_units\">knot</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">9</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">meter second-1</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "            <att name=\"long_name\">Earth Relative Wind Speed</att>\n" + //
            "            <att name=\"standard_name\">wind_speed</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>PL_CRS</sourceName>\n" + //
            "        <destinationName>platformCourse</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">0.61 359.55</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">platform course</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">calculated</att>\n" + //
            "            <att name=\"original_units\">degrees (clockwise towards true north)</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">5</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">degrees (clockwise towards true north)</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "            <att name=\"long_name\">Platform Course</att>\n" + //
            "            <att name=\"units\">degrees_true</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>PL_HD</sourceName>\n" + //
            "        <destinationName>platformHeading</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">1.42 358.77</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">platform heading</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">calculated</att>\n" + //
            "            <att name=\"original_units\">degrees (clockwise towards true north)</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">4</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">degrees (clockwise towards true north)</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Unknown</att>\n" + //
            "            <att name=\"long_name\">Platform Heading</att>\n" + //
            "            <att name=\"units\">degrees_true</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>PL_SPD</sourceName>\n" + //
            "        <destinationName>platformSpeed</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">0.046296 6.6872</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">platform speed over ground</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">calculated</att>\n" + //
            "            <att name=\"original_units\">knot</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">8</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">0.5</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">meter second-1</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Other</att>\n" + //
            "            <att name=\"long_name\">Platform Speed Over Ground</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>PL_WDIR</sourceName>\n" + //
            "        <destinationName>platformWindDirection</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">0.1 359.43</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_interval\" type=\"int\">-9999</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">platform relative wind direction</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"original_units\">degrees (clockwise from bow)</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">7</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">degrees (clockwise from bow)</att>\n" + //
            "            <att name=\"zero_line_reference\" type=\"float\">-9999.0</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "            <att name=\"long_name\">Platform Relative Wind Direction</att>\n" + //
            "            <att name=\"standard_name\">wind_from_direction</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>PL_WSPD</sourceName>\n" + //
            "        <destinationName>platformWindSpeed</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"actual_range\" type=\"floatList\">0.020576 17.30956</att>\n" + //
            "            <att name=\"average_center\">time at end of period</att>\n" + //
            "            <att name=\"average_length\" type=\"short\">60</att>\n" + //
            "            <att name=\"average_method\">average</att>\n" + //
            "            <att name=\"centerline_offset\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"data_interval\" type=\"int\">-9999</att>\n" + //
            "            <att name=\"data_precision\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"distance_from_bow\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"height\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"instrument\">unknown</att>\n" + //
            "            <att name=\"long_name\">platform relative wind speed</att>\n" + //
            "            <att name=\"missing_value\" type=\"float\">-9999.0</att>\n" + //
            "            <att name=\"observation_type\">measured</att>\n" + //
            "            <att name=\"original_units\">knot</att>\n" + //
            "            <att name=\"qcindex\" type=\"int\">10</att>\n" + //
            "            <att name=\"sampling_rate\" type=\"float\">1.0</att>\n" + //
            "            <att name=\"special_value\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"units\">meter second-1</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-8888.0</att>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "            <att name=\"long_name\">Platform Relative Wind Speed</att>\n" + //
            "            <att name=\"standard_name\">wind_speed</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>flag</sourceName>\n" + //
            "        <destinationName>flag</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"A\">Units added</att>\n" + //
            "            <att name=\"B\">Data out of range</att>\n" + //
            "            <att name=\"C\">Non-sequential time</att>\n" + //
            "            <att name=\"D\">Failed T&gt;=Tw&gt;=Td</att>\n" + //
            "            <att name=\"E\">True wind error</att>\n" + //
            "            <att name=\"F\">Velocity unrealistic</att>\n" + //
            "            <att name=\"G\">Value &gt; 4 s. d. from climatology</att>\n" + //
            "            <att name=\"H\">Discontinuity</att>\n" + //
            "            <att name=\"I\">Interesting feature</att>\n" + //
            "            <att name=\"J\">Erroneous</att>\n" + //
            "            <att name=\"K\">Suspect - visual</att>\n" + //
            "            <att name=\"L\">Ocean platform over land</att>\n" + //
            "            <att name=\"long_name\">quality control flags</att>\n" + //
            "            <att name=\"M\">Instrument malfunction</att>\n" + //
            "            <att name=\"N\">In Port</att>\n" + //
            "            <att name=\"O\">Multiple original units</att>\n" + //
            "            <att name=\"P\">Movement uncertain</att>\n" + //
            "            <att name=\"Q\">Pre-flagged as suspect</att>\n" + //
            "            <att name=\"R\">Interpolated data</att>\n" + //
            "            <att name=\"S\">Spike - visual</att>\n" + //
            "            <att name=\"T\">Time duplicate</att>\n" + //
            "            <att name=\"U\">Suspect - statistial</att>\n" + //
            "            <att name=\"V\">Spike - statistical</att>\n" + //
            "            <att name=\"X\">Step - statistical</att>\n" + //
            "            <att name=\"Y\">Suspect between X-flags</att>\n" + //
            "            <att name=\"Z\">Good data</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Quality</att>\n" + //
            "            <att name=\"long_name\">Quality Control Flags</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD getpmelTaoDyAirt() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"pmelTaoDyAirt\" active=\"true\">\n" + //
            "    <reloadEveryNMinutes>1000000</reloadEveryNMinutes>\n" + //
            "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/points/tao/daily/").toURI()).toString()
            + "</fileDir>\n" + //
            "    <recursive>false</recursive>\n" + //
            "    <fileNameRegex>airt[0-9].*_dy\\.cdf</fileNameRegex>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <sortedColumnSourceName>time</sortedColumnSourceName>\n" + //
            "    <sortFilesBySourceNames>global:site_code time</sortFilesBySourceNames>\n" + //
            "    <defaultGraphQuery>longitude,latitude,AT_21&amp;time&gt;=now-7days</defaultGraphQuery>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "        <att name=\"_FillValue\" type=\"float\">1.0E35</att>\n" + //
            "        <att name=\"CREATION_DATE\">07:37 12-JUL-2011</att>\n" + //
            "        <att name=\"Data_info\">Contact Paul Freitag: 206-526-6727</att>\n" + //
            "        <att name=\"Data_Source\">GTMBA Project Office/NOAA/PMEL</att>\n" + //
            "        <att name=\"File_info\">Contact: Dai.C.McClurg@noaa.gov</att>\n" + //
            "        <att name=\"missing_value\" type=\"float\">1.0E35</att>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"_FillValue\"></att>\n" + //
            "        <att name=\"CREATION_DATE\"></att>\n" + //
            "        <att name=\"platform_code\"></att>\n" + //
            "        <att name=\"site_code\"></att>\n" + //
            "        <att name=\"cdm_data_type\">TimeSeries</att>\n" + //
            "        <att name=\"cdm_timeseries_variables\">array, station, wmo_platform_code, longitude, latitude, depth</att>\n"
            + //
            "        <att name=\"subsetVariables\">array, station, wmo_platform_code, longitude, latitude, depth</att>\n"
            + //
            "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
            "        <att name=\"creator_name\">GTMBA Project Office/NOAA/PMEL</att>\n" + //
            "        <att name=\"creator_email\">Dai.C.McClurg@noaa.gov</att>\n" + //
            "        <att name=\"creator_type\">group</att>\n" + //
            "        <att name=\"creator_url\">https://www.pmel.noaa.gov/gtmba/mission</att>\n" + //
            "        <att name=\"id\"></att>\n" + //
            "        <att name=\"infoUrl\">https://www.pmel.noaa.gov/gtmba/mission</att>\n" + //
            "        <att name=\"institution\">NOAA PMEL, TAO/TRITON, RAMA, PIRATA</att>\n" + //
            "        <att name=\"keywords\">\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Temperature &gt; Air Temperature,\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Temperature &gt; Surface Air Temperature,\n" + //
            "air, air_temperature, atmosphere, atmospheric, buoys, centered, daily, depth, identifier, noaa, pirata, pmel, quality, rama, source, station, surface, tao, temperature, time, triton</att>\n"
            + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"license\">Request for Acknowledgement: If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\n"
            + //
            "\n" + //
            "[standard]</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"missing_value\"></att>\n" + //
            "        <att name=\"project\">TAO/TRITON, RAMA, PIRATA</att>\n" + //
            "        <att name=\"sourceUrl\">(local files)</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"summary\">This dataset has daily Air Temperature data from the\n" + //
            "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\n" + //
            "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\n" + //
            "PIRATA (Atlantic Ocean, http://www.pmel.noaa.gov/gtmba/pirata/ )\n" + //
            "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\n"
            + //
            "https://www.pmel.noaa.gov/gtmba/mission .</att>\n" + //
            "        <att name=\"testOutOfDate\">now-3days</att>\n" + //
            "        <att name=\"title\">TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Air Temperature</att>\n"
            + //
            "    </addAttributes>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:array</sourceName>\n" + //
            "        <destinationName>array</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Array</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:site_code</sourceName>\n" + //
            "        <destinationName>station</destinationName>\n" + //
            "        <dataType>String</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"cf_role\">timeseries_id</att>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">Station</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>global:wmo_platform_code</sourceName>\n" + //
            "        <destinationName>wmo_platform_code</destinationName>\n" + //
            "        <dataType>int</dataType>\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Identifier</att>\n" + //
            "            <att name=\"long_name\">WMO Platform Code</att>\n" + //
            "            <att name=\"missing_value\" type=\"int\">2147483647</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>lon</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"epic_code\" type=\"int\">502</att>\n" + //
            "            <att name=\"type\">EVEN</att>\n" + //
            "            <att name=\"units\">degree_east</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"float\">1e35</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Nominal Longitude</att>\n" + //
            "            <att name=\"standard_name\">longitude</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>lat</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"epic_code\" type=\"int\">500</att>\n" + //
            "            <att name=\"type\">EVEN</att>\n" + //
            "            <att name=\"units\">degree_north</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"float\">1e35</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Nominal Latitude</att>\n" + //
            "            <att name=\"standard_name\">latitude</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>time</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"type\">EVEN</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"long_name\">Centered Time</att>\n" + //
            "            <att name=\"standard_name\">time</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>depth</sourceName>\n" + //
            "        <destinationName>depth</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"epic_code\" type=\"int\">3</att>\n" + //
            "            <att name=\"type\">EVEN</att>\n" + //
            "            <att name=\"units\">m</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"float\">1e35</att>\n" + //
            "            <att name=\"_CoordinateAxisType\">Height</att>\n" + //
            "            <att name=\"_CoordinateZisPositive\">down</att>\n" + //
            "            <att name=\"axis\">Z</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Depth</att>\n" + //
            "            <att name=\"positive\">down</att>\n" + //
            "            <att name=\"standard_name\">depth</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>AT_21</sourceName>\n" + //
            "        <destinationName>AT_21</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"epic_code\" type=\"int\">21</att>\n" + //
            "            <att name=\"generic_name\">atemp</att>\n" + //
            "            <att name=\"long_name\">AIR TEMPERATURE (C)</att>\n" + //
            "            <att name=\"name\">AT</att>\n" + //
            "            <att name=\"units\">C</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"float\">1e35</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-10</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">40</att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">Air Temperature</att>\n" + //
            "            <att name=\"standard_name\">air_temperature</att>\n" + //
            "            <att name=\"units\">degree_C</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>QAT_5021</sourceName>\n" + //
            "        <destinationName>QAT_5021</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"epic_code\" type=\"int\">5021</att>\n" + //
            "            <att name=\"generic_name\">qat</att>\n" + //
            "            <att name=\"long_name\">AIRT QUALITY</att>\n" + //
            "            <att name=\"name\">QAT</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"float\">1e35</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">6</att>\n" + //
            "            <att name=\"colorBarContinuous\">false</att>\n" + //
            "            <att name=\"description\">Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QAT_5021&gt;=1 and QAT_5021&lt;=3.</att>\n"
            + //
            "            <att name=\"ioos_category\">Quality</att>\n" + //
            "            <att name=\"long_name\">Air Temperature Quality</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>SAT_6021</sourceName>\n" + //
            "        <destinationName>SAT_6021</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"epic_code\" type=\"int\">6021</att>\n" + //
            "            <att name=\"generic_name\">sat</att>\n" + //
            "            <att name=\"long_name\">AIRT SOURCE</att>\n" + //
            "            <att name=\"name\">SAT</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"missing_value\" type=\"float\">1e35</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">8</att>\n" + //
            "            <att name=\"colorBarContinuous\">false</att>\n" + //
            "            <att name=\"description\">Source Codes:\n" + //
            "0 = No Sensor, No Data\n" + //
            "1 = Real Time (Telemetered Mode)\n" + //
            "2 = Derived from Real Time\n" + //
            "3 = Temporally Interpolated from Real Time\n" + //
            "4 = Source Code Inactive at Present\n" + //
            "5 = Recovered from Instrument RAM (Delayed Mode)\n" + //
            "6 = Derived from RAM\n" + //
            "7 = Temporally Interpolated from RAM</att>\n" + //
            "            <att name=\"ioos_category\">Other</att>\n" + //
            "            <att name=\"long_name\">Air Temperature Source</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD getminiNdbc410() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDTableAggregateRows\" datasetID=\"miniNdbc410\">\n" + //
        "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" + //
        "    <updateEveryNMillis>10</updateEveryNMillis>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"summary\">miniNdbc summary</att>\n" + //
        "        <att name=\"title\">NDBC Standard Meteorological Buoy Data</att>\n" + //
        "    </addAttributes>\n" + //
        "\n" + //
        "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"miniNdbc4102\">\n" + //
        "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" + //
        "    <updateEveryNMillis>10</updateEveryNMillis>\n" + //
        "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/miniNdbc/").toURI()).toString()
        + "</fileDir>\n" + //
        "    <recursive>false</recursive>\n" + //
        "    <fileNameRegex>NDBC_4102._met\\.nc</fileNameRegex>\n" + //
        "    <fileTableInMemory>false</fileTableInMemory>\n" + //
        "    <metadataFrom>last</metadataFrom>\n" + //
        "    <preExtractRegex>^NDBC_</preExtractRegex>\n" + //
        "    <postExtractRegex>_met\\.nc$</postExtractRegex>\n" + //
        "    <extractRegex>.*</extractRegex>\n" + //
        "    <columnNameForExtract>station</columnNameForExtract>\n" + //
        "    <sortedColumnSourceName>TIME</sortedColumnSourceName>\n" + //
        "    <sortFilesBySourceNames>station TIME</sortFilesBySourceNames>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"cdm_data_type\">TimeSeries</att>\n" + //
        "        <att name=\"cdm_timeseries_variables\">station, prefix, longitude, latitude</att>\n" + //
        "        <att name=\"subsetVariables\">station, prefix, longitude, latitude</att>\n" + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"creator_email\">erd.data@noaa.gov</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"date_created\">null</att>\n" + //
        "        <att name=\"date_issued\">null</att>\n" + //
        "        <att name=\"id\">null</att>\n" + //
        "        <att name=\"infoUrl\">https://www.ndbc.noaa.gov/</att>\n" + //
        "        <att name=\"institution\">NOAA NDBC, CoastWatch WCN</att>\n" + //
        "        <att name=\"keywords\">keyword1, keyword2</att>\n" + //
        "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
        "        <att name=\"license\">[standard]</att>\n" + //
        "        <att name=\"sourceUrl\">https://www.ndbc.noaa.gov/</att>\n" + //
        "        <att name=\"summary\">miniNdbc4102 summary</att>\n" + //
        "        <att name=\"title\">NDBC Standard Meteorological Buoy Data 4102</att>\n" + //
        "    </addAttributes>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>station</sourceName>\n" + //
        "        <destinationName>station</destinationName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"long_name\">Station Name</att>\n" + //
        "            <att name=\"cf_role\">timeseries_id</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>=4102</sourceName>\n" + //
        "        <destinationName>prefix</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"comment\">fixed value</att>\n" + //
        "            <att name=\"ioos_category\">Other</att>\n" + //
        "            <att name=\"units\">m</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>LON</sourceName>\n" + //
        "        <destinationName>longitude</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>LAT</sourceName>\n" + //
        "        <destinationName>latitude</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>TIME</sourceName>\n" + //
        "        <destinationName>time</destinationName>\n" + //
        "        <dataType>double</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"point_spacing\">null</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WD</sourceName>\n" + //
        "        <destinationName>wd</destinationName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">360</att>\n" + //
        "            <att name=\"_FillValue\" type=\"short\">32767</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WSPD</sourceName>\n" + //
        "        <destinationName>wspd</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">15</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>ATMP</sourceName>\n" + //
        "        <destinationName>atmp</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">40</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Temperature</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WTMP</sourceName>\n" + //
        "        <destinationName>wtmp</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" + //
        "            <att name=\"colorBarMaximum\" type=\"double\">32</att>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Temperature</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>\n" + //
        "\n" + //
        "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"miniNdbc4103\">\n" + //
        "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" + //
        "    <updateEveryNMillis>10</updateEveryNMillis>\n" + //
        "    <fileDir>" + Path.of(EDDTestDataset.class.getResource("/data/miniNdbc/").toURI()).toString()
        + "</fileDir>\n" + //
        "    <recursive>false</recursive>\n" + //
        "    <fileNameRegex>NDBC_4103._met\\.nc</fileNameRegex>\n" + //
        "    <fileTableInMemory>false</fileTableInMemory>\n" + //
        "    <metadataFrom>last</metadataFrom>\n" + //
        "    <preExtractRegex>^NDBC_</preExtractRegex>\n" + //
        "    <postExtractRegex>_met\\.nc$</postExtractRegex>\n" + //
        "    <extractRegex>.*</extractRegex>\n" + //
        "    <columnNameForExtract>station</columnNameForExtract>\n" + //
        "    <sortedColumnSourceName>TIME</sortedColumnSourceName>\n" + //
        "    <sortFilesBySourceNames>station TIME</sortFilesBySourceNames>\n" + //
        "    <addAttributes>\n" + //
        "        <att name=\"cdm_data_type\">TimeSeries</att>\n" + //
        "        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" + //
        "        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" + //
        "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" + //
        "        <att name=\"Metadata_Conventions\">null</att>\n" + //
        "        <att name=\"date_created\">null</att>\n" + //
        "        <att name=\"date_issued\">null</att>\n" + //
        "        <att name=\"id\">null</att>\n" + //
        "        <att name=\"infoUrl\">https://www.ndbc.noaa.gov/</att>\n" + //
        "        <att name=\"institution\">NOAA NDBC, CoastWatch WCN</att>\n" + //
        "        <att name=\"keywords\">keyword3, keyword4</att>\n" + //
        "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
        "        <att name=\"license\">[standard]</att>\n" + //
        "        <att name=\"sourceUrl\">https://www.ndbc.noaa.gov/</att>\n" + //
        "        <att name=\"summary\">miniNdbc4103 summary</att>\n" + //
        "        <att name=\"title\">NDBC Standard Meteorological Buoy Data 4103</att>\n" + //
        "    </addAttributes>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>station</sourceName>\n" + //
        "        <destinationName>station</destinationName>\n" + //
        "        <dataType>String</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Identifier</att>\n" + //
        "            <att name=\"long_name\">Station Name</att>\n" + //
        "            <att name=\"cf_role\">timeseries_id</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>=4103</sourceName>\n" + //
        "        <destinationName>prefix</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"ioos_category\">Other</att>\n" + //
        "            <att name=\"units\">m</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>LON</sourceName>\n" + //
        "        <destinationName>longitude</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>LAT</sourceName>\n" + //
        "        <destinationName>latitude</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>TIME</sourceName>\n" + //
        "        <destinationName>time</destinationName>\n" + //
        "        <dataType>double</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"point_spacing\">null</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WD</sourceName>\n" + //
        "        <destinationName>wd</destinationName>\n" + //
        "        <dataType>short</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"_FillValue\" type=\"short\">32767</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WSPD</sourceName>\n" + //
        "        <destinationName>wspd</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Wind</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>ATMP</sourceName>\n" + //
        "        <destinationName>atmp</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Temperature</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "    <dataVariable>\n" + //
        "        <sourceName>WTMP</sourceName>\n" + //
        "        <destinationName>wtmp</destinationName>\n" + //
        "        <dataType>float</dataType>\n" + //
        "        <addAttributes>\n" + //
        "            <att name=\"_FillValue\" type=\"float\">-9999999</att>\n" + //
        "            <att name=\"ioos_category\">Temperature</att>\n" + //
        "        </addAttributes>\n" + //
        "    </dataVariable>\n" + //
        "</dataset>\n" + //
        "</dataset>");
  }

  public static EDD gettestEDDTableCacheFiles() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD
        .oneFromXmlFragment(null,
            "<dataset type=\"EDDTableFromNcCFFiles\" datasetID=\"testEDDTableCacheFiles\" active=\"true\">\n" + //
                "    <reloadEveryNMinutes>1000000</reloadEveryNMinutes>\n" + //
                "    <fileDir>"
                + Path.of(EDDTestDataset.class.getResource("/largeFiles/points/testEDDTableCacheFiles/").toURI())
                    .toString()
                + "</fileDir>\n" +
                "    <recursive>false</recursive>\n" + //
                "    <fileNameRegex>.*\\.nc</fileNameRegex>\n" + //
                "    <cacheFromUrl>http://localhost:8080/erddap/files/erdGtsppBest/</cacheFromUrl>\n" + //
                "    <cacheSizeGB>1</cacheSizeGB>\n" + //
                "    <metadataFrom>last</metadataFrom>\n" + //
                "    <sortedColumnSourceName>time</sortedColumnSourceName>\n" + //
                "    <sortFilesBySourceNames>time</sortFilesBySourceNames>   \n" + //
                "    <nThreads>2</nThreads>\n" + //
                "    <addAttributes>\n" + //
                "        <!-- change 2 dates here every month (2nd date is END of processing) -->\n" + //
                "        <att name=\"history\">2018-08-01 csun writeGTSPPnc40.f90 Version 1.8\n" + //
                ".tgz files from ftp.nodc.noaa.gov /pub/gtspp/best_nc/ (https://www.nodc.noaa.gov/GTSPP/)\n" + //
                "2018-08-11 Most recent ingest, clean, and reformat at ERD (erd.data at noaa.gov).</att>\n" + //
                "        <att name=\"id\">erdGtsppBest</att>\n" + //
                "        <att name=\"subsetVariables\">trajectory, org, type, platform, cruise</att>  \n" + //
                "    </addAttributes>\n" + //
                "    <dataVariable>\n" + //
                "        <sourceName>trajectory</sourceName>\n" + //
                "        <dataType>String</dataType>\n" + //
                "    </dataVariable>\n" + //
                "    <dataVariable>\n" + //
                "        <sourceName>org</sourceName>\n" + //
                "        <dataType>String</dataType>\n" + //
                "    </dataVariable>\n" + //
                "    <dataVariable>\n" + //
                "        <sourceName>type</sourceName>\n" + //
                "        <dataType>String</dataType>\n" + //
                "    </dataVariable>\n" + //
                "    <dataVariable>\n" + //
                "        <sourceName>platform</sourceName>\n" + //
                "        <dataType>String</dataType>\n" + //
                "    </dataVariable>\n" + //
                "    <dataVariable>\n" + //
                "        <sourceName>cruise</sourceName>\n" + //
                "        <dataType>String</dataType>\n" + //
                "    </dataVariable>\n" + //
                "    <dataVariable>\n" + //
                "        <sourceName>station_id</sourceName>\n" + //
                "        <dataType>int</dataType>\n" + //
                "    </dataVariable>\n" + //
                "    <dataVariable>\n" + //
                "        <sourceName>longitude</sourceName>\n" + //
                "        <dataType>float</dataType>\n" + //
                "    </dataVariable>\n" + //
                "    <dataVariable>\n" + //
                "        <sourceName>latitude</sourceName>\n" + //
                "        <dataType>float</dataType>\n" + //
                "    </dataVariable>\n" + //
                "    <dataVariable>\n" + //
                "        <sourceName>time</sourceName>\n" + //
                "        <dataType>double</dataType>\n" + //
                "    </dataVariable>\n" + //
                "    <dataVariable>\n" + //
                "        <sourceName>depth</sourceName>\n" + //
                "        <dataType>float</dataType>\n" + //
                "    </dataVariable>\n" + //
                "    <dataVariable>\n" + //
                "        <sourceName>temperature</sourceName>\n" + //
                "        <dataType>float</dataType>\n" + //
                "    </dataVariable>\n" + //
                "    <dataVariable>\n" + //
                "        <sourceName>salinity</sourceName>\n" + //
                "        <dataType>float</dataType>\n" + //
                "    </dataVariable>\n" + //
                "</dataset>");
  }

  public static EDD gettestEDDGridFromNcFilesUnpacked() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDGridFromNcFilesUnpacked\" datasetID=\"testEDDGridFromNcFilesUnpacked\" active=\"true\">\n" + //
            "    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" + //
            "    <updateEveryNMillis>10000</updateEveryNMillis>\n" + //
            "    <fileDir>"
            + Path.of(EDDTestDataset.class.getResource("/data/nc/").toURI()).toString()
            + "</fileDir>\n" + //
            "    <fileNameRegex>scale_factor\\.nc</fileNameRegex>\n" + //
            "    <recursive>true</recursive>\n" + //
            "    <pathRegex>.*</pathRegex>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <matchAxisNDigits>20</matchAxisNDigits>\n" + //
            "    <fileTableInMemory>false</fileTableInMemory>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "        <att name=\"_CoordSysBuilder\">ucar.nc2.dataset.conv.CF1Convention</att>\n" + //
            "        <att name=\"comment\">Interim-MUR(nrt) will be replaced by MUR-Final in about 3 days; MUR = &quot;Multi-scale Ultra-high Reolution&quot;; produced under NASA MEaSUREs program.</att>\n"
            + //
            "        <att name=\"contact\">ghrsst@podaac.jpl.nasa.gov</att>\n" + //
            "        <att name=\"Conventions\">CF-1.0</att>\n" + //
            "        <att name=\"creation_date\">2015-10-06</att>\n" + //
            "        <att name=\"DSD_entry_id\">JPL-L4UHfnd-GLOB-MUR</att>\n" + //
            "        <att name=\"easternmost_longitude\" type=\"float\">180.0</att>\n" + //
            "        <att name=\"file_quality_index\">0</att>\n" + //
            "        <att name=\"GDS_version_id\">GDS-v1.0-rev1.6</att>\n" + //
            "        <att name=\"History\">Translated to CF-1.0 Conventions by Netcdf-Java CDM (NetcdfCFWriter)\n" + //
            "Original Dataset = satellite/MUR/ssta/1day; Translation Date = Thu Oct 08 09:39:01 PDT 2015</att>\n" + //
            "        <att name=\"history\">Interim near-real-time (nrt) version created at nominal 1-day latency.</att>\n"
            + //
            "        <att name=\"institution\">Jet Propulsion Laboratory</att>\n" + //
            "        <att name=\"netcdf_version_id\">3.5</att>\n" + //
            "        <att name=\"northernmost_latitude\" type=\"float\">90.0</att>\n" + //
            "        <att name=\"product_version\">04nrt</att>\n" + //
            "        <att name=\"references\">ftp://mariana.jpl.nasa.gov/mur_sst/tmchin/docs/ATBD/</att>\n" + //
            "        <att name=\"source_data\">AVHRR19_G-NAVO, AVHRR_METOP_A-EUMETSAT, MODIS_A-JPL, MODIS_T-JPL, WSAT-REMSS, iQUAM-NOAA/NESDIS, Ice_Conc-OSISAF</att>\n"
            + //
            "        <att name=\"southernmost_latitude\" type=\"float\">-90.0</att>\n" + //
            "        <att name=\"spatial_resolution\">0.011 degrees</att>\n" + //
            "        <att name=\"start_date\">2015-10-05</att>\n" + //
            "        <att name=\"start_time\">09:00:00 UTC</att>\n" + //
            "        <att name=\"stop_date\">2015-10-05</att>\n" + //
            "        <att name=\"stop_time\">09:00:00 UTC</att>\n" + //
            "        <att name=\"title\">Daily MUR SST, Interim near-real-time (nrt) product</att>\n" + //
            "        <att name=\"westernmost_longitude\" type=\"float\">-180.0</att>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"cdm_data_type\">Grid</att>\n" + //
            "        <att name=\"comment\">Interim-MUR(nrt) will be replaced by MUR-Final in about 3 days; MUR = &quot;Multi-scale Ultra-high Resolution&quot;; produced under NASA MEaSUREs program.</att>\n"
            + //
            "        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" + //
            "        <att name=\"creator_email\">ghrsst@podaac.jpl.nasa.gov</att>\n" + //
            "        <att name=\"creator_name\">GHRSST</att>\n" + //
            "        <att name=\"creator_url\">https://podaac.jpl.nasa.gov/</att>\n" + //
            "        <att name=\"easternmost_longitude\">null</att>\n" + //
            "        <att name=\"History\">null</att>\n" + //
            "        <att name=\"infoUrl\">https://podaac.jpl.nasa.gov/</att>\n" + //
            "        <att name=\"keywords\">analysed, analysed_sst, daily, data, day, earth, environments, foundation, high, interim, jet, laboratory, making, measures, multi, multi-scale, mur, near, near real time, near-real-time, nrt, ocean, oceans,\n"
            + //
            "Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature,\n" + //
            "product, propulsion, real, records, research, resolution, scale, sea, sea_surface_foundation_temperature, sst, surface, system, temperature, time, ultra, ultra-high, use</att>\n"
            + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"northernmost_latitude\">null</att>\n" + //
            "        <att name=\"southernmost_latitude\">null</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"start_date\">null</att>\n" + //
            "        <att name=\"start_time\">null</att>\n" + //
            "        <att name=\"stop_date\">null</att>\n" + //
            "        <att name=\"stop_time\">null</att>\n" + //
            "        <att name=\"summary\">Interim-Multi-scale Ultra-high Resolution (MUR)(nrt) will be replaced by MUR-Final in about 3 days; MUR = &quot;Multi-scale Ultra-high Resolution&quot;; produced under NASA Making Earth System Data Records for Use in Research Environments (MEaSUREs) program.</att>\n"
            + //
            "        <att name=\"westernmost_longitude\">null</att>\n" + //
            "    </addAttributes>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>time</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_CoordinateAxisType\">Time</att>\n" + //
            "            <att name=\"axis\">T</att>\n" + //
            "            <att name=\"long_name\">reference time of sst field</att>\n" + //
            "            <att name=\"standard_name\">time</att>\n" + //
            "            <att name=\"units\">seconds since 1981-01-01 00:00:00 UTC</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>lat</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_CoordinateAxisType\">Lat</att>\n" + //
            "            <att name=\"axis\">Y</att>\n" + //
            "            <att name=\"long_name\">latitude</att>\n" + //
            "            <att name=\"standard_name\">latitude</att>\n" + //
            "            <att name=\"units\">degrees_north</att>\n" + //
            "            <att name=\"valid_max\" type=\"float\">90.0</att>\n" + //
            "            <att name=\"valid_min\" type=\"float\">-90.0</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Latitude</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>lon</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_CoordinateAxisType\">Lon</att>\n" + //
            "            <att name=\"axis\">X</att>\n" + //
            "            <att name=\"long_name\">longitude</att>\n" + //
            "            <att name=\"standard_name\">longitude</att>\n" + //
            "            <att name=\"units\">degrees_east</att>\n" + //
            "            <att name=\"valid_max\" type=\"float\">180.0</att>\n" + //
            "            <att name=\"valid_min\" type=\"float\">-180.0</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Longitude</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>analysed_sst</sourceName>\n" + //
            "        <destinationName>analysed_sst</destinationName>\n" + //
            "        <dataType>double</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" + //
            "            <att name=\"comment\">Interim near-real-time (nrt) version; to be replaced by Final version</att>\n"
            + //
            "            <att name=\"coordinates\">time lat lon </att>\n" + //
            "            <att name=\"long_name\">analysed sea surface temperature</att>\n" + //
            "            <att name=\"standard_name\">sea_surface_foundation_temperature</att>\n" + //
            "            <att name=\"units\">kelvin</att>\n" + //
            "            <att name=\"valid_max\" type=\"double\">330.917</att>\n" + //
            "            <att name=\"valid_min\" type=\"double\">265.383</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">305.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">273.0</att>\n" + //
            "            <att name=\"coordinates\">null</att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD gettestUInt16FileUnpacked() throws Throwable {
    EDStatic.doSetupValidation = false;
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDGridFromNcFilesUnpacked\" datasetID=\"testUInt16FileUnpacked\" active=\"true\">\n" + //
            "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" + //
            "    <updateEveryNMillis>10000</updateEveryNMillis>\n" + //
            "    <fileDir>"
            + Path.of(EDDTestDataset.class.getResource("/data/unsigned/").toURI()).toString()
            + "</fileDir>\n" + //
            "    <fileNameRegex>9km_aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.nc</fileNameRegex>\n" + //
            "    <recursive>true</recursive>\n" + //
            "    <pathRegex>.*</pathRegex>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <matchAxisNDigits>20</matchAxisNDigits>\n" + //
            "    <fileTableInMemory>false</fileTableInMemory>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "        <att name=\"Conventions\">CF-1.0</att>\n" + //
            "        <att name=\"Data_Bins\" type=\"int\">14234182</att>\n" + //
            "        <att name=\"Data_Maximum\" type=\"float\">36.915</att>\n" + //
            "        <att name=\"Data_Minimum\" type=\"float\">-1.999999</att>\n" + //
            "        <att name=\"Easternmost_Longitude\" type=\"float\">180.0</att>\n" + //
            "        <att name=\"End_Day\" type=\"short\">271</att>\n" + //
            "        <att name=\"End_Millisec\" type=\"int\">10806395</att>\n" + //
            "        <att name=\"End_Orbit\" type=\"int\">0</att>\n" + //
            "        <att name=\"End_Time\">2009271030006395</att>\n" + //
            "        <att name=\"End_Year\" type=\"short\">2009</att>\n" + //
            "        <att name=\"geospatial_lat_max\" type=\"double\">89.95833587646484</att>\n" + //
            "        <att name=\"geospatial_lat_min\" type=\"double\">-89.95833587646484</att>\n" + //
            "        <att name=\"geospatial_lon_max\" type=\"double\">-134.04165649414062</att>\n" + //
            "        <att name=\"geospatial_lon_min\" type=\"double\">-136.04165649414062</att>\n" + //
            "        <att name=\"History\">Translated to CF-1.0 Conventions by Netcdf-Java CDM (NetcdfCFWriter)\n" + //
            "Original Dataset = file:/usr/ftp/ncml/catalog_ncml/OceanTemperature/modis/aqua/11um/9km/aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.ncml; Translation Date = Fri Oct 30 09:44:07 GMT-08:00 2015</att>\n"
            + //
            "        <att name=\"Input_Files\">A20092652009272.L3b_8D_SST.main</att>\n" + //
            "        <att name=\"Input_Parameters\">IFILE = /data3/sdpsoper/vdc/vpu2/workbuf/A20092652009272.L3b_8D_SST.main|OFILE = A20092652009272.L3m_8D_SST_9|PFILE = |PROD = sst|PALFILE = DEFAULT|RFLAG = ORIGINAL|MEAS = 1|STYPE = 0|DATAMIN = 0.000000|DATAMAX = 0.000000|LONWEST = -180.000000|LONEAST = 180.000000|LATSOUTH = -90.000000|LATNORTH = 90.000000|RESOLUTION = 9km|PROJECTION = RECT|GAP_FILL = 0|SEAM_LON = -180.000000|PRECISION=I</att>\n"
            + //
            "        <att name=\"Intercept\" type=\"float\">-2.0</att>\n" + //
            "        <att name=\"L2_Flag_Names\">LAND,HISOLZ</att>\n" + //
            "        <att name=\"Latitude_Step\" type=\"float\">0.083333336</att>\n" + //
            "        <att name=\"Latitude_Units\">degrees North</att>\n" + //
            "        <att name=\"Longitude_Step\" type=\"float\">0.083333336</att>\n" + //
            "        <att name=\"Longitude_Units\">degrees East</att>\n" + //
            "        <att name=\"Map_Projection\">Equidistant Cylindrical</att>\n" + //
            "        <att name=\"Measure\">Mean</att>\n" + //
            "        <att name=\"Northernmost_Latitude\" type=\"float\">90.0</att>\n" + //
            "        <att name=\"Number_of_Columns\" type=\"int\">4320</att>\n" + //
            "        <att name=\"Number_of_Lines\" type=\"int\">2160</att>\n" + //
            "        <att name=\"Orbit\" type=\"int\">0</att>\n" + //
            "        <att name=\"Parameter\">Sea Surface Temperature</att>\n" + //
            "        <att name=\"Period_End_Day\" type=\"short\">270</att>\n" + //
            "        <att name=\"Period_End_Year\" type=\"short\">2009</att>\n" + //
            "        <att name=\"Period_Start_Day\" type=\"short\">265</att>\n" + //
            "        <att name=\"Period_Start_Year\" type=\"short\">2009</att>\n" + //
            "        <att name=\"Processing_Control\">smigen par=A20092652009272.L3m_8D_SST_9.param</att>\n" + //
            "        <att name=\"Processing_Time\">2009282201111000</att>\n" + //
            "        <att name=\"Product_Name\">A20092652009272.L3m_8D_SST_9</att>\n" + //
            "        <att name=\"Product_Type\">8-day</att>\n" + //
            "        <att name=\"Replacement_Flag\">ORIGINAL</att>\n" + //
            "        <att name=\"Scaled_Data_Maximum\" type=\"float\">45.0</att>\n" + //
            "        <att name=\"Scaled_Data_Minimum\" type=\"float\">-2.0</att>\n" + //
            "        <att name=\"Scaling\">linear</att>\n" + //
            "        <att name=\"Scaling_Equation\">(Slope*l3m_data) + Intercept = Parameter value</att>\n" + //
            "        <att name=\"Sensor_Name\">MODISA</att>\n" + //
            "        <att name=\"Slope\" type=\"float\">7.17185E-4</att>\n" + //
            "        <att name=\"Software_Name\">smigen</att>\n" + //
            "        <att name=\"Software_Version\">4.0</att>\n" + //
            "        <att name=\"Southernmost_Latitude\" type=\"float\">-90.0</att>\n" + //
            "        <att name=\"start_date\">2002-07-04 UTC</att>\n" + //
            "        <att name=\"Start_Day\" type=\"short\">265</att>\n" + //
            "        <att name=\"Start_Millisec\" type=\"int\">8779</att>\n" + //
            "        <att name=\"Start_Orbit\" type=\"int\">0</att>\n" + //
            "        <att name=\"Start_Time\">2009265000008779</att>\n" + //
            "        <att name=\"start_time\">00:00:00 UTC</att>\n" + //
            "        <att name=\"Start_Year\" type=\"short\">2009</att>\n" + //
            "        <att name=\"Station_Latitude\" type=\"float\">0.0</att>\n" + //
            "        <att name=\"Station_Longitude\" type=\"float\">0.0</att>\n" + //
            "        <att name=\"stop_date\">2015-03-06 UTC</att>\n" + //
            "        <att name=\"stop_time\">23:59:59 UTC</att>\n" + //
            "        <att name=\"SW_Point_Latitude\" type=\"float\">-89.958336</att>\n" + //
            "        <att name=\"SW_Point_Longitude\" type=\"float\">-179.95833</att>\n" + //
            "        <att name=\"Title\">MODISA Level-3 Standard Mapped Image</att>\n" + //
            "        <att name=\"Units\">deg-C</att>\n" + //
            "        <att name=\"Westernmost_Longitude\" type=\"float\">-180.0</att>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"cdm_data_type\">Grid</att>\n" + //
            "        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" + //
            "        <att name=\"Data_Bins\">null</att>\n" + //
            "        <att name=\"Data_Maximum\">null</att>\n" + //
            "        <att name=\"Data_Minimum\">null</att>\n" + //
            "        <att name=\"Easternmost_Longitude\">null</att>\n" + //
            "        <att name=\"End_Day\">null</att>\n" + //
            "        <att name=\"End_Millisec\">null</att>\n" + //
            "        <att name=\"End_Orbit\">null</att>\n" + //
            "        <att name=\"End_Time\">null</att>\n" + //
            "        <att name=\"End_Year\">null</att>\n" + //
            "        <att name=\"History\">null</att>\n" + //
            "        <att name=\"history\">Translated to CF-1.0 Conventions by Netcdf-Java CDM (NetcdfCFWriter)\n" + //
            "Original Dataset = file:/usr/ftp/ncml/catalog_ncml/OceanTemperature/modis/aqua/11um/9km/aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.ncml; Translation Date = Fri Oct 30 09:44:07 GMT-08:00 2015</att>\n"
            + //
            "        <att name=\"infoUrl\">???</att>\n" + //
            "        <att name=\"Input_Files\">null</att>\n" + //
            "        <att name=\"institution\">???</att>\n" + //
            "        <att name=\"Intercept\">null</att>\n" + //
            "        <att name=\"keywords\">aqua, data, earth, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, image, imaging, L3, l3m_data, l3m_qual, mapped, moderate, modis, modisa, ocean, oceans, quality, resolution, science, sea, sea_surface_temperature, smi, spectroradiometer, standard, surface, temperature, time</att>\n"
            + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"Latitude_Step\">null</att>\n" + //
            "        <att name=\"Latitude_Units\">null</att>\n" + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"Longitude_Step\">null</att>\n" + //
            "        <att name=\"Longitude_Units\">null</att>\n" + //
            "        <att name=\"Northernmost_Latitude\">null</att>\n" + //
            "        <att name=\"Number_of_Columns\">null</att>\n" + //
            "        <att name=\"Number_of_Lines\">null</att>\n" + //
            "        <att name=\"Orbit\">null</att>\n" + //
            "        <att name=\"Parameter\">null</att>\n" + //
            "        <att name=\"Period_End_Day\">null</att>\n" + //
            "        <att name=\"Period_End_Year\">null</att>\n" + //
            "        <att name=\"Period_Start_Day\">null</att>\n" + //
            "        <att name=\"Period_Start_Year\">null</att>\n" + //
            "        <att name=\"Scaling\">null</att>\n" + //
            "        <att name=\"Scaling_Equation\">null</att>\n" + //
            "        <att name=\"Slope\">null</att>\n" + //
            "        <att name=\"Southernmost_Latitude\">null</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"start_date\">null</att>\n" + //
            "        <att name=\"Start_Day\">null</att>\n" + //
            "        <att name=\"Start_Millisec\">null</att>\n" + //
            "        <att name=\"Start_Orbit\">null</att>\n" + //
            "        <att name=\"Start_Time\">null</att>\n" + //
            "        <att name=\"start_time\">null</att>\n" + //
            "        <att name=\"Start_Year\">null</att>\n" + //
            "        <att name=\"Station_Latitude\">null</att>\n" + //
            "        <att name=\"Station_Longitude\">null</att>\n" + //
            "        <att name=\"stop_date\">null</att>\n" + //
            "        <att name=\"stop_time\">null</att>\n" + //
            "        <att name=\"summary\">Moderate Resolution Imaging Spectroradiometer on Aqua (MODISA) Level-3 Standard Mapped Image</att>\n"
            + //
            "        <att name=\"SW_Point_Latitude\">null</att>\n" + //
            "        <att name=\"SW_Point_Longitude\">null</att>\n" + //
            "        <att name=\"Title\">null</att>\n" + //
            "        <att name=\"title\">MODISA L3 SMI,</att>\n" + //
            "        <att name=\"Units\">null</att>\n" + //
            "        <att name=\"Westernmost_Longitude\">null</att>\n" + //
            "    </addAttributes>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>time</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_CoordinateAxisType\">Time</att>\n" + //
            "            <att name=\"axis\">T</att>\n" + //
            "            <att name=\"standard_name\">time</att>\n" + //
            "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"long_name\">Time</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>Number_of_Lines</sourceName>     <!-- I change from lat, which is a dim but not a var -->\n"
            + //
            "        <destinationName>latitude</destinationName> \n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Latitude</att>\n" + //
            "            <att name=\"standard_name\">latitude</att>\n" + //
            "            <att name=\"units\">degrees_north</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>Number_of_Columns</sourceName>   <!-- I change from lon, which is a dim but not a var  -->\n"
            + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"long_name\">Longitude</att>\n" + //
            "            <att name=\"standard_name\">longitude</att>\n" + //
            "            <att name=\"units\">degrees_east</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>l3m_data</sourceName>\n" + //
            "        <destinationName>sst</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" + //
            "            <att name=\"coordinates\">time Number_of_Lines Number_of_Columns lat lon</att>\n" + //
            "            <att name=\"long_name\">l3m_data</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"coordinates\">null</att>\n" + //
            "            <att name=\"ioos_category\">Temperature</att>\n" + //
            "            <att name=\"long_name\">Sea Surface Temperature</att>\n" + //
            "            <att name=\"standard_name\">sea_surface_temperature</att>\n" + //
            "            <att name=\"units\">deg_C</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>l3m_qual</sourceName>\n" + //
            "        <destinationName>sst_quality</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"coordinates\">time Number_of_Lines Number_of_Columns lat lon</att>\n" + //
            "            <att name=\"long_name\">l3m_qual</att>\n" + //
            "            <att name=\"valid_range\" type=\"floatList\">-2.0 -1.9985657</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"coordinates\">null</att>\n" + //
            "            <att name=\"ioos_category\">Quality</att>\n" + //
            "            <att name=\"long_name\">Sea Surface Temperature Quality</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD gettestSuperPreciseTimeUnits() throws Throwable {
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDGridFromNcFilesUnpacked\" datasetID=\"testSuperPreciseTimeUnits\" active=\"true\">\n" + //
            "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" + //
            "    <updateEveryNMillis>10000</updateEveryNMillis>\n" + //
            "    <fileDir>"
            + Path.of(EDDTestDataset.class.getResource("/largeFiles/nc/").toURI()).toString()
            + "</fileDir>\n" + //
            "    <fileNameRegex>superPreciseTimeUnits.nc</fileNameRegex>\n" + //
            "    <recursive>true</recursive>\n" + //
            "    <pathRegex>.*</pathRegex>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <matchAxisNDigits>20</matchAxisNDigits>\n" + //
            "    <fileTableInMemory>false</fileTableInMemory>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "        <att name=\"comment\">This Level 3 gridded product combines all 8 x 4 = 32 wind speed and mean square slope (MSS) measurements made by the CYGNSS constellation each second, uniformly sampled in latitude, longitude and time.</att>\n"
            + //
            "        <att name=\"Conventions\">CF-1.6, ACDD-1.3, ISO-8601</att>\n" + //
            "        <att name=\"creator_name\">CYGNSS Science Operations Center</att>\n" + //
            "        <att name=\"creator_type\">institution</att>\n" + //
            "        <att name=\"date_created\">2020-04-28T17:49:27Z</att>\n" + //
            "        <att name=\"date_issued\">2020-04-28T17:49:27Z</att>\n" + //
            "        <att name=\"geospatial_lat_max\">39.9N</att>\n" + //
            "        <att name=\"geospatial_lat_min\">-39.9N</att>\n" + //
            "        <att name=\"geospatial_lon_max\">359.9E</att>\n" + //
            "        <att name=\"geospatial_lon_min\">0.1E</att>\n" + //
            "        <att name=\"history\">Tue Apr 28 17:49:28 2020: ncks -O -L1 -a /tmp/qt_temp.J61461 /tmp/qt_temp.T61461\n"
            + //
            "/data/ops/op_cdr_1_0/apps/src/produce-L3-files/produce-L3-files - - dstore production_1@cygnss-data-1.engin.umich.edu - - day 2020-01-01</att>\n"
            + //
            "        <att name=\"id\">PODAAC-CYGNS-L3C10</att>\n" + //
            "        <att name=\"institution\">University of Michigan Space Physics Research Lab (SPRL)</att>\n" + //
            "        <att name=\"l3_algorithm_version\">cdr-v1.0</att>\n" + //
            "        <att name=\"NCO\">4.4.4</att>\n" + //
            "        <att name=\"netcdf_version_id\">4.3.3.1 of Dec 10 2015 16:44:18 $</att>\n" + //
            "        <att name=\"platform\">Observatory References: cyg1, cyg2, cyg3, cyg4, cyg5, cyg6, cyg7, cyg8</att>\n"
            + //
            "        <att name=\"processing_level\">3</att>\n" + //
            "        <att name=\"program\">CYGNSS</att>\n" + //
            "        <att name=\"project\">CYGNSS</att>\n" + //
            "        <att name=\"publisher_email\">podaac@podaac.jpl.nasa.gov</att>\n" + //
            "        <att name=\"publisher_name\">PO.DAAC</att>\n" + //
            "        <att name=\"publisher_url\">&#x200b;http://podaac.jpl.nasa.gov</att>\n" + //
            "        <att name=\"references\">Ruf, C., P. Chang, M.P. Clarizia, S. Gleason, Z. Jelenak, J. Murray, M. Morris, S. Musko, D. Posselt, D. Provost, D. Starkenburg, V. Zavorotny, CYGNSS Handbook, Ann Arbor, MI, Michigan Pub., ISBN 978-1-60785-380-0, 154 pp, 1 Apr 2016. http://clasp-research.engin.umich.edu/missions/cygnss/reference/cygnss-mission/CYGNSS_Handbook_April2016.pdf\n"
            + //
            "Global Modeling and Assimilation Office (GMAO) (2015), MERRA-2 inst1_2d_asm_Nx: 2d,1-Hourly,Instantaneous,Single-Level,Assimilation,Single-Level Diagnostics V5.12.4, Greenbelt, MD, USA, Goddard Earth Sciences Data and Information Services Center (GES DISC), Accessed: {dates differ for each L1 file. See &#39;source&#39; L1 files for exact timestamps}, https://doi.org/10.5067/3Z173KIE2TPD</att>\n"
            + //
            "        <att name=\"sensor\">Delay Doppler Mapping Instrument (DDMI)</att>\n" + //
            "        <att name=\"ShortName\">CYGNSS_L3_CDR_V1.0</att>\n" + //
            "        <att name=\"source\">cyg.ddmi.s20200101-000000-e20200101-235959.l2.wind-mss-cdr.a10.d10.nc</att>\n"
            + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v30</att>\n" + //
            "        <att name=\"summary\">CYGNSS is a NASA Earth Venture mission, managed by the Earth System Science Pathfinder Program. The mission consists of a constellation of eight small satellites. The eight observatories comprise a constellation that measures the ocean surface wind field with very high temporal resolution and spatial coverage, under all precipitating conditions, and over the full dynamic range of wind speeds experienced in a tropical cyclone. The CYGNSS observatories fly in 510 km circular orbits at a common inclination of 35&#xb0;. Each observatory includes a Delay Doppler Mapping Instrument (DDMI) consisting of a modified GPS receiver capable of measuring surface scattering, a low gain zenith antenna for measurement of the direct GPS signal, and two high gain nadir antennas for measurement of the weaker scattered signal. Each DDMI is capable of measuring 4 simultaneous bi-static reflections, resulting in a total of 32 wind measurements per second by the full constellation.</att>\n"
            + //
            "        <att name=\"time_coverage_duration\">P1DT00H00M00S</att>\n" + //
            "        <att name=\"time_coverage_end\">2020-01-01T23:30:00Z</att>\n" + //
            "        <att name=\"time_coverage_resolution\">P0DT1H0M0S</att>\n" + //
            "        <att name=\"time_coverage_start\">2020-01-01T00:30:00Z</att>\n" + //
            "        <att name=\"title\">CYGNSS Level 3 Climate Data Record Version 1.0</att>\n" + //
            "        <att name=\"version_id\">1.0</att>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"cdm_data_type\">Grid</att>\n" + //
            "        <att name=\"Conventions\">CF-1.6, ACDD-1.3, ISO-8601, COARDS</att>\n" + //
            "        <att name=\"infoUrl\">???</att>\n" + //
            "        <att name=\"institution\">SPRL</att>\n" + //
            "        <att name=\"keywords\">atmosphere, atmospheric, climate, cygnss, data, earth, Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Surface Winds, fetch, global, gps, level, limited, mean, mean_square_slope, mean_square_slope_uncertainty, merra, merra-2, merra2_wind_speed, num_merra2_wind_speed_samples, num_mss_samples, num_wind_speed_samples, num_yslf_wind_speed_samples, number, positioning, quality, record, reference, samples, science, sea, slope, speed, sprl, square, statistics, surface, system, time, uncertainty, version, wind, wind_speed, wind_speed_uncertainty, winds, young, yslf, yslf_wind_speed, yslf_wind_speed_uncertainty</att>\n"
            + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"netcdf_version_id\">null</att>\n" + //
            "        <att name=\"publisher_type\">institution</att>\n" + //
            "        <att name=\"publisher_url\">&#x200b;https://podaac.jpl.nasa.gov</att>\n" + //
            "        <att name=\"summary\">CYGNSS is a NASA Earth Venture mission, managed by the Earth System Science Pathfinder Program. The mission consists of a constellation of eight small satellites. The eight observatories comprise a constellation that measures the ocean surface wind field with very high temporal resolution and spatial coverage, under all precipitating conditions, and over the full dynamic range of wind speeds experienced in a tropical cyclone. The CYGNSS observatories fly in 510 km circular orbits at a common inclination of 35&#xb0;. Each observatory includes a Delay Doppler Mapping Instrument (DDMI) consisting of a modified Global Positioning System (GPS) receiver capable of measuring surface scattering, a low gain zenith antenna for measurement of the direct GPS signal, and two high gain nadir antennas for measurement of the weaker scattered signal. Each DDMI is capable of measuring 4 simultaneous bi-static reflections, resulting in a total of 32 wind measurements per second by the full constellation.</att>\n"
            + //
            "    </addAttributes>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>time</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"int\">24</att>\n" + //
            "            <att name=\"calendar\">gregorian</att>\n" + //
            "            <att name=\"comment\">Timestamp coordinate at the center of the 1 hr bin, at 1 hour resolution. Range is one UTC day.</att>\n"
            + //
            "            <att name=\"long_name\">Reference time of file</att>\n" + //
            "            <att name=\"standard_name\">time</att>\n" + //
            "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>lat</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"int\">400</att>\n" + //
            "            <att name=\"comment\">Latitude coordinate at the center of the 0.2 degree bin, degrees_north, at 0.2 degree resolution. Range is -39.9 .. 39.9.</att>\n"
            + //
            "            <att name=\"long_name\">Latitude</att>\n" + //
            "            <att name=\"standard_name\">latitude</att>\n" + //
            "            <att name=\"units\">degrees_north</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>lon</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"int\">1800</att>\n" + //
            "            <att name=\"comment\">Longitude coordinate at the center of the 0.2 degree bin, degrees_east, at 0.2 degree resolution. Range is 0.1 .. 359.9.</att>\n"
            + //
            "            <att name=\"long_name\">Longitude</att>\n" + //
            "            <att name=\"standard_name\">longitude</att>\n" + //
            "            <att name=\"units\">degrees_east</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>wind_speed</sourceName>\n" + //
            "        <destinationName>wind_speed</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">8 134 600</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" + //
            "            <att name=\"comment\">Minimum variance estimate of the mean wind speed in the bin over the spatial and temporal intervals specified by the bin&#39;s boundaries. This is done using an inverse-variance weighted average of all L2 samples of the wind speed that were made within the bin.</att>\n"
            + //
            "            <att name=\"long_name\">Wind speed</att>\n" + //
            "            <att name=\"standard_name\">wind_speed</att>\n" + //
            "            <att name=\"units\">m s-1</att>\n" + //
            "            <att name=\"valid_range\" type=\"doubleList\">-5.0 100.0</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>wind_speed_uncertainty</sourceName>\n" + //
            "        <destinationName>wind_speed_uncertainty</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">8 134 600</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" + //
            "            <att name=\"comment\">Standard deviation of the error in the mean of all L2 samples of the wind speed within the bin.</att>\n"
            + //
            "            <att name=\"long_name\">Wind speed uncertainty</att>\n" + //
            "            <att name=\"units\">m s-1</att>\n" + //
            "            <att name=\"valid_range\" type=\"doubleList\">0.0 10.0</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Quality</att>\n" + //
            "            <att name=\"standard_name\">wind_speed</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>num_wind_speed_samples</sourceName>\n" + //
            "        <destinationName>num_wind_speed_samples</destinationName>\n" + //
            "        <dataType>int</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">8 134 600</att>\n" + //
            "            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n" + //
            "            <att name=\"comment\">The number of L2 wind speed samples used to calculate wind_speed.</att>\n"
            + //
            "            <att name=\"long_name\">Number of wind speed samples</att>\n" + //
            "            <att name=\"units\">1</att>\n" + //
            "            <att name=\"valid_range\" type=\"intList\">1 100000</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Statistics</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>yslf_wind_speed</sourceName>\n" + //
            "        <destinationName>yslf_wind_speed</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">8 134 600</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" + //
            "            <att name=\"comment\">Minimum variance estimate of the young sea limited fetch mean wind speed in the bin over the spatial and temporal intervals specified by the bin&#39;s boundaries. This is done using an inverse-variance weighted average of all L2 samples of the wind speed that were made within the bin.</att>\n"
            + //
            "            <att name=\"long_name\">Young sea limited fetch wind speed</att>\n" + //
            "            <att name=\"standard_name\">yslf_wind_speed</att>\n" + //
            "            <att name=\"units\">m s-1</att>\n" + //
            "            <att name=\"valid_range\" type=\"doubleList\">-5.0 100.0</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">-50.0</att>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>yslf_wind_speed_uncertainty</sourceName>\n" + //
            "        <destinationName>yslf_wind_speed_uncertainty</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">8 134 600</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" + //
            "            <att name=\"comment\">Standard deviation of the error in the mean of all L2 samples of the young sea limited fetch wind speed within the bin.</att>\n"
            + //
            "            <att name=\"long_name\">Young sea limited fetch wind speed uncertainty</att>\n" + //
            "            <att name=\"units\">m s-1</att>\n" + //
            "            <att name=\"valid_range\" type=\"doubleList\">0.0 10.0</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Quality</att>\n" + //
            "            <att name=\"standard_name\">wind_speed</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>num_yslf_wind_speed_samples</sourceName>\n" + //
            "        <destinationName>num_yslf_wind_speed_samples</destinationName>\n" + //
            "        <dataType>int</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">8 134 600</att>\n" + //
            "            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n" + //
            "            <att name=\"comment\">The number of L2 young sea limited fetch wind speed samples used to calculate yslf_wind_speed.</att>\n"
            + //
            "            <att name=\"long_name\">Number of young sea limited fetch wind speed samples</att>\n" + //
            "            <att name=\"units\">1</att>\n" + //
            "            <att name=\"valid_range\" type=\"intList\">1 100000</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Statistics</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>mean_square_slope</sourceName>\n" + //
            "        <destinationName>mean_square_slope</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">8 134 600</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" + //
            "            <att name=\"comment\">Mean MSS in the bin over the spatial and temporal intervals specified by the bin&#39;s boundaries.</att>\n"
            + //
            "            <att name=\"long_name\">Mean square slope</att>\n" + //
            "            <att name=\"units\">1</att>\n" + //
            "            <att name=\"valid_range\" type=\"doubleList\">0.0 0.04</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">0.05</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Statistics</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>mean_square_slope_uncertainty</sourceName>\n" + //
            "        <destinationName>mean_square_slope_uncertainty</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">8 134 600</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" + //
            "            <att name=\"comment\">Standard deviation of the error in the mean of all L2 samples of the MSS within the bin.</att>\n"
            + //
            "            <att name=\"long_name\">Mean square slope uncertainty</att>\n" + //
            "            <att name=\"units\">1</att>\n" + //
            "            <att name=\"valid_range\" type=\"doubleList\">0.0 0.08</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">0.1</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Quality</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>num_mss_samples</sourceName>\n" + //
            "        <destinationName>num_mss_samples</destinationName>\n" + //
            "        <dataType>int</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">8 134 600</att>\n" + //
            "            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n" + //
            "            <att name=\"comment\">The number of L2 MSS samples used to calculate mean_square_slope.</att>\n"
            + //
            "            <att name=\"long_name\">Number of mean square slope samples</att>\n" + //
            "            <att name=\"units\">1</att>\n" + //
            "            <att name=\"valid_range\" type=\"intList\">1 100000</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Statistics</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>merra2_wind_speed</sourceName>\n" + //
            "        <destinationName>merra2_wind_speed</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">8 134 600</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" + //
            "            <att name=\"comment\">Mean MERRA-2 wind speed in the bin over the spatial and temporal intervals specified by the bin&#39;s boundaries. See https://disc.gsfc.nasa.gov/datasets/M2I1NXASM_5.12.4/summary?keywords=&#37;22MERRA-2&#37;22</att>\n"
            + //
            "            <att name=\"long_name\">MERRA-2 reference wind speed</att>\n" + //
            "            <att name=\"units\">m s-1</att>\n" + //
            "            <att name=\"valid_range\" type=\"doubleList\">0.0 100.0</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Wind</att>\n" + //
            "            <att name=\"standard_name\">wind_speed</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>num_merra2_wind_speed_samples</sourceName>\n" + //
            "        <destinationName>num_merra2_wind_speed_samples</destinationName>\n" + //
            "        <dataType>int</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSizes\" type=\"intList\">8 134 600</att>\n" + //
            "            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n" + //
            "            <att name=\"comment\">The number of L2 MERRA-2 wind speed samples used to calculate merra2_wind_speed.</att>\n"
            + //
            "            <att name=\"long_name\">Number of MERRA-2 wind speed samples</att>\n" + //
            "            <att name=\"units\">1</att>\n" + //
            "            <att name=\"valid_range\" type=\"intList\">1 100000</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" + //
            "            <att name=\"ioos_category\">Statistics</att>\n" + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }

  public static EDD geterdMPOC1day() throws Throwable {
    return EDD.oneFromXmlFragment(null,
        "<dataset type=\"EDDGridFromNcFilesUnpacked\" datasetID=\"erdMPOC1day\" active=\"true\">\n" + //
            "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" + //
            "    <updateEveryNMillis>10000</updateEveryNMillis>\n" + //
            "    <fileDir>"
            + Path.of(EDDTestDataset.class.getResource("/data/satellite/MPOC/1day/").toURI()).toString()
            + "</fileDir>\n" + //
            "    <recursive>true</recursive>\n" + //
            "    <fileNameRegex>.*_4km\\.nc</fileNameRegex>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <matchAxisNDigits>5</matchAxisNDigits>\n" + //
            "    <fileTableInMemory>false</fileTableInMemory>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "        <att name=\"_CoordSysBuilder\">ucar.nc2.dataset.conv.CF1Convention</att>\n" + //
            "        <att name=\"_lastModified\">2015-06-23T06:05:03.000Z</att>\n" + //
            "        <att name=\"cdm_data_type\">grid</att>\n" + //
            "        <att name=\"Conventions\">CF-1.6</att>\n" + //
            "        <att name=\"creator_email\">data@oceancolor.gsfc.nasa.gov</att>\n" + //
            "        <att name=\"creator_name\">NASA/GSFC/OBPG</att>\n" + //
            "        <att name=\"creator_url\">http://oceandata.sci.gsfc.nasa.gov</att>\n" + //
            "        <att name=\"data_bins\" type=\"int\">1920946</att>\n" + //
            "        <att name=\"data_maximum\" type=\"float\">2146.1846</att>\n" + //
            "        <att name=\"data_minimum\" type=\"float\">-2147.4836</att>\n" + //
            "        <att name=\"date_created\">2015-06-23T06:05:03.000Z</att>\n" + //
            "        <att name=\"easternmost_longitude\" type=\"float\">180.0</att>\n" + //
            "        <att name=\"end_orbit_number\" type=\"int\">3537</att>\n" + //
            "        <att name=\"geospatial_lat_max\" type=\"float\">90.0</att>\n" + //
            "        <att name=\"geospatial_lat_min\" type=\"float\">-90.0</att>\n" + //
            "        <att name=\"geospatial_lat_resolution\" type=\"float\">4.6</att>\n" + //
            "        <att name=\"geospatial_lat_units\">km</att>\n" + //
            "        <att name=\"geospatial_lon_max\" type=\"float\">180.0</att>\n" + //
            "        <att name=\"geospatial_lon_min\" type=\"float\">-180.0</att>\n" + //
            "        <att name=\"geospatial_lon_resolution\" type=\"float\">4.6</att>\n" + //
            "        <att name=\"geospatial_lon_units\">km</att>\n" + //
            "        <att name=\"grid_mapping_name\">latitude_longitude</att>\n" + //
            "        <att name=\"history\">smigen par=A2003001.L3m_DAY_POC_poc_4km.nc.param</att>\n" + //
            "        <att name=\"id\">A2003001.L3b_DAY_POC.nc/L3/A2003001.L3b_DAY_POC.nc</att>\n" + //
            "        <att name=\"identifier_product_doi\">10.5067/AQUA/MODIS_OC.2014.0</att>\n" + //
            "        <att name=\"identifier_product_doi_authority\">http://dx.doi.org</att>\n" + //
            "        <att name=\"institution\">NASA Goddard Space Flight Center, Ocean Ecology Laboratory, Ocean Biology Processing Group</att>\n"
            + //
            "        <att name=\"instrument\">MODIS</att>\n" + //
            "        <att name=\"keywords\">Oceans &gt; Ocean Chemistry &gt; Chlorophyll; Oceans &gt; Ocean Optics &gt; Ocean Color</att>\n"
            + //
            "        <att name=\"keywords_vocabulary\">NASA Global Change Master Directory (GCMD) Science Keywords</att>\n"
            + //
            "        <att name=\"l2_flag_names\">ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT</att>\n"
            + //
            "        <att name=\"latitude_step\" type=\"float\">0.041666668</att>\n" + //
            "        <att name=\"latitude_units\">degrees_north</att>\n" + //
            "        <att name=\"license\">http://science.nasa.gov/earth-science/earth-science-data/data-information-policy/</att>\n"
            + //
            "        <att name=\"longitude_step\" type=\"float\">0.041666668</att>\n" + //
            "        <att name=\"longitude_units\">degrees_east</att>\n" + //
            "        <att name=\"map_projection\">Equidistant Cylindrical</att>\n" + //
            "        <att name=\"measure\">Mean</att>\n" + //
            "        <att name=\"Metadata_Conventions\">Unidata Dataset Discovery v1.0</att>\n" + //
            "        <att name=\"naming_authority\">gov.nasa.gsfc.sci.oceandata</att>\n" + //
            "        <att name=\"northernmost_latitude\" type=\"float\">90.0</att>\n" + //
            "        <att name=\"number_of_columns\" type=\"int\">8640</att>\n" + //
            "        <att name=\"number_of_lines\" type=\"int\">4320</att>\n" + //
            "        <att name=\"platform\">Aqua</att>\n" + //
            "        <att name=\"processing_control_input_parameters_datamax\">1000.000000</att>\n" + //
            "        <att name=\"processing_control_input_parameters_datamin\">10.000000</att>\n" + //
            "        <att name=\"processing_control_input_parameters_deflate\">4</att>\n" + //
            "        <att name=\"processing_control_input_parameters_gap_fill\">0</att>\n" + //
            "        <att name=\"processing_control_input_parameters_ifile\">A2003001.L3b_DAY_POC.nc</att>\n" + //
            "        <att name=\"processing_control_input_parameters_latnorth\">90.000000</att>\n" + //
            "        <att name=\"processing_control_input_parameters_latsouth\">-90.000000</att>\n" + //
            "        <att name=\"processing_control_input_parameters_loneast\">180.000000</att>\n" + //
            "        <att name=\"processing_control_input_parameters_lonwest\">-180.000000</att>\n" + //
            "        <att name=\"processing_control_input_parameters_meas\">1</att>\n" + //
            "        <att name=\"processing_control_input_parameters_minobs\">0</att>\n" + //
            "        <att name=\"processing_control_input_parameters_ofile\">A2003001.L3m_DAY_POC_poc_4km.nc</att>\n" + //
            "        <att name=\"processing_control_input_parameters_oformat\">netCDF4</att>\n" + //
            "        <att name=\"processing_control_input_parameters_palfile\">/sdps/sdpsoper/Science/OCSSW/V2015.3/data/common/palette/default.pal</att>\n"
            + //
            "        <att name=\"processing_control_input_parameters_precision\">F</att>\n" + //
            "        <att name=\"processing_control_input_parameters_processing\">2014.0</att>\n" + //
            "        <att name=\"processing_control_input_parameters_prod\">poc</att>\n" + //
            "        <att name=\"processing_control_input_parameters_projection\">RECT</att>\n" + //
            "        <att name=\"processing_control_input_parameters_resolution\">4km</att>\n" + //
            "        <att name=\"processing_control_input_parameters_seam_lon\">-180.000000</att>\n" + //
            "        <att name=\"processing_control_input_parameters_stype\">2</att>\n" + //
            "        <att name=\"processing_control_l2_flag_names\">ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT</att>\n"
            + //
            "        <att name=\"processing_control_software_name\">smigen</att>\n" + //
            "        <att name=\"processing_control_software_version\">5.03</att>\n" + //
            "        <att name=\"processing_control_source\">A2003001.L3b_DAY_POC.nc</att>\n" + //
            "        <att name=\"processing_level\">L3 Mapped</att>\n" + //
            "        <att name=\"processing_version\">2014.0</att>\n" + //
            "        <att name=\"product_name\">A2003001.L3m_DAY_POC_poc_4km.nc</att>\n" + //
            "        <att name=\"project\">Ocean Biology Processing Group (NASA/GSFC/OBPG)</att>\n" + //
            "        <att name=\"publisher_email\">data@oceancolor.gsfc.nasa.gov</att>\n" + //
            "        <att name=\"publisher_name\">NASA/GSFC/OBPG</att>\n" + //
            "        <att name=\"publisher_url\">http://oceandata.sci.gsfc.nasa.gov</att>\n" + //
            "        <att name=\"southernmost_latitude\" type=\"float\">-90.0</att>\n" + //
            "        <att name=\"spatialResolution\">4.60 km</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">NetCDF Climate and Forecast (CF) Metadata Convention</att>\n"
            + //
            "        <att name=\"start_orbit_number\" type=\"int\">3521</att>\n" + //
            "        <att name=\"suggested_image_scaling_applied\">No</att>\n" + //
            "        <att name=\"suggested_image_scaling_maximum\" type=\"float\">1000.0</att>\n" + //
            "        <att name=\"suggested_image_scaling_minimum\" type=\"float\">10.0</att>\n" + //
            "        <att name=\"suggested_image_scaling_type\">LOG</att>\n" + //
            "        <att name=\"sw_point_latitude\" type=\"float\">-89.979164</att>\n" + //
            "        <att name=\"sw_point_longitude\" type=\"float\">-179.97917</att>\n" + //
            "        <att name=\"temporal_range\">day</att>\n" + //
            "        <att name=\"time_coverage_end\">2003-01-02T02:10:04.000Z</att>\n" + //
            "        <att name=\"time_coverage_start\">2002-12-31T23:50:06.000Z</att>\n" + //
            "        <att name=\"title\">MODIS Level-3 Standard Mapped Image</att>\n" + //
            "        <att name=\"westernmost_longitude\" type=\"float\">-180.0</att>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" + //
            "        <att name=\"creator_type\">group</att>\n" + //
            "        <att name=\"creator_url\">https://oceandata.sci.gsfc.nasa.gov</att>\n" + //
            "        <att name=\"data_bins\">null</att>\n" + //
            "        <att name=\"data_maximum\">null</att>\n" + //
            "        <att name=\"data_minimum\">null</att>\n" + //
            "        <att name=\"easternmost_longitude\">null</att>\n" + //
            "        <att name=\"end_orbit_number\">null</att>\n" + //
            "        <att name=\"history\">Datafiles are downloaded ASAP from https://oceandata.sci.gsfc.nasa.gov/MODIS-Aqua/L3SMI to NOAA NMFS SWFSC ERD.\n"
            + //
            "NOAA NMFS SWFSC ERD (erd.data@noaa.gov) uses ERDDAP to add the time variable and slightly modify the metadata.\n"
            + //
            "Direct read of HDF4 file through CDM library.</att>\n" + //
            "        <att name=\"id\">null</att>\n" + //
            "        <att name=\"identifier_product_doi_authority\">https://dx.doi.org</att>\n" + //
            "        <att name=\"infoUrl\">https://coastwatch.pfeg.noaa.gov/infog/MPOC_las.html</att>\n" + //
            "        <att name=\"institution\">NASA/GSFC OBPG</att>\n" + //
            "        <att name=\"keywords\">443/555, biology, carbon, center, chemistry, chlorophyll, color, concentration, data, ecology, flight, goddard, group, gsfc, image, imaging, L3, laboratory, level, level-3, mapped, moderate, modis, mole, mole_concentration_of_particulate_organic_carbon_in_sea_water, nasa, ocean, ocean color, oceans,\n"
            + //
            "Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Chlorophyll; Oceans &gt; Ocean Optics &gt; Ocean Color,\n"
            + //
            "optics, organic, particulate, poc, processing, resolution, sea, seawater, smi, space, spectroradiometer, standard, stramski, time, version, water</att>\n"
            + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"latitude_step\">null</att>\n" + //
            "        <att name=\"latitude_units\">null</att>\n" + //
            "        <att name=\"license\">https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\n"
            + //
            "[standard]</att>\n" + //
            "        <att name=\"longitude_step\">null</att>\n" + //
            "        <att name=\"longitude_units\">null</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" + //
            "        <att name=\"northernmost_latitude\">null</att>\n" + //
            "        <att name=\"number_of_columns\">null</att>\n" + //
            "        <att name=\"number_of_lines\">null</att>\n" + //
            "        <att name=\"product_name\">null</att>\n" + //
            "        <att name=\"publisher_email\">erd.data@noaa.gov</att>\n" + //
            "        <att name=\"publisher_name\">NOAA NMFS SWFSC ERD</att>\n" + //
            "        <att name=\"publisher_type\">institution</att>\n" + //
            "        <att name=\"publisher_url\">https://www.pfeg.noaa.gov</att>\n" + //
            "        <att name=\"southernmost_latitude\">null</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"start_orbit_number\">null</att>\n" + //
            "        <att name=\"suggested_image_scaling_applied\">null</att>\n" + //
            "        <att name=\"suggested_image_scaling_maximum\">null</att>\n" + //
            "        <att name=\"suggested_image_scaling_minimum\">null</att>\n" + //
            "        <att name=\"suggested_image_scaling_type\">null</att>\n" + //
            "        <att name=\"sw_point_latitude\">null</att>\n" + //
            "        <att name=\"sw_point_longitude\">null</att>\n" + //
            "        <att name=\"summary\">MODIS Aqua, Level-3 Standard Mapped Image (SMI), Global, 4km, Particulate Organic Carbon (POC) (1 Day Composite)</att>\n"
            + //
            "        <att name=\"testOutOfDate\">now-4days</att>\n" + //
            "        <att name=\"title\">MODIS Aqua, Level-3 SMI, Global, 4km, Particulate Organic Carbon, 2003-present (1 Day Composite)</att>\n"
            + //
            "        <att name=\"westernmost_longitude\">null</att>\n" + //
            "    </addAttributes>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>***fileName,timeFormat=yyyyDDD,A(\\d{7})\\.L3m_DAY_POC_poc_4km\\.nc,1</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Time</att>\n" + //
            "            <att name=\"long_name\">Centered Time</att>\n" + //
            "            <att name=\"standard_name\">time</att>\n" + //
            "            <att name=\"units\">seconds since 1970-01-01T12:00:00Z</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>lat</sourceName>\n" + //
            "        <destinationName>latitude</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_CoordinateAxisType\">Lat</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-32767.0</att>\n" + //
            "            <att name=\"long_name\">Latitude</att>\n" + //
            "            <att name=\"units\">degree_north</att>\n" + //
            "            <att name=\"valid_max\" type=\"float\">90.0</att>\n" + //
            "            <att name=\"valid_min\" type=\"float\">-90.0</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"standard_name\">latitude</att>\n" + //
            "            <att name=\"units\">degrees_north</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>lon</sourceName>\n" + //
            "        <destinationName>longitude</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_CoordinateAxisType\">Lon</att>\n" + //
            "            <att name=\"_FillValue\" type=\"float\">-32767.0</att>\n" + //
            "            <att name=\"long_name\">Longitude</att>\n" + //
            "            <att name=\"units\">degree_east</att>\n" + //
            "            <att name=\"valid_max\" type=\"float\">180.0</att>\n" + //
            "            <att name=\"valid_min\" type=\"float\">-180.0</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"ioos_category\">Location</att>\n" + //
            "            <att name=\"standard_name\">longitude</att>\n" + //
            "            <att name=\"units\">degrees_east</att>\n" + //
            "        </addAttributes>\n" + //
            "    </axisVariable>\n" + //
            "    <dataVariable>\n" + //
            "        <sourceName>poc</sourceName>\n" + //
            "        <destinationName>poc</destinationName>\n" + //
            "        <dataType>float</dataType>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSize\" type=\"intList\">64 64</att>\n" + //
            "            <att name=\"display_max\" type=\"double\">1000.0</att>\n" + //
            "            <att name=\"display_min\" type=\"double\">10.0</att>\n" + //
            "            <att name=\"display_scale\">log</att>\n" + //
            "            <att name=\"long_name\">Particulate Organic Carbon, D. Stramski, 2007 (443/555 version)</att>\n"
            + //
            "            <att name=\"reference\">Stramski, D., et al. &quot;Relationships between the surface concentration of particulate organic carbon and optical properties in the eastern South Pacific and eastern Atlantic Oceans.&quot; Biogeosciences 5.1 (2008): 171-201.</att>\n"
            + //
            "            <att name=\"standard_name\">mole_concentration_of_particulate_organic_carbon_in_sea_water</att>\n"
            + //
            "            <att name=\"units\">mg m^-3</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_ChunkSize\">null</att>\n" + //
            "            <att name=\"_ChunkSizes\">null</att>\n" + //
            "            <att name=\"colorBarMinimum\" type=\"double\">10</att>\n" + //
            "            <att name=\"colorBarMaximum\" type=\"double\">1000</att>\n" + //
            "            <att name=\"colorBarScale\">Log</att>\n" + //
            "            <att name=\"display_max\">null</att>\n" + //
            "            <att name=\"display_min\">null</att>\n" + //
            "            <att name=\"display_scale\">null</att>\n" + //
            "            <att name=\"ioos_category\">Ocean Color</att>\n" + //
            "            <att name=\"reference\">null</att>\n" + //
            "            <att name=\"references\">Stramski, D., et al. &quot;Relationships between the surface concentration of particulate organic carbon and optical properties in the eastern South Pacific and eastern Atlantic Oceans.&quot; Biogeosciences 5.1 (2008): 171-201.</att>\n"
            + //
            "        </addAttributes>\n" + //
            "    </dataVariable>\n" + //
            "</dataset>");
  }
}
