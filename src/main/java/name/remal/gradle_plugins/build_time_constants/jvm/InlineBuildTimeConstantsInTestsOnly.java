package name.remal.gradle_plugins.build_time_constants.jvm;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * This annotation is supposed to be used for unit tests of this plugin only.
 * Do not use it unless you know what you're doing!
 */
@Retention(CLASS)
@Target(TYPE)
@VisibleForTesting
@interface InlineBuildTimeConstantsInTestsOnly {
}
