# Query Manager

Web app for submitting, monitoring and viewing the results of
long-running RDBMS queries.

## Rationale

This is mostly a web-app I can use at work. We sell software to
customers who (as a result) have large databases. From time to time,
we need to run custom, domain-specific queries to diagnose potential
problems. Thing is, those queries can take a _long time_ to run. Much
longer than a typical browser request timeout.

I thought it would be fun to write a small, uberjar app we could hand
to customer so that they could run those diagnostic queries, maybe a
bunch of them, track whether or not they were still running (in the
background, so to speak) and then view or send us the results.

[Ben Glasser](https://github.com/BenGlasser) came to intern in our
little group and because we didn't have much legitimate QA work for
him to do, he started this app. He's moved on, so now I'm going to
finish it.

## Aims

Some simple goals:

 - An app on the small end of medium level complexity so that I can
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

The client ([ClojureScript][cs]) is moving to [core.async][ca]. Once
that's done, I'll probably refactor it to get rid of the idea that
each area of the screen is a separate "concern".

## Future

 - When entering DB info, pre-populate the right DB port if it hasn't
   already been edited by the user. And do it in such a way that the
   client doesn't have a lot of if/then/else crap.

 - Oh, come on, now. Store the damned credentials on the file system
   so you don't have to re-enter them every time you restart the
   server-side of the app. Sheesh!

 - Right now, the app is a ClojureScript client talking to a fully
   REST style back-end web service API. I'd prefer to remove most of
   the REST API and make the back-end web-service implement queue
   semantics.

 - Resist web-sockets for as long as possible. I love the idea, but I
   want to learn the other stuff, too.

## License

Copyright &copy; 2013 Ben Glasser, Keith Irwin

Distributed under the Eclipse Public License, the same as Clojure.

[cs]: https://github.com/clojure/clojurescript
[ca]: https://github.com/clojure/core.async
