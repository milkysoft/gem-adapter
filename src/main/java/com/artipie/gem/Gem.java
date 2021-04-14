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
import com.artipie.asto.rx.RxCopy;
import com.artipie.asto.rx.RxStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.commons.io.IOUtils;
import org.jruby.Ruby;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

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
    final static RubyRuntimeAdapter evaler = JavaEmbedUtils.newRuntimeAdapter();;
    final static Ruby runtime = JavaEmbedUtils.initialize(new ArrayList<>(0));
    static IRubyObject recvr = null;
    static GemIndexer gemIndexer;
        /**
         * Ctor.
         *
         * @param storage The storage.
         */
    Gem(final Storage storage) {
        this.storage = storage;

        final String script;
        try {
            script = IOUtils.toString(
                Gem.class.getResourceAsStream("/AstoUpdater.rb"),
                StandardCharsets.UTF_8
            );
            evaler.eval(runtime, script);
            if(recvr == null) {
                recvr = evaler.eval(runtime, "AstoUpdater");
            }
            gemIndexer = (GemIndexer) JavaEmbedUtils.invokeMethod(
                runtime, recvr,
                "new",
                null,
                GemIndexer.class
            );
        } catch (final IOException exc) {
            throw new UncheckedIOException(exc);
        }
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
        return this.batchUpdate(key.parent().isPresent() ? key.parent().get() : key);
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
        CompletableFuture<Void> res = CompletableFuture.runAsync(
            () -> {
                rubyUpdater(tmpdir.toString());
            }
        );
        res.join();
        List<Key> keys = new ArrayList<>();
        keys.add(prefix);
        return new RxCopy(new RxStorageWrapper(new FileStorage(tmpdir)), keys)
            .copy(new RxStorageWrapper(this.storage))
            .to(CompletableInterop.await());
    }

    /**
     * Lookup an instance of slice, implemented with JRuby.
     * @param repo The temp repo path.
     * @return The Slice.
     */
    static void rubyUpdater(final String repo) {
            gemIndexer.index(repo);
    }
}

