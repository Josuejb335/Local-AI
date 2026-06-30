package com.localai.presentation.chat;

import com.localai.domain.engine.AIModelEngine;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<AIModelEngine> aiModelEngineProvider;

  public ChatViewModel_Factory(Provider<AIModelEngine> aiModelEngineProvider) {
    this.aiModelEngineProvider = aiModelEngineProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(aiModelEngineProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<AIModelEngine> aiModelEngineProvider) {
    return new ChatViewModel_Factory(aiModelEngineProvider);
  }

  public static ChatViewModel newInstance(AIModelEngine aiModelEngine) {
    return new ChatViewModel(aiModelEngine);
  }
}
