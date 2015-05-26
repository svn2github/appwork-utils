/**
 * Copyright (c) 2009 - 2015 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.utils.io
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author daniel
 *
 */
public class J7FileList {
    public static List<File> findFiles(final Pattern pattern, final File directory, final boolean filesOnly) throws IOException {
        return findFiles(pattern, directory, filesOnly, true);
    }

    public static List<File> findFiles(final Pattern pattern, final File directory, final boolean filesOnly, final boolean patternOnFileNameOnly) throws IOException {
        final ArrayList<File> ret = new ArrayList<File>();
        if (directory != null && directory.exists()) {
            DirectoryStream<Path> stream = null;
            try {
                final Path directoryPath = directory.toPath();
                final FileSystem fs = directoryPath.getFileSystem();
                if (pattern != null) {
                    final PathMatcher matcher = fs.getPathMatcher("regex:" + pattern.pattern());
                    final DirectoryStream.Filter<Path> filter;
                    if (patternOnFileNameOnly) {
                        filter = new DirectoryStream.Filter<Path>() {
                            @Override
                            public boolean accept(Path entry) {
                                return matcher.matches(entry.getFileName());
                            }
                        };
                    } else {
                        filter = new DirectoryStream.Filter<Path>() {
                            @Override
                            public boolean accept(Path entry) {
                                return matcher.matches(entry.toAbsolutePath());
                            }
                        };
                    }
                    stream = fs.provider().newDirectoryStream(directoryPath, filter);
                } else {
                    final DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
                        @Override
                        public boolean accept(Path entry) {
                            return true;
                        }
                    };
                    stream = fs.provider().newDirectoryStream(directoryPath, filter);
                }
                for (final Path path : stream) {
                    final BasicFileAttributes pathAttr = Files.readAttributes(path, BasicFileAttributes.class);
                    if (filesOnly == false || pathAttr.isRegularFile()) {
                        ret.add(path.toFile());
                    }
                }
            } catch (final Throwable e) {
                throw new IOException(e);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Throwable e) {
                    }
                }
            }
        }
        return ret;
    }
}
