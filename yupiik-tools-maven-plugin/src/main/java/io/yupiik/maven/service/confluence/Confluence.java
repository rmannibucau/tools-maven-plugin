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
package io.yupiik.maven.service.confluence;

import lombok.Data;
import org.apache.maven.plugins.annotations.Parameter;

@Data
public class Confluence {
    @Parameter(property = "yupiik.minisite.confluence.ignore")
    private boolean ignore;

    @Parameter(property = "yupiik.minisite.confluence.url")
    private String url;

    @Parameter(property = "yupiik.minisite.confluence.serverId")
    private String serverId;

    // basic ou bearer precomputed with its prefix - to enable the switch between both
    @Parameter(property = "yupiik.minisite.confluence.authorization")
    private String authorization;

    @Parameter(property = "yupiik.minisite.confluence.space")
    private String space;

    @Parameter(property = "yupiik.minisite.confluence.skipIndex")
    private boolean skipIndex;
}
