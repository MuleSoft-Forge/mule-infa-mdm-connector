/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.operation.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Shared utility methods for B360 connector operations (encoding, stream reading).
 */
public final class B360Utils {

    private B360Utils() {}

    /** Shared Jackson ObjectMapper for JSON parsing; use instead of per-class instances. */
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Reads the given input stream fully and returns its contents as a UTF-8 string.
     * Returns an empty string if the stream is null or on error.
     */
    public static String readStreamAsString(InputStream in) {
        if (in == null) return "";
        try {
            byte[] buf = new byte[4096];
            int n;
            StringBuilder sb = new StringBuilder();
            while ((n = in.read(buf)) > 0) {
                sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Reads all bytes from the given input stream. Returns an empty array if the stream is null or on error.
     */
    public static byte[] readFully(InputStream in) {
        if (in == null) return new byte[0];
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    /**
     * Encodes a string for use as a path segment (UTF-8, spaces as %20).
     */
    public static String encodePathSegment(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
                    .replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
