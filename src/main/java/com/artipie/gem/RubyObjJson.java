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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.Variable;

/**
 * Returns some basic information about the given gem.
 *
 * @since 1.0
 */
public class RubyObjJson {

    /**
     * Ruby runtime.
     */
    private final RubyRuntimeAdapter runtime;

    /**
     * Ruby interpreter.
     */
    private final Ruby ruby;

    /**
     * Ruby object.
     */
    private final Path gemdir;

    /**
     * Ruby object.
     */
    private final String gem;

    /**
     * New Ruby object JSON converter.
     * @param gemdir Directory to search for gem
     * @param gem Gem to convert
     */
    RubyObjJson(final Path gemdir, final String gem) {
        this.gemdir = gemdir;
        this.gem = gem;
        this.runtime = JavaEmbedUtils.newRuntimeAdapter();
        this.ruby = JavaEmbedUtils.initialize(Collections.emptyList());
    }

    /**
     * Create JSON info for gem.
     * @param gempath Full path to gem file or null
     * @return JsonObjectBuilder result
     */
    public JsonObject createJson(final Path gempath) {
        final List<Variable<Object>> vars = this.getSpecification(gempath)
            .getVariableList();
        final JsonObjectBuilder obj = Json.createObjectBuilder();
        for (final Variable<Object> var : vars) {
            String name = var.getName();
            if (name.charAt(0) == '@') {
                name = var.getName().substring(1);
            }
            if (var.getValue() != null) {
                obj.add(name, var.getValue().toString());
            }
        }
        return obj.build();
    }

    /**
     * Install new gem.
     * @param gempath Full path to gem file or null
     * @return RubyObject specification
     */
    private RubyObject getSpecification(final Path gempath) {
        String gemfile = "";
        this.runtime.eval(
            this.ruby,
            "require 'rubygems/package.rb'"
        );
        try {
            if (gempath == null) {
                final Optional<String> filename = Files.walk(this.gemdir).map(Path::toString)
                    .filter(file -> file.contains(this.gem) && file.contains(".gem")).findFirst();
                if (filename.isPresent()) {
                    gemfile = filename.get();
                }
            } else {
                gemfile = gempath.toString();
            }
        } catch (final IOException exc) {
            throw new ArtipieIOException(exc);
        }
        final String script = String.format("Gem::Package.new('%s').spec", gemfile);
        return (RubyObject) this.runtime.eval(
            this.ruby, script
        );
    }
}
