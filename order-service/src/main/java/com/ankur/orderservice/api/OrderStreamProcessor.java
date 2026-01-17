package com.ankur.orderservice.api;

import javax.annotation.WillNotClose;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A generic interface for the processing of a stream of bytes representing an order.
 *
 * <p><strong>Important:</strong> do not touch this interface! Don't move, rename or otherwise
 * modify it.
 */
public interface OrderStreamProcessor {
    /**
     * Processes data from the given input stream, sending the result to the provided output stream.
     *
     * @param source The source of data to be processed.
     * @param sink   The sink to which the processing result is sent.
     * @throws IOException Upon failure to read from the source or write to the sink.
     */
    void process(@WillNotClose InputStream source, @WillNotClose OutputStream sink)
            throws IOException;
}
