package io.restall.picodi;

import picocli.CommandLine;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Simple Dependency Injector
 * <p>
 * Lazy by default
 * <p>
 * TODO: Basic usage
 * <p>
 * Threadsafe - needs to only create any given injectable once.
 */
public class Picodi {

    private Set<Class<?>> creatables = new HashSet<>();

    private Set<Class<?>> eagerCreatables = new HashSet<>();

    private Map<Class<?>, Object> injectables = new HashMap<>();

    private Map<Class<?>, Function<CommandLine.IFactory, Object>> injectableCreatorMethods;

    /**
     * Register a class that can be instantiated and injected.
     * <p>
     * The class will only be instantiated if it needed, and it will only be instantiated once.
     * <p>
     * The class must only have 1 constructor or the constructor that should be used must be
     * annotated with {@link PrimaryConstructor}
     * <p>
     * If a class has multiple constructors and the class is not editable then use {@link #register(Class, Function)}
     *
     * @param injectableClass class that should be instantiated if it is needed
     * @return this so that multiple injectables can be declared fluidly.
     */
    public io.restall.picodi.Picodi register(Class<?> injectableClass) {
        register(injectableClass, true);
        return this;
    }

    /**
     * Register a class that can be instantiated and injected.
     * <p>
     * If lazy is true the class will only be instantiated if it needed, and it will only be instantiated once.
     * If lazy is false
     * <p>
     * The class must only have 1 constructor or the constructor that should be used must be
     * annotated with {@link PrimaryConstructor}
     * <p>
     * If a class has multiple constructors and the class is not editable then use {@link #register(Class, Function)}
     *
     * @param injectableClass
     * @param lazy
     * @return
     */
    public io.restall.picodi.Picodi register(Class<?> injectableClass, boolean lazy) {
        creatables.add(injectableClass);
        return this;
    }

    /**
     * Add an object so that it can be injected into
     *
     * @param instance
     * @return
     */
    public io.restall.picodi.Picodi register(Object instance) {
        return this;
    }

    /**
     * Register a class as injectable and describe how it should be created.
     * <p>
     * The {@param creator} function will be called once at most.
     *
     * @param injectableClass
     * @param creator
     * @param <T>
     * @return
     */
    public <T> io.restall.picodi.Picodi register(Class<T> injectableClass, Function<CommandLine.IFactory, T> creator) {
        return this;
    }

    public CommandLine.IFactory createInjector() {
        return new Injector(creatables, eagerCreatables, injectables, injectableCreatorMethods);
    }

    public static class Injector implements CommandLine.IFactory {

        private final Set<Class<?>> creatables;

        private final Map<Class<?>, Object> injectables;

        private final Set<Class<?>> eagerCreatables;

        private final Map<Class<?>, Function<CommandLine.IFactory, Object>> injectableCreatorMethods;

        public Injector(Set<Class<?>> creatables, Set<Class<?>> eagerCreatables, Map<Class<?>, Object> injectables, Map<Class<?>,
                Function<CommandLine.IFactory, Object>> injectableCreatorMethods) {
            this.creatables = creatables;
            this.injectables = injectables;

            // In case any of these classes are needed by creator methods
            this.eagerCreatables = eagerCreatables;
            this.injectableCreatorMethods = injectableCreatorMethods;

            injectables.putAll(injectableCreatorMethods.values()
                    .stream()
                    .map(cls -> cls.apply(this))
                    .collect(Collectors.toMap(Object::getClass, injectable -> injectable)));
        }

        @Override
        public <K> K create(Class<K> cls) {
            return internalCreate(cls, new HashSet<>());
        }

        // Method keeps track of what has already been requested so we can avoid circular dependencies
        private <K> K internalCreate(Class<K> cls, Set<Class> alreadyRequested) {
            // Check if it already exists
            Object existing = injectables.get(cls);
            if (existing != null) {
                return (K) existing;
            }
            boolean creatable = creatables.contains(cls);
            // Check if it
            return null;
        }

        private <K> K instantiate(Class<K> cls) {
            Constructor<K>[] constructors = (Constructor<K>[]) cls.getDeclaredConstructors();

            if (constructors.length > 0) {
                throw new MultipleConstructors("Unable to instantiate %s, class has multiple constructors", cls.toString());
            }

            Constructor<K> constructor = constructors[0];
            Object[] parameters = Arrays.stream(constructor.getParameterTypes())
                    .map(paramCls -> internalCreate(paramCls, new HashSet<>()))
            .toArray();

            try {
                K instantiated = constructor.newInstance(parameters);
                injectables.put(cls, instantiated);

                return instantiated;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new UnexpectedPicodiException(String.format("Unexpected reflection exception when instantiating %s", cls), e);
            }
        }

    }
}
