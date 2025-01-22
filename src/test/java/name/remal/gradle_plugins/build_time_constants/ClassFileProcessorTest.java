package name.remal.gradle_plugins.build_time_constants;

import static java.util.Collections.singletonList;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.packageNameOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.SneakyThrows;
import name.remal.gradle_plugins.build_time_constants.api.BuildTimeConstants;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5778")
class ClassFileProcessorTest extends ClassFileProcessorTestUtils {

    @Test
    void notChanged() {
        assertThrows(
            ClassNotChangedException.class,
            () -> processAndCallTestMethod(NotChanged.class)
        );
    }

    @InlineBuildTimeConstantsInTestsOnly
    private static class NotChanged {
        @SuppressWarnings("unused")
        static Object test() {
            throw new AssertionError();
        }
    }


    @Test
    void notConstantParameter() {
        assertThrows(
            BuildTimeConstantsException.class,
            () -> processAndCallTestMethod(NotConstantParameter.class)
        );
    }

    @InlineBuildTimeConstantsInTestsOnly
    private static class NotConstantParameter {
        @SuppressWarnings("unused")
        @SneakyThrows
        static Object test() {
            return BuildTimeConstants.getClassName(Class.forName(Object.class.getName()));
        }
    }


    @Test
    void getClassName() {
        assertEquals(
            ClassFileProcessorTest.class.getName(),
            processAndCallTestMethod(GetClassName.class)
        );
    }

    @InlineBuildTimeConstantsInTestsOnly
    private static class GetClassName {
        @SuppressWarnings("unused")
        static Object test() {
            return BuildTimeConstants.getClassName(ClassFileProcessorTest.class);
        }
    }

    @Test
    void getClassNameMultiline() {
        assertEquals(
            singletonList(GetClassNameMultiline.class.getName()),
            processAndCallTestMethod(GetClassNameMultiline.class)
        );
    }

    @InlineBuildTimeConstantsInTestsOnly
    private static class GetClassNameMultiline {
        @SuppressWarnings("unused")
        static Object test() {
            return singletonList(
                BuildTimeConstants.getClassName(GetClassNameMultiline.class)
            );
        }
    }


    @Test
    void getClassSimpleName() {
        assertEquals(
            ClassFileProcessorTest.class.getSimpleName(),
            processAndCallTestMethod(GetClassSimpleName.class)
        );
    }

    @InlineBuildTimeConstantsInTestsOnly
    private static class GetClassSimpleName {
        @SuppressWarnings("unused")
        static Object test() {
            return BuildTimeConstants.getClassSimpleName(ClassFileProcessorTest.class);
        }
    }


    @Test
    void getClassPackageName() {
        assertEquals(
            packageNameOf(ClassFileProcessorTest.class),
            processAndCallTestMethod(GetClassPackageName.class)
        );
    }

    @InlineBuildTimeConstantsInTestsOnly
    private static class GetClassPackageName {
        @SuppressWarnings("unused")
        static Object test() {
            return BuildTimeConstants.getClassPackageName(ClassFileProcessorTest.class);
        }
    }


    @Test
    void getClassInternalName() {
        assertEquals(
            getInternalName(ClassFileProcessorTest.class),
            processAndCallTestMethod(GetClassInternalName.class)
        );
    }

    @InlineBuildTimeConstantsInTestsOnly
    private static class GetClassInternalName {
        @SuppressWarnings("unused")
        static Object test() {
            return BuildTimeConstants.getClassInternalName(ClassFileProcessorTest.class);
        }
    }


    @Test
    void getClassDescriptor() {
        assertEquals(
            getDescriptor(ClassFileProcessorTest.class),
            processAndCallTestMethod(GetClassDescriptor.class)
        );
    }

    @InlineBuildTimeConstantsInTestsOnly
    private static class GetClassDescriptor {
        @SuppressWarnings("unused")
        static Object test() {
            return BuildTimeConstants.getClassDescriptor(ClassFileProcessorTest.class);
        }
    }


    @Test
    void getStringProperty() {
        assertEquals(
            "value",
            processAndCallTestMethod(GetStringProperty.class, ImmutableMap.of(
                "key", "value"
            ))
        );

        assertThrows(
            BuildTimeConstantsException.class,
            () -> processAndCallTestMethod(GetStringProperty.class)
        );
    }

    @InlineBuildTimeConstantsInTestsOnly
    private static class GetStringProperty {
        @SuppressWarnings("unused")
        static Object test() {
            return BuildTimeConstants.getStringProperty("key");
        }
    }


