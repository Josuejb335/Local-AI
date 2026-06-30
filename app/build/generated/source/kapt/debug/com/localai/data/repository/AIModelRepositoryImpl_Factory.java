package com.localai.data.repository;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AIModelRepositoryImpl_Factory implements Factory<AIModelRepositoryImpl> {
  private final Provider<Context> contextProvider;

  public AIModelRepositoryImpl_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AIModelRepositoryImpl get() {
    return newInstance(contextProvider.get());
  }

  public static AIModelRepositoryImpl_Factory create(Provider<Context> contextProvider) {
    return new AIModelRepositoryImpl_Factory(contextProvider);
  }

  public static AIModelRepositoryImpl newInstance(Context context) {
    return new AIModelRepositoryImpl(context);
  }
}
