package gov.noaa.pfel.coastwatch.pointdata.parquet;

import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.ParquetEncodingException;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.MessageType;

public class CustomWriteSupport extends WriteSupport<CustomWriteSupport.RowRef> {
  public static class RowRef {
    public int row;
  }

  final MessageType schema;
  RecordConsumer recordConsumer;
  final List<ColumnDescriptor> cols;
  private final Map<String, String> metadata;
  private final PrimitiveArray[] columns;
  private final boolean[] isTimeCol;

  public CustomWriteSupport(Table table, MessageType schema, Map<String, String> metadata) {
    this.schema = schema;
    this.cols = schema.getColumns();
    this.metadata = metadata;

    int nCols = table.nColumns();
    this.columns = new PrimitiveArray[nCols];
    this.isTimeCol = new boolean[nCols];
    for (int col = 0; col < nCols; col++) {
      this.columns[col] = table.getColumn(col);
      this.isTimeCol[col] = table.isTimeColumn(col);
    }
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
  public void write(RowRef ref) {
    int row = ref.row;

    recordConsumer.startMessage();
    for (int i = 0; i < columns.length; ++i) {
      PrimitiveArray pa = columns[i];

      if (pa == null || pa.isMissingValue(row)) {
        continue;
      }

      String fieldName = cols.get(i).getPath()[0];
      recordConsumer.startField(fieldName, i);

      if (isTimeCol[i]) {
        long epochMillis = Math.round(pa.getDouble(row) * 1000.0);
        recordConsumer.addLong(epochMillis);
      } else {
        switch (cols.get(i).getPrimitiveType().getPrimitiveTypeName()) {
          case BOOLEAN:
            if (pa instanceof StringArray) {
              recordConsumer.addBoolean(String2.parseBooleanToInt(pa.getString(row)) == 1);
            } else {
              recordConsumer.addBoolean(pa.getInt(row) == 1);
            }
            break;
          case FLOAT:
            recordConsumer.addFloat(pa.getFloat(row));
            break;
          case DOUBLE:
            recordConsumer.addDouble(pa.getDouble(row));
            break;
          case INT32:
            recordConsumer.addInteger(pa.getInt(row));
            break;
          case INT64:
            recordConsumer.addLong(pa.getLong(row));
            break;
          case BINARY:
            String s = pa.getString(row);
            if (s != null) {
              recordConsumer.addBinary(Binary.fromString(s));
            }
            break;
          default:
            throw new ParquetEncodingException(
                "Unsupported column type: " + cols.get(i).getPrimitiveType());
        }
      }
      recordConsumer.endField(fieldName, i);
    }
    recordConsumer.endMessage();
  }
}
