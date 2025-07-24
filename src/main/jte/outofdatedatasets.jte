@import gov.noaa.pfel.erddap.jte.OutOfDateDatasets
@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.TableOptions
@import gov.noaa.pfel.coastwatch.pointdata.Table;
@param YouAreHere youAreHere
@param int language
@param TableOptions tableOptions
@param Table table

@template.youarehere(youAreHere)

${OutOfDateDatasets.getAdvcoutOfDateAr(language)}

@if(table.nRows() == 0)
    [${OutOfDateDatasets.getNMatchingAr(language)} ${OutOfDateDatasets.getAdvcoutOfDateAr(language)}]
@else
    ${OutOfDateDatasets.getNMatchingAr(language)}${OutOfDateDatasets.getGeneratedAtAr(language)}
    @template.Table(tableOptions)
@endif
