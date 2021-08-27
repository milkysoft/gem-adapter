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
package com.artipie.gem.http;

import com.artipie.asto.Storage;
import com.artipie.gem.Gem;
import com.artipie.gem.TreeNode;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.common.RsJson;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.reactivestreams.Publisher;

/**
 * Returns some basic information about the given gem.
 * <p>
 * Handle {@code GET - /api/v1/gems/[GEM NAME].(json|yaml)}
 * requests, see
 * <a href="https://guides.rubygems.org/rubygems-org-api">RubyGems API</a>
 * for documentation.
 * </p>
 *
 * @since 0.2
 */
public final class ApiGetSlice implements Slice {

    /**
     * Endpoint path pattern.
     */
    public static final Pattern PATH_PATTERN = Pattern.compile("/api/v1/gems/(.+).(json|yml)");

    /**
     * Gem SDK.
     */
    private final Gem sdk;

    /**
     * New slice for handling Get API requests.
     * @param storage Gems storage
     */
    public ApiGetSlice(final Storage storage) {
        this.sdk = new Gem(storage);
    }

    @Override
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Matcher matcher = PATH_PATTERN.matcher(new RequestLineFrom(line).uri().toString());
        if (!matcher.find()) {
            throw new IllegalStateException("Invalid routing schema");
        }
        return new AsyncResponse(
            this.sdk.info(matcher.group(1), data -> ApiGetSlice.buildTree(data).build())
                .thenApply(json -> new RsJson(json))
        );
    }

    /**
     * Json Gem info format.
     * @param data Is data
     * @return Json Object
     */
    private static JsonObjectBuilder buildTree(final TreeNode<ImmutablePair<String, String>> data) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (final TreeNode<ImmutablePair<String, String>> node : data.getchildren()) {
            if (node.isRoot()) {
                continue;
            } else if (node.isLeaf()) {
                if (node.getdata().getRight().contains("|")) {
                    final String[] elements = Arrays.stream(
                        node.getdata().getRight().split("[|]")
                    ).filter(res -> !res.trim().isEmpty()).toArray(String[]::new);
                    final JsonArrayBuilder jsonarray = Json.createArrayBuilder();
                    for (final String element : elements) {
                        jsonarray.add(element);
                    }
                    builder.add(node.getdata().getLeft(), jsonarray);
                } else {
                    builder.add(node.getdata().getLeft(), node.getdata().getRight());
                }
            } else {
                builder.add(node.getdata().getLeft(), buildTree(node).build());
            }
        }
        return builder;
    }
}
