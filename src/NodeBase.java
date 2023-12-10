import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import peersim.cdsim.CDProtocol;
import peersim.core.Node;

/**
 * Base class that implements basic distribued learning functionality. Protocols
 * and strategies should extend this class and implement the shareWeights()
 * method.
 */
public abstract class NodeBase implements CDProtocol {
    
    /**
     * Paths to PyTorch scripts from the project root.
     */
    private static final String initScriptPath = "modules/init.py";
    private static final String trainScriptPath = "modules/trian.py";
    private static final String testScriptPath = "modules/test.py";

    /**
     * The weights for the current model iteration.
     */
    protected ArrayList<Float> modelWeights;

    /**
     * The models that this node has reveiced from peers since the last cycle.
     */
    protected ArrayDeque<ArrayList<Float>> receivedModels;

    /**
     * The test accuracy and loss calucated after each training cycle.
     */
    protected double testAccuracy;
    protected double testLoss;

    /**
     * Tracks whether to train this cycle or share weights.
     */
    private boolean trainCycle = true;

    /**
     * Tracks how many training cycles have been completed.
     */
    private int currentIteration = 0;

    /**
     * Shares this model's weights throughout the network.
     * 
     * This method should be implemented by child classes.
     */
    public abstract void shareWeights(Node node, int protocolID);

    @Override
    public abstract Object clone();

    public NodeBase(String name) {
        try {
            InputStream input = runScript(initScriptPath, 0, false);
            modelWeights = parseWeights(input);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize weights");
        }
    }

    /**
     * Alternates between training and running the share weights protocol deined
     * by child classes. When a protocol is ready to go back to training, setTrain()
     * should be called. If training should be performed every other cycle,
     * children should call setTrain() every time shareWeights() is called.
     */
    @Override
    public void nextCycle(Node node, int protocolID) {
        if (trainCycle) {
            train(node.getIndex());
            currentIteration++;
            trainCycle = false;
        } else {
            shareWeights(node, protocolID);
        }
        test(node.getIndex());
    }

    /**
     * Sets the next cycle to be a training iteration.
     */
    protected void setTrain() {
        trainCycle = true;
    }

    /**
     * @return The current test accuracy after the last training iteration.
     */
    public double getTestAccuracy() {
        return testAccuracy;
    }

    /**
     * Receives the given weights and adds them to a queue.
     */
    public void pushWeights(ArrayList<Float> weights) {
        receivedModels.add(weights);
    }

    /**
     * Performs a training iteration
     */
    private void train(int id) {
        try {
            InputStream scriptOutput = runScript(trainScriptPath, id, true, Integer.toString(currentIteration),
                    Integer.toString(Constants.ITERATIONS));
            modelWeights = parseWeights(scriptOutput);
        } catch (IOException e) {
            System.err.println("Failed to run " + trainScriptPath);
            e.printStackTrace();
        }
    }

    /**
     * Calculates the test accuracy with the current model weights and stores it
     * in testAccuracy and testLoss.
     */
    private void test(int id) {
        try {
            InputStream scriptOutput = runScript(testScriptPath, id, true);
            Scanner scanner = new Scanner(scriptOutput);
            testAccuracy = scanner.nextDouble();
            testLoss = scanner.nextDouble();
            scanner.close();
        } catch (IOException e) {
            System.err.println("Failed to run " + testScriptPath);
            e.printStackTrace();
        }
    }

    private InputStream runScript(String scriptPath, int id, boolean sendWeights, String... extraArgs)
            throws IOException {
        // Setup arguments.
        String[] constArgs = { "python3", scriptPath, Integer.toString(modelWeights.size()),
                Integer.toString(Constants.NETWORK_SIZE), Integer.toString(id) };
        String[] args = Arrays.copyOf(constArgs, constArgs.length + extraArgs.length);
        for (int i = constArgs.length; i < args.length; i++) {
            args[i] = extraArgs[i - constArgs.length];
        }

        // Initialize process builder redirecting stderr to terminal.
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

        // Start the process.
        Process script = processBuilder.start();
        InputStream input = script.getInputStream();
        OutputStream output = script.getOutputStream();

        // Send weights to stdin if requested.
        if (sendWeights) {
            for (int i = 0; i < modelWeights.size(); i++) {
                byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(modelWeights.get(i))
                        .array();
                output.write(bytes);
            }
            output.flush();
        }

        return input;
    }

    private ArrayList<Float> parseWeights(InputStream input) throws IOException {
        ArrayList<Float> weights = new ArrayList<>();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            for (int i = 0; i < bytesRead; i += 4) {
                byte[] bytes = { (byte) buffer[i], (byte) buffer[i + 1], (byte) buffer[i + 2], (byte) buffer[i + 3] };
                float weight = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                weights.add(weight);
            }
        }

        return weights;
    }
}
