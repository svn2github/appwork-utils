//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.appwork.utils.svn;


import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.wc.ISVNInfoHandler;
import org.tmatesoft.svn.core.wc.SVNInfo;

public class InfoEventHandler implements ISVNInfoHandler {

    public void handleInfo(SVNInfo info) {
              org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("-----------------INFO-----------------");
              org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Local Path: " + info.getPath());
              org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("URL: " + info.getURL());

        if (info.isRemote() && info.getRepositoryRootURL() != null) {
                  org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Repository Root URL: " + info.getRepositoryRootURL());
        }

        if (info.getRepositoryUUID() != null) {
                  org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Repository UUID: " + info.getRepositoryUUID());
        }

              org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Revision: " + info.getRevision().getNumber());
              org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Node Kind: " + info.getKind().toString());

        if (!info.isRemote()) {
                  org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Schedule: " + (info.getSchedule() != null ? info.getSchedule() : "normal"));
        }

              org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Last Changed Author: " + info.getAuthor());
              org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Last Changed Revision: " + info.getCommittedRevision().getNumber());
              org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Last Changed Date: " + info.getCommittedDate());

        if (info.getPropTime() != null) {
                  org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Properties Last Updated: " + info.getPropTime());
        }

        if (info.getKind() == SVNNodeKind.FILE && info.getChecksum() != null) {
            if (info.getTextTime() != null) {
                      org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Text Last Updated: " + info.getTextTime());
            }
                  org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Checksum: " + info.getChecksum());
        }

        if (info.getLock() != null) {
            if (info.getLock().getID() != null) {
                      org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Lock Token: " + info.getLock().getID());
            }

                  org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Lock Owner: " + info.getLock().getOwner());
                  org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Lock Created: " + info.getLock().getCreationDate());

            if (info.getLock().getExpirationDate() != null) {
                      org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Lock Expires: " + info.getLock().getExpirationDate());
            }

            if (info.getLock().getComment() != null) {
                      org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Lock Comment: " + info.getLock().getComment());
            }
        }
    }
}
