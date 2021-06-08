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
import com.artipie.asto.fs.FileStorage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.common.RsJson;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jruby.javasupport.JavaEmbedUtils;
import org.reactivestreams.Publisher;

/**
 * Returns some basic information about the given gem. GET - /api/v1/gems/[GEM NAME].(json|yaml)
 * https://guides.rubygems.org/rubygems-org-api/
 *
 * @todo #32:120min Gem Information implementation.
 *  The implementation must be able to response with either json or yaml format. An example response
 *  can be obtained via {@code curl https://rubygems.org/api/v1/gems/rails.json}
 * @since 0.2
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
public final class GemInfo implements Slice {

    /**
     * Endpoint path pattern.
     */
    public static final Pattern PATH_PATTERN = Pattern.compile("/api/v1/gems/([\\w]+).(json|yml)");

    /**
     * Storage where gem is located.
     */
    private final Storage storage;

    /**
     * New gem info.
     * @param storage Gems storage
     */
    public GemInfo(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Matcher matcher = PATH_PATTERN.matcher(new RequestLineFrom(line).uri().toString());
        if (matcher.find()) {
            final String gem = matcher.group(1);
            final String extension = matcher.group(2);
            Logger.info(
                GemInfo.class,
                "Gem info for '%s' has been requested. Extension: '%s'",
                gem,
                extension
            );
            return new AsyncResponse(
                CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            return Files.createTempDirectory("gem");
                        } catch (final IOException exc) {
                            throw new ArtipieIOException(exc);
                        }
                    }
                ).thenCompose(
                    tmpdir -> GemInfo.copyStorage(this.storage, new FileStorage(tmpdir), gem)
                        .thenApply(ignore -> tmpdir)
                ).thenApply(
                    tmpdir -> {
                        final RsJson res;
                        try {
                            res = new RsJson(
                                new RubyObjJson(
                                    JavaEmbedUtils.newRuntimeAdapter(),
                                    JavaEmbedUtils.initialize(Collections.emptyList())
                                ).createJson(
                                    Paths.get(GemInfo.getGemFile(tmpdir, gem))
                                )
                            );
                            Files.delete(tmpdir);
                        } catch (final IOException exc) {
                            throw new ArtipieIOException(exc);
                        }
                        return res;
                    }
                )
            );
        } else {
            throw new IllegalStateException("Not expected path has been matched");
        }
    }

    /**
     * Find gem in a given path.
     * @param tmpdir Path to directory to search for gem
     * @param gem Gem name to get info
     * @return String full path to gem file
     */
    private static String getGemFile(final Path tmpdir, final String gem) {
        try {
            final Optional<String> filename = Files.walk(tmpdir).map(Path::toString)
                .filter(file -> file.contains(gem) && file.contains(".gem")).findFirst();
            if (filename.isPresent()) {
                return filename.get();
            } else {
                throw new ArtipieIOException(String.format("Gem %s not found", gem));
            }
        } catch (final IOException exc) {
            throw new ArtipieIOException(exc);
        }
    }

    /**
     * Copy storage from src to dst.
     * @param src Source storage
     * @param dst Destination storage
     * @param gem Key for gem
     * @return Async result
     */
    private static CompletionStage<Void> copyStorage(final Storage src, final Storage dst,
        final String gem) {
        return Single.fromFuture(src.list(Key.ROOT))
            .map(
                list -> list.stream().filter(
                    key -> key.string().contains(gem)
                ).collect(Collectors.toList()))
            .flatMapObservable(Observable::fromIterable)
            .flatMapSingle(
                key -> Single.fromFuture(
                    src.value(key)
                        .thenCompose(content -> dst.save(key, content))
                        .thenApply(none -> true)
                )
            ).ignoreElements().to(CompletableInterop.await());
    }
}
