/* Licensed under the Apache License, Version 2.0 (the "License");
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
package org.camunda.tngp.broker.incident.data;

public enum IncidentEventType
{
    CREATE(0),
    CREATED(1),

    RESOLVE(2),
    RESOLVED(3),
    RESOLVE_REJECTED(4),
    RESOLVE_FAILED(5),

    DELETE(6),
    DELETED(7),
    DELETE_REJECTED(8);

    // don't change the ids because the incident stream processor use them for the index
    private final int id;

    IncidentEventType(int id)
    {
        this.id = id;
    }

    public int id()
    {
        return id;
    }
}
