# Quarkus quickjs4j Extension

[![Version](https://img.shields.io/maven-central/v/io.quarkiverse.quickjs4j/quarkus-quickjs4j?logo=apache-maven&style=flat-square)](https://search.maven.org/artifact/io.quarkiverse.quickjs4j/quarkus-quickjs4j)

A Quarkus extension that integrates [quickjs4j](https://github.com/roastedroot/quickjs4j) - Java bindings for the QuickJS JavaScript engine - into Quarkus applications. This extension enables seamless execution of JavaScript code within your Java applications using CDI beans and compile-time code generation.

## Features

- 🚀 **Compile-time Code Generation**: Automatically generates CDI beans and proxy classes for JavaScript interfaces
- 💉 **CDI Integration**: Full integration with Quarkus dependency injection
- 📁 **Flexible Script Loading**: Load JavaScript files from classpath, filesystem, or URLs
- 🔧 **Context Support**: Pass Java objects as context to JavaScript execution
- ⚡ **Build-time Optimization**: Leverages Quarkus build-time processing for optimal performance

## Requirements

- Java 21+
- Quarkus 3.20.1+
- Maven 3.6.3+

## Installation

Add the extension to your Quarkus application project:

```xml
<dependency>
    <groupId>io.quarkiverse.quickjs4j</groupId>
    <artifactId>quarkus-quickjs4j</artifactId>
    <version>${quarkus-quickjs4j.version}</version>
</dependency>
```

Also enable the quickjs4j annotation processor (required) to enable code generation
from annotations:

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${maven-compiler-plugin.version}</version>
                <configuration>
                    <source>${maven.compiler.source}</source>
                    <target>${maven.compiler.target}</target>
                    <release>${maven.compiler.release}</release>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>io.quarkiverse.quickjs4j</groupId>
                            <artifactId>quarkus-quickjs4j</artifactId>
                            <version>${quarkus-quickjs4j.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

## Quick Start

### 1. Define a Java Interface

Create a Java interface annotated with `@ScriptInterface` and `@ScriptImplementation`.  This
interface represents the contract that will be implemented by your JavaScript code:

```java
package com.example;

import io.roastedroot.quickjs4j.annotations.ScriptInterface;
import io.quarkiverse.quickjs4j.annotations.ScriptImplementation;

@ScriptInterface
@ScriptImplementation(location = "calculator.js")
public interface Calculator {
    int add(int a, int b);
    int multiply(int a, int b);
    double divide(double a, double b);
}
```

The `@ScriptImplementation` annotation is optional and should be used if e.g. you are 
packaging the JavaScript code with your Quarkus application.  If you are loading the
JavaScript in a more dynamic way, do not include this annotation.

Note: you will be able to directly inject the interface in your CDI service beans
**only if** you have the interface annotated with `@ScriptImplementation`.  Otherwise
you will need to inject a factory.

### 2. Create the JavaScript Implementation

Create `src/main/resources/calculator.js`:

```javascript
function add(a, b) {
    return a + b;
}

function multiply(a, b) {
    return a * b;
}

function divide(a, b) {
    if (b === 0) {
        throw new Error("Division by zero");
    }
    return a / b;
}
export {
  add, multiply, divide
};

```

### 3. Use in Your Application

Inject and use the calculator in your Quarkus application:

```java
package com.example;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MathService {
    
    @Inject
    Calculator calculator;
    
    public int performCalculation() {
        int sum = calculator.add(5, 3);        // Returns 8
        int product = calculator.multiply(4, 7); // Returns 28
        double quotient = calculator.divide(10.0, 2.0); // Returns 5.0
        
        return sum + product + (int) quotient;
    }
}
```

## Advanced Usage

### Using Context Objects

You can configure the `@ScriptInterface` with a Java context class.  This context class represents
the set of methods that will be available for your JavaScript code to invoke (JavaScript --> Java):

```java
package com.example;

import io.roastedroot.quickjs4j.annotations.ScriptInterface;
import io.quarkiverse.quickjs4j.annotations.ScriptImplementation;

@ScriptInterface(context = CalculatorContext.class)
@ScriptImplementation(location = "calculator.js")
public interface Calculator {
    int add(int a, int b);
    int multiply(int a, int b);
    double divide(double a, double b);
}
```

```java
@ApplicationScoped
public class CalculatorContext {
    public void logDivideByZero(int a, int b) {
        System.out.println("ERROR>> Divide by zero: " + a + " / " + b);
    }
}
```

JavaScript implementation with context (note that quickjs4j will generate a .mjs file
for the context functions):

```javascript
// ../../../target/classes/META-INF/quickjs4j/CalculatorContext_Builtins.mjs
function add(a, b) {
    return a + b;
}

function multiply(a, b) {
    return a * b;
}

function divide(a, b) {
    if (b === 0) {
        CalculatorContext_Builtins.logDivideByZero(a, b);
        throw new Error("Division by zero");
    }
    return a / b;
}
export {
  add, multiply, divide
};
```

### Factory Pattern Usage

For more control over script instantiation, you can use the factory pattern:

```java
@ApplicationScoped
public class MathService {

    @Inject
    CalculatorContext context;

    @Inject
    ScriptInterfaceFactory<Calculator, CalculatorContext> calculatorFactory;

    public void performCalculations() {
        // Load the script from some custom location.
        String scriptContent = loadCustomScript();

        // Use the factory to create a new Calculator instance
        Calculator calculator = calculatorFactory.create(scriptContent, context);
        
        // Use the calculator...
        int result = calculator.add(10, 20);
    }
}
```

### Script Loading Options

The extension supports multiple ways to load JavaScript files:

1. **Classpath Resources** (recommended):
   ```java
   @ScriptImplementation(location = "scripts/my-script.js")
   ```

2. **Absolute File Paths**:
   ```java
   @ScriptImplementation(location = "/path/to/script.js")
   ```

3. **Relative File Paths** (relative to working directory):
   ```java
   @ScriptImplementation(location = "scripts/my-script.js")
   ```

## Error Handling

JavaScript errors are propagated as Java exceptions:

```java
try {
    double result = calculator.divide(10, 0);
} catch (RuntimeException e) {
    // Handle JavaScript errors
    logger.error("JavaScript execution failed", e);
}
```

## Build-time Processing

The extension performs build-time code generation, creating:

1. **CDI Bean Classes**: `{InterfaceName}_CDI` - Injectable CDI bean
2. **Factory Classes**: `{InterfaceName}_Factory` - Injectable Factory bean
3. **Proxy Classes**: `{InterfaceName}_Proxy` - Generated by quickjs4j
4. **Context Builtins**: `{InterfaceName}_Builtins` - Generated by quickjs4j

These classes are automatically generated during compilation and don't need to be manually created.

## Contributing

Feel free to contribute to this project by submitting issues or pull requests.

### Development Setup

1. Clone the repository
2. Build the project: `mvn clean install`
3. Run tests: `mvn test`

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Links

- [quickjs4j GitHub Repository](https://github.com/roastedroot/quickjs4j)
- [QuickJS JavaScript Engine](https://bellard.org/quickjs/)
- [Quarkus Extensions](https://quarkus.io/extensions/)
- [Quarkiverse](https://github.com/quarkiverse)

## Support

- 📖 [Documentation](https://github.com/roastedroot/quickjs4j/blob/main/Readme.md)
- 🐛 [Issue Tracker](https://github.com/quarkiverse/quarkus-quickjs4j/issues)

---

**Status**: Experimental - This extension is in active development and APIs may change.
