package name.remal.gradle_plugins.build_time_constants.jvm;

import static java.lang.String.join;

import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.toolkit.testkit.functional.GradleProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class BuildTimeConstantsJvmPluginFunctionalTest {

    private final GradleProject project;


    @BeforeEach
    void beforeEach() {
        project.forBuildFile(build -> {
            build.applyPlugin("name.remal.build-time-constants.jvm");
            build.applyPlugin("java");
            build.addBuildDirMavenRepositories();
        });

        project.writeTextFile("src/main/java/pkg/TestClass.java", join(
            "\n",
            "package pkg;",
            "",
            "import static name.remal.gradle_plugins.build_time_constants.api.BuildTimeConstants.*;",
            "",
            "public class TestClass {",
            "    public static final String OBJECT_INTERNAL_CLASS_NAME = getClassInternalName(Object.class);",
            "}"
        ));
    }

    @Test
    void compilation() {
        project.getBuildFile()
            .registerDefaultTask("compileJava");
        project.assertBuildSuccessfully();
    }

}
