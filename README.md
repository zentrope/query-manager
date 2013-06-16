# Queryizer

Web app for submitting, monitoring and viewing the results of
long-running queries.

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

Copyright &copy; 2013 Ben Glasser

Distributed under the Eclipse Public License, the same as Clojure.
