package com.campus.preview;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Component;

import com.campus.dto.ResourceZipPreviewResponse;

@Component
public class DefaultZipPreviewGenerator implements ZipPreviewGenerator {

    @Override
    public ResourceZipPreviewResponse generate(Long resourceId, String fileName, InputStream zipInputStream)
            throws IOException {
        Map<String, ResourceZipPreviewResponse.Entry> orderedEntries = new TreeMap<>();
        try (ZipInputStream stream = new ZipInputStream(zipInputStream, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                String normalizedPath = normalizePath(entry.getName(), entry.isDirectory());
                if (normalizedPath == null) {
                    stream.closeEntry();
                    continue;
                }
                addImplicitParentDirectories(orderedEntries, normalizedPath);
                if (entry.isDirectory()) {
                    putDirectory(orderedEntries, normalizedPath);
                } else {
                    Long size = entry.getSize() >= 0 ? entry.getSize() : null;
                    putFile(orderedEntries, normalizedPath, size);
                }
                stream.closeEntry();
            }
        }
        List<ResourceZipPreviewResponse.Entry> entries = new ArrayList<>(orderedEntries.values());
        return new ResourceZipPreviewResponse(resourceId, fileName, entries.size(), entries);
    }

    private String normalizePath(String rawPath, boolean directory) {
        if (rawPath == null) {
            return null;
        }
        String candidate = rawPath.replace('\\', '/').trim();
        if (candidate.isEmpty()) {
            return null;
        }
        while (candidate.startsWith("/")) {
            candidate = candidate.substring(1);
        }
        while (candidate.endsWith("/") && !candidate.isEmpty()) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        if (candidate.isEmpty()) {
            return null;
        }

        String[] segments = candidate.split("/");
        List<String> normalizedSegments = new ArrayList<>(segments.length);
        for (String segment : segments) {
            if (segment == null || segment.isBlank() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                return null;
            }
            normalizedSegments.add(segment);
        }
        if (normalizedSegments.isEmpty()) {
            return null;
        }

        String normalized = String.join("/", normalizedSegments);
        return directory ? normalized + "/" : normalized;
    }

    private void addImplicitParentDirectories(Map<String, ResourceZipPreviewResponse.Entry> orderedEntries, String path) {
        String candidate = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int index = candidate.indexOf('/');
        while (index > 0) {
            String parentPath = candidate.substring(0, index + 1);
            putDirectory(orderedEntries, parentPath);
            index = candidate.indexOf('/', index + 1);
        }
    }

    private void putDirectory(Map<String, ResourceZipPreviewResponse.Entry> orderedEntries, String path) {
        orderedEntries.put(path, new ResourceZipPreviewResponse.Entry(path, nameOf(path, true), true, null));
    }

    private void putFile(Map<String, ResourceZipPreviewResponse.Entry> orderedEntries, String path, Long size) {
        orderedEntries.put(path, new ResourceZipPreviewResponse.Entry(path, nameOf(path, false), false, size));
    }

    private String nameOf(String path, boolean directory) {
        String value = directory && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int lastSlash = value.lastIndexOf('/');
        if (lastSlash < 0) {
            return value;
        }
        return value.substring(lastSlash + 1);
    }
}
