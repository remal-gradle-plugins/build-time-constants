package name.remal.gradle_plugins.build_time_constants;

import org.jetbrains.annotations.ApiStatus.Internal;

public class BuildTimeConstantsException extends RuntimeException {

    @Internal
    public BuildTimeConstantsException(String message) {
        super(message);
    }

    @Internal
    public BuildTimeConstantsException(String message, Throwable cause) {
        super(message, cause);
    }

}
