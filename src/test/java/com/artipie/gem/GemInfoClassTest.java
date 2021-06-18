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

import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.json.JsonObject;
import org.apache.commons.io.IOUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * A test for gem submit operation.
 *
 * @since 0.7
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public class GemInfoClassTest {
    /**
     * Path to upload gem file.
     */
    static final String BUILDER_STR = "gviz-0.3.5.gem";

    @Test
    public void queryResultsInOkResponse(@TempDir final Path tmp) throws IOException {
        final Path repo = Paths.get(tmp.toString());
        final Path target = repo.resolve(GemInfoClassTest.BUILDER_STR);
        try (InputStream is = this.getClass()
            .getResourceAsStream("/".concat(GemInfoClassTest.BUILDER_STR));
            OutputStream os = Files.newOutputStream(target)) {
            IOUtils.copy(is, os);
        }
        final Storage storage = new FileStorage(tmp);
        MatcherAssert.assertThat(
            new GemInfoClass(storage, new Gem(storage)),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasBody(
                        new IsJson(
                            new JsonHas(
                                "homepage",
                                new JsonValueIs("https://github.com/melborne/Gviz")
                            )
                        )
                    )
                ),
                new RequestLine(RqMethod.GET, "/api/v1/gems/gviz.json"),
                Headers.EMPTY,
                com.artipie.asto.Content.EMPTY
            )
        );
    }

    @Test
    public void queryResultsInOk(@TempDir final Path tmp) throws IOException {
        final Path repo = Paths.get(tmp.toString());
        final Path target = repo.resolve(GemInfoClassTest.BUILDER_STR);
        try (InputStream is = this.getClass()
            .getResourceAsStream("/".concat(GemInfoClassTest.BUILDER_STR));
            OutputStream os = Files.newOutputStream(target)) {
            IOUtils.copy(is, os);
        }
        final Storage storage = new FileStorage(tmp);
        final String ver = ">= 0";
        MatcherAssert.assertThat(
            new GemInfoClass(storage, new Gem(storage)),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasBody(
                        new IsJson(
                            new AllOf<>(
                                Arrays.asList(
                                    new JsonHas("name", new JsonValueIs("gviz-0.3.5")),
                                    new JsonHas(
                                        "dependencies",
                                        new JsonContains(
                                            new IsService(
                                                "rspec",
                                                "~> 2.1"
                                            ),
                                            new IsService(
                                                "bundler",
                                                "~> 1.5"
                                            ),
                                            new IsService(
                                                "rake",
                                                ver
                                            ),
                                            new IsService(
                                                "thor",
                                                ver
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                ),
                new RequestLine(RqMethod.GET, "/api/v1/dependencies?gems=gviz,thor"),
                Headers.EMPTY,
                com.artipie.asto.Content.EMPTY
            )
        );
    }

    /**
     * Matcher for JSON object representing service.
     *
     * @since 0.1
     */
    private class IsService extends BaseMatcher<JsonObject> {

        /**
         * Expected service type.
         */
        private final String type;

        /**
         * Expected service id.
         */
        private final String id;

        IsService(final String type, final String id) {
            this.type = type;
            this.id = id;
        }

        @Override
        public void describeTo(final Description description) {
            this.delegate().describeTo(description);
        }

        @Override
        public boolean matches(final Object item) {
            return this.delegate().matches(item);
        }

        private Matcher<JsonObject> delegate() {
            return new AllOf<>(
                Arrays.asList(
                    new JsonHas(this.type, new JsonValueIs(this.id))
                )
            );
        }
    }
}
