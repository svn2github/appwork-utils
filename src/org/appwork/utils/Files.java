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
 *     The intent is that the AppWork GmbH is able to provide their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 *
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 *
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact us.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: <e-mail@appwork.org>
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 *
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.os.CrossSystem;

public class Files {
    public static abstract class AbstractHandler<T extends Exception> implements Handler<T> {
        public void intro(final File f) {
        }

        /**
         * @param f
         * @throws IOException
         */
        abstract public void onFile(File f) throws T;

        public void outro(final File f) {
        }
    }

    public static interface Handler<T extends Exception> {
        public void intro(File f) throws T;

        /**
         * @param f
         * @throws IOException
         */
        public void onFile(File f) throws T;

        public void outro(File f) throws T;
    }

    /**
     * @deprecated Use {@link #deleteRecursive(File)} instead delete all files/folders that are given We need the void method handler for
     *             some uninstaller routines
     * @param files
     * @return
     * @throws IOException
     */
    public static void deleteRecursiv(final File file) throws IOException {
        Files.deleteRecursive(file, true);
    }

    /**
     * @deprecated Use {@link #deleteRecursive(File, boolean)} instead We need the void method handler for some uninstaller routines
     * @param file
     * @param b
     * @throws IOException
     */
    public static void deleteRecursiv(final File file, final boolean breakOnError) throws IOException {
        deleteRecursive(file, breakOnError);
    }

    /**
     * delete all files/folders that are given
     *
     * @param files
     * @return
     * @throws IOException
     */
    public static int deleteRecursive(final File file) throws IOException {
        return Files.deleteRecursive(file, true);
    }

    /**
     * @param file
     * @param b
     * @throws IOException
     */
    public static int deleteRecursive(final File file, final boolean breakOnError) throws IOException {
        int ret = 0;
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        if (file.isDirectory()) {
            final File[] files = file.listFiles();
            if (files != null) {
                for (final File f : files) {
                    ret += Files.deleteRecursive(f, breakOnError);
                }
            }
        }
        LoggerFactory.getDefaultLogger().finer(" Delete file " + file);
        final boolean fd = file.delete();
        if (fd) {
            ret++;
        }
        if (file.exists() && !fd && breakOnError) {
            throw new IOException("Could not delete " + file);
        }
        return ret;
    }

    public static LinkedList<String> getDirectories_NonRecursive(final File startDirectory, final boolean includeStart) throws IOException {
        final LinkedList<String> done = new LinkedList<String>();
        File current = null;
        File[] currents = null;
        final java.util.List<File> todo = new ArrayList<File>();
        todo.add(startDirectory);
        while (todo.size() > 0) {
            current = todo.remove(0);
            currents = current.listFiles();
            done.add(current.getCanonicalPath());
            if (currents != null) {
                for (int index = currents.length - 1; index >= 0; index--) {
                    if (currents[index].isDirectory()) {
                        final String temp = currents[index].getCanonicalPath();
                        if (!done.contains(temp)) {
                            todo.add(currents[index]);
                        }
                    }
                }
            }
        }
        /* remove startdirectory if wished */
        if (!includeStart && done.size() > 0) {
            done.remove(0);
        }
        return done;
    }

