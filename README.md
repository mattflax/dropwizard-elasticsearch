Dropwizard Elasticsearch 6
==========================

[![Build Status](https://travis-ci.org/mattflax/dropwizard-elasticsearch6.svg?branch=master)](https://travis-ci.org/mattflax/dropwizard-elasticsearch6)
<!--
[![Coverage Status](https://img.shields.io/coveralls/dropwizard/dropwizard-elasticsearch.svg)](https://coveralls.io/r/dropwizard/dropwizard-elasticsearch)
[![Maven Central](https://img.shields.io/maven-central/v/io.dropwizard.modules/dropwizard-elasticsearch.svg)](http://mvnrepository.com/artifact/io.dropwizard.modules/dropwizard-elasticsearch)
-->

A set of classes for using [Elasticsearch][1] (version 6.2.3 and higher) in a [Dropwizard][2] application.

The package provides a [lifecycle-managed][3] client class (`ManagedEsClient`), a configuration class with the most
common options (`EsConfiguration`), and some [health checks][4] which can instantly be used in any Dropwizard application.

[1]: http://www.elastic.co/
[2]: http://dropwizard.io/1.2.0/docs
[3]: http://dropwizard.io/1.2.0/docs/manual/core.html#managed-objects
[4]: http://dropwizard.io/1.2.0/docs/manual/core.html#health-checks


Usage
-----

Just add `EsConfiguration` to your [Configuration](http://dropwizard.io/1.3.0/docs/manual/core.html#configuration) class and
create an `ManagedEsClient` instance in the run method of your service.

You can also add one of the existing health checks to your [Environment](http://dropwizard.io/1.3.0/docs/manual/core.html#environments)
in the same method. At least the usage of `EsClusterHealthCheck` is strongly advised.


    public class DemoApplication extends Application<DemoConfiguration> {
        // [...]
        @Override
        public void run(DemoConfiguration config, Environment environment) {
            final ManagedEsClient managedClient = new ManagedEsClient(configuration.getEsConfiguration());
            environment.lifecycle().manage(managedClient);
            environment.healthChecks().register("ES cluster health", new EsClusterHealthCheck(managedClient.getClient()));
            // [...]
        }
    }


Configuration
-------------

The following configuration settings are supported by `EsConfiguration`:

* `transportClient`: When `true`, `ManagedEsClient` will create a `TransportClient`, otherwise a `RestClient`; default: `false`
* `servers`: A list of servers for usage with the created client.
* `clusterName`: The name of the Elasticsearch cluster; default: "elasticsearch" (TransportClient only)
* `settings`: Any additional settings for Elasticsearch, see
[Configuration](https://www.elastic.co/guide/en/elasticsearch/reference/6.2/setup-configuration.html) (TransportClient only)
* `settingsFile`: Any additional settings file for Elasticsearch, see
[Configuration](https://www.elastic.co/guide/en/elasticsearch/reference/2.4/setup-configuration.html) (TransportClient only)
* `headers`: Any additional headers that should be sent (RestClient only)
* `sniffer`: Sniffer configuration (RestClient only)
  * `enabled`: Should the Sniffer be enabled; default: `false`
  * `snifferIntervalMillis`: Interval between sniffer checks; default `600000`
  * `sniffOnFailure`: Should the sniffer use a failure listener; default `false`
  * `sniffFailureMillis`: Interval between checks after a failure; default `30000`
  * `useHttps`: Should the sniffer use HTTPS to check nodes; default `false`

An example configuration file for creating a Transport Client could like this:

    transportClient: true
    clusterName: MyClusterName
    servers: [ "localhost:9300" ]
    settings:
      node.name: MyCustomNodeName

The order of precedence is: `transportClient`/`servers`/`clusterName` > `settings` > `settingsFile`, meaning that
any setting in `settingsFile` can be overwritten with `settings` which in turn get overwritten by the specific settings
like `clusterName`.


### Notes about the TransportClient

The TransportClient is deprecated, and you are encouraged to use the
RestClient. This has the advantage of not being tied to the specific
Elasticsearch version being used, but does require more detailed code
for lower-level methods (such as cluster health, retrieve aliases, etc.)

The NodeClient was removed in Elasticsearch 5.x, and it is no longer
valid in this connector.

The suggested alternative is to launch a local coordinating node, with whichever plugins you require,
and use the Transport or Rest client to communicate with that. The coordinating node should join your cluster.

See [Connecting a Client to a Coordinating Only Node](https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/client-connected-to-client-node.html)


Maven Artifacts
---------------

To install using Maven, this github repository can be added to your
pom.xml:

    <repositories>
      <repository>
        <id>dropwizard-elasticsearch-mvn-repo</id>
        <url>https://raw.github.com/mattflax/dropwizard-elasticsearch6/mvn-repo/</url>
        <snapshots>
          <enabled>true</enabled>
          <updatePolicy>always</updatePolicy>
        </snapshots>
      </repository>
    </repositories>

Once that has been done, the project can be added as a regular dependency:

    <dependency>
      <groupId>io.dropwizard.modules</groupId>
      <artifactId>dropwizard-elasticsearch6</artifactId>
      <version>1.3.1-0-SNAPSHOT</version>
    </dependency>


<!--
This project is available on Maven Central. To add it to your project simply add the following dependencies to your
`pom.xml`:

    <dependency>
      <groupId>io.dropwizard.modules</groupId>
      <artifactId>dropwizard-elasticsearch</artifactId>
      <version>1.1.0-1</version>
    </dependency>
-->


Support
-------

Please file bug reports and feature requests in [GitHub issues](https://github.com/dropwizard/dropwizard-elasticsearch/issues).


Acknowledgements
----------------

This project was forked from the initial Dropwizard Elasticsearch connector.

Thanks to Alexander Reelsen (@spinscale) for his [Dropwizard Blog Sample](https://github.com/spinscale/dropwizard-blog-sample)
which sparked the idea for this project.


License
-------

Copyright (c) 2013-2017 Jochen Schalanda

This library is licensed under the Apache License, Version 2.0.

See http://www.apache.org/licenses/LICENSE-2.0.html or the LICENSE file in this repository for the full license text.
