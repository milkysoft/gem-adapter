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


import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.common.RsJson;
import com.artipie.http.slice.SliceSimple;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.reactivestreams.Publisher;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.common.RsJson;
import com.jcabi.log.Logger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Returns some basic information about the given gem. GET - /api/v1/gems/[GEM NAME].(json|yaml)
 * https://guides.rubygems.org/rubygems-org-api/
 *
 * @since 0.2
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
public final class GemInfoClass implements Slice {

    /**
     * Endpoint path pattern.
     */
    public static final Pattern PATH_PATTERN = Pattern.compile("/api/v1/gems/([\\w]+).(json|yml)");

    /**
     * Storage where gem is located.
     */
    private final Storage storage;

    /**
     * Origin gem index instance.
     */
    private final Gem gem;

    /**
     * New gem info.
     * @param storage Gems storage
     * @param gem Is Gem class
     */
    public GemInfoClass(final Storage storage, final Gem gem) {
        this.storage = storage;
        this.gem = gem;
    }

    @Override
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final String deproute = "/api/v1/dependencies";
        final int offset = 26;
        final AsyncResponse res;
        final Matcher matcher = PATH_PATTERN.matcher(new RequestLineFrom(line).uri().toString());
        if (matcher.find()) {
            final String gemname = matcher.group(1);
            final String extension = matcher.group(2);
            Logger.info(
                GemInfoClass.class,
                "Gem info for '%s' has been requested. Extension: '%s'",
                gemname,
                extension
            );
            res = new AsyncResponse(
                this.gem.getInfo(new Key.From(gemname))
                    .thenApply(
                        RsJson::new
                )
            );
        } else if (line.contains(deproute)) {
            final int indexs = line.indexOf(deproute) + offset;
            final int indexe = line.indexOf("HTTP") - 1;
            byte obj[];
            final List<Key> gemkeys = new ArrayList<>(0);
            for (final String gemname : line.substring(indexs, indexe).split(",")) {
                gemkeys.add(new Key.From(gemname));
            }
            try {
                obj = this.gem.getDependencies(gemkeys)
                    .toCompletableFuture().get();
            } catch (final InterruptedException | ExecutionException exc) {
                throw new ArtipieIOException(exc);
            }
            res = new AsyncResponse(
                CompletableFuture.completedFuture(
                    new RsWithBody(ByteBuffer.wrap(obj))
                )
            );
        } else if (line.contains("/latest_specs.4.8.gz")) {
            System.out.println(line);
            final Charset encoding = Charset.forName("ISO-8859-1");
            res = new AsyncResponse(
                    this.gem.getRubyFile(new Key.From("latest_specs.4.8.gz"))
                            .thenApply( out -> new RsWithBody(ByteBuffer.wrap(out)))
            );
        } else if (line.contains("/prerelease_specs.4.8.gz")) {
            System.out.println(line);
            final Charset encoding = Charset.forName("ISO-8859-1");
            res = new AsyncResponse(
                    this.gem.getRubyFile(new Key.From("prerelease_specs.4.8.gz"))
                            .thenApply( out -> new RsWithBody(ByteBuffer.wrap(out)))
            );
        } else if (line.contains("/specs.4.8.gz")) {
            System.out.println(line);
            final Charset encoding = Charset.forName("ISO-8859-1");
            res = new AsyncResponse(
                    this.gem.getRubyFile(new Key.From("specs.4.8.gz"))
                            .thenApply( out -> new RsWithBody(ByteBuffer.wrap(out)))
            );
        } else if (line.contains("/quick/Marshal.4.8/")) {
            System.out.println(line);
            final int ar = line.lastIndexOf("/") + 1;
            final int indexe = line.indexOf("HTTP") - 1;
            final String spec = line.substring(ar, indexe);
            final Charset encoding = Charset.forName("ISO-8859-1");
            res = new AsyncResponse(
                    this.gem.getRubyFile(new Key.From(spec))
                            .thenApply( out -> out == null ? new RsWithBody("", StandardCharsets.UTF_8)
                                :new RsWithBody(ByteBuffer.wrap(out)))
            );
        } else if (line.contains("/versions")) {
            System.out.println(line);
            res = new AsyncResponse(
                    CompletableFuture.completedFuture(new RsWithStatus(RsStatus.NOT_FOUND))
            );
        } else if (line.contains("/info/")) {
            System.out.println(line);
            final int ar = line.lastIndexOf("/") + 1;
            final int indexe = line.indexOf("HTTP") - 1;
            final String spec = line.substring(ar, indexe);
            final Charset encoding = Charset.forName("ISO-8859-1");
            res = new AsyncResponse(
                this.gem.getRubyFile(new Key.From(spec))
                    .thenApply( out -> new RsWithBody(ByteBuffer.wrap(out)))
            );
        } else if (line.contains("/gems/")) {
            System.out.println(line);
            final int ar = line.lastIndexOf("/") + 1;
            final int indexe = line.indexOf("HTTP") - 1;
            final String spec = line.substring(ar, indexe);
            final Charset encoding = Charset.forName("ISO-8859-1");
            res = new AsyncResponse(
                    this.gem.getRubyFile(new Key.From(spec))
                            .thenApply( out -> new RsWithBody(ByteBuffer.wrap(out)))
            );
        } else {
            System.out.println(line);
            res = new AsyncResponse(
                CompletableFuture.completedFuture(new RsWithBody("", StandardCharsets.UTF_8))
            );
        }
        return res;
    }
}
