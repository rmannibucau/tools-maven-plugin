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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Enables to decrypt a value.
 */
@Mojo(name = "decrypt-value", threadSafe = true)
public class DecryptMojo extends BaseCryptMojo {
    /**
     * Value to decrypt.
     */
    @Parameter(property = "yupiik.decrypt.value", required = true)
    private String value;

    /**
     * Should the clear value be printed using maven logger or directly in stdout.
     */
    @Parameter(property = "yupiik.decrypt.useStdout", defaultValue = "false")
    private boolean useStdout;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final var decrypted = codec().decrypt(value);
        if (useStdout) {
            System.out.println(decrypted);
        } else {
            getLog().info(decrypted);
        }
    }
}
