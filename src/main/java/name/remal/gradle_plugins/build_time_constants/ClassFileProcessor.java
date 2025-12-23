package name.remal.gradle_plugins.build_time_constants;

import static com.google.common.hash.Hashing.sha512;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.write;
import static name.remal.gradle_plugins.build_time_constants.BytecodeTestUtils.wrapWithTestClassVisitors;
import static name.remal.gradle_plugins.toolkit.InTestFlags.isInUnitTest;
import static name.remal.gradle_plugins.toolkit.StringUtils.escapeRegex;
import static name.remal.gradle_plugins.toolkit.StringUtils.substringAfterLast;
import static name.remal.gradle_plugins.toolkit.StringUtils.substringBeforeLast;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
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
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SIPUSH;
import static org.objectweb.asm.Type.getDescriptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

@RequiredArgsConstructor
class ClassFileProcessor {

    private static final String BUILD_TIME_CONSTANTS_INTERNAL_NAME =
        "name/remal/gradle_plugins/build_time_constants/api/BuildTimeConstants";

    private static final String LEGACY_BUILD_TIME_CONSTANTS_INTERNAL_NAME =
        "name/remal/gradle_plugins/api/BuildTimeConstants";

    @SuppressWarnings("java:S5803")
    private static final String INLINE_BUILD_TIME_CONSTANTS_IN_TESTS_ONLY_DESC =
        getDescriptor(InlineBuildTimeConstantsInTestsOnly.class);

    private static final String INLINE_BUILD_TIME_CONSTANTS_IN_TESTS_ONLY_LEGACY_DESC =
        "Lname/remal/gradle_plugins/build_time_constants/jvm/InlineBuildTimeConstantsInTestsOnly;";


    private static final boolean PUT_PROPERTY_MAPS_TO_FIELDS = true;
    private static final boolean IN_TEST = isInUnitTest();

    private final Path sourcePath;
    private final Path targetPath;
    private final Map<String, String> properties;
    private final ClassLoader classLoader;

    private boolean changed;

