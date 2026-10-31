package com.ankur.spark.service.common;

import org.apache.parquet.io.OutputFile;
import org.apache.parquet.io.PositionOutputStream;

import java.io.ByteArrayOutputStream;

/** Parquet {@link OutputFile} that writes into a byte array. */
public class InMemoryOutputFile implements OutputFile {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    @Override
    public PositionOutputStream create(long blockSizeHint) {
        return new PositionOutputStream() {
            @Override
            public long getPos() {
                return buffer.size();
            }

            @Override
            public void write(int b) {
                buffer.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                buffer.write(b, off, len);
            }
        };
    }

    @Override
    public PositionOutputStream createOrOverwrite(long blockSizeHint) {
        buffer.reset();
        return create(blockSizeHint);
    }

    @Override
    public boolean supportsBlockSize() {
        return false;
    }

    @Override
    public long defaultBlockSize() {
        return 0;
    }

    public byte[] toArray() {
        return buffer.toByteArray();
    }

    public long size() {
        return buffer.size();
    }
}
