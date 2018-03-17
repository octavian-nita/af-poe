package net.appfold.poe.db

switch (cmd = input('cmd')) {

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
    def confirm = input('confirm')
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

    println 'Dropping database...'
}

def private input(String name) {
    ((binding.variables[name] ?: System.properties[name] ?: '') as String).trim().toLowerCase()
}
