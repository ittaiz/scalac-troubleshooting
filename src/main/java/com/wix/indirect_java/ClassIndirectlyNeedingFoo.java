package com.wix.indirect_java;

import com.wix.direct.ClassUsingFoo;

class ClassIndirectlyNeedingFoo {
    private final ClassUsingFoo classUsingFoo;

    public ClassIndirectlyNeedingFoo(ClassUsingFoo classUsingFoo){

        this.classUsingFoo = classUsingFoo;
    }

    public ClassUsingFoo getClassUsingFoo() {
        return classUsingFoo;
    }
}