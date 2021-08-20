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

import java.nio.file.Path;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Gem metadata parser.
 * @since 1.0
 */
@SuppressWarnings("PMD.ProhibitPublicStaticMethods")
public interface GemMeta {

    /**
     * Json Gem info format.
     */
    InfoFormat<JsonObject> FMT_JSON = data -> buildTree(data).build();

    /**
     * Json Gem info format.
     * @param data Is data
     * @return Json Object
     */
    static JsonObjectBuilder buildTree(TreeNode<ImmutablePair<String, String>> data) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (final TreeNode<ImmutablePair<String, String>> node : data) {
            if (node.isRoot()) {
                continue;
            } else if (node.isLeaf()) {
                builder.add(node.getdata().getLeft(), node.getdata().getRight());
            } else {
                builder.add(node.getdata().getLeft(), buildTree(node).build());
            }
        }
        return builder;
    }

    /**
     * Extract Gem info.
     * @param gem Path to gem
     * @param format Info format
     * @param <T> Format type
     * @return JSON object
     */
    <T> T info(Path gem, InfoFormat<T> format);

    /**
     * Gem info format.
     * @param <T> Format type
     * @since 1.0
     */
    @FunctionalInterface
    interface InfoFormat<T> {

        /**
         * Print Gem info representation to required format.
         * @param data Gem info data
         * @return Formatted info
         */
        T print(TreeNode<ImmutablePair<String, String>> data);
    }
}
