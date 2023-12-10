import java.util.ArrayList;

import peersim.core.IdleProtocol;

/**
 * This class defines the network for the simulation. It can be queried to get
 * the neighbors for a particular node or used to send weights to another node.
 */
public class NetworkLinkable extends IdleProtocol {

    /**
     * The sum of latencies generated by this node.
     */
    private double sendLatencies;

    public NetworkLinkable(String name) {
        super(name);
    }

    /**
     * Sends the given weights to the node at 
     */
    public double sendTo(int index, ArrayList<Float> weights) {
        NodeBase receiver = (NodeBase) neighbors[index];
        receiver.pushWeights(weights);

        // TODO: Generate random latency.
        double latency = 0.0;

        sendLatencies += latency;

        return latency;
    }
}
