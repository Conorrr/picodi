package io.restall.picodi


import spock.lang.Specification

class PicocdiSpec extends Specification {

    def "Can instantiate no args constructor"() {
        given:
            def iFactory = new Picodi().register(TestClasses.NoArg).createIFactory()
        expect:
            iFactory.create(TestClasses.NoArg).class == TestClasses.NoArg
    }

    def "Can instantiate single arg constructor"() {
        given:
            def iFactory = new Picodi().register(TestClasses.A).register(TestClasses.B).createIFactory()
        expect:
            iFactory.create(TestClasses.B).class == TestClasses.B
    }

    def "Exception thrown if required arg cannot be found"() {
        given:
            def iFactory = new Picodi().register(TestClasses.B).createIFactory()
        when:
            iFactory.create(TestClasses.B)
        then:
            def ex = thrown(InjectableNotFound)
            ex.message == 'Injectable not found <class io.restall.picodi.TestClasses$A>'
    }

    def "Exception thrown if requested class is not registered"() {
        given:
            def iFactory = new Picodi().createIFactory()
        when:
            iFactory.create(TestClasses.A)
        then:
            def ex = thrown(InjectableNotFound)
            ex.message == 'Injectable not found <class io.restall.picodi.TestClasses$A>'
    }

    def "Exception thrown if requested class has multiple constructors"() {
        given:
            def iFactory = new Picodi().register(TestClasses.C).createIFactory()
        when:
            iFactory.create(TestClasses.C)
        then:
            def ex = thrown(MultipleConstructors)
            ex.message == 'Unable to instantiate <class io.restall.picodi.TestClasses$C>, class has multiple constructors'
    }

    def "Class is only instantiated once"() {
        given:
            def iFactory = new Picodi().register(TestClasses.D).createIFactory()
            TestClasses.D.reset()
        when:
            iFactory.create(TestClasses.D)
            iFactory.create(TestClasses.D)
        then:
            TestClasses.D.count == 1
    }

    def "a registered instance is returned"() {
        given:
            def c = new TestClasses.C(0)
            def iFactory = new Picodi().register(c).createIFactory()
        when:
            def cFromIFactory = iFactory.create(TestClasses.C)
        then:
            c == cFromIFactory
    }

    def "if an instance is supplied then that instance is used and a new one isn't created"() {
        given:
            TestClasses.D.reset()
            def d = new TestClasses.D()
            def iFactory = new Picodi().register(d).createIFactory()
        when:
            def dFromIFactory = iFactory.create(TestClasses.D)
        then:
            TestClasses.D.count == 1
        and:
            d == dFromIFactory
    }

    def "a registered instance will be injected if it is needed"() {
        given:
            TestClasses.D.reset()
            def d = new TestClasses.D()
            def iFactory = new Picodi().register(d).register(TestClasses.E).createIFactory()
        when:
            def e = iFactory.create(TestClasses.E)
        then:
            e.d == d
            TestClasses.D.count == 1
    }

    def "an exception is thrown if a cyclical dependency is found"() {
        given:
            def iFactory = new Picodi()
                    .register(TestClasses.F)
                    .register(TestClasses.G)
                    .register(TestClasses.H)
                    .createIFactory()
        when:
            iFactory.create(classToCreate)
        then:
            def ex = thrown(CyclicalDependencyException)
            ex.message == "Exception creating <${classToCreate.toString()}>, cyclical dependency detected"
        where:
            classToCreate << [TestClasses.F, TestClasses.G, TestClasses.H]
    }

    def "an exception is thrown if a cyclical dependency is found and the requested class is not a part of that cyclical dependency"() {
        given:
            def iFactory = new Picodi()
                    .register(TestClasses.F)
                    .register(TestClasses.G)
                    .register(TestClasses.H)
                    .register(TestClasses.I)
                    .createIFactory()
        when:
            iFactory.create(TestClasses.I)
        then:
            def ex = thrown(CyclicalDependencyException)
            ex.message == 'Exception creating <class io.restall.picodi.TestClasses$G>, cyclical dependency detected'
    }

    def "a cylical dependency is broken by providing one of the dependencies"() {
        given:
            def h = new TestClasses.H(null)
            def iFactory = new Picodi()
                    .register(TestClasses.F)
                    .register(TestClasses.G)
                    .register(h)
                    .createIFactory()
        when:
            def f = iFactory.create(TestClasses.F)
        then:
            noExceptionThrown()
            f.g.h == h
        where:
            classToCreate << [TestClasses.F, TestClasses.G, TestClasses.H]
    }

    def "eagerly registered class is created when createIFactory is called"() {
        given:
            TestClasses.D.reset()
            def picodi = new Picodi()
                    .register(TestClasses.D, false)
        when:
            picodi.createIFactory()
        then:
            TestClasses.D.count == 1
    }

    def "lazily registered class is not created when createIFactory is called"() {
        given:
            TestClasses.D.reset()
            def picodi = new Picodi()
                    .register(TestClasses.D)
        when:
            picodi.createIFactory()
        then:
            TestClasses.D.count == 0
    }

    def "lazily registered class is not created when createIFactory is called 2"() {
        given:
            TestClasses.D.reset()
            def picodi = new Picodi()
                    .register(TestClasses.D, true)
        when:
            picodi.createIFactory()
        then:
            TestClasses.D.count == 0
    }

    def "returns subtype when requested supertype is an interface"() {
        given:
            def k = new TestClasses.K()
            def iFactory = new Picodi()
                    .register(k)
                    .createIFactory()
        when:
            def j = iFactory.create(TestClasses.J)
        then:
            j == k
    }

    def "creates subtype when requested supertype is an interface"() {
        given:
            def iFactory = new Picodi()
                    .register(TestClasses.K)
                    .createIFactory()
        when:
            def j = iFactory.create(TestClasses.J)
        then:
            j instanceof TestClasses.K
    }

    def "returns subtype when requested supertype is a concrete class"() {
        given:
            def m = new TestClasses.M()
            def iFactory = new Picodi()
                    .register(m)
                    .createIFactory()
        when:
            def l = iFactory.create(TestClasses.L)
        then:
            l == m
    }

    def "creates subtype when requested supertype is a concrete class"() {
        given:
            def iFactory = new Picodi()
                    .register(TestClasses.M)
                    .createIFactory()
        when:
            def l = iFactory.create(TestClasses.L)
        then:
            l instanceof TestClasses.M
    }

    def "returns subtype when requested supertype is an abstract class"() {
        given:
            def o = new TestClasses.O()
            def iFactory = new Picodi()
                    .register(n)
                    .createIFactory()
        when:
            def n = iFactory.create(TestClasses.N)
        then:
            n == o
    }

    def "creates subtype when requested supertype is an abstract class"() {
        given:
            def iFactory = new Picodi()
                    .register(TestClasses.O)
                    .createIFactory()
        when:
            def n = iFactory.create(TestClasses.N)
        then:
            n instanceof TestClasses.O
    }

    // Exception thrown if multiple implementations of a class that is required

    // Given a class has multiple constructors
    // And one of them is annotated with PrimaryConstructor
    // When I request an instance of that class
    // That constructor is called
    // And the instantiated class is created

    // Given a class has multiple constructors
    // And one of them is annotated with PrimaryConstructor
    // When I request an instance of that class and that constructor depends on a non registered class
    // Then a dependency not found exception is thrown

    // Given a class is registered with picodi
    // When I try to register that class again
    // Then an AlreadyRegistered exception is thrown
}
