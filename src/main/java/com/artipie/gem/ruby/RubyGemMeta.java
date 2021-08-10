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

import com.artipie.ArtipieException;
import com.artipie.gem.GemMeta;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
 *
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
     * Ruby adapter.
     */
    private RubyRuntimeAdapter adapter;

    /**
     * Gem variables.
     */
    private List<Variable<Object>> vars;

    /**
     * Ctor.
     *
     * @param ruby Runtime
     */
    public RubyGemMeta(final Ruby ruby) {
        this.ruby = ruby;
    }

    @Override
    public <T> T info(final Path gem, final GemMeta.InfoFormat<T> fmt) {
        this.adapter = JavaEmbedUtils.newRuntimeAdapter();
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        this.adapter.eval(this.ruby, "require 'rubygems/package.rb'");
        final RubyObject spec = (RubyObject) this.adapter.eval(
            this.ruby, String.format(
                "Gem::Package.new('%s').spec", gem.toString()
            )
        );
        this.vars = spec.getVariableList();
        for (final Variable<Object> thevar : this.vars) {
            this.addVar(builder, thevar, gem);
        }
        builder.add("sha", RubyGemMeta.getSha(gem));
        return fmt.print(builder.build());
    }

    /**
     * Add variable.
     *
     * @param builder Json Builder
     * @param thevar Variable to add
     * @param gem Path to gem
     */
    private void addVar(final JsonObjectBuilder builder,
        final Variable<Object> thevar, final Path gem) {
        final String plstr = "@platform";
        final String hstr = "@homepage";
        final String vstr = "@version";
        final String authstr = "authors";
        final String licstr = "licenses";
        if (RubyGemMeta.ATNAME.equals(thevar.getName())) {
            builder.add(
                RubyGemMeta.SNAME, RubyGemMeta.getVar(this.vars, RubyGemMeta.ATNAME)
            );
        } else if ("@authors".equals(thevar.getName())) {
            final JsonArrayBuilder jsonauthors = Json.createArrayBuilder();
            this.getArray(gem, jsonauthors, authstr);
            builder.add(authstr, jsonauthors);
        } else if ("@licenses".equals(thevar.getName())) {
            final JsonArrayBuilder jsonlicenses = Json.createArrayBuilder();
            this.getArray(gem, jsonlicenses, licstr);
            builder.add(licstr, jsonlicenses);
        } else if (hstr.equals(thevar.getName())) {
            builder.add("homepage_uri", RubyGemMeta.getVar(this.vars, hstr));
        } else if (plstr.equals(thevar.getName())) {
            builder.add("platform", RubyGemMeta.getVar(this.vars, plstr));
        } else if (vstr.equals(thevar.getName())) {
            builder.add("version", RubyGemMeta.getVar(this.vars, vstr));
        } else if ("@dependencies".equals(thevar.getName())) {
            final JsonObjectBuilder jsondep = Json.createObjectBuilder();
            this.getDependencies(gem, jsondep);
            builder.add("dependencies", jsondep);
        }
    }

    /**
     * Ruby runtime.
     *
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
     *
     * @param gem Path to gem
     * @param jsondep Dependencies
     */
    private void getDependencies(final Path gem, final JsonObjectBuilder jsondep) {
        final String rtstr = "runtime";
        final String dstr = "development";
        final JsonArrayBuilder devdeps = Json.createArrayBuilder();
        final JsonArrayBuilder runtimedeps = Json.createArrayBuilder();
        final RubyObject deps = (RubyObject) this.adapter.eval(
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
     *
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
     *
     * @param gem Path to gem
     * @param jsonarray Strings array
     * @param property Property to parse
     */
    private void getArray(final Path gem,
        final JsonArrayBuilder jsonarray, final String property) {
        final RubyObject elements = (RubyObject) this.adapter.eval(
            this.ruby, String.format(
                "Gem::Package.new('%s').spec.".concat(property), gem
            )
        );
        int vari = 0;
        while (vari < elements.convertToArray().getLength()) {
            jsonarray.add(elements.convertToArray().get(vari).toString());
            vari = vari + 1;
        }
    }

    /**
     * Calculate File checksum.
     *
     * @param gem Path to gem
     * @return String checksum
     */
    private static String getSha(final Path gem) {
        String res = "";
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            res = getFileChecksum(digest, gem.toFile());
        } catch (final IOException | NoSuchAlgorithmException err) {
            throw new ArtipieException(err);
        }
        return res;
    }

    /**
     * Calculate File checksum.
     *
     * @param digest Digest
     * @param file File to calculate
     * @return String checksum
     * @throws IOException exception
     */
    private static String getFileChecksum(final MessageDigest digest, final File file)
        throws IOException {
        final InputStream fis = Files.newInputStream(Paths.get(file.getAbsolutePath()));
        final byte[] bytearray = new byte[(int) Files.size(file.toPath())];
        final int sixteen = 16;
        final int hundred = 0x100;
        final int twofifty = 0xff;
        int bytescount = fis.read(bytearray);
        while (bytescount != -1) {
            digest.update(bytearray, 0, bytescount);
            bytescount = fis.read(bytearray);
        }
        fis.close();
        final byte[] bytes = digest.digest();
        final StringBuilder sbld = new StringBuilder();
        int icnt = 0;
        while (icnt < bytes.length) {
            sbld.append(Integer.toString((bytes[icnt] & twofifty) + hundred, sixteen).substring(1));
            icnt = icnt + 1;
        }
        return sbld.toString();
    }
}
