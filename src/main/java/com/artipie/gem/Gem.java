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
import com.artipie.asto.SubStorage;
import com.artipie.asto.fs.FileStorage;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.commons.io.FileUtils;
import org.jruby.Ruby;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;

/**
 * An SDK, which servers gem packages.
 * <p>
 * Initialize on first request.
 * Currently, Ruby runtime initialization and Slice evaluation is happening during the GemSlice
 * construction. Instead, the Ruby runtime initialization and Slice evaluation should happen
 * on first request.
 *
 * @since 1.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 */
public class Gem {
    /**
     * Primary evaler.
     */
    static final RubyRuntimeAdapter EVALER = JavaEmbedUtils.newRuntimeAdapter();

    /**
     * Primary runtime.
     */
    static final Ruby RUNTIME = JavaEmbedUtils.initialize(new ArrayList<>(0));

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
     * Batch update Ruby gems for repository.
     *
     * @param prefix Key used Substorage
     * @return Completable action
     */
    public CompletionStage<Void> batchUpdate(final Key prefix) {
        final Storage remote = new SubStorage(prefix, this.storage);
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    return Files.createTempDirectory(prefix.string());
                } catch (final IOException exc) {
                    throw new UncheckedIOException(exc);
                }
            }).thenCompose(
                tmpdir -> {
                    final FileStorage local = new FileStorage(tmpdir);
                    return Single.fromFuture(remote.list(Key.ROOT))
                        .flatMapObservable(Observable::fromIterable)
                        .flatMapSingle(
                            key -> Single.fromFuture(
                                remote.value(key)
                                .thenCompose(content -> local.save(key, content))
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
                    final FileStorage local = new FileStorage(tmpdir);
                    return Single.fromFuture(local.list(Key.ROOT))
                        .flatMapObservable(Observable::fromIterable)
                        .flatMapSingle(
                            key -> Single.fromFuture(
                                local.value(key)
                                    .thenCompose(content -> remote.save(key, content))
                                    .thenApply(none -> true)
                            )
                        ).toList().map(ignore -> true).to(SingleInterop.get())
                        .thenApply(ignore -> tmpdir); }
            )
            .handle(Gem::removeTempDir);
    }

    /**
     * Create indexes for given gem in target folder.
     *
     * @param repo The temp repo path.
     */
    static void rubyUpdater(final String repo) {
        final String script = "require 'rubygems/indexer.rb'\n Gem::Indexer.new(\""
            .concat(repo).concat("\",{ build_modern:true }).generate_index");
        Gem.EVALER.eval(Gem.RUNTIME, script);
    }

    /**
     * Handle async result.
     * @param tmpdir Path directory to remove
     * @param err Error
     * @return Nothing
     */
    private static Void removeTempDir(final Path tmpdir, final Throwable err) {
        try {
            FileUtils.deleteDirectory(new File(tmpdir.toString()));
        } catch (final IOException exc) {
            throw new UncheckedIOException(exc);
        }
        if (err != null) {
            Logger.warn(
                Gem.class, "Failed to update gem indexes: %[exception]s", err
            );
        }
        return null;
    }
}
