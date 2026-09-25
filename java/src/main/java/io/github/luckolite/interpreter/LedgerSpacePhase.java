// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** A skewed oval may sit toward a nonexistent outer ledger, while two inner rules prove a space. */
final class LedgerSpacePhase {
    private LedgerSpacePhase() { }
    static int resolve(float position,float headY,float gap,float outer,float inner,float second) {
        int original=Math.round(position);
        if(!Float.isFinite(position)||!Float.isFinite(headY)||!Float.isFinite(gap)||gap<8
                ||Float.isFinite(outer)||!Float.isFinite(inner)||!Float.isFinite(second)
                ||original%2!=0||original>-2&&original<10)return original;
        int inward=original<0?1:-1;
        float toward=(position-original)*inward;
        if(toward<.2f||toward>.5f||Math.abs(Math.abs(inner-second)-gap)>gap*.12f)return original;
        if((inner-second)*inward<=0)return original;
        float distance=(headY-inner)*inward/gap;
        if(distance<.55f||distance>.95f)return original;
        return original+inward;
    }
}
