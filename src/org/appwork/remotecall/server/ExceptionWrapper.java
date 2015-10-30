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

import java.io.IOException;
import java.lang.reflect.Constructor;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.SimpleMapper;
import org.appwork.storage.Storable;
import org.appwork.storage.simplejson.Ignores;


/**
 * @author thomas
 */
@Ignores({ "" })
public class ExceptionWrapper implements Storable {

    private String _exception;

    private String name;

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private ExceptionWrapper() {
        // we need this for JSON Serial.
    }

    private static final SimpleMapper JSON_MAPPER = new SimpleMapper();

    public ExceptionWrapper(final Throwable e) throws IOException {
        if (e instanceof RemoteCallException) {
            this._exception = JSON_MAPPER.objectToString(e);
        } else {
            if(e.getStackTrace().length>0){
                message = e.getMessage()+ " @"+e.getStackTrace()[0].getClassName()+"."+e.getStackTrace()[0].getMethodName()+"("+e.getStackTrace()[0].getFileName()+":"+e.getStackTrace()[0].getLineNumber()+")";  
            }else{
            message = e.getMessage();
            }
        }

        this.name = e.getClass().getName();
    }

    public Throwable deserialiseException() throws ClassNotFoundException, IOException {
        // tries to cast to the correct exception
        final Class<?> clazz = Class.forName(this.name);
        if (_exception != null) {

            return (Throwable) JSonStorage.restoreFromString(this._exception, clazz);
        } else {
            try {
                Constructor<?> c = clazz.getConstructor(new Class[] { String.class });
                Object ret = c.newInstance(new Object[] { message });
                return (Throwable) ret;
            } catch (Throwable e) {

                try {
                    return (RuntimeException) clazz.newInstance();
                } catch (InstantiationException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (IllegalAccessException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

            }

            return new RuntimeException(name);
        }
    }

    public String getException() {
        return this._exception;
    }

    public String getName() {
        return this.name;
    }

    public void setException(final String _exception) {
        this._exception = _exception;
    }

    public void setName(final String name) {
        this.name = name;
    }

}
