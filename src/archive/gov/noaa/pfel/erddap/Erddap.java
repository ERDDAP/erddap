public class Erddap {
    /**
   * This makes a erddapContent.zip file with the [tomcat]/content/erddap files for distribution.
   *
   * @param removeDir e.g., "c:/programs/_tomcat/samples/"
   * @param destinationDir e.g., "c:/backup/"
   */
  public static void makeErddapContentZip(String removeDir, String destinationDir)
  throws Throwable {
String2.log("*** makeErddapContentZip dir=" + destinationDir);
String baseDir = removeDir + "content/erddap/";
SSR.zip(
    destinationDir + "erddapContent.zip",
    new String[] {
      baseDir + "datasets.xml", baseDir + "setup.xml", baseDir + "images/erddapStart2.css"
    },
    10,
    removeDir);
}
}
