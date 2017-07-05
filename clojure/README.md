# i-ching


## To run the server

lein deps

lein ring server

Currently, hexagram and trigram data are loaded from JSON files in the resources directory. This will be changing to a Neo4j database.

---
## To view an I Ching reading

In your browser, navigate to http://localhost:3000 (which redirects to http://localhost:3000/consult)

---

## To run tests

lein expectations


## To run tests automatically while working on them

lein autoexpect

