/*
 * Copyright 2019 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.launcher.config;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A {@link Service} to read a config file from the disk.
 * On success the {@link Config} instance is validated
 * and provided back to the {@link ConfigManager}.
 */
class ConfigReader extends Service<Void> {
    private static final Logger logger = LoggerFactory.getLogger(ConfigReader.class);

    private final ConfigManager manager;
    private final ConfigValidator validator;

    ConfigReader(ConfigManager manager) {
        this.manager = manager;
        validator = new ConfigValidator(manager.getConfig());
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() {
                final Path configFile = manager.getConfigPath();
                if (Files.exists(configFile)) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(Files.newInputStream(configFile))
                    )) {
                        final Config config = manager.getGson().fromJson(reader, Config.Builder.class)
                                .launcherDir(manager.getLauncherDir())
                                .build();
                        manager.setConfig(validator.validate(config));
                    } catch (IOException e) {
                        logger.error("Failed to read config file: {}", configFile);
                    }
                } else {
                    logger.warn("Config file was not found: {}", configFile);
                }
                return null;
            }
        };
    }

    @Override
    protected void succeeded() {
        logger.debug("Loaded config file: {}", manager.getConfigPath());
    }
}
