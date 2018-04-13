# af-poe/database: A Relational Database Model

  * gathers common practices, recipes and tips for creating and evolving a project's relational database model
  * the resulting (_jar_) artifact contains SQL scripts ([loadable from the classpath](https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResourceAsStream%28java.lang.String%29)
    and organized by dialect) and (eventually) [Flyway Java-based migrations](https://flywaydb.org/getstarted/java) to
    _drop_, _create_ and _migrate_ a relational database schema for the _af-poe_ project
  * currently only the MariaDB SQL dialect is explicitly supported Java-based migrations
    should probably be dialect-agnostic
  * (at least the initial) migrations are organized by feature: upon execution, one migration fully creates the
    model for one feature (e.g. an expense journal with entries and categories, a user model, an audit model...)
  * leverages the [Flyway Maven plugin](https://flywaydb.org/documentation/maven/) to allow evolving the schema
    from the command line

## Basic Usage

## TODO

### User Model

01. Read
    * [User Authentication Best Practices Checklist](https://techblog.bozho.net/user-authentication-best-practices-checklist/)
    * [Implementing User Authentication the Right Way](http://stackabuse.com/implementing-user-authentication-the-right-way/)
    * [Building The Optimal User Database Model For Your Application](https://www.getdonedone.com/building-the-optimal-user-database-model-for-your-application/)
    * [Modeling users](https://www.railstutorial.org/book/modeling_users)
   
02. Implement, as migration, a basic user model
    * ```yaml
      user:
        - login       # natural / business key; [\w-.]{6,24}
        - email       # not null
        - password    # not null, hashed
        - active      # Y/N
        - displayName # .{1,256}
      ```

### Audit Model

   01. Read
       * [Database design for audit logging](https://stackoverflow.com/questions/2015232/database-design-for-audit-logging)
       * [What is the best design for an audit/history table?](https://www.quora.com/What-is-the-best-design-for-an-audit-history-table)
       * [http://ronaldbradford.com/blog/auditing-your-mysql-data-2008-07-15/](http://ronaldbradford.com/blog/auditing-your-mysql-data-2008-07-15/)
   
   02. Implement, as migration, a basic audit model
       * extra columns added to audited tables (all initially); while the semantics of ```created_on``` and
         ```modififed_on``` is to point to ```user.login```, we will probably not want to define foreign keys for them
         ```yaml
         - created_by  # not null, [\w-.]{6,24}
         - created_on  # not null
         - modified_by # not null, [\w-.]{6,24}
         - modified_on # not null
         ```
       * trigger(s) (before) INSERT and UPDATE to ensure the extra columns are populated

## Notes To Self

### Why we initially considered using _*pom*_ packaging
  * [Answer to SO Question: What is “pom” packaging in maven?](https://stackoverflow.com/a/25545817/272939)
  * [SO Question: Maven best practice for creating ad hoc zip artifact](https://stackoverflow.com/questions/7837778/maven-best-practice-for-creating-ad-hoc-zip-artifact)
  * [Plugin bindings for pom packaging](http://maven.apache.org/ref/3.3.3/maven-core/default-bindings.html#Plugin_bindings_for_pom_packaging)
