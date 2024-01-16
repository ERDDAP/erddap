#! ruby

require 'numru/netcdf'
require_relative '../utils'

include NumRu

readme = \
"
"

nc = CFNetCDF.new(__FILE__, readme)
file = nc.netcdf_file
file.put_att("featureType","profile")

p = 142
z = 42

profile_dim = file.def_dim("profile",p)
z_dim = file.def_dim("z",z)

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

alt = file.def_var("z","sfloat",[z_dim])
alt.put_att("units","m")
alt.put_att("standard_name","altitude")
alt.put_att("long_name","height above mean sea level")
alt.put_att("positive","up")
alt.put_att("axis","Z")
alt.put_att("missing_value",-999.9,"sfloat")
	
temp = file.def_var("temperature","sfloat",[z_dim, profile_dim])
temp.put_att("long_name","Water Temperature")
temp.put_att("units","Celsius")
temp.put_att("coordinates", "time lat lon z")
temp.put_att("missing_value",-999.9,"sfloat")

humi = file.def_var("humidity","sfloat",[z_dim, profile_dim])
humi.put_att("long_name","Humidity")
humi.put_att("standard_name","specific_humidity")
humi.put_att("units","Percent")
humi.put_att("coordinates", "time lat lon z")
humi.put_att("missing_value",-999.9,"sfloat")

# Stop the definitions, lets write some data
file.enddef

# Uniquely identifiying values for each profile.
# Just iterates by 1 (0,1,2,3, ... ,p)
profile.put(NArray.int(p).indgen!)

lat.put(NArray.int(p).random!(180))
lon.put(NArray.int(p).random!(180))
time.put(NArray.int(p).indgen!*3600)
alt.put(NArray.float(z).random!(10))

temp.put(NArray.float(p,z).random!(40))
humi.put(NArray.float(p,z).random!(90))

file.close
nc.create_output
