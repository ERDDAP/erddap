function y = chump_make_grd2hdf(infile,hdffile,type,delx,dely)
% program reads an xyz GMT file (lon,lat,var made by command grd2xyz).
% loads it into arrays, orients and saves as a matlab file.
%
% Input:
%        infile is name of input file to be chumped
%            The file must follow either Dave's file name conventions
%            (e.g., AT2004123_2004125_ssta_westus.xyz)
%            or the CoastWatch Browser's file name conventions
%            (e.g., LATssta1day_20050518_W-135E-113S30N50.xyz).
%        outfile is output file name
%        type is output variable name
%        delx is zonal grid spacing
%        dely is horizontal grid spacing
%
%
% 13 Jan 2003. DGF
% modified 26 May 2005 LJS
%
% load in file and figure out grid dimensions
grd=load(infile);
lon = [min(grd(:,1)):delx:max(grd(:,1))];
lat = [min(grd(:,2)):dely:max(grd(:,2))];

% reshape grid
var = reshape(squeeze(grd(:,3)),length(lon),length(lat));

% put longitude into Atlanticentric reference
lon = lon - 360;

% kluge some sort of missing value for HDF
var(find(isnan(var)))=-999;

% prepare to write grid
varname = type;
%eval([type '=var;']);

% create HDF file
sd_id = hdfsd('start',hdffile,'DFACC_CREATE');

% create first SDS
ds_name = varname;
ds_type = 'double';
ds_rank = ndims(var);
ds_dims = fliplr(size(var));
sds_id = hdfsd('create',sd_id,ds_name,ds_type,ds_rank,ds_dims);
  
% Write first array to SDS
ds_start = zeros(1:ndims(var));
ds_stride = [];
ds_edges = fliplr(size(var)); 
stat = hdfsd('writedata',sds_id,ds_start,ds_stride,ds_edges,var);

% close first data set
stat = hdfsd('endaccess',sds_id);

%create 2nd SDS
ds_name = 'Longitude';
ds_type = 'double';
ds_rank = ndims(lon);
ds_dims = fliplr(size(lon));
sds_id = hdfsd('create',sd_id,ds_name,ds_type,ds_rank,ds_dims);

%Write 2nd SDS
ds_start = zeros(1:ndims(lon));
ds_stride = [];
ds_edges = fliplr(size(lon));
stat = hdfsd('writedata',sds_id,ds_start,ds_stride,ds_edges,lon);

%Close 2nd SDS
stat = hdfsd('endaccess',sds_id);

%create 3rd SDS
ds_name = 'Latitude';
ds_type = 'double';
ds_rank = ndims(lat);
ds_dims = fliplr(size(lat));
sds_id = hdfsd('create',sd_id,ds_name,ds_type,ds_rank,ds_dims);

%Write 3rd SDS
ds_start = zeros(1:ndims(lat));
ds_stride = [];
ds_edges = fliplr(size(lat));
stat = hdfsd('writedata',sds_id,ds_start,ds_stride,ds_edges,lat);

%Close 3rd SDS
stat = hdfsd('endaccess',sds_id);


%------------------------------------------------------
%Writes metadata for HDF files using CoastWatch Metadata Specifications

%Breaks filename up using underscores as delimiter
[pathstr, infile_name, infile_ext, versn] = fileparts(infile);
[part1, part2, part3, part4] = strread(infile_name, '%s %s %s %s','delimiter','_');
part1 = char(part1);
part2 = char(part2);
part3 = char(part3);
part4 = char(part4);

