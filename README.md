# signal-game-cljs

A proof-of-concept for a game that involves communication delay.

## Development

To get an interactive development environment run:

    lein fig:build

This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL.

To clean all compiled files:

	lein clean

To create a production build run:

	lein clean
	lein fig:min

## To Play

Start an interactive REPL as above then press the arrow keys
to emit signals.

## License

Copyright © 2020 Mitch Spradlin

