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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link Gem} SDK.
 *
 * @since 1.0
 */
public class GemTest {

    @Test
    public void updateRepoIndex() {
        final Storage repo = new InMemoryStorage();
        final Key target = new Key.From("gems", UUID.randomUUID().toString());
        new TestResource("builder-3.2.4.gem").saveTo(repo, target);
        final Gem gem = new Gem(repo);
        gem.update(target).toCompletableFuture().join();
        MatcherAssert.assertThat(
            new BlockingStorage(repo).list(Key.ROOT)
                .stream().map(Key::string)
                .collect(Collectors.toSet()),
            Matchers.hasItems(
                "prerelease_specs.4.8",
                "prerelease_specs.4.8.gz",
                "specs.4.8",
                "specs.4.8.gz",
                "latest_specs.4.8",
                "latest_specs.4.8.gz",
                "quick/Marshal.4.8/builder-3.2.4.gemspec.rz",
                "gems/builder-3.2.4.gem"
            )
        );
    }
}
