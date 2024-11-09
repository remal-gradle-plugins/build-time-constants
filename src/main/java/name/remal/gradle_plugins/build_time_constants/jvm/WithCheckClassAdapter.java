package name.remal.gradle_plugins.build_time_constants.jvm;

import static lombok.AccessLevel.PRIVATE;

import java.io.PrintWriter;
import lombok.NoArgsConstructor;
import lombok.val;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 * {@link CheckClassAdapter} is an optional dependency, so it's usage should be in a separate class.
 */
@NoArgsConstructor(access = PRIVATE)
abstract class WithCheckClassAdapter {

    private static final boolean TRACE = false;

    @SuppressWarnings({"java:S106", "DefaultCharset"})
    public static ClassVisitor withCheckClassAdapter(ClassVisitor classVisitor) {
        if (TRACE) {
            val writer = new PrintWriter(System.out, true);
            classVisitor = new TraceClassVisitor(classVisitor, writer);
        }

        classVisitor = new CheckClassAdapter(classVisitor);

        return classVisitor;
    }

}
