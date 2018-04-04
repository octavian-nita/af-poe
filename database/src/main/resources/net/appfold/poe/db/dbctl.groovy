package net.appfold.poe.db

import org.flywaydb.core.internal.configuration.ConfigUtils
import org.flywaydb.core.internal.util.logging.console.ConsoleLogCreator

import java.nio.file.Files
import java.nio.file.Paths

import static java.lang.System.*
import static java.lang.reflect.Modifier.*
import static org.flywaydb.core.api.logging.LogFactory.getLog
import static org.flywaydb.core.api.logging.LogFactory.setFallbackLogCreator
import static org.flywaydb.core.internal.configuration.ConfigUtils.*
import static org.flywaydb.core.internal.util.Location.FILESYSTEM_PREFIX
import static org.flywaydb.core.internal.util.StringUtils.tokenizeToStringArray
import static org.flywaydb.core.internal.util.StringUtils.trimOrPad
import static org.flywaydb.core.internal.util.logging.console.ConsoleLog.Level.*

initFlywayLogging()
switch (cmd = cfg('db.cmd').toLowerCase()) {

case 'create':
case 'drop':
    "$cmd"(loadFlywayConfig())
    break

default:
    err.println 'Use -Ddb.cmd=[create|drop] to create or drop, respectively, the database schema and main user'
    throw new RuntimeException(
        cmd ? "Database control command '${cmd}' not supported!" : 'No database control command has been specified!')
}

def create(Properties config) { println 'create...' }

def drop(Properties config) {
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

        def console = console()
        if (console) {
            confirm = console.readLine('Drop the database?  [y/N]: ').trim().toLowerCase()
        }
    }

    if (!(confirm in yes)) {
        err.println "Use -Ddb.confirmDrop=[${yes.join('|')}] to confirm database dropping"
        return 1
    }

    withDbAdminCredentials { username, password -> println "${username}:${password}"
    }
}

private withDbAdminCredentials(callback) {
    String username = cfg('db.adminUsername')
    char[] password = cfg('db.adminPassword').getBytes()

    def console = console()
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

private initFlywayLogging() {
    def level
    switch (cfg('flyway.logLevel')) {
    case ['X', '-X']:
        level = DEBUG
        break
    case ['q', '-q']:
        level = WARN
        break
    default:
        level = INFO
        break
    }
    setFallbackLogCreator(new ConsoleLogCreator(level))
}

private Properties loadFlywayConfig() {
    def final config = new Properties()
    def final cfgEnc = cfg(CONFIG_FILE_ENCODING, 'UTF-8')
    def final loadOpt = ConfigUtils.&loadConfigurationFile.rcurry(cfgEnc, false)
    def final loadMnd = ConfigUtils.&loadConfigurationFile.rcurry(cfgEnc, true)

    // Defaults:
    config.put(LOCATIONS,
               cfg(LOCATIONS, { FILESYSTEM_PREFIX + absolutePath(cfg('db.dialect'), 'migration', Files.&isDirectory) }))

    // Configuration files:
    config.putAll(loadOpt(new File(absolutePath('conf', CONFIG_FILE_NAME))))
    config.putAll(loadOpt(new File(properties['user.home'] as String, CONFIG_FILE_NAME)))
    config.putAll(loadOpt(new File(CONFIG_FILE_NAME)))

    def configFiles = cfg(CONFIG_FILE)
    if (configFiles) {
        getLog(getClass()).warn('configFile is deprecated and will be removed in Flyway 6.0. Use configFiles instead.')
        config.putAll(loadMnd(new File(configFiles)))
    }
    configFiles = cfg(CONFIG_FILES)
    if (configFiles) {
        for (String configFile : tokenizeToStringArray(configFiles, ',')) {
            config.putAll(loadMnd(new File(configFile)))
        }
    }

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

    config.remove(JAR_DIRS)
    config.remove(CONFIG_FILE)
    config.remove(CONFIG_FILES)
    config.remove(CONFIG_FILE_ENCODING)

    def final log = getLog(getClass())
    log.debug("Using configuration:")
    for (def entry : config.entrySet()) {
        def value = entry.value as String
        value = PASSWORD == entry.key ? trimOrPad('', value.length(), '*' as char) : value
        log.debug("${entry.key} -> ${value}")
    }

    return config
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

private String cfg(String name, def defaultOrProvider = '') {
    // Maven / Gradle project passed in bindings?
    def final project = binding.variables.project

    //@fmt:off
    return ((properties[name]                                 ?:
             project?.properties?.get(name)                   ?:
             project?.hasProperty(name)?.getProperty(project) ?:
             binding.variables[name]                          ?:
             getenv(name)                                     ?://@fmt:on
             (defaultOrProvider instanceof Closure ? defaultOrProvider.call() : defaultOrProvider)) as String).trim()
}
