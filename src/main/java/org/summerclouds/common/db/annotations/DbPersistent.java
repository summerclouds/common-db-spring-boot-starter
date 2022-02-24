/**
 * Copyright (C) 2002 Mike Hummel (mh@mhus.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.summerclouds.common.db.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Mark a getter or setter with this annotation to define it as a column in the database.
 * DbPrimaryKey will have the same effect. If you mark a getter or setter as persistent both
 * functions getter and setter must exist and be public. Fou boolenas the getter can also have a
 * 'is...' notation.
 *
 * @author mikehummel
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface DbPersistent {
    int size() default 201;

    String more() default "";

    DbType.TYPE type() default DbType.TYPE.UNKNOWN;

    boolean nullable() default true;

    boolean auto_id() default false;

    boolean virtual() default false;

    String[] features() default {};

    String[] hints() default {};

    String description() default "";

    boolean ro() default false;
}
