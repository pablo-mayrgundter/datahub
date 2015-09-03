[DataHub](http://code.google.com/p/datahub) is a [Google AppEngine](https://developers.google.com/appengine/) library that provides a filesystem view of the GAE [datastore](https://developers.google.com/appengine/docs/java/datastore/) via a [RESTful web service](http://en.wikipedia.org/wiki/Representational_state_transfer).

Data payloads currently must be in [JSON](http://www.json.org/) format, but this restriction will be removed and generic binary data will be supported instead.  JSON and [XML](http://en.wikipedia.org/wiki/XML) support will be handled at a higher layer.

Data storage uses the App Engine low-level datastore API via its own abstract Store API that can be implemented with any [CRUD](http://en.wikipedia.org/wiki/Create,_read,_update_and_delete) persistence layers, such as a SQL database.

DataHub is being designed as the new low-level framework for [DataWiki](http://code.google.com/p/datawiki)

## Developers ##

See the GettingStartedGuide, which include an example you can run locally and test with the commands below.

## REST/CRUD + Search API Example ##

The following examples use `curl` on the UNIX command line.

">" means your command prompt, so don't type it in.  Just type in "curl ..."

"<" prefixes the expected response.

### Create ###

(the "-d '{}'" sends an empty json object as an HTTP POST body):

```
  > curl -d '{}' http://localhost:8080/data
  < HTTP/1.1 200 OK
  < Location: /data/__1__
```

### Named create ###

```
  > curl -X PUT -d '{}' http://localhost:8080/data/Test
  < HTTP/1.1 200 OK
```

### Retrieve ###

```
  > curl http://localhost:8080/data/__1__
  < {
  < "updater": "Anonymous at 0:0:0:0:0:0:0:1%0",
  < "author": "Anonymous at 0:0:0:0:0:0:0:1%0",
  < "updated": "1353281454811",
  < "created": "1353281454811",
  < }
```

### Search ###

```
  > curl http://localhost:8080/data?q='foo:bar'
  < ... the same as above ...
```

### Update ###

(using the above as 1.json after edits)

```
  >  curl -X PUT -d @1.json http://localhost:8080/data/__1__
```

### Delete ###

```
  >  curl -X DELETE http://localhost:8080/data/__1__
```