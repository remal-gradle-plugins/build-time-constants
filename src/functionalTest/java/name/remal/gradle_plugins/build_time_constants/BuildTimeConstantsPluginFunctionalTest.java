package name.remal.gradle_plugins.build_time_constants;

import static java.lang.String.join;

import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.toolkit.testkit.functional.GradleProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class BuildTimeConstantsPluginFunctionalTest {

    private final GradleProject project;


    @BeforeEach
    void beforeEach() {
        project.forBuildFile(build -> {
            build.applyPlugin("name.remal.build-time-constants");
            build.applyPlugin("java");
            build.addBuildDirMavenRepositories();
        });
    }

    @Test
    void getClassInternalName() {
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

        project.assertBuildSuccessfully("compileJava");
    }

    @Test
    void getStringProperty() {
        project.getBuildFile().line("buildTimeConstants.property('prop', 'value')");

        project.writeTextFile("src/main/java/pkg/TestClass.java", join(
            "\n",
            "package pkg;",
            "",
            "import static name.remal.gradle_plugins.build_time_constants.api.BuildTimeConstants.*;",
            "",
            "public class TestClass {",
            "    public static final String PROPERTY = getStringProperty(\"prop\");",
            "}"
        ));

        project.assertBuildSuccessfully("compileJava");
    }

}
