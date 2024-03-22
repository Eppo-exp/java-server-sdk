package com.eppo.sdk.helpers.bandit;

public class BanditModelFactory {

    public static BanditModel build(String modelName) {
        switch(modelName) {
            case RandomBanditModel.MODEL_IDENTIFIER:
                return new RandomBanditModel();
            case FalconBanditModel.MODEL_IDENTIFIER:
                return new FalconBanditModel();
            default:
                throw new IllegalArgumentException("Unknown bandit model " + modelName);
        }
    }
}
