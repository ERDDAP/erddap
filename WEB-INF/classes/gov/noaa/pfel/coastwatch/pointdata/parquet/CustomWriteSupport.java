package gov.noaa.pfel.coastwatch.pointdata.parquet;

import com.cohort.array.PAOne;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.ParquetEncodingException;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.MessageType;

public class CustomWriteSupport extends WriteSupport<List<PAOne>> {
  MessageType schema;
  RecordConsumer recordConsumer;
  List<ColumnDescriptor> cols;
  private Map<String, String> metadata;

  CustomWriteSupport(MessageType schema, Map<String, String> metadata) {
    this.schema = schema;
    this.cols = schema.getColumns();
    this.metadata = metadata;
  }

  @Override
  public WriteContext init(Configuration config) {
    return new WriteContext(schema, metadata);
  }

  @Override
  public void prepareForWrite(RecordConsumer recordConsumer) {
    this.recordConsumer = recordConsumer;
  }

  @Override
  public void write(List<PAOne> values) {
    if (values.size() != cols.size()) {
      throw new ParquetEncodingException(
          "Invalid input data. Expecting "
              + cols.size()
              + " columns. Input had "
              + values.size()
              + " columns ("
              + cols
              + ") : "
              + values);
    }

    recordConsumer.startMessage();
    for (int i = 0; i < cols.size(); ++i) {
      PAOne val = values.get(i);
      // val.length() == 0 indicates a NULL value.
      if (val != null && !val.isMissingValue()) {
        recordConsumer.startField(cols.get(i).getPath()[0], i);
        switch (cols.get(i).getPrimitiveType().getPrimitiveTypeName()) {
          case BOOLEAN:
            recordConsumer.addBoolean(Boolean.parseBoolean(val.getString()));
            break;
          case FLOAT:
            recordConsumer.addFloat(val.getFloat());
            break;
          case DOUBLE:
            recordConsumer.addDouble(val.getDouble());
            break;
          case INT32:
            recordConsumer.addInteger(val.getInt());
            break;
          case INT64:
            recordConsumer.addLong(val.getLong());
            break;
          case BINARY:
            recordConsumer.addBinary(stringToBinary(val.getString()));
            break;
          default:
            throw new ParquetEncodingException("Unsupported column type: " + cols.get(i).getType());
        }
        recordConsumer.endField(cols.get(i).getPath()[0], i);
      }
    }
    recordConsumer.endMessage();
  }

  private Binary stringToBinary(Object value) {
    return Binary.fromString(value.toString());
  }
}
