# Foosball scoreboard demo

[![Build Status](https://travis-ci.org/IQ-Inc/foosball-scoreboard-demo.svg?branch=master)](https://travis-ci.org/IQ-Inc/foosball-scoreboard-demo)

A foosball scoreboard, written in Clojure and ClojureScript. Using an Arduino,
some IR sensors, and an old server, we instrumented a foosball table to keep
score for us. Future goals may include image processing and ball tracking, game
analytics, and a Slack integration.

![Scoreboard demo](./docs/img/scoreboard-demo.png)

## Requirements

- Java 1.8+
- A serial device (ex. Arduino)
- Tested on Chrome, Firefox, and Safari

## Getting started

Download the latest release from GitHub. From the command line, run

```bash
$ # Connect directly to a serial device (specify device)
$ java -jar foosball-scoreboard.jar serial /dev/[your-serial-device]
$ # Connect to a TCP device that mimics the serial device (specify port)
$ java -jar foosball-scoreboard.jar tcp 3667
```

The program starts a webserver and app accessible at
`localhost:3000`. The server monitors the connection for foosball events
and pushes updates to connected clients.

## Keyboard controls

| Key                   |    Event                       |
| --------------------- | ------------------------------ |
|  `SPACE`              | Start a new game               |
|  `b`                  | Swap black team members        |
|  `g`                  | Swap gold team members         |
|  `m`                  | Toggle game modes              |
|  `j/k`                | Decrease/increase mode value   |

## Supported game modes

Use `m` on the keyboard to cycle through game modes:

- First to a maximum score
  - `j/k` decrease / increase max score
- First to a maximum score, win by two points
  - `j/k` decrease / increase max score
- Timed mode, time counts down
  - `j/k` decrease / increase play time
- Timed mode (overtime): sudden-death, next-point-wins mode
  - `j/k` decrease / increase play time

## Events

The server will handle any message that is suffixed with a newline (char
`10`). The server expects the following serial events to correspond to specific
foosball events:

| ASCII `char`          |    Foosball Event    |
| --------------------- | -------------------- |
|  `"BD"`               | Black ball drop      |
|  `"GD"`               | Gold ball drop       |
|  `"BG"`               | Black scores a goal  |
|  `"GG"`               | Gold scores a goal   |

Consider using the `serial-msg!` function from the `repl` namespace to simulate
serial events during testing. If you're testing a TCP event server, consider using
`netcat` to publish the same messages. The codes are configurable in the
`foosball-score.events` namespace.

A six character string is expected to correspond with a player's ID badge.

## Developing

- Clojure 1.8+
- Leiningen, min 2.5.0

Install both, clone the project, and you're ready to go. The frontend may be
interactively developed (port 3449) using figwheel:

```bash
lein figwheel
```

To test the backend and websockets, the server will need to be run
independently (port 3000):

```bash
lein run # Can also use 'lein repl', and start the server with (start-server)
```

A few useful things:

- `lein test` runs the unit tests
- From the REPL, `(use 'foosball-score.systests)` will import a few end-to-end
  tests that are helpful for repeated testing. See the system tests defined in
  `env/dev/clj/foosball-score/systests.clj`.
