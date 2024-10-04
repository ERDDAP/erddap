package gov.noaa.pfel.erddap.dataset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method should be used to create an EDD object from an XML string
 * (non-Sax parser approach)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EDDFromXmlMethod {}
