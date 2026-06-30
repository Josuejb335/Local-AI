package com.localai.data.repository;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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

  private final Provider<OkHttpClient> okHttpClientProvider;

  public AIModelRepositoryImpl_Factory(Provider<Context> contextProvider,
      Provider<OkHttpClient> okHttpClientProvider) {
    this.contextProvider = contextProvider;
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public AIModelRepositoryImpl get() {
    return newInstance(contextProvider.get(), okHttpClientProvider.get());
  }

  public static AIModelRepositoryImpl_Factory create(Provider<Context> contextProvider,
      Provider<OkHttpClient> okHttpClientProvider) {
    return new AIModelRepositoryImpl_Factory(contextProvider, okHttpClientProvider);
  }

  public static AIModelRepositoryImpl newInstance(Context context, OkHttpClient okHttpClient) {
    return new AIModelRepositoryImpl(context, okHttpClient);
  }
}
