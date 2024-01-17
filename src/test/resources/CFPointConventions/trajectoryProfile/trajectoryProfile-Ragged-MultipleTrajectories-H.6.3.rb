#! ruby

require 'numru/netcdf'
require_relative '../utils'

include NumRu

readme = \
"
"

nc = CFNetCDF.new(__FILE__, readme)
file = nc.netcdf_file
file.put_att("featureType","trajectoryProfile")

o = 0 # UNLIMITED
p = 20
t = 5

obs_dim = file.def_dim("obs",o)
profile_dim = file.def_dim("profile",p)
trajectory_dim = file.def_dim("trajectory",t)

trajectory = file.def_var("trajectory","int",[trajectory_dim])
trajectory.put_att("cf_role", "trajectory_id")

lat = file.def_var("lat","sfloat",[profile_dim])
lat.put_att("units","degrees_north")
lat.put_att("long_name","station latitude")
lat.put_att("standard_name","latitude")

lon = file.def_var("lon","sfloat",[profile_dim])
lon.put_att("units","degrees_east")
lon.put_att("long_name","station longitude")
lon.put_att("standard_name","longitude")

rowsize = file.def_var("rowSize","int",[profile_dim])
rowsize.put_att("long_name", "number of obs for this trajectory")
rowsize.put_att("sample_dimension", "obs")

trajectory_index = file.def_var("trajectory_index","int",[profile_dim])
trajectory_index.put_att("long_name", "which trajectory this profile is for")
trajectory_index.put_att("instance_dimension", "trajectory")

time = file.def_var("time","int",[profile_dim])
time.put_att("long_name","time of measurement")
time.put_att("standard_name","time")
time.put_att("units","seconds since 1990-01-01 00:00:00")
time.put_att("missing_value",-999,"int")

alt = file.def_var("z","sfloat",[obs_dim])
alt.put_att("long_name","height above mean sea level")
alt.put_att("standard_name","altitude")
alt.put_att("units","m")
alt.put_att("positive","up")
alt.put_att("axis","Z")
alt.put_att("missing_value",-999,"sfloat")

temp = file.def_var("temperature","sfloat",[obs_dim])
temp.put_att("long_name","Air Temperature")
temp.put_att("standard_name","air_temperature")
temp.put_att("units","Celsius")
temp.put_att("coordinates", "time lat lon z")
temp.put_att("missing_value",-999.9,"sfloat")

humi = file.def_var("humidity","sfloat",[obs_dim])
humi.put_att("long_name","Humidity")
humi.put_att("standard_name","specific_humidity")
humi.put_att("units","Percent")
humi.put_att("coordinates", "time lat lon z")
humi.put_att("missing_value",-999.9,"sfloat")

# Stop the definitions, lets write some data
file.enddef

random = Random.new

# Profiles per trajectory
ppj = (p/t).floor

# Stores the Row Sizes of each trajectory
sizes = []
# Stores the # of profiles
psizes = []

trajectory.put(NArray.int(t).indgen!)

# Iterate over each trajectory and generates random profiles and values
(0..t-1).each do |traj|
  ps_so_far = psizes.inject(0,&:+)
  psizes << ppj

  (0..ppj-1).each do |prof|
    obs_so_far = sizes.inject(0,&:+)
    os = random.rand(10..50)
    sizes << os

    # Indexes
    trajectory_index.put([traj], "start" => [ps_so_far + prof], "end" => [ps_so_far + prof])
    rowsize.put(os, "start" => [ps_so_far + prof], "end" => [ps_so_far + prof])
    timedata = random.rand(0..50)
    multdata = 3600
    time.put(timedata * multdata, "start" => [ps_so_far + prof], "end" => [ps_so_far + prof])
    # Lat/lon
    lat.put(random.rand(40..60), "start" => [ps_so_far + prof], "end" => [ps_so_far + prof])
    lon.put(random.rand(-76..-40), "start" => [ps_so_far + prof], "end" => [ps_so_far + prof])

    # Data vars (obs)
    alt.put(NArray.float(os).indgen!, "start" => [obs_so_far], "end" => [obs_so_far + os - 1])
    temp.put(NArray.float(os).random!(40), "start" => [obs_so_far], "end" => [obs_so_far + os - 1])
    humi.put(NArray.float(os).random!(80), "start" => [obs_so_far], "end" => [obs_so_far + os - 1])
  end

end

file.close
nc.create_output
