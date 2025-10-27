package name.remal.gradle_plugins.build_time_constants;

import static java.util.stream.Collectors.toUnmodifiableList;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.packageNameOf;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.unwrapGeneratedSubclass;
import static name.remal.gradle_plugins.toolkit.testkit.ProjectValidations.executeAfterEvaluateActions;
import static org.assertj.core.api.Assertions.assertThat;

import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.toolkit.testkit.TaskValidations;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.WriteProperties;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class BuildTimeConstantsPluginTest {

    final Project project;

    @BeforeEach
    void beforeEach() {
        project.getPluginManager().apply(BuildTimeConstantsPlugin.class);
        project.getPluginManager().apply("java");

        // dependency test tasks configuration
        project.getTasks().withType(WriteProperties.class).configureEach(task -> {
            task.getDestinationFile().set(project.getLayout().getBuildDirectory().file(
                task.getName() + ".properties"
            ));
        });
    }

    @Test
    void dependsOnExplicitDependency() {
        var testTask = project.getTasks().register("testTask", TestTask.class);

        var extension = project.getExtensions().getByType(BuildTimeConstantsExtension.class);
        extension.dependOn("testTask");

        var compileJava = project.getTasks().named("compileJava", AbstractCompile.class).get();
        var compileJavaDependencies = compileJava.getTaskDependencies().getDependencies(compileJava).stream()
            .map(Task.class::cast)
            .collect(toUnmodifiableList());
        assertThat(compileJavaDependencies)
            .contains(testTask.get());
    }

    @SuppressWarnings("UnusedMethod")
    public abstract static class TestTask extends DefaultTask {
        @OutputFile
        public abstract RegularFileProperty getOutputFile();
    }

    @Test
    void pluginTasksDoNotHavePropertyProblems() {
        executeAfterEvaluateActions(project);

        var taskClassNamePrefix = packageNameOf(BuildTimeConstantsPlugin.class) + '.';
        project.getTasks().stream()
            .filter(task -> {
                if (task instanceof AbstractCompile) {
                    return true;
                }

                var taskClass = unwrapGeneratedSubclass(task.getClass());
                return taskClass.getName().startsWith(taskClassNamePrefix);
            })
            //.map(TaskValidations::markTaskDependenciesAsSkipped)
            .forEach(TaskValidations::assertNoTaskPropertiesProblems);
    }

}
