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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.ContentWithSize;
import java.nio.ByteBuffer;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;

/**
 * A slice, which servers gem packages.
 *
 * @since 1.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 */
public final class SubmitGem implements Slice {
    /**
     * Gem repository storage.
     */
    private final Storage storage;

    /**
     * Origin gem index instance.
     */
    private final Gem gem;

    /**
     * Ctor.
     *
     * @param storage The storage.
     * @param gem The gem.
     */
    public SubmitGem(final Storage storage, final Gem gem) {
        this.storage = storage;
        this.gem = gem;
    }

    /**
     * Ctor.
     *
     * @param line Request URI.
     * @param headers Request headers.
     * @param body Request body, attached file.
     * @return The Slice
     */
    public Response response(final String line, final Iterable<Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final CompletableFuture<Void> res = this.storage.save(
            new Key.From("asdasd"),
            new ContentWithSize(body, headers)
        ).thenCompose(none -> this.gem.batchUpdate(Key.ROOT));
        res.join();
        return new RsWithStatus(RsStatus.OK);
    }
}
