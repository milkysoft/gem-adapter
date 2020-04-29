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

import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

/**
 * A test which ensures {@code gem} console tool compatibility with the adapter.
 *
 * @since 0.2
 * @checkstyle StringLiteralsConcatenationCheck (500 lines)
 */
@SuppressWarnings("PMD.SystemPrintln")
@DisabledIfSystemProperty(named = "os.name", matches = "Windows.*")
public class GemCliITCase {

    @Test
    public void gemPushWorks() throws IOException, InterruptedException {
        final Vertx vertx = Vertx.vertx();
        final VertxSliceServer server = new VertxSliceServer(
            vertx,
            new GemSlice(new InMemoryStorage(), vertx.fileSystem())
        );
        final int port = server.start();
        final String host = String.format("http://host.testcontainers.internal:%d", port);
        Testcontainers.exposeHostPorts(port);
        final RubyContainer ruby = new RubyContainer()
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind("./src/test/resources", "/home");
        ruby.start();
        final Container.ExecResult push = ruby.execInContainer(
            "/bin/bash",
            "-c",
            String.format("GEM_HOST_API_KEY=123 gem push builder-3.2.4.gem --host %s", host)
        );
        Logger.info(GemCliITCase.class, push.getStdout());
        Logger.error(GemCliITCase.class, push.getStderr());
        MatcherAssert.assertThat(
            String.format("'gem push builder-3.2.4.gem --host %s' failed with non-zero code", host),
            push.getExitCode(),
            Matchers.equalTo(0)
        );
        ruby.stop();
        server.close();
        vertx.close();
    }

    /**
     * Inner subclass to instantiate Ruby container.
     *
     * @since 0.1
     */
    private static class RubyContainer extends GenericContainer<RubyContainer> {
        RubyContainer() {
            super("ruby:2.7");
        }
    }
}
