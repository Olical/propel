# Propel [![Clojars Project](https://img.shields.io/clojars/v/olical/propel.svg)](https://clojars.org/olical/propel) [![CircleCI](https://circleci.com/gh/Olical/propel.svg?style=svg)](https://circleci.com/gh/Olical/propel) [![codecov](https://codecov.io/gh/Olical/propel/branch/master/graph/badge.svg)](https://codecov.io/gh/Olical/propel)

Propel helps you start [Clojure][] and [ClojureScript][] REPLs with a prepl.

It consists of a simple command line interface (`propel.main`) as well as a couple of utility functions (`propel.core`) that you can execute from your own `*.main` namespace.

```bash
$ clj --main propel.main --write-port-file
[Propel] Started a :jvm prepl at 127.0.0.1:33979 (written to ".prepl-port")
user=>

# ---

$ clj --main propel.main --help
  -p, --port PORT             Port number, defaults to random open port
  -a, --address ADDRESS       Address for the server, defaults to 127.0.0.1
  -f, --port-file-name FILE   File to write the port to, defaults to .prepl-port
  -w, --[no-]write-port-file  Write the port file? Use of --port-file-name implies true, defaults to false
  -e, --env ENV               What REPL to start ([jvm], node, browser, figwheel, lein-figwheel, rhino, graaljs or nashorn)
  -r, --repl-only             Don't start a prepl, only start a local REPL and connect it to the prepl at the specified port and address.
  -b, --figwheel-build BUILD  Build to use when using the figwheel env (not lein-fighweel), defaults to propel
  -x, --extra EDN             Extra options map you want merged in, you can get creative with this one
  -h, --help                  Print this help
```

## Installation

You can install Propel from [Clojars][], just add the appropriate coordinate to your project.

```edn
;; deps.edn
olical/propel {:mvn/version "1.3.0"}

;; project.clj
[olical/propel "1.3.0"]
```

## Usage

### CLI

When you execute `clj -m propel.main` it'll default to starting a JVM REPL with a socket prepl attached on a random port.

As you can see from the help output above we can provide various `env` values, if you provide `node` you get a node based ClojureScript REPL as well as a socket prepl attached to that same REPL environment.

```bash
$ clj -m propel.main --env node
[Propel] Started a :node prepl at 127.0.0.1:45297
cljs.user=> (defn hello [n] (str "Hello, " n "!"))
#'cljs.user/hello
```

We can also REPL into any existing prepl by specifying a `--port` (`-p`) and `--repl-only` (`-r`).

```bash
$ clj -m propel.main -rp 45297
cljs.user=> (hello "Olical")
"Hello Olical!"
```

You can specify a port or Propel can write the random port to a file, it will use `.prepl-port` by default.

```bash
# Write to the default file.
$ clj -m propel.main -w

# Select a file name, also enables writing the file.
$ clj -m propel.main -e node --port-file-name .node-prepl-port
```

You can specify an extra EDN map to be merged into the internal configuration map (see `propel.core`), this might come in handy if there's some value you want to directly pass to the prepl. Good for hacking around my assumptions.

```bash
# Will enable port file writing without using the CLI.
# -w enables this flag for you.
$ clj -m propel.main --extra '{:port-file? true}'
```

### Functions

`propel.core` exposes two functions:

 * `(propel.core/start-prepl! opts)`
 * `(propel.core/repl opts)`

The command line maps near enough directly to the `opts` argument of `start-prepl!`, you can give it an empty map and all of the defaults will be applied, just like the CLI. To start a node prepl via Clojure instead of the CLI just provide an `:env`.

```clojure
(propel.core/start-prepl! {:env :node, :port-file? true})
(propel.core/start-prepl! {:port 5555})
```

It will return the `opts` map enriched with all of the defaults, selected port, port file path, accept function symbol and more. It's up to you to decide what you want to do with that data.

You can then pass the result from `start-prepl!` through to `repl` which will start a standard REPL attached to the prepl you just created. This is what `propel.main` does for you, starts a prepl then a REPL using the returned data.

