# DnDLang

Un Domain Specific Language (DSL) per la descrizione, simulazione e risoluzione di incontri di combattimento in stile Dungeons & Dragons.

---

## 1. Introduzione

**DnDLang** è un linguaggio di programmazione imperativo tipizzato, sviluppato come DSL per modellare e automatizzare scenari di combattimento regolati dalle meccaniche di *Dungeons & Dragons* (Quinta Edizione).

### Caratteristiche principali

* **Tipizzazione con dichiarazione statica**: le variabili non possono cambiare tipo dopo la dichiarazione iniziale, anche se la verifica di compatibilità avviene a runtime prima di ogni legame in memoria.
* **Espressioni stocastiche native**: i dadi poliedrici da `d3` a `d20` sono costrutti di prima classe del linguaggio, valutati dinamicamente come qualsiasi altra espressione aritmetica.
* **Struttura a macro-sezioni**: separazione rigida e sequenziale tra dichiarazioni di funzioni (`def`), statistiche dei personaggi (`hero`, `foe`) e blocco esecutivo principale (`quest`).
* **Costrutti condizionali e iterativi completi**: ciclo `while` e selezione multipla `switch-case` con supporto nativo per le uscite premature (`break`).
* **Zucchero sintattico di dominio e generico**: operatori per vantaggio e svantaggio (`adv`, `dis`), tiri contrapposti (`save`, `vs`), assegnamenti composti (`+=`, `-=`, `*=`, `/=`), incremento e decremento unitario (`++`, `--`), operatore ternario e stringhe interpolate (`i"...${expr}..."`).
* **Gestione degli errori Fail-Fast**: alla prima anomalia semantica l'interprete interrompe immediatamente l'esecuzione con un messaggio che riporta la riga incriminata.

### Contesto applicativo

DnDLang nasce per consentire a Game Master e game designer di eseguire simulazioni statistiche intensive di incontri di combattimento. Tramite il linguaggio è possibile caricare le schede dei personaggi, definire cicli di comportamento e calcolare automaticamente migliaia di lanci di dado in frazioni di secondo, senza dipendere da motori di gioco esterni.

---

## 2. Guida Rapida

### 2.1 Requisiti di installazione ed esecuzione

L'interprete di DnDLang è sviluppato in Java e richiede i seguenti strumenti sul sistema ospite:

- **Java Development Kit (JDK) 17** o superiore.
- **Apache Maven 3.6+**, usato per la gestione delle dipendenze, il plugin ANTLR4 e la compilazione.
- **ANTLR 4.13+**, gestito internamente tramite il `pom.xml` di Maven.

### 2.2 Comandi per la generazione e l'esecuzione

Tutti i comandi vanno eseguiti dalla directory radice del progetto, dove risiedono `DnDLang.g4` e `pom.xml`.

**1. Generazione dei sorgenti ANTLR e compilazione:**

```bash
mvn clean compile
```

Il comando scarica le librerie necessarie, analizza la grammatica e genera automaticamente i file Java ausiliari (`DnDLangLexer.java`, `DnDLangParser.java`, `DnDLangBaseVisitor.java`), per poi compilare l'intera applicazione inclusi i sorgenti scritti a mano.

**2. Esecuzione di uno script sorgente:**

```bash
mvn exec:java -Dexec.mainClass="it.univr.dndlang.Main" -Dexec.args="programs/quest.dnd"
```

### 2.3 Programma d'esempio: HelloWorld esteso

Il seguente frammento illustra l'interazione tra sezioni, i tipi nativi di dominio e le stringhe interpolate con formattazione automatica:

```dnd
hero: {
    String name = "Xanaphia";
    HP hp = 18;
    AC ac = 16;
    Gold borsa = 15.5;
}

quest: {
    print("--- Sessione Inizializzata ---");
    Int iniziativa = adv d20;
    print(i"L'avventuriera ${hero.name} (AC: ${hero.ac}) scende nel dungeon con ${hero.borsa}. Iniziativa: ${iniziativa}");
}
```

---

