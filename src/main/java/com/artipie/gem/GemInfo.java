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

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.jcabi.log.Logger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.Variable;
import org.json.JSONObject;
import org.reactivestreams.Publisher;

/**
 * Returns some basic information about the given gem. GET - /api/v1/gems/[GEM NAME].(json|yaml)
 * https://guides.rubygems.org/rubygems-org-api/
 *
 * @todo #32:120min Gem Information implementation.
 *  The implementation must be able to response with either json or yaml format. An example response
 *  can be obtained via {@code curl https://rubygems.org/api/v1/gems/rails.json}
 * @since 0.2
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
public final class GemInfo implements Slice {

    /**
     * Endpoint path pattern.
     */
    public static final Pattern PATH_PATTERN = Pattern.compile("/api/v1/gems/([\\w]+).(json|yml)");

    /**
     * Ruby runtime.
     */
    private final RubyRuntimeAdapter runtime;

    /**
     * Ruby interpreter.
     */
    private final Ruby ruby;

    /**
     * Ctor.
     */
    public GemInfo() {
        this.runtime = JavaEmbedUtils.newRuntimeAdapter();
        this.ruby = JavaEmbedUtils.initialize(Collections.emptyList());
    }

    @Override
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Matcher matcher = PATH_PATTERN.matcher(new RequestLineFrom(line).uri().toString());
        Response response = new RsWithStatus(RsStatus.NOT_IMPLEMENTED);
        this.runtime.eval(this.ruby, "require 'rubygems/commands/contents_command.rb'");
        if (matcher.find()) {
            final String gem = matcher.group(1);
            final String extension = matcher.group(2);
            Logger.info(
                GemInfo.class,
                "Gem info for '%s' has been requested. Extension: '%s'",
                gem,
                extension
            );
            final RubyObject gemobject = (RubyObject) this.runtime.eval(
                this.ruby,
                String.format("Gem::Commands::ContentsCommand.new.spec_for('%s')", gem)
            );
            final List<Variable<Object>> vars = gemobject.getVariableList();
            final JSONObject obj = new JSONObject();
            for (int ind = 0; ind < vars.size(); ind = ind + 1) {
                final Variable<Object> var = vars.get(ind);
                String name = var.getName();
                if (var.getName().substring(0, 1).equals("@")) {
                    name = var.getName().substring(1);
                }
                if (gemobject.getVariable(ind) != null) {
                    obj.put(name, gemobject.getVariable(ind).toString());
                }
            }
            if (extension.equals("json")) {
                response = new RsWithBody(
                    new RsWithStatus(RsStatus.OK),
                    obj.toString(), StandardCharsets.UTF_8
                );
            }
        } else {
            throw new IllegalStateException("Not expected path has been matched");
        }
        return response;
    }

}

