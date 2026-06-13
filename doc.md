# DnDLang

Un Domain Specific Language (DSL) per la simulazione di incontri di combattimento in stile Dungeons & Dragons.

---

## 1. Introduzione

**DnDLang** è un linguaggio imperativo tipizzato pensato per modellare scenari di combattimento secondo le meccaniche di _Dungeons & Dragons_ (5e).

### Caratteristiche principali

- **Tipizzazione statica**: il tipo di ogni variabile è fissato alla dichiarazione. La verifica di compatibilità avviene a runtime.
- **Dadi poliedrici nativi**: `d3`–`d20` sono espressioni di prima classe, utilizzabili in qualsiasi contesto aritmetico.
- **Struttura a sezioni**: funzioni (`def`), statistiche dei personaggi (`hero`, `foe`) e corpo principale (`quest`), in quest'ordine.
- **Controllo di flusso**: `if-else`, `while`, `switch-case-default`, `break`.
- **Zucchero sintattico**: vantaggio/svantaggio (`adv`/`dis`), tiri salvezza (`save`/`vs`), assegnamenti composti (`+=`, `-=`, `*=`, `/=`), `++`/`--`, ternario, stringhe interpolate (`i"...${expr}..."`). 
- **Fail-Fast**: al primo errore l'interprete si ferma con messaggio e numero di riga.

### Contesto applicativo

DnDLang permette a Game Master e game designer di simulare incontri di combattimento in modo automatico: si caricano le schede dei personaggi, si definisce la logica di comportamento e si eseguono migliaia di lanci di dado in frazioni di secondo, senza motori di gioco esterni.

---

## 2. Guida Rapida

### Requisiti

- **Java 17** o superiore
- **Apache Maven 3.6+** (gestisce ANTLR 4.13 internamente tramite `pom.xml`)

### Compilazione ed esecuzione

```bash
# dalla directory radice del progetto:
mvn clean compile
mvn exec:java -Dexec.mainClass="it.univr.dndlang.Main" -Dexec.args="programs/session.dnd"
```

`mvn clean compile` genera i sorgenti ANTLR (`DnDLangLexer.java`, `DnDLangParser.java`, `DnDLangBaseVisitor.java`) e compila il progetto.

### Hello World

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

Un programma DnDLang è composto da quattro sezioni, tutte opzionali tranne `quest`, in ordine fisso:

```antlr
program : functionSection? heroSection? foeSection? questSection EOF ;
```

- **`functionSection`**: definizioni di funzioni (`def`). Puramente dichiarativa.
- **`heroSection` (`hero: { ... }`)**: statistiche dell'eroe. Le variabili vengono registrate nello scope globale con prefisso `hero.`.
- **`foeSection` (`foe: { ... }`)**: come `heroSection`, con prefisso `foe.`.
- **`questSection` (`quest: { ... }`)**: corpo esecutivo principale.

L'ordine è vincolante: le funzioni devono precedere `hero` e `foe` perché queste sezioni possono invocarle durante l'inizializzazione (es. `getModifier()` dentro `hero:`).

### 3.2 Tipi di dato

| Tipo     | Descrizione                                                                | Esempio         |
| -------- | -------------------------------------------------------------------------- | --------------- |
| `Int`    | Intero a 32 bit con segno                                                  | `42`, `-5`      |
| `Float`  | Numero in virgola mobile a 64 bit                                          | `3.14`, `-0.5`  |
| `Bool`   | Valore booleano                                                            | `true`, `false` |
| `String` | Sequenza letterale di testo                                                | `"Spada Corta"` |
| `Void`   | Tipo speciale non assegnabile, usato solo come tipo di ritorno di funzione | —               |
| `HP`     | Punti Ferita, compatibile con `Int`                                        | `24`            |
| `AC`     | Classe Armatura, compatibile con `Int`                                     | `15`            |
| `Gold`   | Monete d'oro, compatibile con `Float`                                      | `50.0`          |

