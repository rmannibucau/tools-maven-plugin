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

import io.yupiik.maven.properties.LightProperties;
import io.yupiik.tools.codec.Codec;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.Collections.enumeration;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public abstract class BaseCryptPropertiesMojo extends BaseCryptMojo {
    /**
     * Input properties file path.
     */
    @Parameter(property = "yupiik.crypt-properties.input", required = true)
    protected File input;

    /**
     * Target location of the encrypted properties file.
     */
    @Parameter(property = "yupiik.crypt-properties.output", required = true)
    protected File output;

    /**
     * Should properties structure be preserved (comments, order) or not.
     * This can break properties structure in some rare cases, if so disable this.
     */
    @Parameter(property = "yupiik.crypt-properties.preserveComments", defaultValue = "true")
    protected boolean preserveComments;

    /**
     * List of keys to encrypt the value for.
     */
    @Parameter(property = "yupiik.crypt-properties.includedKeys")
    protected List<String> includedKeys;

    /**
     * List of keys to not encrypt the value for.
     */
    @Parameter(property = "yupiik.crypt-properties.excludedKeys")
    protected List<String> excludedKeys;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final var from = input.toPath();
        if (Files.notExists(from)) {
            throw new IllegalArgumentException("Missing '" + from + "'");
        }

        try {
            final var input = new LightProperties(getLog());
            try (final var read = Files.newBufferedReader(from)) {
                input.load(read, !preserveComments);
            }
            final var inputProps = input.toWorkProperties();

            final var keyFilter = createKeyFilter();

            final var untouched = new Properties();
            untouched.putAll(inputProps.stringPropertyNames().stream()
                    .filter(Predicate.not(keyFilter))
                    .collect(toMap(identity(), inputProps::getProperty)));

            final var transformedSource = new Properties();
            transformedSource.putAll(inputProps.stringPropertyNames().stream()
                    .filter(keyFilter)
                    .collect(toMap(identity(), inputProps::getProperty)));

            final var transformed = new Properties() { // sorted...depends jvm version so override most of them
                @Override
                public Set<String> stringPropertyNames() {
                    return super.stringPropertyNames().stream().sorted().collect(toCollection(LinkedHashSet::new));
                }

                @Override
                public Enumeration<Object> keys() {
                    return enumeration(Collections.list(super.keys()).stream()
                            .sorted(Comparator.comparing(Object::toString))
                            .collect(toList()));
                }

                @Override
                public Set<Map.Entry<Object, Object>> entrySet() {
                    return super.entrySet().stream()
                            .sorted(Comparator.comparing(a -> a.getKey().toString()))
                            .collect(toCollection(LinkedHashSet::new));
                }
            };
            transform(codec(), transformedSource, transformed);
            if (!untouched.isEmpty()) {
                transformed.putAll(untouched);
            }

            final var to = output.toPath();
            if (to.getParent() != null) {
                Files.createDirectories(to.getParent());
            }
            final var outputWriter = Files.newBufferedWriter(to);
            if (preserveComments) {
                try (outputWriter) {
                    input.write(transformed, outputWriter);
                }
            } else {
                try (final var write = new LightProperties.SimplePropertiesWriter(outputWriter)) {
                    transformed.store(write, "# generated by " + getClass().getSimpleName() + " yupiik-tools-maven-plugin");
                }
            }
        } catch (final IOException ioe) {
            throw new MojoFailureException(ioe.getMessage(), ioe);
        }
    }

    private Predicate<String> createKeyFilter() {
        final var hasNoIncludes = includedKeys == null || includedKeys.isEmpty();
        final var hasNoExcludes = excludedKeys == null || excludedKeys.isEmpty();
        if (hasNoIncludes && (hasNoExcludes)) {
            return s -> true;
        }
        if (hasNoExcludes) {
            return predicate(includedKeys);
        }
        if (hasNoIncludes) {
            return Predicate.not(predicate(excludedKeys));
        }
        return predicate(includedKeys).and(Predicate.not(predicate(excludedKeys)));
    }

    private Predicate<String> predicate(final List<String> includedKeys) {
        return includedKeys.stream()
                .map(it -> {
                    if (it.startsWith("starts:")) {
                        final var value = it.substring("starts:".length());
                        return (Predicate<String>) s -> s.startsWith(value);
                    }
                    if (it.startsWith("regex:")) {
                        return Pattern.compile(it.substring("regex:".length())).asPredicate();
                    }
                    return (Predicate<String>) s -> Objects.equals(s, it);
                })
                .reduce(it -> false, Predicate::or);
    }

    protected abstract void transform(Codec codec, Properties data, final Properties out);
}
