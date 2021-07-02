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
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.json.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jruby.Ruby;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;

/**
 * An SDK, which servers gem packages.
 * <p>
 * Performes gem index update using specified indexer implementation.
 * </p>
 * @since 1.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class Gem {

    /**
     * Ruby runtime.
     */
    private final RubyRuntimeAdapter runtime;

    /**
     * Ruby interpreter.
     */
    private final Ruby ruby;

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
        this.runtime = JavaEmbedUtils.newRuntimeAdapter();
        this.ruby = JavaEmbedUtils.initialize(Collections.emptyList());
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
    public CompletionStage<JsonObject> getInfo(final Key gem) {
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
                        final JsonObject obj;
                        try {
                            final Key thekey = this.getGemFile(gem, false).toCompletableFuture().get();
                            obj = rubyjson.getinfo(
                                Paths.get(tmpdir.toString(), thekey.string())
                            );
                            removeTempDir(tmpdir, null);
                        } catch (final InterruptedException | ExecutionException exc) {
                            throw new ArtipieIOException(exc);
                        }
                        return obj;
                    }
                )
        );
    }

    /**
     * Get info Ruby gem.
     *
     * @param gems Ruby gem to extract info
     * @return Completable action
     */
    public CompletionStage<String> getDependencies(final List<Key> gems) {
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    return Files.createTempDirectory("dependency");
                } catch (final IOException exc) {
                    throw new ArtipieIOException(exc);
                }
            }
        ).thenCompose(
            tmpdir -> {
                final Storage newstorage = new FileStorage(tmpdir);
                CompletionStage<Path> res = null;
                for (final Key gem : gems) {
                    res = Gem.copyStorage(this.storage, newstorage, gem)
                        .thenApply(ignore -> tmpdir);
                }
                return res;
            }
        ).thenCompose(
            tmpdir -> this.sharedInfo()
                .thenApply(
                    rubyjson -> {
                        final String obj;
                        try {
                            final List<Path> paths = new ArrayList<>(gems.size());
                            for (final Key gem : gems) {
                                final Key thekey = this.getGemFile(gem, false).toCompletableFuture().get();
                                final Path path = Paths.get(tmpdir.toString(), thekey.string());
                                paths.add(path);
                            }
                            obj = rubyjson.getDependencies(paths);
                            removeTempDir(tmpdir, null);
                        } catch (final InterruptedException | ExecutionException exc) {
                            throw new ArtipieIOException(exc);
                        }
                        return obj;
                    }
                )
        );
    }


    /**
     * Get info Ruby gem.
     *
     * @param filename Ruby file to return
     * @return Completable action
     */
    public CompletionStage<byte[]> getRubyFile(final Key filename) {
        System.out.println(String.format("In getRubyFile for %s", filename.string()));
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return Files.createTempDirectory("statics");
                    } catch (final IOException exc) {
                        throw new ArtipieIOException(exc);
                    }
                }
        ).thenCompose(
                tmpdir -> {
                    final Storage newstorage = new FileStorage(tmpdir);
                    CompletionStage<Path> res = null;
                    return Gem.copyStorage(this.storage, newstorage, filename)
                                .thenApply(ignore -> tmpdir);
                }
        ).thenCompose(
            tmpdir -> this.sharedInfo()
                .thenApply(
                    rubyjson -> {
                        try {
                            System.out.println(String.format("Getting %s", filename.string()));
                            CompletionStage<Key> st = this.getGemFile(filename, true);
                            System.out.println("stage");
                            CompletableFuture<Key> ft = st.toCompletableFuture();
                            System.out.println("future");
                            final Key thekey = ft.get(10, TimeUnit.MILLISECONDS);
                            if(thekey == null) {
                                System.out.println("88888888");
                            } else {
                               System.out.println(String.format("7777777: %s", thekey.string()));
                            }
                            final Path path = Paths.get(tmpdir.toString(), thekey.string());
                            System.out.println(String.format("Reading %s", path));
                            File file = new File(path.toString());
                            try {
                                byte[] fileContent = Files.readAllBytes(file.toPath());
                                return fileContent;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } catch (final TimeoutException | InterruptedException | ExecutionException exc) {
                            Key thekey;
                            try {
                                thekey = this.getGemFile(new Key.From("thor-1.0.1.gemspec.rz"), true).toCompletableFuture()
                                    .get(1, TimeUnit.SECONDS);
                                final Path path = Paths.get(tmpdir.toString(), thekey.string());
                                System.out.println(String.format("Reading %s", path));
                                File file = new File(path.toString());
                                try {
                                    byte[] fileContent = Files.readAllBytes(file.toPath());
                                    return fileContent;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            } catch (TimeoutException e) {
                                e.printStackTrace();
                            }
                        } finally {
                            System.out.println("remove tmp dir");
                            removeTempDir(tmpdir, null);
                        }
                        return null;
                    }
                )
        );
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
        final Set<String> vars = new HashSet<>(
            Arrays.asList(
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
                    key -> true
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
                        return new GemIndex.Synchronized(this.indexer.get());
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
                        return new GemInfo.Synchronized(this.extractor.get());
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
    private CompletionStage<Key> getGemFile(final Key gem, final boolean exact) {
        final CompletableFuture<Key> future = new CompletableFuture<>();
        Single.fromFuture(this.storage.list(Key.ROOT))
            .map(
                list -> list.stream().filter(
                    key -> {
                        return (!exact && key.string().contains(gem.string())) || (exact && key.string().equals(gem.string()));
                    }
                ).limit(1).collect(Collectors.toList()))
            .flatMapObservable(Observable::fromIterable).forEach(future::complete);
        return future;
    }
}

