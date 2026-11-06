# Kevin-Toms-Football-Manager
A Java rewrite of the BASIC version posted by https://github.com/philspil66

## Running

This is a Spring Boot application (Java 25). The interactive console game starts
via a `CommandLineRunner` once the context is ready:

```
mvn spring-boot:run
```

or build and run the executable jar:

```
mvn package
java -jar target/ktfm-0.0.1.jar
```

Set `ktfm.console.enabled=false` to start the context without launching the game.

## Domain model

`FootballManager` uses the typed domain objects in `org.sclerck.ktfm.model` for
players, teams, managers, matches, attributes, finances, and league standings.
Teams hold squads of up to 16 players. A normal `Match` requires both teams to
field 11 players; the game uses simulated results for its opposition. League
results award three points for a win and one for a draw, with standings ordered
by points, goal difference, then goals scored.