## 3. Sintassi

### 3.1 Struttura di un programma

Un programma DnDLang impone un ordine sequenziale fisso tra le macro-sezioni, formalizzato dalla regola di partenza della grammatica:

```antlr
program : functionSection? heroSection? foeSection? questSection EOF ;
```

* **`functionSection`**: blocco opzionale che contiene esclusivamente le definizioni di funzioni precedute dalla keyword `def`. È di natura puramente dichiarativa: le funzioni vengono registrate nell'ambiente ma non eseguite.
* **`heroSection` (`hero: { ... }`)**: blocco deputato alla definizione delle statistiche dell'eroe protagonista. Le variabili dichiarate al suo interno vengono automaticamente registrate con prefisso `hero.` nello scope globale.
* **`foeSection` (`foe: { ... }`)**: funziona come `heroSection`, con prefisso `foe.`.
* **`questSection` (`quest: { ... }`)**: il corpo esecutivo centrale, equivalente al metodo `main`. Qui risiedono i cicli di combattimento e la logica principale del programma.

L'ordine è vincolante: le funzioni devono precedere `hero` e `foe` perché queste sezioni possono chiamarle durante l'inizializzazione delle proprie variabili (come avviene in `quest.dnd`, dove `getModifier()` viene invocata dentro `hero:`).

### 3.2 Tipi di dato

| Tipo | Descrizione | Esempio |
| --- | --- | --- |
| `Int` | Intero a 32 bit con segno | `42`, `-5` |
| `Float` | Numero in virgola mobile a 64 bit | `3.14`, `-0.5` |
| `Bool` | Valore booleano | `true`, `false` |
| `String` | Sequenza letterale di testo | `"Spada Corta"` |
| `Void` | Tipo speciale non assegnabile, usato solo come tipo di ritorno di funzione | — |
| `HP` | Punti Ferita, compatibile con `Int` | `24` |
| `AC` | Classe Armatura, compatibile con `Int` | `15` |
| `Gold` | Monete d'oro, compatibile con `Float` | `50.0` |

I tipi `HP` e `AC` sono semanticamente equivalenti a `Int` a runtime (entrambi rappresentati da `Integer` in Java). Il tipo `Gold` è equivalente a `Float` (rappresentato da `Double`). La distinzione esiste per leggibilità del codice e per la formattazione automatica nelle stringhe interpolate, dove i suffissi ` HP`, ` AC` e ` gp` vengono aggiunti automaticamente.

### 3.3 Token dei dadi poliedrici e operatori di dominio

I dadi tradizionali sono token lessicali di prima classe: `d20`, `d12`, `d10`, `d8`, `d6`, `d4`, `d3`. Vengono valutati come espressioni atomiche in qualsiasi contesto aritmetico, ad esempio `3 * d6 + 4`.

Gli operatori di dominio aggiuntivi sono:

* **`adv` / `dis`**: modificatori prefissi applicabili a qualsiasi dado. `adv d20` lancia due d20 e restituisce il valore massimo (*Vantaggio*); `dis d20` restituisce il minimo (*Svantaggio*). Entrambi stampano a video il dettaglio dei due lanci.
* **`save` / `vs`**: operatori infissi binari che restituiscono un `Bool`. `expr1 save expr2` verifica se il tiro supera o uguaglia la Classe di Difficoltà. `expr1 vs expr2` modella una prova contrapposta in cui il primo operando deve superare *strettamente* il secondo.

### 3.4 Dichiarazione di variabile e assegnamento

Le variabili si dichiarano con tipo esplicito e inizializzatore obbligatorio:

```dnd
Int forza = 16;
HP vita = 35;
Gold borsa = 50.0;
```

Gli assegnamenti semplici e composti operano su variabili già dichiarate:

```dnd
forza = 18;
vita -= 5;     // equivale a vita = vita - 5
borsa += 10.0;
```

### 3.5 Costrutti di controllo del flusso

**If-else:**
```dnd
if (txc >= foe.ac) {
    foe.hp -= danno;
} else {
    print("Mancato!");
}
```

