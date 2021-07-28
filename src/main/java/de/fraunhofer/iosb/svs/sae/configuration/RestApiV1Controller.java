package de.fraunhofer.iosb.svs.sae.configuration;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
@Target(TYPE)
@RestController
@RequestMapping("/api/v1")
public @interface RestApiV1Controller {
    @AliasFor(annotation = RestController.class)
    String value() default "";
}
