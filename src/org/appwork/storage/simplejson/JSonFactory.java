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
package org.appwork.storage.simplejson;

/**
 * @author thomas
 */
public class JSonFactory {
    public static boolean       DEBUG  = false;
    private int                 global = 0;
    private char                c;
    private final String        str;
    final StringBuilder         sb;
    private final StringBuilder sb2;
    private int                 counter;
    private String              debug;

    public JSonFactory(final String json) {
        str = json;
        sb = new StringBuilder();
        sb2 = new StringBuilder();
        counter = 0;
    }

    private ParserException bam(final String expected) {
        String pre = str.substring(Math.max(global - 20, 0), global);
        pre = pre.replace("\r", "\\r").replace("\n", "\\n");
        final StringBuilder sb = new StringBuilder();
        sb.append(expected);
        sb.append("\r\n\t");
        sb.append(pre);
        sb.append(str.substring(global, Math.min(str.length(), global + 20)));
        sb.append("\r\n\t");
        for (int i = 1; i < pre.length(); i++) {
            sb.append("-");
        }
        sb.append('|');
        return new ParserException(sb.toString());
    }

    private String findString() throws ParserException {
        // string
        try {
            sb.delete(0, sb.length());
            c = str.charAt(global++);
            if (c != '\"') {
                throw bam("'\"' expected");
            }
            boolean escaped = false;
            while (true) {
                c = str.charAt(global++);
                switch (c) {
                case '\"':
                    return sb.toString();
                case '\\':
                    escaped = true;
                    while ((c = str.charAt(global++)) == '\\') {
                        escaped = !escaped;
                        if (!escaped) {
                            sb.append("\\");
                        }
                    }
                    if (escaped) {
                        switch (c) {
                        case '"':
                        case '/':
                            sb.append(c);
                            continue;
                        case 'r':
                            sb.append('\r');
                            continue;
                        case 'n':
                            sb.append('\n');
                            continue;
                        case 't':
                            sb.append('\t');
                            continue;
                        case 'f':
                            sb.append('\f');
                            continue;
                        case 'b':
                            sb.append('\b');
                            continue;
                        case 'u':
                            sb2.delete(0, sb2.length());
                            // this.global++;
                            counter = global + 4;
                            for (; global < counter; global++) {
                                c = getChar();
                                if (sb2.length() > 0 || c != '0') {
                                    sb2.append(c);
                                }
                            }
                            // this.global--;
                            if (sb2.length() == 0) {
                                sb.append((char) 0);
                            } else {
                                sb.append((char) Integer.parseInt(sb2.toString(), 16));
                            }
                            continue;
                        default:
                            throw bam("illegal escape char");
                        }
                    } else {
                        global--;
                    }
                    break;
                default:
                    sb.append(c);
                }
            }
        } catch (final StringIndexOutOfBoundsException e) {
            global--;
            throw bam("Unexpected End of String \"" + sb.toString());
        }
    }

    private char getChar() throws ParserException {
        if (JSonFactory.DEBUG) {
            final String pos = str.substring(0, global);
            debug = pos + str.substring(global) + "\r\n";
            for (int i = 0; i < pos.length(); i++) {
                debug += "-";
            }
            debug += '\u2934';
            System.err.println(debug);
        }
        if (global >= str.length()) {
            throw bam("Ended unexpected");
        }
        return str.charAt(global);
    }

    public JSonNode parse() throws ParserException {
        final JSonNode ret = parseValue();
        skipWhiteSpace();
        if (global != str.length()) {
            global++;
            throw bam("Unexpected End of JSonString");
        }
        return ret;
    }

    private JSonArray parseArray() throws ParserException {
        global++;
        final JSonArray ret = new JSonArray();
        while (true) {
            // skip whitespace
            skipWhiteSpace();
            c = getChar();
            switch (c) {
            case ']':
                global++;
                return ret;
            case ',':
                throw bam("Value missing");
            default:
                ret.add(parseValue());
                skipWhiteSpace();
                c = getChar();
                switch (c) {
                case ',':
                    // ok another round:
                    global++;
                    continue;
                case ']':
                    // end
                    global++;
                    return ret;
                default:
                    throw bam("']' or ',' expected");
                }
            }
        }
    }

