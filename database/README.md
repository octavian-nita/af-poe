# af-poe/database: A Relational Database Model for the [af-poe](https://github.com/octavian-nita/af-poe) Project

One of the goals of this module is to extract and outline _common wisdom_ (practices, recipes, tips) employed to _set up_ and _evolve_ a
_relational database schema_ and to _model_ common requirements such as _users_ and _data auditing_. The primary goal is
not to come up with the best, most comprehensive model we could but with a _simple_, yet _real-world_ one that "does the
job" and can easily be assimilated.

The project being organised as a [Maven](https://maven.apache.org/) module, the resulting (_jar_) artifact contains:
* _SQL scripts_, organized by dialect (currently, only the _MariaDB_ SQL dialect is supported)
* (eventually) compiled _[Flyway Java-based migrations](https://flywaydb.org/getstarted/java)_
* database and migration _configuration file(s)_
that can be invoked to _drop_, _create_ and _migrate_ the _[af-poe](https://github.com/octavian-nita/af-poe)_ schema.

Migrations should generally be _organized by feature_: upon execution, one migration fully creates the model for
one feature alone (e.g. one migration creates the expense journal entries and categories, another - the user model etc.)

## Usage

Before building the project or manually creating / migrating the schema, make sure ...

### Automated migrations for JVM-based modules

From a JVM-based project / module, simply depend on the generated artifact and employ the available
[Flyway](https://flywaydb.org/getstarted/java) [API](https://flywaydb.org/documentation/api/).

### Manual schema dropping, creation and migration using Maven

The project leverages the [Flyway](https://flywaydb.org/) [Maven plugin](https://flywaydb.org/documentation/maven/)
to allow executing migrations (as well as other Flyway goals) directly from the command line or during the
[default build lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html). Note that
before executing `mvn flyway:migrate`, `mvn resources:resources` should have already been executed at least once in
order for the database and migration configuration file(s) to be properly generated.

Moreover, by specifying `db.drop` and / or `db.create` properties when executing `mvn install` the user instructs the
build to drop and create, respectively, the af-poe database schema and an associated user with proper access rights
(both named `af_poe` by default). The user will be prompted for a database admin username and password, unless he
specifies the `db.adminUsername` and / or `db.adminPassword` properties as well.

1. Drop the schema when building the project; `force` / `f` inhibits a warning about how dangerous dropping the schema is
   ```
   mvn clean install -Ddb.drop=y
   
   mvn -Ddb.drop=force install
   ```

2. Create the schema when building the project; the admin username is `root`, the admin password is `r00t`
   ```
   mvn clean install -Ddb.create=yes -Ddb.adminUsername=root -Ddb.adminPassword=r00t
   ```

3. The 2 operations can always be combined, no matter in which order; only the admin password prompt will appear now
   ```
   mvn clean -Ddb.drop=f -Ddb.create=y -Ddb.adminUsername=root install
   ```

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
        - login       # natural / business key; [\w-.]{5,24}
        - email       # not null
        - password    # not null, hashed; .{1,18}
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
         - created_by  # not null, [\w-.]{,128} (created_by VARCHAR(128) NOT NULL DEFAULT CURRENT_USER)
         - created_on  # not null               (created_on DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP)
         - modified_by # not null, [\w-.]{,128}
         - modified_on # not null
         ```
       * trigger(s) (before) INSERT and UPDATE to ensure the extra columns are populated

## Resources

* [Database versioning best practices](http://enterprisecraftsmanship.com/2015/08/10/database-versioning-best-practices/)
* [20 Database Design Best Practices](https://dzone.com/articles/20-database-design-best)
* [SQL Style Guide](http://www.sqlstyle.guide/)
