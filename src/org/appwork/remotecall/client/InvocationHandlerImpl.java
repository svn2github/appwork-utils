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
package org.appwork.remotecall.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;

import org.appwork.remotecall.RemoteCallInterface;
import org.appwork.remotecall.Utils;
import org.appwork.remotecall.server.ExceptionWrapper;
import org.appwork.remotecall.server.ParsingException;
import org.appwork.remotecall.server.RemoteCallException;
import org.appwork.remotecall.server.ServerInvokationException;
import org.appwork.storage.InvalidTypeException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.InterfaceParseException;

import org.appwork.utils.reflection.Clazz;

/**
 * @author thomas
 * 
 */
public class InvocationHandlerImpl<T extends RemoteCallInterface> implements InvocationHandler {

    private final RemoteCallClient         client;

    private final String                   name;
    private Class<T>                       interfaceClass;
    private HashMap<String, MethodHandler> handler;

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
     * java.lang.reflect.Method, java.lang.Object[])
     */
    /**
     * @param client
     * @param class1
     * @throws ParsingException
     */
    public InvocationHandlerImpl(final RemoteCallClient client, final Class<T> class1) throws ParsingException {
        this.client = client;
        this.name = class1.getSimpleName();
        interfaceClass = class1;

        parse();

    }

    /**
     * 
     */
    private void parse() {

        Class<?> clazz = this.interfaceClass;
        this.handler = new HashMap<String, MethodHandler>();
        final HashSet<String> dupe = new HashSet<String>();
        while (clazz != null && clazz != RemoteCallInterface.class) {
            for (final Method m : clazz.getDeclaredMethods()) {

                if (!dupe.add(m.getName())) { throw new InterfaceParseException("Method " + m.getName() + " is avlailable twice in " + clazz); }
                try {
                    if (m.getGenericReturnType() != void.class) {
                        JSonStorage.canStore(m.getGenericReturnType(), false);
                    }
                    for (final Type t : m.getGenericParameterTypes()) {
                        JSonStorage.canStore(t, false);
                    }
                } catch (final InvalidTypeException e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                    throw new InterfaceParseException("Json Serialize not possible for " + m);
                }
                for (final Class<?> e : m.getExceptionTypes()) {
                    if (!RemoteCallException.class.isAssignableFrom(e)) {
                        //

                        throw new InterfaceParseException(m + " exceptions do not extend RemoteCallException");
                    }
                    try {
                        e.getConstructors();

                    } catch (final Throwable e1) {
                        throw new InterfaceParseException(e + " no accessable null constructor available");
                    }
                }
                handler.put(m.getName(), new MethodHandler(m));
            }

            // run down the calss hirarchy to find all methods. getMethods does
            // not work, because it only finds public methods
            final Class<?>[] interfaces = clazz.getInterfaces();
            clazz = interfaces[0];

        }

    }

    public final Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {

        Object returnValue;
        Object obj;

        try {

            returnValue = this.client.call(this.name, method, args);

            if (Clazz.isVoid(method.getGenericReturnType())) {
                return null;
            } else if (returnValue instanceof byte[] && Clazz.isByteArray(method.getGenericReturnType())) {
                return returnValue;
            } else {
                final TypeRef<Object> tr = new TypeRef<Object>(method.getGenericReturnType()) {
                };

                obj = JSonStorage.restoreFromString((String) returnValue, tr, null);
                return Utils.convert(obj, tr.getType());
            }

        } catch (final ServerInvokationException e) {

            final ExceptionWrapper exception = JSonStorage.restoreFromString(e.getMessage(), ExceptionWrapper.class);
            final Throwable ex = exception.deserialiseException();
            // search to add the local cause
            final StackTraceElement[] localStack = new Exception().getStackTrace();
            final StackTraceElement[] newStack = new StackTraceElement[localStack.length - 1];
            System.arraycopy(localStack, 2, newStack, 0, localStack.length - 2);
            newStack[newStack.length - 1] = new StackTraceElement("RemoteCallClient via", e.getRemoteID() + "", null, 0);
            ex.setStackTrace(newStack);

            throw ex;
        }

    }

    /**
     * @return
     * 
     */
    public HashMap<String, MethodHandler> getHandler() {
        return handler;

    }
}
