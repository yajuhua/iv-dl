package io.github.yajuhua.invidious.dlj.command.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CmdOption {
    /**
     * eg. "option" for "--option"
     */
    String value() default "";

    String shortForm() default "";
    
    /**
     * eg. "this '--option <value>' takes an integer value"
     */
    String description() default "";

    /**
     * 默认情况下选项是不会传递到yt-dlp
     */
    boolean filter() default false;
}
