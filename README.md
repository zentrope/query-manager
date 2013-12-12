# Query Manager

Web app for submitting, monitoring and viewing the results of
long-running RDBMS queries.

## Rationale

This is mostly a web-app I can use at work. We sell "Enterprise"
software to customers who (as a result) have large databases. From
time to time, we need to run custom, domain-specific queries to
diagnose potential problems. Thing is, those queries can take a _long
time_ to run. Much longer than a typical browser's request timeout.

Wouldn't it be fun to write a small, uberjar app we could hand to
customers so that they could run diagnostic queries, maybe a bunch of
them, track whether or not they were still running (in the background,
so to speak) and then view or send us the results?

Yes!

[Ben Glasser](https://github.com/BenGlasser) came to intern in our
little group to get a taste of the QA world and because we didn't have
much legitimate QA work for him to do, he started this app. He's moved
on to much greater endeavors, so I've finished it. (And rewrote
it. And rewrote it again.)

## Aims

Some simple goals:

 - An app on the small end of medium-level complexity so that I can
   use it to "practice" web-app development and try new things.

 - No proprietary business information so this can be open source.

 - A web-app, sure, but not meant to be an authenticated, multi-user
   app. Just run it, enter in DB connection parameters, run your
   queries, and you're done. That kind of thing.

 - Figure out the simplest way to implement the background-running of
   queries and the simplest way to present them via a single-page web
   app.

That's about it!

## Status

Works. Uses a single REST route as a long-polling style messaging kind
of thing. An event on the server ends up in a queue on the client, and
the other way around.

## Future

 - When entering DB info, pre-populate the right DB port if it hasn't
   already been edited by the user. And do it in such a way that the
   client doesn't have a lot of if/then/else crap.

 - Fix the "state" module to support working at the REPL, perhaps by
   being just a bunch of functions working on a state object held by
   the main name space.

 - ~Persist "state" to disk so that it can be resumed on start up~,
   and also so that it can be zipped up an exported.

 - Some kind of "plugin" thing that allows the app to load a class as
   a means of snarfing connection credentials from proprietary
   systems.

## License

Copyright &copy; 2013 Keith Irwin, Ben Glasser,

Distributed under the Eclipse Public License, the same as Clojure.

[cs]: https://github.com/clojure/clojurescript
[ca]: https://github.com/clojure/core.async
