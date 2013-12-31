# TODO

Just random stuff I may or may not want to do, or that may or may not
be a good idea.

## Client

 - [ ] improve query parsing error messages (if present)
 - [ ] some way to display error messages?
 - [ ] modal screen thing for when server is down
 - [ ] when server returns, refresh?

## Server

 - [ ] all state should be in an "instance" var in main
 - [ ] scrub database password from archive database

## Done

 - [x] property for app title in embedded mode
 - [x] make query manager embeddable for Java apps
 - [x] separate event logic from event delagation
 - [x] fix web.clj go-blocks to be simpler
 - [x] port jobs to be core.async
 - [x] move jobs to state
 - [x] fix lousy CSS around query selection (that jumpy thing)
 - [x] when importing queries, send them to the server all at once
 - [x] move repo to state
 - [x] merge with master
 - [x] persist jobs to local disk cache
 - [x] persist queries to local disk cache
 - [x] restore "export" for queries. oops!
 - [x] provide facility to zip up cache for transport
 - [x] allow export of zip file
