package com.localai.presentation.model;

import com.localai.domain.repository.AIModelRepository;
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
public final class ModelManagerViewModel_Factory implements Factory<ModelManagerViewModel> {
  private final Provider<AIModelRepository> repositoryProvider;

  public ModelManagerViewModel_Factory(Provider<AIModelRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public ModelManagerViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static ModelManagerViewModel_Factory create(
      Provider<AIModelRepository> repositoryProvider) {
    return new ModelManagerViewModel_Factory(repositoryProvider);
  }

  public static ModelManagerViewModel newInstance(AIModelRepository repository) {
    return new ModelManagerViewModel(repository);
  }
}
