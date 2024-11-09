package name.remal.gradle_plugins.build_time_constants.jvm;

import static java.lang.String.format;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.write;
import static name.remal.gradle_plugins.build_time_constants.jvm.WithCheckClassAdapter.withCheckClassAdapter;
import static name.remal.gradle_plugins.toolkit.InTestFlags.isInUnitTest;
import static name.remal.gradle_plugins.toolkit.StringUtils.escapeRegex;
import static name.remal.gradle_plugins.toolkit.StringUtils.substringAfterLast;
import static name.remal.gradle_plugins.toolkit.StringUtils.substringBeforeLast;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.SIPUSH;
import static org.objectweb.asm.Type.getDescriptor;

import com.google.common.collect.ImmutableList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.build_time_constants.BuildTimeConstantsException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

@RequiredArgsConstructor
class ClassFileProcessor {

    private static final boolean IN_TEST = isInUnitTest();

    private final Path sourcePath;
    private final Path targetPath;
    private final Map<String, String> properties;

    private boolean changed;

    @SneakyThrows
    public void process() {
        val classNode = new ClassNode();
        ClassReader classReader;
        try (val inputStream = newInputStream(sourcePath)) {
            classReader = new ClassReader(inputStream);
            classReader.accept(classNode, 0);
        }

        if (classNode.invisibleAnnotations != null && !IN_TEST) {
            for (val annotation : classNode.invisibleAnnotations) {
                if (annotation.desc.equals(INLINE_BUILD_TIME_CONSTANTS_IN_TESTS_ONLY_DESC)) {
                    return;
                }
            }
        }

        processClass(classNode);

        if (changed) {
            val classWriter = new ClassWriter(COMPUTE_MAXS);
            ClassVisitor classVisitor = classWriter;
            if (IN_TEST) {
                classVisitor = withCheckClassAdapter(classVisitor);
            }
            classNode.accept(classVisitor);
            val bytecode = classWriter.toByteArray();

            if (sourcePath != targetPath) {
                val targetDirPath = targetPath.getParent();
                if (targetDirPath != null) {
                    Files.createDirectories(targetDirPath);
                }
            }

            write(targetPath, bytecode);
        }
    }

    @SuppressWarnings("java:S5803")
    private static final String INLINE_BUILD_TIME_CONSTANTS_IN_TESTS_ONLY_DESC =
        getDescriptor(InlineBuildTimeConstantsInTestsOnly.class);


    private void processClass(ClassNode classNode) {
        if (classNode.methods == null || classNode.methods.isEmpty()) {
            return;
        }

        classNode.methods.forEach(methodNode -> {
            val instructions = methodNode.instructions;
            if (instructions == null || instructions.size() == 0) {
                return;
            }

            processInstructions(classNode, methodNode, instructions);

            instructions.forEach(insn -> {
                if (insn instanceof MethodInsnNode) {
                    val methodInsn = (MethodInsnNode) insn;
                    if (methodInsn.owner.equals(BUILD_TIME_CONSTANTS_INTERNAL_NAME)) {
                        throw new BuildTimeConstantsException(format(
                            "Invocation of %s.%s%s was not substituted in %s.%s%s."
                                + " The most common reason is that not constants are used for method invocation.",
                            methodInsn.owner,
                            methodInsn.name,
                            methodInsn.desc,
                            classNode.name,
                            methodNode.name,
                            methodInsn.desc
                        ));
                    }
                }
            });
        });
    }

    private static final String BUILD_TIME_CONSTANTS_INTERNAL_NAME =
        "name/remal/gradle_plugins/build_time_constants/api/BuildTimeConstants";

    private static final String LEGACY_BUILD_TIME_CONSTANTS_INTERNAL_NAME =
        "name/remal/gradle_plugins/api/BuildTimeConstants";


