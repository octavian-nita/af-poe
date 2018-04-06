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
   * `mvn deploy` creates the database schema and main user, asking for admin credentials
   * `mvn flyway:*` performs as described [here](https://flywaydb.org/documentation/maven/)
     (though one should usually execute `mvn resources:resources flyway:*`!)

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

# Notes To Self

  ## Why we initially considered using _*pom*_ packaging

  * [Answer to SO Question: What is “pom” packaging in maven?](https://stackoverflow.com/a/25545817/272939)
  * [SO Question: Maven best practice for creating ad hoc zip artifact](https://stackoverflow.com/questions/7837778/maven-best-practice-for-creating-ad-hoc-zip-artifact)
  * [Plugin bindings for pom packaging](http://maven.apache.org/ref/3.3.3/maven-core/default-bindings.html#Plugin_bindings_for_pom_packaging)
