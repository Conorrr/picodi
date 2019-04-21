package picodi;

public class PicocdiSpec {

    // When I register a function that depends on class A before registering class A
    // I should be able to get the result of the function

    // When I request an instance of a class that isn't registered
    // Then I should get dependency not found exception

    // When I request an instance of a class that depends on a class that isn't registered
    // Then I should get a dependency not found exception for the correct class and details about why it's needed

    // Given a class has multiple constructors
    // When I request an instance of that class
    // Then I should get a Multiple Constructors Exception

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
