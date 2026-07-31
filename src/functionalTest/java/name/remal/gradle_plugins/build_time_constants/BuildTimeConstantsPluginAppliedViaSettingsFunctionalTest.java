package name.remal.gradle_plugins.build_time_constants;

import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.toolkit.testkit.functional.GradleProject;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class BuildTimeConstantsPluginAppliedViaSettingsFunctionalTest {

    private final GradleProject project;

    @Test
    void appliedViaSettingsIsAppliedToProject() {
        project.forSettingsFile(settings -> settings.applyPlugin("name.remal.build-time-constants"));

        // The plugin must NOT be applied via the project's build file: it should reach the project
        // solely through the Settings-level application propagating via GradleLifecycle.beforeProject.
        // The assertion runs at configuration time (not inside doLast): accessing Task.project at
        // execution time is unsupported with the configuration cache.
        project.getBuildFile().line(
            "assert pluginManager.hasPlugin('name.remal.build-time-constants')"
        );
        project.getBuildFile().line("tasks.register('assertPluginApplied')");

        project.assertBuildSuccessfully("assertPluginApplied");
    }

}
