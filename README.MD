#Unexpected (only for me?) ScalaC behavior
This is a recreation of an issue I saw while trying to migrate a codebase from `maven` to `bazel`. One of the main focuses of this migration is to try to **minimize** the dependencies each class needs for compilation so that builds will be triggered only when needed.   
Unfortunately what I saw is that given `ClassIndirectlyNeedingFoo`(uses)->`ClassUsingFoo`(uses)->`Supplier` the compilation of `ClassIndirectlyNeedingFoo` breaks if `Supplier` is not on the classpath.    
Just to be clear `Supplier` isn't in the bytecode or source-code of `ClassIndirectlyNeedingFoo`.

#Update:
So the short answer is that `Why` this is happening isn't totally clear to [anyone](https://github.com/scalacenter/advisoryboard/blob/master/proposals/009-improve-direct-dependency-experience.md#proposal) (see proposal #4) . What is clear is that it's known scalac sometimes needs more dependencies than one would think and it's also clear that sometimes when this happens it's a bug.
For more info read my SO [answer](http://stackoverflow.com/questions/41600346/why-does-scalac-need-a-transitive-dependency-on-the-classpath)

#To recreate:
```shell
#When in the root of this repository:
#Create supplier.jar
cd src/main/java
javac com/foo/common/Supplier.java
jar -cf ../../../common_supplier.jar com/foo/common/Supplier.class
rm **/*.class
#Create direct.jar
scalac -classpath ../../../common_supplier.jar com/wix/direct/ClassUsingFoo.scala
jar -cf ../../../direct.jar com/wix/direct/ClassUsingFoo.class
rm **/*.class
#Show that indirect java compilation works
javac -classpath ../../../direct.jar:/usr/local/Cellar/scala/2.11.7/libexec/lib/scala-library.jar com/wix/indirect_java/ClassIndirectlyNeedingFoo.java
rm **/*.class
#Try to compile the indirect for scala
scalac -classpath ../../../direct.jar com/wix/indirect_scala/ClassIndirectlyNeedingFoo.scala
```
Error message:    
error: missing or invalid dependency detected while loading class file 'ClassUsingFoo.class'.
Could not access term foo in package com,
because it (or its dependencies) are missing. Check your build definition for
missing or conflicting dependencies. (Re-run with `-Ylog-classpath` to see the problematic classpath.)
A full rebuild may help if 'ClassUsingFoo.class' was compiled against an incompatible version of com.
one error found`
```shell
#Cleanup for the next phase
rm ../../../*.jar

#For some reason if the Supplier.java is on com/foo package everything passes... 
#Manually edit the import in ClassUsingFoo.scala from com.foo.common.Supplier to com.foo.Supplier
javac com/foo/Supplier.java
jar -cf ../../../foo_supplier.jar com/foo/Supplier.class
rm **/*.class
#create direct.jar
scalac -classpath ../../../foo_supplier.jar com/wix/direct/ClassUsingFoo.scala
jar -cf ../../../direct.jar com/wix/direct/ClassUsingFoo.class
rm **/*.class
#show that indirect java compilation works
javac -classpath ../../../direct.jar:/usr/local/Cellar/scala/2.11.7/libexec/lib/scala-library.jar com/wix/indirect_java/ClassIndirectlyNeedingFoo.java
#Passes
rm **/*.class
#Try to compile the indirect for scala
scalac -classpath ../../../direct.jar com/wix/indirect_scala/ClassIndirectlyNeedingFoo.scala
#Passes
```

Expanded error when running with `debug` flag:     
[running phase parser on ClassIndirectlyNeedingFoo.scala]
[running phase namer on ClassIndirectlyNeedingFoo.scala]
[running phase packageobjects on ClassIndirectlyNeedingFoo.scala]
[running phase typer on ClassIndirectlyNeedingFoo.scala]
[running phase patmat on ClassIndirectlyNeedingFoo.scala]
[running phase superaccessors on ClassIndirectlyNeedingFoo.scala]
[running phase extmethods on ClassIndirectlyNeedingFoo.scala]
[running phase pickler on ClassIndirectlyNeedingFoo.scala]
[running phase refchecks on ClassIndirectlyNeedingFoo.scala]
[running phase uncurry on ClassIndirectlyNeedingFoo.scala]
[running phase tailcalls on ClassIndirectlyNeedingFoo.scala]
[running phase specialize on ClassIndirectlyNeedingFoo.scala]
[running phase explicitouter on ClassIndirectlyNeedingFoo.scala]
[running phase erasure on ClassIndirectlyNeedingFoo.scala]
warning: !!! creating stub symbol to defer error: missing or invalid dependency detected while loading class file 'ClassUsingFoo.class'.
Could not access term foo in package class com,
because it (or its dependencies) are missing. Check your build definition for
missing or conflicting dependencies. (Re-run with `-Ylog-classpath` to see the problematic classpath.)
A full rebuild may help if 'ClassUsingFoo.class' was compiled against an incompatible version of com.
error: missing or invalid dependency detected while loading class file 'ClassUsingFoo.class'.
Could not access term foo in package class com,
because it (or its dependencies) are missing. Check your build definition for
missing or conflicting dependencies. (Re-run with `-Ylog-classpath` to see the problematic classpath.)
A full rebuild may help if 'ClassUsingFoo.class' was compiled against an incompatible version of com.
java.lang.Throwable
	at scala.reflect.internal.Symbols$StubSymbol$class.fail(Symbols.scala:3481)
	at scala.reflect.internal.Symbols$StubSymbol$class.info(Symbols.scala:3492)
	at scala.reflect.internal.Symbols$StubTermSymbol.info(Symbols.scala:3498)
	at scala.reflect.internal.Symbols$StubTermSymbol.info(Symbols.scala:3498)
	at scala.reflect.internal.pickling.UnPickler$Scan.scala$reflect$internal$pickling$UnPickler$Scan$$nestedObjectSymbol$1(UnPickler.scala:230)
	at scala.reflect.internal.pickling.UnPickler$Scan.readExtSymbol$1(UnPickler.scala:258)
	at scala.reflect.internal.pickling.UnPickler$Scan.readSymbol(UnPickler.scala:284)
	at scala.reflect.internal.pickling.UnPickler$Scan.readSymbolRef(UnPickler.scala:649)
	at scala.reflect.internal.pickling.UnPickler$Scan.readThisType$1(UnPickler.scala:398)
	at scala.reflect.internal.pickling.UnPickler$Scan.readType(UnPickler.scala:413)
	at scala.reflect.internal.pickling.UnPickler$Scan$$anonfun$readTypeRef$1.apply(UnPickler.scala:658)
	at scala.reflect.internal.pickling.UnPickler$Scan$$anonfun$readTypeRef$1.apply(UnPickler.scala:658)
	at scala.reflect.internal.pickling.UnPickler$Scan.at(UnPickler.scala:179)
	at scala.reflect.internal.pickling.UnPickler$Scan.readTypeRef(UnPickler.scala:658)
	at scala.reflect.internal.pickling.UnPickler$Scan.readType(UnPickler.scala:417)
	at scala.reflect.internal.pickling.UnPickler$Scan$LazyTypeRef$$anonfun$6.apply(UnPickler.scala:725)
	at scala.reflect.internal.pickling.UnPickler$Scan$LazyTypeRef$$anonfun$6.apply(UnPickler.scala:725)
	at scala.reflect.internal.pickling.UnPickler$Scan.at(UnPickler.scala:179)
	at scala.reflect.internal.pickling.UnPickler$Scan$LazyTypeRef.completeInternal(UnPickler.scala:725)
	at scala.reflect.internal.pickling.UnPickler$Scan$LazyTypeRef.complete(UnPickler.scala:749)
	at scala.reflect.internal.Symbols$Symbol.info(Symbols.scala:1514)
	at scala.reflect.internal.Symbols$Symbol.tpe_$times(Symbols.scala:1478)
	at scala.reflect.internal.Symbols$Symbol.tpe(Symbols.scala:1443)
	at scala.reflect.internal.Types$$anonfun$45.apply(Types.scala:4577)
	at scala.reflect.internal.Types$$anonfun$45.apply(Types.scala:4577)
	at scala.reflect.internal.util.Collections$class.mapList(Collections.scala:52)
	at scala.reflect.internal.SymbolTable.mapList(SymbolTable.scala:16)
	at scala.tools.nsc.transform.SpecializeTypes.specialized
```


By the way the above was run using Scala 2.11.7; To verify this isn't a solved issue I upgraded via brew to 2.12.1 and recreated-
```scala
error: java.lang.IllegalArgumentException: requirement failed: package common
	at scala.reflect.internal.Types$ModuleTypeRef.<init>(Types.scala:1879)
	at scala.reflect.internal.Types$PackageTypeRef.<init>(Types.scala:1897)
	at scala.reflect.internal.Types$TypeRef$.apply(Types.scala:2401)
	at scala.reflect.internal.Types.typeRef(Types.scala:3553)
	at scala.reflect.internal.Types.typeRef$(Types.scala:3536)
	at scala.reflect.internal.SymbolTable.typeRef(SymbolTable.scala:16)
	at scala.reflect.internal.Symbols$TypeSymbol.newTypeRef(Symbols.scala:3026)
	at scala.reflect.internal.Symbols$TypeSymbol.updateTypeCache(Symbols.scala:3079)
	at scala.reflect.internal.Symbols$TypeSymbol.maybeUpdateTypeCache(Symbols.scala:3065)
	at scala.reflect.internal.Symbols$TypeSymbol.tpe_$times(Symbols.scala:3043)
	at scala.reflect.internal.Symbols$Symbol.typeOfThis(Symbols.scala:2020)
	at scala.reflect.internal.Types$ThisType.underlying(Types.scala:1184)
	at scala.reflect.internal.Types$SimpleTypeProxy.boundSyms(Types.scala:150)
	at scala.reflect.internal.Types$SimpleTypeProxy.boundSyms$(Types.scala:150)
	at scala.reflect.internal.Types$SingletonType.boundSyms(Types.scala:1088)
	at scala.reflect.internal.tpe.TypeMaps$SubstMap.apply(TypeMaps.scala:734)
	at scala.reflect.internal.tpe.TypeMaps$SubstSymMap.apply(TypeMaps.scala:797)
	at scala.reflect.internal.tpe.TypeMaps$TypeMap.mapOver(TypeMaps.scala:110)
	at scala.reflect.internal.tpe.TypeMaps$SubstSymMap.apply(TypeMaps.scala:791)
	at scala.reflect.internal.Types$Type.substSym(Types.scala:727)
	at scala.reflect.internal.Symbols$Symbol.$anonfun$substInfo$1(Symbols.scala:1547)
	at scala.reflect.internal.Symbols$Symbol$$Lambda$467/868964689.apply(Unknown Source)
	at scala.reflect.internal.Symbols$Symbol.modifyInfo(Symbols.scala:1540)
	at scala.reflect.internal.Symbols$Symbol.substInfo(Symbols.scala:1547)
	at scala.reflect.internal.Symbols.$anonfun$deriveSymbols$1(Symbols.scala:3532)
	at scala.reflect.internal.Symbols.deriveSymbols(Symbols.scala:3532)
	at scala.reflect.internal.Symbols.deriveSymbols$(Symbols.scala:3530)
	at scala.reflect.internal.SymbolTable.deriveSymbols(SymbolTable.scala:16)
	at scala.reflect.internal.Symbols.cloneSymbols(Symbols.scala:3592)
	at scala.reflect.internal.Symbols.cloneSymbols$(Symbols.scala:3591)
	at scala.reflect.internal.SymbolTable.cloneSymbols(SymbolTable.scala:16)
	at scala.reflect.internal.Symbols.cloneSymbolsAndModify(Symbols.scala:3603)
	at scala.reflect.internal.Symbols.cloneSymbolsAndModify$(Symbols.scala:3602)
	at scala.reflect.internal.SymbolTable.cloneSymbolsAndModify(SymbolTable.scala:16)
	at scala.reflect.internal.tpe.TypeMaps$TypeMap.mapOver(TypeMaps.scala:245)
	at scala.reflect.internal.tpe.TypeMaps$TypeMap.$anonfun$mapOver$1(TypeMaps.scala:128)
	at scala.reflect.internal.tpe.TypeMaps$TypeMap.mapOver(TypeMaps.scala:202)
	at scala.reflect.internal.transform.UnCurry$$anon$1.apply(UnCurry.scala:53)
	at scala.reflect.internal.transform.UnCurry.transformInfo(UnCurry.scala:154)
	at scala.reflect.internal.transform.UnCurry.transformInfo$(UnCurry.scala:148)
	at scala.tools.nsc.transform.UnCurry.transformInfo(UnCurry.scala:53)
	at scala.tools.nsc.transform.InfoTransform$Phase$$anon$1.transform(InfoTransform.scala:38)
	at scala.reflect.internal.Symbols$Symbol.rawInfo(Symbols.scala:1605)
	at scala.reflect.internal.Symbols$Symbol.info(Symbols.scala:1522)
	at scala.tools.nsc.transform.SpecializeTypes.$anonfun$specializeClass$31(SpecializeTypes.scala:808)
	at scala.tools.nsc.transform.SpecializeTypes.$anonfun$specializeClass$30(SpecializeTypes.scala:801)
	at scala.tools.nsc.transform.SpecializeTypes.specializeClass(SpecializeTypes.scala:799)
	at scala.tools.nsc.transform.SpecializeTypes.transformInfo(SpecializeTypes.scala:1215)
	at scala.tools.nsc.transform.InfoTransform$Phase$$anon$1.transform(InfoTransform.scala:38)
	at scala.reflect.internal.Symbols$Symbol.rawInfo(Symbols.scala:1605)
	at scala.reflect.internal.Symbols$Symbol.info(Symbols.scala:1522)
	at scala.reflect.internal.Symbols$Symbol.isDerivedValueClass(Symbols.scala:812)
	at scala.reflect.internal.transform.Erasure$ErasureMap.apply(Erasure.scala:131)
	at scala.reflect.internal.transform.Erasure$ErasureMap.apply(Erasure.scala:144)
	at scala.reflect.internal.transform.Erasure.specialErasure(Erasure.scala:218)
	at scala.reflect.internal.transform.Erasure.specialErasure$(Erasure.scala:212)
	at scala.tools.nsc.transform.Erasure.specialErasure(Erasure.scala:15)
	at scala.reflect.internal.transform.Erasure.transformInfo(Erasure.scala:393)
	at scala.reflect.internal.transform.Erasure.transformInfo$(Erasure.scala:355)
	at scala.tools.nsc.transform.Erasure.transformInfo(Erasure.scala:15)
	at scala.tools.nsc.transform.InfoTransform$Phase$$anon$1.transform(InfoTransform.scala:38)
	at scala.reflect.internal.Symbols$Symbol.rawInfo(Symbols.scala:1605)
	at scala.reflect.internal.Symbols$Symbol.info(Symbols.scala:1522)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer.$anonfun$checkNoDeclaredDoubleDefs$2(Erasure.scala:845)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer$$Lambda$474/413601558.apply(Unknown Source)
	at scala.reflect.internal.Scopes$Scope.foreach(Scopes.scala:373)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer.$anonfun$checkNoDeclaredDoubleDefs$1(Erasure.scala:845)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer.checkNoDeclaredDoubleDefs(Erasure.scala:845)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer.scala$tools$nsc$transform$Erasure$ErasureTransformer$$checkNoDoubleDefs(Erasure.scala:872)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer$$anon$1.preErase(Erasure.scala:1161)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer$$anon$1.transform(Erasure.scala:1196)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer$$anon$1.transform(Erasure.scala:932)
	at scala.reflect.api.Trees$Transformer.transformTemplate(Trees.scala:2563)
	at scala.reflect.internal.Trees.$anonfun$itransform$4(Trees.scala:1412)
	at scala.reflect.internal.Trees$$Lambda$371/345281752.apply(Unknown Source)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer.atOwner(TypingTransformers.scala:30)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer.atOwner(TypingTransformers.scala:25)
	at scala.reflect.internal.Trees.itransform(Trees.scala:1411)
	at scala.reflect.internal.Trees.itransform$(Trees.scala:1340)
	at scala.reflect.internal.SymbolTable.itransform(SymbolTable.scala:16)
	at scala.reflect.internal.SymbolTable.itransform(SymbolTable.scala:16)
	at scala.reflect.api.Trees$Transformer.transform(Trees.scala:2555)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer.transform(TypingTransformers.scala:44)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer$$anon$1.transform(Erasure.scala:1217)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer$$anon$1.transform(Erasure.scala:932)
	at scala.reflect.api.Trees$Transformer.$anonfun$transformStats$1(Trees.scala:2589)
	at scala.reflect.api.Trees$Transformer.transformStats(Trees.scala:2587)
	at scala.reflect.internal.Trees.$anonfun$itransform$7(Trees.scala:1430)
	at scala.reflect.internal.Trees$$Lambda$370/1293618474.apply(Unknown Source)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer.atOwner(TypingTransformers.scala:30)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer.atOwner(TypingTransformers.scala:25)
	at scala.reflect.internal.Trees.itransform(Trees.scala:1430)
	at scala.reflect.internal.Trees.itransform$(Trees.scala:1340)
	at scala.reflect.internal.SymbolTable.itransform(SymbolTable.scala:16)
	at scala.reflect.internal.SymbolTable.itransform(SymbolTable.scala:16)
	at scala.reflect.api.Trees$Transformer.transform(Trees.scala:2555)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer.super$transform(TypingTransformers.scala:40)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer.$anonfun$transform$2(TypingTransformers.scala:42)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer$$Lambda$369/1205555397.apply(Unknown Source)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer.atOwner(TypingTransformers.scala:30)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer.transform(TypingTransformers.scala:25)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer$$anon$1.transform(Erasure.scala:1217)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer.transform(Erasure.scala:1227)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer.transform(Erasure.scala:805)
	at scala.tools.nsc.ast.Trees$Transformer.transformUnit(Trees.scala:140)
	at scala.tools.nsc.transform.Transform$Phase.apply(Transform.scala:30)
	at scala.tools.nsc.Global$GlobalPhase.$anonfun$applyPhase$1(Global.scala:416)
	at scala.tools.nsc.Global$GlobalPhase.applyPhase(Global.scala:409)
	at scala.tools.nsc.Global$GlobalPhase.$anonfun$run$1(Global.scala:380)
	at scala.tools.nsc.Global$GlobalPhase.$anonfun$run$1$adapted(Global.scala:380)
	at scala.tools.nsc.Global$GlobalPhase$$Lambda$259/796533847.apply(Unknown Source)
	at scala.collection.Iterator.foreach(Iterator.scala:929)
	at scala.collection.Iterator.foreach$(Iterator.scala:929)
	at scala.collection.AbstractIterator.foreach(Iterator.scala:1406)
	at scala.tools.nsc.Global$GlobalPhase.run(Global.scala:380)
	at scala.tools.nsc.Global$Run.compileUnitsInternal(Global.scala:1418)
	at scala.tools.nsc.Global$Run.compileUnits(Global.scala:1403)
	at scala.tools.nsc.Global$Run.compileSources(Global.scala:1398)
	at scala.tools.nsc.Global$Run.compile(Global.scala:1492)
	at scala.tools.nsc.MainClass.doCompile(Main.scala:24)
	at scala.tools.nsc.Driver.process(Driver.scala:55)
	at scala.tools.nsc.Driver.main(Driver.scala:68)
	at scala.tools.nsc.Main.main(Main.scala)
Exception in thread "main" java.lang.IllegalArgumentException: requirement failed: package common
	at scala.reflect.internal.Types$ModuleTypeRef.<init>(Types.scala:1879)
	at scala.reflect.internal.Types$PackageTypeRef.<init>(Types.scala:1897)
	at scala.reflect.internal.Types$TypeRef$.apply(Types.scala:2401)
	at scala.reflect.internal.Types.typeRef(Types.scala:3553)
	at scala.reflect.internal.Types.typeRef$(Types.scala:3536)
	at scala.reflect.internal.SymbolTable.typeRef(SymbolTable.scala:16)
	at scala.reflect.internal.Symbols$TypeSymbol.newTypeRef(Symbols.scala:3026)
	at scala.reflect.internal.Symbols$TypeSymbol.updateTypeCache(Symbols.scala:3079)
	at scala.reflect.internal.Symbols$TypeSymbol.maybeUpdateTypeCache(Symbols.scala:3065)
	at scala.reflect.internal.Symbols$TypeSymbol.tpe_$times(Symbols.scala:3043)
	at scala.reflect.internal.Symbols$Symbol.typeOfThis(Symbols.scala:2020)
	at scala.reflect.internal.Types$ThisType.underlying(Types.scala:1184)
	at scala.reflect.internal.Types$SimpleTypeProxy.boundSyms(Types.scala:150)
	at scala.reflect.internal.Types$SimpleTypeProxy.boundSyms$(Types.scala:150)
	at scala.reflect.internal.Types$SingletonType.boundSyms(Types.scala:1088)
	at scala.reflect.internal.tpe.TypeMaps$SubstMap.apply(TypeMaps.scala:734)
	at scala.reflect.internal.tpe.TypeMaps$SubstSymMap.apply(TypeMaps.scala:797)
	at scala.reflect.internal.tpe.TypeMaps$TypeMap.mapOver(TypeMaps.scala:110)
	at scala.reflect.internal.tpe.TypeMaps$SubstSymMap.apply(TypeMaps.scala:791)
	at scala.reflect.internal.Types$Type.substSym(Types.scala:727)
	at scala.reflect.internal.Symbols$Symbol.$anonfun$substInfo$1(Symbols.scala:1547)
	at scala.reflect.internal.Symbols$Symbol$$Lambda$467/868964689.apply(Unknown Source)
	at scala.reflect.internal.Symbols$Symbol.modifyInfo(Symbols.scala:1540)
	at scala.reflect.internal.Symbols$Symbol.substInfo(Symbols.scala:1547)
	at scala.reflect.internal.Symbols.$anonfun$deriveSymbols$1(Symbols.scala:3532)
	at scala.reflect.internal.Symbols.deriveSymbols(Symbols.scala:3532)
	at scala.reflect.internal.Symbols.deriveSymbols$(Symbols.scala:3530)
	at scala.reflect.internal.SymbolTable.deriveSymbols(SymbolTable.scala:16)
	at scala.reflect.internal.Symbols.cloneSymbols(Symbols.scala:3592)
	at scala.reflect.internal.Symbols.cloneSymbols$(Symbols.scala:3591)
	at scala.reflect.internal.SymbolTable.cloneSymbols(SymbolTable.scala:16)
	at scala.reflect.internal.Symbols.cloneSymbolsAndModify(Symbols.scala:3603)
	at scala.reflect.internal.Symbols.cloneSymbolsAndModify$(Symbols.scala:3602)
	at scala.reflect.internal.SymbolTable.cloneSymbolsAndModify(SymbolTable.scala:16)
	at scala.reflect.internal.tpe.TypeMaps$TypeMap.mapOver(TypeMaps.scala:245)
	at scala.reflect.internal.tpe.TypeMaps$TypeMap.$anonfun$mapOver$1(TypeMaps.scala:128)
	at scala.reflect.internal.tpe.TypeMaps$TypeMap.mapOver(TypeMaps.scala:202)
	at scala.reflect.internal.transform.UnCurry$$anon$1.apply(UnCurry.scala:53)
	at scala.reflect.internal.transform.UnCurry.transformInfo(UnCurry.scala:154)
	at scala.reflect.internal.transform.UnCurry.transformInfo$(UnCurry.scala:148)
	at scala.tools.nsc.transform.UnCurry.transformInfo(UnCurry.scala:53)
	at scala.tools.nsc.transform.InfoTransform$Phase$$anon$1.transform(InfoTransform.scala:38)
	at scala.reflect.internal.Symbols$Symbol.rawInfo(Symbols.scala:1605)
	at scala.reflect.internal.Symbols$Symbol.info(Symbols.scala:1522)
	at scala.tools.nsc.transform.SpecializeTypes.$anonfun$specializeClass$31(SpecializeTypes.scala:808)
	at scala.tools.nsc.transform.SpecializeTypes.$anonfun$specializeClass$30(SpecializeTypes.scala:801)
	at scala.tools.nsc.transform.SpecializeTypes.specializeClass(SpecializeTypes.scala:799)
	at scala.tools.nsc.transform.SpecializeTypes.transformInfo(SpecializeTypes.scala:1215)
	at scala.tools.nsc.transform.InfoTransform$Phase$$anon$1.transform(InfoTransform.scala:38)
	at scala.reflect.internal.Symbols$Symbol.rawInfo(Symbols.scala:1605)
	at scala.reflect.internal.Symbols$Symbol.info(Symbols.scala:1522)
	at scala.reflect.internal.Symbols$Symbol.isDerivedValueClass(Symbols.scala:812)
	at scala.reflect.internal.transform.Erasure$ErasureMap.apply(Erasure.scala:131)
	at scala.reflect.internal.transform.Erasure$ErasureMap.apply(Erasure.scala:144)
	at scala.reflect.internal.transform.Erasure.specialErasure(Erasure.scala:218)
	at scala.reflect.internal.transform.Erasure.specialErasure$(Erasure.scala:212)
	at scala.tools.nsc.transform.Erasure.specialErasure(Erasure.scala:15)
	at scala.reflect.internal.transform.Erasure.transformInfo(Erasure.scala:393)
	at scala.reflect.internal.transform.Erasure.transformInfo$(Erasure.scala:355)
	at scala.tools.nsc.transform.Erasure.transformInfo(Erasure.scala:15)
	at scala.tools.nsc.transform.InfoTransform$Phase$$anon$1.transform(InfoTransform.scala:38)
	at scala.reflect.internal.Symbols$Symbol.rawInfo(Symbols.scala:1605)
	at scala.reflect.internal.Symbols$Symbol.info(Symbols.scala:1522)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer.$anonfun$checkNoDeclaredDoubleDefs$2(Erasure.scala:845)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer$$Lambda$474/413601558.apply(Unknown Source)
	at scala.reflect.internal.Scopes$Scope.foreach(Scopes.scala:373)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer.$anonfun$checkNoDeclaredDoubleDefs$1(Erasure.scala:845)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer.checkNoDeclaredDoubleDefs(Erasure.scala:845)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer.scala$tools$nsc$transform$Erasure$ErasureTransformer$$checkNoDoubleDefs(Erasure.scala:872)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer$$anon$1.preErase(Erasure.scala:1161)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer$$anon$1.transform(Erasure.scala:1196)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer$$anon$1.transform(Erasure.scala:932)
	at scala.reflect.api.Trees$Transformer.transformTemplate(Trees.scala:2563)
	at scala.reflect.internal.Trees.$anonfun$itransform$4(Trees.scala:1412)
	at scala.reflect.internal.Trees$$Lambda$371/345281752.apply(Unknown Source)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer.atOwner(TypingTransformers.scala:30)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer.atOwner(TypingTransformers.scala:25)
	at scala.reflect.internal.Trees.itransform(Trees.scala:1411)
	at scala.reflect.internal.Trees.itransform$(Trees.scala:1340)
	at scala.reflect.internal.SymbolTable.itransform(SymbolTable.scala:16)
	at scala.reflect.internal.SymbolTable.itransform(SymbolTable.scala:16)
	at scala.reflect.api.Trees$Transformer.transform(Trees.scala:2555)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer.transform(TypingTransformers.scala:44)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer$$anon$1.transform(Erasure.scala:1217)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer$$anon$1.transform(Erasure.scala:932)
	at scala.reflect.api.Trees$Transformer.$anonfun$transformStats$1(Trees.scala:2589)
	at scala.reflect.api.Trees$Transformer.transformStats(Trees.scala:2587)
	at scala.reflect.internal.Trees.$anonfun$itransform$7(Trees.scala:1430)
	at scala.reflect.internal.Trees$$Lambda$370/1293618474.apply(Unknown Source)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer.atOwner(TypingTransformers.scala:30)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer.atOwner(TypingTransformers.scala:25)
	at scala.reflect.internal.Trees.itransform(Trees.scala:1430)
	at scala.reflect.internal.Trees.itransform$(Trees.scala:1340)
	at scala.reflect.internal.SymbolTable.itransform(SymbolTable.scala:16)
	at scala.reflect.internal.SymbolTable.itransform(SymbolTable.scala:16)
	at scala.reflect.api.Trees$Transformer.transform(Trees.scala:2555)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer.super$transform(TypingTransformers.scala:40)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer.$anonfun$transform$2(TypingTransformers.scala:42)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer$$Lambda$369/1205555397.apply(Unknown Source)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer.atOwner(TypingTransformers.scala:30)
	at scala.tools.nsc.transform.TypingTransformers$TypingTransformer.transform(TypingTransformers.scala:25)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer$$anon$1.transform(Erasure.scala:1217)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer.transform(Erasure.scala:1227)
	at scala.tools.nsc.transform.Erasure$ErasureTransformer.transform(Erasure.scala:805)
	at scala.tools.nsc.ast.Trees$Transformer.transformUnit(Trees.scala:140)
	at scala.tools.nsc.transform.Transform$Phase.apply(Transform.scala:30)
	at scala.tools.nsc.Global$GlobalPhase.$anonfun$applyPhase$1(Global.scala:416)
	at scala.tools.nsc.Global$GlobalPhase.applyPhase(Global.scala:409)
	at scala.tools.nsc.Global$GlobalPhase.$anonfun$run$1(Global.scala:380)
	at scala.tools.nsc.Global$GlobalPhase.$anonfun$run$1$adapted(Global.scala:380)
	at scala.tools.nsc.Global$GlobalPhase$$Lambda$259/796533847.apply(Unknown Source)
	at scala.collection.Iterator.foreach(Iterator.scala:929)
	at scala.collection.Iterator.foreach$(Iterator.scala:929)
	at scala.collection.AbstractIterator.foreach(Iterator.scala:1406)
	at scala.tools.nsc.Global$GlobalPhase.run(Global.scala:380)
	at scala.tools.nsc.Global$Run.compileUnitsInternal(Global.scala:1418)
	at scala.tools.nsc.Global$Run.compileUnits(Global.scala:1403)
	at scala.tools.nsc.Global$Run.compileSources(Global.scala:1398)
	at scala.tools.nsc.Global$Run.compile(Global.scala:1492)
	at scala.tools.nsc.MainClass.doCompile(Main.scala:24)
	at scala.tools.nsc.Driver.process(Driver.scala:55)
	at scala.tools.nsc.Driver.main(Driver.scala:68)
	at scala.tools.nsc.Main.main(Main.scala)
