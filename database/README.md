# af-poe: A Relational Database Model

 * contains SQL scripts (and more) to drop and create the _af-poe_ relational database and, most notably, evolve its schema
 * currently, only MariaDB is explicitly supported
 * the initial (V1.0.0) creation scripts are organized by feature: upon execution, one script should completely create the model for a feature (e.g. an expense journal with entries and categories, a user model, an audit model - even cross-table, etc.)
 * organized so as to leverage [Flyway](https://flywaydb.org/) migrations through a [Maven plugin](https://flywaydb.org/documentation/maven/)
 * assuming a database server running and accessible at an agreed upon url, simply execute
   ```
   mvn
   ```
