#! ruby

require 'numru/netcdf'
require_relative '../utils'

include NumRu

readme = \
"
"

nc = CFNetCDF.new(__FILE__, readme)
file = nc.netcdf_file
file.put_att("featureType","timeSeries")

s = 0  #UNLIMITED
o = 20
name = 50

station_dim = file.def_dim("station",s)
obs_dim = file.def_dim("obs",o)
name_dim = file.def_dim("name_strlen",name)


lat = file.def_var("lat","sfloat",[station_dim])
lat.put_att("units","degrees_north")
lat.put_att("long_name","station latitude")
lat.put_att("standard_name","latitude")

lon = file.def_var("lon","sfloat",[station_dim])
lon.put_att("units","degrees_east")
lon.put_att("long_name","station longitude")
lon.put_att("standard_name","longitude")

stationelev = file.def_var("station_elevation","sfloat",[station_dim])
stationelev.put_att("long_name","height above the geoid")
stationelev.put_att("standard_name","surface_altitude")
stationelev.put_att("units","m")

stationinfo = file.def_var("station_info","int",[station_dim])
stationinfo.put_att("long_name","station info")

stationname = file.def_var("station_name","char",[name_dim, station_dim])
stationname.put_att("cf_role", "timeseries_id")
stationname.put_att("long_name", "station name")

alt = file.def_var("alt","sfloat",[station_dim])
alt.put_att("long_name","vertical disance above the surface")
alt.put_att("standard_name","height")
alt.put_att("units","m")
alt.put_att("positive","up")
alt.put_att("axis","Z")

time = file.def_var("time","int",[obs_dim, station_dim])
time.put_att("long_name","time of measurement")
time.put_att("standard_name","time")
time.put_att("units","seconds since 1990-01-01 00:00:00")
time.put_att("missing_value", -999.9,"int")

temp = file.def_var("temperature","sfloat",[obs_dim, station_dim])
temp.put_att("long_name","Air Temperature")
temp.put_att("standard_name","air_temperature")
temp.put_att("units","Celsius")
temp.put_att("coordinates", "time lat lon alt")
temp.put_att("missing_value",-999.9,"sfloat")

humi = file.def_var("humidity","sfloat",[obs_dim, station_dim])
humi.put_att("long_name","Humidity")
humi.put_att("standard_name","specific_humidity")
humi.put_att("units","Percent")
humi.put_att("coordinates", "time lat lon alt")
humi.put_att("missing_value",-999.9,"sfloat")

# Stop the definitions, lets write some data
file.enddef

s = 10

alt.put(NArray.float(s).random!(10), "start" => [0], "end" => [s-1])

lat.put(NArray.int(s).random!(180), "start" => [0], "end" => [s-1])
lon.put(NArray.int(s).random!(180), "start" => [0], "end" => [s-1])

blank = Array.new(name)
stats = (0..s-1).map{|n| ("Station-#{n}".split(//).map!{|d|d.ord} + blank)[0..name-1] }
stationname.put(stats, "start" => [0,0], "end" => [name-1,s-1])

stationinfo.put((0..s-1).map{|d|d})

stationelev.put(NArray.float(s).random!(5), "start" => [0], "end" => [s-1])

time.put(NArray.int(o).indgen!*3600, "start" => [0,0], "end" => [-1,0])
time.put(NArray.int(o-10).indgen!*3600, "start" => [0,1], "end" => [o-11,1])
time.put(NArray.int(o-5).indgen!*3600, "start" => [0,2], "end" => [o-6,2])
time.put(NArray.int(o-3).indgen!*3600, "start" => [0,3], "end" => [o-4,3])
time.put(NArray.int(o-16).indgen!*3600, "start" => [0,4], "end" => [o-17,4])
time.put(NArray.int(o).indgen!*3600, "start" => [0,5], "end" => [-1,5])
time.put(NArray.int(o).indgen!*3600, "start" => [0,6], "end" => [-1,6])
time.put(NArray.int(o-4).indgen!*3600, "start" => [0,7], "end" => [o-5,7])
time.put(NArray.int(o-10).indgen!*3600, "start" => [0,8], "end" => [o-11,8])
time.put(NArray.int(o-8).indgen!*3600, "start" => [0,9], "end" => [o-9,9])

temp.put(NArray.int(o).random!(40), "start" => [0,0], "end" => [-1,0])
temp.put(NArray.int(o-10).random!(40), "start" => [0,1], "end" => [o-11,1])
temp.put(NArray.int(o-5).random!(40), "start" => [0,2], "end" => [o-6,2])
temp.put(NArray.int(o-3).random!(40), "start" => [0,3], "end" => [o-4,3])
temp.put(NArray.int(o-16).random!(40), "start" => [0,4], "end" => [o-17,4])
temp.put(NArray.int(o).random!(40), "start" => [0,5], "end" => [-1,5])
temp.put(NArray.int(o).random!(40), "start" => [0,6], "end" => [-1,6])
temp.put(NArray.int(o-4).random!(40), "start" => [0,7], "end" => [o-5,7])
temp.put(NArray.int(o-10).random!(40), "start" => [0,8], "end" => [o-11,8])
temp.put(NArray.int(o-8).random!(40), "start" => [0,9], "end" => [o-9,9])

humi.put(NArray.int(o).random!(90), "start" => [0,0], "end" => [-1,0])
humi.put(NArray.int(o-10).random!(90), "start" => [0,1], "end" => [o-11,1])
humi.put(NArray.int(o-5).random!(90), "start" => [0,2], "end" => [o-6,2])
humi.put(NArray.int(o-3).random!(90), "start" => [0,3], "end" => [o-4,3])
humi.put(NArray.int(o-16).random!(90), "start" => [0,4], "end" => [o-17,4])
humi.put(NArray.int(o).random!(90), "start" => [0,5], "end" => [-1,5])
humi.put(NArray.int(o).random!(90), "start" => [0,6], "end" => [-1,6])
humi.put(NArray.int(o-4).random!(90), "start" => [0,7], "end" => [o-5,7])
humi.put(NArray.int(o-10).random!(90), "start" => [0,8], "end" => [o-11,8])
humi.put(NArray.int(o-8).random!(90), "start" => [0,9], "end" => [o-9,9])


file.close
nc.create_output