%Tests to see if the filename is the "CoastWatch Browser" type
%example: LATssta1day_20050518_W-135E-113S30N50.xyz
%AT - data_code - used later in lookup table "lookup_data_source"
%ssta - data_id - used later in lookup table "lookup_data_id"
%1day - time duration of file.
%20050518 - the LAST date of data if the file is a composite
%   format is 4-digit year, 2-digit month, 2-digit day
%   If the file is from a single pass, the time of the pass will
%   follow immediately after the date
%W-135E-113S30N50  - latitude and longitude of image corners
%
%If file is of this type, the part 3 should begin with 'W'
if part3(1:1) == 'W'
    
    %Gather data code and data id
    data_code = part1(2:3);
    data_id = part1(4:7);
        
    %get information about image date from filename
    date_vector(1) = str2num(part2(1:4));
    date_vector(2) = str2num(part2(5:6));
    date_vector(3) = str2num(part2(7:8));
    serial_date = int32(datenum(date_vector));
    base_date = int32(datenum([1970 1 1]));
    pass_date_end = serial_date - base_date;
    
    %determine if a file is from a single pass or a composite
    
    %If file is from a single pass, determine variables start_time 
    %in seconds and pass_date, the number of days since 01Jan1970
    if length(part2) == 14   %file is from a pass
        comp_flag = 'false';
        
        pass_date = pass_date_end;
        
        hours = str2num(part2(9:10));
        minutes = str2num(part2(11:12));
        seconds = str2num(part2(13:14));
        start_time = hours*3600 + minutes*60 + seconds;
            
    %If file is a composite, pass_date will be a vector of dates
    else comp_flag = 'true';
        
        duration = part1(8:end);
        b = length(duration) - 3;
        
        if duration(1:b) == 'm' %file is a monthly composite
            date_vector_end = [date_vector(1), date_vector(2), 1];
            pass_date_start = int32(datenum(date_vector_end)) - base_date;
        
        else        
        composite_duration = str2num(duration(1:b)) - 1;
        pass_date_start = pass_date_end - int32(composite_duration);
        end
        
        pass_date = [pass_date_start:pass_date_end];
        
        %start_time will also be a vector of the same size as pass_date
        start_time = zeros(size(pass_date));
        if size(start_time,2) > 1
            start_time(end) = 86399;
        end
    end

%Files of the type Dave makes
%example: AT2004123_2004125_ssta_westus.xyz
%AT - data code, used later in lookup table "lookup_data_source"
%2004123 - beginning date of data in image, format is
%   4-digit year and 3-digit day-of-year
%2004125 - ending date of data in image.  If file is from a single
%   pass, this section will read the time of pass - ex. 115000h
%ssta - data id, used later in lookup table "lookup_data_id"
%westus - image region
else %file is Dave type
    
    %Gather data code and data id
    data_code = part1(1:2);
    data_id = part3(1:4);
    
    %gather starting year and date of image
    start_year = int32(str2num(part1(3:6)));
    start_day = int32(str2num(part1(7:9)));
    
    %Determine if file is a pass file or composite
    %Calculate pass_date = number of days since 01Jan1970
    if part2(end) == 'h'  %file is pass
        comp_flag = 'false';
        a = start_year - 1970;
        b = (a / 4) - .1;
        c = round(b);
        pass_date = 366*c + (a - c)*365 + start_day -1;
        
        hours = str2num(part2(1:2));
        minutes = str2num(part2(3:4));
        seconds = str2num(part2(5:6));
        start_time = hours*3600 + minutes*60 + seconds;
        
    %if file is a composite, pass_date will be a vector of dates
    %start_time will also be a vector of the same size
    else comp_flag = 'true';
        end_year = int32(str2num(part2(1:4)));
        end_day = int32(str2num(part2(5:7)));
        
        a = start_year - 1970;
        b = (a / 4) - .1;
        c = round(b);
        pass_date_start = 366*c + (a - c)*365 + start_day -1;
        
        a = end_year - 1970;
        b = (a / 4) - .1;
        c = round(b);
        pass_date_end = 366*c + (a - c)*365 + end_day -1;
        pass_date = [pass_date_start:pass_date_end];
        
        start_time = zeros(size(pass_date));
        if size(start_time,2) > 1
            start_time(end) = 86399;
        end
    end
end
        
%Go to lookup table for metadata information
[satellite, sensor, origin, sat_frac_digits, ds_frac_digits] =...
    lookup_data_source(data_code);
[data_description, data_units, did_frac_digits] = lookup_data_id(data_id);
sensor = [sensor, ', ', data_description];

%---------------------------------------------------------
%Begin writing Global Attributes
hdfsd('setattr',sd_id,'satellite',satellite);
hdfsd('setattr',sd_id,'sensor',sensor);
hdfsd('setattr',sd_id,'origin',origin);
hdfsd('setattr',sd_id,'history','unknown');
hdfsd('setattr',sd_id,'cwhdf_version','3.2');

%Determine pass type
if data_id == 'sstn'
    pass_type = 'night';
elseif data_id == 'sstd'
    pass_type = 'day';
else pass_type = 'day/night';
end

hdfsd('setattr',sd_id,'composite',comp_flag);
hdfsd('setattr',sd_id,'pass_type',pass_type);
hdfsd('setattr',sd_id,'pass_date',pass_date);
hdfsd('setattr',sd_id,'start_time',start_time);

%Write map projection data
hdfsd('setattr',sd_id,'projection_type','mapped');
hdfsd('setattr',sd_id,'projection','Geographic');
hdfsd('setattr',sd_id,'gctp_sys',int32(0));
hdfsd('setattr',sd_id,'gctp_zone',int32(0));

gctp_parm = zeros(1,15);
hdfsd('setattr',sd_id,'gctp_parm',gctp_parm);
hdfsd('setattr',sd_id,'gctp_datum',int32(12));

