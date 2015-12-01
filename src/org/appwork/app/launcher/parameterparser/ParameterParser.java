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
package org.appwork.app.launcher.parameterparser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import org.appwork.utils.IO;
import org.appwork.utils.event.Eventsender;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.parser.ShellParser;

/**
 * This class is used to parse and evaluate Startparameters
 *
 * @author $Author: unknown $
 *
 */
public class ParameterParser {
    /**
     * Stores the Applications startParameters
     */
    private String[]                                                rawArguments;
    /**
     * The eventsenderobjekt is used to add Listenersupport to this class.
     */
    private final Eventsender<CommandSwitchListener, CommandSwitch> eventSender;
    private HashMap<String, CommandSwitch>                          map;
    private LogSource                                               logger;
    private ArrayList<CommandSwitch>                                list;

    public ParameterParser(final String[] args) {
        logger = LoggerFactory.I().getLogger(getClass().getSimpleName());
        rawArguments = args;
        eventSender = new Eventsender<CommandSwitchListener, CommandSwitch>() {

            @Override
            protected void fireEvent(final CommandSwitchListener listener, final CommandSwitch event) {
                listener.executeCommandSwitch(event);

            }

        };
    }

    /**
     * @param string
     * @return
     */
    public CommandSwitch getCommandSwitch(final String string) {
        return map.get(string);
    }

    /**
     * @return the {@link ParameterParser#eventSender}
     * @see ParameterParser#eventSender
     */
    public Eventsender<CommandSwitchListener, CommandSwitch> getEventSender() {
        return eventSender;
    }

    /**
     * @return
     */
    public String[] getRawArguments() {
        // TODO Auto-generated method stub
        return rawArguments;
    }

    public void setRawArguments(final String[] rawArguments) {
        this.rawArguments = rawArguments;
    }

    /**
     * @param string
     * @return
     */
    public boolean hasCommandSwitch(final String string) {
        return map.containsKey(string);
    }

    public HashMap<String, CommandSwitch> getMap() {
        return map;
    }

    /**
     * parses the command row. and fires {@link CommandSwitch} for each switch command
     *
     * @param commandFilePath
     *            TODO
     * @return
     */
    public ParameterParser parse(final File file) {

        map = new HashMap<String, CommandSwitch>();
        list = new ArrayList<CommandSwitch>();
        this.parse(rawArguments);
        if (file != null && file.exists()) {

            try {
                this.parse(ShellParser.splitCommandString(IO.readFileToString(file).replaceAll("[\r\n]", " ")).toArray(new String[] {}));
            } catch (final IOException e) {
                logger.log(e);
            }
        }

        return this;
    }

    /**
     * @param startArguments2
     */
    private void parse(final String[] startArguments) {

        String switchCommand = null;
        final java.util.List<String> params = new ArrayList<String>();
        for (String var : startArguments) {
            if (var.startsWith("-")) {
                while (var.length() > 0 && var.startsWith("-")) {
                    var = var.substring(1);
                }
                if (switchCommand != null || params.size() > 0) {
                    CommandSwitch cs;
                    if (switchCommand != null) {
                        switchCommand = switchCommand.toLowerCase(Locale.ENGLISH);
                    }
                    getEventSender().fireEvent(cs = new CommandSwitch(switchCommand, params.toArray(new String[] {})));
                    map.put(switchCommand, cs);
                    list.add(cs);
                }
                switchCommand = var;

                params.clear();
            } else {
                params.add(var);
            }
        }
        if (switchCommand != null || params.size() > 0) {
            CommandSwitch cs;
            if (switchCommand != null) {
                switchCommand = switchCommand.toLowerCase(Locale.ENGLISH);
            }
            getEventSender().fireEvent(cs = new CommandSwitch(switchCommand, params.toArray(new String[] {})));
            map.put(switchCommand, cs);
            list.add(cs);
        }
    }

    public ArrayList<CommandSwitch> getList() {
        return list;
    }

    public void setList(ArrayList<CommandSwitch> list) {
        this.list = list;
    }

}
