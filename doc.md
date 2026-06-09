# DnDLang

Un Domain Specific Language (DSL) per la descrizione, simulazione e risoluzione di incontri e sessioni di combattimento in stile Dungeons & Dragons.

## 1. Introduzione

### 1.1 Presentazione del Linguaggio
**DnDLang** è un Domain Specific Language (DSL) imperativo, fortemente e staticamente tipizzato, progettato specificamente per modellare, simulare e automatizzare scenari di combattimento, sessioni di gioco e interazioni stocastiche regolate dalle meccaniche dei giochi di ruolo da tavolo, con un riferimento esplicito alla quinta edizione di *Dungeons & Dragons*. Il linguaggio si propone come uno strumento intermedio in grado di unire la rigorosità algoritmica dei linguaggi imperativi tradizionali con l'espressività gergale e le necessità tipiche di un Master o di un game designer.

### 1.2 Caratteristiche Principali e Funzionalità Avanzate
Per rispondere pienamente ai requisiti minimi e avanzati del progetto, DnDLang integra al suo interno le seguenti peculiarità:
- **Espressioni Stocastiche Native**: I dadi poliedrici (da 3 a 20 facce) sono cittadini di prima classe del linguaggio, valutati dinamicamente come espressioni numeriche ad ogni invocazione.
- **Funzioni (Funzionalità Avanzata - Livello 2)**: Supporto completo alla dichiarazione di macro e sub-routine (`def`) con passaggio dei parametri per valore, isolamento dello scope locale e pieno supporto ad algoritmi ricorsivi.
- **Flusso di Controllo Condizionato Switch (Funzionalità Avanzata - Livello 2)**: Costrutto di selezione a scelta multipla `switch-case` con blocco `default` obbligatorio o opzionale, integrato con comandi di uscita prematura (`break`).
- **Zucchero Sintattico (Funzionalità Avanzata - Livello 1)**: Operatori di pre e post incremento/decremento unitario (`++`, `--`), assegnamenti composti (`+=`, `-=`, `*=`, `/=`), operatore ternario condizionale (`cond ? exp1 : exp2`) e stringhe interpolate con prefisso `i"..."` e valutazione dinamica interna tramite sintassi `${expr}`.

### 1.3 Contesto d'Applicazione
Il contesto applicativo ideale per DnDLang risiede nella pre-generazione e simulazione statistica di incontri di combattimento (*encounters*). Permette di caricare schede personaggio prefissate, impostare loop di comportamento per i mostri e calcolare la probabilità di sopravvivenza di un gruppo di avventurieri (Party) contro una determinata minaccia, automatizzando migliaia di lanci di dado in frazioni di secondo senza la necessità di appoggiarsi a motori di gioco esterni o pesanti software general-purpose.

---

## 2. Guida Rapida

