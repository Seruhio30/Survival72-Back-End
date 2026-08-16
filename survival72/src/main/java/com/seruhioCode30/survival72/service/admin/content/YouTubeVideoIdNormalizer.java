package com.seruhioCode30.survival72.service.admin.content;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

@Component
public class YouTubeVideoIdNormalizer {

    private static final Pattern VIDEO_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]{11}$");

    public String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("YouTube video is required.");
        }

        String candidate = value.trim();

        if (VIDEO_ID_PATTERN.matcher(candidate).matches()) {
            return candidate;
        }

        try {
            URI uri = new URI(candidate);
            String host = uri.getHost();

            if (host == null) {
                throw new IllegalArgumentException("Invalid YouTube video.");
            }

            host = host.toLowerCase();

            if (host.equals("youtu.be") || host.equals("www.youtu.be")) {
                return validateId(firstPathSegment(uri.getPath()));
            }

            if (host.equals("youtube.com") || host.equals("www.youtube.com")
                    || host.equals("m.youtube.com")) {
                if ("/watch".equals(uri.getPath())) {
                    return validateId(queryParameter(uri.getRawQuery(), "v"));
                }

                if (uri.getPath() != null
                        && (uri.getPath().startsWith("/shorts/")
                        || uri.getPath().startsWith("/embed/"))) {
                    String[] segments = uri.getPath().split("/");
                    if (segments.length >= 3) {
                        return validateId(segments[2]);
                    }
                }
            }
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid YouTube video.");
        }

        throw new IllegalArgumentException("Invalid YouTube video.");
    }

    private String firstPathSegment(String path) {
        if (path == null) {
            return null;
        }

        String normalized = path.startsWith("/") ? path.substring(1) : path;
        int slashIndex = normalized.indexOf('/');

        return slashIndex >= 0
                ? normalized.substring(0, slashIndex)
                : normalized;
    }

    private String queryParameter(String query, String name) {
        if (query == null) {
            return null;
        }

        for (String parameter : query.split("&")) {
            String[] parts = parameter.split("=", 2);
            if (parts.length == 2 && parts[0].equals(name)) {
                return parts[1];
            }
        }

        return null;
    }

    private String validateId(String candidate) {
        if (candidate != null && VIDEO_ID_PATTERN.matcher(candidate).matches()) {
            return candidate;
        }

        throw new IllegalArgumentException("Invalid YouTube video.");
    }
}