**While:**
```dnd
while (hero.hp > 0 && foe.hp > 0) {
    // corpo del ciclo
}
```

**Switch-case:** ogni `case` è isolato, senza fall-through. Appena viene trovata una corrispondenza, il blocco relativo viene eseguito e lo switch termina. Il `default` è opzionale.

```dnd
switch (txc) {
    case 1:  { print("Fallimento Critico!"); }
    case 20: { print("Successo Critico!"); }
    default: { print("Risultato normale."); }
}
```

**Break:** interrompe il ciclo `while` corrente. Usarlo fuori da un ciclo produce un errore runtime.

### 3.6 Funzioni

Le funzioni si dichiarano nella `functionSection` con la keyword `def`, specificando il tipo di ritorno, il nome, i parametri e il corpo:

```dnd
def Int getModifier(Int score) {
    return (score - 10) / 2;
}
```

Il tipo `Void` è ammesso come tipo di ritorno ma non come tipo di parametro. Le funzioni possono richiamare se stesse ricorsivamente.

### 3.7 Zucchero sintattico

**Incremento e decremento unitario:**

```dnd
round++;   // post-incremento: restituisce il vecchio valore, poi incrementa
++round;   // pre-incremento: incrementa, poi restituisce il nuovo valore
```

**Operatore ternario:**

```dnd
String esito = (hero.hp > 0) ? "VITTORIA" : "SCONFITTA";
```

**Stringhe interpolate:** una stringa preceduta da `i` permette di incorporare espressioni arbitrarie tra `${}`. Per le variabili di tipo `HP`, `AC` e `Gold` viene aggiunto automaticamente il suffisso di tipo.

```dnd
print(i"HP Eroe: ${hero.hp} | Oro: ${hero.gold}");
// Output: HP Eroe: 35 HP | Oro: 50.0 gp
```

### 3.8 Limiti sintattici

* `Void` non è ammesso come tipo di parametro di funzione.
* Non è ammessa la ridichiarazione di una funzione con lo stesso nome.
* `break` è valido solo all'interno di un ciclo `while`; usarlo in un blocco `hero:`, `foe:` o fuori da un ciclo produce un errore runtime immediato.
* La dot-notation (`hero.hp`, `foe.ac`) è riservata ai namespace delle sezioni struct: non è possibile definire variabili con un punto nel nome al di fuori di quelle sezioni.

---

## 4. Semantica

### 4.1 Scelte progettuali principali

* **Dichiarazione del tipo**: il tipo di una variabile è dichiarato esplicitamente al momento della sua creazione e non può cambiare per tutta la durata del programma. La verifica di compatibilità tra il valore calcolato e il tipo dichiarato avviene a runtime, prima di ogni legame in memoria. Non esiste una fase di analisi statica separata prima dell'esecuzione: DnDLang è un interprete tree-walking puro.

* **Visibilità (scoping lessicale)**: ogni blocco `{ ... }` apre un nuovo scope isolato. Le variabili dichiarate in un blocco interno non sopravvivono alla chiusura del blocco. Una variabile può oscurare temporaneamente una variabile omonima di un blocco esterno (*shadowing*): durante l'esecuzione del blocco interno il lookup trova quella locale; all'uscita torna visibile quella esterna.

* **Namespace autogenerati per `hero` e `foe`**: i blocchi `hero:` e `foe:` non aprono un nuovo scope. Le variabili dichiarate al loro interno vengono registrate direttamente nello scope globale con prefisso automatico (`hero.nome`, `foe.hp`, ecc.), simulando una struttura a record accessibile da tutta la `questSection`.

* **Valutazione delle espressioni**: tutte le espressioni aritmetiche, le chiamate di funzione e le stringhe interpolate vengono valutate in modo *eager* (ansioso), cioè immediatamente al momento dell'incontro nel flusso di esecuzione.

