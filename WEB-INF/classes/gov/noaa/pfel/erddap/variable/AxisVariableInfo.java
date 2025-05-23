package gov.noaa.pfel.erddap.variable;

import com.cohort.array.PrimitiveArray;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;

public record AxisVariableInfo(
    String sourceName,
    String destinationName,
    LocalizedAttributes attributes,
    PrimitiveArray values) {}
