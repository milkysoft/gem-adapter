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
import com.artipie.asto.fs.FileStorage;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jruby.Ruby;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * An SDK, which servers gem packages.
 * <p>
 * Initialize on first request.
 * Currently, Ruby runtime initialization and Slice evaluation is happening during the GemSlice
 * construction. Instead, the Ruby runtime initialization and Slice evaluation should happen
 * on first request.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 */
public class Gem {
    /**
     * Primary storage.
     */
    static final RubyRuntimeAdapter EVALER = JavaEmbedUtils.newRuntimeAdapter();

    /**
     * Primary storage.
     */
    static final Ruby RUNTIME = JavaEmbedUtils.initialize(new ArrayList<>(0));

    /**
     * Primary storage.
     */
    private static IRubyObject recvr;

    /**
     * Primary storage.
     */
    private static GemIndexer gemIndexer;

    /**
     * Primary storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param storage The storage.
     */
    Gem(final Storage storage) {
        this.storage = storage;
    }

    /**
     * Lookup an instance of slice, implemented with JRuby.
     *
     * @param key The name of a slice class, implemented in JRuby.
     * @return The Slice.
     */
    public CompletionStage<Void> update(final String key) {
        return this.update(new Key.From(key));
    }

    /**
     * Lookup an instance of slice, implemented with JRuby.
     *
     * @param key The name of a slice class, implemented in JRuby.
     * @return The Slice.
     */
    public CompletionStage<Void> update(final Key key) {
        return this.batchUpdate();
    }

    /**
     * Batch update Ruby gems for repository.
     *
     * @return Completable action
     */
    public CompletionStage<Void> batchUpdate() {
        final String script;
        try {
            if (Gem.recvr == null) {
                script = IOUtils.toString(
                    Gem.class.getResourceAsStream("/AstoUpdater.rb"),
                    StandardCharsets.UTF_8
                );
                Gem.EVALER.eval(Gem.RUNTIME, script);
                Gem.recvr = Gem.EVALER.eval(Gem.RUNTIME, "AstoUpdater");
                Gem.gemIndexer = (GemIndexer) JavaEmbedUtils.invokeMethod(
                    Gem.RUNTIME, Gem.recvr,
                    "new",
                    null,
                    GemIndexer.class
                );
            }
        } catch (final IOException exc) {
            throw new UncheckedIOException(exc);
        }
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    return Files.createTempDirectory("gem");
                } catch (final IOException exc) {
                    throw new UncheckedIOException(exc);
                }
            }).thenCompose(
                tmpdir -> {
                    final FileStorage remote = new FileStorage(tmpdir);
                    return Single.fromFuture(this.storage.list(Key.ROOT))
                        .flatMapObservable(Observable::fromIterable)
                        .flatMapSingle(
                            key -> Single.fromFuture(
                                this.storage.value(key)
                                .thenCompose(content -> remote.save(key, content))
                                .thenApply(none -> true)
                            )
                        ).toList().map(ignore -> true).to(SingleInterop.get())
                        .thenApply(ignore -> tmpdir);
                })
            .thenCompose(
                tmpdir -> CompletableFuture.runAsync(
                    () -> rubyUpdater(tmpdir.toString())
                ).thenApply(ignore -> tmpdir)
            )
            .thenCompose(
                tmpdir -> {
                    final FileStorage remote = new FileStorage(tmpdir);
                    return Single.fromFuture(remote.list(Key.ROOT))
                        .flatMapObservable(Observable::fromIterable)
                        .flatMapSingle(
                            key -> Single.fromFuture(
                                remote.value(key)
                                    .thenCompose(content -> this.storage.save(key, content))
                                    .thenApply(none -> true)
                            )
                        ).toList().map(ignore -> true).to(SingleInterop.get())
                        .thenApply(ignore -> tmpdir); }
            )
            .thenCompose(
                tmpdir -> CompletableFuture.runAsync(
                    () -> {
                        try {
                            FileUtils.deleteDirectory(new File(tmpdir.toString()));
                        } catch (final IOException exc) {
                            throw new UncheckedIOException(exc);
                        }
                    }
                ).thenApply(ignore -> null)
            );
    }

    /**
     * Lookup an instance of slice, implemented with JRuby.
     *
     * @param repo The temp repo path.
     * @return The Slice.
     */
    static String rubyUpdater(final String repo) {
        Gem.gemIndexer.index(repo);
        return repo;
    }
}