* **Valutazione logica non short-circuit**: gli operatori `&&` e `||` valutano sempre entrambi gli operandi, anche quando il risultato è già determinabile dal primo. Questa scelta è deliberata: in un linguaggio orientato alla simulazione stocastica, è fondamentale che i dadi nel secondo operando vengano sempre lanciati, per preservare la coerenza dello stato di gioco. Ad esempio, nell'espressione `hero.hp > 0 && d20 > 5`, il dado viene lanciato indipendentemente dal valore di `hero.hp`.

* **Passaggio dei parametri**: rigorosamente *call-by-value*. Gli argomenti vengono valutati nello scope del chiamante prima di entrare nella funzione, e i parametri formali sono copie locali. Modificare un parametro dentro una funzione non altera la variabile originale.

### 4.2 Gerarchia dei tipi e coercizione implicita

DnDLang supporta un insieme limitato di conversioni implicite, applicate automaticamente dall'interprete tramite la funzione `coerceToDeclaredType` al momento dell'assegnamento o del passaggio di un argomento.

Le relazioni di compatibilità implementate sono le seguenti:

$$\text{HP} \subsetneq \text{Int} \qquad \text{AC} \subsetneq \text{Int} \qquad \text{Int} \subsetneq \text{Gold}$$

In pratica:

* Un valore `Double` assegnato a una variabile `Int`, `HP` o `AC` viene convertito tramite **troncamento verso zero** (la parte decimale viene scartata: `3.9` diventa `3`, `-3.9` diventa `-3`).
* Un valore `Integer` assegnato a una variabile `Gold` viene **promosso** a `Double`.
* Il tipo `Float` non partecipa ad alcuna catena di promozione implicita da `Int`: un valore intero assegnato a una variabile `Float` produce un errore di tipo incompatibile.
* `Bool` e `String` sono completamente isolati: non esiste alcuna forma di conversione implicita o esplicita tra questi tipi e i tipi numerici.

### 4.3 Gestione degli errori a runtime

Il linguaggio adotta una filosofia **Fail-Fast**: alla prima anomalia rilevata l'interprete lancia una `DnDLangError` (una `RuntimeException` custom con numero di riga), che viene catturata in `Main.java` e presentata su `stderr` prima di terminare il processo con exit code 1. Non vengono assegnati valori di fallback né ignorati gli errori.

Gli errori intercettati comprendono:

