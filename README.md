# DnDLang Interpreter

[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://java.com)
[![ANTLR4](https://img.shields.io/badge/ANTLR-4-red?style=for-the-badge)](https://www.antlr.org/)
[![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)

A Domain-Specific Language (DSL) designed for describing, simulating, and resolving _Dungeons & Dragons_ style combat encounters and character sheets.

Developed as a university laboratory project for the Programming Languages course, DnDLang provides Game Masters and players with a programmatic way to test monster stats, automate complex combat loops, and simulate RPG scenarios.

## Key Features

- **Advanced Functions & Recursion (Advanced Feature):** Full support for function declarations, call-by-value parameters, return types, isolated lexical scoping (activation records), and recursive calls.
- **Native Polyhedral Dice:** Say goodbye to generic `random` methods. Natively roll dice using built-in tokens like `d20`, `d12`, `d10`, `d8`, `d6`, `d4`, and `d3` right inside your mathematical expressions. In addition you can use the built in operators
  `adv`, `dis`, `vs`, `save` to simulate contested checks.
- **RPG Domain Types:** Built-in static typing for `HP` (Health Points), `AC` (Armor Class), and `Gold`, alongside standard types (`Int`, `Float`, `String`, `Bool`) with implicit safe coercion.
- **Semantic Blocks & Namespaces:** Code is organized into logical entities (`hero`, `foe`, `quest`). Variables declared inside entities automatically get their own namespace (e.g., `hero.hp`, `foe.name`).
- **Fail-Fast Error Handling:** Strict runtime validation. Errors like division by zero, type mismatches, parameter count violations, or undeclared variables trigger a custom `DnDLangError` that immediately halts execution, pointing to the exact line of the failure.
- **String Interpolation:** Easily inject variables and evaluate complex expressions directly inside strings using `i"Damage dealt: ${d8 + hero.strMod}"`.
- **Syntactic Sugar:** Supports compound assignments (`+=`, `-=`, etc.), pre/post-increment (`++`, `--`), ternary operators (`cond ? true : false`), and `switch-case` statements.

## Getting Started

### Prerequisites

- **Java Development Kit (JDK)** (version 17 or higher recommended)
- **Apache Maven** (for dependency management and build automation)

### Installation & Execution

1.  **Clone the repository:**

    ```bash
    git clone [https://github.com/fazionpietro/dnd-interpreter.git](https://github.com/fazionpietro/dnd-interpreter.git)
    cd dnd-interpreter
    ```

2.  **Compile the project:**
    This command will automatically trigger ANTLR4 to generate the Lexer and Parser from the `DnDLang.g4` grammar, and then compile the Java source code.

    ```bash
    mvn clean compile

    ```

3.  **Run a program:**
    Execute the interpreter by passing the path to a `.dnd` script.

    ```bash
    mvn exec:java -Dexec.mainClass="it.univr.dndlang.Main" -Dexec.args="programs/quest.dnd"
    ```

## Language Syntax Overview

A standard DnDLang program consists of an optional declarative section for functions, followed by entity definition blocks, and closes with a mandatory `quest` block containing the main execution logic.

### Quick Example

```dnd
def Int getModifier(Int score) {
    return (score - 10) / 2;
}

def Int calcDamage(Int dieRoll, Int mod, Bool isCrit) {
    Int base = isCrit ? dieRoll * 2 : dieRoll;
    return base + mod;
}

hero: {
    String name = "Garrick";
    Int strength = 16;

    Int strMod = getModifier(strength);
    HP hp = 35;
    AC ac = 16;
}

foe: {
    String name = "Goblin Capo";
    Int strength = 12;
    Int strMod = getModifier(strength);
    HP hp = 25;
    AC ac = 13;
}

quest: {
    print(i"--- INIZIO SCONTRO: ${hero.name} VS ${foe.name} ---");
    print(i"Modificatore di Forza Eroe: ${hero.strMod}");
    print(i"Modificatore di Forza Nemico: ${foe.strMod}");

    Int round = 1;
    while (hero.hp > 0 && foe.hp > 0) {
        print(i"\n--- ROUND ${round} ---");

        // --- TURNO DELL'EROE ---
        Int txc = d20;
        print(i"${hero.name} lancia per colpire. Dado naturale: ${txc}");

        switch (txc) {
            case 1: {
                print("Fallimento Critico! Ti sbilanci e subisci danni.");
                hero.hp -= 2;
            }
            case 20: {
                // Sfruttiamo calcDamage passando true per il critico
                Int dmg = calcDamage(8, hero.strMod, true);
                foe.hp -= dmg;
                print(i"SUCCESSO CRITICO! Inflitti ${dmg} danni devastanti. HP Mostro: ${foe.hp}");
            }
            default: {
                if (txc + hero.strMod >= foe.ac) {
                    Int dadoDanno = d8;
                    // Riutilizzo della funzione per il danno normale (false)
                    Int dmg = calcDamage(dadoDanno, hero.strMod, false);
                    foe.hp -= dmg;
                    print(i"Colpito! Inflitti ${dmg} danni. HP Mostro: ${foe.hp}");
                } else {
                    print("Mancato! L'arma impatta sullo scudo del Goblin.");
                }
            }
        }

        // --- TURNO DEL NEMICO ---
        if (foe.hp > 0) {
            Int txcFoe = d20;
            if (txcFoe + foe.strMod >= hero.ac) {
                Int dadoDannoFoe = d6;
                Int dmgFoe = calcDamage(dadoDannoFoe, foe.strMod, false);
                hero.hp -= dmgFoe;
                print(i"Il ${foe.name} contrattacca e ti colpisce! Subisci ${dmgFoe} danni. HP Eroe: ${hero.hp}");
            } else {
                print(i"Il ${foe.name} manca il bersaglio.");
            }
        }

        round++;
    }

    String finale = (hero.hp > 0) ? "VITTORIA" : "SCONFITTA";
    print(i"\nEsito finale della Quest: ${finale} al round ${round - 1}!");
}


```

## Architecture

The interpreter is built entirely in **Java** and relies on **ANTLR4**.

- **Lexer & Parser:** Generated by ANTLR4 from the `DnDLang.g4` grammar file.
- **Interpreter:** Implements the **Visitor Pattern** (`DnDInterpreter.java` extending `DnDLangBaseVisitor`), recursively traversing the Abstract Syntax Tree (AST) to evaluate expressions and execute statements.
- **Environment:** A custom stack-based memory structure (`Environment.java`) that handles block scoping (lexical scoping), variable visibility, and namespace auto-prefixing. Function return flows are efficiently handled using a custom `ReturnException` to interrupt the standard AST traversal.

## Repository Structure

- `src/main/antlr4/...` — Contains the `DnDLang.g4` grammar.
- `src/main/java/...` — Contains the Java implementation (Main, Interpreter, Environment, Error Handling).
- `programs/` — Contains sample scripts (`hello.dnd`, `quest.dnd`, `session.dnd`, `errors.dnd`) to test the language and its advanced features.
- `doc.md` — The complete documentation detailing language semantics, architecture, and operational semantics rules.
