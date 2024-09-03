package gov.noaa.pfel.erddap.util;

public interface WorkConsumer<T> {

  // We aren't using the Java Consumer class because EDDTableFromFiles
  // can throw an exception during work processing.
  public void accept(T t) throws Throwable;
}
