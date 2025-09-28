<div align="center">
  <img src="https://github.com/IxionLang/Ixion/blob/main/assets/icon.png" width="200">

<h1>The Ixion Programming Language</h1>
Multi-paradigm compiled programming language for the jvm platform.
</div>


> [!IMPORTANT]
> Before installing the Ixion language, please install jdk 17 ↑.

Greeting in Ixion:
```scala
use <prelude>

def greeting(){
    var langs = [
        "Hello, world!",
        "¡Hola Mundo!",
        "Γειά σου Κόσμε!",
        "Привет, мир!",
        "こんにちは世界！"
    ]
    for i : range(0, len(langs) - 1) {
        println(list_get(langs,i))
    }
}

pub def main(args : string[]){
  greeting()
}
```


Pattern matching example:

```scala
use <prelude>

type my_type = int | float

pub def main(){
    print_type(10)
    print_type(10.0)
}

def print_type(list : my_type){
    match list with
    | int i => println("value " + i + " is integer")
    | float f => println("value " + f + " is float")
}
```


## Contributions
We will review and help with all reasonable pull requests as long as the guidelines below are met.

- The license header must be applied to all java source code files.
- IDE or system-related files should be added to the .gitignore, never committed in pull requests.
- In general, check existing code to make sure your code matches relatively close to the code already in the project.
- Favour readability over compactness.
- If you need help, check out the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) for a reference.