A runtime `HP` e `AC` sono `Integer`, `Gold` è `Double`. La distinzione serve per leggibilità e per i suffissi automatici nelle stringhe interpolate (` HP`, ` AC`, ` gp`).

### 3.3 Dadi e operatori di dominio

I dadi poliedrici (`d20`, `d12`, `d10`, `d8`, `d6`, `d4`, `d3`) sono espressioni atomiche utilizzabili in qualsiasi contesto aritmetico (es. `3 * d6 + 4`).

- **`adv` / `dis`**: lancia due dadi e tiene il massimo (vantaggio) o il minimo (svantaggio). Stampa il dettaglio dei lanci.
- **`save` / `vs`**: operatori infissi che restituiscono `Bool`. `expr1 save expr2` verifica `≥` (tiro salvezza), `expr1 vs expr2` verifica `>` (prova contrapposta).

### 3.4 Dichiarazioni e assegnamenti

Le variabili si dichiarano con tipo esplicito:

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

**Switch-case:** senza fall-through. Il primo `case` corrispondente viene eseguito e lo switch termina. Il `default` è opzionale.

```dnd
switch (txc) {
    case 1:  { print("Fallimento Critico!"); }
    case 20: { print("Successo Critico!"); }
    default: { print("Risultato normale."); }
}
```

**Break:** interrompe il ciclo `while` corrente. Usarlo fuori da un ciclo produce un errore runtime.

### 3.6 Funzioni

```dnd
def Int getModifier(Int score) {
    return (score - 10) / 2;
}
```

Si dichiarano nella `functionSection` con `def`. Il tipo `Void` è ammesso solo come tipo di ritorno. La ricorsione è supportata.

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

**Stringhe interpolate:** il prefisso `i` abilita espressioni arbitrarie tra `${}`. Per `HP`, `AC` e `Gold` il suffisso di tipo viene aggiunto in automatico.

```dnd
print(i"HP Eroe: ${hero.hp} | Oro: ${hero.gold}");
// Output: HP Eroe: 35 HP | Oro: 50.0 gp
```

### 3.8 Limitazioni

- `Void` non è ammesso come tipo di parametro.
- Non si può ridichiarare una funzione con lo stesso nome.
- `break` è valido solo dentro un `while` o `switch`.
- La dot-notation (`hero.hp`, `foe.ac`) è riservata ai namespace delle sezioni `hero`/`foe`.

---

## 4. Semantica

### 4.1 Scelte progettuali

- **Tipizzazione**: il tipo è fissato alla dichiarazione e non cambia. La compatibilità viene verificata a runtime prima di ogni legame in memoria. Non c'è analisi statica separata.

- **Scoping lessicale con shadowing**: ogni blocco `{ ... }` apre un nuovo scope. Una variabile locale può oscurare una omonima esterna; all'uscita dal blocco torna visibile quella esterna.

- **Namespace `hero`/`foe`**: non aprono un nuovo scope. Le variabili vengono registrate nello scope globale con prefisso automatico (`hero.hp`, `foe.ac`), accessibili ovunque.

- **Valutazione eager**: tutte le espressioni vengono valutate immediatamente.

- **Operatori logici non short-circuit**: `&&` e `||` valutano sempre entrambi gli operandi. In un linguaggio stocastico è importante che i dadi vengano sempre lanciati (es. `hero.hp > 0 && d20 > 5` lancia il dado anche se `hero.hp` è 0).

- **Call-by-value**: gli argomenti sono valutati nello scope del chiamante e i parametri formali sono copie locali.

### 4.2 Gerarchia dei tipi e coercizione implicita

L'interprete applica conversioni implicite al momento dell'assegnamento e del passaggio di argomenti.

Le relazioni di compatibilità implementate sono le seguenti:

$$\text{HP} \subsetneq \text{Int} \qquad \text{AC} \subsetneq \text{Int} \qquad \text{Int} \subsetneq \text{Gold}$$

In pratica:

