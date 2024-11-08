package name.remal.gradle_plugins.build_time_constants;

import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public abstract class BuildTimeConstantsBasePlugin implements Plugin<Project> {

    public static final String BUILD_TIME_CONSTANTS_EXTENSION_NAME = doNotInline("buildTimeConstants");

    @Override
    public void apply(Project project) {
        project.getExtensions().create(BUILD_TIME_CONSTANTS_EXTENSION_NAME, BuildTimeConstantsExtension.class);
    }

}
