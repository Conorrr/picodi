# picodi

Tiny Dependency Injection Framework for [picocli](https://picocli.info/)

This project is currently in early stage of development.

## Download
You can add picodi as an external dependency to your project.

### Gradle

```
compile 'io.restall.picodi:picodi:0.0.5'
```

### Maven

```
<dependency>
  <groupId>io.restall.picodi</groupId>
  <artifactId>picodi</artifactId>
  <version>0.0.5</version>
</dependency>
```

## Usage

Picodi is designed to be simple to use and only supports constructor
injection. 

Firstly create a new instance of Picodi then classes and instances
should be registered with that instance. Finally a IFactory can be
created from Picodi.

```
    public static void main(String[] args) {
        CommandLine.run(new Example(), args);
    }
```

See [picodi-example](https://github.com/conorrr/picodi-example) for a more detailed example.

## Excluded Features

A few features have been deliberately excluded. If you need these
features we suggest you use a heavier library like Guice or Spring.

* Classpath scanning
* Dependency naming
