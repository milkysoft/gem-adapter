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
import com.artipie.asto.fs.FileStorage;
import com.artipie.http.rs.RsStatus;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A test for gem submit operation.
 *
 * @since 0.2
 */
public class SubmitGemITCase {

    @Test
    public void testGem(final @TempDir Path tmp) throws IOException {
        final Path repo = Paths.get(tmp.toString(), "Artipie");
        final String prefix = "builder";
        final Path path = repo.resolve(prefix).resolve("gems");
        try {
            Files.createDirectories(path);
        } catch (final IOException exc) {
            throw new UncheckedIOException(exc);
        }
        final Path target = path.resolve("builder-3.2.4.gem");
        try (InputStream is = this.getClass().getResourceAsStream("/builder-3.2.4.gem");
            OutputStream os = Files.newOutputStream(target)) {
            IOUtils.copy(is, os);
        }
        final Gem gem = new Gem(new FileStorage(repo));
        final CompletableFuture<Void> res =
            (CompletableFuture<Void>) gem.batchUpdate(new Key.From(prefix));
        res.join();
        final List<String> files = new ArrayList<>(0);
        try (Stream<Path> paths = Files.walk(repo)) {
            paths.filter(Files::isRegularFile)
                .forEach(filename -> files.add(filename.toString()));
            MatcherAssert.assertThat(
                files,
                Matchers.hasItem("".concat(repo.toString()).concat("/").concat(prefix)
                    .concat("/prerelease_specs.4.8.gz")
                )
            );
            MatcherAssert.assertThat(
                files,
                Matchers.hasItem("".concat(repo.toString()).concat("/").concat(prefix)
                    .concat("/quick/Marshal.4.8/builder-3.2.4.gemspec.rz")
                )
            );
            MatcherAssert.assertThat(
                files, Matchers.hasItem("".concat(repo.toString()).concat("/").concat(prefix)
                    .concat("/latest_specs.4.8")
                )
            );
            MatcherAssert.assertThat(
                files,
                Matchers.hasItem("".concat(repo.toString()).concat("/").concat(prefix)
                    .concat("/gems/builder-3.2.4.gem")
                )
            );
            MatcherAssert.assertThat(
                files, Matchers.hasItem(
                    "".concat(repo.toString()).concat("/").concat(prefix).concat("/specs.4.8.gz")
                )
            );
            MatcherAssert.assertThat(
                files, Matchers.hasItem("".concat(repo.toString()).concat("/").concat(prefix)
                    .concat("/latest_specs.4.8.gz")
                )
            );
        }
    }

    @Test
    public void submitResultsInOkResponse(@TempDir final Path temp) throws IOException {
        final Vertx vertx = Vertx.vertx();
        final VertxSliceServer server = new VertxSliceServer(
            vertx,
            new GemSlice(new FileStorage(temp))
        );
        final WebClient web = WebClient.create(vertx);
        final int port = server.start();
        final byte[] gem = Files.readAllBytes(
            Paths.get("./src/test/resources/builder-3.2.4.gem")
        );
        final int code = web.post(port, "localhost", "/api/v1/gems")
            .rxSendBuffer(Buffer.buffer(gem))
            .blockingGet()
            .statusCode();
        MatcherAssert.assertThat(
            code,
            new IsEqual<>(Integer.parseInt(RsStatus.OK.code()))
        );
        web.close();
        server.close();
        vertx.close();
    }
}
