package net.appfold.poe.db

ctlCommand = ctlCommand == null ? '' : String.valueOf(ctlCommand).trim().toLowerCase()
switch (ctlCommand) {
    case 'migrate':
    case 'create':
    case 'drop':
        "$ctlCommand"()
        break
    case '':
        migrate()
        break
    default:
        throw new RuntimeException("Database control command '${ctlCommand}' not supported!")
}

def migrate() { println 'migrate...' }

def create() { println 'create...' }

def drop() { println 'drop...' }

def propertyMissing(String name) { getClass().superclass.metaClass."$name" = System.getProperty(name) }
