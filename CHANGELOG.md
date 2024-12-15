## Rhizo 3.4 -> 3.5

Fix method invoke & Type consolidating

- fix invoking of native java method
    - ReflectASM is more buggy than expected, so it's now removed, fixing some method invoking problem
- type consolidating
    - variable types in native java method can now be consolidated so that type wrapper can be applied to them
    - for example method `compare(another: T): bool` for `Comparable<T>`, where the type `T` can be consolidated
- fix type wrapper for wrapping JS function to Java interface when there are overloaded methods
    - for example `create(id: string, type: string): Builder` and `create(id: string, modifier: Consumer<Builder>): void`

---

## Rhizo 3.3 -> 3.4

Generics & Better logging

- Generics support
    - Rhizo now provides support for using Java generics to make type wrapper more precise
    - For example, calling `list.add(3)` on `list` whose type is `List<Integer>` will now insert an integer 3 into this list
instead of inserting a `Double` which is very likely to cause problem. The same applies to Map and many more types
- better logging
    - Rhizo can now extract correct file name and line number when using compiler mode, no more `Thread.java:123456` in your logging
- Performance improvement for `JS functions as Java interface`
    - The invoking of interface proxy and abstract class proxy is simplified, reducing memory allocation and provides better
performance, because less object creations and computations are needed
    - This improvement will be more apparent if you have lots of callbacks as Java method parameters, for example `event => {...}`
- more helper methods in NBTUtils

---

## Rhizo 3.2 -> 3.3

Perf++ && Rest Parameter

- support for function rest parameter
    - e.g. `function a(arg1, arg2, ...restParamIsHere) {}`, you can call `a(1, 2)` or `a(1,2,3)` or `a(1,2,3,4)` or ...
- smaller and more efficient name remapper
    - thanks to the uniqueness of intermediary name, we can match field/class names directly, making 
remapping for these two more efficient and complete
    - mapping file is now at version 3, with a more compat storage format, so the Rhizo jar should be smaller now
- better NBT wrapper and AABB wrapper
    - more conversion methods for AABBWrapper
    - more consistent NBTWrapper (somehow there are more than three NBT wrappers in the original Rhino)
- ReflectASM
    - an ASM library that can make method/field invoking 8.7 times faster
    - Rhizo is trying to use this to replace original reflection, and make native access faster
- rewrite native java methods lookup
    - faster, that's all
- performance tweaks for general native java member lookup
- faster math, and support for BigInteger and BigDecimal
    - more precisely, faster when doing integer math
- fix `RemapPrefixForJS` annotation being broken

---

## Rhizo 3.1 -> 3.2

`??` and `?.` operator

- optional chaining operator (a ?. b)
- nullish coalescing operator (a ?? b)
    - note that this operator does not support compile mode yet
- better remapper performance, by caching annotation values, and using a simplified version of remapper sequence
- NBT converting now supports JSON types
- (internal) JavaMembers is rewritten, to guard against broken classes, and expose more useful information
- a new RemapPrefixForJSRep annotation that enables remapping based on multiple prefixes
- an unmodifiable view of rhizo remapper internal is now exposed
    - will be used by ProbeJS Legacy soon, stay tuned for that
- default remapper now includes Rhizo one and annotation-based one
    - this can fix KubeJS having broken startup script remapping

---

## Rhizo 3.0 -> 3.1(beta)

API update

- more internal members are exposed now
- typing annotations like JSInfo are accessible at runtime now
- a new annotation `RemapPrefixForJS` is added for class level prefix-based remapping
- devs can now attach custom properties to a Context

---

## Rhizo 2.2 -> 3.0

Enum++ & Full Remapper

-   Enum Type Wrapper
    -   now you can access enum values by their names or indexes.
    -   E.g. `Direction.EAST` can be accessed via `"east"`
    -   Note: name should be in lower case
-   Full Remapper
    -   Remapper is now rewritten, providing more complete and accurate remapping for methods and fields.
    -   Because of this, remapping on Forge no longer fully relies on an unusual uniqueness, and remapping for Fabric is now actually working.
    -   Note: the remapping file now uses a new storage format, that will make Rhizo jar bigger.
    -   It has been 4 months since the first attempt of bringing remapper to 1.16, and it turns out the first 3 months are completely useless, because writing a new Remapper solution from the ground up is actually faster.
-   Rhizo will now provide a dummy mod with modid `rhizo`
-   Rhizo now includes a mod icon

---

## Rhizo 2.1 -> 2.2

Early Remapper

-   Remapper is now enabled before KubeJS startup script is loaded, so that names used in startup scripts can also be remapped.

---

## Rhizo 2.0 -> 2.1

Compiler Mode

-   Compiler Mode
    -   you can enable Compiler Mode via `rhino.local.properties` file in the root of your game folder.
    -   Under Compiler Mode, Rhizo will compile JS codes into native codes, providing performance improvement.
    -   You can also change optimization level in `rhino.local.properties`. Higher optimization level can usually provide better performance, at the cost of compile time(which is usually already consumed during startup/reload)
    -   NOTE: enabling Compiler Mode will make error reporting less useful, since the line number will be wrong, and file name will always be "SourceFile"
-   you can also toggle mapping generation in `rhino.local.properties`, but the output file is useless currently
    -   generated mapping file is designed for a more complex remapper implementation, and is scheduled to be available in the next release
-   mapping file used by our temporary remapper solution is now driven by `.csv` file, for readablility.

---

## Rhino 1605.1.9 -> Rhizo 1605.2.0

First release

-   removed active but unused codes, to improve performance
-   fixed JSON generation for Iterable
-   REMAPPER!
    -   methods/fields can now be called using readable name, instead of SRG name.
    -   this is currently done via a simple String to String map, and will be replaced by a more standard and robust solution in the future
