package com.artipie.gem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.jruby.Ruby;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyIndexer implements GemIndexer {

    private final Ruby runtime;

    public RubyIndexer(final Ruby runtime) {
        this.runtime = runtime;
    }

    @Override
    public void index(final String repo) {
        target(this.runtime).index(repo);
    }

    /**
     * Endpoint path pattern.
     *
     * @param runtime Is good
     * @return GemINdexer Is good
     */
    private static GemIndexer target(final Ruby runtime) {
        try {
            final  RubyRuntimeAdapter evaler = JavaEmbedUtils.newRuntimeAdapter();
            final String script = IOUtils.toString(
                Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("/AstoUpdater.rb"),
                StandardCharsets.UTF_8
            );
            evaler.eval(runtime, script);
            final IRubyObject ruby = evaler.eval(runtime, "AstoUpdater");
            return  (GemIndexer) JavaEmbedUtils.invokeMethod(
                runtime, ruby,
                "new",
                null,
                GemIndexer.class
            );
        } catch (final IOException exc) {
            throw new UncheckedIOException(exc);
        }
    }
}
