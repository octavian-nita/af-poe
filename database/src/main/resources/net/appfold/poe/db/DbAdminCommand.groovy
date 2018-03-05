package net.appfold.poe.db

abstract class DbAdminCommand {

    protected abstract String getDescription()

    protected abstract String getDriver()

    protected abstract String getUrl()

    protected getUsernameAndPassword() {
        def final console = System.console()

        def username = System.getProperty('poe.username')
        if (username == null && console != null) {
            username = console.readLine('Admin username (<ENTER> for root): ')
            if (username.length() == 0) {
                username = 'root'
            }
        }

        def password = System.getProperty('poe.password')
        if (password == null && console != null) {
            password = console.readPassword('Admin password: ')
        } else {
            password = password?.toCharArray()
        }

        [username, password]
    }

    def execute() {
        printf "%n${this.description}%n%n"

        final def (String username, char[] password) = getUsernameAndPassword()
        println ''
        //Sql.withInstance(this.url, username, new String(password), this.driver, this.&execute)
        println "Username: ${username}"
        println "Password: ${password}"

        printf '%nDone.%n%n'
    }

    protected abstract execute(sql)
}