* Utilizzo o assegnamento di variabili non dichiarate nello scope corrente o in quelli superiori.
* Divisione o modulo per zero, sia con `/` che con `/=`.
* Numero di argomenti passati a una funzione diverso da quello atteso dalla firma.
* Incompatibilità di tipo non risolvibile tramite coercizione (es. assegnare un `Bool` a un `Int`).
* Funzione non-`Void` che termina senza eseguire un'istruzione `return`.
* Funzione `Void` che tenta di restituire un valore.
* Utilizzo di `break` fuori da un ciclo `while` (incluso l'interno di sezioni `hero:` e `foe:`).
* Applicazione degli operatori `&&`, `||`, `!` a operandi non booleani.
* Applicazione dell'operatore unario `-` a un valore non numerico.
* Chiamata a una funzione non dichiarata.
* Ridichiarazione di una funzione con lo stesso nome.

### 4.4 Formalizzazione della semantica operazionale

Di seguito alcune regole di transizione della semantica operazionale strutturata (SOS) di DnDLang, dove lo stato $\sigma : \text{Id} \rightarrow \text{Val}$ mappa identificatori a valori.

**Assegnamento semplice:**

$$\text{Assign} \quad \frac{\langle e,\, \sigma \rangle \rightarrow v}{\langle \texttt{id = e;},\, \sigma \rangle \rightarrow \sigma[\texttt{id} \mapsto v]}$$

**Assegnamento composto additivo (`+=`):**

$$\text{Assign-Compound} \quad \frac{\sigma(\texttt{id}) = v_1 \quad \langle e,\, \sigma \rangle \rightarrow v_2 \quad v_3 = v_1 + v_2}{\langle \texttt{id += e;},\, \sigma \rangle \rightarrow \sigma[\texttt{id} \mapsto v_3]}$$

**While — condizione falsa:**

$$\text{While-False} \quad \frac{\langle \textit{cond},\, \sigma \rangle \rightarrow \texttt{false}}{\langle \texttt{while(}\textit{cond}\texttt{) \{ body \}},\, \sigma \rangle \rightarrow \sigma}$$

**While — condizione vera:**

$$\text{While-True} \quad \frac{\langle \textit{cond},\, \sigma \rangle \rightarrow \texttt{true} \quad \langle \textit{body},\, \sigma \rangle \rightarrow \sigma' \quad \langle \texttt{while(}\textit{cond}\texttt{) \{ body \}},\, \sigma' \rangle \rightarrow \sigma''}{\langle \texttt{while(}\textit{cond}\texttt{) \{ body \}},\, \sigma \rangle \rightarrow \sigma''}$$

**Chiamata di funzione con return (call-by-value):**

$$\text{FunCall} \quad \frac{\text{argomenti valutati in } \sigma \quad \sigma_f = \{p_1 \mapsto v_1, \ldots, p_n \mapsto v_n\} \quad \langle \textit{body},\, \sigma \cup \sigma_f \rangle \;\uparrow\; \texttt{ReturnException}(v)}{\langle f(\textit{args}),\, \sigma \rangle \rightarrow v}$$

La freccia $\uparrow$ indica che l'esecuzione del corpo viene interrotta da una `ReturnException`, il cui valore viene estratto e restituito al chiamante.

**Tiro salvezza:**

$$\text{Save-Success} \quad \frac{\langle e_1,\, \sigma \rangle \rightarrow v_1 \quad \langle e_2,\, \sigma \rangle \rightarrow v_2 \quad v_1 \ge v_2}{\langle e_1 \;\texttt{save}\; e_2,\, \sigma \rangle \rightarrow \texttt{true}}$$

$$\text{Save-Failure} \quad \frac{\langle e_1,\, \sigma \rangle \rightarrow v_1 \quad \langle e_2,\, \sigma \rangle \rightarrow v_2 \quad v_1 < v_2}{\langle e_1 \;\texttt{save}\; e_2,\, \sigma \rangle \rightarrow \texttt{false}}$$

**Lancio con vantaggio:**

$$\text{Dice-Advantage} \quad \frac{r_1 = \text{random}(1, \textit{sides}) \quad r_2 = \text{random}(1, \textit{sides}) \quad v = \max(r_1, r_2)}{\langle \texttt{adv}\; \textit{diceOnly},\, \sigma \rangle \rightarrow v}$$

---

## 5. Implementazione

### 5.1 Architettura del Visitor e visita dell'AST

Il nucleo dell'interprete è la classe `DnDInterpreter`, che estende `DnDLangBaseVisitor<Object>` generata da ANTLR4. Il tipo generico `Object` è necessario perché i diversi nodi dell'albero restituiscono tipi Java eterogenei: `Integer`, `Double`, `Boolean`, `String` o `null`. Non esiste un tipo `Value` wrapper: i valori circolano come `Object` e vengono esaminati con `instanceof` nei punti in cui il tipo è rilevante.

L'interprete visita ricorsivamente l'AST in post-ordine: prima vengono valutati i sottoalberi figli, poi il nodo padre usa i loro risultati per produrre il proprio. Ad esempio, per l'espressione `d20 + strMod`, il visitor visita prima il nodo `D20Expr` (che lancia il dado e restituisce un `Integer`), poi il nodo `IdExpr` per `strMod` (che legge il valore dall'ambiente), e infine `AddSubExpr` somma i due risultati.

### 5.2 Gestione degli scope e tabella dei simboli

La memoria di esecuzione è gestita da `Environment.java`, che implementa una pila di scope tramite la classe interna `Scope`:

```java
private static class Scope {
    final Scope enclosing; // riferimento allo scope immediatamente esterno
    final Map<String, VariableSymbol> variables = new HashMap<>();
    final Map<String, FunctionSymbol>  functions  = new HashMap<>();
}
```

