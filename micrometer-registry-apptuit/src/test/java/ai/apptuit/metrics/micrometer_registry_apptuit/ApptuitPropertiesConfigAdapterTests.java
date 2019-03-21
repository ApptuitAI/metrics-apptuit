/*
 * Copyright 2017 Agilx, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.apptuit.metrics.micrometer_registry_apptuit;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApptuitPropertiesConfigAdapterTests {

    @Test
    public void whenPropertiesTokenIsSetAdapterTokenReturnsIt() {
        ApptuitProperties properties = new ApptuitProperties();
        properties.settoken("123456");
        assertThat(new ApptuitPropertiesConfigAdapter(properties).token())
                .isEqualTo("123456");
    }
}