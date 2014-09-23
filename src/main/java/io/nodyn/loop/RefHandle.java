/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package io.nodyn.loop;

/**
 * @author Bob McWhirter
 */
public class RefHandle {

    private final RefCounted refCounted;
    private final String name;
    private boolean counted;

    public RefHandle(RefCounted refCounted, String name) {
        this( refCounted, true, name );
    }

    public RefHandle(RefCounted refCounted, boolean count, String name) {
        this.refCounted = refCounted;
        this.name = name;
        if ( count ) {
            ref();
        }
    }

    public RefHandle create(String name) {
        return this.refCounted.newHandle( name );
    }

    public RefHandleHandler handler() {
        return new RefHandleHandler( this );
    }

    public synchronized void ref() {
        if ( this.counted ) {
            return;
        }

        this.counted = true;

        this.refCounted.incrCount( this );
    }

    public synchronized void unref() {
        if ( ! this.counted ) {
            return;
        }

        this.counted = false;

        this.refCounted.decrCount( this );
    }

    public String toString() {
        return this.name;
    }


}
