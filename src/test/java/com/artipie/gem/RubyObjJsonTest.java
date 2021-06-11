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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jruby.javasupport.JavaEmbedUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * A test for extract JSON info from gem .
 *
 * @since 1.0
 */
public class RubyObjJsonTest {

    @Test
    public void createJsonByPath(@TempDir final Path tmp) throws IOException {
        final String builderstr = "gviz-0.3.5.gem";
        final String gemattr = "homepage";
        final String attrval = "https://github.com/melborne/Gviz";
        final Path target = tmp.resolve(builderstr);
        try (InputStream is = this.getClass().getResourceAsStream("/".concat(builderstr));
            OutputStream os = Files.newOutputStream(target)) {
            IOUtils.copy(is, os);
        }
//        MatcherAssert.assertThat(
//            new RubyObjJson(
//                JavaEmbedUtils.newRuntimeAdapter(),
//                JavaEmbedUtils.initialize(Collections.emptyList())
//            ).createJson(Paths.get(tmp.toString(), builderstr)),
//            Matchers.allOf(
//                new JsonHas(
//                    gemattr,
//                    new JsonValueIs(attrval)
//                )
//            )
//        );
    }
}
