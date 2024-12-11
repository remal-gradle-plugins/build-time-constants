package name.remal.gradle_plugins.build_time_constants.jvm;

import static name.remal.gradle_plugins.toolkit.PluginUtils.findPluginIdFor;

import lombok.CustomLog;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

@CustomLog
public abstract class BuildTimeConstantsJvmDeprecatedPlugin implements Plugin<Project> {

    @Override
    @SuppressWarnings("java:S2629")
    public void apply(Project project) {
        project.getLogger().warn(
            "Use `{}` plugin instead of deprecated `{}`",
            findPluginIdFor(BuildTimeConstantsJvmPlugin.class),
            findPluginIdFor(BuildTimeConstantsJvmDeprecatedPlugin.class)
        );

        project.getPluginManager().apply(BuildTimeConstantsJvmPlugin.class);
    }

}
