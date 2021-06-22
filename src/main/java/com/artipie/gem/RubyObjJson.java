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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
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
public final class RubyObjJson implements GemInfo {

    /**
     * Ruby runtime.
     */
    private final RubyRuntimeAdapter runtime;

    /**
     * Ruby interpreter.
     */
    private final Ruby ruby;

    /**
     * New Ruby object JSON converter.
     * @param runtime Is Ruby runtime
     * @param ruby Is Ruby system
     */
    RubyObjJson(final RubyRuntimeAdapter runtime, final Ruby ruby) {
        this.runtime = runtime;
        this.ruby = ruby;
    }

    /**
     * Create JSON info for gem.
     * @param gempath Full path to gem file or null
     * @return JsonObjectBuilder result
     */
    public JsonObject getinfo(final Path gempath) {
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
     * Create marshaled binary info for gem.
     * @param gempath Full path to gem file or null
     * @return String result
     */
    public String getDependencies(final Path gempath) {
        final List<Variable<Object>> vars = this.getSpecification(gempath)
            .getVariableList();
        String res = new StringBuilder().append((char) 4)
            .append((char) 8).append('I').append('\"').append("a[{").toString();
        for (final Variable<Object> var : vars) {
            final String name = var.getName();
            if (name.equals("@dependencies")) {
                res = res.concat(":name=>\"").concat(RubyObjJson.getGemName(vars))
                    .concat("\", :number=>\"").concat(RubyObjJson.getGemVersion(vars))
                    .concat("\"");
                res = res.concat(", :platform=>\"ruby\", :dependencies=>[");
                final String val = var.getValue().toString();
                final String[] dependencies = val.substring(1, val.length() - 1).split(",");
                for (final String dependency : dependencies) {
                    String srch = "type=";
                    int indexs = dependency.indexOf(srch) + srch.length();
                    srch = " name=\"";
                    int indexe = dependency.indexOf(srch, indexs);
                    if (":development".equals(dependency.substring(indexs, indexe))) {
                        continue;
                    }
                    srch = " name=\"";
                    indexs = dependency.indexOf(srch) + srch.length();
                    indexe = dependency.indexOf("\" ", indexs);
                    String depname = dependency.substring(indexs, indexe);
                    srch = "requirements=\"";
                    indexs = dependency.indexOf(srch, indexe) + srch.length();
                    indexe = dependency.indexOf("\">", indexs);
                    final String depver = dependency.substring(indexs, indexe);
                    res = res.concat("[\"".concat(depname).concat("\", \"").concat(depver)
                        .concat("\"]"));
                }
                res = res.concat("]}]");
                res = res.concat(new StringBuilder().append((char) 6).append(':').append((char) 6)
                    .append("ET").toString());
            }

        }
        return res;
    }

    /**
     * Get Ruby specification for arbitrary gem.
     * @param gempath Full path to gem file or null
     * @return RubyObject specification
     */
    private RubyObject getSpecification(final Path gempath) {
        return (RubyObject) this.runtime.eval(
            this.ruby, String.format(
                "require 'rubygems/package.rb'\nGem::Package.new('%s').spec", gempath.toString()
            )
        );
    }

    /**
     * Get Ruby gem from Path.
     * @param vars List of Variables
     * @return Gem name with version
     */
    private static String getGemVersion(final List<Variable<Object>> vars) {
        String res = "";
        for (final Variable<Object> var : vars) {
            final String name = var.getName();
            if (name.equals("@version")) {
                res = var.getValue().toString();
                break;
            }
        }
        return res;
    }

    /**
     * Get Ruby gem from Path.
     * @param vars List of Variables
     * @return Gem name with version
     */
    private static String getGemName(final List<Variable<Object>> vars) {
        String res = "";
        for (final Variable<Object> var : vars) {
            final String name = var.getName();
            if (name.equals("@name")) {
                res = var.getValue().toString();
                break;
            }
        }
        return res;
    }
}
