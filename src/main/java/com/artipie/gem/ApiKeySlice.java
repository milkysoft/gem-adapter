package com.artipie.gem;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.auth.Identities;
import com.artipie.http.auth.Permissions;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import com.artipie.http.headers.Authorization;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.google.common.base.Charsets;
import org.reactivestreams.Publisher;

/**
 * Responses on api key requests.
 */
public class ApiKeySlice implements Slice {

    /**
     * Basic authentication prefix.
     */
    private static final String PREFIX = "Basic ";

    /**
     * The perms.
     */
    private Permissions perms;

    /**
     * The users.
     */
    private Identities users;

    /**
     * The Ctor.
     * @param perms The perms.
     * @param users The users.
     */
    public ApiKeySlice(final Permissions perms, final Identities users) {
        this.perms = perms;
        this.users = users;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Response response;
        final Optional<String> user = this.users.user(line, headers);
        if (user.isPresent()) {
            final String key = new RqHeaders(headers, Authorization.NAME).stream()
                .findFirst()
                .filter(hdr -> hdr.startsWith(ApiKeySlice.PREFIX))
                .map(hdr -> hdr.substring(ApiKeySlice.PREFIX.length()))
                .get();
            response = new RsWithBody(key, Charsets.UTF_8);
        } else {
            response = new RsWithStatus(RsStatus.FORBIDDEN);
        }
        return response;
    }
}
