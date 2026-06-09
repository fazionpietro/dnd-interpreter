# DnDLang

Un Domain Specific Language (DSL) per la descrizione, simulazione e risoluzione di incontri e sessioni di combattimento in stile Dungeons & Dragons.

## 1. Introduzione

**DnDLang** è un linguaggio di programmazione imperativo tipizzato sviluppato come DSL per modellare, simulare e automatizzare scenari di combattimento e interazioni stocastiche regolate dalle meccaniche di *Dungeons & Dragons* (Quinta Edizione).

### Caratteristiche principali

* **Tipizzazione statica** con tipi di dominio dedicati al gioco di ruolo (`HP`, `AC`, `Gold`).
* **Espressioni stocastiche native**: i dadi poliedrici (da `d3` a `d20`) sono cittadini di prima classe del linguaggio e vengono valutati dinamicamente.
* **Struttura a macro-sezioni**: separazione rigida e sequenziale tra funzioni (`def`), statistiche dei personaggi (`hero`, `foe`) e blocco esecutivo principale (`quest`).
* **Costrutti condizionali e iterativi completi**: cicli `while` e flusso condizionato `switch-case` con gestione nativa delle uscite premature (`break`).
* **Zucchero sintattico di dominio e generico**: operatori per il vantaggio/svantaggio (`adv`, `dis`), tiri contrapposti (`save`, `vs`), assegnamenti composti (`+=`, `-=`, etc.), incremento/decremento (`++`, `--`), operatore ternario e stringhe interpolate (`i"...${expr}..."`).
* **Gestione degli errori**: controllo dei tipi statico e filosofia Fail-Fast a runtime per arrestare immediatamente il programma in caso di operazioni non permesse (es. divisione per zero).

### Contesto applicativo

DnDLang nasce per permettere a Game Master e game designer di eseguire simulazioni statistiche intensive di incontri di combattimento (*encounters*). Il linguaggio consente di caricare le schede dei personaggi e dei mostri, impostare cicli di comportamento e calcolare rapidamente le probabilità di sopravvivenza automatizzando migliaia di lanci di dado in frazioni di secondo, senza dipendere da motori di gioco esterni.


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

### 3.1 Struttura di un programma

Un programma DnDLang impone un ordine sequenziale immutabile per le macro-sezioni, formalizzato dalla seguente regola di partenza:

```antlr
program : functionSection? heroSection? foeSection? questSection EOF ;

```

* **`functionSection`**: blocco opzionale che contiene esclusivamente le definizioni delle funzioni globali precedute dalla keyword `def`. Ha natura puramente dichiarativa.
* **`heroSection` (`hero: { ... }`)**: blocco isolato deputato alla definizione delle statistiche dell'eroe protagonista.
* **`foeSection` (`foe: { ... }`)**: blocco riservato alle statistiche delle creature ostili.
* **`questSection` (`quest: { ... }`)**: rappresenta il corpo esecutivo centrale (equivalente al metodo `main`). Qui risiedono i cicli di combattimento e la logica algoritmica principale.

### 3.2 Tipi di dato

| Tipo | Descrizione | Esempio di valore |
| --- | --- | --- |
| `Int` | Intero a 32 bit con segno | `42`, `-5` |
| `Float` | Numero in virgola mobile a 64 bit | `3.14`, `-0.5` |
| `Bool` | Valore booleano | `true`, `false` |
| `String` | Sequenza letterale di testo | `"Spada Corta"` |
| `Void` | Tipo speciale non assegnabile (solo per i ritorni di funzione) | - |
| `HP` | Punti Ferita (sottotipo di `Int`) | `24` |
| `AC` | Classe Armatura (sottotipo di `Int`) | `15` |
| `Gold` | Monete d'oro (sottotipo di `Float`) | `50.0` |

I tipi `HP` e `AC` estendono `Int`, mentre `Gold` estende `Float`. Sono compatibili nelle espressioni aritmetiche con i rispettivi tipi primitivi di riferimento attraverso meccanismi di promozione o troncamento implicito.

### 3.3 Token dei Dadi Poliedrici e Operatori di Dominio

La gestione stocastica si basa su token dedicati che mappano i dadi tradizionali:
`d20`, `d12`, `d10`, `d8`, `d6`, `d4`, `d3`.

Vengono valutati come espressioni atomiche all'interno di qualsiasi operazione aritmetica (es. `3 * d6 + 4`). I costrutti speciali per i tiri includono:

* **`adv` / `dis`**: modificatori prefissi applicabili alle espressioni dei dadi per implementare il *Vantaggio* (lancio di due dadi e selezione del valore massimo) o lo *Svantaggio* (selezione del valore minimo).
* **`save` / `vs`**: operatori infissi binari. `expr1 save expr2` verifica se il tiro supera o uguaglia una determinata Classe di Difficoltà (CD). `expr1 vs expr2` modella una prova contrapposta dove il primo elemento deve superare strettamente il secondo.


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