Here's a fairly exhaustive example of the options map, `start-prepl!` and `repl` usage.

```clojure
;; Start a node prepl and REPL into it.
(-> {:env :node
     :port 5555
     :port-file-name ".node-prepl-port"}
    (propel.core/start-prepl!)
    (propel.core/repl))

;; Open a REPL into an exisisting prepl.
(propel.core/repl {:port 8787})
```

### Figwheel

#### Lein (legacy)

When using the [lein-figwheel][] plugin, you may execute something like the following to have figwheel start up from your `project.clj` configuration. Everything should be inferred and should start up as if you typed `lein figwheel` but with a prepl connected.

```bash
$ clj -m propel.main -e lein-figwheel
```

#### Main

You can start a [figwheel][] prepl and REPL just like any other environment through the functions or CLI. The only thing you have to bear in mind is that figwheel-main requires you to create a configuration file, I highly recommend you have a read through the documentation.

The name of the file should be the name of the build suffixed with `.cljs.edn`, so by default Propel will configure figwheel to look for `propel.cljs.edn`, which could look like this.

```clojure
^{:watch-dirs ["cljs-src" "dev"]}
{:main example.core}
```

Now you can start a figwheel REPL through the CLI or function, let's create one though the CLI but specify a different build name.

```bash
$ clj -m propel.main -e figwheel --figwheel-build dev
```

Figwheel will now start up and read it's configuration from `dev.cljs.edn`, it should open a browser tab for you that you can interact with through your REPL or prepl.

If you need to, you can provide different `:figwheel-opts` (which default to `{:mode :serve}`) through the `--extra '{...}'` EDN map argument at the CLI.

## Propel + Conjure

My [Neovim][] Clojure(Script) plugin, [Conjure][], relies entirely on prepl. Connecting it to a REPL started by Propel is a breeze and the main reason for Propel even existing.

With the following `.conjure.edn` file all we need to do is start Propel with a single argument and then open Neovim.

```clojure
{:conns {:propel {:port #slurp-edn ".prepl-port"}}}
```

Now we start Propel in one terminal.

```bash
$ clj -m propel.main -w
```

And open a Clojure file in Neovim in another.

```bash
$ nvim src/foo/bar.clj
```

After a couple of seconds Conjure will be connected to your Propel REPL and you'll have evaluation, autocompletion, documentation lookup and go to definition.

This applies to all of the other `env` values mentioned above, feel free to connect it to `node`, `browser` or `figwheel` too! Just be sure to specify `:lang :cljs` in your `.conjure.edn` next to the `:port` if you want a connection to work with ClojureScript as opposed to Clojure (the default).

## What's a prepl?

The name comes from **p**rogrammable REPL and it's built into Clojure and ClojureScript 1.10+.

It's a REPL that sits behind a socket server, you send code to it and it'll respond with the results, just like a regular REPL. The difference is that it's over a socket (not stdio) and the responses are wrapped in EDN data structures for easy parsing, this is perfect for tool authors.

The more popular community driven equivalent of this concept is the wonderful [nREPL][], almost all current tooling relies on this system.

You can read more about starting prepl servers in my [Clojure socket prepl cookbook][cookbook-post] post. Another way to learn is to use this project for the basics then dig into the source code yourself to find what you need.

## Can you add _X_?

I don't want Propel to be infinitely customisable otherwise I'll lose all of my time to fixing the bugs introduced by those features. It should only contain things 95% of people should find useful early on in a project.

Copying and modification of the source code to fit your needs is encouraged, as opposed to adding every possible flag and feature to Propel. Think of this like a set of minimal examples that are also executable.

Use Propel to get going where it suits your needs, once you outgrow it you'll want to have your own namespaces anyway.

Issues and pull requests are still appreciated, ideally after discussing what you want to do, but only for small quality of life changes. If what you want will significantly change the project or introduce further complexity you might be better off with a fork.

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
[clojars]: https://clojars.org/
[figwheel]: https://github.com/bhauman/lein-figwheel
[lein-figwheel]: https://github.com/bhauman/lein-figwheel
