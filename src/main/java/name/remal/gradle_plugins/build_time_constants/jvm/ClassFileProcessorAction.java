package name.remal.gradle_plugins.build_time_constants.jvm;

import static name.remal.gradle_plugins.toolkit.AbstractCompileUtils.getDestinationDir;

import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.compile.AbstractCompile;

@RequiredArgsConstructor
abstract class ClassFileProcessorAction implements Action<Task> {

    public abstract MapProperty<String, String> getProperties();

    @Override
    public void execute(Task untypedTask) {
        val task = (AbstractCompile) untypedTask;
        val destinationDir = getDestinationDir(task);
        if (destinationDir == null) {
            return;
        }

        val fileTree = getObjects().fileTree().from(destinationDir);
        fileTree.include("**/*.class");
        fileTree.visit(details -> {
            if (!details.isDirectory()) {
                val path = details.getFile().toPath();
                new ClassFileProcessor(path, path, getProperties().get()).process();
            }
        });
    }


    @Inject
    protected abstract ObjectFactory getObjects();

}
