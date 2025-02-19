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
import io.yupiik.tools.codec.simple.SimpleCodec;
import io.yupiik.tools.codec.simple.SimpleCodecConfiguration;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

// we can reuse maven decryption there but we actually want something we can make it evolving independently
// and reuseable without maven (codec-core module)
// so we just wrap codec-core here
public abstract class BaseCryptMojo extends AbstractMojo {
    /**
     * Master password for the enryption (AES/CBC/PKCS5Padding).
     */
    @Parameter(property = "yupiik.crypt.masterPassword", required = true)
    protected String masterPassword;

    protected Codec codec() {
        return new SimpleCodec(SimpleCodecConfiguration.builder()
                .masterPassword(masterPassword)
                .build());
    }
}
