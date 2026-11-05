# Kevin-Toms-Football-Manager
A Java rewrite of the BASIC version posted by https://github.com/philspil66

## Domain model

`FootballManager` uses the typed domain objects in `org.sclerck.ktfm.model` for
players, teams, managers, matches, attributes, finances, and league standings.
Teams hold squads of up to 16 players. A normal `Match` requires both teams to
field 11 players; the game uses simulated results for its opposition. League
results award three points for a win and one for a draw, with standings ordered
by points, goal difference, then goals scored.
