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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import net.miginfocom.swing.MigLayout;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.BinaryLogic;
import org.appwork.utils.StringUtils;
import org.appwork.utils.locale._AWU;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxy.TYPE;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;

public class ProxyDialog extends AbstractDialog<HTTPProxy> implements CaretListener {

    public static void main(final String[] args) throws UnsupportedEncodingException, DialogClosedException, DialogCanceledException {

        Dialog.getInstance().showDialog(new ProxyDialog(HTTPProxy.NONE, "No Connection to the Internet. Please check your Connection settings!"));
    }

    protected JComboBox<ProxyType> cmbType;
    protected ExtTextField         txtHost;
    protected ExtTextField         txtPort;
    protected ExtTextField         txtUser;

    private ExtPasswordField       txtPass;

    public static enum ProxyType implements LabelInterface {
        HTTP() {

            @Override
            public String getLabel() {

                return _AWU.T.ProxyDialog_http();
            }

        },
        SOCKS5() {

            @Override
            public String getLabel() {

                return _AWU.T.ProxyDialog_socks5();
            }

        },
        SOCKS4() {

            @Override
            public String getLabel() {

                return _AWU.T.ProxyDialog_socks4();
            }

        },
        DIRECT() {

            @Override
            public String getLabel() {

                return _AWU.T.ProxyDialog_direct();
            }

        },
        AUTO() {

            @Override
            public String getLabel() {

                return _AWU.T.ProxyDialog_auto();
            }

        }
    }

    private JLabel          lblUser;
    private JLabel          lblPass;
    private JLabel          lblPort;
    private JLabel          lblHost;
    private DelayedRunnable delayer;
    private TYPE            type;
    private final HTTPProxy proxy;
    private ExtTextArea     desc;
    private String          message;

    public void setMessage(String message) {
        this.message = message;
    }

    private ExtCheckBox              cbAuth;
    private boolean                  authRequired = false;

    private ScheduledExecutorService executer;
    private boolean                  typeEditable = true;

    public boolean isTypeEditable() {
        return typeEditable;
    }

    public boolean isHostEditable() {
        return hostEditable;
    }

    public boolean isPortEditable() {
        return portEditable;
    }

    private boolean   hostEditable = true;
    private boolean   portEditable = true;
    private JCheckBox remember;
    private JLabel    rememberLbl;

    public ProxyDialog(final HTTPProxy usedProxy, final String message) {
        super(Dialog.STYLE_HIDE_ICON, _AWU.T.proxydialog_title(), null, _AWU.T.lit_save(), _AWU.T.ABSTRACTDIALOG_BUTTON_CANCEL());
        proxy = usedProxy;
        this.message = message;
    }

    protected MigPanel createBottomPanel() {
        return super.createBottomPanel();
        // TODO Auto-generated method stub
        // MigPanel ret = new MigPanel("ins 0", "[]20[grow,fill][]", "[]");
        // JLabel tocs = new
        // JLabel("<html><font color=\"#999999\"><a font href=\"https://www.oboom.com/#agb\">"
        // + _GUI._.specialdeals_oboom_tocs() + "</a></font></html>");
        // tocs.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // tocs.addMouseListener(new MouseAdapter() {
        // @Override
        // public void mouseClicked(MouseEvent e) {
        // CrossSystem.openURL("https://www.oboom.com/#agb");
        // }
        // });
        // ret.add(tocs, "");
        // return ret;
    }

    @Override
    protected DefaultButtonPanel createBottomButtonPanel() {
        if (!isShowRemember()) {
            return super.createBottomButtonPanel();
        }
        DefaultButtonPanel ret = createButtonPanelImpl("ins 0", "[]", "0[grow,fill]0");

        remember = new JCheckBox();
        rememberLbl = new JLabel(_AWU.T.proxydialog_remember());
        ret.add(rememberLbl, "");
        ret.add(remember, "");
        return ret;
    }

    public boolean isRememberChecked() {
        return new EDTHelper<Boolean>() {

            @Override
            public Boolean edtRun() {

                return remember != null && remember.isSelected();
            }
        }.getReturnValue() == Boolean.TRUE;
    }

