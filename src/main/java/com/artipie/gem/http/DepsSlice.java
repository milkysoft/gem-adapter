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
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Header;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;

/**
 * Returns some basic information about the given gem.
 * <p>
 * Handle {@code GET - /api/v1/gems/[GEM NAME].(json|yaml)}
 * requests, see
 * <a href="https://guides.rubygems.org/rubygems-org-api">RubyGems API</a>
 * for documentation.
 * </p>
 *
 * @since 0.2
 */
public final class DepsSlice implements Slice {
    /**
     * HTTP var.
     */
    private static final String HTTP = "HTTP";

    /**
     * Gem SDK.
     */
    private final com.artipie.gem.Gem sdk;

    /**
     * New slice for handling Get API requests.
     * @param storage Gems storage
     */
    public DepsSlice(final Storage storage) {
        this.sdk = new com.artipie.gem.Gem(storage);
    }

    @Override
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final String deproute = "/api/v1/dependencies";
        final AsyncResponse res;
        final int offset = 26;
        if (line.contains(deproute)) {
            res = this.processdeproute(line, deproute, offset);
        } else if (line.contains("/specs.4.8.gz")) {
            res = new AsyncResponse(
                this.sdk.getRubyFile(new Key.From("specs.4.8.gz"))
                    .thenApply(out -> new RsWithBody(ByteBuffer.wrap(out)))
            );
        } else if (line.contains("/versions")) {
            res = new AsyncResponse(
                CompletableFuture.completedFuture(
                    new RsWithHeaders(
                        new RsWithStatus(RsStatus.FOUND),
                        new Header("Location", "https://index.rubygems.org/versions"),
                        new Header("Content-Type", "text/html;charset=utf-8"),
                        new Header("X-XSS-Protection", "1; mode=block"),
                        new Header("X-Content-Type-Options", "nosniff"),
                        new Header("X-Frame-Options", "SAMEORIGIN"),
                        new Header("Content-Length", "0")
                    )
                )
            );
        } else if (line.contains("/gems/")) {
            final int indexe = line.indexOf(DepsSlice.HTTP) - 1;
            final int arc = line.substring(0, indexe - 1).lastIndexOf('/') + 1;
            final String spec = line.substring(arc, indexe);
            res = new AsyncResponse(
                this.sdk.getRubyFile(new Key.From(spec))
                    .thenApply(out -> new RsWithBody(ByteBuffer.wrap(out)))
            );
        } else {
            throw new IllegalStateException("Invalid routing schema");
        }
        return res;
    }

    /**
     * Process dependency route.
     * @param line Line
     * @param deproute Route
     * @param offset Int
     * @return AsyncResponse
     */
    AsyncResponse processdeproute(final String line, final String deproute,
        final int offset) {
        final int indexs = line.indexOf(deproute) + offset;
        final int indexe = line.indexOf(DepsSlice.HTTP) - 1;
        final List<Key> gemkeys = new ArrayList<>(0);
        for (final String gemname : line.substring(indexs, indexe).split(",")) {
            gemkeys.add(new Key.From(gemname));
        }
        return new AsyncResponse(
            this.sdk.getDependencies(gemkeys)
                .thenApply(data -> new RsWithBody(ByteBuffer.wrap(data)))
        );
    }
}
