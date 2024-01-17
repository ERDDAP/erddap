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
p = 4
z = 30
name = 50
station_dim = file.def_dim("station",s)
profile_dim = file.def_dim("profile",p)
z_dim = file.def_dim("z",z)
name_dim = file.def_dim("name_strlen",name)

lat = file.def_var("lat","float",[station_dim])
lat.put_att("units","degrees_north")
lat.put_att("long_name","station latitude")
lat.put_att("standard_name","latitude")

lon = file.def_var("lon","float",[station_dim])
lon.put_att("units","degrees_east")
lon.put_att("long_name","station longitude")
lon.put_att("standard_name","longitude")

stationinfo = file.def_var("station_info","int",[station_dim])
stationinfo.put_att("long_name","station info")

stationname = file.def_var("station_name","char",[name_dim, station_dim])
stationname.put_att("cf_role", "timeseries_id")
stationname.put_att("long_name", "station name")

alt = file.def_var("alt","float",[z_dim, profile_dim, station_dim])
alt.put_att("standard_name","altitude")
alt.put_att("long_name", "height above mean sea level")
alt.put_att("units","m")
alt.put_att("positive","up")
alt.put_att("axis","Z")

time = file.def_var("time","int",[profile_dim, station_dim])
time.put_att("long_name","time")
time.put_att("standard_name","time")
time.put_att("units","seconds since 1990-01-01 00:00:00")
time.put_att("missing_value",-999,"int")

temp = file.def_var("temperature","sfloat",[z_dim, profile_dim, station_dim])
temp.put_att("long_name","Water Temperature")
temp.put_att("standard_name","sea_water_temperature")
temp.put_att("units","Celsius")
temp.put_att("coordinates", "time lat lon alt")
temp.put_att("missing_value",-999.9,"sfloat")

# Stop the definitions, lets write some data
file.enddef

lat.put([37.5, 32.5])
lon.put([-76.5, -78.3])
stationinfo.put([0,1])

blank = Array.new(name)
name1 = ("Station1".split(//).map!{|d|d.ord} + blank)[0..name-1]
name2 = ("Station2".split(//).map!{|d|d.ord} + blank)[0..name-1]

stationname.put([[name1],[name2]])

time.put( NArray.int(s*p).indgen!*3600)

alt.put(NArray.float(s*p*z).indgen!*2.5)

data = NArray.float(s,p,z).indgen!*0.1
temp.put(data)


file.close
nc.create_output