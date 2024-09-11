/* 
 * MakeErdJavaZip Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package scripts;

import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.DoubleCenterGrids;
import gov.noaa.pfel.coastwatch.griddata.GenerateThreddsXml;
import gov.noaa.pfel.coastwatch.griddata.GridSaveAs;
import gov.noaa.pfel.coastwatch.netcheck.NetCheck;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;

import java.util.Arrays;
import java.util.ArrayList;


/**
 * This is a command line application which makes a .zip file so that
 * DoubleCenterGrids, GridSaveAs, NetCheck, ConvertTable, and GenerateThreddsXml can be distributed.
 * This also makes converttable.jar.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-11-22
 */
public class MakeErdJavaZip  {

    /**
     * This runs MakeErdJava.zip in <contextdirectory>
     * (usually <tomcat>/webapps/cwexperimental).
     *
     * @param args is ignored
     */
    public static void main(String args[]) throws Exception {
        String2.log("\n*** MakeErdJavaZip");
        String errorInMethod = String2.ERROR + " while generating ErdJava.zip:\n";

        //define directories
        String baseDir = File2.webInfParentDirectory() + //with / separator and / at the end
            "WEB-INF/";
        String classPath = baseDir + "classes/";
        String coastWatchDir = classPath + "gov/noaa/pfel/coastwatch/";

        //make the JavaDocs
        String coastWatchClass = "gov.noaa.pfel.coastwatch.";
        String commandLine0 = "C:\\programs\\jdk8u265-b01\\bin\\javadoc" +
            //" -source 1.4" + //use 1.4 for the DODS classes that use "enum" //2011-02-22 Bob Simons changed enum to en.
            " -sourcepath " + classPath +      //root directory of the classes
            " -d "; //+ baseDir + "ConvertTableDoc" + //dir to hold results
        String commandLine2 =
            //space separated .java files
            " " +
            coastWatchDir + "pointdata/ConvertTable.java " +
            coastWatchDir + "pointdata/DigirHelper.java " +
            coastWatchDir + "pointdata/DigirIobisTDSP.java " +
            coastWatchDir + "pointdata/DigirObisTDSP.java " +
            coastWatchDir + "pointdata/ScriptRow.java " +
            coastWatchDir + "pointdata/Table.java " +
            coastWatchDir + "TimePeriods.java " +
            coastWatchDir + "ValidateDataSetProperties.java " +
            //recursively searched packages
            "-classpath " +  //';' separated;  //external packages are important here
                //baseDir + "lib/activation.jar;" +
                baseDir + "lib/commons-compress.jar;" + 
                //baseDir + "lib/commons-discovery.jar;" + 
                //baseDir + "lib/commons-codec-1.3.jar;" +     //these 3 now in netcdfAll-latest
                //baseDir + "lib/commons-httpclient-3.0.1.jar;" + 
                //baseDir + "lib/commons-logging-1.1.jar;" + 
                baseDir + "lib/mail.jar;" +  
                baseDir + "lib/slf4j.jar;" + 
                baseDir + "lib/netcdfAll-latest.jar " + //space after last one
            "-subpackages " +  //the packages to be doc'd   // ':' separated
                //adding a package? add it to dirName below, too
                //"com.sshtools:org.apache.commons.logging:" +  //this external package not very relevant
                "dods:" + //this external package is relevant
                "com.cohort.array:" +  
                "com.cohort.util:" +
                coastWatchClass + "griddata:" +
                coastWatchClass + "netcheck:" +
                coastWatchClass + "util";
                
        //generate javadocs once so it will be in zip file
        String tDir = baseDir + "docs/ErdJavaDoc"; //dir to hold results
        SSR.dosShell("del /s /q " + //delete (/s=recursive /q=quiet) previous results
            String2.replaceAll(tDir, "/", "\\"), 60); 
        String checkNames[] = {
            "/index.html",
            "/com/cohort/array/DoubleArray.html",
            "/gov/noaa/pfel/coastwatch/pointdata/ConvertTable.html",
            "/gov/noaa/pfel/coastwatch/netcheck/NetCheck.html",
            "/gov/noaa/pfel/coastwatch/griddata/GenerateThreddsXml.html",
            "/gov/noaa/pfel/coastwatch/griddata/GridSaveAs.html"};
        for (int i = 0; i < checkNames.length; i++)
            Test.ensureTrue(!File2.isFile(tDir + checkNames[i]), errorInMethod + tDir + checkNames[i] + " not deleted.");
        try {
            String2.log(String2.toNewlineString(SSR.dosShell(
                commandLine0 +  tDir + commandLine2,
                120).toArray()));  
        } catch (Exception e) {
            String2.log(MustBe.throwable(errorInMethod + 
                "(expected) [for zip]:", e));
        }
        for (int i = 0; i < checkNames.length; i++)
            Test.ensureTrue(File2.isFile(tDir + checkNames[i]), errorInMethod + tDir + checkNames[i] + " not found.");

        //generate javadocs again for online use
        tDir = File2.webInfParentDirectory() + //with / separator and / at the end
            "ErdJavaDoc"; //dir to hold results
        SSR.dosShell("del /s /q " + //delete (/s=recursive /q=quiet) previous results
            String2.replaceAll(tDir, "/", "\\"), 60); 
        for (int i = 0; i < checkNames.length; i++)
            Test.ensureTrue(!File2.isFile(tDir + checkNames[i]), errorInMethod + tDir + checkNames[i] + " not deleted.");
        try {
            String2.log(String2.toNewlineString(SSR.dosShell(
                commandLine0 + tDir + commandLine2,
                120).toArray()));  
        } catch (Exception e) {
            String2.log(MustBe.throwable(errorInMethod + 
                " (expected) [for online]:", e));
        }
        for (int i = 0; i < checkNames.length; i++)
            Test.ensureTrue(File2.isFile(tDir + checkNames[i]), errorInMethod + tDir + checkNames[i] + " not found.");

        //make sure relevant files are compiled
        ConvertTable convertTable = new ConvertTable(); 
        GenerateThreddsXml gtdsh = new GenerateThreddsXml();
        GridSaveAs gridSaveAs = new GridSaveAs(); 
        ValidateDataSetProperties validateDataSetProperties = new ValidateDataSetProperties(); 
        DoubleCenterGrids doubleCenterGrids = new DoubleCenterGrids();
        try {
            NetCheck netCheck = new NetCheck(baseDir + "DoesntExist.xml", true); 
        } catch (Exception e) {
            //don't care if error. deployment may be on another computer
            //String2.log(MustBe.throwable(
            //    "MakeNetCheckZip.main test constructors. Ignore this exception:\n", e));
        }
        //I think that is what generates the .xml.log file: delete it
        File2.delete(baseDir + "DoesntExit.xml.log");

        //delete the log created by DoubleCenterGrids.test
        File2.delete("c:/programs/_tomcat/webapps/cwexperimental/WEB-INF/DoubleCenterGrids.log");

        //delete the test file from ConvertTable
        File2.delete("c:/programs/_tomcat/webapps/cwexperimental/WEB-INF/result.nc");

        //accumulate the file names to be zipped
        ArrayList<String> dirNames = new ArrayList();
        dirNames.add(baseDir + "ConvertTable.sh");
        dirNames.add(baseDir + "ConvertTable.bat");
        dirNames.add(baseDir + "DoubleCenterGrids.sh");
        dirNames.add(baseDir + "GridSaveAs.sh");
        dirNames.add(baseDir + "GridSaveAs.bat");
        dirNames.add(baseDir + "GenerateOceanwatchThreddsXml.sh");
        dirNames.add(baseDir + "GenerateOtterThreddsXml.sh");
        dirNames.add(baseDir + "GenerateThredds1ThreddsXml.sh");
        dirNames.add(baseDir + "GenerateThreddsXml.sh");
        dirNames.add(baseDir + "incompleteMainCatalog.xml");
        dirNames.add(baseDir + "iobis.m");
        dirNames.add(baseDir + "NetCheck.sh");
        dirNames.add(baseDir + "NetCheck.bat");
        dirNames.add(baseDir + "NetCheck.xml");
        dirNames.add(baseDir + "obis.m");
        dirNames.add(baseDir + "QN2005193_2005193_ux10_westus.grd");
        dirNames.add(baseDir + "ValidateDataSetProperties.sh");
        dirNames.add(baseDir + "ValidateDataSetProperties.bat");
        //dirNames.add(baseDir + "lib/activation.jar");
        dirNames.add(baseDir + "lib/commons-compress.jar"); 
        //dirNames.add(baseDir + "lib/commons-discovery.jar"); 
        //dirNames.add(baseDir + "lib/commons-codec-1.3.jar"); //these 3 are now in netcdfAll-latest
        //dirNames.add(baseDir + "lib/commons-httpclient-3.0.1.jar"); 
        //dirNames.add(baseDir + "lib/commons-logging-1.1.jar"); 
        dirNames.add(baseDir + "lib/mail.jar");
        dirNames.add(baseDir + "lib/netcdfAll-latest.jar");
        dirNames.add(baseDir + "lib/slf4j.jar");
        String2.add(dirNames, RegexFilenameFilter.recursiveFullNameList(baseDir + "docs/ErdJavaDoc/",         ".+", false)); //javadocs
        String2.add(dirNames, RegexFilenameFilter.recursiveFullNameList(baseDir + "classes/dods/",            ".+", false));
        String2.add(dirNames, RegexFilenameFilter.recursiveFullNameList(baseDir + "classes/com/sshtools/",    ".+", false));
        String2.add(dirNames, RegexFilenameFilter.fullNameList(         baseDir + "classes/com/cohort/array/",".+"));
        String2.add(dirNames, RegexFilenameFilter.fullNameList(         baseDir + "classes/com/cohort/util/", ".+"));
        String2.add(dirNames, RegexFilenameFilter.fullNameList(         coastWatchDir,                        "CWBrowser.properties"));
        String2.add(dirNames, RegexFilenameFilter.fullNameList(         coastWatchDir,                        "BrowserDefault.properties"));
        String2.add(dirNames, RegexFilenameFilter.fullNameList(         coastWatchDir,                        "DataSet.properties"));
        String2.add(dirNames, RegexFilenameFilter.fullNameList(         coastWatchDir,                        "LICENSE\\.txt"));
        String2.add(dirNames, RegexFilenameFilter.fullNameList(         coastWatchDir,                        "OneOf.+"));
        String2.add(dirNames, RegexFilenameFilter.fullNameList(         coastWatchDir,                        "TimePeriods.+"));
        String2.add(dirNames, RegexFilenameFilter.fullNameList(         coastWatchDir,                        "ValidateDataSetProperties.+"));
        String2.add(dirNames, RegexFilenameFilter.fullNameList(         coastWatchDir + "griddata/",          ".+"));
        String2.add(dirNames, RegexFilenameFilter.fullNameList(         coastWatchDir + "hdf/",               ".+"));
        String2.add(dirNames, RegexFilenameFilter.fullNameList(         coastWatchDir + "netcheck/",          ".+"));
        String2.add(dirNames, RegexFilenameFilter.fullNameList(         coastWatchDir + "pointdata/",         "ConvertTable.+"));
        String2.add(dirNames, RegexFilenameFilter.fullNameList(         coastWatchDir + "pointdata/",         "Digir.+"));
        String2.add(dirNames, RegexFilenameFilter.fullNameList(         coastWatchDir + "pointdata/",         "Table.+"));
        String2.add(dirNames, RegexFilenameFilter.fullNameList(         coastWatchDir + "pointdata/",         "LICENSE\\.txt"));
        String2.add(dirNames, RegexFilenameFilter.fullNameList(         coastWatchDir + "util/",              ".+"));

        //convert to sorted String array
        String dirNameArray[] = dirNames.toArray(new String[0]);
        Arrays.sort(dirNameArray);
        //String2.log(String2.toNewlineString(dirNameArray));

        //make the zip file
        String zipName = File2.webInfParentDirectory() + //with / separator and / at the end
            "ErdJava.zip";
        String2.log("MakeErdJavaZip is making " + zipName + ".");
        File2.delete(zipName);
        SSR.zip(zipName, dirNameArray, 60, baseDir);
        String2.log("\nMakeErdJavaZip successfully finished making " + 
            zipName + ".\nnFiles=" + dirNames.size());
    }