%Determine et_affine transformation
% long = a*row + c*col + e
% lat = b*row + d*col + f
a = 0;
b = -dely;
c = delx;
d = 0;
e = min(lon);
f = max(lat);
hdfsd('setattr',sd_id,'et_affine',[a b c d e f]);

%Writes row and column attributes
rows = int32(size(var,2));
cols = int32(size(var,1));
hdfsd('setattr',sd_id,'rows',rows);
hdfsd('setattr',sd_id,'cols',cols);

%polygon attributes would be written here if needed

%-----------------------------------------------
%Variable Metadata

%Opens first dataset
sds_id = hdfsd('select',sd_id,0);

%writes variable metadata for first SDS
[dsname, dsndims, dsdims, dstype, dsatts, stat] =...
    hdfsd('getinfo',sds_id);
hdfsd('setattr',sds_id,'long_name',dsname);
hdfsd('setattr',sds_id,'units',data_units);
hdfsd('setattr',sds_id,'coordsys','Geographic');
hdfsd('setattr',sds_id,'_FillValue',-999);
hdfsd('setattr',sds_id,'missing_value',-999);
hdfsd('setattr',sds_id,'scale_factor',1);
hdfsd('setattr',sds_id,'scale_factor_err',0);
hdfsd('setattr',sds_id,'add_offset',0);
hdfsd('setattr',sds_id,'add_offset_err',0);
hdfsd('setattr',sds_id,'calibrated_nt',int32(0));

if data_code=='AT' |...
        data_code=='AG' | data_code=='GA' |...
        data_code=='PH'
    ds_frac_digits = char(ds_frac_digits);
    ds_frac_digits = str2num(ds_frac_digits);
    data_frac_digits = ds_frac_digits;
    hdfsd('setattr',sds_id,'fraction_digits',int32(data_frac_digits));
elseif data_id=='ux10' | data_id=='uy10' | data_id=='umod' |...
        data_id=='taux' | data_id=='tauy' | data_id=='tmod' |...
        data_id=='curl'
    did_frac_digits = char(did_frac_digits);
    did_frac_digits = str2num(did_frac_digits);
    data_frac_digits = did_frac_digits;
    hdfsd('setattr',sds_id,'fraction_digits',int32(data_frac_digits));
end

%Closes data set
hdfsd('endaccess',sds_id);

%Opens 2nd SDS
sds_id = hdfsd('select',sd_id,1);

%writes variable metadata for 2nd SDS
[dsname, dsndims, dsdims, dstype, dsatts, stat] =...
    hdfsd('getinfo',sds_id);
hdfsd('setattr',sds_id,'long_name',dsname);
hdfsd('setattr',sds_id,'units','degrees');
hdfsd('setattr',sds_id,'coordsys','Geographic');
hdfsd('setattr',sds_id,'_FillValue',-999);
hdfsd('setattr',sds_id,'missing_value',-999);
hdfsd('setattr',sds_id,'scale_factor',1);
hdfsd('setattr',sds_id,'scale_factor_err',0);
hdfsd('setattr',sds_id,'add_offset',0);
hdfsd('setattr',sds_id,'add_offset_err',0);
hdfsd('setattr',sds_id,'calibrated_nt',int32(0));

sat_frac_digits = char(sat_frac_digits);
sat_frac_digits = str2num(sat_frac_digits);
hdfsd('setattr',sds_id,'fraction_digits',int32(sat_frac_digits));

%closes data set
hdfsd('endaccess',sds_id);

%opens 3rd data set
sds_id = hdfsd('select',sd_id,2);

%write variable metadata for 3rd SDS
[dsname, dsndims, dsdims, dstype, dsatts, stat] =...
    hdfsd('getinfo',sds_id);
hdfsd('setattr',sds_id,'long_name',dsname);
hdfsd('setattr',sds_id,'units','degrees');
hdfsd('setattr',sds_id,'coordsys','Geographic');
hdfsd('setattr',sds_id,'_FillValue',-999);
hdfsd('setattr',sds_id,'missing_value',-999);
hdfsd('setattr',sds_id,'scale_factor',1);
hdfsd('setattr',sds_id,'scale_factor_err',0);
hdfsd('setattr',sds_id,'add_offset',0);
hdfsd('setattr',sds_id,'add_offset_err',0);
hdfsd('setattr',sds_id,'calibrated_nt',int32(0));
hdfsd('setattr',sds_id,'fraction_digits',int32(sat_frac_digits));

%closes 3rd dataset
hdfsd('endaccess',sds_id);

%Closes all open datasets and files and saves changes
hdfml('closeall');