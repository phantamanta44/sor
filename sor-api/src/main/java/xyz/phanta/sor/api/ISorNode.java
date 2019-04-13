package xyz.phanta.sor.api;

import java.util.Collection;
import java.util.Collections;

public interface ISorNode {

    String getNodeId();

    default Collection<String> getNodeDependencies() {
        return Collections.emptySet();
    }

    void begin(ISorApi api);

}
