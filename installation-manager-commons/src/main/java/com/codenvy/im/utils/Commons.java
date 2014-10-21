/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.im.utils;

import com.codenvy.dto.server.DtoFactory;
import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.exceptions.AuthenticationException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;

import static com.codenvy.im.utils.Version.compare;
import static com.codenvy.im.utils.Version.valueOf;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newInputStream;

/**
 * @author Anatoliy Bazko
 */
public class Commons {

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

    /** Simplifies the way to combine paths. Takes care about normalization. */
    public static String combinePaths(String apiEndpoint, String path) {
        if (apiEndpoint.endsWith("/")) {
            if (path.startsWith("/")) {
                return apiEndpoint + path.substring(1);
            } else {
                return apiEndpoint + path;
            }
        } else {
            if (path.startsWith("/")) {
                return apiEndpoint + path;
            } else {
                return apiEndpoint + "/" + path;
            }
        }
    }

    /** Adds query parameter to url. */
    public static String addQueryParam(String path, String key, String value) {
        return path + (path.contains("?") ? "&" : "?") + key + "=" + value;
    }

    /** Translates JSON to the list of DTO objects. */
    public static <DTO> List<DTO> createListDtoFromJson(String json, Class<DTO> dtoInterface) throws IOException {
        return DtoFactory.getInstance().createListDtoFromJson(json, dtoInterface);
    }

    /** Translates JSON to the list of DTO objects. */
    public static <DTO> DTO createDtoFromJson(String json, Class<DTO> dtoInterface) throws IOException {
        return DtoFactory.getInstance().createDtoFromJson(json, dtoInterface);
    }

    /** Translates JSON to object. */
    public static <T> T fromJson(String json, Class<T> t) {
        return gson.fromJson(json, t);
    }

    /** @return the version of the artifact out of path */
    public static String extractVersion(Path pathToBinaries) {
        return pathToBinaries.getParent().getFileName().toString();
    }

    /** @return the artifact name out of path */
    public static String extractArtifactName(Path pathToBinaries) {
        return pathToBinaries.getParent().getParent().getFileName().toString();
    }

    /**
     * @return the last version of the artifact in the directory
     * @throws com.codenvy.im.exceptions.ArtifactNotFoundException
     *         if artifact is absent in the repository
     * @throws java.io.IOException
     *         if an I/O error occurs
     */
    public static String getLatestVersion(String artifact, Path dir) throws IOException {
        Version latestVersion = null;

        if (!exists(dir)) {
            throw new ArtifactNotFoundException(artifact);
        }

        Iterator<Path> pathIterator = Files.newDirectoryStream(dir).iterator();
        while (pathIterator.hasNext()) {
            try {
                Path next = pathIterator.next();
                if (Files.isDirectory(next)) {
                    Version version = valueOf(next.getFileName().toString());
                    if (latestVersion == null || compare(version, latestVersion) > 0) {
                        latestVersion = version;
                    }
                }
            } catch (IllegalArgumentException e) {
                // maybe it isn't a version directory
            }
        }

        if (latestVersion == null) {
            throw new ArtifactNotFoundException(artifact);
        }

        return latestVersion.toString();
    }

    /**
     * Convert one-line json string to pretty formatted multiline string.
     *
     * @throws JSONException
     */
    public static String getPrettyPrintingJson(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        return obj.toString(2);
    }

    /** Extract server url from url with path */
    public static String extractServerUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            return url.getProtocol() + "://" + url.getHost();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** Returns correct exception depending on initial type of exception. */
    public static IOException getProperException(IOException e, Artifact artifact) {
        if (e instanceof HttpException) {
            switch (((HttpException)e).getStatus()) {
                case 404:
                    return new ArtifactNotFoundException(artifact.getName());

                case 302:
                    return new AuthenticationException();
            }
        }

        return e;
    }

    public static IOException getProperException(IOException e) {
        if (e instanceof HttpException) {
            switch (((HttpException)e).getStatus()) {
                case 403:
                    return new AuthenticationException();
            }
        }

        return e;
    }

    /** Calculates md5 sum */
    public static String calculateMD5Sum(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            try (InputStream in = newInputStream(file)) {
                int read;
                byte[] buffer = new byte[8192];
                while ((read = in.read(buffer)) != -1) {
                    md.update(buffer, 0, read);
                }
            }

            byte[] digest = md.digest();

            // convert the bytes to hex format
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                sb.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
