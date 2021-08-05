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

import com.artipie.gem.GemMeta;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyObject;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.javasupport.JavaObject;
import org.jruby.runtime.builtin.Variable;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

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
    public <T> T info(final Path gem, final GemMeta.InfoFormat<T> fmt) {
        final RubyRuntimeAdapter adapter = JavaEmbedUtils.newRuntimeAdapter();
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        adapter.eval(this.ruby, "require 'rubygems/package.rb'");
        final RubyObject spec = (RubyObject) adapter.eval(
            this.ruby, String.format(
                "Gem::Package.new('%s').spec", gem.toString()
            )
        );
        List<Variable<Object>> vars = spec.getVariableList();
        for (Variable<Object> dt : vars) {
            if ("@name".equals(dt.getName())) {
                builder.add("name", this.getVar(vars, "@name"));
            } else if ("@authors".equals(dt.getName())) {
                JsonArrayBuilder jsonauthors = Json.createArrayBuilder();
                final RubyObject authors = (RubyObject) adapter.eval(
                    this.ruby, String.format(
                        "Gem::Package.new('%s').spec.authors", gem
                    )
                );
                for (int i =0; i< authors.convertToArray().getLength(); i++) {
                    jsonauthors.add(authors.convertToArray().get(i).toString());
                }
                builder.add("authors", jsonauthors);
            } else if ("@homepage".equals(dt.getName())) {
                builder.add("homepage", this.getVar(vars, "@homepage"));
            } else if("@dependencies".equals(dt.getName())) {
                JsonObjectBuilder jsondep = Json.createObjectBuilder();
                JsonArrayBuilder devdeps = Json.createArrayBuilder();
                JsonArrayBuilder runtimedeps = Json.createArrayBuilder();
                final RubyObject deps = (RubyObject) adapter.eval(
                    this.ruby, String.format(
                        "Gem::Package.new('%s').spec.dependencies", gem
                    )
                );
                for (int i =0; i<deps.convertToArray().getLength(); i++) {
                    RubyObject o = (RubyObject) deps.convertToArray().get(i);
                    List<Variable<Object>> varos = o.getVariableList();
                    for (Variable<Object> var : varos) {
                        if ("@type".equals(var.getName())) {
                            if("development".equals(var.getValue().toString())) {
                                JsonObjectBuilder devdep = Json.createObjectBuilder();
                                devdep.add("name", this.getVar(varos, "@name"));
                                devdep.add("requirements", this.getVar(varos, "@requirement"));
                                devdeps.add(devdep);
                            } else if("runtime".equals(var.getValue().toString())) {
                                JsonObjectBuilder runtimedep = Json.createObjectBuilder();
                                runtimedep.add("name", this.getVar(varos, "@name"));
                                runtimedep.add("requirements", this.getVar(varos, "@requirement"));
                                runtimedeps.add(runtimedep);
                            }

                        }
                    };
                }
                jsondep.add("development", devdeps);
                jsondep.add("runtime", runtimedeps);
                builder.add("dependencies", jsondep);
            }
        }
        return fmt.print(builder.build());
    }

    private String getVar (List<Variable<Object>> varos, String name) {
        String res = "";
        for (Variable<Object> var : varos) {
            if (name.equals(var.getName())) {
                res = var.getValue().toString();
            }
        };
        return res;
    }
}
