%Lookup table for Data Source Code

function [satellite, sensor, origin] = lookup_data_source(data_code)

data_source_table =...
    {'AM', 'NOAA POES spacecraft', 'AVHRR MCSST 11km', 'NASA/JPL PODAAC';
    'E1', 'ERS-1 spacecraft', 'AMS scatterometer', 'ESA/IFREMER (France)';
    'E2', 'ERS-2 spacecraft', 'AMS scatterometer', 'ESA/IFREMER (France)';
    'TP', 'TOPEX/Poseidon spacecraft', 'Topex altimeter', 'NOAA/NESDIS/GRDL';
    'AG', 'NOAA POES spacecraft', 'AVHRR GAC SST', 'NOAA/NESDIS/OSDPD';
    'AH', 'NOAA POES spacecraft', 'AVHRR HRPT', 'NOAA/NESDIS/CoastWatch/HRCWN';
    'AS', 'NOAA POES spacecraft', 'AVHRR HRPT', 'NOAA/NESDIS/CoastWatch/HRCWN';
    'AP', 'Pathfinder SST', 'AVHRR', 'NASA|JPL';
    'SW', 'Orbview-2 spacecraft', 'SeaWiFS GAC', 'NASA/GSFC/DAAC';
    'O1', 'ADEOS spacecraft', 'OCTS GAC', 'NASDA (Japan)';
    'O2', 'ADEOS-II spacecraft', 'GLI', 'NASDA (Japan)';
    'GG', 'Geosat Geosat Mission', ' ', 'NOAA|NODC';
    'GE', 'Geosat Exact Reapeat Mission', ' ', 'NOAA|NODC';
    'GA', 'NOAA GOES spacecraft', 'GOES SST', 'NOAA|NESDIS';
    'GH', 'NOAA GOES spacecraft', 'Hourly GOES SST', 'NOAA|NESDIS';
    'O2', 'Adeos-II spacecraft', 'OCTS GAC', 'NASDA (Japan)';
    'QN', 'Quikscat spacecraft', 'SeaWinds Near real time', 'NASA/JPL';
    'QS', 'QuikSCAT spacecraft', 'Seawinds Science-quality', 'NASA/JPL';
    'OS', 'ADEOS-II spacecraft', 'SeaWinds', 'NOAA/NESDIS';
    'MT', 'Terra spacecraft', 'MODIS', 'NASA|GSFC';
    'MA', 'Aqua spacecraft', 'MODIS', 'NASA|GSFC' ;
    'JA', 'JASON spacecraft', 'Altimeter', 'NASA/JPL';
    'OC', 'Oceansat spacecraft', 'OCM', 'India';
    'TJ', 'Topex and Jason blended altimetry', ' ', 'NASA/JPL';
    'TA', 'Topex and other blended altimeter products', ' ', 'AVISO (fr.)';
    'QW', 'Quikscat mean wind fields', ' ', 'IFREMER (fr.)';
    'MO', 'Terra via direct broadcast', 'Near real time MODIS', 'Oregon State University';
    'MY', 'Aqua via direct broadcast', 'Near real time MODIS', 'Oregon State University';
    'SH', 'Orbview-2 spacecraft', 'SeaWiFS HRPT data', 'NOAA/NOS/CoastWatch/NASA/MBARI';
    'CM', 'CODAR data from Monterey Bay HF Radars', ' ', 'US Naval Postgraduate School';
    'ME', 'Terra Science-quality data', 'MODIS', 'NASA|GSFC|DAAC';
    'MH', 'Aqua Science-quality data', 'MODIS', 'NASA|GSFC|Oceancolor Web';
    'MW', 'Aqua Spacecraft', 'MODIS', 'NASA/GSFC';
    'AT', 'NOAA POES spacecraft', 'AVHRR HRPT 1.25 km SST', 'NWS/NESDIS';
    'PH', 'NOAA POES spacecraft', 'AVHRR', 'NOAA NODC'};

comparison_matrix = strcmp(data_code, data_source_table);
[a, b] = find(comparison_matrix);

if isempty(a) == 1
    satellite = 'unknown';
    sensor = 'unknown';
    origin = 'unknown';
else
    satellite = data_source_table{a,2};
    sensor = data_source_table{a,3};
    origin = data_source_table{a,4};
end
