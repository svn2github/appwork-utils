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
package org.appwork.utils.svn;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.core.wc.ISVNCommitParameters;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNInfoHandler;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNCommitPacket;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class Subversion implements ISVNEventHandler {

    /**
     * checks wether logins are correct or not
     *
     * @param url
     * @param user
     * @param pass
     * @return
     */
    public static boolean checkLogin(final String url, final String user, final String pass) {

        return Boolean.TRUE.equals(new LocaleRunnable<Boolean, RuntimeException>() {

            @Override
            protected Boolean run() throws RuntimeException {
                Subversion subversion = null;
                try {
                    subversion = new Subversion(url, user, pass);
                    return true;
                } catch (final SVNException e) {
                } finally {

                    try {
                        subversion.dispose();
                    } catch (final Throwable e) {
                    }
                }
                return false;
            }

        }.runEnglish());

    }

    private SVNRepository             repository;
    private SVNURL                    svnurl;
    private ISVNAuthenticationManager authManager;
    private SVNClientManager          clientManager;
    private SVNUpdateClient           updateClient;
    private SVNCommitClient           commitClient;

    private SVNWCClient               wcClient;
    private SVNStatusClient           statusClient;

    public Subversion() {
    }

    public Subversion(final String url) throws SVNException {
        try {
            setupType(url);
            checkRoot();
        } catch (final SVNException e) {
            dispose();
            throw e;
        }
    }

    // public static void listEntries(final SVNRepository repository, final File root, final int i, final String path) throws SVNException,
    // IOException {
    // Locale bef = Locale.getDefault();
    // Locale.setDefault(Locale.ENGLISH);
    // try {
    // final Collection entries = repository.getDir(path, i, null, (Collection) null);
    // final File revroot = new File(root, i + "");
    // revroot.mkdirs();
    // final Iterator iterator = entries.iterator();
    // while (iterator.hasNext()) {
    // final SVNDirEntry entry = (SVNDirEntry) iterator.next();
    // final File file = new File(revroot, (path.equals("") ? "" : path + "/") + entry.getName());
    // if (entry.getKind() == SVNNodeKind.DIR) {
    // file.mkdirs();
    // } else {
    // file.delete();
    // IO.writeStringToFile(file, "author=" + entry.getAuthor() + "\r\nrevision=" + entry.getRevision() + "\r\ndate=" + entry.getDate());
    // }
    // if (entry.getKind() == SVNNodeKind.DIR) {
    // listEntries(repository, root, i, (path.equals("")) ? entry.getName() : path + "/" + entry.getName());
    // }
    // }
    // } finally {
    // Locale.setDefault(bef);
    // }
    // }

    public Subversion(final String url, final String user, final String pass) throws SVNException {
        new LocaleRunnable<Boolean, SVNException>() {

            @Override
            protected Boolean run() throws SVNException {
                try {
                    setupType(url);
                    authManager = SVNWCUtil.createDefaultAuthenticationManager(user, pass);

                    ((DefaultSVNAuthenticationManager) authManager).setAuthenticationForced(true);
                    repository.setAuthenticationManager(authManager);

                    checkRoot();
                    return null;
                } catch (final SVNException e) {
                    dispose();
                    throw e;
                }
            }

        }.runEnglish();

    }

    /**
     * WCClient
     */
    @Override
    public void checkCancelled() throws SVNCancelException {
    }

    public long checkout(final File file, final SVNRevision revision, final SVNDepth i) throws SVNException {

        return new LocaleRunnable<Long, SVNException>() {

            @Override
            protected Long run() throws SVNException {

                file.mkdirs();

                final SVNUpdateClient updateClient = getUpdateClient();

                updateClient.setIgnoreExternals(false);
                SVNRevision rev = revision;
                if (rev == null) {
                    rev = SVNRevision.HEAD;
                }

                return updateClient.doCheckout(svnurl, file, rev, rev, i, true);

            }

        }.runEnglish().longValue();

    }

    private void checkRoot() throws SVNException {
        new LocaleRunnable<Void, SVNException>() {

            @Override
            protected Void run() throws SVNException {
                final SVNNodeKind nodeKind = repository.checkPath("", -1);
                if (nodeKind == SVNNodeKind.NONE) {
                    final SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "No entry at URL ''{0}''", svnurl);
                    throw new SVNException(err);
                } else if (nodeKind == SVNNodeKind.FILE) {
                    final SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "Entry at URL ''{0}'' is a file while directory was expected", svnurl);
                    throw new SVNException(err);
                }
                return null;
            }

        }.runEnglish();

    }

    /**
     * Cleans up the file or doirectory
     *
     * @param dstPath
     * @param deleteWCProperties
     * @throws SVNException
     */
    public void cleanUp(final File dstPath, final boolean deleteWCProperties) throws SVNException {
        new LocaleRunnable<Void, SVNException>() {

            @Override
            protected Void run() throws SVNException {
                getWCClient().doCleanup(dstPath, deleteWCProperties);
                return null;
            }

        }.runEnglish();

    }

    /**
     * Commits the wholepath and KEEPS locks
     *
     * @param dstPath
     * @param message
     * @return
     * @throws SVNException
     */
    public SVNCommitInfo commit(final File dstPath, final String message) throws SVNException {
        return commit(message, dstPath);
    }

    public SVNCommitInfo commit(final String message, final File... dstPathes) throws SVNException {
        return new LocaleRunnable<SVNCommitInfo, SVNException>() {

            @Override
            protected SVNCommitInfo run() throws SVNException {
                for (File f : dstPathes) {
                    getWCClient().doAdd(f, true, false, true, SVNDepth.INFINITY, false, false);
                }

                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer("Create CommitPacket");
                final SVNCommitPacket packet = getCommitClient().doCollectCommitItems(dstPathes, false, false, SVNDepth.INFINITY, null);
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer("Transfer Package");
                if (packet == SVNCommitPacket.EMPTY) {
                    return null;
                }
                return getCommitClient().doCommit(packet, true, false, message, null);
            }

        }.runEnglish();

    }

    public void dispose() {

        new LocaleRunnable<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                try {
                    repository.closeSession();
                } catch (final Throwable e) {
                }

                getClientManager().dispose();

                return null;
            }

        }.runEnglish();

    }

    public long downloadFile(final String url, final File resource, final SVNRevision head) throws SVNException {

        return new LocaleRunnable<Long, SVNException>() {

            @Override
            protected Long run() throws SVNException {
                return getUpdateClient().doExport(SVNURL.parseURIDecoded(url), resource, head, head, null, true, null);
            }

        }.runEnglish().longValue();

    }

    public long export(final File file) throws SVNException, IOException {
        try {
            return new LocaleRunnable<Long, Exception>() {

                @Override
                protected Long run() throws Exception {
                    Files.deleteRecursiv(file);
                    file.mkdirs();

                    final ISVNEditor exportEditor = new ExportEditor(file);
                    final long rev = latestRevision();
                    final ISVNReporterBaton reporterBaton = new ExportReporterBaton(rev);

                    repository.update(rev, null, true, reporterBaton, exportEditor);

                    return rev;
                }

            }.runEnglish().longValue();
        } catch (SVNException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new WTFException(e);
        }

    }

    /**
     * Returns all changesets between revision start and end
     *
     * @param start
     * @param end
     * @return
     * @throws SVNException
     */
    @SuppressWarnings("unchecked")
    public java.util.List<SVNLogEntry> getChangeset(final long start, final long end) throws SVNException {
        return new LocaleRunnable<List<SVNLogEntry>, SVNException>() {

            @Override
            public List<SVNLogEntry> run() throws SVNException {

                final Collection<SVNLogEntry> log = repository.log(new String[] { "" }, null, start, end, true, true);

                final java.util.List<SVNLogEntry> list = new ArrayList<SVNLogEntry>();
                list.addAll(log);
                return list;

            }

            public List<SVNLogEntry> runEnglish() {
                // TODO Auto-generated method stub
                return null;
            }

        }.runEnglish();

    }

    private synchronized SVNClientManager getClientManager() {

        if (clientManager == null) {
            final DefaultSVNOptions options = new DefaultSVNOptions(null, true) {
                private String[] ignorePatterns;

                {
                    ignorePatterns = new String[] {};
                }

                @Override
                public String[] getIgnorePatterns() {

                    return ignorePatterns;
                }

            };
            options.setIgnorePatterns(null);
            clientManager = SVNClientManager.newInstance(options, authManager);
        }
        return clientManager;
    }

    public SVNCommitClient getCommitClient() {

        if (commitClient == null) {
            commitClient = getClientManager().getCommitClient();
            commitClient.setEventHandler(this);
            commitClient.setCommitParameters(new ISVNCommitParameters() {

                @Override
                public boolean onDirectoryDeletion(final File directory) {
                    return false;
                }

                @Override
                public boolean onFileDeletion(final File file) {
                    return false;
                }

                @Override
                public Action onMissingDirectory(final File file) {
                    return ISVNCommitParameters.DELETE;
                }

                @Override
                public Action onMissingFile(final File file) {
                    return ISVNCommitParameters.DELETE;
                }
            });
        }
        return commitClient;

    }

    /**
     * Returns an ArrayLIst with Info for all files found in file.
     *
     * @param file
     * @return
     */
    public java.util.List<SVNInfo> getInfo(final File file) {
        return new LocaleRunnable<List<SVNInfo>, RuntimeException>() {

            @Override
            protected List<SVNInfo> run() throws RuntimeException {
                final java.util.List<SVNInfo> ret = new ArrayList<SVNInfo>();
                try {
                    getWCClient().doInfo(file, SVNRevision.UNDEFINED, SVNRevision.WORKING, SVNDepth.getInfinityOrEmptyDepth(true), null, new ISVNInfoHandler() {

                        @Override
                        public void handleInfo(final SVNInfo info) {
                            ret.add(info);
                        }

                    });
                } catch (final SVNException e) {
                    e.printStackTrace();
                }
                return ret;
            }

        }.runEnglish();

    }

    public long getRemoteRevision(final String resource) throws SVNException {
        return new LocaleRunnable<Long, SVNException>() {

            @Override
            protected Long run() throws SVNException {
                final SVNDirEntry de = getRepository().getDir(resource, -1, false, null);
                return de.getRevision();
            }

        }.runEnglish().longValue();

    }

    /**
     * Return repo for external actions
     *
     * @return
     */
    public SVNRepository getRepository() {
        return repository;
    }

    public long getRevision(final File resource) throws SVNException {

        return new LocaleRunnable<Long, SVNException>() {

            @Override
            protected Long run() throws SVNException {
                final long[] ret = new long[] { -1 };

                getWCClient().doInfo(resource, SVNRevision.UNDEFINED, SVNRevision.WORKING, SVNDepth.EMPTY, null, new ISVNInfoHandler() {

                    @Override
                    public void handleInfo(final SVNInfo info) {
                        final long rev = info.getCommittedRevision().getNumber();
                        if (rev > ret[0]) {
                            ret[0] = rev;
                        }

                    }

                });

                return ret[0];
            }

        }.runEnglish().longValue();

    }

    public long getRevisionNoException(final File resource) throws SVNException {

        try {
            return getRevision(resource);
        } catch (final SVNException e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
        return -1;

    }

    private SVNStatusClient getStatusClient() {

        if (statusClient == null) {
            statusClient = getClientManager().getStatusClient();
            statusClient.setEventHandler(this);
        }

        return statusClient;

    }

    public SVNUpdateClient getUpdateClient() {
        if (updateClient == null) {
            updateClient = getClientManager().getUpdateClient();
            updateClient.setEventHandler(this);
        }

        return updateClient;
    }

    public SVNWCClient getWCClient() {
        if (wcClient == null) {
            wcClient = getClientManager().getWCClient();
            wcClient.setEventHandler(this);
        }

        return wcClient;
    }

    /**
     * WCClientHanlder
     *
     * @param event
     * @param progress
     * @throws SVNException
     */
    @Override
    public void handleEvent(final SVNEvent event, final double progress) throws SVNException {
        /* WCCLient */
        final String nullString = " ";
        final SVNEventAction action = event.getAction();
        String pathChangeType = nullString;
        if (action == SVNEventAction.ADD) {
            /*
             * The item is scheduled for addition.
             */
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("A     " + event.getFile());
            return;
        } else if (action == SVNEventAction.COPY) {
            /*
             * The item is scheduled for addition with history (copied, in other words).
             */
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("A  +  " + event.getFile());
            return;
        } else if (action == SVNEventAction.DELETE) {
            /*
             * The item is scheduled for deletion.
             */
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("D     " + event.getFile());
            return;
        } else if (action == SVNEventAction.LOCKED) {
            /*
             * The item is locked.
             */
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("L     " + event.getFile());
            return;
        } else if (action == SVNEventAction.LOCK_FAILED) {
            /*
             * Locking operation failed.
             */
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("failed to lock    " + event.getFile());
            return;
        }

        /* Updatehandler */

        if (action == SVNEventAction.UPDATE_ADD) {
            /*
             * the item was added
             */
            pathChangeType = "A";
        } else if (action == SVNEventAction.UPDATE_DELETE) {
            /*
             * the item was deleted
             */
            pathChangeType = "D";
        } else if (action == SVNEventAction.UPDATE_UPDATE) {
            /*
             * Find out in details what state the item is (after having been updated).
             *
             * Gets the status of file/directory item contents. It is SVNStatusType who contains information on the state of an item.
             */
            final SVNStatusType contentsStatus = event.getContentsStatus();
            if (contentsStatus == SVNStatusType.CHANGED) {
                /*
                 * the item was modified in the repository (got the changes from the repository
                 */
                pathChangeType = "U";
            } else if (contentsStatus == SVNStatusType.CONFLICTED) {
                /*
                 * The file item is in a state of Conflict. That is, changes received from the repository during an update, overlap with
                 * local changes the user has in his working copy.
                 */

                pathChangeType = "C";
            } else if (contentsStatus == SVNStatusType.MERGED) {
                /*
                 * The file item was merGed (those changes that came from the repository did not overlap local changes and were merged into
                 * the file).
                 */
                pathChangeType = "G";
            }
        } else if (action == SVNEventAction.UPDATE_EXTERNAL) {
            /*
             * for externals definitions
             */
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Fetching external item into '" + event.getFile().getAbsolutePath() + "'");
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("External at revision " + event.getRevision());
            return;
        } else if (action == SVNEventAction.UPDATE_COMPLETED) {
            /*
             * Working copy update is completed. Prints out the revision.
             */
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("At revision " + event.getRevision());
            return;
        }

        /*
         * Status of properties of an item. SVNStatusType also contains information on the properties state.
         */
        final SVNStatusType propertiesStatus = event.getPropertiesStatus();
        String propertiesChangeType = nullString;
        if (propertiesStatus == SVNStatusType.CHANGED) {
            /*
             * Properties were updated.
             */
            propertiesChangeType = "U";
        } else if (propertiesStatus == SVNStatusType.CONFLICTED) {
            /*
             * Properties are in conflict with the repository.
             */
            propertiesChangeType = "C";
        } else if (propertiesStatus == SVNStatusType.MERGED) {
            /*
             * Properties that came from the repository were merged with the local ones.
             */
            propertiesChangeType = "G";
        }

        /*
         * Gets the status of the lock.
         */
        String lockLabel = nullString;
        final SVNStatusType lockType = event.getLockStatus();

        if (lockType == SVNStatusType.LOCK_UNLOCKED) {
            /*
             * The lock is broken by someone.
             */
            lockLabel = "B";
        }
        if (pathChangeType != nullString || propertiesChangeType != nullString || lockLabel != nullString) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine(pathChangeType + propertiesChangeType + lockLabel + "       " + event.getFile());
        }

        /*
         * Comitghandler
         */

        if (action == SVNEventAction.COMMIT_MODIFIED) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Sending   " + event.getFile());
        } else if (action == SVNEventAction.COMMIT_DELETED) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Deleting   " + event.getFile());
        } else if (action == SVNEventAction.COMMIT_REPLACED) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Replacing   " + event.getFile());
        } else if (action == SVNEventAction.COMMIT_DELTA_SENT) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Transmitting file data....");
        } else if (action == SVNEventAction.COMMIT_ADDED) {
            /*
             * Gets the MIME-type of the item.
             */
            final String mimeType = event.getMimeType();
            if (SVNProperty.isBinaryMimeType(mimeType)) {
                /*
                 * If the item is a binary file
                 */
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Adding  (bin)  " + event.getFile());
            } else {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Adding         " + event.getFile());
            }
        }

    }

    public long latestRevision() throws SVNException {

        return repository.getLatestRevision();
    }

    /**
     * @param filePathFilter
     * @return
     * @throws SVNException
     * @throws InterruptedException
     */
    public List<SVNDirEntry> listFiles(final FilePathFilter filePathFilter, final String path) throws SVNException, InterruptedException {

        final java.util.List<SVNDirEntry> ret = new ArrayList<SVNDirEntry>();
        final Collection entries = new LocaleRunnable<Collection, SVNException>() {

            @Override
            protected Collection run() throws SVNException {

                return repository.getDir(path, -1, null, (Collection) null);

            }

        }.runEnglish();
        final Iterator iterator = entries.iterator();
        while (iterator.hasNext()) {
            final SVNDirEntry entry = (SVNDirEntry) iterator.next();
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            if (filePathFilter.accept(entry)) {
                entry.setRelativePath((path.equals("") ? "" : path + "/") + entry.getName());
                ret.add(entry);
                System.out.println("/" + (path.equals("") ? "" : path + "/") + entry.getName() + " ( author: '" + entry.getAuthor() + "'; revision: " + entry.getRevision() + "; date: " + entry.getDate() + ")");

            }
            ;
            if (entry.getKind() == SVNNodeKind.DIR) {
                ret.addAll(listFiles(filePathFilter, path.equals("") ? entry.getName() : path + "/" + entry.getName()));
            }
        }
        return ret;
    }

    /**
     * Locks a file or directory as long as it it not locked by someone else
     *
     * @param dstPath
     * @param message
     * @throws SVNException
     */
    public void lock(final File dstPath, final String message) throws SVNException {
        new LocaleRunnable<Void, SVNException>() {

            @Override
            protected Void run() throws SVNException {
                getWCClient().doLock(new File[] { dstPath }, false, message);
                return null;
            }

        }.runEnglish();

    }

    public void resolveConflictedFile(final SVNInfo info, final File file, final ResolveHandler handler) throws Exception {

        final String mine = "<<<<<<< .mine";
        final String delim = "=======";
        final String theirs = ">>>>>>> .r";
        String txt = IO.readFileToString(file);
        String pre, post;
        while (true) {
            int mineStart = txt.indexOf(mine);

            if (mineStart < 0) {
                break;
            }
            mineStart += mine.length();
            final int delimStart = txt.indexOf(delim, mineStart);
            final int theirsEnd = txt.indexOf(theirs, delimStart + delim.length());
            int end = theirsEnd + theirs.length();
            while (end < txt.length() && txt.charAt(end) != '\r' && txt.charAt(end) != '\n') {
                end++;
            }

            pre = txt.substring(0, mineStart - mine.length());
            post = txt.substring(end);
            while (pre.endsWith("\r") || pre.endsWith("\n")) {
                pre = pre.substring(0, pre.length() - 1);
            }
            while (post.startsWith("\r") || post.startsWith("\n")) {
                post = post.substring(1);
            }
            pre += "\r\n";
            post = "\r\n" + post;
            if (pre.trim().length() == 0) {
                pre = pre.trim();
            }
            if (post.trim().length() == 0) {
                post = post.trim();
            }

            final String ftxt = txt;
            final int fmineStart = mineStart;
            final String solve = new LocaleRunnable<String, Exception>() {

                @Override
                protected String run() throws SVNException {

                    return handler.resolveConflict(info, file, ftxt, fmineStart, delimStart, delimStart + delim.length(), theirsEnd);

                }

            }.runEnglish();
            if (solve == null) {
                throw new Exception("Could not resolve");
            }
            txt = pre + solve.trim() + post;
        }
        file.delete();
        IO.writeStringToFile(file, txt);

    }

    public void resolveConflicts(final File file, final ResolveHandler handler) throws SVNException {
        new LocaleRunnable<Void, SVNException>() {

            @Override
            protected Void run() throws SVNException {
                getWCClient().doInfo(file, SVNRevision.UNDEFINED, SVNRevision.WORKING, SVNDepth.getInfinityOrEmptyDepth(true), null, new ISVNInfoHandler() {

                    @Override
                    public void handleInfo(final SVNInfo info) {
                        final File file = info.getConflictWrkFile();
                        if (file != null) {
                            try {
                                Subversion.this.resolveConflictedFile(info, info.getFile(), handler);
                                Subversion.this.getWCClient().doResolve(info.getFile(), SVNDepth.INFINITY, null);
                                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine(file + " resolved");
                            } catch (final Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                return null;
            }

        }.runEnglish();

    }

    public void revert(final File dstPath) throws SVNException {
        revert(dstPath, false);
    }

    /**
     * Reverts the file or directory
     *
     * @param dstPath
     * @throws SVNException
     */
    public void revert(final File dstPath, final boolean deleteUnversionedFiles) throws SVNException {
        new LocaleRunnable<Void, SVNException>() {

            @Override
            protected Void run() throws SVNException {
                try {
                    if (deleteUnversionedFiles) {
                        getStatusClient().doStatus(dstPath, SVNRevision.HEAD, SVNDepth.INFINITY, false, true, false, false, new ISVNStatusHandler() {

                            @Override
                            public void handleStatus(SVNStatus status) throws SVNException {
                                SVNStatusType statusType = status.getContentsStatus();
                                if (statusType == SVNStatusType.STATUS_NONE) {
                                    try {
                                        Files.deleteRecursiv(status.getFile(), true);
                                    } catch (IOException e) {
                                        throw new WTFException(e);
                                    }
                                }

                            }
                        }, null);
                    }
                    getWCClient().doRevert(new File[] { dstPath }, SVNDepth.INFINITY, null);
                } catch (final RuntimeException e) {
                    cleanUp(dstPath, false);
                    throw e;
                } catch (final SVNException e) {

                    cleanUp(dstPath, false);
                    throw e;
                }
                return null;
            }

        }.runEnglish();

    }

    private void setupType(final String url) throws SVNException {
        new LocaleRunnable<Void, SVNException>() {

            @Override
            protected Void run() throws SVNException {
                svnurl = SVNURL.parseURIDecoded(url);

                if (url.startsWith("http")) {
                    DAVRepositoryFactory.setup();
                    repository = SVNRepositoryFactory.create(svnurl);
                } else if (url.startsWith("svn")) {
                    SVNRepositoryFactoryImpl.setup();
                    repository = SVNRepositoryFactory.create(svnurl);
                } else {
                    FSRepositoryFactory.setup();
                    repository = SVNRepositoryFactory.create(svnurl);
                }
                return null;
            }

        }.runEnglish();

    }

    public void showInfo(final File wcPath, final SVNRevision revision, final boolean isRecursive) throws SVNException {

        new LocaleRunnable<Void, SVNException>() {

            @Override
            protected Void run() throws SVNException {
                if (revision == null) {
                    getWCClient().doInfo(wcPath, SVNRevision.UNDEFINED, SVNRevision.HEAD, SVNDepth.getInfinityOrEmptyDepth(isRecursive), null, new InfoEventHandler());

                } else {
                    getWCClient().doInfo(wcPath, SVNRevision.UNDEFINED, revision, SVNDepth.getInfinityOrEmptyDepth(isRecursive), null, new InfoEventHandler());
                }

                return null;
            }

        }.runEnglish();

    }

    public void showStatus(final File wcPath, final boolean isRecursive, final boolean isRemote, final boolean isReportAll, final boolean isIncludeIgnored, final boolean isCollectParentExternals) throws SVNException {
        new LocaleRunnable<Void, SVNException>() {

            @Override
            protected Void run() throws SVNException {
                getClientManager().getStatusClient().doStatus(wcPath, SVNRevision.HEAD, SVNDepth.fromRecurse(isRecursive), isRemote, isReportAll, isIncludeIgnored, isCollectParentExternals, new StatusEventHandler(isRemote), null);

                return null;
            }

        }.runEnglish();

    }

    /**
     * Unlocks this file only if it is locked by you
     *
     * @param dstPath
     * @param message
     * @throws SVNException
     */
    public void unlock(final File dstPath) throws SVNException {
        new LocaleRunnable<Void, SVNException>() {

            @Override
            protected Void run() throws SVNException {
                getWCClient().doUnlock(new File[] { dstPath }, false);
                return null;
            }

        }.runEnglish();

    }

    /**
     * Updates the repo to file. if there is no repo at file, a checkout is performed
     *
     * @param file
     * @param revision
     * @throws SVNException
     * @return revision
     */
    public long update(final File file, final SVNRevision revision) throws SVNException {
        return this.update(file, revision, SVNDepth.INFINITY);

    }

    public long update(final File file, final SVNRevision revision, final SVNDepth i) throws SVNException {

        return new LocaleRunnable<Long, SVNException>() {

            @Override
            protected Long run() throws SVNException {
                SVNDepth fi = i;
                if (fi == null) {
                    fi = SVNDepth.INFINITY;
                }
                // JDIO.removeDirectoryOrFile(file);
                file.mkdirs();

                final SVNUpdateClient updateClient = getUpdateClient();

                updateClient.setIgnoreExternals(false);
                SVNRevision frevision = revision;
                if (frevision == null) {
                    frevision = SVNRevision.HEAD;
                }

                try {

                    // getWCClient().doAdd(path, force, mkdir, climbUnversionedParents,
                    // depth, includeIgnored, makeParents);
                    // long ret = updateClient.doCheckout(svnurl, file, frevision,
                    // frevision, i, true);
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("SVN Update at " + file + " to Revision " + frevision + " depths:" + fi + "  " + svnurl);
                    long ret = updateClient.doUpdate(file, frevision, fi, false, true);
                    if (ret < 0) {
                        // no working copy?
                        ret = updateClient.doCheckout(svnurl, file, frevision, frevision, fi, true);

                    }
                    return ret;
                } catch (final Exception e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info(e.getMessage());
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("SVN Checkout at " + file + "  " + svnurl);
                    return updateClient.doCheckout(svnurl, file, frevision, frevision, fi, true);

                } finally {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("SVN Update finished");
                }

            }

        }.runEnglish().longValue();

    }

    /**
     * @param string
     * @param string2
     * @param bs
     * @throws IOException
     * @throws SVNException
     */
    public void write(final String path, final String commitmessage, final byte[] content) throws SVNException, IOException {
        this.write(path, commitmessage, new ByteArrayInputStream(content));

    }

    public SVNCommitInfo write(final String path, final String commitmessage, final ByteArrayInputStream is) throws SVNException, IOException {
        try {
            return new LocaleRunnable<SVNCommitInfo, Exception>() {

                @Override
                protected SVNCommitInfo run() throws Exception {
                    final File file = new File(Application.getTempResource("svnwrite_" + System.currentTimeMillis()), path);
                    downloadFile(svnurl + (svnurl.toString().endsWith("/") ? "" : "/") + path, file, SVNRevision.HEAD);

                    final SVNDeltaGenerator generator = new SVNDeltaGenerator();

                    final ISVNEditor commitEditor = getRepository().getCommitEditor(commitmessage, null);
                    try {
                        commitEditor.openRoot(-1);
                        commitEditor.openFile(path, -1);
                        commitEditor.applyTextDelta(path, null);
                        final String checksum = generator.sendDelta(path, is, commitEditor, true);
                        commitEditor.closeFile(path, checksum);
                        commitEditor.closeDir();
                        final SVNCommitInfo info = commitEditor.closeEdit();
                        return info;
                    } finally {
                        if (commitEditor != null) {
                            commitEditor.abortEdit();
                        }

                        Files.deleteRecursiv(file.getParentFile());

                    }
                }

            }.runEnglish();
        } catch (SVNException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new WTFException(e);
        }

    }

}
