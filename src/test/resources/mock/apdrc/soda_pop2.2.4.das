Attributes {
    NC_GLOBAL {
        String title "SODA v2.2.4 monthly means";
        String Conventions "COARDS", "GrADS";
        String dataType "Grid";
        String documentation "http://localhost:8089/datadoc/soda_2.2.4.php";
        String history "Fri Jan 30 13:16:24 HST 2026 : imported by GrADS Data Server 2.0";
    }
    lev {
        String units "meters";
        String positive "down";
        String name "Depth";
        String grads_dim "z";
        String grads_mapping "levels";
        String long_name "altitude";
        Float64 minimum 5.01000000000;
        Float64 maximum 5375.00000000000;
        Float32 resolution 137.69205;
    }
    lon {
        String grads_dim "x";
        String grads_mapping "linear";
        String grads_size "720";
        String units "degrees_east";
        String long_name "longitude";
        Float64 minimum 0.25000000000;
        Float64 maximum 359.75000000000;
        Float32 resolution 0.5;
    }
    lat {
        String grads_dim "y";
        String grads_mapping "linear";
        String grads_size "330";
        String units "degrees_north";
        String long_name "latitude";
        Float64 minimum -75.25000000000;
        Float64 maximum 89.25000000000;
        Float32 resolution 0.5;
    }
    time {
        String grads_dim "t";
        String grads_mapping "linear";
        String grads_size "1680";
        String grads_min "00z15jan1871";
        String grads_step "1mo";
        String units "days since 1-1-1 00:00:0.0";
        String long_name "time";
        String minimum "00z15jan1871";
        String maximum "00z15dec2010";
        Float32 resolution 30.43657;
    }
    temp {
        Float32 _FillValue -9.99E33;
        Float32 missing_value -9.99E33;
        String long_name "temperature [degc] ";
    }
    salt {
        Float32 _FillValue -9.99E33;
        Float32 missing_value -9.99E33;
        String long_name "salinity [psu] ";
    }
    u {
        Float32 _FillValue -9.99E33;
        Float32 missing_value -9.99E33;
        String long_name "zonal velocity [m/s] ";
    }
    v {
        Float32 _FillValue -9.99E33;
        Float32 missing_value -9.99E33;
        String long_name "meridional velocity [m/s] ";
    }
    w {
        Float32 _FillValue -9.99E33;
        Float32 missing_value -9.99E33;
        String long_name "vertical velocity [m/s] ";
    }
    taux {
        Float32 _FillValue -9.99E33;
        Float32 missing_value -9.99E33;
        String long_name "zonal wind stress [n/m^2] ";
    }
    tauy {
        Float32 _FillValue -9.99E33;
        Float32 missing_value -9.99E33;
        String long_name "meridional wind stress [n/m^2] ";
    }
    ssh {
        Float32 _FillValue -9.99E33;
        Float32 missing_value -9.99E33;
        String long_name "sea level [m] ";
    }
}