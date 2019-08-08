> Work in progress!

# Propel [![CircleCI](https://circleci.com/gh/Olical/propel.svg?style=svg)](https://circleci.com/gh/Olical/propel) [![codecov](https://codecov.io/gh/Olical/propel/branch/master/graph/badge.svg)](https://codecov.io/gh/Olical/propel)

Starting a prepl server in [Clojure][] (or [ClojureScript][]) may not be the hardest thing in the world, but it's always nice to have some simple defaults within easy reach.

Propel helps you start prepl servers in a few configurations. It won't suit everyone in every situation, but it's a good starting point for most.

Copying and modification of the source code to fit your needs is encouraged, as opposed to adding every possible flag and feature to Propel. Think of this like a set of minimal examples that are also executable.

## Prepl?

The name comes from *p*rogrammable REPL and it's built Clojure and ClojureScript 1.10+.

It's a REPL that sits behind a socket server, you send code to it and it'll respond with the results, just like a regular REPL. The difference is that it's over a socket (not stdio) and the responses are wrapped in EDN data structures for easy parsing, this is perfect for tool authors.

My [Neovim][] Clojure(Script) plugin, [Conjure][], relies entirely on prepl. The more popular community driven equivalent is the wonderful [nREPL][], almost all current tooling relies on this system.

You can read more about starting prepl servers in my [Clojure socket prepl cookbook][cookbook-post] post. Another way to learn is to use this library for the basics then dig into the source code yourself to find what you need.

## Unlicenced

Find the full [unlicense][] in the `UNLICENSE` file, but here's a snippet.

>This is free and unencumbered software released into the public domain.
>
>Anyone is free to copy, modify, publish, use, compile, sell, or distribute this software, either in source code form or as a compiled binary, for any purpose, commercial or non-commercial, and by any means.

[unlicense]: http://unlicense.org/
[clojure]: https://clojure.org/
[clojurescript]: https://clojurescript.org/
[cookbook-post]: https://oli.me.uk/2019-03-22-clojure-socket-prepl-cookbook/
[nrepl]: https://nrepl.org/
[conjure]: https://github.com/Olical/conjure
[neovim]: https://neovim.io/
