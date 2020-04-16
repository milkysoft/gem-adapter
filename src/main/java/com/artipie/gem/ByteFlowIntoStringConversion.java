/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.gem;

import com.artipie.asto.Remaining;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.reactivestreams.Publisher;

/**
 * Flow of Bytebuffers into a Single of String conversion algorithm.
 *
 * @since 0.2
 */
public final class ByteFlowIntoStringConversion {

    /**
     * The flow of bytes.
     */
    private final Publisher<ByteBuffer> flow;

    /**
     * Ctor.
     * @param flow The flow of bytes.
     */
    public ByteFlowIntoStringConversion(final Publisher<ByteBuffer> flow) {
        this.flow = flow;
    }

    /**
     * A string representation.
     * @return The single of string.
     */
    public Single<String> string() {
        return Flowable.fromPublisher(this.flow)
            .toList()
            .map(
                bufs -> {
                    int counter = 0;
                    for (final ByteBuffer buf : bufs) {
                        counter += buf.remaining();
                    }
                    final ByteBuffer bytes = ByteBuffer.allocate(counter);
                    for (final ByteBuffer buf : bufs) {
                        bytes.put(new Remaining(buf).bytes());
                    }
                    return new String(bytes.array(), StandardCharsets.UTF_8);
                }
            );
    }
}
