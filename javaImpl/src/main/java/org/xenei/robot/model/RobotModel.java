package org.xenei.robot.model;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.api.Layer.TrainingMode;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.nn.api.Model;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.learning.config.Sgd;
import org.xenei.robot.navigation.Position;


public class RobotModel {
    
    private static final int INPUTS = 5;
    private static final int OUTPUTS = 2;
    private static final int MIDSIZE = 16;
    
    private MultiLayerNetwork model;

    public RobotModel() {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .weightInit(WeightInit.XAVIER)
                .activation(Activation.RELU)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Sgd(0.05))
                // ... other hyperparameters
                .list( new DenseLayer.Builder().nIn(INPUTS).nOut(MIDSIZE).build(),
                        new DenseLayer.Builder().nIn(MIDSIZE).nOut(MIDSIZE).build(),
                        new DenseLayer.Builder().nIn(MIDSIZE).nOut(MIDSIZE).build(),
                        new OutputLayer.Builder().nIn(MIDSIZE).nOut(OUTPUTS).build()
                        )
                .backpropType(BackpropType.Standard)
                .build();
        
        this.model = new MultiLayerNetwork(conf);
        this.model.init();
    }
    
    public INDArray learn(INDArray start, INDArray expected) {
        INDArray result =  model.output(start, TrainingMode.TRAIN);
        model.fit(start, expected);
        return result;
    }
    
    public void score(double score) {
        model.setScore(score);
    }
}
