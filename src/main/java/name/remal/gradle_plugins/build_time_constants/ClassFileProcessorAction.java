package name.remal.gradle_plugins.build_time_constants;

import static java.nio.file.Files.isDirectory;
import static name.remal.gradle_plugins.toolkit.JvmLanguageCompilationUtils.getJvmLanguagesCompileTaskProperties;
import static name.remal.gradle_plugins.toolkit.SneakyThrowUtils.sneakyThrowsFunction;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.gradle.api.Action;
import org.gradle.api.Describable;
import org.gradle.api.Task;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;

@RequiredArgsConstructor
abstract class ClassFileProcessorAction implements Action<Task>, Describable {

    public abstract MapProperty<String, String> getProperties();

    @Override
    @SneakyThrows
    public void execute(Task task) {
        var compileProperties = getJvmLanguagesCompileTaskProperties(task);
        if (compileProperties == null) {
            return;
        }

        var destinationDir = compileProperties.getDestinationDirectory().getAsFile().getOrNull();
        if (destinationDir == null) {
            return;
        }

        var classpath = getObjects().fileCollection().from(destinationDir).plus(
            compileProperties.getClasspath()
        );
        var classpathUrls = classpath.getFiles().stream()
            .map(File::toPath)
            .filter(Files::exists)
            .map(path -> {
                if (isDirectory(path)) {
                    return path.toUri();
                } else {
                    return URI.create("jar:" + path.toUri() + "!/");
                }
            })
            .map(sneakyThrowsFunction(URI::toURL))
            .toArray(URL[]::new);
        try (var classLoader = new URLClassLoader(classpathUrls, null)) {
            var fileTree = getObjects().fileTree().from(destinationDir);
            fileTree.include("**/*.class");
            fileTree.visit(details -> {
                if (!details.isDirectory()) {
                    var path = details.getFile().toPath();
                    var processor = new ClassFileProcessor(path, path, getProperties().get(), classLoader);
                    processor.process();
                }
            });
        }
    }

    @Override
    public String getDisplayName() {
        return ClassFileProcessorAction.class.getName();
    }


    @Inject
    protected abstract ObjectFactory getObjects();

}
