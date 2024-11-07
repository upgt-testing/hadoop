package org.apache.hadoop.hdfs.remoteProxies;

public interface ResolvedTypeInterface {
    boolean isPrimitive();
    java.lang.String toCanonical();
    boolean isAbstract();
    boolean isArrayType();
    java.lang.Class<?> getRawClass();
    java.lang.Class<?> getParameterSource();
    int containedTypeCount();
    boolean isCollectionLikeType();
    ResolvedTypeInterface getReferencedType();
    boolean isEnumType();
    ResolvedTypeInterface containedType(int arg0);
    boolean isMapLikeType();
    boolean isContainerType();
    ResolvedTypeInterface getKeyType();
    ResolvedTypeInterface getContentType();
    boolean isConcrete();
    boolean isReferenceType();
    boolean isThrowable();
    java.lang.String containedTypeName(int arg0);
    boolean isFinal();
    boolean hasRawClass(java.lang.Class<?> arg0);
    boolean isInterface();
    boolean hasGenericTypes();
}