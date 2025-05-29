package gov.noaa.pfel.erddap.variable;

import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;

public record DataVariableInfo(
    String sourceName, String destinationName, LocalizedAttributes attributes, String dataType) {}
