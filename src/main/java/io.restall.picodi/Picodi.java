package io.restall.picodi;

import picocli.CommandLine;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableSet;

/**
 * Simple Dependency Injector
 * <p>
 * Lazy by default
 * <p>
 * Not Threadsafe - contains race conditions during registration and cannot
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
     * @return picodi so calls can be chained
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
     * @param injectableClass class that will be instantiated
     * @param lazy            whether to instantiate the class when it's needed or ahead of time
     * @return picodi so calls can be chained
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
     * @param instance object that can be injected
     * @return picodi so calls can be chained
     */
    public Picodi register(Object instance) {
        injectables.putAll(supertypeInstanceMap(instance));
        return this;
    }

    /**
     * Register a class as injectable and describe how it should be created.
     * <p>
     * The creator function will be called once at most.
     *
     * @param injectableClass class that will be instantiated
     * @param creator         function that describes how to create the injectableClass
     * @param <T>             type of the injectableClass
     * @return picodi so calls can be chained
     */
    public <T> Picodi register(Class<T> injectableClass, Function<CommandLine.IFactory, T> creator) {
        injectableCreatorMethods.put(injectableClass, (Function<CommandLine.IFactory, Object>) creator);
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
            this.eagerCreatables = eagerCreatables;
            this.injectableCreatorMethods = injectableCreatorMethods;

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

        private <K> K internalCreate(Class<K> cls, Set<Class> alreadyRequested) {
            Object existing = injectables.get(cls);
            if (existing != null) {
                return (K) existing;
            }
            if (creatables.containsKey(cls)) {
                return instantiate((Class<K>) creatables.get(cls), alreadyRequested);
            }
            if (eagerCreatables.containsKey(cls)) {
                return instantiate((Class<K>) eagerCreatables.get(cls), alreadyRequested);
            }
            if (injectableCreatorMethods.containsKey(cls)) {
                K instance = (K) injectableCreatorMethods.get(cls).apply(this);
                injectables.put(cls, instance);
                return instance;
            }
            if (cls.isAnnotationPresent(CommandLine.Command.class)) {
                return instantiate(cls, alreadyRequested);
            }
            if (cls.isMemberClass() && Modifier.isStatic(cls.getModifiers())
                    && cls.getEnclosingClass().isAnnotationPresent(CommandLine.Command.class)) {
                return instantiate(cls, alreadyRequested);
            }

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
