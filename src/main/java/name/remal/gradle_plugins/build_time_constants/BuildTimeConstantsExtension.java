package name.remal.gradle_plugins.build_time_constants;

import java.util.Map;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import org.gradle.api.Action;
import org.gradle.api.provider.MapProperty;

@Getter
@Setter
public abstract class BuildTimeConstantsExtension {

    public abstract MapProperty<String, Object> getProperties();

    public void properties(Action<? super MapProperty<String, Object>> action) {
        action.execute(getProperties());
    }

    public void properties(Map<String, Object> properties) {
        properties.forEach(this::property);
    }

    public void property(String name, @Nullable Object value) {
        if (value != null) {
            getProperties().put(name, value);
        }
    }

}
