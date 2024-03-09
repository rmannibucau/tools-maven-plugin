/*
 * Copyright (c) 2020 - present - Yupiik SAS - https://www.yupiik.com
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
import { useNavigate } from "react-router-dom";

export function BadgeButton({ icon, buttonType, onClick }) {
    const navigate = useNavigate();
    return (
        <button
            className={`btn btn-outline-${buttonType} btn-sm`}
            type="submit" style={{
                padding: '0 0 0 0.3rem',
                border: 'none',
                margin: '0',
            }}
            onClick={() => onClick(navigate)}>
            <svg className="bi me-2" width="16" height="16">
                <use xlinkHref={icon}></use>
            </svg>
        </button>
    );
}