    @SneakyThrows
    @SuppressWarnings("java:S3776")
    public void process() {
        var classNode = new ClassNode();
        ClassReader classReader;
        try (var inputStream = newInputStream(sourcePath)) {
            classReader = new ClassReader(inputStream);
            classReader.accept(classNode, 0);
        }

        if (BUILD_TIME_CONSTANTS_INTERNAL_NAME.equals(classNode.name)
            || LEGACY_BUILD_TIME_CONSTANTS_INTERNAL_NAME.equals(classNode.name)
        ) {
            return;
        }

        if (classNode.invisibleAnnotations != null && !IN_TEST) {
            for (var annotation : classNode.invisibleAnnotations) {
                if (annotation.desc.equals(INLINE_BUILD_TIME_CONSTANTS_IN_TESTS_ONLY_DESC)
                    || annotation.desc.equals(INLINE_BUILD_TIME_CONSTANTS_IN_TESTS_ONLY_LEGACY_DESC)
                ) {
                    return;
                }
            }
        }


        processClass(classNode);


        if (changed) {
            var classWriter = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES) {
                @Override
                protected ClassLoader getClassLoader() {
                    return classLoader;
                }
            };
            ClassVisitor classVisitor = classWriter;
            if (IN_TEST) {
                classVisitor = wrapWithTestClassVisitors(classVisitor);
            }
            classNode.accept(classVisitor);
            var bytecode = classWriter.toByteArray();

            if (sourcePath != targetPath) {
                var targetDirPath = targetPath.getParent();
                if (targetDirPath != null) {
                    Files.createDirectories(targetDirPath);
                }
            }

            write(targetPath, bytecode);
        }
    }


    private void processClass(ClassNode classNode) {
        if (classNode.methods.isEmpty()) {
            return;
        }

        new ArrayList<>(classNode.methods).forEach(methodNode -> {
            var instructions = methodNode.instructions;
            if (instructions.size() == 0) {
                return;
            }

            processInstructions(classNode, methodNode, instructions);

            instructions.forEach(insn -> {
                if (insn instanceof MethodInsnNode) {
                    var methodInsn = (MethodInsnNode) insn;
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

            var methodInsn = (MethodInsnNode) insn;
            if (methodInsn.getOpcode() != INVOKESTATIC) {
                continue;
            }

            if (!methodInsn.owner.equals(BUILD_TIME_CONSTANTS_INTERNAL_NAME)
                && !methodInsn.owner.equals(LEGACY_BUILD_TIME_CONSTANTS_INTERNAL_NAME)
            ) {
                continue;
            }

            var ldcInsn = getPrevLdcInsn(methodInsn);
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
                        newInsns = createConstantMapInsns(
                            classNode,
                            methodInsn.name,
                            ldcInsn.cst,
                            getPropertiesByNamePattern(ldcInsn.cst, String.class, String::valueOf)
                        );
                        break;
                    case "getIntegerProperties":
                        newInsns = createConstantMapInsns(
                            classNode,
                            methodInsn.name,
                            ldcInsn.cst,
                            getPropertiesByNamePattern(ldcInsn.cst, int.class, Integer::parseInt)
                        );
                        break;
                    case "getLongProperties":
                        newInsns = createConstantMapInsns(
                            classNode,
                            methodInsn.name,
                            ldcInsn.cst,
                            getPropertiesByNamePattern(ldcInsn.cst, long.class, Long::parseLong)
                        );
                        break;
                    case "getBooleanProperties":
                        newInsns = createConstantMapInsns(
                            classNode,
                            methodInsn.name,
                            ldcInsn.cst,
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

            var newInsnList = new InsnList();
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

            if (insn instanceof LineNumberNode
                || insn instanceof LabelNode
            ) {
                continue;
            }

            break;
        }
        return null;
    }

    private static List<AbstractInsnNode> createConstantInsns(Object value) {
        return List.of(createConstantInsn(value));
    }

    @SuppressWarnings("java:S5411")
    private static List<AbstractInsnNode> createBoxedConstantInsns(Object value) {
        if (value instanceof Boolean) {
            var boolValue = (Boolean) value;
            return List.of(new FieldInsnNode(
                GETSTATIC,
                "java/lang/Boolean",
                boolValue ? "TRUE" : "FALSE",
                "Ljava/lang/Boolean;"
            ));
        }

        var result = new ArrayList<AbstractInsnNode>();
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

        return result;
    }

    @SuppressWarnings({"java:S3776", "java:S5411"})
    private static AbstractInsnNode createConstantInsn(Object value) {
        if (value instanceof Boolean) {
            var boolValue = (Boolean) value;
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

    private static List<AbstractInsnNode> createConstantMapInsns(
        ClassNode classNode,
        String scope,
        Object value,
        Map<?, ?> values
    ) {
        if (!PUT_PROPERTY_MAPS_TO_FIELDS
            || (classNode.access & ACC_INTERFACE) != 0
        ) {
            return createMapInsns(values);
        }

        var valueHash = sha512().hashString(value.toString(), UTF_8).toString();
        var fieldName = "$" + scope + "$" + valueHash;
        FieldNode field = classNode.fields.stream()
            .filter(it -> it.name.equals(fieldName))
            .findAny()
            .orElse(null);

        if (field == null) {
            field = new FieldNode(
                ACC_PRIVATE | ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC,
                fieldName,
                "Ljava/util/Map;",
                null,
                null
            );
            classNode.fields.add(field);

            MethodNode staticInitMethod = classNode.methods.stream()
                .filter(it -> it.name.equals("<clinit>") && it.desc.equals("()V"))
                .findAny()
                .orElse(null);
            if (staticInitMethod == null) {
                staticInitMethod = new MethodNode(
                    ACC_STATIC,
                    "<clinit>",
                    "()V",
                    null,
                    null
                );
                classNode.methods.add(staticInitMethod);
            }

            InsnList instructions = staticInitMethod.instructions;
            if (instructions.size() == 0) {
                instructions.add(new LabelNode());
                instructions.add(new InsnNode(RETURN));
            }

            var insnsToAdd = new InsnList();
            createMapInsns(values).forEach(insnsToAdd::add);
            insnsToAdd.add(new FieldInsnNode(
                PUTSTATIC,
                classNode.name,
                field.name,
                field.desc
            ));

            var prevInsn = getFirstLabelNode(instructions);
            if (prevInsn == null) {
                instructions.insert(insnsToAdd);
            } else {
                instructions.insert(prevInsn, insnsToAdd);
            }
        }

        return List.of(new FieldInsnNode(
            GETSTATIC,
            classNode.name,
            field.name,
            field.desc
        ));
    }

    private static List<AbstractInsnNode> createMapInsns(Map<?, ?> values) {
        if (values.isEmpty()) {
            return List.of(
                new MethodInsnNode(
                    INVOKESTATIC,
                    "java/util/Collections",
                    "emptyMap",
                    "()Ljava/util/Map;"
                )
            );

        } else if (values.size() == 1) {
            var entry = values.entrySet().iterator().next();
            var result = new ArrayList<AbstractInsnNode>();
            result.add(createConstantInsn(entry.getKey()));
            result.addAll(createBoxedConstantInsns(entry.getValue()));
            result.add(new MethodInsnNode(
                INVOKESTATIC,
                "java/util/Collections",
                "singletonMap",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;"
            ));
            return result;

        } else {
            var result = new ArrayList<AbstractInsnNode>();
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
            return result;
        }
    }

    private <T> Map<String, T> getPropertiesByNamePattern(
        Object propertyNamePattern,
        Class<T> type,
        Function<String, T> converter
    ) {
        Map<String, T> result = new LinkedHashMap<>();

        var pattern = Pattern.compile(
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

    @Nullable
    private static LabelNode getFirstLabelNode(InsnList instructions) {
        AbstractInsnNode insn = instructions.getFirst();
        while (insn != null) {
            if (insn instanceof LabelNode) {
                return (LabelNode) insn;
            }

            if (insn instanceof LineNumberNode) {
                insn = insn.getNext();
                continue;
            }

            break;
        }

        return null;
    }

}
