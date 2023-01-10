/**
 * Copyright (C) 2022 Mike Hummel (mh@mhus.de)
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
package org.summerclouds.common.db;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.lang.ICloseable;
import org.summerclouds.common.core.util.Table;

/**
 * Represents a collection of results.
 *
 * @author mikehummel
 * @param <O>
 */
public interface DbCollection<O> extends Iterable<O>, Iterator<O>, ICloseable {

    /**
     * If recycle is on the original container object will be used for every iteration. If it's off
     * then every time a new object will be created. Default is off.
     *
     * <p>Not every implementation supports recycling.
     *
     * @param on
     * @return x
     */
    DbCollection<O> setRecycle(boolean on);

    boolean isRecycle();

    O current() throws MException;

    @SuppressWarnings({"rawtypes", "unchecked"})
    default void addToList(List list) {
        for (O o : this) {
            list.add(o);
        }
    }

    default List<O> toCacheAndClose() {
        List<O> list = new LinkedList<O>();
        try {
            addToList(list);
        } finally {
            close();
        }
        return list;
    }

    default List<O> toCacheAndClose(int max) {
        List<O> list = new LinkedList<O>();
        try {
            for (O o : this) {
                list.add(o);
                if (max >= 0 && list.size() >= max) break;
            }
        } finally {
            close();
        }
        return list;
    }

    default O[] toArrayAndClose(O[] dummy) {
        List<O> list = toCacheAndClose();
        return list.toArray(dummy);
    }

    /**
     * Transfer Objects to a table view.
     *
     * @param maxSize More the zero, zero or less will disable the parameter
     * @return x
     */
    Table toTableAndClose(int maxSize);

    public default O getNextAndClose() {
        try {
            return hasNext() ? next() : null;
        } finally {
            close();
        }
    }

    default boolean skip(int cnt) { // TODO optimize, do not fully load objects
        for (int i = 0; i < cnt && hasNext(); i++) next();
        return hasNext();
    }
}
