package org.xenei.robot.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class SensorLayer {
    private static final Logger LOG = LoggerFactory.getLogger(SensorLayer.class);
    public static final byte DONT_CARE = (byte) 0xFF;
    private static final Random RANDOM;
    private SensorNeuron[] neurons;
    private BitMap lastResult;
    private byte lastAnswer;
    private byte lastTrigger;

    public enum Answer {
        FF, FS, FR, SF, SS, SR, RF, RS, RR, DONT_CARE;
    }

    static {
        RANDOM = new Random();
        RANDOM.setSeed(System.currentTimeMillis());
    }

    public SensorLayer(int numNeurons) {
        if (numNeurons > 8) {
            throw new IllegalArgumentException("numNeurons > 8");
        }
        neurons = new SensorNeuron[numNeurons];
        for (byte i = 0; i < numNeurons; i++) {
            neurons[i] = new SensorNeuron(i);
        }
        lastResult = new BitMap();
        lastAnswer = DONT_CARE;
    }

    public Answer getAnswer() {
        return lastAnswer == DONT_CARE ? Answer.DONT_CARE : Answer.values()[lastAnswer];
    }

    public byte getTrigger() {
        return lastTrigger;
    }

    public void load(String modelName) {
        File f = new File(modelName);
        if (f.exists()) {
            try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
                int numNeurons = in.readInt();
                neurons = new SensorNeuron[numNeurons];
                for (byte i = 0; i < numNeurons; i++) {
                    neurons[i] = new SensorNeuron(i);
                    neurons[i].read(in);
                }
            } catch (IOException e) {
                LOG.error("Error loading model: {}", modelName, e);
                throw new RuntimeException(e);
            }
        }
    }

    public void save(String modelName) throws IOException {
        File f = new File(modelName);
        if (f.exists()) {
            f.delete();
        }
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(f))) {
            out.write(neurons.length);
            for (SensorNeuron neuron : neurons) {
                neuron.write(out);
            }
        }
    }

    public void resetNeuron(int idx, byte pattern) {
        neurons[idx].reset(pattern);
    }

    private static class MyCollector implements Collector<BitMap, BitMap.Builder, BitMap> {

        @Override
        public Supplier<BitMap.Builder> supplier() {
            return BitMap.Builder::new;
        }

        @Override
        public BiConsumer<BitMap.Builder, BitMap> accumulator() {
            return BitMap.Builder::add;
        }

        @Override
        public BinaryOperator<BitMap.Builder> combiner() {
            return (left, right) -> left.add(right.build());
        }

        @Override
        public Function<BitMap.Builder, BitMap> finisher() {
            return BitMap.Builder::build;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of();
        }
    }

    public byte trigger(byte triggerMap) {
        lastTrigger = triggerMap;
        BitMap result = Arrays.stream(neurons).map(n -> n.trigger(triggerMap)).collect(new MyCollector());
        BitMap fullBitMap = new BitMap();

        // no triggers switched off
        if (result.equalTo(fullBitMap)) {
            lastResult = fullBitMap;
            lastAnswer = DONT_CARE;
        } else if (!result.equalTo(lastResult)) {
            int[] candidates = result.indices();
            int idx = RANDOM.nextInt(candidates.length);
            lastAnswer = (byte) candidates[idx];
        }
        return lastAnswer;
    }

    /**
     * Update the model with results from the next triggerMap.
     *
     * @param triggerMap
     *            the triggerMap after lastAnswer was applied.
     */
    public void feedback(byte triggerMap) {
        Arrays.stream(neurons).forEach(s -> s.feedback(lastAnswer, triggerMap));
    }

    public String toString() {
        return Arrays.stream(neurons).map(SensorNeuron::toString).collect(Collectors.joining(","));
    }
}
