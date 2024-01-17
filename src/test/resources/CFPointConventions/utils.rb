require 'fileutils'

class CFNetCDF

  attr_accessor :netcdf_file

  def initialize(file_name,readme)
    @base_name = File.basename(file_name).gsub(".rb","")
    root_path = File.dirname(file_name) + "/" + @base_name
    @netcdf_path = root_path + "/" + @base_name + ".nc"
    @ncml_path = root_path + "/" + @base_name + ".ncml"
    @cdl_path = root_path + "/" + @base_name + ".cdl"
    @readme_path = root_path + "/README"
    FileUtils.mkdir(root_path) unless File.exists?(root_path)

    @netcdf_file = NetCDF.create(@netcdf_path)
    @netcdf_file.put_att("Conventions","CF-1.6")

    @readme = \
    "** #{@base_name} **
    
    #{readme}
    "
  end

  def create_output
    `ncdump -h #{@netcdf_path} > #{@cdl_path}`
    `ncdump -x -h #{@netcdf_path} > #{@ncml_path}`
    open(@readme_path,"w"){|f| f.write(@readme)}
  end

end