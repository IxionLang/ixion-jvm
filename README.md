<div align="center">
  <img src="https://github.com/IxionLang/Ixion/blob/main/assets/icon.png" width="200">

<h1>The Ixion Programming Language</h1>
Multi-paradigm compiled programming language for the jvm platform.
</div>


> [!IMPORTANT]
> Before installing the language, install jdk.

greeting in Ixion:
```scala
def greeting(steps : int){
    const langs = new String[]{
        "Hello, world!",
		"¡Hola Mundo!",
		"Γειά σου Κόσμε!",
		"Привет, мир!",
		"こんにちは世界！"
    };
    for(var i = 0; i < steps; i+=1){
        println(langs[i]);
    }
}

def main => greeting(2);
```


> [!NOTE]
> The language contains nullable types and non-nullable types.

```scala
def main(args: String[]) {
   var a : String?;
   var b : String = "Hello";
}
```

java ArrayList example:

```scala
using java.util.ArrayList;

def main(args: String[]){
    var list = new ArrayList();

    list.add("Hello");
    list.add("World");

    for(var i = 0; i < list.size(); i++){
        println(list.get(i));
    }
}
```

> [!NOTE]
> The language supports OOP.

Inheritance example:

```scala
class Human {
   var name: String = "";

   this(name: String) {
      this.name = name;
   }
   override def toString => "My name is " + name + ".";

}

class Man ext Human {
    var age : int;

    this(age: int) : ("Artyom") {
       this.age = age;
    }

    override def toString: String {
        var name : String = super.toString();
        return name + " My age is " + age + ".";
    }

}

def main {
   var simpleMan: Human = new Man(16);
   println(simpleMan);
}
```

## Contributions
We will review and help with all reasonable pull requests as long as the guidelines below are met.

- The license header must be applied to all java source code files.
- IDE or system-related files should be added to the .gitignore, never committed in pull requests.
- In general, check existing code to make sure your code matches relatively close to the code already in the project.
- Favour readability over compactness.
- If you need help, check out the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) for a reference.