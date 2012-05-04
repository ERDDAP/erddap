% Lookup data identifier

function [data_description, data_units] = lookup_data_id(data_id)

data_id_table =...
    {'sstn', 'Sea Surface Temperature nightime', 'degrees Celsius';
    'sstd', 'Sea Surface Temperature daytime', 'degrees Celsius';
    'sstb', 'sea Surface Temperature asending orbits only', 'degrees Celsius';
    'sshd', 'Sea Surface Height Deviation', 'm';
    'sshl', 'Sea Surface Height (ssh deviation + levitus for water > 1000m depth)', 'm';
    'ach1', 'AVHRR Channel 1 Albedo', 'percent';
    'ach2', 'AVHRR Channel 2 Albedo', 'percent';
    'ach4', 'AVHRR Channel 4 Brightness temperature', 'degrees Celsius';
    'chla', 'Chlorophyll a pigment concentration', 'mg m^-3';
    'chl2', 'Chlorophyll a pigment concentration for MODIS (SeaWiFS Analog)', 'mg m^-3';
    'nNNN', 'Normalized water-leaving radiance at NNN nm wavelength', 'microWatts m^-2 nm^-1 sr^-1';
    'n412', 'Normalized water-leaving radiance at 412 nm wavelength', 'microWatts m^-2 nm^-1 sr^-1';
    'n443', 'Normalized water-leaving radiance at 443 nm wavelength', 'microWatts m^-2 nm^-1 sr^-1';
    'n490', 'Normalized water-leaving radiance at 490 nm wavelength', 'microWatts m^-2 nm^-1 sr^-1';
    'n510', 'Normalized water-leaving radiance at 510 nm wavelength', 'microWatts m^-2 nm^-1 sr^-1';
    'n555', 'Normalized water-leaving radiance at 555 nm wavelength', 'microWatts m^-2 nm^-1 sr^-1';
    'n670', 'Normalized water-leaving radiance at 670 nm wavelength', 'microWatts m^-2 nm^-1 sr^-1';
    't865', 'Atmospheric optical depth at 865 nm wavelength', 'm^-1';
    'k490', 'Diffuse attenuation coefficient at 490 nm wavelength', 'm^-1';
    'ssta', 'Sea Surface Temperature night and day', 'degrees Celsius';
    'curl', 'Curl of the Wind Stress', 'Pa m^-1';
    'divw', 'Divergence of the Wind', 's^-1';
    'ux10', 'Zonal wind', 'm s^-1';
    'uy10', 'Meridional wind', 'm s^-1';
    'umod', 'Wind Modulus', 'm s^-1';
    'taux', 'Zonal Wind stress', 'Pa'; 
    'tauy', 'Merdional Wind Stress', 'Pa';
    'tmod', 'Wind Stress Modulus', 'Pa';
    'cpig', 'CZCS total pigments', 'mg m^-3 (pigment + phaeopigments)';
    'ugeo', 'Zonal geostrophic current', 'm s^-1';
    'vgeo', 'Meridional geostrophic current', 'm s^-1';
    'uekm', 'Zonal Ekman current', 'm s^-1';
    'vekm', 'Meridional Ekman current', 'm s^-1';
    'wekm', 'Ekman Upwelling', 'm s^-1';
    'emod', 'Modulus of Ekman current', 'm s^-1';
    'swht', 'Significant Wave Heght', 'm';
    'tanm', 'Surface Temperature Anomaly', 'degrees Celsius';
    'canm', 'Surface Chlorophyll a Anomaly', 'mg m^-3';
    'hanm', 'Surface height anomaly', 'm';
    'wanm', 'Wind modulus anomaly', 'm ^-1';
    'crla', 'Wind stress curl anomaly', 'Pa m^-1';
    'usfc', 'Zonal component of surface current', 'm s^-1'
    'vsfc', 'Meriodnal component of surface current', 'm s^-1';
    'u24h', '25 hour running mean of zonal surface current', 'm s^-1';
    'v24h', '25 hour running mean of meridional surface current', 'm s^-1';
    't24h', '25 hour running mean of temperature', 'degrees Celsius'};

comparison_matrix = strcmp(data_id, data_id_table);
[a, b] = find(comparison_matrix);

if isempty(a) == 1
    data_description = 'unknown';
    data_units = 'unknown';
else
    data_description = data_id_table{a,2};
    data_units = data_id_table{a,3};
end