package net.appfold.poe.db

import org.flywaydb.core.api.logging.Log
import org.flywaydb.core.api.logging.LogFactory
import org.flywaydb.core.internal.configuration.ConfigUtils
import org.flywaydb.core.internal.util.logging.console.ConsoleLogCreator

import java.nio.file.Files
import java.nio.file.Paths

import static java.lang.reflect.Modifier.*
import static org.flywaydb.core.api.logging.LogFactory.setFallbackLogCreator
import static org.flywaydb.core.internal.configuration.ConfigUtils.*
import static org.flywaydb.core.internal.util.logging.console.ConsoleLog.Level.*

switch (cmd = cfg('db.cmd').toLowerCase()) {

case 'create':
case 'drop':
    "$cmd"()
    break

default:
    err.println 'Use -Ddb.cmd=[create|drop] to create or drop, respectively, the database schema and main user'
    throw cmd ? new RuntimeException("Database control command '${cmd}' not supported!")
              : new RuntimeException('No database control command has been specified!')
}

def create() { println 'create...' }

def drop() {
    def confirm = cfg('db.confirmDrop').toLowerCase()
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
        System.err.println "Use -Ddb.confirmDrop=[${yes.join('|')}] to confirm database dropping"
        return 1
    }

    withDbAdminCredentials { username, password -> println "${username}:${password}" }
}

private String cfg(String name, def defaultOrProvider = '') {
    // Maven / Gradle project passed in bindings?
    def final project = binding.variables.project
    return ((System.properties[name] ?: project?.properties?.get(name) ?:
                                        project?.hasProperty(name)?.getProperty(project) ?:
                                        binding.variables[name] ?: System.getenv(name) ?:
                                                                   (defaultOrProvider instanceof Closure ?
                                                                    defaultOrProvider.call() :
                                                                    defaultOrProvider)) as String).trim()
}

private withDbAdminCredentials(callback) {
    String username = cfg('db.adminUsername')
    char[] password = cfg('db.adminPassword').getBytes()

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

private String absolutePath(String parent = '', String child = '', Closure<?> test = Files.&isRegularFile) {
    parent = parent ?: ''
    child = child ?: ''
    test = test ?: Files.&isRegularFile

    def final basedir = cfg('basedir', { Paths.get('') as String })

    def path = Paths.get(basedir, parent, child)
    if (test(path)) {
        return path.toAbsolutePath().normalize() as String
    }

    path = Paths.get(basedir, 'target', 'classes', parent, child) // Maven?
    if (test(path)) {
        return path.toAbsolutePath().normalize() as String
    }

    def final packagePath = [*getClass().package.name.split('\\.'), parent, child]

    path = Paths.get(basedir, *packagePath) // under package / namespace?
    if (test(path)) {
        return path.toAbsolutePath().normalize() as String
    }

    path = Paths.get(basedir, 'target', 'classes', *packagePath)
    if (test(path)) {
        return path.toAbsolutePath().normalize() as String
    }

    return Paths.get(basedir, parent, child).toAbsolutePath().normalize() as String
}

private Log initFlywayLogging() {
    def level
    switch (cfg('flyway.logLevel')) {
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
    return LogFactory.getLog(getClass());
}

private Properties loadFlywayConfig() {
    def final config = new Properties()
    def final load = ConfigUtils.&loadConfigurationFile.rcurry(cfg(CONFIG_FILE_ENCODING, 'UTF-8'), false)

    // Defaults:
    config.put(LOCATIONS,
               cfg(LOCATIONS, { 'filesystem:' + absolutePath(cfg('db.dialect'), 'migration', Files.&isDirectory) }))

    // Configuration files:
    config.putAll(load(new File(absolutePath('conf', CONFIG_FILE_NAME))))
    config.putAll(load(new File(System.properties['user.home'] as String, CONFIG_FILE_NAME)))
    config.putAll(load(new File(CONFIG_FILE_NAME)))

    //for (File configFile : determineConfigFilesFromArgs(args, envVars)) {
    //    config.putAll(loadConfigurationFile(configFile, encoding, true));
    //}

    // Environment variables & arguments:
    ConfigUtils.class.declaredFields.each { field ->
        def final mod = field.modifiers
        if (String.class == field.getType() && isPublic(mod) && isStatic(mod) && isFinal(mod)) {
            def key = field.get(null) as String
            def val = cfg(key, '')
            if (val) {
                config.put(key, val)
            }
        }
    }

    return config
}
