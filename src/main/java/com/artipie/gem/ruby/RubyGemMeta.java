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

package com.artipie.gem.ruby;

import com.artipie.asto.ArtipieIOException;
import com.artipie.gem.GemMeta;
import com.artipie.gem.IRubyInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.apache.commons.io.IOUtils;
import org.jruby.Ruby;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;

/**
 * JRuby implementation of GemInfo metadata parser.
 * @since 1.0
 */
public final class RubyGemMeta implements GemMeta {

    /**
     * Ruby runtime.
     */
    private final Ruby ruby;

    /**
     * Ctor.
     * @param ruby Runtime
     */
    public RubyGemMeta(final Ruby ruby) {
        this.ruby = ruby;
    }

    @Override
    public JsonObject info(final Path gem) {
        final String script;
        final JsonObject object;
        final RubyRuntimeAdapter adapter = JavaEmbedUtils.newRuntimeAdapter();
        adapter.eval(this.ruby, "require 'rubygems/package.rb'");
        try {
            script = IOUtils.toString(
                RubyGemMeta.class.getResourceAsStream("/info.rb"),
                StandardCharsets.UTF_8
            );
            adapter.eval(this.ruby, script);
            final IRubyInfo info = (IRubyInfo) JavaEmbedUtils.invokeMethod(
                this.ruby,
                adapter.eval(this.ruby, "Ex"),
                "new",
                new Object[]{gem.toString()},
                IRubyInfo.class
            );
            final org.jruby.RubyString obj = info.info();
            final ByteArrayInputStream bis = new ByteArrayInputStream(obj.getBytes());
            final JsonReader jsonreader = Json.createReader(bis);
            object = jsonreader.readObject();
            jsonreader.close();
        } catch (final IOException exc) {
            throw new ArtipieIOException(exc);
        }
        return object;
    }
}
