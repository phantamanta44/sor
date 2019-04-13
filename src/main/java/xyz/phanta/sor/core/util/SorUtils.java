package xyz.phanta.sor.core.util;

import com.moandjiezana.toml.Toml;
import xyz.phanta.sor.api.ISorNode;
import xyz.phanta.sor.core.launch.SorInitializationException;
import xyz.phanta.sor.core.log.SorLog;

import java.util.*;
import java.util.stream.Collectors;

public class SorUtils {

    public static List<ISorNode> loadNodes(Toml mf) throws SorInitializationException {
        SorLog.info("Loading node jars...");
        List<Class<ISorNode>> nodeClasses = NodeLoader.loadFromManifest(mf);

        SorLog.info("Instantiating node instances...");
        Map<String, ISorNode> nodes = nodeClasses.stream().map(c -> {
            try {
                return c.newInstance();
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
        }).collect(Collectors.toMap(ISorNode::getNodeId, node -> node));

        SorLog.info("Computing dependency tree...");
        Set<String> visited = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        List<ISorNode> order = new LinkedList<>();
        for (ISorNode node : nodes.values()) {
            String nodeId = node.getNodeId();
            if (!visited.contains(nodeId)) computeTopology(nodes, nodeId, node, visited, stack, order);
        }
        return order;
    }

    private static void computeTopology(Map<String, ISorNode> nodes, String nodeId, ISorNode node,
                                        Set<String> visited, Deque<String> stack, List<ISorNode> order)
            throws SorInitializationException {
        visited.add(nodeId);
        stack.push(nodeId);
        for (String depId : node.getNodeDependencies()) computeDependencyTopology(nodes, depId, visited, stack, order);
        stack.pop();
        order.add(node);
    }

    private static void computeDependencyTopology(Map<String, ISorNode> nodes, String nodeId,
                                                  Set<String> visited, Deque<String> stack, List<ISorNode> order)
            throws SorInitializationException {
        if (stack.contains(nodeId)) throw new SorInitializationException("Circular dependency: " + nodeId);
        if (!visited.contains(nodeId)) {
            ISorNode node = nodes.get(nodeId);
            if (node == null) throw new SorInitializationException("Unsatisfied dependency: " + nodeId);
            computeTopology(nodes, nodeId, node, visited, stack, order);
        }
    }

}
