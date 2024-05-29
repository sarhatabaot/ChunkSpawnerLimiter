package com.cyprias.chunkspawnerlimiter.configs.impl;

import com.cyprias.chunkspawnerlimiter.configs.base.BaseConfig;

import java.io.File;
import java.util.Map;

public class BlocksConfig<T> extends BaseConfig {
    private boolean enabled;
    private Map<T, Integer> materialLimits;
    private boolean notifyMessage;
    private boolean notifyTitle;

    private int minY;
    private int maxY;

    public BlocksConfig(File dataFolder) {
        super("blocks.yml", dataFolder, "");
    }
}
