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
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObject;
import org.apache.commons.io.FileUtils;
import org.jruby.Ruby;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;

/**
 * An SDK, which servers gem packages.
 * <p>
 * Performes gem index update using specified indexer implementation.
 * </p>
 * @since 1.0
 */
public final class Gem {

    /**
     * Ruby runtime.
     */
    private static final String GEM_STR = ".gem";

    /**
     * Ruby runtime.
     */
    private RubyRuntimeAdapter runtime;

    /**
     * Ruby interpreter.
     */
    private Ruby ruby;

    /**
     * Gem indexer shared instance cache.
     */
    private final AtomicReference<GemInfo> infocache;

    /**
     * Gem indexer shared instance cache.
     */
    private final AtomicReference<GemIndex> cache;

    /**
     * Gem repository storage.
     */
    private final Storage storage;

    /**
     * Gem info extractor  supplier.
     */
    private final Supplier<GemInfo> extractor;

    /**
     * Gem indexer supplier.
     */
    private final Supplier<GemIndex> indexer;

    /**
     * New Gem SDK with default indexer.
     * @param storage Repository storage.
     */
    Gem(final Storage storage) {
        this.storage = storage;
        this.indexer = () -> new RubyGemIndex(this.runtime, this.ruby);
        this.cache = new AtomicReference<>();
        this.infocache = new AtomicReference<>();
        this.extractor = () -> new RubyObjJson(this.runtime, this.ruby);
    }

    /**
     * Batch update Ruby gems for repository.
     *
     * @param gem Ruby gem for indexing
     * @return Completable action
     */
    public CompletionStage<Void> batchUpdate(final Key gem) {
        this.initialize();
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    return Files.createTempDirectory("gem");
                } catch (final IOException exc) {
                    throw new ArtipieIOException(exc);
                }
            }
        ).thenCompose(
            tmpdir -> Gem.copyStorage(this.storage, new FileStorage(tmpdir), gem)
                .thenApply(ignore -> tmpdir)
        ).thenCompose(
            tmpdir -> this.sharedIndexer()
                .thenAccept(idx -> idx.update(tmpdir))
                .thenApply(ignore -> tmpdir)
        ).thenCompose(
            tmpdir -> Gem.copyStorage(new FileStorage(tmpdir), this.storage, gem)
                .thenApply(ignore -> tmpdir)
        ).handle(Gem::removeTempDir);
    }

    /**
     * Get info Ruby gem.
     *
     * @param gem Ruby gem to extract info
     * @return Completable action
     */
    public CompletionStage<JsonObject> info(final Key gem) {
        this.initialize();
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    return Files.createTempDirectory("info");
                } catch (final IOException exc) {
                    throw new ArtipieIOException(exc);
                }
            }
        ).thenCompose(
            tmpdir -> Gem.copyStorage(this.storage, new FileStorage(tmpdir), gem)
                .thenApply(ignore -> tmpdir)
        ).thenCompose(
            tmpdir -> this.sharedInfo()
                .thenApply(
                    rubyjson -> {
                        JsonObject obj = Json.createObjectBuilder().build();
                        try {
                            final List<Key> thekey = this.getGemFile(gem).toCompletableFuture()
                                .get();
                            if (!thekey.isEmpty()) {
                                obj = rubyjson.info(
                                    Paths.get(tmpdir.toString(), thekey.get(0).string())
                                );
                            }
                        } catch (final InterruptedException | ExecutionException exc) {
                            throw new ArtipieIOException(exc);
                        } finally {
                            removeTempDir(tmpdir, null);
                        }
                        return obj;
                    }
                )
        );
    }

    /**
     * Initialize ruby.
     */
    private void initialize() {
        if (this.runtime == null) {
            this.runtime = JavaEmbedUtils.newRuntimeAdapter();
        }
        if (this.ruby == null) {
            this.ruby = JavaEmbedUtils.initialize(java.util.Collections.emptyList());
        }
    }

    /**
     * Handle async result.
     * @param tmpdir Path directory to remove
     * @param err Error
     * @return Nothing
     */
    private static Void removeTempDir(final Path tmpdir, final Throwable err) {
        try {
            if (tmpdir != null) {
                FileUtils.deleteDirectory(new File(tmpdir.toString()));
            }
        } catch (final IOException exc) {
            throw new ArtipieIOException(exc);
        }
        if (err != null) {
            throw new ArtipieIOException(err);
        }
        return null;
    }

    /**
     * Copy storage from src to dst.
     * @param src Source storage
     * @param dst Destination storage
     * @param gem Key for gem
     * @return Async result
     */
    private static CompletionStage<Void> copyStorage(final Storage src, final Storage dst,
        final Key gem) {
        final java.util.Set<String> vars = new java.util.HashSet<>(
            java.util.Arrays.asList(
                "latest_specs.4.8", "latest_specs.4.8.gz", "prerelease_specs.4.8",
                "prerelease_specs.4.8.gz", "specs.4.8", "specs.4.8.gz"
            )
        );
        vars.add(gem.string());
        final String tmp = gem.string().substring(gem.string().indexOf('/') + 1);
        vars.add(String.format("quick/Marshal.4.8/%sspec.rz", tmp));
        return Single.fromFuture(src.list(Key.ROOT))
            .map(
                list -> list.stream().filter(
                    key -> vars.contains(key.string())
                        || key.string().startsWith(gem.string())
                            && key.string().endsWith(Gem.GEM_STR) && !key.string().contains("/")
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

    /**
     * Get shared ruby indexer instance.
     * @return Async result with gem index
     * @checkstyle ReturnCountCheck (15 lines)
     */
    private CompletionStage<GemIndex> sharedIndexer() {
        return CompletableFuture.supplyAsync(
            () -> this.cache.updateAndGet(
                value -> {
                    if (value == null) {
                        return this.indexer.get();
                    }
                    return value;
                }
            )
        );
    }

    /**
     * Get shared ruby info instance.
     * @return Async result with gem index
     * @checkstyle ReturnCountCheck (15 lines)
     */
    private CompletionStage<GemInfo> sharedInfo() {
        return CompletableFuture.supplyAsync(
            () -> this.infocache.updateAndGet(
                value -> {
                    if (value == null) {
                        return this.extractor.get();
                    }
                    return value;
                }
            )
        );
    }

    /**
     * Find gem in a given path.
     * @param gem Gem name to get info
     * @return String full path to gem file
     */
    private CompletionStage<List<Key>> getGemFile(final Key gem) {
        return Single.fromFuture(this.storage.list(Key.ROOT))
            .map(
                list -> list.stream().filter(
                    key -> key.string().contains(gem.string()) && key.string().endsWith(Gem.GEM_STR)
                        && !key.string().contains("/")
                ).collect(Collectors.toList()))
            .to(SingleInterop.get()).toCompletableFuture();
    }
}
