package gov.noaa.pfel.erddap.handlers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Specifies which State subclass should be used to parse an XML string. */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SaxHandlerClass {
  Class<? extends State> value();
}
