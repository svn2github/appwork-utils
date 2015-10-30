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
package org.appwork.remotecall.server;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.appwork.remotecall.RemoteCallInterface;
import org.appwork.remotecall.ResponseAlreadySentException;
import org.appwork.remotecall.Utils;
import org.appwork.remotecall.client.MethodHandler;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;


@Deprecated
public class RemoteCallServer {

    private final HashMap<String, RemoteCallServiceWrapper> servicesMap;

    @Deprecated
    public RemoteCallServer() {

        servicesMap = new HashMap<String, RemoteCallServiceWrapper>();
    }

    public <T extends RemoteCallInterface> void addHandler(final Class<T> class1, final T serviceImpl) throws ParsingException {

        if (servicesMap.containsKey(class1.getSimpleName())) { throw new IllegalArgumentException("Service " + class1 + " already exists"); }
        servicesMap.put(class1.getSimpleName(), new RemoteCallServiceWrapper(class1, serviceImpl));
    }

    /**
     * IMPORTANT: parameters must be urldecoded!!
     * 
     * @param requestor
     * @param clazz
     * @param method
     * @param parameters
     * @return
     * @throws ServerInvokationException
     */
    protected Object handleRequestReturnData(final Requestor requestor, final String clazz, final String method, final String[] parameters) throws ServerInvokationException {
        try {
            final RemoteCallServiceWrapper service = servicesMap.get(clazz);
            if (service == null) { //
                throw new ServerInvokationException(handleRequestError(requestor, new BadRequestException("Service not defined: " + clazz)), requestor);

            }
            // find method

            final MethodHandler m = service.getHandler(method);
            if (m == null) { throw new ServerInvokationException(handleRequestError(requestor, new BadRequestException("Routine not defined: " + method)), requestor); }

            final TypeRef<Object>[] types = m.getTypeRefs();
            if (types.length != parameters.length) {
                //
                throw new ServerInvokationException(handleRequestError(requestor, new BadRequestException("parameters did not match " + method)), requestor);
            }

            final Object[] params = new Object[types.length];
            try {
                for (int i = 0; i < types.length; i++) {
                    // parameters should already be urldecoded here
                    if (types[i].getType() == Requestor.class) {
                        params[i] = requestor;
                    } else {
                        if (types[i].getType() == String.class && !"null".equals(parameters[i])) {
                            // fix if there is no " around strings
                            if (!parameters[i].startsWith("\"")) {
                                parameters[i] = "\"" + parameters[i];
                            }
                            if (!parameters[i].endsWith("\"") || parameters[i].length() == 1) {
                                parameters[i] += "\"";
                            }
                        }
                        params[i] = Utils.convert(JSonStorage.restoreFromString(parameters[i], types[i]), types[i].getType());
                    }
                }

            } catch (final Exception e) {
                throw new ServerInvokationException(handleRequestError(requestor, new BadRequestException("Parameter deserialize error for " + method)), requestor);
            }

            Object answer;

            answer = service.call(m.getMethod(), params);
            return answer;

        } catch (final InvocationTargetException e1) {

            final Throwable cause = e1.getCause();
            if (cause != null) {
                if (cause instanceof ResponseAlreadySentException) { throw (ResponseAlreadySentException) cause; }
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e1);
                throw new ServerInvokationException(handleRequestError(requestor, cause), requestor);
            }
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e1);
            throw new ServerInvokationException(handleRequestError(requestor, new RuntimeException(e1)), requestor);
        } catch (final ServerInvokationException e) {
            throw e;
        } catch (final Throwable e) {
            throw new ServerInvokationException(handleRequestError(requestor, e), requestor);
        }

    }

    public HashMap<String, RemoteCallServiceWrapper> getServicesMap() {
        return servicesMap;
    }

    protected String handleRequest(final Requestor requestor, final String clazz, final String method, final String[] parameters) throws ServerInvokationException {

        return JSonStorage.serializeToJson(handleRequestReturnData(requestor, clazz, method, parameters));

    }

    protected String handleRequestError(final Requestor requestor, final Throwable e) {
        // TODO byte[]-generated method stub
        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        final StringBuilder sb = new StringBuilder();

        try {
            sb.append(JSonStorage.serializeToJson(new ExceptionWrapper(e)));
        } catch (final Throwable e1) {

            // TODO Auto-generated catch block
            // TODO: URLENCODE here
            e1.printStackTrace();
            sb.append("{\"name\":\"java.lang.Exception\",\"exception\":{\"cause\":null,\"message\":\"Serialize Problem: ");
            sb.append(e1.getMessage());
            sb.append(e1.getLocalizedMessage());
            sb.append("\",\"stackTrace\":[]}}");

        }

        return sb.toString();
    }
}
