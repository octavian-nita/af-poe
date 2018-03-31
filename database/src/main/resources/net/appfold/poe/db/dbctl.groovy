package net.appfold.poe.db

import org.flywaydb.core.api.logging.Log
import org.flywaydb.core.api.logging.LogFactory
import org.flywaydb.core.internal.configuration.ConfigUtils
import org.flywaydb.core.internal.util.logging.console.ConsoleLogCreator

import java.nio.file.Paths

import static java.lang.System.err
import static java.nio.file.Files.isDirectory
import static org.flywaydb.core.api.logging.LogFactory.setFallbackLogCreator
import static org.flywaydb.core.internal.configuration.ConfigUtils.JAR_DIRS
import static org.flywaydb.core.internal.configuration.ConfigUtils.LOCATIONS
import static org.flywaydb.core.internal.util.logging.console.ConsoleLog.Level.*

switch (cmd = env('db.cmd').toLowerCase()) {

case 'create':
case 'drop':
    "$cmd"()
    break

case '':
    err.println "Use -Ddb.cmd=[create|drop] to create / drop the database schema and main user"
    return 1

default:
    throw new RuntimeException("Database control command '${cmd}' not supported!")
}

def create() { println 'create...' }

def drop() {
    def confirm = env('db.confirmDrop').toLowerCase()
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
        err.println "Use -Ddb.confirmDrop=[${yes.join('|')}] to confirm database dropping"
        return 1
    }

    withDbAdminCredentials { username, password -> println "${username}:${password}" }
}

private String env(String name, def defaultOrProvider = '') {
    // Maven / Gradle project passed in bindings?
    def final project = binding.variables.project
    ((System.properties[name] ?: System.getenv(name) ?: project?.properties?.get(name) ?:
                                                        project?.hasProperty(name)?.getProperty(project) ?:
                                                        binding.variables[name] ?:
                                                        (defaultOrProvider instanceof Closure ?
                                                         defaultOrProvider.call() : defaultOrProvider)) as String).
        trim()
}

private withDbAdminCredentials(callback) {
    String username = env('db.adminUsername')
    char[] password = env('db.adminPassword').getBytes()

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

private String sqlAbsolutePath() {
    def final basedir = env('basedir', { Paths.get('') as String })

    def sqlRelativePath = env('db.dialect'), sqlPath

    sqlPath = Paths.get(basedir, sqlRelativePath)
    if (isDirectory(sqlPath)) {
        return sqlPath.toAbsolutePath().normalize() as String
    }

    sqlPath = Paths.get(basedir, 'target', 'classes', sqlRelativePath) // Maven?
    if (isDirectory(sqlPath)) {
        return sqlPath.toAbsolutePath().normalize() as String
    }

    sqlRelativePath = [*getClass().package.name.split('\\.'), sqlRelativePath]

    sqlPath = Paths.get(basedir, *sqlRelativePath) // under package / namespace?
    if (isDirectory(sqlPath)) {
        return sqlPath.toAbsolutePath().normalize() as String
    }

    sqlPath = Paths.get(basedir, 'target', 'classes', *sqlRelativePath)
    if (isDirectory(sqlPath)) {
        return sqlPath.toAbsolutePath().normalize() as String
    }

    Paths.get(basedir, env('db.dialect')).toAbsolutePath().normalize() as String
}

private Log initFlywayLogging() {
    def level
    switch (env('flyway.logLevel')) {
    case '-X':
        level = DEBUG
        break
    case '-q':
        level = WARN
        break
    default:
        level = INFO
        break
    }
    setFallbackLogCreator(new ConsoleLogCreator(level))
    LogFactory.getLog(getClass());
}

private Properties loadFlywayConfig() {
    def final properties = new Properties()

    def flywayEnv = ConfigUtils.environmentVariablesToPropertyMap()

    properties.put(LOCATIONS, env(LOCATIONS, { 'filesystem:' + Paths.get(sqlAbsolutePath(), 'migration') }))
    properties.put(JAR_DIRS, env(JAR_DIRS, { Paths.get(sqlAbsolutePath(), 'migration', 'jars') }))

    // DROP support (for now) for:
    // * jar migrations
    // * loading credentials from Maven settings.xml

    loadConfigurationFromConfigFiles(properties, args, envVars);
    properties.putAll(envVars);
    overrideConfigurationWithArgs(properties, args);

    filterProperties(properties);

    properties
}
