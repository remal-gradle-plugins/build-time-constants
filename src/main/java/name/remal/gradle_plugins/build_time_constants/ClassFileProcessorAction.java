package name.remal.gradle_plugins.build_time_constants;

import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.gradle.api.Action;
import org.gradle.api.Describable;
import org.gradle.api.Task;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.compile.AbstractCompile;

@RequiredArgsConstructor
abstract class ClassFileProcessorAction implements Action<Task>, Describable {

    public abstract MapProperty<String, String> getProperties();

    @Override
    public void execute(Task untypedTask) {
        var task = (AbstractCompile) untypedTask;
        var destinationDir = task.getDestinationDirectory().getAsFile().getOrNull();
        if (destinationDir == null) {
            return;
        }

        var fileTree = getObjects().fileTree().from(destinationDir);
        fileTree.include("**/*.class");
        fileTree.visit(details -> {
            if (!details.isDirectory()) {
                var path = details.getFile().toPath();
                new ClassFileProcessor(path, path, getProperties().get()).process();
            }
        });
    }

    @Override
    public String getDisplayName() {
        return ClassFileProcessorAction.class.getName();
    }


    @Inject
    protected abstract ObjectFactory getObjects();

}
