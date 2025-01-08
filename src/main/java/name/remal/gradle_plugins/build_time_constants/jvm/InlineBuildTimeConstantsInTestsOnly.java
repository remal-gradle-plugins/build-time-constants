package name.remal.gradle_plugins.build_time_constants.jvm;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * @deprecated Use {@link name.remal.gradle_plugins.build_time_constants.InlineBuildTimeConstantsInTestsOnly} instead.
 */
@Deprecated
@Retention(CLASS)
@Target(TYPE)
@VisibleForTesting
public @interface InlineBuildTimeConstantsInTestsOnly {
}
