package com.ankur.spark;

import java.util.*;

import org.apache.commons.io.IOUtils;
import org.apache.parquet.io.DelegatingSeekableInputStream;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.SeekableInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ParquetStream implements InputFile {

    private final byte[] data;

    public ParquetStream(byte[] parquetBytes) {
        this.data = parquetBytes;
    }

    public ParquetStream(InputStream stream) throws IOException {
        this.data = IOUtils.toByteArray(stream);
    }

    @Override
    public long getLength() {
        return this.data.length;
    }

    @Override
    public SeekableInputStream newStream() {
        return new DelegatingSeekableInputStream(new SeekableByteArrayInputStream(this.data)) {
            @Override
            public void seek(long newPos) {
                ((SeekableByteArrayInputStream) this.getStream()).setPos((int) newPos);
            }

            @Override
            public long getPos() {
                return ((SeekableByteArrayInputStream) this.getStream()).getPos();
            }
        };
    }

    private static class SeekableByteArrayInputStream extends ByteArrayInputStream {

        public SeekableByteArrayInputStream(byte[] buf) {
            super(buf);
        }

        public void setPos(int pos) {
            this.pos = pos;
        }

        public int getPos() {
            return this.pos;
        }
    }
}
