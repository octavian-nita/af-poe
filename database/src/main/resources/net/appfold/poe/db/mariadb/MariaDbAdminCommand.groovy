package net.appfold.poe.db.mariadb

import net.appfold.poe.db.DbAdminCommand

abstract class MariaDbAdminCommand extends DbAdminCommand {

    @Override
    protected String getDriver() { 'org.mariadb.jdbc.Driver' }

    @Override
    protected String getUrl() { 'jdbc:mariadb://localhost:3306/' }

    protected MariaDbAdminCommand(description = 'MariaDB admin command...') { super(description) }
}
