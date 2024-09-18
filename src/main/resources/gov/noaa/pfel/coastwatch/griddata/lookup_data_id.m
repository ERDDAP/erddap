% Lookup data identifier

function [data_description, data_units, did_frac_digits] = lookup_data_id(data_id)

data_id_table =...
    {'sstn', 'Sea Surface Temperature nightime', 'celsius', '';
    'sstd', 'Sea Surface Temperature daytime', 'celsius', '';
    'sstb', 'sea Surface Temperature asending orbits only', 'celsius', '';
    'sshd', 'Sea Surface Height Deviation', 'm', '';
    'sshl', 'Sea Surface Height (ssh deviation + levitus for water > 1000m depth)', 'm', '';
    'ach1', 'AVHRR Channel 1 Albedo', 'percent', '';
    'ach2', 'AVHRR Channel 2 Albedo', 'percent', '';
    'ach4', 'AVHRR Channel 4 Brightness temperature', 'celsius', '';
    'chla', 'Chlorophyll a pigment concentration', 'kg/m^3', '';
    'chl2', 'Chlorophyll a pigment concentration for MODIS (SeaWiFS Analog)', 'kg/m^3', '';
    'nNNN', 'Normalized water-leaving radiance at NNN nm wavelength', 'microWatts m^-2 nm^-1 sr^-1', '';
    'n412', 'Normalized water-leaving radiance at 412 nm wavelength', 'microWatts m^-2 nm^-1 sr^-1', '';
    'n443', 'Normalized water-leaving radiance at 443 nm wavelength', 'microWatts m^-2 nm^-1 sr^-1', '';
    'n490', 'Normalized water-leaving radiance at 490 nm wavelength', 'microWatts m^-2 nm^-1 sr^-1', '';
    'n510', 'Normalized water-leaving radiance at 510 nm wavelength', 'microWatts m^-2 nm^-1 sr^-1', '';
    'n555', 'Normalized water-leaving radiance at 555 nm wavelength', 'microWatts m^-2 nm^-1 sr^-1', '';
    'n670', 'Normalized water-leaving radiance at 670 nm wavelength', 'microWatts m^-2 nm^-1 sr^-1', '';
    't865', 'Atmospheric optical depth at 865 nm wavelength', 'm^-1', '';
    'k490', 'Diffuse attenuation coefficient at 490 nm wavelength', 'm^-1', '';
    'ssta', 'Sea Surface Temperature night and day', 'celsius', '';
    'curl', 'Curl of the Wind Stress', 'Pa/m', '2';
    'divw', 'Divergence of the Wind', 's^-1', '';
    'ux10', 'Zonal wind', 'm/s', '0';
    'uy10', 'Meridional wind', 'm/s', '0';
    'umod', 'Wind Modulus', 'm/s', '0';
    'taux', 'Zonal Wind stress', 'Pa', '2';
    'tauy', 'Merdional Wind Stress', 'Pa', '2';
    'tmod', 'Wind Stress Modulus', 'Pa', '2';
    'cpig', 'CZCS total pigments', 'mg/m^3 (pigment + phaeopigments)', '';
    'ugeo', 'Zonal geostrophic current', 'm/s', '';
    'vgeo', 'Meridional geostrophic current', 'm/s', '';
    'uekm', 'Zonal Ekman current', 'm/s', '';
    'vekm', 'Meridional Ekman current', 'm/s', '';
    'wekm', 'Ekman Upwelling', 'm/s', '';
    'emod', 'Modulus of Ekman current', 'm/s', '';
    'swht', 'Significant Wave Heght', 'm', '';
    'tanm', 'Surface Temperature Anomaly', 'celsius', '';
    'canm', 'Surface Chlorophyll a Anomaly', 'mg/m^3', '';
    'hanm', 'Surface height anomaly', 'm', '';
    'wanm', 'Wind modulus anomaly', 'm^-1', '';
    'crla', 'Wind stress curl anomaly', 'Pa/m', '';
    'usfc', 'Zonal component of surface current', 'm/s', '';
    'vsfc', 'Meriodnal component of surface current', 'm/s', '';
    'u24h', '25 hour running mean of zonal surface current', 'm/s', '';
    'v24h', '25 hour running mean of meridional surface current', 'm/s', '';
    't24h', '25 hour running mean of temperature', 'celsius', ''};

comparison_matrix = strcmp(data_id, data_id_table);
[a, b] = find(comparison_matrix);

if isempty(a) == 1
    data_description = 'unknown';
    data_units = 'unknown';
    did_frac_digits = 'unknown';
else
    data_description = data_id_table{a,2};
    data_units = data_id_table{a,3};
    did_frac_digits = data_id_table(a,4);
end