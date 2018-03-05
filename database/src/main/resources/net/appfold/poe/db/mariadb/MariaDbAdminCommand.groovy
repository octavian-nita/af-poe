package net.appfold.poe.db.mariadb

import groovy.transform.PackageScope
import net.appfold.poe.db.DbAdminCommand

@PackageScope
abstract class MariaDbAdminCommand extends DbAdminCommand {

    private final def description

    @PackageScope
    MariaDbAdminCommand(description = 'MariaDB admin command...') { this.description = description }

    @Override
    protected String getDescription() { description }

    @Override
    protected String getDriver() { 'org.mariadb.jdbc.Driver' }

    @Override
    protected String getUrl() { 'jdbc:mariadb://localhost:3306/' }
}
