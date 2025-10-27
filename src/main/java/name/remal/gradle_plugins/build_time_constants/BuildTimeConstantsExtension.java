package name.remal.gradle_plugins.build_time_constants;

import java.util.Map;
import org.gradle.api.Action;
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

}
