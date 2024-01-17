#! ruby

require 'numru/netcdf'
require_relative '../utils'

include NumRu

readme = \
"
http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#idp8360656

There is no wind data for the last 100 profiles

There is no humidity data for the first 3 profiles

There is no temperature data for profiles 50-52 and 100-102

There is no data for profiles 20-24
"

nc = CFNetCDF.new(__FILE__, readme)
file = nc.netcdf_file
file.put_att("featureType","profile")

p = 142
z = 42

profile_dim = file.def_dim("profile",p)
z_dim = file.def_dim("alt",z)

lat = file.def_var("lat","sfloat",[profile_dim])
lat.put_att("units","degrees_north")
lat.put_att("long_name","station latitude")
lat.put_att("standard_name","latitude")

lon = file.def_var("lon","sfloat",[profile_dim])
lon.put_att("units","degrees_east")
lon.put_att("long_name","station longitude")
lon.put_att("standard_name","longitude")

profile = file.def_var("profile","int",[profile_dim])
profile.put_att("cf_role", "profile_id")

time = file.def_var("time","int",[profile_dim])
time.put_att("long_name","time")
time.put_att("standard_name","time")
time.put_att("units","seconds since 1990-01-01 00:00:00")
time.put_att("missing_value",-999,"int")

alt = file.def_var("alt","sfloat",[z_dim, profile_dim])
alt.put_att("units","m")
alt.put_att("standard_name","altitude")
alt.put_att("long_name","height above mean sea level")
alt.put_att("positive","up")
alt.put_att("axis","Z")
alt.put_att("missing_value",-999.9,"sfloat")
	
temp = file.def_var("temperature","sfloat",[z_dim, profile_dim])
temp.put_att("long_name","Air Temperature")
temp.put_att("units","Celsius")
temp.put_att("coordinates", "time lat lon alt")
temp.put_att("missing_value",-999.9,"sfloat")

humi = file.def_var("humidity","sfloat",[z_dim, profile_dim])
humi.put_att("long_name","Humidity")
humi.put_att("standard_name","specific_humidity")
humi.put_att("units","Percent")
humi.put_att("coordinates", "time lat lon alt")
humi.put_att("missing_value",-999.9,"sfloat")

wind = file.def_var("wind_speed","sfloat",[z_dim, profile_dim])
wind.put_att("long_name","Wind Speed")
wind.put_att("standard_name","wind_speed")
wind.put_att("units","m/s")
wind.put_att("coordinates", "time lat lon alt")
wind.put_att("missing_value",-999.9,"sfloat")

# Stop the definitions, lets write some data
file.enddef

# Uniquely identifiying values for each profile.
# Just iterates by 1 (0,1,2,3, ... ,p)
profile.put(NArray.int(p).indgen!)

lat.put(NArray.int(p).random!(180))
lon.put(NArray.int(p).random!(180))
time.put(NArray.int(p).indgen!*3600)

alt.put(NArray.float(p,z).random!(10))
alt.put(NArray.float(z,5).fill!(-999.9), "start" => [0,20], "end" => [z-1,24])

temp.put(NArray.float(p,z).random!(40))
temp.put(NArray.float(z,3).fill!(-999.9), "start" => [0,50], "end" => [z-1,52])
temp.put(NArray.float(z,3).fill!(-999.9), "start" => [0,100], "end" => [z-1,102])
temp.put(NArray.float(z,5).fill!(-999.9), "start" => [0,20], "end" => [z-1,24])

humi.put(NArray.float(p,z).random!(90))
humi.put(NArray.float(z,3).fill!(-999.9), "start" => [0,0], "end" => [z-1,2])
humi.put(NArray.float(z,5).fill!(-999.9), "start" => [0,20], "end" => [z-1,24])

first = p - 100
second = p - first
wind.put(NArray.float(z,first).random!(90), "start" => [0,0], "end" => [z-1,first-1])
wind.put(NArray.float(z,second).fill!(-999.9), "start" => [0,first], "end" => [z-1,p-1])
wind.put(NArray.float(z,5).fill!(-999.9), "start" => [0,20], "end" => [z-1,24])

file.close
nc.create_output
