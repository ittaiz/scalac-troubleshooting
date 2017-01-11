package com.wix.indirect_scala

import com.wix.direct.ClassUsingFoo

class ClassIndirectlyNeedingFoo(val classUsingFoo: ClassUsingFoo)