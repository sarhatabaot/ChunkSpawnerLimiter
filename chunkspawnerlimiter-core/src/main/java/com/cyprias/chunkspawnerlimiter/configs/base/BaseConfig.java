package com.cyprias.chunkspawnerlimiter.configs.base;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;

public class BaseConfig extends BaseFile {
    protected YamlConfigurationLoader loader;
    protected CommentedConfigurationNode rootNode;

    //Load loader + default serializers, load rootNode for use.
    public BaseConfig(String fileName, File dataFolder, String resourcePath) {
        super(fileName, dataFolder, resourcePath);
    }

    pub


}