    /**
     * returns File if it exists (case (In)Sensitive). returns null if file does not exist
     */
    public static File getExistingFile(final File file, final boolean caseSensitive) {
        if (file == null) {
            return null;
        }
        if (caseSensitive) {
            if (file.exists()) {
                return file;
            }
            return null;
        }
        /* get list of files in current directory */
        final String lowerCaseFileName = file.getName().toLowerCase();
        final File parent = file.getParentFile();
        if (parent != null) {
            final File[] list = parent.listFiles();
            if (list != null) {
                for (final File ret : list) {
                    if (ret.getName().equalsIgnoreCase(lowerCaseFileName)) {
                        return ret;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the fileextension(without dot) for a file with the given name
     *
     * @see #getFileNameWithoutExtension(String)
     * @param name
     * @return
     */
    public static String getExtension(final String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        final int index = name.lastIndexOf(".");
        if (index < 0 || index + 1 >= name.length()) {
            return null;
        }
        final String ret = name.substring(index + 1).toLowerCase(Locale.ENGLISH);
        if (ret.matches("^[a-zA-Z0-9\\.\\-\\_]+$")) {
            return ret;
        } else {
            return null;
        }
    }

    /**
     * @see #getExtension(String)
     * @param jar
     * @return
     */
    public static String getFileNameWithoutExtension(final String filename) {
        final int index = filename.lastIndexOf(".");
        if (index < 0) {
            return filename;
        }
        return filename.substring(0, index);
    }

    /**
     * return all files ( and folders if includeDirectories is true ) for the given files
     *
     * @param includeDirectories
     * @param files
     * @return
     */
    public static java.util.List<File> getFiles(final boolean includeDirectories, final boolean includeFiles, final File... files) {
        return Files.getFiles(new FileFilter() {
            @Override
            public boolean accept(final File pathname) {
                if (includeDirectories && pathname.isDirectory()) {
                    return true;
                }
                if (includeFiles && pathname.isFile()) {
                    return true;
                }
                return false;
            }
        }, files);
    }

    /**
     * @param b
     * @param c
     * @param filter
     * @param source
     * @return
     */
    public static java.util.List<File> getFiles(final FileFilter filter, final File... files) {
        final java.util.List<File> ret = new ArrayList<File>();
        if (files != null) {
            for (final File f : files) {
                if (!f.exists()) {
                    continue;
                }
                if (filter == null || filter.accept(f)) {
                    ret.add(f);
                }
                if (f.isDirectory()) {
                    ret.addAll(Files.getFiles(filter, f.listFiles()));
                }
            }
        }
        return ret;
    }

    /**
     * Returns the mikmetype of the file. If unknown, it returns Unknown/extension
     *
     * @param name
     * @return
     */
    public static String getMimeType(final String name) {
        if (name == null) {
            return null;
        }
        final FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String ret = fileNameMap.getContentTypeFor(name);
        if (ret == null) {
            ret = "unknown/" + Files.getExtension(name);
        }
        return ret;
    }

    /**
     * Returns the relative path of file based on root.
     *
     * @param root
     * @param f
     * @return
     */
    public static String getRelativePath(final File root, final File file) {
        return Files.getRelativePath(root.getAbsolutePath(), file.getAbsolutePath());
    }

    public static String getRelativePath(String root, final String file) {
        final String rootPath, filePath;
        if (CrossSystem.isWindows() || CrossSystem.isOS2()) {
            root = root.replace("/", "\\");
            if (!root.endsWith("\\")) {
                root += "\\";
            }
            rootPath = root.toLowerCase(Locale.ENGLISH);
            filePath = file.toLowerCase(Locale.ENGLISH).replace("/", "\\");
            if (rootPath.equals(filePath + "\\")) {
                return "";
            }
        } else {
            if (!root.endsWith("/")) {
                root += "/";
            }
            rootPath = root;
            filePath = file;
            if (rootPath.equals(filePath + "/")) {
                return "";
            }
        }
        if (!filePath.startsWith(rootPath)) {
            return null;
        }
        if (rootPath.equals(filePath)) {
            return "/";
        }
        if (CrossSystem.isWindows() || CrossSystem.isOS2()) {
            return file.substring(rootPath.length()).replace("\\", "/");
        } else {
            return file.substring(rootPath.length());
        }
    }

    public static <T extends Throwable> void internalWalkThroughStructure(final FileHandler<T> handler, final File file, final int depths) throws T {
        if (file.exists() && handler.onFile(file, depths)) {
            if (file.isDirectory()) {
                final File[] directoryFiles = file.listFiles();
                if (directoryFiles != null) {
                    for (final File directoryFile : directoryFiles) {
                        Files.internalWalkThroughStructure(handler, directoryFile, depths + 1);
                    }
                }
            }
        }
    }

    public static <T extends Exception> void internalWalkThroughStructureReverse(final Handler<T> handler, final File file) throws T {
        if (file.exists()) {
            if (file.isDirectory()) {
                final File[] directoryFiles = file.listFiles();
                if (directoryFiles != null) {
                    for (final File directoryFile : directoryFiles) {
                        Files.internalWalkThroughStructureReverse(handler, directoryFile);
                    }
                }
            }
            handler.onFile(file);
        }
    }

    public static <T extends Exception> void walkThroughStructure(final FileHandler<T> handler, final File file) throws T {
        handler.intro(file);
        try {
            Files.internalWalkThroughStructure(handler, file, 0);
        } finally {
            handler.outro(file);
        }
    }

    public static <T extends Exception> void walkThroughStructure(final Handler<T> handler, final File file) throws T {
        handler.intro(file);
        try {
            Files.internalWalkThroughStructure(new FileHandler<T>() {
                @Override
                public void intro(File f) throws T {
                    // handler.intro(f);
                }

                @Override
                public boolean onFile(File f, int depths) throws T {
                    handler.onFile(f);
                    return true;
                }

                @Override
                public void outro(File f) throws T {
                    // handler.outro(f);
                }
            }, file, 0);
        } finally {
            handler.outro(file);
        }
    }

    public static <T extends Exception> void walkThroughStructureReverse(final Handler<T> handler, final File file) throws T {
        handler.intro(file);
        try {
            Files.internalWalkThroughStructureReverse(handler, file);
        } finally {
            handler.outro(file);
        }
    }

    /**
     * @param old
     * @param newFile
     * @return
     */
    public static File getCommonParent(File a, File b) {
        List<File> filesA = new LinkedList<File>();
        List<File> filesB = new LinkedList<File>();
        Files.fill(filesA, a);
        Files.fill(filesB, b);
        File last = null;
        String nameA, nameB;
        for (int i = 0; i < Math.min(filesA.size(), filesB.size()); i++) {
            if (CrossSystem.isWindows()) {
                if (i == 0) {
                    nameA = filesA.get(i).getPath().toLowerCase(Locale.ENGLISH);
                    nameB = filesB.get(i).getPath().toLowerCase(Locale.ENGLISH);
                } else {
                    nameA = filesA.get(i).getName().toLowerCase(Locale.ENGLISH);
                    nameB = filesB.get(i).getName().toLowerCase(Locale.ENGLISH);
                }
            } else {
                if (i == 0) {
                    nameA = filesA.get(i).getPath();
                    nameB = filesB.get(i).getPath();
                } else {
                    nameA = filesA.get(i).getName();
                    nameB = filesB.get(i).getName();
                }
            }
            if (!nameA.equals(nameB)) {
                break;
            }
            last = filesA.get(i);
        }
        return last;
    }

    /**
     * @param filesA
     * @param a
     */
    private static void fill(List<File> filesA, File a) {
        HashSet<File> dupe = new HashSet<File>();
        while (a != null) {
            if (!dupe.add(a)) {
                return;
            }
            filesA.add(0, a);
            File aTmp = a.getParentFile();
            if (aTmp == null || aTmp.equals(a)) {
                break;
            }
            a = aTmp;
        }
    }

    public static File guessRoot(File file) throws IOException {
        return guessRoot(file, false);
    }

    /**
     * @param folder
     * @return
     * @throws IOException
     */
    public static File guessRoot(File file, boolean throwException) throws IOException {
        if (JVMVersion.get() >= JVMVersion.JAVA17 && JVMVersion.get() < JVMVersion.JAVA19) {
            final File ret = Files17.guessRoot(file, throwException);
            if (ret != null) {
                return ret;
            }
        }
        String bestRootMatch = null;
        if (CrossSystem.isUnix()) {
            try {
                final List<ProcMounts> procMounts = ProcMounts.list();
                if (procMounts != null) {
                    final String destination = file.getAbsolutePath();
                    for (final ProcMounts procMount : procMounts) {
                        if (!procMount.isReadOnly() && destination.startsWith(procMount.getMountPoint())) {
                            if (bestRootMatch == null || (procMount.getMountPoint().length() > bestRootMatch.length())) {
                                bestRootMatch = procMount.getMountPoint();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LoggerFactory.getDefaultLogger().log(e);
            }
        }
        if (bestRootMatch == null) {
            // fallback to File.listRoots
            final String destination = file.getAbsolutePath();
            if (!destination.startsWith("\\")) {
                final File[] roots = File.listRoots();
                if (roots != null) {
                    for (final File root : roots) {
                        final String rootString = root.getAbsolutePath();
                        final boolean startsWith;
                        if (CrossSystem.isWindows()) {
                            startsWith = StringUtils.startsWithCaseInsensitive(destination, rootString);
                        } else {
                            startsWith = destination.startsWith(rootString);
                        }
                        if (startsWith) {
                            bestRootMatch = rootString;
                            break;
                        }
                    }
                }
            } else {
                // simple unc support (netshares without assigned drive letter)
                File existingFile = file;
                while (existingFile != null) {
                    if (existingFile.exists()) {
                        bestRootMatch = existingFile.getAbsolutePath();
                    }
                    existingFile = existingFile.getParentFile();
                }
            }
        }
        return bestRootMatch == null ? null : new File(bestRootMatch);
    }

    public static long getUsableDiskspace(File path) throws IOException {
        if (Application.getJavaVersion() >= Application.JAVA17) {
            return Files17.getUsableDiskspace(path);
        } else {
            final File root = guessRoot(path);
            if (root != null) {
                return root.getFreeSpace();
            } else {
                return path.getFreeSpace();
            }
        }
    }
}
