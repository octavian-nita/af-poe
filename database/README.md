# af-poe: A Relational Database Model

 * contains SQL scripts (and possibly more in the future) to drop, create and
   evolve the _af-poe_ relational database schema
 * currently only MariaDB is explicitly supported
 * the initial (V1.0.0) scripts are organized by feature: upon execution, one
   script should completely create the model for one feature (e.g. an expense
   journal with entries and categories, a user model, an audit model, etc.)
 * organized so as to leverage [Flyway](https://flywaydb.org/) migrations
   through the latter's [Maven plugin](https://flywaydb.org/documentation/maven/)
 * assuming a database server running and accessible at an agreed upon url:
   * `mvn clean` drops the database schema and main user, asking for confirmation
     and admin credentials
   * `mvn install` creates the database schema and main user, asking for admin credentials
   * `mvn flyway:*` performs as described [here](https://flywaydb.org/documentation/maven/)

# TODO

 * basic user model
   ```yaml
   user:
   - login       # natural / business key; [\w-.]{6,24}
   - email       # not null
   - password    # not null, hashed
   - active      # Y/N
   - displayName # .{1,64}
   ```
