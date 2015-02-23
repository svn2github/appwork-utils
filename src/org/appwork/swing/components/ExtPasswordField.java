package org.appwork.swing.components;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.appwork.app.gui.BasicGui;
import org.appwork.swing.MigPanel;
import org.appwork.utils.StringUtils;

public class ExtPasswordField extends MigPanel implements FocusListener, DocumentListener, TextComponentInterface, ActionListener {

    /**
     * @author Thomas
     * 
     */
    public final class CustomTextField extends ExtTextField {
        /*
         * (non-Javadoc)
         * 
         * @see org.appwork.swing.components.ExtTextField#replaceSelection(java.lang.String)
         */
        @Override
        public void replaceSelection(String content) {
            ExtPasswordField.this.setText(content);
        }
    }

    /**
     * @author Thomas
     * 
     */
    public final class CustomPasswordField extends JPasswordField {
        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.text.JTextComponent#replaceSelection(java.lang.String)
         */
        private boolean key = false;

        @Override
        public void replaceSelection(String content) {
            if (key) {
                // type on keyboard
                super.replaceSelection(content);
            } else {
                // paste externaly. like contextmenu
                ExtPasswordField.this.setText(content);
            }
        }

        @Override
        protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
            // forward events
            // this will cause to trigger a pressed event on enter. this
            // will the trigger the default action of dialogs - for example
            ExtPasswordField.this.dispatchEvent(e);
            key = true;
            try {
                return super.processKeyBinding(ks, e, condition, pressed);
            } finally {
                key = false;
            }
        }
    }

    /**
     * 
     */
    private static final long serialVersionUID = 9035297840443317147L;

    public static String      MASK             = "••••••••••";

    public static void main(final String[] args) {

        new BasicGui("ExtPasswordField") {

            @Override
            protected void layoutPanel() {
                final ExtPasswordField pw = new ExtPasswordField();
                final ExtPasswordField pwtext = new ExtPasswordField();
                pwtext.setPassword("thomas".toCharArray());
                final ExtPasswordField pwhelp = new ExtPasswordField();
                pwhelp.setName("pwhelp");
                final ExtPasswordField pwhelptext = new ExtPasswordField();
                pwhelptext.setPassword("thomas".toCharArray());
                pwhelp.setHelpText("Please give me a password");
                pwhelptext.setHelpText("BLABLA gimme a pw");
                final MigPanel p = new MigPanel("ins 0,wrap 2", "[][grow,fill]", "[]");
                this.getFrame().setContentPane(p);
                p.add(new JLabel("PW field"));
                p.add(pw);
                p.add(new JLabel("PW width help text"));
                p.add(pwhelp);
                p.add(new JLabel("PW field setpw"));
                p.add(pwtext);
                p.add(new JLabel("PW field setpw &helptext"));
                p.add(pwhelptext);
                p.add(new JButton(new AbstractAction() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = 7405750769257653425L;

                    {
                        this.putValue(Action.NAME, "Print");
                    }

                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        System.out.println(new String(pw.getPassword()));
                        System.out.println(new String(pwhelp.getPassword()));
                        System.out.println(new String(pwtext.getPassword()));
                        System.out.println(new String(pwhelptext.getPassword()));
                    }
                }));

            }

            @Override
            protected void requestExit() {
                // TODO Auto-generated method stub

            }
        };
    }

    private final ExtTextField   renderer;

    private final JPasswordField editor;

    private boolean              rendererMode;

    private char[]               password = new char[] {};

    private String               mask     = null;
    private final AtomicInteger  modifier = new AtomicInteger(0);

    /**
     * @param constraints
     * @param columns
     * @param rows
     */
    public ExtPasswordField() {
        super("ins 0", "[grow,fill]", "[grow,fill]");
        this.renderer = new CustomTextField();
        this.editor = new CustomPasswordField();
        this.renderer.addFocusListener(this);
        this.editor.addFocusListener(this);
        this.add(this.renderer, "hidemode 3");
        this.add(this.editor, "hidemode 3");
        this.editor.setText("");
        // this.renderer.setBackground(Color.RED);
        this.renderer.setText("");
        this.editor.getDocument().addDocumentListener(this);
        this.editor.addActionListener(this);
        this.renderer.setHelpText("");
        this.setRendererMode(true);

    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        return super.processKeyBinding(ks, e, condition, pressed);
    }

    @Override
    public synchronized void addKeyListener(final KeyListener l) {
        this.renderer.addKeyListener(l);
        this.editor.addKeyListener(l);
    }

    @Override
    public synchronized void addMouseListener(final MouseListener l) {
        this.renderer.addMouseListener(l);
        this.editor.addMouseListener(l);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event. DocumentEvent)
     */
    @Override
    public void changedUpdate(final DocumentEvent e) {
        if (this.modifier.get() == 0) {
            if (!Arrays.equals(this.editor.getPassword(), this.getMask().toCharArray())) {
                this.onChanged();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
     */
    @Override
    public void focusGained(final FocusEvent e) {
        if (e.getSource() == this.renderer) {
            this.setRendererMode(false);
            this.editor.requestFocus();
        } else {
            // http://svn.jdownloader.org/issues/49542
            if (this.password.length == 0) {
                this.setEditorText("");
            }
            this.editor.selectAll();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
     */
    @Override
    public void focusLost(final FocusEvent e) {
        if (e.getSource() == this.editor || e == null) {
            final char[] pass = this.editor.getPassword();
            final char[] mask = this.getMask().toCharArray();

            if (!Arrays.equals(pass, mask)) {
                this.password = pass;
            }
            this.setRendererMode(true);
            this.setHelpText(this.getHelpText());
        }
    }

    public javax.swing.text.Document getDocument() {
        return this.editor.getDocument();
    }

    public Color getHelpColor() {
        return this.renderer.getHelpColor();
    }

    public String getHelpText() {
        return this.renderer.getHelpText();
    }

    /**
     * @return
     */
    protected String getMask() {
        return this.mask != null ? this.mask : ExtPasswordField.MASK;
    }

    public char[] getPassword() {
        if (this.editor.isVisible()) {
            final char[] pass = this.editor.getPassword();
            final char[] mask = this.getMask().toCharArray();
            if (!Arrays.equals(pass, mask)) {
                this.password = pass;
            }
        }
        return this.password;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.swing.dialog.TextComponentInterface#getText()
     */
    @Override
    public String getText() {
        final char[] pass = this.getPassword();
        return pass == null ? null : new String(pass);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event. DocumentEvent)
     */
    @Override
    public void insertUpdate(final DocumentEvent e) {
        if (this.modifier.get() == 0) {
            final char[] pass = this.editor.getPassword();
            final char[] mask = this.getMask().toCharArray();
            if (!Arrays.equals(pass, mask)) {
                this.onChanged();
                if (pass.length > 0) {
                    this.renderer.setText(this.getMask());
                } else {
                    this.renderer.setText("");
                }
            }
        }
    }

    public void onChanged() {
    }

    @Override
    public synchronized void removeKeyListener(final KeyListener l) {
        this.renderer.removeKeyListener(l);
        this.editor.removeKeyListener(l);
    }

    @Override
    public synchronized void removeMouseListener(final MouseListener l) {
        this.renderer.removeMouseListener(l);
        this.editor.removeMouseListener(l);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event. DocumentEvent)
     */
    @Override
    public void removeUpdate(final DocumentEvent e) {
        if (this.modifier.get() == 0) {
            if (!Arrays.equals(this.editor.getPassword(), this.getMask().toCharArray())) {
                this.onChanged();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.swing.components.TextComponentInterface#selectAll()
     */
    @Override
    public void selectAll() {
        this.editor.selectAll();
    }

    private void setEditorText(final String text) {
        this.modifier.incrementAndGet();
        try {
            this.editor.setText(text);
        } finally {
            this.modifier.decrementAndGet();
        }
    }

    @Override
    public void setEnabled(final boolean b) {
        this.editor.setEnabled(b);
        this.renderer.setEnabled(b);
        super.setEnabled(b);
    }

    public void setHelpColor(final Color helpColor) {
        this.renderer.setHelpColor(helpColor);
    }

    /**
     * @param addLinksDialog_layoutDialogContent_input_help
     */
    public void setHelpText(final String helpText) {
        this.renderer.setHelpText(helpText);
        if (this.getHelpText() != null && (this.getPassword() == null || this.getPassword().length == 0 || this.getMask().equals(new String(this.getPassword())))) {
            this.renderer.setText(this.getHelpText());
            this.renderer.setForeground(this.getHelpColor());
        } else {
            this.renderer.setText(this.getMask());
            this.renderer.setForeground(this.renderer.getDefaultColor());
        }
        this.setRendererMode(this.rendererMode);

    }

    public void setMask(final String mask) {
        this.mask = mask;
    }

    public void setPassword(final char[] password) {
        this.password = password;
        setEditorText(new String(password));
        this.setHelpText(this.getHelpText());
    }

    /**
     * @param b
     */
    private void setRendererMode(boolean b) {
        this.rendererMode = b;
        b &= this.getHelpText() != null;
        this.renderer.setVisible(b);
        this.editor.setVisible(!b);
        this.revalidate();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.swing.dialog.TextComponentInterface#setText(java.lang .String)
     */
    @Override
    public void setText(final String text) {

        this.setPassword(StringUtils.isEmpty(text) ? new char[0] : text.toCharArray());
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        this.onChanged();
    }

}
