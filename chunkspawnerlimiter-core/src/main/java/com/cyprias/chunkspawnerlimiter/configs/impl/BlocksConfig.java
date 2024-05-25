package com.cyprias.chunkspawnerlimiter.configs.impl;

import com.cyprias.chunkspawnerlimiter.configs.base.BaseConfig;

import java.io.File;

public class BlocksConfig extends BaseConfig {


    public BlocksConfig(File dataFolder) {
        super("blocks.yml", dataFolder, "");
    }
}
