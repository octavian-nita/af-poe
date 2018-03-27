package net.appfold.poe.db

switch (cmd = env('cmd')) {

    case 'migrate':
    case 'create':
    case 'drop':
        "$cmd"()
        break

    case '':
        migrate()
        break

    default:
        throw new RuntimeException("Database control command '${cmd}' not supported!")
}

def migrate() { println 'migrate...' }

def create() { println 'create...' }

def drop() {
    def confirm = env('confirm')
    def final yes = ['y', 'yes', 'true']

    if (!(confirm in yes)) {
        println ''
        println '!! WARNING !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!'
        println '!!                                                          !!'
        println '!! Serious data loss might occur when dropping the database !!'
        println '!!                                                          !!'
        println '!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!'
        println ''

        def console = System.console()
        if (console) {
            confirm = console.readLine('Drop the database [y/N]: ').trim().toLowerCase()
        }
    }

    if (!(confirm in yes)) {
        println "Use -Dconfirm=[${yes.join('|')}] to confirm database dropping"
        return 1
    }

    println ''
    def (username, char[] password) = dbAdminCredentials()
    println ''

    println 'Dropping database...'

    Arrays.fill(password, ' ' as char)
}

def private env(String name, String defaultValue = '') {
    ((binding.variables[name] ?: System.properties[name] ?: defaultValue) as String).trim().toLowerCase()
}

def private dbAdminCredentials() {
    def console = System.console()
    if (console) {
        [console.readLine('DB admin username: ').trim(),
         console.readPassword('DB admin password: ')]
    } else {
        ['', [' '] as char[]]
    }
}
