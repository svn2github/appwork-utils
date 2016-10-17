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
package org.appwork.utils.swing.dialog;

import java.awt.Dimension;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import org.appwork.resources.AWIcon;
import org.appwork.swing.ExtJDialog;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;

import net.miginfocom.swing.MigLayout;

public class InternDialog<T> extends ExtJDialog {
    /**
     *
     */
    private final AbstractDialog<T> dialogModel;
    /**
     *
     */
    private static final long       serialVersionUID = 1L;

    public void setVisible(boolean b) {
        super.setVisible(b);
        // ((JFrame) owner).setVisible(true);
    }

    public InternDialog(final AbstractDialog<T> abstractDialog, final ModalityType modality) {
        super(abstractDialog.getOwner(), modality);
        dialogModel = abstractDialog;
        setLayout(new MigLayout("ins 5", "[]", "[fill,grow][]"));
        // JPanel contentPane;
        // setContentPane(contentPane = new JPanel());
        List<? extends Image> iconlist = Dialog.getInstance().getIconList();
        if (iconlist == null) {
            iconlist = dialogModel.getIconList();
        }
        if (iconlist != null) {
            setIconImages(iconlist);
        } else {
            if (getOwner() == null) {
                final ArrayList<Image> l = new ArrayList<Image>();
                l.add(DialogIcon.DIALOG_INFO.image(16));
                l.add(DialogIcon.DIALOG_INFO.image(32));
                setIconImages(l);
            }
        }
        if (CrossSystem.isMac()) {
            final com.apple.eawt.Application application = com.apple.eawt.Application.getApplication();
            application.requestUserAttention(true);
        }
    }

    public void setTitle(final String title) {
        super.setTitle(title);
        // if(getName()==null) {
        setName(title);
        // }
    }

    public AbstractDialog<T> getDialogModel() {
        return dialogModel;
    }

    @Override
    public void dispose() {
        System.out.println("Dispose Dialog");
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                InternDialog.this.dialogModel.setDisposed(true);
                dialogModel.dispose();
                InternDialog.super.dispose();
            }
        }.waitForEDT();
    }

    @Override
    public Dimension getPreferredSize() {
        return dialogModel.getPreferredSize();
    }

    public Dimension getRawPreferredSize() {
        return super.getPreferredSize();
    }

    /**
     *
     */
    public void realDispose() {
        super.dispose();
    }
    // @Override
    // public void setLayout(final LayoutManager manager) {
    // super.setLayout(manager);
    // }
}