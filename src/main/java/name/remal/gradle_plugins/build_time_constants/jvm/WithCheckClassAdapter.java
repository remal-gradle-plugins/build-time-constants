package name.remal.gradle_plugins.build_time_constants.jvm;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * {@link CheckClassAdapter} is an optional dependency, so it's usage should be in a separate class.
 */
@NoArgsConstructor(access = PRIVATE)
abstract class WithCheckClassAdapter {

    public static ClassVisitor withCheckClassAdapter(ClassVisitor classVisitor) {
        return new CheckClassAdapter(classVisitor);
    }

}
