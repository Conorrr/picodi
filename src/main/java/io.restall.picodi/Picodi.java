package io.restall.picodi;

import picocli.CommandLine;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableSet;

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

    private Map<Class<?>, Class<?>> creatables = new HashMap<>();

    private Map<Class<?>, Class<?>> eagerCreatables = new HashMap<>();

    private Map<Class<?>, Object> injectables = new HashMap<>();

    private Map<Class<?>, Function<CommandLine.IFactory, Object>> injectableCreatorMethods = new HashMap<>();

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
    public Picodi register(Class<?> injectableClass) {
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
    public Picodi register(Class<?> injectableClass, boolean lazy) {
        if (lazy) {
            creatables.putAll(supertypeClassMap(injectableClass));
        } else {
            eagerCreatables.putAll(supertypeClassMap(injectableClass));
        }
        return this;
    }

    private static Map<Class<?>, Class<?>> supertypeClassMap(Class<?> subtype) {
        return getSupertypes(subtype)
                .stream()
                .collect(Collectors.toMap(superclass -> superclass, it -> subtype));
    }

    private static Map<Class<?>, Object> supertypeInstanceMap(Object subtypeInstance) {
        return getSupertypes(subtypeInstance.getClass())
                .stream()
                .collect(Collectors.toMap(superclass -> superclass, it -> subtypeInstance));
    }

    private static Set<Class<?>> getSupertypes(Class<?> subtype) {
        Set<Class<?>> supertypes = new HashSet<>();
        supertypes.add(subtype);
        Class<?> superclass = subtype;
        do {
            superclass = superclass.getSuperclass();
            if (superclass != Object.class) {
                supertypes.add(superclass);
            }
        } while (superclass != Object.class);
        supertypes.addAll(Arrays.asList(subtype.getInterfaces()));
        return supertypes;
    }

    /**
     * Add an instance of an injectable object
     *
     * @param instance
     * @return
     */
    public Picodi register(Object instance) {
        injectables.putAll(supertypeInstanceMap(instance));
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
    public <T> Picodi register(Class<T> injectableClass, Function<CommandLine.IFactory, T> creator) {
        return this;
    }

    public CommandLine.IFactory createIFactory() {
        return new Injector(creatables, eagerCreatables, injectables, injectableCreatorMethods);
    }

    public static class Injector implements CommandLine.IFactory {

        private final Map<Class<?>, Class<?>> creatables;

        private final Map<Class<?>, Object> injectables;

        private final Map<Class<?>, Class<?>> eagerCreatables;

        private final Map<Class<?>, Function<CommandLine.IFactory, Object>> injectableCreatorMethods;

        public Injector(Map<Class<?>, Class<?>> creatables, Map<Class<?>, Class<?>> eagerCreatables, Map<Class<?>, Object> injectables, Map<Class<?>,
                Function<CommandLine.IFactory, Object>> injectableCreatorMethods) {
            this.creatables = creatables;
            this.injectables = injectables;

            // In case any of these classes are needed by creator methods
            this.eagerCreatables = eagerCreatables;
            this.injectableCreatorMethods = injectableCreatorMethods;

            injectables.putAll(injectableCreatorMethods.values()
                    .stream()
                    .distinct()
                    .map(cls -> cls.apply(this))
                    .collect(Collectors.toMap(Object::getClass, injectable -> injectable)));

            injectables.putAll(eagerCreatables.values().stream()
                    .distinct()
                    .map(cls -> instantiate(cls, new HashSet<>()))
                    .collect(Collectors.toMap(Object::getClass, instance -> instance))
            );
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
            if (creatables.containsKey(cls)) {
                return instantiate((Class<K>) creatables.get(cls), alreadyRequested);
            }
            // TODO throw exception if can't be found
            throw new InjectableNotFound("Injectable not found <%s>", cls);
        }

        private <K> K instantiate(Class<K> cls, Set<Class> previouslyRequested) {
            if (previouslyRequested.contains(cls)) {
                throw new CyclicalDependencyException("Exception creating <%s>, cyclical dependency detected", cls);
            }
            Constructor<K>[] constructors = (Constructor<K>[]) cls.getDeclaredConstructors();

            if (constructors.length > 1) {
                throw new MultipleConstructors("Unable to instantiate <%s>, class has multiple constructors", cls.toString());
            }

            Set<Class> alreadyRequested = newSet(previouslyRequested, cls);
            Constructor<K> constructor = constructors[0];
            Object[] parameters = Arrays.stream(constructor.getParameterTypes())
                    .map(paramCls -> internalCreate(paramCls, alreadyRequested))
                    .toArray();

            try {
                K instantiated = constructor.newInstance(parameters);
                injectables.put(cls, instantiated);

                return instantiated;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new UnexpectedPicodiException(String.format("Unexpected reflection exception when instantiating <%s>", cls), e);
            }
        }
    }

    private static Set<Class> newSet(Set<Class> existingHashSet, Class newElement) {
        Set<Class> newSet = new HashSet<>(existingHashSet);
        newSet.add(newElement);

        return unmodifiableSet(newSet);
    }
}
