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
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 */
public final class RubyIndexer implements GemIndexer {
    @Override
    public void index(final String repo) {
        final  RubyRuntimeAdapter evaler = JavaEmbedUtils.newRuntimeAdapter();
        final String script = "require 'rubygems/indexer.rb'\nGem::Indexer.new(\""
            .concat(repo).concat("\",{ build_modern:true }).generate_index");
        evaler.eval(Gem.RUNTIME, script);
    }
}