All'ingresso di ogni blocco `visitBlock()` invoca `env.enterBlock()`, che istanzia un nuovo `Scope` collegandolo al corrente come padre. All'uscita, `env.exitBlock()` ripristina il puntatore al padre, liberando implicitamente le variabili locali. La risoluzione degli identificatori risale la catena `enclosing` fino allo scope globale.

Le funzioni vengono registrate nello scope globale da `visitFunctionDecl()` al momento della visita della `functionSection`. Per questo è sufficiente che siano dichiarate prima della loro chiamata nell'ordine di esecuzione del programma, non necessariamente prima nel testo sorgente — anche se la struttura rigida del linguaggio impone l'uno e l'altro.

Le sezioni `hero:` e `foe:` usano un meccanismo diverso: non aprono un nuovo scope ma dichiarano le proprie variabili direttamente nello scope globale con un prefisso anteposto (`hero.nome`, `foe.hp`). Questo consente alla `questSection` di accedervi come se fossero campi di un record globale.

### 5.3 Difficoltà tecniche e soluzioni adottate

#### A. Interruzione prematura del flusso: `return` e `break`

Il Visitor esplora l'AST tramite chiamate Java ricorsive. Un semplice `return` dentro un blocco annidato interromperebbe solo il metodo Java locale, non la funzione DnDLang intera.

**Soluzione per `return`**: quando l'interprete incontra un `returnStmt`, valuta l'espressione associata e lancia una `ReturnException`. Questa eccezione è costruita con `super(null, null, false, false)` per disabilitare la creazione dello stack trace Java (overhead inutile, dato che non è un errore). Il metodo `visitFunctionCallExpr`, che si trova al vertice della chiamata, racchiude l'esecuzione del corpo in un blocco `try-catch-finally`: il `catch` intercetta la `ReturnException`, ne estrae il valore e verifica la compatibilità con il tipo di ritorno dichiarato; il `finally` garantisce che `env.exitBlock()` e il ripristino di `currentReturnType` avvengano sempre, anche in presenza di eccezioni impreviste. Questa struttura è ciò che rende la ricorsione corretta: ogni invocazione di `visitFunctionCallExpr` salva e ripristina il proprio `currentReturnType` in modo indipendente.

**Soluzione per `break`**: il `break` nei cicli `while` e nei costrutti `switch` è implementato con il flag booleano `isBreaking` nell'interprete. Quando viene incontrato un `breakStmt`, il flag viene posto a `true`. Il metodo `visitBlock()` controlla il flag dopo ogni istruzione e interrompe il proprio loop se attivo, propagando così l'uscita verso l'esterno. Il metodo `visitWhileStmt()` consuma il flag (lo riporta a `false`) dopo aver rilevato il break, impedendo che si propaghi oltre il ciclo. Lo stesso fa `visitSwitchStmt()` dopo l'esecuzione del case corrispondente. Tentare un `break` dentro `hero:` o `foe:`, dove non c'è né un `while` né uno `switch` a consumarlo, viene intercettato da `visitDeclarativeBlock()` che lancia una `DnDLangError`.

#### B. Parsing dinamico delle stringhe interpolate

Le stringhe interpolate `i"..."` richiedono la valutazione di espressioni arbitrarie nel contesto corrente. Il metodo `visitIStringExpr` estrae ogni frammento `${...}` tramite espressione regolare e, per ciascuno, istanzia al volo una pipeline ANTLR completa (`CharStream` → `Lexer` → `TokenStream` → `Parser`). Viene isolato il nodo `expr` e su di esso viene invocato ricorsivamente `this.visit(exprCtx)`, che opera con l'`Environment` corrente e quindi vede tutte le variabili dichiarate fino a quel punto. Dopo la valutazione, se l'espressione corrisponde a una variabile di tipo `HP`, `AC` o `Gold`, il suffisso appropriato (` HP`, ` AC`, ` gp`) viene aggiunto automaticamente al risultato.

---

## 6. Programmi di test e output attesi

### 6.1 `hello.dnd`

