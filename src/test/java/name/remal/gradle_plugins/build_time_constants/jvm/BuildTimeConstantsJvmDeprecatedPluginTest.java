package name.remal.gradle_plugins.build_time_constants.jvm;

import static name.remal.gradle_plugins.toolkit.PluginUtils.findPluginIdFor;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.gradle.api.Project;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class BuildTimeConstantsJvmDeprecatedPluginTest {

    final Project project;

    @Test
    void test() {
        project.getPluginManager().apply(BuildTimeConstantsJvmDeprecatedPlugin.class);

        val expectedPluginId = findPluginIdFor(BuildTimeConstantsJvmPlugin.class);
        assertNotNull(expectedPluginId);
        assertTrue(project.getPluginManager().hasPlugin(expectedPluginId));
    }

}
