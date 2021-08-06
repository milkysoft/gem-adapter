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
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.Variable;

/**
 * JRuby implementation of GemInfo metadata parser.
 * @since 1.0
 */
public final class RubyGemMeta implements GemMeta {

    /**
     * At Name string.
     */
    private static final String ATNAME = "@name";

    /**
     * Name string.
     */
    private static final String SNAME = "name";

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
        final String hstr = "@homepage";
        final RubyRuntimeAdapter adapter = JavaEmbedUtils.newRuntimeAdapter();
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        adapter.eval(this.ruby, "require 'rubygems/package.rb'");
        final RubyObject spec = (RubyObject) adapter.eval(
            this.ruby, String.format(
                "Gem::Package.new('%s').spec", gem.toString()
            )
        );
        final List<Variable<Object>> vars = spec.getVariableList();
        for (final Variable<Object> thevar : vars) {
            if (RubyGemMeta.ATNAME.equals(thevar.getName())) {
                builder.add(RubyGemMeta.SNAME, this.getVar(vars, RubyGemMeta.ATNAME));
            } else if ("@authors".equals(thevar.getName())) {
                final JsonArrayBuilder jsonauthors = Json.createArrayBuilder();
                this.getAuthors(adapter, gem, jsonauthors);
                builder.add("authors", jsonauthors);
            } else if (hstr.equals(thevar.getName())) {
                builder.add("homepage", this.getVar(vars, hstr));
            } else if ("@dependencies".equals(thevar.getName())) {
                final JsonObjectBuilder jsondep = Json.createObjectBuilder();
                this.getDependencies(adapter, gem, jsondep);
                builder.add("dependencies", jsondep);
            }
        }
        return fmt.print(builder.build());
    }

    /**
     * Ruby runtime.
     * @param varos For variables
     * @param name For variable name
     * @return String variable value
     */
    private static String getVar(final List<Variable<Object>> varos, final String name) {
        String res = "";
        for (final Variable<Object> var : varos) {
            if (name.equals(var.getName())) {
                res = var.getValue().toString();
            }
        }
        return res;
    }

    /**
     * Ruby runtime.
     * @param adapter Ruby adapter
     * @param gem Path to gem
     * @param jsondep Dependencies
     */
    private void getDependencies(final RubyRuntimeAdapter adapter, final Path gem,
        final JsonObjectBuilder jsondep) {
        final String rtstr = "runtime";
        final String dstr = "development";
        final JsonArrayBuilder devdeps = Json.createArrayBuilder();
        final JsonArrayBuilder runtimedeps = Json.createArrayBuilder();
        final RubyObject deps = (RubyObject) adapter.eval(
            this.ruby, String.format(
                "Gem::Package.new('%s').spec.dependencies", gem
            )
        );
        int vari = 0;
        while (vari < deps.convertToArray().getLength()) {
            final RubyObject theobj = (RubyObject) deps.convertToArray().get(vari);
            final List<Variable<Object>> varos = theobj.getVariableList();
            for (final Variable<Object> var : varos) {
                if ("@type".equals(var.getName())) {
                    if (dstr.equals(var.getValue().toString())) {
                        devdeps.add(RubyGemMeta.addDep(varos));
                    } else if (rtstr.equals(var.getValue().toString())) {
                        runtimedeps.add(RubyGemMeta.addDep(varos));
                    }
                }
            }
            vari = vari + 1;
        }
        jsondep.add(dstr, devdeps);
        jsondep.add(rtstr, runtimedeps);
    }

    /**
     * Ruby runtime.
     * @param varos Variables
     * @return JsonObject containing dependencies info
     */
    private static JsonObjectBuilder addDep(final List<Variable<Object>> varos) {
        final String reqstr = "@requirement";
        final String rstr = "requirements";
        final JsonObjectBuilder dep = Json.createObjectBuilder();
        dep.add(
            RubyGemMeta.SNAME,
            RubyGemMeta.getVar(varos, RubyGemMeta.ATNAME)
        );
        dep.add(rstr, RubyGemMeta.getVar(varos, reqstr));
        return dep;
    }

    /**
     * Ruby runtime.
     * @param adapter Ruby adapter
     * @param gem Path to gem
     * @param jsonauthors Gem authors
     */
    private void getAuthors(final RubyRuntimeAdapter adapter, final Path gem,
        final JsonArrayBuilder jsonauthors) {
        final RubyObject authors = (RubyObject) adapter.eval(
            this.ruby, String.format(
                "Gem::Package.new('%s').spec.authors", gem
            )
        );
        int vari = 0;
        while (vari < authors.convertToArray().getLength()) {
            jsonauthors.add(authors.convertToArray().get(vari).toString());
            vari = vari + 1;
        }
    }
}
