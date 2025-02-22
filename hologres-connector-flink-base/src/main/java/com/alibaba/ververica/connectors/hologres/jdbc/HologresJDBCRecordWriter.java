package com.alibaba.ververica.connectors.hologres.jdbc;

import org.apache.flink.table.data.DecimalData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;

import com.alibaba.hologres.client.model.Record;
import com.alibaba.ververica.connectors.hologres.api.HologresTableSchema;
import com.alibaba.ververica.connectors.hologres.api.table.RowDataWriter;
import com.alibaba.ververica.connectors.hologres.config.HologresConnectionParam;

import java.sql.Date;
import java.sql.Timestamp;

/** Transform RowData to Record. */
public class HologresJDBCRecordWriter implements RowDataWriter<Record> {
    private transient HologresTableSchema tableSchema;
    private HologresConnectionParam param;
    private transient Record record;

    public HologresJDBCRecordWriter(HologresConnectionParam param) {
        this.param = param;
    }

    @Override
    public void checkHologresTypeSupported(int hologresType, String typeName) {}

    @Override
    public void newRecord() {
        if (tableSchema == null) {
            tableSchema = HologresTableSchema.get(param.getJdbcOptions());
        }
        this.record = new Record(tableSchema.get());
    }

    @Override
    public void writeNull(int index) {
        if (!param.isIgnoreNullWhenUpdate()) {
            this.record.setObject(index, null);
        }
    }

    @Override
    public void writeBoolean(Boolean value, int columnIndexInHologresTable) {
        this.record.setObject(columnIndexInHologresTable, value);
    }

    @Override
    public void writeByte(Byte value, int columnIndexInHologresTable) {
        this.record.setObject(columnIndexInHologresTable, value);
    }

    @Override
    public void writeShort(Short value, int columnIndexInHologresTable) {
        this.record.setObject(columnIndexInHologresTable, value);
    }

    @Override
    public void writeInt(Integer value, int columnIndexInHologresTable) {
        this.record.setObject(columnIndexInHologresTable, value);
    }

    @Override
    public void writeLong(Long value, int columnIndexInHologresTable) {
        this.record.setObject(columnIndexInHologresTable, value);
    }

    @Override
    public void writeFloat(Float value, int columnIndexInHologresTable) {
        this.record.setObject(columnIndexInHologresTable, value);
    }

    @Override
    public void writeDouble(Double value, int columnIndexInHologresTable) {
        this.record.setObject(columnIndexInHologresTable, value);
    }

    @Override
    public void writeString(StringData value, int columnIndexInHologresTable) {
        this.record.setObject(columnIndexInHologresTable, value.toString());
    }

    @Override
    public void writeDate(Integer value, int columnIndexInHologresTable) {
        this.record.setObject(columnIndexInHologresTable, new Date(value * 86400000L));
    }

    @Override
    public void writeTimestampTz(TimestampData value, int columnIndexInHologresTable) {
        this.record.setObject(columnIndexInHologresTable, value.toTimestamp());
    }

    @Override
    public void writeTimestamp(TimestampData value, int columnIndexInHologresTable) {
        this.record.setObject(columnIndexInHologresTable, new Timestamp(value.getMillisecond()));
    }

    @Override
    public void writeBinary(byte[] value, int columnIndexInHologresTable) {
        this.record.setObject(columnIndexInHologresTable, value);
    }

    @Override
    public void writeObject(Object value, int columnIndexInHologresTable) {
        this.record.setObject(columnIndexInHologresTable, value);
    }

    @Override
    public void writeDecimal(
            DecimalData value,
            int columnIndexInHologresTable,
            int decimalPrecision,
            int decimalScale) {
        this.record.setObject(columnIndexInHologresTable, value.toBigDecimal());
    }

    @Override
    public void writeIntArray(int[] value, int columnIndexInHologresTable) {
        this.record.setObject(columnIndexInHologresTable, value);
    }

    @Override
    public void writeLongArray(long[] value, int columnIndexInHologresTable) {
        this.record.setObject(columnIndexInHologresTable, value);
    }

    @Override
    public void writeFloatArray(float[] value, int columnIndexInHologresTable) {
        this.record.setObject(columnIndexInHologresTable, value);
    }

    @Override
    public void writeDoubleArray(double[] value, int columnIndexInHologresTable) {
        this.record.setObject(columnIndexInHologresTable, value);
    }

    @Override
    public void writeBooleanArray(boolean[] value, int columnIndexInHologresTable) {
        this.record.setObject(columnIndexInHologresTable, value);
    }

    @Override
    public void writeStringArray(String[] value, int columnIndexInHologresTable) {
        this.record.setObject(columnIndexInHologresTable, value);
    }

    @Override
    public Record complete() {
        return record;
    }

    @Override
    public RowDataWriter<Record> copy() {
        return new HologresJDBCRecordWriter(param);
    }
}
