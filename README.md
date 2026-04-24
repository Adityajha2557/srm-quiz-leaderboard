# Quiz Leaderboard System

Internship assignment solution for the SRM Quiz Leaderboard problem.

## What this does

Polls the quiz API 10 times (poll 0 to 9), deduplicates events using `roundId + participant` as a unique key, aggregates scores per participant, and submits the final leaderboard.

## Setup

- Java 11 or above (uses java.net.http.HttpClient which was added in Java 11)
- No external libraries needed — JSON parsing is done manually with basic string operations

## How to run

**Step 1:** Open `QuizSolver.java` and change the `REG_NO` field at the top to your actual registration number.

```java
static String REG_NO = "RA2211003011XXX"; // change this
```

**Step 2:** Compile the file

```bash
cd src/main/java
javac QuizSolver.java
```

**Step 3:** Run it

```bash
java QuizSolver
```

The program will take around **50+ seconds** to complete because of the mandatory 5-second delay between polls.

## Key design decisions

### Deduplication
The API can return the same event data in multiple polls. To handle this, every event is identified by the combination of `roundId` and `participant`. If the same combo appears again in a later poll, it's skipped.

Example:
```
Poll 0: { roundId: "R1", participant: "Alice", score: 10 }  -> Added
Poll 3: { roundId: "R1", participant: "Alice", score: 10 }  -> Skipped (duplicate)
Alice's total = 10, not 20
```

### JSON parsing
I manually parse the JSON response using basic string operations instead of pulling in a library like Gson or Jackson. It's a bit rough but works fine for this specific response structure.

### Leaderboard sorting
Sorted by `totalScore` descending (highest score first).

## Expected output structure

```
Starting to poll the API...
Poll 0 -> Status: 200
Response: { ... }
  Added score for Alice from round R1: +10
  Added score for Bob from round R1: +20
Waiting 5 seconds before next poll...
...
Poll 3 -> Status: 200
  Duplicate found, skipping: R1||Alice
...

=== Final Leaderboard ===
Bob -> 120
Alice -> 100
Grand Total: 220

Submitting leaderboard...
Submit Response: { "isCorrect": true, ... }
```

## Things to note

- The program only submits once at the end, after all 10 polls are done
- If you get a wrong answer, check that your `REG_NO` is correct
- The API is idempotent so re-submitting gives the same result

## Project structure

```
QuizLeaderboard/
└── src/
    └── main/
        └── java/
            └── QuizSolver.java
```
