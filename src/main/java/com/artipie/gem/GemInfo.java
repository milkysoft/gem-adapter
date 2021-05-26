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
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.common.RsJson;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.Variable;
import org.reactivestreams.Publisher;

/**
 * Returns some basic information about the given gem. GET - /api/v1/gems/[GEM NAME].(json|yaml)
 * https://guides.rubygems.org/rubygems-org-api/
 *
 * @todo #32:120min Gem Information implementation.
 *  The implementation must be able to response with either json or yaml format. An example response
 *  can be obtained via {@code curl https://rubygems.org/api/v1/gems/rails.json}
 * @since 0.2
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
public final class GemInfo implements Slice {

    /**
     * Endpoint path pattern.
     */
    public static final Pattern PATH_PATTERN = Pattern.compile("/api/v1/gems/([\\w]+).(json|yml)");

    /**
     * Ruby runtime.
     */
    private final RubyRuntimeAdapter runtime;

    /**
     * Ruby interpreter.
     */
    private final Storage storage;

    /**
     * Ruby interpreter.
     */
    private final Ruby ruby;

    /**
     * New gem info.
     * @param storage Gems storage
     * @param runtime Ruby runtime
     * @param ruby Interpreter
     */
    public GemInfo(final Storage storage, final RubyRuntimeAdapter runtime, final Ruby ruby) {
        this.runtime = runtime;
        this.ruby = ruby;
        this.storage = storage;
    }

    /**
     * Initialize indexer.
     */
    public void initialize() {
        this.runtime.eval(
            this.ruby,
            "require 'rubygems/commands/contents_command.rb'\n".concat(
                "require 'rubygems/installer.rb'"
            )
        );
    }

    /**
     * Create new gem indexer.
     * @param storage Gems storage
     * @return A new ruby gem info.
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static GemInfo createNew(final Storage storage) {
        final GemInfo result = new GemInfo(
            storage,
            JavaEmbedUtils.newRuntimeAdapter(),
            JavaEmbedUtils.initialize(Collections.emptyList())
        );
        result.initialize();
        return result;
    }

    @Override
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Matcher matcher = PATH_PATTERN.matcher(new RequestLineFrom(line).uri().toString());
        if (matcher.find()) {
            final String gem = matcher.group(1);
            final Path tmpdir = this.preparedir(gem);
            this.install(tmpdir, gem);
            final String script = String.format(
                "Gem::Commands::ContentsCommand.new.spec_for('%s')", gem
            );
            final String extension = matcher.group(2);
            Logger.info(
                GemInfo.class,
                "Gem info for '%s' has been requested. Extension: '%s'",
                gem,
                extension
            );
            final RubyObject gemobject = (RubyObject) this.runtime.eval(
                this.ruby, script
            );
            final List<Variable<Object>> vars = gemobject.getVariableList();
            final JsonObjectBuilder obj = Json.createObjectBuilder();
            for (int ind = 0; ind < vars.size(); ind = ind + 1) {
                final Variable<Object> var = vars.get(ind);
                String name = var.getName();
                if (var.getName().substring(0, 1).equals("@")) {
                    name = var.getName().substring(1);
                }
                if (gemobject.getVariable(ind) != null) {
                    obj.add(name, gemobject.getVariable(ind).toString());
                }
            }
            return new RsJson(obj);
        } else {
            throw new IllegalStateException("Not expected path has been matched");
        }
    }

    /**
     * Copy storage from src to dst.
     * @param src Source storage
     * @param dst Destination storage
     * @param gem Key for gem
     * @return Async result
     */
    private static CompletionStage<Void> copyStorage(final Storage src, final Storage dst,
        final String gem) {
        return Single.fromFuture(src.list(Key.ROOT))
            .map(
                list -> list.stream().filter(
                    key -> key.string().contains(gem)
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
     * Install new gem.
     * @param tmpdir Gem directory
     * @param gem Is Gem to be installed
     */
    private void install(final Path tmpdir, final String gem) {
        try {
            final List<String> files = Files.walk(tmpdir).map(Path::toString)
                .collect(Collectors.toList());
            for (final String file : files) {
                if (file.contains(gem) && file.contains(".gem")) {
                    final String script = "Gem::Installer.at('"
                        .concat(file).concat("')");
                    this.runtime.eval(this.ruby, script);
                }
            }
        } catch (final IOException exc) {
            Logger.error(GemInfo.class, exc.getMessage());
        }
    }

    /**
     * Prepare directory to install new gem.
     * @param gem Is Gem to be installed
     * @return Path To directory with gem
     */
    private Path preparedir(final String gem) {
        final Path tmpdir;
        try {
            tmpdir = Files.createTempDirectory("gem");
        } catch (final IOException exc) {
            throw new UncheckedIOException(exc);
        }
        CompletableFuture.allOf(
            (CompletableFuture<?>) GemInfo.copyStorage(
                this.storage, new FileStorage(tmpdir), gem
            )
        ).join();
        return tmpdir;
    }
}
