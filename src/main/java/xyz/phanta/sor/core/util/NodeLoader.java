package xyz.phanta.sor.core.util;

import com.moandjiezana.toml.Toml;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import xyz.phanta.sor.api.ISorNode;
import xyz.phanta.sor.core.launch.SorInitializationException;
import xyz.phanta.sor.core.log.SorLog;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class NodeLoader {

    @Nullable
    private static NodeLoader INSTANCE = null;

    private static NodeLoader getInstance() throws SorInitializationException {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new NodeLoader());
    }

    private final Method mAddUrl;
    private final List<String> jars = new ArrayList<>();

    private NodeLoader() throws SorInitializationException {
        try {
            mAddUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            mAddUrl.setAccessible(true);
        } catch (Exception e) {
            throw new SorInitializationException(e);
        }
    }

    static List<Class<ISorNode>> loadFromManifest(Toml mf) throws SorInitializationException {
        NodeLoader loader = getInstance();
        loader.tryLoadFromManifest(mf);
        return loader.finish();
    }

    private void tryLoad(Path path) throws SorInitializationException {
        try {
            jars.add(path.getFileName().toString());
            mAddUrl.invoke(Thread.currentThread().getContextClassLoader(), path.toUri().toURL());
        } catch (Exception e) {
            throw new SorInitializationException(e);
        }
    }

    private void tryLoadFromManifest(Toml mf) throws SorInitializationException {
        try {
            FileSystem fs = FileSystems.getDefault();
            Toml nodes = mf.getTable("nodes");
            if (nodes != null) {
                for (String path : nodes.<String>getList("paths", Collections.emptyList())) {
                    Files.list(fs.getPath(path)).forEach(p -> {
                        if (p.toString().endsWith(".jar")) {
                            SorLog.info("Discovered node source %s", p);
                            try {
                                tryLoad(p);
                            } catch (SorInitializationException e) {
                                throw e.wrap();
                            }
                        }
                    });
                }
                for (String file : nodes.<String>getList("files", Collections.emptyList())) {
                    Path path = fs.getPath(file);
                    SorLog.info("Discovered node source %s", path);
                    tryLoad(path);
                }
            }
        } catch (SorInitializationException.Wrapped e) {
            throw e.unwrap();
        } catch (Exception e) {
            throw new SorInitializationException(e);
        }
    }

    private List<Class<ISorNode>> finish() throws SorInitializationException {
        try (ScanResult scan = new ClassGraph()
                .enableClassInfo()
                .whitelistJars(jars.toArray(new String[0]))
                .scan()) {
            return scan.getClassesImplementing(ISorNode.class.getCanonicalName()).loadClasses(ISorNode.class);
        } catch (Exception e) {
            throw new SorInitializationException(e);
        }
    }

}
