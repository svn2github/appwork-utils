/**
 *
 * ====================================================================================================================================================
 *         "AppWork Utilities" License
 *         The "AppWork Utilities" will be called [The Product] from now on.
 * ====================================================================================================================================================
 *         Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 *         Schwabacher Straße 117
 *         90763 Fürth
 *         Germany
 * === Preamble ===
 *     This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 *     The intent is that the AppWork GmbH is able to provide  their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 *
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 *
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact as.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: e-mail@appwork.org
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 *
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.appwork.utils.os.CrossSystem;

public class Files17 {
    public static File guessRoot(File file) throws IOException {
        return guessRoot(file, false);
    }

    public static File guessRoot(File file, boolean throwException) throws IOException {
        if (JVMVersion.get() >= JVMVersion.JAVA17 && JVMVersion.get() < JVMVersion.JAVA19) {
            FileStore fileFileStore = null;
            File existingFile = file;
            while (existingFile != null) {
                if (existingFile.exists()) {
                    try {
                        fileFileStore = Files.getFileStore(existingFile.toPath());
                        break;
                    } catch (InvalidPathException e) {
                        // wrong locale, java.nio.file.InvalidPathException: Malformed input or input contains unmappable characters
                        if (throwException) {
                            throw e;
                        } else {
                            break;
                        }
                    } catch (IOException e) {
                        // https://bugs.openjdk.java.net/browse/JDK-8165852
                        // https://bugs.openjdk.java.net/browse/JDK-8166162
                        if (throwException) {
                            throw e;
                        } else {
                            if (!StringUtils.containsIgnoreCase(e.getMessage(), "mount point not found")) {
                                throw e;
                            } else {
                                break;
                            }
                        }
                    }
                }
                existingFile = existingFile.getParentFile();
            }
            if (fileFileStore != null) {
                for (final FileStore fileStore : FileSystems.getDefault().getFileStores()) {
                    if (fileStore.equals(fileFileStore)) {
                        final Path fileStorePath = getPath(fileStore);
                        if (fileStorePath != null) {
                            return fileStorePath.toFile();
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Stores the known hacks.
     */
    // from http://stackoverflow.com/questions/10678363/find-the-directory-for-a-filestore, 25.05.2016
    private static final Map<Class<? extends FileStore>, Hacks> hacksMap = new HashMap<Class<? extends FileStore>, Hacks>();
    static {
        // TODO: JDK9
        if (JVMVersion.get() >= JVMVersion.JAVA17 && JVMVersion.get() < JVMVersion.JAVA19) {
            try {
                if (CrossSystem.isWindows()) {
                    try {
                        final Class<? extends FileStore> fileStoreClass = Class.forName("sun.nio.fs.WindowsFileStore").asSubclass(FileStore.class);
                        hacksMap.put(fileStoreClass, new WindowsFileStoreHacks(fileStoreClass));
                    } catch (ClassNotFoundException e) {
                        // Probably not running on Windows.
                    }
                } else {
                    try {
                        final Class<? extends FileStore> fileStoreClass = Class.forName("sun.nio.fs.UnixFileStore").asSubclass(FileStore.class);
                        hacksMap.put(fileStoreClass, new UnixFileStoreHacks(fileStoreClass));
                    } catch (ClassNotFoundException e) {
                        // Probably not running on UNIX.
                    }
                    try {
                        final Class<? extends FileStore> fileStoreClass = Class.forName("sun.nio.fs.LinuxFileStore").asSubclass(FileStore.class);
                        hacksMap.put(fileStoreClass, new UnixFileStoreHacks(fileStoreClass));
                    } catch (ClassNotFoundException e) {
                        // Probably not running on LINUX.
                    }
                    try {
                        final Class<? extends FileStore> fileStoreClass = Class.forName("sun.nio.fs.SolarisFileStore").asSubclass(FileStore.class);
                        hacksMap.put(fileStoreClass, new UnixFileStoreHacks(fileStoreClass));
                    } catch (ClassNotFoundException e) {
                        // Probably not running on SOLARIS.
                    }
                    try {
                        final Class<? extends FileStore> fileStoreClass = Class.forName("sun.nio.fs.BsdFileStore").asSubclass(FileStore.class);
                        hacksMap.put(fileStoreClass, new UnixFileStoreHacks(fileStoreClass));
                    } catch (ClassNotFoundException e) {
                        // Probably not running on BSD.
                    }
                }
            } catch (final Throwable e) {
            }
        }
    }

    /**
     * Gets the path from a file store. For some reason, NIO2 only has a method to go in the other direction.
     *
     * @param store
     *            the file store.
     * @return the path.
     */
    public static Path getPath(FileStore store) {
        final Hacks hacks = hacksMap.get(store.getClass());
        if (hacks == null) {
            return null;
        } else {
            return hacks.getPath(store);
        }
    }

    private static interface Hacks {
        Path getPath(FileStore store);
    }

    private static class WindowsFileStoreHacks implements Hacks {
        private final Field field;

        public WindowsFileStoreHacks(Class<?> fileStoreClass) {
            try {
                field = fileStoreClass.getDeclaredField("root");
                field.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException("file field not found", e);
            }
        }

        @Override
        public Path getPath(FileStore store) {
            try {
                String root = (String) field.get(store);
                return FileSystems.getDefault().getPath(root);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Denied access", e);
            }
        }
    }

    private static class UnixFileStoreHacks implements Hacks {
        private final Field field;

        private UnixFileStoreHacks(Class<?> fileStoreClass) {
            Field field = null;
            try {
                field = fileStoreClass.getDeclaredField("file");
                field.setAccessible(true);
            } catch (NoSuchFieldException e) {
                try {
                    field = fileStoreClass.getSuperclass().getDeclaredField("file");
                    field.setAccessible(true);
                } catch (NoSuchFieldException e2) {
                    throw new IllegalStateException("file field not found", e2);
                }
            }
            this.field = field;
        }

        @Override
        public Path getPath(FileStore store) {
            try {
                return (Path) field.get(store);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Denied access", e);
            }
        }
    }

    public static long getUsableDiskspace(final File path) throws IOException {
        return getUsableDiskspace(path, false);
    }

    public static boolean deleteIfExists(final File file) throws IOException {
        final Path path = file.toPath();
        try {
            return java.nio.file.Files.deleteIfExists(path);
        } catch (final IOException e) {
            if (file.setWritable(true)) {
                return java.nio.file.Files.deleteIfExists(path);
            } else {
                throw e;
            }
        }
    }

    /**
     * @param path
     * @return
     * @throws IOException
     */
    public static long getUsableDiskspace(final File path, boolean throwException) throws IOException {
        File existingFile = path;
        while (existingFile != null) {
            if (existingFile.exists()) {
                try {
                    final FileStore fileFileStore = Files.getFileStore(existingFile.toPath());
                    if (fileFileStore != null) {
                        return fileFileStore.getUsableSpace();
                    }
                    break;
                } catch (InvalidPathException e) {
                    // wrong locale, java.nio.file.InvalidPathException: Malformed input or input contains unmappable characters
                    if (throwException) {
                        throw e;
                    } else {
                        break;
                    }
                } catch (IOException e) {
                    // https://bugs.openjdk.java.net/browse/JDK-8165852
                    // https://bugs.openjdk.java.net/browse/JDK-8166162
                    if (throwException) {
                        throw e;
                    } else {
                        if (!StringUtils.containsIgnoreCase(e.getMessage(), "mount point not found")) {
                            throw e;
                        } else {
                            break;
                        }
                    }
                }
            }
            existingFile = existingFile.getParentFile();
        }
        final File root = org.appwork.utils.Files.guessRoot(path);
        if (root != null) {
            return root.getFreeSpace();
        } else {
            return path.getFreeSpace();
        }
    }
}