    @Test
    void getIntegerProperty() {
        newKeyNumbersStream()
            .filter(number -> Integer.MIN_VALUE <= number && number <= Integer.MAX_VALUE)
            .mapToInt(Math::toIntExact)
            .forEach(number ->
                assertEquals(
                    number,
                    processAndCallTestMethod(GetIntegerProperty.class, ImmutableMap.of(
                        "key", String.valueOf(number)
                    )),
                    String.valueOf(number)
                )
            );

        assertThrows(
            BuildTimeConstantsException.class,
            () -> processAndCallTestMethod(GetIntegerProperty.class)
        );

        assertThrows(
            BuildTimeConstantsException.class,
            () -> processAndCallTestMethod(GetIntegerProperty.class, ImmutableMap.of(
                "key", "string"
            ))
        );
    }

    @InlineBuildTimeConstantsInTestsOnly
    private static class GetIntegerProperty {
        @SuppressWarnings("unused")
        static Object test() {
            return BuildTimeConstants.getIntegerProperty("key");
        }
    }


    @Test
    void getLongProperty() {
        newKeyNumbersStream()
            .forEach(number ->
                assertEquals(
                    number,
                    processAndCallTestMethod(GetLongProperty.class, ImmutableMap.of(
                        "key", String.valueOf(number)
                    )),
                    String.valueOf(number)
                )
            );

        assertThrows(
            BuildTimeConstantsException.class,
            () -> processAndCallTestMethod(GetLongProperty.class)
        );

        assertThrows(
            BuildTimeConstantsException.class,
            () -> processAndCallTestMethod(GetLongProperty.class, ImmutableMap.of(
                "key", "string"
            ))
        );
    }

    @InlineBuildTimeConstantsInTestsOnly
    private static class GetLongProperty {
        @SuppressWarnings("unused")
        static Object test() {
            return BuildTimeConstants.getLongProperty("key");
        }
    }


    @Test
    void getBooleanProperty() {
        assertEquals(
            true,
            processAndCallTestMethod(GetBooleanProperty.class, ImmutableMap.of(
                "key", "tRUe"
            ))
        );

        assertThrows(
            BuildTimeConstantsException.class,
            () -> processAndCallTestMethod(GetBooleanProperty.class)
        );

        assertEquals(
            false,
            processAndCallTestMethod(GetBooleanProperty.class, ImmutableMap.of(
                "key", "string"
            ))
        );
    }

    @InlineBuildTimeConstantsInTestsOnly
    private static class GetBooleanProperty {
        @SuppressWarnings("unused")
        static Object test() {
            return BuildTimeConstants.getBooleanProperty("key");
        }
    }


    @Test
    @SuppressWarnings("unchecked")
    void getStringProperties() {
        {
            var result = (Map<Object, Object>) processAndCallTestMethod(GetStringProperties.class, ImmutableMap.of(
            ));
            assertThrows(UnsupportedOperationException.class, () -> result.put("test", "test"));
            assertEquals(
                ImmutableMap.of(
                ),
                result
            );
        }

        {
            var result = (Map<Object, Object>) processAndCallTestMethod(GetStringProperties.class, ImmutableMap.of(
                "key.1", "value1"
            ));
            assertThrows(UnsupportedOperationException.class, () -> result.put("test", "test"));
            assertEquals(
                ImmutableMap.of(
                    "key.1", "value1"
                ),
                result
            );
        }

        {
            var result = (Map<Object, Object>) processAndCallTestMethod(GetStringProperties.class, ImmutableMap.of(
                "key.1", "value1",
                "key.2", "value2"
            ));
            assertThrows(UnsupportedOperationException.class, () -> result.put("test", "test"));
            assertEquals(
                ImmutableMap.of(
                    "key.1", "value1",
                    "key.2", "value2"
                ),
                result
            );
        }

        {
            var result = (Map<Object, Object>) processAndCallTestMethod(GetStringProperties.class, ImmutableMap.of(
                "key", "value"
            ));
            assertThrows(UnsupportedOperationException.class, () -> result.put("test", "test"));
            assertEquals(
                ImmutableMap.of(
                ),
                result
            );
        }
    }

    @InlineBuildTimeConstantsInTestsOnly
    private static class GetStringProperties {
        @SuppressWarnings("unused")
        static Object test() {
            return BuildTimeConstants.getStringProperties("key.*");
        }
    }