### 2.1 Requisiti d'Installazione ed Esecuzione
L'interprete di DnDLang è sviluppato in ambiente Java e richiede la presenza dei seguenti strumenti sul sistema ospite:
- **Java Development Kit (JDK) 17** o superiore.
- **Apache Maven 3.6+** (utilizzato per la gestione automatica delle dipendenze, l'aggancio del plugin ANTLR4 e la compilazione dei sorgenti).
- **ANTLR 4.13+** (gestito internamente tramite il file `pom.xml` di Maven).

### 2.2 Comandi per la Generazione e l'Esecuzione
Tutti i comandi vanno eseguiti all'interno della directory radice del progetto, dove risiedono il file di grammatica `DnDLang.g4` e il file `pom.xml`.

1. **Generazione dei sorgenti ANTLR e Compilazione**:
```bash
   mvn clean compile
```

Questo comando scarica le librerie necessarie, analizza la grammatica context-free e genera automaticamente i file Java ausiliari (`DnDLangLexer.java`, `DnDLangParser.java`, `DnDLangBaseVisitor.java`) inserendoli nel percorso di build, per poi compilare l'intera applicazione inclusi i file scritti a mano (`Main.java`, `Environment.java`, `DnDInterpreter.java`).

2. **Esecuzione di uno Script Sorgente**:
Per mandare in esecuzione un programma scritto in DnDLang (estensione `.dnd`), si utilizza il plugin Exec di Maven passando come argomento il percorso relativo del file:
```bash
mvn exec:java -Dexec.mainClass="it.univr.dndlang.Main" -Dexec.args="programs/quest.dnd"

```



### 2.3 Programma d'Esempio: "Hello World" Esteso

Di seguito si riporta una porzione di codice minimale ma esaustiva che illustra l'interazione tra le sezioni del programma, l'uso dei tipi nativi di dominio e le stringhe interpolate:

```dnd
hero: {
    String name = "Xanaphia";
    HP hp = 18;
    AC ac = 16;
    Gold borsa = 15.5;
}

quest: {
    print("--- Sessione Inizializzata ---");
    // Uso del dado nativo d20 e operatore di vantaggio
    Int iniziativa = adv d20; 
    
    // Stringa interpolata con accesso al namespace dell'eroe e formattazione automatica dei tipi
    print(i"L'avventuriera ${hero.name} (AC: ${hero.ac}) scende nel dungeon con ${hero.borsa}. Iniziativa: ${iniziativa}");
}

```

---

## 3. Sintassi

### 3.1 Struttura Rigida di un Programma

Al fine di garantire una separazione netta tra la logica riutilizzabile (funzioni), le entità statiche coinvolte (personaggi/mostri) e l'effettivo punto di ingresso del programma (Main loop), la grammatica impone un ordine sequenziale immutabile per le macro-sezioni, formalizzato dalla seguente regola di partenza:

```antlr
program : functionSection? heroSection? foeSection? questSection EOF ;

```

1. **`functionSection`**: Contiene esclusivamente le definizioni delle funzioni globali precedute dalla keyword `def`. Questa sezione è puramente dichiarativa; nessun comando in essa contenuto viene eseguito finché non viene esplicitamente invocato.
2. **`heroSection` (`hero: { ... }`)**: Blocco isolato deputato alla definizione delle statistiche dell'eroe protagonista.
3. **`foeSection` (`foe: { ... }`)**: Blocco speculare al precedente, riservato alle statistiche delle creature ostili.
4. **`questSection` (`quest: { ... }`)**: Rappresenta il corpo esecutivo centrale. Equivale semanticamente al metodo `main` dei linguaggi general-purpose; qui risiedono i cicli di combattimento e la logica algoritmica principale.

### 3.2 Tipi di Dato (Primitivi e di Dominio)

DnDLang si distacca dai linguaggi tradizionali affiancando ai classici tipi primitivi una serie di tipi nativi strettamente legati al dominio del gioco di ruolo:

* **Tipi Primitivi**:
* `Int`: Valori interi a 32 bit (es. modificatori di caratteristica, conteggio dei round).
* `Float`: Valori in virgola mobile a 64 bit per calcoli decimali generici.
* `Bool`: Espressioni booleane standard (`true` o `false`).
* `String`: Sequenze letterali racchiuse da doppi apici (`"..."`).
* `Void`: Tipo speciale non assegnabile, utilizzabile esclusivamente come tipo di ritorno per funzioni che eseguono solo effetti collaterali (es. stampe a video) senza restituire dati.


* **Tipi di Dominio**:
* `HP`: Tipo intero specializzato nella rappresentazione dei Punti Ferita.
* `AC`: Tipo intero specializzato per la Classe Armatura.
* `Gold`: Tipo decimale (virgola mobile) dedicato alla quantificazione delle monete d'oro (`gp`).



### 3.3 Regole Lessicali e Token dei Dadi Poliedrici

La gestione stocastica si basa su token lessicali dedicati che mappano i dadi della tradizione ludica:

```antlr
D20 : 'd20' ; D12 : 'd12' ; D10 : 'd10' ; D8 : 'd8' ; D6 : 'd6' ; D4 : 'd4' ; D3 : 'd3' ;

```

Questi token vengono intercettati dal Lexer e convertiti dal Parser in espressioni atomiche valide all'interno di qualsiasi operazione aritmetica (es. `3 * d6 + 4`).
Le costrutti speciali per i tiri includono:

* **`adv` / `dis**`: Modificatori prefissi applicabili alle espressioni dei dadi per implementare il *Vantaggio* (lancio di due dadi e selezione del valore massimo) o lo *Svantaggio* (selezione del valore minimo).
* **`save` / `vs**`: Operatori infissi binari per la risoluzione rapida dei conflitti. `expr1 save expr2` verifica se il tiro supera o uguaglia una determinata Classe di Difficoltà (CD), mentre `expr1 vs expr2` modella una prova contrapposta dove il primo elemento deve superare strettamente il secondo.


---

## 4. Semantica

### 4.1 Scelte Progettuali Core

Le scelte semantiche alla base di DnDLang mirano a massimizzare la prevedibilità del codice pur gestendo l'intrinseca volatilità dei lanci di dado:

* **Controllo dei Tipi**: Statico. Le variabili non possono mutare il tipo associato al momento della loro dichiarazione iniziale. Il type checking avviene a runtime in modo rigoroso prima di ogni legame in memoria.
* **Visibilità (Scoping)**: Il linguaggio adotta uno scoping di tipo *lessicale* e gerarchico. Ogni blocco racchiuso da parentesi graffe `{ ... }` genera un nuovo ambiente isolato. Le variabili dichiarate nei blocchi interni occultano temporaneamente quelle omonime dei blocchi esterni (*shadowing*).
* **Namespace Autogenerati**: I blocchi `hero` e `foe` applicano automaticamente una semantica di incapsulamento. Tutte le variabili dichiarate al loro interno vengono registrate nell'ambiente globale anteponendo rispettivamente il prefisso `hero.` e `foe.`. Questo consente di accedervi liberamente dall'interno della `questSection` simulando una struttura a record (es. `hero.hp`).
* **Valutazione delle Espressioni**: *Eager* (ansiosa) per tutte le espressioni aritmetiche, chiamate di funzione e stringhe interpolate.
* **Valutazione Logica**: Nonostante la grammatica supporti gli operatori logici standard `&&` e `||`, l'interprete esegue una valutazione completa dei due rami per garantire che eventuali lanci di dado presenti nel secondo operando vengano computati stabilmente, preservando la coerenza dello stato stocastico del gioco.
* **Passaggio dei Parametri**: Rigorosamente *Call-by-Value* (per valore). Le modifiche effettuate sui parametri formali all'interno di una funzione non alterano in alcun modo le variabili originarie passate come argomenti nel blocco chiamante.

### 4.2 Gerarchia dei Tipi e Strategie di Conversione (Coercizione)

DnDLang implementa un sistema di sottotipaggio e coercizione implicita orientato alla sicurezza numerica, strutturato secondo le seguenti regole:

1. I tipi di dominio `HP` e `AC` sono considerati sotto-tipi semantici di `Int`. Qualsiasi valore floating-point o espressione decimal assegnata a variabili di tipo `Int`, `HP` o `AC` viene automaticamente convertita tramite troncamento della parte decimale.
2. Il tipo `Int` è convertibile implicitamente verso `Gold` o `Float`. Se un numero intero viene assegnato a una variabile `Gold`, esso viene promosso a Double.
3. Le stringhe e i booleani sono totalmente isolati: non è ammessa alcuna forma di conversione implicita o esplicita tra tipi testuali/logici e tipi numerici. Un tentativo di addizionare una stringa a un intero produce un blocco immediato.

### 4.3 Gestione Programmatica degli Errori (Fail-Fast)

Il linguaggio adotta una filosofia difensiva definita **Fail-Fast**. L'interprete non tenta di recuperare lo stato del programma o di assegnare valori di fallback (come `null` o `0`) in presenza di anomalie semantiche. Qualsiasi violazione dei vincoli operativi provoca l'arresto immediato del thread esecutivo.
Vengono intercettati e tracciati programmaticamente tramite la classe custom `DnDLangError` i seguenti eventi:

* Utilizzo o assegnamento di variabili non preventivamente dichiarate nello scope corrente o superiore.
* Errori aritmetici irreversibili, specificamente la divisione o il calcolo del modulo per zero (es. `x / 0`).
* Discrepanza tra il numero di argomenti passati a una funzione in fase di chiamata e i parametri previsti dalla firma della stessa.
* Incompatibilità di tipo insanabile durante un assegnamento o il passaggio di un parametro.

### 4.4 Formalizzazione della Semantica Operazionale (Regole di Transizione)

Utilizzando la notazione standard della semantica operazionale strutturata (SOS), definiamo lo stato della memoria come una funzione $\sigma: \text{Id} \rightarrow \text{Val}$. Indichiamo con $\langle e, \sigma \rangle \rightarrow v$ il fatto che la valutazione dell'espressione $e$ nello stato $\sigma$ produce il valore $v$.

**Regola dell'Assegnamento Semplice (`=`)**:
Quando viene valutato un comando di assegnamento, l'espressione a destra viene ridotta a un valore atomico $v$, e la memoria viene aggiornata associando stabilmente quel valore all'identificatore $id$.


$$\text{Assign} \quad \frac{\langle e, \sigma \rangle \rightarrow v}{\langle id = e;, \sigma \rangle \rightarrow \sigma[id \mapsto v]}$$

**Regola dell'Assegnamento Composto Additivo (`+=`)**:
Il comando recupera il valore corrente associato all'identificatore, valuta l'espressione a destra ed esegue la somma algebrica prima di riassegnare il valore finale.


$$\text{Assign-Compound} \quad \frac{\sigma(id) = v_1 \quad \langle e, \sigma \rangle \rightarrow v_2 \quad v_3 = v_1 + v_2}{\langle id \ \mathtt{+=} \ e;, \sigma \rangle \rightarrow \sigma[id \mapsto v_3]}$$

**Regola dei Tiri Speciali — Tiro Salvezza (`save`)**:
L'operatore binario `save` confronta il risultato numerico ottenuto a sinistra (solitamente comprensivo di un lancio di dado) con una soglia fissa a destra (Classe di Difficoltà). Il successo è decretato dal superamento o dall'uguaglianza matematica.


$$\text{Save-Success} \quad \frac{\langle e_1, \sigma \rangle \rightarrow v_1 \quad \langle e_2, \sigma \rangle \rightarrow v_2 \quad v_1 \ge v_2}{\langle e_1 \ \mathtt{save} \ e_2, \sigma \rangle \rightarrow \text{true}}$$

$$\text{Save-Failure} \quad \frac{\langle e_1, \sigma \rangle \rightarrow v_1 \quad \langle e_2, \sigma \rangle \rightarrow v_2 \quad v_1 < v_2}{\langle e_1 \ \mathtt{save} \ e_2, \sigma \rangle \rightarrow \text{false}}$$

**Regola del Lancio con Vantaggio (`adv`)**:
La valutazione di un dado prefissato da `adv` comporta l'esecuzione indipendente di due campionamenti stocastici (lanci) sulla medesima funzione di distribuzione discreta $[1, \text{sides}]$. Il valore finale risultante corrisponde al massimo matematico tra i due esiti.


$$\text{Dice-Advantage} \quad \frac{\text{roll}_1 = \text{random}(1, \text{sides}) \quad \text{roll}_2 = \text{random}(1, \text{sides}) \quad v = \max(\text{roll}_1, \text{roll}_2)}{\langle \mathtt{adv} \ \text{diceOnly}, \sigma \rangle \rightarrow v}$$

---

## 5. Implementazione

### 5.1 Architettura del Visitor ed Esplorazione dell'AST

L'architettura del sistema poggia interamente sulle classi generate da ANTLR4. Il nucleo operativo è rappresentato dalla classe `DnDInterpreter`, la quale estende `DnDLangBaseVisitor<Object>`. Invece di tradurre il codice sorgente in un linguaggio intermedio o in bytecode, l'interprete esegue un'esplorazione diretta e ricorsiva dell'Abstract Syntax Tree (AST) generato dal Parser durante l'analisi del testo. Ciascun metodo `visit...` della classe è responsabile della valutazione semantica di uno specifico nodo dell'albero, restituendo un oggetto Java nativo (`Integer`, `Double`, `Boolean` o `String`) come risultato.

### 5.2 Gestione degli Scope e Record di Attivazione

La gestione della memoria e della visibilità è demandata alla classe `Environment.java`. Questa struttura implementa una pila di ambienti organizzata gerarchicamente tramite una classe interna ricorsiva chiamata `Scope`:

```java
private static class Scope {
    final Scope enclosing; // Riferimento allo scope immediatamente più esterno
    final Map<String, VariableSymbol> variables = new HashMap<>();
    final Map<String, FunctionSymbol> functions = new HashMap<>();
}

```

All'ingresso di un blocco di codice (`visitBlock`), viene invocato il metodo `env.enterBlock()`, il quale istanzia un nuovo `Scope` agganciando quello corrente come padre (`enclosing`). Alla fine del blocco, `env.exitBlock()` ripristina il puntatore al padre, distruggendo implicitamente il record di attivazione locale e liberando la memoria da variabili volatili. La risoluzione degli identificatori avviene risalendo la catena dei padri fino allo scope globale: questo garantisce la corretta implementazione dello scoping lessicale.

### 5.3 Difficoltà Tecniche e Soluzioni Adottate

#### A. Interruzione Prematura del Flusso (`return` e `break`)

Una delle problematiche più complesse nell'implementazione di un interprete basato sul pattern Visitor è l'interruzione immediata dell'esecuzione profonda di un ciclo o di una funzione quando si incontra un comando di uscita precoce come `return` o `break`. Poiché il Visitor esplora ricorsivamente l'albero tramite chiamate a metodi Java, un semplice comando di `return` dentro un blocco `if` nidificato interromperebbe solo il metodo del Visitor locale, non la funzione intera.

* **Soluzione per il `return**`: È stata sfruttata l'infrastruttura dei segnali della Java Virtual Machine tramite il lancio di un'eccezione di controllo personalizzata, denominata `ReturnException`. Quando l'interprete incontra un `returnStmt`, valuta l'eventuale espressione associata e lancia immediatamente la `ReturnException`. Il metodo `visitFunctionCallExpr`, che si trova al vertice della chiamata, racchiude l'attivazione della funzione all'interno di un blocco `try-catch`. Intercettando l'eccezione, estrae il valore racchiuso, ripristina l'ambiente ed esce in modo pulito.
* **Soluzione per il `break**`: Per il comando `break` nei cicli `while` e nei costrutti `switch`, è stato adottato un flag booleano interno all'interprete chiamato `isBreaking`. Quando viene rilevato un `breakStmt`, il flag viene posto a `true`. Tutti i cicli esecutivi di blocco controllano questo flag prima di passare al comando successivo; se attivo, interrompono l'iterazione e propagano l'uscita fino al gestore del ciclo primario, che provvederà a resettare il flag a `false`.

#### B. Parsing Dinamico e Sotto-Alberi per Stringhe Interpolate

L'implementazione delle stringhe interpolate `i"..."` richiedeva la capacità di valutare qualsiasi espressione complessa inserita all'interno dei delimitatori `${}` direttamente nel contesto corrente.

* **Soluzione**: Invece di limitare l'interpolazione a semplici variabili, il metodo `visitIStringExpr` estrae il testo racchiuso tramite espressioni regolari e, per ciascun frammento individuato, **istanzia al volo** un mini-flusso ANTLR completo (`CharStream` -> `Lexer` -> `TokenStream` -> `Parser`). Viene isolato il nodo sintattico `expr` e viene invocato ricorsivamente il Visitor principale (`this.visit(exprCtx)`) sul sotto-albero appena generato. Questo approccio permette di supportare stringhe estremamente potenti come `i"Risultato: ${d20 + getModifier(hero.strength)}"`, riutilizzando la logica di valutazione e lo stato di memoria dell'interprete principale senza alcuna duplicazione di codice.

---

## 6. Programmi di Test e Output Attesi

### 6.1 `hello.dnd`

Questo script convalida l'autogenerazione dei namespace e l'estrazione corretta dei tipi di dominio all'interno delle stringhe interpolate.

**Codice Sorgente**:

```dnd
hero: {
    String name = "Aelar";
    String class = "Monk";
    HP hp = 24;
    AC ac = 15;
    Gold gold = 50.0;
}

quest: {
    Int prova = adv d20;
    print(prova);
    print(i"Eroe: ${hero.name} | Class: ${hero.class} | HP: ${hero.hp} | AC: ${hero.ac} | Gold: ${hero.gold}");
}

```

**Output Atteso a Video**:

```text
[Valore numerico casuale tra 1 e 20 derivante dal lancio con vantaggio]
Eroe: Aelar | Class: Monk | 24 HP | 15 AC | 50.0 gp

```

### 6.2 `errors.dnd`

Script di controllo per verificare la robustezza del meccanismo Fail-Fast in presenza di operazioni matematiche illegali.

**Codice Sorgente**:

```dnd
hero: {
    HP hp = 20;
}

quest: {
    print("--- Test Fallimento Critico (Fail-Fast) ---");
    Int x = 10;
    Int y = 0;
    
    print("Tentativo di divisione per zero... L'interprete deve bloccarsi immediatamente!");
    Int z = x / y;
    
    print("Se vedi questa scritta, il Fail-Fast NON sta funzionando!");
}

```

**Output Atteso su Stderr**:

```text
--- Test Fallimento Critico (Fail-Fast) ---
Tentativo di divisione per zero... L'interprete deve bloccarsi immediatamente!
Errore runtime non gestito alla riga 10: divisione per zero

```

*(Nota: Il programma termina con exit code 1 immediatamente alla riga dell'errore, impedendo la stampa finale).*

### 6.3 `quest.dnd`

Simulazione intensiva di un combattimento strutturato che mette alla prova le funzioni con tipo di ritorno, l'operatore ternario, le espressioni dei dadi nativi e il costrutto condizionale `switch-case` con gestione del colpo critico (caso 20) e del fallimento critico (caso 1).

**Codice Sorgente**:

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
    Int round = 1;
    while (hero.hp > 0 && foe.hp > 0) {
        print(i"\n--- ROUND ${round} ---");
        Int txc = d20;
        print(i"${hero.name} lancia per colpire. Dado naturale: ${txc}");
        
        switch (txc) {
            case 1: {
                print("Fallimento Critico! Ti sbilanci e subisci danni.");
                hero.hp -= 2;
            }
            case 20: {
                Int dmg = calcDamage(8, hero.strMod, true);
                foe.hp -= dmg;
                print(i"SUCCESSO CRITICO! Inflitti ${dmg} danni devastanti. HP Mostro: ${foe.hp}");
            }
            default: {
                if (txc + hero.strMod >= foe.ac) {
                    Int dadoDanno = d8;
                    Int dmg = calcDamage(dadoDanno, hero.strMod, false);
                    foe.hp -= dmg;
                    print(i"Colpito! Inflitti ${dmg} danni. HP Mostro: ${foe.hp}");
                } else {
                    print("Mancato! L'arma impatta sullo scudo del Goblin.");
                }
            }
        }

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

**Output Atteso (Esempio di Tracciato di Gioco Dinamico)**:

```text
--- INIZIO SCONTRO: Garrick VS Goblin Capo ---

--- ROUND 1 ---
Garrick lancia per colpire. Dado naturale: 14
Colpito! Inflitti 7 danni. HP Mostro: 18
Il Goblin Capo contrattacca e ti colpisce! Subisci 4 danni. HP Eroe: 31

--- ROUND 2 ---
Garrick lancia per colpire. Dado naturale: 20
SUCCESSO CRITICO! Inflitti 15 danni devastanti. HP Mostro: 3
Il Goblin Capo manca il bersaglio.

--- ROUND 3 ---
Garrick lancia per colpire. Dado naturale: 11
Colpito! Inflitti 6 danni. HP Mostro: -3

Esito finale della Quest: VITTORIA al round 3!

```

### 6.4 `session.dnd`

Programma avanzato volto a testare lo stack dei record di attivazione della memoria tramite una **funzione ricorsiva** complessa (`furiaBerserker`) in cui la condizione di terminazione (caso base) dipende interamente dall'esito di un'espressione stocastica combinata.

**Codice Sorgente**:

```dnd
def Int furiaBerserker(Int bonusTxC, Int classeArmatura) {
    Int txc = d20 + bonusTxC;
    if (txc >= classeArmatura) {
        Int danno = d8;
        print(i" > Colpo a segno! (TxC: ${txc}). Inflitti ${danno} danni.");
        return danno + furiaBerserker(bonusTxC - 2, classeArmatura);
    } else {
        print(i" > La catena si spezza. L'attacco fallisce (TxC: ${txc}).");
        return 0; 
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
    print(i"--- INIZIO SCONTRO: ${hero.name} VS ${foe.name} ---");
    Int round = 1;

    while (hero.hp > 0 && foe.hp > 0) {
        print(i"\n--- ROUND ${round} ---");
        print(i"${hero.name} entra in Ira e scatena la Furia Berserker!");
        
        Int danniTotali = furiaBerserker(hero.strMod, foe.ac);
        if (danniTotali > 0) {
            foe.hp -= danniTotali;
            print(i"Risultato: La Furia ha inflitto un totale di ${danniTotali} danni! HP Golem: ${foe.hp}");
        } else {
            print("Risultato: Tordek manca clamorosamente il primo colpo.");
        }
        
        if (foe.hp > 0) {
            print(i"Il ${foe.name} contrattacca con uno schianto possente!");
            Int txcFoe = d20 + 4;
            if (txcFoe >= hero.ac) {
                Int dannoNemico = d10 + 5;
                hero.hp -= dannoNemico;
                print(i"Colpito! ${hero.name} subisce ${dannoNemico} danni. HP Eroe: ${hero.hp}");
            } else {
                print("L'eroe schiva il colpo!");
            }
        }
        round++;
    }
    print("----------------------------------------------------------------------");
    if (hero.hp > 0) {
        Gold tesoro = 500.0;
        print(i"Il Golem crolla a terra. Vittoria! Guadagni ${tesoro} gp.");
    } else {
        print("La resistenza del Golem era troppa. Tordek cade in battaglia.");
    }
}

```

**Output Atteso (Esempio di Tracciato di Gioco Dinamico)**:

```text
--- INIZIO SCONTRO: Tordek il Barbaro VS Golem di Carne ---

--- ROUND 1 ---
Tordek il Barbaro entra in Ira e scatena la Furia Berserker!
 > Colpo a segno! (TxC: 22). Inflitti 5 danni.
 > Colpo a segno! (TxC: 18). Inflitti 4 danni.
 > La catena si spezza. L'attacco fallisce (TxC: 11).
Risultato: La Furia ha inflitto un totale di 9 danni in questo turno! HP Golem: 71
Il Golem di Carne contrattacca con uno schianto possente!
L'eroe schiva il colpo!

... [I round proseguono fino alla morte di uno dei due contendenti] ...

----------------------------------------------------------------------
Il Golem crolla a terra. Vittoria! Guadagni 500.0 gp.

```


