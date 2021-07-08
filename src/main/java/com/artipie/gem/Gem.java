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

import java.io.File;
import org.apache.commons.io.FileUtils;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.http.Slice;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.IOUtils;
import org.jruby.Ruby;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.specialized.RubyArrayTwoObject;

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
        final Storage local = new FileStorage(Paths.get("D:\\Data\\Artipie\\gem-adapter\\ggg"));
        final Path tmpdir;
        Paths.get("jjjj");
        try {
            tmpdir = Files.createTempDirectory(prefix.string());
        } catch (final IOException err) {
            throw new IllegalStateException("Failed to create temp dir", err);
        }
        CompletableFuture.runAsync(
            () -> {
                try {
                    Collection<Key> j = local.list(new Key.From("Artipie"))
                        .get();
                    System.out.println(j.size());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }


                rubyUpdater(
                    "AstoUpdater", this.storage, tmpdir.toString(),
                    JavaEmbedUtils.initialize(new ArrayList<>(0))
                );
                //final Storage local = new FileStorage(tmpdir);
            }
        );
        return SingleInterop.fromFuture(local.list(prefix))
            .flatMapPublisher(Flowable::fromIterable)
            .flatMapCompletable(
                key -> new RxStorageWrapper(local)
                    .value(key)
                    .flatMapCompletable(
                        content -> new RxStorageWrapper(this.storage)
                            .save(new Key.From("Artipie"), content)

                    )
            ).to(CompletableInterop.await());
//        return new RxStorageWrapper(local)
//            .value(new Key.From("Artipie"))
//            .flatMapCompletable(
//                content -> {
//                    System.out.println(content);
//                    return new RxStorageWrapper(this.storage)
//                        .save(prefix, content);
//                }
//            );
    }

    public static void main(String[] args) throws Exception{
        Ruby runtime = JavaEmbedUtils.initialize(new ArrayList<>(0));
        final RubyRuntimeAdapter evaler = JavaEmbedUtils.newRuntimeAdapter();
        final String script = IOUtils.toString(
            Gem.class.getResourceAsStream("/example.rb"),
            StandardCharsets.UTF_8
        );
        evaler.eval(runtime, script);
        IRubyObject recvr = JavaEmbedUtils.newRuntimeAdapter().eval(runtime, script);
        Example ex = (Example) JavaEmbedUtils.invokeMethod(
            runtime,
            evaler.eval(runtime, "Ex"),
            "new",
            new Object[]{"gviz-0.3.5.gem"},
            Example.class
        );
        System.out.println("8776786");
        //org.jruby.RubyArray<IRubyObject> deps = ex.dependencies();
        //RubyArrayTwoObject obj = (RubyArrayTwoObject) deps.get(0);
        //String name = (String) obj.get(0);
        //String version = (String) obj.get(1);
        //System.out.printf("val=%s  %s\n", name, version);
        org.jruby.RubyString obj = ex.dependencies();
        FileUtils.writeByteArrayToFile(new File("yyyy.data"), obj.getBytes());
        System.out.printf("val=%s\n", obj);
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
            IRubyObject recvr = JavaEmbedUtils.newRuntimeAdapter().eval(runtime, script);
            JavaEmbedUtils.invokeMethod(
                runtime,
                evaler.eval(runtime, rclass),
                "new",
                new Object[]{storage, repo},
                Slice.class
            );
            return (Slice) JavaEmbedUtils.invokeMethod(
                runtime,
                evaler.eval(runtime, rclass),
                "generate_index2",
                new Object[]{storage, repo},
                Slice.class
            );
        } catch (final IOException exc) {
            throw new UncheckedIOException(exc);
        }
    }
}
