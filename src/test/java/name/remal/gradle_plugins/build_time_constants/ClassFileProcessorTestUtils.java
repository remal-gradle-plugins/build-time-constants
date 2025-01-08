package name.remal.gradle_plugins.build_time_constants;

import static com.google.common.jimfs.Configuration.unix;
import static java.lang.Math.addExact;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newInputStream;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.makeAccessible;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.parallel.Execution;

@Execution(SAME_THREAD)
abstract class ClassFileProcessorTestUtils {

    protected final Object processAndCallTestMethod(Class<?> clazz) {
        return processAndCallTestMethod(clazz, emptyMap());
    }

    @SneakyThrows
    protected final Object processAndCallTestMethod(Class<?> clazz, Map<String, String> properties) {
        try (
            val fileSystem = Jimfs.newFileSystem(unix());
            val classLoader = new CurrentFileSystemClassLoader()
        ) {
            currentFileSystem.set(fileSystem);

            val bytecodePath = '/' + clazz.getName().replace('.', '/') + ".class";
            val bytecodeUrl = requireNonNull(clazz.getResource(bytecodePath));
            val sourcePath = Paths.get(bytecodeUrl.toURI());
            val targetPath = fileSystem.getPath(bytecodePath);
            new ClassFileProcessor(sourcePath, targetPath, properties).process();

            if (!exists(targetPath)) {
                throw new ClassNotChangedException();
            }

            val transformedClass = classLoader.loadClass(clazz.getName());
            val testMethod = makeAccessible(transformedClass.getDeclaredMethod("test"));
            try {
                return testMethod.invoke(null);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }

        } finally {
            currentFileSystem.remove();
        }
    }

    protected static final class ClassNotChangedException extends RuntimeException {
    }


    protected static LongStream newKeyNumbersStream() {
        SortedSet<Long> result = new TreeSet<>();
        long delta = 2;
        for (int bits = 0; bits < Long.SIZE; ++bits) {
            long value = Long.MIN_VALUE >> bits;
            if (bits > 1) {
                for (int n = -1; -delta <= n; --n) {
                    result.add(addExact(value, n));
                }
            }
            for (int n = 0; n <= delta; ++n) {
                result.add(addExact(value, n));
            }
        }
        for (int bits = 0; bits < Long.SIZE; ++bits) {
            long value = Long.MAX_VALUE >> bits;
            if (bits > 1) {
                for (int n = 1; n <= delta; ++n) {
                    result.add(addExact(value, n));
                }
            }
            for (int n = 0; -delta <= n; --n) {
                result.add(addExact(value, n));
            }
        }
        return result.stream().mapToLong(Long::longValue);
    }


    //#region In-memory file system support

    private static final String CURRENT_FILE_SYSTEM_URL_PROTOCOL = "current-fs";

    private static final ThreadLocal<FileSystem> currentFileSystem = new ThreadLocal<>();

    static {
        URL.setURLStreamHandlerFactory(new CurrentFileSystemUrlStreamHandlerFactory());
    }


    /**
     * Classes from this {@link ClassLoader} should be loaded first.
     */
    private static class CurrentFileSystemClassLoader extends URLClassLoader {

        public CurrentFileSystemClassLoader() {
            super(
                new URL[]{createCurrentFileSystemUrl()},
                getSystemClassLoader()
            );
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            final Class<?> clazz;
            try {
                clazz = findClass(name);
            } catch (ClassNotFoundException ignored) {
                return super.loadClass(name, resolve);
            }

            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }

    }


    @SneakyThrows
    private static URL createCurrentFileSystemUrl() {
        return new URL(CURRENT_FILE_SYSTEM_URL_PROTOCOL, null, "/");
    }

    private static class CurrentFileSystemUrlStreamHandlerFactory implements URLStreamHandlerFactory {
        @Nullable
        @Override
        public URLStreamHandler createURLStreamHandler(@Nullable String protocol) {
            if (CURRENT_FILE_SYSTEM_URL_PROTOCOL.equals(protocol)) {
                return new CurrentUrlStreamHandler();
            }

            return null;
        }

    }

    private static class CurrentUrlStreamHandler extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(URL url) {
            return new CurrentUrlConnection(url);
        }
    }

    private static class CurrentUrlConnection extends URLConnection {

        public CurrentUrlConnection(URL url) {
            super(url);
        }

        @Nullable
        private InputStream stream;

        @Override
        public void connect() throws IOException {
            if (stream != null) {
                return;
            }

            val path = currentFileSystem.get().getPath(url.getPath());
            if (isDirectory(path)) {
                throw new AssertionError("A directory: " + path);
            }

            this.stream = newInputStream(path);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            connect();
            return requireNonNull(stream);
        }

    }

    //#endregion

}
