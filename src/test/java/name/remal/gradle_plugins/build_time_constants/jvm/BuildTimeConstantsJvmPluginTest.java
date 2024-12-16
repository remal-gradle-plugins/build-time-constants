package name.remal.gradle_plugins.build_time_constants.jvm;

import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.packageNameOf;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.unwrapGeneratedSubclass;
import static name.remal.gradle_plugins.toolkit.testkit.ProjectValidations.executeAfterEvaluateActions;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.RequiredArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.build_time_constants.BuildTimeConstantsBasePlugin;
import name.remal.gradle_plugins.toolkit.testkit.TaskValidations;
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

    @Test
    void pluginTasksDoNotHavePropertyProblems() {
        project.getPluginManager().apply("java");

        executeAfterEvaluateActions(project);

        val taskClassNamePrefix = packageNameOf(BuildTimeConstantsBasePlugin.class) + '.';
        project.getTasks().stream()
            .filter(task -> {
                val taskClass = unwrapGeneratedSubclass(task.getClass());
                return taskClass.getName().startsWith(taskClassNamePrefix);
            })
            //.map(TaskValidations::markTaskDependenciesAsSkipped)
            .forEach(TaskValidations::assertNoTaskPropertiesProblems);
    }

}
