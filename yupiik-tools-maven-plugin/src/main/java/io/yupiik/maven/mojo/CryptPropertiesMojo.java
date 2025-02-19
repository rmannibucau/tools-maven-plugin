/*
 * Copyright (c) 2020 - 2023 - Yupiik SAS - https://www.yupiik.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.yupiik.maven.mojo;

import io.yupiik.tools.codec.Codec;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Properties;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * Enables to crypt a properties file.
 */
@Mojo(name = "crypt-properties", threadSafe = true)
public class CryptPropertiesMojo extends BaseCryptPropertiesMojo {
    /**
     * If true and output exists, it will be read to compare the encrypted values and keep them if they didnt change.
     */
    @Parameter(property = "yupiik.crypt-properties.reduceDiff", defaultValue = "true")
    protected boolean reduceDiff;

    @Override
    protected void transform(final Codec codec, final Properties from, final Properties to) {
        final var existing = new Properties();
        final var out = output.toPath();
        if (reduceDiff && Files.exists(out)) {
            try (final var reader = Files.newBufferedReader(out)) {
                existing.load(reader);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
        to.putAll(from.stringPropertyNames().stream().collect(toMap(identity(), e -> {
            final var value = from.getProperty(e, "");
            if (codec.isEncrypted(value)) {
                return value;
            }

            final var existingValue = existing.getProperty(e);
            if (existingValue != null && codec.isEncrypted(existingValue) && equals(codec, existingValue, value)) {
                return existingValue;
            }
            return codec.encrypt(value);
        })));
    }

    private boolean equals(final Codec codec, final String existingValue, final String value) {
        try {
            return Objects.equals(value, codec.decrypt(existingValue));
        } catch (final RuntimeException re) {
            return false;
        }
    }
}
