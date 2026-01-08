package name.remal.gradle_plugins.build_time_constants.api;

import com.google.errorprone.annotations.CompileTimeConstant;
import java.util.Map;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class BuildTimeConstants {

    /**
     * Invocation of this method will be replaced with actual class name.
     *
     * <p><b>Class parameter must be a constant expression.</b>
     */
    @NotNull
    @Contract(pure = true)
    public static String getClassName(@NotNull @CompileTimeConstant Class<?> clazz) {
        throw illegalMethodUseException();
    }

    /**
     * Invocation of this method will be replaced with actual class simple name.
     *
     * <p><b>Class parameter must be a constant expression.</b>
     */
    @NotNull
    @Contract(pure = true)
    public static String getClassSimpleName(@NotNull @CompileTimeConstant Class<?> clazz) {
        throw illegalMethodUseException();
    }

    /**
     * Invocation of this method will be replaced with actual class simple name.
     *
     * <p><b>Class parameter must be a constant expression.</b>
     */
    @NotNull
    @Contract(pure = true)
    public static String getClassPackageName(@NotNull @CompileTimeConstant Class<?> clazz) {
        throw illegalMethodUseException();
    }

    /**
     * Invocation of this method will be replaced with actual class internal name.
     *
     * <p><b>Class parameter must be a constant expression.</b>
     */
    @NotNull
    @Contract(pure = true)
    public static String getClassInternalName(@NotNull @CompileTimeConstant Class<?> clazz) {
        throw illegalMethodUseException();
    }

    /**
     * Invocation of this method will be replaced with actual class descriptor.
     *
     * <p><b>Class parameter must be a constant expression.</b>
     */
    @NotNull
    @Contract(pure = true)
    public static String getClassDescriptor(@NotNull @CompileTimeConstant Class<?> clazz) {
        throw illegalMethodUseException();
    }


    /**
     * Invocation of this method will be replaced with actual property value.
     *
     * <p><b>Property name parameter must be a constant expression.</b>
     */
    @NotNull
    @Contract(pure = true)
    public static String getStringProperty(@NotNull @CompileTimeConstant String propertyName) {
        throw illegalMethodUseException();
    }

    /**
     * Invocation of this method will be replaced with actual property value parsed as int.
     *
     * <p><b>Property name parameter must be a constant expression.</b>
     */
    @Contract(pure = true)
    public static int getIntegerProperty(@NotNull @CompileTimeConstant String propertyName) {
        throw illegalMethodUseException();
    }

    /**
     * Invocation of this method will be replaced with actual property value parsed as long.
     *
     * <p><b>Property name parameter must be a constant expression.</b>
     */
    @Contract(pure = true)
    public static long getLongProperty(@NotNull @CompileTimeConstant String propertyName) {
        throw illegalMethodUseException();
    }

    /**
     * Invocation of this method will be replaced with actual property value parsed as boolean.
     *
     * <p><b>Property name parameter must be a constant expression.</b>
     */
    @Contract(pure = true)
    public static boolean getBooleanProperty(@NotNull @CompileTimeConstant String propertyName) {
        throw illegalMethodUseException();
    }


    /**
     * Invocation of this method will be replaced with actual properties values.
     *
     * <p>Property name pattern can contain <code>*</code> char, which is treated as any number of any characters.
     * So, <code>prefix.*</code> matches <code>prefix.</code>, <code>prefix.name</code>,
     * and <code>prefix.name.sub-name</code>.
     *
     * <p><b>Property name pattern parameter must be a constant expression.</b>
     */
    @NotNull
    @Contract(pure = true)
    public static Map<@NotNull String, @NotNull String> getStringProperties(@NotNull @CompileTimeConstant String propertyNamePattern) {
        throw illegalMethodUseException();
    }

    /**
     * Invocation of this method will be replaced with actual properties values parsed as int.
     *
     * <p>Property name pattern can contain <code>*</code> char, which is treated as any number of any characters.
     * So, <code>prefix.*</code> matches <code>prefix.</code>, <code>prefix.name</code>,
     * and <code>prefix.name.sub-name</code>.
     *
     * <p><b>Property name pattern parameter must be a constant expression.</b>
     */
    @NotNull
    @Contract(pure = true)
    public static Map<@NotNull String, @NotNull Integer> getIntegerProperties(@NotNull @CompileTimeConstant String propertyNamePattern) {
        throw illegalMethodUseException();
    }

    /**
     * Invocation of this method will be replaced with actual properties values parsed as long.
     *
     * <p>Property name pattern can contain <code>*</code> char, which is treated as any number of any characters.
     * So, <code>prefix.*</code> matches <code>prefix.</code>, <code>prefix.name</code>,
     * and <code>prefix.name.sub-name</code>.
     *
     * <p><b>Property name pattern parameter must be a constant expression.</b>
     */
    @NotNull
    @Contract(pure = true)
    public static Map<@NotNull String, @NotNull Long> getLongProperties(@NotNull @CompileTimeConstant String propertyNamePattern) {
        throw illegalMethodUseException();
    }

    /**
     * Invocation of this method will be replaced with actual properties values parsed as boolean.
     *
     * <p>Property name pattern can contain <code>*</code> char, which is treated as any number of any characters.
     * So, <code>prefix.*</code> matches <code>prefix.</code>, <code>prefix.name</code>,
     * and <code>prefix.name.sub-name</code>.
     *
     * <p><b>Property name pattern parameter must be a constant expression.</b>
     */
    @NotNull
    @Contract(pure = true)
    public static Map<@NotNull String, @NotNull Boolean> getBooleanProperties(@NotNull @CompileTimeConstant String propertyNamePattern) {
        throw illegalMethodUseException();
    }


    private static Error illegalMethodUseException() {
        throw new Error("This method can't be invoked directly."
            + " Use `name.remal.build-time-constants` Gradle plugin to process invocations of the method.");
    }


    private BuildTimeConstants() {
        throwIllegalClassUseException();
    }

    private static void throwIllegalClassUseException() {
        throw new Error("This class can't be used directly."
            + " Use `name.remal.build-time-constants` Gradle plugin to handle the class's methods.");
    }

}
