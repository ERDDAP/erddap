/* 
 * FishBase Copyright 2011, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.util;

import com.cohort.array.*;
import com.cohort.util.*;

import gov.noaa.pfel.coastwatch.griddata.*;
import gov.noaa.pfel.coastwatch.hdf.*;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.*;
import gov.noaa.pfel.erddap.dataset.*;

import java.io.FileWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


// from netcdfAll-x.jar
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
//import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;



/**
 * This class has static methods related to import/processing of FishBase data.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2011-06-01
 */
public class FishBase  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;


    /** 2011-06-01 Bob used this to convert all manually saved FishBase .html files
     * to .nc.
     * To save from .mdb file to .html:
     * <ol>
     * <li>Open .mdb in Microsoft Access
     * <li>Right click on a table name; Export...
     * <li>Save As Type: HTML documents
     * <li>Export   (it will add .html)
     * </ul>
     *
     * @param tableRegex a regex to choose which tables to process
     */
    public static void convertHtmlToNc(String tableRegex) throws Throwable {
        String htmlDir = "c:/data/FBOther/FBHtml/";
        String ncDir   = "c:/data/FBOther/FBNc/";
        String today = Calendar2.getCurrentISODateTimeStringLocalTZ().substring(0, 10);
        String2.log("FishBase.convertHtmlToNc " + tableRegex);
        
        //get list of files
        String names[] = RegexFilenameFilter.list(htmlDir, ".*\\.html");
        int nNames = names.length;

        //convert all to .nc
        for (int i = 0; i < nNames; i++) {
            String fName = File2.getNameNoExtension(names[i]);
            if (!fName.matches(tableRegex))
                continue;
            String2.log("\n#" + i + "=" + fName);

            //set generic attributes 
            Attributes gatts = new Attributes();
            gatts.set("id", fName); //pre 2019-05-07 was "null" 
            gatts.set("observationDimension", "null");
            gatts.set("cdm_data_type", "Other");
            gatts.set("Conventions", "COARDS, CF-1.6, ACDD-1.3");
            gatts.set("history", "The FishBase 2004 DVD\n" +
                today + " Reformatted for ERDDAP at NOAA SWFSC ERD by erd.data@noaa.gov");
            gatts.set("infoUrl", "http://www.fishbase.org");
            gatts.set("institution", "FishBase");
            gatts.set("license", "This work is licensed under a Creative Commons Attribution-Noncommercial 3.0 Unported License. (CC-BY-NC) You are welcome to include text, numbers and maps from FishBase in your own websites for non-commercial use, given that such inserts are clearly identified as coming from FishBase, with a backward link to the respective source page. Photos and drawings belong to the indicated persons or organizations and have their own copyright statements. Photos and drawings with CC-BY or CC-BY-NC copyrights can be used without further permission, with full attribution to the person or organization and the indication 'from FishBase'.\n" +
"\n" +
"DISCLAIMER: We cannot guarantee the accuracy or completeness of the information in FishBase. Neither ICLARM nor any of its collaborators will be liable for any direct or indirect damage arising out of the use of FishBase.\n" +
"\n" +
EDStatic.standardLicense);
            gatts.set("sourceUrl", "(local files)");
            gatts.set("standard_name_vocabulary", "CF Standard Name Table v55");
            String startReference = "To give due credit to the original authors, please cite:\n" +
"Froese, R. and D. Pauly, Editors. 2004. FishBase 2004 DVD: the " + fName + " table.\n";

            //set table-specific info
            String colNamesRow;
            int maxStringLength = 1000000;

            //ABNORM
            if (fName.equals("ABNORM")) {
                colNamesRow = 
"<TR>\n" +
"<TD>Family</TD>\n" +
"<TD>Genus</TD>\n" +
"<TD>Species</TD>\n" +
"<TD>CommonName</TD>\n" +
"<TD>SpecCode</TD>\n" +
"<TD>StockCode</TD>\n" +
"<TD>LifeStage</TD>\n" +
"<TD>Location</TD>\n" +
"<TD>Season</TD>\n" +
"<TD>AbnormalityDisease</TD>\n" +
"<TD>Prevalence</TD>\n" +
"<TD>Stressor</TD>\n" +
"<TD>LabField</TD>\n" +
"<TD>Concentration</TD>\n" +
"<TD>ABNORMRefNo</TD>\n" +
"<TD>SecondRefNo</TD>\n" +
"<TD>Notes</TD>\n" +
"<TD>Entered</TD>\n" +
"<TD>DateEntered</TD>\n" +
"<TD>Modified</TD>\n" +
"<TD>DateModified</TD>\n" +
"<TD>Expert</TD>\n" +
"<TD>DateChecked</TD>\n" +
"</TR>\n";
                gatts.set("reference", startReference +
"\n" +
"Weis, J. and P. Weis.  1989.  Effects of environmental pollutants on early fish development.  Aquat. Sci. 1(1):45-73.");
                gatts.set("subsetVariables", 
"Genus, Species, CommonName, Family, LifeStage, Location, Season, AbnormalityDisease, Prevalence, Stressor, LabField, Concentration, ABNORMRefNo");
                gatts.set("summary", "Deformities were the focus of the literature review, which this table is based upon. It includes compiled data from both laboratory experiments and field studies. A review by Weis and Weis (1989) was the source of much of the information from laboratory field studies. The aim of the study was to relate fish abnormalities to environmental quality.\n" +
"\n" +
"This table was developed to bring together prevalences of specific externally visible abnormalities in fish populations, as reported from field studies, and causal agents of specific abnormalities, as reported from laboratory experiments. The table provides examples of the use of fish abnormalities as environmental indicators, and, where possible, provides information linking these same abnormalities to agents of environmental degradation.  Information provided for each record in the table includes species, life stage, geographic location, time of year, locality, type of abnormality, the prevalence (mainly from field studies), stressor or suspected stressor (usually only known with certainty from laboratory studies), concentration (only for lab studies), primary reference (immediate source of information) and a secondary reference (original source of information). In addition, notes are provided to expand on or clarify information given in a record.");
                gatts.set("title", "FishBase 2004, Abnormalities");

            //ABNORMRef
            } else if (fName.equals("ABNORMRef")) {
                colNamesRow = 
"<TR>\n" +
"<TD>RefNo</TD>\n" +
"<TD>References</TD>\n" +
"<TD>Author</TD>\n" +
"<TD>Year</TD>\n" +
"<TD>Title</TD>\n" +
"<TD>Source</TD>\n" +
"<TD>FirstAuthor</TD>\n" +
"</TR>\n";
                gatts.set("reference", startReference +
"\n" +
"Weis, J. and P. Weis.  1989.  Effects of environmental pollutants on early fish development.  Aquat. Sci. 1(1):45-73.");
                gatts.set("subsetVariables", 
"???");
                gatts.set("summary", "???");
                gatts.set("title", "FishBase 2004, ???");

            //BIBLIO
            } else if (fName.equals("BIBLIO")) {
                colNamesRow = 
"<TR>\n" +
"<TD>AutoCtr</TD>\n" +
"<TD>RefNo</TD>\n" +
"<TD>SpecCode</TD>\n" +
"<TD>SynCode</TD>\n" +
"<TD>RefPage</TD>\n" +
"<TD>Local</TD>\n" +
"<TD>Comment</TD>\n" +
"<TD>Quote</TD>\n" +
"<TD>Entered</TD>\n" +
"<TD>DateEntered</TD>\n" +
"<TD>Modified</TD>\n" +
"<TD>DateModified</TD>\n" +
"</TR>\n";
                gatts.set("reference", startReference +
"\n" +
"Linnaeus, C. 1758. Systema Naturae per Regna Tria Naturae secundum Classes, Ordinus, Genera, Species cum Characteriibus, Differentiis Synonymis, Locis. 10th ed., Vol. 1. Holmiae Salvii. 824 p.");
                gatts.set("subsetVariables", 
"???");
                gatts.set("summary", 
"References remain attached to the proper species.\n" +
"\n" +
"We also included a field for the Name Used as Valid (thanks to Emily Capuli) for a species in a given reference (not shown in the BIBLIOGRAPHY window but is available from the REFERENCE INFORMATION window through the All species treated button).\n" +
"\n" +
"This close integration of synonyms and references ensures that publications remain attached to the proper biological species, even if the scientific name changes. It also allows us to print automatically updated lists of nomenclatural changes for our references, from Linnaeus (1758) onwards (see ‘Nomenclatural Changes’, below).");
                gatts.set("title", "FishBase 2004, Bibliography");

            //BRAINS
            } else if (fName.equals("BRAINS")) {
                colNamesRow = 
"<tr>\n" +
"<td>Name</td>\n" +
"<td>Genus</td>\n" +
"<td>Species</td>\n" +
"<td>SpecCode</td>\n" +
"<td>StockCode</td>\n" +
"<td>Locality</td>\n" +
"<td>Number</td>\n" +
"<td>Year</td>\n" +
"<td>BodyWeight</td>\n" +
"<td>BrainWeight</td>\n" +
"<td>EncCoeff</td>\n" +
"<td>EncIndex</td>\n" +
"<td>SL</td>\n" +
"<td>TL</td>\n" +
"<td>BRAINSRefNo</td>\n" +
"<td>Remarks</td>\n" +
"<td>AutoCtr</td>\n" +
"</tr>\n";
                gatts.set("reference", startReference +
"\n" +
"Albert, J., R. Froese, R. Bauchot and H. Ito. 1999. Diversity of brain size in fishes: preliminary analysis of a database including 1174 species in 45 orders, p. 647-656. In B. Séret and J.-Y. Sire (eds.) Proceedings of the 5th Indo-Pacific Fisheries Conference, Noumea, New Caledonia, 3-8 November 1997. Soc. Fr. Ichthyol., Paris, France.\n" +
"\n" +
"Bauchot, M.L. and R. Bauchot. 1986. Encephalization in tropical teleost fishes and its correlation with their locomotory habits, p. 678-690. In T. Uyeno, R. Arai, T. Taniuchi and K. Matsuura (eds.) Indo-Pacific Fish Biology: Proceedings of the Second International Conference on Indo-Pacific Fishes. Ichthyological Society of Japan, Tokyo.\n" +
"\n" +
"Bauchot, R., M. Diagne and J.M. Ribet. 1979. Post-hatching growth and allometry of the teleost brain. J. Hirnforsch. 20:29-34.\n" +
"\n" +
"Bauchot, R., J.M. Ridet and M.-L. Bauchot. 1989. The brain organization of butterflyfishes. Environ. Biol. Fish. 25(1/3):205-219.\n" +
"\n" +
"Chin, X. 1996. A photographic atlas of brains of common Caribbean reef fishes. University of South Florida. B.A. thesis. 62 p.\n" +
"\n" +
"Gould, S.J. 1981. The mismeasure of man. W.W. Norton, New York. 352 p.");
                gatts.set("subsetVariables", 
"Name, Genus, Species, SpecCode, StockCode, Locality, Number, Year, BodyWeight, BrainWeight, EncCoeff, EncIndex, SL, TL, BRAINSRefNo");
                gatts.set("summary", 
"Most fishes have small brains, at least when compared with warm-blooded vertebrates. However, holding this against them would be as silly as trying to draw inference about the worth of different groups of people from the (mismeasured) size of their brains (Gould 1981).\n" +
"\n" +
"Rather, we should realize that fish have evolved the brain size they need, and then use the brain size difference among species of fish to draw inferences on their ‘needs’, i.e., on their niche (see, e.g., Bauchot et al. 1989). The brain size database assembled by Roland Bauchot and his collaborators and kindly made available for inclusion as a table of FishBase allows inferences of this sort. The following describes, based on Bauchot and Bauchot (1986), how this database was created.\n" +
"\n" +
"Over 2,800 brains were dissected from over 900 species of teleost fishes (see Fig. 49). Many of the fishes were collected at tropical and subtropical localities such as the Hawaiian and Marshall Islands, New Caledonia, Queensland, Australia, the Philippines, southwest India, Mauritius and Réunion, Gulf of Oman, northern Red Sea, Senegal and the Caribbean, but also in France and the North Atlantic. All fish were weighed before removal of the brain and their standard and/or total length taken. The brain was cut from the spinal cord at the first spinal nerves, the meninges and blood vessels removed, blotted and weighed, and then preserved in Bouin solution.");
                gatts.set("title", "FishBase 2004, Brains");

            //BROODSTOCK
            } else if (fName.equals("BROODSTOCK")) {
                colNamesRow = 
"<tr>\n" +
"<td>AutoCtr</td>\n" +
"<td>SpecCode</td>\n" +
"<td>Stockcode</td>\n" +
"<td>Species</td>\n" +
"<td>CommonName</td>\n" +
"<td>MainRef</td>\n" +
"<td>CountryCode</td>\n" +
"<td>Locality</td>\n" +
"<td>DataRef</td>\n" +
"<td>BreedingI</td>\n" +
"<td>Fecundity</td>\n" +
"<td>CountriesAndRegions</td>\n" +
"<td>BreedingII</td>\n" +
"<td>BreedingIII</td>\n" +
"<td>CultureSystem</td>\n" +
"<td>MainwaterSource</td>\n" +
"<td>SupplWaterSource</td>\n" +
"<td>TempMin</td>\n" +
"<td>TempMax</td>\n" +
"<td>SpawningTempMin</td>\n" +
"<td>SpawningTempMax</td>\n" +
"<td>SalinMin</td>\n" +
"<td>SalinMax</td>\n" +
"<td>pHMin</td>\n" +
"<td>pHMax</td>\n" +
"<td>OxygenMin</td>\n" +
"<td>OxygenMax</td>\n" +
"<td>HardnessMin</td>\n" +
"<td>HardnessMax</td>\n" +
"<td>BroodstockStockingRate</td>\n" +
"<td>Broodstockfemale</td>\n" +
"<td>Broodstockmale</td>\n" +
"<td>SexRatio</td>\n" +
"<td>EggProduction</td>\n" +
"<td>PostMortality</td>\n" +
"<td>FCR</td>\n" +
"<td>Comments</td>\n" +
"<td>Entered</td>\n" +
"<td>DateEntered</td>\n" +
"<td>Modified</td>\n" +
"<td>DateModified</td>\n" +
"<td>Expert</td>\n" +
"<td>DateChecked</td>\n" +
"</tr>\n";
                gatts.set("reference", startReference +
"");
                gatts.set("subsetVariables", 
"???");
                gatts.set("summary", 
"Control of the Reproductive Cycle -- Although several species can be reared on the basis of eggs and larvae collected from the wild (e.g., milkfish or eels), large-scale production of fry needs a broodstock of captive spawners for reliable production of eggs. The ability to control the reproductive cycle of species under cultivation is thus most important. Such knowledge ensures that hatcheries are able to maximize their production of eggs and fry and thus can tailor their production to the needs of the farms which grow fish up to table size.\n" +
"\n" +
"The BROODSTOCK table gives basic information on biotic and abiotic conditions for proper broodstock management. ");
                gatts.set("title", "FishBase 2004, Broodstock");

            //CIGUA
            } else if (fName.equals("CIGUA")) {
                colNamesRow = 
"<tr>\n" +
"<td>RecordNo</td>\n" +
"<td>CountryCode</td>\n" +
"<td>Country</td>\n" +
"<td>Location</td>\n" +
"<td>LatitudeDeg</td>\n" +
"<td>LatitudeMin</td>\n" +
"<td>NorthSouth</td>\n" +
"<td>LongitudeDeg</td>\n" +
"<td>LongitudeMin</td>\n" +
"<td>EastWest</td>\n" +
"<td>Date</td>\n" +
"<td>FishYN</td>\n" +
"<td>Crab</td>\n" +
"<td>Lobst</td>\n" +
"<td>OthCrust</td>\n" +
"<td>Gastr</td>\n" +
"<td>Bivavle</td>\n" +
"<td>OtherMoll</td>\n" +
"<td>Beach</td>\n" +
"<td>ReefPatch</td>\n" +
"<td>Lagoon</td>\n" +
"<td>River</td>\n" +
"<td>Mangrove</td>\n" +
"<td>OuterReef</td>\n" +
"<td>OpenSea</td>\n" +
"<td>FreshNoIce</td>\n" +
"<td>FreshCE</td>\n" +
"<td>Frozen</td>\n" +
"<td>Salted</td>\n" +
"<td>Dried</td>\n" +
"<td>Smoked</td>\n" +
"<td>Pickled</td>\n" +
"<td>Head</td>\n" +
"<td>Flesh</td>\n" +
"<td>Skin</td>\n" +
"<td>Liver</td>\n" +
"<td>Roe</td>\n" +
"<td>OtherOrga</td>\n" +
"<td>Unprepared</td>\n" +
"<td>Marinated</td>\n" +
"<td>Cooked</td>\n" +
"<td>AteThisM</td>\n" +
"<td>FeltSick</td>\n" +
"<td>WereAdmit</td>\n" +
"<td>LocalName</td>\n" +
"<td>EnglishNa</td>\n" +
"<td>Scientific</td>\n" +
"<td>Vendor</td>\n" +
"<td>AreaCaught</td>\n" +
"<td>DateEaten</td>\n" +
"<td>TimeEaten</td>\n" +
"<td>DateSick</td>\n" +
"<td>TimeSick</td>\n" +
"<td>BurningPa</td>\n" +
"<td>TinglingN</td>\n" +
"<td>UrinateDi</td>\n" +
"<td>DifficultW</td>\n" +
"<td>DifficultT</td>\n" +
"<td>DifficultY</td>\n" +
"<td>EyeIrrita</td>\n" +
"<td>PinPricki</td>\n" +
"<td>StrangeTa</td>\n" +
"<td>SkinItchi</td>\n" +
"<td>ExcessSal</td>\n" +
"<td>ExcessSwt</td>\n" +
"<td>Diarrhoea</td>\n" +
"<td>Vomiting</td>\n" +
"<td>FeverChil</td>\n" +
"<td>Headache</td>\n" +
"<td>JointAche</td>\n" +
"<td>MuscleCra</td>\n" +
"<td>Pulse</td>\n" +
"<td>Systolic</td>\n" +
"<td>Diastolic</td>\n" +
"<td>Pupils</td>\n" +
"<td>Death</td>\n" +
"<td>Comments</td>\n" +
"<td>FamCode</td>\n" +
"<td>SpecCode</td>\n" +
"<td>Genus</td>\n" +
"<td>Species</td>\n" +
"<td>Entered</td>\n" +
"<td>DateEntered</td>\n" +
"<td>Modified</td>\n" +
"<td>DateModified</td>\n" +
"<td>Expert</td>\n" +
"<td>DateChecked</td>\n" +
"</tr>\n";
                gatts.set("reference", startReference +
"\n" +
"Anon. 1997. Ciguatera for health care professionals. Sea Grant in the Caribbean. Jan.-March, p.5.\n" +
"\n" +
"Brody, R.W. 1972. Fish poisoning in the Eastern Caribbean. Proc. Gulf Caribb. Fish. Inst. 24: 100-116.\n" +
"\n" +
"Dalzell, P. 1992. Ciguatera fish poisoning and fisheries development in the South Pacific. Bull. Soc. Pathol. Exot. 85 (5): 435-444.\n" +
"\n" +
"Dalzell, P. 1993. Management of ciguatera fish poisoning in the South Pacific. Mem. Queensland Mus. 34(3): 471-480.\n" +
"\n" +
"Lewis, R.J. and M.J. Holmes. 1993. Origin and transfer of toxins involved in ciguatera. Comp. Biochem. Physiol. 160C(3): 615-628.\n" +
"\n" +
"National Research Council (NRC). 1999. From monsoons to microbes: understanding the ocean’s role in human health. National Academy Press, Washington, D.C. 132 p.\n" +
"\n" +
"Olsen, D.A., D.W. Nellis and R.S. Wood. 1984. Ciguatera in the Eastern Caribbean. Mar. Fish. Rev. 46(1): 13-18.\n" +
"\n" +
"Sadovy, Y. 1999. Ciguatera – a continuing problem for Hong Kong’s consumers, live reef fish traders and high-value target species. SPC Live Reef Fish Info. Bull. No. 6. (December 1999):3-4.\n" +
"\n" +
"Tosteson, T.R., D.L. Ballantine and H.D. Durst. 1988. Seasonal frequency of ciguatoxic barracuda in Southwest Puerto Rico. Toxicon. 26(9):795-801.");
                gatts.set("subsetVariables", 
"???");
                gatts.set("summary", 
"Approximately 50,000 people are poisoned annually by ciguatoxic seafood. Ciguatera, first recognized in the 1550s in the Caribbean (NRC 1999), is a form of ichthyotoxism caused by the consumption of mainly reef fish contaminated with the ciguatoxin class of lipid soluble toxins. An estimated 50,000 victims worldwide annually are reported with 20,000-30,000 cases of ciguatera in the Caribbean in Puerto Rico and U.S. Virgin Islands (Anon. 1997; Bomber and Aikman 1988/89 cited in NRC 1999). Only 20-40% of cases are estimated to be reported (NRC 1999). The toxins causing ciguatera have been identified by the primary vector, the dinoflagellate Gambierdiscus toxicus, an epiphyte living on a range of calcareous macroalgae and other substrates on coral reefs. G. toxicus is widely distributed on coral reefs and lagoons but is most prolific in shallow waters (3-15 m) away from terrestrial influences. Herbivorous reef fish browsing on reef algae ingest G. toxicus and concentrate the ciguatoxins in the gut and muscle tissue. Piscivorous reef fish may then become toxic through the consumption of herbivorous fishes and the concentration of the toxins up the food web. Other benthic dinoflagellates such as Prorocentrums, Ostreopsis and Coolia are also linked to ciguatera outbreaks (Tosteson et al. 1988; NRC 1999).\n" +
"\n" +
"Ciguatoxins are not destroyed by cooking and no routine tests are performed to identify contaminated fish, or to predict the timing or occurrence of ciguatera outbreaks on reefs. Ciguatera poisonings are characterized by a range of often severe gastrointestinal and neurological symptoms. Intoxicated individuals may experience diarrhea, vomiting, lethargy, numbness, reversal of temperature perception, itching, tingling and muscular pains. Some of these symptoms such as itching and muscular pain may persist for several months. A recurrence of neurological symptoms may be brought on by consumption of alcohol or certain foods such as other fish, fish-flavored food products, peanut butter, and meat such as chicken and pork. A thorough review of the clinical, epidemiological and ecological aspects of ciguatera has been given by Lewis and Holmes (1993).");
                gatts.set("title", "FishBase 2004, Ciguatera");


            //CLASSES
            } else if (fName.equals("CLASSES")) {
                colNamesRow = 
"<tr>\n" +
"<td>ClassNum</td>\n" +
"<td>Class</td>\n" +
"<td>Synonyms</td>\n" +
"<td>CommonNames</td>\n" +
"<td>Diagnosis</td>\n" +
"<td>MainRef</td>\n" +
"<td>Picture</td>\n" +
"<td>Phylum</td>\n" +
"<td>InterTaxa</td>\n" +
"<td>NODCode</td>\n" +
"<td>Control</td>\n" +
"<td>NODCode2</td>\n" +
"</tr>\n";
                gatts.set("reference", startReference +
"");
                gatts.set("subsetVariables", 
"???");
                gatts.set("summary", 
"This table has information about the classes of fish.");
                gatts.set("title", "FishBase 2004, Classes");

            //COMMNAMES
            } else if (fName.equals("COMNAMES")) {
                colNamesRow = 
"<tr>\n" +
"<td>Name</td>\n" +
"<td>ComName</td>\n" +
"<td>StockCode</td>\n" +
"<td>SpecCode</td>\n" +
"<td>CountryCode</td>\n" +
"<td>Language</td>\n" +
"<td>Script</td>\n" +
"<td>UnicodeText</td>\n" +
"<td>NameType</td>\n" +
"<td>TradeName</td>\n" +
"<td>TradeNameRef</td>\n" +
"<td>ComNamesRefNo</td>\n" +
"<td>Size</td>\n" +
"<td>Sex</td>\n" +
"<td>Language2</td>\n" +
"<td>Locality2</td>\n" +
"<td>Rank</td>\n" +
"<td>Remarks</td>\n" +
"<td>SecondWord</td>\n" +
"<td>ThirdWord</td>\n" +
"<td>FourthWord</td>\n" +
"<td>Entered</td>\n" +
"<td>DateEntered</td>\n" +
"<td>Modified</td>\n" +
"<td>DateModified</td>\n" +
"<td>Expert</td>\n" +
"<td>DateChecked</td>\n" +
"<td>Core</td>\n" +
"<td>Modifier1</td>\n" +
"<td>Modifier2</td>\n" +
"<td>CLOFFSCA</td>\n" +
"<td>AutoCtr</td>\n" +
"</tr>\n";
                gatts.set("reference", startReference +
"Berlin, B. 1992. Ethnobiological classifications: principles of categorization of plants and animals in traditional societies. Princeton University Press, Princeton. 335 p.\n" +
"\n" +
"Bingen, H. von. 1286. Das Buch von den Fischen. Edited by P. Riethe, 1991. Otto Müller Verlag, Salzburg. 150 p.\n" +
"\n" +
"Brewer, D.J. and R.F. Freeman. 1989. Fish and fishing in ancient Egypt. Aris and Philips, Warminster, England. 109 p.\n" +
"\n" +
"Coppola, S.R., W. Fischer, L. Garibaldi, N. Scialabba and K.E. Carpenter. 1994. SPECIESDAB: Global species database for fishery purposes. User’s manual. FAO Computerized Information Series (Fisheries) No. 9. rome, FAO. 103 p.\n" +
"\n" +
"Cotte, M.J. 1944. Poissons et animaux aquatiques au temps de Pline. Paul Lechevalier, Paris. 265 p.\n" +
"\n" +
"Foale, S. 1998. What’s in a name? An analysis of the West Nggela (Solomon Islands) fish taxonomy. SPC Trad. Mar. Resour. Manage. Knowl. Info. Bull. 9:3-19.\n" +
"\n" +
"Froese, R. 1990. FishBase: an information system to support fisheries and aquaculture research. Fishbyte 8(3):21-24.\n" +
"\n" +
"Grabda, E. and T. Heese. 1991. Polskie nazewnictwo popularne Kraglonste i ryby. Cyclostomata et Pisces. Wyzsza Szkola Inzynierska w Koszaline. Koszalin, Poland. 171 p.\n" +
"\n" +
"Grimes, B., Editor. 1992. Ethnologue: Languages of the world. 12th ed. Summer Institute of Linguistics, Dallas, Texas. 938 p.\n" +
"\n" +
"Herre, A.W.C.T. and A.F. Umali. 1948. English and local common names of Philippine fishes. U.S. Dept. of Interior and Fish and Wildlife Serv. Circular No. 14. U.S. Gov’t. Printing Office, Washington. 128 p.\n" +
"\n" +
"Hunn, E. 1980. Sahaptin fish classification. Northw. Anthropol. Res. Notes 14(1):1-19.\n" +
"\n" +
"Johannes, R.E. 1981. Words of the lagoon: fishing and marine lore in Palau District of Micronesia. University of California Press, Berkeley. 245 p.\n" +
"\n" +
"Kotlyar, A.N. 1984. Dictionary of names of marine fishes on the six languages. All Union Research Institute of Marine Fisheries and Oceanography, Moscow.\n" +
"\n" +
"Masuda, H., K. Amaoka, C. Araga, T. Uyeno and T. Yoshino. 1984. The fishes of the Japanese Archipelago. Vol. 1 (text). Tokai University Press, Tokyo, Japan. 437 p. (text), 370 pls.\n" +
"\n" +
"Mohsin, A.K.M., M.A. Ambak and M.M.A. Salam. 1993. Malay, English and scientific names of the fishes of Malaysia. Faculty of Fisheries and Marine Science, Universiti Pertanian Malaysia, Selangor Darul Ehsan, Malaysia, Occas. Publ. 11.\n" +
"\n" +
"Negedly, R., Compiler. 1990. Elsevier’s dictionary of fishery, processing, fish and shellfish names of the world. Elsevier Science Publishers, Amsterdam, The Netherlands. 623 p.\n" +
"\n" +
"Organisation for Economic Co-operation and Development. 1990. Multilingual dictionary of fish and fish products. Fishing News Books, Oxford.\n" +
"\n" +
"Palomares, M.L.D. and D. Pauly. 1993. FishBase as a repository of ethno-ichthyology or indigenous knowledge of fishes. Paper presented at the International Symposium on Indigenous Knowledge (IK) and Sustainable Development, 20-26 September, Silang, Cavite, Philippines (Abstract in Indigenous Knowledge and Development Monitor 1(2):18).\n" +
"\n" +
"Palomares, M.L.D., R. Froese and D. Pauly. 1993. On traditional knowledge, fish and database: a call for contributions. SPC Trad. Mar. Resour. Manage. Knowl. Info. Bull. (2):17-19.\n" +
"\n" +
"Pauly, D., M.L.D. Palomares and R. Froese. 1993. Some prose on a database of indigenous knowledge on fish. Indigenous Knowledge and Development Monitor 1(1):26-27.\n" +
"\n" +
"Randolph, S. and M. Snyder. 1993. The seafood list: FDA's guide to acceptable market names for seafood sold in interstate commerce. U.S. Government Printing Office, Washington, USA. pag. var.\n" +
"\n" +
"Robins, C. R., R.M. Bailey, C.E. Bond, J.R. Brooker, E.A. Lachner, R.N. Lea and W.B. Scott. 1980. A list of common and scientific names of fishes from the United States and Canada. 4th ed. Am. Fish. Soc. Spec. Publ. 12: 1-174.\n" +
"\n" +
"Robins, C.R., R.M. Bailey, C.E. Bond, J.R. Brooker, E.A. Lachner, R.N. Lea and W.B. Scott. 1991a. Common and scientific names of fishes from the United States and Canada. Am. Fish. Soc. Spec. Publ. 20, 183 p.\n" +
"\n" +
"Robins, C.R., R.M. Bailey, C.E. Bond, J.R. Brooker, E.A. Lachner, R.N. Lea and W.B. Scott. 1991b. World fishes important to North Americans. Exclusive of species from continental waters of the United States and Canada. Am. Fish. Soc. Spec. Publ. 21, 243 p.\n" +
"\n" +
"Ruhlen, M. 1991. A guide to the world’s languages. Vol. 1: Classification. With a postscript on recent developments. Stanford University Press, Stanford. 433 p.\n" +
"\n" +
"Sanches, J.G. 1989. Nomenclatura portuguesa de organismos aquaticos. Publicações Avulsas do I.N.I.P. No. 14, Lisboa. 322 p.\n" +
"\n" +
"Thompson, D.W. 1947. A glossary of Greek fishes. Oxford University Press, London. 302 p.\n" +
"\n" +
"Yearsley, G.K., P.R. Last and G.B. Morris. 1997. Codes for Australian Aquatic Biota (CAAB): an upgraded and expanded species coding system for Australian fisheries databases. CSIRO Marine Laboratories, Report 224. CSIRO, Australia.\n" +
"\n" +
"Zaneveld, J.S. 1983. Caribbean fish life. Index to the local and scientific names of the marine fishes and fishlike invertebrates of the Caribbean area (Tropical Western Central Atlantic Ocean). E.J. Brill / Dr. W. Backhuys, Leiden. 163 p.");
                gatts.set("subsetVariables", 
"???");
                gatts.set("summary", 
"Common names are all that most people know about most fish. Claiming that the common names of fish are one of their most important attributes is an understatement. In fact, common names are all that most people know about most fish as shown by the fact that most people accessing FishBase on the Internet do so by common name.\n" +
"\n" +
"Hence, FishBase would not be complete without common names. This fact has been considered very early in the design of FishBase (Froese 1990) and has resulted in the compilation of over 107,000 common names, probably the largest collection of its kind. It has taken us a long time, to realize, however, that each pair of ‘country’ and ‘language’ fields uniquely define a culture, and that a large fraction of what the people belonging to a certain culture know about fishes (i.e., local knowledge) can therefore be captured through the COMMON NAMES table including these fields. ");
                gatts.set("title", "FishBase 2004, Common Names");

            //COUNTRY
            } else if (fName.equals("COUNTRY")) {
                colNamesRow = 
"<tr>\n" +
"<td>PAESE</td>\n" +
"<td>CountryCode</td>\n" +
"<td>ABB</td>\n" +
"<td>ISOCode</td>\n" +
"<td>GreekOriginal</td>\n" +
"<td>Greek</td>\n" +
"<td>French</td>\n" +
"<td>Spanish</td>\n" +
"<td>German</td>\n" +
"<td>Dutch</td>\n" +
"<td>Portuguese</td>\n" +
"<td>Italian</td>\n" +
"<td>Swedish</td>\n" +
"<td>Note</td>\n" +
"<td>Capital</td>\n" +
"<td>LatDeg</td>\n" +
"<td>LatMin</td>\n" +
"<td>NorthSouth</td>\n" +
"<td>LongDeg</td>\n" +
"<td>LongMin</td>\n" +
"<td>EastWest</td>\n" +
"<td>Remark</td>\n" +
"<td>GeographicArea</td>\n" +
"<td>Region</td>\n" +
"<td>Continent</td>\n" +
"<td>AreaCodeInland</td>\n" +
"<td>FAOAreaInland</td>\n" +
"<td>AreaCodeMarineI</td>\n" +
"<td>FAOAreaMarineI</td>\n" +
"<td>AreaCodeMarineII</td>\n" +
"<td>FAOAreaMarineII</td>\n" +
"<td>AreaCodeMarineIII</td>\n" +
"<td>FAOAreaMarineIII</td>\n" +
"<td>AreaCodeMarineIV</td>\n" +
"<td>FAOAreaMarineIV</td>\n" +
"<td>AreaCodeMarineV</td>\n" +
"<td>FAOAreaMarineV</td>\n" +
"<td>Population</td>\n" +
"<td>PopulationYear</td>\n" +
"<td>PopulationRate</td>\n" +
"<td>PopulationRef</td>\n" +
"<td>CoastalPopulation</td>\n" +
"<td>CoastPopRef</td>\n" +
"<td>Area</td>\n" +
"<td>Coastline</td>\n" +
"<td>CoastlineRef</td>\n" +
"<td>ShelfArea</td>\n" +
"<td>ShelfRef</td>\n" +
"<td>EECarea</td>\n" +
"<td>EEZRef</td>\n" +
"<td>Language</td>\n" +
"<td>Currency</td>\n" +
"<td>MarineCount</td>\n" +
"<td>MarineFamCount</td>\n" +
"<td>CompleteMarine</td>\n" +
"<td>MarineLit</td>\n" +
"<td>MarineFamLit</td>\n" +
"<td>CheckMarineRef</td>\n" +
"<td>MarineFlag</td>\n" +
"<td>FreshwaterCount</td>\n" +
"<td>FreshFamCount</td>\n" +
"<td>CompleteFresh</td>\n" +
"<td>FreshwaterLit</td>\n" +
"<td>FreshFamLit</td>\n" +
"<td>CheckFreshRef</td>\n" +
"<td>FreshFlag</td>\n" +
"<td>TotalCount</td>\n" +
"<td>TotalFamCount</td>\n" +
"<td>TotalComplete</td>\n" +
"<td>TotalLit</td>\n" +
"<td>TotalFamLit</td>\n" +
"<td>TotalRef</td>\n" +
"<td>LastUpdate</td>\n" +
"<td>MorphCountFresh</td>\n" +
"<td>OccurCountFresh</td>\n" +
"<td>PicCountFresh</td>\n" +
"<td>GrowthCountFresh</td>\n" +
"<td>FoodCountFresh</td>\n" +
"<td>DietCountFresh</td>\n" +
"<td>ReproductionCountFresh</td>\n" +
"<td>SpawningCountFresh</td>\n" +
"<td>MorphCountMarine</td>\n" +
"<td>OccurCountMarine</td>\n" +
"<td>PicCountMarine</td>\n" +
"<td>GrowthCountMarine</td>\n" +
"<td>FoodCountMarine</td>\n" +
"<td>DietCountMarine</td>\n" +
"<td>ReproductionCountMarine</td>\n" +
"<td>SpawningCountMarine</td>\n" +
"<td>LatDegFill</td>\n" +
"<td>LatMinFill</td>\n" +
"<td>NorthSouthFill</td>\n" +
"<td>LongDegFill</td>\n" +
"<td>LongMinFill</td>\n" +
"<td>EastWestFill</td>\n" +
"<td>MLatDegFill</td>\n" +
"<td>MLatMinFill</td>\n" +
"<td>MNorthSouthFill</td>\n" +
"<td>MLongDegFill</td>\n" +
"<td>MLongMinFill</td>\n" +
"<td>MEastWestFill</td>\n" +
"<td>MLatDegFill2</td>\n" +
"<td>MLatMinFill2</td>\n" +
"<td>MNorthSouthFill2</td>\n" +
"<td>MLongDegFill2</td>\n" +
"<td>MLongMinFill2</td>\n" +
"<td>MEastWestFill2</td>\n" +
"<td>MLatDegFill3</td>\n" +
"<td>MLatMinFill3</td>\n" +
"<td>MNorthSouthFill3</td>\n" +
"<td>MLongDegFill3</td>\n" +
"<td>MLongMinFill3</td>\n" +
"<td>MEastWestFill3</td>\n" +
"<td>MLatDegFill4</td>\n" +
"<td>MLatMinFill4</td>\n" +
"<td>MNorthSouthFill4</td>\n" +
"<td>MLongDegFill4</td>\n" +
"<td>MLongMinFill4</td>\n" +
"<td>MEastWestFill4</td>\n" +
"<td>MLatDegFill5</td>\n" +
"<td>MLatMinFill5</td>\n" +
"<td>MNorthSouthFill5</td>\n" +
"<td>MLongDegFill5</td>\n" +
"<td>MLongMinFill5</td>\n" +
"<td>MEastWestFill5</td>\n" +
"<td>NorthernLatitude</td>\n" +
"<td>NorthernLatitudeNS</td>\n" +
"<td>SouthernLatitude</td>\n" +
"<td>SouthernLatitudeNS</td>\n" +
"<td>WesternLongitude</td>\n" +
"<td>WesternLongitudeEW</td>\n" +
"<td>EasternLongitude</td>\n" +
"<td>EasternLongitudeEW</td>\n" +
"<td>OtherLanguage</td>\n" +
"<td>Geography</td>\n" +
"<td>GeographyRef</td>\n" +
"<td>Hydrography</td>\n" +
"<td>HydrographyRef</td>\n" +
"<td>CommentFresh</td>\n" +
"<td>RefFresh1</td>\n" +
"<td>RefFresh2</td>\n" +
"<td>RefFresh3</td>\n" +
"<td>FreshPrimary</td>\n" +
"<td>FreshSecondary</td>\n" +
"<td>FreshInto</td>\n" +
"<td>InFisheriesReported</td>\n" +
"<td>InFisheriesPotential</td>\n" +
"<td>InAquaReported</td>\n" +
"<td>InAquaPotential</td>\n" +
"<td>ExportReported</td>\n" +
"<td>ExportPotential</td>\n" +
"<td>SportReported</td>\n" +
"<td>SportPotential</td>\n" +
"<td>EndemicReported</td>\n" +
"<td>EndemicPotential</td>\n" +
"<td>Threatened</td>\n" +
"<td>ProtectedReported</td>\n" +
"<td>ProtectedPotential</td>\n" +
"<td>ACP</td>\n" +
"<td>Developed</td>\n" +
"<td>Marine</td>\n" +
"<td>Brackish</td>\n" +
"<td>MarineInto</td>\n" +
"<td>MarineInFisheriesReported</td>\n" +
"<td>MarineInFisheriesPotential</td>\n" +
"<td>MarineInAquaReported</td>\n" +
"<td>MarineInAquaPotential</td>\n" +
"<td>MarineExportReported</td>\n" +
"<td>MarineExportPotential</td>\n" +
"<td>MarineSportReported</td>\n" +
"<td>MarineSportPotential</td>\n" +
"<td>MarineEndemicReported</td>\n" +
"<td>MarineEndemicPotential</td>\n" +
"<td>MarineThreatened</td>\n" +
"<td>MarineProtectedReported</td>\n" +
"<td>MarineProtectedPotential</td>\n" +
"<td>NatFishDB</td>\n" +
"<td>NatFishDB2</td>\n" +
"<td>Factbook</td>\n" +
"<td>NatFishAut</td>\n" +
"<td>TradeNames</td>\n" +
"<td>Entered</td>\n" +
"<td>DateEntered</td>\n" +
"<td>Modified</td>\n" +
"<td>DateModified</td>\n" +
"<td>Expert</td>\n" +
"<td>DateChecked</td>\n" +
"</tr>\n";
                gatts.set("reference", startReference +
"Allen, G.R. 1989. Freshwater fishes of Australia. T.F.H. Publications, Neptune City, New Jersey. 240 p.\n" +
"\n" +
"FAO. 1995. FAO yearbook: Fishery statistics – Catches and landings 1993. Vol. 76. Food and Agriculture Organization of the United Nations, Rome, Italy. 687 p.\n" +
"\n" +
"Randall, J.E., G.R. Allen and R.C. Steene. 1997. Fishes of the Great Barrier Reef and Coral Sea. Revised and expanded edition. Crawford House Publishing Pty. Ltd. Bathurst, NSW Australia. 557 p.\n" +
"\n" +
"Skelton, P.H. 1993. A complete guide to the freshwater fishes of Southern Africa. Southern Book Publisher, South Africa. 388 p.");
                gatts.set("subsetVariables", 
"???");
                gatts.set("summary", 
"The COUNTRIES table lists all countries where a species has been reported to occur.\n" +
"\n" +
"Country governments are the political bodies that deal with fisheries management, research and conservation at the national level. It is therefore important to know all the countries where a species occurs, and vice-versa. As mentioned above, the distributional range of many species is not well established. Country-specific checklists of fishes prepared by non-taxonomists often contain misidentifications and generally cannot be verified; on the other hand, complete checklists published by taxonomists and based on verifiable specimen collections do not exist for many countries. ");
                gatts.set("title", "FishBase 2004, Country");

            //SPECIES
            } else if (fName.equals("SPECIES")) {
                colNamesRow = 
"<tr>\n" +
"<td>Genus</td>\n" +
"<td>Species</td>\n" +
"<td>SpecCode</td>\n" +
"<td>SpeciesRefNo</td>\n" +
"<td>Author</td>\n" +
"<td>AuthorRef</td>\n" +
"<td>FBname</td>\n" +
"<td>FamCode</td>\n" +
"<td>Subfamily</td>\n" +
"<td>Source</td>\n" +
"<td>Remark</td>\n" +
"<td>Fresh</td>\n" +
"<td>Brack</td>\n" +
"<td>Saltwater</td>\n" +
"<td>DemersPelag</td>\n" +
"<td>AnaCat</td>\n" +
"<td>DepthRangeShallow</td>\n" +
"<td>DepthRangeDeep</td>\n" +
"<td>DepthRangeRef</td>\n" +
"<td>DepthRangeComShallow</td>\n" +
"<td>DepthRangeComDeep</td>\n" +
"<td>DepthComRef</td>\n" +
"<td>LongevityWild</td>\n" +
"<td>LongevityWildRef</td>\n" +
"<td>LongevityCaptive</td>\n" +
"<td>LongevityCapRef</td>\n" +
"<td>Length</td>\n" +
"<td>LTypeMaxM</td>\n" +
"<td>LengthFemale</td>\n" +
"<td>LTypeMaxF</td>\n" +
"<td>MaxLengthRef</td>\n" +
"<td>CommonLength</td>\n" +
"<td>LTypeComM</td>\n" +
"<td>CommonLengthF</td>\n" +
"<td>LTypeComF</td>\n" +
"<td>CommonLengthRef</td>\n" +
"<td>Weight</td>\n" +
"<td>WeightFemale</td>\n" +
"<td>MaxWeightRef</td>\n" +
"<td>Pic</td>\n" +
"<td>PictureFemale</td>\n" +
"<td>LarvaPic</td>\n" +
"<td>EggPic</td>\n" +
"<td>ImportanceRef</td>\n" +
"<td>Importance</td>\n" +
"<td>Remarks7</td>\n" +
"<td>LandingStatistics</td>\n" +
"<td>Landings</td>\n" +
"<td>MainCatchingMethod</td>\n" +
"<td>II</td>\n" +
"<td>MSeines</td>\n" +
"<td>MGillnets</td>\n" +
"<td>MCastnets</td>\n" +
"<td>MTraps</td>\n" +
"<td>MSpears</td>\n" +
"<td>MTrawls</td>\n" +
"<td>MDredges</td>\n" +
"<td>MLiftnets</td>\n" +
"<td>MHooksLines</td>\n" +
"<td>MOther</td>\n" +
"<td>UsedforAquaculture</td>\n" +
"<td>LifeCycle</td>\n" +
"<td>AquacultureRef</td>\n" +
"<td>UsedasBait</td>\n" +
"<td>BaitRef</td>\n" +
"<td>Aquarium</td>\n" +
"<td>AquariumFishII</td>\n" +
"<td>AquariumRef</td>\n" +
"<td>GameFish</td>\n" +
"<td>GameRef</td>\n" +
"<td>Dangerous</td>\n" +
"<td>DangerousRef</td>\n" +
"<td>Electrogenic</td>\n" +
"<td>ElectroRef</td>\n" +
"<td>Complete</td>\n" +
"<td>ASFA</td>\n" +
"<td>Entered</td>\n" +
"<td>DateEntered</td>\n" +
"<td>Modified</td>\n" +
"<td>DateModified</td>\n" +
"<td>Expert</td>\n" +
"<td>DateChecked</td>\n" +
"<td>Coordinator</td>\n" +
"<td>Synopsis</td>\n" +
"<td>DateSynopsis</td>\n" +
"<td>Flag</td>\n" +
"<td>Comments</td>\n" +
"<td>VancouverAquarium</td>\n" +
"<td>Profile</td>\n" +
"</tr>\n";
                gatts.set("reference", startReference +
"Daget, J., J.-P. Gosse, G.G. Teugels and D.F.E. Thys van den Audenaerde, Editors. 1984. Checklist of the freshwater fishes of Africa (CLOFFA). Off. Rech. Scient. Tech. Outre-Mer, Paris, and Musée Royal de l’Afrique Centrale, Tervuren. 410 p.\n" +
"\n" +
"Daget, J., J.C. Hureau, C. Karrer, A. Post and L. Saldanha, Editors. 1990. Check-list of the fishes of the eastern tropical Atlantic (CLOFETA). Junta Nacional de Investigaçao Cientifica e Tecnológica, Lisbon, Europ. Ichthyol. Union, Paris and UNESCO, Paris. 519 p.\n" +
"\n" +
"Eschmeyer, W.N., Editor. 1998. Catalog of fishes. Special Publication, California Academy of Sciences, San Francisco. 3 vols. 2905 p.\n" +
"\n" +
"FAO. 1995. FAO yearbook: Fishery statistics – Catches and landings 1993. Vol. 76. Food and Agriculture Organization of the United Nations, Rome, Italy. 687 p.\n" +
"\n" +
"FAO-FIDI. 1994. International Standard Statistical Classification of Aquatic Animals and Plants (ISSCAAP). Fishery Information, Data and Statistics Service, Fisheries Department, FAO, Rome, Italy.\n" +
"\n" +
"Holthus, P.F. and J.E. Maragos. 1995. Marine ecosystem classification for the tropical island Pacific, p. 239-278. In J.E. Maragos, M.N.A. Peterson, L.G. Eldredge, J.E. Bardach and H.F. Takeuchi (eds.) Marine and coastal biodiversity in the tropical island Pacific region. Vol. 1. Species Management and Information Management Priorities. East-West Center, Honolulu, Hawaii. 424 p.\n" +
"\n" +
"Kottelat, M., A.J. Whitten, S.N. Kartikasari and S. Wirjoatmodjo. 1993. Freshwater fishes of Western Indonesia and Sulawesi = Ikan air tawar Indonesia Bagian Barat dan Sulawesi. Periplus Editions, Hong Kong. 293 p.\n" +
"\n" +
"Linnaeus, C. 1758. Systema Naturae per Regna Tria Naturae secundum Classes, Ordinus, Genera, Species cum Characteribus, Differentiis Synonymis, Locis. 10th ed., Vol. 1. Holmiae Salvii. 824 p.\n" +
"\n" +
"Mago-Leccia, F. 1994. Electric fishes of the continental waters of America. Fundacion para el Desarrollo de las Ciencias Fisicas, Matematicas y Naturales (FUDECI), Biblioteca de la Academia de Ciencias Fisicas. Matematicas y Naturales, Caracas, Vol. XXIX . 206 p. + 13 tables.\n" +
"\n" +
"Moller, P. 1995. Electric fishes: history and behavior. Chapman and Hall, London. 584 p.\n" +
"\n" +
"Myers, R.F. 1999. Micronesian reef fishes. Coral Graphics, Barrigada, Guam. 216 p.\n" +
"\n" +
"Nielsen, J.G., D.M. Cohen, D.F. Markle and C.R. Robins. 1999. Ophidiiform fishes of the world (Order Ophidiiformes). An annotated and illustrated catalogue of pearlfishes, cusk-eels, brotulas and other ophidiiform fishes known to date. FAO Fish. Synop. 125(18). 178 p.\n" +
"\n" +
"Pietsch, T.W. and D.B. Grobecker. 1987. Frogfishes of the world. Stanford University Press, Stanford. 420 p.\n" +
"\n" +
"Randall, J.E. 2000. Revision of the Indo-Pacific labrid fishes of the genus Stethojulis, with descriptions of two new species. Indo-Pac. Fish. 31:42 p.\n" +
"\n" +
"Shao, K.-T., S.-C. Shen, T.-S. Chiu and C.-S. Tzeng. 1992. Distribution and database of fishes in Taiwan, p. 173-206. In C.-Y. Peng (ed.) Collections of research studies on ‘Survey of Taiwan biological resources and information management’. Vol. 2. Institute of Botany, Academia Sinica, Taiwan.\n" +
"\n" +
"Smith, M.M. and P.C. Heemstra, Editors. 1995. Revised edition of Smith’s sea fishes. Springer-Verlag, Berlin. 1047 p.\n" +
"\n" +
"Smith-Vaniz, W.F., B.B. Collette and B.E. Luckhurst. 1999. Fishes of Bermuda: history, zoogeography, annotated checklist, and identification keys. ASIH Spec. Publ. No. 4. 424 p.\n");
                gatts.set("subsetVariables", 
"Genus, Species, SpecCode, SpeciesRefNo, Author, AuthorRef, FBname, FamCode, Subfamily, Fresh, Brack, Saltwater, DemersPelag, AnaCat, DepthRangeComShallow, DepthRangeComDeep, LongevityWild, CommonLength, Weight, Pic, PictureFemale, LarvaPic, EggPic, MainCatchingMethod");
                gatts.set("summary", 
"The SPECIES table is the backbone of FishBase, and has the scientific name as its basic unit. Every bit of information in FishBase is attached directly or indirectly to at least one species and it is mostly through this table that information is accessed.\n" +
"\n" +
"The SPECIES table covers all of the estimated 25,000 extant fishes.\n" +
"\n" +
"The SPECIES table presents the valid scientific name and author of a species or subspecies and assigns it to a family, order and class. Where available, a unique English common name is given (see discussion on FishBase name below). Additional information in the SPECIES table relates to maximum age and size, habitat, uses, and general biological remarks. The references used to derive the information are given.");
                gatts.set("title", "FishBase 2004, Species");

            /* 
            //
            } else if (fName.equals("???")) {
                colNamesRow = 
"<tr>\n" +
???
"</tr>\n";
                gatts.set("reference", startReference +
"???other ref.");
                gatts.set("subsetVariables", 
"???");
                gatts.set("summary", 
"???");
                gatts.set("title", "FishBase 2004, ???");
            */
            } else {
                throw new RuntimeException("dataset-specific info not yet defined for " + fName);
            }

            //read and preprocess the file
            String html[] = File2.readFromFileUtf8(htmlDir + fName + ".html");
            if (html[0].length() > 0)
                throw new RuntimeException(html[0]);
            html[1] = String2.replaceAll(html[1], " DIR=LTR", "");
            html[1] = String2.replaceAll(html[1], " ALIGN=RIGHT", "");
            html[1] = String2.replaceAll(html[1], " ALIGN=LEFT", "");
            html[1] = String2.replaceAll(html[1], "</TD>", "");
            html[1] = String2.replaceAll(html[1], "</CAPTION>", "</CAPTION>\n" + colNamesRow);

            //read the .html table
            Table table = new Table();
            table.readHtml(htmlDir + names[i], html[1], 0, false, true); //2nd row not units, simplify
            table.globalAttributes().set(gatts);
            int nRows = table.nRows();

            //convert lat lon
            int latDeg = table.findColumnNumber("LatitudeDeg");
            if (latDeg >= 0 && 
                table.findColumnNumber("LatitudeMin") == latDeg + 1 &&
                table.findColumnNumber("NorthSouth")  == latDeg + 2) {
                FloatArray lat = new FloatArray(nRows, false);
                PrimitiveArray latDegPA = table.getColumn(latDeg);
                PrimitiveArray latMinPA = table.getColumn(latDeg + 1);
                PrimitiveArray NSPA     = table.getColumn(latDeg + 2);
                for (int row = 0; row < nRows; row++) {
                    String ns = NSPA.getString(row).toLowerCase();
                    int factor = 1;
                    if      (ns.startsWith("s")) factor = -1;
                    else if (ns.startsWith("n")) factor = 1;
                    else if (ns.startsWith("" )) factor = 1;
                    else String2.log("Unexpected NorthSouth=" + ns);
                    lat.add((latDegPA.getFloat(row) + latMinPA.getFloat(row)/60f) * factor);
                }
                table.removeColumns(latDeg, latDeg + 3);
                table.addColumn(latDeg, "latitude", lat, 
                    (new Attributes()).add("units", "degrees_north"));                
            }
            int lonDeg = table.findColumnNumber("LongitudeDeg");
            if (lonDeg >= 0 && 
                table.findColumnNumber("LongitudeMin") == lonDeg + 1 &&
                table.findColumnNumber("EastWest")     == lonDeg + 2) {
                FloatArray lon = new FloatArray(nRows, false);
                PrimitiveArray lonDegPA = table.getColumn(lonDeg);
                PrimitiveArray lonMinPA = table.getColumn(lonDeg + 1);
                PrimitiveArray EWPA     = table.getColumn(lonDeg + 2);
                for (int row = 0; row < nRows; row++) {
                    String ew = EWPA.getString(row).toLowerCase();
                    int factor = 1;
                    if      (ew.startsWith("w")) factor = -1;
                    else if (ew.startsWith("e")) factor = 1;
                    else if (ew.startsWith("" )) factor = 1;
                    else String2.log("Unexpected EastWest=" + ew);
                    lon.add((lonDegPA.getFloat(row) + lonMinPA.getFloat(row)/60f) * factor);
                }
                table.removeColumns(lonDeg, lonDeg + 3);
                table.addColumn(lonDeg, "longitude", lon, 
                    (new Attributes()).add("units", "degrees_east"));                
            }

            //sort
            String sortVarNames[] = StringArray.arrayFromCSV(gatts.getString("subsetVariables"));
            int sortCols[] = new int[sortVarNames.length];
            for (int c = 0; c < sortVarNames.length; c++) 
                sortCols[c] = table.findColumnNumber(sortVarNames[c]);
            table.ascendingSort(sortCols);

            //display table info
            for (int col = 0; col < table.nColumns(); col++) {  //nColumns may change
                table.setColumnName(col, 
                    String2.modifyToBeFileNameSafe(table.getColumnName(col)));

                //shorten strings that are too long
                int maxLength = 0;
                PrimitiveArray pa = table.getColumn(col); 
                for (int row = 0; row < pa.size(); row++) {
                    String ts = pa.getString(row);
                    maxLength = Math.max(maxLength, ts.length());
                    if (ts.length() > maxStringLength) 
                        pa.setString(row, ts.substring(0, maxStringLength-3) + "...");
                }
                //remove this column
                if (maxLength == 0) {
                    String2.log(
                        String2.left("" + col, 3) + " " +
                        table.getColumnName(col) + " removed (no data)");
                    table.removeColumn(col);
                    col--;
                    continue;
                }

                //print col info
                String s = pa.getString(0);
                String2.log(
                    String2.left("" + col, 3) + " " +
                    String2.left(table.getColumnName(col), 22) + " " +
                    String2.left(pa.elementTypeString(), 8) + " " +
                    (s.length() > 35? s.substring(0, 35) + "..." : s) +
                    ((pa instanceof StringArray)? " [" + maxLength + "]" : ""));
            }

//    String2.log(EDDTableFromNcFiles.generateDatasetsXml(
//        "c:/data/FBNc/", "ABNORM\\.nc", 
//        "c:/data/FBNc/ABNORM.nc", "", 1000000, 
//        "", "", "", 
//        "", "", 
//        "", 
//        "http://www.fishbase.org", "FishBase", "", "FishBase 2004, Abnormalities", new Attributes()));

            //save as .nc
            table.saveAsFlatNc(ncDir + fName + ".nc", "row", false);

            //generateDatasetsXml
            String2.log(EDDTableFromNcFiles.generateDatasetsXml(
                "c:/data/FBNc/", fName + "\\.nc", 
                "c:/data/FBNc/" + fName + ".nc", "", 
                1000000, //reloadEvery
                "", "", "", 
                "", "", 
                "", 
                "", "", "", "", 0, "", //standardizeWhat, cacheFromUrl
                new Attributes()));

        }
        String2.log("FishBase.convertHtmlToNc finished.");

    }

}