    @SuppressWarnings({"java:S3776", "java:S6541", "java:S127"})
    private void processInstructions(ClassNode classNode, MethodNode methodNode, InsnList instructions) {
        for (
            AbstractInsnNode insn = instructions.getLast();
            insn != null;
            insn = insn.getPrevious()
        ) {
            if (!(insn instanceof MethodInsnNode)) {
                continue;
            }

            val methodInsn = (MethodInsnNode) insn;
            if (methodInsn.getOpcode() != INVOKESTATIC) {
                continue;
            }

            if (!methodInsn.owner.equals(BUILD_TIME_CONSTANTS_INTERNAL_NAME)
                && !methodInsn.owner.equals(LEGACY_BUILD_TIME_CONSTANTS_INTERNAL_NAME)
            ) {
                continue;
            }

            val ldcInsn = getPrevLdcInsn(methodInsn);
            if (ldcInsn == null) {
                continue;
            }

            final List<AbstractInsnNode> newInsns;
            try {
                switch (methodInsn.name) {
                    case "getClassName":
                        newInsns = createConstantInsns(
                            ((Type) ldcInsn.cst).getInternalName().replace('/', '.')
                        );
                        break;
                    case "getClassSimpleName":
                        newInsns = createConstantInsns(
                            substringAfterLast(((Type) ldcInsn.cst).getInternalName(), "/")
                        );
                        break;
                    case "getClassPackageName":
                        newInsns = createConstantInsns(
                            substringBeforeLast(((Type) ldcInsn.cst).getInternalName().replace('/', '.'), ".", "")
                        );
                        break;
                    case "getClassInternalName":
                        newInsns = createConstantInsns(
                            ((Type) ldcInsn.cst).getInternalName()
                        );
                        break;
                    case "getClassDescriptor":
                        newInsns = createConstantInsns(
                            ((Type) ldcInsn.cst).getDescriptor()
                        );
                        break;
                    case "getStringProperty":
                        newInsns = createConstantInsns(
                            getPropertyValue(ldcInsn.cst, String.class, String::valueOf)
                        );
                        break;
                    case "getIntegerProperty":
                        newInsns = createConstantInsns(
                            getPropertyValue(ldcInsn.cst, int.class, Integer::parseInt)
                        );
                        break;
                    case "getLongProperty":
                        newInsns = createConstantInsns(
                            getPropertyValue(ldcInsn.cst, long.class, Long::parseLong)
                        );
                        break;
                    case "getBooleanProperty":
                        newInsns = createConstantInsns(
                            getPropertyValue(ldcInsn.cst, boolean.class, Boolean::parseBoolean)
                        );
                        break;
                    case "getStringProperties":
                        newInsns = createMapInsns(
                            getPropertiesByNamePattern(ldcInsn.cst, String.class, String::valueOf)
                        );
                        break;
                    case "getIntegerProperties":
                        newInsns = createMapInsns(
                            getPropertiesByNamePattern(ldcInsn.cst, int.class, Integer::parseInt)
                        );
                        break;
                    case "getLongProperties":
                        newInsns = createMapInsns(
                            getPropertiesByNamePattern(ldcInsn.cst, long.class, Long::parseLong)
                        );
                        break;
                    case "getBooleanProperties":
                        newInsns = createMapInsns(
                            getPropertiesByNamePattern(ldcInsn.cst, boolean.class, Boolean::parseBoolean)
                        );
                        break;
                    default:
                        throw new UnsupportedOperationException(format(
                            "Unsupported method: %s.%s%s",
                            methodInsn.owner,
                            methodInsn.name,
                            methodInsn.desc
                        ));
                }

            } catch (Throwable e) {
                throw new BuildTimeConstantsException(format(
                    "Error processing %s.%s%s: problem with %s.%s%s method invocation and parameter `%s`.",
                    classNode.name,
                    methodNode.name,
                    methodNode.desc,
                    methodInsn.owner,
                    methodInsn.name,
                    methodInsn.desc,
                    ldcInsn.cst
                ), e);
            }
            if (newInsns.isEmpty()) {
                continue;
            }

            val newInsnList = new InsnList();
            newInsns.forEach(newInsnList::add);
            instructions.insert(methodInsn, newInsnList);

            instructions.remove(methodInsn);
            instructions.remove(ldcInsn);
            insn = newInsns.get(0);

            changed = true;
        }
    }

    @Nullable
    private static LdcInsnNode getPrevLdcInsn(@Nullable AbstractInsnNode insn) {
        while (insn != null) {
            insn = insn.getPrevious();

            if (insn instanceof LdcInsnNode) {
                return (LdcInsnNode) insn;
            }

            if (insn instanceof LineNumberNode) {
                continue;
            }

            break;
        }
        return null;
    }

    private static List<AbstractInsnNode> createConstantInsns(Object value) {
        return ImmutableList.of(createConstantInsn(value));
    }

