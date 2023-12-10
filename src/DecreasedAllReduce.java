import java.util.ArrayList;

import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * This class implements the AllReduce scheme for communicating weight within
 * the network with a constant decreased aggregration frequency.
 */
public class DecreasedAllReduce extends AllReduce {

    /**
     * The cycle frequency in which to perform all reduce.
     */
    private final int SHARE_FREQUENCY;

    private int cycle_count = 0;

    public DecreasedAllReduce(String prefix) {
        super(prefix);
        SHARE_FREQUENCY = Configuration.getInt("protocol.avg.freq");
    }

    @Override
    public void shareWeights(Node node, int protocolID) {
        cycle_count++;

        if (cycle_count % SHARE_FREQUENCY != 0) {
            setTrain();
            return;
        }

        super.shareWeights(node, protocolID);
    }

    @Override
    public Object clone() {
        return new DecreasedAllReduce("DecreasedAllReduce");
    }
}
