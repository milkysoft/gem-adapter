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
package com.artipie.gem.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.gem.Gem;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.ContentWithSize;
import java.nio.ByteBuffer;
import java.util.Map.Entry;
import java.util.UUID;
import org.reactivestreams.Publisher;

/**
 * A slice, which servers gem packages.
 * @since 1.0
 */
public final class SubmitGemSlice implements Slice {

    /**
     * Repository storage.
     */
    private final Storage storage;

    /**
     * Gem SDK.
     */
    private final Gem gem;

    /**
     * Ctor.
     *
     * @param storage The storage.
     */
    public SubmitGemSlice(final Storage storage) {
        this.storage = storage;
        this.gem = new Gem(storage);
    }

    @Override
    public Response response(final String line, final Iterable<Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Key key = new Key.From(
            "gems", UUID.randomUUID().toString().replace("-", "").concat(".gem")
        );
        return new AsyncResponse(
            this.storage.save(
                key, new ContentWithSize(body, headers)
            ).thenCompose(none -> this.gem.update(key))
            .thenCompose(none -> this.storage.delete(key))
            .thenApply(none -> new RsWithStatus(RsStatus.CREATED))
        );
    }
}
