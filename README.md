# Query Manager

Web app for submitting, monitoring and viewing the results of
long-running queries.

## Rationale

This is mostly a web-app I can use at work. We sell software to
customers who (as a result) have large databases. From time to time,
we need to run custom, domain-specific queries to diagnose potential
problems. Thing is, those queries can take a _long time_ to run. Much
longer than a typical browser request timeout.

So, I thought it would be fun to write a small, uberjar app we could
hand to customer so that they could run those diagnostic queries,
maybe a bunch of them, track whether or not they were still running
(in the background, so to speak) and then view or send us the results.

[Ben Glasser](https://github.com/BenGlasser) came to intern in our
little group and because we didn't have much legitimate QA work for
him to do, he started this app. He's moved on, so now I'm going to
finish it.

## Aims

Some simple goals:

 - No proprietary business information so this can be open source.

 - A web-app, sure, but not meant to be an authenticated, multi-user
   app. Just run it, enter in DB connection parameters, run your
   queries, and you're done. That kind of thing.

 - Figure out the simplest way to implement the background-running of
   queries and the simplest way to present them via a single-page web
   app.

That's about it!

## Status

In development.

## Thoughts

 - Client should supply connection params rather than have them stored
   on the server.

 - Each "job" should consist of the query to be run and the connection
   params (if stored at the client).

 - Client should be able to upload and download the
   connection-params + list-of-queries (kinda like saving a document
   in a typical productivity app), but also we should store that whole
   thing on the server, too, let's say.

## License

Copyright &copy; 2013 Ben Glasser, Keith Irwin

Distributed under the Eclipse Public License, the same as Clojure.
