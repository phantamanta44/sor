package xyz.phanta.sor.core.launch;

import com.moandjiezana.toml.Toml;
import xyz.phanta.sor.api.ISorNode;
import xyz.phanta.sor.core.util.SorUtils;

import java.util.Collections;
import java.util.List;

public abstract class NodeLaunchDelegate implements ILaunchDelegate {

    protected final List<ISorNode> nodes;

    public NodeLaunchDelegate(Toml mf) throws SorInitializationException {
        try {
            nodes = Collections.unmodifiableList(SorUtils.loadNodes(mf));
        } catch (SorInitializationException.Wrapped e) {
            throw e.unwrap();
        } catch (Exception e) {
            throw new SorInitializationException(e);
        }
    }

}