    /**
     * @return
     */
    protected boolean isShowRemember() {

        return false;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        txtPass.setEnabled(true);
        lblPass.setEnabled(true);
        lblUser.setEnabled(true);
        txtUser.setEnabled(true);
        txtPort.setEnabled(isPortEditable());
        lblPort.setEnabled(isPortEditable());
        lblHost.setEnabled(isHostEditable());
        txtHost.setEnabled(isHostEditable());
        cbAuth.setEnabled(true);
        if (e.getSource() == cmbType) {

            switch ((ProxyType) (cmbType.getSelectedItem())) {
            case HTTP:
                // http

                if (StringUtils.isEmpty(txtPort.getText())) {
                    txtPort.setText("8080");
                }
                break;
            case SOCKS5:
                // socks5

                if (StringUtils.isEmpty(txtPort.getText())) {
                    txtPort.setText("1080");
                }
                break;
            case SOCKS4:
                // socks4

                if (StringUtils.isEmpty(txtPort.getText())) {
                    txtPort.setText("1080");
                }
                break;
            case DIRECT:
            case AUTO:
                // direct
                txtPass.setEnabled(false);
                lblPass.setEnabled(false);
                lblUser.setEnabled(false);
                txtUser.setEnabled(false);
                txtPort.setEnabled(false);
                lblPort.setEnabled(false);
                lblHost.setEnabled(false);
                txtHost.setEnabled(false);
                cbAuth.setEnabled(false);
                cbAuth.setSelected(false);
                break;

            default:
                txtPass.setEnabled(false);
                lblPass.setEnabled(false);
                lblUser.setEnabled(true);
                txtUser.setEnabled(true);
                lblPort.setEnabled(true);
                if (StringUtils.isEmpty(txtPort.getText())) {
                    txtPort.setText("1080");
                }
            }
            cbAuth.updateDependencies();

        } else {
            super.actionPerformed(e);
        }
    }

    /**
     * update okayButton enabled status, check if host/port(valid number) or host is given
     */
    public void caretUpdate(final CaretEvent e) {
        boolean enable = false;
        try {
            if (cmbType.getSelectedIndex() != 2) {
                if (txtHost.getDocument().getLength() > 0 && txtPort.getDocument().getLength() > 0) {
                    try {
                        final int port = Integer.parseInt(txtPort.getText());
                        if (port > 0 && port < 65535) {
                            enable = true;
                        }
                    } catch (final Throwable ee) {
                    }
                }
            } else {
                if (txtHost.getDocument().getLength() > 0) {
                    enable = true;
                }
            }
        } finally {
            okButton.setEnabled(enable);
        }
    }

    public ProxyType getSelectedType() {
        return new EDTHelper<ProxyType>() {

            @Override
            public ProxyType edtRun() {
                // TODO Auto-generated method stub
                return ((ProxyType) cmbType.getSelectedItem());
            }
        }.getReturnValue();
    }

    /**
     * returns HTTPProxy for given settings
     */
    @Override
    protected HTTPProxy createReturnValue() {
        final int mask = getReturnmask();
        if (BinaryLogic.containsSome(mask, Dialog.RETURN_CLOSED)) {
            return null;
        }
        if (BinaryLogic.containsSome(mask, Dialog.RETURN_CANCEL)) {
            return null;
        }
        try {

            HTTPProxy.TYPE type = null;
            switch (((ProxyType) cmbType.getSelectedItem())) {
            case HTTP:
                type = HTTPProxy.TYPE.HTTP;
                break;
            case SOCKS4:
                type = HTTPProxy.TYPE.SOCKS4;
                break;
            case SOCKS5:
                type = HTTPProxy.TYPE.SOCKS5;
                break;
            case DIRECT:
                type = HTTPProxy.TYPE.DIRECT;
                return HTTPProxy.parseHTTPProxy("direct://" + txtHost.getText());
            default:
                return null;

            }

            final HTTPProxy ret = new HTTPProxy(type, txtHost.getText(), Integer.parseInt(txtPort.getText().trim()));
            if (proxy != null) {
                ret.setPreferNativeImplementation(proxy.isPreferNativeImplementation());
            }
            ret.setPass(txtPass.getText());
            ret.setUser(txtUser.getText());
            return ret;
        } catch (final Throwable e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            return null;
        }
    }

    @Override
    public void dispose() {
        try {
            super.dispose();
            delayer.stop();
        } finally {
            try {
                executer.shutdown();
            } catch (final Throwable e) {
            }
        }
    }

    /**
     * @return
     */
    public String getHost() {
        // TODO Auto-generated method stub
        return txtHost.getText();
    }

    /**
     * @return
     */
    public String getPass() {
        // TODO txtP-generated method stub
        return txtPass.getText();
    }

    /**
     * @return
     */
    public int getPort() {
        try {
            return Integer.parseInt(txtPort.getText());
        } catch (final Exception e) {
            return -1;
        }
    }

