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
import com.artipie.gem.GemIndex;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.jruby.Ruby;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;

/**
 * Ruby runtime gem index implementation.
 *
 * @since 1.0
 */
public final class RubyGemIndex implements GemIndex {

    /**
     * Read only set of metadata item names.
     */
    private static final Set<String> META_NAMES = Collections.unmodifiableSet(
        Stream.of(
            "latest_specs.4.8", "latest_specs.4.8.gz",
            "specs.4.8", "specs.4.8.gz"
        ).collect(Collectors.toSet())
    );

    /**
     * Ruby runtime.
     */
    private final Ruby ruby;

    /**
     * New gem indexer.
     * @param ruby Runtime
     */
    public RubyGemIndex(final Ruby ruby) {
        this.ruby = ruby;
    }

    @Override
    public void update(final Path path) {
        final RubyRuntimeAdapter adapter = JavaEmbedUtils.newRuntimeAdapter();
        try {
            final String script = IOUtils.toString(
                RubyGemIndex.class.getResourceAsStream(String.format("/metarunner.rb")),
                StandardCharsets.UTF_8
            );
            adapter.eval(this.ruby, script);
            JavaEmbedUtils.invokeMethod(
                this.ruby,
                adapter.eval(this.ruby, "MetaRunner"),
                "new",
                new Object[]{path.toString()},
                Object.class
            );
            for (final String file : RubyGemIndex.META_NAMES) {
                final Path source = path.resolve(file);
                final Path target = path.getParent().getParent();
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (final IOException err) {
            throw new ArtipieException(err);
        }
    }
}
