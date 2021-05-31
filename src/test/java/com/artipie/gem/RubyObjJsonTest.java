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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * A test for extract JSON info from gem .
 *
 * @since 1.0
 */
public class RubyObjJsonTest {

    @Test
    public void createJsonIsOk() throws IOException {
        final RubyRuntimeAdapter runtime = JavaEmbedUtils.newRuntimeAdapter();
        final Ruby ruby = JavaEmbedUtils.initialize(Collections.emptyList());
        runtime.eval(ruby, "require 'rubygems/package.rb'");
        RubyObject gemobject = null;
        final Optional<String> filename = Files.walk(Paths.get("./")).map(Path::toString)
            .filter(file -> file.contains("gviz") && file.contains(".gem")).findFirst();
        final String script = "Gem::Package.new('"
            .concat(filename.get()).concat("').spec");
        gemobject = (RubyObject) runtime.eval(ruby, script);
        MatcherAssert.assertThat(
            new RubyObjJson(gemobject).createJson().build(),
            Matchers.allOf(
                new JsonHas(
                    "homepage",
                    new JsonValueIs("https://github.com/melborne/Gviz")
                )
            )
        );
    }
}