    /**
     * This makes converttable.jar specifically for LAS.
     *
     * @param destinationDir e.g., C:/pmelsvn/WebContent/WEB-INF/lib/
     */
    public static void makeConvertTableJar(String destinationDir) throws Exception {
        String2.log("\n*** makeConvertTableJar");
        String errorInMethod = String2.ERROR + " while generating converttable.jar:\n";

        //make sure relevant files are compiled
        ConvertTable convertTable = new ConvertTable(); 

        //define directories
        destinationDir = File2.addSlash(destinationDir);
        String baseDir = (File2.webInfParentDirectory() + //with / separator and / at the end
            "WEB-INF\\classes").substring(2);
        String coastWatchDir = "gov\\noaa\\pfel\\coastwatch\\";

        //accumulate the file names to be zipped
        String ctName = destinationDir + "converttable.jar";
        StringBuilder cmdLine = new StringBuilder();
        cmdLine.append("C:\\programs\\jdk8u265-b01\\bin\\jar cvf " + ctName);
        //I thought I could use -C once and have lots of files after it. 
        //But no. I need to use -C for each file.   (maybe just if 'file' is a directory)
        //And can't use *. List files separately.
        cmdLine.append(" -C " + baseDir + " com\\cohort\\array");
        cmdLine.append(" -C " + baseDir + " com\\cohort\\util");
        cmdLine.append(" -C " + baseDir + " com\\sshtools");
        cmdLine.append(" -C " + baseDir + " " + coastWatchDir + "LICENSE.txt");
        cmdLine.append(" -C " + baseDir + " " + coastWatchDir + "TimePeriods.class");
        cmdLine.append(" -C " + baseDir + " " + coastWatchDir + "TimePeriods.java");
        cmdLine.append(" -C " + baseDir + " " + coastWatchDir + "griddata");
        cmdLine.append(" -C " + baseDir + " " + coastWatchDir + "pointdata\\ConvertTable.class");
        cmdLine.append(" -C " + baseDir + " " + coastWatchDir + "pointdata\\ConvertTable.java");
        cmdLine.append(" -C " + baseDir + " " + coastWatchDir + "pointdata\\Table.class");
        cmdLine.append(" -C " + baseDir + " " + coastWatchDir + "pointdata\\Table.java");
        cmdLine.append(" -C " + baseDir + " " + coastWatchDir + "pointdata\\LICENSE.txt");
        cmdLine.append(" -C " + baseDir + " " + coastWatchDir + "util");

        //make the zip file
        String2.log("MakeErdJavaZip is making " + ctName);
        File2.delete(ctName);
        SSR.verbose = true;
        SSR.dosShell(cmdLine.toString(), 60); 
        String2.log("\nMakeErdJavaZip successfully finished making " + ctName);

    }

    /**
     * This makes cwhdfToNc.zip specifically for LAS.
     *
     * unzip with: unzip cwhdfToNc.zip 
     */
    public static void makeCwhdfToNcZip() throws Exception {
        String2.log("\n*** makeCwhdfToNcZip");
        String errorInMethod = String2.ERROR + " while generating cwhdfToNc.zip:\n";
        
        //delete the zip file
        String zipName = "c:/backup/cwhdfToNc.zip";

        //accumulate the file names to be zipped
        String baseDir = "c:/content/";
        ArrayList<String> dirNames = new ArrayList();
        String2.add(dirNames, RegexFilenameFilter.recursiveFullNameList(baseDir + "cwhdfToNc/", ".+", false)); 

        //convert to sorted String array
        String dirNameArray[] = dirNames.toArray(new String[0]);
        Arrays.sort(dirNameArray);
        //String2.log(String2.toNewlineString(dirNameArray));

        //make the zip file
        String2.log("MakeCwhdfToNcZip is making " + zipName);
        File2.delete(zipName);
        SSR.zip(zipName, dirNameArray, 60, baseDir);
        String2.log("\nMakeCwhdfToNcZip successfully finished making " + 
            zipName + ".\nnFiles=" + dirNames.size());

    }
}