Verifica l'autogenerazione dei namespace `hero.*` e la formattazione automatica dei tipi di dominio nelle stringhe interpolate.

**Codice sorgente:**

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

**Output atteso:**

```
 > [Vantaggio] Lanciati X e Y -> Tengo Z
Z
Eroe: Aelar | Class: Monk | HP: 24 HP | AC: 15 AC | Gold: 50.0 gp
```

*(Dove X, Y e Z sono valori casuali tra 1 e 20. Il suffisso di tipo è aggiunto automaticamente dall'interprete per le variabili HP, AC e Gold.)*

### 6.2 `errors.dnd`

Verifica la robustezza del meccanismo Fail-Fast in presenza di una divisione per zero.

**Codice sorgente:**

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

**Output atteso su stderr:**

```
--- Test Fallimento Critico (Fail-Fast) ---
Tentativo di divisione per zero... L'interprete deve bloccarsi immediatamente!
Errore runtime non gestito alla riga 10: divisione per zero
```

Il programma termina con exit code 1 alla riga dell'errore. L'ultima `print` non viene mai raggiunta.

### 6.3 `quest.dnd`

Simulazione di un combattimento strutturato che esercita le funzioni con tipo di ritorno, l'operatore ternario, i dadi nativi e il costrutto `switch-case` con gestione del colpo critico (dado naturale 20) e del fallimento critico (dado naturale 1).

**Codice sorgente:**

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

**Esempio di tracciato di gioco:**

```
--- INIZIO SCONTRO: Garrick VS Goblin Capo ---
Modificatore di Forza Eroe: 3
Modificatore di Forza Nemico: 1

--- ROUND 1 ---
Garrick lancia per colpire. Dado naturale: 14
Colpito! Inflitti 7 danni. HP Mostro: 18 HP
Il Goblin Capo contrattacca e ti colpisce! Subisci 4 danni. HP Eroe: 31 HP

--- ROUND 2 ---
Garrick lancia per colpire. Dado naturale: 20
SUCCESSO CRITICO! Inflitti 15 danni devastanti. HP Mostro: 3 HP
Il Goblin Capo manca il bersaglio.

--- ROUND 3 ---
Garrick lancia per colpire. Dado naturale: 11
Colpito! Inflitti 6 danni. HP Mostro: -3 HP

Esito finale della Quest: VITTORIA al round 3!
```

### 6.4 `session.dnd`

Programma avanzato che mette alla prova lo stack dei record di attivazione tramite la funzione ricorsiva `furiaBerserker`, il cui caso base dipende dall'esito di un'espressione stocastica.

**Codice sorgente:**

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
            print(i"Risultato: La Furia ha inflitto un totale di ${danniTotali} danni in questo turno! HP Golem: ${foe.hp}");
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
        print(i"Il Golem crolla a terra. Vittoria! Guadagni ${tesoro}.");
    } else {
        print("La resistenza del Golem era troppa. Tordek cade in battaglia.");
    }
}
```

**Esempio di tracciato di gioco:**

```
--- INIZIO SCONTRO: Tordek il Barbaro VS Golem di Carne ---

--- ROUND 1 ---
Tordek il Barbaro entra in Ira e scatena la Furia Berserker!
 > Colpo a segno! (TxC: 22). Inflitti 5 danni.
 > Colpo a segno! (TxC: 18). Inflitti 4 danni.
 > La catena si spezza. L'attacco fallisce (TxC: 11).
Risultato: La Furia ha inflitto un totale di 9 danni in questo turno! HP Golem: 71 HP
Il Golem di Carne contrattacca con uno schianto possente!
L'eroe schiva il colpo!

... [i round proseguono fino alla morte di uno dei due contendenti] ...

----------------------------------------------------------------------
Il Golem crolla a terra. Vittoria! Guadagni 500.0 gp.
```

La terminazione della ricorsione è garantita dalla decrescita monotona di `bonusTxC` (si riduce di 2 a ogni chiamata), che rende prima o poi impossibile superare `classeArmatura` anche con il massimo sul d20.
