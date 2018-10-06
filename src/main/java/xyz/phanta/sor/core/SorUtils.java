package xyz.phanta.sor.core;

import com.moandjiezana.toml.Toml;
import xyz.phanta.sor.api.ISorNode;
import xyz.phanta.sor.core.launch.SorInitializationException;

import java.util.Collection;
import java.util.stream.Collectors;

public class SorUtils {

    public static Collection<ISorNode> loadNodes(Toml mf) throws SorInitializationException {
        Collection<Class<?>> nodeClasses = NodeLoader.loadFromManifest(mf);
        return nodeClasses.stream().map(c -> {
            try {
                return (ISorNode)c.newInstance();
            } catch (ExceptionInInitializerError e) {
                Throwable underlying = e.getException();
                if (underlying instanceof SorInitializationException) {
                    throw ((SorInitializationException)underlying).wrap();
                }
                throw new SorInitializationException(e).wrap();
            } catch (IllegalAccessException e) {
                throw new SorInitializationException(
                        "Could not access public empty constructor in " + c.getCanonicalName()).wrap();
            } catch (InstantiationException e) {
                throw new SorInitializationException(e).wrap();
            }
        }).collect(Collectors.toList());
    }

}