Le conversioni numeriche implicite (coercizione) seguono le catene di sottotipaggio:

$$\text{HP} \subsetneq \text{Int} \subsetneq \text{Float}$$
$$\text{AC} \subsetneq \text{Int} \subsetneq \text{Float}$$
$$\text{Int} \subsetneq \text{Gold} \subsetneq \text{Float}$$
    
Qualsiasi valore decimale assegnato a variabili di tipo `Int`, `HP` o `AC` viene automaticamente convertito tramite troncamento della parte decimale. Il tipo `Int` viene promosso implicitamente verso `Gold` o `Float`. Le stringhe e i booleani sono totalmente isolati: non è ammessa alcuna forma di conversione implicita o esplicita con i tipi numerici. Gli operatori logici `&&` e `||` eseguono una valutazione completa di entrambi i rami per garantire la coerenza dello stato stocastico dei dadi.

### 4.3 Gestione degli errori a runtime

Il linguaggio adotta una filosofia difensiva definita **Fail-Fast**.L'interprete non assegna valori di fallback o di default, ma provoca l'arresto immediato del thread esecutivo in presenza di anomalie semantiche, tracciandole tramite la classe custom `DnDLangError`. Gli errori intercettati includono::

* Utilizzo o assegnamento di variabili non preventivamente dichiarate nello scope corrente o superiore.
* Errori aritmetici irreversibili, specificamente la divisione o il calcolo del modulo per zero (es. `x / 0`).
* Discrepanza tra il numero di argomenti passati a una funzione in fase di chiamata e i parametri previsti dalla firma della stessa.
* Incompatibilità di tipo insanabile durante un assegnamento o il passaggio di un parametro.

### 4.4 Formalizzazione della Semantica Operazionale (Regole di Transizione)

Di seguito alcune regole di transizione della semantica operazionale strutturata (SOS) di DnDLang, dove la memoria è definita come una funzione $\sigma: \text{Id} \rightarrow \text{Val}$.

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

L'architettura del sistema si basa sulle classi generate da ANTLR4. Il nucleo operativo è rappresentato dalla classe `DnDInterpreter`, la quale estende `DnDLangBaseVisitor<Object>`. L'interprete esegue un'esplorazione diretta e ricorsiva dell'Abstract Syntax Tree (AST) generato dal Parser durante l'analisi del testo. Ciascun metodo `visit...` della classe è responsabile della valutazione semantica di uno specifico nodo dell'albero, restituendo un oggetto Java nativo (`Integer`, `Double`, `Boolean` o `String`) come risultato.

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

Poiché il Visitor esplora ricorsivamente l'albero tramite chiamate a metodi Java, un semplice comando di return dentro un blocco nidificato interromperebbe solo il metodo del Visitor locale e non la funzione intera.

* **Soluzione per il `return` **: È stata sfruttata l'infrastruttura dei segnali della Java Virtual Machine tramite il lancio di un'eccezione di controllo personalizzata, denominata `ReturnException`. Quando l'interprete incontra un `returnStmt`, valuta l'eventuale espressione associata e lancia immediatamente la `ReturnException`. Il metodo `visitFunctionCallExpr`, che si trova al vertice della chiamata, racchiude l'attivazione della funzione all'interno di un blocco `try-catch`. Intercettando l'eccezione, estrae il valore racchiuso, ripristina l'ambiente ed esce in modo pulito.
* **Soluzione per il `break`**: Per il comando `break` nei cicli `while` e nei costrutti `switch`, è stato adottato un flag booleano interno all'interprete chiamato `isBreaking`. Quando viene rilevato un `breakStmt`, il flag viene posto a `true`. Tutti i cicli esecutivi di blocco controllano questo flag prima di passare al comando successivo; se attivo, interrompono l'iterazione e propagano l'uscita fino al gestore del ciclo primario, che provvederà a resettare il flag a `false`.

#### B. Parsing Dinamico e Sotto-Alberi per Stringhe Interpolate

L'implementazione delle stringhe interpolate `i"..."` richiedeva la capacità di valutare qualsiasi espressione complessa inserita all'interno dei delimitatori `${}` direttamente nel contesto corrente.
Il metodo visitIStringExpr estrae il testo racchiuso tramite espressioni regolari e, per ciascun frammento individuato, istanzia al volo un mini-flusso ANTLR completo (CharStream -> Lexer -> TokenStream -> Parser). Viene isolato il nodo sintattico expr e viene invocato ricorsivamente il Visitor principale (this.visit(exprCtx)) sul sotto-albero appena generato.

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