    @Test
    @SuppressWarnings("unchecked")
    void getIntegerProperties() {
        {
            var result = (Map<Object, Object>) processAndCallTestMethod(GetIntegerProperties.class, ImmutableMap.of(
            ));
            assertThrows(UnsupportedOperationException.class, () -> result.put("test", "test"));
            assertEquals(
                ImmutableMap.of(
                ),
                result
            );
        }

        {
            var result = (Map<Object, Object>) processAndCallTestMethod(GetIntegerProperties.class, ImmutableMap.of(
                "key.1", "1"
            ));
            assertThrows(UnsupportedOperationException.class, () -> result.put("test", "test"));
            assertEquals(
                ImmutableMap.of(
                    "key.1", 1
                ),
                result
            );
        }

        {
            var result = (Map<Object, Object>) processAndCallTestMethod(GetIntegerProperties.class, ImmutableMap.of(
                "key.1", "1",
                "key.2", "2"
            ));
            assertThrows(UnsupportedOperationException.class, () -> result.put("test", "test"));
            assertEquals(
                ImmutableMap.of(
                    "key.1", 1,
                    "key.2", 2
                ),
                result
            );
        }

        {
            var result = (Map<Object, Object>) processAndCallTestMethod(GetIntegerProperties.class, ImmutableMap.of(
                "key", ""
            ));
            assertThrows(UnsupportedOperationException.class, () -> result.put("test", "test"));
            assertEquals(
                ImmutableMap.of(
                ),
                result
            );
        }

        {
            assertThrows(
                BuildTimeConstantsException.class,
                () -> processAndCallTestMethod(GetIntegerProperties.class, ImmutableMap.of(
                    "key.1", "1",
                    "key.2", "value1"
                ))
            );
        }
    }

    @InlineBuildTimeConstantsInTestsOnly
    private static class GetIntegerProperties {
        @SuppressWarnings("unused")
        static Object test() {
            return BuildTimeConstants.getIntegerProperties("key.*");
        }
    }


    @Test
    @SuppressWarnings("unchecked")
    void getLongProperties() {
        {
            var result = (Map<Object, Object>) processAndCallTestMethod(GetLongProperties.class, ImmutableMap.of(
            ));
            assertThrows(UnsupportedOperationException.class, () -> result.put("test", "test"));
            assertEquals(
                ImmutableMap.of(
                ),
                result
            );
        }

        {
            var result = (Map<Object, Object>) processAndCallTestMethod(GetLongProperties.class, ImmutableMap.of(
                "key.1", "1"
            ));
            assertThrows(UnsupportedOperationException.class, () -> result.put("test", "test"));
            assertEquals(
                ImmutableMap.of(
                    "key.1", 1L
                ),
                result
            );
        }

        {
            var result = (Map<Object, Object>) processAndCallTestMethod(GetLongProperties.class, ImmutableMap.of(
                "key.1", "1",
                "key.2", "2"
            ));
            assertThrows(UnsupportedOperationException.class, () -> result.put("test", "test"));
            assertEquals(
                ImmutableMap.of(
                    "key.1", 1L,
                    "key.2", 2L
                ),
                result
            );
        }

        {
            var result = (Map<Object, Object>) processAndCallTestMethod(GetLongProperties.class, ImmutableMap.of(
                "key", ""
            ));
            assertThrows(UnsupportedOperationException.class, () -> result.put("test", "test"));
            assertEquals(
                ImmutableMap.of(
                ),
                result
            );
        }

        {
            assertThrows(
                BuildTimeConstantsException.class,
                () -> processAndCallTestMethod(GetLongProperties.class, ImmutableMap.of(
                    "key.1", "1",
                    "key.2", "value1"
                ))
            );
        }
    }

    @InlineBuildTimeConstantsInTestsOnly
    private static class GetLongProperties {
        @SuppressWarnings("unused")
        static Object test() {
            return BuildTimeConstants.getLongProperties("key.*");
        }
    }


    @Test
    @SuppressWarnings("unchecked")
    void getBooleanProperties() {
        {
            var result = (Map<Object, Object>) processAndCallTestMethod(GetBooleanProperties.class, ImmutableMap.of(
            ));
            assertThrows(UnsupportedOperationException.class, () -> result.put("test", "test"));
            assertEquals(
                ImmutableMap.of(
                ),
                result
            );
        }

        {
            var result = (Map<Object, Object>) processAndCallTestMethod(GetBooleanProperties.class, ImmutableMap.of(
                "key.1", "true"
            ));
            assertThrows(UnsupportedOperationException.class, () -> result.put("test", "test"));
            assertEquals(
                ImmutableMap.of(
                    "key.1", true
                ),
                result
            );
        }

        {
            var result = (Map<Object, Object>) processAndCallTestMethod(GetBooleanProperties.class, ImmutableMap.of(
                "key.1", "faLSe",
                "key.2", "tRUe"
            ));
            assertThrows(UnsupportedOperationException.class, () -> result.put("test", "test"));
            assertEquals(
                ImmutableMap.of(
                    "key.1", false,
                    "key.2", true
                ),
                result
            );
        }

        {
            var result = (Map<Object, Object>) processAndCallTestMethod(GetBooleanProperties.class, ImmutableMap.of(
                "key", ""
            ));
            assertThrows(UnsupportedOperationException.class, () -> result.put("test", "test"));
            assertEquals(
                ImmutableMap.of(
                ),
                result
            );
        }
    }

    @InlineBuildTimeConstantsInTestsOnly
    private static class GetBooleanProperties {
        @SuppressWarnings("unused")
        static Object test() {
            return BuildTimeConstants.getBooleanProperties("key.*");
        }
    }

}
