package net.appfold.poe.db

import groovy.sql.Sql

abstract class DbAdminCommand {

    protected abstract String getUrl()

    protected abstract String getDriver()

    protected def getUsernameAndPassword() {
        final def console = System.console()

        def username = System.getProperty('db.admin.username')
        if (username == null && console != null) {
            username = console.readLine('Admin username (<ENTER> for root): ')
            if (username.length() == 0) {
                username = 'root'
            }
        }

        def password = System.getProperty('db.admin.password')
        if (password == null && console != null) {
            password = console.readPassword('Admin password: ')
        } else {
            password = password?.toCharArray()
        }

        [username, password]
    }

    protected abstract def execute(sql)

    protected DbAdminCommand(description = 'Database admin command...') { this.description = description }

    final def description

    def execute() {
        printf "%n${description}%n%n"

        final def (String username, char[] password) = getUsernameAndPassword()
        Sql.withInstance(this.url, username, new String(password), this.driver, this.&execute)

        printf '%nDone.%n%n'
    }
}
