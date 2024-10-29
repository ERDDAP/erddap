package gov.noaa.pfel.coastwatch.pointdata.parquet;

import com.cohort.array.PAOne;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.OutputFile;
import org.apache.parquet.schema.MessageType;

public class ParquetWriterBuilder extends ParquetWriter.Builder<List<PAOne>, ParquetWriterBuilder> {

  private CustomWriteSupport writeSupport;

  public ParquetWriterBuilder(MessageType schema, OutputFile file, Map<String, String> metadata) {
    super(file);
    writeSupport = new CustomWriteSupport(schema, metadata);
  }

  @Override
  protected ParquetWriterBuilder self() {
    return this;
  }

  @Override
  protected WriteSupport<List<PAOne>> getWriteSupport(Configuration conf) {
    return writeSupport;
  }
}
