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
import com.artipie.asto.Copy;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.misc.UncheckedIOFunc;
import com.artipie.gem.ruby.Dependencies;
import com.artipie.gem.GemMeta.MetaInfo;
import com.artipie.gem.ruby.RubyGemIndex;
import com.artipie.gem.ruby.RubyGemMeta;
import com.artipie.gem.ruby.SharedRuntime;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.apache.commons.io.FileUtils;

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
     * Read only set of metadata item names.
     */
    private static final Set<Key> META_NAMES = Collections.unmodifiableSet(
        Stream.of(
            "latest_specs.4.8", "latest_specs.4.8.gz", "prerelease_specs.4.8",
            "prerelease_specs.4.8.gz", "specs.4.8", "specs.4.8.gz"
        ).map(Key.From::new).collect(Collectors.toSet())
    );

    /**
     * Gem repository storage.
     */
    private final Storage storage;

    /**
     * Shared ruby runtime.
     */
    private final SharedRuntime shared;

    /**
     * New Gem SDK with default indexer.
     * @param storage Repository storage.
     */
    public Gem(final Storage storage) {
        this.storage = storage;
        this.shared = new SharedRuntime();
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
            tmpdir -> this.shared.apply(Dependencies::new)
                .thenApply(
                    rubyjson -> {
                        try {
                            System.out.println(String.format("Getting %s", filename.string()));
                            CompletionStage<Key> st = this.getGemFile(filename, true, new Key.From("thor-1.0.1.gemspec.rz"));
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
                            System.out.println("ERRRRRRRRRRR!!!!!");
                        } finally {
                            System.out.println("remove tmp dir");
                            removeTempDir(tmpdir);
                        }
                        return null;
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
    public CompletionStage<byte[]> getDependencies(final List<Key> gems) {
        final AtomicReference<Path> dir = new AtomicReference<>();
        return newTempDir().thenCompose(
            tmp -> {
                dir.set(tmp);
                return new Copy(this.storage, key -> META_NAMES.contains(key) || key.equals(gems.get(0)))
                    .copy(new FileStorage(tmp))
                    .thenApply(ignore -> tmp);
            }
        ).thenCompose(
            tmp -> this.shared.apply(RubyGemMeta::new)
                .thenCompose(
                    info -> new FileStorage(tmp).list(Key.ROOT).thenApply(
                        items -> items.stream().findFirst()
                            .map(first -> Paths.get(tmp.toString(), first.string()))
                            .map(path -> info.dependencies(path))
                            .orElseThrow(() -> new ArtipieIOException("gem did not found"))
                    )
                ).handle(removeTempDir(tmp))
        );
    }

    /**
     * Find gem in a given path.
     * @param gem Gem name to get info
     * @return String full path to gem file
     */
    private CompletionStage<Key> getGemFile(final Key gem, final boolean exact, final Key fallout) {
        final CompletableFuture<Key> future = new CompletableFuture<>();
        Single.fromFuture(this.storage.list(Key.ROOT))
            .map(
                list -> list.stream().filter(
                    key -> (!exact && key.string().contains(gem.string()) && key.string().endsWith(".gem")) ||
                        (exact && (key.string().equals(gem.string())))
                ).count() > 0 ?
                    list.stream().filter(
                        key -> (!exact && key.string().contains(gem.string()) && key.string().endsWith(".gem")) ||
                            (exact && (key.string().equals(gem.string())))
                    ).limit(1).collect(Collectors.toList()) :
                    list.stream().filter(
                        key -> (!exact && key.string().contains(gem.string())) || (exact && (key.string().equals(gem.string())
                            || (fallout != null && (key.string().equals(fallout.string())))))
                    ).limit(1).collect(Collectors.toList())
            )
            .flatMapObservable(Observable::fromIterable).forEach(future::complete);
        return future;
    }

    /**
     * Batch update Ruby gems for repository.
     *
     * @param gem Ruby gem for indexing
     * @return Completable action
     */
    public CompletionStage<Void> update(final Key gem) {
        final AtomicReference<Path> dir = new AtomicReference<>();
        return newTempDir().thenCompose(
            tmp -> {
                dir.set(tmp);
                return new Copy(this.storage, key -> META_NAMES.contains(key) || key.equals(gem))
                .copy(new FileStorage(tmp))
                .thenApply(ignore -> tmp);
            }
        ).thenCompose(
            tmp -> this.shared.apply(RubyGemMeta::new)
                .thenApply(
                    meta -> {
                        final RevisionFormat fmt = new RevisionFormat();
                        meta.info(Paths.get(tmp.toString(), gem.string())).print(fmt);
                        return fmt.toString();
                    }
                ).thenApply(
                    new UncheckedIOFunc<>(
                        name -> {
                            final Path path = Paths.get(tmp.toString(), gem.string());
                            final Path target = path.getParent().resolve(name);
                            Files.move(path, target);
                            return target;
                        }
                    )
                )
        ).thenCompose(
            fullpath -> this.shared.apply(RubyGemIndex::new)
                .thenAccept(index -> index.update(fullpath))
                .thenCompose(
                    none -> new Copy(
                        new FileStorage(dir.get())
                    ).copy(this.storage)
                )
                .handle(removeTempDir(dir.get()))
        );
    }

    /**
     * Gem info data.
     * @param gem Gem name
     * @return Future
     */
    public CompletionStage<GemMeta.MetaInfo> info(final String gem) {
        return newTempDir().thenCompose(
            tmp -> new Copy(this.storage, new IsGemKey(gem))
                .copy(new FileStorage(tmp))
                .thenApply(ignore -> tmp)
        ).thenCompose(
            tmp -> this.shared.apply(RubyGemMeta::new)
                .thenCompose(
                    info -> new FileStorage(tmp).list(Key.ROOT).thenApply(
                        items -> items.stream().findFirst()
                            .map(first -> Paths.get(tmp.toString(), first.string()))
                            .map(path -> info.info(path))
                            .orElseThrow(() -> new ArtipieIOException("gem not found"))
                    )
                ).handle(removeTempDir(tmp))
        );
    }

    /**
     * Create new temp dir asynchronously.
     * @return Future
     */
    private static CompletionStage<Path> newTempDir() {
        return CompletableFuture.supplyAsync(
            new UncheckedSupplier<>(
                () -> Files.createTempDirectory(Gem.class.getSimpleName())
            )
        );
    }

    /**
     * Handle async result.
     * @param tmpdir Path directory to remove
     * @param <T> Result type
     * @return Function handler
     */
    private static <T> BiFunction<T, Throwable, T> removeTempDir(
        final Path tmpdir) {
        return (res, err) -> {
            try {
                if (tmpdir != null) {
                    FileUtils.deleteDirectory(new File(tmpdir.toString()));
                }
            } catch (final IOException iox) {
                throw new ArtipieIOException(iox);
            }
            if (err != null) {
                throw new CompletionException(err);
            }
            return res;
        };
    }

    /**
     * Predicate to find gem key by name.
     * @since 1.0
     */
    private static final class IsGemKey implements Predicate<Key>  {

        /**
         * Gem name.
         */
        private final String name;

        /**
         * New predicate.
         * @param name Gem name
         */
        IsGemKey(final String name) {
            this.name = name;
        }

        @Override
        public boolean test(final Key key) {
            final String str = key.string();
            final int idx = str.lastIndexOf(this.name);
            boolean matches = false;
            if (idx >= 0) {
                final String tail = str.substring(idx + this.name.length());
                if (tail.isEmpty() || tail.matches("^[0-9a-zA-Z\\-\\.]+$")) {
                    matches = true;
                }
            }
            return matches;
        }
    }

    /**
     * Revision Gem meta format.
     * @since 1.0
     */
    private static final class RevisionFormat implements GemMeta.MetaFormat {

        /**
         * Gem name.
         */
        private String name;

        /**
         * Gem value.
         */
        private String version;

        @Override
        public void print(final String nme, final String value) {
            if (nme.equals("name")) {
                this.name = value;
            }
            if (nme.equals("version")) {
                this.version = value;
            }
        }

        @Override
        public void print(final String nme, final MetaInfo value) {
            // do nothing
        }

        @Override
        public String toString() {
            return String.format("%s-%s.gem", this.name, this.version);
        }
    }
}
