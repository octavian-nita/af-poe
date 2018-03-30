package net.appfold.poe.db

import org.flywaydb.core.api.logging.Log
import org.flywaydb.core.api.logging.LogFactory
import org.flywaydb.core.internal.configuration.ConfigUtils
import org.flywaydb.core.internal.util.logging.console.ConsoleLogCreator

import java.nio.file.Path
import java.nio.file.Paths

import static java.nio.file.Files.isDirectory
import static org.flywaydb.core.api.logging.LogFactory.setFallbackLogCreator
import static org.flywaydb.core.internal.configuration.ConfigUtils.JAR_DIRS
import static org.flywaydb.core.internal.configuration.ConfigUtils.LOCATIONS
import static org.flywaydb.core.internal.util.logging.console.ConsoleLog.Level.*

//println env('basedir', new File(getLocationOnDisk(getClass())).parentFile.parentFile.absolutePath)
println sqlScriptsAbsolutePath()
return

switch (cmd = env('db.cmd').toLowerCase()) {

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
        println "Use -Ddb.confirmDrop=[${yes.join('|')}] to confirm database dropping"
        return 1
    }

    withDbAdminCredentials { username, password -> println "${username}:${password}" }
}

private String env(String name, def defaultOrProvider = '') {
    def final project = binding.variables.project // eventual Maven / Gradle project passed in the script bindings
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

    properties.put(LOCATIONS, env(LOCATIONS, { -> 'filesystem:' + sqlScriptsAbsolutePath() + 'migration' }))
    properties.put(JAR_DIRS, env(JAR_DIRS, { -> sqlScriptsAbsolutePath() + 'migration/jars' }))

    loadConfigurationFromConfigFiles(properties, args, envVars);
    properties.putAll(envVars);
    overrideConfigurationWithArgs(properties, args);

    filterProperties(properties);

    properties
}

private Path sqlScriptsAbsolutePath() {
    def final basedir = env('basedir', { -> Paths.get('') as String })

    def sqlScriptsRelativePath = env('db.dialect'),
        sqlScriptsAbsolutePath


    sqlScriptsAbsolutePath = Paths.get(basedir, sqlScriptsRelativePath)
    if (isDirectory(sqlScriptsAbsolutePath)) {
        return sqlScriptsAbsolutePath.toAbsolutePath().normalize()
    }

    sqlScriptsAbsolutePath = Paths.get(basedir, 'target', 'classes', sqlScriptsRelativePath) // Maven?
    if (isDirectory(sqlScriptsAbsolutePath)) {
        return sqlScriptsAbsolutePath.toAbsolutePath().normalize()
    }

    sqlScriptsRelativePath = (getClass().package.name.split('\\.') + sqlScriptsRelativePath) as String[]

    sqlScriptsAbsolutePath = Paths.get(basedir, sqlScriptsRelativePath) // under package or namespace?
    if (isDirectory(sqlScriptsAbsolutePath)) {
        return sqlScriptsAbsolutePath.toAbsolutePath().normalize()
    }

    sqlScriptsAbsolutePath = Paths.get(basedir, (['target', 'classes'] + sqlScriptsRelativePath) as String[])
    if (isDirectory(sqlScriptsAbsolutePath)) {
        return sqlScriptsAbsolutePath.toAbsolutePath().normalize()
    }

    Paths.get(basedir, sqlScriptsRelativePath).toAbsolutePath().normalize()
}

private File location(Class<?> clazz = getClass()) {
    if (!clazz) {
        return null
    }

    String codeBase = clazz.protectionDomain?.codeSource?.location?.path
    if (!codeBase) {
        def classRelativePath = clazz.name.replace('.', '/') + '.class'
        def classUrl = clazz.classLoader?.getResource(classRelativePath)

        if (!classUrl) {
            classRelativePath = clazz.name.replace('.', '/') + '.groovy'
            classUrl = clazz.classLoader?.getResource(classRelativePath)
            if (!classUrl) {
                return null
            }
        }

        codeBase = classUrl as String
        if ('jar'.equalsIgnoreCase(classUrl.protocol)) {
            codeBase = codeBase.substring(4).replace('!/' + classRelativePath, '')
        } else {
            codeBase = codeBase.replace(classRelativePath, '')
        }
    }

    if (codeBase.startsWith('file:')) {
        codeBase = codeBase.substring(5)
    }

    def final location = new File(codeBase)
    location.exists() ? (location.isFile() ? location.parentFile.absoluteFile : location.absoluteFile) : null
}
