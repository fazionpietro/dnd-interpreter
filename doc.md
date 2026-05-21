# DnDLang

Un Domain Specific Language (DSL) per la descrizione, simulazione e risoluzione di incontri e sessioni di combattimento in stile Dungeons & Dragons.

## Indice

1. [Introduzione](#1-introduzione)
2. [Guida Rapida](#2-guida-rapida)
3. [Sintassi](#3-sintassi)
4. [Semantica e Funzionalità Avanzate](#4-semantica-e-funzionalità-avanzate)
5. [Implementazione](#5-implementazione)
6. [Programmi di Test](#6-programmi-di-test)

---

## 1. Introduzione

**DnDLang** è un linguaggio di programmazione imperativo sviluppato come DSL per la gestione e la simulazione di schede personaggio e combattimenti (encounter) nei giochi di ruolo cartacei.

Il linguaggio è stato progettato soddisfacendo i requisiti di base e implementando diverse **funzionalità avanzate**, candidandosi al punteggio massimo (con Lode).

### Caratteristiche principali e Funzionalità Avanzate

- **Funzioni (Funzionalità Avanzata - Livello 2)**: Supporto per la dichiarazione di funzioni con parametri passati per valore, tipi di ritorno, scoping isolato (record di attivazione) e supporto completo alla **ricorsione**.
- **Flusso di Controllo Condizionato (Funzionalità Avanzata - Livello 2)**: Implementazione del costrutto `switch-case` con blocco `default` opzionale.
- **Zucchero Sintattico (Funzionalità Avanzata - Livello 1)**: Assegnamenti composti (`+=`, `-=`, `*=`, `/=`), operatori di pre/post incremento (`++`, `--`), operatore ternario (`cond ? vero : falso`) e stringhe interpolate con valutazione eager (`i"Danno: ${d8 + mod}"`).
- **Meccaniche RPG native**: Il linguaggio integra i dadi poliedrici ufficiali (`d20`, `d12`, `d10`, `d8`, `d6`, `d4`, `d3`) come espressioni native oltre che le meccaninche di tiro con vantaggio, svantaggio, contrapposto e salvezza
- **Namespace Autogenerati**: Struttura a blocchi semantici (`hero`, `foe`, `quest`) che organizza automaticamente le variabili qualificandole (es. `hero.hp`).

---

## 2. Guida Rapida

### Requisiti

- **Java 17+**
- **ANTLR 4**
- **Maven** (Consigliato per la gestione delle dipendenze e l'esecuzione)

### Generazione ed Esecuzione

Per generare l'interprete a partire dai file sorgenti e avviare uno script:

```bash
# 1. Compila il progetto e genera le classi Lexer e Parser tramite ANTLR
mvn clean compile

# 2. Esegui uno script specificando il percorso del file .dnd
mvn exec:java -Dexec.mainClass="it.univr.dndlang.Main" -Dexec.args="programs/nome_file.dnd"
```

```

```

### Hello World (con Funzioni e Dadi)

Un semplice programma che mostra dichiarazioni, funzioni e l'uso del d20 nativo.

```dnd
def Int calcolaTxC(Int d20Roll, Int mod) {
    return d20Roll + mod;
}

hero: {
    String name = "Aelar";
    HP hp       = 24;
    AC ac       = 15;
    Int strMod  = 3;
}

quest: {
    Int roll = d20;
    Int totale = calcolaTxC(roll, hero.strMod);
    print: i"Eroe: ${hero.name} | HP: ${hero.hp} | AC: ${hero.ac}";
    print: i"Attacco sferrato! Risultato: ${totale}";
}

```

---

## 3. Sintassi

### 3.1 Struttura di un programma

Un programma DnDLang impone un ordine strutturale rigido per separare dichiarativo ed eseguibile:

```antlr
program : functionSection? heroSection? foeSection? questSection EOF ;

```

- **`functionSection`** (`def Type name() { ... }`): Sezione **esclusivamente dichiarativa** (non eseguibile direttamente) dedicata alle funzioni.
- **`heroSection / foeSection`** (`hero: { ... }`, `foe: { ... }`): Definiscono i record per eroe e nemico. Generano i namespace `hero.` e `foe.`.
- **`questSection`** (`quest: { ... }`): Il blocco logico principale (Main loop) dove avvengono le valutazioni.

### 3.2 Tipi di dato e Dadi Poliedrici

Il linguaggio è staticamente tipizzato e supporta tipi generici e di dominio:

- **Base**: `Int`, `Float`, `Bool`, `String`, `Void` (solo per funzioni).
- **Dominio**: `HP` (Punti Ferita), `AC` (Classe Armatura), `Gold` (Monete d'oro).

**Dadi Nativi:**
I token `d20`, `d12`, `d10`, `d8`, `d6`, `d4` e `d3` sono trattati dal lexer come vere e proprie espressioni. Scrivere `Int danno = 2 * d6 + 4;` ordina all'interprete di simulare dinamicamente il lancio.
inoltre utilizzando `adv`, `dis`, `vs`, `save` si possono simulare i tiri speciali come i tiri con vantaggio/svantaggio,
i tiri contrapposti e i tiri salvezza, come per esempio `adv d20`, `d20 save 15`.

---

## 4. Semantica e Funzionalità Avanzate

### 4.1 Funzioni, Valutazione Eager e Scoping

Le funzioni in DnDLang rispettano una rigorosa semantica _Call-by-Value_ (passaggio per valore) con **valutazione Eager** (ansiosa):

1. Gli argomenti passati alla funzione vengono interamente valutati nello scope del chiamante.
2. L'ambiente (`Environment`) apre un nuovo _Scope_ isolato.
3. I valori valutati vengono associati (binding) ai parametri formali nel nuovo record di attivazione.
4. L'esecuzione procede fino al comando `return` o alla fine del blocco, dopodiché lo scope viene distrutto, prevenendo memory leaks.

Questo isolamento gerarchico (lexical scoping) permette il supporto nativo alla **ricorsione**, gestendo correttamente chiamate annidate della stessa funzione senza collisione di variabili.

### 4.2 Fail-Fast, Type Checking e Coercizione

Il linguaggio implementa una strategia **Fail-Fast** rigorosa. Se si verifica un errore semantico o di runtime (divisione per zero, tipo incompatibile, numero errato di parametri passati a una funzione, variabile non dichiarata), l'interprete lancia un'eccezione custom (`DnDLangError`) che interrompe **immediatamente** l'esecuzione segnalando la riga dell'errore.

- **Coercizione Implicita**: È ammessa in modo sicuro. I tipi numerici `Int` promossi a `Gold` diventano _Double_, mentre i calcoli su `HP` o `AC` vengono troncati esplicitamente a interi. Le stringhe non compatibili generano un arresto immediato del programma.

### 4.3 Semantica Operazionale

Il costrutto della **Chiamata di Funzione** (valutata eagerly) e l'**Assegnamento** possono essere descritti dalle seguenti regole di transizione. Sia $\sigma$ lo stato della memoria e $env$ l'ambiente corrente.

Regola per la valutazione di un assegnamento semplice:

$$\text{Assign} \quad \frac{(\sigma, \ e) \rightarrow (\sigma, \ v)}{(\sigma, \ \mathtt{id = e;}) \rightarrow (\sigma[id \mapsto v], \ \epsilon)}$$

Regola per l'operatore di _Short-Circuit_ logico (`&&`):

$$\text{And-False} \quad \frac{(\sigma, \ e_1) \rightarrow (\sigma, \ \text{false})}{(\sigma, \ e_1 \ \mathtt{\&\&} \ e_2) \rightarrow (\sigma, \ \text{false})}$$

_(Nota: L'espressione $e_2$ non viene mai valutata, evitando eventuali errori a runtime in essa contenuti)._

---

## 5. Implementazione

Il DSL è implementato in Java 17 utilizzando **ANTLR 4** per l'analisi lessicale e sintattica, supportato dal pattern architetturale **Visitor**.

- **`DnDInterpreter.java`**: La classe core. Estende `DnDLangBaseVisitor` ed esplora l'albero sintattico.
- **Il trucco del `return**`: L'interruzione del flusso di controllo per restituire un valore da una funzione è implementato sfruttando la Virtual Machine Java tramite una **`ReturnException`** dedicata. Quando `visitReturnStmt` incontra il costrutto, lancia l'eccezione contenente il valore. L'`if/catch`presente in`visitFunctionCallExpr` la intercetta, distrugge lo scope e restituisce il dato pulito, permettendo uscite premature (early exit) della funzione e cicli ricorsivi leggeri.
- **Stringhe Interpolate Ricorsive**: Il token `i"..."` avvia un'operazione di regex. Ogni stringa `\${expr}` individuata istanzia localmente un mini-parser ANTLR _al volo_ per valutare l'espressione in modo sicuro dentro lo scope attuale, applicando infine i suffissi di dominio (es. `HP` -> `20 HP`).

---

## 6. Programmi di Test

_(Tutti i file sono inclusi nella cartella `programs`)_

### `errors.dnd` (Dimostrazione Fail-Fast)

Garantisce che il linguaggio non permetta calcoli illegali, interrompendosi preventivamente.

```dnd
hero: { HP hp = 20; }
quest: {
    print: "--- Test Fail-Fast: Divisione per zero ---";
    Int x = 10;
    Int y = 0;

    // Il programma esploderà qui, lanciando un DnDLangError alla riga corretta
    Int z = x / y;

    print: "Questa riga non verrà mai stampata!";
}

```

### `session.dnd` (Dimostrazione Ricorsione e Meccaniche Avanzate)

Un test complesso che simula un attacco "Furia Berserker" utilizzando una **funzione ricorsiva** in cui il caso base è determinato stocasticamente dai tiri di dado nativi, generando record di attivazione multipli gestiti dal Lexical Scoping.

```dnd
def Int furiaBerserker(Int bonusTxC, Int classeArmatura) {
    Int txc = d20 + bonusTxC;

    if (txc >= classeArmatura) {
        Int danno = d8 + 1;
        print: i" > Colpo a segno! Inflitti ${danno} danni.";
        // Chiamata ricorsiva con fatica (malus al TxC)
        return danno + furiaBerserker(bonusTxC - 2, classeArmatura);
    } else {
        print: i" > L'attacco fallisce.";
        return 0; // Caso base di uscita
    }
}

hero: {
    String name = "Tordek il Barbaro";
    HP hp = 50;
    AC ac = 16;
    Int strMod = 6;
}
foe: {
    String name = "Golem di Carne";
    HP hp = 80;
    AC ac = 14;
}
quest: {
    print: i"--- INIZIO SCONTRO: ${hero.name} VS ${foe.name} ---";
    Int danniTotali = furiaBerserker(hero.strMod, foe.ac);

    if (danniTotali > 0) {
        foe.hp -= danniTotali;
        print: i"La Furia ha inflitto un totale di ${danniTotali} danni! HP Golem: ${foe.hp}";
    }
}

```

```

```
