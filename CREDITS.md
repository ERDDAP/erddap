[Credits](#credits)
-------------------
Contributions to ERDDAP™ code
*   [MergeIR](#submittedCode)  
    [EDDGridFromMergeIRFiles.java](https://erddap.github.io/setupDatasetsXml.html#EDDGridFromMergeIRFiles) was written and contributed by Jonathan Lafite and Philippe Makowski of R.Tech Engineering (license: copyrighted open source). Thank you, Jonathan and Philippe!  
     
*   TableWriterDataTable  
    [.dataTable (TableWriterDataTable.java)](https://coastwatch.pfeg.noaa.gov/erddap/tabledap/documentation.html#fileType) was written and contributed by Roland Schweitzer of NOAA (license: copyrighted open source). Thank you, Roland!  
     
*   json-ld  
    The initial version of the [Semantic Markup of Datasets with json-ld (JSON Linked Data)](#jsonld) feature (and thus all of the hard work in designing the content) was written and contributed (license: copyrighted open source) by Adam Leadbetter and Rob Fuller of the Marine Institute in Ireland. Thank you, Adam and Rob!  
     
*   orderBy  
    The code for the [orderByMean filter](https://coastwatch.pfeg.noaa.gov/erddap/tabledap/documentation.html#orderByMean) in tabledap and the extensive changes to the code to support the [_variableName/divisor:offset_ notation](https://coastwatch.pfeg.noaa.gov/erddap/tabledap/documentation.html#orderByDivisorOptions) for all orderBy filters was written and contributed (license: copyrighted open source) by Rob Fuller and Adam Leadbetter of the Marine Institute in Ireland. Thank you, Rob and Adam!  
     
*   Borderless Marker Types  
    The code for three new marker types (Borderless Filled Square, Borderless Filled Circle, Borderless Filled Up Triangle) was contributed by Marco Alba of ETT / EMODnet Physics. Thank you, Marco Alba!  
     
*   Translations of messages.xml  
    The initial version of the code in TranslateMessages.java which uses Google's translation service to translate messages.xml into various languages was written by Qi Zeng, who was working as a Google Summer of Code intern. Thank you, Qi!  
     
*   orderBySum  
    The code for the [orderBySum filter](https://coastwatch.pfeg.noaa.gov/erddap/tabledap/documentation.html#orderBySum) in tabledap (based on Rob Fuller and Adam Leadbetter's orderByMean) and the Check All and Uncheck All buttons on the EDDGrid Data Access Form were written and contributed (license: copyrighted open source) by Marco Alba of ETT Solutions and EMODnet. Thank you, Marco!  
     
*   Out-of-range .transparentPng Requests  
    ERDDAP™ now accepts requests for .transparentPng's when the latitude and/or longitude values are partly or fully out-of-range. (This was ERDDAP™ GitHub Issues #19, posted by Rob Fuller -- thanks for posting that, Rob.) The code to fix this was written by Chris John. Thank you, Chris!  
     
*   Display base64 image data in .htmlTable responses  
    The code for displaying base64 image data in .htmlTable responses was contributed by Marco Alba of ETT / EMODnet Physics. Thank you, Marco Alba!  
     
*   nThreads Improvement  
    The nThreads system for EDDTableFromFiles was significantly improved. These changes lead to a huge speed improvement (e.g., 2X speedup when nThreads is set to 2 or more) for the most challenging requests (when a large number of files must be processed to gather the results). These changes will also lead to a general speedup throughout ERDDAP™. The code for these changes was contributed by Chris John. Thank you, Chris!  

*   Color palette EK80 for acoustic datasets. Thank you Rob Cermak!

*   EDDTableAggregateRows aggregation across all children fixed. Thank you Marco Alba!

*   Fix for incorrect varNames in logs. Thank you Ayush Singh!
