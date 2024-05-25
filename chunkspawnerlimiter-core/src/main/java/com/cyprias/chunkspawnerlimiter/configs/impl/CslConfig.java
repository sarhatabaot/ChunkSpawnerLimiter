package com.cyprias.chunkspawnerlimiter.configs.impl;

import com.cyprias.chunkspawnerlimiter.configs.base.BaseConfig;

import java.io.File;

public class CslConfig extends BaseConfig {

    public CslConfig(File dataFolder) {
        super("config.yml", dataFolder, "");
    }


}
