package name.remal.gradle_plugins.build_time_constants;

import static java.util.Arrays.stream;

import java.util.Map;
import java.util.Objects;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.jspecify.annotations.Nullable;

public abstract class BuildTimeConstantsExtension {

    public abstract MapProperty<String, Object> getProperties();

    public void properties(Action<? super MapProperty<String, Object>> action) {
        action.execute(getProperties());
    }

    @SuppressWarnings("java:S2259")
    public void properties(Map<String, @Nullable Object> properties) {
        properties.forEach(this::property);
    }

    public void property(String name, @Nullable Object value) {
        if (value != null) {
            getProperties().put(name, value);
        }
    }


    public abstract ListProperty<Object> getCompilationDependencies();

    /**
     * Will call {@link Task#dependsOn(Object...)} on every JVM compilation task of the project.
     */
    @SuppressWarnings({"java:S2259", "java:S2583", "ConstantValue"})
    public void dependOn(@Nullable Object... dependencies) {
        if (dependencies == null) {
            return;
        }

        stream(dependencies)
            .filter(Objects::nonNull)
            .forEach(getCompilationDependencies()::add);
    }

}