- Un valore `Double` assegnato a una variabile `Int`, `HP` o `AC` viene convertito tramite **troncamento verso zero** (la parte decimale viene scartata: `3.9` diventa `3`, `-3.9` diventa `-3`).
- Un valore `Integer` assegnato a una variabile `Gold` viene **promosso** a `Double`.
- Il tipo `Float` non partecipa ad alcuna catena di promozione implicita da `Int`: un valore intero assegnato a una variabile `Float` produce un errore di tipo incompatibile.
- `Bool` e `String` sono completamente isolati: non esiste alcuna forma di conversione implicita o esplicita tra questi tipi e i tipi numerici.

### 4.3 Gestione degli errori

Approccio **Fail-Fast**: al primo errore l'interprete lancia una `DnDLangError` (con numero di riga), stampata su `stderr`. Il processo termina con exit code 1. Errori intercettati:

- Variabile non dichiarata
- Divisione o modulo per zero
- Arità errata nella chiamata di funzione
- Tipo incompatibile (es. `Bool` assegnato a `Int`)
- Funzione non-`Void` senza `return`, o funzione `Void` che restituisce un valore
- `break` fuori da `while`/`switch`
- Operatori logici (`&&`, `||`, `!`) su operandi non booleani
- Funzione non dichiarata o ridichiarata

### 4.4 Formalizzazione della semantica operazionale

Di seguito le regole di transizione della semantica operazionale strutturata (SOS) di DnDLang. Lo stato è una coppia $(\overline{\sigma}, c)$ dove $\overline{\sigma} = \sigma_1 \cdot \sigma_2 \cdot \ldots \cdot \sigma_n$ è una pila di scope (ogni scope $\sigma$ è una mappa da identificatori a valori) e $c$ è il comando o l'espressione da valutare. La pila è necessaria per modellare lo scoping lessicale: l'elemento più a sinistra è lo scope corrente, gli elementi successivi sono gli scope via via più esterni fino allo scope globale.

La ricerca di una variabile risale la pila fino a trovare un binding: $\text{lookup}(\overline{\sigma}, x)$ restituisce il valore associato a $x$ nel primo scope $\sigma_i$ che lo contiene. L'aggiornamento $\overline{\sigma}[x \mapsto v]$ modifica il binding nello scope in cui $x$ è stato trovato.

**Blocco (apertura e chiusura scope)**

$$
    \text{Block} ~ \frac{
        -
    }{
        (\overline{\sigma},\ \{ c \}) \rightarrow (\varnothing \cdot \overline{\sigma},\ \mathtt{block}(c))
    }
    \quad \quad
    \text{BlockP} ~ \frac{
        (\overline{\sigma},\ c) \rightarrow (\overline{\sigma}',\ c')
    }{
        (\overline{\sigma},\ \mathtt{block}(c)) \rightarrow (\overline{\sigma}',\ \mathtt{block}(c'))
    }
    \quad \quad
    \text{BlockE} ~ \frac{
        -
    }{
        (\sigma \cdot \overline{\sigma},\ \mathtt{block}(\epsilon)) \rightarrow (\overline{\sigma},\ \epsilon)
    }
$$

dove $\varnothing$ è uno scope vuoto. $\text{Block}$ corrisponde a `enterBlock()`, $\text{BlockE}$ a `exitBlock()`.

**Sequenza**

$$
    \text{SeqP} ~ \frac{
        (\overline{\sigma},\ c_1) \rightarrow (\overline{\sigma}',\ c_1')
    }{
        (\overline{\sigma},\ c_1 \,;\, c_2) \rightarrow (\overline{\sigma}',\ c_1' \,;\, c_2)
    }
    \quad \quad
    \text{SeqE} ~ \frac{
        -
    }{
        (\overline{\sigma},\ \epsilon \,;\, c) \rightarrow (\overline{\sigma},\ c)
    }
$$

**Assegnamento semplice**

$$
    \text{Assign} ~ \frac{
        (\overline{\sigma},\ e) \rightarrow v
    }{
        (\overline{\sigma},\ \texttt{id = e;}) \rightarrow \overline{\sigma}[\texttt{id} \mapsto v]
    }
