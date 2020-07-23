package se.plilja.jacksonversioning;

import org.springframework.context.ApplicationContext;

import java.util.Objects;

public final class SpringVersioningModuleBuilder {
    private SpringVersioningModuleBuilder() {
        // should not be instantiated
    }

    private static class InnerBuilder<V extends Comparable<V>> implements SpringVersioningModuleBuilder.WithVersionResolutionStrategy, SpringVersioningModuleBuilder.WithConverterRepository, SpringVersioningModuleBuilder.ReadyForBuilding {
        private final VersionsDescription<V> versionsDescription;
        private VersionedConverterRepository<V> versionedConverterRepository = null;
        private VersionResolutionStrategy<V> versionResolutionStrategy = null;

        private InnerBuilder(VersionsDescription<V> versionsDescription) {
            this.versionsDescription = versionsDescription;
        }

        @Override
        public WithConverterRepository withVersionDeterminedByRequestParameter(String parameterName) {
            versionResolutionStrategy = new RequestParameterVersionResolutionStrategy<>(parameterName, versionsDescription);
            return this;
        }

        @Override
        public WithConverterRepository withVersionDeterminedByRequestHeader(String headerName) {
            versionResolutionStrategy = new RequestHeaderVersionResolutionStrategy<>(headerName, versionsDescription);
            return this;
        }

        @Override
        public ReadyForBuilding withConvertersFromApplicationContext(ApplicationContext applicationContext) {
            versionedConverterRepository = new ApplicationContextConverterRepository<>(applicationContext);
            return this;
        }

        @Override
        public ReadyForBuilding withConvertersCreatedByReflection() {
            versionedConverterRepository = new ReflectionVersionedConverterRepository<>();
            return this;
        }

        @Override
        public VersioningModule build() {
            Objects.requireNonNull(versionsDescription, "VersionsDescription must be set before building");
            Objects.requireNonNull(versionedConverterRepository, "VersionsConverterRepository must be set before building");
            Objects.requireNonNull(versionResolutionStrategy, "VersionResolutionStrategy must be set before building");
            return new VersioningModule(versionsDescription, versionedConverterRepository, versionResolutionStrategy);
        }
    }

    public static <V extends Enum<V>> WithVersionResolutionStrategy withEnumVersions(Class<V> versionsEnum) {
        return new InnerBuilder<>(new EnumVersionsDescription<V>(versionsEnum));
    }

    public static <V extends Comparable<V>> WithVersionResolutionStrategy withCustomVersions(VersionsDescription<V> versionsDescription) {
        return new InnerBuilder<>(versionsDescription);
    }

    public interface WithVersionResolutionStrategy {
        WithConverterRepository withVersionDeterminedByRequestParameter(String parameterName);

        WithConverterRepository withVersionDeterminedByRequestHeader(String headerName);
    }

    public interface WithConverterRepository {
        ReadyForBuilding withConvertersFromApplicationContext(ApplicationContext applicationContext);

        ReadyForBuilding withConvertersCreatedByReflection();
    }

    public interface ReadyForBuilding {
        VersioningModule build();
    }
}

