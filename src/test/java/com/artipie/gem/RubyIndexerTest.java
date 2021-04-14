package com.artipie.gem;

import com.artipie.asto.Key;
import com.artipie.asto.fs.FileStorage;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.io.IOUtils;
import org.jruby.javasupport.JavaEmbedUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class RubyIndexerTest {

    @Test
    public void testUpdater(@TempDir Path tmp) throws Exception{
        Path path = Paths.get(tmp.toFile().getAbsolutePath(), "/gems");
        Files.createDirectories(path);
        Path target = path.resolve("builder-3.2.4.gem");
        try (InputStream is = this.getClass().getResourceAsStream("/builder-3.2.4.gem");
             OutputStream os = Files.newOutputStream(target)) {
            IOUtils.copy(is, os);
        }
        RubyIndexer rubyIndexer = new RubyIndexer(JavaEmbedUtils.initialize(new ArrayList<>(0)));
        rubyIndexer.index(tmp.toString());
    }
}