    /**
     * @return
     */
    public HTTPProxy getProxy() {
        final HTTPProxy ret = new HTTPProxy(getType());
        ret.setHost(getHost());
        ret.setPort(getPort());
        if (proxy != null) {
            ret.setPreferNativeImplementation(proxy.isPreferNativeImplementation());
        }
        if (isAuthEnabled()) {
            ret.setUser(getUser());
            ret.setPass(getPass());
        }
        return ret;
    }

    /**
     * @return
     */
    public TYPE getType() {
        switch (((ProxyType) cmbType.getSelectedItem())) {
        case HTTP:
            // http
            return TYPE.HTTP;

        case SOCKS5:
            // socks5
            return TYPE.SOCKS5;
        case SOCKS4:
            // socks4
            return TYPE.SOCKS4;
        case DIRECT:
            if (StringUtils.isEmpty(txtHost.getText())) {
                return TYPE.NONE;
            } else {
                return TYPE.DIRECT;
            }
        }
        return TYPE.NONE;

    }

    /**
     * @return
     */
    public String getUser() {
        // TODO Auto-generated method stub
        return txtUser.getText();
    }

    /**
     * @return
     */
    private boolean isAuthEnabled() {

        return cbAuth.isSelected();
    }

    public boolean isAuthRequired() {
        return authRequired;
    }

    @Override
    public JComponent layoutDialogContent() {
        final JPanel panel = new JPanel(new MigLayout("ins 0, wrap 4", "[][grow 10,fill][][grow 3,fill]"));
        desc = new ExtTextArea();
        desc.setText(message);
        desc.setLabelMode(true);
        cmbType = new JComboBox();
        cmbType.setModel(createModel());
        final ListCellRenderer org = cmbType.getRenderer();
        cmbType.setRenderer(new ListCellRenderer() {

            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel ret = (JLabel) org.getListCellRendererComponent(list, ((ProxyType) value).getLabel(), index, isSelected, cellHasFocus);

                return ret;
            }
        });

