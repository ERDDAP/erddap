#! ruby

require 'numru/netcdf'
require_relative '../utils'

include NumRu

readme = \
"
http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#idp8399648
"

nc = CFNetCDF.new(__FILE__, readme)
file = nc.netcdf_file
file.put_att("featureType","trajectory")

t = 10
o = 0 # UNLIMITED
name = 50

obs_dim = file.def_dim("obs",o)
trajectory_dim = file.def_dim("trajectory",t)
name_dim = file.def_dim("name_strlen",name)

lat = file.def_var("lat","sfloat",[obs_dim])
lat.put_att("units","degrees_north")
lat.put_att("long_name","station latitude")
lat.put_att("standard_name","latitude")

lon = file.def_var("lon","sfloat",[obs_dim])
lon.put_att("units","degrees_east")
lon.put_att("long_name","station longitude")
lon.put_att("standard_name","longitude")

trajectory_info = file.def_var("trajectory_info","int",[trajectory_dim])
trajectory_info.put_att("long_name", "trajectory info")
trajectory_info.put_att("missing_value",-999,"int")

trajectory_name = file.def_var("trajectory_name","char",[name_dim, trajectory_dim])
trajectory_name.put_att("cf_role", "trajectory_id")
trajectory_name.put_att("long_name", "trajectory name")

trajectory_index = file.def_var("trajectory_index","int",[obs_dim])
trajectory_index.put_att("long_name", "index of trajectory this obs belongs to")
trajectory_index.put_att("instance_dimension", "trajectory")

time = file.def_var("time","int",[obs_dim])
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
alt.put_att("missing_value",-999.9,"sfloat")

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

blank = Array.new(name)
traj_name_data = []
(0..t-1).each do |i|
  traj_name_data << [("Trajectory#{i}".split(//).map!{|d|d.ord} + blank)[0..name-1]]
end
trajectory_name.put(traj_name_data)

# Fill the trajectory_info variable with increasing integers
trajectory_info.put(NArray.int(t).indgen!)

random = Random.new

# Stores the Row Sizes of each trajectory
sizes = []

# Iterate over each trajectory and generates random position and values
(0..t-1).each do |traj|
  obs_so_far = sizes.inject(0,&:+)
  os = random.rand(10..30)
  sizes << os

  trajectory_index.put(NArray.int(os).random!(t), "start" => [obs_so_far], "end" => [obs_so_far + os - 1])

  lat.put(NArray.float(os).random!(45), "start" => [obs_so_far], "end" => [obs_so_far + os - 1])

  lon.put(NArray.float(os).random!(-76), "start" => [obs_so_far], "end" => [obs_so_far + os - 1])

  timedata = NArray.int(os).random!(50)
  multdata = NArray.int(os).fill!(3600)
  time.put(timedata * multdata, "start" => [obs_so_far], "end" => [obs_so_far + os - 1])

  alt.put(NArray.float(os).random!(30), "start" => [obs_so_far], "end" => [obs_so_far + os - 1])

  temp.put(NArray.float(os).random!(40), "start" => [obs_so_far], "end" => [obs_so_far + os - 1])
  
  humi.put(NArray.float(os).random!(80), "start" => [obs_so_far], "end" => [obs_so_far + os - 1])
end

file.close
nc.create_output