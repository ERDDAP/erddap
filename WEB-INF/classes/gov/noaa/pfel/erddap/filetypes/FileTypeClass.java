package gov.noaa.pfel.erddap.filetypes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Specifies which State subclass should be used to parse an XML string. */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FileTypeClass {
  String fileTypeName();

  String fileTypeExtension();

  String infoUrl();

  String versionAdded();

  boolean availableTable() default true;

  boolean availableGrid() default true;

  boolean isImage() default false;

  boolean addContentDispositionHeader() default true;

  String contentType();

  String contentDescription() default "";
}
