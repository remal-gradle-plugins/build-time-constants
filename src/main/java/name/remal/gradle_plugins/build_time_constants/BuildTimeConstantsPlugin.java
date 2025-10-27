package name.remal.gradle_plugins.build_time_constants;

import static java.lang.String.format;
import static name.remal.gradle_plugins.build_time_constants.api.BuildTimeConstants.getStringProperty;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.unwrapProviders;

import java.util.LinkedHashMap;
import javax.inject.Inject;
import name.remal.gradle_plugins.toolkit.JvmLanguageCompilationUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSetContainer;

public abstract class BuildTimeConstantsPlugin implements Plugin<Project> {

    public static final String BUILD_TIME_CONSTANTS_EXTENSION_NAME = doNotInline("buildTimeConstants");

    @Override
    public void apply(Project project) {
        project.getExtensions().create(BUILD_TIME_CONSTANTS_EXTENSION_NAME, BuildTimeConstantsExtension.class);

        project.getPluginManager().withPlugin("java", __ -> {
            var sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
            sourceSets.configureEach(sourceSet ->
                project.getConfigurations().named(sourceSet.getCompileOnlyConfigurationName(), conf -> {
                    var dependencyNotation = format(
                        "%s:%s:%s",
                        getStringProperty("api.groupId"),
                        getStringProperty("api.artifactId"),
                        getStringProperty("api.version")
                    );
                    var dependency = project.getDependencies().create(dependencyNotation);
                    if (dependency instanceof ExternalModuleDependency) {
                        ((ExternalModuleDependency) dependency).version(versions ->
                            versions.strictly(getStringProperty("api.version"))
                        );
                    }
                    conf.getDependencies().add(dependency);
                })
            );
        });


        var extension = project.getExtensions().getByType(BuildTimeConstantsExtension.class);

        var properties = getObjects().mapProperty(String.class, String.class);
        properties.value(getProviders().provider(() -> {
            var result = new LinkedHashMap<String, String>();
            extension.getProperties().get().forEach((Object key, Object value) -> {
                key = unwrapProviders(key);
                value = unwrapProviders(value);
                if (key == null || value == null) {
                    return;
                }

                result.put(key.toString(), value.toString());
            });
            return result;
        })).finalizeValueOnRead();

        var processingAction = getObjects().newInstance(ClassFileProcessorAction.class);
        processingAction.getProperties().set(properties);

        project.getTasks()
            .matching(JvmLanguageCompilationUtils::isJvmLanguageCompileTask)
            .configureEach(task -> {
                task.dependsOn(extension.getCompilationDependencies());

                task.getInputs().property(
                    BuildTimeConstantsExtension.class.getSimpleName() + ".properties",
                    properties
                ).optional(true);

                task.doLast(processingAction);
            });
    }


    @Inject
    protected abstract ObjectFactory getObjects();

    @Inject
    protected abstract ProviderFactory getProviders();

}
