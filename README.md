**xfunctiontests** - Java module with set of functions to be used for writing tests:

- assert execution of external commands (output, return code)
- assert content equality of two files or folders (recursively)
- ...

It integrates with [opentest4j](https://github.com/ota4j-team/opentest4j). This allows IDEs or Build Tools (Gradle etc) to understand any assertion failures reported with **xfunctiontests** and process them natively.

# Requirements

Java 11+

# Download

[Release versions](xfunctiontests/release/CHANGELOG.md)

Or you can add dependency to it as follows:

Gradle:

```
dependencies {
  implementation 'io.github.lambdaprime:id.xfunctiontests:1.0'
}
```

# Documentation

[Documentation](http://portal2.atwebpages.com/xfunctiontests)

[Development](DEVELOPMENT.md)

# Contacts

lambdaprime <intid@protonmail.com>