    private JSonValue parseNumber() throws ParserException, NoNumberException {
        sb.delete(0, sb.length());
        boolean pointFound = false;
        boolean potFound = false;
        c = getChar();
        if (c == '+' || c == '-' || Character.isDigit(c)) {
            sb.append(c);
            while (global + 1 < str.length()) {
                global++;
                c = getChar();
                if (Character.isDigit(c) || !pointFound && c == '.' || pointFound && c == 'e' || pointFound && c == 'E' || potFound && c == '+' || potFound && c == '-') {
                    if (c == '.') {
                        pointFound = true;
                    } else if (pointFound && (c == 'e' || c == 'E')) {
                        potFound = true;
                    }
                    sb.append(c);
                } else {
                    global--;
                    break;
                }
            }
            global++;
            if (pointFound) {
                return new JSonValue(Double.parseDouble(sb.toString()));
            } else {
                return new JSonValue(Long.parseLong(sb.toString()));
            }
        } else {
            throw new NoNumberException();
        }
    }

    private JSonObject parseObject() throws ParserException {
        String key;
        global++;
        final JSonObject ret = new JSonObject();
        skipWhiteSpace();
        c = getChar();
        if (c == '}') {
            global++;
            return ret;
        }
        while (true) {
            // check for object end markers
            bs: switch (c) {
            case '"':
                key = findString();
                skipWhiteSpace();
                c = getChar();
                if (c != ':') {
                    throw bam("':' expected");
                }
                global++;
                skipWhiteSpace();
                ret.put(key, parseValue());
                skipWhiteSpace();
                if (global >= str.length()) {
                    throw bam("} or , expected");
                }
                c = getChar();
                switch (c) {
                case ',':
                    // ok another value...probably
                    global++;
                    break bs;
                case '}':
                    // end of object:
                    global++;
                    return ret;
                default:
                    throw bam(", or }' expected");
                }
            default:
                throw bam("\" expected");
            }
            skipWhiteSpace();
            c = getChar();
        }
    }

    private JSonValue parseString() throws ParserException {
        return new JSonValue(findString());
    }

    private JSonNode parseValue() throws ParserException {
        global = skipWhiteSpace();
        switch (getChar()) {
        case '{':
            return parseObject();
        case '[':
            return parseArray();
        case 'n':
            // null
            global += 4;
            return new JSonValue(null);
        case 't':
            // true;
            global += 4;
            return new JSonValue(true);
        case 'f':
            // false;
            global += 5;
            return new JSonValue(false);
        case '"':
            return parseString();
        }
        try {
            return parseNumber();
        } catch (final NoNumberException e) {
            global++;
            throw bam("Illegal Char");
        }
    }

    private int skipWhiteSpace() {
        while (global < str.length()) {
            if (!Character.isWhitespace(str.charAt(global++))) {
                global--;
                break;
            }
        }
        return global;
    }

    /**
     * @param jsonString
     * @return
     * @throws ParserException
     */
    public static String decodeJavaScriptString(String str) throws ParserException {
        if (str == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        boolean escaped = false;
        char c;
        int global = 0;
        int counter = 0;
        c = str.charAt(global++);
        if (c != '\"') {
            throw new ParserException("'\"' expected");
        }
        sb.append("\"");
        while (global < str.length()) {
            c = str.charAt(global++);
            switch (c) {
            case '\"':
                sb.append("\"");
                return sb.toString();
            case '\\':
                escaped = true;
                while ((c = str.charAt(global++)) == '\\') {
                    escaped = !escaped;
                    if (!escaped) {
                        sb.append("\\");
                    }
                }
                if (escaped) {
                    switch (c) {
                    // case '"':
                    // case '/':
                    // sb.append(c);
                    // continue;
                    // case 'r':
                    // sb.append('\r');
                    // continue;
                    // case 'n':
                    // sb.append('\n');
                    // continue;
                    // case 't':
                    // sb.append('\t');
                    // continue;
                    // case 'f':
                    // sb.append('\f');
                    // continue;
                    // case 'b':
                    // sb.append('\b');
                    // continue;
                    case 'x':
                        sb2.delete(0, sb2.length());
                        // this.global++;
                        counter = global + 2;
                        for (; global < counter; global++) {
                            c = str.charAt(global);
                            if (sb2.length() > 0 || c != '0') {
                                sb2.append(c);
                            }
                        }
                        // this.global--;
                        if (sb2.length() == 0) {
                            sb.append((char) 0);
                        } else {
                            sb.append("\\").append((char) Short.parseShort(sb2.toString(), 16));
                        }
                        continue;
                    default:
                    }
                } else {
                    global--;
                }
                break;
            default:
                sb.append(c);
            }
        }
        throw new ParserException("Unfinished String");
    }
}
