#! ruby

require 'numru/netcdf'
require_relative '../utils'

include NumRu

readme = \
"
"

nc = CFNetCDF.new(__FILE__, readme)
file = nc.netcdf_file
file.put_att("featureType","timeSeriesProfile")

s = 2
z = 4
t = 0 # UNLIMITED
name = 50

station_dim = file.def_dim("station",s)
z_dim = file.def_dim("z",z)
name_dim = file.def_dim("name_strlen",name)
time_dim = file.def_dim("time",t)

lat = file.def_var("lat","sfloat",[station_dim])
lat.put_att("units","degrees_north")
lat.put_att("long_name","station latitude")
lat.put_att("standard_name","latitude")

lon = file.def_var("lon","sfloat",[station_dim])
lon.put_att("units","degrees_east")
lon.put_att("long_name","station longitude")
lon.put_att("standard_name","longitude")

alt = file.def_var("alt","sfloat",[z_dim])
alt.put_att("units","m")
alt.put_att("standard_name","altitude")
alt.put_att("long_name","height below mean sea level")
alt.put_att("positive","down")
alt.put_att("axis","Z")

stationinfo = file.def_var("station_info","int",[station_dim])
stationinfo.put_att("long_name","station info")

stationname = file.def_var("station_name","char",[name_dim, station_dim])
stationname.put_att("cf_role", "timeseries_id")
stationname.put_att("long_name", "station name")

time = file.def_var("time","int",[time_dim])
time.put_att("long_name","time")
time.put_att("standard_name","time")
time.put_att("units","seconds since 1990-01-01 00:00:00")
time.put_att("missing_value",-999,"int")

temp = file.def_var("temperature","sfloat",[station_dim, z_dim, time_dim])
temp.put_att("long_name","Air Temperature")
temp.put_att("standard_name","air_temperature")
temp.put_att("units","Celsius")
temp.put_att("coordinates", "time lat lon alt")
temp.put_att("missing_value",-999.9,"sfloat")

humi = file.def_var("humidity","sfloat",[station_dim, z_dim, time_dim])
humi.put_att("long_name","Humidity")
humi.put_att("standard_name","specific_humidity")
humi.put_att("units","Percent")
humi.put_att("coordinates", "time lat lon alt")
humi.put_att("missing_value",-999.9,"sfloat")

# Stop the definitions, lets write some data
file.enddef

lat.put([37.5, 32.5])
lon.put([-76.5, -78.3])
stationinfo.put([0,1])

blank = Array.new(name)
name1 = ("Station1".split(//).map!{|d|d.ord} + blank)[0..name-1]
name2 = ("Station2".split(//).map!{|d|d.ord} + blank)[0..name-1]

stationname.put([[name1],[name2]])

time.put(NArray.int(100).indgen!*3600, "start" => [0], "end" => [99])
alt.put(NArray.float(z).indgen!*10)

temp.put(NArray.float(s,z,100).random!(40))
humi.put(NArray.float(s,z,100).random!(90))


file.close
nc.create_output