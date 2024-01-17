#! ruby

require 'numru/netcdf'
require_relative '../utils'

include NumRu

readme = \
"http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#idp8443504 *

This assumes that there are the same number of profiles for all
trajectories, and the same number of vertical level for every profile.

Use case:

Unlimited Dimension:
   Any of the three dimensions (profile, trajectory, or z) can be the
   NetCDF unlimited dimension if it might be useful to enlarge it.
   One must know the maximum size of the non-unlimited dimensions before file creation.
"

nc = CFNetCDF.new(__FILE__, readme)
file = nc.netcdf_file
file.put_att("featureType","trajectoryProfile")

t = 0 # UNLIMITED
p = 10
z = 8

trajectory_dim = file.def_dim("trajectory",t)
profile_dim = file.def_dim("profile",p)
z_dim = file.def_dim("z",z)

lat = file.def_var("lat","sfloat",[profile_dim, trajectory_dim])
lat.put_att("units","degrees_north")
lat.put_att("long_name","Latitude")
lat.put_att("standard_name","latitude")
lat.put_att("missing_value",-999.9,"sfloat")

lon = file.def_var("lon","sfloat",[profile_dim, trajectory_dim])
lon.put_att("units","degrees_east")
lon.put_att("long_name","Longitude")
lon.put_att("standard_name","longitude")
lon.put_att("missing_value",-999.9,"sfloat")

trajectory = file.def_var("trajectory","int",[trajectory_dim])
trajectory.put_att("cf_role","trajectory_id")

alt = file.def_var("alt","sfloat",[z_dim, profile_dim, trajectory_dim])
alt.put_att("standard_name","altitude")
alt.put_att("long_name", "height below mean sea level")
alt.put_att("units","m")
alt.put_att("positive","down")
alt.put_att("axis","Z")
alt.put_att("missing_value",-999.9,"sfloat")

time = file.def_var("time","int",[profile_dim, trajectory_dim])
time.put_att("long_name","time")
time.put_att("standard_name","time")
time.put_att("units","seconds since 1990-01-01 00:00:00")
time.put_att("missing_value",-999,"int")

temp = file.def_var("temperature","sfloat",[z_dim, profile_dim, trajectory_dim])
temp.put_att("long_name","Water Temperature")
temp.put_att("standard_name","air_temperature")
temp.put_att("units","Celsius")
temp.put_att("coordinates", "time lat lon alt")
temp.put_att("missing_value",-999.9,"sfloat")

salt = file.def_var("salinity","sfloat",[z_dim, profile_dim, trajectory_dim])
salt.put_att("long_name","Sea Water Salinity")
salt.put_att("standard_name","sea_water_salinity")
salt.put_att("units","PSU")
salt.put_att("coordinates", "time lat lon alt")
salt.put_att("missing_value",-999.9,"sfloat")

# Stop the definitions, lets write some data
file.enddef

num_t = 6

trajectory.put( NArray.int(num_t).indgen!, "start" => [0], "end" => [num_t - 1])

random = Random.new

# Iterate over each trajectory and generate a random number of profiles and depths
# for each
(0..num_t-1).each do |traj|
  ps = Random.rand(p-1) + 1
  dep = Random.rand(z-1) + 1

	lat.put(NArray.float(ps).random!(45), "start" => [0,traj], "end" => [ps-1,traj])
	lat.put(NArray.float(p-ps).fill(-999.9), "start" => [ps,traj], "end" => [p-1,traj])

  lon.put(NArray.float(ps).random!(-76), "start" => [0,traj], "end" => [ps-1,traj])
  lon.put(NArray.float(p-ps).fill(-999.9), "start" => [ps,traj], "end" => [p-1,traj])

  time.put(NArray.float(ps).indgen!*3600, "start" => [0,traj], "end" => [ps-1,traj])
  time.put(NArray.int(p-ps).fill(-999), "start" => [ps,traj], "end" => [p-1,traj])

  (0..ps-1).each do |set| 
    alt.put(NArray.float(dep).indgen!, "start" => [0,set,traj], "end" => [dep-1,set,traj])
    alt.put(NArray.float(z-dep).fill(-999.9), "start" => [dep,set,traj], "end" => [z-1,set,traj])
    temp.put(NArray.float(dep).random!(40), "start" => [0,set,traj], "end" => [dep-1,set,traj])
    temp.put(NArray.float(z-dep).fill(-999.9), "start" => [dep,set,traj], "end" => [z-1,set,traj])
    salt.put(NArray.float(dep).random!(80), "start" => [0,set,traj], "end" => [dep-1,set,traj])
    salt.put(NArray.float(z-dep).fill(-999.9), "start" => [dep,set,traj], "end" => [z-1,set,traj])
  end
end

file.close
nc.create_output
