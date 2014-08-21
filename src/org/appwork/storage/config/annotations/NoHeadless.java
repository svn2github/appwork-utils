package org.appwork.storage.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * use this annotation for configinterface entries which should be available in
 * the aboutconfig, but only in non-jared IDE mode
 * 
 * @author thomas
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface NoHeadless {

}
