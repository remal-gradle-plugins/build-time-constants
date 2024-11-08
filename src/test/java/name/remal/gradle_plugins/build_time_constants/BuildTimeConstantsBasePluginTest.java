package name.remal.gradle_plugins.build_time_constants;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import lombok.RequiredArgsConstructor;
import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class BuildTimeConstantsBasePluginTest {

    final Project project;

    @BeforeEach
    void beforeEach() {
        project.getPluginManager().apply(BuildTimeConstantsBasePlugin.class);
    }

    @Test
    void extensionIsAdded() {
        assertNotNull(project.getExtensions().getByType(BuildTimeConstantsExtension.class));
    }

}