    private static List<AbstractInsnNode> createBoxedConstantInsns(Object value) {
        if (value instanceof Boolean) {
            val boolValue = (Boolean) value;
            return ImmutableList.of(new FieldInsnNode(
                GETSTATIC,
                "java/lang/Boolean",
                boolValue ? "TRUE" : "FALSE",
                "Ljava/lang/Boolean;"
            ));
        }

        val result = ImmutableList.<AbstractInsnNode>builder();
        result.add(createConstantInsn(value));

        if (value instanceof Character) {
            result.add(new MethodInsnNode(
                INVOKESTATIC,
                "java/lang/Character",
                "valueOf",
                "(C)Ljava/lang/Character;"
            ));

        } else if (value instanceof Byte) {
            result.add(new MethodInsnNode(
                INVOKESTATIC,
                "java/lang/Byte",
                "valueOf",
                "(B)Ljava/lang/Byte;"
            ));

        } else if (value instanceof Short) {
            result.add(new MethodInsnNode(
                INVOKESTATIC,
                "java/lang/Short",
                "valueOf",
                "(S)Ljava/lang/Short;"
            ));

        } else if (value instanceof Integer) {
            result.add(new MethodInsnNode(
                INVOKESTATIC,
                "java/lang/Integer",
                "valueOf",
                "(I)Ljava/lang/Integer;"
            ));

        } else if (value instanceof Long) {
            result.add(new MethodInsnNode(
                INVOKESTATIC,
                "java/lang/Long",
                "valueOf",
                "(J)Ljava/lang/Long;"
            ));

        } else if (value instanceof Float) {
            result.add(new MethodInsnNode(
                INVOKESTATIC,
                "java/lang/Float",
                "valueOf",
                "(F)Ljava/lang/Float;"
            ));

        } else if (value instanceof Double) {
            result.add(new MethodInsnNode(
                INVOKESTATIC,
                "java/lang/Double",
                "valueOf",
                "(D)Ljava/lang/Double;"
            ));
        }

        return result.build();
    }

    @SuppressWarnings("java:S3776")
    private static AbstractInsnNode createConstantInsn(Object value) {
        if (value instanceof Boolean) {
            val boolValue = (Boolean) value;
            return new InsnNode(boolValue ? ICONST_1 : ICONST_0);
        }

        if (value instanceof Integer) {
            int intValue = (Integer) value;
            if (-1 <= intValue && intValue <= 5) {
                return new InsnNode(ICONST_0 + intValue);
            }
            if (-128 <= intValue && intValue <= 127) {
                return new IntInsnNode(BIPUSH, intValue);
            }
            if (-32768 <= intValue && intValue <= 32767) {
                return new IntInsnNode(SIPUSH, intValue);
            }
        }

        if (value instanceof Long) {
            long longValue = (Long) value;
            if (0 <= longValue && longValue <= 1) {
                return new InsnNode(LCONST_0 + (int) longValue);
            }
        }

        return new LdcInsnNode(value);
    }

    private <T> T getPropertyValue(Object propertyName, Class<T> type, Function<String, T> converter) {
        String stringValue = properties.get(propertyName.toString());
        if (stringValue == null) {
            throw new IllegalStateException(format(
                "Property '%s' is not set",
                propertyName
            ));
        }

        try {
            return converter.apply(stringValue);
        } catch (Exception e) {
            throw new IllegalStateException(format(
                "Property '%s' is not %s",
                propertyName,
                type.getSimpleName()
            ));
        }
    }

    private static List<AbstractInsnNode> createMapInsns(Map<?, ?> values) {
        if (values.isEmpty()) {
            return ImmutableList.of(
                new MethodInsnNode(
                    INVOKESTATIC,
                    "java/util/Collections",
                    "emptyMap",
                    "()Ljava/util/Map;"
                )
            );

        } else if (values.size() == 1) {
            val entry = values.entrySet().iterator().next();
            val result = ImmutableList.<AbstractInsnNode>builder();
            result.add(createConstantInsn(entry.getKey()));
            result.addAll(createBoxedConstantInsns(entry.getValue()));
            result.add(new MethodInsnNode(
                INVOKESTATIC,
                "java/util/Collections",
                "singletonMap",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;"
            ));
            return result.build();

        } else {
            val result = ImmutableList.<AbstractInsnNode>builder();
            result.add(new TypeInsnNode(NEW, "java/util/LinkedHashMap"));
            result.add(new InsnNode(DUP));
            result.add(createConstantInsn(values.size()));
            result.add(new MethodInsnNode(INVOKESPECIAL, "java/util/LinkedHashMap", "<init>", "(I)V"));
            values.forEach((key, value) -> {
                result.add(new InsnNode(DUP));
                result.add(createConstantInsn(key));
                result.addAll(createBoxedConstantInsns(value));
                result.add(new MethodInsnNode(
                    INVOKEVIRTUAL,
                    "java/util/LinkedHashMap",
                    "put",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
                ));
                result.add(new InsnNode(POP));
            });
            result.add(new MethodInsnNode(
                INVOKESTATIC,
                "java/util/Collections",
                "unmodifiableMap",
                "(Ljava/util/Map;)Ljava/util/Map;"
            ));
            return result.build();
        }
    }

    private <T> Map<String, T> getPropertiesByNamePattern(
        Object propertyNamePattern,
        Class<T> type,
        Function<String, T> converter
    ) {
        Map<String, T> result = new LinkedHashMap<>();

        val pattern = Pattern.compile(
            escapeRegex(propertyNamePattern.toString())
                .replace("\\*", ".*")
        );
        properties.forEach((key, stringValue) -> {
            if (pattern.matcher(key).matches()) {
                result.put(key, getPropertyValue(key, type, converter));
            }
        });

        return result;
    }

}
