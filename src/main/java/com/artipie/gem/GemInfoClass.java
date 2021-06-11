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

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.common.RsJson;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
     */
    public GemInfoClass(final Storage storage, final Gem gem) {
        this.storage = storage;
        this.gem = gem;
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
                GemInfoClass.class,
                "Gem info for '%s' has been requested. Extension: '%s'",
                gem,
                extension
            );
            Key key = null;
            try {
                key = getGemFile(gem).toCompletableFuture().get();
                System.out.println(String.format("Key extracted: %s", key.string()));
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return new AsyncResponse(
                this.gem.getInfo(key)
                    .thenApply(
                        RsJson::new
                )
            );
        } else {
            throw new IllegalStateException("Not expected path has been matched");
        }
    }

    /**
     * Find gem in a given path.
     * @param gem Gem name to get info
     * @return String full path to gem file
     */
    private CompletionStage<Key> getGemFile(final String gem) {
        final CompletableFuture<Key> future = new CompletableFuture<>();
        Single.fromFuture(this.storage.list(Key.ROOT))
            .map(
                list -> list.stream().filter(
                    key -> {
                        System.out.println(String.format("Key: %s, gem: %s", key.string(), gem));
                        return key.string().contains(gem);
                    }
                ).limit(1).collect(Collectors.toList()))
            .flatMapObservable(Observable::fromIterable).forEach(future::complete);
        return future;
//        try {
//            final Optional<String> filename = Files.walk(tmpdir).map(Path::toString)
//                .filter(file -> file.contains(gem) && file.contains(".gem")).findFirst();
//            if (filename.isPresent()) {
//                return filename.get();
//            } else {
//                throw new ArtipieIOException(String.format("Gem %s not found", gem));
//            }
//        } catch (final IOException exc) {
//            throw new ArtipieIOException(exc);
//        }
    }
}

