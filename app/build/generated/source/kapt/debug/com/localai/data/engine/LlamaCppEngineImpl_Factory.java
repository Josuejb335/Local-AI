package com.localai.data.engine;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class LlamaCppEngineImpl_Factory implements Factory<LlamaCppEngineImpl> {
  @Override
  public LlamaCppEngineImpl get() {
    return newInstance();
  }

  public static LlamaCppEngineImpl_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static LlamaCppEngineImpl newInstance() {
    return new LlamaCppEngineImpl();
  }

  private static final class InstanceHolder {
    private static final LlamaCppEngineImpl_Factory INSTANCE = new LlamaCppEngineImpl_Factory();
  }
}
