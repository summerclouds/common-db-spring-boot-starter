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
package org.summerclouds.common.db.transaction;

import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.db.annotations.DbTransactionable;
import org.summerclouds.common.db.annotations.TransactionConnection;

public class TransactionPool extends MLog {

    private static TransactionPool instance;
    private ThreadLocal<LockBase> lock = new ThreadLocal<>();
    private ThreadLocal<Encapsulation> encapsulate = new ThreadLocal<>();

    public static synchronized TransactionPool instance() {
        if (instance == null) instance = new TransactionPool();
        return instance;
    }

    /**
     * Return current transaction, if cascaded transactions, return the last/current
     *
     * @return x
     */
    public LockBase getLock() {
        //		synchronized (pool) {
        LockBase out = lock.get();
        LockBase nested = out.getNested();
        if (nested != null) return nested;
        return out;
        //		}
    }

    /**
     * Return current transaction, if cascaded transactions, return the base
     *
     * @return x
     */
    public LockBase getLockBase() {
        //		synchronized (pool) {
        LockBase out = lock.get();
        return out;
        //		}
    }

    public void releaseLock() {
        //		synchronized (pool) {
        try {
            LockBase out = lock.get();
            if (out == null) return;
            LockBase nested = out.popNestedLock();
            if (nested == null) {
                log().d("releaseLock", out);
                lock.remove();
                out.release();
            } else {
                log().d("releaseLock nested", out, nested);
            }
        } catch (Throwable t) {
            log().w(t);
        }
        //		}
    }

    public void lock(long timeout, LockBase transaction) {
        //		synchronized (pool) {
        LockBase current = lock.get();
        if (current != null) {
            if (!current.isLocked()) {
                lock.remove();
                current = null;
            }
        }
        if (current != null) {
            current.pushNestedLock(transaction);
            log().d("lock nested", current, transaction);
        } else {
            lock.set(transaction);
            transaction.lock(timeout);
            log().d("lock", transaction);
        }
        //		}
    }

    public boolean encapsulate(DbTransactionable owner) {
        //		synchronized (encapsulate) {
        Encapsulation enc = encapsulate.get();
        if (enc == null) {
            enc = new Encapsulation();
            encapsulate.set(enc);
        }
        return enc.append(owner);
        //		}
    }

    public void releaseEncapsulate() {
        Encapsulation enc = encapsulate.get();
        if (enc == null) return;
        enc.clear();
        encapsulate.remove();
    }

    public TransactionConnection getConnection(DbTransactionable owner) {
        Encapsulation enc = encapsulate.get();
        if (enc == null) return null;
        return enc.getCurrent(owner);
    }

    public boolean commitAndRelease() {
        boolean res = commit();
        releaseEncapsulate();
        return res;
    }

    public boolean rollbackAndRelease() {
        boolean res = rollback();
        releaseEncapsulate();
        return res;
    }

    public boolean commit() {
        Encapsulation enc = encapsulate.get();
        if (enc == null) {
            log().d("encapsulate not set - ignore commit");
            return false;
        }
        return enc.commit();
    }

    public boolean rollback() {
        Encapsulation enc = encapsulate.get();
        if (enc == null) return false;
        return enc.rollback();
    }

    public void clear() {
        lock.remove();
        Encapsulation enc = encapsulate.get();
        if (enc != null) enc.clear();
        encapsulate.remove();
    }

    public boolean isInTransaction(DbTransactionable owner) {
        Encapsulation enc = encapsulate.get();
        if (enc == null) return false;
        return enc.isInTransaction(owner);
    }
}
