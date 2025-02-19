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
package io.yupiik.asciidoc.parser.resolver;

import io.yupiik.asciidoc.parser.internal.LocalContextResolver;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * The resolver enables to resolve a document by its (often relative) path, for example used by xref implementation.
 */
@FunctionalInterface
public interface ContentResolver {
    Optional<List<String>> resolve(String ref, Charset encoding);

    /**
     * Creates a content resolver.
     *
     * @param base the base to resolve relative references from.
     * @return an instance of local content resolver.
     */
    static ContentResolver of(final Path base) {
        return new LocalContextResolver(base);
    }
}