$$

**Assegnamento composto additivo (`+=`)**

$$
    \text{AssignAdd} ~ \frac{
        \text{lookup}(\overline{\sigma},\ \texttt{id}) = v_1
        \quad
        (\overline{\sigma},\ e) \rightarrow v_2
        \quad
        v_3 = v_1 + v_2
    }{
        (\overline{\sigma},\ \texttt{id += e;}) \rightarrow \overline{\sigma}[\texttt{id} \mapsto v_3]
    }
$$

Le regole per `-=`, `*=`, `/=` sono analoghe con l'operazione corrispondente.

**While**

$$
    \text{While-F} ~ \frac{
        (\overline{\sigma},\ \textit{cond}) \rightarrow \texttt{false}
    }{
        (\overline{\sigma},\ \texttt{while(}\textit{cond}\texttt{) \{ body \}}) \rightarrow \overline{\sigma}
    }
$$

$$
    \text{While-T} ~ \frac{
        (\overline{\sigma},\ \textit{cond}) \rightarrow \texttt{true}
        \quad
        (\overline{\sigma},\ \textit{body}) \rightarrow \overline{\sigma}'
        \quad
        (\overline{\sigma}',\ \texttt{while(}\textit{cond}\texttt{) \{ body \}}) \rightarrow \overline{\sigma}''
    }{
        (\overline{\sigma},\ \texttt{while(}\textit{cond}\texttt{) \{ body \}}) \rightarrow \overline{\sigma}''
    }
$$

**Chiamata di funzione (call-by-value)**

$$
    \text{FunCall} ~ \frac{
        (\overline{\sigma},\ a_i) \rightarrow v_i \quad \forall i \in 1..n
        \quad
        \sigma_f = \{p_1 \mapsto v_1, \ldots, p_n \mapsto v_n\}
        \quad
        (\sigma_f \cdot \overline{\sigma},\ \textit{body}) \;\uparrow\; \texttt{return}(v)
    }{
        (\overline{\sigma},\ f(a_1, \ldots, a_n)) \rightarrow v
    }
$$

Gli argomenti $a_i$ vengono valutati nello scope del chiamante. Viene poi creato un nuovo scope $\sigma_f$ con i parametri formali $p_i$ legati ai valori $v_i$, e il corpo viene eseguito nella pila estesa $\sigma_f \cdot \overline{\sigma}$. La notazione $\uparrow\;\texttt{return}(v)$ indica che l'esecuzione del corpo viene interrotta dall'istruzione `return`, che trasporta il valore $v$ al chiamante. Al termine, lo scope $\sigma_f$ viene rimosso dalla pila.

**Tiro salvezza**

$$
    \text{Save-S} ~ \frac{
        (\overline{\sigma},\ e_1) \rightarrow v_1
        \quad
        (\overline{\sigma},\ e_2) \rightarrow v_2
        \quad
        v_1 \ge v_2
    }{
        (\overline{\sigma},\ e_1 \;\texttt{save}\; e_2) \rightarrow \texttt{true}
    }
    \quad \quad
    \text{Save-F} ~ \frac{
        (\overline{\sigma},\ e_1) \rightarrow v_1
        \quad
        (\overline{\sigma},\ e_2) \rightarrow v_2
        \quad
        v_1 < v_2
    }{
        (\overline{\sigma},\ e_1 \;\texttt{save}\; e_2) \rightarrow \texttt{false}
    }
$$

**Lancio con vantaggio**

$$
    \text{Adv} ~ \frac{
        r_1 = \text{random}(1,\ \textit{sides})
        \quad
        r_2 = \text{random}(1,\ \textit{sides})
        \quad
        v = \max(r_1,\ r_2)
    }{
        (\overline{\sigma},\ \texttt{adv}\ \textit{dice}) \rightarrow v
    }
$$



---

## 5. Implementazione

### 5.1 Il Visitor

`DnDInterpreter` estende `DnDLangBaseVisitor<Object>` generata da ANTLR4. I valori circolano come `Object` (`Integer`, `Double`, `Boolean`, `String` o `null`) e vengono distinti con `instanceof` dove necessario.

L'AST viene visitato ricorsivamente in post-ordine: prima i sottoalberi figli, poi il nodo padre usa i risultati. Per esempio, `d20 + strMod` valuta prima `D20Expr` (lancio del dado), poi `IdExpr` (lookup della variabile), infine `AddSubExpr` somma i due valori.

### 5.2 Gestione degli scope

`Environment.java` implementa una pila di scope tramite la classe interna `Scope`:

```java
private static class Scope {
    final Scope enclosing;
    final Map<String, VariableSymbol> variables = new HashMap<>();
    final Map<String, FunctionSymbol>  functions  = new HashMap<>();
}
```

`enterBlock()` crea un nuovo scope collegato al corrente come padre; `exitBlock()` ripristina il padre. La risoluzione degli identificatori risale la catena `enclosing` fino allo scope globale.

Le funzioni vengono registrate nello scope globale al momento della visita della `functionSection`. Le sezioni `hero:` e `foe:` non aprono un nuovo scope: dichiarano le variabili direttamente nello scope globale con prefisso (`hero.hp`, `foe.ac`).

### 5.3 Difficoltà tecniche e soluzioni adottate

#### A. Implementazione `return` 

L'interprete esplora l'AST attraverso chiamate Java ricorsive: una funzione DnDLang genera una catena del tipo `visitFunctionCallExpr → visitBlock → visitIfStmt → visitBlock → visitReturnStmt`. Quando l'esecuzione incontra un `return`, deve interrompere *tutti* questi livelli in un colpo solo e riportare il valore al punto di chiamata originale. Un normale `return` Java non basta: uscirebbe solo dal metodo corrente, e il blocco superiore continuerebbe ad eseguire le istruzioni successive come se niente fosse.

La soluzione è usare un'eccezione come salto non-locale. `visitReturnStmt` valuta l'espressione e lancia una `ReturnException` che trasporta il valore di ritorno. L'eccezione risale lo stack senza essere catturata da nessun metodo intermedio, fino ad arrivare a `visitFunctionCallExpr`, dove un blocco `try-catch` la intercetta, ne estrae il valore e verifica che sia compatibile con il tipo di ritorno dichiarato. La `ReturnException` è costruita con `super(null, null, false, false)` per evitare la generazione dello stack trace Java, dato che non si tratta di un errore ma di un meccanismo di controllo del flusso. Il blocco `finally` si occupa della pulizia: chiude lo scope locale con `env.exitBlock()` e ripristina il `currentReturnType` del chiamante, garantendo che queste operazioni avvengano sempre, anche in caso di eccezioni impreviste. Questo è anche ciò che rende corretta la ricorsione: ogni invocazione di `visitFunctionCallExpr` salva e ripristina il proprio contesto in modo indipendente.

#### B. Implementazione `break`

Il `break` ha un problema simile ma meno profondo: deve uscire solo dal ciclo o dallo switch che lo contiene, non dall'intera funzione. Per questo basta un approccio più semplice: un flag booleano `isBreaking`. Quando l'interprete incontra un `breakStmt`, alza il flag; `visitBlock` lo controlla dopo ogni istruzione e smette di iterare se è attivo; infine `visitWhileStmt` o `visitSwitchStmt` lo consumano (riportandolo a `false`) impedendo che si propaghi oltre. Un `break` usato fuori contesto (dentro `hero:` o `foe:`) viene intercettato da `visitDeclarativeBlock`, che lancia un errore.

#### C. Parsing dinamico delle stringhe interpolate

Le stringhe `i"..."` possono contenere espressioni arbitrarie dentro `${...}`. Per valutarle, `visitIStringExpr` estrae ogni frammento con un'espressione regolare e per ciascuno crea al volo un'intera pipeline ANTLR (lexer, token stream, parser), isola il nodo `expr` risultante e lo valuta con `this.visit()`, che opera sull'`Environment` corrente e quindi vede tutte le variabili in scope. Se il risultato corrisponde a una variabile di tipo `HP`, `AC` o `Gold`, il suffisso appropriato viene aggiunto in automatico.

---

## 6. Programmi di test e output attesi

### `hello.dnd` – Tipi di dominio e interpolazione

Programma minimale che verifica la dichiarazione di variabili con tipi di dominio (`HP`, `AC`, `Gold`) e l'aggiunta automatica dei suffissi nelle stringhe interpolate.

```dnd
hero: {
    String name = "Aelar";
    String class = "Monk";
    HP hp = 24;
    AC ac = 15;
    Gold gold = 50.0;
}

quest: {
    print(i"Eroe: ${hero.name} | Class: ${hero.class} | HP: ${hero.hp} | AC: ${hero.ac} | Gold: ${hero.gold}");
}
```
> **Output** <br>
> Eroe: Aelar | Class: Monk | HP: 24 HP | AC: 15 AC | Gold: 50.0 gp

### `errors.dnd` – Fail-fast su errore runtime

Verifica che l'interprete termini immediatamente alla prima divisione per zero, senza eseguire le istruzioni successive.

```dnd
hero: {
    HP hp = 20;
}

quest: {
    print("--- Test Fallimento Critico (Fail-Fast) ---");
    Int x = 10;
    Int y = 0;
    print("Tentativo di divisione per zero...");
    Int z = x / y;
    print("Se vedi questa scritta, il Fail-Fast NON sta funzionando!");
}
```
> **Output** <br>
> --- Test Fallimento Critico (Fail-Fast) --- <br>
> Tentativo di divisione per zero... <br>
> Errore runtime non gestito alla riga 10: divisione per zero

Il programma termina con exit code 1. L'ultima `print` non viene mai raggiunta.

### `session.dnd` – Sessione di gioco completa

_(codice completo in `programs/session.dnd`)_

Simulazione di una sessione di avventura strutturata che esercita la maggior parte delle funzionalità del linguaggio in un contesto realistico. Il programma copre:

- **Funzioni con tipo di ritorno e ricorsione**: `getModifier`, `calcDamage` e `furiaBerserker` (catena ricorsiva con terminazione garantita dalla decrescita del bonus)
- **Tiro salvezza** (`save`) su trappola ambientale
- **Combattimento con `while`/`switch`/`if-else`**: colpo critico (dado 20), fallimento critico (dado 1), caso default
- **Vantaggio** (`adv d20`): il nemico attacca con vantaggio
- **Post-decremento e assegnamento composto**: gestione delle pozioni (`hero.pozioni--`, `hero.hp += cura`)
- **Operatore ternario**: determinazione dell'esito finale
- **Interpolazione con suffissi**: stampa automatica di HP, AC, gp

Di seguito le parti più significative. La trappola con tiro salvezza:

```dnd
print("Il pavimento cede sotto i tuoi piedi: tiro salvezza su Destrezza!");
Bool schivaTrappola = (d20 + hero.dexMod) save 13;

if (!schivaTrappola) {
    Int dannoTrappola = d6 + 2;
    hero.hp -= dannoTrappola;
    print(i"Non schivi in tempo: subisci ${dannoTrappola} danni. HP: ${hero.hp}");
} else {
    print("Salti via in tempo, illeso!");
}
```

La gestione delle pozioni con post-decremento:

```dnd
if (hero.hp < 22 && hero.pozioni > 0) {
    hero.pozioni--;
    Int cura = d10 + 4;
    hero.hp += cura;
    print(i"${hero.name} beve una pozione: recupera ${cura} HP. HP: ${hero.hp} (pozioni rimaste: ${hero.pozioni})");
}
```

Esempio di tracciato (i valori dei dadi variano ad ogni esecuzione):

> **Output** <br>
> === Tordek entra nella tana dell'Orco Sciamano === <br>
> Il pavimento cede sotto i tuoi piedi: tiro salvezza su Destrezza! <br>
> &ensp;\> [ Tiro Salvezza ] Totale: 9 vs CD: 13 -> FALLIMENTO <br>
> Non schivi in tempo: subisci 6 danni. HP: 39 HP <br>
> <br>
> === INIZIA IL COMBATTIMENTO contro Orco Sciamano (HP 35 HP) === <br>
> <br>
> --- Round 1 --- <br>
> Tordek attacca (TxC 24 vs AC 13 AC) <br>
> Colpito! 11 danni. HP Orco Sciamano: 24 HP <br>
> &ensp;\> [Vantaggio] Lanciati 14 e 8 -> Tengo 14 <br>
> Orco Sciamano ti colpisce con la sua ascia: 5 danni. HP: 34 HP <br>
> ... <br>
> === FINE COMBATTIMENTO === <br>
> Esito: VITTORIA dopo 4 round. <br>
> Sul corpo dell'orco trovi 75.5 gp. Oro totale: 155.5 gp

### `showcase.dnd` – Vetrina delle funzionalità

_(codice completo in `programs/showcase.dnd`)_

Programma dimostrativo che riunisce tutte le funzionalità del linguaggio in un unico file. Copre, in ordine di apparizione:

- **Funzioni** con return e ricorsione (`getModifier`, `calcDamage`, `furiaBerserker`)
- **Sezioni `hero:` e `foe:`** con chiamata a funzione nella dichiarazione (`getModifier(strength)`)
- **Precedenza degli operatori**: `2 + 3 * 4` restituisce 14
- **Operatori logici e dadi**: `adv d20`, `save`
- **Zucchero sintattico**: `++`, `+=`
- **Scoping con shadowing**: variabile `x` ridichiarata in blocco interno
- **Combattimento**: ciclo `while` con `switch-case-default`, `if-else`, operatore ternario

Di seguito la sezione sullo scoping e lo shadowing:

```dnd
Int x = 10;
{
    Int x = 99; // shadow
    print(i"x interno: ${x}");
}
print(i"x esterno: ${x}");
```

E la parte iniziale della quest che verifica precedenza, logica e dadi:

```dnd
quest: {
    print(i"--- ${hero.name} VS ${foe.name} ---");
    print(i"HP: ${hero.hp}/${foe.hp} | AC: ${hero.ac}/${foe.ac} | Gold: ${hero.gold}");

    Int precedenza = 2 + 3 * 4;
    print(i"Precedenza (2 + 3 * 4): ${precedenza}");

    Bool entrambiVivi = (hero.hp > 0) && (foe.hp > 0);
    print(i"Entrambi vivi: ${entrambiVivi}");

    Int tiroVantaggio = adv d20;
    print(i"Tiro con vantaggio: ${tiroVantaggio}");

    Bool resisteVeleno = (d20 + hero.strMod) save 14;
    print(i"Resiste al veleno? ${resisteVeleno}");

    Int contatore = 0;
    contatore++;
    ++contatore;
    print(i"Contatore dopo ++ ++: ${contatore}");

    hero.gold += 50;
    print(i"Oro dopo += 50: ${hero.gold}");
    // ...
}
```

Esempio di output parziale (valori dei dadi variabili):

> **Output** <br>
> --- Tordek il Barbaro VS Golem di Carne --- <br>
> HP: 50 HP/80 HP | AC: 16 AC/14 AC | Gold: 120.0 gp <br>
> Precedenza (2 + 3 * 4): 14 <br>
> Entrambi vivi: true <br>
> &ensp;\> [Vantaggio] Lanciati 12 e 17 -> Tengo 17 <br>
> Tiro con vantaggio: 17 <br>
> &ensp;\> [ Tiro Salvezza ] Totale: 22 vs CD: 14 -> SUCCESSO <br>
> Resiste al veleno? true <br>
> Contatore dopo ++ ++: 2 <br>
> Oro dopo += 50: 170.0 gp <br>
> x interno: 99 <br>
> x esterno: 10 <br>
> ... <br>
> Esito: VITTORIA al round 3!

