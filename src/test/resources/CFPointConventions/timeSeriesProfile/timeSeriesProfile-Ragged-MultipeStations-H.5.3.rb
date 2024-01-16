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
o = 12  #UNLIMITED
name = 50

station_dim = file.def_dim("station",s)
profile_dim = file.def_dim("profile",p)
obs_dim = file.def_dim("obs",o)
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

profile = file.def_var("profile","int",[profile_dim])
profile.put_att("cf_role", "profile_id")

time = file.def_var("time","int",[profile_dim])
time.put_att("long_name","time")
time.put_att("standard_name","time")
time.put_att("units","seconds since 1990-01-01 00:00:00")
time.put_att("missing_value",-999,"int")

stationindex = file.def_var("station_index","int",[profile_dim])
stationindex.put_att("long_name", "the station this profile is associated with")
stationindex.put_att("instance_dimension", "station")

rowsize = file.def_var("row_size","int",[profile_dim])
rowsize.put_att("long_name", "number of obs in this profile")
rowsize.put_att("sample_dimension", "obs")

height = file.def_var("height","float",[obs_dim])
height.put_att("long_name","height above sea surface")
height.put_att("standard_name","height")
height.put_att("units","meters")
height.put_att("axis","Z")
height.put_att("positive","up")

temp = file.def_var("temperature","sfloat",[obs_dim])
temp.put_att("standard_name","sea_water_temperature")
temp.put_att("long_name","Water Temperature")
temp.put_att("units","Celsius")
temp.put_att("coordinates", "time lat lon height")
temp.put_att("missing_value",-999.9,"sfloat")

# Stop the definitions, lets write some data
file.enddef

lat.put([37.5, 32.5])
lon.put([-76.5, -78.3])
stationinfo.put([0,1])
profile.put([0,1,2,3])
stationindex.put([0,1,0,1])
rowsize.put([2,4,2,4])

blank = Array.new(name)
name1 = ("Station1".split(//).map!{|d|d.ord} + blank)[0..name-1]
name2 = ("Station2".split(//).map!{|d|d.ord} + blank)[0..name-1]

stationname.put([[name1],[name2]])

time.put( NArray.int(p).indgen!*3600)

# row_size is 2,4,2,4
# station_index is 0,1,0,1
heights = [0.5,1.5]  + [0.8,1.8,2.8,3.8] + [0.5,1.5] + [0.8,1.8,2.8,3.8]
temp_data = [6.7,6.9]  + [7.6,7.7,7.9,8.0] + [6.7,7.0] + [8.2,7.7,7.8,9.1]
height.put(heights)
temp.put(temp_data)


file.close
nc.create_output