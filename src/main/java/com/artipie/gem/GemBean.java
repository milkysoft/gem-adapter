package com.artipie.gem;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.ext.KeyLastPart;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.lock.Lock;
import com.artipie.asto.lock.storage.StorageLock;
import com.artipie.asto.rx.RxStorageWrapper;

import com.artipie.http.Slice;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Completable;
import com.artipie.asto.Key;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.io.IOUtils;
import org.jruby.Ruby;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;

public class GemBean {
    /**
     * Primary storage.
     */
    private final Storage storage;

    GemBean(final Storage storage) {
        this.storage = storage;
    }

    public Completable update(final String key) {
        return this.update(new Key.From(key));
    }

    public Completable update(final Key key) {
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
     * Batch update RPM files for repository.
     * @param prefix Repository key prefix
     * @return Completable action
     */
    public Completable batchUpdate(final Key prefix) {
        final Path tmpdir;
        try {
            tmpdir = Files.createTempDirectory("repo-");
        } catch (final IOException err) {
            throw new IllegalStateException("Failed to create temp dir", err);
        }
        final Storage local = new FileStorage(tmpdir);
        rubyLookUp("GemBean", local, "jjjj", null);
        return this.doWithLock(
            prefix,
            () -> SingleInterop.fromFuture(this.storage.list(prefix))
                .flatMapPublisher(Flowable::fromIterable)
                .filter(key -> key.string().endsWith("xml.gz"))
                .flatMapCompletable(
                    key -> new RxStorageWrapper(this.storage)
                        .value(key)
                        .flatMapCompletable(
                            content -> new RxStorageWrapper(local)
                                .save(new Key.From(new KeyLastPart(key).get()), content)
                        )
                )
        ).doOnTerminate(
            () -> {

            }
        );
    }

    /**
     * Removes old metadata.
     * @param preserve Metadata to keep
     * @param prefix Repo prefix
     * @return Completable
     */
    private Completable removeOldMetadata(final Set<String> preserve, final Key prefix) {
        return new RxStorageWrapper(this.storage).list(new Key.From(prefix, "repodata"))
            .flatMapObservable(Observable::fromIterable)
            .filter(item -> !preserve.contains(Paths.get(item.string()).getFileName().toString()))
            .flatMapCompletable(
                item -> new RxStorageWrapper(this.storage).delete(item)
            );
    }

    /**
     * Performs operation under root lock with one hour expiration time.
     *
     * @param target Lock target key.
     * @param operation Operation.
     * @return Completion of operation and lock.
     */
    private Completable doWithLock(final Key target, final Supplier<Completable> operation) {
        final Lock lock = new StorageLock(
            this.storage,
            target,
            Instant.now().plus(Duration.ofHours(1))
        );
        return Completable.fromFuture(
            lock.acquire()
                .thenCompose(nothing -> operation.get().to(CompletableInterop.await()))
                .thenCompose(nothing -> lock.release())
                .toCompletableFuture()
        );
    }


    public static Slice rubyLookUp(final String rclass,
                                    final Storage storage, final String repoPath,
                                    final Ruby runtime) {
        try {
            final RubyRuntimeAdapter evaler = JavaEmbedUtils.newRuntimeAdapter();
            final String script = IOUtils.toString(
                GemSlice.class.getResourceAsStream(String.format("/%s.rb", rclass)),
                StandardCharsets.UTF_8
            );
            evaler.eval(runtime, script);
            return (Slice) JavaEmbedUtils.invokeMethod(
                runtime,
                evaler.eval(runtime, rclass),
                "new",
                new Object[]{storage,repoPath},
                Slice.class
            );
        } catch (final IOException exc) {
            throw new UncheckedIOException(exc);
        }
    }
}
