package com.artipie.gem;

import java.nio.file.Path;

public interface GemsMerger {
    Path merge(String metadata, String specs);
}
