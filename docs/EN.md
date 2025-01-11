# Documentation

* [First program](#First-program)
* [Variables and Types](#Variables-and-data-types)
* [Nullable types](#Nullable-types)

# First program
The entry point to the program is the
`main` function. Everything inside it
will be executed when the program is started.

First, let's say hello to the world
using the `print` or
`println` function.

````scala
def main {
    println("Hello World");
}
````

If the function does not take arguments,
then parentheses are not necessary,
so this is equivalent:

````scala
def main(){
    println("Hello World");
}
````

# Variables and data types

You can create a variable using the
`var` keyword.
After that, the variable name is specified,
and then the value it will store.
````scala
def main {
    var test = 10;
    println(test);
}
````
We expect to see `10` in the console.
When the type is not explicitly specified, the compiler
determines it itself, and in this case,
the type will be `byte`.

You can specify the type of
the variable through a colon, for example:
````scala
def main {
    var test : String = 10;
    println(test);
}
````
This code will cause an error, since the
String type cannot store a number, it is needed
to store strings.

````scala
def main {
    var test : String = "Hello World";
    println(test);
}
````
This code will already be valid, and in the
console we will see Hello World.

# Nullable types

Variables that are not
primitive can store the value
`null`. Ixion has a special

syntax for such variables.
For example, `String` is not a primitive type,
which means it can store `null`.

But here's the problem, if we write this
code:
````scala
def main {
    var test : String;
    println(test);
}
````

Then we will see an error:
````
[Ixion Exception]
│> [2:14] in file "test.ix" ['test']:
│> Cannot default initialize variable of type 'java.lang.String'
````

It says that the variable
was not assigned a default value.
Before this, we always assigned a value to a variable of type String, but now
we didn't, which means in theory
it should store `null`.

To give the variable this ability,
we'll change our construction as follows:
````scala
def main {
    var test : String?;
    println(test);
}
````
After the type name, there is a `?` sign,
our variable now stores `null`,
which means that's what it will print to the console.