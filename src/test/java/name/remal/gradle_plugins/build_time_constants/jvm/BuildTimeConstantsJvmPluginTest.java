package name.remal.gradle_plugins.build_time_constants.jvm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.build_time_constants.BuildTimeConstantsBasePlugin;
import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class BuildTimeConstantsJvmPluginTest {

    final Project project;

    @BeforeEach
    void beforeEach() {
        project.getPluginManager().apply(BuildTimeConstantsJvmPlugin.class);
    }

    @Test
    void basePluginIsApplied() {
        assertTrue(project.getPlugins().hasPlugin(BuildTimeConstantsBasePlugin.class));
    }

}
