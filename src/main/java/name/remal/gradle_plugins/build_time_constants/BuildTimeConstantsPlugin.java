package name.remal.gradle_plugins.build_time_constants;

import static java.lang.String.format;
import static name.remal.gradle_plugins.build_time_constants.BuildTimeConstantsApiBuildInfo.BUILD_TIME_CONSTANTS_API_ARTIFACT_ID;
import static name.remal.gradle_plugins.build_time_constants.BuildTimeConstantsApiBuildInfo.BUILD_TIME_CONSTANTS_API_GROUP;
import static name.remal.gradle_plugins.build_time_constants.BuildTimeConstantsApiBuildInfo.BUILD_TIME_CONSTANTS_API_VERSION;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.unwrapProviders;

import java.util.LinkedHashMap;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.val;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.AbstractCompile;

public abstract class BuildTimeConstantsPlugin implements Plugin<Project> {

    public static final String BUILD_TIME_CONSTANTS_EXTENSION_NAME = doNotInline("buildTimeConstants");

    @Override
    public void apply(Project project) {
        project.getExtensions().create(BUILD_TIME_CONSTANTS_EXTENSION_NAME, BuildTimeConstantsExtension.class);

        project.getPluginManager().withPlugin("java", __ -> {
            val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
            sourceSets.configureEach(sourceSet ->
                project.getConfigurations().named(sourceSet.getCompileOnlyConfigurationName(), conf -> {
                    val dependencyNotation = format(
                        "%s:%s:%s",
                        BUILD_TIME_CONSTANTS_API_GROUP,
                        BUILD_TIME_CONSTANTS_API_ARTIFACT_ID,
                        BUILD_TIME_CONSTANTS_API_VERSION
                    );
                    val dependency = project.getDependencies().create(dependencyNotation);
                    if (dependency instanceof ExternalModuleDependency) {
                        ((ExternalModuleDependency) dependency).version(versions ->
                            versions.strictly(BUILD_TIME_CONSTANTS_API_VERSION)
                        );
                    }
                    conf.getDependencies().add(dependency);
                })
            );
        });

        val extension = project.getExtensions().getByType(BuildTimeConstantsExtension.class);

        val objects = getObjects();
        val allProperties = extension.getProperties();

        val properties = objects.mapProperty(String.class, String.class);
        properties.set(getProviders().provider(() -> {
            val result = new LinkedHashMap<String, String>();
            allProperties.get().forEach((@Nullable Object key, @Nullable Object value) -> {
                key = unwrapProviders(key);
                value = unwrapProviders(value);
                if (key == null || value == null) {
                    return;
                }

                result.put(key.toString(), value.toString());
            });
            return result;
        }));
        properties.finalizeValueOnRead();

        val processingAction = objects.newInstance(ClassFileProcessorAction.class);
        processingAction.getProperties().set(properties);

        project.getTasks().withType(AbstractCompile.class).configureEach(task -> {
            task.getInputs().property(
                BuildTimeConstantsExtension.class.getSimpleName() + ".properties",
                properties
            );

            task.doLast(processingAction);
        });
    }


    @Inject
    protected abstract ObjectFactory getObjects();

    @Inject
    protected abstract ProviderFactory getProviders();

}
