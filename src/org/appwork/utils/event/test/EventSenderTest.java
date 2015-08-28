/**
 * Copyright (c) 2009 - 2015 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.utils.event.test
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.event.test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.appwork.utils.IO;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;

/**
 * @author daniel
 *
 */
public class EventSenderTest {
    /**
     * @param name
     * @param file
     * @throws IOException
     */
    private static void create(final String name, final File file) throws IOException {
        String pkg = "";
        System.out.println("");
        File p = file;
        do {
            if (pkg.length() > 0) {
                pkg = "." + pkg;
            }
            pkg = p.getName() + pkg;

        } while ((p = p.getParentFile()) != null && !p.getName().equals("src"));

        StringBuilder sb = new StringBuilder();
        final String senderName = name + "EventSender";
        final String eventName = name + "Event";
        final String listenerName = name + "Listener";

        sb.append("package " + pkg + ";\r\n\r\n");
        sb.append("import org.appwork.utils.event.Eventsender;\r\n\r\n");
        sb.append("public class " + senderName + " extends Eventsender<" + listenerName + ", " + eventName + "> {\r\n\r\n");
        sb.append("@Override\r\n");
        sb.append("protected void fireEvent(" + listenerName + " listener, " + eventName + " event) {\r\nswitch (event.getType()) {\r\n//fill\r\ndefault: System.out.println(\"Unhandled Event: \"+event); \r\n}\r\n}");
        sb.append("}");
        new File(file, senderName + ".java").delete();
        IO.writeStringToFile(new File(file, senderName + ".java"), sb.toString());
        sb = new StringBuilder();

        sb.append("package " + pkg + ";\r\n\r\n");
        sb.append("import java.util.EventListener;\r\n\r\n");
        sb.append("public interface " + listenerName + " extends EventListener {\r\n\r\n}");
        new File(file, listenerName + ".java").delete();
        IO.writeStringToFile(new File(file, listenerName + ".java"), sb.toString());

        sb = new StringBuilder();
        sb.append("package " + pkg + ";\r\n\r\n");
        sb.append("import org.appwork.utils.event.SimpleEvent;\r\n\r\n");
        sb.append("public class " + eventName + " extends SimpleEvent<Object, Object, " + eventName + ".Type> {\r\n\r\n");
        sb.append("public static enum Type{\r\n}\r\n");
        sb.append("public " + eventName + "(Object caller, Type type, Object... parameters) {\r\n");
        sb.append("super(caller, type, parameters);\r\n}\r\n");
        sb.append("}");
        new File(file, eventName + ".java").delete();
        IO.writeStringToFile(new File(file, eventName + ".java"), sb.toString());
    }

    public static void main(final String[] args) throws DialogClosedException, DialogCanceledException, IOException, URISyntaxException {
        final URL root = Thread.currentThread().getClass().getResource("/");
        final File rootFile = new File(root.toURI());
        final String name = Dialog.getInstance().showInputDialog("Enter Name");

        final ExtFileChooserDialog d = new ExtFileChooserDialog(0, "Choose folder", null, null);
        d.setStorageID("EventSenderCReater");
        d.setFileSelectionMode(FileChooserSelectionMode.DIRECTORIES_ONLY);

        d.setType(FileChooserType.OPEN_DIALOG);
        d.setMultiSelection(false);
        d.setPreSelection(rootFile.getParentFile().getParentFile());
        try {
            Dialog.I().showDialog(d);
        } catch (final DialogClosedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final DialogCanceledException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        create(name, d.getSelectedFile());
        System.exit(1);
    }
}
