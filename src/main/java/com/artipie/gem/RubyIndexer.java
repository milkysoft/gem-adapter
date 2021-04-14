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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
public final class RubyIndexer implements GemIndexer {
    /**
     * Primary storage.
     */
    private final Ruby runtime;

    /**
     * Primary storage.
     *
     * @param runtime Is good
     */
    public RubyIndexer(final Ruby runtime) {
        this.runtime = runtime;
    }

    @Override
    public void index(final String repo) {
        target(this.runtime).index(repo);
    }

    /**
     * Endpoint path pattern.
     *
     * @param runtime Is good
     * @return GemINdexer Is good
     */
    private static GemIndexer target(final Ruby runtime) {
        try {
            final  RubyRuntimeAdapter evaler = JavaEmbedUtils.newRuntimeAdapter();
            final String script = IOUtils.toString(
                GemIndexer.class.getResourceAsStream("/AstoUpdater.rb"),
                StandardCharsets.UTF_8
            );
            evaler.eval(runtime, script);
            final IRubyObject ruby = evaler.eval(runtime, "AstoUpdater");
            return  (GemIndexer) JavaEmbedUtils.invokeMethod(
                runtime, ruby,
                "new",
                null,
                GemIndexer.class
            );
        } catch (final IOException exc) {
            throw new UncheckedIOException(exc);
        }
    }
}
