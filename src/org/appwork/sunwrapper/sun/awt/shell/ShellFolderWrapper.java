/**
 * Copyright (c) 2009 - 2014 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.sunwrapper.sun.awt.shell
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.sunwrapper.sun.awt.shell;

import java.awt.Image;
import java.io.File;
import java.io.FileNotFoundException;

import org.appwork.sunwrapper.WrapperNotAvailableException;
import org.appwork.utils.logging2.extmanager.LoggerFactory;

/**
 * @author Thomas
 * 
 */
public class ShellFolderWrapper {

    /**
     * @param file
     * @throws WrapperNotAvailableException
     * @throws FileNotFoundException
     */
    public static Image getIcon(File file) throws WrapperNotAvailableException, FileNotFoundException {

        try {
            sun.awt.shell.ShellFolder shellFolder = sun.awt.shell.ShellFolder.getShellFolder(file);
            return shellFolder.getIcon(true);
        } catch (FileNotFoundException e) {
            throw e;

        } catch (NoClassDefFoundError e) {
            LoggerFactory.I().getLogger(ShellFolderWrapper.class.getName()).log(e);
            throw new WrapperNotAvailableException(e);
        }

    }

    /**
     * @param f
     * @return
     * @throws FileNotFoundException
     */
    public static File getShellFolderIfAvailable(File f) throws FileNotFoundException, InternalError {
        try {
            return sun.awt.shell.ShellFolder.getShellFolder(f);
        } catch (InternalError e) {
            throw e;
        } catch (NoClassDefFoundError e) {
            LoggerFactory.I().getLogger(ShellFolderWrapper.class.getName()).log(e);
            return f;
        }

    }

    /**
     * @param networkFolder
     * @param b
     * @return
     */
    public static File[] listFiles(File networkFolder, boolean b) {
        try {
            return ((sun.awt.shell.ShellFolder) networkFolder).listFiles(b);
        } catch (NoClassDefFoundError e) {
            LoggerFactory.I().getLogger(ShellFolderWrapper.class.getName()).log(e);
            return networkFolder.listFiles();
        }
    }

    /**
     * @param f
     * @return
     */
    public static boolean isInstanceof(File f) {
        try {
            return f instanceof sun.awt.shell.ShellFolder;
        } catch (NoClassDefFoundError e) {
            LoggerFactory.I().getLogger(ShellFolderWrapper.class.getName()).log(e);
            return false;
        }
    }

    /**
     * @param string
     * @return
     */
    public static Object get(String key) {
        try {
            return sun.awt.shell.ShellFolder.get(key);
        } catch (NoClassDefFoundError e) {
            LoggerFactory.I().getLogger(ShellFolderWrapper.class.getName()).log(e);
            return null;
        }
    }

}
