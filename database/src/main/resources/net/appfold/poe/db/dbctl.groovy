package net.appfold.poe.db

switch (cmd = env('cmd').toLowerCase()) {

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
    def confirm = env('confirm').toLowerCase()
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
            confirm = console.readLine('Drop the database?  [y/N]: ').trim().toLowerCase()
        }
    }

    if (!(confirm in yes)) {
        println "Use -Dconfirm=[${yes.join('|')}] to confirm database dropping"
        return 1
    }

    withDbAdminCredentials { username, password ->
        println "${username}:${password}"
        println 'Dropping database...'
    }
}

def private withDbAdminCredentials(callback) {
    String username = env('dbAdminUsername')
    char[] password = env('dbAdminPassword').getBytes()

    def console = System.console()
    if (console) {
        if (!username) {
            username = console.readLine('DB admin username [admin]: ').trim()
            if (!username) {
                username = 'admin'
            }
        }
        if (!password) {
            password = console.readPassword('DB admin password: ')
        }
    }

    try {
        callback(username, password)
    } finally {
        if (password) {
            Arrays.fill(password, ' ' as char)
        }
    }
}

def private env(String name, String defaultValue = '') {
    ((binding.variables[name] ?: System.properties[name] ?: defaultValue) as String).trim()
}
