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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.apache.commons.io.IOUtils;
import org.jruby.javasupport.JavaEmbedUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * An SDK, which servers gem packages.
 * <p>
 * Initialize on first request.
 * Currently, Ruby runtime initialization and Slice evaluation is happening during the GemSlice
 * construction. Instead, the Ruby runtime initialization and Slice evaluation should happen
 * on first request.
 *
 * @since 0.1
 */
class RubyIndexerTest {

    @Test
    public void testUpdater(final @TempDir Path tmp) throws Exception {
        final Path path = Paths.get(tmp.toFile().getAbsolutePath(), "/gems");
        Files.createDirectories(path);
        final Path target = path.resolve("builder-3.2.4.gem");
        try (InputStream is = this.getClass().getResourceAsStream("/builder-3.2.4.gem");
            OutputStream os = Files.newOutputStream(target)) {
            IOUtils.copy(is, os);
        }
        final RubyIndexer rubyindexer = new RubyIndexer(
            JavaEmbedUtils.initialize(new ArrayList<>(0))
        );
        rubyindexer.index(tmp.toString());
    }
}
