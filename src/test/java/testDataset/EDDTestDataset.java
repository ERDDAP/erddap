package testDataset;

import gov.noaa.pfel.erddap.dataset.EDD;

public class EDDTestDataset {
  public static EDD gethawaii_d90f_20ee_c4cb_LonPM180() throws Throwable {
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
            "aqua, chemistry, chlorophyll, chlorophyll-a, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, day, degrees, global, modis, noaa, npp, ocean, ocean color, oceans, quality, science, science quality, sea, seawater, water, wcn</att>\n" + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"title\">Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)</att>\n" + //
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
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDGridFromDap\" datasetID=\"hawaii_d90f_20ee_c4cb\" active=\"true\">\n" + //
    "    <sourceUrl>http://apdrc.soest.hawaii.edu/dods/public_data/SODA/soda_pop2.2.4</sourceUrl>\n" + //
    "    <accessibleViaWMS>false</accessibleViaWMS>\n" + //
    "    <reloadEveryNMinutes>15000</reloadEveryNMinutes>\n" + //
    "    <defaultDataQuery>temp[last][0][0:last][0:last],salt[last][0][0:last][0:last],u[last][0][0:last][0:last],v[last][0][0:last][0:last],w[last][0][0:last][0:last]</defaultDataQuery>\n" + //
    "    <defaultGraphQuery>temp[last][0][0:last][0:last]&amp;.draw=surface&amp;.vars=longitude|latitude|temp</defaultGraphQuery>\n" + //
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
    "circulation, currents, density, depths, eastward, eastward_sea_water_velocity, means, monthly, northward, northward_sea_water_velocity, ocean, oceans, pop, salinity, sea, sea_water_practical_salinity, sea_water_temperature, seawater, soda, tamu, temperature, umd, upward, upward_sea_water_velocity, velocity, water</att>\n" + //
    "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
    "        <att name=\"license\">[standard]</att>\n" + //
    "        <att name=\"Metadata_Conventions\">null</att>\n" + //
    "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
    "        <att name=\"summary\">Simple Ocean Data Assimilation (SODA) version 2.2.4 - A reanalysis of ocean \n" + //
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
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDGridFromDap\" datasetID=\"testActualRange2\" active=\"true\">\n" + //
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
            "        <att name=\"keywords\">25n, 25n-25s, 25s, clouds, data, days, earth, esrl, highly, hrc, hrc.nmissdays, laboratory, latitude, longitude, meteorology, missing, month, monthly, noaa, reflective, research, system, time</att>\n" + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"References\">null</att>\n" + //
            "        <att name=\"references\">https://www.esrl.noaa.gov/psd/data/gridded/data.noaa.hrc.html</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"summary\">NOAA Highly Reflective Clouds, 25N-25S (noaa hrc, hrc.nmissdays)</att>\n" + //
            "        <att name=\"title\">NOAA Highly Reflective Clouds, 25N-25S (noaa hrc, hrc.nmissdays), 1.0&#xb0;, 1971-1985</att>\n" + //
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
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDGridFromDap\" datasetID=\"hycom_GLBa008_tyx\" active=\"true\">\n" + //
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
            "air, atmosphere, atmospheric, bnd.layr.thickness, circulation, density, downward, elevation, flux, fresh, glba0.08, heat, heat flux, height, hycom, hydrology, into, laboratory, layer, level, mix.l., mix.layr.dens, mix.layr.saln, mix.layr.temp, mix.layr.thickness, mixed, mixed layer, mixl., naval, ocean, ocean_mixed_layer_thickness, oceanography, oceans, physical, physical oceanography, radiation, research, salinity, saln., sea, sea level, sea_surface_elevation, sea_water_practical_salinity, seawater, surf., surface, surface_downward_heat_flux_in_air, temp., temperature, thickness, topography, trend, u-velocity, v-velocity, velocity, water, water_flux_into_ocean</att>\n" + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"title\">HYCOM GLBa0.08 1/12 deg HYCOM + NCODA Global Hindcast Analysis (Combined), 2008-2018, [time][Y][X]</att>\n" + //
            "\n" + //
            "        <att name=\"creator_email\">George.Halliwell@noaa.gov</att>\n" + //
            "        <att name=\"creator_name\">Naval Research Laboratory, HYCOM</att>\n" + //
            "        <att name=\"creator_type\">institution</att>\n" + //
            "        <att name=\"infoUrl\">https://hycom.org/dataserver/glb-analysis/</att>\n" + //
            "        <att name=\"publisher_email\">hycomdata@coaps.fsu.edu</att>\n" + //
            "        <att name=\"publisher_name\">FSU COAPS</att>\n" + //
            "        <att name=\"publisher_url\">http://www.coaps.fsu.edu/</att>\n" + //
            "        <att name=\"summary\">HYbrid Coordinate Ocean Model (HYCOM) GLBa0.08 from Hycom.  The hybrid coordinate is one that is isopycnal in the open, stratified ocean, but smoothly reverts to a terrain-following coordinate in shallow coastal regions, and to z-level coordinates in the mixed layer and/or unstratified seas. The hybrid coordinate extends the geographic range of applicability of traditional isopycnic coordinate circulation models (the basis of the present hybrid code), such as the Miami Isopycnic Coordinate Ocean Model (MICOM) and the Navy Layered Ocean Model (NLOM), toward shallow coastal seas and unstratified parts of the world ocean.</att>\n" + //
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
            "blended, coastwatch, day, degrees, experimental, global, noaa, ocean, oceans, sea, sea_surface_temperature, sst, surface, temperature, wcn</att>\n" + //
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
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDGridFromDap\" datasetID=\"NCOM_Region7_2D\">\n" + //
            "     <sourceUrl>http://edac-dap.northerngulfinstitute.org/pydap/NCOM/region7/latest_ncom_glb_reg7.nc</sourceUrl>\n" + //
            "     <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" + //
            "     <addAttributes>\n" + //
            "         <att name=\"Conventions\">Global NCOM</att>\n" + //
            "         <att name=\"creator_email\">frank.bub@navy.mil</att>\n" + //
            "         <att name=\"creator_name\">Naval Research Lab (NRL)</att>\n" + //
            "         <att name=\"creator_url\">www7320.nrlssc.navy.mil/global_ncom/</att>\n" + //
            "         <att name=\"Metadata_Conventions\">null</att>\n" + //
            "         <att name=\"infoUrl\">http://edac-dap.northerngulfinstitute.org/dapnav/NCOM/region7/latest_ncom_glb_reg7.nc</att>\n" + //
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
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDGridFromDap\" datasetID=\"jplNesdisG17v271\" active=\"true\">\n" + //
            "    <sourceUrl>https://thredds.jpl.nasa.gov/thredds/dodsC/OceanTemperature/ABI_G17-STAR-L3C-v2.71.nc</sourceUrl>\n" + //
            "    <reloadEveryNMinutes>180</reloadEveryNMinutes>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "        <att name=\"acknowledgement\">Please acknowledge the use of these data with the following statement: These data were provided by Group for High Resolution Sea Surface Temperature (GHRSST) and the National Oceanic and Atmospheric Administration (NOAA).</att>\n" + //
            "        <att name=\"cdm_data_type\">grid</att>\n" + //
            "        <att name=\"col_count\" type=\"int\">18000</att>\n" + //
            "        <att name=\"col_start\" type=\"int\">0</att>\n" + //
            "        <att name=\"comment\">SSTs are a weighted average of the SSTs of contributing pixels. WARNING: some applications are unable to properly handle signed byte values. If byte values &gt; 127 are encountered, subtract 256 from this reported value.</att>\n" + //
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
            "        <att name=\"history\">Created by the L2-to-L3 conversion tool,  which was developed and provided by NOAA/NESDIS/STAR and CCNY. The version is 4.2.6</att>\n" + //
            "        <att name=\"id\">ABI_G17-STAR-L3C-v2.71</att>\n" + //
            "        <att name=\"institution\">NOAA/NESDIS/STAR</att>\n" + //
            "        <att name=\"keywords\">Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature</att>\n" + //
            "        <att name=\"keywords_vocabulary\">NASA Global Change Master Directory (GCMD) Science Keywords</att>\n" + //
            "        <att name=\"license\">GHRSST protocol describes data use as free and open</att>\n" + //
            "        <att name=\"Metadata_Conventions\">Unidata Dataset Discovery v1.0</att>\n" + //
            "        <att name=\"metadata_link\">https://podaac.jpl.nasa.gov/ws/metadata/dataset/?format=iso&amp;shortName=ABI_G17-STAR-L3C-v2.71</att>\n" + //
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
            "        <att name=\"references\">Data convention: GHRSST Data Specification (GDS) v2.0. Algorithms: ACSPO-ABI (NOAA/NESDIS/STAR)</att>\n" + //
            "        <att name=\"row_count\" type=\"int\">5920</att>\n" + //
            "        <att name=\"row_start\" type=\"int\">1540</att>\n" + //
            "        <att name=\"sensor\">ABI</att>\n" + //
            "        <att name=\"source\">l2p_source : 20200708020000-STAR-L2P_GHRSST-SSTsubskin-ABI_G17-ACSPO_V2.71-v02.0-fv01.0.nc</att>\n" + //
            "        <att name=\"southernmost_latitude\" type=\"float\">-81.15</att>\n" + //
            "        <att name=\"spatial_resolution\">0.02 deg</att>\n" + //
            "        <att name=\"sst_luts\">LUT_ABI_G17_L2C_DEPTH_DAYNIGHT_V01.00_20181002.txt</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table (v26, 08 November 2013)</att>\n" + //
            "        <att name=\"start_time\">20200708T020031Z</att>\n" + //
            "        <att name=\"stop_time\">20200708T020939Z</att>\n" + //
            "        <att name=\"Sub_Lon\" type=\"double\">-137.0</att>\n" + //
            "        <att name=\"summary\">Sea surface temperature retrievals produced by NOAA/NESDIS/STAR from the ABI sensor.</att>\n" + //
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
            "        <att name=\"keywords\">abi, angle, atmosphere, atmospheric, bias, contributing, data, deviation, difference, dt_analysis, earth, Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Surface Winds, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, environmental, error, estimate, flags, g17, g17-star-l3c-v2.71, information, l2p, l2p_flags, l2ps, l3c, latitude, level, longitude, national, nesdis, noaa, number, ocean, oceans, or_number_of_pixels, pixel, pixels, quality, quality_level, reference, satellite, satellite_zenith_angle, science, sea, sea_surface_subskin_temperature, sea_surface_temperature, sensor, service, single, skin, speed, sses, sses_bias, sses_standard_deviation, sst, sst_dtime, standard, star, statistics, sub, sub-skin, subskin, surface, temperature, time, v2.71, value, wind, wind_speed, winds, zenith</att>\n" + //
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
            "        <att name=\"summary\">Sea surface temperature and Wind Speed retrievals produced by NOAA/NESDIS/STAR from the ABI sensor on GOES-17.</att>\n" + //
            "        <att name=\"testOutOfDate\">now-3days</att>\n" + //
            "        <att name=\"time_coverage_end\">2020-07-08T02:09:39Z</att>\n" + //
            "        <att name=\"time_coverage_start\">2020-07-08T02:00:31Z</att>\n" + //
            "        <att name=\"title\">SST and Wind Speed, NOAA/NESDIS/STAR, ABI G17-STAR-L3C-v2.71, Pacific Ocean, 0.02&#xb0;, 2019-present, Hourly</att>\n" + //
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
            "            <att name=\"comment\">SST obtained by regression with buoy measurements, sensitive to skin SST. Further information at (Petrenko et al., JGR, 2014; doi:10.1002/2013JD020637)</att>\n" + //
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
            "            <att name=\"comment\">SST quality levels: 5 corresponds to &#xe2;&#x80;&#x9c;clear-sky&#xe2;&#x80;&#x9d; pixels and is recommended for operational applications and validation.</att>\n" + //
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
            "            <att name=\"comment\">L2P common flags in bits 1-6 and data provider flags (from ACSPO mask) in bits 9-16: bit01 (0=IR: 1=microwave); bit02 (0=ocean; 1=land); bit03 (0=no ice; 1=ice); bits04-07 (reserved,set to 0); bit08 (0=anti-solar; 1=solar); bit09 (0=radiance valid; 1=invalid); bit10 (0=night; 1=day); bit11 (0=ocean; 1=land); bit12 (0=good quality data; 1=degraded quality data due to &quot;twilight&quot; region); bit13 (0=no glint; 1=glint); bit14 (0=no snow/ice; 1=snow/ice); bits15-16 (00=clear; 01=probably clear; 10=cloudy; 11=clear-sky mask undefined)</att>\n" + //
            "            <att name=\"coordinates\">nj ni</att>\n" + //
            "            <att name=\"flag_masks\" type=\"shortList\">1 2 4 128 256 512 1024 2048 4096 8192 16384 -32768 -16384</att>\n" + //
            "            <att name=\"flag_meanings\">microwave land ice solar invalid day land twilight glint ice probably_clear cloudy mask_undefined</att>\n" + //
            "            <att name=\"grid_mapping\">crs</att>\n" + //
            "            <att name=\"long_name\">L2P flags</att>\n" + //
            "            <att name=\"valid_max\" type=\"short\">32767</att>\n" + //
            "            <att name=\"valid_min\" type=\"short\">-32768</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"ushort\">65535</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n" + //
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
            "            <att name=\"comment\">Original number of pixels from the L2Ps contributing to the SST value, not weighted</att>\n" + //
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
            "            <att name=\"comment\">Deviation from reference SST, i.e., dt_analysis = SST - reference SST</att>\n" + //
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
            "            <att name=\"comment\">Bias is derived against Piecewise Regression SST produced by local regressions with buoys. Subtracting sses_bias from sea_surface_temperature produces more accurate estimate of SST at the depth of buoys. Further information at (Petrenko et al., JTECH, 2016; doi:10.1175/JTECH-D-15-0166.1)</att>\n" + //
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
            "            <att name=\"comment\">Standard deviation of sea_surface_temperature from SST measured by drifting buoys. Further information at (Petrenko et al., JTECH, 2016; doi:10.1175/JTECH-D-15-0166.1)</att>\n" + //
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
            "            <att name=\"comment\">Typically represents surface winds (10 meters above the sea surface)</att>\n" + //
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
            "            <att name=\"comment\">time plus sst_dtime gives seconds since 1981-01-01 00:00:00 UTC</att>\n" + //
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
            "altitude, arc, atmosphere, bathymetry, coastal, earth science, hawaii, height, model, ngdc, noaa, oceans, relief, second, station, topography, vol.</att>\n" + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"license\">[standard]</att>\n" + //
            "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" + //
            "        <att name=\"references\">Divins, D.L., and D. Metzger, NGDC Coastal Relief Model, https://www.ngdc.noaa.gov/mgg/coastal/coastal.html</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"summary\">This Coastal Relief Gridded database provides the first comprehensive view of the US Coastal Zone; one that extends from the coastal state boundaries to as far offshore as the NOS hydrographic data will support a continuous view of the seafloor. In many cases, this seaward limit reaches out to, and in places even beyond the continental slope. The gridded database contains data for the entire coastal zone of the conterminous US, including Hawaii and Puerto Rico.</att>\n" + //
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
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDGridFromDap\" datasetID=\"mb7201adc\">\n" + //
            "    <sourceUrl>http://coast-enviro.er.usgs.gov/thredds/dodsC/DATAFILES/MYRTLEBEACH/7201adc-a.nc</sourceUrl>\n" + //
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
    return EDD.oneFromXmlFragment(null, "<dataset type=\"EDDGridFromNcFiles\" datasetID=\"nodcPH2sstd1day\" active=\"true\">\n" + //
            "    <reloadEveryNMinutes>10000</reloadEveryNMinutes>\n" + //
            "    <fileDir>D:/ERDDATA/u00/satellite/PH2/sstd/1day/</fileDir>\n" + // TODO: this needs to be a project relative path, but it contains large data files
            "    <fileNameRegex>\\d{14}-NODC.*\\.nc.ncml</fileNameRegex>\n" + //
            "    <accessibleViaFiles>false</accessibleViaFiles> <!-- because .ncml -->\n" + //
            "    <recursive>false</recursive>\n" + //
            "    <pathRegex>.*</pathRegex>\n" + //
            "    <metadataFrom>last</metadataFrom>\n" + //
            "    <matchAxisNDigits>20</matchAxisNDigits>\n" + //
            "    <fileTableInMemory>false</fileTableInMemory>\n" + //
            "    <!-- sourceAttributes>\n" + //
            "        <att name=\"acknowledgment\">Please acknowledge the use of these data with the following statement: These data were provided by GHRSST and the US National Oceanographic Data Center. This project was supported in part by a grant from the NOAA Climate Data Record (CDR) Program for satellites.</att>\n" + //
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
            "        <att name=\"history\">smigen_both ifile=2012365.b4kd3-pf5ap-n19-sst.hdf ofile=2012365.i4kd3-pf5ap-n19-sst.hdf prod=sst datamin=-3.0 datamax=40.0 precision=I projection=RECT resolution=4km gap_fill=2 ; ./hdf2nc_PFV52_L3C.x -v ./Data_PFV52/PFV52_HDF/2012/2012365.i4kd3-pf5ap-n19-sst.hdf</att>\n" + //
            "        <att name=\"id\">AVHRR_Pathfinder-NODC-L3C-v5.2</att>\n" + //
            "        <att name=\"institution\">NODC</att>\n" + //
            "        <att name=\"keywords\">Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature</att>\n" + //
            "        <att name=\"keywords_vocabulary\">NASA Global Change Master Directory (GCMD) Science Keywords</att>\n" + //
            "        <att name=\"license\">These data are available for use without restriction.</att>\n" + //
            "        <att name=\"Metadata_Conventions\">null</att>\n" + //
            "        <att name=\"metadata_link\">http://pathfinder.nodc.noaa.gov/ISO-AVHRR_Pathfinder-NODC-L3C-v5.2.html</att>\n" + //
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
            "        <att name=\"references\">http://pathfinder.nodc.noaa.gov and Casey, K.S., T.B. Brandon, P. Cornillon, and R. Evans: The Past, Present and Future of the AVHRR Pathfinder SST Program, in Oceanography from Space: Revisited, eds. V. Barale, J.F.R. Gower, and L. Alberotanza, Springer, 2010. DOI: 10.1007/978-90-481-8681-5_16.</att>\n" + //
            "        <att name=\"sensor\">AVHRR_GAC</att>\n" + //
            "        <att name=\"source\">AVHRR_GAC-CLASS-L1B-NOAA_19-v1</att>\n" + //
            "        <att name=\"southernmost_latitude\" type=\"double\">-90.0</att>\n" + //
            "        <att name=\"spatial_resolution\">0.0417 degree</att>\n" + //
            "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" + //
            "        <att name=\"start_time\">20121230T001833Z</att>\n" + //
            "        <att name=\"stop_time\">20121231T065419Z</att>\n" + //
            "        <att name=\"summary\">This netCDF-4 file contains sea surface temperature (SST) data produced as part of the AVHRR Pathfinder SST Project. These data were created using Version 5.2 of the Pathfinder algorithm and the file is nearly but not completely compliant with the GHRSST Data Specifications V2.0 (GDS2).  The file does not encode time according to the GDS2 specifications, and the sses_bias and sses_standard_deviation variables are empty.  Full compliance with GDS2 specifications will be achieved in the future Pathfinder Version 6. These data were created as a partnership between the University of Miami and the US NOAA/National Oceanographic Data Center (NODC).</att>\n" + //
            "        <att name=\"time_coverage_end\">20121231T065419Z</att>\n" + //
            "        <att name=\"time_coverage_start\">20121230T001833Z</att>\n" + //
            "        <att name=\"title\">AVHRR Pathfinder Version 5.2 L3-Collated (L3C) sea surface temperature</att>\n" + //
            "        <att name=\"uuid\">AAF19A5A-0E08-4812-9CAB-B7B9FCAAF26E</att>\n" + //
            "        <att name=\"westernmost_longitude\" type=\"double\">-180.0</att>\n" + //
            "    </sourceAttributes -->\n" + //
            "    <addAttributes>\n" + //
            "        <att name=\"acknowledgement\">Please acknowledge the use of these data with the following statement: These data were provided by GHRSST and the US National Oceanographic Data Center. This project was supported in part by a grant from the NOAA Climate Data Record (CDR) Program for satellites.</att>\n" + //
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
            "        <att name=\"history\">smigen_both ifile=2012365.b4kd3-pf5ap-n19-sst.hdf ofile=2012365.i4kd3-pf5ap-n19-sst.hdf prod=sst datamin=-3.0 datamax=40.0 precision=I projection=RECT resolution=4km gap_fill=2 ; ./hdf2nc_PFV52_L3C.x -v ./Data_PFV52/PFV52_HDF/2012/2012365.i4kd3-pf5ap-n19-sst.hdf\n" + //
            "2014-02-11 Files downloaded from https://data.nodc.noaa.gov/pathfinder/Version5.2 to NOAA NMFS SWFSC ERD by erd.data@noaa.gov . Aggregated with reference times from the dates in the file names. Removed other, misleading time values from the file names and the data files. Removed empty sses_bias and sses_standard_deviation variables. Removed aerosol_dynamic_indicator because of incorrect add_offset and scale_factor in files from 1981 through 2000.</att>\n" + //
            "        <att name=\"infoUrl\">https://www.nodc.noaa.gov/SatelliteData/pathfinder4km/</att>\n" + //
            "        <att name=\"institution\">NOAA NCEI</att>\n" + //
            "        <att name=\"keywords\">10m, aerosol, analysis, area, atmosphere,\n" + //
            "Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Surface Winds,\n" + //
            "atmospheric, avhrr, bias, climate, collated, cryosphere,\n" + //
            "Earth Science &gt; Cryosphere &gt; Sea Ice &gt; Ice Extent,\n" + //
            "data, deviation, difference, distribution, dynamic, estimate, extent, flag, flags, fraction, ice, ice distribution, indicator, l2p, l3-collated, l3c, last, measurement, noaa, nodc, ocean, oceans,\n" + //
            "Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature,\n" + //
            "Earth Science &gt; Oceans &gt; Sea Ice &gt; Ice Extent,\n" + //
            "pathfinder, quality, record, reference, sea, sea_ice_area_fraction, sea_surface_skin_temperature, sea_surface_temperature, skin, speed, sses, sst, standard, statistics, surface, temperature, time, version, wind, wind_speed, winds</att>\n" + //
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" + //
            "        <att name=\"license\">These data are available for use without restriction.  Please acknowledge the use of these data with the following statement: &quot;These data were provided by GHRSST and the US National Oceanographic Data Center. This project was supported in part by a grant from the NOAA Climate Data Record (CDR) Program for satellites.&quot; and cite the following publication:\n" + //
            "Casey, K.S., T.B. Brandon, P. Cornillon, and R. Evans (2010). &quot;The Past, Present and Future of the AVHRR Pathfinder SST Program&quot;, in Oceanography from Space: Revisited, eds. V. Barale, J.F.R. Gower, and L. Alberotanza, Springer. DOI: 10.1007/978-90-481-8681-5_16.\n" + //
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
            "        <att name=\"summary\">Daytime measurements from the Advanced Very High Resolution Radiometer (AVHRR) Pathfinder Version 5.2 L3-Collated (L3C) sea surface temperature. This netCDF-4 file contains sea surface temperature (SST) data produced as part of the AVHRR Pathfinder SST Project. These data were created using Version 5.2 of the Pathfinder algorithm and the file is nearly but not completely compliant with the Global High-Resolution Sea Surface Temperature (GHRSST) Data Specifications V2.0 (GDS2).  The file does not encode time according to the GDS2 specifications, and the sses_bias and sses_standard_deviation variables are empty.  Full compliance with GDS2 specifications will be achieved in the future Pathfinder Version 6. These data were created as a partnership between the University of Miami and the US NOAA/National Oceanographic Data Center (NODC).\n" + //
            "        \n" + //
            "2014-02-11 Files downloaded from https://data.nodc.noaa.gov/pathfinder/Version5.2 to NOAA NMFS SWFSC ERD by erd.data@noaa.gov . Aggregated with reference times from the dates in the file names. Removed other, misleading time values from the file names and the data files. Removed empty sses_bias and sses_standard_deviation variables. Removed aerosol_dynamic_indicator because of incorrect add_offset and scale_factor in files from 1981 through 2000.</att> \n" + //
            "        <att name=\"title\">SST, Pathfinder Ver 5.2 (L3C), Day, Global, 0.0417&deg;, 1981-2012, Science Quality (1 Day Composite)</att>\n" + //
            "        <att name=\"uuid\">null</att>\n" + //
            "    </addAttributes>\n" + //
            "    <axisVariable>\n" + //
            "        <sourceName>time</sourceName>\n" + //
            "        <destinationName>time</destinationName>\n" + //
            "        <!-- sourceAttributes>\n" + //
            "            <att name=\"_ChunkSize\" type=\"int\">1</att>\n" + //
            "            <att name=\"axis\">T</att>\n" + //
            "            <att name=\"calendar\">Gregorian</att>\n" + //
            "            <att name=\"comment\">This is the reference time of the SST file. Add sst_dtime to this value to get pixel-by-pixel times. Note: in PFV5.2 that sst_dtime is empty. PFV6 will contain the correct sst_dtime values.</att>\n" + //
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
            "      :comment = \"time plus sst_dtime gives seconds after 1981-01-01 00:00:00. Note: in PFV5.2 this sst_dtime i\n" + //
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
            "            <att name=\"comment\">Bias estimate derived using the techniques described at http://www.ghrsst.org/S\n" + //
            "SES-Description-of-schemes.html. Note: in PFV5.2 this sses_bias is empty. PFV6 will contain the correct sses_bi\n" + //
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
            "            <att name=\"comment\">Standard deviation estimate derived using the techniques described at http://ww\n" + //
            "w.ghrsst.org/SSES-Description-of-schemes.html. Note: in PFV5.2 this sses_standard_deviation is empty. PFV6 will\n" + //
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
            "            <att name=\"comment\">The difference between this SST and the previous day&#39;s SST.</att>\n" + //
            "            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" + //
            "            <att name=\"long_name\">Deviation from last SST analysis</att>\n" + //
            "            <att name=\"reference\">AVHRR_OI, with inland values populated from AVHRR_Pathfinder daily climatological SST. For more information on this reference field see http://accession.nodc.noaa.gov/0071180.</att>\n" + //
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
            "            <att name=\"references\">AVHRR_OI, with inland values populated from AVHRR_Pathfinder daily climatological SST. For more information on this reference field see https://data.nodc.noaa.gov/cgi-bin/iso?id=gov.noaa.nodc:0071180.</att>\n" + //
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
            "            <att name=\"comment\">These wind speeds were created by NCEP-DOE Atmospheric Model Intercomparison Project (AMIP-II) reanalysis (R-2) and represent winds at 10 metres above the sea surface.</att>\n" + //
            "            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" + //
            "            <att name=\"height\">10 m</att>\n" + //
            "            <att name=\"long_name\">10m wind speed</att>\n" + //
            "            <att name=\"scale_factor\" type=\"double\">1.0</att>\n" + //
            "            <att name=\"source\">NCEP/DOE AMIP-II Reanalysis (Reanalysis-2): u_wind.10m.gauss.2012.nc, v_wind.10m.gauss.2012.nc</att>\n" + //
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
            "            <att name=\"comment\">Sea ice concentration data are taken from the EUMETSAT Ocean and Sea Ice Satellite Application Facility (OSISAF) Global Daily Sea Ice Concentration Reprocessing Data Set (http://accession.nodc.noaa.gov/0068294) when these data are available. The data are reprojected and interpolated from their original polar stereographic projection at 10km spatial resolution to the 4km Pathfinder Version 5.2 grid. When the OSISAF data are not available for both hemispheres on a given day, the sea ice concentration data are taken from the sea_ice_fraction variable found in the L4 GHRSST DailyOI SST product from NOAA/NCDC, and are interpolated from the 25km DailyOI grid to the 4km Pathfinder Version 5.2 grid.</att>\n" + //
            "            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" + //
            "            <att name=\"long_name\">sea ice fraction</att>\n" + //
            "            <att name=\"reference\">Reynolds, et al.(2006) Daily High-resolution Blended Analyses. Available at ftp://eclipse.ncdc.noaa.gov/pub/OI-daily/daily-sst.pdf</att>\n" + //
            "            <att name=\"scale_factor\" type=\"double\">0.01</att>\n" + //
            "            <att name=\"source\">NOAA/NESDIS/NCDC Daily optimum interpolation(OI) SST on 1/4-degree grid: 20121230-NCDC-L4LRblend-GLOB-v01-fv02_0-AVHRR_OI.nc.gz</att>\n" + //
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
            "            <att name=\"comment\">Sea ice concentration data are taken from the EUMETSAT Ocean and Sea Ice Satellite Application Facility (OSISAF) Global Daily Sea Ice Concentration Reprocessing Data Set (https://data.nodc.noaa.gov/cgi-bin/iso?id=gov.noaa.nodc:0068294) when these data are available. The data are reprojected and interpolated from their original polar stereographic projection at 10km spatial resolution to the 4km Pathfinder Version 5.2 grid. When the OSISAF data are not available for both hemispheres on a given day, the sea ice concentration data are taken from the sea_ice_fraction variable found in the L4 GHRSST DailyOI SST product from NOAA/NCDC, and are interpolated from the 25km DailyOI grid to the 4km Pathfinder Version 5.2 grid.</att>\n" + //
            "            <att name=\"ioos_category\">Ice Distribution</att>\n" + //
            "            <att name=\"long_name\">Sea Ice Fraction</att>\n" + //
            "            <att name=\"units\">1</att>\n" + //
            "            <att name=\"reference\">null</att>\n" + //
            "            <att name=\"references\">Reynolds, et al.(2006) Daily High-resolution Blended Analyses. Available at https://journals.ametsoc.org/doi/full/10.1175/2007JCLI1824.1</att>\n" + //
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
            "            <att name=\"comment\">Aerosol optical thickness (100 KM) data are taken from the CLASS AERO100 products, which are created from AVHRR channel 1 optical thickness retrievals from AVHRR global area coverage (GAC) data. The aerosol optical thickness measurements are interpolated from their original 1 degree x 1 degree resolution to the 4km Pathfinder Version 5.2 grid.</att>\n" + //
            "            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" + //
            "            <att name=\"long_name\">aerosol dynamic indicator</att>\n" + //
            "            <att name=\"reference\">http://www.class.ncdc.noaa.gov/saa/products/search?sub_id=0&amp;datatype_family=AERO100&amp;submit.x=25&amp;submit.y=12</att>\n" + //
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
            "            <att name=\"references\">https://www.class.ncdc.noaa.gov/saa/products/search?sub_id=0&amp;datatype_family=AERO100&amp;submit.x=25&amp;submit.y=12</att>\n" + //
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
            "            <att name=\"comment\">Note, the native Pathfinder processing system returns quality levels ranging from 0 to 7 (7 is best quality; -1 represents missing data) and has been converted to the extent possible into the six levels required by the GDS2 (ranging from 0 to 5, where 5 is best). Below is the conversion table:\n" + //
            " GDS2 required quality_level 5  =  native Pathfinder quality level 7 == best_quality\n" + //
            " GDS2 required quality_level 4  =  native Pathfinder quality level 4-6 == acceptable_quality\n" + //
            " GDS2 required quality_level 3  =  native Pathfinder quality level 2-3 == low_quality\n" + //
            " GDS2 required quality_level 2  =  native Pathfinder quality level 1 == worst_quality\n" + //
            " GDS2 required quality_level 1  =  native Pathfinder quality level 0 = bad_data\n" + //
            " GDS2 required quality_level 0  =  native Pathfinder quality level -1 = missing_data\n" + //
            " The original Pathfinder quality level is recorded in the optional variable pathfinder_quality_level.</att>\n" + //
            "            <att name=\"flag_meanings\">bad_data worst_quality low_quality acceptable_quality best_quality</att>\n" + //
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
            "            <att name=\"comment\">This variable contains the native Pathfinder processing system quality levels, ranging from 0 to 7, where 0 is worst and 7 is best. And value -1 represents missing data.</att>\n" + //
            "            <att name=\"flag_meanings\">bad_data worst_quality low_quality low_quality acceptable_quality acceptable_quality acceptable_quality best_quality</att>\n" + //
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
            "            <att name=\"comment\">Bit zero (0) is always set to zero to indicate infrared data. Bit one (1) is set to zero for any pixel over water (ocean, lakes and rivers). Land pixels were determined by rasterizing the Global Self-consistent Hierarchical High-resolution Shoreline (GSHHS) Database from the NOAA National Geophysical Data Center. Any 4 km Pathfinder pixel whose area is 50&#37; or more covered by land has bit one (1) set to 1. Bit two (2) is set to 1 when the sea_ice_fraction is 0.15 or greater. Bits three (3) and four (4) indicate lake and river pixels, respectively, and were determined by rasterizing the US World Wildlife Fund&#39;s Global Lakes and Wetlands Database. Any 4 km Pathfinder pixel whose area is 50&#37; or more covered by lake has bit three (3) set to 1. Any 4 km Pathfinder pixel whose area is 50&#37; or more covered by river has bit four (4) set to 1.</att>\n" + //
            "            <att name=\"flag_masks\" type=\"shortList\">1 2 4 8 16 32 64 128 256</att>\n" + //
            "            <att name=\"flag_meanings\">microwave land ice lake river reserved_for_future_use unused_currently unused_currently unused_currently</att>\n" + //
            "            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" + //
            "            <att name=\"long_name\">L2P flags</att>\n" + //
            "        </sourceAttributes -->\n" + //
            "        <addAttributes>\n" + //
            "            <att name=\"_FillValue\" type=\"short\">32767</att> <!-- added by addFillValueAttributes at 2020-10-28T11:47:36 -->\n" + //
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
}
