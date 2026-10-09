package gov.noaa.pfel.coastwatch.pointdata.parquet;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.pointdata.parquet.CustomWriteSupport.RowRef;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.OutputFile;
import org.apache.parquet.schema.MessageType;

public class ParquetWriterBuilder extends ParquetWriter.Builder<RowRef, ParquetWriterBuilder> {

  private final CustomWriteSupport writeSupport;

  public ParquetWriterBuilder(
      Table table, MessageType schema, OutputFile file, Map<String, String> metadata) {
    super(file);
    writeSupport = new CustomWriteSupport(table, schema, metadata);
  }

  @Override
  protected ParquetWriterBuilder self() {
    return this;
  }

  @Override
  protected WriteSupport<RowRef> getWriteSupport(Configuration conf) {
    return writeSupport;
  }
}
