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
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.http.Slice;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.commons.io.IOUtils;
import org.jruby.Ruby;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;

/**
 * An SDK, which servers gem packages.
 *
 * Initialize on first request.
 *  Currently, Ruby runtime initialization and Slice evaluation is happening during the GemSlice
 *  construction. Instead, the Ruby runtime initialization and Slice evaluation should happen
 *  on first request.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 */
public class Gem {
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
     * @param key The name of a slice class, implemented in JRuby.
     * @return The Slice.
     */
    public CompletionStage<Void> update(final String key) {
        return this.update(new Key.From(key));
    }

    /**
     * Lookup an instance of slice, implemented with JRuby.
     * @param key The name of a slice class, implemented in JRuby.
     * @return The Slice.
     */
    public CompletionStage<Void> update(final Key key) {
        final String[] parts = key.string().split("/");
        final Key folder;
        if (parts.length == 1) {
            folder = Key.ROOT;
        } else {
            folder = new Key.From(
                Arrays.stream(parts)
                    .limit(parts.length - 1)
                    .toArray(String[]::new)
            );
        }
        return this.batchUpdate(folder);
    }

    /**
     * Batch update Ruby gems for repository.
     * @param prefix Repository key prefix
     * @return Completable action
     */
    public CompletionStage<Void> batchUpdate(final Key prefix) {
        final Path tmpdir;
        try {
            tmpdir = Files.createTempDirectory(prefix.string());
        } catch (final IOException err) {
            throw new IllegalStateException("Failed to create temp dir", err);
        }
        return CompletableFuture.runAsync(
            () -> {
                rubyUpdater(
                    "AstoUpdater", this.storage, tmpdir.toString(),
                    JavaEmbedUtils.initialize(new ArrayList<>(0))
                );
                new RxStorageWrapper(this.storage)
                    .value(prefix)
                    .flatMapCompletable(
                        content -> new RxStorageWrapper(this.storage)
                            .save(prefix, content)
                    );
            }
        );
    }

    /**
     * Lookup an instance of slice, implemented with JRuby.
     * @param rclass The name of a slice class, implemented in JRuby.
     * @param storage The storage to pass directly to Ruby instance.
     * @param repo The temp repo path.
     * @param runtime The JRuby runtime.
     * @return The Slice.
     */
    static Slice rubyUpdater(final String rclass, final Storage storage, final String repo,
        final Ruby runtime) {
        try {
            final RubyRuntimeAdapter evaler = JavaEmbedUtils.newRuntimeAdapter();
            final String script = IOUtils.toString(
                Gem.class.getResourceAsStream(String.format("/%s.rb", rclass)),
                StandardCharsets.UTF_8
            );
            evaler.eval(runtime, script);
            return (Slice) JavaEmbedUtils.invokeMethod(
                runtime,
                evaler.eval(runtime, rclass),
                "new",
                new Object[]{storage, repo},
                Slice.class
            );
        } catch (final IOException exc) {
            throw new UncheckedIOException(exc);
        }
    }
}
