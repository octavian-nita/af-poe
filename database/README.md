# af-poe/database: A Relational Database Model

This project / module tries to extract and outline _common wisdom_ (practices, recipes and tips) employed when
_setting up_ and _maintaining_ a _relational database schema_ and modeling common requirements such as _users_
and _data auditing_. The primary goal is not to come up with the best, most comprehensive model we can but with
a _simple_, _good-enough_ one that can be easily assimilated.

The project being organised as a [Maven](https://maven.apache.org/) module, the resulting (_jar_) artifact contains:

* _SQL scripts_, organized by dialect, that can be [loaded from the classpath](
https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResourceAsStream%28java.lang.String%29)
* (eventually) compiled _[Flyway Java-based migrations](https://flywaydb.org/getstarted/java)_
* database and migration configuration file(s)

that can be used to _drop_, _create_ and / or _migrate_ the _[af-poe](https://github.com/octavian-nita/af-poe)_ project
schema. Currently, only the MariaDB SQL dialect is explicitly supported; Java-based migrations should generally be
dialect-agnostic.

At least the initial migrations are _organized by feature_: upon execution, one migration fully creates the model for
one feature (e.g. an expense journal with entries and categories, a user model, a data audit model).

## Basic Usage

The project leverages the [Flyway Maven plugin](https://flywaydb.org/documentation/maven/) to allow evolving the schema
from the command line.

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

## Notes

### Do use

* [SQL Style Guide](http://www.sqlstyle.guide/)

### Why we initially considered using _*pom*_ packaging
* [Answer to SO Question: What is “pom” packaging in maven?](https://stackoverflow.com/a/25545817/272939)
* [SO Question: Maven best practice for creating ad hoc zip artifact](https://stackoverflow.com/questions/7837778/maven-best-practice-for-creating-ad-hoc-zip-artifact)
* [Plugin bindings for pom packaging](http://maven.apache.org/ref/3.3.3/maven-core/default-bindings.html#Plugin_bindings_for_pom_packaging)