        cmbType.addActionListener(this);
        lblHost = new JLabel(_AWU.T.ProxyDialog_hostport());
        desc.setFont(lblHost.getFont());
        txtHost = new ExtTextField() {
            @Override
            public void onChanged() {

                if (delayer != null) {
                    delayer.resetAndStart();
                }

            }

        };
        executer = Executors.newSingleThreadScheduledExecutor();
        delayer = new DelayedRunnable(executer, 2000) {

            @Override
            public void delayedrun() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        ProxyDialog.this.set(txtHost.getText());

                    }
                };

            }

            @Override
            public String getID() {
                return "ProxyDialog";
            }

        };
        txtHost.addCaretListener(this);
        lblPort = new JLabel(":");
        txtPort = new ExtTextField();

        txtPort.setText("8080");
        txtPort.addCaretListener(this);
        lblUser = new JLabel(_AWU.T.ProxyDialog_username());
        txtUser = new ExtTextField();

        lblPass = new JLabel(_AWU.T.ProxyDialog_password());
        txtPass = new ExtPasswordField();

        ;
        cbAuth = new ExtCheckBox(txtUser, lblPass, txtPass, lblUser);

        txtHost.setHelpText(_AWU.T.ProxyDialog_hostport_help());
        txtUser.setHelpText(_AWU.T.ProxyDialog_username_help());
        txtPass.setHelpText(_AWU.T.ProxyDialog_password_help());

        final JLabel lblCheckBox = new JLabel(_AWU.T.ProxyDialog_requires_auth());
        final MigPanel cbPanel = new MigPanel("ins 0", "[][grow]", "[]");

        cbPanel.add(cbAuth);
        cbPanel.add(lblCheckBox);
        // Layout#

        panel.add(desc, "spanx,pushx,growx,gapbottom 10");
        panel.add(new JLabel(_AWU.T.ProxyDialog_type()), "gapleft 10");
        panel.add(cmbType, "spanx");

        panel.add(lblHost, "gapleft 10");
        panel.add(txtHost);
        panel.add(lblPort);
        panel.add(txtPort, "shrinkx");
        panel.add(cbPanel, "spanx,gaptop 5,gapleft 5");
        panel.add(lblUser, "gapleft 10");
        panel.add(txtUser, "spanx");

        panel.add(lblPass, "gapleft 10");
        panel.add(txtPass, "spanx");

        // lblType.setEnabled(typeEditable);

        okButton.setEnabled(true);
        registerFocus(txtPort);
        registerFocus(txtUser);
        registerFocus(txtHost);
        // set(ClipboardMonitoring.getINSTANCE().getCurrentContent());
        this.set(proxy);
        cbAuth.setSelected(cbAuth.isSelected() || isAuthRequired());
        cbAuth.updateDependencies();
        return panel;
    }

    /**
     * @return
     */
    protected ComboBoxModel<ProxyType> createModel() {
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (ProxyType t : ProxyType.values()) {
            if (t != ProxyType.AUTO) {
                model.addElement(t);
            }
        }
        return model;
    }

    /**
     * @param txtPort2
     */
    private void registerFocus(final JTextField field) {
        field.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(final FocusEvent e) {
                field.selectAll();
            }

            @Override
            public void focusLost(final FocusEvent e) {
                // TODO Auto-generated method stub

            }
        });

    }

    /**
     * @param proxy2
     */
    protected void set(final HTTPProxy p) {
        if (p == null) {
            return;
        }
        new EDTRunner() {
            /*
             * (non-Javadoc)
             *
             * @see org.appwork.utils.swing.EDTRunner#runInEDT()
             */
            @Override
            protected void runInEDT() {
                txtUser.setText(p.getUser());
                cbAuth.setEnabled(true);
                switch (p.getType()) {

                case DIRECT:
                case NONE:

                    txtPort.setEnabled(false);
                    txtHost.setEnabled(false);
                    lblPort.setEnabled(false);
                    lblHost.setEnabled(false);
                    cbAuth.setEnabled(false);

                    cmbType.setSelectedItem(ProxyType.DIRECT);
                    txtHost.setText(p.getLocal());
                    break;
                case HTTP:

                    txtPort.setEnabled(portEditable);
                    txtHost.setEnabled(hostEditable);
                    lblPort.setEnabled(portEditable);
                    lblHost.setEnabled(hostEditable);
                    cmbType.setSelectedItem(ProxyType.HTTP);
                    txtHost.setText(p.getHost());
                    txtPort.setText(p.getPort() + "");
                    txtUser.setText(p.getUser());
                    txtPass.setText(p.getPass());
                    break;

                case SOCKS4:
                    txtPort.setEnabled(portEditable);
                    txtHost.setEnabled(hostEditable);
                    lblPort.setEnabled(portEditable);
                    lblHost.setEnabled(hostEditable);
                    cmbType.setSelectedItem(ProxyType.SOCKS4);
                    txtHost.setText(p.getHost());
                    txtPort.setText(p.getPort() + "");
                    break;
                case SOCKS5:
                    txtPort.setEnabled(portEditable);
                    txtHost.setEnabled(hostEditable);
                    lblPort.setEnabled(portEditable);
                    lblHost.setEnabled(hostEditable);
                    cmbType.setSelectedItem(ProxyType.SOCKS5);
                    txtHost.setText(p.getHost());
                    txtPort.setText(p.getPort() + "");
                    txtUser.setText(p.getUser());
                    txtPass.setText(p.getPass());
                    break;
                }
                cbAuth.setSelected(StringUtils.isNotEmpty(txtHost.getText()));
                cmbType.setEnabled(typeEditable);
            }
        };
    }

    protected void set(final String text) {

        final int carPos = txtHost.getCaretPosition();
        String myText = text;
        if (myText.endsWith(":")) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            try {
                final URL url = new URL(myText);
                txtHost.setText(url.getHost());
                if (url.getPort() > 0) {
                    txtPort.setText(url.getPort() + "");
                }
                final String userInfo = url.getUserInfo();
                if (userInfo != null) {
                    final int in = userInfo.indexOf(":");
                    if (in >= 0) {
                        txtUser.setText(userInfo.substring(0, in));
                        txtPass.setText(userInfo.substring(in + 1));
                    } else {
                        txtUser.setText(userInfo);
                    }
                }
                return;
            } catch (final MalformedURLException e) {
                if (text.contains(":")) {
                    myText = "http://" + myText;
                }
            }
        }

        txtHost.setCaretPosition(carPos);

    }

    /**
     * @param b
     */
    public void setAuthRequired(final boolean b) {
        authRequired = b;
        if (cbAuth != null) {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    cbAuth.setSelected(b);

                }
            };
        }

    }

    /**
     * @param b
     */
    public void setTypeEditable(final boolean b) {
        typeEditable = b;

    }

    /**
     * @param b
     */
    public void setHostEditable(final boolean b) {
        hostEditable = b;

    }

    /**
     * @param b
     */
    public void setPortEditable(final boolean b) {
        portEditable = b;

    }

}
